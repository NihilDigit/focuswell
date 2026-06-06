package dev.nihildigit.focuswell.domain

import java.time.Instant
import java.time.LocalTime

const val RESERVE_RECOVERY_FOCUS_MINUTES: Double = 120.0
const val RESERVE_RECOVERY_GRANT_MINUTES: Double = 180.0
const val SAVINGS_INTEREST_FIRST_TIER_MINUTES: Double = 360.0
const val SAVINGS_INTEREST_SECOND_TIER_MINUTES: Double = 1440.0
const val SAVINGS_INTEREST_FIRST_TIER_RATE: Double = 0.05
const val SAVINGS_INTEREST_SECOND_TIER_RATE: Double = 0.08
const val SAVINGS_INTEREST_THIRD_TIER_RATE: Double = 0.12

fun savingsInterestMinutes(reserveMinutes: Double): Double {
  val balance = reserveMinutes.coerceAtLeast(0.0)
  val first = minOf(balance, SAVINGS_INTEREST_FIRST_TIER_MINUTES)
  val second = (minOf(balance, SAVINGS_INTEREST_SECOND_TIER_MINUTES) - SAVINGS_INTEREST_FIRST_TIER_MINUTES).coerceAtLeast(0.0)
  val third = (balance - SAVINGS_INTEREST_SECOND_TIER_MINUTES).coerceAtLeast(0.0)
  return first * SAVINGS_INTEREST_FIRST_TIER_RATE + second * SAVINGS_INTEREST_SECOND_TIER_RATE + third * SAVINGS_INTEREST_THIRD_TIER_RATE
}

fun savingsInterestRateLabel(): String = "5/8/12% daily"

enum class SessionType(val label: String, val rate: Double) {
  Input("Input", 0.5),
  Output("Output", 0.25),
}

enum class FocusOutcome(val label: String, val multiplier: Double) {
  AsPlanned("As planned", 1.0),
  Partial("Partial", 0.8),
  Drifted("Drifted", 0.3),
  Interrupted("Interrupted", 1.0),
}

fun focusOutcomeMultiplier(result: String): Double {
  val trimmed = result.trim()
  val outcome =
    FocusOutcome.entries.firstOrNull { it.label == trimmed }
      ?: FocusOutcome.entries.firstOrNull { trimmed.startsWith("${it.label} · ") }
  return outcome?.multiplier ?: FocusOutcome.AsPlanned.multiplier
}

enum class IdeaQuadrant(val label: String, val supporting: String) {
  Inbox("Inbox", "Captured, not sorted"),
  DoNow("Do now", "Important and urgent"),
  Schedule("Schedule", "Important, not urgent"),
  Contain("Contain", "Urgent, but keep it bounded"),
  Explore("Explore", "Interesting without pressure"),
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
  val rewardMinutes: Double = 15.0,
  val progressLabel: String? = null,
  val ruleTagName: String? = null,
  val ruleTargetMinutes: Double? = null,
  val wakeTime: String? = null,
  val archivedAt: Instant? = null,
)

data class FocusWellRules(
  val dailyGrantMinutes: Double = 60.0,
  val dayBoundaryHour: Int = 4,
  val wakeTargetHour: Int = 5,
  val sleepProtectionStartHour: Int = 23,
  val sleepProtectionEndHour: Int = 7,
  val sleepProtectionMultiplier: Double = 2.0,
  val longSessionRemindersEnabled: Boolean = true,
  val phoneUsageChargeFreePackages: Set<String> = emptySet(),
) {
  val safeDailyGrantMinutes: Double
    get() = dailyGrantMinutes.coerceIn(0.0, 24.0 * 60.0)

  val safeDayBoundaryHour: Int
    get() = dayBoundaryHour.coerceIn(0, 12)

  val safeWakeTargetHour: Int
    get() = wakeTargetHour.coerceIn(0, 23)

  val safeSleepProtectionStartHour: Int
    get() = sleepProtectionStartHour.coerceIn(0, 23)

  val safeSleepProtectionEndHour: Int
    get() = sleepProtectionEndHour.coerceIn(0, 23)

  val safeSleepProtectionMultiplier: Double
    get() = sleepProtectionMultiplier.coerceIn(1.0, 5.0)

  val safePhoneUsageChargeFreePackages: Set<String>
    get() =
      phoneUsageChargeFreePackages
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSortedSet()

  val dayBoundaryTime: LocalTime
    get() = LocalTime.of(safeDayBoundaryHour, 0)

  val wakeTargetTime: LocalTime
    get() = LocalTime.of(safeWakeTargetHour, 0)

  val sleepProtectionStartTime: LocalTime
    get() = LocalTime.of(safeSleepProtectionStartHour, 0)

  val sleepProtectionEndTime: LocalTime
    get() = LocalTime.of(safeSleepProtectionEndHour, 0)

  fun normalized(): FocusWellRules =
    copy(
      dailyGrantMinutes = safeDailyGrantMinutes,
      dayBoundaryHour = safeDayBoundaryHour,
      wakeTargetHour = safeWakeTargetHour,
      sleepProtectionStartHour = safeSleepProtectionStartHour,
      sleepProtectionEndHour = safeSleepProtectionEndHour,
      sleepProtectionMultiplier = safeSleepProtectionMultiplier,
      longSessionRemindersEnabled = longSessionRemindersEnabled,
      phoneUsageChargeFreePackages = safePhoneUsageChargeFreePackages,
    )
}

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

data class PhoneUsageApp(
  val packageName: String,
  val appName: String,
  val durationMillis: Long,
)

data class PhoneUsageSlice(
  val packageName: String,
  val appName: String,
  val startedAt: Instant,
  val endedAt: Instant,
  val durationMillis: Long,
)

data class PhoneUsageSegment(
  val id: String,
  val startedAt: Instant,
  val endedAt: Instant,
  val costMinutes: Double,
  val topApps: List<PhoneUsageApp>,
  val slices: List<PhoneUsageSlice> = emptyList(),
)

data class Idea(
  val id: String,
  val text: String,
  val quadrant: IdeaQuadrant = IdeaQuadrant.Inbox,
  val checklist: List<IdeaChecklistItem> = emptyList(),
  val createdAt: Instant,
  val updatedAt: Instant,
  val archivedAt: Instant? = null,
)

data class IdeaChecklistItem(
  val id: String,
  val text: String,
  val checked: Boolean = false,
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
  Ideas("Ideas"),
  Plan("Plan"),
  Settings("Settings"),
}

data class FocusWellUiState(
  val destination: Destination = Destination.Today,
  val reserveMinutes: Double = 0.0,
  val dailyDate: String = "",
  val stateUpdatedAt: Instant = Instant.EPOCH,
  val rules: FocusWellRules = FocusWellRules(),
  val activeMode: ActiveMode = ActiveMode.None,
  val tags: List<TagConfig> = defaultTags,
  val trackers: List<DailyTracker> = defaultTrackers,
  val focusRecords: List<FocusRecord> = emptyList(),
  val leisureRecords: List<LeisureRecord> = emptyList(),
  val ideas: List<Idea> = emptyList(),
  val ledger: List<LedgerEntry> = emptyList(),
  val lastCheckInDailyDate: String? = null,
  val lastPhoneUsageSettlementAt: Instant? = null,
  val dailyGrantPausedUntilDate: String? = null,
  val importError: String? = null,
)

val FocusWellUiState.reserveLocked: Boolean
  get() = dailyGrantPausedUntilDate != null

val defaultTags =
  listOf(
    TagConfig(id = "math", name = "math", multiplier = 2.0),
    TagConfig(id = "408", name = "408", multiplier = 1.5),
  )

val defaultTrackers =
  listOf(
    DailyTracker(id = "aerobic", label = "Aerobic", completed = false, rewardMinutes = 30.0),
    DailyTracker(id = "vocabulary", label = "Vocabulary", completed = false, rewardMinutes = 15.0),
    DailyTracker(id = "codewars", label = "CodeWars", completed = false, rewardMinutes = 15.0),
    DailyTracker(
      id = "math-3h",
      label = "Math",
      completed = false,
      rewardMinutes = 60.0,
      progressLabel = "0m / 3h",
      ruleTagName = "math",
      ruleTargetMinutes = 180.0,
    ),
    DailyTracker(
      id = "408-3h",
      label = "408",
      completed = false,
      rewardMinutes = 60.0,
      progressLabel = "0m / 3h",
      ruleTagName = "408",
      ruleTargetMinutes = 180.0,
    ),
  )
