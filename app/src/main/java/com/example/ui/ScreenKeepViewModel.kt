package com.example.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AppSettings
import com.example.data.local.SettingsPreferences
import com.example.data.model.AppWithRule
import com.example.data.model.InstalledAppItem
import com.example.data.model.KeepAwakeRuleEntity
import com.example.data.model.WakeMode
import com.example.data.repository.InstalledAppsRepository
import com.example.service.KeepAwakeService
import com.example.service.MonitorRuntimeState
import com.example.service.MonitorState
import com.example.service.UsageStatsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppFilter {
  USER_ONLY,
  ALL,
  SELECTED_ONLY,
}

data class ScreenKeepUiState(
  val isLoadingApps: Boolean = true,
  val hasUsagePermission: Boolean = false,
  val hasNotificationPermission: Boolean = true,
  val settings: AppSettings = AppSettings(),
  val monitorState: MonitorState = MonitorState(),
  val filteredApps: List<AppWithRule> = emptyList(),
  val configuredRules: List<AppWithRule> = emptyList(),
  val enabledRulesCount: Int = 0,
  val searchQuery: String = "",
  val appFilter: AppFilter = AppFilter.USER_ONLY,
  val editingApp: AppWithRule? = null,
)

class ScreenKeepViewModel(application: Application) : AndroidViewModel(application) {

  private val context = application.applicationContext
  private val ruleDao = AppDatabase.getInstance(context).keepAwakeRuleDao()
  private val settingsPrefs = SettingsPreferences(context)
  private val appsRepo = InstalledAppsRepository(context)

  private val installedAppsFlow = MutableStateFlow<List<InstalledAppItem>>(emptyList())
  private val isLoadingAppsFlow = MutableStateFlow(true)
  private val hasUsagePermissionFlow = MutableStateFlow(checkUsagePermission())
  private val hasNotificationPermissionFlow = MutableStateFlow(checkNotificationPermission())
  private val searchQueryFlow = MutableStateFlow("")
  private val appFilterFlow = MutableStateFlow(AppFilter.USER_ONLY)
  private val editingAppFlow = MutableStateFlow<AppWithRule?>(null)

  private val baseCombined =
    combine(
      installedAppsFlow,
      ruleDao.observeAllRules(),
      isLoadingAppsFlow,
      hasUsagePermissionFlow,
      hasNotificationPermissionFlow,
    ) { apps, rules, isLoading, usagePerm, notifPerm ->
      BaseData(apps, rules, isLoading, usagePerm, notifPerm)
    }

  private val prefsCombined =
    combine(
      settingsPrefs.settingsFlow,
      MonitorRuntimeState.state,
      searchQueryFlow,
      appFilterFlow,
      editingAppFlow,
    ) { settings, monitor, query, filter, editing ->
      PrefsData(settings, monitor, query, filter, editing)
    }

  val uiState: StateFlow<ScreenKeepUiState> =
    combine(baseCombined, prefsCombined) { base, prefs ->
        val rulesByPkg = base.rules.associateBy { it.packageName }
        val appsByPkg = base.apps.associateBy { it.packageName }

        val configured =
          base.rules.map { rule ->
            val app =
              appsByPkg[rule.packageName]
                ?: InstalledAppItem(
                  packageName = rule.packageName,
                  label = rule.appLabel,
                  isSystemApp = false,
                  icon = null,
                )
            AppWithRule(app = app, rule = rule)
          }

        val queryLower = prefs.query.trim().lowercase()
        val filtered =
          base.apps
            .map { app -> AppWithRule(app = app, rule = rulesByPkg[app.packageName]) }
            .filter { item ->
              val matchesFilter =
                when (prefs.filter) {
                  AppFilter.USER_ONLY -> !item.app.isSystemApp || item.rule != null
                  AppFilter.ALL -> true
                  AppFilter.SELECTED_ONLY -> item.rule != null
                }
              val matchesQuery =
                queryLower.isEmpty() ||
                  item.app.label.lowercase().contains(queryLower) ||
                  item.app.packageName.lowercase().contains(queryLower)
              matchesFilter && matchesQuery
            }

        ScreenKeepUiState(
          isLoadingApps = base.isLoading,
          hasUsagePermission = base.hasUsagePermission,
          hasNotificationPermission = base.hasNotificationPermission,
          settings = prefs.settings,
          monitorState = prefs.monitor,
          filteredApps = filtered,
          configuredRules = configured,
          enabledRulesCount = base.rules.count { it.isEnabled },
          searchQuery = prefs.query,
          appFilter = prefs.filter,
          editingApp = prefs.editing,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue =
          ScreenKeepUiState(
            hasUsagePermission = hasUsagePermissionFlow.value,
            hasNotificationPermission = hasNotificationPermissionFlow.value,
          ),
      )

  init {
    refreshInstalledApps()
  }

  fun refreshPermissions() {
    val usageGranted = checkUsagePermission()
    val notifGranted = checkNotificationPermission()
    hasUsagePermissionFlow.value = usageGranted
    hasNotificationPermissionFlow.value = notifGranted

    if (usageGranted && uiState.value.settings.serviceEnabled && !MonitorRuntimeState.state.value.isServiceRunning) {
      KeepAwakeService.start(context)
    }
  }

  fun refreshInstalledApps() {
    viewModelScope.launch {
      isLoadingAppsFlow.value = true
      installedAppsFlow.value = appsRepo.loadLaunchableApps()
      isLoadingAppsFlow.value = false
    }
  }

  fun setSearchQuery(query: String) {
    searchQueryFlow.value = query
  }

  fun setAppFilter(filter: AppFilter) {
    appFilterFlow.value = filter
  }

  fun openRuleEditor(item: AppWithRule) {
    editingAppFlow.value = item
  }

  fun closeRuleEditor() {
    editingAppFlow.value = null
  }

  fun toggleMasterService(enable: Boolean) {
    viewModelScope.launch {
      if (enable && !checkUsagePermission()) {
        UsageStatsHelper.openUsageAccessSettings(context)
        return@launch
      }
      settingsPrefs.setServiceEnabled(enable)
      if (enable) {
        KeepAwakeService.start(context)
      } else {
        KeepAwakeService.stop(context)
      }
    }
  }

  fun toggleAppSelection(item: AppWithRule, enabled: Boolean) {
    viewModelScope.launch {
      val currentSettings = uiState.value.settings
      val existing = item.rule
      if (existing == null) {
        if (enabled) {
          ruleDao.upsertRule(
            KeepAwakeRuleEntity(
              packageName = item.app.packageName,
              appLabel = item.app.label,
              isEnabled = true,
              wakeMode = currentSettings.defaultWakeMode,
              maxDurationMinutes = currentSettings.defaultMaxDurationMinutes,
            )
          )
        }
      } else {
        ruleDao.setRuleEnabled(item.app.packageName, enabled)
      }
    }
  }

  fun saveRule(
    packageName: String,
    appLabel: String,
    isEnabled: Boolean,
    wakeMode: WakeMode,
    maxDurationMinutes: Int,
  ) {
    viewModelScope.launch {
      ruleDao.upsertRule(
        KeepAwakeRuleEntity(
          packageName = packageName,
          appLabel = appLabel,
          isEnabled = isEnabled,
          wakeMode = wakeMode,
          maxDurationMinutes = maxDurationMinutes,
        )
      )
      editingAppFlow.value = null
    }
  }

  fun removeRule(packageName: String) {
    viewModelScope.launch {
      ruleDao.deleteRule(packageName)
      editingAppFlow.value = null
    }
  }

  fun clearAllRules() {
    viewModelScope.launch { ruleDao.clearAllRules() }
  }

  fun setDefaultWakeMode(mode: WakeMode) {
    viewModelScope.launch { settingsPrefs.setDefaultWakeMode(mode) }
  }

  fun setDefaultMaxDurationMinutes(minutes: Int) {
    viewModelScope.launch { settingsPrefs.setDefaultMaxDurationMinutes(minutes) }
  }

  fun setPollingIntervalMs(intervalMs: Long) {
    viewModelScope.launch { settingsPrefs.setPollingIntervalMs(intervalMs) }
  }

  fun setPauseOnLowBattery(enabled: Boolean) {
    viewModelScope.launch { settingsPrefs.setPauseOnLowBattery(enabled) }
  }

  fun setLowBatteryThreshold(threshold: Int) {
    viewModelScope.launch { settingsPrefs.setLowBatteryThreshold(threshold) }
  }

  fun setStartOnBoot(enabled: Boolean) {
    viewModelScope.launch { settingsPrefs.setStartOnBoot(enabled) }
  }

  private fun checkUsagePermission(): Boolean = UsageStatsHelper.hasUsageStatsPermission(context)

  private fun checkNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  private data class BaseData(
    val apps: List<InstalledAppItem>,
    val rules: List<KeepAwakeRuleEntity>,
    val isLoading: Boolean,
    val hasUsagePermission: Boolean,
    val hasNotificationPermission: Boolean,
  )

  private data class PrefsData(
    val settings: AppSettings,
    val monitor: MonitorState,
    val query: String,
    val filter: AppFilter,
    val editing: AppWithRule?,
  )
}
