package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ClearAllDataScreen(
  phrase: String,
  onPhraseChange: (String) -> Unit,
  onExport: () -> Unit,
  onCancel: () -> Unit,
  onConfirm: () -> Unit,
) {
  LazyColumn(
    contentPadding = PaddingValues(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      CalmPanel {
        Text("Clear all data", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
          "This removes local records, reserve history, trackers, tag settings, reminder registration, and device identity.",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    item {
      CalmPanel {
        Text("Export first", style = MaterialTheme.typography.titleLarge)
        Text("Save a JSON backup before clearing if you may need these records later.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilledTonalButton(onClick = onExport, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(24.dp)) {
          Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(8.dp))
          Text("Export JSON")
        }
      }
    }
    item {
      CalmPanel {
        Text("Confirm", style = MaterialTheme.typography.titleLarge)
        Text("Type CLEAR to reset FocusWell on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
          value = phrase,
          onValueChange = onPhraseChange,
          label = { Text("Confirmation") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(52.dp), shape = ControlStartShape) {
            Text("Cancel")
          }
          Button(
            onClick = onConfirm,
            enabled = phrase == "CLEAR",
            modifier = Modifier.weight(1f).height(52.dp),
            shape = ControlEndShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          ) {
            Text("Clear")
          }
        }
      }
    }
  }
}
