package dev.nihildigit.focuswell.data

import android.content.Context
import dev.nihildigit.focuswell.data.db.RoomFocusWellStore
import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.IdeaChecklistItem
import dev.nihildigit.focuswell.domain.IdeaQuadrant
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.domain.defaultTags
import dev.nihildigit.focuswell.domain.defaultTrackers
import dev.nihildigit.focuswell.domain.reserveLocked
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate

class FocusWellRepository internal constructor(
  private val store: FocusWellStore,
  private val now: () -> Instant = Instant::now,
) {
  constructor(context: Context) : this(RoomFocusWellStore(context))

  private val _state = MutableStateFlow(FocusWellUiState())
  val state: StateFlow<FocusWellUiState> = _state
  private val jsonCodec = FocusWellStateJsonCodec(now)
  private val dailyMaintenance = FocusWellDailyMaintenance(now)
  private val mutationMutex = Mutex()
  private var initialized = false

  suspend fun initialize() {
    val shouldRunMaintenance =
      mutationMutex.withLock {
        if (initialized) {
          false
        } else {
          _state.value = loadState()
          initialized = true
          true
        }
      }
    if (!shouldRunMaintenance) {
      return
    }
    settleDailyTrackers()
    settleDailyInterest()
    ensureDailyGrants()
    rollDailyState()
  }

  suspend fun toggleTracker(id: String) {
    mutate { state -> state.withToggledManualTracker(id) }
  }

  suspend fun addTag(name: String, multiplier: Double) {
    val createdAt = now()
    mutate { state -> state.withAddedTag(name = name, multiplier = multiplier, createdAt = createdAt) }
  }

  suspend fun archiveTag(id: String) {
    val archivedAt = now()
    mutate { state -> state.withArchivedTag(id = id, archivedAt = archivedAt) }
  }

  suspend fun updateTag(id: String, name: String, multiplier: Double) {
    mutate { state -> state.withUpdatedTag(id = id, name = name, multiplier = multiplier) }
  }

  suspend fun addBooleanTracker(label: String, rewardMinutes: Double) {
    val createdAt = now()
    mutate { state -> state.withAddedManualTracker(label = label, rewardMinutes = rewardMinutes, createdAt = createdAt) }
  }

  suspend fun addRuleTracker(label: String, tagName: String, targetMinutes: Double, rewardMinutes: Double) {
    val createdAt = now()
    mutate { state ->
      state.withAddedRuleTracker(
        label = label,
        tagName = tagName,
        targetMinutes = targetMinutes,
        rewardMinutes = rewardMinutes,
        createdAt = createdAt,
      )
    }
  }

  suspend fun archiveTracker(id: String) {
    val archivedAt = now()
    mutate { state -> state.withArchivedTracker(id = id, archivedAt = archivedAt) }
  }

  suspend fun updateManualTracker(id: String, label: String, rewardMinutes: Double) {
    mutate { state -> state.withUpdatedManualTracker(id = id, label = label, rewardMinutes = rewardMinutes) }
  }

  suspend fun updateRuleTracker(id: String, label: String, tagName: String, targetMinutes: Double, rewardMinutes: Double) {
    mutate { state ->
      state.withUpdatedRuleTracker(
        id = id,
        label = label,
        tagName = tagName,
        targetMinutes = targetMinutes,
        rewardMinutes = rewardMinutes,
      )
    }
  }

  suspend fun updateRules(rules: FocusWellRules) {
    mutate { state -> state.copy(rules = rules.normalized()) }
  }

  suspend fun completeMorningCheckIn(
    checkInStartedAt: Instant,
    phoneCostMinutes: Double,
    reviewedSegmentCount: Int,
    settledUntil: Instant,
  ) {
    mutate { state ->
      val today = TimeAccounting.dailyDate(checkInStartedAt, rules = state.rules)
      val todayText = today.toString()
      if (state.lastCheckInDailyDate == todayText) return@mutate state

      val existingIds = state.ledger.mapTo(mutableSetOf()) { it.id }
      val entries = mutableListOf<LedgerEntry>()
      val wakeBonus = wakeBonusEntry(state, today, checkInStartedAt, existingIds)
      if (wakeBonus != null) entries += wakeBonus

      val phoneCharge =
        if (state.reserveLocked) {
          PhoneUsageLedgerCharge(entry = null, exceededReserve = false)
        } else {
          morningCheckInPhoneUsageCharge(
            dailyDate = todayText,
            checkInStartedAt = checkInStartedAt,
            phoneCostMinutes = phoneCostMinutes,
            availableMinutes = state.ledger.sumOf { it.deltaMinutes } + entries.sumOf { it.deltaMinutes },
            reviewedSegmentCount = reviewedSegmentCount,
          )
        }
      phoneCharge.entry?.let { entries += it }

      state.copy(
        lastCheckInDailyDate = todayText,
        lastPhoneUsageSettlementAt = maxSettlementInstant(state.lastPhoneUsageSettlementAt, settledUntil),
        dailyGrantPausedUntilDate =
          if (phoneCharge.exceededReserve) todayText else state.dailyGrantPausedUntilDate,
        ledger = entries.asReversed() + state.ledger,
      )
    }
  }

  suspend fun completePhoneUsageSettlement(
    settlementStartedAt: Instant,
    phoneCostMinutes: Double,
    reviewedSegmentCount: Int,
    settledUntil: Instant,
  ) {
    mutate { state ->
      val today = TimeAccounting.dailyDate(settlementStartedAt, rules = state.rules)
      val phoneCharge =
        if (state.reserveLocked) {
          PhoneUsageLedgerCharge(entry = null, exceededReserve = false)
        } else {
          settlementPhoneUsageCharge(
            settlementStartedAt = settlementStartedAt,
            phoneCostMinutes = phoneCostMinutes,
            availableMinutes = state.ledger.sumOf { it.deltaMinutes },
            reviewedSegmentCount = reviewedSegmentCount,
          )
        }
      val entries = phoneCharge.entry?.let(::listOf).orEmpty()

      state.copy(
        lastPhoneUsageSettlementAt = maxSettlementInstant(state.lastPhoneUsageSettlementAt, settledUntil),
        dailyGrantPausedUntilDate =
          if (phoneCharge.exceededReserve) today.toString() else state.dailyGrantPausedUntilDate,
        ledger = entries + state.ledger,
      )
    }
  }

  suspend fun startFocus(task: String, type: SessionType, tagId: String?): ActiveMode.Focus? {
    val startedAt = now()
    val sessionId = FocusWellIds.focus(startedAt)
    var activeFocus: ActiveMode.Focus? = null
    mutate { state ->
      val started = state.withStartedFocusSession(task = task, type = type, tagId = tagId, startedAt = startedAt, reminderSessionId = sessionId)
      activeFocus = started.focus
      started.state
    }
    return activeFocus
  }

  suspend fun pauseFocus() {
    val pausedAt = now()
    mutate { state -> state.withPausedFocusSession(pausedAt) }
  }

  suspend fun resumeFocus() {
    val resumedAt = now()
    mutate { state -> state.withResumedFocusSession(resumedAt) }
  }

  suspend fun endFocus(result: String, correctionMinutes: Double = 0.0): String? {
    var reminderSessionId: String? = null
    val endedAt = now()
    mutate { state ->
      val ended = state.withEndedFocusSession(endedAt = endedAt, result = result, correctionMinutes = correctionMinutes)
      reminderSessionId = ended?.reminderSessionId
      ended?.state ?: state
    }
    return reminderSessionId
  }

  suspend fun startLeisure(): ActiveMode.Leisure? {
    val startedAt = now()
    val sessionId = FocusWellIds.leisure(startedAt)
    var activeLeisure: ActiveMode.Leisure? = null
    mutate { state ->
      val started = state.withStartedLeisureSession(startedAt = startedAt, reminderSessionId = sessionId)
      activeLeisure = started?.leisure
      started?.state ?: state
    }
    return activeLeisure
  }

  suspend fun endLeisure(): String? {
    var reminderSessionId: String? = null
    val endedAt = now()
    mutate { state ->
      val ended = state.withEndedLeisureSession(endedAt)
      reminderSessionId = ended?.reminderSessionId
      ended?.state ?: state
    }
    return reminderSessionId
  }

  suspend fun endDepleted() {
    mutate { it.copy(activeMode = ActiveMode.None) }
  }

  suspend fun clearAllData() {
    val seeded = seedState().copy(stateUpdatedAt = now())
    mutationMutex.withLock {
      store.clear()
      store.persistChange(previous = null, next = seeded)
      _state.value = seeded
      initialized = true
    }
    settleDailyInterest()
    ensureDailyGrants()
    rollDailyState()
  }

  suspend fun addIdea(text: String) {
    val createdAt = now()
    mutate { state -> state.withAddedIdea(text = text, createdAt = createdAt) }
  }

  suspend fun moveIdea(id: String, quadrant: IdeaQuadrant) {
    val updatedAt = now()
    mutate { state -> state.withMovedIdea(id = id, quadrant = quadrant, updatedAt = updatedAt) }
  }

  suspend fun updateIdea(id: String, text: String, checklist: List<IdeaChecklistItem>) {
    val updatedAt = now()
    mutate { state -> state.withUpdatedIdea(id = id, text = text, checklist = checklist, updatedAt = updatedAt) }
  }

  suspend fun archiveIdea(id: String) {
    val archivedAt = now()
    mutate { state -> state.withArchivedIdea(id = id, archivedAt = archivedAt) }
  }

  fun exportJson(): String = jsonCodec.encode(_state.value)

  suspend fun importJson(raw: String, touchUpdatedAt: Boolean = true): Boolean {
    val imported = runCatching { jsonCodec.decode(raw) }.getOrNull() ?: return false
    val normalized =
      imported
        .let { if (touchUpdatedAt) it.copy(stateUpdatedAt = now()) else it }
        .withLedgerBackedReserve()
    mutationMutex.withLock {
      store.clear()
      store.persistChange(previous = null, next = normalized)
      _state.value = normalized
      initialized = true
    }
    settleDailyInterest()
    ensureDailyGrants()
    rollDailyState()
    return true
  }

  suspend fun deleteFocusRecord(id: String) {
    val deletedAt = now()
    mutate { state -> state.withDeletedFocusRecord(id = id, deletedAt = deletedAt) }
  }

  suspend fun updateFocusRecord(id: String, result: String, activeMinutes: Double) {
    val updatedAt = now()
    mutate { state -> state.withUpdatedFocusRecord(id = id, result = result, activeMinutes = activeMinutes, updatedAt = updatedAt) }
  }

  suspend fun addManualAdjustment(title: String, deltaMinutes: Double, note: String?) {
    val createdAt = now()
    mutate { state ->
      state.withAddedManualAdjustment(
        title = title,
        deltaMinutes = deltaMinutes,
        note = note,
        createdAt = createdAt,
      )
    }
  }

  suspend fun addManualFocusRecord(task: String, activeMinutes: Double, note: String?, type: SessionType, tagId: String?) {
    val createdAt = now()
    mutate { state ->
      val tag = tagId?.let { selectedId -> state.tags.firstOrNull { it.id == selectedId } }
      state.withAddedManualFocusRecord(
        task = task,
        activeMinutes = activeMinutes,
        note = note,
        type = type,
        tag = tag,
        createdAt = createdAt,
      )
    }
  }

  suspend fun deleteLeisureRecord(id: String) {
    val deletedAt = now()
    mutate { state -> state.withDeletedLeisureRecord(id = id, deletedAt = deletedAt) }
  }

  private suspend fun ensureDailyGrants() {
    mutate(dailyMaintenance::ensureDailyGrants)
  }

  private suspend fun settleDailyTrackers() {
    mutate(dailyMaintenance::settleDailyTrackers)
  }

  private suspend fun settleDailyInterest() {
    mutate(dailyMaintenance::settleDailyInterest)
  }

  private suspend fun rollDailyState() {
    mutate(dailyMaintenance::rollDailyState)
  }

  private suspend fun mutate(transform: (FocusWellUiState) -> FocusWellUiState) {
    if (!initialized) {
      initialize()
    }
    mutationMutex.withLock {
      val current = _state.value
      val transformed = transform(current)
      if (transformed == current) {
        return
      } else {
        val next =
          transformed
          .copy(stateUpdatedAt = now())
          .withComputedTrackers()
          .withLedgerBackedReserve()
        store.persistChange(previous = current, next = next)
        _state.value = next
      }
    }
  }

  private suspend fun loadState(): FocusWellUiState {
    val stored = store.loadState()?.withComputedTrackers()?.withLedgerBackedReserve()
    if (stored != null) return stored
    return seedState().also { store.persistChange(previous = null, next = it) }
  }

  private fun seedState(): FocusWellUiState =
    FocusWellUiState(
      reserveMinutes = 0.0,
      dailyDate = TimeAccounting.dailyDate(now(), rules = FocusWellRules()).toString(),
      stateUpdatedAt = now(),
      tags = defaultTags,
      trackers = defaultTrackers,
      focusRecords = emptyList(),
      leisureRecords = emptyList(),
      ideas = emptyList(),
      ledger = emptyList(),
      lastCheckInDailyDate = null,
      lastPhoneUsageSettlementAt = null,
      dailyGrantPausedUntilDate = null,
    )

  private fun maxSettlementInstant(current: Instant?, next: Instant): Instant =
    if (current == null || next.isAfter(current)) next else current

  private fun wakeBonusEntry(
    state: FocusWellUiState,
    today: LocalDate,
    checkInStartedAt: Instant,
    existingIds: Set<String>,
  ): LedgerEntry? {
    val id = FocusWellIds.wakeBonus(today)
    if (id in existingIds) return null
    val target = state.rules.normalized().wakeTargetTime
    if (!TimeAccounting.isWakeBonusEligible(checkInStartedAt, rules = state.rules)) return null
    return LedgerEntry(
      id = id,
      title = "Wake bonus",
      deltaMinutes = 30.0,
      createdAt = checkInStartedAt,
      note = "Checked in near ${target.toString().take(5)}",
    )
  }
}
