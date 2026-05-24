package dev.nihildigit.focuswell.reminders

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.nihildigit.focuswell.notifications.ensureNotificationChannel
import dev.nihildigit.focuswell.notifications.postFocusWellNotification
import java.time.Instant

class FocusWellMessagingService : FirebaseMessagingService() {
  override fun onMessageReceived(message: RemoteMessage) {
    val data = message.data
    val receivedAt = Instant.now()
    val firedAt = data["firedAtUtc"]?.let { runCatching { Instant.parse(it) }.getOrNull() }
    val dueAt = data["dueAtUtc"]?.let { runCatching { Instant.parse(it) }.getOrNull() }
    Log.i(
      "FocusWellPush",
      "Received FCM reminder tag=${data["tag"] ?: "none"} kind=${data["kind"] ?: "none"} reminderId=${data["reminderId"] ?: "none"} dueAtUtc=${data["dueAtUtc"] ?: "none"} firedAtUtc=${data["firedAtUtc"] ?: "none"} receivedAtUtc=$receivedAt backendToDeviceDelayMs=${firedAt?.let { receivedAt.toEpochMilli() - it.toEpochMilli() } ?: "unknown"} dueToDeviceDelayMs=${dueAt?.let { receivedAt.toEpochMilli() - it.toEpochMilli() } ?: "unknown"}",
    )
    ensureNotificationChannel(this)
    postFocusWellNotification(
      context = this,
      id = data["tag"]?.hashCode() ?: System.currentTimeMillis().toInt(),
      title = data["title"] ?: message.notification?.title ?: "FocusWell",
      body = data["body"] ?: message.notification?.body ?: "Reminder",
    )
    Log.i(
      "FocusWellPush",
      "Posted FCM reminder notification tag=${data["tag"] ?: "none"} kind=${data["kind"] ?: "none"} reminderId=${data["reminderId"] ?: "none"} postedAtUtc=${Instant.now()}",
    )
  }

  override fun onNewToken(token: String) {
    // The next scheduled reminder refreshes registration with the backend.
  }
}
