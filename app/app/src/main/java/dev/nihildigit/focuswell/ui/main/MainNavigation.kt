package dev.nihildigit.focuswell.ui.main

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nihildigit.focuswell.domain.Destination

@Composable
internal fun FocusWellNavigationBar(
  selected: Destination,
  onDestination: (Destination) -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  ShortNavigationBar(
    containerColor = colors.surfaceContainer,
    contentColor = colors.onSurfaceVariant,
  ) {
    Destination.entries.forEach { destination ->
      ShortNavigationBarItem(
        selected = selected == destination,
        onClick = { onDestination(destination) },
        icon = { DestinationIcon(destination = destination) },
        label = {
          Text(
            destination.label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        colors =
          ShortNavigationBarItemDefaults.colors(
            selectedIconColor = colors.onSecondaryContainer,
            selectedTextColor = colors.secondary,
            selectedIndicatorColor = colors.secondaryContainer,
            unselectedIconColor = colors.onSurfaceVariant,
            unselectedTextColor = colors.onSurfaceVariant,
          ),
      )
    }
  }
}

@Composable
internal fun FocusWellNavigationRail(
  selected: Destination,
  onDestination: (Destination) -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  NavigationRail(
    containerColor = colors.surfaceContainer,
    contentColor = colors.onSurfaceVariant,
  ) {
    Spacer(Modifier.height(12.dp))
    Destination.entries.forEach { destination ->
      NavigationRailItem(
        selected = selected == destination,
        onClick = { onDestination(destination) },
        icon = { DestinationIcon(destination = destination) },
        label = {
          Text(
            destination.label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        colors =
          NavigationRailItemDefaults.colors(
            selectedIconColor = colors.onSecondaryContainer,
            selectedTextColor = colors.secondary,
            indicatorColor = colors.secondaryContainer,
            unselectedIconColor = colors.onSurfaceVariant,
            unselectedTextColor = colors.onSurfaceVariant,
          ),
      )
    }
  }
}

@Composable
internal fun DestinationIcon(destination: Destination) {
  val icon =
    when (destination) {
      Destination.Today -> Icons.Rounded.Today
      Destination.Reserve -> Icons.Rounded.AccountBalanceWallet
      Destination.Ideas -> Icons.Rounded.Lightbulb
      Destination.Plan -> Icons.AutoMirrored.Rounded.EventNote
      Destination.Settings -> Icons.Rounded.Settings
    }
  Icon(
    imageVector = icon,
    contentDescription = null,
  )
}
