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

  @Test
  fun leisureRemainingContextText_explainsFutureSleepProtectionCost() {
    assertEquals(
      "419m reserve · 2.0x after 23:00",
      leisureRemainingContextText(
        reserveMinutes = 418.867,
        sleepProtection = false,
        sleepProtectionMultiplier = 2.0,
        sleepProtectionStartHour = 23,
      ),
    )
  }

  @Test
  fun leisureRemainingContextText_explainsCurrentSleepProtectionCost() {
    assertEquals(
      "419m reserve · 2.0x cost now",
      leisureRemainingContextText(
        reserveMinutes = 418.867,
        sleepProtection = true,
        sleepProtectionMultiplier = 2.0,
        sleepProtectionStartHour = 23,
      ),
    )
  }

  @Test
  fun leisureRemainingContextText_keepsLowBalanceWarningFirst() {
    assertEquals(
      "5 min left",
      leisureRemainingContextText(
        reserveMinutes = 4.5,
        sleepProtection = true,
        sleepProtectionMultiplier = 2.0,
        sleepProtectionStartHour = 23,
      ),
    )
  }
}
