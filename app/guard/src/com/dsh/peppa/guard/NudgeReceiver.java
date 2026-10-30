package com.dsh.peppa.guard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 开机补刀的执行端:只有当前台确实是奇异果桌面 / FallbackHome 时才拉 Projectivy,
 * 用户已经在看别的 App 就不打扰。
 */
public class NudgeReceiver extends BroadcastReceiver {

    static final String TAG = "PeppaGuard";
    static final String EXTRA_INDEX = "index";

    /** 前几发在无障碍服务还没连上时,允许盲拉(只在开机头十几秒)。 */
    private static final int BLIND_INDEX_MAX = 2;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) {
            return;
        }
        int index = intent == null ? -1 : intent.getIntExtra(EXTRA_INDEX, -1);
        String fg = GuardAccessibilityService.foregroundPkg;

        if (fg == null) {
            if (index >= 0 && index <= BLIND_INDEX_MAX) {
                Log.i(TAG, "nudge #" + index + " blind launch");
                Launcher.start(context);
            }
            return;
        }
        if (Launcher.HOME_PKG.equals(fg)) {
            return;
        }
        if (GuardAccessibilityService.STOCK_PKG.equals(fg)
                || GuardAccessibilityService.FALLBACK_PKG.equals(fg)) {
            Log.i(TAG, "nudge #" + index + " fg=" + fg + " -> projectivy");
            Launcher.start(context);
        }
    }
}
