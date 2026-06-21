package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.domain.focusOutcomeMultiplier
import java.time.Instant

internal fun FocusWellUiState.withDeletedFocusRecord(
  id: String,
  deletedAt: Instant,
): FocusWellUiState {
  val record = focusRecords.firstOrNull { it.id == id && it.deletedAt == null } ?: return this
  val adjustment =
    LedgerEntry(
      id = FocusWellIds.deleteAdjustment(id, deletedAt),
      title = "Deleted focus",
      deltaMinutes = -record.earnedMinutes,
      createdAt = deletedAt,
      note = record.task,
      sourceId = id,
    )
  return copy(
    reserveMinutes = (reserveMinutes - record.earnedMinutes).coerceAtLeast(0.0),
    focusRecords = focusRecords.map { if (it.id == id) it.copy(deletedAt = deletedAt) else it },
    ledger = listOf(adjustment) + ledger,
  )
}

internal fun FocusWellUiState.withUpdatedFocusRecord(
  id: String,
  result: String,
  activeMinutes: Double,
  updatedAt: Instant,
): FocusWellUiState {
  val record = focusRecords.firstOrNull { it.id == id && it.deletedAt == null } ?: return this
  val safeMinutes = activeMinutes.coerceAtLeast(0.0)
  val savedResult = result.ifBlank { record.result }
  val newEarned =
    TimeAccounting.focusEarnedMinutes(
      activeDurationMinutes = safeMinutes,
      typeRate = record.typeRate,
      tagMultiplier = record.tagMultiplier,
      outcomeMultiplier = focusOutcomeMultiplier(savedResult),
    )
  val delta = newEarned - record.earnedMinutes
  val updated =
    record.copy(
      result = savedResult,
      activeDurationMinutes = safeMinutes,
      earnedMinutes = newEarned,
    )
  val adjustment =
    LedgerEntry(
      id = FocusWellIds.editAdjustment(id, updatedAt),
      title = "Edited focus",
      deltaMinutes = delta,
      createdAt = updatedAt,
      note = "Original ${record.earnedMinutes.roundMinutes()} -> ${newEarned.roundMinutes()}",
      sourceId = id,
    )
  return copy(
    reserveMinutes = (reserveMinutes + delta).coerceAtLeast(0.0),
    focusRecords = focusRecords.map { if (it.id == id) updated else it },
    ledger = if (delta == 0.0) ledger else listOf(adjustment) + ledger,
  )
}

internal fun FocusWellUiState.withDeletedLeisureRecord(
  id: String,
  deletedAt: Instant,
): FocusWellUiState {
  val record = leisureRecords.firstOrNull { it.id == id && it.deletedAt == null } ?: return this
  val adjustment =
    LedgerEntry(
      id = FocusWellIds.deleteAdjustment(id, deletedAt),
      title = "Deleted leisure",
      deltaMinutes = record.costMinutes,
      createdAt = deletedAt,
      sourceId = id,
    )
  return copy(
    reserveMinutes = reserveMinutes + record.costMinutes,
    leisureRecords = leisureRecords.map { if (it.id == id) it.copy(deletedAt = deletedAt) else it },
    ledger = listOf(adjustment) + ledger,
  )
}

internal fun FocusWellUiState.withAddedManualAdjustment(
  title: String,
  deltaMinutes: Double,
  note: String?,
  tagName: String?,
  createdAt: Instant,
): FocusWellUiState {
  val trimmedTitle = title.trim().ifBlank { "Manual adjustment" }
  val trimmedTagName = tagName?.trim()?.ifBlank { null }
  val safeDelta =
    if (deltaMinutes < 0.0) {
      deltaMinutes.coerceAtLeast(-reserveMinutes)
    } else {
      deltaMinutes
    }
  if (safeDelta == 0.0) return this
  val adjustment =
    LedgerEntry(
      id = FocusWellIds.manualAdjustment(createdAt),
      title = trimmedTitle,
      deltaMinutes = safeDelta,
      createdAt = createdAt,
      note = note?.trim()?.ifBlank { null },
      tagName = trimmedTagName,
    )
  return copy(
    reserveMinutes = (reserveMinutes + safeDelta).coerceAtLeast(0.0),
    ledger = listOf(adjustment) + ledger,
  )
}
