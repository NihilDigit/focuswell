package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IdeaCaptureSheet(
  onDismiss: () -> Unit,
  onSave: (String) -> Unit,
) {
  var text by remember { mutableStateOf("") }
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
    Column(
      modifier = Modifier.padding(horizontal = 20.dp).imePadding().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("Capture idea", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Idea") },
        minLines = 3,
        maxLines = 5,
        modifier = Modifier.fillMaxWidth(),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(54.dp), shape = ControlStartShape) {
          Text("Cancel")
        }
        Button(
          enabled = text.isNotBlank(),
          onClick = { onSave(text) },
          modifier = Modifier.weight(1f).height(54.dp),
          shape = ControlEndShape,
        ) {
          Text("Save")
        }
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}
