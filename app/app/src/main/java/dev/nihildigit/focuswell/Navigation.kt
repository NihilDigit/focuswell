package dev.nihildigit.focuswell

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.nihildigit.focuswell.theme.ThemeMode
import dev.nihildigit.focuswell.ui.main.MainScreen

@Composable
fun MainNavigation(
  themeMode: ThemeMode,
  onThemeModeChange: (ThemeMode) -> Unit,
) {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(themeMode = themeMode, onThemeModeChange = onThemeModeChange)
        }
      },
  )
}
