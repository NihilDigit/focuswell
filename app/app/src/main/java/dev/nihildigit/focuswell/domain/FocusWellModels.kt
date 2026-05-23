package dev.nihildigit.focuswell.domain

import java.time.Instant

enum class SessionType(val label: String, val rate: Double) {
  Input("Input", 0.5),
  Output("Output", 0.25),
}

data class TagConfig(
  val id: String,
  val name: String,
  val multiplier: Double,
  val archivedAt: Instant? = null,
)

data class DailyTracker(
  val id: String,
  val label: String,
  val completed: Boolean,
  val rewardMinutes: Double = 10.0,
  val progressLabel: String? = null,
  val ruleTagName: String? = null,
  val ruleTargetMinutes: Double? = null,
  val wakeTime: String? = null,
  val archivedAt: Instant? = null,
)

sealed interface ActiveMode {
  data object None : ActiveMode

  data class Focus(
    val task: String,
    val type: SessionType,
    val tag: TagConfig?,
    val startedAt: Instant,
    val reminderSessionId: String,
    val revision: Int = 1,
    val paused: Boolean = false,
    val pausedAt: Instant? = null,
    val pausedDurationMillis: Long = 0,
  ) : ActiveMode

  data class Leisure(
    val startedAt: Instant,
    val reminderSessionId: String,
    val revision: Int = 1,
  ) : ActiveMode

  data class WindDown(
    val startedAt: Instant,
  ) : ActiveMode

  data object Depleted : ActiveMode
}

data class LedgerEntry(
  val id: String,
  val title: String,
  val deltaMinutes: Double,
  val createdAt: Instant,
  val note: String? = null,
  val sourceId: String? = null,
)

data class FocusRecord(
  val id: String,
  val task: String,
  val result: String,
  val type: SessionType,
  val tagName: String?,
  val tagMultiplier: Double,
  val typeRate: Double,
  val startedAt: Instant,
  val endedAt: Instant,
  val activeDurationMinutes: Double,
  val earnedMinutes: Double,
  val dailyDate: String,
  val deletedAt: Instant? = null,
)

data class LeisureRecord(
  val id: String,
  val startedAt: Instant,
  val endedAt: Instant,
  val elapsedMinutes: Double,
  val costMinutes: Double,
  val dailyDate: String,
  val deletedAt: Instant? = null,
)

enum class Destination(val label: String) {
  Today("Today"),
  Reserve("Balance"),
  Settings("Settings"),
}

data class FocusWellUiState(
  val destination: Destination = Destination.Today,
  val reserveMinutes: Double = 0.0,
  val dailyDate: String = "",
  val activeMode: ActiveMode = ActiveMode.None,
  val tags: List<TagConfig> = defaultTags,
  val trackers: List<DailyTracker> = defaultTrackers,
  val focusRecords: List<FocusRecord> = emptyList(),
  val leisureRecords: List<LeisureRecord> = emptyList(),
  val ledger: List<LedgerEntry> = emptyList(),
  val importError: String? = null,
)

val defaultTags =
  listOf(
    TagConfig(id = "math", name = "math", multiplier = 2.0),
    TagConfig(id = "408", name = "408", multiplier = 1.5),
  )

val defaultTrackers =
  listOf(
    DailyTracker(id = "aerobic", label = "Aerobic", completed = false, rewardMinutes = 10.0),
    DailyTracker(id = "wake", label = "Wake by 9", completed = false, rewardMinutes = 10.0),
    DailyTracker(id = "vocabulary", label = "Vocabulary", completed = false, rewardMinutes = 10.0),
    DailyTracker(id = "codewars", label = "CodeWars", completed = false, rewardMinutes = 10.0),
    DailyTracker(
      id = "math-3h",
      label = "Math",
      completed = false,
      rewardMinutes = 10.0,
      progressLabel = "0m / 3h",
      ruleTagName = "math",
      ruleTargetMinutes = 180.0,
    ),
    DailyTracker(
      id = "408-3h",
      label = "408",
      completed = false,
      rewardMinutes = 10.0,
      progressLabel = "0m / 3h",
      ruleTagName = "408",
      ruleTargetMinutes = 180.0,
    ),
  )
