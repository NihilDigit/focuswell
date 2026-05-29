package dev.nihildigit.focuswell.sync

import android.content.Context
import android.net.Uri
import dev.nihildigit.focuswell.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.UUID

data class CloudSyncUser(
  val id: Long,
  val login: String,
)

data class CloudSnapshotMetadata(
  val updatedAtUtc: Instant,
  val uploadedAtUtc: Instant,
  val appVersion: String,
  val jsonHash: String,
)

data class CloudSnapshot(
  val metadata: CloudSnapshotMetadata,
  val payload: JSONObject,
)

data class CloudSyncSession(
  val accessToken: String,
  val user: CloudSyncUser,
)

class CloudSyncClient(context: Context) {
  private val prefs = context.applicationContext.getSharedPreferences("focuswell-cloud-sync", Context.MODE_PRIVATE)
  private val backendUrl = BuildConfig.FOCUSWELL_BACKEND_URL.trimEnd('/')

  fun cachedSession(): CloudSyncSession? {
    val token = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
    val login = prefs.getString(KEY_GITHUB_LOGIN, null) ?: return null
    val id = prefs.getLong(KEY_GITHUB_USER_ID, 0L).takeIf { it > 0L } ?: return null
    return CloudSyncSession(accessToken = token, user = CloudSyncUser(id = id, login = login))
  }

  fun authUri(state: String = UUID.randomUUID().toString()): Uri =
    Uri.Builder()
      .scheme("https")
      .authority("github.com")
      .path("/login/oauth/authorize")
      .appendQueryParameter("client_id", GITHUB_CLIENT_ID)
      .appendQueryParameter("redirect_uri", REDIRECT_URI)
      .appendQueryParameter("scope", "")
      .appendQueryParameter("state", state)
      .build()

  suspend fun exchangeCode(code: String): CloudSyncSession {
    val json =
      postJson(
        path = "/api/sync/oauth/exchange",
        body = JSONObject().put("code", code),
      )
    val user = json.getJSONObject("user").toUser()
    val session = CloudSyncSession(accessToken = json.getString("accessToken"), user = user)
    prefs
      .edit()
      .putString(KEY_ACCESS_TOKEN, session.accessToken)
      .putLong(KEY_GITHUB_USER_ID, user.id)
      .putString(KEY_GITHUB_LOGIN, user.login)
      .apply()
    return session
  }

  suspend fun getSnapshot(session: CloudSyncSession): CloudSnapshot? {
    val json = requestJson(path = "/api/sync/snapshot", method = "GET", token = session.accessToken)
    val snapshot = json.optJSONObject("snapshot") ?: return null
    return CloudSnapshot(
      metadata = snapshot.getJSONObject("metadata").toMetadata(),
      payload = snapshot.getJSONObject("payload"),
    )
  }

  suspend fun putSnapshot(
    session: CloudSyncSession,
    updatedAtUtc: Instant,
    payload: JSONObject,
  ): CloudSnapshotMetadata {
    val json =
      requestJson(
        path = "/api/sync/snapshot",
        method = "POST",
        token = session.accessToken,
        body =
          JSONObject()
            .put("updatedAtUtc", updatedAtUtc.toString())
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("payload", payload),
      )
    return json.getJSONObject("metadata").toMetadata()
  }

  fun signOut() {
    prefs.edit().clear().apply()
  }

  private suspend fun postJson(path: String, body: JSONObject): JSONObject =
    requestJson(path = path, method = "POST", token = null, body = body)

  private suspend fun requestJson(
    path: String,
    method: String,
    token: String?,
    body: JSONObject? = null,
  ): JSONObject =
    withContext(Dispatchers.IO) {
      val connection = (URL("$backendUrl$path").openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 10_000
        readTimeout = 15_000
        setRequestProperty("accept", "application/json")
        setRequestProperty("content-type", "application/json")
        if (token != null) setRequestProperty("authorization", "Bearer $token")
        if (body != null) doOutput = true
      }
      try {
        if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        val text =
          (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
        if (status !in 200..299) error("Cloud sync returned HTTP $status")
        JSONObject(text)
      } finally {
        connection.disconnect()
      }
    }

  private fun JSONObject.toUser(): CloudSyncUser =
    CloudSyncUser(id = getLong("id"), login = getString("login"))

  private fun JSONObject.toMetadata(): CloudSnapshotMetadata =
    CloudSnapshotMetadata(
      updatedAtUtc = Instant.parse(getString("updatedAtUtc")),
      uploadedAtUtc = Instant.parse(getString("uploadedAtUtc")),
      appVersion = getString("appVersion"),
      jsonHash = getString("jsonHash"),
    )

  private companion object {
    const val GITHUB_CLIENT_ID = "Ov23liyYDohki31BD358"
    const val REDIRECT_URI = "focuswell://sync/oauth"
    const val KEY_ACCESS_TOKEN = "accessToken"
    const val KEY_GITHUB_USER_ID = "githubUserId"
    const val KEY_GITHUB_LOGIN = "githubLogin"
  }
}
