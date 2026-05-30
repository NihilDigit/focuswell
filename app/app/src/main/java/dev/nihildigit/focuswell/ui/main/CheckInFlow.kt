package dev.nihildigit.focuswell.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.FocusWellUiState
import java.time.Instant

private enum class CheckInStep {
  Income,
  Correction,
  Settlement,
}

@Composable
internal fun MorningCheckInGate(
  state: MorningCheckInUiState,
  appState: FocusWellUiState,
  onComplete: (Set<String>) -> Unit,
  modifier: Modifier = Modifier,
) {
  var step by remember(state.dailyDate) { mutableStateOf(CheckInStep.Income) }
  var fairUseIds by remember(state.dailyDate, state.segments) { mutableStateOf(emptySet<String>()) }
  val incomeItems = remember(appState.dailyDate, appState.ledger, state.startedAt) { checkInIncomeItems(appState, state.startedAt ?: Instant.now()) }
  val settlement =
    remember(state.segments, fairUseIds, appState.reserveMinutes, incomeItems) {
      checkInSettlementSummary(
        segments = state.segments,
        fairUseIds = fairUseIds,
        availableMinutes = appState.reserveMinutes + incomeItems.filter { it.label == "Wake bonus" }.sumOf { it.minutes },
      )
    }
  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = modifier.fillMaxSize(),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      AnimatedContent(
        targetState = step,
        transitionSpec = {
          fadeIn(animationSpec = tween(durationMillis = 140, delayMillis = 40)) togetherWith
            fadeOut(animationSpec = tween(durationMillis = 90))
        },
        label = "morning-check-in-step",
        modifier = Modifier.fillMaxSize(),
      ) {
        when (it) {
          CheckInStep.Income ->
            CheckInIncomeScreen(
              incomeItems = incomeItems,
              onContinue = { step = CheckInStep.Correction },
            )

          CheckInStep.Correction ->
            CheckInCorrectionScreen(
              state = state,
              fairUseIds = fairUseIds,
              phoneCost = settlement.phoneCost,
              onToggleFairUse = { segmentId ->
                fairUseIds = if (segmentId in fairUseIds) fairUseIds - segmentId else fairUseIds + segmentId
              },
              onContinue = { step = CheckInStep.Settlement },
            )

          CheckInStep.Settlement ->
            CheckInSettlementScreen(
              incomeMinutes = incomeItems.sumOf { item -> item.minutes },
              settlement = settlement,
              rules = appState.rules,
              showIncome = true,
              onBack = { step = CheckInStep.Correction },
              onDone = { onComplete(fairUseIds) },
            )
        }
      }
    }
  }
}

@Composable
internal fun PhoneUsageSettlementGate(
  state: MorningCheckInUiState,
  appState: FocusWellUiState,
  onCancel: () -> Unit,
  onComplete: (Set<String>) -> Unit,
  modifier: Modifier = Modifier,
) {
  var step by remember(state.startedAt) { mutableStateOf(CheckInStep.Correction) }
  var fairUseIds by remember(state.startedAt, state.segments) { mutableStateOf(emptySet<String>()) }
  val settlement =
    remember(state.segments, fairUseIds, appState.reserveMinutes) {
      checkInSettlementSummary(
        segments = state.segments,
        fairUseIds = fairUseIds,
        availableMinutes = appState.reserveMinutes,
      )
    }
  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = modifier.fillMaxSize(),
  ) {
    AnimatedContent(
      targetState = step,
      transitionSpec = {
        fadeIn(animationSpec = tween(durationMillis = 140, delayMillis = 40)) togetherWith
          fadeOut(animationSpec = tween(durationMillis = 90))
      },
      label = "phone-settlement-step",
      modifier = Modifier.fillMaxSize(),
    ) {
      when (it) {
        CheckInStep.Income,
        CheckInStep.Correction ->
          CheckInCorrectionScreen(
            state = state,
            fairUseIds = fairUseIds,
            phoneCost = settlement.phoneCost,
            onToggleFairUse = { segmentId ->
              fairUseIds = if (segmentId in fairUseIds) fairUseIds - segmentId else fairUseIds + segmentId
            },
            onContinue = { step = CheckInStep.Settlement },
            onCancel = onCancel,
            title = "Phone use",
            subtitle = "Settle recent blocks now.",
          )

        CheckInStep.Settlement ->
          CheckInSettlementScreen(
            incomeMinutes = 0.0,
            settlement = settlement,
            rules = appState.rules,
            showIncome = false,
            onBack = { step = CheckInStep.Correction },
            onDone = { onComplete(fairUseIds) },
          )
      }
    }
  }
}

@Composable
private fun CheckInIncomeScreen(
  incomeItems: List<CheckInIncomeItem>,
  onContinue: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    CheckInStepHeader(
      title = "Income",
      subtitle = "Completed rewards are ready.",
    )
    Box(
      modifier = Modifier.fillMaxWidth().weight(1f),
      contentAlignment = Alignment.Center,
    ) {
      val displayCount = incomeItems.size.coerceAtLeast(1)
      val itemSpacing = 14.dp
      val estimatedItemHeight = 74.dp
      val contentHeight = estimatedItemHeight * displayCount + itemSpacing * (displayCount - 1)
      Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = contentHeight),
        verticalArrangement = Arrangement.spacedBy(itemSpacing, Alignment.CenterVertically),
      ) {
        if (incomeItems.isEmpty()) {
          CalmPanel {
            Text("No rewards to settle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Continue to phone correction.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        } else {
          incomeItems.forEachIndexed { index, item ->
            AnimatedAccountingItem(
              label = item.label,
              amount = item.minutes,
              tone = MaterialTheme.colorScheme.primary,
              delayMillis = index * 170,
            )
          }
        }
      }
    }
    Button(
      onClick = onContinue,
      modifier = Modifier.fillMaxWidth().height(54.dp),
      shape = ControlEndShape,
    ) {
      Text("Continue")
    }
  }
}

@Composable
private fun CheckInCorrectionScreen(
  state: MorningCheckInUiState,
  fairUseIds: Set<String>,
  phoneCost: Double,
  onToggleFairUse: (String) -> Unit,
  onContinue: () -> Unit,
  onCancel: (() -> Unit)? = null,
  title: String = "Correction",
  subtitle: String = "Swipe right for Fair Use.",
) {
  val segments = remember(state.segments) { state.segments.sortedBy { it.startedAt } }
  var currentIndex by remember(segments) { mutableStateOf(0) }
  var reviewHistory by remember(segments) { mutableStateOf(emptyList<Pair<String, Boolean>>()) }
  val currentSegment = segments.getOrNull(currentIndex)
  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top,
    ) {
      CheckInStepHeader(
        title = title,
        subtitle = subtitle,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (reviewHistory.isNotEmpty()) {
          TextButton(
            onClick = {
              val last = reviewHistory.last()
              reviewHistory = reviewHistory.dropLast(1)
              currentIndex = (currentIndex - 1).coerceAtLeast(0)
              if (last.second && last.first in fairUseIds) {
                onToggleFairUse(last.first)
              }
            },
          ) {
            Text("Undo")
          }
        }
        if (onCancel != null) {
          TextButton(onClick = onCancel) {
            Text("Cancel")
          }
        }
      }
    }
    if (state.loading) {
      Box(modifier = Modifier.weight(1f)) {
        CalmPanel {
          Text("Reading local usage events", style = MaterialTheme.typography.titleMedium)
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
      }
    } else if (state.segments.isEmpty()) {
      Box(modifier = Modifier.weight(1f)) {
        CalmPanel {
          Text("No phone blocks found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          Text("No non-Focus/Leisure phone block reached the review threshold.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    } else {
      Box(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentAlignment = Alignment.Center,
      ) {
        if (currentSegment == null) {
          CalmPanel {
            Text("All blocks reviewed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${fairUseIds.size} marked Fair Use", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        } else {
          PhoneUsageSegmentCard(
            segment = currentSegment,
            index = currentIndex,
            total = segments.size,
            onCount = {
              reviewHistory = reviewHistory + (currentSegment.id to false)
              currentIndex += 1
            },
            onFairUse = {
              if (currentSegment.id !in fairUseIds) {
                onToggleFairUse(currentSegment.id)
              }
              reviewHistory = reviewHistory + (currentSegment.id to true)
              currentIndex += 1
            },
          )
        }
      }
    }
    Button(
      onClick = onContinue,
      enabled = !state.loading && (segments.isEmpty() || currentSegment == null),
      modifier = Modifier.fillMaxWidth().height(54.dp),
      shape = ControlEndShape,
    ) {
      Text("Continue")
    }
  }
}
