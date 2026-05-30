package dev.nihildigit.focuswell.ui.main

import dev.nihildigit.focuswell.domain.PhoneUsageSegment
import java.time.Instant

data class MorningCheckInUiState(
  val dailyDate: String? = null,
  val startedAt: Instant? = null,
  val settledUntil: Instant? = null,
  val loading: Boolean = false,
  val segments: List<PhoneUsageSegment> = emptyList(),
)

internal fun billablePhoneCostMinutes(
  segments: List<PhoneUsageSegment>,
  fairUseSegmentIds: Set<String>,
): Double =
  segments
    .filterNot { it.id in fairUseSegmentIds }
    .sumOf { it.costMinutes }
