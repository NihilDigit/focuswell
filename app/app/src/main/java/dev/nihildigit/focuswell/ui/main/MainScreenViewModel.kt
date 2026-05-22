package dev.nihildigit.focuswell.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nihildigit.focuswell.data.FocusWellRepository
import dev.nihildigit.focuswell.domain.Destination
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.reminders.ReminderClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = FocusWellRepository(application)
  private val reminders = ReminderClient(application)
  private val destination = MutableStateFlow(Destination.Today)
  private val exportText = MutableStateFlow<String?>(null)
  private val importError = MutableStateFlow<String?>(null)
  private var focusReminderSessionId: String? = null
  private var leisureReminderSessionId: String? = null

  init {
    viewModelScope.launch {
      runCatching { reminders.refreshFcmRegistration() }
    }
  }

  val uiState: StateFlow<FocusWellUiState> =
    combine(repository.state, destination, exportText, importError) {
        state,
        selectedDestination,
        export,
        importError ->
        state.copy(
          destination = selectedDestination,
          exportText = export,
          importError = importError,
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusWellUiState())

  fun selectDestination(destination: Destination) {
    this.destination.value = destination
  }

  fun toggleTracker(id: String) = repository.toggleTracker(id)

  fun setWakeTime(value: String) = repository.setWakeTime(value)

  fun startFocus(task: String, type: SessionType, tagId: String) {
    repository.startFocus(task, type, tagId)
    val sessionId = "focus-${System.currentTimeMillis()}"
    focusReminderSessionId = sessionId
    viewModelScope.launch {
      runCatching { reminders.scheduleFocusStaleReminder(sessionId, revision = 1) }
    }
  }

  fun pauseFocus() = repository.pauseFocus()

  fun resumeFocus() = repository.resumeFocus()

  fun endFocus(result: String) {
    repository.endFocus(result)
    focusReminderSessionId?.let { sessionId ->
      viewModelScope.launch { runCatching { reminders.cancelSession(sessionId) } }
    }
    focusReminderSessionId = null
  }

  fun startLeisure() {
    val reserveMinutes = uiState.value.reserveMinutes
    repository.startLeisure()
    val sessionId = "leisure-${System.currentTimeMillis()}"
    leisureReminderSessionId = sessionId
    viewModelScope.launch {
      runCatching { reminders.scheduleLeisureReminders(sessionId, revision = 1, reserveMinutes = reserveMinutes) }
    }
  }

  fun endLeisure() {
    repository.endLeisure()
    leisureReminderSessionId?.let { sessionId ->
      viewModelScope.launch { runCatching { reminders.cancelSession(sessionId) } }
    }
    leisureReminderSessionId = null
  }

  fun startWindDown() = repository.startWindDown()

  fun endWindDown() = repository.endWindDown()

  fun endDepleted() = repository.endDepleted()

  fun exportJson() {
    exportText.value = repository.exportJson()
  }

  fun dismissExport() {
    exportText.value = null
  }

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
    exportText.value = null
  }

  fun deleteFocusRecord(id: String) = repository.deleteFocusRecord(id)

  fun updateFocusRecord(id: String, result: String, activeMinutes: Double) =
    repository.updateFocusRecord(id, result, activeMinutes)

  fun deleteLeisureRecord(id: String) = repository.deleteLeisureRecord(id)

  fun addTag(name: String, multiplier: Double) = repository.addTag(name, multiplier)

  fun archiveTag(id: String) = repository.archiveTag(id)

  fun addBooleanTracker(label: String) = repository.addBooleanTracker(label)

  fun addRuleTracker(label: String, tagName: String, targetMinutes: Double) =
    repository.addRuleTracker(label, tagName, targetMinutes)

  fun archiveTracker(id: String) = repository.archiveTracker(id)
}
