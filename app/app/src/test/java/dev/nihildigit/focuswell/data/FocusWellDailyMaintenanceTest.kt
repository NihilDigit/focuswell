package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.FocusWellUiState
import java.time.Instant
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FocusWellDailyMaintenanceTest {
  private val previousTimeZone = TimeZone.getDefault()

  @Before
  fun setUp() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  }

  @After
  fun tearDown() {
    TimeZone.setDefault(previousTimeZone)
  }

  @Test
  fun settleDailyInterest_compoundsCarriedReserveAcrossBusinessDays() {
    val maintenance = FocusWellDailyMaintenance { Instant.parse("2026-05-22T04:00:00Z") }
    val state =
      FocusWellUiState(
        dailyDate = "2026-05-20",
        reserveMinutes = 100.0,
        rules = FocusWellRules(dayBoundaryHour = 4),
        ledger = listOf(ledger(id = "daily-grant-2026-05-20", delta = 100.0)),
      )

    val updated = maintenance.settleDailyInterest(state)

    assertEquals(
      listOf("daily-interest-2026-05-22", "daily-interest-2026-05-21", "daily-grant-2026-05-20"),
      updated.ledger.map { it.id },
    )
    assertEquals(110.25, updated.reserveMinutes, 0.0001)
  }

  @Test
  fun settleDailyInterest_skipsLockedReserve() {
    val maintenance = FocusWellDailyMaintenance { Instant.parse("2026-05-22T04:00:00Z") }
    val state =
      FocusWellUiState(
        dailyDate = "2026-05-20",
        reserveMinutes = 100.0,
        rules = FocusWellRules(dayBoundaryHour = 4),
        dailyGrantPausedUntilDate = "2026-05-20",
        ledger = listOf(ledger(id = "daily-grant-2026-05-20", delta = 100.0)),
      )

    val updated = maintenance.settleDailyInterest(state)

    assertEquals(listOf("daily-grant-2026-05-20"), updated.ledger.map { it.id })
    assertEquals(100.0, updated.reserveMinutes, 0.0001)
  }

  @Test
  fun ensureDailyGrants_keepsGrantingWhileReserveIsLocked() {
    val maintenance = FocusWellDailyMaintenance { Instant.parse("2026-05-22T04:00:00Z") }
    val state =
      FocusWellUiState(
        dailyDate = "2026-05-20",
        rules = FocusWellRules(dayBoundaryHour = 4, dailyGrantMinutes = 60.0),
        dailyGrantPausedUntilDate = "2026-05-21",
      )

    val updated = maintenance.ensureDailyGrants(state)

    assertEquals(
      listOf(
        "daily-grant-2026-05-22",
        "daily-grant-2026-05-21",
        "daily-grant-2026-05-20",
      ),
      updated.ledger.map { it.id },
    )
    assertEquals(180.0, updated.reserveMinutes, 0.0001)
  }

  private fun ledger(id: String, delta: Double): dev.nihildigit.focuswell.domain.LedgerEntry =
    dev.nihildigit.focuswell.domain.LedgerEntry(
      id = id,
      title = "Test",
      deltaMinutes = delta,
      createdAt = Instant.parse("2026-05-20T04:00:00Z"),
    )
}
