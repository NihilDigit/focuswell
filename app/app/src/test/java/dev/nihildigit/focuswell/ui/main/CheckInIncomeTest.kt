package dev.nihildigit.focuswell.ui.main

import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.TimeAccounting
import java.time.Instant
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CheckInIncomeTest {
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
  fun checkInIncomeItems_includesTodayGrantPreviousTrackerRewardsAndWakeBonus() {
    val state =
      FocusWellUiState(
        dailyDate = "2026-05-20",
        rules = FocusWellRules(wakeTargetHour = 9),
        ledger =
          listOf(
            ledger(id = "daily-grant-2026-05-20", title = "Daily grant", delta = 60.0),
            ledger(id = "tracker-reward-2026-05-19-reading", title = "Daily tracker", delta = 15.0, note = "Reading"),
            ledger(id = "tracker-reward-2026-05-18-old", title = "Daily tracker", delta = 10.0, note = "Old"),
            ledger(id = "daily-grant-2026-05-19", title = "Daily grant", delta = 60.0),
          ),
      )

    val items = checkInIncomeItems(state, Instant.parse("2026-05-20T08:30:00Z"))

    assertEquals(
      listOf(
        CheckInIncomeItem("Daily grant", 60.0),
        CheckInIncomeItem("Reading", 15.0),
        CheckInIncomeItem("Wake bonus", 30.0),
      ),
      items,
    )
  }

  @Test
  fun wakeBonusEligible_usesConfiguredWakeWindow() {
    val rules = FocusWellRules(wakeTargetHour = 9)

    assertTrue(TimeAccounting.isWakeBonusEligible(Instant.parse("2026-05-20T08:00:00Z"), rules = rules))
    assertTrue(TimeAccounting.isWakeBonusEligible(Instant.parse("2026-05-20T09:30:00Z"), rules = rules))
    assertFalse(TimeAccounting.isWakeBonusEligible(Instant.parse("2026-05-20T07:59:00Z"), rules = rules))
    assertFalse(TimeAccounting.isWakeBonusEligible(Instant.parse("2026-05-20T09:31:00Z"), rules = rules))
  }

  private fun ledger(
    id: String,
    title: String,
    delta: Double,
    note: String? = null,
  ): LedgerEntry =
    LedgerEntry(
      id = id,
      title = title,
      deltaMinutes = delta,
      createdAt = Instant.parse("2026-05-20T05:00:00Z"),
      note = note,
    )
}
