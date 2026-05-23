package dev.nihildigit.focuswell.ui.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
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
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.notifications.postFocusWellNotification
import dev.nihildigit.focuswell.theme.FocusWellTheme
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val TodayHeroShape = RoundedCornerShape(topStart = 42.dp, topEnd = 48.dp, bottomEnd = 30.dp, bottomStart = 46.dp)
private val TodayPanelShape = RoundedCornerShape(topStart = 30.dp, topEnd = 42.dp, bottomEnd = 24.dp, bottomStart = 30.dp)
private val ActiveTimerShape = RoundedCornerShape(topStart = 44.dp, topEnd = 32.dp, bottomEnd = 44.dp, bottomStart = 32.dp)
private val FocusActionShape = RoundedCornerShape(topStart = 34.dp, topEnd = 20.dp, bottomEnd = 30.dp, bottomStart = 34.dp)
private val LeisureActionShape = RoundedCornerShape(topStart = 20.dp, topEnd = 34.dp, bottomEnd = 34.dp, bottomStart = 30.dp)
private val ControlStartShape = RoundedCornerShape(topStart = 26.dp, topEnd = 14.dp, bottomEnd = 22.dp, bottomStart = 26.dp)
private val ControlEndShape = RoundedCornerShape(topStart = 14.dp, topEnd = 26.dp, bottomEnd = 26.dp, bottomStart = 22.dp)
private val CalmPanelShape = RoundedCornerShape(22.dp)
private val LedgerRowShape = RoundedCornerShape(topStart = 14.dp, topEnd = 18.dp, bottomEnd = 14.dp, bottomStart = 18.dp)
private val FocusOutcomeOptions = listOf("As planned", "Partial", "Drifted", "Interrupted")

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
  onStartFocus: (String, SessionType, String?) -> Unit,
  onPauseFocus: () -> Unit,
  onResumeFocus: () -> Unit,
  onEndFocus: (String) -> Unit,
  onStartLeisure: () -> Unit,
  onEndLeisure: () -> Unit,
  onStartWindDown: () -> Unit,
  onEndWindDown: () -> Unit,
  onEndDepleted: () -> Unit,
  onExportJson: () -> String,
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
  val activeMode = state.activeMode
  LazyColumn(
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    if (activeMode == ActiveMode.None) {
      item { ReserveHeader(state.reserveMinutes) }
    }
    item {
      when (val mode = activeMode) {
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
    if (activeMode != ActiveMode.None) {
      item { CompactReserveHeader(state.reserveMinutes) }
    }
    item {
      TrackerGrid(
        trackers = state.trackers.filter { it.archivedAt == null },
        onToggleTracker = onToggleTracker,
      )
    }
  }
}

@Composable
private fun CompactReserveHeader(reserveMinutes: Double) {
  val label =
    when {
      reserveMinutes < 60 -> "${reserveMinutes.roundToInt()} min available"
      reserveMinutes <= 300 -> "${(reserveMinutes / 60.0).formatOne()} h available"
      else -> "Reserve is sufficient"
    }
  Surface(
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f),
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 30.dp, bottomEnd = 22.dp, bottomStart = 28.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
        Text("Leisure reserve", style = MaterialTheme.typography.labelLarge)
        Text(label, style = MaterialTheme.typography.titleMedium)
      }
      Text(
        "${reserveMinutes.roundToInt()}m",
        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
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
    shape = TodayHeroShape,
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
    val waterCenterY = rim.center.y + (0.5f - fill) * 20.dp.toPx()
    val waterAlpha = 0.46f + fill * 0.28f
    fun disturbancePath(
      centerXRatio: Float,
      centerYOffset: Float,
      width: Float,
      amplitude: Float,
      cycles: Float,
      phaseOffset: Float,
      steps: Int,
    ): Path {
      val centerY = waterCenterY + centerYOffset
      val rx = rim.width / 2f - 24.dp.toPx()
      val ry = rim.height / 2f - 11.dp.toPx()
      val normalizedY = ((centerY - rim.center.y) / ry).coerceIn(-0.84f, 0.84f)
      val halfChord = (rx * sqrt((1f - normalizedY * normalizedY).coerceAtLeast(0f))).coerceAtLeast(width / 2f)
      val desiredCenter = rim.center.x + (centerXRatio - 0.5f) * halfChord * 2f
      val centerX = desiredCenter.coerceIn(rim.center.x - halfChord + width / 2f, rim.center.x + halfChord - width / 2f)
      val startX = centerX - width / 2f
      return Path().apply {
        repeat(steps + 1) { index ->
          val t = index / steps.toFloat()
          val edgeFade = sin((PI * t).toFloat()).coerceAtLeast(0f)
          val localPhase = (PI * 2).toFloat() * cycles * t + phase * 0.9f + phaseOffset
          val crest =
            (sin(localPhase) * 0.82f + sin(localPhase * 1.9f + phaseOffset) * 0.18f) *
              amplitude *
              edgeFade
          val x = startX + width * t
          val y = centerY + crest
          if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
      }
    }
    val disturbances =
      listOf(
        disturbancePath(
          centerXRatio = 0.3f,
          centerYOffset = 1.dp.toPx(),
          width = 34.dp.toPx(),
          amplitude = 3.1.dp.toPx(),
          cycles = 1.85f,
          phaseOffset = 0.3f,
          steps = 22,
        ) to waterAlpha,
        disturbancePath(
          centerXRatio = 0.56f,
          centerYOffset = -5.dp.toPx(),
          width = 24.dp.toPx(),
          amplitude = 1.6.dp.toPx(),
          cycles = 1.4f,
          phaseOffset = 2.1f,
          steps = 16,
        ) to (0.22f * shimmer),
        disturbancePath(
          centerXRatio = 0.72f,
          centerYOffset = 4.dp.toPx(),
          width = 30.dp.toPx(),
          amplitude = 2.6.dp.toPx(),
          cycles = 1.7f,
          phaseOffset = 4.0f,
          steps = 20,
        ) to (waterAlpha * 0.9f),
        disturbancePath(
          centerXRatio = 0.48f,
          centerYOffset = 7.dp.toPx(),
          width = 22.dp.toPx(),
          amplitude = 1.25.dp.toPx(),
          cycles = 1.2f,
          phaseOffset = 5.4f,
          steps = 14,
        ) to (waterAlpha * 0.46f),
      )

    drawArc(
      color = colorScheme.surface.copy(alpha = 0.28f),
      startAngle = 188f,
      sweepAngle = 238f,
      useCenter = false,
      topLeft = Offset(rim.left, rim.top),
      size = Size(rim.width, rim.height),
      style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
    )
    drawArc(
      color = colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
      startAngle = 12f,
      sweepAngle = 128f,
      useCenter = false,
      topLeft = Offset(rim.left + 2.dp.toPx(), rim.top + 1.dp.toPx()),
      size = Size(rim.width - 4.dp.toPx(), rim.height - 2.dp.toPx()),
      style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
    )
    drawArc(
      color = colorScheme.surface.copy(alpha = 0.22f),
      startAngle = 196f,
      sweepAngle = 118f,
      useCenter = false,
      topLeft = Offset(rim.left + 16.dp.toPx(), rim.top + 11.dp.toPx()),
      size = Size(rim.width - 32.dp.toPx(), rim.height - 20.dp.toPx()),
      style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
    )
    drawArc(
      color = colorScheme.onPrimaryContainer.copy(alpha = 0.14f),
      startAngle = 334f,
      sweepAngle = 78f,
      useCenter = false,
      topLeft = Offset(rim.left + 17.dp.toPx(), rim.top + 11.dp.toPx()),
      size = Size(rim.width - 34.dp.toPx(), rim.height - 22.dp.toPx()),
      style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
    )
    disturbances.forEachIndexed { index, (path, alpha) ->
      drawPath(
        path = path,
        color = if (index == 1) colorScheme.surface.copy(alpha = alpha) else colorScheme.tertiary.copy(alpha = alpha),
        style = Stroke(width = if (index == 1) 1.7.dp.toPx() else 3.dp.toPx(), cap = StrokeCap.Round),
      )
    }
  }
}

@Composable
private fun IdleTimerSurface(
  onStartFocusClick: () -> Unit,
  onStartLeisure: () -> Unit,
  leisureEnabled: Boolean,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = TodayPanelShape,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(horizontal = 2.dp)) {
        Text("Ready when you are", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
          "Start focus to earn reserve, or spend leisure with one tap.",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
          onClick = onStartFocusClick,
          modifier = Modifier.weight(1f).height(76.dp),
          shape = FocusActionShape,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Text("Start Focus", style = MaterialTheme.typography.labelLarge, maxLines = 1)
          }
        }
        FilledTonalButton(
          onClick = onStartLeisure,
          enabled = leisureEnabled,
          modifier = Modifier.weight(1f).height(76.dp),
          shape = LeisureActionShape,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Rounded.Timer, contentDescription = null)
            Text("Start Leisure", style = MaterialTheme.typography.labelLarge, maxLines = 1)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveFocusSurface(
  focus: ActiveMode.Focus,
  onPauseFocus: () -> Unit,
  onResumeFocus: () -> Unit,
  onEndFocus: (String) -> Unit,
) {
  var showEnd by remember { mutableStateOf(false) }
  var outcome by remember { mutableStateOf(FocusOutcomeOptions.first()) }
  var outcomeNote by remember { mutableStateOf("") }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val now = rememberNow(paused = focus.paused)
  val elapsedEnd = if (focus.paused && focus.pausedAt != null) focus.pausedAt else now
  val elapsed =
    Duration.between(focus.startedAt, elapsedEnd)
      .minusMillis(focus.pausedDurationMillis)
      .coerceAtLeast(Duration.ZERO)
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    TimerOrganism(
      label = if (focus.paused) "Paused" else "Focus time",
      time = formatDuration(elapsed),
      tone = MaterialTheme.colorScheme.primary,
      supporting = "Earning ${effectiveRate(focus.type, focus.tag?.multiplier ?: 1.0)}x real time",
    )
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainer,
      contentColor = MaterialTheme.colorScheme.onSurface,
      shape = TodayPanelShape,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          focus.task,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          StatusBadge(focus.type.label, MaterialTheme.colorScheme.primary)
          StatusBadge(focus.tag?.name ?: "No tag", MaterialTheme.colorScheme.secondary)
        }
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
      if (focus.paused) {
        Button(
          onClick = onResumeFocus,
          modifier = Modifier.weight(1f).height(52.dp),
          shape = ControlStartShape,
        ) {
          Icon(Icons.Rounded.PlayArrow, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Resume")
        }
      } else {
        OutlinedButton(
          onClick = onPauseFocus,
          modifier = Modifier.weight(1f).height(52.dp),
          shape = ControlStartShape,
        ) {
          Icon(Icons.Rounded.Pause, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Pause")
        }
      }
      Button(
        onClick = { showEnd = true },
        modifier = Modifier.weight(1f).height(52.dp),
        shape = ControlEndShape,
      ) {
        Icon(Icons.Rounded.Stop, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("End")
      }
    }
  }

  if (showEnd) {
    FocusResultSheet(
      outcome = outcome,
      note = outcomeNote,
      onOutcomeChange = { outcome = it },
      onNoteChange = { outcomeNote = it },
      sheetState = sheetState,
      onDismiss = { showEnd = false },
      onSave = {
        showEnd = false
        onEndFocus(formatOutcomeResult(outcome, outcomeNote))
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FocusResultSheet(
  outcome: String,
  note: String,
  onOutcomeChange: (String) -> Unit,
  onNoteChange: (String) -> Unit,
  sheetState: androidx.compose.material3.SheetState,
  onDismiss: () -> Unit,
  onSave: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier
          .padding(horizontal = 20.dp)
          .imePadding()
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Session outcome", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
          "Pick one result. Add a short note only when it will help later.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      FocusOutcomeOptions.chunked(2).forEach { rowOptions ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          rowOptions.forEach { option ->
            ResultChoice(
              label = option,
              selected = outcome == option,
              onClick = { onOutcomeChange(option) },
              modifier = Modifier.weight(1f),
            )
          }
          if (rowOptions.size == 1) {
            Spacer(Modifier.weight(1f))
          }
        }
      }
      OutlinedTextField(
        value = note,
        onValueChange = onNoteChange,
        label = { Text("Optional note") },
        placeholder = { Text(outcome) },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier.fillMaxWidth(),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(54.dp), shape = ControlStartShape) {
          Text("Cancel")
        }
        Button(
          enabled = outcome.isNotBlank(),
          onClick = onSave,
          modifier = Modifier.weight(1f).height(54.dp),
          shape = ControlEndShape,
        ) {
          Text("Save result")
        }
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
private fun ResultChoice(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tone = MaterialTheme.colorScheme.primary
  Surface(
    onClick = onClick,
    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
    contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(20.dp),
    modifier =
      modifier
        .height(58.dp)
        .border(
          width = 1.dp,
          color = if (selected) tone.copy(alpha = 0.34f) else MaterialTheme.colorScheme.outlineVariant,
          shape = RoundedCornerShape(20.dp),
        ),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (selected) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp), tint = tone)
      }
      Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
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
  val spent = TimeAccounting.leisureCostMinutes(leisure.startedAt, now)
  val liveRemainingMinutes = (reserveMinutes - spent).coerceAtLeast(0.0)
  val remaining = Duration.ofSeconds((liveRemainingMinutes * 60).roundToInt().toLong())
  val isSleepProtection = isSleepProtectionNow(now)
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
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    LeisureTimerSurface(
      remaining = formatDuration(remaining),
      progress = progress,
      supporting = lowBalanceText(liveRemainingMinutes),
      sleepProtection = isSleepProtection,
    )
    Button(onClick = onEndLeisure, modifier = Modifier.fillMaxWidth().height(56.dp), shape = FocusActionShape) {
      Icon(Icons.Rounded.Stop, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Text("End Leisure")
    }
  }
}

@Composable
private fun LeisureTimerSurface(
  remaining: String,
  progress: Float,
  supporting: String?,
  sleepProtection: Boolean,
) {
  val tone = MaterialTheme.colorScheme.secondary
  val container = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.52f)
  val content = MaterialTheme.colorScheme.onSecondaryContainer
  val surfaceVeil = MaterialTheme.colorScheme.surface.copy(alpha = 0.30f)
  Surface(
    color = container,
    contentColor = content,
    shape = ActiveTimerShape,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box {
      Canvas(modifier = Modifier.matchParentSize()) {
        drawArc(
          color = tone.copy(alpha = 0.10f),
          startAngle = 205f,
          sweepAngle = 108f,
          useCenter = false,
          topLeft = Offset(size.width * 0.64f, -size.height * 0.16f),
          size = Size(size.width * 0.50f, size.width * 0.50f),
          style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
          color = surfaceVeil,
          startAngle = 196f,
          sweepAngle = 88f,
          useCenter = false,
          topLeft = Offset(-size.width * 0.18f, size.height * 0.62f),
          size = Size(size.width * 0.58f, size.width * 0.58f),
          style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
        )
      }
      Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          StatusBadge("Leisure running", tone)
          if (sleepProtection) {
            StatusBadge("Sleep protection 2x", tone)
          }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Remaining", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            remaining,
            style =
              (if (remaining.length > 5) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge)
                .copy(fontFamily = FontFamily.Monospace),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
          )
          supporting?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        ExpressiveProgressIndicator(progress = progress, tone = tone)
      }
    }
  }
}

@Composable
private fun ExpressiveProgressIndicator(progress: Float, tone: Color, modifier: Modifier = Modifier) {
  val actualProgress = progress.coerceIn(0f, 1f)
  val trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
  val stopColor = MaterialTheme.colorScheme.surface
  Canvas(modifier = modifier.fillMaxWidth().height(28.dp)) {
    val stroke = 8.dp.toPx()
    val centerY = size.height / 2f
    val startX = stroke / 2f
    val endX = size.width - stroke / 2f
    val trackWidth = endX - startX
    val activeEndX = startX + trackWidth * actualProgress
    drawLine(
      color = trackColor,
      start = Offset(startX, centerY),
      end = Offset(endX, centerY),
      strokeWidth = stroke,
      cap = StrokeCap.Round,
    )
    if (actualProgress > 0.01f) {
      val wave = Path().apply {
        moveTo(startX, centerY)
        var x = startX
        while (x <= activeEndX) {
          val normalized = (x - startX) / trackWidth
          val y = centerY + sin(normalized * PI.toFloat() * 8f) * 3.dp.toPx()
          lineTo(x, y)
          x += 6.dp.toPx()
        }
        lineTo(activeEndX, centerY)
      }
      drawPath(wave, color = tone, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
    drawCircle(color = tone, radius = 4.dp.toPx(), center = Offset(endX, centerY))
    drawCircle(color = stopColor, radius = 2.dp.toPx(), center = Offset(endX, centerY))
    if (actualProgress in 0.02f..0.98f) {
      drawCircle(color = tone, radius = 5.dp.toPx(), center = Offset(activeEndX, centerY))
    }
  }
}

private fun lowBalanceText(remainingMinutes: Double): String? {
  return when {
    remainingMinutes <= 1.0 -> "1 min left"
    remainingMinutes <= 5.0 -> "5 min left"
    remainingMinutes <= 10.0 -> "10 min left"
    else -> null
  }
}

private fun isSleepProtectionNow(now: Instant): Boolean {
  val localTime = now.atZone(TimeAccounting.focusWellZone).toLocalTime()
  return localTime >= java.time.LocalTime.of(1, 0) && localTime < java.time.LocalTime.of(4, 0)
}

@Composable
private fun DepletedSurface(onEndLeisure: () -> Unit, onStartWindDown: () -> Unit) {
  CalmPanel {
    Text("Balance used up", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("Another 60 min arrives at 04:00.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(12.dp))
    Button(onClick = onEndLeisure, modifier = Modifier.fillMaxWidth(), shape = FocusActionShape) {
      Icon(Icons.Rounded.Stop, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Text("End Leisure")
    }
    OutlinedButton(onClick = onStartWindDown, modifier = Modifier.fillMaxWidth(), shape = LeisureActionShape) {
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
  Button(onClick = onEndWindDown, modifier = Modifier.fillMaxWidth().height(52.dp), shape = FocusActionShape) {
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
) {
  val secondary = MaterialTheme.colorScheme.secondary
  val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
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
          Text("Resets at 04:00", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
          "${trackers.count { it.completed }}/${trackers.size}",
          style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Canvas(modifier = Modifier.fillMaxWidth().height(18.dp)) {
        val centerY = size.height / 2f
        drawLine(
          color = onSurfaceVariant.copy(alpha = 0.22f),
          start = Offset(7.dp.toPx(), centerY),
          end = Offset(size.width - 7.dp.toPx(), centerY),
          strokeWidth = 2.dp.toPx(),
          cap = StrokeCap.Round,
        )
        val count = trackers.size.coerceAtLeast(1)
        trackers.forEachIndexed { index, tracker ->
          val x = if (count == 1) size.width / 2f else 7.dp.toPx() + (size.width - 14.dp.toPx()) * index / (count - 1)
          drawCircle(
            color =
              if (tracker.completed) secondary
              else onSurfaceVariant.copy(alpha = 0.32f),
            radius = if (tracker.completed) 4.5.dp.toPx() else 3.5.dp.toPx(),
            center = Offset(x, centerY),
          )
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

@Composable
private fun DailyTrackerTile(tracker: DailyTracker, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val isRuleTracker = tracker.ruleTagName != null && tracker.ruleTargetMinutes != null
  val container =
    if (tracker.completed) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHigh
  val content =
    if (tracker.completed) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurface
  Surface(
    onClick = onClick,
    enabled = !isRuleTracker,
    color = container,
    contentColor = content,
    shape =
      if (tracker.completed) {
        RoundedCornerShape(topStart = 26.dp, topEnd = 18.dp, bottomEnd = 26.dp, bottomStart = 20.dp)
      } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 24.dp, bottomEnd = 18.dp, bottomStart = 22.dp)
      },
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
        modifier = Modifier.size(28.dp),
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(tracker.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          trackerStatusText(tracker),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (isRuleTracker) {
          LinearProgressIndicator(progress = { trackerProgress(tracker) }, modifier = Modifier.fillMaxWidth())
        }
      }
    }
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
    targetValue = if (tracker.completed) 28.dp else 20.dp,
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
    item { BalanceSummary(state.reserveMinutes, state.ledger.sumOf { it.deltaMinutes }) }
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
private fun BalanceSummary(reserveMinutes: Double, netMovement: Double) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = CalmPanelShape,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(18.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
        Text("Balance", style = MaterialTheme.typography.headlineSmall)
        Text("Auditable leisure account", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          "${reserveMinutes.roundToInt()} min",
          style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
        )
        Text(
          "net ${signedMinutes(netMovement)}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
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
  val tabs = listOf("Focus", "Leisure", "Trackers", "Tags")
  BoxWithConstraints {
    val useTwoColumns = maxWidth >= 620.dp
    LazyColumn(
      contentPadding = PaddingValues(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item { SectionHeader(title = "History", subtitle = "Past sessions and editable records") }
      item { HistoryTabBar(tabs = tabs, selected = tab, onSelected = { tab = it }) }
      when (tab) {
        "Focus" -> {
          val records = state.focusRecords.filter { it.deletedAt == null }
          if (records.isEmpty()) item { EmptyRecordText("No focus sessions yet.") }
          if (useTwoColumns) {
            records.chunked(2).forEach { rowRecords ->
              item(key = rowRecords.joinToString { it.id }) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                  rowRecords.forEach { record ->
                    FocusRecordRow(
                      record = record,
                      onDelete = { onDeleteFocusRecord(record.id) },
                      onUpdate = { result, minutes -> onUpdateFocusRecord(record.id, result, minutes) },
                      modifier = Modifier.weight(1f),
                    )
                  }
                  if (rowRecords.size == 1) Spacer(Modifier.weight(1f))
                }
              }
            }
          } else {
            items(records, key = { it.id }) { record ->
              FocusRecordRow(
                record = record,
                onDelete = { onDeleteFocusRecord(record.id) },
                onUpdate = { result, minutes -> onUpdateFocusRecord(record.id, result, minutes) },
              )
            }
          }
        }

        "Leisure" -> {
          val records = state.leisureRecords.filter { it.deletedAt == null }
          if (records.isEmpty()) item { EmptyRecordText("No leisure sessions yet.") }
          if (useTwoColumns) {
            records.chunked(2).forEach { rowRecords ->
              item(key = rowRecords.joinToString { it.id }) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                  rowRecords.forEach { record ->
                    LeisureRecordRow(
                      record = record,
                      onDelete = { onDeleteLeisureRecord(record.id) },
                      modifier = Modifier.weight(1f),
                    )
                  }
                  if (rowRecords.size == 1) Spacer(Modifier.weight(1f))
                }
              }
            }
          } else {
            items(records, key = { it.id }) { record ->
              LeisureRecordRow(record = record, onDelete = { onDeleteLeisureRecord(record.id) })
            }
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
}

@Composable
private fun HistoryTabBar(
  tabs: List<String>,
  selected: String,
  onSelected: (String) -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      tabs.forEach { label ->
        val isSelected = selected == label
        Surface(
          onClick = { onSelected(label) },
          color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
          contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.weight(if (isSelected) 1.08f else 1f).height(46.dp),
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
              label,
              style = MaterialTheme.typography.labelLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FocusRecordRow(
  record: FocusRecord,
  onDelete: () -> Unit,
  onUpdate: (String, Double) -> Unit,
  modifier: Modifier = Modifier,
) {
  var editing by remember { mutableStateOf(false) }
  val resultParts = remember(record.id) { parseOutcomeResult(record.result) }
  var outcome by remember(record.id) { mutableStateOf(resultParts.first) }
  var note by remember(record.id) { mutableStateOf(resultParts.second) }
  var minutes by remember(record.id) { mutableStateOf(record.activeDurationMinutes.roundToInt().toString()) }
  val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = LedgerRowShape,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        signedMinutes(record.earnedMinutes),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (record.earnedMinutes > 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.width(76.dp),
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          "${record.type.label} · ${record.tagName ?: "Untagged"}",
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(record.task, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          "${record.activeDurationMinutes.roundToInt()} min · ${record.result}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { editing = true }, modifier = Modifier.size(40.dp)) {
          Icon(
            Icons.Rounded.Edit,
            contentDescription = "Edit",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
          )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
          Icon(
            Icons.Rounded.Delete,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(22.dp),
          )
        }
      }
    }
  }
  if (editing) {
    FocusRecordEditSheet(
      record = record,
      outcome = outcome,
      note = note,
      minutes = minutes,
      onOutcomeChange = { outcome = it },
      onNoteChange = { note = it },
      onMinutesChange = { minutes = it },
      sheetState = editSheetState,
      onDismiss = { editing = false },
      onSave = {
        editing = false
        onUpdate(formatOutcomeResult(outcome, note), minutes.toDoubleOrNull() ?: record.activeDurationMinutes)
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FocusRecordEditSheet(
  record: FocusRecord,
  outcome: String,
  note: String,
  minutes: String,
  onOutcomeChange: (String) -> Unit,
  onNoteChange: (String) -> Unit,
  onMinutesChange: (String) -> Unit,
  sheetState: androidx.compose.material3.SheetState,
  onDismiss: () -> Unit,
  onSave: () -> Unit,
) {
  val newMinutes = minutes.toDoubleOrNull() ?: record.activeDurationMinutes
  val newEarned = newMinutes * record.typeRate * record.tagMultiplier
  val delta = newEarned - record.earnedMinutes
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier
          .padding(horizontal = 20.dp)
          .imePadding()
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Edit focus record", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
          "${record.type.label} · ${record.tagName ?: "Untagged"}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      OutlinedTextField(
        value = minutes,
        onValueChange = onMinutesChange,
        label = { Text("Active minutes") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      FocusOutcomeOptions.chunked(2).forEach { rowOptions ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          rowOptions.forEach { option ->
            ResultChoice(
              label = option,
              selected = outcome == option,
              onClick = { onOutcomeChange(option) },
              modifier = Modifier.weight(1f),
            )
          }
          if (rowOptions.size == 1) {
            Spacer(Modifier.weight(1f))
          }
        }
      }
      OutlinedTextField(
        value = note,
        onValueChange = onNoteChange,
        label = { Text("Optional note") },
        placeholder = { Text(outcome) },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier.fillMaxWidth(),
      )
      BalanceDeltaPreview(
        original = record.earnedMinutes,
        updated = newEarned,
        delta = delta,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(54.dp), shape = ControlStartShape) {
          Text("Cancel")
        }
        Button(
          enabled = outcome.isNotBlank() && minutes.toDoubleOrNull() != null,
          onClick = onSave,
          modifier = Modifier.weight(1f).height(54.dp),
          shape = ControlEndShape,
        ) {
          Text("Apply change")
        }
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
private fun BalanceDeltaPreview(original: Double, updated: Double, delta: Double) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Original earned", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(signedMinutes(original), fontWeight = FontWeight.SemiBold)
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("New earned", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(signedMinutes(updated), fontWeight = FontWeight.SemiBold)
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Balance delta", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
          signedMinutes(delta),
          color =
            when {
              delta > 0.0 -> MaterialTheme.colorScheme.primary
              delta < 0.0 -> MaterialTheme.colorScheme.error
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
private fun LeisureRecordRow(record: LeisureRecord, onDelete: () -> Unit, modifier: Modifier = Modifier) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = LedgerRowShape,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        signedMinutes(-record.costMinutes),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.width(76.dp),
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("Leisure", style = MaterialTheme.typography.titleMedium)
        Text(
          "${record.elapsedMinutes.roundToInt()} min elapsed",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
        Icon(
          Icons.Rounded.Delete,
          contentDescription = "Delete",
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(22.dp),
        )
      }
    }
  }
}

@Composable
private fun EmptyRecordText(text: String) {
  Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SettingsRuleRow(
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
      style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.End,
    )
  }
}

@Composable
private fun SettingsListRow(
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
private fun SettingsAddTagForm(
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
private fun SettingsAddBooleanTrackerForm(
  trackerLabel: String,
  onTrackerLabelChange: (String) -> Unit,
  onAddTracker: () -> Unit,
) {
  SettingsCreateForm(
    title = "New checklist item",
    supporting = "Manual daily completion.",
    actionLabel = "Add tracker",
    icon = Icons.Rounded.CheckCircle,
    enabled = trackerLabel.isNotBlank(),
    onSubmit = onAddTracker,
  ) {
    OutlinedTextField(
      value = trackerLabel,
      onValueChange = onTrackerLabelChange,
      label = { Text("Label") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun SettingsAddRuleTrackerForm(
  ruleLabel: String,
  ruleTag: String,
  ruleHours: String,
  onRuleLabelChange: (String) -> Unit,
  onRuleTagChange: (String) -> Unit,
  onRuleHoursChange: (String) -> Unit,
  onAddRuleTracker: () -> Unit,
) {
  SettingsCreateForm(
    title = "New rule tracker",
    supporting = "Auto-completes from focused time.",
    actionLabel = "Add rule",
    icon = Icons.Rounded.Timer,
    enabled = ruleLabel.isNotBlank() && ruleTag.isNotBlank(),
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
      OutlinedTextField(
        value = ruleTag,
        onValueChange = onRuleTagChange,
        label = { Text("Tag") },
        singleLine = true,
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
    }
  }
}

@Composable
private fun SettingsCreateForm(
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

@Composable
private fun SettingsDataActionRow(
  title: String,
  supporting: String,
  icon: ImageVector,
  actionLabel: String,
  onClick: () -> Unit,
  destructive: Boolean = false,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        color =
          if (destructive) MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
          else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f),
        contentColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
        shape = CircleShape,
      ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp))
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      if (destructive) {
        OutlinedButton(
          onClick = onClick,
          modifier = Modifier.height(44.dp),
          shape = RoundedCornerShape(22.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
          Text(actionLabel)
        }
      } else {
        FilledTonalButton(onClick = onClick, modifier = Modifier.height(44.dp), shape = RoundedCornerShape(22.dp)) {
          Text(actionLabel)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
  state: FocusWellUiState,
  onExportJson: () -> String,
  onImportJson: (String) -> Unit,
  onClearAllData: () -> Unit,
  onAddTag: (String, Double) -> Unit,
  onArchiveTag: (String) -> Unit,
  onAddBooleanTracker: (String) -> Unit,
  onAddRuleTracker: (String, String, Double) -> Unit,
  onArchiveTracker: (String) -> Unit,
) {
  val context = LocalContext.current
  var confirmClear by remember { mutableStateOf(false) }
  var pendingExportText by remember { mutableStateOf<String?>(null) }
  val exportLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      val exportText = pendingExportText
      pendingExportText = null
      if (uri != null && exportText != null) {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
          stream.write(exportText.toByteArray(Charsets.UTF_8))
        }
      }
    }
  val importLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        val imported =
          context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().use { it.readText() }
          }
        imported?.let(onImportJson)
      }
    }
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
        SettingsRuleRow(
          title = "Daily grant",
          value = "60 min",
          supporting = "Added at the day boundary.",
          icon = Icons.Rounded.AccountBalanceWallet,
        )
        SettingsRuleRow(
          title = "Day boundary",
          value = "04:00",
          supporting = "New FocusWell day starts here.",
          icon = Icons.Rounded.Today,
        )
        SettingsRuleRow(
          title = "Sleep protection",
          value = "01:00 · 2x",
          supporting = "Late leisure spends faster.",
          icon = Icons.Rounded.Bedtime,
        )
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
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        SettingsAddTagForm(
          tagName = tagName,
          tagMultiplier = tagMultiplier,
          onTagNameChange = { tagName = it },
          onTagMultiplierChange = { tagMultiplier = it },
          onAddTag = {
            onAddTag(tagName, tagMultiplier.toDoubleOrNull() ?: 1.0)
            tagName = ""
            tagMultiplier = "1.0"
          },
        )
      }
    }
    item {
      CalmPanel {
        Text("Trackers", style = MaterialTheme.typography.headlineSmall)
        state.trackers.filter { it.archivedAt == null }.forEach {
          SettingsListRow(
            title = it.label,
            supporting = it.progressLabel ?: if (it.ruleTagName != null) "Rule tracker" else "Manual item",
            onArchive = { onArchiveTracker(it.id) },
          )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        SettingsAddBooleanTrackerForm(
          trackerLabel = trackerLabel,
          onTrackerLabelChange = { trackerLabel = it },
          onAddTracker = {
            onAddBooleanTracker(trackerLabel)
            trackerLabel = ""
          },
        )
        SettingsAddRuleTrackerForm(
          ruleLabel = ruleLabel,
          ruleTag = ruleTag,
          ruleHours = ruleHours,
          onRuleLabelChange = { ruleLabel = it },
          onRuleTagChange = { ruleTag = it },
          onRuleHoursChange = { ruleHours = it },
          onAddRuleTracker = {
            onAddRuleTracker(ruleLabel, ruleTag, (ruleHours.toDoubleOrNull() ?: 3.0) * 60.0)
            ruleLabel = ""
          },
        )
      }
    }
    item {
      CalmPanel {
        Text("Data", style = MaterialTheme.typography.headlineSmall)
        SettingsDataActionRow(
          title = "Export",
          supporting = "Save a complete JSON backup.",
          icon = Icons.Rounded.Download,
          actionLabel = "Export",
          onClick = {
            pendingExportText = onExportJson()
            exportLauncher.launch("focuswell-export.json")
          },
        )
        SettingsDataActionRow(
          title = "Import",
          supporting = "Restore from a JSON export.",
          icon = Icons.Rounded.Upload,
          actionLabel = "Import",
          onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
        )
        SettingsDataActionRow(
          title = "Clear all data",
          supporting = "Remove records, reserve history, trackers, and settings.",
          icon = Icons.Rounded.Delete,
          actionLabel = "Clear",
          onClick = { confirmClear = true },
          destructive = true,
        )
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
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.46f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          "${type.label} · ${tag?.name ?: "No tag"} · earns ${effectiveRate(type, tag?.multiplier ?: 1.0)}x real time",
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
          style = MaterialTheme.typography.bodyMedium,
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
private fun ConnectedSessionTypeGroup(
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
        Surface(
          onClick = { onSelected(type) },
          color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
          contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
          shape = RoundedCornerShape(if (isSelected) 24.dp else 20.dp),
          modifier =
            Modifier
              .weight(if (isSelected) 1.14f else 1f)
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

@Composable
private fun TimerOrganism(
  label: String,
  time: String,
  tone: Color,
  progress: Float? = null,
  supporting: String? = null,
) {
  Box(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 228.dp)
        .aspectRatio(1.38f)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh, ActiveTimerShape)
        .border(1.dp, tone.copy(alpha = 0.16f), ActiveTimerShape)
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
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
      StatusBadge(label, tone)
      Text(
        time,
        style =
          (if (time.length > 5) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge)
            .copy(fontFamily = FontFamily.Monospace),
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
      )
      supporting?.let {
        Text(
          it,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
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
    shape = LedgerRowShape,
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
    shape = CalmPanelShape,
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
  return tracker.progressLabel
    ?: when {
      tracker.completed -> "Done"
      else -> "Open"
    }
}

private fun parseOutcomeResult(result: String): Pair<String, String> {
  val trimmed = result.trim()
  val direct = FocusOutcomeOptions.firstOrNull { it == trimmed }
  if (direct != null) return direct to ""
  val option = FocusOutcomeOptions.firstOrNull { trimmed.startsWith("$it · ") }
  if (option != null) return option to trimmed.removePrefix("$option · ").trim()
  return FocusOutcomeOptions.first() to trimmed
}

private fun formatOutcomeResult(outcome: String, note: String): String {
  val trimmedNote = note.trim()
  return if (trimmedNote.isBlank()) outcome else "$outcome · $trimmedNote"
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
      onStartFocus = { _, _, _ -> },
      onPauseFocus = {},
      onResumeFocus = {},
      onEndFocus = {},
      onStartLeisure = {},
      onEndLeisure = {},
      onStartWindDown = {},
      onEndWindDown = {},
      onEndDepleted = {},
      onExportJson = { "" },
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
