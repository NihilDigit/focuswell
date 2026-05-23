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
  onEndFocus: (String) -> Unit,
) {
  var showEnd by remember { mutableStateOf(false) }
  var outcome by remember { mutableStateOf(FocusOutcomeOptions.first()) }
  var outcomeNote by remember { mutableStateOf("") }
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
      note = outcomeNote,
      onOutcomeChange = { outcome = it },
      onNoteChange = { outcomeNote = it },
      sheetState = sheetState,
      onDismiss = { showEnd = false },
      onSave = {
        showEnd = false
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onEndFocus(formatOutcomeResult(outcome, outcomeNote))
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
internal fun ResultChoice(
  label: String,
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
      Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

@Composable
internal fun ActiveLeisureSurface(
  leisure: ActiveMode.Leisure,
  reserveMinutes: Double,
  rules: FocusWellRules,
  onEndLeisure: () -> Unit,
  onStartWindDown: () -> Unit,
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
  val remaining = Duration.ofSeconds((liveRemainingMinutes * 60).roundToInt().toLong())
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
    DepletedSurface(rules = normalizedRules, onEndLeisure = onEndLeisure, onStartWindDown = onStartWindDown)
    return
  }
  val progress = if (reserveMinutes <= 0.0) 0f else (liveRemainingMinutes / reserveMinutes).toFloat().coerceIn(0f, 1f)
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    LeisureTimerSurface(
      remaining = formatDuration(remaining),
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

@Composable
internal fun DepletedSurface(
  rules: FocusWellRules,
  onEndLeisure: () -> Unit,
  onStartWindDown: () -> Unit,
) {
  val normalizedRules = rules.normalized()
  CalmPanel {
    Text("Balance used up", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(
      "Another ${normalizedRules.dailyGrantMinutes.roundToInt()} min arrives at ${normalizedRules.safeDayBoundaryHour.activeHourLabel()}.",
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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

private fun Int.activeHourLabel(): String = "%02d:00".format(this.coerceIn(0, 23))

@Composable
internal fun WindDownSurface(windDown: ActiveMode.WindDown, onEndWindDown: () -> Unit) {
  val elapsed = Duration.between(windDown.startedAt, rememberNow()).coerceAtLeast(Duration.ZERO)
  TimerOrganism(label = "Wind-down", time = formatDuration(elapsed), tone = MaterialTheme.colorScheme.secondary)
  Text("No earning. No spending.", color = MaterialTheme.colorScheme.onSurfaceVariant)
  Button(onClick = onEndWindDown, modifier = Modifier.fillMaxWidth().height(52.dp), shape = FocusActionShape) {
    Icon(Icons.Rounded.Stop, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text("End")
  }
}
