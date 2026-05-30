package dev.nihildigit.focuswell.data.db

import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.Idea
import dev.nihildigit.focuswell.domain.IdeaChecklistItem
import dev.nihildigit.focuswell.domain.IdeaQuadrant
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.LeisureRecord
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.TagConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.Instant

@Serializable
private data class StoredIdeaChecklistItem(
  val id: String = "",
  val text: String = "",
  val checked: Boolean = false,
)

internal fun FocusWellUiState.toAppStateEntity(): AppStateEntity =
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
    stateUpdatedAt = stateUpdatedAt.toString(),
    dailyGrantMinutes = normalizedRules.dailyGrantMinutes,
    dayBoundaryHour = normalizedRules.dayBoundaryHour,
    wakeTargetHour = normalizedRules.wakeTargetHour,
    sleepProtectionStartHour = normalizedRules.sleepProtectionStartHour,
    sleepProtectionEndHour = normalizedRules.sleepProtectionEndHour,
    sleepProtectionMultiplier = normalizedRules.sleepProtectionMultiplier,
    longSessionRemindersEnabled = normalizedRules.longSessionRemindersEnabled,
    lastCheckInDailyDate = lastCheckInDailyDate,
    lastPhoneUsageSettlementAt = lastPhoneUsageSettlementAt?.toString(),
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

internal fun AppStateEntity.toActiveMode(): ActiveMode =
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

internal fun TagConfig.toEntity(sortOrder: Int): TagEntity =
  TagEntity(
    id = id,
    sortOrder = sortOrder,
    name = name,
    multiplier = multiplier,
    archivedAt = archivedAt?.toString(),
  )

internal fun TagEntity.toDomain(): TagConfig =
  TagConfig(
    id = id,
    name = name,
    multiplier = multiplier,
    archivedAt = archivedAt?.let(Instant::parse),
  )

internal fun DailyTracker.toEntity(sortOrder: Int): DailyTrackerEntity =
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

internal fun DailyTrackerEntity.toDomain(): DailyTracker =
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

internal fun FocusRecord.toEntity(): FocusRecordEntity =
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

internal fun FocusRecordEntity.toDomain(): FocusRecord =
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

internal fun LeisureRecord.toEntity(): LeisureRecordEntity =
  LeisureRecordEntity(
    id = id,
    startedAt = startedAt.toString(),
    endedAt = endedAt.toString(),
    elapsedMinutes = elapsedMinutes,
    costMinutes = costMinutes,
    dailyDate = dailyDate,
    deletedAt = deletedAt?.toString(),
  )

internal fun LeisureRecordEntity.toDomain(): LeisureRecord =
  LeisureRecord(
    id = id,
    startedAt = Instant.parse(startedAt),
    endedAt = Instant.parse(endedAt),
    elapsedMinutes = elapsedMinutes,
    costMinutes = costMinutes,
    dailyDate = dailyDate,
    deletedAt = deletedAt?.let(Instant::parse),
  )

internal fun Idea.toEntity(): IdeaEntity =
  IdeaEntity(
    id = id,
    text = text,
    quadrant = quadrant.name,
    checklistJson = checklistToJson(checklist),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    archivedAt = archivedAt?.toString(),
  )

internal fun IdeaEntity.toDomain(): Idea =
  Idea(
    id = id,
    text = text,
    quadrant = runCatching { IdeaQuadrant.valueOf(quadrant) }.getOrDefault(IdeaQuadrant.Inbox),
    checklist = jsonToChecklist(checklistJson),
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
    archivedAt = archivedAt?.let(Instant::parse),
  )

private val roomJson =
  Json {
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
  }

private fun checklistToJson(items: List<IdeaChecklistItem>): String =
  roomJson.encodeToString(
    serializer = ListSerializer(StoredIdeaChecklistItem.serializer()),
    value =
      items.map { item ->
        StoredIdeaChecklistItem(
          id = item.id,
          text = item.text,
          checked = item.checked,
        )
      },
  )

private fun jsonToChecklist(raw: String): List<IdeaChecklistItem> =
  runCatching {
    roomJson
      .decodeFromString(ListSerializer(StoredIdeaChecklistItem.serializer()), raw)
      .mapIndexed { index, item ->
        IdeaChecklistItem(
          id = item.id.ifBlank { "task-$index" },
          text = item.text,
          checked = item.checked,
        )
      }
      .filter { it.text.isNotBlank() }
  }.getOrDefault(emptyList())

internal fun LedgerEntry.toEntity(): LedgerEntryEntity =
  LedgerEntryEntity(
    id = id,
    title = title,
    deltaMinutes = deltaMinutes,
    createdAt = createdAt.toString(),
    note = note,
    sourceId = sourceId,
  )

internal fun LedgerEntryEntity.toDomain(): LedgerEntry =
  LedgerEntry(
    id = id,
    title = title,
    deltaMinutes = deltaMinutes,
    createdAt = Instant.parse(createdAt),
    note = note,
    sourceId = sourceId,
  )
