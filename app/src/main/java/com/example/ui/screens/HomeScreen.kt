package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppWithRule
import com.example.service.MonitorPauseReason
import com.example.ui.ScreenKeepUiState
import com.example.ui.components.AppIconImage
import com.example.ui.components.DurationBadge
import com.example.ui.components.StatusPulseDot
import com.example.ui.components.WakeModeBadge

@Composable
fun HomeScreen(
  uiState: ScreenKeepUiState,
  onToggleMasterService: (Boolean) -> Unit,
  onRequestUsagePermission: () -> Unit,
  onRequestNotificationPermission: () -> Unit,
  onToggleRuleEnabled: (AppWithRule, Boolean) -> Unit,
  onEditRule: (AppWithRule) -> Unit,
  onNavigateToApps: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item(key = "hero_card") {
      HeroMonitorCard(
        uiState = uiState,
        onToggleMasterService = onToggleMasterService,
      )
    }

    item(key = "usage_permission_banner") {
      AnimatedVisibility(
        visible = !uiState.hasUsagePermission,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
      ) {
        PermissionBannerCard(
          icon = Icons.Rounded.Security,
          title = stringResource(R.string.perm_usage_title),
          description = stringResource(R.string.perm_usage_desc),
          actionLabel = stringResource(R.string.perm_usage_action),
          isUrgent = true,
          onAction = onRequestUsagePermission,
          actionTag = "grant_usage_access_button",
        )
      }
    }

    item(key = "notification_permission_banner") {
      AnimatedVisibility(
        visible = uiState.hasUsagePermission && !uiState.hasNotificationPermission,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
      ) {
        PermissionBannerCard(
          icon = Icons.Rounded.NotificationsActive,
          title = stringResource(R.string.perm_notif_title),
          description = stringResource(R.string.perm_notif_desc),
          actionLabel = stringResource(R.string.perm_notif_action),
          isUrgent = false,
          onAction = onRequestNotificationPermission,
          actionTag = "grant_notification_button",
        )
      }
    }

    item(key = "rules_header") {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
      ) {
        Text(
          text = stringResource(R.string.section_active_rules),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        if (uiState.configuredRules.isNotEmpty()) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(999.dp),
          ) {
            Text(
              text = "${uiState.enabledRulesCount} / ${uiState.configuredRules.size}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
      }
    }

    if (uiState.configuredRules.isEmpty()) {
      item(key = "empty_rules") {
        EmptyRulesCard(onNavigateToApps = onNavigateToApps)
      }
    } else {
      items(
        items = uiState.configuredRules,
        key = { it.app.packageName },
      ) { item ->
        val isActiveNow =
          uiState.monitorState.isWakeLockHeld &&
            uiState.monitorState.activeRulePackage == item.app.packageName
        ConfiguredRuleCard(
          item = item,
          isActiveNow = isActiveNow,
          onToggleEnabled = { enabled -> onToggleRuleEnabled(item, enabled) },
          onClick = { onEditRule(item) },
        )
      }
    }

    item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(12.dp)) }
  }
}

@Composable
private fun HeroMonitorCard(
  uiState: ScreenKeepUiState,
  onToggleMasterService: (Boolean) -> Unit,
) {
  val monitor = uiState.monitorState
  val isRunning = monitor.isServiceRunning
  val isAwakeHeld = monitor.isWakeLockHeld
  val isLowBatteryPaused = monitor.pauseReason == MonitorPauseReason.LOW_BATTERY

  val glowColor =
    when {
      isAwakeHeld -> MaterialTheme.colorScheme.primary
      isRunning -> MaterialTheme.colorScheme.secondary
      else -> Color.Transparent
    }

  val statusText =
    when {
      !uiState.hasUsagePermission -> stringResource(R.string.status_needs_permission)
      isLowBatteryPaused -> stringResource(R.string.status_low_battery_paused)
      isAwakeHeld -> stringResource(R.string.status_active_awake)
      isRunning -> stringResource(R.string.status_monitoring)
      else -> stringResource(R.string.status_stopped)
    }

  val dotColor =
    when {
      !uiState.hasUsagePermission -> MaterialTheme.colorScheme.error
      isLowBatteryPaused -> MaterialTheme.colorScheme.error
      isAwakeHeld -> MaterialTheme.colorScheme.primary
      isRunning -> MaterialTheme.colorScheme.secondary
      else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

  val borderColor =
    when {
      isAwakeHeld -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
      isRunning -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
      else -> MaterialTheme.colorScheme.outlineVariant
    }

  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(1.dp, borderColor),
    tonalElevation = 2.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .drawBehind {
            if (glowColor != Color.Transparent) {
              drawCircle(
                brush =
                  Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.24f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.15f),
                    radius = size.maxDimension * 0.85f,
                  )
              )
            }
          }
          .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          StatusPulseDot(isAnimating = isRunning && uiState.hasUsagePermission, color = dotColor)
          Text(
            text =
              if (isRunning) stringResource(R.string.master_switch_on)
              else stringResource(R.string.master_switch_off),
            style = MaterialTheme.typography.labelLarge,
            color = dotColor,
          )
        }

        Switch(
          checked = uiState.settings.serviceEnabled && isRunning,
          onCheckedChange = onToggleMasterService,
          colors =
            SwitchDefaults.colors(
              checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
              checkedTrackColor =
                if (isAwakeHeld) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondary,
            ),
          modifier = Modifier.testTag("master_service_switch"),
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = statusText,
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )

        val fgLabel = monitor.currentForegroundLabel
        val fgPkg = monitor.currentForegroundPackage
        if (isRunning && !fgLabel.isNullOrBlank()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector =
                if (isAwakeHeld) Icons.Rounded.WbSunny else Icons.Rounded.Visibility,
              contentDescription = null,
              tint =
                if (isAwakeHeld) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "${stringResource(R.string.current_foreground_label)}: $fgLabel",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            if (!fgPkg.isNullOrBlank()) {
              Text(
                text = fgPkg,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        } else {
          Text(
            text =
              if (uiState.enabledRulesCount > 0)
                "已配置 ${uiState.enabledRulesCount} 个常亮应用规则，开启后自动监测前台应用"
              else stringResource(R.string.current_foreground_none),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        MetricPill(
          label = "生效规则",
          value = "${uiState.enabledRulesCount} 个",
          modifier = Modifier.weight(1f),
        )
        MetricPill(
          label = "检测间隔",
          value = "${uiState.settings.pollingIntervalMs / 1000f}s",
          modifier = Modifier.weight(1f),
        )
        MetricPill(
          label = "低电保护",
          value =
            if (uiState.settings.pauseOnLowBattery) "${uiState.settings.lowBatteryThreshold}%"
            else "关闭",
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun MetricPill(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    shape = RoundedCornerShape(14.dp),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
private fun PermissionBannerCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  description: String,
  actionLabel: String,
  isUrgent: Boolean,
  onAction: () -> Unit,
  actionTag: String,
) {
  val containerColor =
    if (isUrgent) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
  val accentColor =
    if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

  Surface(
    color = containerColor,
    shape = MaterialTheme.shapes.medium,
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
      Box(
        modifier =
          Modifier.size(42.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(22.dp),
        )
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Button(
        onClick = onAction,
        shape = RoundedCornerShape(12.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        modifier = Modifier.testTag(actionTag),
      ) {
        Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
      }
    }
  }
}

@Composable
private fun ConfiguredRuleCard(
  item: AppWithRule,
  isActiveNow: Boolean,
  onToggleEnabled: (Boolean) -> Unit,
  onClick: () -> Unit,
) {
  val rule = item.rule ?: return
  val borderStroke =
    if (isActiveNow) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.medium,
    border = borderStroke,
    tonalElevation = if (isActiveNow) 4.dp else 1.dp,
    modifier =
      Modifier.fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .clickable(onClick = onClick)
        .testTag("rule_card_${item.app.packageName}"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
      AppIconImage(
        drawable = item.app.icon,
        contentDescription = item.app.label,
        modifier = Modifier.size(46.dp),
      )

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = item.app.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          if (isActiveNow) {
            Icon(
              imageVector = Icons.Rounded.Bolt,
              contentDescription = stringResource(R.string.status_active_awake),
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          WakeModeBadge(wakeMode = rule.wakeMode)
          DurationBadge(maxDurationMinutes = rule.maxDurationMinutes)
        }
      }

      Switch(
        checked = rule.isEnabled,
        onCheckedChange = onToggleEnabled,
        modifier = Modifier.testTag("rule_switch_${item.app.packageName}"),
      )
    }
  }
}

@Composable
private fun EmptyRulesCard(onNavigateToApps: () -> Unit) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
      Box(
        modifier =
          Modifier.size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.WbSunny,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(32.dp),
        )
      }

      Text(
        text = stringResource(R.string.empty_rules_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = stringResource(R.string.empty_rules_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Button(
        onClick = onNavigateToApps,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.height(48.dp).testTag("browse_apps_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Apps,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.empty_rules_cta))
      }
    }
  }
}
