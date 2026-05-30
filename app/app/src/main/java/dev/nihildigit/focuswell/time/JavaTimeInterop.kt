package dev.nihildigit.focuswell.time

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.datetime.LocalTime as KotlinLocalTime
import kotlinx.datetime.TimeZone as KotlinTimeZone
import kotlin.time.Instant as KotlinInstant
import kotlin.time.Instant.Companion.fromEpochMilliseconds

internal fun Instant.toKotlinInstant(): KotlinInstant =
  fromEpochMilliseconds(toEpochMilli())

internal fun KotlinInstant.toJavaInstant(): Instant =
  Instant.ofEpochMilli(toEpochMilliseconds())

internal fun LocalTime.toKotlinLocalTime(): KotlinLocalTime =
  KotlinLocalTime(
    hour = hour,
    minute = minute,
    second = second,
    nanosecond = nano,
  )

internal fun ZoneId.toKotlinTimeZone(): KotlinTimeZone =
  if (id == "Z") KotlinTimeZone.UTC else KotlinTimeZone.of(id)
