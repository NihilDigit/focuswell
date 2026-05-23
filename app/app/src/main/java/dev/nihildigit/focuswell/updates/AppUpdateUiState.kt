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
