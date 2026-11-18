package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WakeMode
import com.example.ui.ScreenKeepUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  uiState: ScreenKeepUiState,
  onDefaultWakeModeChange: (WakeMode) -> Unit,
  onDefaultMaxDurationChange: (Int) -> Unit,
  onPollingIntervalChange: (Long) -> Unit,
  onPauseOnLowBatteryChange: (Boolean) -> Unit,
  onLowBatteryThresholdChange: (Int) -> Unit,
  onStartOnBootChange: (Boolean) -> Unit,
  onOpenUsageAccessSettings: () -> Unit,
  onOpenOverlaySettings: () -> Unit,
  onRequestNotificationPermission: () -> Unit,
  onOpenBatteryOptimizationSettings: () -> Unit,
  onClearAllRules: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showClearConfirmDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item(key = "defaults_section") {
      SettingsSectionCard(title = stringResource(R.string.settings_defaults_section)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = stringResource(R.string.rule_sheet_mode),
            style = MaterialTheme.typography.bodyLarge,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = uiState.settings.defaultWakeMode == WakeMode.BRIGHT,
              onClick = { onDefaultWakeModeChange(WakeMode.BRIGHT) },
              label = { Text(stringResource(R.string.wake_mode_bright)) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
              modifier = Modifier.testTag("default_mode_bright_chip"),
            )
            FilterChip(
              selected = uiState.settings.defaultWakeMode == WakeMode.DIM,
              onClick = { onDefaultWakeModeChange(WakeMode.DIM) },
              label = { Text(stringResource(R.string.wake_mode_dim)) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
              modifier = Modifier.testTag("default_mode_dim_chip"),
            )
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          Text(
            text = stringResource(R.string.rule_sheet_timeout),
            style = MaterialTheme.typography.bodyLarge,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            listOf(0, 15, 30, 60).forEach { minutes ->
              val label =
                if (minutes == 0) stringResource(R.string.timeout_unlimited)
                else stringResource(R.string.timeout_minutes, minutes)
              FilterChip(
                selected = uiState.settings.defaultMaxDurationMinutes == minutes,
                onClick = { onDefaultMaxDurationChange(minutes) },
                label = { Text(label) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  ),
                modifier = Modifier.testTag("default_timeout_chip_$minutes"),
              )
            }
          }
        }
      }
    }

    item(key = "monitor_section") {
      SettingsSectionCard(title = stringResource(R.string.settings_monitor_section)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Text(
            text = stringResource(R.string.settings_polling_interval),
            style = MaterialTheme.typography.bodyLarge,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1000L to "1.0s 灵敏", 1500L to "1.5s 均衡", 3000L to "3.0s 省电").forEach {
              (ms, label) ->
              FilterChip(
                selected = uiState.settings.pollingIntervalMs == ms,
                onClick = { onPollingIntervalChange(ms) },
                label = { Text(label) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  ),
                modifier = Modifier.testTag("polling_chip_$ms"),
              )
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.settings_low_battery_pause),
                style = MaterialTheme.typography.bodyLarge,
              )
              Text(
                text =
                  stringResource(
                    R.string.settings_low_battery_desc,
                    uiState.settings.lowBatteryThreshold,
                  ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = uiState.settings.pauseOnLowBattery,
              onCheckedChange = onPauseOnLowBatteryChange,
              modifier = Modifier.testTag("low_battery_pause_switch"),
            )
          }

          if (uiState.settings.pauseOnLowBattery) {
            Slider(
              value = uiState.settings.lowBatteryThreshold.toFloat(),
              onValueChange = { onLowBatteryThresholdChange(it.roundToInt()) },
              valueRange = 5f..40f,
              steps = 6,
              modifier = Modifier.testTag("low_battery_slider"),
            )
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(
              imageVector = Icons.Rounded.PowerSettingsNew,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.settings_boot_start),
                style = MaterialTheme.typography.bodyLarge,
              )
              Text(
                text = stringResource(R.string.settings_boot_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = uiState.settings.startOnBoot,
              onCheckedChange = onStartOnBootChange,
              modifier = Modifier.testTag("boot_start_switch"),
            )
          }
        }
      }
    }

    item(key = "power_and_permissions_section") {
      SettingsSectionCard(title = stringResource(R.string.settings_power_section)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          SettingsActionRow(
            icon = Icons.Rounded.Security,
            title = stringResource(R.string.settings_usage_access_title),
            subtitle =
              if (uiState.hasUsagePermission) stringResource(R.string.status_granted)
              else stringResource(R.string.status_not_granted),
            isHighlighted = !uiState.hasUsagePermission,
            onClick = onOpenUsageAccessSettings,
            tag = "settings_usage_access_row",
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          SettingsActionRow(
            icon = Icons.Rounded.Tune,
            title = "悬浮窗常亮权限（推荐）",
            subtitle =
              if (uiState.hasOverlayPermission) "已授权 · 使用高可靠透明常亮层"
              else "未授权 · 部分机型可能受限，建议开启",
            isHighlighted = !uiState.hasOverlayPermission,
            onClick = onOpenOverlaySettings,
            tag = "settings_overlay_access_row",
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          SettingsActionRow(
            icon = Icons.Rounded.NotificationsActive,
            title = stringResource(R.string.perm_notif_title),
            subtitle =
              if (uiState.hasNotificationPermission) stringResource(R.string.status_granted)
              else stringResource(R.string.status_not_granted),
            isHighlighted = !uiState.hasNotificationPermission,
            onClick = onRequestNotificationPermission,
            tag = "settings_notification_row",
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          SettingsActionRow(
            icon = Icons.Rounded.BatteryChargingFull,
            title = stringResource(R.string.settings_battery_opt_title),
            subtitle = stringResource(R.string.settings_battery_opt_desc),
            isHighlighted = false,
            onClick = onOpenBatteryOptimizationSettings,
            tag = "settings_battery_opt_row",
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Tune,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "提示：可在系统下拉快捷开关面板中添加「前台常亮」磁贴，随时一键启停监听。",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    if (uiState.configuredRules.isNotEmpty()) {
      item(key = "clear_rules_button") {
        OutlinedButton(
          onClick = { showClearConfirmDialog = true },
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("clear_all_rules_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteSweep,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp).padding(end = 4.dp),
          )
          Text(
            text = stringResource(R.string.settings_clear_rules),
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    }

    item(key = "settings_bottom_spacer") { Spacer(modifier = Modifier.height(16.dp)) }
  }

  if (showClearConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearConfirmDialog = false },
      title = { Text(stringResource(R.string.settings_clear_rules)) },
      text = { Text("确定要清空全部已配置的应用常亮规则吗？此操作无法撤销。") },
      confirmButton = {
        TextButton(
          onClick = {
            onClearAllRules()
            showClearConfirmDialog = false
          },
          modifier = Modifier.testTag("confirm_clear_rules_button"),
        ) {
          Text("清空", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirmDialog = false }) {
          Text("取消")
        }
      },
    )
  }
}

@Composable
private fun SettingsSectionCard(
  title: String,
  content: @Composable () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(start = 4.dp),
    )
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = MaterialTheme.shapes.medium,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(16.dp)) { content() }
    }
  }
}

@Composable
private fun SettingsActionRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  isHighlighted: Boolean,
  onClick: () -> Unit,
  tag: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .padding(vertical = 10.dp)
        .testTag(tag),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint =
        if (isHighlighted) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(text = title, style = MaterialTheme.typography.bodyLarge)
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color =
          if (isHighlighted) MaterialTheme.colorScheme.error
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Icon(
      imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
