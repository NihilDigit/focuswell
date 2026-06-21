package dev.nihildigit.focuswell.data

import java.time.Instant
import java.time.LocalDate

internal object FocusWellIds {
  fun tag(createdAt: Instant): String = "tag-${createdAt.toEpochMilli()}"

  fun manualTracker(createdAt: Instant): String = "tracker-${createdAt.toEpochMilli()}"

  fun ruleTracker(createdAt: Instant): String = "rule-${createdAt.toEpochMilli()}"

  fun phoneCheckIn(dailyDate: String, startedAt: Instant): String =
    "phone-checkin-$dailyDate-${startedAt.toEpochMilli()}"

  fun phoneSettlement(startedAt: Instant): String =
    "phone-settlement-${startedAt.toEpochMilli()}"

  fun focus(instant: Instant): String = "focus-${instant.toEpochMilli()}"

  fun manualFocus(createdAt: Instant): String = "manual-focus-${createdAt.toEpochMilli()}"

  fun leisure(instant: Instant): String = "leisure-${instant.toEpochMilli()}"

  fun ledger(sourceId: String): String = "ledger-$sourceId"

  fun reserveRecovery(sourceId: String): String = "reserve-recovery-$sourceId"

  fun idea(createdAt: Instant): String = "idea-${createdAt.toEpochMilli()}"

  fun deleteAdjustment(sourceId: String, createdAt: Instant): String =
    "delete-$sourceId-${createdAt.toEpochMilli()}"

  fun editAdjustment(sourceId: String, createdAt: Instant): String =
    "edit-$sourceId-${createdAt.toEpochMilli()}"

  fun manualAdjustment(createdAt: Instant): String = "manual-adjustment-${createdAt.toEpochMilli()}"

  fun dailyGrant(date: LocalDate): String = "daily-grant-$date"

  fun dailyInterest(date: LocalDate): String = "daily-interest-$date"

  fun trackerReward(date: LocalDate, trackerId: String): String =
    "tracker-reward-$date-$trackerId"

  fun wakeBonus(date: LocalDate): String = "wake-bonus-$date"
}
