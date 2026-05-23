package dev.nihildigit.focuswell.reminders

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.nihildigit.focuswell.notifications.ensureNotificationChannel
import dev.nihildigit.focuswell.notifications.postFocusWellNotification

class FocusWellMessagingService : FirebaseMessagingService() {
  override fun onMessageReceived(message: RemoteMessage) {
    val data = message.data
    Log.i("FocusWellPush", "Received FCM reminder tag=${data["tag"] ?: "none"}")
    ensureNotificationChannel(this)
    postFocusWellNotification(
      context = this,
      id = data["tag"]?.hashCode() ?: System.currentTimeMillis().toInt(),
      title = data["title"] ?: message.notification?.title ?: "FocusWell",
      body = data["body"] ?: message.notification?.body ?: "Reminder",
    )
  }

  override fun onNewToken(token: String) {
    // The next scheduled reminder refreshes registration with the backend.
  }
}
