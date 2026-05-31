package dev.nihildigit.focuswell.ui.main

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.theme.ThemeMode
import dev.nihildigit.focuswell.updates.AppUpdateUiState

@Composable
internal fun SettingsScreen(
  state: FocusWellUiState,
  onExportJson: ((String) -> Unit) -> Unit,
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
  var selectingChargeFreeApps by remember { mutableStateOf(false) }
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
    BackHandler {
      clearPhrase = ""
      confirmClear = false
    }
    ClearAllDataScreen(
      phrase = clearPhrase,
      onPhraseChange = { clearPhrase = it },
      onExport = {
        onExportJson { exportText ->
          pendingExportText = exportText
          exportLauncher.launch("focuswell-export.json")
        }
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
  if (selectingChargeFreeApps) {
    BackHandler { selectingChargeFreeApps = false }
    ChargeFreeAppsScreen(
      selectedPackages = state.rules.normalized().phoneUsageChargeFreePackages,
      onSelectedPackagesChange = { packages ->
        onUpdateRules(state.rules.normalized().copy(phoneUsageChargeFreePackages = packages))
      },
      onBack = { selectingChargeFreeApps = false },
    )
    return
  }
  LazyColumn(
    contentPadding = PaddingValues(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item { Text("Settings", style = MaterialTheme.typography.headlineSmall) }
    item {
      SettingsThemeSection(
        themeMode = themeMode,
        onThemeModeChange = onThemeModeChange,
      )
    }
    item {
      SettingsRulesSection(
        rules = state.rules,
        onUpdateRules = onUpdateRules,
        onManageChargeFreeApps = { selectingChargeFreeApps = true },
      )
    }
    item {
      SettingsRemindersSection(
        rules = state.rules,
        pushRegistrationState = pushRegistrationState,
        notificationPermissionGranted = notificationPermissionGranted,
        onUpdateRules = onUpdateRules,
        onEnablePush = onEnablePush,
        onDisablePush = onDisablePush,
      )
    }
    item {
      SettingsDataSection(
        cloudSyncState = cloudSyncState,
        onStartCloudSync = onStartCloudSync,
        onSignOutCloudSync = onSignOutCloudSync,
        onExport = { confirmExport = true },
        onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
        onClearAllData = { confirmClear = true },
      )
    }
    item {
      SettingsUpdateSection(
        updateState = updateState,
        onCheckUpdate = onCheckUpdate,
        onDownloadUpdate = onDownloadUpdate,
        onInstallUpdate = onInstallUpdate,
        onOpenReleasePage = onOpenUpdateReleasePage,
      )
    }
  }

  if (confirmExport) {
    ExportJsonDialog(
      onDismiss = { confirmExport = false },
      onConfirm = {
        confirmExport = false
        onExportJson { exportText ->
          pendingExportText = exportText
          exportLauncher.launch("focuswell-export.json")
        }
      },
    )
  }

  pendingImportText?.let { importText ->
    ImportJsonDialog(
      onDismiss = { pendingImportText = null },
      onConfirm = {
        pendingImportText = null
        onImportJson(importText)
      },
    )
  }

  cloudSyncState.pendingDecision?.let { decision ->
    CloudSyncDecisionDialog(
      decision = decision,
      onUpload = onCloudSyncUpload,
      onRestore = onCloudSyncRestore,
      onDismiss = onDismissCloudSyncDecision,
    )
  }

  CloudSyncMessageDialog(
    state = cloudSyncState,
    onDismiss = onDismissCloudSyncMessage,
  )

}
