package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.data.model.InstalledAppItem
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledAppsRepository(private val context: Context) {

  suspend fun loadLaunchableApps(): List<InstalledAppItem> =
    withContext(Dispatchers.IO) {
      val pm = context.packageManager
      val launcherIntent =
        Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }

      val resolved =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
          @Suppress("DEPRECATION") pm.queryIntentActivities(launcherIntent, 0)
        }

      val collator = Collator.getInstance(Locale.getDefault())
      val seenPackages = HashSet<String>()

      resolved
        .mapNotNull { info ->
          val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
          if (pkg == context.packageName || !seenPackages.add(pkg)) {
            return@mapNotNull null
          }
          val appInfo = info.activityInfo.applicationInfo
          val isSystem =
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
              (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
          val label = info.loadLabel(pm)?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: pkg
          val icon = runCatching { info.loadIcon(pm) }.getOrNull()
          InstalledAppItem(
            packageName = pkg,
            label = label,
            isSystemApp = isSystem,
            icon = icon,
          )
        }
        .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

  fun resolveAppLabel(packageName: String): String {
    val pm = context.packageManager
    return runCatching {
        val appInfo =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
          } else {
            @Suppress("DEPRECATION") pm.getApplicationInfo(packageName, 0)
          }
        pm.getApplicationLabel(appInfo).toString()
      }
      .getOrDefault(packageName)
  }
}
