package com.dsh.peppa.guard;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/** 统一入口:把 Projectivy 桌面拉到前台。 */
final class Launcher {

    static final String HOME_PKG = "com.spocky.projengmenu";
    static final String HOME_CLS = "com.spocky.projengmenu.ui.home.MainActivity";

    private Launcher() {
    }

    /** @return 是否成功把 Intent 发出去(找不到 Projectivy 时返回 false)。 */
    static boolean start(Context ctx) {
        if (ctx == null) {
            return false;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName(HOME_PKG, HOME_CLS));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            ctx.startActivity(intent);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
