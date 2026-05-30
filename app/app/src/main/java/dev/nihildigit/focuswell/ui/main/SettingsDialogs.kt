package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

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
    text = { Text("Import replaces the current FocusWell state on this device. Export first if you may need the current records later.") },
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
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        when (decision.kind) {
          CloudSyncDecisionKind.Upload -> "Upload local backup?"
          CloudSyncDecisionKind.Restore -> "Restore cloud backup?"
        }
      )
    },
    text = {
      Text(
        "Local updated: ${decision.localUpdatedAt}\nCloud updated: ${decision.cloudMetadata.updatedAtUtc}\nCloud app: ${decision.cloudMetadata.appVersion}"
      )
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
          when (decision.kind) {
            CloudSyncDecisionKind.Upload -> "Upload"
            CloudSyncDecisionKind.Restore -> "Restore"
          }
        )
      }
    },
    dismissButton = {
      Row {
        TextButton(onClick = onDismiss) { Text("Cancel") }
      }
    },
  )
}

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
