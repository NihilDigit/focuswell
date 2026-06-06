package dev.nihildigit.focuswell.ui.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.savingsInterestRateLabel

@Composable
internal fun ReserveHeader(reserveMinutes: Double, todayNetMovement: Double, reserveLocked: Boolean) {
  val fillTarget = (reserveMinutes / 360.0).coerceIn(0.08, 1.0).toFloat()
  val fill by animateFloatAsState(
    targetValue = fillTarget,
    animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
    label = "well-fill",
  )
  val shimmerTarget = if (reserveMinutes > 0.0 && reserveMinutes <= 300.0) 1f else 0f
  val shimmer by animateFloatAsState(
    targetValue = shimmerTarget,
    animationSpec = tween(durationMillis = 300),
    label = "well-shimmer",
  )
  val headline =
    when {
      reserveMinutes < 30 -> "Low reserve"
      reserveMinutes < 60 -> "${compactMinutes(reserveMinutes)} left"
      reserveMinutes <= 300 -> "${(reserveMinutes / 60.0).formatOne()} h banked"
      else -> "${(reserveMinutes / 60.0).formatOne()} h saved"
    }
  Surface(
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    shape = TodayHeroShape,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(modifier = Modifier.height(254.dp)) {
      ReserveWellDrawing(
        fill = fill,
        phase = 0f,
        shimmer = shimmer,
        modifier = Modifier.matchParentSize(),
      )
      Column(
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier =
          Modifier
            .align(Alignment.CenterStart)
            .padding(start = 22.dp, top = 22.dp, end = 94.dp, bottom = 22.dp),
      ) {
        Text("Leisure well", style = MaterialTheme.typography.labelLarge)
        Text(headline, style = MaterialTheme.typography.headlineLarge)
        Text(
          if (reserveLocked) "Locked: save only" else savingsInterestRateLabel(),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        shape = CircleShape,
        modifier = Modifier.align(Alignment.TopEnd).padding(top = 28.dp, end = 22.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Today", style = MaterialTheme.typography.labelMedium)
          Text(
            signedMinutes(todayNetMovement),
            style = tabularNumbers(MaterialTheme.typography.titleMedium),
            color =
              when {
                todayNetMovement > 0.0 -> MaterialTheme.colorScheme.primary
                todayNetMovement < 0.0 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onPrimaryContainer
              },
          )
        }
      }
    }
  }
}
