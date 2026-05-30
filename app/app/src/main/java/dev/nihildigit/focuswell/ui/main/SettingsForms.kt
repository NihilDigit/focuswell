package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import dev.nihildigit.focuswell.domain.TagConfig

@Composable
internal fun SettingsAddTagForm(
  tagName: String,
  tagMultiplier: String,
  onTagNameChange: (String) -> Unit,
  onTagMultiplierChange: (String) -> Unit,
  onAddTag: () -> Unit,
) {
  SettingsCreateForm(
    title = "New tag",
    supporting = "Tags multiply focus earnings.",
    actionLabel = "Add tag",
    icon = Icons.Rounded.Add,
    enabled = tagName.isNotBlank(),
    onSubmit = onAddTag,
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        value = tagName,
        onValueChange = onTagNameChange,
        label = { Text("Name") },
        singleLine = true,
        modifier = Modifier.weight(1f),
      )
      OutlinedTextField(
        value = tagMultiplier,
        onValueChange = onTagMultiplierChange,
        label = { Text("Rate") },
        singleLine = true,
        suffix = { Text("x") },
        modifier = Modifier.width(118.dp),
      )
    }
  }
}

@Composable
internal fun SettingsAddBooleanTrackerForm(
  trackerLabel: String,
  trackerReward: String,
  onTrackerLabelChange: (String) -> Unit,
  onTrackerRewardChange: (String) -> Unit,
  onAddTracker: () -> Unit,
) {
  SettingsCreateForm(
    title = "New checklist item",
    supporting = "Manual daily completion.",
    actionLabel = "Add tracker",
    icon = Icons.Rounded.CheckCircle,
    enabled = trackerLabel.isNotBlank() && trackerReward.toDoubleOrNull() != null,
    onSubmit = onAddTracker,
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        value = trackerLabel,
        onValueChange = onTrackerLabelChange,
        label = { Text("Label") },
        singleLine = true,
        modifier = Modifier.weight(1f),
      )
      OutlinedTextField(
        value = trackerReward,
        onValueChange = onTrackerRewardChange,
        label = { Text("Reward") },
        singleLine = true,
        suffix = { Text("m") },
        modifier = Modifier.width(126.dp),
      )
    }
  }
}

@Composable
internal fun SettingsAddRuleTrackerForm(
  ruleLabel: String,
  ruleTag: String,
  ruleHours: String,
  ruleReward: String,
  tags: List<TagConfig>,
  onRuleLabelChange: (String) -> Unit,
  onRuleTagChange: (String) -> Unit,
  onRuleHoursChange: (String) -> Unit,
  onRuleRewardChange: (String) -> Unit,
  onAddRuleTracker: () -> Unit,
) {
  var showTagPicker by remember { mutableStateOf(false) }
  SettingsCreateForm(
    title = "New rule tracker",
    supporting = "Auto-completes from focused time.",
    actionLabel = "Add rule",
    icon = Icons.Rounded.Timer,
    enabled = ruleLabel.isNotBlank() && tags.any { it.name == ruleTag } && ruleHours.toDoubleOrNull() != null && ruleReward.toDoubleOrNull() != null,
    onSubmit = onAddRuleTracker,
  ) {
    OutlinedTextField(
      value = ruleLabel,
      onValueChange = onRuleLabelChange,
      label = { Text("Label") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
      TagPickerField(
        selectedTag = ruleTag,
        enabled = tags.isNotEmpty(),
        onClick = { showTagPicker = true },
        modifier = Modifier.weight(1f),
      )
      OutlinedTextField(
        value = ruleHours,
        onValueChange = onRuleHoursChange,
        label = { Text("Target") },
        singleLine = true,
        suffix = { Text("h") },
        modifier = Modifier.width(126.dp),
      )
      OutlinedTextField(
        value = ruleReward,
        onValueChange = onRuleRewardChange,
        label = { Text("Reward") },
        singleLine = true,
        suffix = { Text("m") },
        modifier = Modifier.width(126.dp),
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
              selected = tag.name == ruleTag,
              onClick = {
                onRuleTagChange(tag.name)
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
internal fun TagPickerField(
  selectedTag: String,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
    modifier = modifier.height(64.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
        Text("Tag", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
          selectedTag.ifBlank { "No tags" },
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Icon(Icons.Rounded.ExpandMore, contentDescription = null)
    }
  }
}

@Composable
internal fun TagPickerOption(
  tag: TagConfig,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(tag.name, style = MaterialTheme.typography.titleMedium)
        Text("${tag.multiplier.formatThree()}x multiplier", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = null)
    }
  }
}

@Composable
internal fun SettingsCreateForm(
  title: String,
  supporting: String,
  actionLabel: String,
  icon: ImageVector,
  enabled: Boolean,
  onSubmit: () -> Unit,
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          shape = CircleShape,
        ) {
          Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp).size(18.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
          Text(title, style = MaterialTheme.typography.titleMedium)
          Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      content()
      FilledTonalButton(
        onClick = onSubmit,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(24.dp),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(actionLabel)
      }
    }
  }
}
