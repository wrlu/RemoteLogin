package net.wrlu.remotelogin.hook;

import net.wrlu.xposed.framework.HookInterface;

import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookFinishBooting implements HookInterface {
    private static boolean isInitialized = false;

    @Override
    public void onHookPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        startHook(loadPackageParam.classLoader);
    }

    @Override
    public boolean isTarget(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        return "android".equals(loadPackageParam.packageName);
    }

    public void startHook(ClassLoader classLoader) {
        Class<?> amsClass = XposedHelpers.findClass(
                "com.android.server.am.ActivityManagerService", classLoader);

        XposedBridge.hookAllMethods(amsClass, "finishBooting", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (isInitialized) return;
                isInitialized = true;

                Thread thread = new BootInitThread(classLoader);
                thread.setDaemon(true);
                thread.start();
            }
        });
    }

    static class BootInitThread extends Thread {
        private final ClassLoader mClassLoader;

        public BootInitThread(ClassLoader classLoader) {
            mClassLoader = classLoader;
        }

        @Override
        public void run() {
            AbsRemoteLogin remoteLogin = AbsRemoteLogin.create();
            remoteLogin.init(mClassLoader);
        }
    }
}
