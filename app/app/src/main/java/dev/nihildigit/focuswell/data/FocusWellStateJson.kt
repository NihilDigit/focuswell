package dev.nihildigit.focuswell.data

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
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.domain.defaultTags
import dev.nihildigit.focuswell.domain.defaultTrackers
import kotlinx.serialization.json.Json
import java.time.Instant

internal class FocusWellStateJsonCodec(
  private val now: () -> Instant,
) {
  private val json =
    Json {
      encodeDefaults = true
      explicitNulls = false
      ignoreUnknownKeys = true
      prettyPrint = true
    }

  fun encode(state: FocusWellUiState): String =
    json.encodeToString(SerializedFocusWellState.serializer(), state.toSerialized())

  fun decode(raw: String): FocusWellUiState =
    json.decodeFromString(SerializedFocusWellState.serializer(), raw).toDomain(now)
}

private fun FocusWellUiState.toSerialized(): SerializedFocusWellState =
  SerializedFocusWellState(
    stateUpdatedAtUtc = stateUpdatedAt.toString(),
    reserveMinutes = reserveMinutes,
    dailyDate = dailyDate,
    rules = rules.normalized().toSerialized(),
    activeMode = activeMode.toSerialized(),
    tags = tags.map(TagConfig::toSerialized),
    trackers = trackers.map(DailyTracker::toSerialized),
    focusRecords = focusRecords.map(FocusRecord::toSerialized),
    leisureRecords = leisureRecords.map(LeisureRecord::toSerialized),
    ideas = ideas.map(Idea::toSerialized),
    ledger = ledger.map(LedgerEntry::toSerialized),
    lastCheckInDailyDate = lastCheckInDailyDate,
    lastPhoneUsageSettlementAt = lastPhoneUsageSettlementAt?.toString(),
    dailyGrantPausedUntilDate = dailyGrantPausedUntilDate,
  )

private fun SerializedFocusWellState.toDomain(now: () -> Instant): FocusWellUiState =
  FocusWellUiState(
    reserveMinutes = reserveMinutes,
    dailyDate = dailyDate ?: TimeAccounting.dailyDate(now()).toString(),
    stateUpdatedAt = stateUpdatedAtUtc?.toInstantOrNull() ?: now(),
    rules = rules?.toDomain()?.normalized() ?: FocusWellRules(),
    activeMode = activeMode.toDomain(),
    tags = tags.map(SerializedTag::toDomain).ifEmpty { defaultTags },
    trackers = trackers.map(SerializedDailyTracker::toDomain).ifEmpty { defaultTrackers },
    focusRecords = focusRecords.map(SerializedFocusRecord::toDomain),
    leisureRecords = leisureRecords.map(SerializedLeisureRecord::toDomain),
    ideas = ideas.map(SerializedIdea::toDomain),
    ledger = ledger.map(SerializedLedgerEntry::toDomain),
    lastCheckInDailyDate = lastCheckInDailyDate,
    lastPhoneUsageSettlementAt = lastPhoneUsageSettlementAt?.toInstantOrNull(),
    dailyGrantPausedUntilDate = dailyGrantPausedUntilDate,
  ).withComputedTrackers().withLedgerBackedReserve()

private fun FocusWellRules.toSerialized(): SerializedRules =
  SerializedRules(
    dailyGrantMinutes = dailyGrantMinutes,
    dayBoundaryHour = dayBoundaryHour,
    wakeTargetHour = wakeTargetHour,
    wakeTargetMinute = wakeTargetMinute,
    sleepProtectionStartHour = sleepProtectionStartHour,
    sleepProtectionEndHour = sleepProtectionEndHour,
    sleepProtectionMultiplier = sleepProtectionMultiplier,
    longSessionRemindersEnabled = longSessionRemindersEnabled,
    phoneUsageChargeFreePackages = phoneUsageChargeFreePackages.toList(),
  )

private fun SerializedRules.toDomain(): FocusWellRules =
  FocusWellRules(
    dailyGrantMinutes = dailyGrantMinutes,
    dayBoundaryHour = dayBoundaryHour,
    wakeTargetHour = wakeTargetHour,
    wakeTargetMinute = wakeTargetMinute,
    sleepProtectionStartHour = sleepProtectionStartHour,
    sleepProtectionEndHour = sleepProtectionEndHour,
    sleepProtectionMultiplier = sleepProtectionMultiplier,
    longSessionRemindersEnabled = longSessionRemindersEnabled,
    phoneUsageChargeFreePackages = phoneUsageChargeFreePackages.toSet(),
  )

private fun ActiveMode.toSerialized(): SerializedActiveMode =
  when (this) {
    ActiveMode.None -> SerializedActiveMode(kind = "none")
    is ActiveMode.Focus ->
      SerializedActiveMode(
        kind = "focus",
        task = task,
        type = type.name,
        tag = tag?.toSerialized(),
        startedAt = startedAt.toString(),
        reminderSessionId = reminderSessionId,
        revision = revision,
        paused = paused,
        pausedAt = pausedAt?.toString(),
        pausedDurationMillis = pausedDurationMillis,
      )

    is ActiveMode.Leisure ->
      SerializedActiveMode(
        kind = "leisure",
        startedAt = startedAt.toString(),
        reminderSessionId = reminderSessionId,
        revision = revision,
      )

    ActiveMode.Depleted -> SerializedActiveMode(kind = "depleted")
  }

private fun SerializedActiveMode?.toDomain(): ActiveMode {
  if (this == null) return ActiveMode.None
  return when (kind) {
    "focus" -> {
      val started = startedAt?.let(Instant::parse) ?: return ActiveMode.None
      ActiveMode.Focus(
        task = task.orEmpty(),
        type = type?.let { runCatching { SessionType.valueOf(it) }.getOrNull() } ?: SessionType.Input,
        tag = tag?.toDomain(),
        startedAt = started,
        reminderSessionId = reminderSessionId ?: "focus-${started.toEpochMilli()}",
        revision = revision,
        paused = paused,
        pausedAt = pausedAt?.toInstantOrNull(),
        pausedDurationMillis = pausedDurationMillis,
      )
    }

    "leisure" -> {
      val started = startedAt?.let(Instant::parse) ?: return ActiveMode.None
      ActiveMode.Leisure(
        startedAt = started,
        reminderSessionId = reminderSessionId ?: "leisure-${started.toEpochMilli()}",
        revision = revision,
      )
    }

    "depleted" -> ActiveMode.Depleted
    "windDown" -> ActiveMode.None
    else -> ActiveMode.None
  }
}

private fun TagConfig.toSerialized(): SerializedTag =
  SerializedTag(id = id, name = name, multiplier = multiplier, archivedAt = archivedAt?.toString())

private fun SerializedTag.toDomain(): TagConfig =
  TagConfig(id = id, name = name, multiplier = multiplier, archivedAt = archivedAt?.toInstantOrNull())

private fun DailyTracker.toSerialized(): SerializedDailyTracker =
  SerializedDailyTracker(
    id = id,
    label = label,
    completed = completed,
    rewardMinutes = rewardMinutes,
    progressLabel = progressLabel,
    ruleTagName = ruleTagName,
    ruleTargetMinutes = ruleTargetMinutes,
    wakeTime = wakeTime,
    archivedAt = archivedAt?.toString(),
  )

private fun SerializedDailyTracker.toDomain(): DailyTracker =
  DailyTracker(
    id = id,
    label = label,
    completed = completed,
    rewardMinutes = rewardMinutes ?: if (ruleTagName == null) 15.0 else 60.0,
    progressLabel = progressLabel,
    ruleTagName = ruleTagName,
    ruleTargetMinutes = ruleTargetMinutes,
    wakeTime = wakeTime,
    archivedAt = archivedAt?.toInstantOrNull(),
  )

private fun FocusRecord.toSerialized(): SerializedFocusRecord =
  SerializedFocusRecord(
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

private fun SerializedFocusRecord.toDomain(): FocusRecord =
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
    dailyDate = dailyDate ?: TimeAccounting.dailyDate(Instant.parse(endedAt)).toString(),
    deletedAt = deletedAt?.toInstantOrNull(),
  )

private fun LeisureRecord.toSerialized(): SerializedLeisureRecord =
  SerializedLeisureRecord(
    id = id,
    startedAt = startedAt.toString(),
    endedAt = endedAt.toString(),
    elapsedMinutes = elapsedMinutes,
    costMinutes = costMinutes,
    dailyDate = dailyDate,
    deletedAt = deletedAt?.toString(),
  )

private fun SerializedLeisureRecord.toDomain(): LeisureRecord =
  LeisureRecord(
    id = id,
    startedAt = Instant.parse(startedAt),
    endedAt = Instant.parse(endedAt),
    elapsedMinutes = elapsedMinutes,
    costMinutes = costMinutes,
    dailyDate = dailyDate ?: TimeAccounting.dailyDate(Instant.parse(endedAt)).toString(),
    deletedAt = deletedAt?.toInstantOrNull(),
  )

private fun Idea.toSerialized(): SerializedIdea =
  SerializedIdea(
    id = id,
    text = text,
    quadrant = quadrant.name,
    checklist = checklist.map(IdeaChecklistItem::toSerialized),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    archivedAt = archivedAt?.toString(),
  )

private fun SerializedIdea.toDomain(): Idea =
  Idea(
    id = id,
    text = text,
    quadrant = runCatching { IdeaQuadrant.valueOf(quadrant) }.getOrDefault(IdeaQuadrant.Inbox),
    checklist = checklist.map(SerializedIdeaChecklistItem::toDomain),
    createdAt = Instant.parse(createdAt),
    updatedAt = updatedAt?.toInstantOrNull() ?: Instant.parse(createdAt),
    archivedAt = archivedAt?.toInstantOrNull(),
  )

private fun IdeaChecklistItem.toSerialized(): SerializedIdeaChecklistItem =
  SerializedIdeaChecklistItem(id = id, text = text, checked = checked)

private fun SerializedIdeaChecklistItem.toDomain(): IdeaChecklistItem =
  IdeaChecklistItem(id = id, text = text, checked = checked)

private fun LedgerEntry.toSerialized(): SerializedLedgerEntry =
  SerializedLedgerEntry(
    id = id,
    title = title,
    deltaMinutes = deltaMinutes,
    createdAt = createdAt.toString(),
    note = note,
    sourceId = sourceId,
  )

private fun SerializedLedgerEntry.toDomain(): LedgerEntry =
  LedgerEntry(
    id = id,
    title = title,
    deltaMinutes = deltaMinutes,
    createdAt = Instant.parse(createdAt),
    note = note,
    sourceId = sourceId,
  )

private fun String.toInstantOrNull(): Instant? =
  runCatching { Instant.parse(this) }.getOrNull()
