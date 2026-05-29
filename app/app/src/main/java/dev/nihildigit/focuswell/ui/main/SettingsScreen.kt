package dev.nihildigit.focuswell.ui.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import dev.nihildigit.focuswell.BuildConfig
import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.Destination
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.LeisureRecord
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.TagConfig
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.notifications.postFocusWellNotification
import dev.nihildigit.focuswell.theme.FocusWellTheme
import dev.nihildigit.focuswell.theme.ThemeMode
import dev.nihildigit.focuswell.updates.AppUpdateUiState
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
internal fun SettingsRuleRow(
  title: String,
  value: String,
  supporting: String,
  icon: ImageVector,
) {
  Row(
    modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      shape = CircleShape,
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp))
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(
      value,
      style = tabularNumbers(MaterialTheme.typography.titleMedium),
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.End,
    )
  }
}

@Composable
internal fun SettingsListRow(
  title: String,
  supporting: String,
  onArchive: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      FilledTonalButton(onClick = onArchive, modifier = Modifier.width(132.dp).height(44.dp), shape = RoundedCornerShape(22.dp)) {
        Icon(Icons.Rounded.Archive, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Archive", maxLines = 1)
      }
    }
  }
}

@Composable
internal fun SettingsTrackerRow(
  tracker: DailyTracker,
  onRewardChange: (Double) -> Unit,
  onArchive: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().heightIn(min = 78.dp).padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(tracker.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          tracker.progressLabel ?: if (tracker.ruleTagName != null) "Rule" else "Manual",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      RewardStepper(
        rewardMinutes = tracker.rewardMinutes,
        onRewardChange = onRewardChange,
      )
      IconButton(onClick = onArchive, modifier = Modifier.size(40.dp)) {
        Icon(Icons.Rounded.Archive, contentDescription = "Archive", modifier = Modifier.size(20.dp))
      }
    }
  }
}

@Composable
internal fun RewardStepper(
  rewardMinutes: Double,
  onRewardChange: (Double) -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = CircleShape,
    modifier = Modifier.height(40.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      IconButton(
        onClick = { onRewardChange((rewardMinutes - 5.0).coerceAtLeast(0.0)) },
        modifier = Modifier.size(32.dp),
      ) {
        Icon(Icons.Rounded.Remove, contentDescription = "Decrease reward", modifier = Modifier.size(18.dp))
      }
      Text(
        compactMinutes(rewardMinutes),
        style = tabularNumbers(MaterialTheme.typography.labelLarge),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.width(38.dp),
        textAlign = TextAlign.Center,
      )
      IconButton(
        onClick = { onRewardChange(rewardMinutes + 5.0) },
        modifier = Modifier.size(32.dp),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = "Increase reward", modifier = Modifier.size(18.dp))
      }
    }
  }
}

@Composable
internal fun SettingsRuleControlRow(
  title: String,
  value: String,
  supporting: String,
  icon: ImageVector,
  onDecrease: () -> Unit,
  onIncrease: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      shape = CircleShape,
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp))
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    RuleStepper(value = value, onDecrease = onDecrease, onIncrease = onIncrease)
  }
}

@Composable
internal fun SettingsSwitchRow(
  title: String,
  supporting: String,
  icon: ImageVector,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      shape = CircleShape,
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp))
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
internal fun RuleStepper(
  value: String,
  onDecrease: () -> Unit,
  onIncrease: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = CircleShape,
    modifier = Modifier.height(42.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      IconButton(onClick = onDecrease, modifier = Modifier.size(34.dp)) {
        Icon(Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
      }
      Text(
        value,
        style = tabularNumbers(MaterialTheme.typography.labelLarge),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.width(58.dp),
        textAlign = TextAlign.Center,
        maxLines = 1,
      )
      IconButton(onClick = onIncrease, modifier = Modifier.size(34.dp)) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
      }
    }
  }
}

@Composable
internal fun SettingsAddTagForm(
  tagName: String,
  tagMultiplier: String,
  onTagNameChange: (String) -> Unit,
  onTagMultiplierChange: (String) -> Unit,
  onAddTag: () -> Unit,
) {
  SettingsCreateForm(
    title = "New tag",
    supporting = "Tags multiply focus earnings.",
    actionLabel = "Add tag",
    icon = Icons.Rounded.Add,
    enabled = tagName.isNotBlank(),
    onSubmit = onAddTag,
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        value = tagName,
        onValueChange = onTagNameChange,
        label = { Text("Name") },
        singleLine = true,
        modifier = Modifier.weight(1f),
      )
      OutlinedTextField(
        value = tagMultiplier,
        onValueChange = onTagMultiplierChange,
        label = { Text("Rate") },
        singleLine = true,
        suffix = { Text("x") },
        modifier = Modifier.width(118.dp),
      )
    }
  }
}

@Composable
internal fun SettingsAddBooleanTrackerForm(
  trackerLabel: String,
  trackerReward: String,
  onTrackerLabelChange: (String) -> Unit,
  onTrackerRewardChange: (String) -> Unit,
  onAddTracker: () -> Unit,
) {
  SettingsCreateForm(
    title = "New checklist item",
    supporting = "Manual daily completion.",
    actionLabel = "Add tracker",
    icon = Icons.Rounded.CheckCircle,
    enabled = trackerLabel.isNotBlank() && trackerReward.toDoubleOrNull() != null,
    onSubmit = onAddTracker,
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        value = trackerLabel,
        onValueChange = onTrackerLabelChange,
        label = { Text("Label") },
        singleLine = true,
        modifier = Modifier.weight(1f),
      )
      OutlinedTextField(
        value = trackerReward,
        onValueChange = onTrackerRewardChange,
        label = { Text("Reward") },
        singleLine = true,
        suffix = { Text("m") },
        modifier = Modifier.width(126.dp),
      )
    }
  }
}

@Composable
internal fun SettingsAddRuleTrackerForm(
  ruleLabel: String,
  ruleTag: String,
  ruleHours: String,
  ruleReward: String,
  tags: List<TagConfig>,
  onRuleLabelChange: (String) -> Unit,
  onRuleTagChange: (String) -> Unit,
  onRuleHoursChange: (String) -> Unit,
  onRuleRewardChange: (String) -> Unit,
  onAddRuleTracker: () -> Unit,
) {
  var showTagPicker by remember { mutableStateOf(false) }
  SettingsCreateForm(
    title = "New rule tracker",
    supporting = "Auto-completes from focused time.",
    actionLabel = "Add rule",
    icon = Icons.Rounded.Timer,
    enabled = ruleLabel.isNotBlank() && tags.any { it.name == ruleTag } && ruleHours.toDoubleOrNull() != null && ruleReward.toDoubleOrNull() != null,
    onSubmit = onAddRuleTracker,
  ) {
    OutlinedTextField(
      value = ruleLabel,
      onValueChange = onRuleLabelChange,
      label = { Text("Label") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
      TagPickerField(
        selectedTag = ruleTag,
        enabled = tags.isNotEmpty(),
        onClick = { showTagPicker = true },
        modifier = Modifier.weight(1f),
      )
      OutlinedTextField(
        value = ruleHours,
        onValueChange = onRuleHoursChange,
        label = { Text("Target") },
        singleLine = true,
        suffix = { Text("h") },
        modifier = Modifier.width(126.dp),
      )
      OutlinedTextField(
        value = ruleReward,
        onValueChange = onRuleRewardChange,
        label = { Text("Reward") },
        singleLine = true,
        suffix = { Text("m") },
        modifier = Modifier.width(126.dp),
      )
    }
  }
  if (showTagPicker) {
    AlertDialog(
      onDismissRequest = { showTagPicker = false },
      title = { Text("Choose tag") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          tags.forEach { tag ->
            TagPickerOption(
              tag = tag,
              selected = tag.name == ruleTag,
              onClick = {
                onRuleTagChange(tag.name)
                showTagPicker = false
              },
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showTagPicker = false }) {
          Text("Done")
        }
      },
    )
  }
}

@Composable
internal fun TagPickerField(
  selectedTag: String,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
    modifier = modifier.height(64.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
        Text("Tag", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
          selectedTag.ifBlank { "No tags" },
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Icon(Icons.Rounded.ExpandMore, contentDescription = null)
    }
  }
}

@Composable
internal fun TagPickerOption(
  tag: TagConfig,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(tag.name, style = MaterialTheme.typography.titleMedium)
        Text("${tag.multiplier.formatThree()}x multiplier", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = null)
    }
  }
}

@Composable
internal fun SettingsCreateForm(
  title: String,
  supporting: String,
  actionLabel: String,
  icon: ImageVector,
  enabled: Boolean,
  onSubmit: () -> Unit,
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          shape = CircleShape,
        ) {
          Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp).size(18.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
          Text(title, style = MaterialTheme.typography.titleMedium)
          Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      content()
      FilledTonalButton(
        onClick = onSubmit,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(24.dp),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(actionLabel)
      }
    }
  }
}

@Composable
internal fun SettingsDataActionRow(
  title: String,
  supporting: String,
  icon: ImageVector,
  actionLabel: String,
  onClick: () -> Unit,
  destructive: Boolean = false,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        color =
          if (destructive) MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
          else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f),
        contentColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
        shape = CircleShape,
      ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp))
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      if (destructive) {
        OutlinedButton(
          onClick = onClick,
          modifier = Modifier.height(44.dp),
          shape = RoundedCornerShape(22.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
          Text(actionLabel)
        }
      } else {
        FilledTonalButton(onClick = onClick, modifier = Modifier.height(44.dp), shape = RoundedCornerShape(22.dp)) {
          Text(actionLabel)
        }
      }
    }
  }
}

@Composable
internal fun SettingsUpdateRow(
  updateState: AppUpdateUiState,
  onCheckUpdate: () -> Unit,
  onDownloadUpdate: () -> Unit,
  onInstallUpdate: () -> Unit,
  onOpenReleasePage: () -> Unit,
) {
  val release = updateState.latestRelease
  val title = release?.let { "FocusWell ${it.tagName}" } ?: "Current ${BuildConfig.VERSION_NAME}"
  val supporting =
    updateState.error
      ?: updateState.message
      ?: "Check GitHub Releases for a signed APK update."
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f),
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          shape = CircleShape,
        ) {
          Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(title, style = MaterialTheme.typography.titleMedium)
          Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      if (updateState.downloading) {
        LinearProgressIndicator(
          progress = { ((updateState.progress ?: 0) / 100f).coerceIn(0f, 1f) },
          modifier = Modifier.fillMaxWidth(),
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilledTonalButton(
          onClick = onCheckUpdate,
          enabled = !updateState.checking && !updateState.downloading,
          modifier = Modifier.weight(1f).height(44.dp),
          shape = RoundedCornerShape(22.dp),
        ) {
          Text(if (updateState.checking) "Checking" else "Check")
        }
        when {
          updateState.downloadedApk != null -> {
            Button(onClick = onInstallUpdate, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(22.dp)) {
              Text("Install")
            }
          }
          updateState.selection != null -> {
            Button(
              onClick = onDownloadUpdate,
              enabled = !updateState.downloading,
              modifier = Modifier.weight(1f).height(44.dp),
              shape = RoundedCornerShape(22.dp),
            ) {
              Text(if (updateState.downloading) "${updateState.progress ?: 0}%" else "Download")
            }
          }
          release != null -> {
            OutlinedButton(onClick = onOpenReleasePage, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(22.dp)) {
              Text("Release")
            }
          }
        }
      }
      release?.body?.takeIf { it.isNotBlank() }?.lineSequence()?.firstOrNull()?.let { firstLine ->
        Text(firstLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
internal fun SettingsPushRegistrationRow(
  pushRegistrationState: PushRegistrationUiState,
  notificationPermissionGranted: Boolean,
  onEnablePush: () -> Unit,
  onDisablePush: () -> Unit,
) {
  val status = pushRegistrationState.status
  val checked = status.enabled
  val ready = status.enabled && status.hasFcmToken && notificationPermissionGranted
  val supporting =
    when {
      !status.enabled -> "Remote reminder delivery is off."
      !notificationPermissionGranted -> "Notification permission is missing."
      status.lastError != null -> status.lastError
      status.hasFcmToken -> "Registered for remote reminders."
      else -> "Register FCM and allow notifications."
    }
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        color =
          if (ready) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f)
          else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
        contentColor =
          if (ready) MaterialTheme.colorScheme.onSecondaryContainer
          else MaterialTheme.colorScheme.error,
        shape = CircleShape,
      ) {
        Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp))
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Push", style = MaterialTheme.typography.titleMedium)
        Text(
          supporting,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Switch(
        checked = checked,
        onCheckedChange = {
          if (!pushRegistrationState.refreshing) {
            if (it) onEnablePush() else onDisablePush()
          }
        },
        enabled = !pushRegistrationState.refreshing,
      )
    }
    if (pushRegistrationState.refreshing) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeModePicker(
  selected: ThemeMode,
  onSelected: (ThemeMode) -> Unit,
) {
  Row(
    modifier =
      Modifier
        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
        .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ThemeMode.entries.forEach { mode ->
      val checked = selected == mode
      IconToggleButton(
        checked = checked,
        onCheckedChange = { onSelected(mode) },
        modifier = Modifier.size(48.dp),
        shape = if (checked) CircleShape else RoundedCornerShape(16.dp),
        colors =
          IconButtonDefaults.iconToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
          ),
      ) {
        Icon(themeModeIcon(mode), contentDescription = mode.label)
      }
    }
  }
}

internal fun themeModeIcon(mode: ThemeMode): ImageVector =
  when (mode) {
    ThemeMode.System -> Icons.Rounded.BrightnessAuto
    ThemeMode.Dark -> Icons.Rounded.DarkMode
    ThemeMode.Light -> Icons.Rounded.LightMode
  }

private fun Int.hourLabel(): String = "%02d:00".format(this.coerceIn(0, 23))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
  state: FocusWellUiState,
  onExportJson: () -> String,
  onImportJson: (String) -> Unit,
  onClearAllData: () -> Unit,
  onUpdateRules: (FocusWellRules) -> Unit,
  updateState: AppUpdateUiState,
  cloudSyncState: CloudSyncUiState,
  pushRegistrationState: PushRegistrationUiState,
  onStartCloudSync: () -> Unit,
  onCloudSyncUpload: () -> Unit,
  onCloudSyncRestore: () -> Unit,
  onDismissCloudSyncDecision: () -> Unit,
  onDismissCloudSyncMessage: () -> Unit,
  onSignOutCloudSync: () -> Unit,
  onCheckUpdate: () -> Unit,
  onDownloadUpdate: () -> Unit,
  onInstallUpdate: () -> Unit,
  onOpenUpdateReleasePage: () -> Unit,
  notificationPermissionGranted: Boolean,
  onEnablePush: () -> Unit,
  onDisablePush: () -> Unit,
  themeMode: ThemeMode,
  onThemeModeChange: (ThemeMode) -> Unit,
) {
  val context = LocalContext.current
  var confirmClear by remember { mutableStateOf(false) }
  var confirmExport by remember { mutableStateOf(false) }
  var pendingImportText by remember { mutableStateOf<String?>(null) }
  var pendingExportText by remember { mutableStateOf<String?>(null) }
  val exportLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      val exportText = pendingExportText
      pendingExportText = null
      if (uri != null && exportText != null) {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
          stream.write(exportText.toByteArray(Charsets.UTF_8))
        }
      }
    }
  val importLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        val imported =
          context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().use { it.readText() }
          }
        pendingImportText = imported
      }
    }
  var clearPhrase by remember { mutableStateOf("") }
  if (confirmClear) {
    ClearAllDataScreen(
      phrase = clearPhrase,
      onPhraseChange = { clearPhrase = it },
      onExport = {
        pendingExportText = onExportJson()
        exportLauncher.launch("focuswell-export.json")
      },
      onCancel = {
        clearPhrase = ""
        confirmClear = false
      },
      onConfirm = {
        clearPhrase = ""
        confirmClear = false
        onClearAllData()
      },
    )
    return
  }
  LazyColumn(
    contentPadding = PaddingValues(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item { Text("Settings", style = MaterialTheme.typography.headlineSmall) }
    item {
      CalmPanel {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Theme", style = MaterialTheme.typography.titleLarge)
          ThemeModePicker(selected = themeMode, onSelected = onThemeModeChange)
        }
      }
    }
    item {
      CalmPanel {
        val rules = state.rules.normalized()
        Text("Rules", style = MaterialTheme.typography.titleLarge)
        SettingsRuleControlRow(
          title = "Daily grant",
          value = compactMinutes(rules.dailyGrantMinutes),
          supporting = "Added at boundary.",
          icon = Icons.Rounded.AccountBalanceWallet,
          onDecrease = { onUpdateRules(rules.copy(dailyGrantMinutes = rules.dailyGrantMinutes - 5.0)) },
          onIncrease = { onUpdateRules(rules.copy(dailyGrantMinutes = rules.dailyGrantMinutes + 5.0)) },
        )
        SettingsRuleControlRow(
          title = "Boundary",
          value = rules.dayBoundaryHour.hourLabel(),
          supporting = "New day starts here.",
          icon = Icons.Rounded.Today,
          onDecrease = { onUpdateRules(rules.copy(dayBoundaryHour = rules.dayBoundaryHour - 1)) },
          onIncrease = { onUpdateRules(rules.copy(dayBoundaryHour = rules.dayBoundaryHour + 1)) },
        )
        SettingsRuleControlRow(
          title = "Wake time",
          value = rules.wakeTargetHour.hourLabel(),
          supporting = "Check-in bonus from 1h before to 30m after.",
          icon = Icons.Rounded.LightMode,
          onDecrease = { onUpdateRules(rules.copy(wakeTargetHour = rules.wakeTargetHour - 1)) },
          onIncrease = { onUpdateRules(rules.copy(wakeTargetHour = rules.wakeTargetHour + 1)) },
        )
        SettingsRuleControlRow(
          title = "Sleep start",
          value = rules.sleepProtectionStartHour.hourLabel(),
          supporting = "Ideal sleep window begins.",
          icon = Icons.Rounded.Bedtime,
          onDecrease = { onUpdateRules(rules.copy(sleepProtectionStartHour = rules.sleepProtectionStartHour - 1)) },
          onIncrease = { onUpdateRules(rules.copy(sleepProtectionStartHour = rules.sleepProtectionStartHour + 1)) },
        )
        SettingsRuleControlRow(
          title = "Sleep end",
          value = rules.sleepProtectionEndHour.hourLabel(),
          supporting = "Ideal sleep window ends.",
          icon = Icons.Rounded.LightMode,
          onDecrease = { onUpdateRules(rules.copy(sleepProtectionEndHour = rules.sleepProtectionEndHour - 1)) },
          onIncrease = { onUpdateRules(rules.copy(sleepProtectionEndHour = rules.sleepProtectionEndHour + 1)) },
        )
        SettingsRuleControlRow(
          title = "Sleep rate",
          value = "${rules.sleepProtectionMultiplier.formatOne()}x",
          supporting = "Cost multiplier during sleep protection.",
          icon = Icons.Rounded.Timer,
          onDecrease = { onUpdateRules(rules.copy(sleepProtectionMultiplier = rules.sleepProtectionMultiplier - 0.5)) },
          onIncrease = { onUpdateRules(rules.copy(sleepProtectionMultiplier = rules.sleepProtectionMultiplier + 0.5)) },
        )
      }
    }
    item {
      CalmPanel {
        val rules = state.rules.normalized()
        Text("Reminders", style = MaterialTheme.typography.titleLarge)
        SettingsPushRegistrationRow(
          pushRegistrationState = pushRegistrationState,
          notificationPermissionGranted = notificationPermissionGranted,
          onEnablePush = onEnablePush,
          onDisablePush = onDisablePush,
        )
        SettingsSwitchRow(
          title = "Long reminders",
          supporting = "Notify at 1h, 3h, and 5h during focus or leisure.",
          icon = Icons.Rounded.Timer,
          checked = rules.longSessionRemindersEnabled,
          onCheckedChange = { onUpdateRules(rules.copy(longSessionRemindersEnabled = it)) },
        )
      }
    }
    item {
      CalmPanel {
        Text("Data", style = MaterialTheme.typography.titleLarge)
        SettingsDataActionRow(
          title = "Sync with Cloud",
          supporting =
            when {
              cloudSyncState.syncing -> "Checking GitHub cloud backup."
              cloudSyncState.userLogin != null -> "Signed in as ${cloudSyncState.userLogin}."
              else -> "Use GitHub to store one cloud JSON backup."
            },
          icon = Icons.Rounded.CloudUpload,
          actionLabel = if (cloudSyncState.syncing) "Syncing" else "Sync",
          onClick = onStartCloudSync,
        )
        if (cloudSyncState.userLogin != null) {
          TextButton(onClick = onSignOutCloudSync, enabled = !cloudSyncState.syncing) {
            Text("Sign out of cloud sync")
          }
        }
        SettingsDataActionRow(
          title = "Export",
          supporting = "Save a complete JSON backup.",
          icon = Icons.Rounded.Download,
          actionLabel = "Export",
          onClick = { confirmExport = true },
        )
        SettingsDataActionRow(
          title = "Import",
          supporting = "Restore from a JSON export.",
          icon = Icons.Rounded.Upload,
          actionLabel = "Import",
          onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
        )
        SettingsDataActionRow(
          title = "Clear all data",
          supporting = "Remove records, reserve history, trackers, and settings.",
          icon = Icons.Rounded.Delete,
          actionLabel = "Clear",
          onClick = { confirmClear = true },
          destructive = true,
        )
      }
    }
    item {
      CalmPanel {
        Text("Update", style = MaterialTheme.typography.titleLarge)
        SettingsUpdateRow(
          updateState = updateState,
          onCheckUpdate = onCheckUpdate,
          onDownloadUpdate = onDownloadUpdate,
          onInstallUpdate = onInstallUpdate,
          onOpenReleasePage = onOpenUpdateReleasePage,
        )
      }
    }
  }

  if (confirmExport) {
    AlertDialog(
      onDismissRequest = { confirmExport = false },
      title = { Text("Export JSON backup?") },
      text = {
        Text("This creates a complete FocusWell backup file with records, reserve history, ideas, trackers, tags, rules, and ledger entries.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            confirmExport = false
            pendingExportText = onExportJson()
            exportLauncher.launch("focuswell-export.json")
          },
        ) {
          Text("Export")
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmExport = false }) {
          Text("Cancel")
        }
      },
    )
  }

  pendingImportText?.let { importText ->
    AlertDialog(
      onDismissRequest = { pendingImportText = null },
      title = { Text("Import JSON backup?") },
      text = { Text("Import replaces the current FocusWell state on this device. Export first if you may need the current records later.") },
      confirmButton = {
        TextButton(
          onClick = {
            pendingImportText = null
            onImportJson(importText)
          },
        ) {
          Text("Import")
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingImportText = null }) {
          Text("Cancel")
        }
      },
    )
  }

  cloudSyncState.pendingDecision?.let { decision ->
    AlertDialog(
      onDismissRequest = onDismissCloudSyncDecision,
      title = {
        Text(
          when (decision.kind) {
            CloudSyncDecisionKind.Upload -> "Upload local backup?"
            CloudSyncDecisionKind.Restore -> "Restore cloud backup?"
            CloudSyncDecisionKind.Choose -> "Choose sync direction"
          }
        )
      },
      text = {
        Text(
          "Local updated: ${decision.localUpdatedAt}\nCloud updated: ${decision.cloudMetadata.updatedAtUtc}\nCloud app: ${decision.cloudMetadata.appVersion}"
        )
      },
      confirmButton = {
        TextButton(
          onClick =
            when (decision.kind) {
              CloudSyncDecisionKind.Upload -> onCloudSyncUpload
              CloudSyncDecisionKind.Restore -> onCloudSyncRestore
              CloudSyncDecisionKind.Choose -> onCloudSyncUpload
            },
        ) {
          Text(
            when (decision.kind) {
              CloudSyncDecisionKind.Upload -> "Upload"
              CloudSyncDecisionKind.Restore -> "Restore"
              CloudSyncDecisionKind.Choose -> "Upload local"
            }
          )
        }
      },
      dismissButton = {
        Row {
          if (decision.kind == CloudSyncDecisionKind.Choose) {
            TextButton(onClick = onCloudSyncRestore) { Text("Restore cloud") }
          }
          TextButton(onClick = onDismissCloudSyncDecision) { Text("Cancel") }
        }
      },
    )
  }

  if (cloudSyncState.message != null || cloudSyncState.error != null) {
    AlertDialog(
      onDismissRequest = onDismissCloudSyncMessage,
      title = { Text(if (cloudSyncState.error == null) "Cloud sync" else "Cloud sync failed") },
      text = { Text(cloudSyncState.error ?: cloudSyncState.message.orEmpty()) },
      confirmButton = { TextButton(onClick = onDismissCloudSyncMessage) { Text("Done") } },
    )
  }

}

@Composable
internal fun ClearAllDataScreen(
  phrase: String,
  onPhraseChange: (String) -> Unit,
  onExport: () -> Unit,
  onCancel: () -> Unit,
  onConfirm: () -> Unit,
) {
  LazyColumn(
    contentPadding = PaddingValues(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      CalmPanel {
        Text("Clear all data", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
          "This removes local records, reserve history, trackers, tag settings, reminder registration, and device identity.",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    item {
      CalmPanel {
        Text("Export first", style = MaterialTheme.typography.titleLarge)
        Text("Save a JSON backup before clearing if you may need these records later.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilledTonalButton(onClick = onExport, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(24.dp)) {
          Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(8.dp))
          Text("Export JSON")
        }
      }
    }
    item {
      CalmPanel {
        Text("Confirm", style = MaterialTheme.typography.titleLarge)
        Text("Type CLEAR to reset FocusWell on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
          value = phrase,
          onValueChange = onPhraseChange,
          label = { Text("Confirmation") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(52.dp), shape = ControlStartShape) {
            Text("Cancel")
          }
          Button(
            onClick = onConfirm,
            enabled = phrase == "CLEAR",
            modifier = Modifier.weight(1f).height(52.dp),
            shape = ControlEndShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          ) {
            Text("Clear")
          }
        }
      }
    }
  }

}
