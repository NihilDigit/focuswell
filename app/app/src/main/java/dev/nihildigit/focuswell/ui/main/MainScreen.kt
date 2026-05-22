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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
  onStartFocus: (String, SessionType, String) -> Unit,
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
      )
    }
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
    Button(onClick = { showEnd = true }, modifier = Modifier.weight(1f).height(52.dp)) {
      Text("End")
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
    item { Text("Records", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
    item {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Text(tracker.label, fontWeight = FontWeight.SemiBold)
            Text(tracker.progressLabel ?: if (tracker.completed) "Done" else "Open")
          }
        }
      }
      "Tags" -> {
        items(state.tags.filter { it.archivedAt == null }, key = { it.id }) { tag ->
          CalmPanel {
            Text(tag.name, fontWeight = FontWeight.SemiBold)
            Text("${tag.multiplier}x")
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
        Text("${record.type.label} · ${record.tagName}", fontWeight = FontWeight.Bold)
        Text(record.task)
        Text(record.result, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${record.activeDurationMinutes.roundToInt()} min · ${signedMinutes(record.earnedMinutes)}")
      }
      TextButton(onClick = onDelete) { Text("Delete") }
    }
    TextButton(onClick = { editing = true }) { Text("Edit") }
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
      TextButton(onClick = onDelete) { Text("Delete") }
    }
  }
}

@Composable
private fun EmptyRecordText(text: String) {
  Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        state.tags.filter { it.archivedAt == null }.forEach {
          Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("${it.name} ${it.multiplier}x")
            TextButton(onClick = { onArchiveTag(it.id) }) { Text("Archive") }
          }
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
          Text("Add tag")
        }
      }
    }
    item {
      CalmPanel {
        Text("Trackers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        state.trackers.filter { it.archivedAt == null }.forEach {
          Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(it.label)
            TextButton(onClick = { onArchiveTracker(it.id) }) { Text("Archive") }
          }
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
          Text("Add rule tracker")
        }
      }
    }
    item {
      CalmPanel {
        Text("Data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        TextButton(onClick = onExportJson) { Text("Export JSON") }
        TextButton(onClick = { showImport = true }) { Text("Import JSON") }
        TextButton(onClick = { confirmClear = true }) { Text("Clear all data") }
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
  onStart: (String, SessionType, String) -> Unit,
) {
  var task by remember { mutableStateOf("") }
  var type by remember { mutableStateOf(SessionType.Input) }
  val activeTags = state.tags.filter { it.archivedAt == null }.ifEmpty { state.tags }
  var tagId by remember { mutableStateOf(activeTags.first().id) }
  val tag = activeTags.firstOrNull { it.id == tagId } ?: activeTags.first()

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
        activeTags.forEach { tag ->
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
