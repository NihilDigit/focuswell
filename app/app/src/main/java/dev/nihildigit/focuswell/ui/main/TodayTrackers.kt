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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.FocusWellRules

@Composable
internal fun TrackerGrid(
  trackers: List<DailyTracker>,
  previewTrackers: List<DailyTracker> = trackers,
  rules: FocusWellRules,
  onToggleTracker: (String) -> Unit,
) {
  val completedCount = trackers.count { it.completed }
  val previewById = previewTrackers.associateBy { it.id }
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
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text("Daily", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
          Text(
            "Resets at ${rules.normalized().safeDayBoundaryHour.todayHourLabel()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        DailySummaryChip(rewardMinutes = trackers.filter { it.completed }.sumOf { it.rewardMinutes })
      }
      DailyCompletionProgress(progress = dailyProgress)
      if (trackers.isEmpty()) {
        Text(
          "No daily trackers yet.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 6.dp),
        )
      } else {
        trackers.forEach { tracker ->
          val previewTracker = previewById[tracker.id] ?: tracker
          DailyTrackerTile(
            tracker = previewTracker,
            savedProgress = trackerProgress(tracker),
            previewProgress = trackerProgress(previewTracker),
            onClick = { onToggleTracker(tracker.id) },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}

@Composable
private fun DailySummaryChip(rewardMinutes: Double) {
  Surface(
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.64f),
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    shape = RoundedCornerShape(22.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(5.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        signedCompactMinutes(rewardMinutes),
        style = tabularNumbers(MaterialTheme.typography.titleMedium),
      )
    }
  }
}

@Composable
private fun DailyCompletionProgress(progress: Float) {
  val active = MaterialTheme.colorScheme.primary
  val track = MaterialTheme.colorScheme.surfaceContainerHigh
  val endStop = MaterialTheme.colorScheme.onSurfaceVariant
  Canvas(modifier = Modifier.fillMaxWidth().height(18.dp)) {
    val height = 12.dp.toPx()
    val top = (size.height - height) / 2f
    val radius = height / 2f
    val activeWidth = size.width * progress.coerceIn(0f, 1f)
    drawRoundRect(
      color = track,
      topLeft = Offset(0f, top),
      size = Size(size.width, height),
      cornerRadius = CornerRadius(radius, radius),
    )
    if (activeWidth > 0f) {
      drawRoundRect(
        color = active,
        topLeft = Offset(0f, top),
        size = Size(activeWidth.coerceAtLeast(radius * 1.5f).coerceAtMost(size.width), height),
        cornerRadius = CornerRadius(radius, radius),
      )
    }
    drawCircle(
      color = endStop.copy(alpha = 0.58f),
      radius = 2.2.dp.toPx(),
      center = Offset(size.width - radius, size.height / 2f),
    )
  }
}

private fun Int.todayHourLabel(): String = "%02d:00".format(this.coerceIn(0, 23))

@Composable
internal fun DailyTrackerTile(
  tracker: DailyTracker,
  savedProgress: Float = trackerProgress(tracker),
  previewProgress: Float = savedProgress,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isRuleTracker = tracker.ruleTagName != null && tracker.ruleTargetMinutes != null
  val targetContainer =
    if (tracker.completed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
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
  val savedTrackerProgress by animateFloatAsState(
    targetValue = savedProgress,
    animationSpec = focusWellDefaultSpatialSpec(),
    label = "tracker-rule-progress",
  )
  val previewTrackerProgress by animateFloatAsState(
    targetValue = previewProgress,
    animationSpec = focusWellDefaultSpatialSpec(),
    label = "tracker-rule-preview-progress",
  )
  Surface(
    onClick = onClick,
    enabled = !isRuleTracker,
    color = container,
    contentColor = content,
    shape = if (tracker.completed) RoundedCornerShape(20.dp) else RoundedCornerShape(16.dp),
    border =
      if (tracker.completed) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
      else null,
    modifier = modifier.heightIn(min = 66.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      DailyTrackerLeadingState(
        tracker = tracker,
        isRuleTracker = isRuleTracker,
        progress = savedTrackerProgress,
        previewProgress = previewTrackerProgress,
      )
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
          if (!tracker.completed) {
            Text(
              compactMinutes(tracker.rewardMinutes),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DailyTrackerLeadingState(
  tracker: DailyTracker,
  isRuleTracker: Boolean,
  progress: Float,
  previewProgress: Float,
) {
  if (isRuleTracker) {
    val indicatorColor =
      if (tracker.completed) MaterialTheme.colorScheme.primary
      else MaterialTheme.colorScheme.onSurfaceVariant
    val previewColor = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = 3.dp.toPx()
        val radius = (size.minDimension - stroke) / 2f
        drawCircle(
          color = indicatorColor.copy(alpha = 0.18f),
          radius = radius,
          style = Stroke(width = stroke),
        )
        val savedSweep = 360f * progress.coerceIn(0f, 1f)
        val previewSweep = 360f * (previewProgress - progress).coerceAtLeast(0f).coerceAtMost(1f)
        drawArc(
          color = indicatorColor,
          startAngle = -90f,
          sweepAngle = savedSweep,
          useCenter = false,
          style = Stroke(width = stroke, cap = StrokeCap.Round),
          size = Size(radius * 2f, radius * 2f),
          topLeft = Offset((size.width - radius * 2f) / 2f, (size.height - radius * 2f) / 2f),
        )
        if (previewSweep > 0f) {
          drawArc(
            color = previewColor.copy(alpha = 0.76f),
            startAngle = -90f + savedSweep,
            sweepAngle = previewSweep,
            useCenter = false,
            style =
              Stroke(
                width = stroke,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())),
              ),
            size = Size(radius * 2f, radius * 2f),
            topLeft = Offset((size.width - radius * 2f) / 2f, (size.height - radius * 2f) / 2f),
          )
        }
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
