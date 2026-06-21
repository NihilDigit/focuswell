package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.LeisureRecord
import dev.nihildigit.focuswell.domain.RESERVE_RECOVERY_FOCUS_MINUTES
import dev.nihildigit.focuswell.domain.RESERVE_RECOVERY_GRANT_MINUTES
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.domain.focusOutcomeMultiplier
import dev.nihildigit.focuswell.domain.reserveLocked
import dev.nihildigit.focuswell.time.toKotlinInstant
import java.time.Instant
import kotlin.time.Duration as KotlinDuration
import kotlin.time.Duration.Companion.milliseconds

internal data class StartedFocusSession(
  val state: FocusWellUiState,
  val focus: ActiveMode.Focus,
)

internal data class EndedFocusSession(
  val state: FocusWellUiState,
  val reminderSessionId: String,
)

internal data class StartedLeisureSession(
  val state: FocusWellUiState,
  val leisure: ActiveMode.Leisure,
)

internal data class EndedLeisureSession(
  val state: FocusWellUiState,
  val reminderSessionId: String,
)

internal fun FocusWellUiState.withStartedFocusSession(
  task: String,
  type: SessionType,
  tagId: String?,
  startedAt: Instant,
  reminderSessionId: String,
): StartedFocusSession {
  val trimmed = task.trim().ifBlank { "Focus session" }
  val tag = tagId?.let { selectedId -> tags.firstOrNull { it.id == selectedId } }
  val focus =
    ActiveMode.Focus(
      task = trimmed,
      type = type,
      tag = tag,
      startedAt = startedAt,
      reminderSessionId = reminderSessionId,
    )
  return StartedFocusSession(
    state = copy(activeMode = focus),
    focus = focus,
  )
}

internal fun FocusWellUiState.withPausedFocusSession(pausedAt: Instant): FocusWellUiState {
  val focus = activeMode as? ActiveMode.Focus ?: return this
  return if (focus.paused) this else copy(activeMode = focus.copy(paused = true, pausedAt = pausedAt))
}

internal fun FocusWellUiState.withResumedFocusSession(resumedAt: Instant): FocusWellUiState {
  val focus = activeMode as? ActiveMode.Focus ?: return this
  val pausedAt = focus.pausedAt
  val extraPaused =
    if (pausedAt == null) 0L else (resumedAt.toKotlinInstant() - pausedAt.toKotlinInstant()).inWholeMilliseconds.coerceAtLeast(0)
  return copy(
    activeMode =
      focus.copy(
        paused = false,
        pausedAt = null,
        pausedDurationMillis = focus.pausedDurationMillis + extraPaused,
      )
  )
}

internal fun FocusWellUiState.withEndedFocusSession(
  endedAt: Instant,
  result: String,
  correctionMinutes: Double = 0.0,
): EndedFocusSession? {
  val focus = activeMode as? ActiveMode.Focus ?: return null
  val currentPauseMillis =
    if (focus.paused && focus.pausedAt != null) {
      (endedAt.toKotlinInstant() - focus.pausedAt.toKotlinInstant()).inWholeMilliseconds.coerceAtLeast(0)
    } else {
      0L
    }
  val activeDuration =
    (endedAt.toKotlinInstant() - focus.startedAt.toKotlinInstant() - (focus.pausedDurationMillis + currentPauseMillis).milliseconds)
      .coerceAtLeast(KotlinDuration.ZERO)
  val activeDurationMinutes = activeDuration.inWholeMilliseconds / 60_000.0
  val adjustedActiveMinutes = (activeDurationMinutes - correctionMinutes.coerceAtLeast(0.0)).coerceAtLeast(0.0)
  val savedResult = result.ifBlank { "As planned" }
  val hasAnyPause = focus.pausedDurationMillis + currentPauseMillis > 0L
  val unlocksReserve = reserveLocked && activeDurationMinutes >= RESERVE_RECOVERY_FOCUS_MINUTES && !hasAnyPause
  if (reserveLocked && !unlocksReserve) {
    return EndedFocusSession(
      state = copy(activeMode = ActiveMode.None),
      reminderSessionId = focus.reminderSessionId,
    )
  }
  val earned =
    TimeAccounting.focusEarnedMinutes(
      activeDurationMinutes = adjustedActiveMinutes,
      typeRate = focus.type.rate,
      tagMultiplier = focus.tag?.multiplier ?: 1.0,
      outcomeMultiplier = focusOutcomeMultiplier(savedResult),
    )
  val record =
    FocusRecord(
      id = FocusWellIds.focus(endedAt),
      task = focus.task,
      result = savedResult,
      type = focus.type,
      tagName = focus.tag?.name,
      tagMultiplier = focus.tag?.multiplier ?: 1.0,
      typeRate = focus.type.rate,
      startedAt = focus.startedAt,
      endedAt = endedAt,
      activeDurationMinutes = adjustedActiveMinutes,
      earnedMinutes = earned,
      dailyDate = TimeAccounting.dailyDate(endedAt, rules = rules).toString(),
    )
  val entry =
    LedgerEntry(
      id = FocusWellIds.ledger(record.id),
      title = "Focus · ${focus.type.label}${focus.tag?.let { " ${it.name}" } ?: ""}",
      deltaMinutes = earned,
      createdAt = endedAt,
      note = record.result,
      sourceId = record.id,
    )
  val recoveryEntry =
    if (unlocksReserve) {
      LedgerEntry(
        id = FocusWellIds.reserveRecovery(record.id),
        title = "Recovery focus",
        deltaMinutes = RESERVE_RECOVERY_GRANT_MINUTES,
        createdAt = endedAt,
        note = "2h continuous focus unlocked reserve",
        sourceId = record.id,
      )
    } else {
      null
    }
  return EndedFocusSession(
    state =
      copy(
        reserveMinutes = reserveMinutes + earned + (recoveryEntry?.deltaMinutes ?: 0.0),
        activeMode = ActiveMode.None,
        dailyGrantPausedUntilDate = if (unlocksReserve) null else dailyGrantPausedUntilDate,
        lastPhoneUsageSettlementAt = if (unlocksReserve && (lastPhoneUsageSettlementAt == null || endedAt.isAfter(lastPhoneUsageSettlementAt))) endedAt else lastPhoneUsageSettlementAt,
        focusRecords = listOf(record) + focusRecords,
        ledger = listOfNotNull(recoveryEntry, entry) + ledger,
      ),
    reminderSessionId = focus.reminderSessionId,
  )
}

internal fun FocusWellUiState.withStartedLeisureSession(
  startedAt: Instant,
  reminderSessionId: String,
): StartedLeisureSession? {
  if (reserveLocked || reserveMinutes <= 0.0) return null
  val leisure = ActiveMode.Leisure(startedAt = startedAt, reminderSessionId = reminderSessionId)
  return StartedLeisureSession(
    state = copy(activeMode = leisure),
    leisure = leisure,
  )
}

internal fun FocusWellUiState.withEndedLeisureSession(endedAt: Instant): EndedLeisureSession? {
  val leisure = activeMode as? ActiveMode.Leisure ?: return null
  val rawCost = TimeAccounting.leisureCostMinutes(leisure.startedAt, endedAt, rules = rules)
  val depleted = rawCost >= reserveMinutes
  val effectiveEndedAt =
    if (depleted) {
      TimeAccounting.instantWhenLeisureCostReaches(leisure.startedAt, reserveMinutes, rules = rules)
    } else {
      endedAt
    }
  val cost = if (depleted) reserveMinutes else rawCost
  val nextReserve = reserveMinutes - cost
  val elapsed =
    (effectiveEndedAt.toKotlinInstant() - leisure.startedAt.toKotlinInstant())
      .inWholeMilliseconds
      .coerceAtLeast(0) / 60_000.0
  val record =
    LeisureRecord(
      id = FocusWellIds.leisure(endedAt),
      startedAt = leisure.startedAt,
      endedAt = effectiveEndedAt,
      elapsedMinutes = elapsed,
      costMinutes = cost,
      dailyDate = TimeAccounting.dailyDate(effectiveEndedAt, rules = rules).toString(),
    )
  val entry =
    LedgerEntry(
      id = FocusWellIds.ledger(record.id),
      title = "Leisure",
      deltaMinutes = -cost,
      createdAt = endedAt,
      sourceId = record.id,
    )
  return EndedLeisureSession(
    state =
      copy(
        reserveMinutes = nextReserve,
        activeMode = if (nextReserve <= 0.0) ActiveMode.Depleted else ActiveMode.None,
        dailyGrantPausedUntilDate = if (nextReserve <= 0.0) TimeAccounting.dailyDate(effectiveEndedAt, rules = rules).toString() else dailyGrantPausedUntilDate,
        leisureRecords = listOf(record) + leisureRecords,
        ledger = listOf(entry) + ledger,
      ),
    reminderSessionId = leisure.reminderSessionId,
  )
}
