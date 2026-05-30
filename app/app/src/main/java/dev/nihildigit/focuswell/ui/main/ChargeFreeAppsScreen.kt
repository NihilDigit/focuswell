package dev.nihildigit.focuswell.ui.main

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class InstalledUserApp(
  val label: String,
  val packageName: String,
)

@Composable
internal fun ChargeFreeAppsScreen(
  selectedPackages: Set<String>,
  onSelectedPackagesChange: (Set<String>) -> Unit,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  var installedApps by remember { mutableStateOf<List<InstalledUserApp>?>(null) }
  var currentSelectedPackages by remember(selectedPackages) { mutableStateOf(selectedPackages) }

  LaunchedEffect(context.packageName) {
    installedApps = withContext(Dispatchers.Default) { installedUserApps(context) }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    ChargeFreeAppsTopBar(
      selectedCount = currentSelectedPackages.size,
      onBack = onBack,
    )
    val apps = installedApps
    if (apps == null) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else {
      val visiblePackages = apps.mapTo(mutableSetOf()) { it.packageName }
      val unavailableSelected =
        currentSelectedPackages
          .filterNot { it in visiblePackages }
          .map { packageName -> InstalledUserApp(label = packageName, packageName = packageName) }
      LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(apps + unavailableSelected, key = { it.packageName }) { app ->
          val selected = app.packageName in currentSelectedPackages
          ChargeFreeAppRow(
            app = app,
            selected = selected,
            onClick = {
              val nextPackages =
                if (selected) currentSelectedPackages - app.packageName
                else currentSelectedPackages + app.packageName
              currentSelectedPackages = nextPackages
              onSelectedPackagesChange(nextPackages)
            },
          )
        }
      }
    }
  }
}

@Composable
private fun ChargeFreeAppsTopBar(
  selectedCount: Int,
  onBack: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    IconButton(onClick = onBack) {
      Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text("Charge-free apps", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      Text(
        "$selectedCount selected",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ChargeFreeAppRow(
  app: InstalledUserApp,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
    contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Checkbox(checked = selected, onCheckedChange = { onClick() })
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(app.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          app.packageName,
          style = MaterialTheme.typography.bodySmall,
          color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

private fun installedUserApps(context: Context): List<InstalledUserApp> {
  val packageManager = context.packageManager
  val launcherIntent =
    Intent(Intent.ACTION_MAIN)
      .addCategory(Intent.CATEGORY_LAUNCHER)
  val launcherApps =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      packageManager.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
    } else {
      @Suppress("DEPRECATION")
      packageManager.queryIntentActivities(launcherIntent, 0)
    }
  val systemFlags = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
  return launcherApps
    .asSequence()
    .mapNotNull { resolveInfo -> resolveInfo.activityInfo?.applicationInfo }
    .filter { app -> app.packageName != context.packageName }
    .filter { app -> app.flags and systemFlags == 0 }
    .distinctBy { app -> app.packageName }
    .map { app ->
      InstalledUserApp(
        label = packageManager.getApplicationLabel(app).toString(),
        packageName = app.packageName,
      )
    }
    .sortedWith(compareBy<InstalledUserApp> { it.label.lowercase() }.thenBy { it.packageName })
    .toList()
}
