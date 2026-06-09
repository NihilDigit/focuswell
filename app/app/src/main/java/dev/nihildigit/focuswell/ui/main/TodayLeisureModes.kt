package dev.nihildigit.focuswell.ui.main

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.time.toKotlinInstant
import java.time.Instant
import kotlin.time.Duration

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
  val normalizedRules = rules.normalized()
  val spent = TimeAccounting.leisureCostMinutes(leisure.startedAt, now, rules = normalizedRules)
  val liveRemainingMinutes = (reserveMinutes - spent).coerceAtLeast(0.0)
  val elapsed = (now.toKotlinInstant() - leisure.startedAt.toKotlinInstant()).coerceAtLeast(Duration.ZERO)
  val depletionAt = TimeAccounting.instantWhenLeisureCostReaches(leisure.startedAt, reserveMinutes, rules = normalizedRules)
  val totalDisplayDuration = (depletionAt.toKotlinInstant() - leisure.startedAt.toKotlinInstant()).coerceAtLeast(Duration.ZERO)
  val displayRemaining = (depletionAt.toKotlinInstant() - now.toKotlinInstant()).coerceAtLeast(Duration.ZERO)
  val isSleepProtection = TimeAccounting.isSleepProtection(now, rules = normalizedRules)
  if (liveRemainingMinutes <= 0.0) {
    LaunchedEffect(leisure.startedAt) {
      onEndLeisure()
    }
    DepletedSurface(rules = normalizedRules, onEndLeisure = onEndLeisure)
    return
  }
  val progress =
    if (totalDisplayDuration <= Duration.ZERO) {
      0f
    } else {
      (displayRemaining.inWholeMilliseconds.toDouble() / totalDisplayDuration.inWholeMilliseconds).toFloat().coerceIn(0f, 1f)
    }
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    LeisureTimerSurface(
      remaining = formatPreciseDuration(displayRemaining),
      elapsed = formatDuration(elapsed),
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
          StatusBadge("${compactMinutes(normalizedRules.dailyGrantMinutes)} at ${normalizedRules.safeDayBoundaryHour.activeHourLabel()}", tone)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Leisure is out", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
          Text(
            "Your balance is at zero. Daily can keep saving, but Leisure stays locked until one uninterrupted 2h focus gives a 3h restart bonus.",
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
              "${normalizedRules.safeDayBoundaryHour.activeHourLabel()} · ${compactMinutes(normalizedRules.dailyGrantMinutes)}",
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
