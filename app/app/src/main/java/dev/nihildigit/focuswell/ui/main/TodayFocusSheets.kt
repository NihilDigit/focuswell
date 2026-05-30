package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.focusOutcomeMultiplier
import dev.nihildigit.focuswell.time.toKotlinInstant
import dev.nihildigit.focuswell.usage.FocusAppUsage
import dev.nihildigit.focuswell.usage.focusAppUsage
import dev.nihildigit.focuswell.usage.hasUsageAccess
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FocusResultSheet(
  outcome: String,
  focus: ActiveMode.Focus,
  onOutcomeChange: (String) -> Unit,
  sheetState: SheetState,
  onDismiss: () -> Unit,
  onSave: (Double) -> Unit,
) {
  val context = LocalContext.current
  val hasCorrection = remember { hasUsageAccess(context) }
  var appUsages by remember(focus.startedAt) { mutableStateOf<List<FocusAppUsage>>(emptyList()) }
  var focusPackages by remember(focus.startedAt) { mutableStateOf(setOf<String>()) }
  val correctionMinutes = appUsages.filterNot { it.packageName in focusPackages }.sumOf { it.durationMillis } / 60_000.0
  val activeEnd = remember { Instant.now() }
  val rawMinutes = focus.rawActiveMinutesUntil(activeEnd)
  val adjustedMinutes = (rawMinutes - correctionMinutes).coerceAtLeast(0.0)
  val tagMultiplier = focus.tag?.multiplier ?: 1.0
  val outcomeMultiplier = focusOutcomeMultiplier(outcome)
  val projectedEarned = adjustedMinutes * focus.type.rate * tagMultiplier * outcomeMultiplier

  LaunchedEffect(hasCorrection, focus.startedAt, activeEnd) {
    if (hasCorrection) {
      appUsages = withContext(Dispatchers.Default) { focusAppUsage(context, focus.startedAt, activeEnd) }
    }
  }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier
          .padding(horizontal = 20.dp)
          .imePadding()
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Session outcome", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
          "Pick one result. Add a short note only when it will help later.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      FocusOutcomeOptions.chunked(2).forEach { rowOptions ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          rowOptions.forEach { option ->
            ResultChoice(
              label = option,
              supporting = "${focusOutcomeMultiplier(option).formatOne()}x",
              selected = outcome == option,
              onClick = { onOutcomeChange(option) },
              modifier = Modifier.weight(1f),
            )
          }
          if (rowOptions.size == 1) {
            Spacer(Modifier.weight(1f))
          }
        }
      }
      if (hasCorrection && appUsages.isNotEmpty()) {
        FocusUsageCorrection(
          appUsages = appUsages,
          focusPackages = focusPackages,
          onToggleFocusPackage = { packageName ->
            focusPackages =
              if (packageName in focusPackages) focusPackages - packageName else focusPackages + packageName
          },
        )
      }
      CalmPanel {
        SettlementFormula(
          rawMinutes = rawMinutes,
          correctionMinutes = correctionMinutes,
          adjustedMinutes = adjustedMinutes,
          typeRate = focus.type.rate,
          tagMultiplier = tagMultiplier,
          outcomeMultiplier = outcomeMultiplier,
          projectedEarned = projectedEarned,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(54.dp), shape = ControlStartShape) {
          Text("Cancel")
        }
        Button(
          enabled = outcome.isNotBlank(),
          onClick = { onSave(correctionMinutes) },
          modifier = Modifier.weight(1f).height(54.dp),
          shape = ControlEndShape,
        ) {
          Text("Save result")
        }
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
internal fun FocusResultNoteDialog(
  outcome: String,
  onSkip: () -> Unit,
  onSave: (String) -> Unit,
) {
  var note by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onSkip,
    title = { Text("Add a note?") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          "Optional context for this $outcome session.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Note") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      TextButton(onClick = { onSave(note) }) {
        Text(if (note.isBlank()) "Save without note" else "Save note")
      }
    },
  )
}

private fun ActiveMode.Focus.rawActiveMinutesUntil(activeEnd: Instant): Double {
  val activeDuration =
    (activeEnd.toKotlinInstant() - startedAt.toKotlinInstant() - pausedDurationMillis.milliseconds)
      .coerceAtLeast(Duration.ZERO)
  return activeDuration.inWholeMilliseconds / 60_000.0
}
