package dev.nihildigit.focuswell.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
internal abstract class FocusWellDao {
  @Query("SELECT * FROM app_state WHERE id = 0")
  abstract suspend fun appState(): AppStateEntity?

  @Query("SELECT * FROM tag_configs ORDER BY sortOrder ASC")
  abstract suspend fun tags(): List<TagEntity>

  @Query("SELECT * FROM daily_trackers ORDER BY sortOrder ASC")
  abstract suspend fun trackers(): List<DailyTrackerEntity>

  @Query("SELECT * FROM focus_records ORDER BY endedAt DESC, id DESC")
  abstract suspend fun focusRecords(): List<FocusRecordEntity>

  @Query("SELECT * FROM leisure_records ORDER BY endedAt DESC, id DESC")
  abstract suspend fun leisureRecords(): List<LeisureRecordEntity>

  @Query("SELECT * FROM ledger_entries ORDER BY createdAt DESC, id DESC")
  abstract suspend fun ledger(): List<LedgerEntryEntity>

  @Upsert
  abstract suspend fun upsertAppState(state: AppStateEntity)

  @Upsert
  abstract suspend fun upsertTags(tags: List<TagEntity>)

  @Upsert
  abstract suspend fun upsertTrackers(trackers: List<DailyTrackerEntity>)

  @Upsert
  abstract suspend fun upsertFocusRecords(records: List<FocusRecordEntity>)

  @Upsert
  abstract suspend fun upsertLeisureRecords(records: List<LeisureRecordEntity>)

  @Upsert
  abstract suspend fun upsertLedger(entries: List<LedgerEntryEntity>)

  @Query("DELETE FROM tag_configs WHERE id IN (:ids)")
  abstract suspend fun deleteTags(ids: List<String>)

  @Query("DELETE FROM daily_trackers WHERE id IN (:ids)")
  abstract suspend fun deleteTrackers(ids: List<String>)

  @Query("DELETE FROM focus_records WHERE id IN (:ids)")
  abstract suspend fun deleteFocusRecords(ids: List<String>)

  @Query("DELETE FROM leisure_records WHERE id IN (:ids)")
  abstract suspend fun deleteLeisureRecords(ids: List<String>)

  @Query("DELETE FROM ledger_entries WHERE id IN (:ids)")
  abstract suspend fun deleteLedgerEntries(ids: List<String>)

  @Query("DELETE FROM app_state")
  abstract suspend fun deleteAppState()

  @Query("DELETE FROM tag_configs")
  abstract suspend fun deleteAllTags()

  @Query("DELETE FROM daily_trackers")
  abstract suspend fun deleteAllTrackers()

  @Query("DELETE FROM focus_records")
  abstract suspend fun deleteAllFocusRecords()

  @Query("DELETE FROM leisure_records")
  abstract suspend fun deleteAllLeisureRecords()

  @Query("DELETE FROM ledger_entries")
  abstract suspend fun deleteAllLedgerEntries()

  @Transaction
  open suspend fun clearAll() {
    deleteAllLedgerEntries()
    deleteAllLeisureRecords()
    deleteAllFocusRecords()
    deleteAllTrackers()
    deleteAllTags()
    deleteAppState()
  }

  @Transaction
  open suspend fun replaceState(
    appState: AppStateEntity,
    tags: List<TagEntity>,
    trackers: List<DailyTrackerEntity>,
    focusRecords: List<FocusRecordEntity>,
    leisureRecords: List<LeisureRecordEntity>,
    ledger: List<LedgerEntryEntity>,
  ) {
    clearAll()
    upsertAppState(appState)
    upsertTags(tags)
    upsertTrackers(trackers)
    upsertFocusRecords(focusRecords)
    upsertLeisureRecords(leisureRecords)
    upsertLedger(ledger)
  }

  @Transaction
  open suspend fun applyStateDiff(
    appState: AppStateEntity,
    changedTags: List<TagEntity>,
    removedTagIds: List<String>,
    changedTrackers: List<DailyTrackerEntity>,
    removedTrackerIds: List<String>,
    changedFocusRecords: List<FocusRecordEntity>,
    removedFocusRecordIds: List<String>,
    changedLeisureRecords: List<LeisureRecordEntity>,
    removedLeisureRecordIds: List<String>,
    changedLedgerEntries: List<LedgerEntryEntity>,
    removedLedgerEntryIds: List<String>,
  ) {
    upsertAppState(appState)
    if (changedTags.isNotEmpty()) upsertTags(changedTags)
    if (removedTagIds.isNotEmpty()) deleteTags(removedTagIds)
    if (changedTrackers.isNotEmpty()) upsertTrackers(changedTrackers)
    if (removedTrackerIds.isNotEmpty()) deleteTrackers(removedTrackerIds)
    if (changedFocusRecords.isNotEmpty()) upsertFocusRecords(changedFocusRecords)
    if (removedFocusRecordIds.isNotEmpty()) deleteFocusRecords(removedFocusRecordIds)
    if (changedLeisureRecords.isNotEmpty()) upsertLeisureRecords(changedLeisureRecords)
    if (removedLeisureRecordIds.isNotEmpty()) deleteLeisureRecords(removedLeisureRecordIds)
    if (changedLedgerEntries.isNotEmpty()) upsertLedger(changedLedgerEntries)
    if (removedLedgerEntryIds.isNotEmpty()) deleteLedgerEntries(removedLedgerEntryIds)
  }
}
