package net.wrlu.remotelogin.hook;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import net.wrlu.remotelogin.callback.ActivityStartInterceptor;
import net.wrlu.remotelogin.utils.ContextManager;
import net.wrlu.xposed.framework.HookInterface;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookActivityStarter implements HookInterface {
    private static final String TAG = "HookActivityStarter";

    public static final int START_SUCCESS = 0;
    private ActivityStartInterceptor mInterceptor = null;

    public void setInterceptor(ActivityStartInterceptor interceptor) {
        this.mInterceptor = interceptor;
    }

    @Override
    public void onHookPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        startHook(loadPackageParam.classLoader);
    }

    @Override
    public boolean isTarget(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        return "android".equals(loadPackageParam.packageName);
    }

    public void startHook(ClassLoader classLoader) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            XposedHelpers.findAndHookMethod("com.android.server.wm.ActivityStarter",
                    classLoader, "execute", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (mInterceptor != null) {
                                Object request = XposedHelpers.getObjectField(param.thisObject, "mRequest");
                                Intent intent = (Intent) XposedHelpers.getObjectField(request, "intent");
                                if (mInterceptor.onInterceptStartActivity(intent)) {
                                    param.setResult(START_SUCCESS);
                                }
                            }
                        }
                    });
        } else {
            XposedHelpers.findAndHookMethod("com.android.server.am.ActivityStarter",
                    classLoader, "execute", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (mInterceptor != null) {
                                Object request = XposedHelpers.getObjectField(param.thisObject, "mRequest");
                                Intent intent = (Intent) XposedHelpers.getObjectField(request, "intent");
                                if (mInterceptor.onInterceptStartActivity(intent)) {
                                    param.setResult(START_SUCCESS);
                                }
                            }
                        }
                    });
        }
    }

    /**
     * 在 system_server 中伪造身份启动 Activity
     *
     * @param callingPackage 发起方包名 (如 "com.tencent.mm")
     * @param intent         要启动的组件 Intent
     * @param userId         目标用户 ID (如 0 代表主空间)
     */
    public static void startActivity(String callingPackage, Intent intent, int userId) {
        Context systemContext = ContextManager.getSystemContext();

        Class<?> activityManagerClass = XposedHelpers.findClass("android.app.ActivityManager", null);
        Object iActivityManager = XposedHelpers.callStaticMethod(activityManagerClass, "getService");

        int INTENT_SENDER_ACTIVITY = 2; // ActivityManager.INTENT_SENDER_ACTIVITY
        IBinder token = null;
        String resultWho = null;
        int requestCode = 0;
        Intent[] intents = new Intent[]{intent};
        String[] resolvedTypes = new String[]{intent.resolveTypeIfNeeded(systemContext.getContentResolver())};

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        Object iIntentSender = XposedHelpers.callMethod(iActivityManager, "getIntentSender",
                INTENT_SENDER_ACTIVITY,
                callingPackage,
                token,
                resultWho,
                requestCode,
                intents,
                resolvedTypes,
                flags,
                null, // bOptions
                userId
        );

        if (iIntentSender == null) {
            return;
        }

        IntentSender intentSender = (IntentSender)
                XposedHelpers.newInstance(IntentSender.class, iIntentSender);

        try {
            intentSender.sendIntent(systemContext, 0, null, null, null);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Send Intent failed.", e);
        }
    }
}
