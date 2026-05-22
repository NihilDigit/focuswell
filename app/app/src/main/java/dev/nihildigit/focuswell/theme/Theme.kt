package dev.nihildigit.focuswell.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = Pine80,
    onPrimary = Pine20,
    primaryContainer = Color(0xFF1A4F35),
    onPrimaryContainer = Color(0xFFE6F6EA),
    secondary = Slate80,
    onSecondary = Slate20,
    secondaryContainer = Color(0xFF344253),
    onSecondaryContainer = Color(0xFFEAF1F8),
    tertiary = Amber80,
    onTertiary = Amber20,
    tertiaryContainer = Color(0xFF604100),
    onTertiaryContainer = Color(0xFFFFECC4),
    background = FocusBackgroundDark,
    onBackground = Color(0xFFE3E9E1),
    surface = FocusSurfaceDark,
    onSurface = Color(0xFFE3E9E1),
    surfaceVariant = FocusSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFC3CEC0),
    surfaceContainer = FocusSurfaceVariantDark,
    surfaceContainerHigh = Color(0xFF2F3B30),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Pine40,
    onPrimary = Color.White,
    secondary = Slate40,
    onSecondary = Color.White,
    tertiary = Amber40,
    onTertiary = Color.White,
    background = FocusBackgroundLight,
    onBackground = Color(0xFF182018),
    surface = FocusSurfaceLight,
    onSurface = Color(0xFF182018),
    surfaceVariant = FocusSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF4E5A4D),
    surfaceContainer = Color(0xFFEEF5EC),
    surfaceContainerHigh = Color(0xFFE4EEE2),
    primaryContainer = Color(0xFFCDEED9),
    onPrimaryContainer = Color(0xFF0D3A25),
    secondaryContainer = Color(0xFFDCE6F0),
    onSecondaryContainer = Color(0xFF273344),
    tertiaryContainer = Color(0xFFFFDFA3),
    onTertiaryContainer = Color(0xFF422D00),
  )

private val FocusShapes =
  Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
  )

@Composable
fun FocusWellTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, shapes = FocusShapes, content = content)
}
