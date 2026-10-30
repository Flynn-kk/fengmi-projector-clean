package com.dsh.peppa.guard;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 开机后分几发延时去拉 Projectivy。
 *
 * 固件会在开机后自己把奇异果桌面拉起来,Projectivy 自己的开机广播偶尔抢不过;
 * 这里在 2~60 秒之间补几刀,确保最后停在 Projectivy 上。每发都会先看前台包名,
 * 已经不在桌面上了就不动(避免把正在看片的用户拽回首页)。
 */
public class BootReceiver extends BroadcastReceiver {

    static final String TAG = "PeppaGuard";

    /** 开机后补刀的时机(毫秒)。 */
    private static final long[] DELAYS_MS = { 2000L, 6000L, 15000L, 30000L, 50000L, 65000L };

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        Log.i(TAG, "boot receiver action=" + action);
        if (context == null) {
            return;
        }
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (int i = 0; i < DELAYS_MS.length; i++) {
            Intent nudge = new Intent(context, NudgeReceiver.class);
            nudge.putExtra(NudgeReceiver.EXTRA_INDEX, i);
            PendingIntent pi = PendingIntent.getBroadcast(
                    context, 1000 + i, nudge, PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                am.setExact(AlarmManager.RTC_WAKEUP, now + DELAYS_MS[i], pi);
            } catch (Throwable t) {
                Log.e(TAG, "setExact failed", t);
            }
        }
    }
}
