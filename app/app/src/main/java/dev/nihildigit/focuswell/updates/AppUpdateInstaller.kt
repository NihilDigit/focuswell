package dev.nihildigit.focuswell.updates

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class AppUpdateInstaller(
  private val context: Context,
  private val client: GitHubReleaseClient = GitHubReleaseClient(),
  private val now: () -> Instant = Clock.System::now,
) {
  private val appContext = context.applicationContext

  suspend fun downloadAndVerify(selection: AppUpdateSelection, onProgress: (Int) -> Unit): File =
    withContext(Dispatchers.IO) {
      onProgress(0)
      val apkFile = downloadApk(selection.apk, onProgress)
      val checksumAsset = selection.checksum ?: error("Release is missing SHA256SUMS.txt")
      val expected = expectedSha256ForAsset(client.fetchText(checksumAsset.downloadUrl), selection.apk.name)
        ?: error("SHA256SUMS.txt does not list ${selection.apk.name}")
      val actual = sha256(apkFile)
      if (!actual.equals(expected, ignoreCase = true)) {
        error("Downloaded APK checksum did not match")
      }
      onProgress(100)
      apkFile
    }

  @Suppress("DEPRECATION")
  fun install(apkFile: File) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !appContext.packageManager.canRequestPackageInstalls()) {
      val settingsIntent =
        Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
          .setData(Uri.parse("package:${appContext.packageName}"))
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      appContext.startActivity(settingsIntent)
      throw AppUpdateInstallPermissionRequired()
    }

    val apkUri =
      FileProvider.getUriForFile(
        appContext,
        "${appContext.packageName}.fileprovider",
        apkFile,
      )
    val installIntent =
      Intent(Intent.ACTION_INSTALL_PACKAGE)
        .setData(apkUri)
        .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        .putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, appContext.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    appContext.startActivity(installIntent)
  }

  fun openReleasePage(release: AppUpdateRelease) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    appContext.startActivity(intent)
  }

  private suspend fun downloadApk(asset: AppUpdateAsset, onProgress: (Int) -> Unit): File {
    val manager = appContext.getSystemService(DownloadManager::class.java)
    val destinationName = updateDestinationName(asset.name, now())
    val request =
      DownloadManager.Request(Uri.parse(asset.downloadUrl))
        .setTitle(asset.name)
        .setDescription("Downloading FocusWell update")
        .setMimeType("application/vnd.android.package-archive")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, destinationName)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(false)
    val downloadId = manager.enqueue(request)
    return try {
      waitForDownload(manager, downloadId, asset.sizeBytes, onProgress)
    } catch (error: Throwable) {
      manager.remove(downloadId)
      throw error
    }
  }

  private suspend fun waitForDownload(
    manager: DownloadManager,
    downloadId: Long,
    expectedBytes: Long,
    onProgress: (Int) -> Unit,
  ): File {
    val query = DownloadManager.Query().setFilterById(downloadId)
    while (true) {
      manager.query(query).use { cursor ->
        if (!cursor.moveToFirst()) error("Download disappeared")
        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).takeIf { it > 0 } ?: expectedBytes
        if (total > 0) {
          onProgress(((downloaded * 100) / total).toInt().coerceIn(0, 99))
        }
        when (status) {
          DownloadManager.STATUS_SUCCESSFUL -> {
            val localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            return File(Uri.parse(localUri).path ?: error("Downloaded file path is unavailable"))
          }
          DownloadManager.STATUS_FAILED -> {
            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            error("Download failed with reason $reason")
          }
        }
      }
      delay(500)
    }
  }

  private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
      val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
      while (true) {
        val read = input.read(buffer)
        if (read <= 0) break
        digest.update(buffer, 0, read)
      }
    }
    return digest.digest().joinToString(separator = "") { "%02x".format(Locale.US, it.toInt() and 0xff) }
  }
}

@OptIn(ExperimentalTime::class)
internal fun updateDestinationName(assetName: String, downloadedAt: Instant): String =
  "updates/${downloadedAt.toEpochMilliseconds()}-$assetName"

class AppUpdateInstallPermissionRequired :
  IllegalStateException("Allow FocusWell to install unknown apps, then return and tap Install.")
