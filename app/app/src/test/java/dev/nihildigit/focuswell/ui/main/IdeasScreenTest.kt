package dev.nihildigit.focuswell.ui.main

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class IdeasScreenTest {
  @Test
  @OptIn(ExperimentalTime::class)
  fun newIdeaChecklistItem_usesInjectedCreationInstant() {
    val item =
      newIdeaChecklistItem(
        text = "Sketch matrix",
        index = 2,
        createdAt = Instant.parse("2026-05-20T05:00:00Z"),
      )

    assertEquals("task-1779253200000-2", item.id)
    assertEquals("Sketch matrix", item.text)
  }
}
