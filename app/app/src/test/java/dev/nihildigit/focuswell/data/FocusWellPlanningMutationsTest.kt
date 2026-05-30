package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.FocusWellUiState
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
}
