package dev.nihildigit.focuswell.ui.main

import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.PhoneUsageSegment
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInSettlementSummaryTest {
  @Test
  fun checkInSettlementSummary_excludesFairUseSegmentsAndCapsDeduction() {
    val summary =
      checkInSettlementSummary(
        segments =
          listOf(
            segment(id = "count", costMinutes = 30.0),
            segment(id = "fair", costMinutes = 20.0),
          ),
        fairUseIds = setOf("fair"),
        availableMinutes = 10.0,
      )

    assertEquals(30.0, summary.phoneCost, 0.0001)
    assertEquals(10.0, summary.deducted, 0.0001)
    assertEquals(0.0, summary.remaining, 0.0001)
    assertEquals(1, summary.fairUseCount)
    assertEquals(2, summary.reviewedSegmentCount)
    assertTrue(summary.exceeded)
  }

  @Test
  fun checkInSettlementSummary_keepsRemainingReserveWhenAvailableCoversCost() {
    val summary =
      checkInSettlementSummary(
        segments = listOf(segment(id = "count", costMinutes = 12.0)),
        fairUseIds = emptySet(),
        availableMinutes = 20.0,
      )

    assertEquals(12.0, summary.deducted, 0.0001)
    assertEquals(8.0, summary.remaining, 0.0001)
    assertFalse(summary.exceeded)
  }

  @Test
  fun frozenDailyGrantLabel_pointsToFocusRestartAndStoredDailyGrant() {
    assertEquals("2h focus · 45m keeps saving", frozenDailyGrantLabel(FocusWellRules(dailyGrantMinutes = 45.0)))
  }

  private fun segment(id: String, costMinutes: Double): PhoneUsageSegment =
    PhoneUsageSegment(
      id = id,
      startedAt = Instant.parse("2026-05-20T05:00:00Z"),
      endedAt = Instant.parse("2026-05-20T05:10:00Z"),
      costMinutes = costMinutes,
      topApps = emptyList(),
    )
}
