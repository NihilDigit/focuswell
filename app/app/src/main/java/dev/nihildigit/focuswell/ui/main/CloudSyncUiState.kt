package dev.nihildigit.focuswell.ui.main

import android.net.Uri
import dev.nihildigit.focuswell.sync.CloudSnapshot
import dev.nihildigit.focuswell.sync.CloudSnapshotMetadata
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.time.Instant

enum class CloudSyncDecisionKind {
  Upload,
  Restore,
}

data class CloudSyncDecision(
  val kind: CloudSyncDecisionKind,
  val localUpdatedAt: Instant,
  val cloudMetadata: CloudSnapshotMetadata,
  val cloudPayload: String?,
)

data class CloudSyncUiState(
  val userLogin: String? = null,
  val syncing: Boolean = false,
  val message: String? = null,
  val error: String? = null,
  val pendingDecision: CloudSyncDecision? = null,
)

internal fun CloudSyncUiState.cloudCheckStarted(): CloudSyncUiState =
  copy(syncing = true, error = null, pendingDecision = null)

internal fun CloudSyncUiState.cloudSignInPrompted(): CloudSyncUiState =
  copy(message = "Finish GitHub sign in in the browser.", error = null)

internal fun CloudSyncUiState.cloudSignInOpenFailed(error: Throwable): CloudSyncUiState =
  copy(error = error.message ?: "Could not open GitHub sign in.")

internal fun CloudSyncUiState.cloudOAuthRejected(error: String): CloudSyncUiState =
  copy(error = "GitHub sign in failed: $error")

internal fun CloudSyncUiState.cloudOAuthStarted(): CloudSyncUiState =
  copy(syncing = true, message = "Finishing GitHub sign in.", error = null)

internal fun cloudOAuthSucceeded(userLogin: String): CloudSyncUiState =
  CloudSyncUiState(userLogin = userLogin, syncing = false, message = "Signed in as $userLogin.")

internal fun CloudSyncUiState.cloudOAuthFailed(error: Throwable): CloudSyncUiState =
  copy(syncing = false, error = error.message ?: "GitHub sign in failed.")

internal fun CloudSyncUiState.cloudSnapshotLoaded(userLogin: String): CloudSyncUiState =
  copy(syncing = false, userLogin = userLogin)

internal fun CloudSyncUiState.cloudCheckFailed(error: Throwable): CloudSyncUiState =
  copy(syncing = false, error = error.message ?: "Cloud sync failed.")

internal fun CloudSyncUiState.withCloudDecision(
  localUpdatedAt: Instant,
  snapshot: CloudSnapshot,
): CloudSyncUiState {
  val decision = cloudSyncDecision(localUpdatedAt = localUpdatedAt, snapshot = snapshot)
  return if (decision == null) {
    copy(
      pendingDecision = null,
      message = "Local and cloud backups are already in sync.",
      error = null,
    )
  } else {
    copy(
      pendingDecision = decision,
      message = null,
      error = null,
    )
  }
}

internal fun CloudSyncUiState.cloudUploadStarted(): CloudSyncUiState =
  copy(syncing = true, pendingDecision = null, error = null)

internal fun CloudSyncUiState.cloudUploadSucceeded(userLogin: String): CloudSyncUiState =
  copy(
    syncing = false,
    userLogin = userLogin,
    message = "Uploaded local backup to cloud.",
    error = null,
  )

internal fun CloudSyncUiState.cloudUploadFailed(error: Throwable): CloudSyncUiState =
  copy(syncing = false, error = error.message ?: "Cloud upload failed.")

internal fun CloudSyncUiState.cloudRestoreSucceeded(): CloudSyncUiState =
  copy(
    pendingDecision = null,
    message = "Restored cloud backup to this device.",
    error = null,
  )

internal fun CloudSyncUiState.cloudRestoreFailed(): CloudSyncUiState =
  copy(
    pendingDecision = null,
    error = "Cloud backup could not be restored.",
  )

internal fun cloudSignedOut(): CloudSyncUiState =
  CloudSyncUiState(message = "Signed out of cloud sync.")

internal sealed interface CloudSyncRedirect {
  data class Code(val value: String) : CloudSyncRedirect
  data class Rejected(val error: String) : CloudSyncRedirect
  data object Ignored : CloudSyncRedirect
}

internal fun cloudSyncRedirect(
  scheme: String?,
  host: String?,
  path: String?,
  code: String?,
  error: String?,
): CloudSyncRedirect {
  if (scheme != "focuswell" || host != "sync" || path != "/oauth") return CloudSyncRedirect.Ignored
  if (error != null) return CloudSyncRedirect.Rejected(error)
  return code?.let(CloudSyncRedirect::Code) ?: CloudSyncRedirect.Ignored
}

internal fun Uri.toCloudSyncRedirect(): CloudSyncRedirect =
  cloudSyncRedirect(
    scheme = scheme,
    host = host,
    path = path,
    code = getQueryParameter("code"),
    error = getQueryParameter("error"),
  )

internal fun cloudPayloadFromExport(exportedJson: String): JsonObject =
  Json.parseToJsonElement(exportedJson).jsonObject

internal fun cloudSyncDecision(
  localUpdatedAt: Instant,
  snapshot: CloudSnapshot,
): CloudSyncDecision? {
  val cloudUpdatedAt = snapshot.metadata.updatedAtUtc
  return when {
    localUpdatedAt.isAfter(cloudUpdatedAt) ->
      CloudSyncDecision(
        kind = CloudSyncDecisionKind.Upload,
        localUpdatedAt = localUpdatedAt,
        cloudMetadata = snapshot.metadata,
        cloudPayload = null,
      )
    cloudUpdatedAt.isAfter(localUpdatedAt) ->
      CloudSyncDecision(
        kind = CloudSyncDecisionKind.Restore,
        localUpdatedAt = localUpdatedAt,
        cloudMetadata = snapshot.metadata,
        cloudPayload = snapshot.payload.toString(),
      )
    else -> null
  }
}
