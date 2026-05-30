package dev.nihildigit.focuswell.usage

import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.LeisureRecord
import dev.nihildigit.focuswell.domain.SessionType
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

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
    val start = Instant.parse("2026-05-20T12:00:00Z").toEpochMilli()

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
    val start = Instant.parse("2026-05-20T12:00:00Z").toEpochMilli()

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
    val start = Instant.parse("2026-05-20T12:00:00Z").toEpochMilli()

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
    val start = Instant.parse("2026-05-20T12:00:00Z").toEpochMilli()

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
  fun clusterPhoneUsageIntervals_usesChargeFreeAppsForClusteringButNotCost() {
    val start = Instant.parse("2026-05-20T12:00:00Z").toEpochMilli()

    val segments =
      clusterPhoneUsageIntervals(
        intervals =
          listOf(
            UsageInterval("app.words", start, start + 2 * 60_000L),
            UsageInterval("app.video", start + 2 * 60_000L, start + 4 * 60_000L),
            UsageInterval("app.words", start + 4 * 60_000L, start + 6 * 60_000L),
          ),
        startedAtMillis = start,
        endedAtMillis = start + 10 * 60_000L,
        chargeFreePackages = setOf("app.words"),
        zone = TimeZone.UTC,
      )

    assertEquals(1, segments.size)
    assertEquals(2.0, segments.first().costMinutes, 0.0001)
    assertEquals(listOf("app.words", "app.video"), segments.first().topApps.map { it.packageName })
  }

  @Test
  fun hasBillablePhoneUsageSegment_ignoresChargeFreeOnlySegments() {
    val start = Instant.parse("2026-05-20T12:00:00Z").toEpochMilli()

    val hasBillableSegment =
      hasBillablePhoneUsageSegment(
        intervals = listOf(UsageInterval("app.words", start, start + 6 * 60_000L)),
        startedAtMillis = start,
        endedAtMillis = start + 10 * 60_000L,
        chargeFreePackages = setOf("app.words"),
        zone = TimeZone.UTC,
      )

    assertEquals(false, hasBillableSegment)
  }

  @Test
  fun hasBillablePhoneUsageSegment_detectsChargeableSegments() {
    val start = Instant.parse("2026-05-20T12:00:00Z").toEpochMilli()

    val hasBillableSegment =
      hasBillablePhoneUsageSegment(
        intervals = listOf(UsageInterval("app.video", start, start + 6 * 60_000L)),
        startedAtMillis = start,
        endedAtMillis = start + 10 * 60_000L,
        chargeFreePackages = setOf("app.words"),
        zone = TimeZone.UTC,
      )

    assertEquals(true, hasBillableSegment)
  }

  @Test
  fun clusterPhoneUsageIntervals_appliesSleepProtectionMultiplierToCost() {
    val start = Instant.parse("2026-05-20T01:00:00Z").toEpochMilli()

    val segments =
      clusterPhoneUsageIntervals(
        intervals = listOf(UsageInterval("app.video", start, start + 5 * 60_000L)),
        startedAtMillis = start,
        endedAtMillis = start + 10 * 60_000L,
        zone = TimeZone.UTC,
      )

    assertEquals(1, segments.size)
    assertEquals(10.0, segments.first().costMinutes, 0.0001)
  }

  @Test
  fun clusterPhoneUsageIntervals_excludesLeisureOverlap() {
    val start = Instant.parse("2026-05-20T12:00:00Z").toEpochMilli()

    val segments =
      clusterPhoneUsageIntervals(
        intervals = listOf(UsageInterval("app.video", start, start + 10 * 60_000L)),
        startedAtMillis = start,
        endedAtMillis = start + 20 * 60_000L,
        excludedIntervals = listOf(start + 3 * 60_000L to start + 8 * 60_000L),
      )

    assertEquals(emptyList<Any>(), segments)
  }

  @Test
  fun clusterPhoneUsageIntervals_excludesFocusOverlap() {
    val start = Instant.parse("2026-05-20T12:00:00Z").toEpochMilli()

    val segments =
      clusterPhoneUsageIntervals(
        intervals = listOf(UsageInterval("app.video", start, start + 10 * 60_000L)),
        startedAtMillis = start,
        endedAtMillis = start + 20 * 60_000L,
        excludedIntervals = listOf(start + 3 * 60_000L to start + 8 * 60_000L),
      )

    assertEquals(emptyList<Any>(), segments)
  }

  @Test
  fun excludedSessionIntervals_includesFocusAndLeisureRecords() {
    val start = Instant.parse("2026-05-20T12:00:00Z")
    val focus = focusRecord(start.plusSeconds(60), start.plusSeconds(120))
    val leisure = leisureRecord(start.plusSeconds(180), start.plusSeconds(240))

    val intervals =
      excludedSessionIntervals(
        startedAt = start,
        endedAt = start.plusSeconds(600),
        focusRecords = listOf(focus),
        leisureRecords = listOf(leisure),
      )

    assertEquals(
      listOf(
        start.plusSeconds(60).toEpochMilli() to start.plusSeconds(120).toEpochMilli(),
        start.plusSeconds(180).toEpochMilli() to start.plusSeconds(240).toEpochMilli(),
      ),
      intervals,
    )
  }

  private fun focusRecord(startedAt: Instant, endedAt: Instant): FocusRecord =
    FocusRecord(
      id = "focus",
      task = "Task",
      result = "As planned",
      type = SessionType.Input,
      tagName = null,
      tagMultiplier = 1.0,
      typeRate = SessionType.Input.rate,
      startedAt = startedAt,
      endedAt = endedAt,
      activeDurationMinutes = 1.0,
      earnedMinutes = 0.5,
      dailyDate = "2026-05-20",
    )

  private fun leisureRecord(startedAt: Instant, endedAt: Instant): LeisureRecord =
    LeisureRecord(
      id = "leisure",
      startedAt = startedAt,
      endedAt = endedAt,
      elapsedMinutes = 1.0,
      costMinutes = 1.0,
      dailyDate = "2026-05-20",
    )
}
