package dev.nihildigit.focuswell.reminders

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import dev.nihildigit.focuswell.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

data class DeviceIdentity(
  val deviceId: String,
  val installSecret: String,
)

class ReminderClient(context: Context) {
  private val appContext = context.applicationContext
  private val prefs = context.getSharedPreferences("focuswell-reminders", Context.MODE_PRIVATE)
  private val backendUrl = BuildConfig.FOCUSWELL_BACKEND_URL.trimEnd('/')

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
    ensureRegistered()
    schedule(
      sessionId = sessionId,
      revision = revision,
      reminders =
        listOf(
          reminder("focus_stale_3h", Instant.now().plus(Duration.ofHours(3))),
        ),
    )
  }

  suspend fun scheduleLeisureReminders(sessionId: String, revision: Int, reserveMinutes: Double) {
    ensureRegistered()
    val now = Instant.now()
    val reminders =
      buildList {
        if (reserveMinutes > 10.0) add(reminder("leisure_10m_left", now.plus(minutes(reserveMinutes - 10.0))))
        if (reserveMinutes > 5.0) add(reminder("leisure_5m_left", now.plus(minutes(reserveMinutes - 5.0))))
        if (reserveMinutes > 1.0) add(reminder("leisure_1m_left", now.plus(minutes(reserveMinutes - 1.0))))
        if (reserveMinutes > 0.0) add(reminder("leisure_depleted", now.plus(minutes(reserveMinutes))))
        nextLateNightInstant(now)?.let { add(reminder("late_night_rate_started", it)) }
      }
    schedule(sessionId = sessionId, revision = revision, reminders = reminders)
  }

  suspend fun cancelSession(sessionId: String) {
    val identity = identity
    post(
      path = "/api/reminders/cancel",
      body =
        JSONObject()
          .put("deviceId", identity.deviceId)
          .put("installSecret", identity.installSecret)
          .put("sessionId", sessionId),
    )
  }

  suspend fun refreshFcmRegistration() {
    ensureRegistered(forceTokenRefresh = true)
  }

  private suspend fun ensureRegistered(forceTokenRefresh: Boolean = false) {
    val identity = identity
    val token = fcmToken(forceTokenRefresh)
    post(
      path = "/api/devices/register",
      body =
        JSONObject()
          .put("deviceId", identity.deviceId)
          .put("installSecretHash", sha256Hex(identity.installSecret))
          .put("nowUtc", Instant.now().toString())
          .apply {
            if (token != null) put("fcmToken", token)
          },
    )
  }

  private suspend fun schedule(
    sessionId: String,
    revision: Int,
    reminders: List<JSONObject>,
  ) {
    if (reminders.isEmpty()) return
    val identity = identity
    post(
      path = "/api/reminders/schedule",
      body =
        JSONObject()
          .put("deviceId", identity.deviceId)
          .put("installSecret", identity.installSecret)
          .put("sessionId", sessionId)
          .put("revision", revision)
          .put("reminders", JSONArray(reminders)),
    )
  }

  private fun fcmToken(forceRefresh: Boolean): String? =
    runCatching {
        if (forceRefresh) FirebaseMessaging.getInstance().deleteToken()
        Tasks.await(FirebaseMessaging.getInstance().token)
      }
      .getOrNull()

  private fun post(path: String, body: JSONObject) {
    val connection = (URL("$backendUrl$path").openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      connectTimeout = 10_000
      readTimeout = 10_000
      doOutput = true
      setRequestProperty("content-type", "application/json")
    }
    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
    val status = connection.responseCode
    if (status !in 200..299) error("Reminder backend returned HTTP $status")
    connection.disconnect()
  }

  private fun sha256Hex(value: String): String =
    MessageDigest.getInstance("SHA-256")
      .digest(value.toByteArray(Charsets.UTF_8))
      .joinToString("") { "%02x".format(it) }

  private fun reminder(kind: String, dueAt: Instant): JSONObject =
    JSONObject().put("kind", kind).put("dueAtUtc", dueAt.toString())

  private fun minutes(value: Double): Duration = Duration.ofMillis((value * 60_000).toLong().coerceAtLeast(0))

  private fun nextLateNightInstant(now: Instant): Instant? {
    val zone = ZoneId.of("Asia/Shanghai")
    val localNow = LocalDateTime.ofInstant(now, zone)
    val todayOne = LocalDateTime.of(LocalDate.now(zone), LocalTime.of(1, 0))
    val nextOne = if (localNow.isBefore(todayOne)) todayOne else todayOne.plusDays(1)
    return nextOne.atZone(zone).toInstant().takeIf { Duration.between(now, it) <= Duration.ofHours(16) }
  }

  private companion object {
    const val KEY_DEVICE_ID = "deviceId"
    const val KEY_INSTALL_SECRET = "installSecret"
  }
}
