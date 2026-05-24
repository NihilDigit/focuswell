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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StartFocusSheet(
  state: FocusWellUiState,
  onDismiss: () -> Unit,
  onStart: (String, SessionType, String?) -> Unit,
) {
  var task by remember { mutableStateOf("") }
  var type by remember { mutableStateOf(SessionType.Input) }
  val activeTags = state.tags.filter { it.archivedAt == null }.ifEmpty { state.tags }
  var tagId by remember { mutableStateOf<String?>(null) }
  val tag = tagId?.let { selectedId -> activeTags.firstOrNull { it.id == selectedId } }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val recentTasks =
    remember(state.focusRecords) {
      state.focusRecords
        .asSequence()
        .filter { it.deletedAt == null }
        .sortedByDescending { it.startedAt }
        .map { it.task.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(5)
        .toList()
    }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier
          .padding(horizontal = 20.dp, vertical = 12.dp)
          .imePadding()
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("Start Focus", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      OutlinedTextField(
        value = task,
        onValueChange = { task = it },
        label = { Text("Task") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      if (recentTasks.isNotEmpty()) {
        RecentFocusTaskRow(
          tasks = recentTasks,
          selectedTask = task.trim(),
          onSelectTask = { task = it },
        )
      }
      ConnectedSessionTypeGroup(selected = type, onSelected = { type = it })
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = tagId == null,
          onClick = { tagId = null },
          label = { Text("No tag") },
        )
        activeTags.forEach { tag ->
          FilterChip(
            selected = tagId == tag.id,
            onClick = { tagId = tag.id },
            label = { Text(tag.name) },
          )
        }
      }
      CalmPanel {
        StartFocusSettlementPreview(
          type = type,
          tagName = tag?.name ?: "No tag",
          tagMultiplier = tag?.multiplier ?: 1.0,
        )
      }
      Button(
        enabled = task.trim().isNotEmpty(),
        onClick = { onStart(task, type, tagId) },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = FocusActionShape,
      ) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Start")
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
internal fun StartFocusSettlementPreview(
  type: SessionType,
  tagName: String,
  tagMultiplier: Double,
) {
  val earnedPerMinute = type.rate * tagMultiplier
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
