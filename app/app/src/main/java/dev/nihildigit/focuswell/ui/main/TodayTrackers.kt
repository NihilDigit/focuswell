package dev.nihildigit.focuswell.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.FocusWellRules

@Composable
internal fun TrackerGrid(
  trackers: List<DailyTracker>,
  rules: FocusWellRules,
  onToggleTracker: (String) -> Unit,
) {
  val completedCount = trackers.count { it.completed }
  val trackerCount = trackers.size
  val dailyProgress by animateFloatAsState(
    targetValue = if (trackerCount == 0) 0f else completedCount / trackerCount.toFloat(),
    animationSpec = focusWellDefaultSpatialSpec(),
    label = "daily-completion-progress",
  )
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = TodayPanelShape,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text("Daily", style = MaterialTheme.typography.headlineSmall)
          Text(
            "Resets at ${rules.normalized().safeDayBoundaryHour.todayHourLabel()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          "$completedCount/$trackerCount",
          style = tabularNumbers(MaterialTheme.typography.titleMedium),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      DailyCompletionProgress(progress = dailyProgress)
      trackers.forEach { tracker ->
        DailyTrackerTile(
          tracker = tracker,
          onClick = { onToggleTracker(tracker.id) },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
private fun DailyCompletionProgress(progress: Float) {
  LinearProgressIndicator(
    progress = { progress.coerceIn(0f, 1f) },
    modifier = Modifier.fillMaxWidth().height(8.dp),
  )
}

private fun Int.todayHourLabel(): String = "%02d:00".format(this.coerceIn(0, 23))

@Composable
internal fun DailyTrackerTile(tracker: DailyTracker, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val isRuleTracker = tracker.ruleTagName != null && tracker.ruleTargetMinutes != null
  val targetContainer =
    if (tracker.completed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
    else MaterialTheme.colorScheme.surfaceContainerHigh
  val container by animateColorAsState(
    targetValue = targetContainer,
    animationSpec = focusWellFastEffectsSpec(),
    label = "tracker-tile-container",
  )
  val targetContent =
    MaterialTheme.colorScheme.onSurface
  val content by animateColorAsState(
    targetValue = targetContent,
    animationSpec = focusWellFastEffectsSpec(),
    label = "tracker-tile-content",
  )
  val trackerProgress by animateFloatAsState(
    targetValue = trackerProgress(tracker),
    animationSpec = focusWellDefaultSpatialSpec(),
    label = "tracker-rule-progress",
  )
  Surface(
    onClick = onClick,
    enabled = !isRuleTracker,
    color = container,
    contentColor = content,
    shape = RoundedCornerShape(16.dp),
    modifier = modifier.heightIn(min = 68.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      DailyTrackerLeadingState(tracker = tracker, isRuleTracker = isRuleTracker, progress = trackerProgress)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(tracker.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(
            trackerStatusText(tracker),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            if (tracker.completed) signedCompactMinutes(tracker.rewardMinutes) else compactMinutes(tracker.rewardMinutes),
            style = MaterialTheme.typography.labelMedium,
            color =
              if (tracker.completed) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
private fun DailyTrackerLeadingState(tracker: DailyTracker, isRuleTracker: Boolean, progress: Float) {
  if (isRuleTracker) {
    val indicatorColor =
      if (tracker.completed) MaterialTheme.colorScheme.primary
      else MaterialTheme.colorScheme.onSurfaceVariant
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = 3.dp.toPx()
        val radius = (size.minDimension - stroke) / 2f
        drawCircle(
          color = indicatorColor.copy(alpha = 0.18f),
          radius = radius,
          style = Stroke(width = stroke),
        )
        drawArc(
          color = indicatorColor,
          startAngle = -90f,
          sweepAngle = 360f * progress.coerceIn(0f, 1f),
          useCenter = false,
          style = Stroke(width = stroke, cap = StrokeCap.Round),
          size = Size(radius * 2f, radius * 2f),
          topLeft = Offset((size.width - radius * 2f) / 2f, (size.height - radius * 2f) / 2f),
        )
      }
      Surface(
        color =
          if (tracker.completed) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = indicatorColor,
        shape = CircleShape,
        modifier = Modifier.size(30.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
        }
      }
    }
  } else {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
      Surface(
        color =
          if (tracker.completed) MaterialTheme.colorScheme.primary
          else Color.Transparent,
        contentColor =
          if (tracker.completed) MaterialTheme.colorScheme.onPrimary
          else MaterialTheme.colorScheme.outline,
        shape = CircleShape,
        border =
          if (tracker.completed) null
          else BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.size(30.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          if (tracker.completed) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }
}
