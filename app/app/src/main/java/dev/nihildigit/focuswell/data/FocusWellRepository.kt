package dev.nihildigit.focuswell.data

import android.content.Context
import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.LeisureRecord
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.TagConfig
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.domain.defaultTags
import dev.nihildigit.focuswell.domain.defaultTrackers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class FocusWellRepository(context: Context) {
  private val prefs = context.getSharedPreferences("focuswell-store", Context.MODE_PRIVATE)
  private val _state = MutableStateFlow(loadState())
  val state: StateFlow<FocusWellUiState> = _state

  init {
    ensureDailyGrant()
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
      state.copy(tags = state.tags.map { if (it.id == id) it.copy(archivedAt = Instant.now()) else it })
    }
  }

  fun addBooleanTracker(label: String) {
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
            )
      )
    }
  }

  fun addRuleTracker(label: String, tagName: String, targetMinutes: Double) {
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
              ruleTagName = trimmedTag,
              ruleTargetMinutes = targetMinutes,
            )
      )
    }
  }

  fun archiveTracker(id: String) {
    mutate { state ->
      state.copy(
        trackers = state.trackers.map { if (it.id == id) it.copy(archivedAt = Instant.now()) else it }
      )
    }
  }

  fun startFocus(task: String, type: SessionType, tagId: String) {
    val trimmed = task.trim()
    if (trimmed.isEmpty()) return
    mutate { state ->
      val tag = state.tags.firstOrNull { it.id == tagId } ?: state.tags.first()
      state.copy(
        activeMode =
          ActiveMode.Focus(
            task = trimmed,
            type = type,
            tag = tag,
            startedAt = Instant.now(),
          )
      )
    }
  }

  fun pauseFocus() {
    mutate { state ->
      val focus = state.activeMode as? ActiveMode.Focus ?: return@mutate state
      if (focus.paused) state else state.copy(activeMode = focus.copy(paused = true, pausedAt = Instant.now()))
    }
  }

  fun resumeFocus() {
    mutate { state ->
      val focus = state.activeMode as? ActiveMode.Focus ?: return@mutate state
      val pausedAt = focus.pausedAt
      val extraPaused =
        if (pausedAt == null) 0L else Duration.between(pausedAt, Instant.now()).toMillis().coerceAtLeast(0)
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

  fun endFocus(result: String) {
    mutate { state ->
      val focus = state.activeMode as? ActiveMode.Focus ?: return@mutate state
      val now = Instant.now()
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
      val earned =
        TimeAccounting.focusEarnedMinutes(
          activeDuration = activeDuration,
          type = focus.type,
          tagMultiplier = focus.tag.multiplier,
        )
      val record =
        FocusRecord(
          id = "focus-${now.toEpochMilli()}",
          task = focus.task,
          result = result.ifBlank { "As planned" },
          type = focus.type,
          tagName = focus.tag.name,
          tagMultiplier = focus.tag.multiplier,
          typeRate = focus.type.rate,
          startedAt = focus.startedAt,
          endedAt = now,
          activeDurationMinutes = activeDuration.toMillis() / 60_000.0,
          earnedMinutes = earned,
        )
      val entry =
        LedgerEntry(
          id = "ledger-${record.id}",
          title = "Focus · ${focus.type.label} ${focus.tag.name}",
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
  }

  fun startLeisure() {
    mutate { state ->
      if (state.reserveMinutes <= 0.0) state
      else state.copy(activeMode = ActiveMode.Leisure(startedAt = Instant.now()))
    }
  }

  fun endLeisure() {
    mutate { state ->
      val leisure = state.activeMode as? ActiveMode.Leisure ?: return@mutate state
      val now = Instant.now()
      val cost =
        TimeAccounting.leisureCostMinutes(leisure.startedAt, now).coerceAtMost(state.reserveMinutes)
      val elapsed = Duration.between(leisure.startedAt, now).toMillis().coerceAtLeast(0) / 60_000.0
      val record =
        LeisureRecord(
          id = "leisure-${now.toEpochMilli()}",
          startedAt = leisure.startedAt,
          endedAt = now,
          elapsedMinutes = elapsed,
          costMinutes = cost,
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
  }

  fun startWindDown() {
    mutate { it.copy(activeMode = ActiveMode.WindDown(startedAt = Instant.now())) }
  }

  fun endWindDown() {
    mutate { it.copy(activeMode = ActiveMode.None) }
  }

  fun endDepleted() {
    mutate { it.copy(activeMode = ActiveMode.None) }
  }

  fun clearAllData() {
    prefs.edit().clear().apply()
    _state.value = seedState()
    ensureDailyGrant()
  }

  fun exportJson(): String = stateToJson(_state.value).toString(2)

  fun importJson(raw: String): Boolean {
    val imported = runCatching { jsonToState(JSONObject(raw)) }.getOrNull() ?: return false
    _state.value = imported
    saveState(imported)
    ensureDailyGrant()
    return true
  }

  fun deleteFocusRecord(id: String) {
    mutate { state ->
      val record = state.focusRecords.firstOrNull { it.id == id && it.deletedAt == null } ?: return@mutate state
      val now = Instant.now()
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
      val newEarned = safeMinutes * record.typeRate * record.tagMultiplier
      val delta = newEarned - record.earnedMinutes
      val now = Instant.now()
      val updated =
        record.copy(
          result = result.ifBlank { record.result },
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
      val now = Instant.now()
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

  private fun ensureDailyGrant() {
    val today = TimeAccounting.dailyDate(Instant.now())
    mutate { state ->
      if (state.ledger.any { it.id == dailyGrantId(today) }) return@mutate state
      val entry =
        LedgerEntry(
          id = dailyGrantId(today),
          title = "Daily grant",
          deltaMinutes = 60.0,
          createdAt = Instant.now(),
        )
      state.copy(
        reserveMinutes = state.reserveMinutes + 60.0,
        ledger = listOf(entry) + state.ledger,
      )
    }
  }

  private fun mutate(transform: (FocusWellUiState) -> FocusWellUiState) {
    _state.update { current ->
      transform(current).withComputedTrackers().also { saveState(it) }
    }
  }

  private fun loadState(): FocusWellUiState {
    val raw = prefs.getString(KEY_STATE, null) ?: return seedState()
    return runCatching { jsonToState(JSONObject(raw)) }.getOrElse { seedState() }
  }

  private fun saveState(state: FocusWellUiState) {
    prefs.edit().putString(KEY_STATE, stateToJson(state).toString()).apply()
  }

  private fun seedState(): FocusWellUiState =
    FocusWellUiState(
      reserveMinutes = 0.0,
      tags = defaultTags,
      trackers = defaultTrackers,
      focusRecords = emptyList(),
      leisureRecords = emptyList(),
      ledger = emptyList(),
    )

  private fun dailyGrantId(date: LocalDate): String = "daily-grant-$date"

  private fun stateToJson(state: FocusWellUiState): JSONObject =
    JSONObject()
      .put("reserveMinutes", state.reserveMinutes)
      .put("activeMode", activeModeToJson(state.activeMode))
      .put("tags", JSONArray(state.tags.map(::tagToJson)))
      .put("trackers", JSONArray(state.trackers.map(::trackerToJson)))
      .put("focusRecords", JSONArray(state.focusRecords.map(::focusRecordToJson)))
      .put("leisureRecords", JSONArray(state.leisureRecords.map(::leisureRecordToJson)))
      .put("ledger", JSONArray(state.ledger.map(::ledgerToJson)))

  private fun jsonToState(json: JSONObject): FocusWellUiState =
    FocusWellUiState(
      reserveMinutes = json.optDouble("reserveMinutes", 0.0),
      activeMode = jsonToActiveMode(json.optJSONObject("activeMode")),
      tags = json.optJSONArray("tags")?.mapObjects(::jsonToTag).orEmpty().ifEmpty { defaultTags },
      trackers =
        json.optJSONArray("trackers")?.mapObjects(::jsonToTracker).orEmpty().ifEmpty {
          defaultTrackers
        },
      focusRecords = json.optJSONArray("focusRecords")?.mapObjects(::jsonToFocusRecord).orEmpty(),
      leisureRecords = json.optJSONArray("leisureRecords")?.mapObjects(::jsonToLeisureRecord).orEmpty(),
      ledger = json.optJSONArray("ledger")?.mapObjects(::jsonToLedger).orEmpty(),
    ).withComputedTrackers()

  private fun activeModeToJson(mode: ActiveMode): JSONObject =
    when (mode) {
      ActiveMode.None -> JSONObject().put("kind", "none")
      is ActiveMode.Focus ->
        JSONObject()
          .put("kind", "focus")
          .put("task", mode.task)
          .put("type", mode.type.name)
          .put("tag", tagToJson(mode.tag))
          .put("startedAt", mode.startedAt.toString())
          .put("paused", mode.paused)
          .put("pausedAt", mode.pausedAt?.toString())
          .put("pausedDurationMillis", mode.pausedDurationMillis)

      is ActiveMode.Leisure ->
        JSONObject().put("kind", "leisure").put("startedAt", mode.startedAt.toString())

      is ActiveMode.WindDown ->
        JSONObject().put("kind", "windDown").put("startedAt", mode.startedAt.toString())

      ActiveMode.Depleted -> JSONObject().put("kind", "depleted")
    }

  private fun jsonToActiveMode(json: JSONObject?): ActiveMode {
    if (json == null) return ActiveMode.None
    return when (json.optString("kind")) {
      "focus" ->
        ActiveMode.Focus(
          task = json.optString("task"),
          type = runCatching { SessionType.valueOf(json.optString("type")) }.getOrDefault(SessionType.Input),
          tag = json.optJSONObject("tag")?.let(::jsonToTag) ?: defaultTags.first(),
          startedAt = json.optInstant("startedAt"),
          paused = json.optBoolean("paused"),
          pausedAt = json.optNullableInstant("pausedAt"),
          pausedDurationMillis = json.optLong("pausedDurationMillis", 0L),
        )

      "leisure" -> ActiveMode.Leisure(startedAt = json.optInstant("startedAt"))
      "windDown" -> ActiveMode.WindDown(startedAt = json.optInstant("startedAt"))
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
      .put("progressLabel", tracker.progressLabel)
      .put("ruleTagName", tracker.ruleTagName)
      .put("ruleTargetMinutes", tracker.ruleTargetMinutes)
      .put("archivedAt", tracker.archivedAt?.toString())

  private fun jsonToTracker(json: JSONObject): DailyTracker =
    DailyTracker(
      id = json.optString("id"),
      label = json.optString("label"),
      completed = json.optBoolean("completed"),
      progressLabel = json.optStringOrNull("progressLabel"),
      ruleTagName = json.optStringOrNull("ruleTagName"),
      ruleTargetMinutes = if (json.has("ruleTargetMinutes") && !json.isNull("ruleTargetMinutes")) json.optDouble("ruleTargetMinutes") else null,
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
      .put("deletedAt", record.deletedAt?.toString())

  private fun jsonToFocusRecord(json: JSONObject): FocusRecord =
    FocusRecord(
      id = json.optString("id"),
      task = json.optString("task"),
      result = json.optString("result"),
      type = runCatching { SessionType.valueOf(json.optString("type")) }.getOrDefault(SessionType.Input),
      tagName = json.optString("tagName"),
      tagMultiplier = json.optDouble("tagMultiplier", 1.0),
      typeRate = json.optDouble("typeRate", 0.5),
      startedAt = json.optInstant("startedAt"),
      endedAt = json.optInstant("endedAt"),
      activeDurationMinutes = json.optDouble("activeDurationMinutes"),
      earnedMinutes = json.optDouble("earnedMinutes"),
      deletedAt = json.optNullableInstant("deletedAt"),
    )

  private fun leisureRecordToJson(record: LeisureRecord): JSONObject =
    JSONObject()
      .put("id", record.id)
      .put("startedAt", record.startedAt.toString())
      .put("endedAt", record.endedAt.toString())
      .put("elapsedMinutes", record.elapsedMinutes)
      .put("costMinutes", record.costMinutes)
      .put("deletedAt", record.deletedAt?.toString())

  private fun jsonToLeisureRecord(json: JSONObject): LeisureRecord =
    LeisureRecord(
      id = json.optString("id"),
      startedAt = json.optInstant("startedAt"),
      endedAt = json.optInstant("endedAt"),
      elapsedMinutes = json.optDouble("elapsedMinutes"),
      costMinutes = json.optDouble("costMinutes"),
      deletedAt = json.optNullableInstant("deletedAt"),
    )

  private fun JSONObject.optInstant(key: String): Instant =
    runCatching { Instant.parse(optString(key)) }.getOrDefault(Instant.now())

  private fun JSONObject.optNullableInstant(key: String): Instant? {
    val value = optStringOrNull(key) ?: return null
    return runCatching { Instant.parse(value) }.getOrNull()
  }

  private fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) optString(key) else null

  private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    List(length()) { index -> transform(getJSONObject(index)) }

  private fun FocusWellUiState.withComputedTrackers(): FocusWellUiState {
    val activeFocusRecords = focusRecords.filter { it.deletedAt == null }
    val computed =
      trackers.map { tracker ->
        val tagName = tracker.ruleTagName
        val target = tracker.ruleTargetMinutes
        if (tracker.archivedAt != null || tagName == null || target == null) {
          tracker
        } else {
          val minutes =
            activeFocusRecords
              .filter { it.tagName.equals(tagName, ignoreCase = true) }
              .sumOf { it.activeDurationMinutes }
          tracker.copy(
            completed = minutes >= target,
            progressLabel = "${minutes.roundMinutes()} / ${target.roundTarget()}",
          )
        }
      }
    return copy(trackers = computed)
  }

  private fun Double.roundMinutes(): String {
    val rounded = toInt()
    return if (rounded >= 60) "${rounded / 60}h ${rounded % 60}m" else "${rounded}m"
  }

  private fun Double.roundTarget(): String {
    val rounded = toInt()
    return if (rounded % 60 == 0) "${rounded / 60}h" else "${rounded}m"
  }

  private companion object {
    const val KEY_STATE = "state"
  }
}
