package com.dsh.peppa.guard;

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 只是个"存在证明"页面:装好后启动一次,应用就不处于 stopped 状态了
 * (Android 会拦掉 stopped 应用发起的 Activity 启动和开机广播)。
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView title = new TextView(this);
        title.setText("桌面守卫 已安装");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        title.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView body = new TextView(this);
        body.setText("\n• Home 键直接回 Projectivy\n"
                + "• 开机后自动拉回 Projectivy\n"
                + "• 检测到奇异果桌面/FallbackHome 会立刻切回\n\n"
                + "本页面只是让系统解除\"stopped\"状态,按返回退出即可。");
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        body.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int pad = (int) (48 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);
        root.addView(title);
        root.addView(body);
        setContentView(root);
    }
}
