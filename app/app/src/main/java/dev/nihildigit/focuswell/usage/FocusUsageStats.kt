package dev.nihildigit.focuswell.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import dev.nihildigit.focuswell.domain.LeisureRecord
import dev.nihildigit.focuswell.domain.PhoneUsageApp
import dev.nihildigit.focuswell.domain.PhoneUsageSegment
import dev.nihildigit.focuswell.domain.PhoneUsageSlice
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.TimeAccounting
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.floor

data class FocusAppUsage(
  val packageName: String,
  val appName: String,
  val icon: Drawable?,
  val durationMillis: Long,
)

internal data class UsageInterval(
  val packageName: String,
  val startedAtMillis: Long,
  val endedAtMillis: Long,
)

fun hasUsageAccess(context: Context): Boolean {
  val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
  val mode =
    appOps.checkOpNoThrow(
      AppOpsManager.OPSTR_GET_USAGE_STATS,
      Process.myUid(),
      context.packageName,
    )
  return mode == AppOpsManager.MODE_ALLOWED
}

fun phoneUsageSegments(
  context: Context,
  startedAt: Instant,
  endedAt: Instant,
  leisureRecords: List<LeisureRecord>,
  rules: FocusWellRules = FocusWellRules(),
  zone: ZoneId = TimeAccounting.focusWellZone,
): List<PhoneUsageSegment> {
  if (!hasUsageAccess(context) || !endedAt.isAfter(startedAt)) return emptyList()

  val packageManager = context.packageManager
  return clusterPhoneUsageIntervals(
    intervals =
      usageIntervals(
        context = context,
        startedAt = startedAt,
        endedAt = endedAt,
      ),
    startedAtMillis = startedAt.toEpochMilli(),
    endedAtMillis = endedAt.toEpochMilli(),
    excludedIntervals =
      leisureRecords
        .filter { it.deletedAt == null && it.endedAt.isAfter(startedAt) && it.startedAt.isBefore(endedAt) }
        .map { it.startedAt.toEpochMilli() to it.endedAt.toEpochMilli() },
    appName = { packageName -> packageManager.appLabel(packageName) ?: packageName.fallbackAppName() },
    rules = rules,
    zone = zone,
  )
}

fun focusAppUsage(
  context: Context,
  startedAt: Instant,
  endedAt: Instant,
): List<FocusAppUsage> {
  if (!hasUsageAccess(context) || !endedAt.isAfter(startedAt)) return emptyList()

  val totals = mutableMapOf<String, Long>()
  usageIntervals(context, startedAt, endedAt).forEach { interval ->
    val elapsed = (interval.endedAtMillis - interval.startedAtMillis).coerceAtLeast(0)
    totals[interval.packageName] = (totals[interval.packageName] ?: 0L) + elapsed
  }

  val packageManager = context.packageManager
  return totals
    .filterValues { it >= 10_000L }
    .map { (packageName, durationMillis) ->
      FocusAppUsage(
        packageName = packageName,
        appName = packageManager.appLabel(packageName) ?: packageName.fallbackAppName(),
        icon = packageManager.appIcon(packageName),
        durationMillis = durationMillis,
      )
    }
    .sortedByDescending { it.durationMillis }
}

internal fun usageIntervals(
  context: Context,
  startedAt: Instant,
  endedAt: Instant,
): List<UsageInterval> {
  if (!endedAt.isAfter(startedAt)) return emptyList()
  val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
  val packageManager = context.packageManager
  val startMillis = startedAt.toEpochMilli()
  val endMillis = endedAt.toEpochMilli()
  val events = usageStats.queryEvents(startMillis, endMillis)
  val foregroundIntervals = mutableListOf<UsageInterval>()
  val screenIntervals = mutableListOf<Pair<Long, Long>>()
  var foregroundPackage: String? = null
  var foregroundStartedAt = startMillis
  var screenInteractive: Boolean? = null
  var screenStartedAt = startMillis
  val event = UsageEvents.Event()

  while (events.hasNextEvent()) {
    events.getNextEvent(event)

    when (event.eventType) {
      UsageEvents.Event.ACTIVITY_RESUMED -> {
        val packageName = event.packageName ?: continue
        if (shouldExcludePhoneUsagePackage(context, packageManager, packageName)) continue
        foregroundPackage?.let { active ->
          foregroundIntervals += UsageInterval(active, foregroundStartedAt, event.timeStamp.coerceIn(startMillis, endMillis))
        }
        foregroundPackage = packageName
        foregroundStartedAt = event.timeStamp.coerceAtLeast(startMillis)
      }

      UsageEvents.Event.ACTIVITY_PAUSED -> {
        val packageName = event.packageName ?: continue
        if (shouldExcludePhoneUsagePackage(context, packageManager, packageName)) continue
        if (foregroundPackage == packageName) {
          foregroundIntervals += UsageInterval(packageName, foregroundStartedAt, event.timeStamp.coerceIn(startMillis, endMillis))
          foregroundPackage = null
          foregroundStartedAt = event.timeStamp
        }
      }

      UsageEvents.Event.SCREEN_INTERACTIVE -> {
        if (screenInteractive != true) {
          screenInteractive = true
          screenStartedAt = event.timeStamp.coerceAtLeast(startMillis)
        }
      }

      UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
        if (screenInteractive != false) {
          val screenStart = if (screenInteractive == null) startMillis else screenStartedAt
          val screenEnd = event.timeStamp.coerceIn(startMillis, endMillis)
          if (screenEnd > screenStart) screenIntervals += screenStart to screenEnd
          screenInteractive = false
          screenStartedAt = event.timeStamp
        }
      }
    }
  }

  foregroundPackage?.let { active ->
    foregroundIntervals += UsageInterval(active, foregroundStartedAt, endMillis)
  }
  if (screenInteractive == true) {
    screenIntervals += screenStartedAt.coerceIn(startMillis, endMillis) to endMillis
  }

  val effectiveScreenIntervals = screenIntervals.filter { (start, end) -> end > start }.ifEmpty { listOf(startMillis to endMillis) }
  return foregroundIntervals
    .flatMap { interval ->
      intersectWindows(
        interval.startedAtMillis.coerceIn(startMillis, endMillis),
        interval.endedAtMillis.coerceIn(startMillis, endMillis),
        effectiveScreenIntervals,
      ).map { (screenStart, screenEnd) ->
        interval.copy(startedAtMillis = screenStart, endedAtMillis = screenEnd)
      }
    }
}

private fun shouldExcludePhoneUsagePackage(
  context: Context,
  packageManager: PackageManager,
  packageName: String,
): Boolean {
  if (packageName == context.packageName || packageName.startsWith("dev.nihildigit.focuswell")) return true
  val appInfo =
    runCatching {
      packageManager.getApplicationInfo(packageName, 0)
    }.getOrNull()
  if (appInfo != null) {
    val systemFlags = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
    if (appInfo.flags and systemFlags != 0) return true
  }
  return false
}

internal fun clusterPhoneUsageIntervals(
  intervals: List<UsageInterval>,
  startedAtMillis: Long,
  endedAtMillis: Long,
  excludedIntervals: List<Pair<Long, Long>> = emptyList(),
  occupiedMillisPerMinute: Long = 50_000L,
  minimumOccupiedMinutes: Int = 5,
  mergeGapMinutes: Int = 1,
  appName: (String) -> String = { it.fallbackAppName() },
  rules: FocusWellRules = FocusWellRules(),
  zone: ZoneId = TimeAccounting.focusWellZone,
): List<PhoneUsageSegment> {
  if (endedAtMillis <= startedAtMillis) return emptyList()
  val minuteMillis = Duration.ofMinutes(1).toMillis()
  val firstMinute = floor(startedAtMillis / minuteMillis.toDouble()).toLong() * minuteMillis
  val lastMinute = ceil(endedAtMillis / minuteMillis.toDouble()).toLong() * minuteMillis
  val minutePackages = linkedMapOf<Long, MutableMap<String, Long>>()

  intervals.forEach { interval ->
    val clipped = subtractExcluded(
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
          slices.sumOf { slice ->
            TimeAccounting.leisureCostMinutes(slice.startedAt, slice.endedAt, zone = zone, rules = rules)
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

private fun List<PhoneUsageSlice>.mergeAdjacentSlices(): List<PhoneUsageSlice> {
  if (isEmpty()) return emptyList()
  val merged = mutableListOf<PhoneUsageSlice>()
  forEach { slice ->
    val previous = merged.lastOrNull()
    if (previous != null && previous.packageName == slice.packageName && !slice.startedAt.isAfter(previous.endedAt.plusMillis(1))) {
      merged[merged.lastIndex] =
        previous.copy(
          endedAt = maxOf(previous.endedAt, slice.endedAt),
          durationMillis = Duration.between(previous.startedAt, maxOf(previous.endedAt, slice.endedAt)).toMillis(),
        )
    } else {
      merged += slice
    }
  }
  return merged
}

private fun intersectWindows(
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

private fun PackageManager.appLabel(packageName: String): String? =
  runCatching {
    val info = getApplicationInfo(packageName, 0)
    getApplicationLabel(info).toString()
  }.getOrNull()

private fun PackageManager.appIcon(packageName: String): Drawable? =
  runCatching { getApplicationIcon(packageName) }.getOrNull()

internal fun String.fallbackAppName(): String {
  val compactName = substringAfterLast('.').replace('_', ' ').replace('-', ' ').trim()
  return compactName.ifBlank { this }
}
