package net.wrlu.xposed.framework;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public interface HookInterface {
    void onHookPackage(XC_LoadPackage.LoadPackageParam loadPackageParam);
    boolean isTarget(XC_LoadPackage.LoadPackageParam loadPackageParam);
}
