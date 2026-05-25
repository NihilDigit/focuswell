package dev.nihildigit.focuswell.usage

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class FocusUsageStatsTest {
  @Test
  fun fallbackAppName_usesReadableLastPackageSegment() {
    assertEquals("youtube", "com.google.android.youtube".fallbackAppName())
    assertEquals("reader app", "org.example.reader_app".fallbackAppName())
  }

  @Test
  fun fallbackAppName_keepsPackageWhenNoReadableSegmentExists() {
    assertEquals("com.", "com.".fallbackAppName())
  }

  @Test
  fun clusterPhoneUsageIntervals_requiresFiveOccupiedMinutes() {
    val start = Instant.parse("2026-05-20T04:00:00Z").toEpochMilli()

    val segments =
      clusterPhoneUsageIntervals(
        intervals = listOf(UsageInterval("app.video", start, start + 4 * 60_000L + 49_000L)),
        startedAtMillis = start,
        endedAtMillis = start + 10 * 60_000L,
      )

    assertEquals(emptyList<Any>(), segments)
  }

  @Test
  fun clusterPhoneUsageIntervals_mergesOneMinuteGap() {
    val start = Instant.parse("2026-05-20T04:00:00Z").toEpochMilli()

    val segments =
      clusterPhoneUsageIntervals(
        intervals =
          listOf(
            UsageInterval("app.video", start, start + 5 * 60_000L),
            UsageInterval("app.chat", start + 6 * 60_000L, start + 11 * 60_000L),
          ),
        startedAtMillis = start,
        endedAtMillis = start + 20 * 60_000L,
      )

    assertEquals(1, segments.size)
    assertEquals(10.0, segments.first().costMinutes, 0.0001)
    assertEquals(listOf("app.video", "app.chat"), segments.first().topApps.map { it.packageName })
  }

  @Test
  fun clusterPhoneUsageIntervals_keepsChronologicalSlices() {
    val start = Instant.parse("2026-05-20T04:00:00Z").toEpochMilli()

    val segments =
      clusterPhoneUsageIntervals(
        intervals =
          listOf(
            UsageInterval("app.video", start, start + 3 * 60_000L),
            UsageInterval("app.chat", start + 3 * 60_000L, start + 6 * 60_000L),
          ),
        startedAtMillis = start,
        endedAtMillis = start + 10 * 60_000L,
      )

    assertEquals(listOf("app.video", "app.chat"), segments.first().slices.map { it.packageName })
    assertEquals(start + 3 * 60_000L, segments.first().slices[1].startedAt.toEpochMilli())
  }

  @Test
  fun clusterPhoneUsageIntervals_chargesActualScreenOnTimeWithinOccupiedMinutes() {
    val start = Instant.parse("2026-05-20T04:00:00Z").toEpochMilli()

    val segments =
      clusterPhoneUsageIntervals(
        intervals =
          listOf(
            UsageInterval("app.video", start, start + 50_000L),
            UsageInterval("app.video", start + 60_000L, start + 110_000L),
            UsageInterval("app.video", start + 120_000L, start + 170_000L),
            UsageInterval("app.video", start + 180_000L, start + 230_000L),
            UsageInterval("app.video", start + 240_000L, start + 290_000L),
          ),
        startedAtMillis = start,
        endedAtMillis = start + 10 * 60_000L,
      )

    assertEquals(1, segments.size)
    assertEquals(250_000.0 / 60_000.0, segments.first().costMinutes, 0.0001)
  }

  @Test
  fun clusterPhoneUsageIntervals_appliesSleepProtectionMultiplierToCost() {
    val start = Instant.parse("2026-05-20T01:00:00Z").toEpochMilli()

    val segments =
      clusterPhoneUsageIntervals(
        intervals = listOf(UsageInterval("app.video", start, start + 5 * 60_000L)),
        startedAtMillis = start,
        endedAtMillis = start + 10 * 60_000L,
        zone = ZoneOffset.UTC,
      )

    assertEquals(1, segments.size)
    assertEquals(10.0, segments.first().costMinutes, 0.0001)
  }

  @Test
  fun clusterPhoneUsageIntervals_excludesLeisureOverlap() {
    val start = Instant.parse("2026-05-20T04:00:00Z").toEpochMilli()

    val segments =
      clusterPhoneUsageIntervals(
        intervals = listOf(UsageInterval("app.video", start, start + 10 * 60_000L)),
        startedAtMillis = start,
        endedAtMillis = start + 20 * 60_000L,
        excludedIntervals = listOf(start + 3 * 60_000L to start + 8 * 60_000L),
      )

    assertEquals(emptyList<Any>(), segments)
  }
}
