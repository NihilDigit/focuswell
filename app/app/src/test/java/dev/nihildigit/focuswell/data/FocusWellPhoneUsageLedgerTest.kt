package dev.nihildigit.focuswell.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusWellPhoneUsageLedgerTest {
  @Test
  fun morningCheckInPhoneUsageCharge_writesDetectedFairUseNote() {
    val charge =
      morningCheckInPhoneUsageCharge(
        dailyDate = "2026-05-20",
        checkInStartedAt = Instant.parse("2026-05-20T05:00:00Z"),
        phoneCostMinutes = 12.4,
        availableMinutes = 30.0,
        reviewedSegmentCount = 2,
      )

    val entry = checkNotNull(charge.entry)
    assertFalse(charge.exceededReserve)
    assertEquals("phone-checkin-2026-05-20-1779253200000", entry.id)
    assertEquals("Phone usage", entry.title)
    assertEquals(-12.4, entry.deltaMinutes, 0.0001)
    assertEquals("Detected 12m after Fair Use across 2 segments.", entry.note)
  }

  @Test
  fun settlementPhoneUsageCharge_writesSettledFairUseNote() {
    val charge =
      settlementPhoneUsageCharge(
        settlementStartedAt = Instant.parse("2026-05-20T05:00:00Z"),
        phoneCostMinutes = 12.4,
        availableMinutes = 30.0,
        reviewedSegmentCount = 1,
      )

    val entry = checkNotNull(charge.entry)
    assertFalse(charge.exceededReserve)
    assertEquals("phone-settlement-1779253200000", entry.id)
    assertEquals("Settled 12m after Fair Use across 1 segment.", entry.note)
  }

  @Test
  fun phoneUsageCharge_capsDeductionAndMarksReserveExceeded() {
    val charge =
      settlementPhoneUsageCharge(
        settlementStartedAt = Instant.parse("2026-05-20T05:00:00Z"),
        phoneCostMinutes = 42.0,
        availableMinutes = 10.0,
        reviewedSegmentCount = 3,
      )

    val entry = checkNotNull(charge.entry)
    assertTrue(charge.exceededReserve)
    assertEquals("Phone usage cleared reserve", entry.title)
    assertEquals(-10.0, entry.deltaMinutes, 0.0001)
    assertEquals("Detected 42m; cleared 10m; daily grant paused for 3 days.", entry.note)
  }

  @Test
  fun phoneUsageCharge_skipsLedgerEntryWhenNothingWasReviewedOrCharged() {
    val charge =
      settlementPhoneUsageCharge(
        settlementStartedAt = Instant.parse("2026-05-20T05:00:00Z"),
        phoneCostMinutes = 0.0,
        availableMinutes = 10.0,
        reviewedSegmentCount = 0,
      )

    assertNull(charge.entry)
    assertFalse(charge.exceededReserve)
  }
}
