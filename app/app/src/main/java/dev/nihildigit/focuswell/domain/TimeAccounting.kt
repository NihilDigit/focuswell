package dev.nihildigit.focuswell.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max

object TimeAccounting {
  val focusWellZone: ZoneId
    get() = ZoneId.systemDefault()

  fun dailyDate(
    instant: Instant,
    zone: ZoneId = focusWellZone,
    rules: FocusWellRules = FocusWellRules(),
  ): LocalDate {
    val dayBoundary = Duration.ofHours(rules.normalized().safeDayBoundaryHour.toLong())
    return instant.atZone(zone).toLocalDateTime().minus(dayBoundary).toLocalDate()
  }

  fun focusEarnedMinutes(
    activeDuration: Duration,
    type: SessionType,
    tagMultiplier: Double,
    outcomeMultiplier: Double = 1.0,
  ): Double {
    val minutes = activeDuration.toMillis() / 60_000.0
    return focusEarnedMinutes(minutes, type.rate, tagMultiplier, outcomeMultiplier)
  }

  fun focusEarnedMinutes(
    activeDurationMinutes: Double,
    typeRate: Double,
    tagMultiplier: Double,
    outcomeMultiplier: Double = 1.0,
  ): Double {
    return max(0.0, activeDurationMinutes.coerceAtLeast(0.0) * typeRate * tagMultiplier * outcomeMultiplier.coerceAtLeast(0.0))
  }

  fun leisureCostMinutes(
    startedAt: Instant,
    endedAt: Instant,
    zone: ZoneId = focusWellZone,
    rules: FocusWellRules = FocusWellRules(),
  ): Double {
    if (!endedAt.isAfter(startedAt)) return 0.0
    val normalizedRules = rules.normalized()

    var cursor = startedAt
    var total = 0.0
    while (cursor.isBefore(endedAt)) {
      val nextBoundary = nextSleepProtectionBoundary(cursor, zone, normalizedRules)
      val segmentEnd = minOf(endedAt, nextBoundary)
      val rate = if (isSleepProtection(cursor, zone, normalizedRules)) normalizedRules.safeSleepProtectionMultiplier else 1.0
      total += Duration.between(cursor, segmentEnd).toMillis() / 60_000.0 * rate
      cursor = segmentEnd
    }
    return total
  }

  fun instantWhenLeisureCostReaches(
    startedAt: Instant,
    costMinutes: Double,
    zone: ZoneId = focusWellZone,
    rules: FocusWellRules = FocusWellRules(),
  ): Instant {
    if (costMinutes <= 0.0) return startedAt
    val normalizedRules = rules.normalized()

    var cursor = startedAt
    var remainingCost = costMinutes
    while (true) {
      val nextBoundary = nextSleepProtectionBoundary(cursor, zone, normalizedRules)
      val rate = if (isSleepProtection(cursor, zone, normalizedRules)) normalizedRules.safeSleepProtectionMultiplier else 1.0
      val segmentRealMinutes = Duration.between(cursor, nextBoundary).toMillis() / 60_000.0
      val segmentCost = segmentRealMinutes * rate
      if (remainingCost <= segmentCost) {
        val realMillis = (remainingCost / rate * 60_000).toLong().coerceAtLeast(0)
        return cursor.plusMillis(realMillis)
      }
      remainingCost -= segmentCost
      cursor = nextBoundary
    }
  }

  fun isSleepProtection(
    instant: Instant,
    zone: ZoneId = focusWellZone,
    rules: FocusWellRules = FocusWellRules(),
  ): Boolean {
    val normalizedRules = rules.normalized()
    val localTime = instant.atZone(zone).toLocalTime()
    return !localTime.isBefore(normalizedRules.sleepProtectionStartTime) &&
      localTime.isBefore(normalizedRules.sleepProtectionEndTime)
  }

  private fun nextSleepProtectionBoundary(
    instant: Instant,
    zone: ZoneId,
    rules: FocusWellRules,
  ): Instant {
    val local = instant.atZone(zone)
    val date = local.toLocalDate()
    val time = local.toLocalTime()
    val sleepProtectionStart = rules.sleepProtectionStartTime
    val sleepProtectionEnd = rules.sleepProtectionEndTime
    val boundaryLocal =
      when {
        time.isBefore(sleepProtectionStart) -> date.atTime(sleepProtectionStart)
        time.isBefore(sleepProtectionEnd) -> date.atTime(sleepProtectionEnd)
        else -> date.plusDays(1).atTime(sleepProtectionStart)
      }
    return boundaryLocal.atZone(zone).toInstant()
  }
}
