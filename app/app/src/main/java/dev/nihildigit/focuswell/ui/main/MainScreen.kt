package dev.nihildigit.focuswell.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
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
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.notifications.postFocusWellNotification
import dev.nihildigit.focuswell.theme.FocusWellTheme
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  MainScreen(
    state = state,
    onDestination = viewModel::selectDestination,
    onToggleTracker = viewModel::toggleTracker,
    onSetWakeTime = viewModel::setWakeTime,
    onStartFocus = viewModel::startFocus,
    onPauseFocus = viewModel::pauseFocus,
    onResumeFocus = viewModel::resumeFocus,
    onEndFocus = viewModel::endFocus,
    onStartLeisure = viewModel::startLeisure,
    onEndLeisure = viewModel::endLeisure,
    onStartWindDown = viewModel::startWindDown,
    onEndWindDown = viewModel::endWindDown,
    onEndDepleted = viewModel::endDepleted,
    onExportJson = viewModel::exportJson,
    onDismissExport = viewModel::dismissExport,
    onImportJson = viewModel::importJson,
    onDismissImportError = viewModel::dismissImportError,
    onClearAllData = viewModel::clearAllData,
    onDeleteFocusRecord = viewModel::deleteFocusRecord,
    onUpdateFocusRecord = viewModel::updateFocusRecord,
    onDeleteLeisureRecord = viewModel::deleteLeisureRecord,
    onAddTag = viewModel::addTag,
    onArchiveTag = viewModel::archiveTag,
    onAddBooleanTracker = viewModel::addBooleanTracker,
    onAddRuleTracker = viewModel::addRuleTracker,
    onArchiveTracker = viewModel::archiveTracker,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
internal fun MainScreen(
  state: FocusWellUiState,
  onDestination: (Destination) -> Unit,
  onToggleTracker: (String) -> Unit,
  onSetWakeTime: (String) -> Unit,
  onStartFocus: (String, SessionType, String?) -> Unit,
  onPauseFocus: () -> Unit,
  onResumeFocus: () -> Unit,
  onEndFocus: (String) -> Unit,
  onStartLeisure: () -> Unit,
  onEndLeisure: () -> Unit,
  onStartWindDown: () -> Unit,
  onEndWindDown: () -> Unit,
  onEndDepleted: () -> Unit,
  onExportJson: () -> Unit,
  onDismissExport: () -> Unit,
  onImportJson: (String) -> Unit,
  onDismissImportError: () -> Unit,
  onClearAllData: () -> Unit,
  onDeleteFocusRecord: (String) -> Unit,
  onUpdateFocusRecord: (String, String, Double) -> Unit,
  onDeleteLeisureRecord: (String) -> Unit,
  onAddTag: (String, Double) -> Unit,
  onArchiveTag: (String) -> Unit,
  onAddBooleanTracker: (String) -> Unit,
  onAddRuleTracker: (String, String, Double) -> Unit,
  onArchiveTracker: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showFocusSheet by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    bottomBar = {
      NavigationBar {
        Destination.entries.forEach { destination ->
          NavigationBarItem(
            selected = state.destination == destination,
            onClick = { onDestination(destination) },
            icon = { DestinationIcon(destination = destination, selected = state.destination == destination) },
            label = { Text(destination.label) },
          )
        }
      }
    },
  ) { innerPadding ->
    AnimatedContent(
      targetState = state.destination,
      label = "destination",
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .safeDrawingPadding(),
    ) { destination ->
      when (destination) {
        Destination.Today ->
          TodayScreen(
            state = state,
            onToggleTracker = onToggleTracker,
            onSetWakeTime = onSetWakeTime,
            onStartFocusClick = { showFocusSheet = true },
            onStartLeisure = onStartLeisure,
            onPauseFocus = onPauseFocus,
            onResumeFocus = onResumeFocus,
            onEndFocus = onEndFocus,
            onEndLeisure = onEndLeisure,
            onStartWindDown = onStartWindDown,
            onEndWindDown = onEndWindDown,
            onEndDepleted = onEndDepleted,
          )

        Destination.Reserve -> ReserveScreen(state)
        Destination.Records ->
          RecordsScreen(
            state = state,
            onDeleteFocusRecord = onDeleteFocusRecord,
            onUpdateFocusRecord = onUpdateFocusRecord,
            onDeleteLeisureRecord = onDeleteLeisureRecord,
          )
        Destination.Settings ->
          SettingsScreen(
            state = state,
            onExportJson = onExportJson,
            onImportJson = onImportJson,
            onClearAllData = onClearAllData,
            onAddTag = onAddTag,
            onArchiveTag = onArchiveTag,
            onAddBooleanTracker = onAddBooleanTracker,
            onAddRuleTracker = onAddRuleTracker,
            onArchiveTracker = onArchiveTracker,
          )
      }
    }
  }

  if (showFocusSheet) {
    StartFocusSheet(
      state = state,
      onDismiss = { showFocusSheet = false },
      onStart = { task, type, tagId ->
        showFocusSheet = false
        onStartFocus(task, type, tagId)
      },
    )
  }

  state.exportText?.let { export ->
    AlertDialog(
      onDismissRequest = onDismissExport,
      title = { Text("Export JSON") },
      text = { Text(export, maxLines = 12) },
      confirmButton = { TextButton(onClick = onDismissExport) { Text("Done") } },
    )
  }

  state.importError?.let { error ->
    AlertDialog(
      onDismissRequest = onDismissImportError,
      title = { Text("Import failed") },
      text = { Text(error) },
      confirmButton = { TextButton(onClick = onDismissImportError) { Text("Done") } },
    )
  }
}

@Composable
private fun TodayScreen(
  state: FocusWellUiState,
  onToggleTracker: (String) -> Unit,
  onSetWakeTime: (String) -> Unit,
  onStartFocusClick: () -> Unit,
  onStartLeisure: () -> Unit,
  onPauseFocus: () -> Unit,
  onResumeFocus: () -> Unit,
  onEndFocus: (String) -> Unit,
  onEndLeisure: () -> Unit,
  onStartWindDown: () -> Unit,
  onEndWindDown: () -> Unit,
  onEndDepleted: () -> Unit,
) {
  LazyColumn(
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    item { ReserveHeader(state.reserveMinutes) }
    item {
      when (val mode = state.activeMode) {
        ActiveMode.None ->
          IdleTimerSurface(
            onStartFocusClick = onStartFocusClick,
            onStartLeisure = onStartLeisure,
            leisureEnabled = state.reserveMinutes > 0.0,
          )

        is ActiveMode.Focus ->
          ActiveFocusSurface(
            focus = mode,
            onPauseFocus = onPauseFocus,
            onResumeFocus = onResumeFocus,
            onEndFocus = onEndFocus,
          )

        is ActiveMode.Leisure ->
          ActiveLeisureSurface(
            leisure = mode,
            reserveMinutes = state.reserveMinutes,
            onEndLeisure = onEndLeisure,
            onStartWindDown = onStartWindDown,
          )

        is ActiveMode.WindDown ->
          WindDownSurface(windDown = mode, onEndWindDown = onEndWindDown)

        ActiveMode.Depleted ->
          DepletedSurface(onEndLeisure = onEndDepleted, onStartWindDown = onStartWindDown)
      }
    }
    item {
      TrackerGrid(
        trackers = state.trackers.filter { it.archivedAt == null },
        onToggleTracker = onToggleTracker,
        onSetWakeTime = onSetWakeTime,
      )
    }
  }
}

@Composable
private fun ReserveHeader(reserveMinutes: Double) {
  val fillTarget = (reserveMinutes / 180.0).coerceIn(0.08, 1.0).toFloat()
  val fill by animateFloatAsState(
    targetValue = fillTarget,
    animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
    label = "well-fill",
  )
  val shimmerTarget = if (reserveMinutes > 0.0) 1f else 0f
  val shimmer by animateFloatAsState(
    targetValue = shimmerTarget,
    animationSpec = tween(durationMillis = 300),
    label = "well-shimmer",
  )
  val wavePhase by rememberInfiniteTransition(label = "well-wave").animateFloat(
    initialValue = 0f,
    targetValue = (PI * 2).toFloat(),
    animationSpec = infiniteRepeatable(animation = tween(durationMillis = 3600, easing = LinearEasing)),
    label = "well-wave-phase",
  )
  val headline =
    when {
      reserveMinutes < 30 -> "Low reserve"
      reserveMinutes < 60 -> "${reserveMinutes.roundToInt()} min left"
      reserveMinutes <= 300 -> "${(reserveMinutes / 60.0).formatOne()} h banked"
      else -> "Enough for tonight"
    }
  val supporting =
    when {
      reserveMinutes < 30 -> "Focus can refill the buffer."
      reserveMinutes < 60 -> "Keep leisure intentional."
      else -> "Ready when you are."
    }
  Surface(
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    shape = MaterialTheme.shapes.extraLarge,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(modifier = Modifier.height(254.dp)) {
      ReserveWellDrawing(
        fill = fill,
        phase = wavePhase,
        shimmer = shimmer,
        modifier = Modifier.matchParentSize(),
      )
      Column(
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier =
          Modifier
            .align(Alignment.CenterStart)
            .padding(start = 22.dp, top = 22.dp, end = 94.dp, bottom = 22.dp),
      ) {
        Text("Leisure well", style = MaterialTheme.typography.labelLarge)
        Text(headline, style = MaterialTheme.typography.displayMedium)
        Text(supporting, style = MaterialTheme.typography.bodyLarge)
      }
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        shape = CircleShape,
        modifier = Modifier.align(Alignment.TopEnd).padding(top = 28.dp, end = 22.dp),
      ) {
        Text(
          "${reserveMinutes.roundToInt()}m",
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
          style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
        )
      }
    }
  }
}

@Composable
private fun ReserveWellDrawing(
  fill: Float,
  phase: Float,
  shimmer: Float,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  Canvas(modifier = modifier) {
    val glyphWidth = 150.dp.toPx()
    val left = size.width - 118.dp.toPx()
    val top = size.height - 82.dp.toPx()
    val right = left + glyphWidth
    val rim =
      Rect(
        left + 8.dp.toPx(),
        top + 10.dp.toPx(),
        right - 8.dp.toPx(),
        top + 72.dp.toPx(),
      )
    val waterCenterY = rim.center.y + (0.5f - fill) * 8.dp.toPx()
    val waterLine =
      Path().apply {
        val steps = 24
        repeat(steps + 1) { index ->
          val x = rim.left + 34.dp.toPx() + (rim.width - 68.dp.toPx()) * index / steps
          val crest = sin(phase + index * 0.66f) * 3.2.dp.toPx()
          val y = waterCenterY + crest
          if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
      }
    val glintLine =
      Path().apply {
        val steps = 12
        repeat(steps + 1) { index ->
          val x = rim.left + 48.dp.toPx() + (rim.width * 0.28f) * index / steps
          val crest = sin(phase + index * 0.76f) * 1.6.dp.toPx()
          val y = waterCenterY - 8.dp.toPx() + crest
          if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
      }

    drawOval(
      color = colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
      topLeft = Offset(rim.left + 8.dp.toPx(), rim.top + 9.dp.toPx()),
      size = Size(rim.width, rim.height),
      style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
    )
    drawOval(
      color = colorScheme.surface.copy(alpha = 0.3f),
      topLeft = Offset(rim.left, rim.top),
      size = Size(rim.width, rim.height),
      style = Stroke(width = 13.dp.toPx(), cap = StrokeCap.Round),
    )
    drawOval(
      color = colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
      topLeft = Offset(rim.left, rim.top),
      size = Size(rim.width, rim.height),
      style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
    )
    drawPath(
      path = waterLine,
      color = colorScheme.tertiary.copy(alpha = 0.72f),
      style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
    )
    drawPath(
      path = glintLine,
      color = colorScheme.surface.copy(alpha = 0.22f * shimmer),
      style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
    )
  }
}

@Composable
private fun IdleTimerSurface(
  onStartFocusClick: () -> Unit,
  onStartLeisure: () -> Unit,
  leisureEnabled: Boolean,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = MaterialTheme.shapes.extraLarge,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Ready when you are", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
          "Start focus to earn reserve, or spend leisure with one tap.",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = onStartFocusClick, modifier = Modifier.weight(1f).height(68.dp)) {
          Icon(Icons.Rounded.PlayArrow, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Focus", style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
        FilledTonalButton(
          onClick = onStartLeisure,
          enabled = leisureEnabled,
          modifier = Modifier.weight(1f).height(68.dp),
        ) {
          Icon(Icons.Rounded.Timer, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Leisure", style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
      }
    }
  }
}

@Composable
private fun ActiveFocusSurface(
  focus: ActiveMode.Focus,
  onPauseFocus: () -> Unit,
  onResumeFocus: () -> Unit,
  onEndFocus: (String) -> Unit,
) {
  var showEnd by remember { mutableStateOf(false) }
  var result by remember { mutableStateOf("As planned") }
  val now = rememberNow(paused = focus.paused)
  val currentPauseMillis =
    if (focus.paused && focus.pausedAt != null) {
      Duration.between(focus.pausedAt, Instant.now()).toMillis().coerceAtLeast(0)
    } else {
      0L
    }
  val elapsed =
    Duration.between(focus.startedAt, now)
      .minusMillis(focus.pausedDurationMillis + currentPauseMillis)
      .coerceAtLeast(Duration.ZERO)
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    TimerOrganism(
      label = if (focus.paused) "Paused" else "Earning ${effectiveRate(focus.type, focus.tag?.multiplier ?: 1.0)}x",
      time = formatDuration(elapsed),
      tone = MaterialTheme.colorScheme.primary,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(focus.task, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      Text(
        "${focus.type.label} · ${focus.tag?.name ?: "No tag"}",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
      if (focus.paused) {
        Button(onClick = onResumeFocus, modifier = Modifier.weight(1f).height(52.dp)) {
          Icon(Icons.Rounded.PlayArrow, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Resume")
        }
      } else {
        OutlinedButton(onClick = onPauseFocus, modifier = Modifier.weight(1f).height(52.dp)) {
          Icon(Icons.Rounded.Pause, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Pause")
        }
      }
      Button(onClick = { showEnd = true }, modifier = Modifier.weight(1f).height(52.dp)) {
        Icon(Icons.Rounded.Stop, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("End")
      }
    }
  }

  if (showEnd) {
    AlertDialog(
      onDismissRequest = { showEnd = false },
      title = { Text("What came out of this?") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("As planned", "Partial", "Drifted", "Interrupted").forEach { option ->
              AssistChip(onClick = { result = option }, label = { Text(option) })
            }
          }
          OutlinedTextField(
            value = result,
            onValueChange = { result = it },
            label = { Text("Result") },
            minLines = 2,
          )
        }
      },
      confirmButton = {
        TextButton(
          enabled = result.isNotBlank(),
          onClick = {
            showEnd = false
            onEndFocus(result)
          }
        ) {
          Text("Save result")
        }
      },
      dismissButton = { TextButton(onClick = { showEnd = false }) { Text("Cancel") } },
    )
  }
}

@Composable
private fun ActiveLeisureSurface(
  leisure: ActiveMode.Leisure,
  reserveMinutes: Double,
  onEndLeisure: () -> Unit,
  onStartWindDown: () -> Unit,
) {
  val now = rememberNow()
  val context = LocalContext.current
  var notified10 by remember(leisure.startedAt) { mutableStateOf(false) }
  var notified5 by remember(leisure.startedAt) { mutableStateOf(false) }
  var notified1 by remember(leisure.startedAt) { mutableStateOf(false) }
  var notifiedDepleted by remember(leisure.startedAt) { mutableStateOf(false) }
  val elapsed = Duration.between(leisure.startedAt, now).coerceAtLeast(Duration.ZERO)
  val spent = TimeAccounting.leisureCostMinutes(leisure.startedAt, now)
  val liveRemainingMinutes = (reserveMinutes - spent).coerceAtLeast(0.0)
  val remaining = Duration.ofSeconds((liveRemainingMinutes * 60).roundToInt().toLong())
  LaunchedEffect(liveRemainingMinutes) {
    when {
      liveRemainingMinutes <= 0.0 && !notifiedDepleted -> {
        notifiedDepleted = true
        postFocusWellNotification(context, 400, "Balance used up", "Another 60 min arrives at 04:00.")
      }
      liveRemainingMinutes <= 1.0 && !notified1 -> {
        notified1 = true
        postFocusWellNotification(context, 401, "1 min left", "Your leisure reserve is almost used up.")
      }
      liveRemainingMinutes <= 5.0 && !notified5 -> {
        notified5 = true
        postFocusWellNotification(context, 405, "5 min left", "Your leisure reserve is running low.")
      }
      liveRemainingMinutes <= 10.0 && !notified10 -> {
        notified10 = true
        postFocusWellNotification(context, 410, "10 min left", "Your leisure reserve is running low.")
      }
    }
  }
  if (liveRemainingMinutes <= 0.0) {
    LaunchedEffect(leisure.startedAt) {
      onEndLeisure()
    }
    DepletedSurface(onEndLeisure = onEndLeisure, onStartWindDown = onStartWindDown)
    return
  }
  val progress = if (reserveMinutes <= 0.0) 0f else (liveRemainingMinutes / reserveMinutes).toFloat().coerceIn(0f, 1f)
  TimerOrganism(
    label = "Remaining",
    time = formatDuration(remaining),
    tone = MaterialTheme.colorScheme.tertiary,
    progress = progress,
  )
  Surface(
    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    shape = MaterialTheme.shapes.large,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text("Elapsed", color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(formatDuration(elapsed), fontWeight = FontWeight.SemiBold)
    }
  }
  Button(onClick = onEndLeisure, modifier = Modifier.fillMaxWidth().height(52.dp)) {
    Icon(Icons.Rounded.Stop, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text("End Leisure")
  }
}

@Composable
private fun DepletedSurface(onEndLeisure: () -> Unit, onStartWindDown: () -> Unit) {
  CalmPanel {
    Text("Balance used up", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("Another 60 min arrives at 04:00.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(12.dp))
    Button(onClick = onEndLeisure, modifier = Modifier.fillMaxWidth()) {
      Icon(Icons.Rounded.Stop, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Text("End Leisure")
    }
    OutlinedButton(onClick = onStartWindDown, modifier = Modifier.fillMaxWidth()) {
      Icon(Icons.Rounded.Bedtime, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Text("Start Wind-down")
    }
  }
}

@Composable
private fun WindDownSurface(windDown: ActiveMode.WindDown, onEndWindDown: () -> Unit) {
  val elapsed = Duration.between(windDown.startedAt, rememberNow()).coerceAtLeast(Duration.ZERO)
  TimerOrganism(label = "Wind-down", time = formatDuration(elapsed), tone = MaterialTheme.colorScheme.secondary)
  Text("No earning. No spending.", color = MaterialTheme.colorScheme.onSurfaceVariant)
  Button(onClick = onEndWindDown, modifier = Modifier.fillMaxWidth().height(52.dp)) {
    Icon(Icons.Rounded.Stop, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text("End")
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrackerGrid(
  trackers: List<DailyTracker>,
  onToggleTracker: (String) -> Unit,
  onSetWakeTime: (String) -> Unit,
) {
  var wakeDialog by remember { mutableStateOf(false) }
  var wakeValue by remember { mutableStateOf("09:00") }
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SectionHeader(title = "Daily", subtitle = "Small checks, reset at 04:00")
    trackers.chunked(2).forEach { rowTrackers ->
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        rowTrackers.forEach { tracker ->
          TrackerPill(
            tracker = tracker,
            onClick = { if (tracker.id == "wake") wakeDialog = true else onToggleTracker(tracker.id) },
            modifier = Modifier.weight(1f),
          )
        }
        if (rowTrackers.size == 1) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }
  if (wakeDialog) {
    AlertDialog(
      onDismissRequest = { wakeDialog = false },
      title = { Text("Wake time") },
      text = {
        OutlinedTextField(
          value = wakeValue,
          onValueChange = { wakeValue = it },
          label = { Text("HH:mm") },
          singleLine = true,
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            wakeDialog = false
            onSetWakeTime(wakeValue)
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = { TextButton(onClick = { wakeDialog = false }) { Text("Cancel") } },
    )
  }
}

@Composable
private fun TrackerPill(tracker: DailyTracker, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val isRuleTracker = tracker.ruleTagName != null && tracker.ruleTargetMinutes != null
  val targetContainer =
    if (tracker.completed) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceContainer
  val container by animateColorAsState(
    targetValue = targetContainer,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "tracker-container",
  )
  val targetContent =
    if (tracker.completed) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurface
  val content by animateColorAsState(targetValue = targetContent, label = "tracker-content")
  val corner by animateDpAsState(
    targetValue = if (tracker.completed) 30.dp else 24.dp,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
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
          trackerStatusText(tracker),
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

@Composable
private fun ReserveScreen(state: FocusWellUiState) {
  LazyColumn(
    contentPadding = PaddingValues(20.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item { ReserveHeader(state.reserveMinutes) }
    item {
      CalmPanel {
        SectionHeader(title = "Ledger", subtitle = "Most recent balance changes")
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Net movement", color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(signedMinutes(state.ledger.sumOf { it.deltaMinutes }), fontWeight = FontWeight.Bold)
        }
      }
    }
    items(state.ledger) { LedgerRow(it) }
  }
}

@Composable
private fun RecordsScreen(
  state: FocusWellUiState,
  onDeleteFocusRecord: (String) -> Unit,
  onUpdateFocusRecord: (String, String, Double) -> Unit,
  onDeleteLeisureRecord: (String) -> Unit,
) {
  var tab by remember { mutableStateOf("Focus") }
  LazyColumn(
    contentPadding = PaddingValues(20.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item { SectionHeader(title = "History", subtitle = "Edit past sessions without rewriting the ledger") }
    item {
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Focus", "Leisure", "Trackers", "Tags").forEach { label ->
          FilterChip(selected = tab == label, onClick = { tab = label }, label = { Text(label) })
        }
      }
    }
    when (tab) {
      "Focus" -> {
        val records = state.focusRecords.filter { it.deletedAt == null }
        if (records.isEmpty()) item { EmptyRecordText("No focus sessions yet.") }
        items(records, key = { it.id }) { record ->
          FocusRecordRow(
            record = record,
            onDelete = { onDeleteFocusRecord(record.id) },
            onUpdate = { result, minutes -> onUpdateFocusRecord(record.id, result, minutes) },
          )
        }
      }
      "Leisure" -> {
        val records = state.leisureRecords.filter { it.deletedAt == null }
        if (records.isEmpty()) item { EmptyRecordText("No leisure sessions yet.") }
        items(records, key = { it.id }) { record ->
          LeisureRecordRow(record = record, onDelete = { onDeleteLeisureRecord(record.id) })
        }
      }
      "Trackers" -> {
        items(state.trackers.filter { it.archivedAt == null }, key = { it.id }) { tracker ->
          CalmPanel {
            Text(tracker.label, style = MaterialTheme.typography.titleMedium)
            Text(tracker.progressLabel ?: if (tracker.completed) "Done" else "Open")
          }
        }
      }
      "Tags" -> {
        items(state.tags.filter { it.archivedAt == null }, key = { it.id }) { tag ->
          CalmPanel {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(tag.name, style = MaterialTheme.typography.titleMedium)
              StatusBadge("${tag.multiplier}x", MaterialTheme.colorScheme.secondary)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FocusRecordRow(
  record: FocusRecord,
  onDelete: () -> Unit,
  onUpdate: (String, Double) -> Unit,
) {
  var editing by remember { mutableStateOf(false) }
  var result by remember(record.id) { mutableStateOf(record.result) }
  var minutes by remember(record.id) { mutableStateOf(record.activeDurationMinutes.roundToInt().toString()) }
  CalmPanel {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${record.type.label} · ${record.tagName ?: "Untagged"}", fontWeight = FontWeight.Bold)
        Text(record.task)
        Text(record.result, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${record.activeDurationMinutes.roundToInt()} min · ${signedMinutes(record.earnedMinutes)}")
      }
      IconButton(onClick = onDelete) {
        Icon(Icons.Rounded.Delete, contentDescription = "Delete")
      }
    }
    TextButton(onClick = { editing = true }) {
      Icon(Icons.Rounded.Edit, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Text("Edit")
    }
  }
  if (editing) {
    val newMinutes = minutes.toDoubleOrNull() ?: record.activeDurationMinutes
    val newEarned = newMinutes * record.typeRate * record.tagMultiplier
    AlertDialog(
      onDismissRequest = { editing = false },
      title = { Text("Edit focus record") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(value = result, onValueChange = { result = it }, label = { Text("Result") })
          OutlinedTextField(value = minutes, onValueChange = { minutes = it }, label = { Text("Active minutes") })
          Text("Original earned ${signedMinutes(record.earnedMinutes)}")
          Text("New earned ${signedMinutes(newEarned)}")
          Text("Balance delta ${signedMinutes(newEarned - record.earnedMinutes)}")
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            editing = false
            onUpdate(result, newMinutes)
          }
        ) {
          Text("Save and apply balance change")
        }
      },
      dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } },
    )
  }
}

@Composable
private fun LeisureRecordRow(record: LeisureRecord, onDelete: () -> Unit) {
  CalmPanel {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Leisure", fontWeight = FontWeight.Bold)
        Text("${record.elapsedMinutes.roundToInt()} min elapsed")
        Text("${signedMinutes(-record.costMinutes)} cost", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      IconButton(onClick = onDelete) {
        Icon(Icons.Rounded.Delete, contentDescription = "Delete")
      }
    }
  }
}

@Composable
private fun EmptyRecordText(text: String) {
  Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
      value,
      style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun SettingsListRow(
  title: String,
  supporting: String,
  onArchive: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    TextButton(onClick = onArchive, modifier = Modifier.width(132.dp)) {
      Icon(Icons.Rounded.Archive, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Text("Archive")
    }
  }
}

@Composable
private fun SettingsScreen(
  state: FocusWellUiState,
  onExportJson: () -> Unit,
  onImportJson: (String) -> Unit,
  onClearAllData: () -> Unit,
  onAddTag: (String, Double) -> Unit,
  onArchiveTag: (String) -> Unit,
  onAddBooleanTracker: (String) -> Unit,
  onAddRuleTracker: (String, String, Double) -> Unit,
  onArchiveTracker: (String) -> Unit,
) {
  var confirmClear by remember { mutableStateOf(false) }
  var showImport by remember { mutableStateOf(false) }
  var importText by remember { mutableStateOf("") }
  var tagName by remember { mutableStateOf("") }
  var tagMultiplier by remember { mutableStateOf("1.0") }
  var trackerLabel by remember { mutableStateOf("") }
  var ruleLabel by remember { mutableStateOf("") }
  var ruleTag by remember { mutableStateOf("math") }
  var ruleHours by remember { mutableStateOf("3") }
  LazyColumn(
    contentPadding = PaddingValues(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item { Text("Settings", style = MaterialTheme.typography.headlineLarge) }
    item {
      CalmPanel {
        Text("Rules", style = MaterialTheme.typography.headlineSmall)
        SettingsInfoRow(label = "Daily grant", value = "60 min")
        SettingsInfoRow(label = "Day boundary", value = "04:00")
        SettingsInfoRow(label = "Sleep protection", value = "01:00 · 2x")
      }
    }
    item {
      CalmPanel {
        Text("Tags", style = MaterialTheme.typography.headlineSmall)
        state.tags.filter { it.archivedAt == null }.forEach {
          SettingsListRow(
            title = it.name,
            supporting = "${it.multiplier.formatThree()}x multiplier",
            onArchive = { onArchiveTag(it.id) },
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(value = tagName, onValueChange = { tagName = it }, label = { Text("Tag") }, modifier = Modifier.weight(1f))
          OutlinedTextField(value = tagMultiplier, onValueChange = { tagMultiplier = it }, label = { Text("x") }, modifier = Modifier.width(88.dp))
        }
        TextButton(
          onClick = {
            onAddTag(tagName, tagMultiplier.toDoubleOrNull() ?: 1.0)
            tagName = ""
            tagMultiplier = "1.0"
          }
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Add tag")
        }
      }
    }
    item {
      CalmPanel {
        Text("Trackers", style = MaterialTheme.typography.headlineSmall)
        state.trackers.filter { it.archivedAt == null }.forEach {
          SettingsListRow(
            title = it.label,
            supporting = it.progressLabel ?: it.wakeTime ?: if (it.ruleTagName != null) "Rule tracker" else "Boolean tracker",
            onArchive = { onArchiveTracker(it.id) },
          )
        }
        OutlinedTextField(
          value = trackerLabel,
          onValueChange = { trackerLabel = it },
          label = { Text("Boolean tracker") },
          modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
          onClick = {
            onAddBooleanTracker(trackerLabel)
            trackerLabel = ""
          }
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Add boolean tracker")
        }
        OutlinedTextField(
          value = ruleLabel,
          onValueChange = { ruleLabel = it },
          label = { Text("Rule label") },
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = ruleTag, onValueChange = { ruleTag = it }, label = { Text("Tag") }, modifier = Modifier.weight(1f))
          OutlinedTextField(value = ruleHours, onValueChange = { ruleHours = it }, label = { Text("Hours") }, modifier = Modifier.width(112.dp))
        }
        TextButton(
          onClick = {
            onAddRuleTracker(ruleLabel, ruleTag, (ruleHours.toDoubleOrNull() ?: 3.0) * 60.0)
            ruleLabel = ""
          }
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Add rule tracker")
        }
      }
    }
    item {
      CalmPanel {
        Text("Data", style = MaterialTheme.typography.headlineSmall)
        TextButton(onClick = onExportJson) {
          Icon(Icons.Rounded.Download, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Export JSON")
        }
        TextButton(onClick = { showImport = true }) {
          Icon(Icons.Rounded.Upload, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Import JSON")
        }
        TextButton(onClick = { confirmClear = true }) {
          Icon(Icons.Rounded.Delete, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Clear all data")
        }
      }
    }
  }

  if (confirmClear) {
    AlertDialog(
      onDismissRequest = { confirmClear = false },
      title = { Text("Clear all data") },
      text = { Text("This removes local records, reserve history, trackers, and settings.") },
      confirmButton = {
        TextButton(
          onClick = {
            confirmClear = false
            onClearAllData()
          }
        ) {
          Text("Clear")
        }
      },
      dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
    )
  }

  if (showImport) {
    AlertDialog(
      onDismissRequest = { showImport = false },
      title = { Text("Import JSON") },
      text = {
        OutlinedTextField(
          value = importText,
          onValueChange = { importText = it },
          minLines = 5,
          maxLines = 10,
          label = { Text("FocusWell export") },
        )
      },
      confirmButton = {
        TextButton(
          enabled = importText.isNotBlank(),
          onClick = {
            showImport = false
            onImportJson(importText)
            importText = ""
          }
        ) {
          Text("Import")
        }
      },
      dismissButton = { TextButton(onClick = { showImport = false }) { Text("Cancel") } },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartFocusSheet(
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
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SessionType.entries.forEachIndexed { index, sessionType ->
          SegmentedButton(
            selected = type == sessionType,
            onClick = { type = sessionType },
            shape = SegmentedButtonDefaults.itemShape(index, SessionType.entries.size),
          ) {
            Text(sessionType.label)
          }
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
      Text(
        "${type.label} · ${tag?.name ?: "No tag"} · earns ${effectiveRate(type, tag?.multiplier ?: 1.0)}x real time",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Button(
        enabled = task.trim().isNotEmpty(),
        onClick = { onStart(task, type, tagId) },
        modifier = Modifier.fillMaxWidth().height(56.dp),
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
private fun TimerOrganism(
  label: String,
  time: String,
  tone: Color,
  progress: Float? = null,
) {
  Box(
    modifier =
      Modifier
        .fillMaxWidth()
        .aspectRatio(1.25f)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.extraLarge)
        .border(1.dp, tone.copy(alpha = 0.16f), MaterialTheme.shapes.extraLarge)
        .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    progress?.let { actualProgress ->
      Canvas(modifier = Modifier.size(210.dp)) {
        drawArc(
          color = tone.copy(alpha = 0.16f),
          startAngle = -90f,
          sweepAngle = 360f,
          useCenter = false,
          style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round),
          size = Size(size.minDimension, size.minDimension),
          topLeft = Offset((size.width - size.minDimension) / 2, 0f),
        )
        drawArc(
          color = tone,
          startAngle = -90f,
          sweepAngle = 360f * actualProgress.coerceIn(0f, 1f),
          useCenter = false,
          style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round),
          size = Size(size.minDimension, size.minDimension),
          topLeft = Offset((size.width - size.minDimension) / 2, 0f),
        )
      }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
      StatusBadge(label, tone)
      Text(
        time,
        style = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Monospace),
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun StatusBadge(text: String, tone: Color, modifier: Modifier = Modifier) {
  val container by animateColorAsState(targetValue = tone.copy(alpha = 0.14f), label = "badge-container")
  Surface(
    color = container,
    contentColor = tone,
    shape = CircleShape,
    modifier = modifier,
  ) {
    Text(
      text,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
      style = MaterialTheme.typography.labelLarge,
    )
  }
}

@Composable
private fun LedgerRow(entry: LedgerEntry) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        signedMinutes(entry.deltaMinutes),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color =
          when {
            entry.deltaMinutes > 0.0 -> MaterialTheme.colorScheme.primary
            entry.deltaMinutes < 0.0 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          },
        modifier = Modifier.width(86.dp),
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(entry.title, fontWeight = FontWeight.SemiBold)
        entry.note?.let {
          Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }
}

@Composable
private fun CalmPanel(content: @Composable ColumnScope.() -> Unit) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    shape = MaterialTheme.shapes.large,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      content()
    }
  }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    subtitle?.let {
      Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
private fun DestinationIcon(destination: Destination, selected: Boolean) {
  val icon =
    when (destination) {
      Destination.Today -> Icons.Rounded.Today
      Destination.Reserve -> Icons.Rounded.AccountBalanceWallet
      Destination.Records -> Icons.Rounded.History
      Destination.Settings -> Icons.Rounded.Settings
    }
  Icon(
    imageVector = icon,
    contentDescription = null,
    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun rememberNow(paused: Boolean = false): Instant {
  var now by remember { mutableStateOf(Instant.now()) }
  LaunchedEffect(paused) {
    while (!paused) {
      now = Instant.now()
      delay(250)
    }
  }
  return now
}

private fun formatDuration(duration: Duration): String {
  val totalSeconds = duration.seconds.coerceAtLeast(0)
  val hours = totalSeconds / 3600
  val minutes = (totalSeconds % 3600) / 60
  val seconds = totalSeconds % 60
  return if (hours > 0) {
    "%d:%02d:%02d".format(hours, minutes, seconds)
  } else {
    "%02d:%02d".format(minutes, seconds)
  }
}

private fun effectiveRate(type: SessionType, tagMultiplier: Double): String {
  return (type.rate * tagMultiplier).formatThree()
}

private fun signedMinutes(minutes: Double): String {
  val rounded = minutes.roundToInt()
  return when {
    rounded > 0 -> "+$rounded min"
    rounded < 0 -> "$rounded min"
    else -> "0 min"
  }
}

private fun trackerProgress(tracker: DailyTracker): Float {
  if (tracker.completed) return 1f
  val label = tracker.progressLabel ?: return 0f
  val parts = label.split("/")
  if (parts.size != 2) return 0f
  val current = parseDurationMinutes(parts[0])
  val target = parseDurationMinutes(parts[1])
  if (target <= 0.0) return 0f
  return (current / target).toFloat().coerceIn(0f, 1f)
}

private fun trackerStatusText(tracker: DailyTracker): String {
  return tracker.wakeTime
    ?: tracker.progressLabel
    ?: when {
      tracker.id == "wake" -> "Set time"
      tracker.completed -> "Done"
      else -> "Open"
    }
}

private fun parseDurationMinutes(text: String): Double {
  val trimmed = text.trim()
  var total = 0.0
  Regex("""(\d+(?:\.\d+)?)\s*h""").find(trimmed)?.let { total += it.groupValues[1].toDoubleOrNull()?.times(60.0) ?: 0.0 }
  Regex("""(\d+(?:\.\d+)?)\s*m""").find(trimmed)?.let { total += it.groupValues[1].toDoubleOrNull() ?: 0.0 }
  if (total > 0.0) return total
  return trimmed.toDoubleOrNull() ?: 0.0
}

private fun Double.formatOne(): String = "%.1f".format(this)

private fun Double.formatThree(): String {
  val text = "%.3f".format(this)
  return text.trimEnd('0').trimEnd('.')
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
  FocusWellTheme(dynamicColor = false) {
    MainScreen(
      state = FocusWellUiState(),
      onDestination = {},
      onToggleTracker = {},
      onSetWakeTime = {},
      onStartFocus = { _, _, _ -> },
      onPauseFocus = {},
      onResumeFocus = {},
      onEndFocus = {},
      onStartLeisure = {},
      onEndLeisure = {},
      onStartWindDown = {},
      onEndWindDown = {},
      onEndDepleted = {},
      onExportJson = {},
      onDismissExport = {},
      onImportJson = {},
      onDismissImportError = {},
      onClearAllData = {},
      onDeleteFocusRecord = {},
      onUpdateFocusRecord = { _, _, _ -> },
      onDeleteLeisureRecord = {},
      onAddTag = { _, _ -> },
      onArchiveTag = {},
      onAddBooleanTracker = {},
      onAddRuleTracker = { _, _, _ -> },
      onArchiveTracker = {},
    )
  }
}
