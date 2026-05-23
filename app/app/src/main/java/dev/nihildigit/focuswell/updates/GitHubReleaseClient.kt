package dev.nihildigit.focuswell.updates

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GitHubReleaseClient(
  private val latestReleaseUrl: String = "https://api.github.com/repos/NihilDigit/focuswell/releases/latest",
) {
  fun fetchLatestRelease(): AppUpdateRelease {
    val json = JSONObject(fetchText(latestReleaseUrl, accept = "application/vnd.github+json"))
    val assetsJson = json.optJSONArray("assets")
    val assets =
      buildList {
        if (assetsJson != null) {
          for (index in 0 until assetsJson.length()) {
            val asset = assetsJson.getJSONObject(index)
            add(
              AppUpdateAsset(
                name = asset.getString("name"),
                downloadUrl = asset.getString("browser_download_url"),
                sizeBytes = asset.optLong("size", 0L),
              ),
            )
          }
        }
      }
    return AppUpdateRelease(
      tagName = json.getString("tag_name"),
      name = json.optString("name", json.getString("tag_name")),
      body = json.optString("body"),
      htmlUrl = json.getString("html_url"),
      assets = assets,
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
