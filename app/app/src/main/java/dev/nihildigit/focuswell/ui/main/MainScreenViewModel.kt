package dev.nihildigit.focuswell.ui.main

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nihildigit.focuswell.data.FocusWellRepository
import dev.nihildigit.focuswell.domain.Destination
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.IdeaChecklistItem
import dev.nihildigit.focuswell.domain.IdeaQuadrant
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.domain.reserveLocked
import dev.nihildigit.focuswell.reminders.ReminderClient
import dev.nihildigit.focuswell.sync.CloudSnapshot
import dev.nihildigit.focuswell.sync.CloudSyncClient
import dev.nihildigit.focuswell.sync.CloudSyncSession
import dev.nihildigit.focuswell.updates.AppUpdateInstaller
import dev.nihildigit.focuswell.updates.AppUpdateUiState
import dev.nihildigit.focuswell.updates.GitHubReleaseClient
import dev.nihildigit.focuswell.usage.hasPhoneUsageSettlementContent
import dev.nihildigit.focuswell.usage.phoneUsageSegments
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = FocusWellRepository(application)
  private val reminders = ReminderClient(application)
  private val cloudSync = CloudSyncClient(application)
  private val updateClient = GitHubReleaseClient()
  private val updateCoordinator = AppUpdateCoordinator(updateClient, AppUpdateInstaller(application, updateClient))
  private val destination = MutableStateFlow(Destination.Today)
  private val importError = MutableStateFlow<String?>(null)
  val updateState: StateFlow<AppUpdateUiState> = updateCoordinator.state
  private val _cloudSyncState =
    MutableStateFlow(CloudSyncUiState(userLogin = cloudSync.cachedSession()?.user?.login))
  val cloudSyncState: StateFlow<CloudSyncUiState> = _cloudSyncState
  private val _morningCheckInState = MutableStateFlow(MorningCheckInUiState())
  val morningCheckInState: StateFlow<MorningCheckInUiState> = _morningCheckInState
  private val _phoneSettlementState = MutableStateFlow(MorningCheckInUiState())
  val phoneSettlementState: StateFlow<MorningCheckInUiState> = _phoneSettlementState
  private val _phoneSettlementAvailable = MutableStateFlow(false)
  val phoneSettlementAvailable: StateFlow<Boolean> = _phoneSettlementAvailable
  private var phoneSettlementAvailabilityJob: Job? = null
  private val _pushRegistrationState =
    MutableStateFlow(PushRegistrationUiState(status = reminders.cachedRegistrationStatus()))
  val pushRegistrationState: StateFlow<PushRegistrationUiState> = _pushRegistrationState
  private val initialization = viewModelScope.launch { repository.initialize() }

  init {
    refreshPushRegistration(forceTokenRefresh = false)
  }

  val uiState: StateFlow<FocusWellUiState> =
    combine(repository.state, destination, importError) {
        state,
        selectedDestination,
        importError ->
        state.copy(
          destination = if (state.reserveLocked) Destination.Today else selectedDestination,
          importError = importError,
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusWellUiState())

  fun selectDestination(destination: Destination) {
    this.destination.value = destination
  }

  fun refreshPhoneUsageSettlementAvailability() {
    phoneSettlementAvailabilityJob?.cancel()
    phoneSettlementAvailabilityJob =
      viewModelScope.launch {
        val state = repository.state.value
        if (state.dailyDate.isBlank() || state.reserveLocked) {
          _phoneSettlementAvailable.value = false
          return@launch
        }
        val checkedAt = Instant.now()
        val available =
          withContext(Dispatchers.Default) {
            val rules = state.rules.normalized()
            val window = phoneUsageSettlementWindow(state, checkedAt)
            hasPhoneUsageSettlementContent(
              context = getApplication(),
              startedAt = window.startedAt,
              endedAt = window.endedAt,
              focusRecords = state.focusRecords,
              leisureRecords = state.leisureRecords,
              rules = rules,
              zone = TimeAccounting.focusWellTimeZone,
            )
          }
        _phoneSettlementAvailable.value = available
      }
  }

  fun toggleTracker(id: String) {
    viewModelScope.launch { repository.toggleTracker(id) }
  }

  fun startFocus(task: String, type: SessionType, tagId: String?) {
    viewModelScope.launch {
      val rules = uiState.value.rules
      val focus = repository.startFocus(task, type, tagId) ?: return@launch
      runCatching { reminders.scheduleFocusReminders(focus.reminderSessionId, revision = focus.revision, rules = rules) }
        .onFailure { Log.e("FocusWellPush", "Failed to schedule focus reminder", it) }
    }
  }

  fun pauseFocus() {
    viewModelScope.launch { repository.pauseFocus() }
  }

  fun resumeFocus() {
    viewModelScope.launch { repository.resumeFocus() }
  }

  fun endFocus(result: String, correctionMinutes: Double) {
    viewModelScope.launch {
      repository.endFocus(result, correctionMinutes)?.let { sessionId ->
        runCatching { reminders.cancelSession(sessionId) }
          .onFailure { Log.e("FocusWellPush", "Failed to cancel focus reminder", it) }
      }
    }
  }

  fun startLeisure() {
    viewModelScope.launch {
      val reserveMinutes = uiState.value.reserveMinutes
      val rules = uiState.value.rules
      val leisure = repository.startLeisure() ?: return@launch
      runCatching {
        reminders.scheduleLeisureReminders(
          sessionId = leisure.reminderSessionId,
          revision = leisure.revision,
          reserveMinutes = reserveMinutes,
          rules = rules,
        )
      }.onFailure { Log.e("FocusWellPush", "Failed to schedule leisure reminders", it) }
    }
  }

  fun endLeisure() {
    viewModelScope.launch {
      repository.endLeisure()?.let { sessionId ->
        runCatching { reminders.cancelSession(sessionId) }
          .onFailure { Log.e("FocusWellPush", "Failed to cancel leisure reminder", it) }
      }
    }
  }

  fun endDepleted() {
    viewModelScope.launch { repository.endDepleted() }
  }

  fun exportJson(onExported: (String) -> Unit) {
    viewModelScope.launch {
      initialization.join()
      onExported(repository.exportJson())
    }
  }

  fun importJson(raw: String) {
    viewModelScope.launch {
      importError.value =
        if (repository.importJson(raw)) {
          null
        } else {
          "Import failed. Check that the JSON came from FocusWell export."
        }
    }
  }

  fun startCloudSync() {
    if (_cloudSyncState.value.syncing) return
    val session = cloudSync.cachedSession()
    if (session == null) {
      runCatching {
        getApplication<Application>().startActivity(
          Intent(Intent.ACTION_VIEW, cloudSync.beginOAuthUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        _cloudSyncState.value = _cloudSyncState.value.cloudSignInPrompted()
      }.onFailure { error ->
        _cloudSyncState.value = _cloudSyncState.value.cloudSignInOpenFailed(error)
      }
      return
    }
    checkCloudSnapshot(session)
  }

  fun handleCloudSyncRedirect(uri: Uri) {
    val code =
      when (val redirect = uri.toCloudSyncRedirect()) {
        is CloudSyncRedirect.Code -> {
          if (!cloudSync.consumePendingOAuthState(redirect.state)) {
            _cloudSyncState.value = _cloudSyncState.value.cloudOAuthStateMismatch()
            return
          }
          redirect.value
        }
        is CloudSyncRedirect.Rejected -> {
          if (!cloudSync.consumePendingOAuthState(redirect.state)) {
            _cloudSyncState.value = _cloudSyncState.value.cloudOAuthStateMismatch()
            return
          }
          _cloudSyncState.value = _cloudSyncState.value.cloudOAuthRejected(redirect.error)
          return
        }
        CloudSyncRedirect.Ignored -> return
      }
    _cloudSyncState.value = _cloudSyncState.value.cloudOAuthStarted()
    viewModelScope.launch {
      runCatching { cloudSync.exchangeCode(code) }
        .onSuccess { session ->
          _cloudSyncState.value = cloudOAuthSucceeded(session.user.login)
          checkCloudSnapshot(session)
        }
        .onFailure { error ->
          _cloudSyncState.value = _cloudSyncState.value.cloudOAuthFailed(error)
        }
    }
  }

  fun chooseCloudSyncUpload() {
    val session = cloudSync.cachedSession() ?: return
    uploadLocalSnapshot(session)
  }

  fun chooseCloudSyncRestore() {
    val payload = _cloudSyncState.value.pendingDecision?.cloudPayload ?: return
    viewModelScope.launch {
      if (repository.importJson(payload, touchUpdatedAt = false)) {
        _cloudSyncState.value = _cloudSyncState.value.cloudRestoreSucceeded()
      } else {
        _cloudSyncState.value = _cloudSyncState.value.cloudRestoreFailed()
      }
    }
  }

  fun dismissCloudSyncDecision() {
    _cloudSyncState.value = _cloudSyncState.value.copy(pendingDecision = null)
  }

  fun dismissCloudSyncMessage() {
    _cloudSyncState.value = _cloudSyncState.value.copy(message = null, error = null)
  }

  fun signOutCloudSync() {
    cloudSync.signOut()
    _cloudSyncState.value = cloudSignedOut()
  }

  fun dismissImportError() {
    importError.value = null
  }

  fun clearAllData() {
    viewModelScope.launch {
      repository.clearAllData()
      reminders.rotateIdentity()
      _pushRegistrationState.value = PushRegistrationUiState(status = reminders.cachedRegistrationStatus())
    }
  }

  private fun checkCloudSnapshot(session: CloudSyncSession) {
    _cloudSyncState.value = _cloudSyncState.value.cloudCheckStarted()
    viewModelScope.launch {
      initialization.join()
      runCatching { cloudSync.getSnapshot(session) }
        .onSuccess { snapshot ->
          _cloudSyncState.value = _cloudSyncState.value.cloudSnapshotLoaded(session.user.login)
          if (snapshot == null) {
            uploadLocalSnapshot(session)
          } else {
            decideCloudSync(snapshot)
          }
        }
        .onFailure { error ->
          _cloudSyncState.value = _cloudSyncState.value.cloudCheckFailed(error)
        }
    }
  }

  private fun decideCloudSync(snapshot: CloudSnapshot) {
    val localUpdatedAt = repository.state.value.stateUpdatedAt
    _cloudSyncState.value = _cloudSyncState.value.withCloudDecision(localUpdatedAt = localUpdatedAt, snapshot = snapshot)
  }

  private fun uploadLocalSnapshot(session: CloudSyncSession) {
    _cloudSyncState.value = _cloudSyncState.value.cloudUploadStarted()
    viewModelScope.launch {
      initialization.join()
      val localState = repository.state.value
      val payload = cloudPayloadFromExport(repository.exportJson())
      runCatching { cloudSync.putSnapshot(session = session, updatedAtUtc = localState.stateUpdatedAt, payload = payload) }
        .onSuccess {
          _cloudSyncState.value = _cloudSyncState.value.cloudUploadSucceeded(session.user.login)
        }
        .onFailure { error ->
          _cloudSyncState.value = _cloudSyncState.value.cloudUploadFailed(error)
        }
    }
  }

  fun refreshPushRegistration(forceTokenRefresh: Boolean = true) {
    if (_pushRegistrationState.value.refreshing) return
    _pushRegistrationState.value = _pushRegistrationState.value.refreshStarted()
    viewModelScope.launch {
      runCatching { reminders.refreshFcmRegistration(forceTokenRefresh = forceTokenRefresh) }
        .onSuccess { status ->
          _pushRegistrationState.value = pushRegistrationSucceeded(status)
          if (status.hasFcmToken) {
            Log.i("FocusWellPush", "Refreshed FCM registration with token")
          } else {
            Log.w("FocusWellPush", "Refreshed reminder registration without FCM token: ${status.lastError ?: "unknown"}")
          }
        }
        .onFailure { error ->
          Log.e("FocusWellPush", "Failed to refresh FCM registration", error)
          _pushRegistrationState.value = _pushRegistrationState.value.refreshFailed(error)
        }
    }
  }

  fun disablePush() {
    if (_pushRegistrationState.value.refreshing) return
    _pushRegistrationState.value = _pushRegistrationState.value.refreshStarted()
    val activeSessionId = activeReminderSessionId(repository.state.value.activeMode)
    viewModelScope.launch {
      runCatching {
        activeSessionId?.let {
          runCatching { reminders.cancelSession(it) }
            .onFailure { error -> Log.e("FocusWellPush", "Failed to cancel active reminders while disabling push", error) }
        }
        reminders.disablePush()
      }.onSuccess { status ->
        _pushRegistrationState.value = pushRegistrationSucceeded(status)
      }.onFailure { error ->
        Log.e("FocusWellPush", "Failed to disable push", error)
        _pushRegistrationState.value = pushDisableFailed(reminders.cachedRegistrationStatus(), error)
      }
    }
  }

  fun checkForUpdate() {
    updateCoordinator.checkForUpdate(viewModelScope)
  }

  fun downloadUpdate() {
    updateCoordinator.downloadUpdate(viewModelScope)
  }

  fun installDownloadedUpdate() {
    updateCoordinator.installDownloadedUpdate()
  }

  fun openUpdateReleasePage() {
    updateCoordinator.openReleasePage()
  }

  fun deleteFocusRecord(id: String) {
    viewModelScope.launch { repository.deleteFocusRecord(id) }
  }

  fun updateFocusRecord(id: String, result: String, activeMinutes: Double) {
    viewModelScope.launch { repository.updateFocusRecord(id, result, activeMinutes) }
  }

  fun addManualAdjustment(title: String, deltaMinutes: Double, note: String?) {
    viewModelScope.launch { repository.addManualAdjustment(title, deltaMinutes, note) }
  }

  fun addManualFocusRecord(task: String, activeMinutes: Double, note: String?, type: SessionType, tagId: String?) {
    viewModelScope.launch { repository.addManualFocusRecord(task, activeMinutes, note, type, tagId) }
  }

  fun deleteLeisureRecord(id: String) {
    viewModelScope.launch { repository.deleteLeisureRecord(id) }
  }

  fun addIdea(text: String) {
    viewModelScope.launch { repository.addIdea(text) }
  }

  fun moveIdea(id: String, quadrant: IdeaQuadrant) {
    viewModelScope.launch { repository.moveIdea(id, quadrant) }
  }

  fun updateIdea(id: String, text: String, checklist: List<IdeaChecklistItem>) {
    viewModelScope.launch { repository.updateIdea(id, text, checklist) }
  }

  fun archiveIdea(id: String) {
    viewModelScope.launch { repository.archiveIdea(id) }
  }

  fun addTag(name: String, multiplier: Double) {
    viewModelScope.launch { repository.addTag(name, multiplier) }
  }

  fun archiveTag(id: String) {
    viewModelScope.launch { repository.archiveTag(id) }
  }

  fun updateTag(id: String, name: String, multiplier: Double) {
    viewModelScope.launch { repository.updateTag(id, name, multiplier) }
  }

  fun addBooleanTracker(label: String, rewardMinutes: Double) {
    viewModelScope.launch { repository.addBooleanTracker(label, rewardMinutes) }
  }

  fun addRuleTracker(label: String, tagName: String, targetMinutes: Double, rewardMinutes: Double) {
    viewModelScope.launch { repository.addRuleTracker(label, tagName, targetMinutes, rewardMinutes) }
  }

  fun archiveTracker(id: String) {
    viewModelScope.launch { repository.archiveTracker(id) }
  }

  fun updateManualTracker(id: String, label: String, rewardMinutes: Double) {
    viewModelScope.launch { repository.updateManualTracker(id, label, rewardMinutes) }
  }

  fun updateRuleTracker(id: String, label: String, tagName: String, targetMinutes: Double, rewardMinutes: Double) {
    viewModelScope.launch { repository.updateRuleTracker(id, label, tagName, targetMinutes, rewardMinutes) }
  }

  fun updateRules(rules: FocusWellRules) {
    viewModelScope.launch { repository.updateRules(rules) }
  }

  fun loadMorningCheckInIfNeeded() {
    val state = repository.state.value
    val today = state.dailyDate
    if (today.isBlank() || state.lastCheckInDailyDate == today) return
    val current = _morningCheckInState.value
    if (current.dailyDate == today && (current.loading || current.startedAt != null)) return
    val startedAt = Instant.now()
    _morningCheckInState.value = MorningCheckInUiState(dailyDate = today, startedAt = startedAt, loading = true)
    viewModelScope.launch {
      val segments =
        if (state.reserveLocked) {
          emptyList()
        } else {
          withContext(Dispatchers.Default) {
            val rules = state.rules.normalized()
            val window = morningCheckInUsageWindow(state, today)
            phoneUsageSegments(
              context = getApplication(),
              startedAt = window.startedAt,
              endedAt = window.endedAt,
              focusRecords = state.focusRecords,
              leisureRecords = state.leisureRecords,
              rules = rules,
              zone = TimeAccounting.focusWellTimeZone,
            )
          }
        }
      _morningCheckInState.value =
        MorningCheckInUiState(
          dailyDate = today,
          startedAt = startedAt,
          settledUntil = morningCheckInUsageWindow(state, today).settledUntil,
          loading = false,
          segments = segments,
        )
    }
  }

  fun completeMorningCheckIn(fairUseSegmentIds: Set<String>) {
    val checkIn = _morningCheckInState.value
    val startedAt = checkIn.startedAt ?: Instant.now()
    val phoneCost = billablePhoneCostMinutes(checkIn.segments, fairUseSegmentIds)
    viewModelScope.launch {
      repository.completeMorningCheckIn(
        checkInStartedAt = startedAt,
        phoneCostMinutes = phoneCost,
        reviewedSegmentCount = checkIn.segments.size,
        settledUntil = checkIn.settledUntil ?: startedAt,
      )
      _morningCheckInState.value = MorningCheckInUiState()
    }
  }

  fun startPhoneUsageSettlement() {
    val state = repository.state.value
    val current = _phoneSettlementState.value
    if (state.reserveLocked || current.loading || current.startedAt != null) return
    val startedAt = Instant.now()
    _phoneSettlementState.value =
      MorningCheckInUiState(
        dailyDate = state.dailyDate,
        startedAt = startedAt,
        settledUntil = startedAt,
        loading = true,
      )
    viewModelScope.launch {
      val segments =
        withContext(Dispatchers.Default) {
          val rules = state.rules.normalized()
          val window = phoneUsageSettlementWindow(state, startedAt)
          phoneUsageSegments(
            context = getApplication(),
            startedAt = window.startedAt,
            endedAt = window.endedAt,
            focusRecords = state.focusRecords,
            leisureRecords = state.leisureRecords,
            rules = rules,
            zone = TimeAccounting.focusWellTimeZone,
          )
        }
      _phoneSettlementState.value =
        MorningCheckInUiState(
          dailyDate = state.dailyDate,
          startedAt = startedAt,
          settledUntil = phoneUsageSettlementWindow(state, startedAt).settledUntil,
          loading = false,
          segments = segments,
        )
    }
  }

  fun cancelPhoneUsageSettlement() {
    _phoneSettlementState.value = MorningCheckInUiState()
  }

  fun completePhoneUsageSettlement(fairUseSegmentIds: Set<String>) {
    val settlement = _phoneSettlementState.value
    val startedAt = settlement.startedAt ?: Instant.now()
    val phoneCost = billablePhoneCostMinutes(settlement.segments, fairUseSegmentIds)
    viewModelScope.launch {
      repository.completePhoneUsageSettlement(
        settlementStartedAt = startedAt,
        phoneCostMinutes = phoneCost,
        reviewedSegmentCount = settlement.segments.size,
        settledUntil = settlement.settledUntil ?: startedAt,
      )
      _phoneSettlementState.value = MorningCheckInUiState()
      refreshPhoneUsageSettlementAvailability()
    }
  }
}
