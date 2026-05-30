package dev.nihildigit.focuswell.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FocusWellMessagingServiceTest {
  @Test
  fun reminderNotificationId_prefersRemoteTag() {
    assertEquals(
      "focus-123".hashCode(),
      reminderNotificationId(
        mapOf(
          "tag" to "focus-123",
          "reminderId" to "ignored",
        )
      ),
    )
  }

  @Test
  fun reminderNotificationId_usesStableReminderFieldsWhenTagIsMissing() {
    val first =
      reminderNotificationId(
        mapOf(
          "reminderId" to "focus-123-1-focus_duration_1h",
          "sessionId" to "focus-123",
          "kind" to "focus_duration_1h",
          "dueAtUtc" to "2026-05-20T06:00:00Z",
        )
      )
    val second =
      reminderNotificationId(
        mapOf(
          "reminderId" to "focus-123-1-focus_duration_1h",
          "sessionId" to "focus-123",
          "kind" to "focus_duration_1h",
          "dueAtUtc" to "2026-05-20T06:00:00Z",
        )
      )

    assertEquals(first, second)
    assertNotEquals("focuswell-reminder".hashCode(), first)
  }
}
