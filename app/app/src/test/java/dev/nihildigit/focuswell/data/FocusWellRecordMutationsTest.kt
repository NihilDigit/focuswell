package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.LeisureRecord
import dev.nihildigit.focuswell.domain.SessionType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FocusWellRecordMutationsTest {
  private val startedAt = Instant.parse("2026-05-20T05:00:00Z")
  private val endedAt = Instant.parse("2026-05-20T06:00:00Z")
  private val changedAt = Instant.parse("2026-05-20T07:00:00Z")

  @Test
  fun withDeletedFocusRecord_marksDeletedAndAddsNegativeAdjustment() {
    val state = FocusWellUiState(reserveMinutes = 40.0, focusRecords = listOf(focusRecord(earnedMinutes = 30.0)))

    val updated = state.withDeletedFocusRecord(id = "focus", deletedAt = changedAt)

    assertEquals(changedAt, updated.focusRecords.single().deletedAt)
    assertEquals(10.0, updated.reserveMinutes, 0.0001)
    assertEquals("Deleted focus", updated.ledger.first().title)
    assertEquals(-30.0, updated.ledger.first().deltaMinutes, 0.0001)
  }

  @Test
  fun withUpdatedFocusRecord_recomputesEarnedMinutesAndAddsAdjustment() {
    val state =
      FocusWellUiState(
        reserveMinutes = 30.0,
        focusRecords = listOf(focusRecord(earnedMinutes = 30.0, activeDurationMinutes = 60.0)),
      )

    val updated =
      state.withUpdatedFocusRecord(
        id = "focus",
        result = "Partial",
        activeMinutes = 120.0,
        updatedAt = changedAt,
      )

    val record = updated.focusRecords.single()
    assertEquals("Partial", record.result)
    assertEquals(120.0, record.activeDurationMinutes, 0.0001)
    assertEquals(48.0, record.earnedMinutes, 0.0001)
    assertEquals(48.0, updated.reserveMinutes, 0.0001)
    assertEquals(18.0, updated.ledger.first().deltaMinutes, 0.0001)
  }

  @Test
  fun withUpdatedFocusRecord_skipsAdjustmentWhenEarnedMinutesDoNotChange() {
    val state =
      FocusWellUiState(
        reserveMinutes = 30.0,
        focusRecords = listOf(focusRecord(earnedMinutes = 30.0, activeDurationMinutes = 60.0)),
        ledger = listOf(ledgerEntry()),
      )

    val updated =
      state.withUpdatedFocusRecord(
        id = "focus",
        result = "As planned",
        activeMinutes = 60.0,
        updatedAt = changedAt,
      )

    assertEquals(listOf("original-ledger"), updated.ledger.map { it.id })
  }

  @Test
  fun withDeletedLeisureRecord_restoresCostAndAddsPositiveAdjustment() {
    val state = FocusWellUiState(reserveMinutes = 10.0, leisureRecords = listOf(leisureRecord(costMinutes = 25.0)))

    val updated = state.withDeletedLeisureRecord(id = "leisure", deletedAt = changedAt)

    assertEquals(changedAt, updated.leisureRecords.single().deletedAt)
    assertEquals(35.0, updated.reserveMinutes, 0.0001)
    assertEquals("Deleted leisure", updated.ledger.first().title)
    assertEquals(25.0, updated.ledger.first().deltaMinutes, 0.0001)
  }

  @Test
  fun withAddedManualAdjustment_addsLedgerEntryAndCapsNegativeDeltaAtReserve() {
    val state = FocusWellUiState(reserveMinutes = 10.0)

    val updated =
      state.withAddedManualAdjustment(
        title = "  Correction  ",
        deltaMinutes = -15.0,
        note = "  overcounted  ",
        tagName = "  math  ",
        createdAt = changedAt,
      )

    assertEquals(0.0, updated.reserveMinutes, 0.0001)
    assertEquals("Correction", updated.ledger.first().title)
    assertEquals(-10.0, updated.ledger.first().deltaMinutes, 0.0001)
    assertEquals("overcounted", updated.ledger.first().note)
    assertEquals("math", updated.ledger.first().tagName)
  }

  @Test
  fun withAddedManualAdjustment_ignoresZeroDelta() {
    val state = FocusWellUiState(reserveMinutes = 10.0, ledger = listOf(ledgerEntry()))

    val updated =
      state.withAddedManualAdjustment(
        title = "Correction",
        deltaMinutes = 0.0,
        note = null,
        tagName = "math",
        createdAt = changedAt,
      )

    assertSame(state, updated)
  }

  @Test
  fun recordMutations_returnSameStateWhenRecordIsMissingOrDeleted() {
    val deletedFocus = focusRecord(earnedMinutes = 10.0, deletedAt = changedAt)
    val state = FocusWellUiState(focusRecords = listOf(deletedFocus))

    assertSame(state, state.withDeletedFocusRecord(id = "missing", deletedAt = changedAt))
    assertSame(state, state.withUpdatedFocusRecord(id = "focus", result = "Edited", activeMinutes = 90.0, updatedAt = changedAt))
  }

  private fun focusRecord(
    earnedMinutes: Double,
    activeDurationMinutes: Double = 60.0,
    deletedAt: Instant? = null,
  ): FocusRecord =
    FocusRecord(
      id = "focus",
      task = "Read",
      result = "As planned",
      type = SessionType.Input,
      tagName = null,
      tagMultiplier = 1.0,
      typeRate = 0.5,
      startedAt = startedAt,
      endedAt = endedAt,
      activeDurationMinutes = activeDurationMinutes,
      earnedMinutes = earnedMinutes,
      dailyDate = "2026-05-20",
      deletedAt = deletedAt,
    )

  private fun leisureRecord(costMinutes: Double): LeisureRecord =
    LeisureRecord(
      id = "leisure",
      startedAt = startedAt,
      endedAt = endedAt,
      elapsedMinutes = 60.0,
      costMinutes = costMinutes,
      dailyDate = "2026-05-20",
    )

  private fun ledgerEntry(): LedgerEntry =
    LedgerEntry(
      id = "original-ledger",
      title = "Focus",
      deltaMinutes = 30.0,
      createdAt = endedAt,
    )
}
