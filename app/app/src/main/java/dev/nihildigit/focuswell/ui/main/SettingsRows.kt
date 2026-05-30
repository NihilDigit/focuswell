package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.DailyTracker

@Composable
internal fun SettingsRuleRow(
  title: String,
  value: String,
  supporting: String,
  icon: ImageVector,
) {
  Row(
    modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      shape = CircleShape,
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp))
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(
      value,
      style = tabularNumbers(MaterialTheme.typography.titleMedium),
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.End,
    )
  }
}

@Composable
internal fun SettingsListRow(
  title: String,
  supporting: String,
  onArchive: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      FilledTonalButton(onClick = onArchive, modifier = Modifier.width(132.dp).height(44.dp), shape = RoundedCornerShape(22.dp)) {
        Icon(Icons.Rounded.Archive, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Archive", maxLines = 1)
      }
    }
  }
}

@Composable
internal fun SettingsTrackerRow(
  tracker: DailyTracker,
  onRewardChange: (Double) -> Unit,
  onArchive: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().heightIn(min = 78.dp).padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(tracker.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          tracker.progressLabel ?: if (tracker.ruleTagName != null) "Rule" else "Manual",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      RewardStepper(
        rewardMinutes = tracker.rewardMinutes,
        onRewardChange = onRewardChange,
      )
      IconButton(onClick = onArchive, modifier = Modifier.size(40.dp)) {
        Icon(Icons.Rounded.Archive, contentDescription = "Archive", modifier = Modifier.size(20.dp))
      }
    }
  }
}

@Composable
internal fun RewardStepper(
  rewardMinutes: Double,
  onRewardChange: (Double) -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = CircleShape,
    modifier = Modifier.height(40.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      IconButton(
        onClick = { onRewardChange((rewardMinutes - 5.0).coerceAtLeast(0.0)) },
        modifier = Modifier.size(32.dp),
      ) {
        Icon(Icons.Rounded.Remove, contentDescription = "Decrease reward", modifier = Modifier.size(18.dp))
      }
      Text(
        compactMinutes(rewardMinutes),
        style = tabularNumbers(MaterialTheme.typography.labelLarge),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.width(38.dp),
        textAlign = TextAlign.Center,
      )
      IconButton(
        onClick = { onRewardChange(rewardMinutes + 5.0) },
        modifier = Modifier.size(32.dp),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = "Increase reward", modifier = Modifier.size(18.dp))
      }
    }
  }
}

@Composable
internal fun SettingsRuleControlRow(
  title: String,
  value: String,
  supporting: String,
  icon: ImageVector,
  onDecrease: () -> Unit,
  onIncrease: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      shape = CircleShape,
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp))
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    RuleStepper(value = value, onDecrease = onDecrease, onIncrease = onIncrease)
  }
}

@Composable
internal fun SettingsSwitchRow(
  title: String,
  supporting: String,
  icon: ImageVector,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      shape = CircleShape,
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp))
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
internal fun SettingsRuleActionRow(
  title: String,
  value: String,
  supporting: String,
  icon: ImageVector,
  actionLabel: String,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      shape = CircleShape,
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp))
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(
      value,
      style = tabularNumbers(MaterialTheme.typography.titleMedium),
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.End,
    )
    TextButton(onClick = onClick, modifier = Modifier.height(44.dp)) {
      Text(actionLabel)
    }
  }
}

@Composable
internal fun RuleStepper(
  value: String,
  onDecrease: () -> Unit,
  onIncrease: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = CircleShape,
    modifier = Modifier.height(42.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      IconButton(onClick = onDecrease, modifier = Modifier.size(34.dp)) {
        Icon(Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
      }
      Text(
        value,
        style = tabularNumbers(MaterialTheme.typography.labelLarge),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.width(58.dp),
        textAlign = TextAlign.Center,
        maxLines = 1,
      )
      IconButton(onClick = onIncrease, modifier = Modifier.size(34.dp)) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
      }
    }
  }
}
