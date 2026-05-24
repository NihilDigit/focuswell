package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.nihildigit.focuswell.domain.Idea
import dev.nihildigit.focuswell.domain.IdeaChecklistItem
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

private fun IdeaQuadrant.icon(): ImageVector =
  when (this) {
    IdeaQuadrant.Inbox -> Icons.Rounded.RadioButtonUnchecked
    IdeaQuadrant.DoNow -> Icons.Rounded.CheckCircle
    IdeaQuadrant.Schedule -> Icons.AutoMirrored.Rounded.EventNote
    IdeaQuadrant.Contain -> Icons.Rounded.Timer
    IdeaQuadrant.Explore -> Icons.Rounded.Lightbulb
  }

@OptIn(ExperimentalMaterial3Api::class)
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
  var containerOrigin by remember { mutableStateOf(Offset.Zero) }
  val chipBounds = remember { mutableStateMapOf<IdeaQuadrant, Rect>() }
  val rowBounds = remember { mutableStateMapOf<String, Rect>() }
  val activeIdeas = ideas.filter { it.archivedAt == null }
  val filteredIdeas =
    if (selectedQuadrants.isEmpty()) {
      activeIdeas
    } else {
      activeIdeas.filter { it.quadrant in selectedQuadrants }
    }
  val hoveredQuadrant =
    if (selectedQuadrants.isEmpty()) {
      draggingIdeaId?.let {
      chipBounds.entries.firstOrNull { (_, bounds) -> bounds.contains(dragPosition) }?.key
      }
    } else {
      null
    }

  fun finishDrag() {
    val idea = activeIdeas.firstOrNull { it.id == draggingIdeaId }
    val target = hoveredQuadrant
    if (idea != null && target != null && target != idea.quadrant) {
      onMoveIdea(idea.id, target)
    }
    draggingIdeaId = null
    dragPosition = Offset.Zero
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
            onClick = { editingIdeaId = idea.id },
            onPositioned = { bounds -> rowBounds[idea.id] = bounds },
            onDragStart = { localOffset ->
              draggingIdeaId = idea.id
              dragPosition = (rowBounds[idea.id]?.topLeft ?: Offset.Zero) + localOffset
            },
            onDrag = { amount ->
              dragPosition += amount
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

@Composable
private fun IdeasHeader(
  selectedQuadrants: Set<IdeaQuadrant>,
  hoveredQuadrant: IdeaQuadrant?,
  onSelectedChange: (IdeaQuadrant) -> Unit,
  onChipPositioned: (IdeaQuadrant, Rect) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom,
    ) {
      SectionHeader(title = "Ideas", subtitle = "Capture loose thoughts, then tag what deserves attention.")
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
private fun DragScrim(hoveredQuadrant: IdeaQuadrant?) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdeaCreateSheet(
  onDismiss: () -> Unit,
  onSave: (String) -> Unit,
) {
  var text by remember { mutableStateOf("") }
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
    Column(
      modifier =
        Modifier
          .padding(horizontal = 20.dp)
          .imePadding()
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("Add idea", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Idea") },
        minLines = 3,
        maxLines = 6,
        modifier = Modifier.fillMaxWidth(),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(54.dp), shape = ControlStartShape) {
          Text("Cancel")
        }
        Button(
          enabled = text.isNotBlank(),
          onClick = { onSave(text) },
          modifier = Modifier.weight(1f).height(54.dp),
          shape = ControlEndShape,
        ) {
          Text("Save")
        }
      }
      Spacer(Modifier.height(16.dp))
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
    border = if (expanded) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
private fun IdeaRow(
  idea: Idea,
  dragging: Boolean,
  dragEnabled: Boolean,
  onClick: () -> Unit,
  onPositioned: (Rect) -> Unit,
  onDragStart: (Offset) -> Unit,
  onDrag: (Offset) -> Unit,
  onDragEnd: () -> Unit,
) {
  Surface(
    shape = LedgerRowShape,
    color = MaterialTheme.colorScheme.surfaceContainer,
    shadowElevation = 0.dp,
    modifier =
      Modifier
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
      modifier = Modifier.padding(14.dp).fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.Top,
    ) {
      IdeaQuadrantBadge(quadrant = idea.quadrant)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
          idea.text,
          style = MaterialTheme.typography.bodyLarge,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis,
        )
        if (idea.checklist.isNotEmpty()) {
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
      Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
  }
}

@Composable
private fun IdeaDragArrow(
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

@Composable
private fun IdeaQuadrantBadge(quadrant: IdeaQuadrant) {
  val colors = MaterialTheme.colorScheme
  val (container, content) =
    when (quadrant) {
      IdeaQuadrant.Inbox -> colors.surfaceContainerHigh to colors.onSurfaceVariant
      IdeaQuadrant.DoNow -> colors.primaryContainer to colors.onPrimaryContainer
      IdeaQuadrant.Schedule -> colors.secondaryContainer to colors.onSecondaryContainer
      IdeaQuadrant.Contain -> colors.tertiaryContainer to colors.onTertiaryContainer
      IdeaQuadrant.Explore -> colors.surfaceContainer to colors.primary
    }
  Surface(color = container, contentColor = content, shape = CircleShape) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(5.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(quadrant.icon(), contentDescription = null, modifier = Modifier.size(15.dp))
      Text(quadrant.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdeaEditSheet(
  idea: Idea,
  onDismiss: () -> Unit,
  onSave: (String, List<IdeaChecklistItem>) -> Unit,
  onArchive: () -> Unit,
) {
  var text by remember(idea.id) { mutableStateOf(idea.text) }
  var checklist by remember(idea.id) { mutableStateOf(idea.checklist) }
  var taskText by remember(idea.id) { mutableStateOf("") }

  fun addTask() {
    val trimmed = taskText.trim()
    if (trimmed.isEmpty()) return
    checklist =
      checklist +
        IdeaChecklistItem(
          id = "task-${System.currentTimeMillis()}-${checklist.size}",
          text = trimmed,
        )
    taskText = ""
  }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
    Column(
      modifier =
        Modifier
          .padding(horizontal = 20.dp)
          .imePadding()
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("Edit idea", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Idea") },
        minLines = 3,
        maxLines = 6,
        modifier = Modifier.fillMaxWidth(),
      )
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Small tasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        checklist.forEach { item ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Checkbox(
              checked = item.checked,
              onCheckedChange = { checked ->
                checklist = checklist.map { if (it.id == item.id) it.copy(checked = checked) else it }
              },
            )
            OutlinedTextField(
              value = item.text,
              onValueChange = { value ->
                checklist = checklist.map { if (it.id == item.id) it.copy(text = value) else it }
              },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            IconButton(
              onClick = { checklist = checklist.filterNot { it.id == item.id } },
            ) {
              Icon(Icons.Rounded.Delete, contentDescription = "Delete task")
            }
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = taskText,
            onValueChange = { taskText = it },
            label = { Text("New task") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          IconButton(
            enabled = taskText.isNotBlank(),
            onClick = ::addTask,
          ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add task")
          }
        }
        if (checklist.isEmpty()) {
          Text(
            "Add a small next step if this idea needs one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onArchive, modifier = Modifier.weight(1f).height(54.dp), shape = ControlStartShape) {
          Icon(Icons.Rounded.Archive, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Archive")
        }
        Button(
          enabled = text.isNotBlank(),
          onClick = {
            val pendingTask = taskText.trim()
            val savedChecklist =
              if (pendingTask.isEmpty()) {
                checklist
              } else {
                checklist +
                  IdeaChecklistItem(
                    id = "task-${System.currentTimeMillis()}-${checklist.size}",
                    text = pendingTask,
                  )
              }
            onSave(text, savedChecklist)
          },
          modifier = Modifier.weight(1f).height(54.dp),
          shape = ControlEndShape,
        ) {
          Text("Save")
        }
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
private fun EmptyIdeaRow(selectedQuadrants: Set<IdeaQuadrant>) {
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
