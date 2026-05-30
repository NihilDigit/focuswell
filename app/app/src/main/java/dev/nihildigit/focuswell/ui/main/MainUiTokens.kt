package dev.nihildigit.focuswell.ui.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.ActiveMode
import dev.nihildigit.focuswell.domain.Destination
import dev.nihildigit.focuswell.domain.FocusOutcome

internal val TodayHeroShape = RoundedCornerShape(topStart = 42.dp, topEnd = 48.dp, bottomEnd = 30.dp, bottomStart = 46.dp)
internal val TodayPanelShape = RoundedCornerShape(topStart = 30.dp, topEnd = 42.dp, bottomEnd = 24.dp, bottomStart = 30.dp)
internal val ActiveTimerShape = RoundedCornerShape(topStart = 44.dp, topEnd = 32.dp, bottomEnd = 44.dp, bottomStart = 32.dp)
internal val FocusActionShape = RoundedCornerShape(topStart = 34.dp, topEnd = 20.dp, bottomEnd = 30.dp, bottomStart = 34.dp)
internal val LeisureActionShape = RoundedCornerShape(topStart = 20.dp, topEnd = 34.dp, bottomEnd = 34.dp, bottomStart = 30.dp)
internal val ControlStartShape = RoundedCornerShape(topStart = 26.dp, topEnd = 14.dp, bottomEnd = 22.dp, bottomStart = 26.dp)
internal val ControlEndShape = RoundedCornerShape(topStart = 14.dp, topEnd = 26.dp, bottomEnd = 26.dp, bottomStart = 22.dp)
internal val CalmPanelShape = RoundedCornerShape(16.dp)
internal val LedgerRowShape = RoundedCornerShape(14.dp)
internal val FocusOutcomeOptions = FocusOutcome.entries.map { it.label }

internal enum class ActiveModeMotionKey {
  Idle,
  Focus,
  Leisure,
  Depleted,
}

internal fun AnimatedContentTransitionScope<Destination>.destinationMotionTransform(): ContentTransform =
  fadeIn(animationSpec = tween(durationMillis = 140, delayMillis = 40)) togetherWith
    fadeOut(animationSpec = tween(durationMillis = 90))

internal fun AnimatedContentTransitionScope<ActiveModeMotionKey>.activeModeMotionTransform(): ContentTransform {
  val direction = if (activeModeOrder(targetState) >= activeModeOrder(initialState)) 1 else -1
  return (
    slideInHorizontally(animationSpec = focusWellDefaultSpatialSpec()) { width -> width * direction / 7 } +
      fadeIn(animationSpec = focusWellFastEffectsSpec())
    ) togetherWith (
      slideOutHorizontally(animationSpec = focusWellFastSpatialSpec()) { width -> -width * direction / 10 } +
        fadeOut(animationSpec = focusWellFastEffectsSpec())
      )
}

internal fun <T> focusWellFastSpatialSpec(): FiniteAnimationSpec<T> =
  spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy)

internal fun <T> focusWellDefaultSpatialSpec(): FiniteAnimationSpec<T> =
  spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)

internal fun <T> focusWellFastEffectsSpec(): FiniteAnimationSpec<T> =
  spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioNoBouncy)

internal fun destinationOrder(destination: Destination): Int {
  return when (destination) {
    Destination.Today -> 0
    Destination.Reserve -> 1
    Destination.Ideas -> 2
    Destination.Plan -> 3
    Destination.Settings -> 4
  }
}

internal fun activeModeMotionKey(mode: ActiveMode): ActiveModeMotionKey {
  return when (mode) {
    ActiveMode.None -> ActiveModeMotionKey.Idle
    is ActiveMode.Focus -> ActiveModeMotionKey.Focus
    is ActiveMode.Leisure -> ActiveModeMotionKey.Leisure
    ActiveMode.Depleted -> ActiveModeMotionKey.Depleted
  }
}

internal fun activeModeOrder(mode: ActiveModeMotionKey): Int {
  return when (mode) {
    ActiveModeMotionKey.Idle -> 0
    ActiveModeMotionKey.Focus -> 1
    ActiveModeMotionKey.Leisure -> 2
    ActiveModeMotionKey.Depleted -> 3
  }
}

internal fun tabularNumbers(style: TextStyle): TextStyle = style.copy(fontFeatureSettings = "tnum")
