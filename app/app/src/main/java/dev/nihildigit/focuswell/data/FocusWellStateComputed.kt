package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.time.toKotlinInstant
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal fun FocusWellUiState.withComputedTrackers(now: Instant? = null): FocusWellUiState {
  val activeFocusRecords = focusRecords.filter { it.deletedAt == null && it.dailyDate == dailyDate }
  val activeFocus = activeMode as? ActiveMode.Focus
  val includeActiveFocus =
    activeFocus != null &&
      now != null &&
      TimeAccounting.dailyDate(now, rules = rules).toString() == dailyDate
  val computed =
    trackers.map { tracker ->
      val tagName = tracker.ruleTagName
      val target = tracker.ruleTargetMinutes
      if (tracker.archivedAt != null || tagName == null || target == null) {
        tracker
      } else {
        val recordMinutes =
          activeFocusRecords
            .filter { it.tagName?.equals(tagName, ignoreCase = true) == true }
            .sumOf { it.activeDurationMinutes }
        val activeMinutes =
          if (includeActiveFocus && activeFocus.tag?.name?.equals(tagName, ignoreCase = true) == true) {
            activeFocus.activeDurationMinutesUntil(now)
          } else {
            0.0
          }
        val minutes = recordMinutes + activeMinutes
        tracker.copy(
          completed = recordMinutes >= target,
          progressLabel = "${minutes.roundMinutes()} / ${target.roundTarget()}",
        )
      }
    }
  return copy(trackers = computed)
}

private fun ActiveMode.Focus.activeDurationMinutesUntil(now: Instant): Double {
  val elapsedEnd = if (paused && pausedAt != null) pausedAt else now
  val activeDuration =
    (elapsedEnd.toKotlinInstant() - startedAt.toKotlinInstant() - pausedDurationMillis.milliseconds)
      .coerceAtLeast(Duration.ZERO)
  return activeDuration.inWholeMilliseconds / 60_000.0
}

internal fun FocusWellUiState.withLedgerBackedReserve(): FocusWellUiState =
  copy(reserveMinutes = ledger.sumOf { it.deltaMinutes }.coerceAtLeast(0.0))

internal fun Double.roundMinutes(): String {
  val rounded = toInt()
  return if (rounded >= 60) "${rounded / 60}h ${rounded % 60}m" else "${rounded}m"
}

internal fun Double.roundTarget(): String {
  val rounded = toInt()
  return if (rounded % 60 == 0) "${rounded / 60}h" else "${rounded}m"
}
