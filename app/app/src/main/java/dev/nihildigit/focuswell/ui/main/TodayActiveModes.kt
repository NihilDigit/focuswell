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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.Image
import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.Destination
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.focusOutcomeMultiplier
import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.LeisureRecord
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.TagConfig
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.notifications.postFocusWellNotification
import dev.nihildigit.focuswell.theme.FocusWellTheme
import dev.nihildigit.focuswell.theme.ThemeMode
import dev.nihildigit.focuswell.usage.FocusAppUsage
import dev.nihildigit.focuswell.usage.focusAppUsage
import dev.nihildigit.focuswell.usage.hasUsageAccess
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Checkbox
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val LEISURE_END_HOLD_MILLIS = 950

@Composable
internal fun IdleTimerSurface(
  onStartFocusClick: () -> Unit,
  onStartLeisure: () -> Unit,
  leisureEnabled: Boolean,
) {
  val haptics = LocalHapticFeedback.current
  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
      Text("Ready when you are", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      Text(
        "Start focus to earn reserve, or spend leisure with one tap.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
      Button(
        onClick = {
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onStartFocusClick()
        },
        modifier = Modifier.weight(1f).height(76.dp),
        shape = FocusActionShape,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Icon(Icons.Rounded.Timer, contentDescription = null)
          Text("Start Focus", style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
      }
      FilledTonalButton(
        onClick = {
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onStartLeisure()
        },
        enabled = leisureEnabled,
        modifier = Modifier.weight(1f).height(76.dp),
        shape = LeisureActionShape,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Icon(Icons.Rounded.Bedtime, contentDescription = null)
          Text("Start Leisure", style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActiveFocusSurface(
  focus: ActiveMode.Focus,
  onPauseFocus: () -> Unit,
  onResumeFocus: () -> Unit,
  onEndFocus: (String, Double) -> Unit,
  onAddIdea: (String) -> Unit,
) {
  var showEnd by remember { mutableStateOf(false) }
  var showIdeaCapture by remember { mutableStateOf(false) }
  var outcome by remember { mutableStateOf(FocusOutcomeOptions.first()) }
  var pendingResult by remember { mutableStateOf<Pair<String, Double>?>(null) }
  val haptics = LocalHapticFeedback.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val now = rememberNow(paused = focus.paused)
  val elapsedEnd = if (focus.paused && focus.pausedAt != null) focus.pausedAt else now
  val elapsed =
    Duration.between(focus.startedAt, elapsedEnd)
      .minusMillis(focus.pausedDurationMillis)
      .coerceAtLeast(Duration.ZERO)
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    FocusTimerSurface(focus = focus, elapsed = elapsed)
    FilledTonalButton(
      onClick = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        showIdeaCapture = true
      },
      modifier = Modifier.fillMaxWidth().height(50.dp),
      shape = RoundedCornerShape(22.dp),
    ) {
      Icon(Icons.Rounded.Lightbulb, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Text("Capture idea")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
      if (focus.paused) {
        Button(
          onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onResumeFocus()
          },
          modifier = Modifier.weight(1f).height(52.dp),
          shape = ControlStartShape,
        ) {
          Icon(Icons.Rounded.PlayArrow, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Resume")
        }
      } else {
        OutlinedButton(
          onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onPauseFocus()
          },
          modifier = Modifier.weight(1f).height(52.dp),
          shape = ControlStartShape,
        ) {
          Icon(Icons.Rounded.Pause, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Pause")
        }
      }
      Button(
        onClick = {
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          showEnd = true
        },
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
      focus = focus,
      onOutcomeChange = { outcome = it },
      sheetState = sheetState,
      onDismiss = { showEnd = false },
      onSave = {
        showEnd = false
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        pendingResult = outcome to it
      },
    )
  }

  pendingResult?.let { result ->
    FocusResultNoteDialog(
      outcome = result.first,
      onSkip = {
        pendingResult = null
        onEndFocus(result.first, result.second)
      },
      onSave = { note ->
        pendingResult = null
        onEndFocus(formatOutcomeResult(result.first, note), result.second)
      },
    )
  }

  if (showIdeaCapture) {
    IdeaCaptureSheet(
      onDismiss = { showIdeaCapture = false },
      onSave = {
        showIdeaCapture = false
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onAddIdea(it)
      },
    )
  }
}

@Composable
internal fun FocusTimerSurface(
  focus: ActiveMode.Focus,
  elapsed: Duration,
) {
  val rate = focus.type.rate * (focus.tag?.multiplier ?: 1.0)
  val earnedNow = elapsed.toMillis().coerceAtLeast(0).toDouble() / 60_000.0 * rate
  val tone = MaterialTheme.colorScheme.primary
  val container = MaterialTheme.colorScheme.primaryContainer
  val content = MaterialTheme.colorScheme.onPrimaryContainer
  Surface(
    color = container,
    contentColor = content,
    shape = ActiveTimerShape,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box {
      FocusFieldDrawing(tone = tone, modifier = Modifier.matchParentSize())
      Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          StatusBadge(if (focus.paused) "Paused" else "Focus running", tone)
          StatusBadge("${effectiveRate(focus.type, focus.tag?.multiplier ?: 1.0)}x earn", MaterialTheme.colorScheme.secondary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            focus.task,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            if (focus.tag == null) focus.type.label else "${focus.type.label} · ${focus.tag.name}",
            style = MaterialTheme.typography.bodyMedium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom,
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Elapsed", style = MaterialTheme.typography.labelLarge, color = content)
            Text(
              formatDuration(elapsed),
              style = tabularNumbers(MaterialTheme.typography.displayMedium),
              maxLines = 1,
              softWrap = false,
            )
          }
          Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("If ended now", style = MaterialTheme.typography.labelLarge, color = content)
            Text(
              "+${earnedNow.roundToInt()}m",
              style = tabularNumbers(MaterialTheme.typography.headlineMedium),
              fontWeight = FontWeight.ExtraBold,
              color = tone,
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun FocusFieldDrawing(tone: Color, modifier: Modifier = Modifier) {
  val veil = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)
  Canvas(modifier = modifier) {
    val stroke = 12.dp.toPx()
    val left = size.width * 0.58f
    val top = size.height * 0.08f
    repeat(5) { index ->
      val x = left + index * 18.dp.toPx()
      drawLine(
        color = tone.copy(alpha = 0.08f + index * 0.018f),
        start = Offset(x, top),
        end = Offset(x + 30.dp.toPx(), size.height - 28.dp.toPx()),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
      )
    }
    drawArc(
      color = veil,
      startAngle = 198f,
      sweepAngle = 88f,
      useCenter = false,
      topLeft = Offset(-size.width * 0.16f, size.height * 0.60f),
      size = Size(size.width * 0.56f, size.width * 0.56f),
      style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round),
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FocusResultSheet(
  outcome: String,
  focus: ActiveMode.Focus,
  onOutcomeChange: (String) -> Unit,
  sheetState: androidx.compose.material3.SheetState,
  onDismiss: () -> Unit,
  onSave: (Double) -> Unit,
) {
  val context = LocalContext.current
  val hasCorrection = remember { hasUsageAccess(context) }
  var appUsages by remember(focus.startedAt) { mutableStateOf<List<FocusAppUsage>>(emptyList()) }
  var focusPackages by remember(focus.startedAt) { mutableStateOf(setOf<String>()) }
  val correctionMinutes = appUsages.filterNot { it.packageName in focusPackages }.sumOf { it.durationMillis } / 60_000.0
  val activeEnd = remember { Instant.now() }
  val rawMinutes =
    Duration.between(focus.startedAt, activeEnd)
      .minusMillis(focus.pausedDurationMillis)
      .coerceAtLeast(Duration.ZERO)
      .toMillis() / 60_000.0
  val adjustedMinutes = (rawMinutes - correctionMinutes).coerceAtLeast(0.0)
  val tagMultiplier = focus.tag?.multiplier ?: 1.0
  val outcomeMultiplier = focusOutcomeMultiplier(outcome)
  val projectedEarned = adjustedMinutes * focus.type.rate * tagMultiplier * outcomeMultiplier

  LaunchedEffect(hasCorrection, focus.startedAt, activeEnd) {
    if (hasCorrection) {
      appUsages = withContext(Dispatchers.Default) { focusAppUsage(context, focus.startedAt, activeEnd) }
    }
  }

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
              supporting = "${focusOutcomeMultiplier(option).formatOne()}x",
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
      if (hasCorrection && appUsages.isNotEmpty()) {
        FocusUsageCorrection(
          appUsages = appUsages,
          focusPackages = focusPackages,
          onToggleFocusPackage = { packageName ->
            focusPackages =
              if (packageName in focusPackages) focusPackages - packageName else focusPackages + packageName
          },
        )
      }
      CalmPanel {
        SettlementFormula(
          rawMinutes = rawMinutes,
          correctionMinutes = correctionMinutes,
          adjustedMinutes = adjustedMinutes,
          typeRate = focus.type.rate,
          tagMultiplier = tagMultiplier,
          outcomeMultiplier = outcomeMultiplier,
          projectedEarned = projectedEarned,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(54.dp), shape = ControlStartShape) {
          Text("Cancel")
        }
        Button(
          enabled = outcome.isNotBlank(),
          onClick = { onSave(correctionMinutes) },
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
internal fun SettlementFormula(
  rawMinutes: Double,
  correctionMinutes: Double,
  adjustedMinutes: Double,
  typeRate: Double,
  tagMultiplier: Double,
  outcomeMultiplier: Double,
  projectedEarned: Double,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Settlement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    FormulaLine("Raw focus", "${rawMinutes.roundToInt()}m")
    FormulaLine("Deducted app time", "-${correctionMinutes.roundToInt()}m")
    FormulaLine("Counted focus", "${adjustedMinutes.roundToInt()}m")
    FormulaLine("Type rate", "${typeRate.formatThree()}x")
    FormulaLine("Tag multiplier", "${tagMultiplier.formatThree()}x")
    FormulaLine("Outcome multiplier", "${outcomeMultiplier.formatOne()}x")
    HorizontalDivider()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Formula", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        "${adjustedMinutes.roundToInt()}m × ${typeRate.formatThree()} × ${tagMultiplier.formatThree()} × ${outcomeMultiplier.formatOne()}",
        style = tabularNumbers(MaterialTheme.typography.labelLarge),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Earned reserve", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      Text(
        "+${projectedEarned.roundToInt()}m",
        style = tabularNumbers(MaterialTheme.typography.titleMedium),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
private fun FormulaLine(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = tabularNumbers(MaterialTheme.typography.bodyMedium), fontWeight = FontWeight.SemiBold)
  }
}

@Composable
internal fun FocusResultNoteDialog(
  outcome: String,
  onSkip: () -> Unit,
  onSave: (String) -> Unit,
) {
  var note by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onSkip,
    title = { Text("Add a note?") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          "Optional context for this $outcome session.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Note") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      TextButton(onClick = { onSave(note) }) {
        Text(if (note.isBlank()) "Save without note" else "Save note")
      }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IdeaCaptureSheet(
  onDismiss: () -> Unit,
  onSave: (String) -> Unit,
) {
  var text by remember { mutableStateOf("") }
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
    Column(
      modifier = Modifier.padding(horizontal = 20.dp).imePadding().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("Capture idea", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Idea") },
        minLines = 3,
        maxLines = 5,
        modifier = Modifier.fillMaxWidth(),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(54.dp), shape = ControlStartShape) {
          Text("Cancel")
        }
        Button(
          enabled = text.isNotBlank(),
          onClick = { onSave(text) },
          modifier = Modifier.weight(1f).height(54.dp),
          shape = ControlEndShape,
        ) {
          Text("Save")
        }
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
internal fun FocusUsageCorrection(
  appUsages: List<FocusAppUsage>,
  focusPackages: Set<String>,
  onToggleFocusPackage: (String) -> Unit,
) {
  CalmPanel {
    Text("App correction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    appUsages.forEach { usage ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        usage.icon?.let { icon ->
          val bitmap = remember(icon) { icon.toBitmap(width = 48, height = 48).asImageBitmap() }
          Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(32.dp))
        } ?: Icon(Icons.Rounded.RadioButtonUnchecked, contentDescription = null, modifier = Modifier.size(32.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(usage.appName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text("${(usage.durationMillis / 60_000.0).roundToInt()}m", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = usage.packageName in focusPackages,
            onCheckedChange = { onToggleFocusPackage(usage.packageName) },
          )
          Text("Count", style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}

@Composable
internal fun ResultChoice(
  label: String,
  supporting: String = "${focusOutcomeMultiplier(label).formatOne()}x",
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val (icon, tone) = focusOutcomeVisual(label)
  Surface(
    onClick = onClick,
    color = if (selected) tone.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainer,
    contentColor = if (selected) tone else MaterialTheme.colorScheme.onSurface,
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
      Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (selected) tone else MaterialTheme.colorScheme.onSurfaceVariant)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(supporting, style = MaterialTheme.typography.labelSmall, color = if (selected) tone else MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
internal fun ActiveLeisureSurface(
  leisure: ActiveMode.Leisure,
  reserveMinutes: Double,
  rules: FocusWellRules,
  onEndLeisure: () -> Unit,
) {
  val now = rememberNow()
  val context = LocalContext.current
  val haptics = LocalHapticFeedback.current
  var notified10 by remember(leisure.startedAt) { mutableStateOf(false) }
  var notified5 by remember(leisure.startedAt) { mutableStateOf(false) }
  var notified1 by remember(leisure.startedAt) { mutableStateOf(false) }
  var notifiedDepleted by remember(leisure.startedAt) { mutableStateOf(false) }
  val normalizedRules = rules.normalized()
  val spent = TimeAccounting.leisureCostMinutes(leisure.startedAt, now, rules = normalizedRules)
  val liveRemainingMinutes = (reserveMinutes - spent).coerceAtLeast(0.0)
  val depletionAt = TimeAccounting.instantWhenLeisureCostReaches(leisure.startedAt, reserveMinutes, rules = normalizedRules)
  val totalDisplayDuration = Duration.between(leisure.startedAt, depletionAt).coerceAtLeast(Duration.ZERO)
  val displayRemaining = Duration.between(now, depletionAt).coerceAtLeast(Duration.ZERO)
  val isSleepProtection = TimeAccounting.isSleepProtection(now, rules = normalizedRules)
  LaunchedEffect(liveRemainingMinutes) {
    when {
      liveRemainingMinutes <= 0.0 && !notifiedDepleted -> {
        notifiedDepleted = true
        postFocusWellNotification(
          context,
          400,
          "Balance used up",
          "Another ${normalizedRules.dailyGrantMinutes.roundToInt()} min arrives at ${normalizedRules.safeDayBoundaryHour.activeHourLabel()}.",
        )
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
    DepletedSurface(rules = normalizedRules, onEndLeisure = onEndLeisure)
    return
  }
  val progress =
    if (totalDisplayDuration.isZero || totalDisplayDuration.isNegative) {
      0f
    } else {
      (displayRemaining.toMillis().toDouble() / totalDisplayDuration.toMillis()).toFloat().coerceIn(0f, 1f)
    }
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    LeisureTimerSurface(
      remaining = formatDuration(displayRemaining),
      progress = progress,
      supporting = lowBalanceText(liveRemainingMinutes),
      sleepProtection = isSleepProtection,
      sleepProtectionMultiplier = normalizedRules.sleepProtectionMultiplier,
    )
    HoldToEndLeisureButton(
      onTapWithoutHold = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        Toast.makeText(context, "Keep holding to end leisure", Toast.LENGTH_SHORT).show()
      },
      onConfirmed = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onEndLeisure()
      },
    )
  }
}

@Composable
internal fun HoldToEndLeisureButton(
  onTapWithoutHold: () -> Unit,
  onConfirmed: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var holding by remember { mutableStateOf(false) }
  var completed by remember { mutableStateOf(false) }
  val holdProgress by animateFloatAsState(
    targetValue = if (holding) 1f else 0f,
    animationSpec = tween(durationMillis = if (holding) LEISURE_END_HOLD_MILLIS else 180),
    label = "hold-to-end-progress",
  )
  val container by animateColorAsState(
    targetValue = if (holding) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiaryContainer,
    animationSpec = focusWellFastEffectsSpec(),
    label = "hold-to-end-container",
  )
  val content by animateColorAsState(
    targetValue = if (holding) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onTertiaryContainer,
    animationSpec = focusWellFastEffectsSpec(),
    label = "hold-to-end-content",
  )

  LaunchedEffect(holding) {
    if (holding) {
      delay(LEISURE_END_HOLD_MILLIS.toLong())
      completed = true
      holding = false
      onConfirmed()
    }
  }

  Surface(
    color = container,
    contentColor = content,
    shape = FocusActionShape,
    modifier =
      modifier
        .fillMaxWidth()
        .height(60.dp)
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            completed = false
            holding = true
            val up = waitForUpOrCancellation()
            if (up != null && !completed) {
              holding = false
              onTapWithoutHold()
            } else {
              holding = false
            }
          }
        },
  ) {
    Box {
      Box(
        modifier =
          Modifier
            .fillMaxWidth(holdProgress.coerceIn(0f, 1f))
            .height(60.dp)
            .background(content.copy(alpha = 0.14f), FocusActionShape),
      )
      Row(
        modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.Stop, contentDescription = null)
          Text(if (holding) "Keep holding" else "Hold to end", style = MaterialTheme.typography.labelLarge)
        }
        Text(
          if (holding) "${(holdProgress * 100).roundToInt()}%" else "Press and hold",
          style = tabularNumbers(MaterialTheme.typography.labelMedium),
          color = content,
        )
      }
    }
  }
}

@Composable
internal fun LeisureTimerSurface(
  remaining: String,
  progress: Float,
  supporting: String?,
  sleepProtection: Boolean,
  sleepProtectionMultiplier: Double,
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
            StatusBadge("Sleep protection ${sleepProtectionMultiplier.formatOne()}x", tone)
          }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Remaining", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            remaining,
            style =
              tabularNumbers(if (remaining.length > 5) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge),
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
internal fun ExpressiveProgressIndicator(progress: Float, tone: Color, modifier: Modifier = Modifier) {
  val actualProgress by animateFloatAsState(
    targetValue = progress.coerceIn(0f, 1f),
    animationSpec = focusWellDefaultSpatialSpec(),
    label = "leisure-progress",
  )
  val trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
  val stopColor = MaterialTheme.colorScheme.surface
  Canvas(modifier = modifier.fillMaxWidth().height(28.dp)) {
    val stroke = 8.dp.toPx()
    val centerY = size.height / 2f
    val startX = stroke / 2f
    val endX = size.width - stroke / 2f
    val trackWidth = endX - startX
    val activeProgress = actualProgress.coerceIn(0f, 1f)
    val activeEndX = startX + trackWidth * activeProgress
    drawLine(
      color = trackColor,
      start = Offset(startX, centerY),
      end = Offset(endX, centerY),
      strokeWidth = stroke,
      cap = StrokeCap.Round,
    )
    if (activeProgress > 0.01f) {
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
    if (activeProgress in 0.02f..0.98f) {
      drawCircle(color = tone, radius = 5.dp.toPx(), center = Offset(activeEndX, centerY))
    }
  }
}

internal fun lowBalanceText(remainingMinutes: Double): String? {
  return when {
    remainingMinutes <= 1.0 -> "1 min left"
    remainingMinutes <= 5.0 -> "5 min left"
    remainingMinutes <= 10.0 -> "10 min left"
    else -> null
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DepletedSurface(
  rules: FocusWellRules,
  onEndLeisure: () -> Unit,
) {
  val normalizedRules = rules.normalized()
  val colorScheme = MaterialTheme.colorScheme
  val tone = MaterialTheme.colorScheme.tertiary
  Surface(
    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.54f),
    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    shape = ActiveTimerShape,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box {
      Canvas(modifier = Modifier.matchParentSize()) {
        drawArc(
          color = tone.copy(alpha = 0.12f),
          startAngle = 210f,
          sweepAngle = 96f,
          useCenter = false,
          topLeft = Offset(size.width * 0.62f, -size.height * 0.10f),
          size = Size(size.width * 0.48f, size.width * 0.48f),
          style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
          color = colorScheme.surface.copy(alpha = 0.34f),
          startAngle = 192f,
          sweepAngle = 74f,
          useCenter = false,
          topLeft = Offset(-size.width * 0.20f, size.height * 0.58f),
          size = Size(size.width * 0.54f, size.width * 0.54f),
          style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
        )
      }
      Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          StatusBadge("Reserve depleted", tone)
          StatusBadge("${normalizedRules.dailyGrantMinutes.roundToInt()}m at ${normalizedRules.safeDayBoundaryHour.activeHourLabel()}", tone)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Leisure is out", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
          Text(
            "Your balance is at zero. Finish this session to return to Today.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Surface(
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.54f),
          contentColor = MaterialTheme.colorScheme.onSurface,
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Rounded.Bedtime, contentDescription = null, modifier = Modifier.size(20.dp), tint = tone)
              Text("Next grant", style = MaterialTheme.typography.labelLarge)
            }
            Text(
              "${normalizedRules.safeDayBoundaryHour.activeHourLabel()} · ${normalizedRules.dailyGrantMinutes.roundToInt()}m",
              style = tabularNumbers(MaterialTheme.typography.titleMedium),
            )
          }
        }
        Button(
          onClick = onEndLeisure,
          modifier = Modifier.fillMaxWidth().height(58.dp),
          shape = FocusActionShape,
        ) {
          Icon(Icons.Rounded.Stop, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Finish Leisure")
        }
      }
    }
  }
}

private fun Int.activeHourLabel(): String = "%02d:00".format(this.coerceIn(0, 23))
