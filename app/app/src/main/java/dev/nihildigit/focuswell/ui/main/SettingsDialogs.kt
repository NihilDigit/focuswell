package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun ExportJsonDialog(
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Export JSON backup?") },
    text = {
      Text("This creates a complete FocusWell backup file with records, reserve history, ideas, trackers, tags, rules, and ledger entries.")
    },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text("Export")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

@Composable
internal fun ImportJsonDialog(
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Import JSON backup?") },
    text = { Text("Import replaces the current FocusWell state on this device. Export first if you may need these records later.") },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text("Import")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

@Composable
internal fun CloudSyncDecisionDialog(
  decision: CloudSyncDecision,
  onUpload: () -> Unit,
  onRestore: () -> Unit,
  onDismiss: () -> Unit,
) {
  val isUpload = decision.kind == CloudSyncDecisionKind.Upload
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        if (isUpload) "Update cloud backup?" else "Restore cloud backup?"
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
          if (isUpload) {
            "This device has newer FocusWell data. Updating cloud replaces the cloud backup with this device."
          } else {
            "The cloud backup is newer. Restoring replaces the FocusWell data on this device."
          },
          style = MaterialTheme.typography.bodyMedium,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          CloudSyncSnapshotRow(
            label = "This device",
            icon = Icons.Rounded.PhoneAndroid,
            time = decision.localUpdatedAt.cloudSyncTimeText(),
            status = if (isUpload) "Newer" else "Older",
            highlighted = isUpload,
          )
          CloudSyncSnapshotRow(
            label = "Cloud backup",
            icon = Icons.Rounded.Cloud,
            time = decision.cloudMetadata.updatedAtUtc.cloudSyncTimeText(),
            status = if (isUpload) "Older" else "Newer",
            highlighted = !isUpload,
          )
        }
        Text(
          "Backup app: FocusWell ${decision.cloudMetadata.appVersion}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick =
          when (decision.kind) {
            CloudSyncDecisionKind.Upload -> onUpload
            CloudSyncDecisionKind.Restore -> onRestore
          },
      ) {
        Text(
          if (isUpload) "Update cloud" else "Restore here"
        )
      }
    },
    dismissButton = {
      Row {
        TextButton(onClick = onDismiss) { Text("Not now") }
      }
    },
  )
}

@Composable
private fun CloudSyncSnapshotRow(
  label: String,
  icon: ImageVector,
  time: String,
  status: String,
  highlighted: Boolean,
) {
  val containerColor =
    if (highlighted) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHigh
  val contentColor =
    if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface
  Surface(
    color = containerColor,
    contentColor = contentColor,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        color = contentColor.copy(alpha = 0.12f),
        contentColor = contentColor,
        shape = CircleShape,
      ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp).size(20.dp))
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(status, style = MaterialTheme.typography.bodySmall)
      }
      Text(time, style = tabularNumbers(MaterialTheme.typography.labelLarge), fontWeight = FontWeight.SemiBold)
    }
  }
}

private val cloudSyncTimeFormatter: DateTimeFormatter =
  DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault())

private fun Instant.cloudSyncTimeText(): String =
  atZone(ZoneId.systemDefault()).format(cloudSyncTimeFormatter)

@Composable
internal fun CloudSyncMessageDialog(
  state: CloudSyncUiState,
  onDismiss: () -> Unit,
) {
  if (state.message == null && state.error == null) return
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (state.error == null) "Cloud sync" else "Cloud sync failed") },
    text = { Text(state.error ?: state.message.orEmpty()) },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
  )
}
