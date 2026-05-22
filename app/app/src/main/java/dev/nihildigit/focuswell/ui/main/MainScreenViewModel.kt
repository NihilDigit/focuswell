package dev.nihildigit.focuswell.ui.main

import androidx.lifecycle.ViewModel
import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.Destination
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.TimeAccounting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.time.Duration
import java.time.Instant

class MainScreenViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(FocusWellUiState())
  val uiState: StateFlow<FocusWellUiState> = _uiState

  fun selectDestination(destination: Destination) {
    _uiState.update { it.copy(destination = destination) }
  }

  fun toggleTracker(id: String) {
    _uiState.update { state ->
      state.copy(
        trackers =
          state.trackers.map {
            if (it.id == id && it.progressLabel == null) it.copy(completed = !it.completed) else it
          }
      )
    }
  }

  fun startFocus(task: String, type: SessionType, tagId: String) {
    val trimmed = task.trim()
    if (trimmed.isEmpty()) return
    _uiState.update { state ->
      val tag = state.tags.firstOrNull { it.id == tagId } ?: state.tags.first()
      state.copy(
        activeMode =
          ActiveMode.Focus(
            task = trimmed,
            type = type,
            tag = tag,
            startedAt = Instant.now(),
          )
      )
    }
  }

  fun pauseFocus() {
    _uiState.update { state ->
      val focus = state.activeMode as? ActiveMode.Focus ?: return@update state
      state.copy(activeMode = focus.copy(paused = true))
    }
  }

  fun resumeFocus() {
    _uiState.update { state ->
      val focus = state.activeMode as? ActiveMode.Focus ?: return@update state
      state.copy(activeMode = focus.copy(paused = false))
    }
  }

  fun endFocus(result: String) {
    _uiState.update { state ->
      val focus = state.activeMode as? ActiveMode.Focus ?: return@update state
      val now = Instant.now()
      val earned =
        TimeAccounting.focusEarnedMinutes(
          activeDuration = Duration.between(focus.startedAt, now),
          type = focus.type,
          tagMultiplier = focus.tag.multiplier,
        )
      val entry =
        LedgerEntry(
          id = "focus-${now.toEpochMilli()}",
          title = "Focus · ${focus.type.label} ${focus.tag.name}",
          deltaMinutes = earned,
          createdAt = now,
        )
      state.copy(
        reserveMinutes = state.reserveMinutes + earned,
        activeMode = ActiveMode.None,
        ledger = listOf(entry) + state.ledger,
      )
    }
  }

  fun startLeisure() {
    _uiState.update { state ->
      if (state.reserveMinutes <= 0.0) state
      else state.copy(activeMode = ActiveMode.Leisure(startedAt = Instant.now()))
    }
  }

  fun endLeisure() {
    _uiState.update { state ->
      val leisure = state.activeMode as? ActiveMode.Leisure ?: return@update state
      val now = Instant.now()
      val cost =
        TimeAccounting.leisureCostMinutes(startedAt = leisure.startedAt, endedAt = now)
          .coerceAtMost(state.reserveMinutes)
      val entry =
        LedgerEntry(
          id = "leisure-${now.toEpochMilli()}",
          title = "Leisure",
          deltaMinutes = -cost,
          createdAt = now,
        )
      state.copy(
        reserveMinutes = state.reserveMinutes - cost,
        activeMode = ActiveMode.None,
        ledger = listOf(entry) + state.ledger,
      )
    }
  }

  fun startWindDown() {
    _uiState.update { it.copy(activeMode = ActiveMode.WindDown(startedAt = Instant.now())) }
  }

  fun endWindDown() {
    _uiState.update { it.copy(activeMode = ActiveMode.None) }
  }
}
