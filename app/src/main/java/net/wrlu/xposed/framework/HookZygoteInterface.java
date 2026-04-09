package net.wrlu.xposed.framework;

import de.robv.android.xposed.IXposedHookZygoteInit;

public interface HookZygoteInterface {
    void onHookZygote(IXposedHookZygoteInit.StartupParam startupParam);
    boolean isTargetModule(String modulePath, boolean isSystemServer);
}
