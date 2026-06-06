package dev.nihildigit.focuswell.ui.main

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.Destination
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.IdeaChecklistItem
import dev.nihildigit.focuswell.domain.IdeaQuadrant
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.TagConfig
import dev.nihildigit.focuswell.domain.reserveLocked
import dev.nihildigit.focuswell.notifications.canPostNotifications
import dev.nihildigit.focuswell.notifications.postFocusWellNotification
import dev.nihildigit.focuswell.theme.ThemeMode
import dev.nihildigit.focuswell.usage.hasUsageAccess
import dev.nihildigit.focuswell.updates.AppUpdateUiState
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun MainScreen(
  themeMode: ThemeMode,
  onThemeModeChange: (ThemeMode) -> Unit,
  syncRedirectUri: Uri? = null,
  onSyncRedirectConsumed: () -> Unit = {},
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val updateState by viewModel.updateState.collectAsStateWithLifecycle()
  val cloudSyncState by viewModel.cloudSyncState.collectAsStateWithLifecycle()
  val pushRegistrationState by viewModel.pushRegistrationState.collectAsStateWithLifecycle()
  val morningCheckInState by viewModel.morningCheckInState.collectAsStateWithLifecycle()
  val phoneSettlementState by viewModel.phoneSettlementState.collectAsStateWithLifecycle()
  val phoneSettlementAvailable by viewModel.phoneSettlementAvailable.collectAsStateWithLifecycle()
  val latestState by rememberUpdatedState(state)
  val lifecycleOwner = LocalLifecycleOwner.current
  LaunchedEffect(state.dailyDate, state.lastCheckInDailyDate) {
    viewModel.loadMorningCheckInIfNeeded()
  }
  DisposableEffect(lifecycleOwner) {
    val observer =
      LifecycleEventObserver { _, event ->
        val current = latestState
        if (event == Lifecycle.Event.ON_RESUME && current.destination == Destination.Today && current.activeMode == ActiveMode.None) {
          viewModel.refreshPhoneUsageSettlementAvailability()
        }
      }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }
  LaunchedEffect(
    state.destination,
    state.dailyDate,
    state.lastPhoneUsageSettlementAt,
    state.focusRecords,
    state.leisureRecords,
    state.rules,
    state.activeMode,
    state.dailyGrantPausedUntilDate,
  ) {
    if (state.destination == Destination.Today && state.activeMode == ActiveMode.None) {
      viewModel.refreshPhoneUsageSettlementAvailability()
    }
  }
  LaunchedEffect(syncRedirectUri) {
    syncRedirectUri?.let {
      viewModel.handleCloudSyncRedirect(it)
      onSyncRedirectConsumed()
    }
  }
  MainScreen(
    state = state,
    updateState = updateState,
    cloudSyncState = cloudSyncState,
    pushRegistrationState = pushRegistrationState,
    morningCheckInState = morningCheckInState,
    phoneSettlementState = phoneSettlementState,
    phoneSettlementAvailable = phoneSettlementAvailable,
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
    onStartCloudSync = viewModel::startCloudSync,
    onCloudSyncUpload = viewModel::chooseCloudSyncUpload,
    onCloudSyncRestore = viewModel::chooseCloudSyncRestore,
    onDismissCloudSyncDecision = viewModel::dismissCloudSyncDecision,
    onDismissCloudSyncMessage = viewModel::dismissCloudSyncMessage,
    onSignOutCloudSync = viewModel::signOutCloudSync,
    onClearAllData = viewModel::clearAllData,
    onDeleteFocusRecord = viewModel::deleteFocusRecord,
    onUpdateFocusRecord = viewModel::updateFocusRecord,
    onAddManualAdjustment = viewModel::addManualAdjustment,
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
    onStartPhoneUsageSettlement = viewModel::startPhoneUsageSettlement,
    onCancelPhoneUsageSettlement = viewModel::cancelPhoneUsageSettlement,
    onCompletePhoneUsageSettlement = viewModel::completePhoneUsageSettlement,
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
  cloudSyncState: CloudSyncUiState,
  pushRegistrationState: PushRegistrationUiState,
  morningCheckInState: MorningCheckInUiState,
  phoneSettlementState: MorningCheckInUiState,
  phoneSettlementAvailable: Boolean,
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
  onExportJson: ((String) -> Unit) -> Unit,
  onImportJson: (String) -> Unit,
  onDismissImportError: () -> Unit,
  onStartCloudSync: () -> Unit,
  onCloudSyncUpload: () -> Unit,
  onCloudSyncRestore: () -> Unit,
  onDismissCloudSyncDecision: () -> Unit,
  onDismissCloudSyncMessage: () -> Unit,
  onSignOutCloudSync: () -> Unit,
  onClearAllData: () -> Unit,
  onDeleteFocusRecord: (String) -> Unit,
  onUpdateFocusRecord: (String, String, Double) -> Unit,
  onAddManualAdjustment: (String, Double, String?) -> Unit,
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
  onStartPhoneUsageSettlement: () -> Unit = {},
  onCancelPhoneUsageSettlement: () -> Unit = {},
  onCompletePhoneUsageSettlement: (Set<String>) -> Unit = {},
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
    val phoneSettlementActive = phoneSettlementState.startedAt != null || phoneSettlementState.loading
    val navigationHidden = state.activeMode is ActiveMode.Focus || state.reserveLocked || checkInRequired || phoneSettlementActive
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
        } else if (phoneSettlementActive) {
          PhoneUsageSettlementGate(
            state = phoneSettlementState,
            appState = state,
            onCancel = onCancelPhoneUsageSettlement,
            onComplete = onCompletePhoneUsageSettlement,
            modifier = Modifier.weight(1f),
          )
        } else {
          DestinationContent(
            state = state,
            onToggleTracker = onToggleTracker,
            onStartFocusClick = { showFocusSheet = true },
            onSettlePhoneUse = {
              if (hasUsageAccess(context)) {
                onStartPhoneUsageSettlement()
              } else {
                showUsageAccessPrompt = true
              }
            },
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
            onAddManualAdjustment = onAddManualAdjustment,
            onDeleteLeisureRecord = onDeleteLeisureRecord,
            onMoveIdea = onMoveIdea,
            onUpdateIdea = onUpdateIdea,
            onArchiveIdea = onArchiveIdea,
            onExportJson = onExportJson,
            onImportJson = onImportJson,
            cloudSyncState = cloudSyncState,
            onStartCloudSync = onStartCloudSync,
            onCloudSyncUpload = onCloudSyncUpload,
            onCloudSyncRestore = onCloudSyncRestore,
            onDismissCloudSyncDecision = onDismissCloudSyncDecision,
            onDismissCloudSyncMessage = onDismissCloudSyncMessage,
            onSignOutCloudSync = onSignOutCloudSync,
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
            phoneSettlementAvailable = phoneSettlementAvailable,
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
    ImportErrorDialog(error = error, onDismiss = onDismissImportError)
  }

  if (showUsageAccessPrompt && !hasUsageAccess(context)) {
    UsageAccessPromptDialog(
      context = context,
      onDismiss = { showUsageAccessPrompt = false },
    )
  }

}
