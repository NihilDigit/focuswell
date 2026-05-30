package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.SessionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StartFocusSheet(
  state: FocusWellUiState,
  onDismiss: () -> Unit,
  onStart: (String, SessionType, String?) -> Unit,
) {
  var task by remember { mutableStateOf("") }
  var type by remember { mutableStateOf(SessionType.Input) }
  val activeTags = state.tags.filter { it.archivedAt == null }.ifEmpty { state.tags }
  var tagId by remember { mutableStateOf<String?>(null) }
  val tag = tagId?.let { selectedId -> activeTags.firstOrNull { it.id == selectedId } }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val recentTasks =
    remember(state.focusRecords) {
      state.focusRecords
        .asSequence()
        .filter { it.deletedAt == null }
        .sortedByDescending { it.startedAt }
        .map { it.task.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(5)
        .toList()
    }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier
          .padding(horizontal = 20.dp, vertical = 12.dp)
          .imePadding()
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("Start Focus", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      OutlinedTextField(
        value = task,
        onValueChange = { task = it },
        label = { Text("Task") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      if (recentTasks.isNotEmpty()) {
        RecentFocusTaskRow(
          tasks = recentTasks,
          selectedTask = task.trim(),
          onSelectTask = { task = it },
        )
      }
      ConnectedSessionTypeGroup(selected = type, onSelected = { type = it })
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = tagId == null,
          onClick = { tagId = null },
          label = { Text("No tag") },
        )
        activeTags.forEach { tag ->
          FilterChip(
            selected = tagId == tag.id,
            onClick = { tagId = tag.id },
            label = { Text(tag.name) },
          )
        }
      }
      CalmPanel {
        StartFocusSettlementPreview(
          type = type,
          tagName = tag?.name ?: "No tag",
          tagMultiplier = tag?.multiplier ?: 1.0,
        )
      }
      Button(
        enabled = task.trim().isNotEmpty(),
        onClick = { onStart(task, type, tagId) },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = FocusActionShape,
      ) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Start")
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}
