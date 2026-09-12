package com.termux.app.droiduse;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import rikka.shizuku.Shizuku;

/** Foreground capability probe. No background automation or arbitrary privileged commands. */
public class PhoneToolsActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView status;
    private TextView result;
    private LinearLayout apps;
    private Button refreshApps;
    private boolean loading;
    private final Shizuku.OnBinderReceivedListener received = () -> runOnUiThread(this::refreshStatus);
    private final Shizuku.OnBinderDeadListener dead = () -> runOnUiThread(this::refreshStatus);
    private final Shizuku.OnRequestPermissionResultListener permission = (request, grant) ->
        runOnUiThread(() -> {
            if (request != 701) return;
            refreshStatus();
            show(grant == PackageManager.PERMISSION_GRANTED ? "Shizuku 授权成功" : "授权被拒绝，可在 Shizuku 中修改授权");
        });

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        page.setPadding(pad, pad, pad, pad);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(page);
        setContentView(scroll);
        TextView title = new TextView(this);
        title.setText("DroidUse 手机能力"); title.setTextSize(24); page.addView(title);
        status = new TextView(this); page.addView(status);
        result = new TextView(this); result.setTextIsSelectable(true); page.addView(result);
        button(page, "刷新连接状态", this::refreshStatus);
        button(page, "申请 Shizuku 授权", this::authorize);
        button(page, "查询 Shizuku 运行身份", this::probeIdentity);
        refreshApps = button(page, "查询可启动应用", this::listApps);
        button(page, "返回终端", this::finish);
        TextView note = new TextView(this);
        note.setText("应用列表与启动使用 Android 原生接口，不需要 Shizuku。运行身份查询使用 Shizuku。点击应用只请求启动，不代表后续任务完成。");
        page.addView(note);
        apps = new LinearLayout(this); apps.setOrientation(LinearLayout.VERTICAL); page.addView(apps);
        Shizuku.addBinderReceivedListenerSticky(received);
        Shizuku.addBinderDeadListener(dead);
        Shizuku.addRequestPermissionResultListener(permission);
        refreshStatus();
    }

    private Button button(LinearLayout parent, String text, Runnable action) {
        Button button = new Button(this); button.setText(text);
        button.setOnClickListener(v -> action.run()); parent.addView(button); return button;
    }
    private boolean live() { return !isFinishing() && !isDestroyed(); }
    private void show(String text) { if (live()) result.setText(text); }
    private boolean available() {
        if (!Shizuku.pingBinder()) { show("Shizuku 未连接，请先通过无线调试启动 Shizuku，再返回刷新"); return false; }
        if (Shizuku.isPreV11()) { show("Shizuku 版本过旧，请升级"); return false; }
        return true;
    }
    private void refreshStatus() {
        if (!live()) return;
        try {
            if (!Shizuku.pingBinder()) { status.setText("Shizuku：未连接"); return; }
            if (Shizuku.isPreV11()) { status.setText("Shizuku：版本不支持"); return; }
            status.setText(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                ? "Shizuku：已连接、已授权" : "Shizuku：已连接、未授权");
        } catch (RuntimeException e) { status.setText("Shizuku：连接失效，请重试"); }
    }
    private void authorize() {
        try {
            if (!available()) return;
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) { show("已经授权"); return; }
            if (Shizuku.shouldShowRequestPermissionRationale()) { show("请打开 Shizuku 的应用管理，允许 DroidUse 使用"); return; }
            Shizuku.requestPermission(701);
        } catch (RuntimeException e) { show("授权请求失败：" + e.getClass().getSimpleName()); }
        refreshStatus();
    }
    private void probeIdentity() {
        try {
            if (!available()) return;
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) { show("请先授权"); return; }
            int uid = Shizuku.getUid();
            show("Shizuku 运行 UID：" + uid + (uid == 2000 ? "（ADB / shell）" : uid == 0 ? "（Root）" : "（其他身份）"));
        } catch (RuntimeException e) { show("查询失败：连接中断或权限已撤销"); }
        refreshStatus();
    }
    private void listApps() {
        if (loading) return;
        loading = true; refreshApps.setEnabled(false); apps.removeAllViews(); show("正在查询可启动应用…");
        worker.execute(() -> {
            try {
                PackageManager pm = getPackageManager();
                Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
                Map<String, String> unique = new TreeMap<>();
                for (ResolveInfo info : pm.queryIntentActivities(intent, 0)) {
                    if (info.activityInfo != null && info.activityInfo.exported && info.activityInfo.enabled)
                        unique.put(info.activityInfo.packageName, info.loadLabel(pm).toString());
                }
                runOnUiThread(() -> {
                    if (!live()) return;
                    loading = false; refreshApps.setEnabled(true);
                    for (Map.Entry<String, String> entry : unique.entrySet()) {
                        final String pkg = entry.getKey();
                        button(apps, entry.getValue() + "\n" + pkg, () -> launch(pkg));
                    }
                    show("找到 " + unique.size() + " 个可启动应用（非全部已安装包）");
                });
            } catch (RuntimeException e) {
                runOnUiThread(() -> {
                    if (!live()) return;
                    loading = false; refreshApps.setEnabled(true); show("应用查询失败：" + e.getClass().getSimpleName());
                });
            }
        });
    }
    private void launch(String pkg) {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(pkg);
            if (intent == null) { show("应用已卸载或没有可启动入口"); return; }
            startActivity(intent); show("已请求启动：" + pkg + "，请检查实际页面");
        } catch (RuntimeException e) { show("启动失败：" + e.getClass().getSimpleName()); }
    }
    @Override protected void onResume() { super.onResume(); if (status != null) refreshStatus(); }
    @Override protected void onDestroy() {
        Shizuku.removeBinderReceivedListener(received);
        Shizuku.removeBinderDeadListener(dead);
        Shizuku.removeRequestPermissionResultListener(permission);
        worker.shutdownNow(); super.onDestroy();
    }
}
