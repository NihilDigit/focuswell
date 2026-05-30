package dev.nihildigit.focuswell.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.SessionType

@Composable
internal fun StartFocusSettlementPreview(
  type: SessionType,
  tagName: String,
  tagMultiplier: Double,
) {
  val earnedPerMinute = type.rate * tagMultiplier
  androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Settlement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    StartFormulaLine("Session type", type.label)
    StartFormulaLine("Tag", tagName)
    StartFormulaLine("Type rate", "${type.rate.formatThree()}x")
    StartFormulaLine("Tag multiplier", "${tagMultiplier.formatThree()}x")
    HorizontalDivider()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Formula", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        "1 min × ${type.rate.formatThree()} × ${tagMultiplier.formatThree()}",
        style = tabularNumbers(MaterialTheme.typography.labelLarge),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Earned per min", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      Text(
        "+${earnedPerMinute.formatThree()} min",
        style = tabularNumbers(MaterialTheme.typography.titleMedium),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
private fun StartFormulaLine(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = tabularNumbers(MaterialTheme.typography.bodyMedium), fontWeight = FontWeight.SemiBold)
  }
}

@Composable
internal fun RecentFocusTaskRow(
  tasks: List<String>,
  selectedTask: String,
  onSelectTask: (String) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    tasks.take(5).forEach { recentTask ->
      val selected = selectedTask == recentTask
      Surface(
        color =
          if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f)
          else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
        contentColor =
          if (selected) MaterialTheme.colorScheme.onSecondaryContainer
          else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = CircleShape,
        modifier =
          Modifier
            .height(34.dp)
            .clickable { onSelectTask(recentTask) },
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            recentTask,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 132.dp),
          )
        }
      }
    }
  }
}

@Composable
internal fun ConnectedSessionTypeGroup(
  selected: SessionType,
  onSelected: (SessionType) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(28.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      SessionType.entries.forEach { type ->
        val isSelected = selected == type
        val weight by animateFloatAsState(
          targetValue = if (isSelected) 1.14f else 1f,
          animationSpec = focusWellFastSpatialSpec(),
          label = "session-type-weight",
        )
        val corner by animateDpAsState(
          targetValue = if (isSelected) 24.dp else 20.dp,
          animationSpec = focusWellFastSpatialSpec(),
          label = "session-type-corner",
        )
        val container by animateColorAsState(
          targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
          animationSpec = focusWellFastEffectsSpec(),
          label = "session-type-container",
        )
        val content by animateColorAsState(
          targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
          animationSpec = focusWellFastEffectsSpec(),
          label = "session-type-content",
        )
        Surface(
          onClick = { onSelected(type) },
          color = container,
          contentColor = content,
          shape = RoundedCornerShape(corner),
          modifier =
            Modifier
              .weight(weight)
              .height(52.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = if (type == SessionType.Input) Icons.Rounded.Download else Icons.Rounded.Upload,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
            Text(type.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
          }
        }
      }
    }
  }
}
