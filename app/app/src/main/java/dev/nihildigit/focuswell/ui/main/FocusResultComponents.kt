package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import dev.nihildigit.focuswell.domain.focusOutcomeMultiplier
import dev.nihildigit.focuswell.usage.FocusAppUsage

@Composable
internal fun SettlementFormula(
  rawMinutes: Double,
  correctionMinutes: Double,
  adjustedMinutes: Double,
  typeRate: Double,
  tagMultiplier: Double,
  outcomeMultiplier: Double,
  projectedEarned: Double,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Settlement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    FormulaLine("Raw focus", compactMinutes(rawMinutes))
    FormulaLine("Deducted app time", signedCompactMinutes(-correctionMinutes))
    FormulaLine("Counted focus", compactMinutes(adjustedMinutes))
    FormulaLine("Type rate", "${typeRate.formatThree()}x")
    FormulaLine("Tag multiplier", "${tagMultiplier.formatThree()}x")
    FormulaLine("Outcome multiplier", "${outcomeMultiplier.formatOne()}x")
    HorizontalDivider()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Formula", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        "${compactMinutes(adjustedMinutes)} × ${typeRate.formatThree()} × ${tagMultiplier.formatThree()} × ${outcomeMultiplier.formatOne()}",
        style = tabularNumbers(MaterialTheme.typography.labelLarge),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Earned reserve", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      Text(
        signedCompactMinutes(projectedEarned),
        style = tabularNumbers(MaterialTheme.typography.titleMedium),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
private fun FormulaLine(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = tabularNumbers(MaterialTheme.typography.bodyMedium), fontWeight = FontWeight.SemiBold)
  }
}

@Composable
internal fun FocusUsageCorrection(
  appUsages: List<FocusAppUsage>,
  focusPackages: Set<String>,
  onToggleFocusPackage: (String) -> Unit,
) {
  CalmPanel {
    Text("App correction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    appUsages.forEach { usage ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        usage.icon?.let { icon ->
          val bitmap = remember(icon) { icon.toBitmap(width = 48, height = 48).asImageBitmap() }
          Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(32.dp))
        } ?: Icon(Icons.Rounded.RadioButtonUnchecked, contentDescription = null, modifier = Modifier.size(32.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(usage.appName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(compactMinutes(usage.durationMillis / 60_000.0), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = usage.packageName in focusPackages,
            onCheckedChange = { onToggleFocusPackage(usage.packageName) },
          )
          Text("Count", style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}

@Composable
internal fun ResultChoice(
  label: String,
  supporting: String = "${focusOutcomeMultiplier(label).formatOne()}x",
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val (icon, tone) = focusOutcomeVisual(label)
  Surface(
    onClick = onClick,
    color = if (selected) tone.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainer,
    contentColor = if (selected) tone else MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(20.dp),
    modifier =
      modifier
        .height(58.dp)
        .border(
          width = 1.dp,
          color = if (selected) tone.copy(alpha = 0.34f) else MaterialTheme.colorScheme.outlineVariant,
          shape = RoundedCornerShape(20.dp),
        ),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (selected) tone else MaterialTheme.colorScheme.onSurfaceVariant)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(supporting, style = MaterialTheme.typography.labelSmall, color = if (selected) tone else MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}
