package dev.nihildigit.focuswell.reminders

import android.content.Context
import java.util.UUID

data class DeviceIdentity(
  val deviceId: String,
  val installSecret: String,
)

class ReminderClient(context: Context) {
  private val prefs = context.getSharedPreferences("focuswell-reminders", Context.MODE_PRIVATE)

  val identity: DeviceIdentity
    get() {
      val existingDeviceId = prefs.getString(KEY_DEVICE_ID, null)
      val existingSecret = prefs.getString(KEY_INSTALL_SECRET, null)
      if (existingDeviceId != null && existingSecret != null) {
        return DeviceIdentity(existingDeviceId, existingSecret)
      }
      val next = DeviceIdentity(UUID.randomUUID().toString(), UUID.randomUUID().toString())
      prefs
        .edit()
        .putString(KEY_DEVICE_ID, next.deviceId)
        .putString(KEY_INSTALL_SECRET, next.installSecret)
        .apply()
      return next
    }

  fun rotateIdentity(): DeviceIdentity {
    val next = DeviceIdentity(UUID.randomUUID().toString(), UUID.randomUUID().toString())
    prefs
      .edit()
      .putString(KEY_DEVICE_ID, next.deviceId)
      .putString(KEY_INSTALL_SECRET, next.installSecret)
      .apply()
    return next
  }

  suspend fun scheduleFocusStaleReminder(sessionId: String, revision: Int) {
    // Backend integration point: send device identity, sessionId, revision,
    // and focus_stale_3h dueAtUtc. Timer truth stays local.
  }

  suspend fun scheduleLeisureReminders(sessionId: String, revision: Int) {
    // Backend integration point: send low-balance, depleted, and sleep-protection
    // reminders. Local notifications already cover the foreground case.
  }

  suspend fun cancelSession(sessionId: String) {
    // Backend integration point: cancel pending reminders by rotating revision
    // or cancelling the active session plan.
  }

  private companion object {
    const val KEY_DEVICE_ID = "deviceId"
    const val KEY_INSTALL_SECRET = "installSecret"
  }
}
