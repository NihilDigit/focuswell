package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
internal fun ReserveWellDrawing(
  fill: Float,
  phase: Float,
  shimmer: Float,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  Canvas(modifier = modifier) {
    val wallColor = colorScheme.onPrimaryContainer
    val rippleColor = colorScheme.primary
    val glintColor = colorScheme.secondary
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
      color = wallColor.copy(alpha = 0.34f),
      startAngle = 188f,
      sweepAngle = 238f,
      useCenter = false,
      topLeft = Offset(rim.left, rim.top),
      size = Size(rim.width, rim.height),
      style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
    )
    drawArc(
      color = wallColor.copy(alpha = 0.22f),
      startAngle = 12f,
      sweepAngle = 128f,
      useCenter = false,
      topLeft = Offset(rim.left + 2.dp.toPx(), rim.top + 1.dp.toPx()),
      size = Size(rim.width - 4.dp.toPx(), rim.height - 2.dp.toPx()),
      style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
    )
    drawArc(
      color = wallColor.copy(alpha = 0.2f),
      startAngle = 196f,
      sweepAngle = 118f,
      useCenter = false,
      topLeft = Offset(rim.left + 16.dp.toPx(), rim.top + 11.dp.toPx()),
      size = Size(rim.width - 32.dp.toPx(), rim.height - 20.dp.toPx()),
      style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
    )
    drawArc(
      color = wallColor.copy(alpha = 0.16f),
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
        color = if (index == 1) glintColor.copy(alpha = alpha * 0.75f) else rippleColor.copy(alpha = alpha),
        style = Stroke(width = if (index == 1) 1.7.dp.toPx() else 3.dp.toPx(), cap = StrokeCap.Round),
      )
    }
  }
}
