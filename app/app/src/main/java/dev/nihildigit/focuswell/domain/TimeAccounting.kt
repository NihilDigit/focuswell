package dev.nihildigit.focuswell.domain

import dev.nihildigit.focuswell.time.toJavaInstant
import dev.nihildigit.focuswell.time.toKotlinInstant
import dev.nihildigit.focuswell.time.toKotlinLocalTime
import dev.nihildigit.focuswell.time.toKotlinTimeZone
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate as KotlinLocalDate
import kotlinx.datetime.LocalDateTime as KotlinLocalDateTime
import kotlinx.datetime.LocalTime as KotlinLocalTime
import kotlinx.datetime.TimeZone as KotlinTimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant as KotlinInstant
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object TimeAccounting {
  val focusWellTimeZone: KotlinTimeZone
    get() = KotlinTimeZone.currentSystemDefault()

  val focusWellZone: ZoneId
    get() = ZoneId.of(focusWellTimeZone.id)

  fun dailyDate(
    instant: Instant,
    zone: ZoneId = focusWellZone,
    rules: FocusWellRules = FocusWellRules(),
  ): LocalDate {
    val normalizedRules = rules.normalized()
    val local = instant.toKotlinInstant().toLocalDateTime(zone.toKotlinTimeZone())
    val date =
      if (local.time.hour < normalizedRules.safeDayBoundaryHour) {
        local.date.minus(1, DateTimeUnit.DAY)
      } else {
        local.date
      }
    return date.toJavaLocalDate()
  }

  fun focusEarnedMinutes(
    activeDuration: Duration,
    type: SessionType,
    tagMultiplier: Double,
    outcomeMultiplier: Double = 1.0,
  ): Double {
    val minutes = activeDuration.inWholeMilliseconds / 60_000.0
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

  fun businessDayBoundaryInstant(
    dailyDate: String,
    dayOffset: Int = 0,
    zone: ZoneId = focusWellZone,
    rules: FocusWellRules = FocusWellRules(),
  ): Instant {
    val normalizedRules = rules.normalized()
    val date = KotlinLocalDate.parse(dailyDate).offsetByDays(dayOffset)
    return date.atTime(normalizedRules.dayBoundaryTime)
      .toInstant(zone.toKotlinTimeZone())
      .toJavaInstant()
  }

  fun leisureCostMinutes(
    startedAt: Instant,
    endedAt: Instant,
    zone: ZoneId = focusWellZone,
    rules: FocusWellRules = FocusWellRules(),
  ): Double =
    leisureCostMinutes(
      startedAt = startedAt.toKotlinInstant(),
      endedAt = endedAt.toKotlinInstant(),
      zone = zone.toKotlinTimeZone(),
      rules = rules,
    )

  fun leisureCostMinutes(
    startedAt: KotlinInstant,
    endedAt: KotlinInstant,
    zone: KotlinTimeZone = focusWellTimeZone,
    rules: FocusWellRules = FocusWellRules(),
  ): Double {
    if (endedAt <= startedAt) return 0.0
    val normalizedRules = rules.normalized()

    var cursor = startedAt
    var total = 0.0
    while (cursor < endedAt) {
      val nextBoundary = nextSleepProtectionBoundary(cursor, zone, normalizedRules)
      val segmentEnd = minOf(endedAt, nextBoundary)
      val rate = if (isSleepProtection(cursor, zone, normalizedRules)) normalizedRules.safeSleepProtectionMultiplier else 1.0
      total += minutesBetween(cursor, segmentEnd) * rate
      cursor = segmentEnd
    }
    return total
  }

  fun instantWhenLeisureCostReaches(
    startedAt: Instant,
    costMinutes: Double,
    zone: ZoneId = focusWellZone,
    rules: FocusWellRules = FocusWellRules(),
  ): Instant =
    instantWhenLeisureCostReaches(
      startedAt = startedAt.toKotlinInstant(),
      costMinutes = costMinutes,
      zone = zone.toKotlinTimeZone(),
      rules = rules,
    ).toJavaInstant()

  fun instantWhenLeisureCostReaches(
    startedAt: KotlinInstant,
    costMinutes: Double,
    zone: KotlinTimeZone = focusWellTimeZone,
    rules: FocusWellRules = FocusWellRules(),
  ): KotlinInstant {
    if (costMinutes <= 0.0) return startedAt
    val normalizedRules = rules.normalized()

    var cursor = startedAt
    var remainingCost = costMinutes
    while (true) {
      val nextBoundary = nextSleepProtectionBoundary(cursor, zone, normalizedRules)
      val rate = if (isSleepProtection(cursor, zone, normalizedRules)) normalizedRules.safeSleepProtectionMultiplier else 1.0
      val segmentRealMinutes = minutesBetween(cursor, nextBoundary)
      val segmentCost = segmentRealMinutes * rate
      if (remainingCost <= segmentCost) {
        val realMillis = (remainingCost / rate * 60_000).toLong().coerceAtLeast(0)
        return cursor + realMillis.milliseconds
      }
      remainingCost -= segmentCost
      cursor = nextBoundary
    }
  }

  fun isSleepProtection(
    instant: Instant,
    zone: ZoneId = focusWellZone,
    rules: FocusWellRules = FocusWellRules(),
  ): Boolean =
    isSleepProtection(
      instant = instant.toKotlinInstant(),
      zone = zone.toKotlinTimeZone(),
      rules = rules,
    )

  fun isSleepProtection(
    instant: KotlinInstant,
    zone: KotlinTimeZone = focusWellTimeZone,
    rules: FocusWellRules = FocusWellRules(),
  ): Boolean {
    val normalizedRules = rules.normalized()
    val localTime = instant.toLocalDateTime(zone).time.totalMinutes
    val sleepProtectionStart = normalizedRules.sleepProtectionStartTime
    val sleepProtectionEnd = normalizedRules.sleepProtectionEndTime
    val startMinutes = sleepProtectionStart.totalMinutes
    val endMinutes = sleepProtectionEnd.totalMinutes
    return when {
      sleepProtectionStart == sleepProtectionEnd -> false
      startMinutes < endMinutes ->
        localTime >= startMinutes && localTime < endMinutes
      else ->
        localTime >= startMinutes || localTime < endMinutes
    }
  }

  fun isWakeBonusEligible(
    instant: Instant,
    zone: ZoneId = focusWellZone,
    rules: FocusWellRules = FocusWellRules(),
  ): Boolean =
    isWakeBonusEligible(
      instant = instant.toKotlinInstant(),
      zone = zone.toKotlinTimeZone(),
      rules = rules,
    )

  fun isWakeBonusEligible(
    instant: KotlinInstant,
    zone: KotlinTimeZone = focusWellTimeZone,
    rules: FocusWellRules = FocusWellRules(),
  ): Boolean {
    val normalizedRules = rules.normalized()
    val localMinutes = instant.toLocalDateTime(zone).time.totalMinutes
    val targetMinutes = normalizedRules.wakeTargetTime.totalMinutes
    return localMinutes - targetMinutes in -60..30
  }

  private fun nextSleepProtectionBoundary(
    instant: KotlinInstant,
    zone: KotlinTimeZone,
    rules: FocusWellRules,
  ): KotlinInstant {
    val local = instant.toLocalDateTime(zone)
    val date = local.date
    val time = local.time.totalMinutes
    val sleepProtectionStart = rules.sleepProtectionStartTime
    val sleepProtectionEnd = rules.sleepProtectionEndTime
    val startMinutes = sleepProtectionStart.totalMinutes
    val endMinutes = sleepProtectionEnd.totalMinutes
    val boundaryLocal =
      if (startMinutes < endMinutes) {
        when {
          time < startMinutes -> date.atTime(sleepProtectionStart)
          time < endMinutes -> date.atTime(sleepProtectionEnd)
          else -> date.plus(1, DateTimeUnit.DAY).atTime(sleepProtectionStart)
        }
      } else {
        when {
          time < endMinutes -> date.atTime(sleepProtectionEnd)
          time < startMinutes -> date.atTime(sleepProtectionStart)
          else -> date.plus(1, DateTimeUnit.DAY).atTime(sleepProtectionEnd)
        }
      }
    return boundaryLocal.toInstant(zone)
  }

  private fun kotlinx.datetime.LocalDate.atTime(time: LocalTime): KotlinLocalDateTime =
    KotlinLocalDateTime(
      date = this,
      time = time.toKotlinLocalTime(),
    )

  private fun kotlinx.datetime.LocalDate.offsetByDays(days: Int): kotlinx.datetime.LocalDate =
    when {
      days > 0 -> plus(days, DateTimeUnit.DAY)
      days < 0 -> minus(-days, DateTimeUnit.DAY)
      else -> this
    }

  private val LocalTime.totalMinutes: Int
    get() = hour * 60 + minute

  private val KotlinLocalTime.totalMinutes: Int
    get() = hour * 60 + minute

  private fun minutesBetween(start: KotlinInstant, end: KotlinInstant): Double =
    (end - start).inWholeMilliseconds / 60_000.0
}
