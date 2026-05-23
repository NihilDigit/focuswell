package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.FocusWellUiState

internal interface FocusWellStore {
  fun loadState(): FocusWellUiState?

  fun persistChange(previous: FocusWellUiState?, next: FocusWellUiState)

  fun clear()
}
