package net.wrlu.remotelogin.transfer;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

import net.wrlu.remotelogin.SettingsProvider;
import net.wrlu.remotelogin.utils.PackageNames;

public class Role {
    private static final String TAG = "Role";
    public static final String WEIXIN_HOST = "weixin_host";
    public static final String SUPER_HOST = "super_host";
    public static final String CLIENT = "client";
    static final String SP_NAME = "settings";
    static final String SP_KEY_NAME = "role";

    public static String getRole(Context context) {
        try {
            Cursor cursor = context.getContentResolver().query(SettingsProvider.CONTENT_URI,
                    null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                String role = cursor.getString(0);
                cursor.close();
                return role;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to getRole.", e);
        }
        return null;
    }

    public static String getRoleLocal(Context context) {
        if (PackageNames.SELF.equals(context.getPackageName())) {
            SharedPreferences sharedPreferences = context.createDeviceProtectedStorageContext()
                    .getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            return sharedPreferences.getString(SP_KEY_NAME, null);
        }
        return null;
    }


    public static void setRoleLocal(Context context, String role) {
        if (PackageNames.SELF.equals(context.getPackageName())) {
            SharedPreferences sharedPreferences = context.createDeviceProtectedStorageContext().
                    getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            sharedPreferences.edit().putString(SP_KEY_NAME, role).apply();
        }
    }
}
