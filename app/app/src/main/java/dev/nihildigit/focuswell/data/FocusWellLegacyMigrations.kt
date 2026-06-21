package dev.nihildigit.focuswell.data

import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.TimeAccounting
import java.time.Instant

internal data class LegacyTaggedManualLedgerEntry(
  val id: String,
  val title: String,
  val rawMinutes: Double,
  val createdAt: Instant,
  val note: String?,
  val tagName: String,
)

internal fun FocusWellUiState.withMigratedTaggedManualLedgerEntries(
  legacyEntries: List<LegacyTaggedManualLedgerEntry>,
): FocusWellUiState {
  if (legacyEntries.isEmpty()) return this
  var migrated = this
  legacyEntries.forEach { legacy ->
    val tag = tags.firstOrNull { it.name.equals(legacy.tagName, ignoreCase = true) } ?: return@forEach
    val recordId = FocusWellIds.manualFocus(legacy.createdAt)
    if (migrated.focusRecords.any { it.id == recordId } || migrated.ledger.any { it.id == FocusWellIds.ledger(recordId) }) {
      migrated = migrated.copy(ledger = migrated.ledger.filterNot { it.id == legacy.id })
      return@forEach
    }
    val activeMinutes = legacy.rawMinutes.coerceAtLeast(0.0)
    if (activeMinutes == 0.0) return@forEach
    val result = legacy.note?.trim()?.ifBlank { null }?.let { "As planned · $it" } ?: "As planned"
    val earned =
      TimeAccounting.focusEarnedMinutes(
        activeDurationMinutes = activeMinutes,
        typeRate = SessionType.Input.rate,
        tagMultiplier = tag.multiplier,
        outcomeMultiplier = 1.0,
      )
    val record =
      FocusRecord(
        id = recordId,
        task = legacy.title.ifBlank { "Manual focus" },
        result = result,
        type = SessionType.Input,
        tagName = tag.name,
        tagMultiplier = tag.multiplier,
        typeRate = SessionType.Input.rate,
        startedAt = legacy.createdAt.minusMillis((activeMinutes * 60_000.0).toLong().coerceAtLeast(0L)),
        endedAt = legacy.createdAt,
        activeDurationMinutes = activeMinutes,
        earnedMinutes = earned,
        dailyDate = TimeAccounting.dailyDate(legacy.createdAt, rules = migrated.rules).toString(),
      )
    val entry =
      LedgerEntry(
        id = FocusWellIds.ledger(record.id),
        title = "Focus · ${record.type.label} ${tag.name}",
        deltaMinutes = earned,
        createdAt = legacy.createdAt,
        note = record.result,
        sourceId = record.id,
      )
    migrated =
      migrated.copy(
        focusRecords = listOf(record) + migrated.focusRecords,
        ledger = listOf(entry) + migrated.ledger.filterNot { it.id == legacy.id },
      )
  }
  return migrated
}
