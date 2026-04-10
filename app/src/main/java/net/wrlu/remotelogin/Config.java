package net.wrlu.remotelogin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import net.wrlu.remotelogin.utils.PackageNames;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Config {
    private static final String TAG = "RemoteLoginConfig";
    public static class Role {
        public static final String CONFIG_NAME = "role";
        public static final String WEIXIN_HOST = "weixin_host";
        public static final String SUPER_HOST = "super_host";
        public static final String CLIENT = "client";
    }
    private static final Map<String, Boolean> lockedMap = new HashMap<>();
    private static final String SP_NAME = "config";
    private static final String PROPERTY_BASE = "persist.wrlu.rl.config";

    public static String get(Context context, String name) {
        String role = getFromProperty(name);
        if (role != null && !role.isEmpty()) {
            lockedMap.put(name, true);
            return role;
        }

        lockedMap.put(name, false);

        if (PackageNames.SELF.equals(context.getPackageName())) {
            return getFromPreference(context, name);
        } else {
            return getFromProvider(context, name);
        }
    }

    public static boolean isLockedByProperty(String name) {
        return Boolean.TRUE.equals(lockedMap.getOrDefault(name, false));
    }

    public static void setToPreference(Context context, String name, String value) {
        if (PackageNames.SELF.equals(context.getPackageName())) {
            SharedPreferences sharedPreferences = context.createDeviceProtectedStorageContext().
                    getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            sharedPreferences.edit().putString(name, value).apply();
        }
    }

    @SuppressLint("PrivateApi")
    private static String getFromProperty(String name) {
        try {
           Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method getMethod = systemPropertiesClass
                    .getDeclaredMethod("get", String.class, String.class);
            return (String) getMethod.invoke(null, PROPERTY_BASE + "." + name, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to getRoleFromProperty.", e);
        }
        return null;
    }

    private static String getFromProvider(Context context, String name) {
        Cursor cursor = context.getContentResolver().query(
                Uri.parse("content://" + ConfigProvider.AUTHORITY + "/" + name),
                null, null, null, null);
        String value = null;

        if (cursor != null && cursor.moveToFirst()) {
            value = cursor.getString(0);
            cursor.close();
        }
        return value;
    }

    private static String getFromPreference(Context context, String name) {
        SharedPreferences sharedPreferences = context.createDeviceProtectedStorageContext()
                .getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(name, null);
    }
}
