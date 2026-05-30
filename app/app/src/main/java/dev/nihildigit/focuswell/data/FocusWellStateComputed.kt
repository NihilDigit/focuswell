package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.FocusWellUiState

internal fun FocusWellUiState.withComputedTrackers(): FocusWellUiState {
  val activeFocusRecords = focusRecords.filter { it.deletedAt == null && it.dailyDate == dailyDate }
  val computed =
    trackers.map { tracker ->
      val tagName = tracker.ruleTagName
      val target = tracker.ruleTargetMinutes
      if (tracker.archivedAt != null || tagName == null || target == null) {
        tracker
      } else {
        val minutes =
          activeFocusRecords
            .filter { it.tagName?.equals(tagName, ignoreCase = true) == true }
            .sumOf { it.activeDurationMinutes }
        tracker.copy(
          completed = minutes >= target,
          progressLabel = "${minutes.roundMinutes()} / ${target.roundTarget()}",
        )
      }
    }
  return copy(trackers = computed)
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
