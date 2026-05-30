package dev.nihildigit.focuswell.ui.main

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.nihildigit.focuswell.domain.IdeaQuadrant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IdeaDragDropTest {
  @Test
  fun dropTargetAt_returnsNearestExpandedChipHit() {
    val chipBounds =
      mapOf(
        IdeaQuadrant.DoNow to Rect(left = 0f, top = 0f, right = 40f, bottom = 40f),
        IdeaQuadrant.Schedule to Rect(left = 42f, top = 0f, right = 82f, bottom = 40f),
      )

    assertEquals(
      IdeaQuadrant.Schedule,
      dropTargetAt(position = Offset(48f, 20f), chipBounds = chipBounds, slopPx = 8f),
    )
  }

  @Test
  fun dropTargetAt_returnsNullOutsideExpandedBounds() {
    val chipBounds = mapOf(IdeaQuadrant.DoNow to Rect(left = 0f, top = 0f, right = 40f, bottom = 40f))

    assertNull(dropTargetAt(position = Offset(60f, 20f), chipBounds = chipBounds, slopPx = 8f))
  }
}
