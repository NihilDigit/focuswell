package dev.nihildigit.focuswell.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.nihildigit.focuswell.domain.DailyTracker
import kotlin.math.roundToInt

internal fun signedMinutes(minutes: Double): String {
  if (minutes > 0.0 && minutes < 1.0) return "+<1m"
  if (minutes < 0.0 && minutes > -1.0) return "-<1m"
  val rounded = minutes.roundToInt()
  return when {
    rounded > 0 -> "+${rounded}m"
    rounded < 0 -> "${rounded}m"
    else -> "0"
  }
}

internal fun compactMinutes(minutes: Double): String {
  if (minutes == 0.0) return "0"
  if (minutes > 0.0 && minutes < 1.0) return "<1m"
  if (minutes < 0.0 && minutes > -1.0) return "-<1m"
  return "${minutes.roundToInt()}m"
}

internal fun signedCompactMinutes(minutes: Double): String =
  when {
    minutes > 0.0 && minutes < 1.0 -> "+<1m"
    minutes < 0.0 && minutes > -1.0 -> "-<1m"
    else -> signedMinutes(minutes)
  }

internal fun trackerProgress(tracker: DailyTracker): Float {
  if (tracker.completed) return 1f
  val label = tracker.progressLabel ?: return 0f
  val parts = label.split("/")
  if (parts.size != 2) return 0f
  val current = parseDurationMinutes(parts[0])
  val target = parseDurationMinutes(parts[1])
  if (target <= 0.0) return 0f
  return (current / target).toFloat().coerceIn(0f, 1f)
}

internal fun trackerStatusText(tracker: DailyTracker): String {
  return tracker.progressLabel
    ?: when {
      tracker.completed -> "Done"
      else -> "Open"
    }
}

@Composable
internal fun focusOutcomeVisual(outcome: String): Pair<ImageVector, Color> {
  return when (outcome) {
    "As planned" -> Icons.Rounded.CheckCircle to MaterialTheme.colorScheme.primary
    "Partial" -> Icons.Rounded.RadioButtonUnchecked to MaterialTheme.colorScheme.secondary
    "Drifted" -> Icons.Rounded.Pause to MaterialTheme.colorScheme.tertiary
    "Interrupted" -> Icons.Rounded.Stop to MaterialTheme.colorScheme.error
    else -> Icons.Rounded.RadioButtonUnchecked to MaterialTheme.colorScheme.onSurfaceVariant
  }
}

internal fun parseOutcomeResult(result: String): Pair<String, String> {
  val trimmed = result.trim()
  val direct = FocusOutcomeOptions.firstOrNull { it == trimmed }
  if (direct != null) return direct to ""
  val option = FocusOutcomeOptions.firstOrNull { trimmed.startsWith("$it · ") }
  if (option != null) return option to trimmed.removePrefix("$option · ").trim()
  return FocusOutcomeOptions.first() to trimmed
}

internal fun formatOutcomeResult(outcome: String, note: String): String {
  val trimmedNote = note.trim()
  return if (trimmedNote.isBlank()) outcome else "$outcome · $trimmedNote"
}

internal fun parseDurationMinutes(text: String): Double {
  val trimmed = text.trim()
  var total = 0.0
  Regex("""(\d+(?:\.\d+)?)\s*h""").find(trimmed)?.let { total += it.groupValues[1].toDoubleOrNull()?.times(60.0) ?: 0.0 }
  Regex("""(\d+(?:\.\d+)?)\s*m""").find(trimmed)?.let { total += it.groupValues[1].toDoubleOrNull() ?: 0.0 }
  if (total > 0.0) return total
  return trimmed.toDoubleOrNull() ?: 0.0
}

internal fun Double.formatOne(): String = "%.1f".format(this)

internal fun Double.formatThree(): String {
  val text = "%.3f".format(this)
  return text.trimEnd('0').trimEnd('.')
}
