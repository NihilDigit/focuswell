package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.theme.ThemeMode
import dev.nihildigit.focuswell.updates.AppUpdateUiState

private fun Int.hourLabel(): String = "%02d:00".format(this.coerceIn(0, 23))

@Composable
internal fun SettingsThemeSection(
  themeMode: ThemeMode,
  onThemeModeChange: (ThemeMode) -> Unit,
) {
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

@Composable
internal fun SettingsRulesSection(
  rules: FocusWellRules,
  onUpdateRules: (FocusWellRules) -> Unit,
  onManageChargeFreeApps: () -> Unit,
) {
  val normalizedRules = rules.normalized()
  CalmPanel {
    Text("Rules", style = MaterialTheme.typography.titleLarge)
    SettingsRuleControlRow(
      title = "Daily grant",
      value = compactMinutes(normalizedRules.dailyGrantMinutes),
      supporting = "Added at day boundary",
      icon = Icons.Rounded.AccountBalanceWallet,
      onDecrease = { onUpdateRules(normalizedRules.copy(dailyGrantMinutes = normalizedRules.dailyGrantMinutes - 5.0)) },
      onIncrease = { onUpdateRules(normalizedRules.copy(dailyGrantMinutes = normalizedRules.dailyGrantMinutes + 5.0)) },
    )
    SettingsRuleControlRow(
      title = "Boundary",
      value = normalizedRules.dayBoundaryHour.hourLabel(),
      supporting = "New day starts here",
      icon = Icons.Rounded.Today,
      onDecrease = { onUpdateRules(normalizedRules.copy(dayBoundaryHour = normalizedRules.dayBoundaryHour - 1)) },
      onIncrease = { onUpdateRules(normalizedRules.copy(dayBoundaryHour = normalizedRules.dayBoundaryHour + 1)) },
    )
    SettingsRuleControlRow(
      title = "Wake time",
      value = normalizedRules.wakeTargetHour.hourLabel(),
      supporting = "Bonus from 1 hour before to 30 minutes after",
      icon = Icons.Rounded.LightMode,
      onDecrease = { onUpdateRules(normalizedRules.copy(wakeTargetHour = normalizedRules.wakeTargetHour - 1)) },
      onIncrease = { onUpdateRules(normalizedRules.copy(wakeTargetHour = normalizedRules.wakeTargetHour + 1)) },
    )
    SettingsRuleControlRow(
      title = "Sleep start",
      value = normalizedRules.sleepProtectionStartHour.hourLabel(),
      supporting = "Ideal sleep window begins",
      icon = Icons.Rounded.Bedtime,
      onDecrease = { onUpdateRules(normalizedRules.copy(sleepProtectionStartHour = normalizedRules.sleepProtectionStartHour - 1)) },
      onIncrease = { onUpdateRules(normalizedRules.copy(sleepProtectionStartHour = normalizedRules.sleepProtectionStartHour + 1)) },
    )
    SettingsRuleControlRow(
      title = "Sleep end",
      value = normalizedRules.sleepProtectionEndHour.hourLabel(),
      supporting = "Ideal sleep window ends",
      icon = Icons.Rounded.LightMode,
      onDecrease = { onUpdateRules(normalizedRules.copy(sleepProtectionEndHour = normalizedRules.sleepProtectionEndHour - 1)) },
      onIncrease = { onUpdateRules(normalizedRules.copy(sleepProtectionEndHour = normalizedRules.sleepProtectionEndHour + 1)) },
    )
    SettingsRuleControlRow(
      title = "Sleep rate",
      value = "${normalizedRules.sleepProtectionMultiplier.formatOne()}x",
      supporting = "Cost multiplier during sleep protection",
      icon = Icons.Rounded.Timer,
      onDecrease = { onUpdateRules(normalizedRules.copy(sleepProtectionMultiplier = normalizedRules.sleepProtectionMultiplier - 0.5)) },
      onIncrease = { onUpdateRules(normalizedRules.copy(sleepProtectionMultiplier = normalizedRules.sleepProtectionMultiplier + 0.5)) },
    )
    SettingsRuleActionRow(
      title = "Free apps",
      value = normalizedRules.phoneUsageChargeFreePackages.size.toString(),
      supporting = "No cost in phone correction",
      icon = Icons.Rounded.Apps,
      actionLabel = "Choose",
      onClick = onManageChargeFreeApps,
    )
  }
}

@Composable
internal fun SettingsRemindersSection(
  rules: FocusWellRules,
  pushRegistrationState: PushRegistrationUiState,
  notificationPermissionGranted: Boolean,
  onUpdateRules: (FocusWellRules) -> Unit,
  onEnablePush: () -> Unit,
  onDisablePush: () -> Unit,
) {
  val normalizedRules = rules.normalized()
  CalmPanel {
    Text("Reminders", style = MaterialTheme.typography.titleLarge)
    SettingsPushRegistrationRow(
      pushRegistrationState = pushRegistrationState,
      notificationPermissionGranted = notificationPermissionGranted,
      onEnablePush = onEnablePush,
      onDisablePush = onDisablePush,
    )
    SettingsSwitchRow(
      title = "Long reminders",
      supporting = "Notify at 1 hour, 3 hours, and 5 hours during focus or leisure",
      icon = Icons.Rounded.Timer,
      checked = normalizedRules.longSessionRemindersEnabled,
      onCheckedChange = { onUpdateRules(normalizedRules.copy(longSessionRemindersEnabled = it)) },
    )
  }
}

@Composable
internal fun SettingsDataSection(
  cloudSyncState: CloudSyncUiState,
  onStartCloudSync: () -> Unit,
  onSignOutCloudSync: () -> Unit,
  onExport: () -> Unit,
  onImport: () -> Unit,
  onClearAllData: () -> Unit,
) {
  CalmPanel {
    Text("Data", style = MaterialTheme.typography.titleLarge)
    SettingsDataActionRow(
      title = "Cloud backup",
      supporting =
        when {
          cloudSyncState.syncing -> "Checking FocusWell cloud"
          cloudSyncState.userLogin != null -> "Signed in as ${cloudSyncState.userLogin}"
          else -> "Sign in with GitHub to use FocusWell cloud"
        },
      icon = Icons.Rounded.Cloud,
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
      supporting = "Save a complete JSON backup",
      icon = Icons.Rounded.Download,
      actionLabel = "Export",
      onClick = onExport,
    )
    SettingsDataActionRow(
      title = "Import",
      supporting = "Restore from a JSON export",
      icon = Icons.Rounded.Upload,
      actionLabel = "Import",
      onClick = onImport,
    )
    SettingsDataActionRow(
      title = "Reset",
      supporting = "Remove records, reserve history, trackers, and settings",
      icon = Icons.Rounded.Delete,
      actionLabel = "Clear",
      onClick = onClearAllData,
      destructive = true,
    )
  }
}

@Composable
internal fun SettingsUpdateSection(
  updateState: AppUpdateUiState,
  onCheckUpdate: () -> Unit,
  onDownloadUpdate: () -> Unit,
  onInstallUpdate: () -> Unit,
  onOpenReleasePage: () -> Unit,
) {
  CalmPanel {
    Text("Update", style = MaterialTheme.typography.titleLarge)
    SettingsUpdateRow(
      updateState = updateState,
      onCheckUpdate = onCheckUpdate,
      onDownloadUpdate = onDownloadUpdate,
      onInstallUpdate = onInstallUpdate,
      onOpenReleasePage = onOpenReleasePage,
    )
  }
}
