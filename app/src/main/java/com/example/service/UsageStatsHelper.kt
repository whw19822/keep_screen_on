package com.example.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

object UsageStatsHelper {

  fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
          AppOpsManager.OPSTR_GET_USAGE_STATS,
          Process.myUid(),
          context.packageName,
        )
      } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
          AppOpsManager.OPSTR_GET_USAGE_STATS,
          Process.myUid(),
          context.packageName,
        )
      }
    return if (mode == AppOpsManager.MODE_DEFAULT) {
      context.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      mode == AppOpsManager.MODE_ALLOWED
    }
  }

  fun canDrawOverlays(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
  }

  fun openUsageAccessSettings(context: Context) {
    val targetedIntent =
      Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    val started = runCatching { context.startActivity(targetedIntent) }.isSuccess
    if (!started) {
      val fallbackIntent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      runCatching { context.startActivity(fallbackIntent) }
    }
  }

  fun openOverlaySettings(context: Context) {
    val targetedIntent =
      Intent(
          Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
          Uri.fromParts("package", context.packageName, null),
        )
        .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    val started = runCatching { context.startActivity(targetedIntent) }.isSuccess
    if (!started) {
      val fallbackIntent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      runCatching { context.startActivity(fallbackIntent) }
    }
  }

  fun openBatteryOptimizationSettings(context: Context) {
    val intent =
      Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    runCatching { context.startActivity(intent) }
  }

  /**
   * Resolves the most recently resumed foreground package within [lookbackMs],
   * falling back to queryUsageStats when [useUsageStatsFallback] is true.
   */
  fun queryLatestForegroundPackage(
    context: Context,
    lookbackMs: Long,
    useUsageStatsFallback: Boolean = false,
  ): String? {
    if (!hasUsageStatsPermission(context)) return null
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
    val now = System.currentTimeMillis()
    val start = (now - lookbackMs).coerceAtLeast(0L)
    val events = runCatching { usm.queryEvents(start, now) }.getOrNull()

    var latestPackage: String? = null
    var latestTimestamp = 0L
    if (events != null) {
      val event = UsageEvents.Event()
      while (events.hasNextEvent()) {
        events.getNextEvent(event)
        val isResumed =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
          } else {
            @Suppress("DEPRECATION") event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
          }
        if (isResumed && event.timeStamp >= latestTimestamp && !event.packageName.isNullOrBlank()) {
          latestTimestamp = event.timeStamp
          latestPackage = event.packageName
        }
      }
    }

    if (latestPackage != null) return latestPackage

    if (useUsageStatsFallback) {
      val stats =
        runCatching {
            usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, now)
          }
          .getOrNull()
      val recent =
        stats
          ?.filter { !it.packageName.isNullOrBlank() && it.lastTimeUsed > 0L }
          ?.maxByOrNull { it.lastTimeUsed }
      return recent?.packageName
    }

    return null
  }
}
