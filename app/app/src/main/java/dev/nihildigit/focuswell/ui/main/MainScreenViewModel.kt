package dev.nihildigit.focuswell.ui.main

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nihildigit.focuswell.BuildConfig
import dev.nihildigit.focuswell.data.FocusWellRepository
import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.Destination
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.IdeaChecklistItem
import dev.nihildigit.focuswell.domain.IdeaQuadrant
import dev.nihildigit.focuswell.domain.PhoneUsageSegment
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.reminders.PushRegistrationStatus
import dev.nihildigit.focuswell.reminders.ReminderClient
import dev.nihildigit.focuswell.sync.CloudSnapshot
import dev.nihildigit.focuswell.sync.CloudSnapshotMetadata
import dev.nihildigit.focuswell.sync.CloudSyncClient
import dev.nihildigit.focuswell.sync.CloudSyncSession
import dev.nihildigit.focuswell.updates.AppUpdateInstaller
import dev.nihildigit.focuswell.updates.AppUpdateUiState
import dev.nihildigit.focuswell.updates.GitHubReleaseClient
import dev.nihildigit.focuswell.updates.selectUpdateAsset
import dev.nihildigit.focuswell.usage.phoneUsageSegments
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import org.json.JSONObject

data class PushRegistrationUiState(
  val status: PushRegistrationStatus,
  val refreshing: Boolean = false,
)

data class MorningCheckInUiState(
  val dailyDate: String? = null,
  val startedAt: Instant? = null,
  val loading: Boolean = false,
  val segments: List<PhoneUsageSegment> = emptyList(),
)

enum class CloudSyncDecisionKind {
  Upload,
  Restore,
  Choose,
}

data class CloudSyncDecision(
  val kind: CloudSyncDecisionKind,
  val localUpdatedAt: Instant,
  val cloudMetadata: CloudSnapshotMetadata,
  val cloudPayload: String?,
)

data class CloudSyncUiState(
  val userLogin: String? = null,
  val syncing: Boolean = false,
  val message: String? = null,
  val error: String? = null,
  val pendingDecision: CloudSyncDecision? = null,
)

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = FocusWellRepository(application)
  private val reminders = ReminderClient(application)
  private val cloudSync = CloudSyncClient(application)
  private val updateClient = GitHubReleaseClient()
  private val updateInstaller = AppUpdateInstaller(application, updateClient)
  private val destination = MutableStateFlow(Destination.Today)
  private val importError = MutableStateFlow<String?>(null)
  private val _updateState = MutableStateFlow(AppUpdateUiState())
  val updateState: StateFlow<AppUpdateUiState> = _updateState
  private val _cloudSyncState =
    MutableStateFlow(CloudSyncUiState(userLogin = cloudSync.cachedSession()?.user?.login))
  val cloudSyncState: StateFlow<CloudSyncUiState> = _cloudSyncState
  private val _morningCheckInState = MutableStateFlow(MorningCheckInUiState())
  val morningCheckInState: StateFlow<MorningCheckInUiState> = _morningCheckInState
  private val _pushRegistrationState =
    MutableStateFlow(PushRegistrationUiState(status = reminders.cachedRegistrationStatus()))
  val pushRegistrationState: StateFlow<PushRegistrationUiState> = _pushRegistrationState

  init {
    refreshPushRegistration(forceTokenRefresh = false)
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

  fun startFocus(task: String, type: SessionType, tagId: String?) {
    val rules = uiState.value.rules
    val focus = repository.startFocus(task, type, tagId) ?: return
    viewModelScope.launch {
      runCatching { reminders.scheduleFocusReminders(focus.reminderSessionId, revision = focus.revision, rules = rules) }
        .onFailure { Log.e("FocusWellPush", "Failed to schedule focus reminder", it) }
    }
  }

  fun pauseFocus() = repository.pauseFocus()

  fun resumeFocus() = repository.resumeFocus()

  fun endFocus(result: String, correctionMinutes: Double) {
    repository.endFocus(result, correctionMinutes)?.let { sessionId ->
      viewModelScope.launch {
        runCatching { reminders.cancelSession(sessionId) }
          .onFailure { Log.e("FocusWellPush", "Failed to cancel focus reminder", it) }
      }
    }
  }

  fun startLeisure() {
    val reserveMinutes = uiState.value.reserveMinutes
    val rules = uiState.value.rules
    val leisure = repository.startLeisure() ?: return
    viewModelScope.launch {
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
    repository.endLeisure()?.let { sessionId ->
      viewModelScope.launch {
        runCatching { reminders.cancelSession(sessionId) }
          .onFailure { Log.e("FocusWellPush", "Failed to cancel leisure reminder", it) }
      }
    }
  }

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

  fun startCloudSync() {
    if (_cloudSyncState.value.syncing) return
    val session = cloudSync.cachedSession()
    if (session == null) {
      runCatching {
        getApplication<Application>().startActivity(
          Intent(Intent.ACTION_VIEW, cloudSync.authUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        _cloudSyncState.value = _cloudSyncState.value.copy(message = "Finish GitHub sign in in the browser.", error = null)
      }.onFailure { error ->
        _cloudSyncState.value = _cloudSyncState.value.copy(error = error.message ?: "Could not open GitHub sign in.")
      }
      return
    }
    checkCloudSnapshot(session)
  }

  fun handleCloudSyncRedirect(uri: Uri) {
    if (uri.scheme != "focuswell" || uri.host != "sync" || uri.path != "/oauth") return
    val error = uri.getQueryParameter("error")
    if (error != null) {
      _cloudSyncState.value = _cloudSyncState.value.copy(error = "GitHub sign in failed: $error")
      return
    }
    val code = uri.getQueryParameter("code") ?: return
    _cloudSyncState.value = _cloudSyncState.value.copy(syncing = true, message = "Finishing GitHub sign in.", error = null)
    viewModelScope.launch {
      runCatching { cloudSync.exchangeCode(code) }
        .onSuccess { session ->
          _cloudSyncState.value = CloudSyncUiState(userLogin = session.user.login, syncing = false, message = "Signed in as ${session.user.login}.")
          checkCloudSnapshot(session)
        }
        .onFailure { error ->
          _cloudSyncState.value =
            _cloudSyncState.value.copy(syncing = false, error = error.message ?: "GitHub sign in failed.")
        }
    }
  }

  fun chooseCloudSyncUpload() {
    val session = cloudSync.cachedSession() ?: return
    uploadLocalSnapshot(session)
  }

  fun chooseCloudSyncRestore() {
    val payload = _cloudSyncState.value.pendingDecision?.cloudPayload ?: return
    if (repository.importJson(payload, touchUpdatedAt = false)) {
      _cloudSyncState.value =
        _cloudSyncState.value.copy(
          pendingDecision = null,
          message = "Restored cloud backup to this device.",
          error = null,
        )
    } else {
      _cloudSyncState.value =
        _cloudSyncState.value.copy(
          pendingDecision = null,
          error = "Cloud backup could not be restored.",
        )
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
    _cloudSyncState.value = CloudSyncUiState(message = "Signed out of cloud sync.")
  }

  fun dismissImportError() {
    importError.value = null
  }

  fun clearAllData() {
    repository.clearAllData()
    reminders.rotateIdentity()
    _pushRegistrationState.value = PushRegistrationUiState(status = reminders.cachedRegistrationStatus())
  }

  private fun checkCloudSnapshot(session: CloudSyncSession) {
    _cloudSyncState.value = _cloudSyncState.value.copy(syncing = true, error = null, pendingDecision = null)
    viewModelScope.launch {
      runCatching { cloudSync.getSnapshot(session) }
        .onSuccess { snapshot ->
          _cloudSyncState.value = _cloudSyncState.value.copy(syncing = false, userLogin = session.user.login)
          if (snapshot == null) {
            uploadLocalSnapshot(session)
          } else {
            decideCloudSync(snapshot)
          }
        }
        .onFailure { error ->
          _cloudSyncState.value =
            _cloudSyncState.value.copy(syncing = false, error = error.message ?: "Cloud sync failed.")
        }
    }
  }

  private fun decideCloudSync(snapshot: CloudSnapshot) {
    val localUpdatedAt = repository.state.value.stateUpdatedAt
    val cloudUpdatedAt = snapshot.metadata.updatedAtUtc
    _cloudSyncState.value =
      when {
        localUpdatedAt.isAfter(cloudUpdatedAt) ->
          _cloudSyncState.value.copy(
            pendingDecision =
              CloudSyncDecision(
                kind = CloudSyncDecisionKind.Upload,
                localUpdatedAt = localUpdatedAt,
                cloudMetadata = snapshot.metadata,
                cloudPayload = null,
              ),
            message = null,
            error = null,
          )
        cloudUpdatedAt.isAfter(localUpdatedAt) ->
          _cloudSyncState.value.copy(
            pendingDecision =
              CloudSyncDecision(
                kind = CloudSyncDecisionKind.Restore,
                localUpdatedAt = localUpdatedAt,
                cloudMetadata = snapshot.metadata,
                cloudPayload = snapshot.payload.toString(),
              ),
            message = null,
            error = null,
          )
        else ->
          _cloudSyncState.value.copy(message = "Local and cloud backups are already in sync.", error = null)
      }
  }

  private fun uploadLocalSnapshot(session: CloudSyncSession) {
    _cloudSyncState.value = _cloudSyncState.value.copy(syncing = true, pendingDecision = null, error = null)
    val localState = repository.state.value
    val payload = JSONObject(repository.exportJson())
    viewModelScope.launch {
      runCatching { cloudSync.putSnapshot(session = session, updatedAtUtc = localState.stateUpdatedAt, payload = payload) }
        .onSuccess {
          _cloudSyncState.value =
            _cloudSyncState.value.copy(
              syncing = false,
              userLogin = session.user.login,
              message = "Uploaded local backup to cloud.",
              error = null,
            )
        }
        .onFailure { error ->
          _cloudSyncState.value =
            _cloudSyncState.value.copy(syncing = false, error = error.message ?: "Cloud upload failed.")
        }
    }
  }

  fun refreshPushRegistration(forceTokenRefresh: Boolean = true) {
    if (_pushRegistrationState.value.refreshing) return
    _pushRegistrationState.value = _pushRegistrationState.value.copy(refreshing = true)
    viewModelScope.launch {
      runCatching { reminders.refreshFcmRegistration(forceTokenRefresh = forceTokenRefresh) }
        .onSuccess { status ->
          _pushRegistrationState.value = PushRegistrationUiState(status = status)
          if (status.hasFcmToken) {
            Log.i("FocusWellPush", "Refreshed FCM registration with token")
          } else {
            Log.w("FocusWellPush", "Refreshed reminder registration without FCM token: ${status.lastError ?: "unknown"}")
          }
        }
        .onFailure { error ->
          Log.e("FocusWellPush", "Failed to refresh FCM registration", error)
          _pushRegistrationState.value =
            PushRegistrationUiState(
              status = _pushRegistrationState.value.status.copy(lastError = error.message ?: "Registration failed"),
            )
        }
    }
  }

  fun disablePush() {
    if (_pushRegistrationState.value.refreshing) return
    _pushRegistrationState.value = _pushRegistrationState.value.copy(refreshing = true)
    val activeSessionId =
      when (val active = repository.state.value.activeMode) {
        is ActiveMode.Focus -> active.reminderSessionId
        is ActiveMode.Leisure -> active.reminderSessionId
        else -> null
      }
    viewModelScope.launch {
      runCatching {
        activeSessionId?.let {
          runCatching { reminders.cancelSession(it) }
            .onFailure { error -> Log.e("FocusWellPush", "Failed to cancel active reminders while disabling push", error) }
        }
        reminders.disablePush()
      }.onSuccess { status ->
        _pushRegistrationState.value = PushRegistrationUiState(status = status)
      }.onFailure { error ->
        Log.e("FocusWellPush", "Failed to disable push", error)
        _pushRegistrationState.value =
          PushRegistrationUiState(
            status =
              reminders.cachedRegistrationStatus().copy(
                enabled = false,
                hasFcmToken = false,
                lastError = error.message ?: "Could not disable push",
              ),
          )
      }
    }
  }

  fun checkForUpdate() {
    if (_updateState.value.checking || _updateState.value.downloading) return
    _updateState.value = _updateState.value.copy(checking = true, message = null, error = null)
    viewModelScope.launch {
      runCatching {
        withContext(Dispatchers.IO) { updateClient.fetchLatestRelease() }
      }.onSuccess { release ->
        val selection =
          if (release.versionCode > BuildConfig.VERSION_CODE) {
            selectUpdateAsset(release, Build.SUPPORTED_ABIS.toList())
          } else {
            null
          }
        _updateState.value =
          AppUpdateUiState(
            latestRelease = release,
            selection = selection,
            message =
              when {
                release.versionCode <= BuildConfig.VERSION_CODE -> "FocusWell is up to date."
                selection == null -> "Update found, but no APK matches this device."
                else -> "FocusWell ${release.tagName} is available."
              },
          )
      }.onFailure { error ->
        _updateState.value = AppUpdateUiState(error = error.message ?: "Update check failed.")
      }
    }
  }

  fun downloadUpdate() {
    val selection = _updateState.value.selection ?: return
    if (_updateState.value.downloading) return
    _updateState.value = _updateState.value.copy(downloading = true, progress = 0, message = "Downloading update.", error = null)
    viewModelScope.launch {
      runCatching {
        updateInstaller.downloadAndVerify(selection) { progress ->
          _updateState.value = _updateState.value.copy(progress = progress)
        }
      }.onSuccess { apk ->
        _updateState.value =
          _updateState.value.copy(
            downloading = false,
            progress = 100,
            downloadedApk = apk,
            message = "Update downloaded. Install when ready.",
            error = null,
          )
      }.onFailure { error ->
        _updateState.value =
          _updateState.value.copy(
            downloading = false,
            progress = null,
            downloadedApk = null,
            error = error.message ?: "Download failed.",
          )
      }
    }
  }

  fun installDownloadedUpdate() {
    val apk = _updateState.value.downloadedApk ?: return
    runCatching { updateInstaller.install(apk) }
      .onFailure { error ->
        _updateState.value = _updateState.value.copy(error = error.message ?: "Could not open installer.")
      }
  }

  fun openUpdateReleasePage() {
    val release = _updateState.value.latestRelease ?: return
    runCatching { updateInstaller.openReleasePage(release) }
      .onFailure { error ->
        _updateState.value = _updateState.value.copy(error = error.message ?: "Could not open release page.")
      }
  }

  fun deleteFocusRecord(id: String) = repository.deleteFocusRecord(id)

  fun updateFocusRecord(id: String, result: String, activeMinutes: Double) =
    repository.updateFocusRecord(id, result, activeMinutes)

  fun deleteLeisureRecord(id: String) = repository.deleteLeisureRecord(id)

  fun addIdea(text: String) = repository.addIdea(text)

  fun moveIdea(id: String, quadrant: IdeaQuadrant) = repository.moveIdea(id, quadrant)

  fun updateIdea(id: String, text: String, checklist: List<IdeaChecklistItem>) = repository.updateIdea(id, text, checklist)

  fun archiveIdea(id: String) = repository.archiveIdea(id)

  fun addTag(name: String, multiplier: Double) = repository.addTag(name, multiplier)

  fun archiveTag(id: String) = repository.archiveTag(id)

  fun updateTag(id: String, name: String, multiplier: Double) = repository.updateTag(id, name, multiplier)

  fun addBooleanTracker(label: String, rewardMinutes: Double) = repository.addBooleanTracker(label, rewardMinutes)

  fun addRuleTracker(label: String, tagName: String, targetMinutes: Double, rewardMinutes: Double) =
    repository.addRuleTracker(label, tagName, targetMinutes, rewardMinutes)

  fun archiveTracker(id: String) = repository.archiveTracker(id)

  fun updateManualTracker(id: String, label: String, rewardMinutes: Double) =
    repository.updateManualTracker(id, label, rewardMinutes)

  fun updateRuleTracker(id: String, label: String, tagName: String, targetMinutes: Double, rewardMinutes: Double) =
    repository.updateRuleTracker(id, label, tagName, targetMinutes, rewardMinutes)

  fun updateRules(rules: FocusWellRules) = repository.updateRules(rules)

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
        withContext(Dispatchers.Default) {
          val rules = state.rules.normalized()
          val date = LocalDate.parse(today)
          val previousDate = date.minusDays(1)
          val start = previousDate.atTime(rules.dayBoundaryTime).atZone(TimeAccounting.focusWellZone).toInstant()
          val end = date.atTime(rules.dayBoundaryTime).atZone(TimeAccounting.focusWellZone).toInstant()
          phoneUsageSegments(
            context = getApplication(),
            startedAt = start,
            endedAt = end,
            focusRecords = state.focusRecords,
            leisureRecords = state.leisureRecords,
            rules = rules,
            zone = TimeAccounting.focusWellZone,
          )
        }
      _morningCheckInState.value =
        MorningCheckInUiState(
          dailyDate = today,
          startedAt = startedAt,
          loading = false,
          segments = segments,
        )
    }
  }

  fun completeMorningCheckIn(fairUseSegmentIds: Set<String>) {
    val checkIn = _morningCheckInState.value
    val startedAt = checkIn.startedAt ?: Instant.now()
    val phoneCost = checkIn.segments.filterNot { it.id in fairUseSegmentIds }.sumOf { it.costMinutes }
    repository.completeMorningCheckIn(
      checkInStartedAt = startedAt,
      phoneCostMinutes = phoneCost,
      reviewedSegmentCount = checkIn.segments.size,
    )
    _morningCheckInState.value = MorningCheckInUiState()
  }
}
