package dev.nihildigit.focuswell.ui.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.DailyTracker
import dev.nihildigit.focuswell.domain.Destination
import dev.nihildigit.focuswell.domain.FocusWellUiState
import dev.nihildigit.focuswell.domain.FocusWellRules
import dev.nihildigit.focuswell.domain.FocusRecord
import dev.nihildigit.focuswell.domain.LedgerEntry
import dev.nihildigit.focuswell.domain.LeisureRecord
import dev.nihildigit.focuswell.domain.SessionType
import dev.nihildigit.focuswell.domain.TagConfig
import dev.nihildigit.focuswell.domain.TimeAccounting
import dev.nihildigit.focuswell.notifications.postFocusWellNotification
import dev.nihildigit.focuswell.theme.FocusWellTheme
import dev.nihildigit.focuswell.theme.ThemeMode
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

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
      records.filter {
        when (filter) {
          BalanceRecordFilter.All -> true
          BalanceRecordFilter.Focus -> it is BalanceRecordItem.Focus
          BalanceRecordFilter.Leisure -> it is BalanceRecordItem.Leisure
          BalanceRecordFilter.Adjustments -> it is BalanceRecordItem.Adjustment
        }
      }
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

@Composable
internal fun NetBalanceChart(entries: List<LedgerEntry>, rules: FocusWellRules) {
  val points = remember(entries, rules) { sevenDayNetPoints(entries, rules) }
  val total = points.sumOf { it.netMinutes }
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = CalmPanelShape,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text("7-day net", style = MaterialTheme.typography.titleLarge)
          Text("Daily net movement", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
          signedMinutes(total),
          style = tabularNumbers(MaterialTheme.typography.titleMedium),
          color =
            when {
              total > 0.0 -> MaterialTheme.colorScheme.primary
              total < 0.0 -> MaterialTheme.colorScheme.tertiary
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
      }
      NetLineChart(points = points)
    }
  }
}

@Composable
internal fun NetLineChart(points: List<DailyNetPoint>, modifier: Modifier = Modifier) {
  val primary = MaterialTheme.colorScheme.primary
  val tertiary = MaterialTheme.colorScheme.tertiary
  val outline = MaterialTheme.colorScheme.outlineVariant
  val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
  Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
    val maxAbs = points.maxOfOrNull { kotlin.math.abs(it.netMinutes) }?.coerceAtLeast(15.0) ?: 15.0
    val axisMax = (ceil(maxAbs / 15.0) * 15.0).coerceAtLeast(15.0)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Column(
        modifier = Modifier.width(46.dp).height(146.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
      ) {
        Text(signedMinutes(axisMax), style = tabularNumbers(MaterialTheme.typography.labelSmall), color = onSurfaceVariant)
        Text("0", style = tabularNumbers(MaterialTheme.typography.labelSmall), color = onSurfaceVariant)
        Text(signedMinutes(-axisMax), style = tabularNumbers(MaterialTheme.typography.labelSmall), color = onSurfaceVariant)
      }
      Canvas(modifier = Modifier.weight(1f).height(146.dp)) {
        val horizontalPadding = 8.dp.toPx()
        val topPadding = 12.dp.toPx()
        val bottomPadding = 14.dp.toPx()
        val chartWidth = size.width - horizontalPadding * 2
        val chartHeight = size.height - topPadding - bottomPadding
        val zeroY = topPadding + chartHeight / 2f
        listOf(topPadding, zeroY, topPadding + chartHeight).forEach { y ->
          drawLine(
            color = outline,
            start = Offset(horizontalPadding, y),
            end = Offset(size.width - horizontalPadding, y),
            strokeWidth = 1.dp.toPx(),
          )
        }
        val offsets =
          points.mapIndexed { index, point ->
            val x = horizontalPadding + chartWidth * index / (points.size - 1).coerceAtLeast(1)
            val y = zeroY - (point.netMinutes / axisMax).toFloat() * chartHeight / 2f
            Offset(x, y)
          }
        if (offsets.size > 1) {
          val path = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { lineTo(it.x, it.y) }
          }
          drawPath(path, color = primary, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        }
        offsets.forEachIndexed { index, offset ->
          val point = points[index]
          val color = if (point.netMinutes < 0.0) tertiary else primary
          drawCircle(color = color.copy(alpha = 0.18f), radius = 8.dp.toPx(), center = offset)
          drawCircle(color = color, radius = 4.dp.toPx(), center = offset)
        }
      }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Spacer(Modifier.width(54.dp))
      points.forEach {
        Text(
          it.label,
          style = MaterialTheme.typography.labelSmall,
          color = onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
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
    border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = modifier.height(42.dp),
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
      Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
  }
}

@Composable
internal fun BalanceRecordRow(
  item: BalanceRecordItem,
  onEditFocusRecord: (String) -> Unit,
  onShowLeisureRecord: (String) -> Unit,
) {
  when (item) {
    is BalanceRecordItem.Focus ->
      BalanceFocusRecordRow(
        record = item.record,
        onClick = { onEditFocusRecord(item.record.id) },
      )
    is BalanceRecordItem.Leisure ->
      BalanceLeisureRecordRow(record = item.record, onClick = { onShowLeisureRecord(item.record.id) })
    is BalanceRecordItem.Adjustment -> BalanceAdjustmentRow(entry = item.entry)
  }
}

@Composable
internal fun BalanceFocusRecordRow(
  record: FocusRecord,
  onClick: () -> Unit,
) {
  val resultParts = remember(record.id) { parseOutcomeResult(record.result) }
  BalanceRecordSurface(onClick = onClick) {
    BalanceDeltaText(delta = record.earnedMinutes, color = MaterialTheme.colorScheme.primary)
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        InlineRecordTypeIcon(icon = Icons.Rounded.Timer, color = MaterialTheme.colorScheme.primary)
        Text(record.task, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Text(
        "${record.type.label} · ${record.tagName ?: "Untagged"} · ${record.activeDurationMinutes.roundToInt()} min",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      FocusOutcomeMark(outcome = resultParts.first, note = resultParts.second)
    }
    Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BalanceFocusRecordSheet(
  record: FocusRecord,
  onDismiss: () -> Unit,
  onDelete: () -> Unit,
  onUpdate: (String, Double) -> Unit,
) {
  val resultParts = remember(record.id) { parseOutcomeResult(record.result) }
  var outcome by remember(record.id) { mutableStateOf(resultParts.first) }
  var note by remember(record.id) { mutableStateOf(resultParts.second) }
  var minutes by remember(record.id) { mutableStateOf(record.activeDurationMinutes.roundToInt().toString()) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  FocusRecordEditSheet(
    record = record,
    outcome = outcome,
    note = note,
    minutes = minutes,
    onOutcomeChange = { outcome = it },
    onNoteChange = { note = it },
    onMinutesChange = { minutes = it },
    sheetState = sheetState,
    onDismiss = onDismiss,
    onSave = { onUpdate(formatOutcomeResult(outcome, note), minutes.toDoubleOrNull() ?: record.activeDurationMinutes) },
    onDelete = onDelete,
  )
}

@Composable
internal fun FocusOutcomeMark(outcome: String, note: String) {
  val (icon, color) = focusOutcomeVisual(outcome)
  Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
    Text(
      if (note.isBlank()) outcome else "$outcome · $note",
      style = MaterialTheme.typography.labelMedium,
      color = color,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BalanceLeisureRecordRow(record: LeisureRecord, onClick: () -> Unit) {
  BalanceRecordSurface(onClick = onClick) {
    BalanceDeltaText(delta = -record.costMinutes, color = MaterialTheme.colorScheme.tertiary)
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        InlineRecordTypeIcon(icon = Icons.Rounded.Bedtime, color = MaterialTheme.colorScheme.tertiary)
        Text("Leisure", style = MaterialTheme.typography.titleMedium)
      }
      Text(
        "${record.elapsedMinutes.roundToInt()} real min · ${record.costMinutes.roundToInt()} charged",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(localRecordTime(record.endedAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BalanceLeisureRecordSheet(
  record: LeisureRecord,
  onDismiss: () -> Unit,
  onDelete: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("Leisure record", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      BalanceDeltaPreview(original = 0.0, updated = -record.costMinutes, delta = -record.costMinutes)
      Button(
        onClick = onDelete,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = ControlEndShape,
      ) {
        Icon(Icons.Rounded.Delete, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Delete record")
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
internal fun BalanceAdjustmentRow(entry: LedgerEntry) {
  BalanceRecordSurface(onClick = {}, enabled = false) {
    BalanceDeltaText(
      delta = entry.deltaMinutes,
      color =
        when {
          entry.deltaMinutes > 0.0 -> MaterialTheme.colorScheme.primary
          entry.deltaMinutes < 0.0 -> MaterialTheme.colorScheme.tertiary
          else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        InlineRecordTypeIcon(icon = Icons.Rounded.Edit, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(entry.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      entry.note?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Text(localRecordTime(entry.createdAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
internal fun InlineRecordTypeIcon(icon: ImageVector, color: Color) {
  Surface(
    color = color.copy(alpha = 0.14f),
    contentColor = color,
    shape = CircleShape,
    modifier = Modifier.size(24.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
    }
  }
}

@Composable
internal fun BalanceRecordSurface(
  onClick: () -> Unit,
  enabled: Boolean = true,
  content: @Composable RowScope.() -> Unit,
) {
  Surface(
    onClick = onClick,
    enabled = enabled,
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = LedgerRowShape,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      content = content,
    )
  }
}

@Composable
internal fun BalanceDeltaText(delta: Double, color: Color) {
  Text(
    signedMinutes(delta),
    style = tabularNumbers(MaterialTheme.typography.titleMedium),
    fontWeight = FontWeight.Bold,
    color = color,
    modifier = Modifier.width(76.dp),
  )
}

internal data class DailyNetPoint(
  val date: java.time.LocalDate,
  val label: String,
  val netMinutes: Double,
)

internal fun sevenDayNetPoints(entries: List<LedgerEntry>, rules: FocusWellRules): List<DailyNetPoint> {
  val normalizedRules = rules.normalized()
  val today = TimeAccounting.dailyDate(Instant.now(), rules = normalizedRules)
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

internal fun todayNetMovement(entries: List<LedgerEntry>, rules: FocusWellRules): Double {
  val normalizedRules = rules.normalized()
  val today = TimeAccounting.dailyDate(Instant.now(), rules = normalizedRules)
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

internal fun localRecordTime(instant: Instant): String {
  val local = instant.atZone(TimeAccounting.focusWellZone)
  return "%02d:%02d".format(local.hour, local.minute)
}
