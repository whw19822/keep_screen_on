package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val settings = SettingsPreferences(context.applicationContext).settingsFlow.first()
        if (
          settings.startOnBoot &&
            settings.serviceEnabled &&
            UsageStatsHelper.hasUsageStatsPermission(context.applicationContext)
        ) {
          KeepAwakeService.start(context.applicationContext)
        }
      } finally {
        pendingResult.finish()
      }
    }
  }
}
