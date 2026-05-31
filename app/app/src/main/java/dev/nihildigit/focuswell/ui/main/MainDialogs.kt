package dev.nihildigit.focuswell.ui.main

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
internal fun ImportErrorDialog(
  error: String,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Import failed") },
    text = { Text(error) },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
  )
}

@Composable
internal fun UsageAccessPromptDialog(
  context: Context,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Enable app correction") },
    text = {
      Text(
        "FocusWell can use Android usage access to review phone use during settlement. Usage data stays on this device.",
      )
    },
    confirmButton = {
      TextButton(
        onClick = {
          onDismiss()
          context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        },
      ) {
        Text("Open settings")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Not now")
      }
    },
  )
}
