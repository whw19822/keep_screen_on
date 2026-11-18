package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppWithRule
import com.example.data.model.WakeMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleConfigBottomSheet(
  item: AppWithRule,
  defaultWakeMode: WakeMode,
  defaultMaxDurationMinutes: Int,
  onDismiss: () -> Unit,
  onSave: (Boolean, WakeMode, Int) -> Unit,
  onRemove: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var isEnabled by remember(item) { mutableStateOf(item.rule?.isEnabled ?: true) }
  var selectedMode by
    remember(item) { mutableStateOf(item.rule?.wakeMode ?: defaultWakeMode) }
  var selectedDuration by
    remember(item) {
      mutableIntStateOf(item.rule?.maxDurationMinutes ?: defaultMaxDurationMinutes)
    }

  val durationOptions = listOf(0, 15, 30, 60, 120)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 24.dp)
          .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        AppIconImage(
          drawable = item.app.icon,
          contentDescription = item.app.label,
          modifier = Modifier.size(52.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.app.label,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = item.app.packageName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
          Text(
            text = stringResource(R.string.rule_sheet_enable),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
          )
          Switch(
            checked = isEnabled,
            onCheckedChange = { isEnabled = it },
            modifier = Modifier.testTag("rule_sheet_enable_switch"),
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = stringResource(R.string.rule_sheet_mode),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          WakeModeOptionCard(
            title = stringResource(R.string.wake_mode_bright),
            description = stringResource(R.string.wake_mode_bright_desc),
            icon = Icons.Rounded.BrightnessHigh,
            selected = selectedMode == WakeMode.BRIGHT,
            onClick = { selectedMode = WakeMode.BRIGHT },
            modifier = Modifier.weight(1f).testTag("wake_mode_bright_option"),
          )
          WakeModeOptionCard(
            title = stringResource(R.string.wake_mode_dim),
            description = stringResource(R.string.wake_mode_dim_desc),
            icon = Icons.Rounded.BrightnessLow,
            selected = selectedMode == WakeMode.DIM,
            onClick = { selectedMode = WakeMode.DIM },
            modifier = Modifier.weight(1f).testTag("wake_mode_dim_option"),
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = stringResource(R.string.rule_sheet_timeout),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          durationOptions.forEach { minutes ->
            val label =
              if (minutes == 0) stringResource(R.string.timeout_unlimited)
              else stringResource(R.string.timeout_minutes, minutes)
            FilterChip(
              selected = selectedDuration == minutes,
              onClick = { selectedDuration = minutes },
              label = { Text(label) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
              modifier = Modifier.testTag("duration_chip_$minutes"),
            )
          }
        }
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        if (item.rule != null) {
          OutlinedButton(
            onClick = onRemove,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.height(50.dp).testTag("remove_rule_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = stringResource(R.string.rule_sheet_remove),
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.rule_sheet_remove),
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
        Button(
          onClick = { onSave(isEnabled, selectedMode, selectedDuration) },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).height(50.dp).testTag("save_rule_button"),
        ) {
          Text(
            text = stringResource(R.string.rule_sheet_save),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}

@Composable
private fun WakeModeOptionCard(
  title: String,
  description: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val bg =
    if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerLow
  val border =
    if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  val contentColor =
    if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

  Surface(
    color = bg,
    border = border,
    shape = MaterialTheme.shapes.medium,
    modifier = modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp),
      )
      Text(text = title, style = MaterialTheme.typography.titleMedium, color = contentColor)
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color =
          if (selected) contentColor.copy(alpha = 0.85f)
          else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
      )
    }
  }
}
