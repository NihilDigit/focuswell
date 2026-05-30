package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.materialkolor.blend.Blend
import com.materialkolor.hct.Hct
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import dev.nihildigit.focuswell.domain.PhoneUsageSegment
import dev.nihildigit.focuswell.domain.PhoneUsageSlice
import dev.nihildigit.focuswell.time.toKotlinInstant
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

private data class TimelineAppGroup(
  val packageName: String,
  val appName: String,
  val slices: List<PhoneUsageSlice>,
  val durationMillis: Long,
  val isOthers: Boolean = false,
)

@Composable
internal fun PhoneUsageTimeline(segment: PhoneUsageSegment) {
  val slices =
    segment.slices.ifEmpty {
      segment.topApps.map { app ->
        PhoneUsageSlice(
          packageName = app.packageName,
          appName = app.appName,
          startedAt = segment.startedAt,
          endedAt = segment.endedAt,
          durationMillis = app.durationMillis,
        )
      }
    }
  val appGroups = remember(slices) { timelineAppGroups(slices) }
  val visiblePackages = remember(appGroups) { appGroups.filterNot { it.isOthers }.mapTo(mutableSetOf()) { it.packageName } }
  val packageColors = appGroups.filterNot { it.isOthers }.associate { it.packageName to appTimelineColor(it.packageName) }
  val othersColor = MaterialTheme.colorScheme.outlineVariant
  val visualSlices = remember(slices) { mergeNearbyTimelineSlices(slices) }
  val startMillis = segment.startedAt.toEpochMilli()
  val spanMillis = (segment.endedAt.toEpochMilli() - startMillis).coerceAtLeast(1L)
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        "${segment.startedAt.shortLocalTime()} -> ${segment.endedAt.shortLocalTime()}",
        style = tabularNumbers(MaterialTheme.typography.titleLarge),
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(999.dp),
      ) {
        Text(
          signedCompactMinutes(-segment.costMinutes),
          style = tabularNumbers(MaterialTheme.typography.labelLarge),
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
      }
    }
    Canvas(modifier = Modifier.fillMaxWidth().height(34.dp)) {
      val trackHeight = size.height
      val minSliceWidth = 1.5.dp.toPx()
      val minStripeWidth = 1.2.dp.toPx()
      val maxStripeWidth = 4.2.dp.toPx()
      val stripeGap = 0.8.dp.toPx()
      visualSlices.forEach { slice ->
        val color =
          if (slice.packageName in visiblePackages) {
            packageColors.getValue(slice.packageName)
          } else {
            othersColor
          }
        val leftRatio = ((slice.startedAt.toEpochMilli() - startMillis).toFloat() / spanMillis).coerceIn(0f, 1f)
        val rightRatio = ((slice.endedAt.toEpochMilli() - startMillis).toFloat() / spanMillis).coerceIn(0f, 1f)
        val left = leftRatio * size.width
        val right = (rightRatio * size.width).coerceAtLeast(left + minSliceWidth).coerceAtMost(size.width)
        var x = left
        var stripeIndex = 0
        while (x < right) {
          val stripeSeed = slice.packageName.hashCode() * 31 + slice.startedAt.epochSecond.toInt() + stripeIndex * 17
          val stripeWidth =
            (minStripeWidth + ((stripeSeed and Int.MAX_VALUE) % 1000) / 1000f * (maxStripeWidth - minStripeWidth))
              .coerceAtMost(right - x)
          val alpha = 0.68f + ((stripeSeed ushr 8) and 0xFF) / 255f * 0.20f
          drawRect(
            color = color.copy(alpha = alpha),
            topLeft = Offset(x, 0f),
            size = Size(stripeWidth, trackHeight),
          )
          x += stripeWidth + stripeGap
          stripeIndex += 1
        }
      }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      appGroups.forEach { group ->
        val color = if (group.isOthers) MaterialTheme.colorScheme.outline else packageColors.getValue(group.packageName)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier
                .size(10.dp)
                .background(color = color.copy(alpha = 0.86f), shape = CircleShape)
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
              group.appName,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.Medium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              "${group.slices.size} slice${if (group.slices.size == 1) "" else "s"}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            compactMinutes(group.durationMillis.coerceAtLeast(0L) / 60_000.0),
            style = tabularNumbers(MaterialTheme.typography.bodyMedium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

private fun mergeNearbyTimelineSlices(slices: List<PhoneUsageSlice>): List<PhoneUsageSlice> {
  if (slices.isEmpty()) return emptyList()
  val sorted = slices.sortedWith(compareBy<PhoneUsageSlice> { it.startedAt }.thenBy { it.endedAt })
  val merged = mutableListOf<PhoneUsageSlice>()
  var current = sorted.first()
  sorted.drop(1).forEach { next ->
    val gap = (next.startedAt.toEpochMilli() - current.endedAt.toEpochMilli()).milliseconds
    if (current.packageName == next.packageName && gap in 0.milliseconds..60_000.milliseconds) {
      current =
        current.copy(
          endedAt = maxOf(current.endedAt, next.endedAt),
          durationMillis = current.durationMillis + next.durationMillis,
        )
    } else {
      merged += current
      current = next
    }
  }
  merged += current
  return merged
}

@Composable
private fun appTimelineColor(packageName: String): Color {
  val darkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  val colors = remember(darkTheme) { timelinePaletteColors(darkTheme) }
  return colors[(packageName.hashCode() and Int.MAX_VALUE) % colors.size]
}

private fun timelinePaletteColors(darkTheme: Boolean): List<Color> {
  val tone = if (darkTheme) 76.0 else 46.0
  val chroma = if (darkTheme) 42.0 else 48.0
  return TimelineCategoricalBaseColors.map { baseColor ->
    val harmonized = Blend.harmonize(baseColor, TimelineMaterialSeed)
    val hct = Hct.fromInt(harmonized)
    Color(Hct.from(hct.hue, chroma, tone).toInt())
  }
}

private val TimelineMaterialSeed = 0xFF246B49.toInt()

private val TimelineCategoricalBaseColors =
  listOf(
    0xFFE53935.toInt(),
    0xFFD81B60.toInt(),
    0xFF8E24AA.toInt(),
    0xFF5E35B1.toInt(),
    0xFF3949AB.toInt(),
    0xFF1E88E5.toInt(),
    0xFF00ACC1.toInt(),
    0xFF00897B.toInt(),
    0xFF43A047.toInt(),
    0xFF7CB342.toInt(),
    0xFFFDD835.toInt(),
    0xFFFB8C00.toInt(),
  )

private fun timelineAppGroups(slices: List<PhoneUsageSlice>): List<TimelineAppGroup> {
  val ordered = linkedMapOf<String, MutableList<PhoneUsageSlice>>()
  slices.forEach { slice ->
    ordered.getOrPut(slice.packageName) { mutableListOf() } += slice
  }
  val groups =
    ordered.map { (packageName, groupedSlices) ->
      TimelineAppGroup(
        packageName = packageName,
        appName = groupedSlices.first().appName,
        slices = groupedSlices,
        durationMillis = groupedSlices.sumOf { it.durationMillis },
      )
    }
  val top = groups.sortedByDescending { it.durationMillis }.take(6)
  val topPackages = top.mapTo(mutableSetOf()) { it.packageName }
  val othersSlices = slices.filterNot { it.packageName in topPackages }
  val others =
    if (othersSlices.isEmpty()) {
      emptyList()
    } else {
      listOf(
        TimelineAppGroup(
          packageName = "__others__",
          appName = "Others",
          slices = othersSlices,
          durationMillis = othersSlices.sumOf { it.durationMillis },
          isOthers = true,
        )
      )
    }
  return top + others
}

private fun Instant.shortLocalTime(): String =
  toKotlinInstant()
    .toLocalDateTime(TimeZone.currentSystemDefault())
    .time
    .let { "%02d:%02d".format(it.hour, it.minute) }
