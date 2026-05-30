package dev.nihildigit.focuswell.ui.main

import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.reminders.PushRegistrationStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PushRegistrationUiStateTest {
  @Test
  fun activeReminderSessionId_returnsFocusOrLeisureSessionIdOnly() {
    assertEquals(
      "focus-session",
      activeReminderSessionId(
        ActiveMode.Focus(
          task = "Read",
          type = SessionType.Input,
          tag = null,
          startedAt = Instant.parse("2026-05-20T05:00:00Z"),
          reminderSessionId = "focus-session",
        ),
      ),
    )
    assertEquals(
      "leisure-session",
      activeReminderSessionId(
        ActiveMode.Leisure(
          startedAt = Instant.parse("2026-05-20T06:00:00Z"),
          reminderSessionId = "leisure-session",
        ),
      ),
    )
    assertNull(activeReminderSessionId(ActiveMode.None))
  }

  @Test
  fun refreshFailed_preservesStatusAndStoresError() {
    val state =
      PushRegistrationUiState(
        status = PushRegistrationStatus(deviceId = "device", hasFcmToken = true),
        refreshing = true,
      ).refreshFailed(IllegalStateException("FCM unavailable"))

    assertFalse(state.refreshing)
    assertEquals("device", state.status.deviceId)
    assertEquals("FCM unavailable", state.status.lastError)
  }

  @Test
  fun pushDisableFailed_marksPushDisabledEvenWhenBackendFails() {
    val state =
      pushDisableFailed(
        cachedStatus = PushRegistrationStatus(deviceId = "device", enabled = true, hasFcmToken = true),
        error = IllegalStateException("Backend unavailable"),
      )

    assertFalse(state.status.enabled)
    assertFalse(state.status.hasFcmToken)
    assertEquals("Backend unavailable", state.status.lastError)
  }
}
