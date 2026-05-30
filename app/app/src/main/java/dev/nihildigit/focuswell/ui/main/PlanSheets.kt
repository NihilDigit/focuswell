package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.TagConfig
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TagPlanSheet(
  tag: TagConfig?,
  onDismiss: () -> Unit,
  onSave: (String, Double) -> Unit,
  onArchive: (() -> Unit)?,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var name by remember(tag?.id) { mutableStateOf(tag?.name.orEmpty()) }
  var multiplier by remember(tag?.id) { mutableStateOf(tag?.multiplier?.formatThree() ?: "1.0") }
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    PlanSheetContent(title = if (tag == null) "Add tag" else "Edit tag") {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(value = multiplier, onValueChange = { multiplier = it }, label = { Text("Rate") }, singleLine = true, suffix = { Text("x") }, modifier = Modifier.width(118.dp))
      }
      PlanSheetActions(
        enabled = name.isNotBlank() && multiplier.toDoubleOrNull() != null,
        saveLabel = if (tag == null) "Add tag" else "Save tag",
        onSave = { onSave(name, multiplier.toDoubleOrNull() ?: 1.0) },
        onArchive = onArchive,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackerPlanSheet(
  tracker: DailyTracker?,
  tags: List<TagConfig>,
  ruleMode: Boolean = tracker?.ruleTagName != null,
  onDismiss: () -> Unit,
  onSaveManual: (String, Double) -> Unit,
  onSaveRule: (String, String, Double, Double) -> Unit,
  onArchive: (() -> Unit)?,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val defaultReward = if (ruleMode) "60" else "15"
  var label by remember(tracker?.id) { mutableStateOf(tracker?.label.orEmpty()) }
  var reward by remember(tracker?.id, ruleMode) { mutableStateOf(tracker?.rewardMinutes?.roundToInt()?.toString() ?: defaultReward) }
  var tagName by remember(tracker?.id, tags.map { it.name }) { mutableStateOf(tracker?.ruleTagName ?: tags.firstOrNull()?.name.orEmpty()) }
  var targetHours by remember(tracker?.id) { mutableStateOf(tracker?.ruleTargetMinutes?.div(60.0)?.formatOne() ?: "3") }
  var showTagPicker by remember { mutableStateOf(false) }
  LaunchedEffect(tags.map { it.name }) {
    if (ruleMode && tags.none { it.name == tagName }) tagName = tags.firstOrNull()?.name.orEmpty()
  }
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    PlanSheetContent(title = if (tracker == null) if (ruleMode) "Add rule tracker" else "Add tracker" else "Edit tracker") {
      OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") }, singleLine = true, modifier = Modifier.fillMaxWidth())
      if (ruleMode) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
          TagPickerField(selectedTag = tagName, enabled = tags.isNotEmpty(), onClick = { showTagPicker = true }, modifier = Modifier.weight(1f))
          OutlinedTextField(value = targetHours, onValueChange = { targetHours = it }, label = { Text("Target") }, singleLine = true, suffix = { Text("h") }, modifier = Modifier.width(126.dp))
        }
      }
      OutlinedTextField(value = reward, onValueChange = { reward = it }, label = { Text("Reward") }, singleLine = true, suffix = { Text("m") }, modifier = Modifier.fillMaxWidth())
      PlanSheetActions(
        enabled =
          label.isNotBlank() &&
            reward.toDoubleOrNull() != null &&
            (!ruleMode || (tagName.isNotBlank() && targetHours.toDoubleOrNull() != null)),
        saveLabel = if (tracker == null) "Add tracker" else "Save tracker",
        onSave = {
          if (ruleMode) {
            onSaveRule(label, tagName, (targetHours.toDoubleOrNull() ?: 3.0) * 60.0, reward.toDoubleOrNull() ?: 60.0)
          } else {
            onSaveManual(label, reward.toDoubleOrNull() ?: 15.0)
          }
        },
        onArchive = onArchive,
      )
    }
  }
  if (showTagPicker) {
    AlertDialog(
      onDismissRequest = { showTagPicker = false },
      title = { Text("Choose tag") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          tags.forEach { tag ->
            TagPickerOption(
              tag = tag,
              selected = tag.name == tagName,
              onClick = {
                tagName = tag.name
                showTagPicker = false
              },
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showTagPicker = false }) {
          Text("Done")
        }
      },
    )
  }
}

@Composable
private fun PlanSheetContent(title: String, content: @Composable ColumnScope.() -> Unit) {
  Column(
    modifier = Modifier.padding(horizontal = 20.dp).imePadding().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    content()
    Spacer(Modifier.height(16.dp))
  }
}

@Composable
private fun PlanSheetActions(
  enabled: Boolean,
  saveLabel: String,
  onSave: () -> Unit,
  onArchive: (() -> Unit)?,
) {
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
    Button(enabled = enabled, onClick = onSave, modifier = Modifier.weight(1f).height(54.dp), shape = ControlEndShape) {
      Text(saveLabel)
    }
    onArchive?.let {
      OutlinedButton(
        onClick = it,
        modifier = Modifier.height(54.dp),
        shape = ControlStartShape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
      ) {
        Icon(Icons.Rounded.Archive, contentDescription = null)
      }
    }
  }
}
