package dev.nihildigit.focuswell.ui.main

import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.TimeAccounting
import java.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate as KotlinLocalDate
import kotlinx.datetime.minus

internal data class CheckInIncomeItem(
  val label: String,
  val minutes: Double,
)

internal fun checkInIncomeItems(state: FocusWellUiState, startedAt: Instant): List<CheckInIncomeItem> {
  val dailyDate = state.dailyDate
  val items = mutableListOf<CheckInIncomeItem>()
  state.ledger.firstOrNull { it.id == "daily-grant-$dailyDate" && it.deltaMinutes > 0.0 }?.let {
    items += CheckInIncomeItem("Daily grant", it.deltaMinutes)
  }
  state.ledger.firstOrNull { it.id == "daily-interest-$dailyDate" && it.deltaMinutes > 0.0 }?.let {
    items += CheckInIncomeItem("Savings interest", it.deltaMinutes)
  }
  val previousDate =
    runCatching { KotlinLocalDate.parse(dailyDate).minus(1, DateTimeUnit.DAY).toString() }
      .getOrNull()
  state.ledger
    .filter { it.title == "Daily tracker" && it.deltaMinutes > 0.0 && (previousDate == null || it.id.startsWith("tracker-reward-$previousDate-")) }
    .sortedBy { it.note ?: it.id }
    .forEach { items += CheckInIncomeItem(it.note ?: "Daily tracker", it.deltaMinutes) }
  if (TimeAccounting.isWakeBonusEligible(startedAt, rules = state.rules)) {
    items += CheckInIncomeItem("Wake bonus", 30.0)
  }
  return items
}
