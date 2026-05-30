package dev.nihildigit.focuswell.usage

import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.PhoneUsageApp
import dev.nihildigit.focuswell.domain.PhoneUsageSegment
import dev.nihildigit.focuswell.domain.PhoneUsageSlice
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.time.toKotlinInstant
import kotlinx.datetime.TimeZone
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.time.Duration.Companion.minutes

internal fun clusterPhoneUsageIntervals(
  intervals: List<UsageInterval>,
  startedAtMillis: Long,
  endedAtMillis: Long,
  excludedIntervals: List<Pair<Long, Long>> = emptyList(),
  occupiedMillisPerMinute: Long = 50_000L,
  minimumOccupiedMinutes: Int = 5,
  mergeGapMinutes: Int = 1,
  chargeFreePackages: Set<String> = emptySet(),
  appName: (String) -> String = { it.fallbackAppName() },
  rules: FocusWellRules = FocusWellRules(),
  zone: TimeZone = TimeAccounting.focusWellTimeZone,
): List<PhoneUsageSegment> {
  if (endedAtMillis <= startedAtMillis) return emptyList()
  val minuteMillis = 1.minutes.inWholeMilliseconds
  val firstMinute = floor(startedAtMillis / minuteMillis.toDouble()).toLong() * minuteMillis
  val lastMinute = ceil(endedAtMillis / minuteMillis.toDouble()).toLong() * minuteMillis
  val minutePackages = linkedMapOf<Long, MutableMap<String, Long>>()

  intervals.forEach { interval ->
    val clipped =
      subtractExcluded(
        interval.startedAtMillis.coerceAtLeast(startedAtMillis),
        interval.endedAtMillis.coerceAtMost(endedAtMillis),
        excludedIntervals,
      )
    clipped.forEach { (segmentStart, segmentEnd) ->
      if (segmentEnd <= segmentStart) return@forEach
      var minuteStart = floor(segmentStart / minuteMillis.toDouble()).toLong() * minuteMillis
      while (minuteStart < segmentEnd) {
        val minuteEnd = minuteStart + minuteMillis
        val overlap = (minOf(segmentEnd, minuteEnd) - maxOf(segmentStart, minuteStart)).coerceAtLeast(0L)
        if (overlap > 0L) {
          val packages = minutePackages.getOrPut(minuteStart) { linkedMapOf() }
          packages[interval.packageName] = (packages[interval.packageName] ?: 0L) + overlap
        }
        minuteStart += minuteMillis
      }
    }
  }

  val occupiedMinutes =
    generateSequence(firstMinute) { minute -> (minute + minuteMillis).takeIf { it < lastMinute } }
      .filter { minute -> (minutePackages[minute]?.values?.sum() ?: 0L) >= occupiedMillisPerMinute }
      .toList()
  if (occupiedMinutes.isEmpty()) return emptyList()

  val clusters = mutableListOf<List<Long>>()
  var current = mutableListOf(occupiedMinutes.first())
  occupiedMinutes.drop(1).forEach { minute ->
    val gapMinutes = ((minute - current.last()) / minuteMillis - 1).toInt()
    if (gapMinutes <= mergeGapMinutes) {
      current += minute
    } else {
      clusters += current
      current = mutableListOf(minute)
    }
  }
  clusters += current

  return clusters
    .filter { cluster -> cluster.size >= minimumOccupiedMinutes }
    .map { cluster ->
      val appTotals = linkedMapOf<String, Long>()
      cluster.forEach { minute ->
        minutePackages[minute]?.forEach { (packageName, millis) ->
          appTotals[packageName] = (appTotals[packageName] ?: 0L) + millis
        }
      }
      val start = cluster.first()
      val end = cluster.last() + minuteMillis
      val eligibleWindows = cluster.map { minute -> minute to minute + minuteMillis }
      val slices =
        intervals
          .flatMap { interval ->
            subtractExcluded(
              interval.startedAtMillis.coerceAtLeast(start),
              interval.endedAtMillis.coerceAtMost(end),
              excludedIntervals,
            ).flatMap { (sliceStart, sliceEnd) ->
              intersectWindows(sliceStart, sliceEnd, eligibleWindows).map { (eligibleStart, eligibleEnd) ->
                PhoneUsageSlice(
                  packageName = interval.packageName,
                  appName = appName(interval.packageName),
                  startedAt = Instant.ofEpochMilli(eligibleStart),
                  endedAt = Instant.ofEpochMilli(eligibleEnd),
                  durationMillis = eligibleEnd - eligibleStart,
                )
              }
            }
          }
          .sortedBy { it.startedAt }
          .mergeAdjacentSlices()
      PhoneUsageSegment(
        id = "phone-${start}-${end}",
        startedAt = Instant.ofEpochMilli(start),
        endedAt = Instant.ofEpochMilli(end),
        costMinutes =
          slices.filterNot { it.packageName in chargeFreePackages }.sumOf { slice ->
            TimeAccounting.leisureCostMinutes(
              startedAt = slice.startedAt.toKotlinInstant(),
              endedAt = slice.endedAt.toKotlinInstant(),
              zone = zone,
              rules = rules,
            )
          },
        topApps =
          appTotals.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { (packageName, durationMillis) ->
              PhoneUsageApp(
                packageName = packageName,
                appName = appName(packageName),
                durationMillis = durationMillis,
              )
            },
        slices = slices,
      )
    }
}

internal fun hasBillablePhoneUsageSegment(
  intervals: List<UsageInterval>,
  startedAtMillis: Long,
  endedAtMillis: Long,
  excludedIntervals: List<Pair<Long, Long>> = emptyList(),
  chargeFreePackages: Set<String> = emptySet(),
  rules: FocusWellRules = FocusWellRules(),
  zone: TimeZone = TimeAccounting.focusWellTimeZone,
): Boolean =
  clusterPhoneUsageIntervals(
    intervals = intervals,
    startedAtMillis = startedAtMillis,
    endedAtMillis = endedAtMillis,
    excludedIntervals = excludedIntervals,
    chargeFreePackages = chargeFreePackages,
    rules = rules,
    zone = zone,
  ).any { it.costMinutes > 0.0 }

private fun List<PhoneUsageSlice>.mergeAdjacentSlices(): List<PhoneUsageSlice> {
  if (isEmpty()) return emptyList()
  val merged = mutableListOf<PhoneUsageSlice>()
  forEach { slice ->
    val previous = merged.lastOrNull()
    if (previous != null && previous.packageName == slice.packageName && !slice.startedAt.isAfter(previous.endedAt.plusMillis(1))) {
      merged[merged.lastIndex] =
        previous.copy(
          endedAt = maxOf(previous.endedAt, slice.endedAt),
          durationMillis =
            (maxOf(previous.endedAt, slice.endedAt).toKotlinInstant() - previous.startedAt.toKotlinInstant())
              .inWholeMilliseconds,
        )
    } else {
      merged += slice
    }
  }
  return merged
}

internal fun intersectWindows(
  startMillis: Long,
  endMillis: Long,
  windows: List<Pair<Long, Long>>,
): List<Pair<Long, Long>> {
  if (endMillis <= startMillis) return emptyList()
  return windows.mapNotNull { (windowStart, windowEnd) ->
    val start = maxOf(startMillis, windowStart)
    val end = minOf(endMillis, windowEnd)
    if (end > start) start to end else null
  }
}

private fun subtractExcluded(
  startMillis: Long,
  endMillis: Long,
  excludedIntervals: List<Pair<Long, Long>>,
): List<Pair<Long, Long>> {
  if (endMillis <= startMillis) return emptyList()
  val sorted =
    excludedIntervals
      .mapNotNull { (start, end) ->
        val clippedStart = start.coerceAtLeast(startMillis)
        val clippedEnd = end.coerceAtMost(endMillis)
        if (clippedEnd > clippedStart) clippedStart to clippedEnd else null
      }
      .sortedBy { it.first }
  if (sorted.isEmpty()) return listOf(startMillis to endMillis)

  val remaining = mutableListOf<Pair<Long, Long>>()
  var cursor = startMillis
  sorted.forEach { (excludedStart, excludedEnd) ->
    if (excludedStart > cursor) remaining += cursor to excludedStart
    cursor = maxOf(cursor, excludedEnd)
  }
  if (cursor < endMillis) remaining += cursor to endMillis
  return remaining
}
