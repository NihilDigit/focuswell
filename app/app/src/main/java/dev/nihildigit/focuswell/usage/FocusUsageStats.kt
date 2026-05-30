package dev.nihildigit.focuswell.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.LeisureRecord
import dev.nihildigit.focuswell.domain.PhoneUsageApp
import dev.nihildigit.focuswell.domain.PhoneUsageSegment
import dev.nihildigit.focuswell.domain.PhoneUsageSlice
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.TimeAccounting
import kotlinx.datetime.TimeZone
import java.time.Instant

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
  focusRecords: List<FocusRecord>,
  leisureRecords: List<LeisureRecord>,
  rules: FocusWellRules = FocusWellRules(),
  zone: TimeZone = TimeAccounting.focusWellTimeZone,
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
    excludedIntervals = excludedSessionIntervals(startedAt, endedAt, focusRecords, leisureRecords),
    appName = { packageName -> packageManager.appLabel(packageName) ?: packageName.fallbackAppName() },
    rules = rules,
    zone = zone,
  )
}

internal fun excludedSessionIntervals(
  startedAt: Instant,
  endedAt: Instant,
  focusRecords: List<FocusRecord>,
  leisureRecords: List<LeisureRecord>,
): List<Pair<Long, Long>> {
  val focusIntervals =
    focusRecords
      .filter { it.deletedAt == null && it.endedAt.isAfter(startedAt) && it.startedAt.isBefore(endedAt) }
      .map { it.startedAt.toEpochMilli() to it.endedAt.toEpochMilli() }
  val leisureIntervals =
    leisureRecords
      .filter { it.deletedAt == null && it.endedAt.isAfter(startedAt) && it.startedAt.isBefore(endedAt) }
      .map { it.startedAt.toEpochMilli() to it.endedAt.toEpochMilli() }
  return focusIntervals + leisureIntervals
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
