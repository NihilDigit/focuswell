package dev.nihildigit.focuswell.ui.main

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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
import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.Destination
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.FocusOutcome
import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.IdeaChecklistItem
import dev.nihildigit.focuswell.domain.IdeaQuadrant
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.LeisureRecord
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.TagConfig
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.notifications.canPostNotifications
import dev.nihildigit.focuswell.notifications.postFocusWellNotification
import dev.nihildigit.focuswell.reminders.PushRegistrationStatus
import dev.nihildigit.focuswell.theme.FocusWellTheme
import dev.nihildigit.focuswell.theme.ThemeMode
import dev.nihildigit.focuswell.usage.hasUsageAccess
import dev.nihildigit.focuswell.updates.AppUpdateUiState
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal val TodayHeroShape = RoundedCornerShape(topStart = 42.dp, topEnd = 48.dp, bottomEnd = 30.dp, bottomStart = 46.dp)
internal val TodayPanelShape = RoundedCornerShape(topStart = 30.dp, topEnd = 42.dp, bottomEnd = 24.dp, bottomStart = 30.dp)
internal val ActiveTimerShape = RoundedCornerShape(topStart = 44.dp, topEnd = 32.dp, bottomEnd = 44.dp, bottomStart = 32.dp)
internal val FocusActionShape = RoundedCornerShape(topStart = 34.dp, topEnd = 20.dp, bottomEnd = 30.dp, bottomStart = 34.dp)
internal val LeisureActionShape = RoundedCornerShape(topStart = 20.dp, topEnd = 34.dp, bottomEnd = 34.dp, bottomStart = 30.dp)
internal val ControlStartShape = RoundedCornerShape(topStart = 26.dp, topEnd = 14.dp, bottomEnd = 22.dp, bottomStart = 26.dp)
internal val ControlEndShape = RoundedCornerShape(topStart = 14.dp, topEnd = 26.dp, bottomEnd = 26.dp, bottomStart = 22.dp)
internal val CalmPanelShape = RoundedCornerShape(16.dp)
internal val LedgerRowShape = RoundedCornerShape(14.dp)
internal val FocusOutcomeOptions = FocusOutcome.entries.map { it.label }

internal enum class ActiveModeMotionKey {
  Idle,
  Focus,
  Leisure,
  Depleted,
}

internal fun AnimatedContentTransitionScope<Destination>.destinationMotionTransform(): ContentTransform =
  fadeIn(animationSpec = tween(durationMillis = 140, delayMillis = 40)) togetherWith
    fadeOut(animationSpec = tween(durationMillis = 90))

internal fun AnimatedContentTransitionScope<ActiveModeMotionKey>.activeModeMotionTransform(): ContentTransform {
  val direction = if (activeModeOrder(targetState) >= activeModeOrder(initialState)) 1 else -1
  return (
    slideInHorizontally(animationSpec = focusWellDefaultSpatialSpec()) { width -> width * direction / 7 } +
      fadeIn(animationSpec = focusWellFastEffectsSpec())
    ) togetherWith (
      slideOutHorizontally(animationSpec = focusWellFastSpatialSpec()) { width -> -width * direction / 10 } +
        fadeOut(animationSpec = focusWellFastEffectsSpec())
      )
}

internal fun <T> focusWellFastSpatialSpec(): FiniteAnimationSpec<T> =
  spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy)

internal fun <T> focusWellDefaultSpatialSpec(): FiniteAnimationSpec<T> =
  spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)

internal fun <T> focusWellFastEffectsSpec(): FiniteAnimationSpec<T> =
  spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioNoBouncy)

internal fun destinationOrder(destination: Destination): Int {
  return when (destination) {
    Destination.Today -> 0
    Destination.Reserve -> 1
    Destination.Ideas -> 2
    Destination.Plan -> 3
    Destination.Settings -> 4
  }
}

internal fun activeModeMotionKey(mode: ActiveMode): ActiveModeMotionKey {
  return when (mode) {
    ActiveMode.None -> ActiveModeMotionKey.Idle
    is ActiveMode.Focus -> ActiveModeMotionKey.Focus
    is ActiveMode.Leisure -> ActiveModeMotionKey.Leisure
    ActiveMode.Depleted -> ActiveModeMotionKey.Depleted
  }
}

internal fun activeModeOrder(mode: ActiveModeMotionKey): Int {
  return when (mode) {
    ActiveModeMotionKey.Idle -> 0
    ActiveModeMotionKey.Focus -> 1
    ActiveModeMotionKey.Leisure -> 2
    ActiveModeMotionKey.Depleted -> 3
  }
}

internal fun tabularNumbers(style: TextStyle): TextStyle = style.copy(fontFeatureSettings = "tnum")

@Composable
fun MainScreen(
  themeMode: ThemeMode,
  onThemeModeChange: (ThemeMode) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val updateState by viewModel.updateState.collectAsStateWithLifecycle()
  val pushRegistrationState by viewModel.pushRegistrationState.collectAsStateWithLifecycle()
  MainScreen(
    state = state,
    updateState = updateState,
    pushRegistrationState = pushRegistrationState,
    onDestination = viewModel::selectDestination,
    onToggleTracker = viewModel::toggleTracker,
    onStartFocus = viewModel::startFocus,
    onPauseFocus = viewModel::pauseFocus,
    onResumeFocus = viewModel::resumeFocus,
    onEndFocus = viewModel::endFocus,
    onAddIdea = viewModel::addIdea,
    onStartLeisure = viewModel::startLeisure,
    onEndLeisure = viewModel::endLeisure,
    onEndDepleted = viewModel::endDepleted,
    onExportJson = viewModel::exportJson,
    onImportJson = viewModel::importJson,
    onDismissImportError = viewModel::dismissImportError,
    onClearAllData = viewModel::clearAllData,
    onDeleteFocusRecord = viewModel::deleteFocusRecord,
    onUpdateFocusRecord = viewModel::updateFocusRecord,
    onDeleteLeisureRecord = viewModel::deleteLeisureRecord,
    onMoveIdea = viewModel::moveIdea,
    onUpdateIdea = viewModel::updateIdea,
    onArchiveIdea = viewModel::archiveIdea,
    onAddTag = viewModel::addTag,
    onArchiveTag = viewModel::archiveTag,
    onAddBooleanTracker = viewModel::addBooleanTracker,
    onAddRuleTracker = viewModel::addRuleTracker,
    onArchiveTracker = viewModel::archiveTracker,
    onUpdateTag = viewModel::updateTag,
    onUpdateManualTracker = viewModel::updateManualTracker,
    onUpdateRuleTracker = viewModel::updateRuleTracker,
    onUpdateRules = viewModel::updateRules,
    onCheckUpdate = viewModel::checkForUpdate,
    onDownloadUpdate = viewModel::downloadUpdate,
    onInstallUpdate = viewModel::installDownloadedUpdate,
    onOpenUpdateReleasePage = viewModel::openUpdateReleasePage,
    onRefreshPushRegistration = viewModel::refreshPushRegistration,
    themeMode = themeMode,
    onThemeModeChange = onThemeModeChange,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
internal fun MainScreen(
  state: FocusWellUiState,
  updateState: AppUpdateUiState,
  pushRegistrationState: PushRegistrationUiState,
  onDestination: (Destination) -> Unit,
  onToggleTracker: (String) -> Unit,
  onStartFocus: (String, SessionType, String?) -> Unit,
  onPauseFocus: () -> Unit,
  onResumeFocus: () -> Unit,
  onEndFocus: (String, Double) -> Unit,
  onAddIdea: (String) -> Unit,
  onStartLeisure: () -> Unit,
  onEndLeisure: () -> Unit,
  onEndDepleted: () -> Unit,
  onExportJson: () -> String,
  onImportJson: (String) -> Unit,
  onDismissImportError: () -> Unit,
  onClearAllData: () -> Unit,
  onDeleteFocusRecord: (String) -> Unit,
  onUpdateFocusRecord: (String, String, Double) -> Unit,
  onDeleteLeisureRecord: (String) -> Unit,
  onMoveIdea: (String, IdeaQuadrant) -> Unit,
  onUpdateIdea: (String, String, List<IdeaChecklistItem>) -> Unit,
  onArchiveIdea: (String) -> Unit,
  onAddTag: (String, Double) -> Unit,
  onArchiveTag: (String) -> Unit,
  onAddBooleanTracker: (String, Double) -> Unit,
  onAddRuleTracker: (String, String, Double, Double) -> Unit,
  onArchiveTracker: (String) -> Unit,
  onUpdateTag: (String, String, Double) -> Unit,
  onUpdateManualTracker: (String, String, Double) -> Unit,
  onUpdateRuleTracker: (String, String, String, Double, Double) -> Unit,
  onUpdateRules: (FocusWellRules) -> Unit,
  onCheckUpdate: () -> Unit,
  onDownloadUpdate: () -> Unit,
  onInstallUpdate: () -> Unit,
  onOpenUpdateReleasePage: () -> Unit,
  onRefreshPushRegistration: () -> Unit,
  themeMode: ThemeMode,
  onThemeModeChange: (ThemeMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showFocusSheet by remember { mutableStateOf(false) }
  val context = LocalContext.current
  var showUsageAccessPrompt by remember { mutableStateOf(!hasUsageAccess(context)) }
  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
      // Timer reminders are best effort; the timestamp ledger remains correct without notifications.
    }
  fun requestReminderPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canPostNotifications(context)) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      bottomBar = {
        if (!useRail) {
          FocusWellNavigationBar(selected = state.destination, onDestination = onDestination)
        }
      },
    ) { innerPadding ->
      Row(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding),
      ) {
        if (useRail) {
          FocusWellNavigationRail(selected = state.destination, onDestination = onDestination)
        }
        DestinationContent(
          state = state,
          onToggleTracker = onToggleTracker,
          onStartFocusClick = { showFocusSheet = true },
          onStartLeisure = {
            requestReminderPermissionIfNeeded()
            onStartLeisure()
          },
          onPauseFocus = onPauseFocus,
          onResumeFocus = onResumeFocus,
          onEndFocus = onEndFocus,
          onAddIdea = onAddIdea,
          onEndLeisure = onEndLeisure,
          onEndDepleted = onEndDepleted,
          onDeleteFocusRecord = onDeleteFocusRecord,
          onUpdateFocusRecord = onUpdateFocusRecord,
          onDeleteLeisureRecord = onDeleteLeisureRecord,
          onMoveIdea = onMoveIdea,
          onUpdateIdea = onUpdateIdea,
          onArchiveIdea = onArchiveIdea,
          onExportJson = onExportJson,
          onImportJson = onImportJson,
          onClearAllData = onClearAllData,
          onAddTag = onAddTag,
          onArchiveTag = onArchiveTag,
          onAddBooleanTracker = onAddBooleanTracker,
          onAddRuleTracker = onAddRuleTracker,
          onArchiveTracker = onArchiveTracker,
          onUpdateTag = onUpdateTag,
          onUpdateManualTracker = onUpdateManualTracker,
          onUpdateRuleTracker = onUpdateRuleTracker,
          onUpdateRules = onUpdateRules,
          updateState = updateState,
          pushRegistrationState = pushRegistrationState,
          onCheckUpdate = onCheckUpdate,
          onDownloadUpdate = onDownloadUpdate,
          onInstallUpdate = onInstallUpdate,
          onOpenUpdateReleasePage = onOpenUpdateReleasePage,
          onRefreshPushRegistration = onRefreshPushRegistration,
          themeMode = themeMode,
          onThemeModeChange = onThemeModeChange,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }

  if (showFocusSheet) {
    StartFocusSheet(
      state = state,
      onDismiss = { showFocusSheet = false },
      onStart = { task, type, tagId ->
        showFocusSheet = false
        requestReminderPermissionIfNeeded()
        onStartFocus(task, type, tagId)
      },
    )
  }

  state.importError?.let { error ->
    AlertDialog(
      onDismissRequest = onDismissImportError,
      title = { Text("Import failed") },
      text = { Text(error) },
      confirmButton = { TextButton(onClick = onDismissImportError) { Text("Done") } },
    )
  }

  if (showUsageAccessPrompt && !hasUsageAccess(context)) {
    AlertDialog(
      onDismissRequest = { showUsageAccessPrompt = false },
      title = { Text("Enable app correction") },
      text = {
        Text(
          "FocusWell can use Android usage access to show app time at focus settlement. Usage data stays local.",
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            showUsageAccessPrompt = false
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
          },
        ) {
          Text("Open settings")
        }
      },
      dismissButton = {
        TextButton(onClick = { showUsageAccessPrompt = false }) {
          Text("Not now")
        }
      },
    )
  }
}

@Composable
private fun DestinationContent(
  state: FocusWellUiState,
  updateState: AppUpdateUiState,
  pushRegistrationState: PushRegistrationUiState,
  onToggleTracker: (String) -> Unit,
  onStartFocusClick: () -> Unit,
  onStartLeisure: () -> Unit,
  onPauseFocus: () -> Unit,
  onResumeFocus: () -> Unit,
  onEndFocus: (String, Double) -> Unit,
  onAddIdea: (String) -> Unit,
  onEndLeisure: () -> Unit,
  onEndDepleted: () -> Unit,
  onDeleteFocusRecord: (String) -> Unit,
  onUpdateFocusRecord: (String, String, Double) -> Unit,
  onDeleteLeisureRecord: (String) -> Unit,
  onMoveIdea: (String, IdeaQuadrant) -> Unit,
  onUpdateIdea: (String, String, List<IdeaChecklistItem>) -> Unit,
  onArchiveIdea: (String) -> Unit,
  onExportJson: () -> String,
  onImportJson: (String) -> Unit,
  onClearAllData: () -> Unit,
  onAddTag: (String, Double) -> Unit,
  onArchiveTag: (String) -> Unit,
  onAddBooleanTracker: (String, Double) -> Unit,
  onAddRuleTracker: (String, String, Double, Double) -> Unit,
  onArchiveTracker: (String) -> Unit,
  onUpdateTag: (String, String, Double) -> Unit,
  onUpdateManualTracker: (String, String, Double) -> Unit,
  onUpdateRuleTracker: (String, String, String, Double, Double) -> Unit,
  onUpdateRules: (FocusWellRules) -> Unit,
  onCheckUpdate: () -> Unit,
  onDownloadUpdate: () -> Unit,
  onInstallUpdate: () -> Unit,
  onOpenUpdateReleasePage: () -> Unit,
  onRefreshPushRegistration: () -> Unit,
  themeMode: ThemeMode,
  onThemeModeChange: (ThemeMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedContent(
    targetState = state.destination,
    transitionSpec = { destinationMotionTransform() },
    label = "destination",
    modifier = modifier.fillMaxSize(),
  ) { destination ->
    when (destination) {
      Destination.Today ->
        TodayScreen(
          state = state,
          onToggleTracker = onToggleTracker,
          onStartFocusClick = onStartFocusClick,
          onStartLeisure = onStartLeisure,
          onPauseFocus = onPauseFocus,
          onResumeFocus = onResumeFocus,
          onEndFocus = onEndFocus,
          onAddIdea = onAddIdea,
          onEndLeisure = onEndLeisure,
          onEndDepleted = onEndDepleted,
        )

      Destination.Reserve ->
        ReserveScreen(
          state = state,
          onDeleteFocusRecord = onDeleteFocusRecord,
          onUpdateFocusRecord = onUpdateFocusRecord,
          onDeleteLeisureRecord = onDeleteLeisureRecord,
        )
      Destination.Ideas ->
        IdeasScreen(
          ideas = state.ideas,
          onAddIdea = onAddIdea,
          onMoveIdea = onMoveIdea,
          onUpdateIdea = onUpdateIdea,
          onArchiveIdea = onArchiveIdea,
        )
      Destination.Plan ->
        PlanScreen(
          state = state,
          onAddTag = onAddTag,
          onArchiveTag = onArchiveTag,
          onUpdateTag = onUpdateTag,
          onAddBooleanTracker = onAddBooleanTracker,
          onAddRuleTracker = onAddRuleTracker,
          onArchiveTracker = onArchiveTracker,
          onUpdateManualTracker = onUpdateManualTracker,
          onUpdateRuleTracker = onUpdateRuleTracker,
        )
      Destination.Settings ->
        SettingsScreen(
          state = state,
          onExportJson = onExportJson,
          onImportJson = onImportJson,
          onClearAllData = onClearAllData,
          onUpdateRules = onUpdateRules,
          updateState = updateState,
          pushRegistrationState = pushRegistrationState,
          onCheckUpdate = onCheckUpdate,
          onDownloadUpdate = onDownloadUpdate,
          onInstallUpdate = onInstallUpdate,
          onOpenUpdateReleasePage = onOpenUpdateReleasePage,
          onRefreshPushRegistration = onRefreshPushRegistration,
          themeMode = themeMode,
          onThemeModeChange = onThemeModeChange,
        )
    }
  }
}

@Composable
internal fun TimerOrganism(
  label: String,
  time: String,
  tone: Color,
  progress: Float? = null,
  supporting: String? = null,
) {
  val animatedProgress by animateFloatAsState(
    targetValue = progress?.coerceIn(0f, 1f) ?: 0f,
    animationSpec = focusWellDefaultSpatialSpec(),
    label = "timer-progress",
  )
  Box(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 228.dp)
        .aspectRatio(1.38f)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh, ActiveTimerShape)
        .border(1.dp, tone.copy(alpha = 0.16f), ActiveTimerShape)
        .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    progress?.let {
      Canvas(modifier = Modifier.size(210.dp)) {
        drawArc(
          color = tone.copy(alpha = 0.16f),
          startAngle = -90f,
          sweepAngle = 360f,
          useCenter = false,
          style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round),
          size = Size(size.minDimension, size.minDimension),
          topLeft = Offset((size.width - size.minDimension) / 2, 0f),
        )
        drawArc(
          color = tone,
          startAngle = -90f,
          sweepAngle = 360f * animatedProgress.coerceIn(0f, 1f),
          useCenter = false,
          style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round),
          size = Size(size.minDimension, size.minDimension),
          topLeft = Offset((size.width - size.minDimension) / 2, 0f),
        )
      }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
      StatusBadge(label, tone)
      Text(
        time,
        style =
          tabularNumbers(if (time.length > 5) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge),
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
      )
      supporting?.let {
        Text(
          it,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
internal fun StatusBadge(text: String, tone: Color, modifier: Modifier = Modifier) {
  val container by animateColorAsState(
    targetValue = tone.copy(alpha = 0.14f),
    animationSpec = focusWellFastEffectsSpec(),
    label = "badge-container",
  )
  Surface(
    color = container,
    contentColor = tone,
    shape = CircleShape,
    modifier = modifier,
  ) {
    Text(
      text,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
      style = MaterialTheme.typography.labelLarge,
    )
  }
}

@Composable
internal fun LedgerRow(entry: LedgerEntry) {
  Surface(
    shape = LedgerRowShape,
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        signedMinutes(entry.deltaMinutes),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color =
          when {
            entry.deltaMinutes > 0.0 -> MaterialTheme.colorScheme.primary
            entry.deltaMinutes < 0.0 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          },
        modifier = Modifier.width(86.dp),
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(entry.title, fontWeight = FontWeight.SemiBold)
        entry.note?.let {
          Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }
}

@Composable
internal fun CalmPanel(content: @Composable ColumnScope.() -> Unit) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    shape = CalmPanelShape,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      content()
    }
  }
}

@Composable
internal fun SectionHeader(title: String, subtitle: String? = null) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    subtitle?.let {
      Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
internal fun FocusWellNavigationBar(
  selected: Destination,
  onDestination: (Destination) -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  ShortNavigationBar(
    containerColor = colors.surfaceContainer,
    contentColor = colors.onSurfaceVariant,
  ) {
    Destination.entries.forEach { destination ->
      ShortNavigationBarItem(
        selected = selected == destination,
        onClick = { onDestination(destination) },
        icon = { DestinationIcon(destination = destination) },
        label = {
          Text(
            destination.label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        colors =
          ShortNavigationBarItemDefaults.colors(
            selectedIconColor = colors.onSecondaryContainer,
            selectedTextColor = colors.secondary,
            selectedIndicatorColor = colors.secondaryContainer,
            unselectedIconColor = colors.onSurfaceVariant,
            unselectedTextColor = colors.onSurfaceVariant,
          ),
      )
    }
  }
}

@Composable
internal fun FocusWellNavigationRail(
  selected: Destination,
  onDestination: (Destination) -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  NavigationRail(
    containerColor = colors.surfaceContainer,
    contentColor = colors.onSurfaceVariant,
  ) {
    Spacer(Modifier.height(12.dp))
    Destination.entries.forEach { destination ->
      NavigationRailItem(
        selected = selected == destination,
        onClick = { onDestination(destination) },
        icon = { DestinationIcon(destination = destination) },
        label = {
          Text(
            destination.label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        colors =
          NavigationRailItemDefaults.colors(
            selectedIconColor = colors.onSecondaryContainer,
            selectedTextColor = colors.secondary,
            indicatorColor = colors.secondaryContainer,
            unselectedIconColor = colors.onSurfaceVariant,
            unselectedTextColor = colors.onSurfaceVariant,
          ),
      )
    }
  }
}

@Composable
internal fun DestinationIcon(destination: Destination) {
  val icon =
    when (destination) {
      Destination.Today -> Icons.Rounded.Today
      Destination.Reserve -> Icons.Rounded.AccountBalanceWallet
      Destination.Ideas -> Icons.Rounded.Lightbulb
      Destination.Plan -> Icons.AutoMirrored.Rounded.EventNote
      Destination.Settings -> Icons.Rounded.Settings
    }
  Icon(
    imageVector = icon,
    contentDescription = null,
  )
}

@Composable
internal fun rememberNow(paused: Boolean = false): Instant {
  var now by remember { mutableStateOf(Instant.now()) }
  LaunchedEffect(paused) {
    while (!paused) {
      now = Instant.now()
      delay(250)
    }
  }
  return now
}

internal fun formatDuration(duration: Duration): String {
  val totalSeconds = duration.seconds.coerceAtLeast(0)
  val hours = totalSeconds / 3600
  val minutes = (totalSeconds % 3600) / 60
  val seconds = totalSeconds % 60
  return if (hours > 0) {
    "%d:%02d:%02d".format(hours, minutes, seconds)
  } else {
    "%02d:%02d".format(minutes, seconds)
  }
}

internal fun effectiveRate(type: SessionType, tagMultiplier: Double): String {
  return (type.rate * tagMultiplier).formatThree()
}

internal fun signedMinutes(minutes: Double): String {
  val rounded = minutes.roundToInt()
  return when {
    rounded > 0 -> "+$rounded min"
    rounded < 0 -> "$rounded min"
    else -> "0 min"
  }
}

internal fun trackerProgress(tracker: DailyTracker): Float {
  if (tracker.completed) return 1f
  val label = tracker.progressLabel ?: return 0f
  val parts = label.split("/")
  if (parts.size != 2) return 0f
  val current = parseDurationMinutes(parts[0])
  val target = parseDurationMinutes(parts[1])
  if (target <= 0.0) return 0f
  return (current / target).toFloat().coerceIn(0f, 1f)
}

internal fun trackerStatusText(tracker: DailyTracker): String {
  return tracker.progressLabel
    ?: when {
      tracker.completed -> "Done"
      else -> "Open"
    }
}

@Composable
internal fun focusOutcomeVisual(outcome: String): Pair<ImageVector, Color> {
  return when (outcome) {
    "As planned" -> Icons.Rounded.CheckCircle to MaterialTheme.colorScheme.primary
    "Partial" -> Icons.Rounded.RadioButtonUnchecked to MaterialTheme.colorScheme.secondary
    "Drifted" -> Icons.Rounded.Pause to MaterialTheme.colorScheme.tertiary
    "Interrupted" -> Icons.Rounded.Stop to MaterialTheme.colorScheme.error
    else -> Icons.Rounded.RadioButtonUnchecked to MaterialTheme.colorScheme.onSurfaceVariant
  }
}

internal fun parseOutcomeResult(result: String): Pair<String, String> {
  val trimmed = result.trim()
  val direct = FocusOutcomeOptions.firstOrNull { it == trimmed }
  if (direct != null) return direct to ""
  val option = FocusOutcomeOptions.firstOrNull { trimmed.startsWith("$it · ") }
  if (option != null) return option to trimmed.removePrefix("$option · ").trim()
  return FocusOutcomeOptions.first() to trimmed
}

internal fun formatOutcomeResult(outcome: String, note: String): String {
  val trimmedNote = note.trim()
  return if (trimmedNote.isBlank()) outcome else "$outcome · $trimmedNote"
}

internal fun parseDurationMinutes(text: String): Double {
  val trimmed = text.trim()
  var total = 0.0
  Regex("""(\d+(?:\.\d+)?)\s*h""").find(trimmed)?.let { total += it.groupValues[1].toDoubleOrNull()?.times(60.0) ?: 0.0 }
  Regex("""(\d+(?:\.\d+)?)\s*m""").find(trimmed)?.let { total += it.groupValues[1].toDoubleOrNull() ?: 0.0 }
  if (total > 0.0) return total
  return trimmed.toDoubleOrNull() ?: 0.0
}

internal fun Double.formatOne(): String = "%.1f".format(this)

internal fun Double.formatThree(): String {
  val text = "%.3f".format(this)
  return text.trimEnd('0').trimEnd('.')
}

@Preview(showBackground = true)
@Composable
internal fun MainScreenPreview() {
  FocusWellTheme(dynamicColor = false) {
    MainScreen(
      state = FocusWellUiState(),
      updateState = AppUpdateUiState(),
      pushRegistrationState = PushRegistrationUiState(status = PushRegistrationStatus(deviceId = "preview", hasFcmToken = false)),
      onDestination = {},
      onToggleTracker = {},
      onStartFocus = { _, _, _ -> },
      onPauseFocus = {},
      onResumeFocus = {},
      onEndFocus = { _, _ -> },
      onAddIdea = {},
      onStartLeisure = {},
      onEndLeisure = {},
      onEndDepleted = {},
      onExportJson = { "" },
      onImportJson = {},
      onDismissImportError = {},
      onClearAllData = {},
      onDeleteFocusRecord = {},
      onUpdateFocusRecord = { _, _, _ -> },
      onDeleteLeisureRecord = {},
      onMoveIdea = { _, _ -> },
      onUpdateIdea = { _, _, _ -> },
      onArchiveIdea = {},
      onAddTag = { _, _ -> },
      onArchiveTag = {},
      onAddBooleanTracker = { _, _ -> },
      onAddRuleTracker = { _, _, _, _ -> },
      onArchiveTracker = {},
      onUpdateTag = { _, _, _ -> },
      onUpdateManualTracker = { _, _, _ -> },
      onUpdateRuleTracker = { _, _, _, _, _ -> },
      onUpdateRules = {},
      onCheckUpdate = {},
      onDownloadUpdate = {},
      onInstallUpdate = {},
      onOpenUpdateReleasePage = {},
      onRefreshPushRegistration = {},
      themeMode = ThemeMode.System,
      onThemeModeChange = {},
    )
  }
}
