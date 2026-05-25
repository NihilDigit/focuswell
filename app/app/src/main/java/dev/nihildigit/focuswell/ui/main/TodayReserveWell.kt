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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.FilterChip
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
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
internal fun ReserveHeader(reserveMinutes: Double, todayNetMovement: Double) {
  val fillTarget = (reserveMinutes / 180.0).coerceIn(0.08, 1.0).toFloat()
  val fill by animateFloatAsState(
    targetValue = fillTarget,
    animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
    label = "well-fill",
  )
  val shimmerTarget = if (reserveMinutes > 0.0) 1f else 0f
  val shimmer by animateFloatAsState(
    targetValue = shimmerTarget,
    animationSpec = tween(durationMillis = 300),
    label = "well-shimmer",
  )
  val wavePhase by rememberInfiniteTransition(label = "well-wave").animateFloat(
    initialValue = 0f,
    targetValue = (PI * 2).toFloat(),
    animationSpec = infiniteRepeatable(animation = tween(durationMillis = 3600, easing = LinearEasing)),
    label = "well-wave-phase",
  )
  val headline =
    when {
      reserveMinutes < 30 -> "Low reserve"
      reserveMinutes < 60 -> "${compactMinutes(reserveMinutes)} left"
      reserveMinutes <= 300 -> "${(reserveMinutes / 60.0).formatOne()} h banked"
      else -> "Enough for tonight"
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
        phase = wavePhase,
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

@Composable
internal fun ReserveWellDrawing(
  fill: Float,
  phase: Float,
  shimmer: Float,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  Canvas(modifier = modifier) {
    val wallColor = colorScheme.onPrimaryContainer
    val rippleColor = colorScheme.primary
    val glintColor = colorScheme.secondary
    val glyphWidth = 150.dp.toPx()
    val left = size.width - 118.dp.toPx()
    val top = size.height - 82.dp.toPx()
    val right = left + glyphWidth
    val rim =
      Rect(
        left + 8.dp.toPx(),
        top + 10.dp.toPx(),
        right - 8.dp.toPx(),
        top + 72.dp.toPx(),
      )
    val waterCenterY = rim.center.y + (0.5f - fill) * 20.dp.toPx()
    val waterAlpha = 0.46f + fill * 0.28f
    fun disturbancePath(
      centerXRatio: Float,
      centerYOffset: Float,
      width: Float,
      amplitude: Float,
      cycles: Float,
      phaseOffset: Float,
      steps: Int,
    ): Path {
      val centerY = waterCenterY + centerYOffset
      val rx = rim.width / 2f - 24.dp.toPx()
      val ry = rim.height / 2f - 11.dp.toPx()
      val normalizedY = ((centerY - rim.center.y) / ry).coerceIn(-0.84f, 0.84f)
      val halfChord = (rx * sqrt((1f - normalizedY * normalizedY).coerceAtLeast(0f))).coerceAtLeast(width / 2f)
      val desiredCenter = rim.center.x + (centerXRatio - 0.5f) * halfChord * 2f
      val centerX = desiredCenter.coerceIn(rim.center.x - halfChord + width / 2f, rim.center.x + halfChord - width / 2f)
      val startX = centerX - width / 2f
      return Path().apply {
        repeat(steps + 1) { index ->
          val t = index / steps.toFloat()
          val edgeFade = sin((PI * t).toFloat()).coerceAtLeast(0f)
          val localPhase = (PI * 2).toFloat() * cycles * t + phase * 0.9f + phaseOffset
          val crest =
            (sin(localPhase) * 0.82f + sin(localPhase * 1.9f + phaseOffset) * 0.18f) *
              amplitude *
              edgeFade
          val x = startX + width * t
          val y = centerY + crest
          if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
      }
    }
    val disturbances =
      listOf(
        disturbancePath(
          centerXRatio = 0.3f,
          centerYOffset = 1.dp.toPx(),
          width = 34.dp.toPx(),
          amplitude = 3.1.dp.toPx(),
          cycles = 1.85f,
          phaseOffset = 0.3f,
          steps = 22,
        ) to waterAlpha,
        disturbancePath(
          centerXRatio = 0.56f,
          centerYOffset = -5.dp.toPx(),
          width = 24.dp.toPx(),
          amplitude = 1.6.dp.toPx(),
          cycles = 1.4f,
          phaseOffset = 2.1f,
          steps = 16,
        ) to (0.22f * shimmer),
        disturbancePath(
          centerXRatio = 0.72f,
          centerYOffset = 4.dp.toPx(),
          width = 30.dp.toPx(),
          amplitude = 2.6.dp.toPx(),
          cycles = 1.7f,
          phaseOffset = 4.0f,
          steps = 20,
        ) to (waterAlpha * 0.9f),
        disturbancePath(
          centerXRatio = 0.48f,
          centerYOffset = 7.dp.toPx(),
          width = 22.dp.toPx(),
          amplitude = 1.25.dp.toPx(),
          cycles = 1.2f,
          phaseOffset = 5.4f,
          steps = 14,
        ) to (waterAlpha * 0.46f),
      )

    drawArc(
      color = wallColor.copy(alpha = 0.34f),
      startAngle = 188f,
      sweepAngle = 238f,
      useCenter = false,
      topLeft = Offset(rim.left, rim.top),
      size = Size(rim.width, rim.height),
      style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
    )
    drawArc(
      color = wallColor.copy(alpha = 0.22f),
      startAngle = 12f,
      sweepAngle = 128f,
      useCenter = false,
      topLeft = Offset(rim.left + 2.dp.toPx(), rim.top + 1.dp.toPx()),
      size = Size(rim.width - 4.dp.toPx(), rim.height - 2.dp.toPx()),
      style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
    )
    drawArc(
      color = wallColor.copy(alpha = 0.2f),
      startAngle = 196f,
      sweepAngle = 118f,
      useCenter = false,
      topLeft = Offset(rim.left + 16.dp.toPx(), rim.top + 11.dp.toPx()),
      size = Size(rim.width - 32.dp.toPx(), rim.height - 20.dp.toPx()),
      style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
    )
    drawArc(
      color = wallColor.copy(alpha = 0.16f),
      startAngle = 334f,
      sweepAngle = 78f,
      useCenter = false,
      topLeft = Offset(rim.left + 17.dp.toPx(), rim.top + 11.dp.toPx()),
      size = Size(rim.width - 34.dp.toPx(), rim.height - 22.dp.toPx()),
      style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
    )
    disturbances.forEachIndexed { index, (path, alpha) ->
      drawPath(
        path = path,
        color = if (index == 1) glintColor.copy(alpha = alpha * 0.75f) else rippleColor.copy(alpha = alpha),
        style = Stroke(width = if (index == 1) 1.7.dp.toPx() else 3.dp.toPx(), cap = StrokeCap.Round),
      )
    }
  }
}
