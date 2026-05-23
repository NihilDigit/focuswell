package dev.nihildigit.focuswell.updates

import java.util.Locale

data class AppUpdateRelease(
  val tagName: String,
  val name: String,
  val body: String,
  val htmlUrl: String,
  val assets: List<AppUpdateAsset>,
) {
  val versionCode: Int
    get() = versionCodeFromTag(tagName)
}

data class AppUpdateAsset(
  val name: String,
  val downloadUrl: String,
  val sizeBytes: Long,
)

data class AppUpdateSelection(
  val apk: AppUpdateAsset,
  val checksum: AppUpdateAsset?,
)

fun versionCodeFromTag(tagName: String): Int {
  val parts = tagName.trim().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
  if (parts.size < 3) return 0
  return parts[0] * 10_000 + parts[1] * 100 + parts[2]
}

fun selectUpdateAsset(release: AppUpdateRelease, supportedAbis: List<String>): AppUpdateSelection? {
  val checksum = release.assets.firstOrNull { it.name.equals("SHA256SUMS.txt", ignoreCase = true) }
  val normalizedAssets = release.assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
  val apk =
    supportedAbis.firstNotNullOfOrNull { abi ->
      normalizedAssets.firstOrNull { asset ->
        asset.name.lowercase(Locale.US).contains("-${abi.lowercase(Locale.US)}.apk")
      }
    }
  return apk?.let { AppUpdateSelection(apk = it, checksum = checksum) }
}

fun expectedSha256ForAsset(sha256Sums: String, assetName: String): String? =
  sha256Sums
    .lineSequence()
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .firstNotNullOfOrNull { line ->
      val parts = line.split(Regex("\\s+"), limit = 2)
      val hash = parts.getOrNull(0)?.takeIf { it.matches(Regex("[A-Fa-f0-9]{64}")) }
      val name = parts.getOrNull(1)?.trim()?.removePrefix("*")
      if (hash != null && name == assetName) hash.lowercase(Locale.US) else null
    }
