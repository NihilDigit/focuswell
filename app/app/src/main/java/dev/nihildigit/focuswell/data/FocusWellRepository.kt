package dev.nihildigit.focuswell.data

import android.content.Context
import dev.nihildigit.focuswell.data.db.RoomFocusWellStore
import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.Idea
import dev.nihildigit.focuswell.domain.IdeaChecklistItem
import dev.nihildigit.focuswell.domain.IdeaQuadrant
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.LeisureRecord
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.TagConfig
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.domain.defaultTags
import dev.nihildigit.focuswell.domain.defaultTrackers
import dev.nihildigit.focuswell.domain.focusOutcomeMultiplier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class FocusWellRepository internal constructor(
  private val store: FocusWellStore,
  private val now: () -> Instant = Instant::now,
) {
  constructor(context: Context) : this(RoomFocusWellStore(context))

  private val _state = MutableStateFlow(loadState())
  val state: StateFlow<FocusWellUiState> = _state

  init {
    settleDailyTrackers()
    ensureDailyGrants()
    rollDailyState()
  }

  fun toggleTracker(id: String) {
    mutate { state ->
      state.copy(
        trackers =
          state.trackers.map {
            if (it.id == id && it.ruleTagName == null) it.copy(completed = !it.completed) else it
          }
      )
    }
  }

  fun setWakeTime(value: String) {
    val parsed = runCatching { LocalTime.parse(value.trim()) }.getOrNull() ?: return
    mutate { state ->
      state.copy(
        trackers =
          state.trackers.map {
            if (it.id == "wake") {
              it.copy(
                wakeTime = parsed.toString(),
                completed = !parsed.isAfter(LocalTime.of(9, 0)),
              )
            } else {
              it
            }
          }
      )
    }
  }

  fun addTag(name: String, multiplier: Double) {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return
    mutate { state ->
      if (state.tags.any { it.name.equals(trimmed, ignoreCase = true) && it.archivedAt == null }) {
        state
      } else {
        state.copy(
          tags =
            state.tags +
              TagConfig(
                id = "tag-${System.currentTimeMillis()}",
                name = trimmed,
                multiplier = multiplier.coerceAtLeast(0.0),
              )
        )
      }
    }
  }

  fun archiveTag(id: String) {
    mutate { state ->
      state.copy(tags = state.tags.map { if (it.id == id) it.copy(archivedAt = now()) else it })
    }
  }

  fun updateTag(id: String, name: String, multiplier: Double) {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return
    mutate { state ->
      if (state.tags.any { it.id != id && it.name.equals(trimmed, ignoreCase = true) && it.archivedAt == null }) {
        state
      } else {
        state.copy(
          tags =
            state.tags.map {
              if (it.id == id) it.copy(name = trimmed, multiplier = multiplier.coerceAtLeast(0.0)) else it
            }
        )
      }
    }
  }

  fun addBooleanTracker(label: String, rewardMinutes: Double) {
    val trimmed = label.trim()
    if (trimmed.isEmpty()) return
    mutate { state ->
      state.copy(
        trackers =
          state.trackers +
            DailyTracker(
              id = "tracker-${System.currentTimeMillis()}",
              label = trimmed,
              completed = false,
              rewardMinutes = rewardMinutes.coerceAtLeast(0.0),
            )
      )
    }
  }

  fun addRuleTracker(label: String, tagName: String, targetMinutes: Double, rewardMinutes: Double) {
    val trimmedLabel = label.trim()
    val trimmedTag = tagName.trim()
    if (trimmedLabel.isEmpty() || trimmedTag.isEmpty() || targetMinutes <= 0.0) return
    mutate { state ->
      state.copy(
        trackers =
          state.trackers +
            DailyTracker(
              id = "rule-${System.currentTimeMillis()}",
              label = trimmedLabel,
              completed = false,
              rewardMinutes = rewardMinutes.coerceAtLeast(0.0),
              ruleTagName = trimmedTag,
              ruleTargetMinutes = targetMinutes,
            )
      )
    }
  }

  fun archiveTracker(id: String) {
    mutate { state ->
      state.copy(
        trackers = state.trackers.map { if (it.id == id) it.copy(archivedAt = now()) else it }
      )
    }
  }

  fun updateManualTracker(id: String, label: String, rewardMinutes: Double) {
    val trimmed = label.trim()
    if (trimmed.isEmpty()) return
    mutate { state ->
      state.copy(
        trackers =
          state.trackers.map {
            if (it.id == id) {
              it.copy(label = trimmed, rewardMinutes = rewardMinutes.coerceAtLeast(0.0))
            } else {
              it
            }
          }
      )
    }
  }

  fun updateRuleTracker(id: String, label: String, tagName: String, targetMinutes: Double, rewardMinutes: Double) {
    val trimmedLabel = label.trim()
    val trimmedTag = tagName.trim()
    if (trimmedLabel.isEmpty() || trimmedTag.isEmpty() || targetMinutes <= 0.0) return
    mutate { state ->
      state.copy(
        trackers =
          state.trackers.map {
            if (it.id == id) {
              it.copy(
                label = trimmedLabel,
                rewardMinutes = rewardMinutes.coerceAtLeast(0.0),
                ruleTagName = trimmedTag,
                ruleTargetMinutes = targetMinutes,
              )
            } else {
              it
            }
          }
      )
    }
  }

  fun updateRules(rules: FocusWellRules) {
    mutate { state -> state.copy(rules = rules.normalized()) }
  }

  fun completeMorningCheckIn(
    checkInStartedAt: Instant,
    phoneCostMinutes: Double,
    reviewedSegmentCount: Int,
  ) {
    mutate { state ->
      val today = TimeAccounting.dailyDate(checkInStartedAt, rules = state.rules)
      val todayText = today.toString()
      if (state.lastCheckInDailyDate == todayText) return@mutate state

      val existingIds = state.ledger.mapTo(mutableSetOf()) { it.id }
      val entries = mutableListOf<LedgerEntry>()
      val wakeBonus = wakeBonusEntry(state, today, checkInStartedAt, existingIds)
      if (wakeBonus != null) entries += wakeBonus

      val available = (state.ledger.sumOf { it.deltaMinutes } + entries.sumOf { it.deltaMinutes }).coerceAtLeast(0.0)
      val safePhoneCost = phoneCostMinutes.coerceAtLeast(0.0)
      val deducted = minOf(safePhoneCost, available)
      val exceeded = safePhoneCost > available
      if (safePhoneCost > 0.0 || reviewedSegmentCount > 0) {
        entries +=
          LedgerEntry(
            id = "phone-checkin-$todayText-${checkInStartedAt.toEpochMilli()}",
            title = if (exceeded) "Phone usage cleared reserve" else "Phone usage",
            deltaMinutes = -deducted,
            createdAt = checkInStartedAt,
            note =
              if (exceeded) {
                "Detected ${safePhoneCost.roundMinutes()}; cleared ${deducted.roundMinutes()}; daily grant paused for 3 days."
              } else {
                "Detected ${safePhoneCost.roundMinutes()} after Fair Use across $reviewedSegmentCount segment${if (reviewedSegmentCount == 1) "" else "s"}."
              },
          )
      }

      state.copy(
        lastCheckInDailyDate = todayText,
        dailyGrantPausedUntilDate =
          if (exceeded) today.plusDays(3).toString() else state.dailyGrantPausedUntilDate,
        ledger = entries.asReversed() + state.ledger,
      )
    }
  }

  fun startFocus(task: String, type: SessionType, tagId: String?): ActiveMode.Focus? {
    val trimmed = task.trim().ifBlank { "Focus session" }
    val now = now()
    val sessionId = "focus-${now.toEpochMilli()}"
    var activeFocus: ActiveMode.Focus? = null
    mutate { state ->
      val tag = tagId?.let { selectedId -> state.tags.firstOrNull { it.id == selectedId } }
      val focus =
        ActiveMode.Focus(
          task = trimmed,
          type = type,
          tag = tag,
          startedAt = now,
          reminderSessionId = sessionId,
        )
      activeFocus = focus
      state.copy(
        activeMode = focus
      )
    }
    return activeFocus
  }

  fun pauseFocus() {
    mutate { state ->
      val focus = state.activeMode as? ActiveMode.Focus ?: return@mutate state
      if (focus.paused) state else state.copy(activeMode = focus.copy(paused = true, pausedAt = now()))
    }
  }

  fun resumeFocus() {
    mutate { state ->
      val focus = state.activeMode as? ActiveMode.Focus ?: return@mutate state
      val pausedAt = focus.pausedAt
      val extraPaused =
        if (pausedAt == null) 0L else Duration.between(pausedAt, now()).toMillis().coerceAtLeast(0)
      state.copy(
        activeMode =
          focus.copy(
            paused = false,
            pausedAt = null,
            pausedDurationMillis = focus.pausedDurationMillis + extraPaused,
          )
      )
    }
  }

  fun endFocus(result: String, correctionMinutes: Double = 0.0): String? {
    var reminderSessionId: String? = null
    mutate { state ->
      val focus = state.activeMode as? ActiveMode.Focus ?: return@mutate state
      val now = now()
      reminderSessionId = focus.reminderSessionId
      val currentPauseMillis =
        if (focus.paused && focus.pausedAt != null) {
          Duration.between(focus.pausedAt, now).toMillis().coerceAtLeast(0)
        } else {
          0L
        }
      val activeDuration =
        Duration.between(focus.startedAt, now)
          .minusMillis(focus.pausedDurationMillis + currentPauseMillis)
          .coerceAtLeast(Duration.ZERO)
      val activeDurationMinutes = activeDuration.toMillis() / 60_000.0
      val adjustedActiveMinutes = (activeDurationMinutes - correctionMinutes.coerceAtLeast(0.0)).coerceAtLeast(0.0)
      val savedResult = result.ifBlank { "As planned" }
      val earned =
        TimeAccounting.focusEarnedMinutes(
          activeDurationMinutes = adjustedActiveMinutes,
          typeRate = focus.type.rate,
          tagMultiplier = focus.tag?.multiplier ?: 1.0,
          outcomeMultiplier = focusOutcomeMultiplier(savedResult),
        )
      val record =
        FocusRecord(
          id = "focus-${now.toEpochMilli()}",
          task = focus.task,
          result = savedResult,
          type = focus.type,
          tagName = focus.tag?.name,
          tagMultiplier = focus.tag?.multiplier ?: 1.0,
          typeRate = focus.type.rate,
          startedAt = focus.startedAt,
          endedAt = now,
          activeDurationMinutes = adjustedActiveMinutes,
          earnedMinutes = earned,
          dailyDate = TimeAccounting.dailyDate(now, rules = state.rules).toString(),
        )
      val entry =
        LedgerEntry(
          id = "ledger-${record.id}",
          title = "Focus · ${focus.type.label}${focus.tag?.let { " ${it.name}" } ?: ""}",
          deltaMinutes = earned,
          createdAt = now,
          note = record.result,
          sourceId = record.id,
        )
      state.copy(
        reserveMinutes = state.reserveMinutes + earned,
        activeMode = ActiveMode.None,
        focusRecords = listOf(record) + state.focusRecords,
        ledger = listOf(entry) + state.ledger,
      )
    }
    return reminderSessionId
  }

  fun startLeisure(): ActiveMode.Leisure? {
    val now = now()
    val sessionId = "leisure-${now.toEpochMilli()}"
    var activeLeisure: ActiveMode.Leisure? = null
    mutate { state ->
      if (state.reserveMinutes <= 0.0) state
      else {
        val leisure = ActiveMode.Leisure(startedAt = now, reminderSessionId = sessionId)
        activeLeisure = leisure
        state.copy(activeMode = leisure)
      }
    }
    return activeLeisure
  }

  fun endLeisure(): String? {
    var reminderSessionId: String? = null
    mutate { state ->
      val leisure = state.activeMode as? ActiveMode.Leisure ?: return@mutate state
      val now = now()
      reminderSessionId = leisure.reminderSessionId
      val rawCost = TimeAccounting.leisureCostMinutes(leisure.startedAt, now, rules = state.rules)
      val depleted = rawCost >= state.reserveMinutes
      val effectiveEndedAt =
        if (depleted) {
          TimeAccounting.instantWhenLeisureCostReaches(leisure.startedAt, state.reserveMinutes, rules = state.rules)
        } else {
          now
        }
      val cost = if (depleted) state.reserveMinutes else rawCost
      val elapsed = Duration.between(leisure.startedAt, effectiveEndedAt).toMillis().coerceAtLeast(0) / 60_000.0
      val record =
        LeisureRecord(
          id = "leisure-${now.toEpochMilli()}",
          startedAt = leisure.startedAt,
          endedAt = effectiveEndedAt,
          elapsedMinutes = elapsed,
          costMinutes = cost,
          dailyDate = TimeAccounting.dailyDate(effectiveEndedAt, rules = state.rules).toString(),
        )
      val entry =
        LedgerEntry(
          id = "ledger-${record.id}",
          title = "Leisure",
          deltaMinutes = -cost,
          createdAt = now,
          sourceId = record.id,
        )
      state.copy(
        reserveMinutes = state.reserveMinutes - cost,
        activeMode = if (state.reserveMinutes - cost <= 0.0) ActiveMode.Depleted else ActiveMode.None,
        leisureRecords = listOf(record) + state.leisureRecords,
        ledger = listOf(entry) + state.ledger,
      )
    }
    return reminderSessionId
  }

  fun endDepleted() {
    mutate { it.copy(activeMode = ActiveMode.None) }
  }

  fun clearAllData() {
    store.clear()
    val seeded = seedState()
    store.persistChange(previous = null, next = seeded)
    _state.value = seeded
    ensureDailyGrants()
    rollDailyState()
  }

  fun addIdea(text: String) {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return
    val createdAt = now()
    mutate { state ->
      state.copy(
        ideas =
          listOf(
            Idea(
              id = "idea-${createdAt.toEpochMilli()}",
              text = trimmed,
              quadrant = IdeaQuadrant.Inbox,
              createdAt = createdAt,
              updatedAt = createdAt,
            )
          ) + state.ideas
      )
    }
  }

  fun moveIdea(id: String, quadrant: IdeaQuadrant) {
    val updatedAt = now()
    mutate { state ->
      state.copy(
        ideas =
          state.ideas.map {
            if (it.id == id && it.archivedAt == null) it.copy(quadrant = quadrant, updatedAt = updatedAt) else it
          }
      )
    }
  }

  fun updateIdea(id: String, text: String, checklist: List<IdeaChecklistItem>) {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return
    val updatedAt = now()
    val cleanedChecklist =
      checklist
        .map { it.copy(text = it.text.trim()) }
        .filter { it.text.isNotEmpty() }
    mutate { state ->
      state.copy(
        ideas =
          state.ideas.map {
            if (it.id == id && it.archivedAt == null) {
              it.copy(text = trimmed, checklist = cleanedChecklist, updatedAt = updatedAt)
            } else {
              it
            }
          }
      )
    }
  }

  fun archiveIdea(id: String) {
    val archivedAt = now()
    mutate { state ->
      state.copy(
        ideas =
          state.ideas.map {
            if (it.id == id && it.archivedAt == null) it.copy(archivedAt = archivedAt, updatedAt = archivedAt) else it
          }
      )
    }
  }

  fun exportJson(): String = stateToJson(_state.value).toString(2)

  fun importJson(raw: String): Boolean {
    val imported = runCatching { jsonToState(JSONObject(raw)) }.getOrNull() ?: return false
    val normalized = imported.withLedgerBackedReserve()
    store.clear()
    store.persistChange(previous = null, next = normalized)
    _state.value = normalized
    ensureDailyGrants()
    rollDailyState()
    return true
  }

  fun deleteFocusRecord(id: String) {
    mutate { state ->
      val record = state.focusRecords.firstOrNull { it.id == id && it.deletedAt == null } ?: return@mutate state
      val now = now()
      val adjustment =
        LedgerEntry(
          id = "delete-$id-${now.toEpochMilli()}",
          title = "Deleted focus",
          deltaMinutes = -record.earnedMinutes,
          createdAt = now,
          note = record.task,
          sourceId = id,
        )
      state.copy(
        reserveMinutes = (state.reserveMinutes - record.earnedMinutes).coerceAtLeast(0.0),
        focusRecords = state.focusRecords.map { if (it.id == id) it.copy(deletedAt = now) else it },
        ledger = listOf(adjustment) + state.ledger,
      )
    }
  }

  fun updateFocusRecord(id: String, result: String, activeMinutes: Double) {
    mutate { state ->
      val record = state.focusRecords.firstOrNull { it.id == id && it.deletedAt == null } ?: return@mutate state
      val safeMinutes = activeMinutes.coerceAtLeast(0.0)
      val savedResult = result.ifBlank { record.result }
      val newEarned =
        TimeAccounting.focusEarnedMinutes(
          activeDurationMinutes = safeMinutes,
          typeRate = record.typeRate,
          tagMultiplier = record.tagMultiplier,
          outcomeMultiplier = focusOutcomeMultiplier(savedResult),
        )
      val delta = newEarned - record.earnedMinutes
      val now = now()
      val updated =
        record.copy(
          result = savedResult,
          activeDurationMinutes = safeMinutes,
          earnedMinutes = newEarned,
        )
      val adjustment =
        LedgerEntry(
          id = "edit-$id-${now.toEpochMilli()}",
          title = "Edited focus",
          deltaMinutes = delta,
          createdAt = now,
          note = "Original ${record.earnedMinutes.roundMinutes()} -> ${newEarned.roundMinutes()}",
          sourceId = id,
        )
      state.copy(
        reserveMinutes = (state.reserveMinutes + delta).coerceAtLeast(0.0),
        focusRecords = state.focusRecords.map { if (it.id == id) updated else it },
        ledger = if (delta == 0.0) state.ledger else listOf(adjustment) + state.ledger,
      )
    }
  }

  fun deleteLeisureRecord(id: String) {
    mutate { state ->
      val record = state.leisureRecords.firstOrNull { it.id == id && it.deletedAt == null } ?: return@mutate state
      val now = now()
      val adjustment =
        LedgerEntry(
          id = "delete-$id-${now.toEpochMilli()}",
          title = "Deleted leisure",
          deltaMinutes = record.costMinutes,
          createdAt = now,
          sourceId = id,
        )
      state.copy(
        reserveMinutes = state.reserveMinutes + record.costMinutes,
        leisureRecords = state.leisureRecords.map { if (it.id == id) it.copy(deletedAt = now) else it },
        ledger = listOf(adjustment) + state.ledger,
      )
    }
  }

  private fun ensureDailyGrants() {
    val rules = _state.value.rules
    val today = TimeAccounting.dailyDate(now(), rules = rules)
    val start = runCatching { LocalDate.parse(_state.value.dailyDate) }.getOrDefault(today)
    val grantStart = if (start.isAfter(today)) today else start
    mutate { state ->
      val existingIds = state.ledger.mapTo(mutableSetOf()) { it.id }
      val grants =
        generateSequence(grantStart) { date -> date.plusDays(1).takeIf { !it.isAfter(today) } }
          .filter { date -> dailyGrantId(date) !in existingIds && pausedDailyGrantId(date) !in existingIds }
          .map { date ->
            if (isDailyGrantPaused(state, date)) {
              LedgerEntry(
                id = pausedDailyGrantId(date),
                title = "Daily grant paused",
                deltaMinutes = 0.0,
                createdAt = dailyGrantInstant(date, state.rules),
              )
            } else {
              LedgerEntry(
                id = dailyGrantId(date),
                title = "Daily grant",
                deltaMinutes = state.rules.safeDailyGrantMinutes,
                createdAt = dailyGrantInstant(date, state.rules),
              )
            }
          }
          .toList()
      if (grants.isEmpty()) return@mutate state
      state.copy(
        reserveMinutes = state.reserveMinutes + grants.sumOf { it.deltaMinutes },
        ledger = grants.asReversed() + state.ledger,
      )
    }
  }

  private fun settleDailyTrackers() {
    val currentDate = runCatching { LocalDate.parse(_state.value.dailyDate) }.getOrNull() ?: return
    val rules = _state.value.rules
    val today = TimeAccounting.dailyDate(now(), rules = rules)
    if (!currentDate.isBefore(today)) return
    mutate { state ->
      val existingIds = state.ledger.mapTo(mutableSetOf()) { it.id }
      val rewards =
        state.trackers
          .filter { it.archivedAt == null && it.completed && it.rewardMinutes > 0.0 }
          .filter { trackerRewardId(currentDate, it.id) !in existingIds }
          .map {
            LedgerEntry(
              id = trackerRewardId(currentDate, it.id),
              title = "Daily tracker",
              deltaMinutes = it.rewardMinutes,
              createdAt = dailyGrantInstant(currentDate.plusDays(1), state.rules),
              note = it.label,
              sourceId = it.id,
            )
          }
      if (rewards.isEmpty()) return@mutate state
      state.copy(
        reserveMinutes = state.reserveMinutes + rewards.sumOf { it.deltaMinutes },
        ledger = rewards.asReversed() + state.ledger,
      )
    }
  }

  private fun rollDailyState() {
    mutate { state ->
      val today = TimeAccounting.dailyDate(now(), rules = state.rules).toString()
      if (state.dailyDate == today) return@mutate state
      state.copy(
        dailyDate = today,
        trackers =
          state.trackers.map {
            if (it.ruleTagName == null) {
              it.copy(completed = false, wakeTime = null)
            } else {
              it.copy(completed = false, progressLabel = "0m / ${it.ruleTargetMinutes?.roundTarget() ?: ""}")
            }
          },
      )
    }
  }

  private fun mutate(transform: (FocusWellUiState) -> FocusWellUiState) {
    _state.update { current ->
      transform(current)
        .withComputedTrackers()
        .withLedgerBackedReserve()
        .also { store.persistChange(previous = current, next = it) }
    }
  }

  private fun loadState(): FocusWellUiState {
    val stored = store.loadState()?.withComputedTrackers()?.withLedgerBackedReserve()
    if (stored != null) return stored
    return seedState().also { store.persistChange(previous = null, next = it) }
  }

  private fun seedState(): FocusWellUiState =
    FocusWellUiState(
      reserveMinutes = 0.0,
      dailyDate = TimeAccounting.dailyDate(now()).toString(),
      tags = defaultTags,
      trackers = defaultTrackers,
      focusRecords = emptyList(),
      leisureRecords = emptyList(),
      ideas = emptyList(),
      ledger = emptyList(),
      lastCheckInDailyDate = null,
      dailyGrantPausedUntilDate = null,
    )

  private fun dailyGrantId(date: LocalDate): String = "daily-grant-$date"

  private fun pausedDailyGrantId(date: LocalDate): String = "daily-grant-paused-$date"

  private fun trackerRewardId(date: LocalDate, trackerId: String): String = "tracker-reward-$date-$trackerId"

  private fun dailyGrantInstant(date: LocalDate, rules: FocusWellRules = FocusWellRules()): Instant =
    date.atTime(rules.normalized().dayBoundaryTime).atZone(TimeAccounting.focusWellZone).toInstant()

  private fun isDailyGrantPaused(state: FocusWellUiState, date: LocalDate): Boolean {
    val pausedUntil = state.dailyGrantPausedUntilDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return false
    return !date.isAfter(pausedUntil)
  }

  private fun wakeBonusEntry(
    state: FocusWellUiState,
    today: LocalDate,
    checkInStartedAt: Instant,
    existingIds: Set<String>,
  ): LedgerEntry? {
    val id = "wake-bonus-$today"
    if (id in existingIds) return null
    val target = wakeTargetTime(state) ?: return null
    val localTime = checkInStartedAt.atZone(TimeAccounting.focusWellZone).toLocalTime()
    val deltaMinutes = Duration.between(target, localTime).toMinutes()
    if (deltaMinutes < -30 || deltaMinutes > 30) return null
    return LedgerEntry(
      id = id,
      title = "Wake bonus",
      deltaMinutes = 30.0,
      createdAt = checkInStartedAt,
      note = "Checked in near ${target.toString().take(5)}",
    )
  }

  private fun wakeTargetTime(state: FocusWellUiState): LocalTime? {
    val wakeTracker = state.trackers.firstOrNull { it.id == "wake" && it.archivedAt == null } ?: return null
    wakeTracker.wakeTime?.let { stored ->
      runCatching { LocalTime.parse(stored) }.getOrNull()?.let { return it }
    }
    val hour = Regex("""Wake\s+by\s+(\d{1,2})""", RegexOption.IGNORE_CASE).find(wakeTracker.label)?.groupValues?.getOrNull(1)?.toIntOrNull()
    return LocalTime.of((hour ?: 9).coerceIn(0, 23), 0)
  }

  private fun stateToJson(state: FocusWellUiState): JSONObject =
    JSONObject()
      .put("reserveMinutes", state.reserveMinutes)
      .put("dailyDate", state.dailyDate)
      .put("rules", rulesToJson(state.rules))
      .put("activeMode", activeModeToJson(state.activeMode))
      .put("tags", JSONArray(state.tags.map(::tagToJson)))
      .put("trackers", JSONArray(state.trackers.map(::trackerToJson)))
      .put("focusRecords", JSONArray(state.focusRecords.map(::focusRecordToJson)))
      .put("leisureRecords", JSONArray(state.leisureRecords.map(::leisureRecordToJson)))
      .put("ideas", JSONArray(state.ideas.map(::ideaToJson)))
      .put("ledger", JSONArray(state.ledger.map(::ledgerToJson)))
      .put("lastCheckInDailyDate", state.lastCheckInDailyDate)
      .put("dailyGrantPausedUntilDate", state.dailyGrantPausedUntilDate)

  private fun jsonToState(json: JSONObject): FocusWellUiState =
    FocusWellUiState(
      reserveMinutes = json.optDouble("reserveMinutes", 0.0),
      dailyDate = json.optString("dailyDate", TimeAccounting.dailyDate(now()).toString()),
      rules = json.optJSONObject("rules")?.let(::jsonToRules)?.normalized() ?: FocusWellRules(),
      activeMode = jsonToActiveMode(json.optJSONObject("activeMode")),
      tags = json.optJSONArray("tags")?.mapObjects(::jsonToTag).orEmpty().ifEmpty { defaultTags },
      trackers =
        json.optJSONArray("trackers")?.mapObjects(::jsonToTracker).orEmpty().ifEmpty {
          defaultTrackers
        },
      focusRecords = json.optJSONArray("focusRecords")?.mapObjects(::jsonToFocusRecord).orEmpty(),
      leisureRecords = json.optJSONArray("leisureRecords")?.mapObjects(::jsonToLeisureRecord).orEmpty(),
      ideas = json.optJSONArray("ideas")?.mapObjects(::jsonToIdea).orEmpty(),
      ledger = json.optJSONArray("ledger")?.mapObjects(::jsonToLedger).orEmpty(),
      lastCheckInDailyDate = json.optStringOrNull("lastCheckInDailyDate"),
      dailyGrantPausedUntilDate = json.optStringOrNull("dailyGrantPausedUntilDate"),
    ).withComputedTrackers().withLedgerBackedReserve()

  private fun rulesToJson(rules: FocusWellRules): JSONObject {
    val normalizedRules = rules.normalized()
    return JSONObject()
      .put("dailyGrantMinutes", normalizedRules.dailyGrantMinutes)
      .put("dayBoundaryHour", normalizedRules.dayBoundaryHour)
      .put("sleepProtectionStartHour", normalizedRules.sleepProtectionStartHour)
      .put("sleepProtectionMultiplier", normalizedRules.sleepProtectionMultiplier)
      .put("longSessionRemindersEnabled", normalizedRules.longSessionRemindersEnabled)
  }

  private fun jsonToRules(json: JSONObject): FocusWellRules =
    FocusWellRules(
      dailyGrantMinutes = json.optDouble("dailyGrantMinutes", 60.0),
      dayBoundaryHour = json.optInt("dayBoundaryHour", 4),
      sleepProtectionStartHour = json.optInt("sleepProtectionStartHour", 1),
      sleepProtectionMultiplier = json.optDouble("sleepProtectionMultiplier", 2.0),
      longSessionRemindersEnabled = json.optBoolean("longSessionRemindersEnabled", true),
    )

  private fun activeModeToJson(mode: ActiveMode): JSONObject =
    when (mode) {
      ActiveMode.None -> JSONObject().put("kind", "none")
      is ActiveMode.Focus ->
        JSONObject()
          .put("kind", "focus")
          .put("task", mode.task)
          .put("type", mode.type.name)
          .put("tag", mode.tag?.let(::tagToJson))
          .put("startedAt", mode.startedAt.toString())
          .put("reminderSessionId", mode.reminderSessionId)
          .put("revision", mode.revision)
          .put("paused", mode.paused)
          .put("pausedAt", mode.pausedAt?.toString())
          .put("pausedDurationMillis", mode.pausedDurationMillis)

      is ActiveMode.Leisure ->
        JSONObject()
          .put("kind", "leisure")
          .put("startedAt", mode.startedAt.toString())
          .put("reminderSessionId", mode.reminderSessionId)
          .put("revision", mode.revision)

      ActiveMode.Depleted -> JSONObject().put("kind", "depleted")
    }

  private fun jsonToActiveMode(json: JSONObject?): ActiveMode {
    if (json == null) return ActiveMode.None
    return when (json.optString("kind")) {
      "focus" ->
        ActiveMode.Focus(
          task = json.optString("task"),
          type = runCatching { SessionType.valueOf(json.optString("type")) }.getOrDefault(SessionType.Input),
          tag = json.optJSONObject("tag")?.let(::jsonToTag),
          startedAt = json.optInstant("startedAt"),
          reminderSessionId = json.optStringOrNull("reminderSessionId") ?: "focus-${json.optInstant("startedAt").toEpochMilli()}",
          revision = json.optInt("revision", 1),
          paused = json.optBoolean("paused"),
          pausedAt = json.optNullableInstant("pausedAt"),
          pausedDurationMillis = json.optLong("pausedDurationMillis", 0L),
        )

      "leisure" -> {
        val startedAt = json.optInstant("startedAt")
        ActiveMode.Leisure(
          startedAt = startedAt,
          reminderSessionId = json.optStringOrNull("reminderSessionId") ?: "leisure-${startedAt.toEpochMilli()}",
          revision = json.optInt("revision", 1),
        )
      }
      "windDown" -> ActiveMode.None
      "depleted" -> ActiveMode.Depleted
      else -> ActiveMode.None
    }
  }

  private fun tagToJson(tag: TagConfig): JSONObject =
    JSONObject()
      .put("id", tag.id)
      .put("name", tag.name)
      .put("multiplier", tag.multiplier)
      .put("archivedAt", tag.archivedAt?.toString())

  private fun jsonToTag(json: JSONObject): TagConfig =
    TagConfig(
      id = json.optString("id"),
      name = json.optString("name"),
      multiplier = json.optDouble("multiplier", 1.0),
      archivedAt = json.optNullableInstant("archivedAt"),
    )

  private fun trackerToJson(tracker: DailyTracker): JSONObject =
    JSONObject()
      .put("id", tracker.id)
      .put("label", tracker.label)
      .put("completed", tracker.completed)
      .put("rewardMinutes", tracker.rewardMinutes)
      .put("progressLabel", tracker.progressLabel)
      .put("ruleTagName", tracker.ruleTagName)
      .put("ruleTargetMinutes", tracker.ruleTargetMinutes)
      .put("wakeTime", tracker.wakeTime)
      .put("archivedAt", tracker.archivedAt?.toString())

  private fun jsonToTracker(json: JSONObject): DailyTracker =
    DailyTracker(
      id = json.optString("id"),
      label = json.optString("label"),
      completed = json.optBoolean("completed"),
      rewardMinutes = json.optDouble("rewardMinutes", if (json.optStringOrNull("ruleTagName") == null) 15.0 else 60.0),
      progressLabel = json.optStringOrNull("progressLabel"),
      ruleTagName = json.optStringOrNull("ruleTagName"),
      ruleTargetMinutes = if (json.has("ruleTargetMinutes") && !json.isNull("ruleTargetMinutes")) json.optDouble("ruleTargetMinutes") else null,
      wakeTime = json.optStringOrNull("wakeTime"),
      archivedAt = json.optNullableInstant("archivedAt"),
    )

  private fun ledgerToJson(entry: LedgerEntry): JSONObject =
    JSONObject()
      .put("id", entry.id)
      .put("title", entry.title)
      .put("deltaMinutes", entry.deltaMinutes)
      .put("createdAt", entry.createdAt.toString())
      .put("note", entry.note)
      .put("sourceId", entry.sourceId)

  private fun jsonToLedger(json: JSONObject): LedgerEntry =
    LedgerEntry(
      id = json.optString("id"),
      title = json.optString("title"),
      deltaMinutes = json.optDouble("deltaMinutes"),
      createdAt = json.optInstant("createdAt"),
      note = json.optStringOrNull("note"),
      sourceId = json.optStringOrNull("sourceId"),
    )

  private fun focusRecordToJson(record: FocusRecord): JSONObject =
    JSONObject()
      .put("id", record.id)
      .put("task", record.task)
      .put("result", record.result)
      .put("type", record.type.name)
      .put("tagName", record.tagName)
      .put("tagMultiplier", record.tagMultiplier)
      .put("typeRate", record.typeRate)
      .put("startedAt", record.startedAt.toString())
      .put("endedAt", record.endedAt.toString())
      .put("activeDurationMinutes", record.activeDurationMinutes)
      .put("earnedMinutes", record.earnedMinutes)
      .put("dailyDate", record.dailyDate)
      .put("deletedAt", record.deletedAt?.toString())

  private fun jsonToFocusRecord(json: JSONObject): FocusRecord =
    FocusRecord(
      id = json.optString("id"),
      task = json.optString("task"),
      result = json.optString("result"),
      type = runCatching { SessionType.valueOf(json.optString("type")) }.getOrDefault(SessionType.Input),
      tagName = json.optStringOrNull("tagName"),
      tagMultiplier = json.optDouble("tagMultiplier", 1.0),
      typeRate = json.optDouble("typeRate", 0.5),
      startedAt = json.optInstant("startedAt"),
      endedAt = json.optInstant("endedAt"),
      activeDurationMinutes = json.optDouble("activeDurationMinutes"),
      earnedMinutes = json.optDouble("earnedMinutes"),
      dailyDate = json.optString("dailyDate", TimeAccounting.dailyDate(json.optInstant("endedAt")).toString()),
      deletedAt = json.optNullableInstant("deletedAt"),
    )

  private fun leisureRecordToJson(record: LeisureRecord): JSONObject =
    JSONObject()
      .put("id", record.id)
      .put("startedAt", record.startedAt.toString())
      .put("endedAt", record.endedAt.toString())
      .put("elapsedMinutes", record.elapsedMinutes)
      .put("costMinutes", record.costMinutes)
      .put("dailyDate", record.dailyDate)
      .put("deletedAt", record.deletedAt?.toString())

  private fun ideaToJson(idea: Idea): JSONObject =
    JSONObject()
      .put("id", idea.id)
      .put("text", idea.text)
      .put("quadrant", idea.quadrant.name)
      .put("checklist", JSONArray(idea.checklist.map(::ideaChecklistItemToJson)))
      .put("createdAt", idea.createdAt.toString())
      .put("updatedAt", idea.updatedAt.toString())
      .put("archivedAt", idea.archivedAt?.toString())

  private fun ideaChecklistItemToJson(item: IdeaChecklistItem): JSONObject =
    JSONObject()
      .put("id", item.id)
      .put("text", item.text)
      .put("checked", item.checked)

  private fun jsonToIdea(json: JSONObject): Idea =
    Idea(
      id = json.optString("id"),
      text = json.optString("text"),
      quadrant = runCatching { IdeaQuadrant.valueOf(json.optString("quadrant")) }.getOrDefault(IdeaQuadrant.Inbox),
      checklist = json.optJSONArray("checklist")?.mapObjects(::jsonToIdeaChecklistItem).orEmpty(),
      createdAt = json.optInstant("createdAt"),
      updatedAt = json.optNullableInstant("updatedAt") ?: json.optInstant("createdAt"),
      archivedAt = json.optNullableInstant("archivedAt"),
    )

  private fun jsonToIdeaChecklistItem(json: JSONObject): IdeaChecklistItem =
    IdeaChecklistItem(
      id = json.optString("id"),
      text = json.optString("text"),
      checked = json.optBoolean("checked"),
    )

  private fun jsonToLeisureRecord(json: JSONObject): LeisureRecord =
    LeisureRecord(
      id = json.optString("id"),
      startedAt = json.optInstant("startedAt"),
      endedAt = json.optInstant("endedAt"),
      elapsedMinutes = json.optDouble("elapsedMinutes"),
      costMinutes = json.optDouble("costMinutes"),
      dailyDate = json.optString("dailyDate", TimeAccounting.dailyDate(json.optInstant("endedAt")).toString()),
      deletedAt = json.optNullableInstant("deletedAt"),
    )

  private fun JSONObject.optInstant(key: String): Instant =
    Instant.parse(getString(key))

  private fun JSONObject.optNullableInstant(key: String): Instant? {
    val value = optStringOrNull(key) ?: return null
    return runCatching { Instant.parse(value) }.getOrNull()
  }

  private fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) optString(key) else null

  private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    List(length()) { index -> transform(getJSONObject(index)) }

  private fun FocusWellUiState.withComputedTrackers(): FocusWellUiState {
    val activeFocusRecords = focusRecords.filter { it.deletedAt == null && it.dailyDate == dailyDate }
    val computed =
      trackers.map { tracker ->
        val tagName = tracker.ruleTagName
        val target = tracker.ruleTargetMinutes
        if (tracker.archivedAt != null || tagName == null || target == null) {
          tracker
        } else {
          val minutes =
            activeFocusRecords
              .filter { it.tagName?.equals(tagName, ignoreCase = true) == true }
              .sumOf { it.activeDurationMinutes }
          tracker.copy(
            completed = minutes >= target,
            progressLabel = "${minutes.roundMinutes()} / ${target.roundTarget()}",
          )
        }
      }
    return copy(trackers = computed)
  }

  private fun FocusWellUiState.withLedgerBackedReserve(): FocusWellUiState =
    copy(reserveMinutes = ledger.sumOf { it.deltaMinutes }.coerceAtLeast(0.0))

  private fun Double.roundMinutes(): String {
    val rounded = toInt()
    return if (rounded >= 60) "${rounded / 60}h ${rounded % 60}m" else "${rounded}m"
  }

  private fun Double.roundTarget(): String {
    val rounded = toInt()
    return if (rounded % 60 == 0) "${rounded / 60}h" else "${rounded}m"
  }
}
