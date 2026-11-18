# 常亮助手 (ScreenKeep)

**常亮助手** 是一款基于 Kotlin 与 Jetpack Compose 构建的 Android 实用工具应用，能够在用户指定的目标应用处于前台时自动保持屏幕常亮，并在切换离开或锁屏后自动释放常亮锁以节省电量。

## 核心功能

- **前台应用自动监测**：基于系统 `UsageStatsManager`（使用情况访问权限）在本地实时监测当前处于前台的应用，无需无障碍服务权限，不收集或上传任何隐私数据。
- **单应用精细化规则**：
  - **常亮模式**：支持为每个应用单独选择「高亮常亮（`BRIGHT`）」或「微暗唤醒（`DIM`，允许屏幕变暗但不熄屏锁屏）」。
  - **安全定时**：支持设置单次最长常亮时长（不限时 / 15 / 30 / 60 / 120 分钟），超时自动释放唤醒锁。
- **应用列表与筛选**：通过 Android `<queries>` 机制获取可启动应用列表（无需 `QUERY_ALL_PACKAGES` 权限），支持按「仅用户应用」「全部应用」「已添加」筛选以及应用名称/包名搜索。
- **电源保护与监听策略**：
  - **低电量保护**：未充电且电量低于设定阈值（5%–40% 可调）时自动暂停常亮锁。
  - **锁屏即释**：监听系统 `ACTION_SCREEN_OFF` / `ACTION_SCREEN_ON` 广播，手动熄屏后立即释放 `WakeLock` 并暂停轮询。
  - **灵活配置**：支持自定义默认常亮模式、默认定时时长、前台检测间隔（1.0s / 1.5s / 3.0s）及开机自启。
- **系统快捷磁贴**：内置 `KeepAwakeTileService`，可在系统下拉控制中心添加「前台常亮」快捷开关，随时一键启停监听服务。

## 权限说明

| 权限 | 用途 |
| --- | --- |
| `PACKAGE_USAGE_STATS` | 在本地查询前台运行的应用切换事件（需用户在系统设置中手动授权） |
| `SYSTEM_ALERT_WINDOW` | 通过 1px 透明 `FLAG_KEEP_SCREEN_ON` 悬浮视图稳定防熄屏，兼容现代 Android 与各类 OEM 系统 |
| `WAKE_LOCK` | 在匹配到目标应用时维持屏幕唤醒状态（兼容回退引擎） |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | 保持前台监测服务稳定运行并展示实时状态通知 |
| `POST_NOTIFICATIONS` | 在 Android 13+ 设备上显示服务状态通知与快捷停止按钮 |
| `RECEIVE_BOOT_COMPLETED` | 当用户开启「开机自动启动监听」后，在系统重启时恢复服务 |

## 技术栈与项目结构

- **UI 与架构**：Jetpack Compose (Material Design 3)、自适应 `NavigationBar` / `NavigationRail`、Navigation Compose、MVVM (`ViewModel` + `StateFlow`)
- **本地持久化**：Room（存储单应用常亮规则）与 DataStore Preferences（存储全局监听与电源策略）
- **核心模块**：
  - `app/src/main/java/com/example/service/KeepAwakeService.kt`：前台监听服务与 `WakeLock` 生命周期管理
  - `app/src/main/java/com/example/service/UsageStatsHelper.kt`：使用情况权限检查与前台应用事件解析
  - `app/src/main/java/com/example/service/KeepAwakeTileService.kt`：系统快捷设置磁贴服务
  - `app/src/main/java/com/example/data/repository/InstalledAppsRepository.kt`：基于 `PackageManager` 的可启动应用发现
  - `app/src/main/java/com/example/data/local/`：Room 数据库、DAO 与 DataStore 设置管理
  - `app/src/main/java/com/example/ui/`：首页监控面板、应用选择页、设置页与规则配置弹窗

## 构建与测试

- **运行本地单元测试与 Robolectric 测试**：
  ```bash
  gradle :app:testDebugUnitTest
  ```
- **构建 Debug APK**：
  ```bash
  gradle assembleDebug
  ```

## 使用指南

1. 启动应用后，在首页点击 **去授权**，前往系统设置开启 **使用情况访问权限**（Android 13+ 建议同步允许通知权限）。
2. 切换至 **应用** 标签页，勾选需要保持常亮的应用，或点击应用条目自定义「高亮/微暗」模式与安全时长。
3. 回到 **监控** 页面开启主开关，或通过系统下拉快捷磁贴一键开启；打开目标应用即可自动保持屏幕常亮。
