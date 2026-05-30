package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.SessionType
import java.time.Instant
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FocusWellSessionMutationsTest {
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
  fun withEndedFocusSession_excludesExistingAndCurrentPauseBeforeCorrection() {
    val state =
      FocusWellUiState(
        activeMode =
          ActiveMode.Focus(
            task = "Math",
            type = SessionType.Input,
            tag = null,
            startedAt = Instant.parse("2026-05-20T05:00:00Z"),
            paused = true,
            pausedAt = Instant.parse("2026-05-20T05:20:00Z"),
            pausedDurationMillis = 10 * 60_000L,
            reminderSessionId = "focus-session",
          )
      )

    val ended =
      state.withEndedFocusSession(
        endedAt = Instant.parse("2026-05-20T06:00:00Z"),
        result = "As planned",
        correctionMinutes = 5.0,
      )

    requireNotNull(ended)
    assertEquals("focus-session", ended.reminderSessionId)
    assertEquals(5.0, ended.state.focusRecords.first().activeDurationMinutes, 0.0001)
    assertEquals(2.5, ended.state.focusRecords.first().earnedMinutes, 0.0001)
    assertEquals(2.5, ended.state.ledger.first().deltaMinutes, 0.0001)
    assertEquals(ActiveMode.None, ended.state.activeMode)
  }

  @Test
  fun withEndedLeisureSession_capsCostAndEndsAtReserveExhaustion() {
    val state =
      FocusWellUiState(
        reserveMinutes = 10.0,
        activeMode =
          ActiveMode.Leisure(
            startedAt = Instant.parse("2026-05-20T12:00:00Z"),
            reminderSessionId = "leisure-session",
          ),
        ledger = listOf(LedgerEntry(id = "daily-grant", title = "Daily grant", deltaMinutes = 10.0, createdAt = Instant.parse("2026-05-20T04:00:00Z"))),
      )

    val ended = state.withEndedLeisureSession(Instant.parse("2026-05-20T12:20:00Z"))

    requireNotNull(ended)
    assertEquals("leisure-session", ended.reminderSessionId)
    assertEquals(0.0, ended.state.reserveMinutes, 0.0001)
    assertTrue(ended.state.activeMode is ActiveMode.Depleted)
    assertEquals(10.0, ended.state.leisureRecords.first().costMinutes, 0.0001)
    assertEquals(Instant.parse("2026-05-20T12:10:00Z"), ended.state.leisureRecords.first().endedAt)
  }
}
