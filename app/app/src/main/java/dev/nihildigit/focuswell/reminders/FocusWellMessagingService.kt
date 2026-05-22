package dev.nihildigit.focuswell.reminders

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.nihildigit.focuswell.notifications.ensureNotificationChannel
import dev.nihildigit.focuswell.notifications.postFocusWellNotification

class FocusWellMessagingService : FirebaseMessagingService() {
  override fun onMessageReceived(message: RemoteMessage) {
    val notification = message.notification
    ensureNotificationChannel(this)
    postFocusWellNotification(
      context = this,
      id = message.data["tag"]?.hashCode() ?: System.currentTimeMillis().toInt(),
      title = notification?.title ?: "FocusWell",
      body = notification?.body ?: "Reminder",
    )
  }

  override fun onNewToken(token: String) {
    // The next scheduled reminder refreshes registration with the backend.
  }
}
