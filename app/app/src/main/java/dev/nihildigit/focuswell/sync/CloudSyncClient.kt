package dev.nihildigit.focuswell.sync

import android.content.Context
import android.net.Uri
import dev.nihildigit.focuswell.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
  val payload: JsonObject,
)

data class CloudSyncSession(
  val accessToken: String,
  val user: CloudSyncUser,
)

class CloudSyncClient(context: Context) {
  private val prefs = context.applicationContext.getSharedPreferences("focuswell-cloud-sync", Context.MODE_PRIVATE)
  private val backendUrl = BuildConfig.FOCUSWELL_BACKEND_URL.trimEnd('/')
  private val json =
    Json {
      ignoreUnknownKeys = true
      explicitNulls = false
    }

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
    val response =
      requestJson<OAuthExchangeResponse>(
        path = "/api/sync/oauth/exchange",
        method = "POST",
        token = null,
        body = json.encodeToString(OAuthExchangeRequest(code = code)),
      )
    val user = response.user.toDomain()
    val session = CloudSyncSession(accessToken = response.accessToken, user = user)
    prefs
      .edit()
      .putString(KEY_ACCESS_TOKEN, session.accessToken)
      .putLong(KEY_GITHUB_USER_ID, user.id)
      .putString(KEY_GITHUB_LOGIN, user.login)
      .apply()
    return session
  }

  suspend fun getSnapshot(session: CloudSyncSession): CloudSnapshot? {
    val response = requestJson<GetSnapshotResponse>(path = "/api/sync/snapshot", method = "GET", token = session.accessToken)
    val snapshot = response.snapshot ?: return null
    return CloudSnapshot(
      metadata = snapshot.metadata.toDomain(),
      payload = snapshot.payload,
    )
  }

  suspend fun putSnapshot(
    session: CloudSyncSession,
    updatedAtUtc: Instant,
    payload: JsonObject,
  ): CloudSnapshotMetadata {
    val response =
      requestJson<PutSnapshotResponse>(
        path = "/api/sync/snapshot",
        method = "POST",
        token = session.accessToken,
        body =
          json.encodeToString(
            PutSnapshotRequest(
              updatedAtUtc = updatedAtUtc.toString(),
              appVersion = BuildConfig.VERSION_NAME,
              payload = payload,
            )
          ),
      )
    return response.metadata.toDomain()
  }

  fun signOut() {
    prefs.edit().clear().apply()
  }

  private suspend inline fun <reified T> requestJson(
    path: String,
    method: String,
    token: String?,
    body: String? = null,
  ): T =
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
        if (body != null) connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        val text =
          (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
        if (status !in 200..299) error("Cloud sync returned HTTP $status")
        json.decodeFromString<T>(text)
      } finally {
        connection.disconnect()
      }
    }

  private companion object {
    const val GITHUB_CLIENT_ID = "Ov23liyYDohki31BD358"
    const val REDIRECT_URI = "focuswell://sync/oauth"
    const val KEY_ACCESS_TOKEN = "accessToken"
    const val KEY_GITHUB_USER_ID = "githubUserId"
    const val KEY_GITHUB_LOGIN = "githubLogin"
  }
}

@Serializable
private data class OAuthExchangeRequest(
  val code: String,
)

@Serializable
private data class OAuthExchangeResponse(
  val accessToken: String,
  val user: CloudSyncUserJson,
)

@Serializable
private data class CloudSyncUserJson(
  val id: Long,
  val login: String,
) {
  fun toDomain(): CloudSyncUser = CloudSyncUser(id = id, login = login)
}

@Serializable
private data class GetSnapshotResponse(
  val snapshot: CloudSnapshotJson? = null,
)

@Serializable
private data class PutSnapshotRequest(
  val updatedAtUtc: String,
  val appVersion: String,
  val payload: JsonObject,
)

@Serializable
private data class PutSnapshotResponse(
  val metadata: CloudSnapshotMetadataJson,
)

@Serializable
private data class CloudSnapshotJson(
  val metadata: CloudSnapshotMetadataJson,
  val payload: JsonObject = buildJsonObject {},
)

@Serializable
private data class CloudSnapshotMetadataJson(
  val updatedAtUtc: String,
  val uploadedAtUtc: String,
  val appVersion: String,
  val jsonHash: String,
) {
  fun toDomain(): CloudSnapshotMetadata =
    CloudSnapshotMetadata(
      updatedAtUtc = Instant.parse(updatedAtUtc),
      uploadedAtUtc = Instant.parse(uploadedAtUtc),
      appVersion = appVersion,
      jsonHash = jsonHash,
    )
}
