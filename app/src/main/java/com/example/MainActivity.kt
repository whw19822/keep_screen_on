package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.service.UsageStatsHelper
import com.example.ui.ScreenKeepViewModel
import com.example.ui.components.RuleConfigBottomSheet
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ScreenKeepTheme

private enum class TopDestination(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
) {
  HOME("home", R.string.nav_home, Icons.Rounded.WbSunny, Icons.Outlined.WbSunny, "nav_home_tab"),
  APPS("apps", R.string.nav_apps, Icons.Rounded.Apps, Icons.Outlined.Apps, "nav_apps_tab"),
  SETTINGS(
    "settings",
    R.string.nav_settings,
    Icons.Rounded.Settings,
    Icons.Outlined.Settings,
    "nav_settings_tab",
  ),
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { ScreenKeepTheme { ScreenKeepRootApp() } }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenKeepRootApp(viewModel: ScreenKeepViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = backStackEntry?.destination

  val notificationLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
      onResult = { viewModel.refreshPermissions() },
    )

  val requestNotificationPermission = {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissions() }

  val navigateToTab: (TopDestination) -> Unit = { dest ->
    navController.navigate(dest.route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useNavigationRail) {
        NavigationRail(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          modifier =
            Modifier.fillMaxHeight()
              .windowInsetsPadding(WindowInsets.statusBars)
              .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
          TopDestination.entries.forEach { dest ->
            val selected =
              currentDestination?.hierarchy?.any { it.route == dest.route } == true
            NavigationRailItem(
              selected = selected,
              onClick = { navigateToTab(dest) },
              icon = {
                Icon(
                  imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                  contentDescription = stringResource(dest.labelRes),
                )
              },
              label = { Text(stringResource(dest.labelRes)) },
              modifier = Modifier.testTag(dest.testTag),
            )
          }
        }
      }

      Scaffold(
        modifier = Modifier.weight(1f).fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
          CenterAlignedTopAppBar(
            title = {
              val titleRes =
                when (currentDestination?.route) {
                  TopDestination.APPS.route -> R.string.nav_apps
                  TopDestination.SETTINGS.route -> R.string.nav_settings
                  else -> R.string.app_name
                }
              Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleLarge)
            },
            colors =
              TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
              ),
          )
        },
        bottomBar = {
          if (!useNavigationRail) {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              windowInsets = WindowInsets.navigationBars,
            ) {
              TopDestination.entries.forEach { dest ->
                val selected =
                  currentDestination?.hierarchy?.any { it.route == dest.route } == true
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateToTab(dest) },
                  icon = {
                    Icon(
                      imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                      contentDescription = stringResource(dest.labelRes),
                    )
                  },
                  label = { Text(stringResource(dest.labelRes)) },
                  modifier = Modifier.testTag(dest.testTag),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Box(
          modifier =
            Modifier.fillMaxSize()
              .padding(innerPadding)
              .let { mod ->
                if (useNavigationRail) mod.windowInsetsPadding(WindowInsets.navigationBars)
                else mod
              },
          contentAlignment = Alignment.TopCenter,
        ) {
          NavHost(
            navController = navController,
            startDestination = TopDestination.HOME.route,
            modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
          ) {
            composable(TopDestination.HOME.route) {
              HomeScreen(
                uiState = uiState,
                onToggleMasterService = { enable ->
                  if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !uiState.hasNotificationPermission) {
                    requestNotificationPermission()
                  }
                  viewModel.toggleMasterService(enable)
                },
                onRequestUsagePermission = { UsageStatsHelper.openUsageAccessSettings(context) },
                onRequestOverlayPermission = { UsageStatsHelper.openOverlaySettings(context) },
                onRequestNotificationPermission = requestNotificationPermission,
                onToggleRuleEnabled = { item, enabled ->
                  viewModel.toggleAppSelection(item, enabled)
                },
                onEditRule = { item -> viewModel.openRuleEditor(item) },
                onNavigateToApps = { navigateToTab(TopDestination.APPS) },
              )
            }

            composable(TopDestination.APPS.route) {
              AppsScreen(
                uiState = uiState,
                onSearchQueryChange = viewModel::setSearchQuery,
                onFilterChange = viewModel::setAppFilter,
                onRefreshApps = viewModel::refreshInstalledApps,
                onToggleAppRule = viewModel::toggleAppSelection,
                onOpenRuleEditor = viewModel::openRuleEditor,
              )
            }

            composable(TopDestination.SETTINGS.route) {
              SettingsScreen(
                uiState = uiState,
                onDefaultWakeModeChange = viewModel::setDefaultWakeMode,
                onDefaultMaxDurationChange = viewModel::setDefaultMaxDurationMinutes,
                onPollingIntervalChange = viewModel::setPollingIntervalMs,
                onPauseOnLowBatteryChange = viewModel::setPauseOnLowBattery,
                onLowBatteryThresholdChange = viewModel::setLowBatteryThreshold,
                onStartOnBootChange = viewModel::setStartOnBoot,
                onOpenUsageAccessSettings = { UsageStatsHelper.openUsageAccessSettings(context) },
                onOpenOverlaySettings = { UsageStatsHelper.openOverlaySettings(context) },
                onRequestNotificationPermission = requestNotificationPermission,
                onOpenBatteryOptimizationSettings = {
                  UsageStatsHelper.openBatteryOptimizationSettings(context)
                },
                onClearAllRules = viewModel::clearAllRules,
              )
            }
          }
        }
      }
    }
  }

  val editingApp = uiState.editingApp
  if (editingApp != null) {
    RuleConfigBottomSheet(
      item = editingApp,
      defaultWakeMode = uiState.settings.defaultWakeMode,
      defaultMaxDurationMinutes = uiState.settings.defaultMaxDurationMinutes,
      onDismiss = viewModel::closeRuleEditor,
      onSave = { enabled, mode, duration ->
        viewModel.saveRule(
          packageName = editingApp.app.packageName,
          appLabel = editingApp.app.label,
          isEnabled = enabled,
          wakeMode = mode,
          maxDurationMinutes = duration,
        )
      },
      onRemove = { viewModel.removeRule(editingApp.app.packageName) },
    )
  }
}
