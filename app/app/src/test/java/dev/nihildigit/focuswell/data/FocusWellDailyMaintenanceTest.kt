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
  fun ensureDailyGrants_writesPausedGrantEntriesThroughPausedUntilDate() {
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
        "daily-grant-paused-2026-05-21",
        "daily-grant-paused-2026-05-20",
      ),
      updated.ledger.map { it.id },
    )
    assertEquals(60.0, updated.reserveMinutes, 0.0001)
  }
}
