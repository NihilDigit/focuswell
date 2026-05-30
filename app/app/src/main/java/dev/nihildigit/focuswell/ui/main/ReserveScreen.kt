package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.FocusWellUiState

@Composable
internal fun ReserveScreen(
  state: FocusWellUiState,
  onDeleteFocusRecord: (String) -> Unit,
  onUpdateFocusRecord: (String, String, Double) -> Unit,
  onDeleteLeisureRecord: (String) -> Unit,
) {
  var filter by remember { mutableStateOf(BalanceRecordFilter.All) }
  var editingFocusRecordId by remember { mutableStateOf<String?>(null) }
  var showingLeisureRecordId by remember { mutableStateOf<String?>(null) }
  val focusSourceIds = state.focusRecords.mapTo(mutableSetOf()) { it.id }
  val leisureSourceIds = state.leisureRecords.mapTo(mutableSetOf()) { it.id }
  val records =
    remember(state.focusRecords, state.leisureRecords, state.ledger) {
      balanceRecordItems(
        focusRecords = state.focusRecords,
        leisureRecords = state.leisureRecords,
        ledger = state.ledger,
        focusSourceIds = focusSourceIds,
        leisureSourceIds = leisureSourceIds,
      )
    }
  val filteredRecords =
    remember(records, filter) {
      filteredBalanceRecordItems(records, filter)
    }
  LazyColumn(
    contentPadding = PaddingValues(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item { NetBalanceChart(entries = state.ledger, rules = state.rules) }
    item {
      RecordsFilterHeader(
        selected = filter,
        onSelected = { filter = it },
        totalCount = records.size,
      )
    }
    if (filteredRecords.isEmpty()) {
      item { EmptyRecordText("No matching records yet.") }
    } else {
      items(filteredRecords, key = { it.id }) { item ->
        BalanceRecordRow(
          item = item,
          onEditFocusRecord = { editingFocusRecordId = it },
          onShowLeisureRecord = { showingLeisureRecordId = it },
        )
      }
    }
  }
  state.focusRecords.firstOrNull { it.id == editingFocusRecordId && it.deletedAt == null }?.let { record ->
    BalanceFocusRecordSheet(
      record = record,
      onDismiss = { editingFocusRecordId = null },
      onDelete = {
        editingFocusRecordId = null
        onDeleteFocusRecord(record.id)
      },
      onUpdate = { result, minutes ->
        editingFocusRecordId = null
        onUpdateFocusRecord(record.id, result, minutes)
      },
    )
  }
  state.leisureRecords.firstOrNull { it.id == showingLeisureRecordId && it.deletedAt == null }?.let { record ->
    BalanceLeisureRecordSheet(
      record = record,
      onDismiss = { showingLeisureRecordId = null },
      onDelete = {
        showingLeisureRecordId = null
        onDeleteLeisureRecord(record.id)
      },
    )
  }
}
