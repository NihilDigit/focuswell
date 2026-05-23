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
  fun dailyDate_beforeFourAm_usesPreviousBusinessDate() {
    val instant = Instant.parse("2026-05-21T19:30:00Z") // 03:30, 2026-05-22 Shanghai

    assertEquals(LocalDate.parse("2026-05-21"), TimeAccounting.dailyDate(instant, shanghai))
  }

  @Test
  fun dailyDate_afterFourAm_usesCurrentBusinessDate() {
    val instant = Instant.parse("2026-05-21T20:30:00Z") // 04:30, 2026-05-22 Shanghai

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
    val instant = Instant.parse("2026-05-22T07:30:00Z")

    try {
      TimeZone.setDefault(TimeZone.getTimeZone("America/New_York")) // 03:30, 2026-05-22 New York
      assertEquals(LocalDate.parse("2026-05-21"), TimeAccounting.dailyDate(instant))

      TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo")) // 16:30, 2026-05-22 Tokyo
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
  fun leisureCostMinutes_splitsAtSleepProtectionStart() {
    val startedAt = Instant.parse("2026-05-21T16:40:00Z") // 00:40 Shanghai
    val endedAt = Instant.parse("2026-05-21T17:20:00Z") // 01:20 Shanghai

    assertEquals(60.0, TimeAccounting.leisureCostMinutes(startedAt, endedAt, shanghai), 0.0001)
  }

  @Test
  fun leisureCostMinutes_returnsToNormalAfterFourAm() {
    val startedAt = Instant.parse("2026-05-21T19:40:00Z") // 03:40 Shanghai
    val endedAt = Instant.parse("2026-05-21T20:20:00Z") // 04:20 Shanghai

    assertEquals(60.0, TimeAccounting.leisureCostMinutes(startedAt, endedAt, shanghai), 0.0001)
  }

  @Test
  fun leisureCostMinutes_usesConfiguredSleepWindowAndMultiplier() {
    val startedAt = Instant.parse("2026-05-21T18:10:00Z") // 02:10 Shanghai
    val endedAt = Instant.parse("2026-05-21T18:30:00Z") // 02:30 Shanghai
    val rules = FocusWellRules(dayBoundaryHour = 6, sleepProtectionStartHour = 2, sleepProtectionMultiplier = 3.0)

    assertEquals(60.0, TimeAccounting.leisureCostMinutes(startedAt, endedAt, shanghai, rules), 0.0001)
  }

  @Test
  fun leisureCostMinutes_defaultZoneUsesSystemSleepWindow() {
    val previous = TimeZone.getDefault()
    val startedAt = Instant.parse("2026-05-22T04:40:00Z") // 00:40 New York
    val endedAt = Instant.parse("2026-05-22T05:20:00Z") // 01:20 New York

    try {
      TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))

      assertEquals(60.0, TimeAccounting.leisureCostMinutes(startedAt, endedAt), 0.0001)
    } finally {
      TimeZone.setDefault(previous)
    }
  }

  @Test
  fun instantWhenLeisureCostReaches_splitsAcrossSleepProtectionStart() {
    val startedAt = Instant.parse("2026-05-21T16:40:00Z") // 00:40 Shanghai

    assertEquals(
      Instant.parse("2026-05-21T17:20:00Z"), // 01:20 Shanghai
      TimeAccounting.instantWhenLeisureCostReaches(startedAt, costMinutes = 60.0, zone = shanghai),
    )
  }

  @Test
  fun instantWhenLeisureCostReaches_usesDoubleRateDuringSleepProtection() {
    val startedAt = Instant.parse("2026-05-21T17:10:00Z") // 01:10 Shanghai

    assertEquals(
      Instant.parse("2026-05-21T17:40:00Z"), // 01:40 Shanghai
      TimeAccounting.instantWhenLeisureCostReaches(startedAt, costMinutes = 60.0, zone = shanghai),
    )
  }
}
