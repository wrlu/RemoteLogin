package net.wrlu.remotelogin.hook;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import net.wrlu.remotelogin.callback.IntentTransferListener;
import net.wrlu.remotelogin.Config;
import net.wrlu.remotelogin.transfer.WSClient;
import net.wrlu.remotelogin.utils.ContextManager;
import net.wrlu.remotelogin.utils.PackageNames;
import net.wrlu.xposed.framework.HookInterface;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@Deprecated
public class WXRemoteLoginServer implements HookInterface, IntentTransferListener {
    private static final String TAG = "WXRemoteLoginServer";
    private transient String fromDeviceId = null;

    @Override
    public void onHookPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        initWebSocket();
        hookWxApiActivity(loadPackageParam.packageName, loadPackageParam.classLoader);
    }

    @Override
    public boolean isTarget(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        return !PackageNames.WEIXIN_PACKAGE.equals(loadPackageParam.packageName) &&
                !"android".equals(loadPackageParam.packageName) &&
                !"system".equals(loadPackageParam.packageName) &&
                loadPackageParam.processName.equals(loadPackageParam.packageName);
    }

    private void initWebSocket() {
        WSClient client = WSClient.getInstance();
        client.setIntentTransferListener(this);
        client.connect();
        client.registerHost(Config.Role.WEIXIN_HOST);
    }

    @Override
    public void onReceiveIntent(String from, Intent intent) {
        fromDeviceId = from;

        if (isWxEntry(intent)) {
            Context context = ContextManager.getAppContext();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            Log.w(TAG, "onReceiveIntent: checkIntent failed.");
        }
    }

    private boolean isWxEntry(Intent intent) {
        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            return componentName.getPackageName().equals("com.tencent.mm") &&
                    componentName.getClassName().equals("com.tencent.mm.plugin.base.stub.WXEntryActivity");
        }
        return false;
    }

    private void hookWxApiActivity(String packageName, ClassLoader classLoader) {
        String wxApiActivityName = packageName + ".wxapi.WXEntryActivity";

        XposedHelpers.findAndHookMethod(
                android.app.Activity.class,
                "onCreate", Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String className = param.thisObject.getClass().getName();
                        if (className.equals(wxApiActivityName)) {
                            Activity activity = (Activity) param.thisObject;
                            Intent intent = activity.getIntent();

                            if (intent == null) return;

                            sendIntentToRemote(intent);
                        }
                    }
                });

        XposedHelpers.findAndHookMethod(
                android.app.Activity.class,
                "onNewIntent", Intent.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String className = param.thisObject.getClass().getName();
                        if (className.equals(wxApiActivityName)) {
                            Intent intent = (Intent) param.args[0];

                            if (intent == null) return;

                            sendIntentToRemote(intent);
                        }
                    }
                });
    }

    private void sendIntentToRemote(Intent intent) {
        WSClient client = WSClient.getInstance();
        client.sendIntent(fromDeviceId, intent);
        fromDeviceId = null;
    }
}
