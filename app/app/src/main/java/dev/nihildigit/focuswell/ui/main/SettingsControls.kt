package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.theme.ThemeMode

@Composable
internal fun SettingsDataActionRow(
  title: String,
  supporting: String,
  icon: ImageVector,
  actionLabel: String,
  onClick: () -> Unit,
  destructive: Boolean = false,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        color =
          if (destructive) MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
          else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f),
        contentColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
        shape = CircleShape,
      ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp))
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      if (destructive) {
        OutlinedButton(
          onClick = onClick,
          modifier = Modifier.height(44.dp),
          shape = RoundedCornerShape(22.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
          Text(actionLabel)
        }
      } else {
        FilledTonalButton(onClick = onClick, modifier = Modifier.height(44.dp), shape = RoundedCornerShape(22.dp)) {
          Text(actionLabel)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeModePicker(
  selected: ThemeMode,
  onSelected: (ThemeMode) -> Unit,
) {
  Row(
    modifier =
      Modifier
        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
        .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ThemeMode.entries.forEach { mode ->
      val checked = selected == mode
      IconToggleButton(
        checked = checked,
        onCheckedChange = { onSelected(mode) },
        modifier = Modifier.size(48.dp),
        shape = if (checked) CircleShape else RoundedCornerShape(16.dp),
        colors =
          IconButtonDefaults.iconToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
          ),
      ) {
        Icon(themeModeIcon(mode), contentDescription = mode.label)
      }
    }
  }
}

internal fun themeModeIcon(mode: ThemeMode): ImageVector =
  when (mode) {
    ThemeMode.System -> Icons.Rounded.BrightnessAuto
    ThemeMode.Dark -> Icons.Rounded.DarkMode
    ThemeMode.Light -> Icons.Rounded.LightMode
  }
