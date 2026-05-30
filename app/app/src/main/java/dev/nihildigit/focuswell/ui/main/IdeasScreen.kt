package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.Idea
import dev.nihildigit.focuswell.domain.IdeaChecklistItem
import dev.nihildigit.focuswell.domain.IdeaQuadrant

@Composable
internal fun IdeasScreen(
  ideas: List<Idea>,
  onAddIdea: (String) -> Unit,
  onMoveIdea: (String, IdeaQuadrant) -> Unit,
  onUpdateIdea: (String, String, List<IdeaChecklistItem>) -> Unit,
  onArchiveIdea: (String) -> Unit,
) {
  var selectedQuadrants by remember { mutableStateOf(emptySet<IdeaQuadrant>()) }
  var editingIdeaId by remember { mutableStateOf<String?>(null) }
  var draggingIdeaId by remember { mutableStateOf<String?>(null) }
  var showAddIdea by remember { mutableStateOf(false) }
  var dragPosition by remember { mutableStateOf(Offset.Zero) }
  var dragTarget by remember { mutableStateOf<IdeaQuadrant?>(null) }
  var containerOrigin by remember { mutableStateOf(Offset.Zero) }
  val chipBounds = remember { mutableStateMapOf<IdeaQuadrant, Rect>() }
  val rowBounds = remember { mutableStateMapOf<String, Rect>() }
  val density = LocalDensity.current
  val chipDropSlopPx = with(density) { 24.dp.toPx() }
  val activeIdeas = ideas.filter { it.archivedAt == null }
  val filteredIdeas =
    if (selectedQuadrants.isEmpty()) {
      activeIdeas
    } else {
      activeIdeas.filter { it.quadrant in selectedQuadrants }
    }
  val hoveredQuadrant = if (selectedQuadrants.isEmpty()) dragTarget else null

  fun updateDragPosition(position: Offset) {
    dragPosition = position
    dragTarget =
      if (selectedQuadrants.isEmpty()) {
        dropTargetAt(position, chipBounds, chipDropSlopPx)
      } else {
        null
      }
  }

  fun finishDrag() {
    val idea = activeIdeas.firstOrNull { it.id == draggingIdeaId }
    val target = dragTarget
    if (idea != null && target != null && target != idea.quadrant) {
      onMoveIdea(idea.id, target)
    }
    draggingIdeaId = null
    dragPosition = Offset.Zero
    dragTarget = null
  }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .onGloballyPositioned { containerOrigin = it.boundsInRoot().topLeft },
  ) {
    LazyColumn(
      contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        IdeasHeader(
          selectedQuadrants = selectedQuadrants,
          hoveredQuadrant = hoveredQuadrant,
          onSelectedChange = { quadrant ->
            selectedQuadrants =
              if (quadrant in selectedQuadrants) selectedQuadrants - quadrant else selectedQuadrants + quadrant
          },
          onChipPositioned = { quadrant, bounds -> chipBounds[quadrant] = bounds },
        )
      }
      if (filteredIdeas.isEmpty()) {
        item { EmptyIdeaRow(selectedQuadrants) }
      } else {
        items(filteredIdeas, key = { it.id }) { idea ->
          IdeaRow(
            idea = idea,
            dragging = draggingIdeaId == idea.id,
            dragEnabled = selectedQuadrants.isEmpty(),
            modifier = Modifier.animateItem(),
            onClick = { editingIdeaId = idea.id },
            onArchive = { onArchiveIdea(idea.id) },
            onPositioned = { bounds -> rowBounds[idea.id] = bounds },
            onDragStart = { localOffset ->
              draggingIdeaId = idea.id
              updateDragPosition((rowBounds[idea.id]?.topLeft ?: Offset.Zero) + localOffset)
            },
            onDrag = { amount ->
              updateDragPosition(dragPosition + amount)
            },
            onDragEnd = ::finishDrag,
          )
        }
      }
    }
    draggingIdeaId?.let {
      DragScrim(hoveredQuadrant = hoveredQuadrant)
      activeIdeas.firstOrNull { idea -> idea.id == draggingIdeaId }?.let { idea ->
        IdeaDragArrow(
          originBounds = rowBounds[idea.id],
          dragPosition = dragPosition,
          containerOrigin = containerOrigin,
        )
      }
    }
    ExtendedFloatingActionButton(
      onClick = { showAddIdea = true },
      icon = { Icon(Icons.Rounded.Lightbulb, contentDescription = null) },
      text = { Text("Add idea") },
      modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
    )
  }

  if (showAddIdea) {
    IdeaCreateSheet(
      onDismiss = { showAddIdea = false },
      onSave = {
        showAddIdea = false
        onAddIdea(it)
      },
    )
  }

  activeIdeas.firstOrNull { it.id == editingIdeaId }?.let { idea ->
    IdeaEditSheet(
      idea = idea,
      onDismiss = { editingIdeaId = null },
      onSave = { text, checklist ->
        editingIdeaId = null
        onUpdateIdea(idea.id, text, checklist)
      },
      onArchive = {
        editingIdeaId = null
        onArchiveIdea(idea.id)
      },
    )
  }
}
