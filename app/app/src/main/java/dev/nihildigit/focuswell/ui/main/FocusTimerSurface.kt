package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.TagConfig
import dev.nihildigit.focuswell.theme.FocusWellExpressiveFontFamily
import dev.nihildigit.focuswell.theme.FocusWellTheme
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun FocusTimerSurface(
  focus: ActiveMode.Focus,
  elapsed: Duration,
) {
  val rate = focus.type.rate * (focus.tag?.multiplier ?: 1.0)
  val earnedNow = elapsed.inWholeMilliseconds.coerceAtLeast(0).toDouble() / 60_000.0 * rate
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
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FocusWellExpressiveFontFamily),
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
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
            Text("Elapsed", style = MaterialTheme.typography.labelLarge, color = content)
            Text(
              formatPreciseDuration(elapsed),
              style = tabularNumbers(MaterialTheme.typography.displayMedium),
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Clip,
            )
          }
          Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.52f),
            contentColor = content,
            shape = CalmPanelShape,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                "If ended now",
                style = MaterialTheme.typography.labelLarge,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              Text(
                signedCompactMinutes(earnedNow),
                style = tabularNumbers(MaterialTheme.typography.headlineSmall),
                fontWeight = FontWeight.ExtraBold,
                color = tone,
                maxLines = 1,
                softWrap = false,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FocusFieldDrawing(tone: Color, modifier: Modifier = Modifier) {
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

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun FocusTimerSurfaceLongElapsedPreview() {
  FocusWellTheme(dynamicColor = false) {
    FocusTimerSurface(
      focus =
        ActiveMode.Focus(
          task = "Draft release notes and verify Android build",
          type = SessionType.Output,
          tag = TagConfig(id = "deep", name = "Deep work", multiplier = 1.5),
          startedAt = Instant.parse("2026-05-31T08:00:00Z"),
          reminderSessionId = "preview",
        ),
      elapsed = 1.hours + 12.minutes,
    )
  }
}
