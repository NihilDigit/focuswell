package dev.nihildigit.focuswell.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

class ReserveHeaderTest {
  @Test
  fun reserveInterestPreviewText_showsEstimatedTomorrowInterest() {
    assertEquals("+14m @5% interest", reserveInterestPreviewText(reserveMinutes = 270.0, reserveLocked = false))
  }

  @Test
  fun reserveInterestPreviewText_showsBlendedRateAcrossInterestTiers() {
    assertEquals("+47m @6.5% interest", reserveInterestPreviewText(reserveMinutes = 720.0, reserveLocked = false))
  }

  @Test
  fun reserveInterestPreviewText_keepsLockedStateExplicit() {
    assertEquals("Locked: save only", reserveInterestPreviewText(reserveMinutes = 270.0, reserveLocked = true))
  }

  @Test
  fun reserveInterestPreviewText_usesShortEmptyReserveCopy() {
    assertEquals("No reserve yet", reserveInterestPreviewText(reserveMinutes = 0.0, reserveLocked = false))
  }
}
