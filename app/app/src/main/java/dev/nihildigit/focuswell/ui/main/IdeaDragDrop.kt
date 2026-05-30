package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.nihildigit.focuswell.domain.IdeaQuadrant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val IdeaFilterQuadrants =
  listOf(
    IdeaQuadrant.DoNow,
    IdeaQuadrant.Schedule,
    IdeaQuadrant.Contain,
    IdeaQuadrant.Explore,
  )

internal fun IdeaQuadrant.icon(): ImageVector =
  when (this) {
    IdeaQuadrant.Inbox -> Icons.Rounded.RadioButtonUnchecked
    IdeaQuadrant.DoNow -> Icons.Rounded.CheckCircle
    IdeaQuadrant.Schedule -> Icons.AutoMirrored.Rounded.EventNote
    IdeaQuadrant.Contain -> Icons.Rounded.Timer
    IdeaQuadrant.Explore -> Icons.Rounded.Lightbulb
  }

internal fun dropTargetAt(
  position: Offset,
  chipBounds: Map<IdeaQuadrant, Rect>,
  slopPx: Float,
): IdeaQuadrant? =
  chipBounds.entries
    .filter { (_, bounds) ->
      Rect(
        left = bounds.left - slopPx,
        top = bounds.top - slopPx,
        right = bounds.right + slopPx,
        bottom = bounds.bottom + slopPx,
      ).contains(position)
    }
    .minByOrNull { (_, bounds) ->
      val dx = position.x - bounds.center.x
      val dy = position.y - bounds.center.y
      dx * dx + dy * dy
    }
    ?.key

@Composable
internal fun IdeasHeader(
  selectedQuadrants: Set<IdeaQuadrant>,
  hoveredQuadrant: IdeaQuadrant?,
  onSelectedChange: (IdeaQuadrant) -> Unit,
  onChipPositioned: (IdeaQuadrant, Rect) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom,
    ) {
      SectionHeader(title = "Ideas", subtitle = "Capture loose thoughts, then place what deserves attention.")
    }
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
      IdeaFilterQuadrants.forEach { quadrant ->
        IdeaDropFilterChip(
          quadrant = quadrant,
          selected = quadrant in selectedQuadrants,
          highlighted = hoveredQuadrant == quadrant,
          onClick = { onSelectedChange(quadrant) },
          onPositioned = { onChipPositioned(quadrant, it) },
        )
      }
    }
  }
}

@Composable
internal fun DragScrim(hoveredQuadrant: IdeaQuadrant?) {
  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .zIndex(4f),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      contentColor = MaterialTheme.colorScheme.onSurface,
      shape = CalmPanelShape,
      tonalElevation = 3.dp,
      modifier =
        Modifier
          .align(Alignment.BottomCenter)
          .padding(start = 20.dp, end = 20.dp, bottom = 92.dp)
          .fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val target = hoveredQuadrant
        Icon((target ?: IdeaQuadrant.Inbox).icon(), contentDescription = null, modifier = Modifier.size(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            if (target == null) "Drop on a tag chip" else "Move to ${target.label}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            target?.supporting ?: "Drag over a chip, then release to retag this idea.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun IdeaDropFilterChip(
  quadrant: IdeaQuadrant,
  selected: Boolean,
  highlighted: Boolean,
  onClick: () -> Unit,
  onPositioned: (Rect) -> Unit,
  modifier: Modifier = Modifier,
) {
  val expanded = selected || highlighted
  Surface(
    onClick = onClick,
    color =
      when {
        highlighted -> MaterialTheme.colorScheme.primaryContainer
        selected -> MaterialTheme.colorScheme.secondaryContainer
        else -> androidx.compose.ui.graphics.Color.Transparent
      },
    contentColor =
      when {
        highlighted -> MaterialTheme.colorScheme.onPrimaryContainer
        selected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
      },
    shape = CircleShape,
    border = if (expanded) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier =
      modifier
        .height(42.dp)
        .onGloballyPositioned { onPositioned(it.boundsInRoot()) },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = if (expanded) 12.dp else 11.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(quadrant.icon(), contentDescription = quadrant.label, modifier = Modifier.size(20.dp))
      if (expanded) {
        Text(quadrant.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
  }
}

@Composable
internal fun IdeaDragArrow(
  originBounds: Rect?,
  dragPosition: Offset,
  containerOrigin: Offset,
) {
  val color = MaterialTheme.colorScheme.primary
  Canvas(
    modifier =
      Modifier
        .fillMaxSize()
        .zIndex(4.5f),
  ) {
    val origin = (originBounds?.center ?: return@Canvas) - containerOrigin
    val end = dragPosition - containerOrigin
    val dx = end.x - origin.x
    val dy = end.y - origin.y
    val distance = kotlin.math.hypot(dx, dy)
    if (distance < 56.dp.toPx()) return@Canvas

    val angle = atan2(dy, dx)
    val lineEnd = end - Offset(cos(angle) * 14.dp.toPx(), sin(angle) * 14.dp.toPx())
    drawLine(
      color = color.copy(alpha = 0.52f),
      start = origin,
      end = lineEnd,
      strokeWidth = 2.dp.toPx(),
      cap = StrokeCap.Round,
    )

    val wingLength = 9.dp.toPx()
    val wingAngle = 0.62f
    val left =
      lineEnd -
        Offset(
          cos(angle - wingAngle) * wingLength,
          sin(angle - wingAngle) * wingLength,
        )
    val right =
      lineEnd -
        Offset(
          cos(angle + wingAngle) * wingLength,
          sin(angle + wingAngle) * wingLength,
        )
    drawLine(color = color.copy(alpha = 0.58f), start = lineEnd, end = left, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color.copy(alpha = 0.58f), start = lineEnd, end = right, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
  }
}
