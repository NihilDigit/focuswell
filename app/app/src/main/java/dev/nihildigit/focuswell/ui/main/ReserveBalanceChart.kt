package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.LedgerEntry
import kotlin.math.ceil

@Composable
internal fun NetBalanceChart(entries: List<LedgerEntry>, rules: FocusWellRules) {
  val points = remember(entries, rules) { sevenDayNetPoints(entries, rules) }
  val total = points.sumOf { it.netMinutes }
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = CalmPanelShape,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text("7-day net", style = MaterialTheme.typography.titleLarge)
          Text("Daily net movement", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
          signedMinutes(total),
          style = tabularNumbers(MaterialTheme.typography.titleMedium),
          color =
            when {
              total > 0.0 -> MaterialTheme.colorScheme.primary
              total < 0.0 -> MaterialTheme.colorScheme.tertiary
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
      }
      NetLineChart(points = points)
    }
  }
}

@Composable
internal fun NetLineChart(points: List<DailyNetPoint>, modifier: Modifier = Modifier) {
  val primary = MaterialTheme.colorScheme.primary
  val tertiary = MaterialTheme.colorScheme.tertiary
  val outline = MaterialTheme.colorScheme.outlineVariant
  val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
  Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
    val maxAbs = points.maxOfOrNull { kotlin.math.abs(it.netMinutes) }?.coerceAtLeast(15.0) ?: 15.0
    val axisMax = (ceil(maxAbs / 15.0) * 15.0).coerceAtLeast(15.0)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Column(
        modifier = Modifier.width(46.dp).height(146.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
      ) {
        Text(signedMinutes(axisMax), style = tabularNumbers(MaterialTheme.typography.labelSmall), color = onSurfaceVariant)
        Text("0", style = tabularNumbers(MaterialTheme.typography.labelSmall), color = onSurfaceVariant)
        Text(signedMinutes(-axisMax), style = tabularNumbers(MaterialTheme.typography.labelSmall), color = onSurfaceVariant)
      }
      Canvas(modifier = Modifier.weight(1f).height(146.dp)) {
        val horizontalPadding = 8.dp.toPx()
        val topPadding = 12.dp.toPx()
        val bottomPadding = 14.dp.toPx()
        val chartWidth = size.width - horizontalPadding * 2
        val chartHeight = size.height - topPadding - bottomPadding
        val zeroY = topPadding + chartHeight / 2f
        listOf(topPadding, zeroY, topPadding + chartHeight).forEach { y ->
          drawLine(
            color = outline,
            start = Offset(horizontalPadding, y),
            end = Offset(size.width - horizontalPadding, y),
            strokeWidth = 1.dp.toPx(),
          )
        }
        val offsets =
          points.mapIndexed { index, point ->
            val x = horizontalPadding + chartWidth * index / (points.size - 1).coerceAtLeast(1)
            val y = zeroY - (point.netMinutes / axisMax).toFloat() * chartHeight / 2f
            Offset(x, y)
          }
        if (offsets.size > 1) {
          val path =
            Path().apply {
              moveTo(offsets.first().x, offsets.first().y)
              offsets.drop(1).forEach { lineTo(it.x, it.y) }
            }
          drawPath(path, color = primary, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        }
        offsets.forEachIndexed { index, offset ->
          val point = points[index]
          val color = if (point.netMinutes < 0.0) tertiary else primary
          drawCircle(color = color.copy(alpha = 0.18f), radius = 8.dp.toPx(), center = offset)
          drawCircle(color = color, radius = 4.dp.toPx(), center = offset)
        }
      }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Spacer(Modifier.width(54.dp))
      points.forEach {
        Text(
          it.label,
          style = MaterialTheme.typography.labelSmall,
          color = onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}
