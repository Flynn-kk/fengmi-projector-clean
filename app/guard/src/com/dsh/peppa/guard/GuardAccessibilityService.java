package com.dsh.peppa.guard;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * 峰米/FengOS:ROM 只允许系统应用当默认桌面,第三方桌面(Projectivy)永远进不了 HOME 候选,
 * 所以只能从外面抢。这个服务干两件事:
 *   1) 吃掉遥控器 Home 键,直接拉起 Projectivy(根本不给奇异果桌面出场的机会);
 *   2) 盯窗口变化,奇异果桌面 / FallbackHome 一冒头就连着补几枪,把它顶下去(覆盖开机那一下)。
 *
 * 为什么要"连补几枪":AMS 在 HOME 正在 resume 的时候会把别的启动请求直接掐掉
 * (日志: Activity start request from ... stopped),Projectivy 自己的抢占也吃这个亏,
 * 所以单发不可靠,要错开时间补。
 */
public class GuardAccessibilityService extends AccessibilityService {

    static final String TAG = "PeppaGuard";

    static final String STOCK_PKG = "com.tvfengmi.launcher";
    static final String FALLBACK_PKG = "com.android.tv.settings";
    static final String FALLBACK_CLS = "FallbackHome";

    private static final long[] BURST_MS = { 0L, 500L, 1500L, 3000L, 5000L, 7500L, 10000L, 13000L };

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastBurstMs;

    /** 最近处于前台的包名,给开机补刀那几发判断用。 */
    static volatile String foregroundPkg;

    @Override
    public void onServiceConnected() {
        try {
            Log.i(TAG, "connected flags=0x" + Integer.toHexString(getServiceInfo().flags)
                    + " caps=" + getServiceInfo().getCapabilities());
        } catch (Throwable t) {
            Log.e(TAG, "dump info failed", t);
        }
        if (STOCK_PKG.equals(foregroundPkg)) {
            burst("service-connected");
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        CharSequence pkgSeq = event.getPackageName();
        if (pkgSeq == null) {
            return;
        }
        String pkg = pkgSeq.toString();
        foregroundPkg = pkg;

        CharSequence clsSeq = event.getClassName();
        String cls = clsSeq == null ? "" : clsSeq.toString();

        if (STOCK_PKG.equals(pkg)) {
            burst("stock-home");
        } else if (FALLBACK_PKG.equals(pkg) && cls.contains(FALLBACK_CLS)) {
            burst("fallback-home");
        }
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event == null || event.getKeyCode() != KeyEvent.KEYCODE_HOME) {
            return false;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            boolean ok = Launcher.start(this);
            Log.i(TAG, "home-key consumed ok=" + ok);
            return ok;
        }
        return true;
    }

    @Override
    public void onInterrupt() {
    }

    /** 错开时间连续拉几次 Projectivy。 */
    private void burst(String why) {
        long now = SystemClock.uptimeMillis();
        if (now - lastBurstMs < 1500L) {
            return;
        }
        lastBurstMs = now;
        Log.i(TAG, "burst via " + why);
        for (long delay : BURST_MS) {
            if (delay == 0L) {
                Launcher.start(this);
            } else {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Launcher.start(GuardAccessibilityService.this);
                    }
                }, delay);
            }
        }
    }
}

