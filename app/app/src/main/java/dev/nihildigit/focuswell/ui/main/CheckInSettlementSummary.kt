package dev.nihildigit.focuswell.ui.main

import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.PhoneUsageSegment

internal data class CheckInSettlementSummary(
  val phoneCost: Double,
  val available: Double,
  val deducted: Double,
  val remaining: Double,
  val exceeded: Boolean,
  val fairUseCount: Int,
  val reviewedSegmentCount: Int,
)

internal fun checkInSettlementSummary(
  segments: List<PhoneUsageSegment>,
  fairUseIds: Set<String>,
  availableMinutes: Double,
): CheckInSettlementSummary {
  val phoneCost = segments.filterNot { it.id in fairUseIds }.sumOf { it.costMinutes }
  val available = availableMinutes.coerceAtLeast(0.0)
  val deducted = minOf(phoneCost, available)
  return CheckInSettlementSummary(
    phoneCost = phoneCost,
    available = available,
    deducted = deducted,
    remaining = (available - deducted).coerceAtLeast(0.0),
    exceeded = phoneCost > available,
    fairUseCount = fairUseIds.size,
    reviewedSegmentCount = segments.size,
  )
}

internal fun frozenDailyGrantLabel(rules: FocusWellRules): String =
  "2h focus · ${compactMinutes(rules.normalized().dailyGrantMinutes)} keeps saving"
