package net.wrlu.remotelogin;

import net.wrlu.remotelogin.hook.HookFinishBooting;
import net.wrlu.xposed.framework.HookInterface;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by wrlu on 2024/3/13.
 */
public class RemoteLoginRouter implements IXposedHookLoadPackage {
    private static final List<Class<? extends HookInterface>> packageHookers = new ArrayList<>();

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
}
