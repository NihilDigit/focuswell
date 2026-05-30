package dev.nihildigit.focuswell.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.nihildigit.focuswell.domain.SessionType
import java.time.Instant
import kotlinx.coroutines.delay
import kotlin.time.Duration

@Composable
internal fun rememberNow(paused: Boolean = false): Instant {
  var now by remember { mutableStateOf(Instant.now()) }
  LaunchedEffect(paused) {
    while (!paused) {
      now = Instant.now()
      delay(250)
    }
  }
  return now
}

internal fun formatDuration(duration: Duration): String {
  val totalSeconds = duration.inWholeSeconds.coerceAtLeast(0)
  return formatDurationSeconds(totalSeconds)
}

private fun formatDurationSeconds(totalSeconds: Long): String {
  if (totalSeconds < 60) return "<1m"
  val totalMinutes = (totalSeconds / 60).coerceAtLeast(0)
  return "${totalMinutes}m"
}

internal fun formatPreciseDuration(duration: Duration): String {
  val totalSeconds = duration.inWholeSeconds.coerceAtLeast(0)
  return formatPreciseDurationSeconds(totalSeconds)
}

private fun formatPreciseDurationSeconds(totalSeconds: Long): String {
  val hours = totalSeconds / 3600
  val minutes = (totalSeconds % 3600) / 60
  val seconds = totalSeconds % 60
  return if (hours > 0) {
    "%d:%02d:%02d".format(hours, minutes, seconds)
  } else {
    "%02d:%02d".format(minutes, seconds)
  }
}

internal fun effectiveRate(type: SessionType, tagMultiplier: Double): String =
  (type.rate * tagMultiplier).formatThree()
