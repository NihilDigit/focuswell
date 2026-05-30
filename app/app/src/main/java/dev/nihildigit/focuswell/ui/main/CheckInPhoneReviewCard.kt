package dev.nihildigit.focuswell.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.PhoneUsageSegment
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
internal fun PhoneUsageSegmentCard(
  segment: PhoneUsageSegment,
  index: Int,
  total: Int,
  onCount: () -> Unit,
  onFairUse: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val offsetX = remember(segment.id) { Animatable(0f) }
  var cardWidth by remember(segment.id) { mutableFloatStateOf(0f) }
  val threshold = with(LocalDensity.current) { 116.dp.toPx() }
  val offset = offsetX.value
  val rotation = (offset / threshold).coerceIn(-1f, 1f) * 4f
  val fairAlpha = (offset / threshold).coerceIn(0f, 1f)
  val countAlpha = (-offset / threshold).coerceIn(0f, 1f)
  val dragProgress = (abs(offset) / threshold).coerceIn(0f, 1f)
  val draggedRight = offset > 0f
  val cardElevation by animateDpAsState(
    targetValue = if (dragProgress > 0.04f) 6.dp else 1.dp,
    animationSpec = focusWellFastEffectsSpec(),
    label = "phone-review-card-elevation",
  )
  val cardColor by animateColorAsState(
    targetValue =
      when {
        offset > 8f -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.26f + dragProgress * 0.22f)
        offset < -8f -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.24f + dragProgress * 0.20f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
      },
    animationSpec = focusWellFastEffectsSpec(),
    label = "phone-review-card-color",
  )
  val borderColor by animateColorAsState(
    targetValue =
      when {
        offset > 8f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f + dragProgress * 0.34f)
        offset < -8f -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f + dragProgress * 0.34f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
      },
    animationSpec = focusWellFastEffectsSpec(),
    label = "phone-review-card-border",
  )
  val actualMinutes = segment.slices.sumOf { it.durationMillis } / 60_000.0
  val sleepProtected = segment.costMinutes > actualMinutes + 0.01
  Box(
    modifier = Modifier.fillMaxWidth().heightIn(min = 420.dp),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = cardColor),
      border = BorderStroke(1.dp, borderColor),
      elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
      shape = RoundedCornerShape(26.dp),
      modifier =
        Modifier
          .fillMaxWidth()
          .onSizeChanged { cardWidth = it.width.toFloat() }
          .graphicsLayer {
            translationX = offset
            rotationZ = rotation
          }
          .pointerInput(segment.id) {
            detectDragGestures(
              onDragStart = {
                scope.launch { offsetX.stop() }
              },
              onDragEnd = {
                val releasedOffset = offsetX.value
                val exitDistance = maxOf(cardWidth + threshold, threshold * 3f)
                when {
                  releasedOffset > threshold ->
                    scope.launch {
                      offsetX.animateTo(exitDistance, animationSpec = tween(durationMillis = 150))
                      offsetX.snapTo(0f)
                      onFairUse()
                    }
                  releasedOffset < -threshold ->
                    scope.launch {
                      offsetX.animateTo(-exitDistance, animationSpec = tween(durationMillis = 150))
                      offsetX.snapTo(0f)
                      onCount()
                    }
                  else ->
                    scope.launch {
                      offsetX.animateTo(
                        0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                      )
                    }
                }
              },
              onDragCancel = {
                scope.launch {
                  offsetX.animateTo(
                    0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                  )
                }
              },
              onDrag = { change, dragAmount ->
                change.consume()
                val next = offsetX.value + resistedCardDragDelta(offsetX.value, dragAmount.x, threshold)
                scope.launch { offsetX.snapTo(next) }
              },
            )
          },
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            "${index + 1} / $total",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (sleepProtected) {
              Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(999.dp),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(Icons.Rounded.Bedtime, contentDescription = null, modifier = Modifier.size(14.dp))
                  Text("Sleep x2", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
              }
            }
            if (dragProgress > 0.04f) {
              Surface(
                color =
                  if (draggedRight) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f + fairAlpha * 0.18f)
                  else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f + countAlpha * 0.16f),
                contentColor = if (draggedRight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(999.dp),
              ) {
                Text(
                  if (draggedRight) "Fair Use" else "Count",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
              }
            }
          }
        }
        PhoneUsageTimeline(segment = segment)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            "Count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.44f + countAlpha * 0.44f),
          )
          Text(
            "Fair Use",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.44f + fairAlpha * 0.44f),
          )
        }
      }
    }
  }
}

internal fun resistedCardDragDelta(currentOffset: Float, delta: Float, threshold: Float): Float {
  val next = currentOffset + delta
  if (abs(next) <= threshold) return delta
  val overshoot = (abs(next) - threshold).coerceAtLeast(0f)
  val resistance = 0.42f / (1f + overshoot / (threshold * 1.8f))
  return delta * resistance.coerceIn(0.16f, 0.42f)
}
