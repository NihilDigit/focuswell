package dev.nihildigit.focuswell.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInPhoneReviewCardTest {
  @Test
  fun resistedCardDragDelta_keepsRawDeltaBeforeThreshold() {
    assertEquals(24f, resistedCardDragDelta(currentOffset = 20f, delta = 24f, threshold = 100f), 0.0001f)
    assertEquals(-24f, resistedCardDragDelta(currentOffset = -20f, delta = -24f, threshold = 100f), 0.0001f)
  }

  @Test
  fun resistedCardDragDelta_appliesResistanceAfterThreshold() {
    val resisted = resistedCardDragDelta(currentOffset = 120f, delta = 40f, threshold = 100f)

    assertTrue(resisted in 6.4f..16.8f)
  }
}
