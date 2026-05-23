package dev.nihildigit.focuswell.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
  fun expectedSha256ForAsset_parsesSha256Sums() {
    val hash = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    val sums =
      """
      $hash  focuswell-26.5.6-arm64-v8a.apk
      abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd  focuswell-26.5.6-x86_64.apk
      """.trimIndent()

    assertEquals(hash, expectedSha256ForAsset(sums, "focuswell-26.5.6-arm64-v8a.apk"))
  }
}
