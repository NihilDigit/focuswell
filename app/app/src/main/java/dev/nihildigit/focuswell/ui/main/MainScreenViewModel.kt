package dev.nihildigit.focuswell.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nihildigit.focuswell.data.FocusWellRepository
import dev.nihildigit.focuswell.domain.Destination
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.SessionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = FocusWellRepository(application)
  private val destination = MutableStateFlow(Destination.Today)
  private val exportText = MutableStateFlow<String?>(null)
  private val importError = MutableStateFlow<String?>(null)

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

  fun startFocus(task: String, type: SessionType, tagId: String) =
    repository.startFocus(task, type, tagId)

  fun pauseFocus() = repository.pauseFocus()

  fun resumeFocus() = repository.resumeFocus()

  fun endFocus(result: String) = repository.endFocus(result)

  fun startLeisure() = repository.startLeisure()

  fun endLeisure() = repository.endLeisure()

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
