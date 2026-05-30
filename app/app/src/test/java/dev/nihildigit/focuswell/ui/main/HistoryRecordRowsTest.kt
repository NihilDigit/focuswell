package dev.nihildigit.focuswell.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryRecordRowsTest {
  @Test
  fun focusRecordEditBalance_usesTypedMinutesWhenValid() {
    val balance =
      focusRecordEditBalance(
        currentActiveMinutes = 40.0,
        currentEarnedMinutes = 20.0,
        typeRate = 0.5,
        tagMultiplier = 1.5,
        minutesText = "60",
      )

    assertEquals(60.0, balance.activeMinutes, 0.0001)
    assertEquals(45.0, balance.updatedEarnedMinutes, 0.0001)
    assertEquals(25.0, balance.deltaMinutes, 0.0001)
  }

  @Test
  fun focusRecordEditBalance_fallsBackToCurrentMinutesWhenInvalid() {
    val balance =
      focusRecordEditBalance(
        currentActiveMinutes = 40.0,
        currentEarnedMinutes = 20.0,
        typeRate = 0.5,
        tagMultiplier = 1.5,
        minutesText = "abc",
      )

    assertEquals(40.0, balance.activeMinutes, 0.0001)
    assertEquals(30.0, balance.updatedEarnedMinutes, 0.0001)
    assertEquals(10.0, balance.deltaMinutes, 0.0001)
  }
}
