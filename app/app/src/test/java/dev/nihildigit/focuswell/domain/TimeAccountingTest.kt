package dev.nihildigit.focuswell.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class TimeAccountingTest {
  @Test
  fun dailyDate_beforeFourAm_usesPreviousBusinessDate() {
    val instant = Instant.parse("2026-05-21T19:30:00Z") // 03:30, 2026-05-22 Shanghai

    assertEquals(LocalDate.parse("2026-05-21"), TimeAccounting.dailyDate(instant))
  }

  @Test
  fun dailyDate_afterFourAm_usesCurrentBusinessDate() {
    val instant = Instant.parse("2026-05-21T20:30:00Z") // 04:30, 2026-05-22 Shanghai

    assertEquals(LocalDate.parse("2026-05-22"), TimeAccounting.dailyDate(instant))
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

    assertEquals(60.0, TimeAccounting.leisureCostMinutes(startedAt, endedAt), 0.0001)
  }

  @Test
  fun leisureCostMinutes_returnsToNormalAfterFourAm() {
    val startedAt = Instant.parse("2026-05-21T19:40:00Z") // 03:40 Shanghai
    val endedAt = Instant.parse("2026-05-21T20:20:00Z") // 04:20 Shanghai

    assertEquals(60.0, TimeAccounting.leisureCostMinutes(startedAt, endedAt), 0.0001)
  }

  @Test
  fun instantWhenLeisureCostReaches_splitsAcrossSleepProtectionStart() {
    val startedAt = Instant.parse("2026-05-21T16:40:00Z") // 00:40 Shanghai

    assertEquals(
      Instant.parse("2026-05-21T17:20:00Z"), // 01:20 Shanghai
      TimeAccounting.instantWhenLeisureCostReaches(startedAt, costMinutes = 60.0),
    )
  }

  @Test
  fun instantWhenLeisureCostReaches_usesDoubleRateDuringSleepProtection() {
    val startedAt = Instant.parse("2026-05-21T17:10:00Z") // 01:10 Shanghai

    assertEquals(
      Instant.parse("2026-05-21T17:40:00Z"), // 01:40 Shanghai
      TimeAccounting.instantWhenLeisureCostReaches(startedAt, costMinutes = 60.0),
    )
  }
}
