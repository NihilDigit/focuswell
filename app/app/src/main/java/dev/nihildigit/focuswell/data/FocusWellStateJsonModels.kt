package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.IdeaQuadrant
import dev.nihildigit.focuswell.domain.SessionType
import kotlinx.serialization.Serializable

@Serializable
internal data class SerializedFocusWellState(
  val stateUpdatedAtUtc: String? = null,
  val reserveMinutes: Double = 0.0,
  val dailyDate: String? = null,
  val rules: SerializedRules? = null,
  val activeMode: SerializedActiveMode? = null,
  val tags: List<SerializedTag> = emptyList(),
  val trackers: List<SerializedDailyTracker> = emptyList(),
  val focusRecords: List<SerializedFocusRecord> = emptyList(),
  val leisureRecords: List<SerializedLeisureRecord> = emptyList(),
  val ideas: List<SerializedIdea> = emptyList(),
  val ledger: List<SerializedLedgerEntry> = emptyList(),
  val lastCheckInDailyDate: String? = null,
  val lastPhoneUsageSettlementAt: String? = null,
  val dailyGrantPausedUntilDate: String? = null,
)

@Serializable
internal data class SerializedRules(
  val dailyGrantMinutes: Double = FocusWellRules().dailyGrantMinutes,
  val dayBoundaryHour: Int = FocusWellRules().dayBoundaryHour,
  val wakeTargetHour: Int = FocusWellRules().wakeTargetHour,
  val wakeTargetMinute: Int = FocusWellRules().wakeTargetMinute,
  val sleepProtectionStartHour: Int = FocusWellRules().sleepProtectionStartHour,
  val sleepProtectionEndHour: Int = FocusWellRules().sleepProtectionEndHour,
  val sleepProtectionMultiplier: Double = FocusWellRules().sleepProtectionMultiplier,
  val longSessionRemindersEnabled: Boolean = FocusWellRules().longSessionRemindersEnabled,
  val phoneUsageChargeFreePackages: List<String> = emptyList(),
)

@Serializable
internal data class SerializedActiveMode(
  val kind: String,
  val task: String? = null,
  val type: String? = null,
  val tag: SerializedTag? = null,
  val startedAt: String? = null,
  val reminderSessionId: String? = null,
  val revision: Int = 1,
  val paused: Boolean = false,
  val pausedAt: String? = null,
  val pausedDurationMillis: Long = 0L,
)

@Serializable
internal data class SerializedTag(
  val id: String = "",
  val name: String = "",
  val multiplier: Double = 1.0,
  val archivedAt: String? = null,
)

@Serializable
internal data class SerializedDailyTracker(
  val id: String = "",
  val label: String = "",
  val completed: Boolean = false,
  val rewardMinutes: Double? = null,
  val progressLabel: String? = null,
  val ruleTagName: String? = null,
  val ruleTargetMinutes: Double? = null,
  val wakeTime: String? = null,
  val archivedAt: String? = null,
)

@Serializable
internal data class SerializedFocusRecord(
  val id: String = "",
  val task: String = "",
  val result: String = "",
  val type: String = SessionType.Input.name,
  val tagName: String? = null,
  val tagMultiplier: Double = 1.0,
  val typeRate: Double = 0.5,
  val startedAt: String,
  val endedAt: String,
  val activeDurationMinutes: Double = 0.0,
  val earnedMinutes: Double = 0.0,
  val dailyDate: String? = null,
  val deletedAt: String? = null,
)

@Serializable
internal data class SerializedLeisureRecord(
  val id: String = "",
  val startedAt: String,
  val endedAt: String,
  val elapsedMinutes: Double = 0.0,
  val costMinutes: Double = 0.0,
  val dailyDate: String? = null,
  val deletedAt: String? = null,
)

@Serializable
internal data class SerializedIdea(
  val id: String = "",
  val text: String = "",
  val quadrant: String = IdeaQuadrant.Inbox.name,
  val checklist: List<SerializedIdeaChecklistItem> = emptyList(),
  val createdAt: String,
  val updatedAt: String? = null,
  val archivedAt: String? = null,
)

@Serializable
internal data class SerializedIdeaChecklistItem(
  val id: String = "",
  val text: String = "",
  val checked: Boolean = false,
)

@Serializable
internal data class SerializedLedgerEntry(
  val id: String = "",
  val title: String = "",
  val deltaMinutes: Double = 0.0,
  val createdAt: String,
  val note: String? = null,
  val sourceId: String? = null,
)
