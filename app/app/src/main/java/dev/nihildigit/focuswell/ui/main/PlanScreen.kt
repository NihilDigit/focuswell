package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.TagConfig
import androidx.compose.ui.unit.dp

@Composable
internal fun PlanScreen(
  state: FocusWellUiState,
  onAddTag: (String, Double) -> Unit,
  onArchiveTag: (String) -> Unit,
  onUpdateTag: (String, String, Double) -> Unit,
  onAddBooleanTracker: (String, Double) -> Unit,
  onAddRuleTracker: (String, String, Double, Double) -> Unit,
  onArchiveTracker: (String) -> Unit,
  onUpdateManualTracker: (String, String, Double) -> Unit,
  onUpdateRuleTracker: (String, String, String, Double, Double) -> Unit,
) {
  val tags = state.tags.filter { it.archivedAt == null }
  val trackers = state.trackers.filter { it.archivedAt == null }
  var editingTagId by remember { mutableStateOf<String?>(null) }
  var editingTrackerId by remember { mutableStateOf<String?>(null) }
  var addingTag by remember { mutableStateOf(false) }
  var addingManualTracker by remember { mutableStateOf(false) }
  var addingRuleTracker by remember { mutableStateOf(false) }

  LazyColumn(
    contentPadding = PaddingValues(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item { Text("Plan", style = MaterialTheme.typography.headlineSmall) }
    item {
      PlanSectionHeader(
        title = "Tags",
        subtitle = "${tags.size} active focus tags",
        actionLabel = "Add tag",
        onAdd = { addingTag = true },
      )
    }
    if (tags.isEmpty()) {
      item { EmptyRecordText("No tags yet.") }
    } else {
      items(tags, key = { it.id }) { tag ->
        PlanTagRow(tag = tag, onClick = { editingTagId = tag.id })
      }
    }
    item {
      PlanSectionHeader(
        title = "Daily trackers",
        subtitle = "${trackers.size} active · ${compactMinutes(trackers.sumOf { it.rewardMinutes })} possible",
        actionLabel = "Add tracker",
        onAdd = { addingManualTracker = true },
        secondaryActionLabel = "Add rule",
        onSecondaryAdd = { addingRuleTracker = true },
      )
    }
    if (trackers.isEmpty()) {
      item { EmptyRecordText("No daily trackers yet.") }
    } else {
      items(trackers, key = { it.id }) { tracker ->
        PlanTrackerRow(tracker = tracker, onClick = { editingTrackerId = tracker.id })
      }
    }
  }

  tags.firstOrNull { it.id == editingTagId }?.let { tag ->
    TagPlanSheet(
      tag = tag,
      onDismiss = { editingTagId = null },
      onSave = { name, multiplier ->
        editingTagId = null
        onUpdateTag(tag.id, name, multiplier)
      },
      onArchive = {
        editingTagId = null
        onArchiveTag(tag.id)
      },
    )
  }
  trackers.firstOrNull { it.id == editingTrackerId }?.let { tracker ->
    TrackerPlanSheet(
      tracker = tracker,
      tags = tags,
      onDismiss = { editingTrackerId = null },
      onSaveManual = { label, reward ->
        editingTrackerId = null
        onUpdateManualTracker(tracker.id, label, reward)
      },
      onSaveRule = { label, tagName, targetMinutes, reward ->
        editingTrackerId = null
        onUpdateRuleTracker(tracker.id, label, tagName, targetMinutes, reward)
      },
      onArchive = {
        editingTrackerId = null
        onArchiveTracker(tracker.id)
      },
    )
  }
  if (addingTag) {
    TagPlanSheet(
      tag = null,
      onDismiss = { addingTag = false },
      onSave = { name, multiplier ->
        addingTag = false
        onAddTag(name, multiplier)
      },
      onArchive = null,
    )
  }
  if (addingManualTracker) {
    TrackerPlanSheet(
      tracker = null,
      tags = tags,
      ruleMode = false,
      onDismiss = { addingManualTracker = false },
      onSaveManual = { label, reward ->
        addingManualTracker = false
        onAddBooleanTracker(label, reward)
      },
      onSaveRule = { _, _, _, _ -> },
      onArchive = null,
    )
  }
  if (addingRuleTracker) {
    TrackerPlanSheet(
      tracker = null,
      tags = tags,
      ruleMode = true,
      onDismiss = { addingRuleTracker = false },
      onSaveManual = { _, _ -> },
      onSaveRule = { label, tagName, targetMinutes, reward ->
        addingRuleTracker = false
        onAddRuleTracker(label, tagName, targetMinutes, reward)
      },
      onArchive = null,
    )
  }
}
