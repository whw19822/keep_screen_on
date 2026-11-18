package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppWithRule
import com.example.ui.AppFilter
import com.example.ui.ScreenKeepUiState
import com.example.ui.components.AppIconImage
import com.example.ui.components.WakeModeBadge

@Composable
fun AppsScreen(
  uiState: ScreenKeepUiState,
  onSearchQueryChange: (String) -> Unit,
  onFilterChange: (AppFilter) -> Unit,
  onRefreshApps: () -> Unit,
  onToggleAppRule: (AppWithRule, Boolean) -> Unit,
  onOpenRuleEditor: (AppWithRule) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        OutlinedTextField(
          value = uiState.searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = {
            Text(
              text = stringResource(R.string.search_apps_placeholder),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.Search,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          },
          trailingIcon = {
            if (uiState.searchQuery.isNotEmpty()) {
              IconButton(
                onClick = { onSearchQueryChange("") },
                modifier = Modifier.testTag("clear_search_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Close,
                  contentDescription = "清除搜索",
                )
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
          modifier = Modifier.weight(1f).testTag("apps_search_input"),
        )

        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          IconButton(
            onClick = onRefreshApps,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("refresh_apps_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = "刷新应用列表",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      ) {
        AppFilterChip(
          selected = uiState.appFilter == AppFilter.USER_ONLY,
          label = stringResource(R.string.filter_user_apps),
          onClick = { onFilterChange(AppFilter.USER_ONLY) },
          tag = "filter_user_apps",
        )
        AppFilterChip(
          selected = uiState.appFilter == AppFilter.ALL,
          label = stringResource(R.string.filter_all_apps),
          onClick = { onFilterChange(AppFilter.ALL) },
          tag = "filter_all_apps",
        )
        AppFilterChip(
          selected = uiState.appFilter == AppFilter.SELECTED_ONLY,
          label = "${stringResource(R.string.filter_selected_only)} (${uiState.configuredRules.size})",
          onClick = { onFilterChange(AppFilter.SELECTED_ONLY) },
          tag = "filter_selected_only",
        )
      }
    }

    if (uiState.isLoadingApps) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else if (uiState.filteredApps.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "没有匹配的应用",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(
          items = uiState.filteredApps,
          key = { it.app.packageName },
        ) { item ->
          AppPickerRow(
            item = item,
            onToggle = { enabled -> onToggleAppRule(item, enabled) },
            onClick = { onOpenRuleEditor(item) },
          )
        }
        item(key = "apps_bottom_spacer") { Spacer(modifier = Modifier.height(16.dp)) }
      }
    }
  }
}

@Composable
private fun AppFilterChip(
  selected: Boolean,
  label: String,
  onClick: () -> Unit,
  tag: String,
) {
  FilterChip(
    selected = selected,
    onClick = onClick,
    label = { Text(label) },
    colors =
      FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
      ),
    modifier = Modifier.testTag(tag),
  )
}

@Composable
private fun AppPickerRow(
  item: AppWithRule,
  onToggle: (Boolean) -> Unit,
  onClick: () -> Unit,
) {
  val isSelected = item.rule?.isEnabled == true
  Surface(
    color =
      if (isSelected) MaterialTheme.colorScheme.surfaceContainerLow
      else MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.medium,
    border =
      BorderStroke(
        width = if (isSelected) 1.25.dp else 1.dp,
        color =
          if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
          else MaterialTheme.colorScheme.outlineVariant,
      ),
    modifier =
      Modifier.fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .clickable(onClick = onClick)
        .testTag("app_item_card_${item.app.packageName}"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
      AppIconImage(
        drawable = item.app.icon,
        contentDescription = item.app.label,
        modifier = Modifier.size(44.dp),
      )

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
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
          if (item.app.isSystemApp) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(
                text = "系统",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }

        Text(
          text = item.app.packageName,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        if (item.rule != null) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp),
          ) {
            WakeModeBadge(wakeMode = item.rule.wakeMode)
            Icon(
              imageVector = Icons.Rounded.Tune,
              contentDescription = "点击自定义规则",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
          }
        }
      }

      Switch(
        checked = isSelected,
        onCheckedChange = onToggle,
        modifier = Modifier.testTag("app_rule_switch_${item.app.packageName}"),
      )
    }
  }
}
