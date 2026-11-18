package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.AppSettings
import com.example.data.local.SettingsPreferences
import com.example.data.model.KeepAwakeRuleEntity
import com.example.data.model.WakeMode
import com.example.data.repository.InstalledAppsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class KeepAwakeService : Service() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var monitorJob: Job? = null

  private lateinit var settingsPrefs: SettingsPreferences
  private lateinit var appsRepo: InstalledAppsRepository

  @Volatile private var enabledRulesMap: Map<String, KeepAwakeRuleEntity> = emptyMap()
  @Volatile private var currentSettings: AppSettings = AppSettings()
  @Volatile private var isScreenInteractive: Boolean = true

  private var lastKnownForegroundPackage: String? = null
  private var activeWakeLock: PowerManager.WakeLock? = null
  private var heldWakeMode: WakeMode? = null
  private var heldForPackage: String? = null
  private var wakeLockAcquiredAt: Long = 0L
  private var timedOutPackage: String? = null
  private var lastNotificationSignature: String = ""

  private val screenReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
          Intent.ACTION_SCREEN_OFF -> {
            isScreenInteractive = false
            timedOutPackage = null
            releaseWakeLock()
            MonitorRuntimeState.update {
              it.copy(
                isWakeLockHeld = false,
                wakeLockMode = null,
                wakeLockStartedAtMillis = null,
                pauseReason = MonitorPauseReason.SCREEN_OFF,
              )
            }
          }
          Intent.ACTION_SCREEN_ON,
          Intent.ACTION_USER_PRESENT -> {
            isScreenInteractive = true
            evaluateOnce(initialLookbackMs = 15_000L)
          }
        }
      }
    }

  override fun onCreate() {
    super.onCreate()
    settingsPrefs = SettingsPreferences(applicationContext)
    appsRepo = InstalledAppsRepository(applicationContext)
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    isScreenInteractive = powerManager.isInteractive

    createNotificationChannel()
    val filter =
      IntentFilter().apply {
        addAction(Intent.ACTION_SCREEN_OFF)
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(Intent.ACTION_USER_PRESENT)
      }
    ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

    observeRulesAndSettings()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP_SERVICE) {
      serviceScope.launch { settingsPrefs.setServiceEnabled(false) }
      stopSelf()
      return START_NOT_STICKY
    }

    val notification = buildNotification(title = getString(R.string.notif_title_standby), text = getString(R.string.notif_text_standby, enabledRulesMap.size))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      ServiceCompat.startForeground(
        this,
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }

    MonitorRuntimeState.update { it.copy(isServiceRunning = true) }
    startMonitorLoop()
    return START_STICKY
  }

  override fun onDestroy() {
    monitorJob?.cancel()
    releaseWakeLock()
    runCatching { unregisterReceiver(screenReceiver) }
    MonitorRuntimeState.reset()
    serviceScope.cancel()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun observeRulesAndSettings() {
    val dao = AppDatabase.getInstance(applicationContext).keepAwakeRuleDao()
    serviceScope.launch {
      dao.observeEnabledRules().collectLatest { rules ->
        enabledRulesMap = rules.associateBy { it.packageName }
        evaluateOnce(initialLookbackMs = 5_000L)
      }
    }
    serviceScope.launch {
      settingsPrefs.settingsFlow.collectLatest { settings ->
        currentSettings = settings
        evaluateOnce(initialLookbackMs = 5_000L)
      }
    }
  }

  private fun startMonitorLoop() {
    monitorJob?.cancel()
    monitorJob =
      serviceScope.launch {
        // Initial wider lookback so we immediately detect the current foreground app
        evaluateOnce(initialLookbackMs = 24L * 60L * 60L * 1000L)
        while (isActive) {
          val interval = currentSettings.pollingIntervalMs.coerceIn(750L, 5000L)
          delay(interval)
          if (isScreenInteractive) {
            evaluateOnce(initialLookbackMs = interval * 3L + 2_000L)
          }
        }
      }
  }

  @Synchronized
  private fun evaluateOnce(initialLookbackMs: Long) {
    if (!UsageStatsHelper.hasUsageStatsPermission(applicationContext)) {
      releaseWakeLock()
      MonitorRuntimeState.update {
        it.copy(
          isServiceRunning = true,
          isWakeLockHeld = false,
          wakeLockMode = null,
          wakeLockStartedAtMillis = null,
          pauseReason = MonitorPauseReason.NO_USAGE_PERMISSION,
        )
      }
      return
    }

    if (!isScreenInteractive) {
      releaseWakeLock()
      return
    }

    val detected = UsageStatsHelper.queryLatestForegroundPackage(applicationContext, initialLookbackMs)
    if (detected != null) {
      if (detected != lastKnownForegroundPackage) {
        timedOutPackage = null
      }
      lastKnownForegroundPackage = detected
    }

    val fgPackage = lastKnownForegroundPackage
    val fgLabel = fgPackage?.let { appsRepo.resolveAppLabel(it) }

    val (batteryPct, isCharging) = readBatteryStatus()
    if (currentSettings.pauseOnLowBattery && !isCharging && batteryPct <= currentSettings.lowBatteryThreshold) {
      releaseWakeLock()
      updateNotificationIfChanged(
        title = getString(R.string.notif_title_paused_battery, batteryPct),
        text = getString(R.string.notif_text_paused_battery),
      )
      MonitorRuntimeState.update {
        it.copy(
          isServiceRunning = true,
          currentForegroundPackage = fgPackage,
          currentForegroundLabel = fgLabel,
          activeRulePackage = null,
          activeRuleLabel = null,
          isWakeLockHeld = false,
          wakeLockMode = null,
          wakeLockStartedAtMillis = null,
          pauseReason = MonitorPauseReason.LOW_BATTERY,
          batteryLevelPercent = batteryPct,
        )
      }
      return
    }

    val matchingRule = fgPackage?.let { enabledRulesMap[it] }
    if (matchingRule == null || !matchingRule.isEnabled) {
      releaseWakeLock()
      updateNotificationIfChanged(
        title = getString(R.string.notif_title_standby),
        text = getString(R.string.notif_text_standby, enabledRulesMap.size),
      )
      MonitorRuntimeState.update {
        it.copy(
          isServiceRunning = true,
          currentForegroundPackage = fgPackage,
          currentForegroundLabel = fgLabel,
          activeRulePackage = null,
          activeRuleLabel = null,
          isWakeLockHeld = false,
          wakeLockMode = null,
          wakeLockStartedAtMillis = null,
          pauseReason = MonitorPauseReason.NONE,
          batteryLevelPercent = batteryPct,
        )
      }
      return
    }

    // Check if safety timer has already expired for this foreground session
    if (timedOutPackage == matchingRule.packageName) {
      releaseWakeLock()
      updateNotificationIfChanged(
        title = getString(R.string.notif_title_standby),
        text = getString(R.string.notif_text_standby, enabledRulesMap.size),
      )
      MonitorRuntimeState.update {
        it.copy(
          isServiceRunning = true,
          currentForegroundPackage = fgPackage,
          currentForegroundLabel = fgLabel,
          activeRulePackage = matchingRule.packageName,
          activeRuleLabel = matchingRule.appLabel,
          isWakeLockHeld = false,
          wakeLockMode = null,
          wakeLockStartedAtMillis = null,
          pauseReason = MonitorPauseReason.SAFETY_TIMEOUT_REACHED,
          batteryLevelPercent = batteryPct,
        )
      }
      return
    }

    // Check active safety duration limit
    if (
      heldForPackage == matchingRule.packageName &&
        matchingRule.maxDurationMinutes > 0 &&
        wakeLockAcquiredAt > 0L
    ) {
      val elapsedMs = System.currentTimeMillis() - wakeLockAcquiredAt
      if (elapsedMs >= matchingRule.maxDurationMinutes * 60_000L) {
        timedOutPackage = matchingRule.packageName
        releaseWakeLock()
        MonitorRuntimeState.update {
          it.copy(
            isServiceRunning = true,
            currentForegroundPackage = fgPackage,
            currentForegroundLabel = fgLabel,
            activeRulePackage = matchingRule.packageName,
            activeRuleLabel = matchingRule.appLabel,
            isWakeLockHeld = false,
            wakeLockMode = null,
            wakeLockStartedAtMillis = null,
            pauseReason = MonitorPauseReason.SAFETY_TIMEOUT_REACHED,
            batteryLevelPercent = batteryPct,
          )
        }
        return
      }
    }

    acquireOrUpdateWakeLock(matchingRule)
    val notifText =
      if (matchingRule.wakeMode == WakeMode.BRIGHT) {
        getString(R.string.notif_text_bright)
      } else {
        getString(R.string.notif_text_dim)
      }
    updateNotificationIfChanged(
      title = getString(R.string.notif_title_active, matchingRule.appLabel),
      text = notifText,
    )
    MonitorRuntimeState.update {
      it.copy(
        isServiceRunning = true,
        currentForegroundPackage = fgPackage,
        currentForegroundLabel = fgLabel,
        activeRulePackage = matchingRule.packageName,
        activeRuleLabel = matchingRule.appLabel,
        isWakeLockHeld = activeWakeLock?.isHeld == true,
        wakeLockMode = matchingRule.wakeMode,
        wakeLockStartedAtMillis = wakeLockAcquiredAt,
        pauseReason = MonitorPauseReason.NONE,
        batteryLevelPercent = batteryPct,
      )
    }
  }

  private fun acquireOrUpdateWakeLock(rule: KeepAwakeRuleEntity) {
    if (
      activeWakeLock?.isHeld == true &&
        heldForPackage == rule.packageName &&
        heldWakeMode == rule.wakeMode
    ) {
      return
    }
    releaseWakeLock()
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    @Suppress("DEPRECATION")
    val levelFlags =
      when (rule.wakeMode) {
        WakeMode.BRIGHT -> PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE
        WakeMode.DIM -> PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE
      }
    val wl =
      pm.newWakeLock(levelFlags, "ScreenKeep:ForegroundRule:${rule.packageName}").apply {
        setReferenceCounted(false)
        acquire()
      }
    activeWakeLock = wl
    heldForPackage = rule.packageName
    heldWakeMode = rule.wakeMode
    wakeLockAcquiredAt = System.currentTimeMillis()
  }

  private fun releaseWakeLock() {
    runCatching {
      if (activeWakeLock?.isHeld == true) {
        activeWakeLock?.release()
      }
    }
    activeWakeLock = null
    heldForPackage = null
    heldWakeMode = null
    wakeLockAcquiredAt = 0L
  }

  private fun readBatteryStatus(): Pair<Int, Boolean> {
    val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging =
      status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
    return pct to isCharging
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val channel =
        NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW,
          )
          .apply {
            description = getString(R.string.notif_channel_desc)
            setShowBadge(false)
          }
      manager.createNotificationChannel(channel)
    }
  }

  private fun updateNotificationIfChanged(title: String, text: String) {
    val signature = "$title|$text"
    if (signature == lastNotificationSignature) return
    lastNotificationSignature = signature
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(NOTIFICATION_ID, buildNotification(title, text))
  }

  private fun buildNotification(title: String, text: String): Notification {
    val openAppIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val stopIntent =
      PendingIntent.getService(
        this,
        1,
        Intent(this, KeepAwakeService::class.java).apply { action = ACTION_STOP_SERVICE },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_tile_sun)
      .setContentTitle(title)
      .setContentText(text)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setContentIntent(openAppIntent)
      .addAction(0, getString(R.string.notif_action_stop), stopIntent)
      .build()
  }

  companion object {
    const val CHANNEL_ID = "keep_awake_monitor"
    const val NOTIFICATION_ID = 1001
    const val ACTION_STOP_SERVICE = "com.example.action.STOP_KEEP_AWAKE"

    fun start(context: Context) {
      val intent = Intent(context, KeepAwakeService::class.java)
      ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, KeepAwakeService::class.java))
    }
  }
}
