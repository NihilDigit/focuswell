package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.time.toKotlinInstant
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun IdleTimerSurface(
  onStartFocusClick: () -> Unit,
  onSettlePhoneUse: () -> Unit,
  phoneSettlementAvailable: Boolean,
  onStartLeisure: () -> Unit,
  leisureEnabled: Boolean,
  reserveLocked: Boolean,
) {
  val haptics = LocalHapticFeedback.current
  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainer,
      contentColor = MaterialTheme.colorScheme.onSurface,
      shape = TodayPanelShape,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(if (reserveLocked) "2h focus to restart" else "Ready when you are", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
          Text(
            if (reserveLocked) "Daily minutes can keep saving, but Leisure stays locked until one uninterrupted 2h focus" else "Start focus to earn reserve, or spend leisure when it’s time",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          Button(
            onClick = {
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onStartFocusClick()
            },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            modifier = Modifier.weight(1f).height(82.dp),
            shape = FocusActionShape,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
              Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(24.dp))
              Text("Focus", style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
          }
          Button(
            onClick = {
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onStartLeisure()
            },
            enabled = leisureEnabled,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
              ),
            modifier = Modifier.weight(1f).height(82.dp),
            shape = LeisureActionShape,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
              Icon(Icons.Rounded.Bedtime, contentDescription = null, modifier = Modifier.size(24.dp))
              Text("Leisure", style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
          }
        }
      }
    }
    AnimatedVisibility(
      visible = phoneSettlementAvailable,
      enter = fadeIn() + expandVertically(),
      exit = shrinkVertically() + fadeOut(),
    ) {
      TextButton(
        onClick = {
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onSettlePhoneUse()
        },
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(22.dp),
      ) {
        Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Settle phone use")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActiveFocusSurface(
  focus: ActiveMode.Focus,
  now: Instant? = null,
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
  val currentNow = now ?: rememberNow(paused = focus.paused)
  val elapsedEnd = if (focus.paused && focus.pausedAt != null) focus.pausedAt else currentNow
  val elapsed =
    (elapsedEnd.toKotlinInstant() - focus.startedAt.toKotlinInstant() - focus.pausedDurationMillis.milliseconds)
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
