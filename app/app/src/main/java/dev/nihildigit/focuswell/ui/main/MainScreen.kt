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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.rounded.AcUnit
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.graphics.drawable.toBitmap
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
import dev.nihildigit.focuswell.domain.PhoneUsageSegment
import dev.nihildigit.focuswell.domain.PhoneUsageSlice
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
import com.materialkolor.blend.Blend
import com.materialkolor.hct.Hct
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.abs
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

private enum class CheckInStep {
  Income,
  Correction,
  Settlement,
}

private data class CheckInIncomeItem(
  val label: String,
  val minutes: Double,
)

private data class TimelineAppGroup(
  val packageName: String,
  val appName: String,
  val slices: List<PhoneUsageSlice>,
  val durationMillis: Long,
  val isOthers: Boolean = false,
)

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
  val morningCheckInState by viewModel.morningCheckInState.collectAsStateWithLifecycle()
  LaunchedEffect(state.dailyDate, state.lastCheckInDailyDate) {
    viewModel.loadMorningCheckInIfNeeded()
  }
  MainScreen(
    state = state,
    updateState = updateState,
    pushRegistrationState = pushRegistrationState,
    morningCheckInState = morningCheckInState,
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
    onDisablePush = viewModel::disablePush,
    onCompleteMorningCheckIn = viewModel::completeMorningCheckIn,
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
  morningCheckInState: MorningCheckInUiState,
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
  onDisablePush: () -> Unit = {},
  onCompleteMorningCheckIn: (Set<String>) -> Unit = {},
  notificationPermissionGranted: Boolean = true,
  onEnablePush: () -> Unit = {},
  themeMode: ThemeMode,
  onThemeModeChange: (ThemeMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showFocusSheet by remember { mutableStateOf(false) }
  val context = LocalContext.current
  var showUsageAccessPrompt by remember { mutableStateOf(!hasUsageAccess(context)) }
  var notificationPermissionReady by remember { mutableStateOf(notificationPermissionGranted && canPostNotifications(context)) }
  val pushEnabled = pushRegistrationState.status.enabled
  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      notificationPermissionReady = granted || canPostNotifications(context)
      if (notificationPermissionReady) {
        onRefreshPushRegistration()
      }
      // Timer reminders are best effort; the timestamp ledger remains correct without notifications.
    }
  fun requestReminderPermissionIfNeeded() {
    if (!pushEnabled) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canPostNotifications(context)) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      notificationPermissionReady = true
    }
  }
  fun enablePush() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canPostNotifications(context)) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      notificationPermissionReady = true
      onRefreshPushRegistration()
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp
    val checkInRequired = state.lastCheckInDailyDate != state.dailyDate
    val navigationHidden = state.activeMode is ActiveMode.Focus || checkInRequired
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      bottomBar = {
        if (!useRail && !navigationHidden) {
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
        if (useRail && !navigationHidden) {
          FocusWellNavigationRail(selected = state.destination, onDestination = onDestination)
        }
        if (checkInRequired) {
          MorningCheckInGate(
            state = morningCheckInState,
            appState = state,
            onComplete = onCompleteMorningCheckIn,
            modifier = Modifier.weight(1f),
          )
        } else {
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
            notificationPermissionGranted = notificationPermissionReady,
            onEnablePush = ::enablePush,
            onDisablePush = onDisablePush,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            modifier = Modifier.weight(1f),
          )
        }
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
private fun MorningCheckInGate(
  state: MorningCheckInUiState,
  appState: FocusWellUiState,
  onComplete: (Set<String>) -> Unit,
  modifier: Modifier = Modifier,
) {
  var step by remember(state.dailyDate) { mutableStateOf(CheckInStep.Income) }
  var fairUseIds by remember(state.dailyDate, state.segments) { mutableStateOf(emptySet<String>()) }
  val incomeItems = remember(appState.dailyDate, appState.ledger, state.startedAt) { checkInIncomeItems(appState, state.startedAt ?: Instant.now()) }
  val phoneCost = state.segments.filterNot { it.id in fairUseIds }.sumOf { it.costMinutes }
  val available = appState.reserveMinutes + incomeItems.filter { it.label == "Wake bonus" }.sumOf { it.minutes }
  val deducted = minOf(phoneCost, available)
  val remaining = (available - deducted).coerceAtLeast(0.0)
  val exceeded = phoneCost > available
  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = modifier.fillMaxSize(),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      AnimatedContent(
        targetState = step,
        transitionSpec = {
          fadeIn(animationSpec = tween(durationMillis = 140, delayMillis = 40)) togetherWith
            fadeOut(animationSpec = tween(durationMillis = 90))
        },
        label = "morning-check-in-step",
        modifier = Modifier.fillMaxSize(),
      ) {
        when (it) {
          CheckInStep.Income ->
            CheckInIncomeScreen(
              incomeItems = incomeItems,
              onContinue = { step = CheckInStep.Correction },
            )

          CheckInStep.Correction ->
            CheckInCorrectionScreen(
              state = state,
              fairUseIds = fairUseIds,
              phoneCost = phoneCost,
              onToggleFairUse = { segmentId ->
                fairUseIds = if (segmentId in fairUseIds) fairUseIds - segmentId else fairUseIds + segmentId
              },
              onContinue = { step = CheckInStep.Settlement },
            )

          CheckInStep.Settlement ->
            CheckInSettlementScreen(
              incomeMinutes = incomeItems.sumOf { item -> item.minutes },
              phoneCost = phoneCost,
              deducted = deducted,
              remaining = remaining,
              exceeded = exceeded,
              fairUseCount = fairUseIds.size,
              reviewedSegmentCount = state.segments.size,
              onBack = { step = CheckInStep.Correction },
              onDone = { onComplete(fairUseIds) },
            )
        }
      }
    }
  }
}

@Composable
private fun CheckInIncomeScreen(
  incomeItems: List<CheckInIncomeItem>,
  onContinue: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    CheckInStepHeader(
      title = "Income",
      subtitle = "Completed rewards are ready.",
    )
    Box(
      modifier = Modifier.fillMaxWidth().weight(1f),
      contentAlignment = Alignment.Center,
    ) {
      val displayCount = incomeItems.size.coerceAtLeast(1)
      val itemSpacing = 14.dp
      val estimatedItemHeight = 74.dp
      val contentHeight = estimatedItemHeight * displayCount + itemSpacing * (displayCount - 1)
      Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = contentHeight),
        verticalArrangement = Arrangement.spacedBy(itemSpacing, Alignment.CenterVertically),
      ) {
        if (incomeItems.isEmpty()) {
          CalmPanel {
            Text("No rewards to settle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Continue to phone correction.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        } else {
          incomeItems.forEachIndexed { index, item ->
            AnimatedAccountingItem(
              label = item.label,
              amount = item.minutes,
              tone = MaterialTheme.colorScheme.primary,
              delayMillis = index * 170,
            )
          }
        }
      }
    }
    Button(
      onClick = onContinue,
      modifier = Modifier.fillMaxWidth().height(54.dp),
      shape = ControlEndShape,
    ) {
      Text("Continue")
    }
  }
}

@Composable
private fun CheckInCorrectionScreen(
  state: MorningCheckInUiState,
  fairUseIds: Set<String>,
  phoneCost: Double,
  onToggleFairUse: (String) -> Unit,
  onContinue: () -> Unit,
) {
  val segments = remember(state.segments) { state.segments.sortedBy { it.startedAt } }
  var currentIndex by remember(segments) { mutableStateOf(0) }
  var reviewHistory by remember(segments) { mutableStateOf(emptyList<Pair<String, Boolean>>()) }
  val currentSegment = segments.getOrNull(currentIndex)
  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top,
    ) {
      CheckInStepHeader(
        title = "Correction",
        subtitle = "Swipe right for Fair Use.",
      )
      if (reviewHistory.isNotEmpty()) {
        TextButton(
          onClick = {
            val last = reviewHistory.last()
            reviewHistory = reviewHistory.dropLast(1)
            currentIndex = (currentIndex - 1).coerceAtLeast(0)
            if (last.second && last.first in fairUseIds) {
              onToggleFairUse(last.first)
            }
          },
        ) {
          Text("Undo")
        }
      }
    }
    if (state.loading) {
      Box(modifier = Modifier.weight(1f)) {
        CalmPanel {
          Text("Reading local usage events", style = MaterialTheme.typography.titleMedium)
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
      }
    } else if (state.segments.isEmpty()) {
      Box(modifier = Modifier.weight(1f)) {
        CalmPanel {
          Text("No phone blocks found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          Text("No non-Focus/Leisure phone block reached the review threshold.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    } else {
      Box(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentAlignment = Alignment.Center,
      ) {
        if (currentSegment == null) {
          CalmPanel {
            Text("All blocks reviewed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${fairUseIds.size} marked Fair Use", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        } else {
          PhoneUsageSegmentCard(
            segment = currentSegment,
            index = currentIndex,
            total = segments.size,
            onCount = {
              reviewHistory = reviewHistory + (currentSegment.id to false)
              currentIndex += 1
            },
            onFairUse = {
              if (currentSegment.id !in fairUseIds) {
                onToggleFairUse(currentSegment.id)
              }
              reviewHistory = reviewHistory + (currentSegment.id to true)
              currentIndex += 1
            },
          )
        }
      }
    }
    Button(
      onClick = onContinue,
      enabled = !state.loading && (segments.isEmpty() || currentSegment == null),
      modifier = Modifier.fillMaxWidth().height(54.dp),
      shape = ControlEndShape,
    ) {
      Text("Continue")
    }
  }
}

@Composable
private fun CheckInSettlementScreen(
  incomeMinutes: Double,
  phoneCost: Double,
  deducted: Double,
  remaining: Double,
  exceeded: Boolean,
  fairUseCount: Int,
  reviewedSegmentCount: Int,
  onBack: () -> Unit,
  onDone: () -> Unit,
) {
  LazyColumn(
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      CheckInStepHeader(
        title = "Settlement",
        subtitle = "Review the final accounting.",
      )
    }
    item {
      AnimatedAccountingItem(
        label = "Income",
        amount = incomeMinutes,
        tone = MaterialTheme.colorScheme.primary,
        delayMillis = 0,
      )
    }
    item {
      AnimatedAccountingItem(
        label = "Fair Use",
        amount = fairUseCount.toDouble(),
        valueText = "$fairUseCount / $reviewedSegmentCount",
        tone = MaterialTheme.colorScheme.secondary,
        delayMillis = 170,
      )
    }
    item {
      AnimatedAccountingItem(
        label = "Phone correction",
        amount = -deducted,
        valueText = signedCompactMinutes(-deducted),
        tone = MaterialTheme.colorScheme.tertiary,
        delayMillis = 340,
      )
    }
    item {
      CalmPanel {
        Text("Formula", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        CheckInFormulaLine("Phone cost", signedCompactMinutes(-phoneCost))
        CheckInFormulaLine("Deducted", signedCompactMinutes(-deducted))
        CheckInFormulaLine("Remaining", compactMinutes(remaining))
      }
    }
    if (exceeded) {
      item { FrozenDailyGrantPanel() }
    }
    item {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(54.dp), shape = ControlStartShape) {
          Text("Back")
        }
        Button(onClick = onDone, modifier = Modifier.weight(1f).height(54.dp), shape = ControlEndShape) {
          Text("Done")
        }
      }
    }
  }
}

@Composable
private fun PhoneUsageSegmentCard(
  segment: PhoneUsageSegment,
  index: Int,
  total: Int,
  onCount: () -> Unit,
  onFairUse: () -> Unit,
) {
  var offsetX by remember(segment.id) { mutableStateOf(0f) }
  val threshold = with(LocalDensity.current) { 116.dp.toPx() }
  val rotation = (offsetX / threshold).coerceIn(-1f, 1f) * 5f
  val fairAlpha = (offsetX / threshold).coerceIn(0f, 1f)
  val countAlpha = (-offsetX / threshold).coerceIn(0f, 1f)
  val dragProgress = (abs(offsetX) / threshold).coerceIn(0f, 1f)
  val draggedRight = offsetX > 0f
  val cardElevation by animateDpAsState(
    targetValue = if (dragProgress > 0.04f) 6.dp else 1.dp,
    animationSpec = focusWellFastEffectsSpec(),
    label = "phone-review-card-elevation",
  )
  val cardColor by animateColorAsState(
    targetValue =
      when {
        offsetX > 8f -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.26f + dragProgress * 0.22f)
        offsetX < -8f -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.24f + dragProgress * 0.20f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
      },
    animationSpec = focusWellFastEffectsSpec(),
    label = "phone-review-card-color",
  )
  val borderColor by animateColorAsState(
    targetValue =
      when {
        offsetX > 8f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f + dragProgress * 0.34f)
        offsetX < -8f -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f + dragProgress * 0.34f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
      },
    animationSpec = focusWellFastEffectsSpec(),
    label = "phone-review-card-border",
  )
  val actualMinutes = segment.slices.sumOf { it.durationMillis } / 60_000.0
  val sleepProtected = segment.costMinutes > actualMinutes + 0.01
  Box(
    modifier = Modifier.fillMaxWidth().heightIn(min = 420.dp),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = cardColor),
      border = BorderStroke(1.dp, borderColor),
      elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
      shape = RoundedCornerShape(26.dp),
      modifier =
        Modifier
          .fillMaxWidth()
          .graphicsLayer {
            translationX = offsetX
            rotationZ = rotation
          }
          .pointerInput(segment.id) {
            detectDragGestures(
              onDragEnd = {
                when {
                  offsetX > threshold -> onFairUse()
                  offsetX < -threshold -> onCount()
                }
                offsetX = 0f
              },
              onDragCancel = { offsetX = 0f },
              onDrag = { change, dragAmount ->
                change.consume()
                offsetX += dragAmount.x
              },
            )
          },
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            "${index + 1} / $total",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (sleepProtected) {
              Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(999.dp),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(Icons.Rounded.Bedtime, contentDescription = null, modifier = Modifier.size(14.dp))
                  Text("Sleep x2", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
              }
            }
            if (dragProgress > 0.04f) {
              Surface(
                color =
                  if (draggedRight) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f + fairAlpha * 0.18f)
                  else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f + countAlpha * 0.16f),
                contentColor = if (draggedRight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(999.dp),
              ) {
                Text(
                  if (draggedRight) "Fair Use" else "Count",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
              }
            }
          }
        }
        PhoneUsageTimeline(segment = segment)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            "Count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.44f + countAlpha * 0.44f),
          )
          Text(
            "Fair Use",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.44f + fairAlpha * 0.44f),
          )
        }
      }
    }
  }
}

@Composable
private fun PhoneUsageTimeline(segment: PhoneUsageSegment) {
  val slices = segment.slices.ifEmpty {
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
          val stripeWidth = (minStripeWidth + ((stripeSeed and Int.MAX_VALUE) % 1000) / 1000f * (maxStripeWidth - minStripeWidth)).coerceAtMost(right - x)
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
            compactDurationMinutes(Duration.ofMillis(group.durationMillis)),
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
    val gapMillis = Duration.between(current.endedAt, next.startedAt).toMillis()
    if (current.packageName == next.packageName && gapMillis in 0..60_000) {
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

@Composable
private fun PhoneUsageSegmentRow(
  segment: PhoneUsageSegment,
  fairUse: Boolean,
  onToggle: () -> Unit,
) {
  Surface(
    onClick = onToggle,
    color = if (fairUse) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f) else Color.Transparent,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(0.dp),
    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      AppIconStack(segment = segment, modifier = Modifier.width(82.dp))
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          "${segment.startedAt.shortLocalTime()} -> ${segment.endedAt.shortLocalTime()}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
        )
        Surface(
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
          shape = RoundedCornerShape(999.dp),
        ) {
          Text(
            compactMinutes(segment.costMinutes),
            style = tabularNumbers(MaterialTheme.typography.labelSmall),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }
      Icon(
        imageVector = if (fairUse) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
        contentDescription = null,
        tint = if (fairUse) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(24.dp),
      )
    }
  }
}

@Composable
private fun CheckInStepHeader(title: String, subtitle: String) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun AnimatedAccountingItem(
  label: String,
  amount: Double,
  tone: Color,
  valueText: String? = null,
  delayMillis: Int = 0,
) {
  var entered by remember(label, amount, valueText) { mutableStateOf(false) }
  var checked by remember(label, amount, valueText) { mutableStateOf(false) }
  LaunchedEffect(label, amount, valueText, delayMillis) {
    entered = false
    checked = false
    delay(260L + delayMillis)
    entered = true
    delay(280L)
    checked = true
  }
  val entranceAlpha by animateFloatAsState(
    targetValue = if (entered) 1f else 0f,
    animationSpec = tween(durationMillis = 240),
    label = "checkin-accounting-alpha",
  )
  val entranceOffset by animateDpAsState(
    targetValue = if (entered) 0.dp else 18.dp,
    animationSpec = tween(durationMillis = 260),
    label = "checkin-accounting-offset",
  )
  val animatedAmount by animateFloatAsState(
    targetValue = if (checked) amount.toFloat() else 0f,
    animationSpec = tween(durationMillis = 680, easing = LinearEasing),
    label = "checkin-accounting-amount",
  )
  val container by animateColorAsState(
    targetValue = if (checked) tone.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainer,
    animationSpec = focusWellFastEffectsSpec(),
    label = "checkin-accounting-container",
  )
  Surface(
    color = container,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = LedgerRowShape,
    modifier =
      Modifier
        .fillMaxWidth()
        .offset(y = entranceOffset)
        .alpha(entranceAlpha)
        .heightIn(min = 64.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(color = tone, contentColor = MaterialTheme.colorScheme.onPrimary, shape = CircleShape, modifier = Modifier.size(32.dp)) {
        Box(contentAlignment = Alignment.Center) {
          AnimatedContent(
            targetState = checked,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(80)) },
            label = "checkin-accounting-check",
          ) { visible ->
            if (visible) {
              Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(19.dp))
            } else {
              Spacer(Modifier.size(19.dp))
            }
          }
        }
      }
      Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
      Text(
        valueText?.takeIf { checked } ?: signedMinutes(animatedAmount.toDouble()),
        style = tabularNumbers(MaterialTheme.typography.titleMedium),
        fontWeight = FontWeight.Bold,
        color = tone,
      )
    }
  }
}

@Composable
private fun CheckInFormulaLine(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = tabularNumbers(MaterialTheme.typography.bodyMedium), fontWeight = FontWeight.SemiBold)
  }
}

@Composable
private fun FrozenDailyGrantPanel() {
  Surface(
    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.52f),
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(18.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = CircleShape, modifier = Modifier.size(46.dp)) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Rounded.AcUnit, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Daily grant paused", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Unconditional grant only", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Text("+60 x3", style = tabularNumbers(MaterialTheme.typography.titleLarge), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
    }
  }
}

@Composable
private fun AppIconStack(segment: PhoneUsageSegment, modifier: Modifier = Modifier) {
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically, modifier = modifier.wrapContentWidth(Alignment.Start)) {
    segment.topApps.take(3).forEachIndexed { index, app ->
      val size = 28.dp
      AppPackageIcon(
        packageName = app.packageName,
        appName = app.appName,
        modifier = Modifier.size(size),
      )
    }
  }
}

@Composable
private fun AppPackageIcon(packageName: String, appName: String, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val bitmap = remember(packageName) {
    runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap() }.getOrNull()
  }
  if (bitmap != null) {
    Image(
      bitmap = bitmap,
      contentDescription = appName,
      modifier = modifier.clip(CircleShape),
    )
  } else {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurfaceVariant, shape = CircleShape, modifier = modifier) {
      Box(contentAlignment = Alignment.Center) {
        Text(
          appName.take(1).uppercase(),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }
}

private fun checkInIncomeItems(state: FocusWellUiState, startedAt: Instant): List<CheckInIncomeItem> {
  val dailyDate = state.dailyDate
  val items = mutableListOf<CheckInIncomeItem>()
  state.ledger.firstOrNull { it.id == "daily-grant-$dailyDate" && it.deltaMinutes > 0.0 }?.let {
    items += CheckInIncomeItem("Daily grant", it.deltaMinutes)
  }
  val today = runCatching { LocalDate.parse(dailyDate) }.getOrNull()
  val previousDate = today?.minusDays(1)?.toString()
  state.ledger
    .filter { it.title == "Daily tracker" && it.deltaMinutes > 0.0 && (previousDate == null || it.id.startsWith("tracker-reward-$previousDate-")) }
    .sortedBy { it.note ?: it.id }
    .forEach { items += CheckInIncomeItem(it.note ?: "Daily tracker", it.deltaMinutes) }
  if (isWakeBonusEligible(state, startedAt)) {
    items += CheckInIncomeItem("Wake bonus", 30.0)
  }
  return items
}

private fun isWakeBonusEligible(state: FocusWellUiState, startedAt: Instant): Boolean {
  val target = state.rules.normalized().wakeTargetTime
  val local = startedAt.atZone(ZoneId.systemDefault()).toLocalTime()
  val delta = Duration.between(target, local).toMinutes()
  return delta in -60..30
}

private val CheckInTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun Instant.shortLocalTime(): String =
  atZone(ZoneId.systemDefault()).format(CheckInTimeFormatter)

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
  notificationPermissionGranted: Boolean,
  onEnablePush: () -> Unit,
  onDisablePush: () -> Unit,
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
          notificationPermissionGranted = notificationPermissionGranted,
          onEnablePush = onEnablePush,
          onDisablePush = onDisablePush,
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
  if (totalSeconds < 60) return "<1m"
  val totalMinutes = (totalSeconds / 60).coerceAtLeast(0)
  return "${totalMinutes}m"
}

internal fun formatPreciseDuration(duration: Duration): String {
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
  if (minutes > 0.0 && minutes < 1.0) return "+<1m"
  if (minutes < 0.0 && minutes > -1.0) return "-<1m"
  val rounded = minutes.roundToInt()
  return when {
    rounded > 0 -> "+${rounded}m"
    rounded < 0 -> "${rounded}m"
    else -> "0"
  }
}

internal fun compactMinutes(minutes: Double): String {
  if (minutes == 0.0) return "0"
  if (minutes > 0.0 && minutes < 1.0) return "<1m"
  if (minutes < 0.0 && minutes > -1.0) return "-<1m"
  return "${minutes.roundToInt()}m"
}

internal fun signedCompactMinutes(minutes: Double): String =
  when {
    minutes > 0.0 && minutes < 1.0 -> "+<1m"
    minutes < 0.0 && minutes > -1.0 -> "-<1m"
    else -> signedMinutes(minutes)
  }

internal fun compactDurationMinutes(duration: Duration): String =
  compactMinutes(duration.toMillis().coerceAtLeast(0L) / 60_000.0)

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
      morningCheckInState = MorningCheckInUiState(),
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
      onDisablePush = {},
      onCompleteMorningCheckIn = {},
      notificationPermissionGranted = false,
      onEnablePush = {},
      themeMode = ThemeMode.System,
      onThemeModeChange = {},
    )
  }
}
