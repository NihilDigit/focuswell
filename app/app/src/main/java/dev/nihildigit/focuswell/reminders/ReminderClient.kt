package dev.nihildigit.focuswell.reminders

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.BuildConfig
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.security.SecureStringStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.hours

data class DeviceIdentity(
  val deviceId: String,
  val installSecret: String,
)

data class PushRegistrationStatus(
  val deviceId: String,
  val enabled: Boolean = true,
  val hasFcmToken: Boolean,
  val lastRegisteredAt: Instant? = null,
  val lastError: String? = null,
)

class ReminderClient(context: Context) {
  private val appContext = context.applicationContext
  private val prefs = context.getSharedPreferences("focuswell-reminders", Context.MODE_PRIVATE)
  private val securePrefs = SecureStringStore(context.applicationContext, "focuswell-reminders")
  private val backendUrl = BuildConfig.FOCUSWELL_BACKEND_URL.trimEnd('/')
  private val json = Json { explicitNulls = false }

  val identity: DeviceIdentity
    get() {
      val existingDeviceId = prefs.getString(KEY_DEVICE_ID, null)
      val existingSecret = securePrefs.getString(KEY_INSTALL_SECRET, null)
      if (existingDeviceId != null && existingSecret != null) {
        return DeviceIdentity(existingDeviceId, existingSecret)
      }
      val next = DeviceIdentity(UUID.randomUUID().toString(), UUID.randomUUID().toString())
      prefs
        .edit()
        .putString(KEY_DEVICE_ID, next.deviceId)
        .apply()
      securePrefs.putString(KEY_INSTALL_SECRET, next.installSecret)
      return next
    }

  fun rotateIdentity(): DeviceIdentity {
    val next = DeviceIdentity(UUID.randomUUID().toString(), UUID.randomUUID().toString())
    prefs
      .edit()
      .putString(KEY_DEVICE_ID, next.deviceId)
      .putBoolean(KEY_PUSH_ENABLED, true)
      .remove(KEY_LAST_REGISTERED_AT)
      .remove(KEY_LAST_REGISTRATION_HAD_FCM_TOKEN)
      .remove(KEY_LAST_REGISTRATION_ERROR)
      .apply()
    securePrefs.putString(KEY_INSTALL_SECRET, next.installSecret)
    return next
  }

  suspend fun scheduleFocusReminders(sessionId: String, revision: Int, rules: FocusWellRules) {
    if (!pushEnabled()) return
    ensureRegistered()
    val now = Instant.now()
    schedule(
      sessionId = sessionId,
      revision = revision,
      reminders =
        buildList {
          if (rules.normalized().longSessionRemindersEnabled) {
            add(reminder("focus_duration_1h", now.plusMillis(1.hours.inWholeMilliseconds)))
            add(reminder("focus_duration_3h", now.plusMillis(3.hours.inWholeMilliseconds)))
            add(reminder("focus_duration_5h", now.plusMillis(5.hours.inWholeMilliseconds)))
          } else {
            add(reminder("focus_stale_3h", now.plusMillis(3.hours.inWholeMilliseconds)))
          }
        },
    )
  }

  suspend fun scheduleLeisureReminders(
    sessionId: String,
    revision: Int,
    reserveMinutes: Double,
    rules: FocusWellRules,
  ) {
    if (!pushEnabled()) return
    ensureRegistered()
    val now = Instant.now()
    val reminders =
      buildList {
        if (rules.normalized().longSessionRemindersEnabled) {
          add(reminder("leisure_duration_1h", now.plusMillis(1.hours.inWholeMilliseconds)))
          add(reminder("leisure_duration_3h", now.plusMillis(3.hours.inWholeMilliseconds)))
          add(reminder("leisure_duration_5h", now.plusMillis(5.hours.inWholeMilliseconds)))
        }
        if (reserveMinutes > 10.0) add(reminder("leisure_10m_left", leisureCostDueAt(now, reserveMinutes - 10.0, rules)))
        if (reserveMinutes > 5.0) add(reminder("leisure_5m_left", leisureCostDueAt(now, reserveMinutes - 5.0, rules)))
        if (reserveMinutes > 1.0) add(reminder("leisure_1m_left", leisureCostDueAt(now, reserveMinutes - 1.0, rules)))
        if (reserveMinutes > 0.0) add(reminder("leisure_depleted", leisureCostDueAt(now, reserveMinutes, rules)))
        nextLateNightInstant(now, rules)?.let { add(reminder("late_night_rate_started", it)) }
      }
    schedule(sessionId = sessionId, revision = revision, reminders = reminders)
  }

  suspend fun cancelSession(sessionId: String) {
    val identity = identity
    post(
      path = "/api/reminders/cancel",
      body =
        CancelSessionRequest(
          deviceId = identity.deviceId,
          installSecret = identity.installSecret,
          sessionId = sessionId,
        ),
    )
  }

  fun cachedRegistrationStatus(): PushRegistrationStatus =
    PushRegistrationStatus(
      deviceId = identity.deviceId,
      enabled = pushEnabled(),
      hasFcmToken = prefs.getBoolean(KEY_LAST_REGISTRATION_HAD_FCM_TOKEN, false),
      lastRegisteredAt = prefs.getString(KEY_LAST_REGISTERED_AT, null)?.let { runCatching { Instant.parse(it) }.getOrNull() },
      lastError = prefs.getString(KEY_LAST_REGISTRATION_ERROR, null),
    )

  suspend fun refreshFcmRegistration(forceTokenRefresh: Boolean = true): PushRegistrationStatus {
    prefs.edit().putBoolean(KEY_PUSH_ENABLED, true).apply()
    return ensureRegistered(forceTokenRefresh = forceTokenRefresh)
  }

  suspend fun disablePush(): PushRegistrationStatus {
    prefs
      .edit()
      .putBoolean(KEY_PUSH_ENABLED, false)
      .putBoolean(KEY_LAST_REGISTRATION_HAD_FCM_TOKEN, false)
      .remove(KEY_LAST_REGISTRATION_ERROR)
      .apply()
    withContext(Dispatchers.IO) {
      runCatching { Tasks.await(FirebaseMessaging.getInstance().deleteToken()) }
    }
    return cachedRegistrationStatus()
  }

  private fun pushEnabled(): Boolean = prefs.getBoolean(KEY_PUSH_ENABLED, true)

  private suspend fun ensureRegistered(forceTokenRefresh: Boolean = false): PushRegistrationStatus {
    val identity = identity
    val tokenResult = fcmToken(forceTokenRefresh)
    val token = tokenResult.getOrNull()
    val tokenError = tokenResult.exceptionOrNull()
    post(
      path = "/api/devices/register",
      body =
        RegisterDeviceRequest(
          deviceId = identity.deviceId,
          installSecret = identity.installSecret,
          installSecretHash = sha256Hex(identity.installSecret),
          nowUtc = Instant.now().toString(),
          fcmToken = token,
        ),
    )
    val registeredAt = Instant.now()
    val status =
      PushRegistrationStatus(
        deviceId = identity.deviceId,
        enabled = pushEnabled(),
        hasFcmToken = token != null,
        lastRegisteredAt = registeredAt,
        lastError = tokenError?.message,
      )
    prefs
      .edit()
      .putBoolean(KEY_LAST_REGISTRATION_HAD_FCM_TOKEN, status.hasFcmToken)
      .putString(KEY_LAST_REGISTERED_AT, registeredAt.toString())
      .apply {
        if (status.lastError == null) remove(KEY_LAST_REGISTRATION_ERROR)
        else putString(KEY_LAST_REGISTRATION_ERROR, status.lastError)
      }
      .apply()
    return status
  }

  private suspend fun schedule(
    sessionId: String,
    revision: Int,
    reminders: List<ReminderRequest>,
  ) {
    if (reminders.isEmpty()) return
    val identity = identity
    post(
      path = "/api/reminders/schedule",
      body =
        ScheduleRemindersRequest(
          deviceId = identity.deviceId,
          installSecret = identity.installSecret,
          sessionId = sessionId,
          revision = revision,
          reminders = reminders,
        ),
    )
  }

  private suspend fun fcmToken(forceRefresh: Boolean): Result<String> =
    withContext(Dispatchers.IO) {
      runCatching {
        if (forceRefresh) FirebaseMessaging.getInstance().deleteToken()
        Tasks.await(FirebaseMessaging.getInstance().token)
      }
    }

  private suspend inline fun <reified T> post(path: String, body: T) {
    val bodyText = json.encodeToString(body)
    withContext(Dispatchers.IO) {
      val connection = (URL("$backendUrl$path").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 10_000
        readTimeout = 10_000
        doOutput = true
        setRequestProperty("content-type", "application/json")
      }
      try {
        connection.outputStream.use { it.write(bodyText.toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        if (status !in 200..299) error("Reminder backend returned HTTP $status")
      } finally {
        connection.disconnect()
      }
    }
  }

  private fun sha256Hex(value: String): String =
    MessageDigest.getInstance("SHA-256")
      .digest(value.toByteArray(Charsets.UTF_8))
      .joinToString("") { "%02x".format(it) }

  private fun reminder(kind: String, dueAt: Instant): ReminderRequest =
    ReminderRequest(kind = kind, dueAtUtc = dueAt.toString())

  private fun leisureCostDueAt(startedAt: Instant, costMinutes: Double, rules: FocusWellRules): Instant =
    TimeAccounting.instantWhenLeisureCostReaches(startedAt, costMinutes, rules = rules)

  private companion object {
    const val KEY_DEVICE_ID = "deviceId"
    const val KEY_INSTALL_SECRET = "installSecret"
    const val KEY_PUSH_ENABLED = "pushEnabled"
    const val KEY_LAST_REGISTERED_AT = "lastRegisteredAt"
    const val KEY_LAST_REGISTRATION_HAD_FCM_TOKEN = "lastRegistrationHadFcmToken"
    const val KEY_LAST_REGISTRATION_ERROR = "lastRegistrationError"
  }
}

@Serializable
private data class CancelSessionRequest(
  val deviceId: String,
  val installSecret: String,
  val sessionId: String,
)

@Serializable
private data class RegisterDeviceRequest(
  val deviceId: String,
  val installSecret: String,
  val installSecretHash: String,
  val nowUtc: String,
  val fcmToken: String? = null,
)

@Serializable
private data class ScheduleRemindersRequest(
  val deviceId: String,
  val installSecret: String,
  val sessionId: String,
  val revision: Int,
  val reminders: List<ReminderRequest>,
)

@Serializable
private data class ReminderRequest(
  val kind: String,
  val dueAtUtc: String,
)
