package net.wrlu.remotelogin.hook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import net.wrlu.remotelogin.Config;
import net.wrlu.remotelogin.utils.ContextManager;
import net.wrlu.remotelogin.utils.PackageNames;
import net.wrlu.remotelogin.callback.IntentTransferListener;
import net.wrlu.remotelogin.transfer.WSClient;
import net.wrlu.xposed.framework.HookInterface;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@Deprecated
public class WXRemoteLoginClient implements HookInterface, IntentTransferListener {
    private static final String TAG = "WXRemoteLoginClient";
    @Override
    public void onHookPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        initWebSocket();
        hookWXEntryActivity(loadPackageParam.classLoader);
    }

    @Override
    public boolean isTarget(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        return PackageNames.WEIXIN_PACKAGE.equals(loadPackageParam.packageName) &&
                loadPackageParam.processName.equals(loadPackageParam.packageName);
    }

    private void initWebSocket() {
        WSClient client = WSClient.getInstance();
        client.setIntentTransferListener(this);
        client.connect();
    }

    private void hookWXEntryActivity(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod("com.tencent.mm.plugin.base.stub.WXEntryActivity",
                classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Activity activity = (Activity) param.thisObject;
                        Intent intent = activity.getIntent();

                        if (intent == null) return;

                        sendIntentToRemote(activity, intent);
                        startWXMainAndFinish(activity);
                    }
                });

        XposedHelpers.findAndHookMethod("com.tencent.mm.plugin.base.stub.WXEntryActivity",
                classLoader, "onNewIntent", Intent.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Activity activity = (Activity) param.thisObject;
                        Intent intent = (Intent) param.args[0];

                        if (intent == null) return;

                        sendIntentToRemote(activity, intent);
                        startWXMainAndFinish(activity);
                    }
                });
    }

    private void sendIntentToRemote(Activity activity, Intent intent) {
        WSClient client = WSClient.getInstance();
        client.sendIntent(Config.Role.WEIXIN_HOST, intent);

        Toast.makeText(activity, "已转发拉起微信Request，请查看远端设备。", Toast.LENGTH_LONG).show();
    }

    private void startWXMainAndFinish(Activity activity) {
        PackageManager pm = activity.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(PackageNames.WEIXIN_PACKAGE);
        activity.startActivity(intent);
        activity.finish();
    }

    @Override
    public void onReceiveIntent(String from, Intent intent) {
        Context context = ContextManager.getAppContext();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
