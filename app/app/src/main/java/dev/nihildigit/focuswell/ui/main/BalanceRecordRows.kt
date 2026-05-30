package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.LeisureRecord
import kotlin.math.roundToInt

@Composable
internal fun BalanceRecordRow(
  item: BalanceRecordItem,
  onEditFocusRecord: (String) -> Unit,
  onShowLeisureRecord: (String) -> Unit,
) {
  when (item) {
    is BalanceRecordItem.Focus ->
      BalanceFocusRecordRow(
        record = item.record,
        onClick = { onEditFocusRecord(item.record.id) },
      )
    is BalanceRecordItem.Leisure ->
      BalanceLeisureRecordRow(record = item.record, onClick = { onShowLeisureRecord(item.record.id) })
    is BalanceRecordItem.Adjustment -> BalanceAdjustmentRow(entry = item.entry)
  }
}

@Composable
internal fun BalanceFocusRecordRow(
  record: FocusRecord,
  onClick: () -> Unit,
) {
  val resultParts = remember(record.id) { parseOutcomeResult(record.result) }
  BalanceRecordSurface(onClick = onClick) {
    BalanceDeltaText(delta = record.earnedMinutes, color = MaterialTheme.colorScheme.primary)
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        InlineRecordTypeIcon(icon = Icons.Rounded.Timer, color = MaterialTheme.colorScheme.primary)
        Text(record.task, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Text(
        "${record.type.label} · ${record.tagName ?: "Untagged"} · ${compactMinutes(record.activeDurationMinutes)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      FocusOutcomeMark(outcome = resultParts.first, note = resultParts.second)
    }
    Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BalanceFocusRecordSheet(
  record: FocusRecord,
  onDismiss: () -> Unit,
  onDelete: () -> Unit,
  onUpdate: (String, Double) -> Unit,
) {
  val resultParts = remember(record.id) { parseOutcomeResult(record.result) }
  var outcome by remember(record.id) { mutableStateOf(resultParts.first) }
  var note by remember(record.id) { mutableStateOf(resultParts.second) }
  var minutes by remember(record.id) { mutableStateOf(record.activeDurationMinutes.roundToInt().toString()) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  FocusRecordEditSheet(
    record = record,
    outcome = outcome,
    note = note,
    minutes = minutes,
    onOutcomeChange = { outcome = it },
    onNoteChange = { note = it },
    onMinutesChange = { minutes = it },
    sheetState = sheetState,
    onDismiss = onDismiss,
    onSave = { onUpdate(formatOutcomeResult(outcome, note), minutes.toDoubleOrNull() ?: record.activeDurationMinutes) },
    onDelete = onDelete,
  )
}

@Composable
internal fun FocusOutcomeMark(outcome: String, note: String) {
  val (icon, color) = focusOutcomeVisual(outcome)
  Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
    Text(
      if (note.isBlank()) outcome else "$outcome · $note",
      style = MaterialTheme.typography.labelMedium,
      color = color,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BalanceLeisureRecordRow(record: LeisureRecord, onClick: () -> Unit) {
  BalanceRecordSurface(onClick = onClick) {
    BalanceDeltaText(delta = -record.costMinutes, color = MaterialTheme.colorScheme.tertiary)
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        InlineRecordTypeIcon(icon = Icons.Rounded.Bedtime, color = MaterialTheme.colorScheme.tertiary)
        Text("Leisure", style = MaterialTheme.typography.titleMedium)
      }
      Text(
        "${compactMinutes(record.elapsedMinutes)} real · ${compactMinutes(record.costMinutes)} charged",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(localRecordTime(record.endedAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BalanceLeisureRecordSheet(
  record: LeisureRecord,
  onDismiss: () -> Unit,
  onDelete: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("Leisure record", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      BalanceDeltaPreview(original = 0.0, updated = -record.costMinutes, delta = -record.costMinutes)
      Button(
        onClick = onDelete,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = ControlEndShape,
      ) {
        Icon(Icons.Rounded.Delete, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Delete record")
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
internal fun BalanceAdjustmentRow(entry: LedgerEntry) {
  BalanceRecordSurface(onClick = {}, enabled = false) {
    BalanceDeltaText(
      delta = entry.deltaMinutes,
      color =
        when {
          entry.deltaMinutes > 0.0 -> MaterialTheme.colorScheme.primary
          entry.deltaMinutes < 0.0 -> MaterialTheme.colorScheme.tertiary
          else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        InlineRecordTypeIcon(icon = Icons.Rounded.Edit, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(entry.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      entry.note?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Text(localRecordTime(entry.createdAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
internal fun InlineRecordTypeIcon(icon: ImageVector, color: Color) {
  Surface(
    color = color.copy(alpha = 0.14f),
    contentColor = color,
    shape = CircleShape,
    modifier = Modifier.size(24.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
    }
  }
}

@Composable
internal fun BalanceRecordSurface(
  onClick: () -> Unit,
  enabled: Boolean = true,
  content: @Composable RowScope.() -> Unit,
) {
  Surface(
    onClick = onClick,
    enabled = enabled,
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = LedgerRowShape,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      content = content,
    )
  }
}

@Composable
internal fun BalanceDeltaText(delta: Double, color: Color) {
  Text(
    signedMinutes(delta),
    style = tabularNumbers(MaterialTheme.typography.titleMedium),
    fontWeight = FontWeight.Bold,
    color = color,
    modifier = Modifier.width(76.dp),
  )
}
