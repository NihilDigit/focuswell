package dev.nihildigit.focuswell.ui.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.Destination
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.LeisureRecord
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.TagConfig
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.notifications.postFocusWellNotification
import dev.nihildigit.focuswell.theme.FocusWellTheme
import dev.nihildigit.focuswell.theme.ThemeMode
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TrackerGrid(
  trackers: List<DailyTracker>,
  rules: FocusWellRules,
  onToggleTracker: (String) -> Unit,
) {
  val secondary = MaterialTheme.colorScheme.secondary
  val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
  val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
  val onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer
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
          "${trackers.count { it.completed }}/${trackers.size}",
          style = tabularNumbers(MaterialTheme.typography.titleMedium),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
        val centerY = size.height / 2f
        val startX = 10.dp.toPx()
        val endX = size.width - 10.dp.toPx()
        val trackWidth = (endX - startX).coerceAtLeast(1f)
        drawLine(
          color = onSurfaceVariant.copy(alpha = 0.16f),
          start = Offset(startX, centerY),
          end = Offset(endX, centerY),
          strokeWidth = 4.dp.toPx(),
          cap = StrokeCap.Round,
        )
        val count = trackers.size.coerceAtLeast(1)
        trackers.forEachIndexed { index, tracker ->
          val x = if (count == 1) size.width / 2f else startX + trackWidth * index / (count - 1)
          val completed = tracker.completed
          drawCircle(
            color = if (completed) secondary.copy(alpha = 0.18f) else surfaceContainerHigh,
            radius = 8.dp.toPx(),
            center = Offset(x, centerY),
          )
          drawCircle(
            color = if (completed) secondary else onSurfaceVariant.copy(alpha = 0.36f),
            radius = if (completed) 4.9.dp.toPx() else 3.7.dp.toPx(),
            center = Offset(x, centerY),
          )
          if (completed) {
            drawCircle(
              color = onSecondaryContainer.copy(alpha = 0.92f),
              radius = 1.8.dp.toPx(),
              center = Offset(x, centerY),
            )
          }
        }
      }
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

private fun Int.todayHourLabel(): String = "%02d:00".format(this.coerceIn(0, 23))

@Composable
internal fun DailyTrackerTile(tracker: DailyTracker, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val isRuleTracker = tracker.ruleTagName != null && tracker.ruleTargetMinutes != null
  val targetContainer =
    if (tracker.completed) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHigh
  val container by animateColorAsState(
    targetValue = targetContainer,
    animationSpec = focusWellFastEffectsSpec(),
    label = "tracker-tile-container",
  )
  val targetContent =
    if (tracker.completed) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurface
  val content by animateColorAsState(
    targetValue = targetContent,
    animationSpec = focusWellFastEffectsSpec(),
    label = "tracker-tile-content",
  )
  val topStart by animateDpAsState(
    targetValue = if (tracker.completed) 26.dp else 18.dp,
    animationSpec = focusWellFastSpatialSpec(),
    label = "tracker-tile-top-start",
  )
  val topEnd by animateDpAsState(
    targetValue = if (tracker.completed) 18.dp else 24.dp,
    animationSpec = focusWellFastSpatialSpec(),
    label = "tracker-tile-top-end",
  )
  val bottomEnd by animateDpAsState(
    targetValue = if (tracker.completed) 26.dp else 18.dp,
    animationSpec = focusWellFastSpatialSpec(),
    label = "tracker-tile-bottom-end",
  )
  val bottomStart by animateDpAsState(
    targetValue = if (tracker.completed) 20.dp else 22.dp,
    animationSpec = focusWellFastSpatialSpec(),
    label = "tracker-tile-bottom-start",
  )
  val iconSize by animateDpAsState(
    targetValue = if (tracker.completed) 30.dp else 26.dp,
    animationSpec = focusWellFastSpatialSpec(),
    label = "tracker-icon-size",
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
    shape = RoundedCornerShape(topStart = topStart, topEnd = topEnd, bottomEnd = bottomEnd, bottomStart = bottomStart),
    modifier = modifier.heightIn(min = 76.dp),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        when {
          isRuleTracker -> Icons.Rounded.Timer
          tracker.completed -> Icons.Rounded.CheckCircle
          else -> Icons.Rounded.RadioButtonUnchecked
        },
        contentDescription = null,
        tint = if (tracker.completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
        modifier = Modifier.size(iconSize),
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(tracker.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          "${trackerStatusText(tracker)} · +${tracker.rewardMinutes.roundToInt()}m",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (isRuleTracker) {
          LinearProgressIndicator(progress = { trackerProgress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        }
      }
    }
  }
}

@Composable
internal fun TrackerPill(tracker: DailyTracker, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val isRuleTracker = tracker.ruleTagName != null && tracker.ruleTargetMinutes != null
  val targetContainer =
    if (tracker.completed) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceContainer
  val container by animateColorAsState(
    targetValue = targetContainer,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
    label = "tracker-container",
  )
  val targetContent =
    if (tracker.completed) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurface
  val content by animateColorAsState(targetValue = targetContent, label = "tracker-content")
  val corner by animateDpAsState(
    targetValue = if (tracker.completed) 28.dp else 20.dp,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
    label = "tracker-corner",
  )
  Surface(
    onClick = onClick,
    enabled = !isRuleTracker,
    color = container,
    contentColor = content,
    shape = RoundedCornerShape(corner),
    modifier = modifier.heightIn(min = 72.dp),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        when {
          isRuleTracker -> Icons.Rounded.Timer
          tracker.completed -> Icons.Rounded.CheckCircle
          else -> Icons.Rounded.RadioButtonUnchecked
        },
        contentDescription = null,
        tint = if (tracker.completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
        modifier = Modifier.size(30.dp),
      )
      Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
        Text(tracker.label, style = MaterialTheme.typography.titleMedium)
        Text(
          "${trackerStatusText(tracker)} · +${tracker.rewardMinutes.roundToInt()}m",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isRuleTracker) {
          LinearProgressIndicator(
            progress = { trackerProgress(tracker) },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}
