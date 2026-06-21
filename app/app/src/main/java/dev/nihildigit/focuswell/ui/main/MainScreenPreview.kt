package dev.nihildigit.focuswell.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.reminders.PushRegistrationStatus
import dev.nihildigit.focuswell.theme.FocusWellTheme
import dev.nihildigit.focuswell.theme.ThemeMode
import dev.nihildigit.focuswell.updates.AppUpdateUiState

@Preview(showBackground = true)
@Composable
internal fun MainScreenPreview() {
  FocusWellTheme(dynamicColor = false) {
    MainScreen(
      state = FocusWellUiState(),
      updateState = AppUpdateUiState(),
      cloudSyncState = CloudSyncUiState(),
      pushRegistrationState = PushRegistrationUiState(status = PushRegistrationStatus(deviceId = "preview", hasFcmToken = false)),
      morningCheckInState = MorningCheckInUiState(),
      phoneSettlementState = MorningCheckInUiState(),
      phoneSettlementAvailable = true,
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
      onExportJson = { onExported -> onExported("") },
      onImportJson = {},
      onDismissImportError = {},
      onStartCloudSync = {},
      onCloudSyncUpload = {},
      onCloudSyncRestore = {},
      onDismissCloudSyncDecision = {},
      onDismissCloudSyncMessage = {},
      onSignOutCloudSync = {},
      onClearAllData = {},
      onDeleteFocusRecord = {},
      onUpdateFocusRecord = { _, _, _ -> },
      onAddManualAdjustment = { _, _, _ -> },
      onAddManualFocusRecord = { _, _, _, _, _ -> },
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
      onStartPhoneUsageSettlement = {},
      onCancelPhoneUsageSettlement = {},
      onCompletePhoneUsageSettlement = {},
      notificationPermissionGranted = false,
      onEnablePush = {},
      themeMode = ThemeMode.System,
      onThemeModeChange = {},
    )
  }
}
