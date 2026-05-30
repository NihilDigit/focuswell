package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.TagConfig
import java.time.Instant

internal fun FocusWellUiState.withToggledManualTracker(id: String): FocusWellUiState =
  copy(
    trackers =
      trackers.map {
        if (it.id == id && it.ruleTagName == null) it.copy(completed = !it.completed) else it
      }
  )

internal fun FocusWellUiState.withAddedTag(
  name: String,
  multiplier: Double,
  createdAt: Instant,
): FocusWellUiState {
  val trimmed = name.trim()
  if (trimmed.isEmpty()) return this
  if (tags.any { it.name.equals(trimmed, ignoreCase = true) && it.archivedAt == null }) return this
  return copy(
    tags =
      tags +
        TagConfig(
          id = FocusWellIds.tag(createdAt),
          name = trimmed,
          multiplier = multiplier.coerceAtLeast(0.0),
        )
  )
}

internal fun FocusWellUiState.withArchivedTag(
  id: String,
  archivedAt: Instant,
): FocusWellUiState =
  copy(tags = tags.map { if (it.id == id) it.copy(archivedAt = archivedAt) else it })

internal fun FocusWellUiState.withUpdatedTag(
  id: String,
  name: String,
  multiplier: Double,
): FocusWellUiState {
  val trimmed = name.trim()
  if (trimmed.isEmpty()) return this
  if (tags.any { it.id != id && it.name.equals(trimmed, ignoreCase = true) && it.archivedAt == null }) return this
  return copy(
    tags =
      tags.map {
        if (it.id == id) it.copy(name = trimmed, multiplier = multiplier.coerceAtLeast(0.0)) else it
      }
  )
}

internal fun FocusWellUiState.withAddedManualTracker(
  label: String,
  rewardMinutes: Double,
  createdAt: Instant,
): FocusWellUiState {
  val trimmed = label.trim()
  if (trimmed.isEmpty()) return this
  return copy(
    trackers =
      trackers +
        DailyTracker(
          id = FocusWellIds.manualTracker(createdAt),
          label = trimmed,
          completed = false,
          rewardMinutes = rewardMinutes.coerceAtLeast(0.0),
        )
  )
}

internal fun FocusWellUiState.withAddedRuleTracker(
  label: String,
  tagName: String,
  targetMinutes: Double,
  rewardMinutes: Double,
  createdAt: Instant,
): FocusWellUiState {
  val trimmedLabel = label.trim()
  val trimmedTag = tagName.trim()
  if (trimmedLabel.isEmpty() || trimmedTag.isEmpty() || targetMinutes <= 0.0) return this
  return copy(
    trackers =
      trackers +
        DailyTracker(
          id = FocusWellIds.ruleTracker(createdAt),
          label = trimmedLabel,
          completed = false,
          rewardMinutes = rewardMinutes.coerceAtLeast(0.0),
          ruleTagName = trimmedTag,
          ruleTargetMinutes = targetMinutes,
        )
  )
}

internal fun FocusWellUiState.withArchivedTracker(
  id: String,
  archivedAt: Instant,
): FocusWellUiState =
  copy(trackers = trackers.map { if (it.id == id) it.copy(archivedAt = archivedAt) else it })

internal fun FocusWellUiState.withUpdatedManualTracker(
  id: String,
  label: String,
  rewardMinutes: Double,
): FocusWellUiState {
  val trimmed = label.trim()
  if (trimmed.isEmpty()) return this
  return copy(
    trackers =
      trackers.map {
        if (it.id == id) {
          it.copy(label = trimmed, rewardMinutes = rewardMinutes.coerceAtLeast(0.0))
        } else {
          it
        }
      }
  )
}

internal fun FocusWellUiState.withUpdatedRuleTracker(
  id: String,
  label: String,
  tagName: String,
  targetMinutes: Double,
  rewardMinutes: Double,
): FocusWellUiState {
  val trimmedLabel = label.trim()
  val trimmedTag = tagName.trim()
  if (trimmedLabel.isEmpty() || trimmedTag.isEmpty() || targetMinutes <= 0.0) return this
  return copy(
    trackers =
      trackers.map {
        if (it.id == id) {
          it.copy(
            label = trimmedLabel,
            rewardMinutes = rewardMinutes.coerceAtLeast(0.0),
            ruleTagName = trimmedTag,
            ruleTargetMinutes = targetMinutes,
          )
        } else {
          it
        }
      }
  )
}
