package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.Idea
import dev.nihildigit.focuswell.domain.IdeaChecklistItem
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IdeaCreateSheet(
  onDismiss: () -> Unit,
  onSave: (String) -> Unit,
) {
  var text by remember { mutableStateOf("") }
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
    Column(
      modifier =
        Modifier
          .padding(horizontal = 20.dp)
          .imePadding()
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("Add idea", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Idea") },
        minLines = 3,
        maxLines = 6,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IdeaEditSheet(
  idea: Idea,
  onDismiss: () -> Unit,
  onSave: (String, List<IdeaChecklistItem>) -> Unit,
  onArchive: () -> Unit,
) {
  var text by remember(idea.id) { mutableStateOf(idea.text) }
  var checklist by remember(idea.id) { mutableStateOf(idea.checklist) }
  var taskText by remember(idea.id) { mutableStateOf("") }

  fun addTask() {
    val trimmed = taskText.trim()
    if (trimmed.isEmpty()) return
    checklist = checklist + newIdeaChecklistItem(text = trimmed, index = checklist.size)
    taskText = ""
  }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
    Column(
      modifier =
        Modifier
          .padding(horizontal = 20.dp)
          .imePadding()
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("Edit idea", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Idea") },
        minLines = 3,
        maxLines = 6,
        modifier = Modifier.fillMaxWidth(),
      )
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Small tasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        checklist.forEach { item ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Checkbox(
              checked = item.checked,
              onCheckedChange = { checked ->
                checklist = checklist.map { if (it.id == item.id) it.copy(checked = checked) else it }
              },
            )
            OutlinedTextField(
              value = item.text,
              onValueChange = { value ->
                checklist = checklist.map { if (it.id == item.id) it.copy(text = value) else it }
              },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            IconButton(
              onClick = { checklist = checklist.filterNot { it.id == item.id } },
            ) {
              Icon(Icons.Rounded.Delete, contentDescription = "Delete task")
            }
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = taskText,
            onValueChange = { taskText = it },
            label = { Text("New task") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          IconButton(
            enabled = taskText.isNotBlank(),
            onClick = ::addTask,
          ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add task")
          }
        }
        if (checklist.isEmpty()) {
          Text(
            "Add a small next step if this idea needs one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onArchive, modifier = Modifier.weight(1f).height(54.dp), shape = ControlStartShape) {
          Icon(Icons.Rounded.Archive, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Archive")
        }
        Button(
          enabled = text.isNotBlank(),
          onClick = {
            val pendingTask = taskText.trim()
            val savedChecklist =
              if (pendingTask.isEmpty()) {
                checklist
              } else {
                checklist + newIdeaChecklistItem(text = pendingTask, index = checklist.size)
              }
            onSave(text, savedChecklist)
          },
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

@OptIn(ExperimentalTime::class)
internal fun newIdeaChecklistItem(
  text: String,
  index: Int,
  createdAt: Instant = Clock.System.now(),
): IdeaChecklistItem =
  IdeaChecklistItem(
    id = "task-${createdAt.toEpochMilliseconds()}-$index",
    text = text,
  )
