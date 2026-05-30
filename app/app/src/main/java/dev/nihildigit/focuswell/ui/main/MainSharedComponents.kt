package dev.nihildigit.focuswell.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.LedgerEntry

@Composable
internal fun TimerOrganism(
  label: String,
  time: String,
  tone: Color,
  progress: Float? = null,
  supporting: String? = null,
) {
  val animatedProgress by animateFloatAsState(
    targetValue = progress?.coerceIn(0f, 1f) ?: 0f,
    animationSpec = focusWellDefaultSpatialSpec(),
    label = "timer-progress",
  )
  Box(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 228.dp)
        .aspectRatio(1.38f)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh, ActiveTimerShape)
        .border(1.dp, tone.copy(alpha = 0.16f), ActiveTimerShape)
        .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    progress?.let {
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
          sweepAngle = 360f * animatedProgress.coerceIn(0f, 1f),
          useCenter = false,
          style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round),
          size = Size(size.minDimension, size.minDimension),
          topLeft = Offset((size.width - size.minDimension) / 2, 0f),
        )
      }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
      StatusBadge(label, tone)
      Text(
        time,
        style =
          tabularNumbers(if (time.length > 5) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge),
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
      )
      supporting?.let {
        Text(
          it,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
internal fun StatusBadge(text: String, tone: Color, modifier: Modifier = Modifier) {
  val container by animateColorAsState(
    targetValue = tone.copy(alpha = 0.14f),
    animationSpec = focusWellFastEffectsSpec(),
    label = "badge-container",
  )
  Surface(
    color = container,
    contentColor = tone,
    shape = CircleShape,
    modifier = modifier,
  ) {
    Text(
      text,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
      style = MaterialTheme.typography.labelLarge,
    )
  }
}

@Composable
internal fun LedgerRow(entry: LedgerEntry) {
  Surface(
    shape = LedgerRowShape,
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        signedMinutes(entry.deltaMinutes),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color =
          when {
            entry.deltaMinutes > 0.0 -> MaterialTheme.colorScheme.primary
            entry.deltaMinutes < 0.0 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          },
        modifier = Modifier.width(86.dp),
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(entry.title, fontWeight = FontWeight.SemiBold)
        entry.note?.let {
          Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }
}

@Composable
internal fun CalmPanel(content: @Composable ColumnScope.() -> Unit) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    shape = CalmPanelShape,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      content()
    }
  }
}

@Composable
internal fun SectionHeader(title: String, subtitle: String? = null) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    subtitle?.let {
      Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
