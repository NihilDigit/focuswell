package dev.nihildigit.focuswell.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

class TimeAccountingTest {
  private val shanghai: ZoneId = ZoneId.of("Asia/Shanghai")

  @Test
  fun dailyDate_afterMidnight_usesCurrentBusinessDate() {
    val instant = Instant.parse("2026-05-21T16:30:00Z") // 00:30, 2026-05-22 Shanghai

    assertEquals(LocalDate.parse("2026-05-22"), TimeAccounting.dailyDate(instant, shanghai))
  }

  @Test
  fun dailyDate_beforeMidnight_usesCurrentBusinessDate() {
    val instant = Instant.parse("2026-05-22T15:30:00Z") // 23:30, 2026-05-22 Shanghai

    assertEquals(LocalDate.parse("2026-05-22"), TimeAccounting.dailyDate(instant, shanghai))
  }

  @Test
  fun dailyDate_exactMidnight_usesNewBusinessDate() {
    val instant = Instant.parse("2026-05-21T16:00:00Z") // 00:00, 2026-05-22 Shanghai

    assertEquals(LocalDate.parse("2026-05-22"), TimeAccounting.dailyDate(instant, shanghai))
  }

  @Test
  fun dailyDate_usesConfiguredDayBoundary() {
    val instant = Instant.parse("2026-05-21T22:30:00Z") // 06:30, 2026-05-22 Shanghai
    val rules = FocusWellRules(dayBoundaryHour = 7)

    assertEquals(LocalDate.parse("2026-05-21"), TimeAccounting.dailyDate(instant, shanghai, rules))
  }

  @Test
  fun dailyDate_defaultZoneUsesSystemTimeZone() {
    val previous = TimeZone.getDefault()
    val instant = Instant.parse("2026-05-22T03:30:00Z")

    try {
      TimeZone.setDefault(TimeZone.getTimeZone("America/New_York")) // 23:30, 2026-05-21 New York
      assertEquals(LocalDate.parse("2026-05-21"), TimeAccounting.dailyDate(instant))

      TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo")) // 12:30, 2026-05-22 Tokyo
      assertEquals(LocalDate.parse("2026-05-22"), TimeAccounting.dailyDate(instant))
    } finally {
      TimeZone.setDefault(previous)
    }
  }

  @Test
  fun focusEarnedMinutes_appliesTypeRateAndTagMultiplier() {
    val earned =
      TimeAccounting.focusEarnedMinutes(
        activeDuration = Duration.ofMinutes(120),
        type = SessionType.Input,
        tagMultiplier = 2.0,
      )

    assertEquals(120.0, earned, 0.0001)
  }

  @Test
  fun focusEarnedMinutes_appliesOutcomeMultiplierAfterAdjustedDuration() {
    val earned =
      TimeAccounting.focusEarnedMinutes(
        activeDurationMinutes = 40.0,
        typeRate = SessionType.Input.rate,
        tagMultiplier = 1.5,
        outcomeMultiplier = focusOutcomeMultiplier("Drifted"),
      )

    assertEquals(9.0, earned, 0.0001)
  }

  @Test
  fun leisureCostMinutes_splitsAtSleepProtectionStart() {
    val startedAt = Instant.parse("2026-05-21T12:40:00Z") // 20:40 Shanghai
    val endedAt = Instant.parse("2026-05-21T13:20:00Z") // 21:20 Shanghai

    assertEquals(60.0, TimeAccounting.leisureCostMinutes(startedAt, endedAt, shanghai), 0.0001)
  }

  @Test
  fun leisureCostMinutes_returnsToNormalAfterFiveAm() {
    val startedAt = Instant.parse("2026-05-21T20:40:00Z") // 04:40 Shanghai
    val endedAt = Instant.parse("2026-05-21T21:20:00Z") // 05:20 Shanghai

    assertEquals(60.0, TimeAccounting.leisureCostMinutes(startedAt, endedAt, shanghai), 0.0001)
  }

  @Test
  fun leisureCostMinutes_usesConfiguredSleepWindowAndMultiplier() {
    val startedAt = Instant.parse("2026-05-21T18:10:00Z") // 02:10 Shanghai
    val endedAt = Instant.parse("2026-05-21T18:30:00Z") // 02:30 Shanghai
    val rules = FocusWellRules(dayBoundaryHour = 6, sleepProtectionStartHour = 2, sleepProtectionEndHour = 3, sleepProtectionMultiplier = 3.0)

    assertEquals(60.0, TimeAccounting.leisureCostMinutes(startedAt, endedAt, shanghai, rules), 0.0001)
  }

  @Test
  fun leisureCostMinutes_defaultSleepWindowIsIndependentOfDayBoundary() {
    val startedAt = Instant.parse("2026-05-21T19:40:00Z") // 03:40 Shanghai
    val endedAt = Instant.parse("2026-05-21T20:20:00Z") // 04:20 Shanghai

    assertEquals(80.0, TimeAccounting.leisureCostMinutes(startedAt, endedAt, shanghai), 0.0001)
  }

  @Test
  fun leisureCostMinutes_defaultZoneUsesSystemSleepWindow() {
    val previous = TimeZone.getDefault()
    val startedAt = Instant.parse("2026-05-22T00:40:00Z") // 20:40 New York
    val endedAt = Instant.parse("2026-05-22T01:20:00Z") // 21:20 New York

    try {
      TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))

      assertEquals(60.0, TimeAccounting.leisureCostMinutes(startedAt, endedAt), 0.0001)
    } finally {
      TimeZone.setDefault(previous)
    }
  }

  @Test
  fun instantWhenLeisureCostReaches_splitsAcrossSleepProtectionStart() {
    val startedAt = Instant.parse("2026-05-21T12:40:00Z") // 20:40 Shanghai

    assertEquals(
      Instant.parse("2026-05-21T13:20:00Z"), // 21:20 Shanghai
      TimeAccounting.instantWhenLeisureCostReaches(startedAt, costMinutes = 60.0, zone = shanghai),
    )
  }

  @Test
  fun instantWhenLeisureCostReaches_usesDoubleRateDuringSleepProtection() {
    val startedAt = Instant.parse("2026-05-21T13:10:00Z") // 21:10 Shanghai

    assertEquals(
      Instant.parse("2026-05-21T13:40:00Z"), // 21:40 Shanghai
      TimeAccounting.instantWhenLeisureCostReaches(startedAt, costMinutes = 60.0, zone = shanghai),
    )
  }
}
