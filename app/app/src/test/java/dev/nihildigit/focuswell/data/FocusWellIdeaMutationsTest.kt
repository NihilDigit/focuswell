package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.Idea
import dev.nihildigit.focuswell.domain.IdeaChecklistItem
import dev.nihildigit.focuswell.domain.IdeaQuadrant
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FocusWellIdeaMutationsTest {
  private val createdAt = Instant.parse("2026-05-20T05:00:00Z")
  private val updatedAt = Instant.parse("2026-05-20T06:00:00Z")

  @Test
  fun withAddedIdea_trimsTextAndPrependsInboxIdea() {
    val state = FocusWellUiState(ideas = listOf(idea(id = "old", text = "old")))
    val updated = state.withAddedIdea("  Draft proof  ", createdAt = createdAt)

    assertEquals("Draft proof", updated.ideas.first().text)
    assertEquals(IdeaQuadrant.Inbox, updated.ideas.first().quadrant)
    assertEquals("old", updated.ideas.last().id)
  }

  @Test
  fun withAddedIdea_ignoresBlankText() {
    val state = FocusWellUiState()

    assertSame(state, state.withAddedIdea(" ", createdAt = createdAt))
  }

  @Test
  fun withUpdatedIdea_cleansChecklistAndSkipsArchivedIdeas() {
    val archived = idea(id = "archived", text = "done", archivedAt = updatedAt)
    val active = idea(id = "active", text = "old")
    val state = FocusWellUiState(ideas = listOf(archived, active))

    val updated =
      state.withUpdatedIdea(
        id = "active",
        text = "  new  ",
        checklist =
          listOf(
            IdeaChecklistItem(id = "keep", text = "  Ship  "),
            IdeaChecklistItem(id = "drop", text = " "),
          ),
        updatedAt = updatedAt,
      )

    val edited = updated.ideas.first { it.id == "active" }
    assertEquals("new", edited.text)
    assertEquals(listOf(IdeaChecklistItem(id = "keep", text = "Ship")), edited.checklist)
    assertEquals("done", updated.ideas.first { it.id == "archived" }.text)
  }

  @Test
  fun withMovedIdea_andArchive_ignoreArchivedIdeas() {
    val state =
      FocusWellUiState(
        ideas =
          listOf(
            idea(id = "active", text = "active"),
            idea(id = "archived", text = "archived", archivedAt = createdAt),
          ),
      )

    val moved =
      state
        .withMovedIdea(id = "active", quadrant = IdeaQuadrant.Schedule, updatedAt = updatedAt)
        .withMovedIdea(id = "archived", quadrant = IdeaQuadrant.Explore, updatedAt = updatedAt)
    val archived = moved.withArchivedIdea(id = "active", archivedAt = updatedAt)

    assertEquals(IdeaQuadrant.Schedule, moved.ideas.first { it.id == "active" }.quadrant)
    assertEquals(IdeaQuadrant.Inbox, moved.ideas.first { it.id == "archived" }.quadrant)
    assertEquals(updatedAt, archived.ideas.first { it.id == "active" }.archivedAt)
  }

  private fun idea(
    id: String,
    text: String,
    archivedAt: Instant? = null,
  ): Idea =
    Idea(
      id = id,
      text = text,
      quadrant = IdeaQuadrant.Inbox,
      createdAt = createdAt,
      updatedAt = createdAt,
      archivedAt = archivedAt,
    )
}
