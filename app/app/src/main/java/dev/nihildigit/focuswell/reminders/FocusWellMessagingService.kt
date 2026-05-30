package dev.nihildigit.focuswell.reminders

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.nihildigit.focuswell.data.db.FocusWellDatabase
import dev.nihildigit.focuswell.notifications.ensureNotificationChannel
import dev.nihildigit.focuswell.notifications.postFocusWellNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant

class FocusWellMessagingService : FirebaseMessagingService() {
  private val cancellationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onMessageReceived(message: RemoteMessage) {
    handleMessage(message)
  }

  private fun handleMessage(message: RemoteMessage) {
    val data = message.data
    val receivedAt = Instant.now()
    val firedAt = data["firedAtUtc"]?.let { runCatching { Instant.parse(it) }.getOrNull() }
    val dueAt = data["dueAtUtc"]?.let { runCatching { Instant.parse(it) }.getOrNull() }
    val kind = data["kind"]
    val reminderId = data["reminderId"]
    val sessionId = data["sessionId"] ?: sessionIdFromReminderId(reminderId = reminderId, kind = kind)
    val revision = data["revision"]?.toIntOrNull() ?: revisionFromReminderId(reminderId = reminderId, kind = kind)
    Log.i(
      "FocusWellPush",
      "Received FCM reminder tag=${data["tag"] ?: "none"} kind=${kind ?: "none"} reminderId=${reminderId ?: "none"} sessionId=${sessionId ?: "none"} revision=${revision ?: "none"} dueAtUtc=${data["dueAtUtc"] ?: "none"} firedAtUtc=${data["firedAtUtc"] ?: "none"} receivedAtUtc=$receivedAt backendToDeviceDelayMs=${firedAt?.let { receivedAt.toEpochMilli() - it.toEpochMilli() } ?: "unknown"} dueToDeviceDelayMs=${dueAt?.let { receivedAt.toEpochMilli() - it.toEpochMilli() } ?: "unknown"}",
    )
    if (!matchesActiveSession(kind = kind, sessionId = sessionId, revision = revision)) {
      Log.i(
        "FocusWellPush",
        "Dropped stale FCM reminder kind=${kind ?: "none"} reminderId=${reminderId ?: "none"} sessionId=${sessionId ?: "none"} revision=${revision ?: "none"}",
      )
      cancelStaleSession(sessionId = sessionId, kind = kind, reminderId = reminderId)
      return
    }
    ensureNotificationChannel(this)
    postFocusWellNotification(
      context = this,
      id = reminderNotificationId(data),
      title = data["title"] ?: message.notification?.title ?: "FocusWell",
      body = data["body"] ?: message.notification?.body ?: "Reminder",
    )
    Log.i(
      "FocusWellPush",
      "Posted FCM reminder notification tag=${data["tag"] ?: "none"} kind=${kind ?: "none"} reminderId=${data["reminderId"] ?: "none"} postedAtUtc=${Instant.now()}",
    )
  }

  override fun onNewToken(token: String) {
    // The next scheduled reminder refreshes registration with the backend.
  }

  private fun matchesActiveSession(kind: String?, sessionId: String?, revision: Int?): Boolean {
    if (kind == null || sessionId == null || revision == null) return false
    val active = activeReminderSession(this) ?: return false
    return when {
      kind.startsWith("focus_") ->
        active.kind == "focus" &&
          active.sessionId == sessionId &&
          active.revision == revision
      kind.startsWith("leisure_") || kind == "late_night_rate_started" ->
        active.kind == "leisure" &&
          active.sessionId == sessionId &&
          active.revision == revision
      else -> false
    }
  }

  private fun cancelStaleSession(sessionId: String?, kind: String?, reminderId: String?) {
    if (sessionId == null) return
    cancellationScope.launch {
      runCatching { ReminderClient(this@FocusWellMessagingService).cancelSession(sessionId) }
        .onSuccess {
          Log.i(
            "FocusWellPush",
            "Cancelled stale remote reminder session kind=${kind ?: "none"} reminderId=${reminderId ?: "none"} sessionId=$sessionId",
          )
          ensureNotificationChannel(this@FocusWellMessagingService)
          postFocusWellNotification(
            context = this@FocusWellMessagingService,
            id = "stale-$sessionId".hashCode(),
            title = "Stale reminder cleared",
            body = "A cloud reminder no longer matched your current timer.",
          )
        }
        .onFailure { error ->
          Log.e(
            "FocusWellPush",
            "Failed to cancel stale remote reminder session kind=${kind ?: "none"} reminderId=${reminderId ?: "none"} sessionId=$sessionId",
            error,
          )
        }
    }
  }

  private fun sessionIdFromReminderId(reminderId: String?, kind: String?): String? =
    reminderParts(reminderId = reminderId, kind = kind)?.first

  private fun revisionFromReminderId(reminderId: String?, kind: String?): Int? =
    reminderParts(reminderId = reminderId, kind = kind)?.second

  private fun reminderParts(reminderId: String?, kind: String?): Pair<String, Int>? {
    if (reminderId == null || kind == null) return null
    val suffix = "-$kind"
    if (!reminderId.endsWith(suffix)) return null
    val beforeKind = reminderId.removeSuffix(suffix)
    val revisionSeparator = beforeKind.lastIndexOf('-')
    if (revisionSeparator <= 0) return null
    val sessionId = beforeKind.substring(0, revisionSeparator)
    val revision = beforeKind.substring(revisionSeparator + 1).toIntOrNull() ?: return null
    return sessionId to revision
  }
}

internal fun reminderNotificationId(data: Map<String, String>): Int {
  data["tag"]?.let { return it.hashCode() }
  val stableKey =
    listOfNotNull(
      data["reminderId"],
      data["sessionId"],
      data["kind"],
      data["dueAtUtc"],
    ).joinToString(separator = "|")
  return stableKey.ifBlank { "focuswell-reminder" }.hashCode()
}

private data class ActiveReminderSession(
  val kind: String,
  val sessionId: String,
  val revision: Int,
)

private fun activeReminderSession(context: Context): ActiveReminderSession? {
  val cursor =
    FocusWellDatabase.get(context)
      .openHelper
      .readableDatabase
      .query("SELECT activeKind, activeReminderSessionId, activeRevision FROM app_state LIMIT 1")
  cursor.use {
    if (!it.moveToFirst()) return null
    val kind = it.getString(0) ?: return null
    val sessionId = it.getString(1) ?: return null
    val revision = it.getInt(2)
    return ActiveReminderSession(kind = kind, sessionId = sessionId, revision = revision)
  }
}
