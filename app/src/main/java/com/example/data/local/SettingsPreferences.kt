package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.WakeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "screenkeep_prefs")

data class AppSettings(
  val serviceEnabled: Boolean = false,
  val defaultWakeMode: WakeMode = WakeMode.BRIGHT,
  val defaultMaxDurationMinutes: Int = 0,
  val pollingIntervalMs: Long = 1500L,
  val pauseOnLowBattery: Boolean = true,
  val lowBatteryThreshold: Int = 15,
  val startOnBoot: Boolean = false,
)

class SettingsPreferences(private val context: Context) {

  private object Keys {
    val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
    val DEFAULT_WAKE_MODE = stringPreferencesKey("default_wake_mode")
    val DEFAULT_MAX_DURATION_MIN = intPreferencesKey("default_max_duration_min")
    val POLLING_INTERVAL_MS = longPreferencesKey("polling_interval_ms")
    val PAUSE_ON_LOW_BATTERY = booleanPreferencesKey("pause_on_low_battery")
    val LOW_BATTERY_THRESHOLD = intPreferencesKey("low_battery_threshold")
    val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
  }

  val settingsFlow: Flow<AppSettings> =
    context.dataStore.data.map { prefs ->
      AppSettings(
        serviceEnabled = prefs[Keys.SERVICE_ENABLED] ?: false,
        defaultWakeMode =
          prefs[Keys.DEFAULT_WAKE_MODE]?.let {
            runCatching { WakeMode.valueOf(it) }.getOrNull()
          } ?: WakeMode.BRIGHT,
        defaultMaxDurationMinutes = prefs[Keys.DEFAULT_MAX_DURATION_MIN] ?: 0,
        pollingIntervalMs = prefs[Keys.POLLING_INTERVAL_MS] ?: 1500L,
        pauseOnLowBattery = prefs[Keys.PAUSE_ON_LOW_BATTERY] ?: true,
        lowBatteryThreshold = prefs[Keys.LOW_BATTERY_THRESHOLD] ?: 15,
        startOnBoot = prefs[Keys.START_ON_BOOT] ?: false,
      )
    }

  suspend fun setServiceEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.SERVICE_ENABLED] = enabled }
  }

  suspend fun setDefaultWakeMode(mode: WakeMode) {
    context.dataStore.edit { it[Keys.DEFAULT_WAKE_MODE] = mode.name }
  }

  suspend fun setDefaultMaxDurationMinutes(minutes: Int) {
    context.dataStore.edit { it[Keys.DEFAULT_MAX_DURATION_MIN] = minutes }
  }

  suspend fun setPollingIntervalMs(intervalMs: Long) {
    context.dataStore.edit { it[Keys.POLLING_INTERVAL_MS] = intervalMs }
  }

  suspend fun setPauseOnLowBattery(enabled: Boolean) {
    context.dataStore.edit { it[Keys.PAUSE_ON_LOW_BATTERY] = enabled }
  }

  suspend fun setLowBatteryThreshold(threshold: Int) {
    context.dataStore.edit { it[Keys.LOW_BATTERY_THRESHOLD] = threshold.coerceIn(5, 50) }
  }

  suspend fun setStartOnBoot(enabled: Boolean) {
    context.dataStore.edit { it[Keys.START_ON_BOOT] = enabled }
  }
}
