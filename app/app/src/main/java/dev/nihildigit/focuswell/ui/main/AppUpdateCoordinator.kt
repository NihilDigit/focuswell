package dev.nihildigit.focuswell.ui.main

import android.os.Build
import dev.nihildigit.focuswell.BuildConfig
import dev.nihildigit.focuswell.updates.AppUpdateInstaller
import dev.nihildigit.focuswell.updates.AppUpdateInstallPermissionRequired
import dev.nihildigit.focuswell.updates.AppUpdateUiState
import dev.nihildigit.focuswell.updates.GitHubReleaseClient
import dev.nihildigit.focuswell.updates.appUpdateCheckState
import dev.nihildigit.focuswell.updates.updateCheckFailed
import dev.nihildigit.focuswell.updates.updateCheckStarted
import dev.nihildigit.focuswell.updates.updateDownloadFailed
import dev.nihildigit.focuswell.updates.updateDownloadStarted
import dev.nihildigit.focuswell.updates.updateDownloadSucceeded
import dev.nihildigit.focuswell.updates.updateInstallFailed
import dev.nihildigit.focuswell.updates.updateInstallPermissionRequired
import dev.nihildigit.focuswell.updates.updateReleasePageOpenFailed
import dev.nihildigit.focuswell.updates.withDownloadProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AppUpdateCoordinator(
  private val updateClient: GitHubReleaseClient,
  private val updateInstaller: AppUpdateInstaller,
  private val currentVersionCode: Int = BuildConfig.VERSION_CODE,
  private val supportedAbis: List<String> = Build.SUPPORTED_ABIS.toList(),
) {
  private val _state = MutableStateFlow(AppUpdateUiState())
  val state: StateFlow<AppUpdateUiState> = _state

  fun checkForUpdate(scope: CoroutineScope) {
    if (_state.value.checking || _state.value.downloading) return
    _state.value = _state.value.updateCheckStarted()
    scope.launch {
      runCatching {
        withContext(Dispatchers.IO) { updateClient.fetchLatestRelease() }
      }.onSuccess { release ->
        _state.value =
          appUpdateCheckState(
            release = release,
            currentVersionCode = currentVersionCode,
            supportedAbis = supportedAbis,
          )
      }.onFailure { error ->
        _state.value = updateCheckFailed(error)
      }
    }
  }

  fun downloadUpdate(scope: CoroutineScope) {
    val selection = _state.value.selection ?: return
    if (_state.value.downloading) return
    _state.value = _state.value.updateDownloadStarted()
    scope.launch {
      runCatching {
        updateInstaller.downloadAndVerify(selection) { progress ->
          _state.value = _state.value.withDownloadProgress(progress)
        }
      }.onSuccess { apk ->
        _state.value = _state.value.updateDownloadSucceeded(apk)
        installDownloadedUpdate()
      }.onFailure { error ->
        _state.value = _state.value.updateDownloadFailed(error)
      }
    }
  }

  fun installDownloadedUpdate() {
    val apk = _state.value.downloadedApk ?: return
    runCatching { updateInstaller.install(apk) }
      .onFailure { error ->
        _state.value =
          if (error is AppUpdateInstallPermissionRequired) {
            _state.value.updateInstallPermissionRequired(error)
          } else {
            _state.value.updateInstallFailed(error)
          }
      }
  }

  fun openReleasePage() {
    val release = _state.value.latestRelease ?: return
    runCatching { updateInstaller.openReleasePage(release) }
      .onFailure { error ->
        _state.value = _state.value.updateReleasePageOpenFailed(error)
      }
  }
}
