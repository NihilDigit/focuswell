package dev.nihildigit.focuswell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.nihildigit.focuswell.notifications.ensureNotificationChannel
import dev.nihildigit.focuswell.theme.FocusWellTheme
import dev.nihildigit.focuswell.theme.loadThemeMode
import dev.nihildigit.focuswell.theme.saveThemeMode

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ensureNotificationChannel(this)

    enableEdgeToEdge()
    setContent {
      var themeMode by remember { mutableStateOf(loadThemeMode(this@MainActivity)) }
      val systemDarkTheme = isSystemInDarkTheme()
      FocusWellTheme(darkTheme = themeMode.resolveDarkTheme(systemDarkTheme), dynamicColor = true) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(
            themeMode = themeMode,
            onThemeModeChange = { mode ->
              themeMode = mode
              saveThemeMode(this@MainActivity, mode)
            },
          )
        }
      }
    }
  }
}
