package dev.nihildigit.focuswell.reminders

import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.time.toJavaInstant
import dev.nihildigit.focuswell.time.toKotlinInstant
import dev.nihildigit.focuswell.time.toKotlinLocalTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.time.Instant
import java.time.LocalTime
import kotlin.time.Duration.Companion.hours

internal fun nextLateNightInstant(
  now: Instant,
  rules: FocusWellRules,
  zone: TimeZone = TimeAccounting.focusWellTimeZone,
): Instant? {
  val normalizedRules = rules.normalized()
  val localNow = now.toKotlinInstant().toLocalDateTime(zone)
  val todayStart = localNow.date.atTime(normalizedRules.sleepProtectionStartTime)
  val nextStart =
    if (localNow < todayStart) {
      todayStart
    } else {
      localNow.date.plus(1, DateTimeUnit.DAY).atTime(normalizedRules.sleepProtectionStartTime)
    }
  val nextInstant = nextStart.toInstant(zone).toJavaInstant()
  return nextInstant.takeIf { (it.toKotlinInstant() - now.toKotlinInstant()) <= 16.hours }
}

private fun LocalDate.atTime(time: LocalTime): LocalDateTime =
  LocalDateTime(
    date = this,
    time = time.toKotlinLocalTime(),
  )
