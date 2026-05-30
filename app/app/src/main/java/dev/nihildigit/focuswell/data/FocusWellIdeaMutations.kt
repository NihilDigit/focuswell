package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.Idea
import dev.nihildigit.focuswell.domain.IdeaChecklistItem
import dev.nihildigit.focuswell.domain.IdeaQuadrant
import java.time.Instant

internal fun FocusWellUiState.withAddedIdea(
  text: String,
  createdAt: Instant,
): FocusWellUiState {
  val trimmed = text.trim()
  if (trimmed.isEmpty()) return this
  return copy(
    ideas =
      listOf(
        Idea(
          id = FocusWellIds.idea(createdAt),
          text = trimmed,
          quadrant = IdeaQuadrant.Inbox,
          createdAt = createdAt,
          updatedAt = createdAt,
        )
      ) + ideas
  )
}

internal fun FocusWellUiState.withMovedIdea(
  id: String,
  quadrant: IdeaQuadrant,
  updatedAt: Instant,
): FocusWellUiState =
  copy(
    ideas =
      ideas.map {
        if (it.id == id && it.archivedAt == null) it.copy(quadrant = quadrant, updatedAt = updatedAt) else it
      }
  )

internal fun FocusWellUiState.withUpdatedIdea(
  id: String,
  text: String,
  checklist: List<IdeaChecklistItem>,
  updatedAt: Instant,
): FocusWellUiState {
  val trimmed = text.trim()
  if (trimmed.isEmpty()) return this
  val cleanedChecklist =
    checklist
      .map { it.copy(text = it.text.trim()) }
      .filter { it.text.isNotEmpty() }
  return copy(
    ideas =
      ideas.map {
        if (it.id == id && it.archivedAt == null) {
          it.copy(text = trimmed, checklist = cleanedChecklist, updatedAt = updatedAt)
        } else {
          it
        }
      }
  )
}

internal fun FocusWellUiState.withArchivedIdea(
  id: String,
  archivedAt: Instant,
): FocusWellUiState =
  copy(
    ideas =
      ideas.map {
        if (it.id == id && it.archivedAt == null) it.copy(archivedAt = archivedAt, updatedAt = archivedAt) else it
      }
  )
