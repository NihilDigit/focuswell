package dev.nihildigit.focuswell.ui.main

import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.FocusWellUiState
import java.time.Instant
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PhoneUsageReviewWindowTest {
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
  fun morningCheckInUsageWindow_usesPreviousBusinessBoundaryWhenNoSettlementCursor() {
    val state =
      FocusWellUiState(
        dailyDate = "2026-05-20",
        rules = FocusWellRules(dayBoundaryHour = 4),
      )

    val window = morningCheckInUsageWindow(state)

    assertEquals(Instant.parse("2026-05-19T04:00:00Z"), window.startedAt)
    assertEquals(Instant.parse("2026-05-20T04:00:00Z"), window.endedAt)
    assertEquals(window.endedAt, window.settledUntil)
  }

  @Test
  fun morningCheckInUsageWindow_startsFromSettlementCursorInsideBusinessDay() {
    val state =
      FocusWellUiState(
        dailyDate = "2026-05-20",
        rules = FocusWellRules(dayBoundaryHour = 4),
        lastPhoneUsageSettlementAt = Instant.parse("2026-05-19T12:30:00Z"),
      )

    val window = morningCheckInUsageWindow(state)

    assertEquals(Instant.parse("2026-05-19T12:30:00Z"), window.startedAt)
    assertEquals(Instant.parse("2026-05-20T04:00:00Z"), window.endedAt)
  }

  @Test
  fun phoneUsageSettlementWindow_usesTodayBoundaryOrCursor() {
    val state =
      FocusWellUiState(
        dailyDate = "2026-05-20",
        rules = FocusWellRules(dayBoundaryHour = 4),
        lastPhoneUsageSettlementAt = Instant.parse("2026-05-20T09:00:00Z"),
      )
    val startedAt = Instant.parse("2026-05-20T12:00:00Z")

    val window = phoneUsageSettlementWindow(state, startedAt)

    assertEquals(Instant.parse("2026-05-20T09:00:00Z"), window.startedAt)
    assertEquals(startedAt, window.endedAt)
    assertEquals(startedAt, window.settledUntil)
  }
}
