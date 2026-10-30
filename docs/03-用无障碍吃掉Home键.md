# 03 · 用无障碍吃掉遥控器 Home 键

整个方案最关键的发现。

## 普通应用拦不住 Home 键

Android 从很早就不让普通 App 拦截 Home 键，`onKeyDown` 里收到 HOME 也无能为力。

## 但无障碍服务可以

`AccessibilityService` 提供了 `onKeyEvent`，只要在服务配置里声明"要过滤按键"：

```xml
<accessibility-service
    android:canRequestFilterKeyEvents="true"
    android:accessibilityFlags="flagRequestFilterKeyEvents"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:canRetrieveWindowContent="false"
    android:notificationTimeout="200" />
```

运行时的 `AccessibilityServiceInfo.flags` 里会出现 `0x20`（`FLAG_REQUEST_FILTER_KEY_EVENTS`）。

然后：

```java
@Override
public boolean onKeyEvent(KeyEvent event) {
    if (event.getKeyCode() != KeyEvent.KEYCODE_HOME) return false;
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
        Launcher.start(this);   // 拉起 Projectivy
        return true;            // 吃掉，系统不会发 HOME intent
    }
    return true;                // 抬起也吃掉
}
```

返回 `true` 就表示这个按键被消费，系统的 HOME 处理不会发生 —— 奇异果桌面根本没机会出场。

## 一个必须知道的测试坑

**`adb shell input keyevent 3` 注入的事件不走这个过滤。**

`input` 是通过 `InputManager.injectInputEvent` 注入到系统层的，绕过了无障碍的按键过滤链。
所以用 adb 测，看起来"没生效"，但用真遥控器就是好的。

正确验法：

1. 在投影仪上按遥控器的 Home 键
2. 立刻看日志：

```powershell
adb logcat -d | findstr PeppaGuard
# 期望看到：home-key consumed ok=true
# 同时不该有：START u0 {cmp=com.tvfengmi.launcher/...}
```

实测三次按键，`home-key consumed ok=true` ×3，同期 `START ... tvfengmi` 0 次，
用户确认"按 Home 直接回 Projectivy，没有闪屏"。

## 代价

无障碍服务拿到按键过滤能力后，会参与所有按键的分发。这个服务只处理 HOME、
其它按键立刻 `return false` 交还系统，所以不影响音量、方向键。

（对比：创维那台上曾因为另一个带按键过滤能力的无障碍服务，导致音量键"按一下就一直加"，
见 skyworth-tv-clean 仓库的踩坑记录。同一类能力，用不好会踩雷。）
