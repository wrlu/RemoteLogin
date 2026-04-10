package net.wrlu.remotelogin;

import android.content.Context;

import net.wrlu.remotelogin.hook.HookFinishBooting;
import net.wrlu.remotelogin.hook.RemoteLoginClient;
import net.wrlu.remotelogin.hook.RemoteLoginServer;
import net.wrlu.remotelogin.hook.WXRemoteLoginClient;
import net.wrlu.remotelogin.hook.WXRemoteLoginServer;
import net.wrlu.xposed.framework.HookInterface;
import net.wrlu.xposed.framework.HookZygoteInterface;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by wrlu on 2024/3/13.
 */
public class RemoteLoginRouter implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static final List<Class<? extends HookInterface>> packageHookers = new ArrayList<>();
    private static final List<Class<? extends HookZygoteInterface>> zygoteHookers = new ArrayList<>();

    static {
        packageHookers.add(HookFinishBooting.class);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        String packageName = loadPackageParam.packageName;
        for (Class<? extends HookInterface> hooker : packageHookers) {
            try {
                String hookerName = hooker.getName();
                HookInterface hookInterface = hooker.newInstance();

                if (hookInterface.isTarget(loadPackageParam)) {
                    XposedBridge.log("Load package hooker " + hookerName);
                    hookInterface.onHookPackage(loadPackageParam);
                } else {
                    XposedBridge.log("Not target package " + packageName +
                            " for hooker " + hookerName);
                }
            } catch (ReflectiveOperationException e) {
                XposedBridge.log(e);
            }
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        for (Class<? extends HookZygoteInterface> hooker : zygoteHookers) {
            try {
                String hookerName = hooker.getName();
                HookZygoteInterface hookZygoteInterface = hooker.newInstance();

                if (hookZygoteInterface.isTargetModule(startupParam.modulePath,
                        startupParam.startsSystemServer)) {
                    XposedBridge.log("Load zygote hooker " + hookerName);
                    hookZygoteInterface.onHookZygote(startupParam);
                } else {
                    XposedBridge.log("Not target module " + startupParam.modulePath +
                            " for hooker " + hookerName);
                }
            } catch (ReflectiveOperationException e) {
                XposedBridge.log(e);
            }
        }
    }
}
