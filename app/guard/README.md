# 桌面守卫（com.dsh.peppa.guard）

约 9 KB 的 APK：一个无障碍服务 + 两个广播接收器 + 一个空壳 Activity。

## 工具链

和 skyworth-tv-clean 用的是同一套，放在 `app/toolchain/` 或任意目录后用 `-Toolchain` 指定：

| 东西 | 从哪拿 |
|---|---|
| JDK 17 | <https://repo.huaweicloud.com/openjdk/17.0.2/openjdk-17.0.2-windows-x64_bin.zip> |
| `android-30.jar` | <https://raw.githubusercontent.com/Sable/android-platforms/master/android-30/android.jar> |
| `r8.jar` | <https://maven.aliyun.com/repository/google/com/android/tools/r8/8.5.35/r8-8.5.35.jar> |
| `aapt2` | <https://maven.aliyun.com/repository/google/com/android/tools/build/aapt2/7.4.2-8841542/aapt2-7.4.2-8841542-windows.jar> |
| `guard.keystore` | 自己生成（见下） |

目录结构要求：

```
toolchain/
├── jdk-17.0.2/bin/{javac,java,jarsigner}.exe
├── aapt2/aapt2.exe
├── android-30.jar
├── r8.jar
└── guard.keystore
```

自己生成 keystore（别名 `guard`，口令 `guardpass`，脚本里写死了这两个值）：

```powershell
keytool -genkeypair -keystore guard.keystore -alias guard -keyalg RSA -keysize 2048 `
        -validity 10000 -storepass guardpass -keypass guardpass -dname "CN=PeppaGuard"
```

## 编译

```powershell
.\build.ps1 -Toolchain D:\path\to\toolchain
# 产物：peppa-guard.apk
```

六步：`aapt2 compile` → `aapt2 link` → `javac -encoding UTF-8` → `d8` → 打包 `classes.dex` →
`jarsigner`（v1 签名，targetSdk 27，Android 9 认）。

## 装

```powershell
adb install -r .\peppa-guard.apk
adb shell am start -n com.dsh.peppa.guard/.MainActivity   # 解 stopped，必须
adb shell input keyevent KEYCODE_BACK
```

然后把服务加进无障碍（见仓库根 README 的部署段）。

## 卸

```powershell
.\uninstall.ps1 -Device 192.168.1.60:5555
```
