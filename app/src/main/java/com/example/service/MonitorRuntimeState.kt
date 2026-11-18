package com.example.service

import com.example.data.model.WakeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class MonitorPauseReason {
  NONE,
  NO_USAGE_PERMISSION,
  LOW_BATTERY,
  SCREEN_OFF,
  SAFETY_TIMEOUT_REACHED,
}

data class MonitorState(
  val isServiceRunning: Boolean = false,
  val currentForegroundPackage: String? = null,
  val currentForegroundLabel: String? = null,
  val activeRulePackage: String? = null,
  val activeRuleLabel: String? = null,
  val isWakeLockHeld: Boolean = false,
  val wakeLockMode: WakeMode? = null,
  val wakeLockStartedAtMillis: Long? = null,
  val pauseReason: MonitorPauseReason = MonitorPauseReason.NONE,
  val batteryLevelPercent: Int = 100,
)

object MonitorRuntimeState {
  private val _state = MutableStateFlow(MonitorState())
  val state: StateFlow<MonitorState> = _state.asStateFlow()

  fun update(transform: (MonitorState) -> MonitorState) {
    _state.update(transform)
  }

  fun reset() {
    _state.value = MonitorState()
  }
}
