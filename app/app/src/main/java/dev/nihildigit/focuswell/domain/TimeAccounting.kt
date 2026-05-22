package dev.nihildigit.focuswell.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.max

object TimeAccounting {
  val focusWellZone: ZoneId = ZoneId.of("Asia/Shanghai")
  private val dayBoundary: Duration = Duration.ofHours(4)
  private val sleepProtectionStart: LocalTime = LocalTime.of(1, 0)
  private val sleepProtectionEnd: LocalTime = LocalTime.of(4, 0)

  fun dailyDate(instant: Instant, zone: ZoneId = focusWellZone): LocalDate {
    return instant.atZone(zone).toLocalDateTime().minus(dayBoundary).toLocalDate()
  }

  fun focusEarnedMinutes(
    activeDuration: Duration,
    type: SessionType,
    tagMultiplier: Double,
  ): Double {
    val minutes = activeDuration.toMillis() / 60_000.0
    return max(0.0, minutes * type.rate * tagMultiplier)
  }

  fun leisureCostMinutes(
    startedAt: Instant,
    endedAt: Instant,
    zone: ZoneId = focusWellZone,
  ): Double {
    if (!endedAt.isAfter(startedAt)) return 0.0

    var cursor = startedAt
    var total = 0.0
    while (cursor.isBefore(endedAt)) {
      val nextBoundary = nextSleepProtectionBoundary(cursor, zone)
      val segmentEnd = minOf(endedAt, nextBoundary)
      val rate = if (isSleepProtection(cursor, zone)) 2.0 else 1.0
      total += Duration.between(cursor, segmentEnd).toMillis() / 60_000.0 * rate
      cursor = segmentEnd
    }
    return total
  }

  private fun isSleepProtection(instant: Instant, zone: ZoneId): Boolean {
    val localTime = instant.atZone(zone).toLocalTime()
    return !localTime.isBefore(sleepProtectionStart) && localTime.isBefore(sleepProtectionEnd)
  }

  private fun nextSleepProtectionBoundary(instant: Instant, zone: ZoneId): Instant {
    val local = instant.atZone(zone)
    val date = local.toLocalDate()
    val time = local.toLocalTime()
    val boundaryLocal =
      when {
        time.isBefore(sleepProtectionStart) -> date.atTime(sleepProtectionStart)
        time.isBefore(sleepProtectionEnd) -> date.atTime(sleepProtectionEnd)
        else -> date.plusDays(1).atTime(sleepProtectionStart)
      }
    return boundaryLocal.atZone(zone).toInstant()
  }
}
