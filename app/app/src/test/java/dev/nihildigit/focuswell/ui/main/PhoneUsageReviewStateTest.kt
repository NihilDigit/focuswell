package dev.nihildigit.focuswell.ui.main

import dev.nihildigit.focuswell.domain.PhoneUsageSegment
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneUsageReviewStateTest {
  @Test
  fun billablePhoneCostMinutes_excludesFairUseSegments() {
    val segments =
      listOf(
        segment(id = "counted", costMinutes = 12.5),
        segment(id = "fair", costMinutes = 30.0),
      )

    assertEquals(12.5, billablePhoneCostMinutes(segments, fairUseSegmentIds = setOf("fair")), 0.0001)
  }

  private fun segment(
    id: String,
    costMinutes: Double,
  ): PhoneUsageSegment =
    PhoneUsageSegment(
      id = id,
      startedAt = Instant.parse("2026-05-20T00:00:00Z"),
      endedAt = Instant.parse("2026-05-20T00:30:00Z"),
      costMinutes = costMinutes,
      topApps = emptyList(),
    )
}
