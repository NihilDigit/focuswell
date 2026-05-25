package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.TagConfig
import kotlin.math.roundToInt

@Composable
internal fun PlanScreen(
  state: FocusWellUiState,
  onAddTag: (String, Double) -> Unit,
  onArchiveTag: (String) -> Unit,
  onUpdateTag: (String, String, Double) -> Unit,
  onAddBooleanTracker: (String, Double) -> Unit,
  onAddRuleTracker: (String, String, Double, Double) -> Unit,
  onArchiveTracker: (String) -> Unit,
  onUpdateManualTracker: (String, String, Double) -> Unit,
  onUpdateRuleTracker: (String, String, String, Double, Double) -> Unit,
) {
  val tags = state.tags.filter { it.archivedAt == null }
  val trackers = state.trackers.filter { it.archivedAt == null }
  var editingTagId by remember { mutableStateOf<String?>(null) }
  var editingTrackerId by remember { mutableStateOf<String?>(null) }
  var addingTag by remember { mutableStateOf(false) }
  var addingManualTracker by remember { mutableStateOf(false) }
  var addingRuleTracker by remember { mutableStateOf(false) }

  LazyColumn(
    contentPadding = PaddingValues(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item { Text("Plan", style = MaterialTheme.typography.headlineSmall) }
    item {
      PlanSectionHeader(
        title = "Tags",
        subtitle = "${tags.size} active focus tags",
        actionLabel = "Add tag",
        onAdd = { addingTag = true },
      )
    }
    if (tags.isEmpty()) {
      item { EmptyRecordText("No tags yet.") }
    } else {
      items(tags, key = { it.id }) { tag ->
        PlanTagRow(tag = tag, onClick = { editingTagId = tag.id })
      }
    }
    item {
      PlanSectionHeader(
        title = "Daily trackers",
        subtitle = "${trackers.size} active · ${compactMinutes(trackers.sumOf { it.rewardMinutes })} possible",
        actionLabel = "Add tracker",
        onAdd = { addingManualTracker = true },
        secondaryActionLabel = "Add rule",
        onSecondaryAdd = { addingRuleTracker = true },
      )
    }
    if (trackers.isEmpty()) {
      item { EmptyRecordText("No daily trackers yet.") }
    } else {
      items(trackers, key = { it.id }) { tracker ->
        PlanTrackerRow(tracker = tracker, onClick = { editingTrackerId = tracker.id })
      }
    }
  }

  tags.firstOrNull { it.id == editingTagId }?.let { tag ->
    TagPlanSheet(
      tag = tag,
      onDismiss = { editingTagId = null },
      onSave = { name, multiplier ->
        editingTagId = null
        onUpdateTag(tag.id, name, multiplier)
      },
      onArchive = {
        editingTagId = null
        onArchiveTag(tag.id)
      },
    )
  }
  trackers.firstOrNull { it.id == editingTrackerId }?.let { tracker ->
    TrackerPlanSheet(
      tracker = tracker,
      tags = tags,
      onDismiss = { editingTrackerId = null },
      onSaveManual = { label, reward ->
        editingTrackerId = null
        onUpdateManualTracker(tracker.id, label, reward)
      },
      onSaveRule = { label, tagName, targetMinutes, reward ->
        editingTrackerId = null
        onUpdateRuleTracker(tracker.id, label, tagName, targetMinutes, reward)
      },
      onArchive = {
        editingTrackerId = null
        onArchiveTracker(tracker.id)
      },
    )
  }
  if (addingTag) {
    TagPlanSheet(
      tag = null,
      onDismiss = { addingTag = false },
      onSave = { name, multiplier ->
        addingTag = false
        onAddTag(name, multiplier)
      },
      onArchive = null,
    )
  }
  if (addingManualTracker) {
    TrackerPlanSheet(
      tracker = null,
      tags = tags,
      ruleMode = false,
      onDismiss = { addingManualTracker = false },
      onSaveManual = { label, reward ->
        addingManualTracker = false
        onAddBooleanTracker(label, reward)
      },
      onSaveRule = { _, _, _, _ -> },
      onArchive = null,
    )
  }
  if (addingRuleTracker) {
    TrackerPlanSheet(
      tracker = null,
      tags = tags,
      ruleMode = true,
      onDismiss = { addingRuleTracker = false },
      onSaveManual = { _, _ -> },
      onSaveRule = { label, tagName, targetMinutes, reward ->
        addingRuleTracker = false
        onAddRuleTracker(label, tagName, targetMinutes, reward)
      },
      onArchive = null,
    )
  }
}

@Composable
internal fun PlanSectionHeader(
  title: String,
  subtitle: String,
  actionLabel: String,
  onAdd: () -> Unit,
  secondaryActionLabel: String? = null,
  onSecondaryAdd: (() -> Unit)? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SectionHeader(title = title, subtitle = subtitle)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      onSecondaryAdd?.let { secondary ->
        PlanIconAction(icon = Icons.Rounded.Timer, contentDescription = secondaryActionLabel ?: "Add rule", onClick = secondary)
      }
      PlanIconAction(icon = Icons.Rounded.Add, contentDescription = actionLabel, onClick = onAdd)
    }
  }
}

@Composable
internal fun PlanIconAction(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
  FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(48.dp), shape = CircleShape) {
    Icon(icon, contentDescription = contentDescription)
  }
}

@Composable
internal fun PlanTagRow(tag: TagConfig, onClick: () -> Unit) {
  PlanRow(
    icon = Icons.Rounded.Edit,
    iconTone = MaterialTheme.colorScheme.primary,
    title = tag.name,
    supporting = "${tag.multiplier.formatThree()}x focus multiplier",
    trailing = "Edit",
    onClick = onClick,
  )
}

@Composable
internal fun PlanTrackerRow(tracker: DailyTracker, onClick: () -> Unit) {
  val isRule = tracker.ruleTagName != null && tracker.ruleTargetMinutes != null
  val supporting =
    if (isRule) {
      "Rule · ${signedCompactMinutes(tracker.rewardMinutes)}"
    } else {
      "Manual · ${signedCompactMinutes(tracker.rewardMinutes)}"
    }
  PlanRow(
    icon = if (isRule) Icons.Rounded.Timer else Icons.Rounded.RadioButtonUnchecked,
    iconTone = if (isRule) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
    title = tracker.label,
    supporting = supporting,
    trailing = if (tracker.completed) "Done" else "Open",
    onClick = onClick,
  )
}

@Composable
internal fun PlanRow(
  icon: ImageVector,
  iconTone: androidx.compose.ui.graphics.Color,
  title: String,
  supporting: String,
  trailing: String,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = LedgerRowShape,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(color = iconTone.copy(alpha = 0.14f), contentColor = iconTone, shape = CircleShape, modifier = Modifier.size(38.dp)) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
          Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
    }
  }
}

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
internal fun PlanSheetContent(title: String, content: @Composable ColumnScope.() -> Unit) {
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
internal fun PlanSheetActions(
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
