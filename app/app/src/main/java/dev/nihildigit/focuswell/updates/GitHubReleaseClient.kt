package dev.nihildigit.focuswell.updates

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

class GitHubReleaseClient(
  private val latestReleaseUrl: String = "https://api.github.com/repos/NihilDigit/focuswell/releases/latest",
) {
  private val json = Json { ignoreUnknownKeys = true }

  fun fetchLatestRelease(): AppUpdateRelease {
    val release = json.decodeFromString(GitHubReleaseResponse.serializer(), fetchText(latestReleaseUrl, accept = "application/vnd.github+json"))
    return AppUpdateRelease(
      tagName = release.tagName,
      name = release.name ?: release.tagName,
      body = release.body.orEmpty(),
      htmlUrl = release.htmlUrl,
      assets =
        release.assets.map { asset ->
          AppUpdateAsset(
            name = asset.name,
            downloadUrl = asset.downloadUrl,
            sizeBytes = asset.sizeBytes,
          )
        },
    )
  }

  fun fetchText(url: String): String = fetchText(url, accept = "text/plain")

  private fun fetchText(url: String, accept: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 15_000
    connection.readTimeout = 20_000
    connection.setRequestProperty("Accept", accept)
    connection.setRequestProperty("User-Agent", "FocusWell")
    return try {
      val status = connection.responseCode
      if (status !in 200..299) error("GitHub returned HTTP $status")
      connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
      connection.disconnect()
    }
  }
}

@Serializable
private data class GitHubReleaseResponse(
  @SerialName("tag_name") val tagName: String,
  val name: String? = null,
  val body: String? = null,
  @SerialName("html_url") val htmlUrl: String,
  val assets: List<GitHubReleaseAssetResponse> = emptyList(),
)

@Serializable
private data class GitHubReleaseAssetResponse(
  val name: String,
  @SerialName("browser_download_url") val downloadUrl: String,
  @SerialName("size") val sizeBytes: Long = 0L,
)
