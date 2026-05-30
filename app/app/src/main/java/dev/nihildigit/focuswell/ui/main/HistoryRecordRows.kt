package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.LeisureRecord
import kotlin.math.roundToInt

internal data class FocusRecordEditBalance(
  val activeMinutes: Double,
  val updatedEarnedMinutes: Double,
  val deltaMinutes: Double,
)

internal fun focusRecordEditBalance(
  currentActiveMinutes: Double,
  currentEarnedMinutes: Double,
  typeRate: Double,
  tagMultiplier: Double,
  minutesText: String,
): FocusRecordEditBalance {
  val activeMinutes = minutesText.toDoubleOrNull() ?: currentActiveMinutes
  val updatedEarnedMinutes = activeMinutes * typeRate * tagMultiplier
  return FocusRecordEditBalance(
    activeMinutes = activeMinutes,
    updatedEarnedMinutes = updatedEarnedMinutes,
    deltaMinutes = updatedEarnedMinutes - currentEarnedMinutes,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FocusRecordRow(
  record: FocusRecord,
  onDelete: () -> Unit,
  onUpdate: (String, Double) -> Unit,
  modifier: Modifier = Modifier,
) {
  var editing by remember { mutableStateOf(false) }
  val resultParts = remember(record.id) { parseOutcomeResult(record.result) }
  var outcome by remember(record.id) { mutableStateOf(resultParts.first) }
  var note by remember(record.id) { mutableStateOf(resultParts.second) }
  var minutes by remember(record.id) { mutableStateOf(record.activeDurationMinutes.roundToInt().toString()) }
  val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = LedgerRowShape,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        signedMinutes(record.earnedMinutes),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (record.earnedMinutes > 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.width(76.dp),
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          "${record.type.label} · ${record.tagName ?: "Untagged"}",
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(record.task, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          "${compactMinutes(record.activeDurationMinutes)} · ${record.result}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { editing = true }, modifier = Modifier.size(48.dp)) {
          Icon(
            Icons.Rounded.Edit,
            contentDescription = "Edit",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
          )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
          Icon(
            Icons.Rounded.Delete,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(22.dp),
          )
        }
      }
    }
  }
  if (editing) {
    FocusRecordEditSheet(
      record = record,
      outcome = outcome,
      note = note,
      minutes = minutes,
      onOutcomeChange = { outcome = it },
      onNoteChange = { note = it },
      onMinutesChange = { minutes = it },
      sheetState = editSheetState,
      onDismiss = { editing = false },
      onSave = {
        editing = false
        onUpdate(formatOutcomeResult(outcome, note), minutes.toDoubleOrNull() ?: record.activeDurationMinutes)
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FocusRecordEditSheet(
  record: FocusRecord,
  outcome: String,
  note: String,
  minutes: String,
  onOutcomeChange: (String) -> Unit,
  onNoteChange: (String) -> Unit,
  onMinutesChange: (String) -> Unit,
  sheetState: SheetState,
  onDismiss: () -> Unit,
  onSave: () -> Unit,
  onDelete: (() -> Unit)? = null,
) {
  val balance =
    focusRecordEditBalance(
      currentActiveMinutes = record.activeDurationMinutes,
      currentEarnedMinutes = record.earnedMinutes,
      typeRate = record.typeRate,
      tagMultiplier = record.tagMultiplier,
      minutesText = minutes,
    )
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier
          .padding(horizontal = 20.dp)
          .imePadding()
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Edit focus record", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
          "${record.type.label} · ${record.tagName ?: "Untagged"}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      OutlinedTextField(
        value = minutes,
        onValueChange = onMinutesChange,
        label = { Text("Active minutes") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      FocusOutcomeOptions.chunked(2).forEach { rowOptions ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          rowOptions.forEach { option ->
            ResultChoice(
              label = option,
              selected = outcome == option,
              onClick = { onOutcomeChange(option) },
              modifier = Modifier.weight(1f),
            )
          }
          if (rowOptions.size == 1) {
            Spacer(Modifier.weight(1f))
          }
        }
      }
      OutlinedTextField(
        value = note,
        onValueChange = onNoteChange,
        label = { Text("Optional note") },
        placeholder = { Text(outcome) },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier.fillMaxWidth(),
      )
      BalanceDeltaPreview(
        original = record.earnedMinutes,
        updated = balance.updatedEarnedMinutes,
        delta = balance.deltaMinutes,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(54.dp), shape = ControlStartShape) {
          Text("Cancel")
        }
        Button(
          enabled = outcome.isNotBlank() && minutes.toDoubleOrNull() != null,
          onClick = onSave,
          modifier = Modifier.weight(1f).height(54.dp),
          shape = ControlEndShape,
        ) {
          Text("Apply change")
        }
      }
      onDelete?.let { delete ->
        OutlinedButton(
          onClick = delete,
          modifier = Modifier.fillMaxWidth().height(52.dp),
          shape = CalmPanelShape,
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
          Icon(Icons.Rounded.Delete, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Delete record")
        }
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
internal fun BalanceDeltaPreview(original: Double, updated: Double, delta: Double) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = CalmPanelShape,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Original earned", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(signedMinutes(original), fontWeight = FontWeight.SemiBold)
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("New earned", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(signedMinutes(updated), fontWeight = FontWeight.SemiBold)
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Balance delta", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
          signedMinutes(delta),
          color =
            when {
              delta > 0.0 -> MaterialTheme.colorScheme.primary
              delta < 0.0 -> MaterialTheme.colorScheme.error
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
internal fun LeisureRecordRow(record: LeisureRecord, onDelete: () -> Unit, modifier: Modifier = Modifier) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = LedgerRowShape,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        signedMinutes(-record.costMinutes),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.width(76.dp),
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("Leisure", style = MaterialTheme.typography.titleMedium)
        Text(
          "${compactMinutes(record.elapsedMinutes)} elapsed",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
        Icon(
          Icons.Rounded.Delete,
          contentDescription = "Delete",
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(22.dp),
        )
      }
    }
  }
}

@Composable
internal fun EmptyRecordText(text: String) {
  Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
