package dev.nihildigit.focuswell.updates

import java.io.File

data class AppUpdateUiState(
  val checking: Boolean = false,
  val downloading: Boolean = false,
  val progress: Int? = null,
  val latestRelease: AppUpdateRelease? = null,
  val selection: AppUpdateSelection? = null,
  val downloadedApk: File? = null,
  val message: String? = null,
  val error: String? = null,
) {
  val updateAvailable: Boolean
    get() = selection != null
}

fun appUpdateCheckState(
  release: AppUpdateRelease,
  currentVersionCode: Int,
  supportedAbis: List<String>,
): AppUpdateUiState {
  val selection =
    if (release.versionCode > currentVersionCode) {
      selectUpdateAsset(release, supportedAbis)
    } else {
      null
    }
  return AppUpdateUiState(
    latestRelease = release,
    selection = selection,
    message =
      when {
        release.versionCode <= currentVersionCode -> "FocusWell is up to date."
        selection == null -> "Update found, but no APK matches this device."
        else -> "FocusWell ${release.tagName} is available."
      },
  )
}

fun AppUpdateUiState.updateCheckStarted(): AppUpdateUiState =
  copy(checking = true, message = null, error = null)

fun updateCheckFailed(error: Throwable): AppUpdateUiState =
  AppUpdateUiState(error = error.message ?: "Update check failed.")

fun AppUpdateUiState.updateDownloadStarted(): AppUpdateUiState =
  copy(downloading = true, progress = 0, message = "Downloading update.", error = null)

fun AppUpdateUiState.withDownloadProgress(progress: Int): AppUpdateUiState =
  copy(progress = progress)

fun AppUpdateUiState.updateDownloadSucceeded(apk: File): AppUpdateUiState =
  copy(
    downloading = false,
    progress = 100,
    downloadedApk = apk,
    message = "Update downloaded. Opening installer.",
    error = null,
  )

fun AppUpdateUiState.updateDownloadFailed(error: Throwable): AppUpdateUiState =
  copy(
    downloading = false,
    progress = null,
    downloadedApk = null,
    error = error.message ?: "Download failed.",
  )

fun AppUpdateUiState.updateInstallFailed(error: Throwable): AppUpdateUiState =
  copy(error = error.message ?: "Could not open installer.")

fun AppUpdateUiState.updateReleasePageOpenFailed(error: Throwable): AppUpdateUiState =
  copy(error = error.message ?: "Could not open release page.")
