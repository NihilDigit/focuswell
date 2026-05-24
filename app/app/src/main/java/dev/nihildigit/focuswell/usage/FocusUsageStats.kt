package dev.nihildigit.focuswell.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import java.time.Instant

data class FocusAppUsage(
  val packageName: String,
  val appName: String,
  val icon: Drawable?,
  val durationMillis: Long,
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

fun focusAppUsage(
  context: Context,
  startedAt: Instant,
  endedAt: Instant,
): List<FocusAppUsage> {
  if (!hasUsageAccess(context) || !endedAt.isAfter(startedAt)) return emptyList()

  val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
  val startMillis = startedAt.toEpochMilli()
  val endMillis = endedAt.toEpochMilli()
  val events = usageStats.queryEvents(startMillis, endMillis)
  val totals = mutableMapOf<String, Long>()
  var foregroundPackage: String? = null
  var foregroundStartedAt = startMillis
  val event = UsageEvents.Event()

  while (events.hasNextEvent()) {
    events.getNextEvent(event)
    val packageName = event.packageName ?: continue
    if (packageName == context.packageName) continue

    when (event.eventType) {
      UsageEvents.Event.ACTIVITY_RESUMED -> {
        foregroundPackage?.let { active ->
          val elapsed = (event.timeStamp - foregroundStartedAt).coerceAtLeast(0)
          totals[active] = (totals[active] ?: 0L) + elapsed
        }
        foregroundPackage = packageName
        foregroundStartedAt = event.timeStamp.coerceAtLeast(startMillis)
      }

      UsageEvents.Event.ACTIVITY_PAUSED -> {
        if (foregroundPackage == packageName) {
          val elapsed = (event.timeStamp - foregroundStartedAt).coerceAtLeast(0)
          totals[packageName] = (totals[packageName] ?: 0L) + elapsed
          foregroundPackage = null
          foregroundStartedAt = event.timeStamp
        }
      }
    }
  }

  foregroundPackage?.let { active ->
    totals[active] = (totals[active] ?: 0L) + (endMillis - foregroundStartedAt).coerceAtLeast(0)
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
