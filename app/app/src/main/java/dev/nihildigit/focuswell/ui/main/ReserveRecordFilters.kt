package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

internal enum class BalanceRecordFilter(val label: String) {
  All("All"),
  Focus("Focus"),
  Leisure("Leisure"),
  Adjustments("Adjust"),
}

internal fun BalanceRecordFilter.icon(): ImageVector =
  when (this) {
    BalanceRecordFilter.All -> Icons.Rounded.AccountBalanceWallet
    BalanceRecordFilter.Focus -> Icons.Rounded.Timer
    BalanceRecordFilter.Leisure -> Icons.Rounded.Bedtime
    BalanceRecordFilter.Adjustments -> Icons.Rounded.Edit
  }

@Composable
internal fun RecordsFilterHeader(
  selected: BalanceRecordFilter,
  onSelected: (BalanceRecordFilter) -> Unit,
  totalCount: Int,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom,
    ) {
      SectionHeader(title = "Records", subtitle = "$totalCount ledger-backed items")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
      BalanceRecordFilter.entries.forEach { filter ->
        CompactFilterChip(
          icon = filter.icon(),
          contentDescription = filter.label,
          selected = selected == filter,
          onClick = { onSelected(filter) },
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
internal fun CompactFilterChip(
  icon: ImageVector,
  contentDescription: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
    contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    shape = CircleShape,
    border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = modifier.height(42.dp),
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
      Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
  }
}
