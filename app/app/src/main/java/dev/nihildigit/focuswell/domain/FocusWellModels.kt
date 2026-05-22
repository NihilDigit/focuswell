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
  val progressLabel: String? = null,
)

sealed interface ActiveMode {
  data object None : ActiveMode

  data class Focus(
    val task: String,
    val type: SessionType,
    val tag: TagConfig,
    val startedAt: Instant,
    val paused: Boolean = false,
  ) : ActiveMode

  data class Leisure(
    val startedAt: Instant,
  ) : ActiveMode

  data class WindDown(
    val startedAt: Instant,
  ) : ActiveMode
}

data class LedgerEntry(
  val id: String,
  val title: String,
  val deltaMinutes: Double,
  val createdAt: Instant,
)

enum class Destination(val label: String) {
  Today("Today"),
  Reserve("Reserve"),
  Records("Records"),
  Settings("Settings"),
}

data class FocusWellUiState(
  val destination: Destination = Destination.Today,
  val reserveMinutes: Double = 60.0,
  val activeMode: ActiveMode = ActiveMode.None,
  val tags: List<TagConfig> = defaultTags,
  val trackers: List<DailyTracker> = defaultTrackers,
  val ledger: List<LedgerEntry> =
    listOf(
      LedgerEntry(
        id = "daily-grant",
        title = "Daily grant",
        deltaMinutes = 60.0,
        createdAt = Instant.now(),
      )
    ),
)

val defaultTags =
  listOf(
    TagConfig(id = "math", name = "math", multiplier = 2.0),
    TagConfig(id = "408", name = "408", multiplier = 1.5),
  )

val defaultTrackers =
  listOf(
    DailyTracker(id = "aerobic", label = "Aerobic", completed = false),
    DailyTracker(id = "wake", label = "Wake by 9", completed = false),
    DailyTracker(id = "vocabulary", label = "Vocabulary", completed = false),
    DailyTracker(id = "codewars", label = "CodeWars", completed = false),
    DailyTracker(id = "math-3h", label = "Math", completed = false, progressLabel = "0m / 3h"),
    DailyTracker(id = "408-3h", label = "408", completed = false, progressLabel = "0m / 3h"),
  )
