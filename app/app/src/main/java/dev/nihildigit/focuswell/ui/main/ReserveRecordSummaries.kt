package dev.nihildigit.focuswell.ui.main

import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.LeisureRecord
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.time.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import java.time.Instant
import java.time.LocalDate

internal sealed interface BalanceRecordItem {
  val id: String
  val occurredAt: Instant
  val deltaMinutes: Double

  data class Focus(val record: FocusRecord) : BalanceRecordItem {
    override val id: String = "focus-${record.id}"
    override val occurredAt: Instant = record.endedAt
    override val deltaMinutes: Double = record.earnedMinutes
  }

  data class Leisure(val record: LeisureRecord) : BalanceRecordItem {
    override val id: String = "leisure-${record.id}"
    override val occurredAt: Instant = record.endedAt
    override val deltaMinutes: Double = -record.costMinutes
  }

  data class Adjustment(val entry: LedgerEntry) : BalanceRecordItem {
    override val id: String = "ledger-${entry.id}"
    override val occurredAt: Instant = entry.createdAt
    override val deltaMinutes: Double = entry.deltaMinutes
  }
}

internal data class DailyNetPoint(
  val date: LocalDate,
  val label: String,
  val netMinutes: Double,
)

internal fun sevenDayNetPoints(
  entries: List<LedgerEntry>,
  rules: FocusWellRules,
  today: LocalDate = TimeAccounting.dailyDate(Instant.now(), rules = rules.normalized()),
): List<DailyNetPoint> {
  val normalizedRules = rules.normalized()
  val byDate = entries.groupBy { TimeAccounting.dailyDate(it.createdAt, rules = normalizedRules) }
  return (6 downTo 0).map { daysAgo ->
    val date = today.minusDays(daysAgo.toLong())
    DailyNetPoint(
      date = date,
      label = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
      netMinutes = byDate[date].orEmpty().sumOf { it.deltaMinutes },
    )
  }
}

internal fun todayNetMovement(
  entries: List<LedgerEntry>,
  rules: FocusWellRules,
  today: LocalDate = TimeAccounting.dailyDate(Instant.now(), rules = rules.normalized()),
): Double {
  val normalizedRules = rules.normalized()
  return entries.filter { TimeAccounting.dailyDate(it.createdAt, rules = normalizedRules) == today }.sumOf { it.deltaMinutes }
}

internal fun balanceRecordItems(
  focusRecords: List<FocusRecord>,
  leisureRecords: List<LeisureRecord>,
  ledger: List<LedgerEntry>,
  focusSourceIds: Set<String>,
  leisureSourceIds: Set<String>,
): List<BalanceRecordItem> {
  val focusItems = focusRecords.filter { it.deletedAt == null }.map { BalanceRecordItem.Focus(it) }
  val leisureItems = leisureRecords.filter { it.deletedAt == null }.map { BalanceRecordItem.Leisure(it) }
  val ledgerItems =
    ledger
      .filter { entry ->
        entry.sourceId == null ||
          entry.title.contains("Deleted", ignoreCase = true) ||
          entry.title.contains("Edited", ignoreCase = true) ||
          (entry.sourceId !in focusSourceIds && entry.sourceId !in leisureSourceIds)
      }
      .map { BalanceRecordItem.Adjustment(it) }
  return (focusItems + leisureItems + ledgerItems).sortedByDescending { it.occurredAt }
}

internal fun filteredBalanceRecordItems(
  records: List<BalanceRecordItem>,
  filter: BalanceRecordFilter,
): List<BalanceRecordItem> =
  records.filter {
    when (filter) {
      BalanceRecordFilter.All -> true
      BalanceRecordFilter.Focus -> it is BalanceRecordItem.Focus
      BalanceRecordFilter.Leisure -> it is BalanceRecordItem.Leisure
      BalanceRecordFilter.Adjustments -> it is BalanceRecordItem.Adjustment
    }
  }

internal fun localRecordTime(instant: Instant): String {
  val localTime = instant.toKotlinInstant().toLocalDateTime(TimeAccounting.focusWellTimeZone).time
  return "%02d:%02d".format(localTime.hour, localTime.minute)
}
