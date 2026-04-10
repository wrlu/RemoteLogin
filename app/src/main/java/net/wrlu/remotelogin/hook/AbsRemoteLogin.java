package net.wrlu.remotelogin.hook;

import android.content.Intent;

import net.wrlu.remotelogin.callback.ActivityStartInterceptor;
import net.wrlu.remotelogin.callback.IntentTransferListener;
import net.wrlu.remotelogin.Config;
import net.wrlu.remotelogin.transfer.WSClient;
import net.wrlu.remotelogin.utils.ContextManager;

public abstract class AbsRemoteLogin implements ActivityStartInterceptor, IntentTransferListener {
    final WSClient mWSClient;
    final HookActivityStarter mActivityStarterHooker;

    public AbsRemoteLogin() {
        mWSClient = WSClient.getInstance();
        mWSClient.setIntentTransferListener(this);

        mActivityStarterHooker = new HookActivityStarter();
        mActivityStarterHooker.setInterceptor(this);
    }

    public static AbsRemoteLogin create() {
        return isSuperRole() ? new RemoteLoginServer() : new RemoteLoginClient();
    }

    public void init(ClassLoader classLoader) {
        mWSClient.connect();
        mActivityStarterHooker.startHook(classLoader);
    }

    public static boolean isSuperRole() {
        return Config.Role.SUPER_HOST.equals(Config.get(ContextManager.getSystemContext(),
                Config.Role.CONFIG_NAME));
    }

    @Override
    public boolean onInterceptStartActivity(Intent intent) {
        String toDeviceId = getReceiverDeviceId();
        if (isTargetIntent(intent) && toDeviceId != null) {
            mWSClient.sendIntent(toDeviceId, intent);
            return true;
        }
        return false;
    }

    public abstract String getReceiverDeviceId();

    public abstract boolean isTargetIntent(Intent intent);
}
