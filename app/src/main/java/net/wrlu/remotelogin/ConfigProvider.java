package net.wrlu.remotelogin;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import androidx.annotation.NonNull;

public class ConfigProvider extends ContentProvider {
    public static final String AUTHORITY = "net.wrlu.remotelogin.config";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection,
                        String selection, String[] selectionArgs, String sortOrder) {
        String role = null;
        Context context = getContext();
        if (context != null) {
            role = Config.get(context, Config.NAME_ROLE);
        }
        MatrixCursor cursor = new MatrixCursor(new String[]{ "value" });
        cursor.addRow(new Object[]{ role });
        return cursor;
    }

    @Override
    public String getType(@NonNull Uri uri) { return null; }
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) { return null; }
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override
    public int update(@NonNull Uri uri, ContentValues values,
                      String selection, String[] selectionArgs) { return 0; }
}
