package dev.nihildigit.focuswell.ui.main

import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.LeisureRecord
import dev.nihildigit.focuswell.domain.SessionType
import java.time.Instant
import java.time.LocalDate
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReserveRecordSummariesTest {
  private val previousTimeZone = TimeZone.getDefault()

  @Before
  fun setUp() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  }

  @After
  fun tearDown() {
    TimeZone.setDefault(previousTimeZone)
  }

  @Test
  fun sevenDayNetPoints_groupsLedgerEntriesByBusinessDate() {
    val rules = FocusWellRules(dayBoundaryHour = 4)
    val entries =
      listOf(
        ledger(id = "late", delta = 10.0, createdAt = "2026-05-20T03:30:00Z"),
        ledger(id = "today", delta = -5.0, createdAt = "2026-05-20T05:00:00Z"),
      )

    val points = sevenDayNetPoints(entries, rules, today = LocalDate.parse("2026-05-20"))

    assertEquals(7, points.size)
    assertEquals(10.0, points.first { it.date == LocalDate.parse("2026-05-19") }.netMinutes, 0.0001)
    assertEquals(-5.0, points.first { it.date == LocalDate.parse("2026-05-20") }.netMinutes, 0.0001)
  }

  @Test
  fun balanceRecordItems_hidesLedgerEntriesAlreadyRepresentedByRecords() {
    val focus = focusRecord()
    val leisure = leisureRecord()
    val items =
      balanceRecordItems(
        focusRecords = listOf(focus),
        leisureRecords = listOf(leisure),
        ledger =
          listOf(
            ledger(id = "ledger-focus", delta = 20.0, createdAt = "2026-05-20T05:10:00Z", sourceId = focus.id),
            ledger(id = "deleted-focus", title = "Deleted focus", delta = -20.0, createdAt = "2026-05-20T06:10:00Z", sourceId = focus.id),
            ledger(id = "manual", title = "Manual adjustment", delta = 5.0, createdAt = "2026-05-20T07:10:00Z"),
          ),
        focusSourceIds = setOf(focus.id),
        leisureSourceIds = setOf(leisure.id),
      )

    assertTrue(items.any { it is BalanceRecordItem.Focus && it.record.id == focus.id })
    assertTrue(items.any { it is BalanceRecordItem.Leisure && it.record.id == leisure.id })
    assertTrue(items.any { it is BalanceRecordItem.Adjustment && it.entry.id == "deleted-focus" })
    assertTrue(items.any { it is BalanceRecordItem.Adjustment && it.entry.id == "manual" })
    assertTrue(items.none { it is BalanceRecordItem.Adjustment && it.entry.id == "ledger-focus" })
  }

  @Test
  fun filteredBalanceRecordItems_returnsOnlyRequestedType() {
    val focus = BalanceRecordItem.Focus(focusRecord())
    val leisure = BalanceRecordItem.Leisure(leisureRecord())
    val adjustment = BalanceRecordItem.Adjustment(ledger(id = "manual", delta = 5.0, createdAt = "2026-05-20T07:10:00Z"))
    val records = listOf(focus, leisure, adjustment)

    assertEquals(listOf(focus), filteredBalanceRecordItems(records, BalanceRecordFilter.Focus))
    assertEquals(listOf(leisure), filteredBalanceRecordItems(records, BalanceRecordFilter.Leisure))
    assertEquals(listOf(adjustment), filteredBalanceRecordItems(records, BalanceRecordFilter.Adjustments))
    assertEquals(records, filteredBalanceRecordItems(records, BalanceRecordFilter.All))
  }

  private fun focusRecord(): FocusRecord =
    FocusRecord(
      id = "focus-1",
      task = "Math",
      result = "As planned",
      type = SessionType.Input,
      tagName = "math",
      tagMultiplier = 1.0,
      typeRate = SessionType.Input.rate,
      startedAt = Instant.parse("2026-05-20T04:00:00Z"),
      endedAt = Instant.parse("2026-05-20T05:00:00Z"),
      activeDurationMinutes = 60.0,
      earnedMinutes = 30.0,
      dailyDate = "2026-05-20",
    )

  private fun leisureRecord(): LeisureRecord =
    LeisureRecord(
      id = "leisure-1",
      startedAt = Instant.parse("2026-05-20T05:00:00Z"),
      endedAt = Instant.parse("2026-05-20T05:20:00Z"),
      elapsedMinutes = 20.0,
      costMinutes = 20.0,
      dailyDate = "2026-05-20",
    )

  private fun ledger(
    id: String,
    delta: Double,
    createdAt: String,
    title: String = "Focus",
    sourceId: String? = null,
  ): LedgerEntry =
    LedgerEntry(
      id = id,
      title = title,
      deltaMinutes = delta,
      createdAt = Instant.parse(createdAt),
      sourceId = sourceId,
    )
}
