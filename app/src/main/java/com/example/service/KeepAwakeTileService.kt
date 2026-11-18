package com.example.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.data.local.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class KeepAwakeTileService : TileService() {

  private val tileScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  override fun onStartListening() {
    super.onStartListening()
    refreshTileState()
  }

  override fun onClick() {
    super.onClick()
    val prefs = SettingsPreferences(applicationContext)
    tileScope.launch {
      val isRunning = MonitorRuntimeState.state.value.isServiceRunning
      if (isRunning) {
        prefs.setServiceEnabled(false)
        KeepAwakeService.stop(applicationContext)
      } else if (UsageStatsHelper.hasUsageStatsPermission(applicationContext)) {
        prefs.setServiceEnabled(true)
        KeepAwakeService.start(applicationContext)
      } else {
        UsageStatsHelper.openUsageAccessSettings(applicationContext)
      }
      refreshTileState()
    }
  }

  override fun onDestroy() {
    tileScope.cancel()
    super.onDestroy()
  }

  private fun refreshTileState() {
    val tile = qsTile ?: return
    val prefs = SettingsPreferences(applicationContext)
    tileScope.launch {
      val enabled = prefs.settingsFlow.first().serviceEnabled
      val running = MonitorRuntimeState.state.value.isServiceRunning || enabled
      tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
      tile.updateTile()
    }
  }
}
