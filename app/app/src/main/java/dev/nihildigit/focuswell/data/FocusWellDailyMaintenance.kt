package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.domain.reserveLocked
import dev.nihildigit.focuswell.domain.savingsInterestMinutes
import java.time.Instant
import java.time.LocalDate

internal class FocusWellDailyMaintenance(
  private val now: () -> Instant,
) {
  fun ensureDailyGrants(state: FocusWellUiState): FocusWellUiState {
    val today = TimeAccounting.dailyDate(now(), rules = state.rules)
    val start = runCatching { LocalDate.parse(state.dailyDate) }.getOrDefault(today)
    val grantStart = if (start.isAfter(today)) today else start
    val existingIds = state.ledger.mapTo(mutableSetOf()) { it.id }
    val grants =
      generateSequence(grantStart) { date -> date.plusDays(1).takeIf { !it.isAfter(today) } }
        .filter { date -> FocusWellIds.dailyGrant(date) !in existingIds }
        .map { date ->
          LedgerEntry(
            id = FocusWellIds.dailyGrant(date),
            title = "Daily grant",
            deltaMinutes = state.rules.safeDailyGrantMinutes,
            createdAt = dailyGrantInstant(date, state.rules),
          )
        }
        .toList()
    if (grants.isEmpty()) return state
    return state.copy(
      reserveMinutes = state.reserveMinutes + grants.sumOf { it.deltaMinutes },
      ledger = grants.asReversed() + state.ledger,
    )
  }

  fun settleDailyTrackers(state: FocusWellUiState): FocusWellUiState {
    val currentDate = runCatching { LocalDate.parse(state.dailyDate) }.getOrNull() ?: return state
    val today = TimeAccounting.dailyDate(now(), rules = state.rules)
    if (!currentDate.isBefore(today)) return state
    val existingIds = state.ledger.mapTo(mutableSetOf()) { it.id }
    val rewards =
      state.trackers
        .filter { it.archivedAt == null && it.completed && it.rewardMinutes > 0.0 }
        .filter { FocusWellIds.trackerReward(currentDate, it.id) !in existingIds }
        .map {
          LedgerEntry(
            id = FocusWellIds.trackerReward(currentDate, it.id),
            title = "Daily tracker",
            deltaMinutes = it.rewardMinutes,
            createdAt = dailyGrantInstant(currentDate.plusDays(1), state.rules),
            note = it.label,
            sourceId = it.id,
          )
        }
    if (rewards.isEmpty()) return state
    return state.copy(
      reserveMinutes = state.reserveMinutes + rewards.sumOf { it.deltaMinutes },
      ledger = rewards.asReversed() + state.ledger,
    )
  }

  fun settleDailyInterest(state: FocusWellUiState): FocusWellUiState {
    val currentDate = runCatching { LocalDate.parse(state.dailyDate) }.getOrNull() ?: return state
    val today = TimeAccounting.dailyDate(now(), rules = state.rules)
    if (!currentDate.isBefore(today)) return state
    if (state.reserveLocked) return state

    val normalizedRules = state.rules.normalized()
    val existingIds = state.ledger.mapTo(mutableSetOf()) { it.id }
    var runningBalance = state.ledger.sumOf { it.deltaMinutes }.coerceAtLeast(0.0)
    val interestEntries = mutableListOf<LedgerEntry>()
    generateSequence(currentDate.plusDays(1)) { date -> date.plusDays(1).takeIf { !it.isAfter(today) } }
      .forEach { date ->
        val id = FocusWellIds.dailyInterest(date)
        if (id !in existingIds) {
          val interest = savingsInterestMinutes(runningBalance)
          if (interest > 0.0) {
            interestEntries +=
              LedgerEntry(
                id = id,
                title = "Savings interest",
                deltaMinutes = interest,
                createdAt = dailyGrantInstant(date, normalizedRules),
                note = "5/8/12% daily savings interest",
              )
            runningBalance += interest
          }
        }
      }
    if (interestEntries.isEmpty()) return state
    return state.copy(
      reserveMinutes = state.reserveMinutes + interestEntries.sumOf { it.deltaMinutes },
      ledger = interestEntries.asReversed() + state.ledger,
    )
  }

  fun rollDailyState(state: FocusWellUiState): FocusWellUiState {
    val today = TimeAccounting.dailyDate(now(), rules = state.rules).toString()
    if (state.dailyDate == today) return state
    return state.copy(
      dailyDate = today,
      trackers =
        state.trackers.map {
          if (it.ruleTagName == null) {
            it.copy(completed = false)
          } else {
            it.copy(completed = false, progressLabel = "0m / ${it.ruleTargetMinutes?.roundTarget() ?: ""}")
          }
        },
    )
  }

  private fun dailyGrantInstant(date: LocalDate, rules: FocusWellRules = FocusWellRules()): Instant =
    TimeAccounting.businessDayBoundaryInstant(date.toString(), rules = rules)

}
