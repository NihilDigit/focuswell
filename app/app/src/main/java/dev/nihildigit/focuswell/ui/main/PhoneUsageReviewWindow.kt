package dev.nihildigit.focuswell.ui.main

import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.TimeAccounting
import java.time.Instant

internal data class PhoneUsageReviewWindow(
  val startedAt: Instant,
  val endedAt: Instant,
  val settledUntil: Instant,
)

internal fun morningCheckInUsageWindow(
  state: FocusWellUiState,
  dailyDate: String = state.dailyDate,
): PhoneUsageReviewWindow {
  val rules = state.rules.normalized()
  val effectiveDailyDate =
    dailyDate.takeIf { it.isNotBlank() }
      ?: TimeAccounting.dailyDate(Instant.now(), rules = rules).toString()
  val boundaryEnd = TimeAccounting.businessDayBoundaryInstant(effectiveDailyDate, rules = rules)
  val boundaryStart = TimeAccounting.businessDayBoundaryInstant(effectiveDailyDate, dayOffset = -1, rules = rules)
  val startedAt =
    state.lastPhoneUsageSettlementAt
      ?.takeIf { it.isAfter(boundaryStart) }
      ?: boundaryStart
  return PhoneUsageReviewWindow(
    startedAt = startedAt,
    endedAt = boundaryEnd,
    settledUntil = boundaryEnd,
  )
}

internal fun phoneUsageSettlementWindow(
  state: FocusWellUiState,
  startedAt: Instant,
): PhoneUsageReviewWindow {
  val rules = state.rules.normalized()
  val effectiveDailyDate =
    state.dailyDate.takeIf { it.isNotBlank() }
      ?: TimeAccounting.dailyDate(startedAt, rules = rules).toString()
  val dayStart = TimeAccounting.businessDayBoundaryInstant(effectiveDailyDate, rules = rules)
  val queryStartedAt =
    state.lastPhoneUsageSettlementAt
      ?.takeIf { it.isAfter(dayStart) }
      ?: dayStart
  return PhoneUsageReviewWindow(
    startedAt = queryStartedAt,
    endedAt = startedAt,
    settledUntil = startedAt,
  )
}
