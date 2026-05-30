package dev.nihildigit.focuswell.data.db

import android.content.Context
import dev.nihildigit.focuswell.data.FocusWellStore
import dev.nihildigit.focuswell.data.withLedgerBackedReserve
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.FocusWellRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

internal class RoomFocusWellStore(
  private val dao: FocusWellDao,
) : FocusWellStore {
  constructor(context: Context) : this(FocusWellDatabase.get(context).focusWellDao())

  override suspend fun loadState(): FocusWellUiState? =
    withContext(Dispatchers.IO) {
      val appState = dao.appState() ?: return@withContext null
      FocusWellUiState(
        dailyDate = appState.dailyDate,
        stateUpdatedAt = Instant.parse(appState.stateUpdatedAt),
        rules =
          FocusWellRules(
            dailyGrantMinutes = appState.dailyGrantMinutes,
            dayBoundaryHour = appState.dayBoundaryHour,
            wakeTargetHour = appState.wakeTargetHour,
            sleepProtectionStartHour = appState.sleepProtectionStartHour,
            sleepProtectionEndHour = appState.sleepProtectionEndHour,
            sleepProtectionMultiplier = appState.sleepProtectionMultiplier,
            longSessionRemindersEnabled = appState.longSessionRemindersEnabled,
            phoneUsageChargeFreePackages = appState.phoneUsageChargeFreePackagesJson.toStoredPackageSet(),
          ).normalized(),
        activeMode = appState.toActiveMode(),
        tags = dao.tags().map { it.toDomain() },
        trackers = dao.trackers().map { it.toDomain() },
        focusRecords = dao.focusRecords().map { it.toDomain() },
        leisureRecords = dao.leisureRecords().map { it.toDomain() },
        ideas = dao.ideas().map { it.toDomain() },
        ledger = dao.ledger().map { it.toDomain() },
        lastCheckInDailyDate = appState.lastCheckInDailyDate,
        lastPhoneUsageSettlementAt = appState.lastPhoneUsageSettlementAt?.let(Instant::parse),
        dailyGrantPausedUntilDate = appState.dailyGrantPausedUntilDate,
      ).withLedgerBackedReserve()
    }

  override suspend fun persistChange(previous: FocusWellUiState?, next: FocusWellUiState) {
    withContext(Dispatchers.IO) {
      if (previous == null) {
        dao.replaceState(
          appState = next.toAppStateEntity(),
          tags = next.tags.mapIndexed { index, tag -> tag.toEntity(index) },
          trackers = next.trackers.mapIndexed { index, tracker -> tracker.toEntity(index) },
          focusRecords = next.focusRecords.map { it.toEntity() },
          leisureRecords = next.leisureRecords.map { it.toEntity() },
          ideas = next.ideas.map { it.toEntity() },
          ledger = next.ledger.map { it.toEntity() },
        )
        return@withContext
      }

      val tagDiff =
        listDiff(
          previous = previous.tags.mapIndexed { index, tag -> tag.toEntity(index) },
          next = next.tags.mapIndexed { index, tag -> tag.toEntity(index) },
          id = TagEntity::id,
        )
      val trackerDiff =
        listDiff(
          previous = previous.trackers.mapIndexed { index, tracker -> tracker.toEntity(index) },
          next = next.trackers.mapIndexed { index, tracker -> tracker.toEntity(index) },
          id = DailyTrackerEntity::id,
        )
      val focusRecordDiff =
        listDiff(
          previous = previous.focusRecords.map { it.toEntity() },
          next = next.focusRecords.map { it.toEntity() },
          id = FocusRecordEntity::id,
        )
      val leisureRecordDiff =
        listDiff(
          previous = previous.leisureRecords.map { it.toEntity() },
          next = next.leisureRecords.map { it.toEntity() },
          id = LeisureRecordEntity::id,
        )
      val ideaDiff =
        listDiff(
          previous = previous.ideas.map { it.toEntity() },
          next = next.ideas.map { it.toEntity() },
          id = IdeaEntity::id,
        )
      val ledgerDiff =
        listDiff(
          previous = previous.ledger.map { it.toEntity() },
          next = next.ledger.map { it.toEntity() },
          id = LedgerEntryEntity::id,
        )
      dao.applyStateDiff(
        appState = next.toAppStateEntity(),
        changedTags = tagDiff.changed,
        removedTagIds = tagDiff.removedIds,
        changedTrackers = trackerDiff.changed,
        removedTrackerIds = trackerDiff.removedIds,
        changedFocusRecords = focusRecordDiff.changed,
        removedFocusRecordIds = focusRecordDiff.removedIds,
        changedLeisureRecords = leisureRecordDiff.changed,
        removedLeisureRecordIds = leisureRecordDiff.removedIds,
        changedIdeas = ideaDiff.changed,
        removedIdeaIds = ideaDiff.removedIds,
        changedLedgerEntries = ledgerDiff.changed,
        removedLedgerEntryIds = ledgerDiff.removedIds,
      )
    }
  }

  override suspend fun clear() {
    withContext(Dispatchers.IO) {
      dao.clearAll()
    }
  }

  private fun <T> listDiff(
    previous: List<T>,
    next: List<T>,
    id: (T) -> String,
  ): EntityDiff<T> {
    val previousById = previous.associateBy(id)
    val nextById = next.associateBy(id)
    val changed = next.filter { previousById[id(it)] != it }
    val removed = previousById.keys - nextById.keys
    return EntityDiff(changed = changed, removedIds = removed.toList())
  }
}

private data class EntityDiff<T>(
  val changed: List<T>,
  val removedIds: List<String>,
)
