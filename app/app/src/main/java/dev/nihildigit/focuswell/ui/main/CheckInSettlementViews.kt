package dev.nihildigit.focuswell.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.FocusWellRules
import kotlinx.coroutines.delay

@Composable
internal fun CheckInSettlementScreen(
  incomeMinutes: Double,
  settlement: CheckInSettlementSummary,
  rules: FocusWellRules,
  showIncome: Boolean = true,
  onBack: () -> Unit,
  onDone: () -> Unit,
) {
  LazyColumn(
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      CheckInStepHeader(
        title = "Settlement",
        subtitle = "Review the final accounting",
      )
    }
    if (showIncome) {
      item {
        AnimatedAccountingItem(
          label = "Income",
          amount = incomeMinutes,
          tone = MaterialTheme.colorScheme.primary,
          delayMillis = 0,
        )
      }
    }
    item {
      AnimatedAccountingItem(
        label = "Fair Use",
        amount = settlement.fairUseCount.toDouble(),
        valueText = "${settlement.fairUseCount} / ${settlement.reviewedSegmentCount}",
        tone = MaterialTheme.colorScheme.secondary,
        delayMillis = if (showIncome) 170 else 0,
      )
    }
    item {
      AnimatedAccountingItem(
        label = "Phone correction",
        amount = -settlement.deducted,
        valueText = signedCompactMinutes(-settlement.deducted),
        tone = MaterialTheme.colorScheme.tertiary,
        delayMillis = if (showIncome) 340 else 170,
      )
    }
    item {
      CalmPanel {
        Text("Formula", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        CheckInFormulaLine("Phone cost", signedCompactMinutes(-settlement.phoneCost))
        CheckInFormulaLine("Deducted", signedCompactMinutes(-settlement.deducted))
        CheckInFormulaLine("Remaining", compactMinutes(settlement.remaining))
      }
    }
    if (settlement.exceeded) {
      item { FrozenDailyGrantPanel(rules = rules) }
    }
    item {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(54.dp), shape = ControlStartShape) {
          Text("Back")
        }
        Button(onClick = onDone, modifier = Modifier.weight(1f).height(54.dp), shape = ControlEndShape) {
          Text("Done")
        }
      }
    }
  }
}

@Composable
internal fun CheckInStepHeader(title: String, subtitle: String) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
internal fun AnimatedAccountingItem(
  label: String,
  amount: Double,
  tone: Color,
  valueText: String? = null,
  delayMillis: Int = 0,
) {
  var entered by remember(label, amount, valueText) { mutableStateOf(false) }
  var checked by remember(label, amount, valueText) { mutableStateOf(false) }
  LaunchedEffect(label, amount, valueText, delayMillis) {
    entered = false
    checked = false
    delay(260L + delayMillis)
    entered = true
    delay(280L)
    checked = true
  }
  val entranceAlpha by animateFloatAsState(
    targetValue = if (entered) 1f else 0f,
    animationSpec = tween(durationMillis = 240),
    label = "checkin-accounting-alpha",
  )
  val entranceOffset by animateDpAsState(
    targetValue = if (entered) 0.dp else 18.dp,
    animationSpec = tween(durationMillis = 260),
    label = "checkin-accounting-offset",
  )
  val animatedAmount by animateFloatAsState(
    targetValue = if (checked) amount.toFloat() else 0f,
    animationSpec = tween(durationMillis = 680, easing = LinearEasing),
    label = "checkin-accounting-amount",
  )
  val container by animateColorAsState(
    targetValue = if (checked) tone.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainer,
    animationSpec = focusWellFastEffectsSpec(),
    label = "checkin-accounting-container",
  )
  Surface(
    color = container,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = LedgerRowShape,
    modifier =
      Modifier
        .fillMaxWidth()
        .offset(y = entranceOffset)
        .alpha(entranceAlpha)
        .heightIn(min = 64.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(color = tone, contentColor = MaterialTheme.colorScheme.onPrimary, shape = CircleShape, modifier = Modifier.size(32.dp)) {
        Box(contentAlignment = Alignment.Center) {
          AnimatedContent(
            targetState = checked,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(80)) },
            label = "checkin-accounting-check",
          ) { visible ->
            if (visible) {
              Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(19.dp))
            } else {
              Spacer(Modifier.size(19.dp))
            }
          }
        }
      }
      Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
      Text(
        valueText?.takeIf { checked } ?: signedMinutes(animatedAmount.toDouble()),
        style = tabularNumbers(MaterialTheme.typography.titleMedium),
        fontWeight = FontWeight.Bold,
        color = tone,
      )
    }
  }
}

@Composable
private fun CheckInFormulaLine(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = tabularNumbers(MaterialTheme.typography.bodyMedium), fontWeight = FontWeight.SemiBold)
  }
}

@Composable
private fun FrozenDailyGrantPanel(rules: FocusWellRules) {
  Surface(
    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.52f),
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(18.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = CircleShape, modifier = Modifier.size(46.dp)) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Rounded.AcUnit, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Daily grant paused", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Unconditional grant only", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Text(frozenDailyGrantLabel(rules), style = tabularNumbers(MaterialTheme.typography.titleLarge), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
    }
  }
}
