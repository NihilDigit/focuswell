package dev.nihildigit.focuswell.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nihildigit.focuswell.data.FocusWellRepository
import dev.nihildigit.focuswell.domain.Destination
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.reminders.ReminderClient
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = FocusWellRepository(application)
  private val reminders = ReminderClient(application)
  private val destination = MutableStateFlow(Destination.Today)
  private val importError = MutableStateFlow<String?>(null)

  init {
    viewModelScope.launch {
      runCatching { reminders.refreshFcmRegistration() }
        .onSuccess { Log.i("FocusWellPush", "Refreshed FCM registration") }
        .onFailure { Log.e("FocusWellPush", "Failed to refresh FCM registration", it) }
    }
  }

  val uiState: StateFlow<FocusWellUiState> =
    combine(repository.state, destination, importError) {
        state,
        selectedDestination,
        importError ->
        state.copy(
          destination = selectedDestination,
          importError = importError,
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusWellUiState())

  fun selectDestination(destination: Destination) {
    this.destination.value = destination
  }

  fun toggleTracker(id: String) = repository.toggleTracker(id)

  fun setWakeTime(value: String) = repository.setWakeTime(value)

  fun startFocus(task: String, type: SessionType, tagId: String?) {
    val focus = repository.startFocus(task, type, tagId) ?: return
    viewModelScope.launch {
      runCatching { reminders.scheduleFocusStaleReminder(focus.reminderSessionId, revision = focus.revision) }
        .onFailure { Log.e("FocusWellPush", "Failed to schedule focus reminder", it) }
    }
  }

  fun pauseFocus() = repository.pauseFocus()

  fun resumeFocus() = repository.resumeFocus()

  fun endFocus(result: String) {
    repository.endFocus(result)?.let { sessionId ->
      viewModelScope.launch {
        runCatching { reminders.cancelSession(sessionId) }
          .onFailure { Log.e("FocusWellPush", "Failed to cancel focus reminder", it) }
      }
    }
  }

  fun startLeisure() {
    val reserveMinutes = uiState.value.reserveMinutes
    val leisure = repository.startLeisure() ?: return
    viewModelScope.launch {
      runCatching {
        reminders.scheduleLeisureReminders(
          sessionId = leisure.reminderSessionId,
          revision = leisure.revision,
          reserveMinutes = reserveMinutes,
        )
      }.onFailure { Log.e("FocusWellPush", "Failed to schedule leisure reminders", it) }
    }
  }

  fun endLeisure() {
    repository.endLeisure()?.let { sessionId ->
      viewModelScope.launch {
        runCatching { reminders.cancelSession(sessionId) }
          .onFailure { Log.e("FocusWellPush", "Failed to cancel leisure reminder", it) }
      }
    }
  }

  fun startWindDown() = repository.startWindDown()

  fun endWindDown() = repository.endWindDown()

  fun endDepleted() = repository.endDepleted()

  fun exportJson(): String = repository.exportJson()

  fun importJson(raw: String) {
    importError.value =
      if (repository.importJson(raw)) {
        null
      } else {
        "Import failed. Check that the JSON came from FocusWell export."
      }
  }

  fun dismissImportError() {
    importError.value = null
  }

  fun clearAllData() {
    repository.clearAllData()
    reminders.rotateIdentity()
  }

  fun deleteFocusRecord(id: String) = repository.deleteFocusRecord(id)

  fun updateFocusRecord(id: String, result: String, activeMinutes: Double) =
    repository.updateFocusRecord(id, result, activeMinutes)

  fun deleteLeisureRecord(id: String) = repository.deleteLeisureRecord(id)

  fun addTag(name: String, multiplier: Double) = repository.addTag(name, multiplier)

  fun archiveTag(id: String) = repository.archiveTag(id)

  fun addBooleanTracker(label: String, rewardMinutes: Double) = repository.addBooleanTracker(label, rewardMinutes)

  fun addRuleTracker(label: String, tagName: String, targetMinutes: Double, rewardMinutes: Double) =
    repository.addRuleTracker(label, tagName, targetMinutes, rewardMinutes)

  fun archiveTracker(id: String) = repository.archiveTracker(id)

  fun updateTrackerReward(id: String, rewardMinutes: Double) = repository.updateTrackerReward(id, rewardMinutes)
}
