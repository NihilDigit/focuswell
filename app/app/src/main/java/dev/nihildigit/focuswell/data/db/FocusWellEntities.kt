package dev.nihildigit.focuswell.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_state")
internal data class AppStateEntity(
  @PrimaryKey val id: Int = 0,
  val dailyDate: String,
  val dailyGrantMinutes: Double = 60.0,
  val dayBoundaryHour: Int = 4,
  val sleepProtectionStartHour: Int = 1,
  val sleepProtectionMultiplier: Double = 2.0,
  val activeKind: String,
  val activeStartedAt: String? = null,
  val activeFocusTask: String? = null,
  val activeFocusType: String? = null,
  val activeTagId: String? = null,
  val activeTagName: String? = null,
  val activeTagMultiplier: Double? = null,
  val activeTagArchivedAt: String? = null,
  val activeReminderSessionId: String? = null,
  val activeRevision: Int = 1,
  val activePaused: Boolean = false,
  val activePausedAt: String? = null,
  val activePausedDurationMillis: Long = 0,
)

@Entity(tableName = "tag_configs")
internal data class TagEntity(
  @PrimaryKey val id: String,
  val sortOrder: Int,
  val name: String,
  val multiplier: Double,
  val archivedAt: String?,
)

@Entity(tableName = "daily_trackers")
internal data class DailyTrackerEntity(
  @PrimaryKey val id: String,
  val sortOrder: Int,
  val label: String,
  val completed: Boolean,
  val rewardMinutes: Double,
  val progressLabel: String?,
  val ruleTagName: String?,
  val ruleTargetMinutes: Double?,
  val wakeTime: String?,
  val archivedAt: String?,
)

@Entity(tableName = "focus_records")
internal data class FocusRecordEntity(
  @PrimaryKey val id: String,
  val task: String,
  val result: String,
  val type: String,
  val tagName: String?,
  val tagMultiplier: Double,
  val typeRate: Double,
  val startedAt: String,
  val endedAt: String,
  val activeDurationMinutes: Double,
  val earnedMinutes: Double,
  val dailyDate: String,
  val deletedAt: String?,
)

@Entity(tableName = "leisure_records")
internal data class LeisureRecordEntity(
  @PrimaryKey val id: String,
  val startedAt: String,
  val endedAt: String,
  val elapsedMinutes: Double,
  val costMinutes: Double,
  val dailyDate: String,
  val deletedAt: String?,
)

@Entity(tableName = "ideas")
internal data class IdeaEntity(
  @PrimaryKey val id: String,
  val text: String,
  val quadrant: String,
  val checklistJson: String = "[]",
  val createdAt: String,
  val updatedAt: String,
  val archivedAt: String?,
)

@Entity(tableName = "ledger_entries")
internal data class LedgerEntryEntity(
  @PrimaryKey val id: String,
  val title: String,
  val deltaMinutes: Double,
  val createdAt: String,
  val note: String?,
  val sourceId: String?,
)
