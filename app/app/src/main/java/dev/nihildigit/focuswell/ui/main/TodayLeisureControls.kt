package dev.nihildigit.focuswell.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val LEISURE_END_HOLD_MILLIS = 950

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
