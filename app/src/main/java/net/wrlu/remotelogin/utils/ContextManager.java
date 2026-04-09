package net.wrlu.remotelogin.utils;

import android.content.Context;

import de.robv.android.xposed.XposedHelpers;

public class ContextManager {

    public static Context getAppContext() {
        return (Context) XposedHelpers.callStaticMethod(
                XposedHelpers.findClass("android.app.ActivityThread", null),
                "currentApplication");
    }

    public static Context getSystemContext() {
        Object activityThread = XposedHelpers.callStaticMethod(
                XposedHelpers.findClass("android.app.ActivityThread", null),
                "currentActivityThread");
        return (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");
    }
}
