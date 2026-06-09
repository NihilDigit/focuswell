package dev.nihildigit.focuswell.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodayLeisureTimerTest {
  @Test
  fun lowBalanceText_usesCoarseWarningThresholds() {
    assertEquals("1 min left", lowBalanceText(0.5))
    assertEquals("5 min left", lowBalanceText(4.5))
    assertEquals("10 min left", lowBalanceText(9.5))
    assertNull(lowBalanceText(10.5))
  }
}
