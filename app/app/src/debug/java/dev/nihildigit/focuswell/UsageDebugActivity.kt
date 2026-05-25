package dev.nihildigit.focuswell

import android.app.Activity
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.os.Bundle
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import dev.nihildigit.focuswell.data.FocusWellRepository
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.usage.clusterPhoneUsageIntervals
import dev.nihildigit.focuswell.usage.hasUsageAccess
import dev.nihildigit.focuswell.usage.usageIntervals
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor

class UsageDebugActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val textView =
      TextView(this).apply {
        setTextIsSelectable(true)
        textSize = 12f
        typeface = android.graphics.Typeface.MONOSPACE
        setPadding(24, 24, 24, 24)
        text = "Loading usage diagnostics..."
      }
    setContentView(ScrollView(this).apply { addView(textView) })

    Thread {
      val report =
        runCatching { buildUsageReport() }
          .getOrElse { error -> "Usage diagnostics failed:\n${Log.getStackTraceString(error)}" }
      Log.i("FocusWellUsageDebug", report.chunkedLogSafe())
      runOnUiThread { textView.text = report }
    }.start()
  }

  private fun buildUsageReport(): String {
    val repository = FocusWellRepository(this)
    val state = repository.state.value
    val rules = state.rules.normalized()
    val zone = TimeAccounting.focusWellZone
    val today = TimeAccounting.dailyDate(Instant.now(), rules = rules)
    val date = intent.getStringExtra("dailyDate")?.let(LocalDate::parse) ?: today
    val start = date.minusDays(1).atTime(rules.dayBoundaryTime).atZone(zone).toInstant()
    val end = date.atTime(rules.dayBoundaryTime).atZone(zone).toInstant()
    val excluded =
      state.leisureRecords
        .filter { it.deletedAt == null && it.endedAt.isAfter(start) && it.startedAt.isBefore(end) }
        .map { it.startedAt.toEpochMilli() to it.endedAt.toEpochMilli() }
    val packageManager = packageManager
    val foreground = rawForegroundIntervals(start, end)
    val screen = rawScreenIntervals(start, end)
    val effective = usageIntervals(this, start, end)
    val segments =
      clusterPhoneUsageIntervals(
        intervals = effective,
        startedAtMillis = start.toEpochMilli(),
        endedAtMillis = end.toEpochMilli(),
        excludedIntervals = excluded,
        appName = { packageName -> runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString() }.getOrDefault(packageName) },
        rules = rules,
        zone = zone,
      )

    return buildString {
      appendLine("FocusWell Usage Debug")
      appendLine("usageAccess=${hasUsageAccess(this@UsageDebugActivity)}")
      appendLine("dailyDate=$date")
      appendLine("window=${start.short(zone)} -> ${end.short(zone)}")
      appendLine("rules dayBoundary=${rules.dayBoundaryTime} sleep=${rules.sleepProtectionStartTime}-${rules.sleepProtectionEndTime} x${rules.safeSleepProtectionMultiplier}")
      appendLine("excludedLeisure=${excluded.size}")
      appendLine()
      appendLine("Raw foreground intervals (${foreground.size})")
      foreground.take(80).forEachIndexed { index, interval ->
        appendLine("${index + 1}. ${interval.packageName.shortPackage()} ${interval.startedAtMillis.short(zone)} -> ${interval.endedAtMillis.short(zone)} ${interval.durationText()}")
      }
      appendLine()
      appendLine("Raw screen intervals (${screen.size})")
      screen.take(80).forEachIndexed { index, interval ->
        appendLine("${index + 1}. ${interval.first.short(zone)} -> ${interval.second.short(zone)} ${Duration.ofMillis(interval.second - interval.first).toMinutes()}m")
      }
      appendLine()
      appendLine("Foreground ∩ screen intervals (${effective.size})")
      effective.take(120).forEachIndexed { index, interval ->
        appendLine("${index + 1}. ${interval.packageName.shortPackage()} ${interval.startedAtMillis.short(zone)} -> ${interval.endedAtMillis.short(zone)} ${interval.durationText()}")
      }
      appendLine()
      appendLine(occupiedMinuteReport(effective, start, end))
      appendLine()
      appendLine("Segments (${segments.size})")
      segments.forEachIndexed { index, segment ->
        val screenMinutes = segment.slices.sumOf { it.durationMillis } / 60_000.0
        val spanMinutes = Duration.between(segment.startedAt, segment.endedAt).toMillis() / 60_000.0
        appendLine("${index + 1}. ${segment.startedAt.short(zone)} -> ${segment.endedAt.short(zone)} span=${spanMinutes.fmt()}m screen=${screenMinutes.fmt()}m cost=${segment.costMinutes.fmt()}m slices=${segment.slices.size}")
        segment.slices.forEach { slice ->
          appendLine("   - ${slice.appName} ${slice.startedAt.short(zone)} -> ${slice.endedAt.short(zone)} ${(slice.durationMillis / 60_000.0).fmt()}m")
        }
      }
    }
  }

  private fun rawForegroundIntervals(startedAt: Instant, endedAt: Instant): List<dev.nihildigit.focuswell.usage.UsageInterval> {
    val startMillis = startedAt.toEpochMilli()
    val endMillis = endedAt.toEpochMilli()
    val usageStats = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
    val events = usageStats.queryEvents(startMillis, endMillis)
    val intervals = mutableListOf<dev.nihildigit.focuswell.usage.UsageInterval>()
    var foregroundPackage: String? = null
    var foregroundStartedAt = startMillis
    val event = UsageEvents.Event()
    while (events.hasNextEvent()) {
      events.getNextEvent(event)
      when (event.eventType) {
        UsageEvents.Event.ACTIVITY_RESUMED -> {
          val packageName = event.packageName ?: continue
          if (packageName == this@UsageDebugActivity.packageName) continue
          foregroundPackage?.let { active ->
            intervals += dev.nihildigit.focuswell.usage.UsageInterval(active, foregroundStartedAt, event.timeStamp.coerceIn(startMillis, endMillis))
          }
          foregroundPackage = packageName
          foregroundStartedAt = event.timeStamp.coerceAtLeast(startMillis)
        }

        UsageEvents.Event.ACTIVITY_PAUSED -> {
          val packageName = event.packageName ?: continue
          if (foregroundPackage == packageName) {
            intervals += dev.nihildigit.focuswell.usage.UsageInterval(packageName, foregroundStartedAt, event.timeStamp.coerceIn(startMillis, endMillis))
            foregroundPackage = null
            foregroundStartedAt = event.timeStamp
          }
        }
      }
    }
    foregroundPackage?.let { active -> intervals += dev.nihildigit.focuswell.usage.UsageInterval(active, foregroundStartedAt, endMillis) }
    return intervals.filter { it.endedAtMillis > it.startedAtMillis }
  }

  private fun rawScreenIntervals(startedAt: Instant, endedAt: Instant): List<Pair<Long, Long>> {
    val startMillis = startedAt.toEpochMilli()
    val endMillis = endedAt.toEpochMilli()
    val usageStats = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
    val events = usageStats.queryEvents(startMillis, endMillis)
    val intervals = mutableListOf<Pair<Long, Long>>()
    var screenInteractive: Boolean? = null
    var screenStartedAt = startMillis
    val event = UsageEvents.Event()
    while (events.hasNextEvent()) {
      events.getNextEvent(event)
      when (event.eventType) {
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
            if (screenEnd > screenStart) intervals += screenStart to screenEnd
            screenInteractive = false
            screenStartedAt = event.timeStamp
          }
        }
      }
    }
    if (screenInteractive == true) intervals += screenStartedAt.coerceIn(startMillis, endMillis) to endMillis
    return intervals.filter { it.second > it.first }
  }

  private fun occupiedMinuteReport(
    intervals: List<dev.nihildigit.focuswell.usage.UsageInterval>,
    startedAt: Instant,
    endedAt: Instant,
  ): String {
    val minuteMillis = Duration.ofMinutes(1).toMillis()
    val firstMinute = floor(startedAt.toEpochMilli() / minuteMillis.toDouble()).toLong() * minuteMillis
    val lastMinute = ceil(endedAt.toEpochMilli() / minuteMillis.toDouble()).toLong() * minuteMillis
    val minuteTotals = linkedMapOf<Long, Long>()
    intervals.forEach { interval ->
      var minuteStart = floor(interval.startedAtMillis / minuteMillis.toDouble()).toLong() * minuteMillis
      while (minuteStart < interval.endedAtMillis) {
        val minuteEnd = minuteStart + minuteMillis
        val overlap = (minOf(interval.endedAtMillis, minuteEnd) - maxOf(interval.startedAtMillis, minuteStart)).coerceAtLeast(0)
        if (overlap > 0) minuteTotals[minuteStart] = (minuteTotals[minuteStart] ?: 0L) + overlap
        minuteStart += minuteMillis
      }
    }
    val occupied = generateSequence(firstMinute) { minute -> (minute + minuteMillis).takeIf { it < lastMinute } }
      .filter { (minuteTotals[it] ?: 0L) >= 50_000L }
      .toList()
    return buildString {
      appendLine("Occupied minutes ${occupied.size}")
      occupied.take(120).forEach { minute ->
        appendLine("- ${minute.short(TimeAccounting.focusWellZone)} ${(minuteTotals[minute] ?: 0L) / 1000}s")
      }
    }
  }
}

private fun Long.short(zone: ZoneId): String = Instant.ofEpochMilli(this).short(zone)

private fun Instant.short(zone: ZoneId): String = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").format(atZone(zone))

private fun dev.nihildigit.focuswell.usage.UsageInterval.durationText(): String =
  "${((endedAtMillis - startedAtMillis).coerceAtLeast(0) / 60_000.0).fmt()}m"

private fun Double.fmt(): String = String.format("%.2f", this)

private fun String.shortPackage(): String = substringAfterLast('.')

private fun String.chunkedLogSafe(): String {
  chunked(3500).forEachIndexed { index, chunk -> Log.i("FocusWellUsageDebug", "chunk=$index\n$chunk") }
  return "Usage diagnostics written in chunks. First 3500 chars:\n" + take(3500)
}
