package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.Idea
import dev.nihildigit.focuswell.domain.IdeaQuadrant

@Composable
internal fun IdeaRow(
  idea: Idea,
  dragging: Boolean,
  dragEnabled: Boolean,
  onClick: () -> Unit,
  onArchive: () -> Unit,
  onPositioned: (Rect) -> Unit,
  onDragStart: (Offset) -> Unit,
  onDrag: (Offset) -> Unit,
  onDragEnd: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val hasChecklist = idea.checklist.isNotEmpty()
  val ideaTextStyle =
    if (idea.text.hasCjk()) {
      MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Default)
    } else {
      MaterialTheme.typography.bodyLarge
    }
  Surface(
    shape = LedgerRowShape,
    color = MaterialTheme.colorScheme.surfaceContainer,
    shadowElevation = 0.dp,
    modifier =
      modifier
        .fillMaxWidth()
        .onGloballyPositioned { onPositioned(it.boundsInRoot()) }
        .graphicsLayer {
          if (dragging) {
            alpha = 0.42f
          }
        }
        .pointerInput(idea.id) {
          detectTapGestures(onTap = { onClick() })
        }
        .then(
          if (dragEnabled) {
            Modifier.pointerInput(idea.id, dragEnabled) {
              detectDragGesturesAfterLongPress(
                onDragStart = onDragStart,
                onDragCancel = onDragEnd,
                onDragEnd = onDragEnd,
                onDrag = { _, amount -> onDrag(amount) },
              )
            }
          } else {
            Modifier
          }
        ),
  ) {
    Row(
      modifier =
        Modifier
          .padding(horizontal = 14.dp, vertical = if (hasChecklist) 12.dp else 10.dp)
          .fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IdeaQuadrantLabel(quadrant = idea.quadrant)
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = if (hasChecklist) Arrangement.spacedBy(5.dp) else Arrangement.Center,
      ) {
        Text(
          idea.text,
          style = ideaTextStyle,
          maxLines = if (hasChecklist) 4 else 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (hasChecklist) {
          val done = idea.checklist.count { it.checked }
          val preview = idea.checklist.take(2).joinToString(" · ") { it.text }
          Text(
            "$done/${idea.checklist.size} tasks · $preview",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      IconButton(onClick = onArchive, modifier = Modifier.size(if (hasChecklist) 36.dp else 32.dp)) {
        Icon(Icons.Rounded.Archive, contentDescription = "Archive idea", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
      }
    }
  }
}

private fun String.hasCjk(): Boolean =
  any { char ->
    val block = Character.UnicodeBlock.of(char)
    block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
      block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
      block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
      block == Character.UnicodeBlock.HIRAGANA ||
      block == Character.UnicodeBlock.KATAKANA ||
      block == Character.UnicodeBlock.HANGUL_SYLLABLES
  }

@Composable
private fun IdeaQuadrantLabel(quadrant: IdeaQuadrant) {
  val colors = MaterialTheme.colorScheme
  val content =
    when (quadrant) {
      IdeaQuadrant.Inbox -> colors.onSurfaceVariant
      IdeaQuadrant.DoNow -> colors.primary
      IdeaQuadrant.Schedule -> colors.secondary
      IdeaQuadrant.Contain -> colors.tertiary
      IdeaQuadrant.Explore -> colors.primary
    }
  Row(
    modifier = Modifier.width(68.dp),
    horizontalArrangement = Arrangement.spacedBy(3.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(quadrant.icon(), contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
    Text(
      quadrant.label,
      style = MaterialTheme.typography.labelSmall,
      color = content,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
internal fun EmptyIdeaRow(selectedQuadrants: Set<IdeaQuadrant>) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.62f),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      if (selectedQuadrants.isEmpty()) "No ideas captured yet." else "No matching ideas.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(14.dp),
    )
  }
}
