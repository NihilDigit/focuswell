package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.FocusWellUiState

@Composable
internal fun RecordsScreen(
  state: FocusWellUiState,
  onDeleteFocusRecord: (String) -> Unit,
  onUpdateFocusRecord: (String, String, Double) -> Unit,
  onDeleteLeisureRecord: (String) -> Unit,
) {
  var tab by remember { mutableStateOf("Focus") }
  val tabs = listOf("Focus", "Leisure", "Trackers", "Tags")
  BoxWithConstraints {
    val useTwoColumns = maxWidth >= 620.dp
    LazyColumn(
      contentPadding = PaddingValues(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item { SectionHeader(title = "History", subtitle = "Past sessions and editable records") }
      item { HistoryTabBar(tabs = tabs, selected = tab, onSelected = { tab = it }) }
      when (tab) {
        "Focus" -> {
          val records = state.focusRecords.filter { it.deletedAt == null }
          if (records.isEmpty()) item { EmptyRecordText("No focus sessions yet.") }
          if (useTwoColumns) {
            records.chunked(2).forEach { rowRecords ->
              item(key = rowRecords.joinToString { it.id }) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                  rowRecords.forEach { record ->
                    FocusRecordRow(
                      record = record,
                      onDelete = { onDeleteFocusRecord(record.id) },
                      onUpdate = { result, minutes -> onUpdateFocusRecord(record.id, result, minutes) },
                      modifier = Modifier.weight(1f),
                    )
                  }
                  if (rowRecords.size == 1) Spacer(Modifier.weight(1f))
                }
              }
            }
          } else {
            items(records, key = { it.id }) { record ->
              FocusRecordRow(
                record = record,
                onDelete = { onDeleteFocusRecord(record.id) },
                onUpdate = { result, minutes -> onUpdateFocusRecord(record.id, result, minutes) },
              )
            }
          }
        }

        "Leisure" -> {
          val records = state.leisureRecords.filter { it.deletedAt == null }
          if (records.isEmpty()) item { EmptyRecordText("No leisure sessions yet.") }
          if (useTwoColumns) {
            records.chunked(2).forEach { rowRecords ->
              item(key = rowRecords.joinToString { it.id }) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                  rowRecords.forEach { record ->
                    LeisureRecordRow(
                      record = record,
                      onDelete = { onDeleteLeisureRecord(record.id) },
                      modifier = Modifier.weight(1f),
                    )
                  }
                  if (rowRecords.size == 1) Spacer(Modifier.weight(1f))
                }
              }
            }
          } else {
            items(records, key = { it.id }) { record ->
              LeisureRecordRow(record = record, onDelete = { onDeleteLeisureRecord(record.id) })
            }
          }
        }

        "Trackers" -> {
          items(state.trackers.filter { it.archivedAt == null }, key = { it.id }) { tracker ->
            CalmPanel {
              Text(tracker.label, style = MaterialTheme.typography.titleMedium)
              Text(tracker.progressLabel ?: if (tracker.completed) "Done" else "Open")
            }
          }
        }

        "Tags" -> {
          items(state.tags.filter { it.archivedAt == null }, key = { it.id }) { tag ->
            CalmPanel {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(tag.name, style = MaterialTheme.typography.titleMedium)
                StatusBadge("${tag.multiplier}x", MaterialTheme.colorScheme.secondary)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun HistoryTabBar(
  tabs: List<String>,
  selected: String,
  onSelected: (String) -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      tabs.forEach { label ->
        val isSelected = selected == label
        Surface(
          onClick = { onSelected(label) },
          color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
          contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.weight(if (isSelected) 1.08f else 1f).height(46.dp),
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
              label,
              style = MaterialTheme.typography.labelLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}
