package dev.nihildigit.focuswell.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.Destination
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.theme.FocusWellTheme
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

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
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
internal fun MainScreen(
  state: FocusWellUiState,
  onDestination: (Destination) -> Unit,
  onToggleTracker: (String) -> Unit,
  onStartFocus: (String, SessionType, String) -> Unit,
  onPauseFocus: () -> Unit,
  onResumeFocus: () -> Unit,
  onEndFocus: (String) -> Unit,
  onStartLeisure: () -> Unit,
  onEndLeisure: () -> Unit,
  onStartWindDown: () -> Unit,
  onEndWindDown: () -> Unit,
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
            icon = { DestinationDot(selected = state.destination == destination) },
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
          )

        Destination.Reserve -> ReserveScreen(state)
        Destination.Records -> RecordsScreen(state)
        Destination.Settings -> SettingsScreen(state)
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
      }
    }
    item { TrackerGrid(trackers = state.trackers, onToggleTracker = onToggleTracker) }
  }
}

@Composable
private fun ReserveHeader(reserveMinutes: Double) {
  val label =
    when {
      reserveMinutes < 60 -> "${reserveMinutes.roundToInt()} min left"
      reserveMinutes <= 300 -> "${(reserveMinutes / 60.0).formatOne()} h available"
      else -> "Reserve is sufficient"
    }
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(label, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text("Ready when you are", color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun IdleTimerSurface(
  onStartFocusClick: () -> Unit,
  onStartLeisure: () -> Unit,
  leisureEnabled: Boolean,
) {
  TimerOrganism(label = "FocusWell", time = "00:00", tone = MaterialTheme.colorScheme.primary)
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
    Button(onClick = onStartFocusClick, modifier = Modifier.weight(1f).height(56.dp)) {
      Text("Start Focus")
    }
    ElevatedButton(
      onClick = onStartLeisure,
      enabled = leisureEnabled,
      modifier = Modifier.weight(1f).height(56.dp),
    ) {
      Text("Start Leisure")
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
  val now = rememberNow(paused = focus.paused)
  val elapsed = Duration.between(focus.startedAt, now).coerceAtLeast(Duration.ZERO)
  TimerOrganism(
    label = if (focus.paused) "Paused" else "Earning ${effectiveRate(focus.type, focus.tag.multiplier)}x",
    time = formatDuration(elapsed),
    tone = MaterialTheme.colorScheme.primary,
  )
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(focus.task, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(
      "${focus.type.label} · ${focus.tag.name}",
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
    if (focus.paused) {
      Button(onClick = onResumeFocus, modifier = Modifier.weight(1f).height(52.dp)) {
        Text("Resume")
      }
    } else {
      OutlinedButton(onClick = onPauseFocus, modifier = Modifier.weight(1f).height(52.dp)) {
        Text("Pause")
      }
    }
    Button(onClick = { onEndFocus("As planned") }, modifier = Modifier.weight(1f).height(52.dp)) {
      Text("End")
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
  val elapsed = Duration.between(leisure.startedAt, now).coerceAtLeast(Duration.ZERO)
  val remaining = Duration.ofSeconds((reserveMinutes * 60).roundToInt().toLong())
  if (reserveMinutes <= 0.0) {
    DepletedSurface(onEndLeisure = onEndLeisure, onStartWindDown = onStartWindDown)
    return
  }
  TimerOrganism(label = "Remaining", time = formatDuration(remaining), tone = MaterialTheme.colorScheme.tertiary)
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text("Elapsed", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(formatDuration(elapsed), fontWeight = FontWeight.SemiBold)
  }
  Button(onClick = onEndLeisure, modifier = Modifier.fillMaxWidth().height(52.dp)) {
    Text("End Leisure")
  }
}

@Composable
private fun DepletedSurface(onEndLeisure: () -> Unit, onStartWindDown: () -> Unit) {
  CalmPanel {
    Text("Balance used up", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("Another 60 min arrives at 04:00.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(12.dp))
    Button(onClick = onEndLeisure, modifier = Modifier.fillMaxWidth()) { Text("End Leisure") }
    OutlinedButton(onClick = onStartWindDown, modifier = Modifier.fillMaxWidth()) {
      Text("Start Wind-down")
    }
  }
}

@Composable
private fun WindDownSurface(windDown: ActiveMode.WindDown, onEndWindDown: () -> Unit) {
  val elapsed = Duration.between(windDown.startedAt, rememberNow()).coerceAtLeast(Duration.ZERO)
  TimerOrganism(label = "Wind-down", time = formatDuration(elapsed), tone = MaterialTheme.colorScheme.secondary)
  Text("No earning. No spending.", color = MaterialTheme.colorScheme.onSurfaceVariant)
  Button(onClick = onEndWindDown, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("End") }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrackerGrid(trackers: List<DailyTracker>, onToggleTracker: (String) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("Daily", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      maxItemsInEachRow = 2,
    ) {
      trackers.forEach { tracker ->
        FilterChip(
          selected = tracker.completed,
          onClick = { onToggleTracker(tracker.id) },
          label = {
            Column(modifier = Modifier.width(136.dp)) {
              Text(tracker.label)
              tracker.progressLabel?.let {
                Text(it, style = MaterialTheme.typography.labelSmall)
              }
            }
          },
        )
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
        Text("Today", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Net ${signedMinutes(state.ledger.sumOf { it.deltaMinutes })}")
      }
    }
    items(state.ledger) { LedgerRow(it) }
  }
}

@Composable
private fun RecordsScreen(state: FocusWellUiState) {
  LazyColumn(
    contentPadding = PaddingValues(20.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item { Text("Records", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
    item {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Focus", "Leisure", "Trackers", "Tags").forEach { label ->
          AssistChip(onClick = {}, label = { Text(label) })
        }
      }
    }
    items(state.ledger) { LedgerRow(it) }
  }
}

@Composable
private fun SettingsScreen(state: FocusWellUiState) {
  LazyColumn(
    contentPadding = PaddingValues(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item { Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
    item {
      CalmPanel {
        Text("Rules", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Daily grant: 60 min")
        Text("Day boundary: 04:00")
        Text("Sleep protection: 01:00 · 2x")
      }
    }
    item {
      CalmPanel {
        Text("Tags", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        state.tags.forEach { Text("${it.name} ${it.multiplier}x") }
      }
    }
    item {
      CalmPanel {
        Text("Data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Export JSON")
        Text("Import JSON")
        Text("Clear all data")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartFocusSheet(
  state: FocusWellUiState,
  onDismiss: () -> Unit,
  onStart: (String, SessionType, String) -> Unit,
) {
  var task by remember { mutableStateOf("") }
  var type by remember { mutableStateOf(SessionType.Input) }
  var tagId by remember { mutableStateOf(state.tags.first().id) }
  val tag = state.tags.first { it.id == tagId }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
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
        state.tags.forEach { tag ->
          FilterChip(
            selected = tagId == tag.id,
            onClick = { tagId = tag.id },
            label = { Text(tag.name) },
          )
        }
      }
      Text(
        "${type.label} · ${tag.name} · earns ${effectiveRate(type, tag.multiplier)}x real time",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Button(
        enabled = task.trim().isNotEmpty(),
        onClick = { onStart(task, type, tagId) },
        modifier = Modifier.fillMaxWidth().height(56.dp),
      ) {
        Text("Start")
      }
      Spacer(Modifier.height(12.dp))
    }
  }
}

@Composable
private fun TimerOrganism(label: String, time: String, tone: Color) {
  Box(
    modifier =
      Modifier
        .fillMaxWidth()
        .aspectRatio(1.25f)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(36.dp))
        .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
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
        sweepAngle = 245f,
        useCenter = false,
        style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round),
        size = Size(size.minDimension, size.minDimension),
        topLeft = Offset((size.width - size.minDimension) / 2, 0f),
      )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        time,
        fontSize = 54.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun LedgerRow(entry: LedgerEntry) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(entry.title, fontWeight = FontWeight.SemiBold)
      Text(signedMinutes(entry.deltaMinutes), fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun CalmPanel(content: @Composable ColumnScope.() -> Unit) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    shape = RoundedCornerShape(22.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      content()
    }
  }
}

@Composable
private fun DestinationDot(selected: Boolean) {
  Box(
    modifier =
      Modifier
        .size(if (selected) 12.dp else 8.dp)
        .background(
          if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
          CircleShape,
        )
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
  return if (rounded >= 0) "+$rounded min" else "$rounded min"
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
    )
  }
}
