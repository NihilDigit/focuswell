package dev.nihildigit.focuswell.data.db

import android.content.Context
import dev.nihildigit.focuswell.data.FocusWellStore
import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.LeisureRecord
import dev.nihildigit.focuswell.domain.Idea
import dev.nihildigit.focuswell.domain.IdeaChecklistItem
import dev.nihildigit.focuswell.domain.IdeaQuadrant
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.TagConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

internal class RoomFocusWellStore(
  private val dao: FocusWellDao,
) : FocusWellStore {
  constructor(context: Context) : this(FocusWellDatabase.get(context).focusWellDao())

  override fun loadState(): FocusWellUiState? =
    runBlocking(Dispatchers.IO) {
      val appState = dao.appState() ?: return@runBlocking null
      FocusWellUiState(
        dailyDate = appState.dailyDate,
        rules =
          FocusWellRules(
            dailyGrantMinutes = appState.dailyGrantMinutes,
            dayBoundaryHour = appState.dayBoundaryHour,
            wakeTargetHour = appState.wakeTargetHour,
            sleepProtectionStartHour = appState.sleepProtectionStartHour,
            sleepProtectionMultiplier = appState.sleepProtectionMultiplier,
            longSessionRemindersEnabled = appState.longSessionRemindersEnabled,
          ).normalized(),
        activeMode = appState.toActiveMode(),
        tags = dao.tags().map { it.toDomain() },
        trackers = dao.trackers().map { it.toDomain() },
        focusRecords = dao.focusRecords().map { it.toDomain() },
        leisureRecords = dao.leisureRecords().map { it.toDomain() },
        ideas = dao.ideas().map { it.toDomain() },
        ledger = dao.ledger().map { it.toDomain() },
        lastCheckInDailyDate = appState.lastCheckInDailyDate,
        dailyGrantPausedUntilDate = appState.dailyGrantPausedUntilDate,
      ).withLedgerBackedReserve()
    }

  override fun persistChange(previous: FocusWellUiState?, next: FocusWellUiState) {
    runBlocking(Dispatchers.IO) {
      if (previous == null) {
        dao.replaceState(
          appState = next.toAppStateEntity(),
          tags = next.tags.mapIndexed { index, tag -> tag.toEntity(index) },
          trackers = next.trackers.mapIndexed { index, tracker -> tracker.toEntity(index) },
          focusRecords = next.focusRecords.map { it.toEntity() },
          leisureRecords = next.leisureRecords.map { it.toEntity() },
          ideas = next.ideas.map { it.toEntity() },
          ledger = next.ledger.map { it.toEntity() },
        )
        return@runBlocking
      }

      val tagDiff =
        listDiff(
          previous = previous.tags.mapIndexed { index, tag -> tag.toEntity(index) },
          next = next.tags.mapIndexed { index, tag -> tag.toEntity(index) },
          id = TagEntity::id,
        )
      val trackerDiff =
        listDiff(
          previous = previous.trackers.mapIndexed { index, tracker -> tracker.toEntity(index) },
          next = next.trackers.mapIndexed { index, tracker -> tracker.toEntity(index) },
          id = DailyTrackerEntity::id,
        )
      val focusRecordDiff =
        listDiff(
          previous = previous.focusRecords.map { it.toEntity() },
          next = next.focusRecords.map { it.toEntity() },
          id = FocusRecordEntity::id,
        )
      val leisureRecordDiff =
        listDiff(
          previous = previous.leisureRecords.map { it.toEntity() },
          next = next.leisureRecords.map { it.toEntity() },
          id = LeisureRecordEntity::id,
        )
      val ideaDiff =
        listDiff(
          previous = previous.ideas.map { it.toEntity() },
          next = next.ideas.map { it.toEntity() },
          id = IdeaEntity::id,
        )
      val ledgerDiff =
        listDiff(
          previous = previous.ledger.map { it.toEntity() },
          next = next.ledger.map { it.toEntity() },
          id = LedgerEntryEntity::id,
        )
      dao.applyStateDiff(
        appState = next.toAppStateEntity(),
        changedTags = tagDiff.changed,
        removedTagIds = tagDiff.removedIds,
        changedTrackers = trackerDiff.changed,
        removedTrackerIds = trackerDiff.removedIds,
        changedFocusRecords = focusRecordDiff.changed,
        removedFocusRecordIds = focusRecordDiff.removedIds,
        changedLeisureRecords = leisureRecordDiff.changed,
        removedLeisureRecordIds = leisureRecordDiff.removedIds,
        changedIdeas = ideaDiff.changed,
        removedIdeaIds = ideaDiff.removedIds,
        changedLedgerEntries = ledgerDiff.changed,
        removedLedgerEntryIds = ledgerDiff.removedIds,
      )
    }
  }

  override fun clear() {
    runBlocking(Dispatchers.IO) {
      dao.clearAll()
    }
  }

  private fun <T> listDiff(
    previous: List<T>,
    next: List<T>,
    id: (T) -> String,
  ): EntityDiff<T> {
    val previousById = previous.associateBy(id)
    val nextById = next.associateBy(id)
    val changed = next.filter { previousById[id(it)] != it }
    val removed = previousById.keys - nextById.keys
    return EntityDiff(changed = changed, removedIds = removed.toList())
  }
}

private data class EntityDiff<T>(
  val changed: List<T>,
  val removedIds: List<String>,
)

private fun FocusWellUiState.toAppStateEntity(): AppStateEntity =
  when (val mode = activeMode) {
    ActiveMode.None ->
      baseAppStateEntity(activeKind = "none")

    is ActiveMode.Focus ->
      baseAppStateEntity(
        activeKind = "focus",
        activeStartedAt = mode.startedAt.toString(),
        activeFocusTask = mode.task,
        activeFocusType = mode.type.name,
        activeTagId = mode.tag?.id,
        activeTagName = mode.tag?.name,
        activeTagMultiplier = mode.tag?.multiplier,
        activeTagArchivedAt = mode.tag?.archivedAt?.toString(),
        activeReminderSessionId = mode.reminderSessionId,
        activeRevision = mode.revision,
        activePaused = mode.paused,
        activePausedAt = mode.pausedAt?.toString(),
        activePausedDurationMillis = mode.pausedDurationMillis,
      )

    is ActiveMode.Leisure ->
      baseAppStateEntity(
        activeKind = "leisure",
        activeStartedAt = mode.startedAt.toString(),
        activeReminderSessionId = mode.reminderSessionId,
        activeRevision = mode.revision,
      )

    ActiveMode.Depleted ->
      baseAppStateEntity(activeKind = "depleted")
  }

private fun FocusWellUiState.baseAppStateEntity(
  activeKind: String,
  activeStartedAt: String? = null,
  activeFocusTask: String? = null,
  activeFocusType: String? = null,
  activeTagId: String? = null,
  activeTagName: String? = null,
  activeTagMultiplier: Double? = null,
  activeTagArchivedAt: String? = null,
  activeReminderSessionId: String? = null,
  activeRevision: Int = 1,
  activePaused: Boolean = false,
  activePausedAt: String? = null,
  activePausedDurationMillis: Long = 0,
): AppStateEntity {
  val normalizedRules = rules.normalized()
  return AppStateEntity(
    dailyDate = dailyDate,
    dailyGrantMinutes = normalizedRules.dailyGrantMinutes,
    dayBoundaryHour = normalizedRules.dayBoundaryHour,
    wakeTargetHour = normalizedRules.wakeTargetHour,
    sleepProtectionStartHour = normalizedRules.sleepProtectionStartHour,
    sleepProtectionMultiplier = normalizedRules.sleepProtectionMultiplier,
    longSessionRemindersEnabled = normalizedRules.longSessionRemindersEnabled,
    lastCheckInDailyDate = lastCheckInDailyDate,
    dailyGrantPausedUntilDate = dailyGrantPausedUntilDate,
    activeKind = activeKind,
    activeStartedAt = activeStartedAt,
    activeFocusTask = activeFocusTask,
    activeFocusType = activeFocusType,
    activeTagId = activeTagId,
    activeTagName = activeTagName,
    activeTagMultiplier = activeTagMultiplier,
    activeTagArchivedAt = activeTagArchivedAt,
    activeReminderSessionId = activeReminderSessionId,
    activeRevision = activeRevision,
    activePaused = activePaused,
    activePausedAt = activePausedAt,
    activePausedDurationMillis = activePausedDurationMillis,
  )
}

private fun AppStateEntity.toActiveMode(): ActiveMode =
  when (activeKind) {
    "focus" ->
      ActiveMode.Focus(
        task = activeFocusTask.orEmpty(),
        type = activeFocusType?.let { runCatching { SessionType.valueOf(it) }.getOrNull() } ?: SessionType.Input,
        tag =
          activeTagName?.let { name ->
            TagConfig(
              id = activeTagId.orEmpty(),
              name = name,
              multiplier = activeTagMultiplier ?: 1.0,
              archivedAt = activeTagArchivedAt?.let(Instant::parse),
            )
          },
        startedAt = Instant.parse(activeStartedAt),
        reminderSessionId = activeReminderSessionId ?: "focus-${Instant.parse(activeStartedAt).toEpochMilli()}",
        revision = activeRevision,
        paused = activePaused,
        pausedAt = activePausedAt?.let(Instant::parse),
        pausedDurationMillis = activePausedDurationMillis,
      )

    "leisure" -> {
      val startedAt = Instant.parse(activeStartedAt)
      ActiveMode.Leisure(
        startedAt = startedAt,
        reminderSessionId = activeReminderSessionId ?: "leisure-${startedAt.toEpochMilli()}",
        revision = activeRevision,
      )
    }

    "windDown" -> ActiveMode.None
    "depleted" -> ActiveMode.Depleted
    else -> ActiveMode.None
  }

private fun TagConfig.toEntity(sortOrder: Int): TagEntity =
  TagEntity(
    id = id,
    sortOrder = sortOrder,
    name = name,
    multiplier = multiplier,
    archivedAt = archivedAt?.toString(),
  )

private fun TagEntity.toDomain(): TagConfig =
  TagConfig(
    id = id,
    name = name,
    multiplier = multiplier,
    archivedAt = archivedAt?.let(Instant::parse),
  )

private fun DailyTracker.toEntity(sortOrder: Int): DailyTrackerEntity =
  DailyTrackerEntity(
    id = id,
    sortOrder = sortOrder,
    label = label,
    completed = completed,
    rewardMinutes = rewardMinutes,
    progressLabel = progressLabel,
    ruleTagName = ruleTagName,
    ruleTargetMinutes = ruleTargetMinutes,
    wakeTime = wakeTime,
    archivedAt = archivedAt?.toString(),
  )

private fun DailyTrackerEntity.toDomain(): DailyTracker =
  DailyTracker(
    id = id,
    label = label,
    completed = completed,
    rewardMinutes = rewardMinutes,
    progressLabel = progressLabel,
    ruleTagName = ruleTagName,
    ruleTargetMinutes = ruleTargetMinutes,
    wakeTime = wakeTime,
    archivedAt = archivedAt?.let(Instant::parse),
  )

private fun FocusRecord.toEntity(): FocusRecordEntity =
  FocusRecordEntity(
    id = id,
    task = task,
    result = result,
    type = type.name,
    tagName = tagName,
    tagMultiplier = tagMultiplier,
    typeRate = typeRate,
    startedAt = startedAt.toString(),
    endedAt = endedAt.toString(),
    activeDurationMinutes = activeDurationMinutes,
    earnedMinutes = earnedMinutes,
    dailyDate = dailyDate,
    deletedAt = deletedAt?.toString(),
  )

private fun FocusRecordEntity.toDomain(): FocusRecord =
  FocusRecord(
    id = id,
    task = task,
    result = result,
    type = runCatching { SessionType.valueOf(type) }.getOrDefault(SessionType.Input),
    tagName = tagName,
    tagMultiplier = tagMultiplier,
    typeRate = typeRate,
    startedAt = Instant.parse(startedAt),
    endedAt = Instant.parse(endedAt),
    activeDurationMinutes = activeDurationMinutes,
    earnedMinutes = earnedMinutes,
    dailyDate = dailyDate,
    deletedAt = deletedAt?.let(Instant::parse),
  )

private fun LeisureRecord.toEntity(): LeisureRecordEntity =
  LeisureRecordEntity(
    id = id,
    startedAt = startedAt.toString(),
    endedAt = endedAt.toString(),
    elapsedMinutes = elapsedMinutes,
    costMinutes = costMinutes,
    dailyDate = dailyDate,
    deletedAt = deletedAt?.toString(),
  )

private fun LeisureRecordEntity.toDomain(): LeisureRecord =
  LeisureRecord(
    id = id,
    startedAt = Instant.parse(startedAt),
    endedAt = Instant.parse(endedAt),
    elapsedMinutes = elapsedMinutes,
    costMinutes = costMinutes,
    dailyDate = dailyDate,
    deletedAt = deletedAt?.let(Instant::parse),
  )

private fun Idea.toEntity(): IdeaEntity =
  IdeaEntity(
    id = id,
    text = text,
    quadrant = quadrant.name,
    checklistJson = checklistToJson(checklist).toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    archivedAt = archivedAt?.toString(),
  )

private fun IdeaEntity.toDomain(): Idea =
  Idea(
    id = id,
    text = text,
    quadrant = runCatching { IdeaQuadrant.valueOf(quadrant) }.getOrDefault(IdeaQuadrant.Inbox),
    checklist = jsonToChecklist(checklistJson),
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
    archivedAt = archivedAt?.let(Instant::parse),
  )

private fun checklistToJson(items: List<IdeaChecklistItem>): JSONArray =
  JSONArray(
    items.map { item ->
      JSONObject()
        .put("id", item.id)
        .put("text", item.text)
        .put("checked", item.checked)
    }
  )

private fun jsonToChecklist(raw: String): List<IdeaChecklistItem> =
  runCatching {
    val array = JSONArray(raw)
    List(array.length()) { index ->
      val item = array.getJSONObject(index)
      IdeaChecklistItem(
        id = item.optString("id", "task-$index"),
        text = item.optString("text"),
        checked = item.optBoolean("checked"),
      )
    }.filter { it.text.isNotBlank() }
  }.getOrDefault(emptyList())

private fun LedgerEntry.toEntity(): LedgerEntryEntity =
  LedgerEntryEntity(
    id = id,
    title = title,
    deltaMinutes = deltaMinutes,
    createdAt = createdAt.toString(),
    note = note,
    sourceId = sourceId,
  )

private fun LedgerEntryEntity.toDomain(): LedgerEntry =
  LedgerEntry(
    id = id,
    title = title,
    deltaMinutes = deltaMinutes,
    createdAt = Instant.parse(createdAt),
    note = note,
    sourceId = sourceId,
  )

private fun FocusWellUiState.withLedgerBackedReserve(): FocusWellUiState =
  copy(reserveMinutes = ledger.sumOf { it.deltaMinutes }.coerceAtLeast(0.0))
