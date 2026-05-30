package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.TagConfig

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
private fun PlanIconAction(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
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
private fun PlanRow(
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
        Box(contentAlignment = Alignment.Center) {
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
