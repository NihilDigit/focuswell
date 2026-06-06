package dev.nihildigit.focuswell.data

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusWellIdsTest {
  @Test
  fun sessionAndLedgerIds_keepPersistedPrefixes() {
    val instant = Instant.parse("2026-05-20T05:00:00Z")

    assertEquals("focus-1779253200000", FocusWellIds.focus(instant))
    assertEquals("leisure-1779253200000", FocusWellIds.leisure(instant))
    assertEquals("ledger-focus-1779253200000", FocusWellIds.ledger(FocusWellIds.focus(instant)))
  }

  @Test
  fun dayScopedLedgerIds_keepStableDateFormat() {
    val date = LocalDate.parse("2026-05-20")

    assertEquals("daily-grant-2026-05-20", FocusWellIds.dailyGrant(date))
    assertEquals("daily-interest-2026-05-20", FocusWellIds.dailyInterest(date))
    assertEquals("tracker-reward-2026-05-20-tracker-1", FocusWellIds.trackerReward(date, "tracker-1"))
    assertEquals("wake-bonus-2026-05-20", FocusWellIds.wakeBonus(date))
  }

  @Test
  fun generatedConfigIds_useInjectedClockInstant() {
    val instant = Instant.parse("2026-05-20T05:00:00Z")

    assertEquals("tag-1779253200000", FocusWellIds.tag(instant))
    assertEquals("tracker-1779253200000", FocusWellIds.manualTracker(instant))
    assertEquals("rule-1779253200000", FocusWellIds.ruleTracker(instant))
  }
}
