package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.LedgerEntry
import java.time.Instant

internal data class PhoneUsageLedgerCharge(
  val entry: LedgerEntry?,
  val exceededReserve: Boolean,
)

internal fun morningCheckInPhoneUsageCharge(
  dailyDate: String,
  checkInStartedAt: Instant,
  phoneCostMinutes: Double,
  availableMinutes: Double,
  reviewedSegmentCount: Int,
): PhoneUsageLedgerCharge =
  phoneUsageLedgerCharge(
    id = FocusWellIds.phoneCheckIn(dailyDate, checkInStartedAt),
    createdAt = checkInStartedAt,
    phoneCostMinutes = phoneCostMinutes,
    availableMinutes = availableMinutes,
    reviewedSegmentCount = reviewedSegmentCount,
    settledPrefix = "Detected",
  )

internal fun settlementPhoneUsageCharge(
  settlementStartedAt: Instant,
  phoneCostMinutes: Double,
  availableMinutes: Double,
  reviewedSegmentCount: Int,
): PhoneUsageLedgerCharge =
  phoneUsageLedgerCharge(
    id = FocusWellIds.phoneSettlement(settlementStartedAt),
    createdAt = settlementStartedAt,
    phoneCostMinutes = phoneCostMinutes,
    availableMinutes = availableMinutes,
    reviewedSegmentCount = reviewedSegmentCount,
    settledPrefix = "Settled",
  )

private fun phoneUsageLedgerCharge(
  id: String,
  createdAt: Instant,
  phoneCostMinutes: Double,
  availableMinutes: Double,
  reviewedSegmentCount: Int,
  settledPrefix: String,
): PhoneUsageLedgerCharge {
  val safePhoneCost = phoneCostMinutes.coerceAtLeast(0.0)
  val available = availableMinutes.coerceAtLeast(0.0)
  val deducted = minOf(safePhoneCost, available)
  val exceeded = safePhoneCost > available
  val entry =
    if (safePhoneCost > 0.0 || reviewedSegmentCount > 0) {
      LedgerEntry(
        id = id,
        title = if (exceeded) "Phone usage cleared reserve" else "Phone usage",
        deltaMinutes = -deducted,
        createdAt = createdAt,
        note =
          if (exceeded) {
            "Detected ${safePhoneCost.roundMinutes()}; cleared ${deducted.roundMinutes()}; leisure locked until a 2h focus restart."
          } else {
            "$settledPrefix ${safePhoneCost.roundMinutes()} after Fair Use across $reviewedSegmentCount segment${if (reviewedSegmentCount == 1) "" else "s"}."
          },
      )
    } else {
      null
    }
  return PhoneUsageLedgerCharge(entry = entry, exceededReserve = exceeded)
}
