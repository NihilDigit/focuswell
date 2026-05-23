package dev.nihildigit.focuswell.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.nihildigit.focuswell.R

const val FOCUSWELL_CHANNEL_ID = "focuswell-reminders"

fun ensureNotificationChannel(context: Context) {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
  val manager = context.getSystemService(NotificationManager::class.java)
  val channel =
    NotificationChannel(
      FOCUSWELL_CHANNEL_ID,
      "FocusWell reminders",
      NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
      description = "Focus, leisure, and reserve reminders"
    }
  manager.createNotificationChannel(channel)
}

fun canPostNotifications(context: Context): Boolean =
  Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
      PackageManager.PERMISSION_GRANTED

fun postFocusWellNotification(
  context: Context,
  id: Int,
  title: String,
  body: String,
) {
  if (!canPostNotifications(context)) {
    Log.w("FocusWellPush", "Notification blocked because POST_NOTIFICATIONS is not granted")
    return
  }
  val notification =
    NotificationCompat.Builder(context, FOCUSWELL_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_monochrome)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setAutoCancel(true)
      .build()
  NotificationManagerCompat.from(context).notify(id, notification)
  Log.i("FocusWellPush", "Posted local notification id=$id")
}
