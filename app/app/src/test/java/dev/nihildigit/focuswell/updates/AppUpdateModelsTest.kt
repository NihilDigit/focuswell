package dev.nihildigit.focuswell.updates

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class AppUpdateModelsTest {
  @Test
  fun versionCodeFromTag_matchesReleaseBuildConvention() {
    assertEquals(260506, versionCodeFromTag("26.5.6"))
    assertEquals(260506, versionCodeFromTag("v26.5.6"))
  }

  @Test
  fun selectUpdateAsset_usesFirstSupportedAbi() {
    val release =
      AppUpdateRelease(
        tagName = "26.5.6",
        name = "FocusWell 26.5.6",
        body = "",
        htmlUrl = "https://github.com/NihilDigit/focuswell/releases/tag/26.5.6",
        assets =
          listOf(
            AppUpdateAsset("focuswell-26.5.6-armeabi-v7a.apk", "v7a", 1),
            AppUpdateAsset("focuswell-26.5.6-arm64-v8a.apk", "arm64", 1),
            AppUpdateAsset("SHA256SUMS.txt", "sha", 1),
          ),
      )

    val selection = selectUpdateAsset(release, listOf("arm64-v8a", "armeabi-v7a"))

    assertEquals("focuswell-26.5.6-arm64-v8a.apk", selection?.apk?.name)
    assertEquals("SHA256SUMS.txt", selection?.checksum?.name)
  }

  @Test
  fun selectUpdateAsset_returnsNullWhenNoAbiMatches() {
    val release =
      AppUpdateRelease(
        tagName = "26.5.6",
        name = "FocusWell 26.5.6",
        body = "",
        htmlUrl = "",
        assets = listOf(AppUpdateAsset("focuswell-26.5.6-x86_64.apk", "x86", 1)),
      )

    assertNull(selectUpdateAsset(release, listOf("arm64-v8a")))
  }

  @Test
  fun appUpdateCheckState_reportsUpToDateWhenReleaseIsNotNewer() {
    val state =
      appUpdateCheckState(
        release = release(tagName = "26.5.6"),
        currentVersionCode = 260506,
        supportedAbis = listOf("arm64-v8a"),
      )

    assertEquals("FocusWell is up to date.", state.message)
    assertNull(state.selection)
  }

  @Test
  fun appUpdateCheckState_reportsNoMatchingApkWhenReleaseIsNewerButAbiDoesNotMatch() {
    val state =
      appUpdateCheckState(
        release =
          release(
            tagName = "26.5.7",
            assets = listOf(AppUpdateAsset("focuswell-26.5.7-x86_64.apk", "x86", 1)),
          ),
        currentVersionCode = 260506,
        supportedAbis = listOf("arm64-v8a"),
      )

    assertEquals("Update found, but no APK matches this device.", state.message)
    assertNull(state.selection)
  }

  @Test
  fun appUpdateCheckState_selectsApkWhenReleaseIsNewerAndAbiMatches() {
    val state =
      appUpdateCheckState(
        release = release(tagName = "26.5.7"),
        currentVersionCode = 260506,
        supportedAbis = listOf("arm64-v8a"),
      )

    assertEquals("FocusWell 26.5.7 is available.", state.message)
    assertEquals("focuswell-26.5.7-arm64-v8a.apk", state.selection?.apk?.name)
  }

  @Test
  fun updateDownloadSucceeded_marksDownloadCompleteAndPromptsInstaller() {
    val apk = File("focuswell.apk")
    val state = AppUpdateUiState(downloading = true, progress = 34).updateDownloadSucceeded(apk)

    assertFalse(state.downloading)
    assertEquals(100, state.progress)
    assertEquals(apk, state.downloadedApk)
    assertEquals("Update downloaded. Opening installer.", state.message)
    assertNull(state.error)
  }

  @Test
  fun updateDownloadFailed_clearsProgressAndDownloadedApk() {
    val state =
      AppUpdateUiState(
        downloading = true,
        progress = 70,
        downloadedApk = File("partial.apk"),
      ).updateDownloadFailed(IllegalStateException("Checksum mismatch"))

    assertFalse(state.downloading)
    assertNull(state.progress)
    assertNull(state.downloadedApk)
    assertEquals("Checksum mismatch", state.error)
  }

  @Test
  fun expectedSha256ForAsset_parsesSha256Sums() {
    val hash = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    val sums =
      """
      $hash  focuswell-26.5.6-arm64-v8a.apk
      abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd  focuswell-26.5.6-x86_64.apk
      """.trimIndent()

    assertEquals(hash, expectedSha256ForAsset(sums, "focuswell-26.5.6-arm64-v8a.apk"))
  }

  @Test
  @OptIn(ExperimentalTime::class)
  fun updateDestinationName_usesInjectedDownloadInstant() {
    assertEquals(
      "updates/1779253200000-focuswell-26.5.6-arm64-v8a.apk",
      updateDestinationName(
        assetName = "focuswell-26.5.6-arm64-v8a.apk",
        downloadedAt = Instant.parse("2026-05-20T05:00:00Z"),
      ),
    )
  }

  private fun release(
    tagName: String,
    assets: List<AppUpdateAsset> =
      listOf(
        AppUpdateAsset("focuswell-$tagName-arm64-v8a.apk", "apk", 1),
        AppUpdateAsset("SHA256SUMS.txt", "sha", 1),
      ),
  ): AppUpdateRelease =
    AppUpdateRelease(
      tagName = tagName,
      name = "FocusWell $tagName",
      body = "",
      htmlUrl = "https://github.com/NihilDigit/focuswell/releases/tag/$tagName",
      assets = assets,
    )
}
