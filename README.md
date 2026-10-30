# 峰米投影仪（小明 Q3 Pro）：把强推的奇异果桌面换掉

> FENG OS 从 2025 年起强推**奇异果（GITV/银河）桌面**，第三方桌面永远进不了"默认桌面"候选。
> 用一个无障碍服务吃掉遥控器 Home 键 + 窗口一冒头就连环补刀，把它稳定压住。
> 不 root、不禁用系统桌面、随时可卸。

<table>
<tr><td align="center">系统强推的奇异果桌面</td><td align="center">Projectivy 桌面</td></tr>
<tr>
<td><img src="assets/screenshots/奇异果桌面.jpg" width="420"></td>
<td><img src="assets/screenshots/Projectivy桌面.jpg" width="420"></td>
</tr>
</table>

## 这台设备

| 项 | 值 |
|---|---|
| 市场名 | 小明 Q3 Pro（Xiaomi Peppa Series Projector） |
| 品牌 | 峰米 Fengmi，FENG OS |
| 系统 | Android 9（SDK 28），Amlogic |
| 屏幕 | 1920×1080 / density 320 |
| SELinux | **enforcing**，无 root（只有 uid 2000 shell） |
| 固件 | `PPR1.180610.011.3.1.1.3775` |
| 自带桌面 | `com.tvfengmi.launcher` v14.2.0.175155（`/system/priv-app/GitvVideo`），首页 `com.gala.video.app.epg.HomeActivity` |
| 兜底桌面 | `com.android.tv.settings/.system.FallbackHome`（显示 FengOS 开机/自动对焦画面） |
| 替代桌面 | Projectivy Launcher `com.spocky.projengmenu` v4.63 |
| 守卫 | `com.dsh.peppa.guard`（本仓库 `app/guard/`） |

## 为什么不能"简单设个默认桌面"

这台 ROM 有三条硬限制，全部实测：

1. **只允许系统应用当默认桌面**。Projectivy、AS Launcher 都声明了 HOME，但
   `cmd package set-home-activity` 直接报 `IllegalArgumentException: ... cannot be home on user 0`。
2. **禁用奇异果桌面 = Home 键掉进 FallbackHome**。按 Home 会显示 FengOS「自动调焦中」，
   等 4~6 秒才被抢回来。所以**不能禁它**。
3. **AMS 会在 HOME 启动的那几秒掐掉别的启动请求**（`Activity start request from <uid> stopped`）。
   这期间无论是替代桌面自己还是第三方去 `startActivity` 都会被拒，约 5 秒后才放行。
   纯靠"抢"一定有 3~6 秒闪屏。

## 解法：从按键这一层截胡

关键发现：**无障碍服务能吃到物理遥控器的 Home 键**。

```java
@Override
public boolean onKeyEvent(KeyEvent event) {
    if (event.getKeyCode() != KeyEvent.KEYCODE_HOME) return false;
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
        Launcher.start(this);   // 直接拉起 Projectivy
        return true;            // 吃掉，系统根本不会发 HOME intent
    }
    return true;
}
```

配 `android:canRequestFilterKeyEvents="true"` +
`android:accessibilityFlags="flagRequestFilterKeyEvents"`。

**注意**：`adb shell input keyevent 3` 注入的事件**不走**这个过滤，所以用 adb 测不出效果，
必须按真遥控器。日志里能看到 `home-key consumed ok=true`，同期 `START ... tvfengmi` 为 0 次。

按键这一层解决了"按 Home 闪 3~6 秒"的问题；开机那一下还是会被系统桌面占住，所以再加一层补刀：

| 组件 | 作用 |
|---|---|
| `GuardAccessibilityService.onKeyEvent` | 遥控器 Home 按下 → 拉起 Projectivy 并吃掉按键 |
| `GuardAccessibilityService.onAccessibilityEvent` | 窗口变成 `com.tvfengmi.launcher` 或 `FallbackHome` → 按 `{0,500,1500,3000,5000,7500,10000,13000}ms` 连环补枪 |
| `BootReceiver` | `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` → AlarmManager 在 2/6/15/30/50/65 秒各补一发 |
| `NudgeReceiver` | 补刀执行端；只在**前台确实是奇异果/FallbackHome**时才拉，用户在别的 App 里不打扰 |
| `MainActivity` | 只用来解除安装后的 stopped 状态 |

## 部署

```powershell
# 1. 编译（需要 JDK17 + android.jar + aapt2 + r8，见 app/guard/README）
.\app\guard\build.ps1 -Toolchain D:\path\to\toolchain

# 2. 装
adb connect <PROJECTOR_IP>:5555
adb install -r .\app\guard\peppa-guard.apk

# 3. 解 stopped（必须，否则开机广播收不到、Activity 启动被拒）
adb shell am start -n com.dsh.peppa.guard/.MainActivity
adb shell input keyevent KEYCODE_BACK

# 4. 把这个服务加进无障碍（保留已有的，别覆盖）
adb shell settings get secure enabled_accessibility_services
# 假设原值是 A，则设为 A:com.dsh.peppa.guard/com.dsh.peppa.guard.GuardAccessibilityService
adb shell settings put secure enabled_accessibility_services "<A>:com.dsh.peppa.guard/com.dsh.peppa.guard.GuardAccessibilityService"
adb shell settings put secure accessibility_enabled 1
```

## 验收

```powershell
# 按遥控器 Home 键（不是 adb 注入），然后看日志
adb logcat -d | findstr PeppaGuard
# 期望：home-key consumed ok=true，且没有 START com.tvfengmi
```

实测结果：遥控器按 Home **直接回 Projectivy**，无闪屏；连续重启 3 次全部落在 Projectivy。

## 回滚

```powershell
.\app\guard\uninstall.ps1 -Device <PROJECTOR_IP>:5555
```

只会把守卫从无障碍列表摘掉再卸载。**奇异果桌面全程没被禁用**，所以不需要恢复它。

## 相关

同一套思路在创维电视上不完全适用（那边是固件读 `persist` 属性），见
[skyworth-tv-clean](https://github.com/Flynn-kk/skyworth-tv-clean)。

## License

MIT
