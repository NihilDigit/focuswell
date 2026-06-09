package dev.nihildigit.focuswell.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.theme.FocusWellTheme
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun LeisureTimerSurface(
  remaining: String,
  elapsed: String,
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
          StatusBadge("Leisure running", tone)
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
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          LeisureSessionStat(
            label = "Elapsed",
            value = elapsed,
            modifier = Modifier.weight(1f),
          )
          if (sleepProtection) {
            LeisureSessionStat(
              label = "Sleep protection",
              value = "${sleepProtectionMultiplier.formatOne()}x cost",
              modifier = Modifier.weight(1f),
            )
          }
        }
        ExpressiveProgressIndicator(progress = progress, tone = tone)
      }
    }
  }
}

@Composable
private fun LeisureSessionStat(label: String, value: String, modifier: Modifier = Modifier) {
  Surface(
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.52f),
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    shape = CalmPanelShape,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        value,
        style = tabularNumbers(MaterialTheme.typography.titleMedium),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
internal fun ExpressiveProgressIndicator(progress: Float, tone: Color, modifier: Modifier = Modifier) {
  val sparkPhase by rememberInfiniteTransition(label = "leisure-sparks").animateFloat(
    initialValue = 0f,
    targetValue = (PI * 2).toFloat(),
    animationSpec = infiniteRepeatable(animation = tween(durationMillis = 900, easing = LinearEasing)),
    label = "leisure-spark-phase",
  )
  val actualProgress by animateFloatAsState(
    targetValue = progress.coerceIn(0f, 1f),
    animationSpec = focusWellDefaultSpatialSpec(),
    label = "leisure-progress",
  )
  val trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
  val stopColor = MaterialTheme.colorScheme.surface
  val emberColor = MaterialTheme.colorScheme.tertiary
  Canvas(modifier = modifier.fillMaxWidth().height(28.dp)) {
    val stroke = 8.dp.toPx()
    val centerY = size.height / 2f
    val startX = stroke / 2f
    val endX = size.width - stroke / 2f
    val trackWidth = endX - startX
    val remainingProgress = actualProgress.coerceIn(0f, 1f)
    val burnX = startX + trackWidth * remainingProgress
    drawLine(
      color = trackColor,
      start = Offset(startX, centerY),
      end = Offset(endX, centerY),
      strokeWidth = stroke,
      cap = StrokeCap.Round,
    )
    if (remainingProgress > 0.01f) {
      val wave = Path().apply {
        moveTo(startX, centerY)
        var x = startX
        while (x <= burnX) {
          val normalized = (x - startX) / trackWidth
          val y = centerY + sin(normalized * PI.toFloat() * 8f) * 3.dp.toPx()
          lineTo(x, y)
          x += 6.dp.toPx()
        }
        lineTo(burnX, centerY)
      }
      drawPath(wave, color = tone, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
    drawCircle(color = tone, radius = 4.dp.toPx(), center = Offset(endX, centerY))
    drawCircle(color = stopColor, radius = 2.dp.toPx(), center = Offset(endX, centerY))
    if (remainingProgress > 0.01f) {
      drawCircle(color = emberColor, radius = 5.5.dp.toPx(), center = Offset(burnX, centerY))
      drawCircle(color = stopColor.copy(alpha = 0.65f), radius = 2.2.dp.toPx(), center = Offset(burnX, centerY))
      repeat(4) { index ->
        val phase = sparkPhase + index * 1.7f
        val drift = (sin(phase) * 4.dp.toPx()) - index * 2.dp.toPx()
        val lift = -5.dp.toPx() - index * 1.8.dp.toPx() - sin(phase * 1.4f) * 3.dp.toPx()
        val alpha = 0.72f - index * 0.13f
        drawCircle(
          color = emberColor.copy(alpha = alpha.coerceIn(0.18f, 0.72f)),
          radius = (2.5f - index * 0.28f).dp.toPx(),
          center = Offset(burnX + drift, centerY + lift),
        )
      }
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

internal fun leisureRemainingContextText(
  reserveMinutes: Double,
  sleepProtection: Boolean,
  sleepProtectionMultiplier: Double,
  sleepProtectionStartHour: Int,
): String {
  lowBalanceText(reserveMinutes)?.let { return it }
  val reserve = "${compactMinutes(reserveMinutes)} reserve"
  val multiplier = sleepProtectionMultiplier.formatOne()
  return if (sleepProtection) {
    "$reserve · ${multiplier}x cost now"
  } else {
    "$reserve · ${multiplier}x after ${sleepProtectionStartHour.activeHourLabel()}"
  }
}

private fun Int.activeHourLabel(): String = "%02d:00".format(this.coerceIn(0, 23))

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun LeisureTimerSurfaceSleepProtectionPreview() {
  FocusWellTheme(dynamicColor = false) {
    LeisureTimerSurface(
      remaining = "1:06:42",
      elapsed = "1h",
      progress = 0.58f,
      supporting = null,
      sleepProtection = true,
      sleepProtectionMultiplier = 2.0,
    )
  }
}
