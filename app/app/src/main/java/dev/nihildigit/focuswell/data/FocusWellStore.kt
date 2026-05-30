package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.FocusWellUiState

internal interface FocusWellStore {
  suspend fun loadState(): FocusWellUiState?

  suspend fun persistChange(previous: FocusWellUiState?, next: FocusWellUiState)

  suspend fun clear()
}
