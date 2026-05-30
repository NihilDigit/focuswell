package dev.nihildigit.focuswell.ui.main

import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.reminders.PushRegistrationStatus

data class PushRegistrationUiState(
  val status: PushRegistrationStatus,
  val refreshing: Boolean = false,
)

internal fun PushRegistrationUiState.refreshStarted(): PushRegistrationUiState =
  copy(refreshing = true)

internal fun pushRegistrationSucceeded(status: PushRegistrationStatus): PushRegistrationUiState =
  PushRegistrationUiState(status = status)

internal fun PushRegistrationUiState.refreshFailed(error: Throwable): PushRegistrationUiState =
  PushRegistrationUiState(
    status = status.copy(lastError = error.message ?: "Registration failed"),
  )

internal fun pushDisableFailed(
  cachedStatus: PushRegistrationStatus,
  error: Throwable,
): PushRegistrationUiState =
  PushRegistrationUiState(
    status =
      cachedStatus.copy(
        enabled = false,
        hasFcmToken = false,
        lastError = error.message ?: "Could not disable push",
      ),
  )

internal fun activeReminderSessionId(activeMode: ActiveMode): String? =
  when (activeMode) {
    is ActiveMode.Focus -> activeMode.reminderSessionId
    is ActiveMode.Leisure -> activeMode.reminderSessionId
    else -> null
  }
