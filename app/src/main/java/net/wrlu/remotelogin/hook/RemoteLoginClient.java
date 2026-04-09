package net.wrlu.remotelogin.hook;

import android.content.ComponentName;
import android.content.Intent;

import net.wrlu.remotelogin.transfer.Role;

public class RemoteLoginClient extends AbsRemoteLogin {
    private static final String TAG = "RemoteLoginClient";

    @Override
    public boolean isTargetIntent(Intent intent) {
        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            return componentName.getPackageName().equals("com.tencent.mm") &&
                    componentName.getClassName().equals("com.tencent.mm.plugin.base.stub.WXEntryActivity");
        }
        return false;
    }

    @Override
    public void onReceiveIntent(String from, Intent intent) {
        HookActivityStarter.startActivity("android", intent, 0);
    }

    @Override
    public String getReceiverDeviceId() {
        return Role.SUPER_HOST;
    }
}
