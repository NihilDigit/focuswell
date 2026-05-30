package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.BuildConfig
import dev.nihildigit.focuswell.updates.AppUpdateUiState

@Composable
internal fun SettingsUpdateRow(
  updateState: AppUpdateUiState,
  onCheckUpdate: () -> Unit,
  onDownloadUpdate: () -> Unit,
  onInstallUpdate: () -> Unit,
  onOpenReleasePage: () -> Unit,
) {
  val release = updateState.latestRelease
  val title = release?.let { "FocusWell ${it.tagName}" } ?: "Current ${BuildConfig.VERSION_NAME}"
  val supporting =
    updateState.error
      ?: updateState.message
      ?: "Check GitHub Releases for a signed APK update."
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f),
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          shape = CircleShape,
        ) {
          Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(title, style = MaterialTheme.typography.titleMedium)
          Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      if (updateState.downloading) {
        LinearProgressIndicator(
          progress = { ((updateState.progress ?: 0) / 100f).coerceIn(0f, 1f) },
          modifier = Modifier.fillMaxWidth(),
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilledTonalButton(
          onClick = onCheckUpdate,
          enabled = !updateState.checking && !updateState.downloading,
          modifier = Modifier.weight(1f).height(44.dp),
          shape = RoundedCornerShape(22.dp),
        ) {
          Text(if (updateState.checking) "Checking" else "Check")
        }
        when {
          updateState.downloadedApk != null -> {
            Button(onClick = onInstallUpdate, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(22.dp)) {
              Text("Install")
            }
          }
          updateState.selection != null -> {
            Button(
              onClick = onDownloadUpdate,
              enabled = !updateState.downloading,
              modifier = Modifier.weight(1f).height(44.dp),
              shape = RoundedCornerShape(22.dp),
            ) {
              Text(if (updateState.downloading) "${updateState.progress ?: 0}%" else "Download")
            }
          }
          release != null -> {
            OutlinedButton(onClick = onOpenReleasePage, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(22.dp)) {
              Text("Release")
            }
          }
        }
      }
      release?.body?.takeIf { it.isNotBlank() }?.lineSequence()?.firstOrNull()?.let { firstLine ->
        Text(firstLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
internal fun SettingsPushRegistrationRow(
  pushRegistrationState: PushRegistrationUiState,
  notificationPermissionGranted: Boolean,
  onEnablePush: () -> Unit,
  onDisablePush: () -> Unit,
) {
  val status = pushRegistrationState.status
  val checked = status.enabled
  val ready = status.enabled && status.hasFcmToken && notificationPermissionGranted
  val supporting =
    when {
      !status.enabled -> "Remote reminder delivery is off."
      !notificationPermissionGranted -> "Notification permission is missing."
      status.lastError != null -> status.lastError
      status.hasFcmToken -> "Registered for remote reminders."
      else -> "Register FCM and allow notifications."
    }
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        color =
          if (ready) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f)
          else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
        contentColor =
          if (ready) MaterialTheme.colorScheme.onSecondaryContainer
          else MaterialTheme.colorScheme.error,
        shape = CircleShape,
      ) {
        Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp))
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Push", style = MaterialTheme.typography.titleMedium)
        Text(
          supporting,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Switch(
        checked = checked,
        onCheckedChange = {
          if (!pushRegistrationState.refreshing) {
            if (it) onEnablePush() else onDisablePush()
          }
        },
        enabled = !pushRegistrationState.refreshing,
      )
    }
    if (pushRegistrationState.refreshing) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
  }
}
