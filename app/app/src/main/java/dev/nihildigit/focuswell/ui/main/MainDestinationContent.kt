package dev.nihildigit.focuswell.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import dev.nihildigit.focuswell.domain.Destination
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.IdeaChecklistItem
import dev.nihildigit.focuswell.domain.IdeaQuadrant
import dev.nihildigit.focuswell.updates.AppUpdateUiState
import dev.nihildigit.focuswell.theme.ThemeMode

@Composable
internal fun DestinationContent(
  state: FocusWellUiState,
  updateState: AppUpdateUiState,
  cloudSyncState: CloudSyncUiState,
  pushRegistrationState: PushRegistrationUiState,
  onToggleTracker: (String) -> Unit,
  onStartFocusClick: () -> Unit,
  onSettlePhoneUse: () -> Unit,
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
  onExportJson: ((String) -> Unit) -> Unit,
  onImportJson: (String) -> Unit,
  onStartCloudSync: () -> Unit,
  onCloudSyncUpload: () -> Unit,
  onCloudSyncRestore: () -> Unit,
  onDismissCloudSyncDecision: () -> Unit,
  onDismissCloudSyncMessage: () -> Unit,
  onSignOutCloudSync: () -> Unit,
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
  phoneSettlementAvailable: Boolean,
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
          onSettlePhoneUse = onSettlePhoneUse,
          phoneSettlementAvailable = phoneSettlementAvailable,
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
          cloudSyncState = cloudSyncState,
          pushRegistrationState = pushRegistrationState,
          onStartCloudSync = onStartCloudSync,
          onCloudSyncUpload = onCloudSyncUpload,
          onCloudSyncRestore = onCloudSyncRestore,
          onDismissCloudSyncDecision = onDismissCloudSyncDecision,
          onDismissCloudSyncMessage = onDismissCloudSyncMessage,
          onSignOutCloudSync = onSignOutCloudSync,
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
