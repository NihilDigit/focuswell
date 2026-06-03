package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.TagConfig
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FocusWellPlanningMutationsTest {
  private val now = Instant.parse("2026-05-20T05:00:00Z")

  @Test
  fun withAddedTag_ignoresBlankOrDuplicateActiveTag() {
    val state = FocusWellUiState(tags = listOf(TagConfig(id = "math", name = "Math", multiplier = 1.0)))

    assertSame(state, state.withAddedTag(" ", multiplier = 2.0, createdAt = now))
    assertSame(state, state.withAddedTag("math", multiplier = 2.0, createdAt = now))
  }

  @Test
  fun withUpdatedTag_trimsNameAndClampsMultiplier() {
    val state = FocusWellUiState(tags = listOf(TagConfig(id = "math", name = "Math", multiplier = 1.0)))
    val updated = state.withUpdatedTag(id = "math", name = "  Deep Math  ", multiplier = -1.0)

    assertEquals("Deep Math", updated.tags.single().name)
    assertEquals(0.0, updated.tags.single().multiplier, 0.0001)
  }

  @Test
  fun withAddedRuleTracker_rejectsInvalidTargetAndTrimsFields() {
    val state = FocusWellUiState(trackers = emptyList())

    assertSame(
      state,
      state.withAddedRuleTracker(
        label = "Math",
        tagName = "math",
        targetMinutes = 0.0,
        rewardMinutes = 60.0,
        createdAt = now,
      ),
    )

    val updated =
      state.withAddedRuleTracker(
        label = "  Math  ",
        tagName = "  math  ",
        targetMinutes = 180.0,
        rewardMinutes = -5.0,
        createdAt = now,
      )

    assertEquals("Math", updated.trackers.single().label)
    assertEquals("math", updated.trackers.single().ruleTagName)
    assertEquals(180.0, updated.trackers.single().ruleTargetMinutes ?: 0.0, 0.0001)
    assertEquals(0.0, updated.trackers.single().rewardMinutes, 0.0001)
  }

  @Test
  fun withToggledManualTracker_doesNotToggleRuleTracker() {
    val state =
      FocusWellUiState(
        trackers =
          listOf(
            DailyTracker(id = "manual", label = "Manual", completed = false),
            DailyTracker(id = "rule", label = "Rule", completed = false, ruleTagName = "math"),
          ),
      )

    val updated = state.withToggledManualTracker("manual").withToggledManualTracker("rule")

    assertEquals(true, updated.trackers.first { it.id == "manual" }.completed)
    assertEquals(false, updated.trackers.first { it.id == "rule" }.completed)
  }

  @Test
  fun withComputedTrackers_activeFocusUpdatesProgressButNotCompletion() {
    val state =
      FocusWellUiState(
        dailyDate = "2026-05-20",
        activeMode =
          ActiveMode.Focus(
            task = "Math",
            type = SessionType.Input,
            tag = TagConfig(id = "math", name = "math", multiplier = 2.0),
            startedAt = Instant.parse("2026-05-20T05:00:00Z"),
            reminderSessionId = "focus-1",
          ),
        trackers =
          listOf(
            DailyTracker(
              id = "math-90m",
              label = "Math",
              completed = false,
              ruleTagName = "math",
              ruleTargetMinutes = 90.0,
            )
          ),
        focusRecords =
          listOf(
            FocusRecord(
              id = "record-1",
              task = "Math",
              result = "As planned",
              type = SessionType.Input,
              tagName = "math",
              tagMultiplier = 2.0,
              typeRate = 0.5,
              startedAt = Instant.parse("2026-05-20T03:00:00Z"),
              endedAt = Instant.parse("2026-05-20T04:00:00Z"),
              activeDurationMinutes = 60.0,
              earnedMinutes = 60.0,
              dailyDate = "2026-05-20",
            )
          ),
      )

    val persisted = state.withComputedTrackers().trackers.single()
    val displayed = state.withComputedTrackers(Instant.parse("2026-05-20T05:30:00Z")).trackers.single()

    assertEquals(false, persisted.completed)
    assertEquals("1h 0m / 90m", persisted.progressLabel)
    assertEquals(false, displayed.completed)
    assertEquals("1h 30m / 90m", displayed.progressLabel)
  }
}
