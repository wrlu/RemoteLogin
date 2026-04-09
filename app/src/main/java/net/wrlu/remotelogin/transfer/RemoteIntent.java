package net.wrlu.remotelogin.transfer;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.util.Set;

public class RemoteIntent implements Parcelable {
    private static final String TAG = "RemoteIntent";
    private final Intent intent;

    public RemoteIntent(Intent intent) {
        this.intent = intent;
    }

    public Intent getIntent() {
        return intent;
    }

    protected RemoteIntent(Parcel in) {
        this.intent = new Intent();
        readFromParcel(in);
    }

    public static String serialize(Intent intent) {
        if (intent == null) return null;

        RemoteIntent remoteIntent = new RemoteIntent(intent);

        Parcel data = Parcel.obtain();
        remoteIntent.writeToParcel(data, 0);

        data.setDataPosition(0);
        byte[] bytes = data.marshall();
        data.recycle();

        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    public static Intent deserialize(String data) {
        if (data == null || data.isEmpty()) return null;

        byte[] bytes = Base64.decode(data, Base64.NO_WRAP);
        Parcel parcel = Parcel.obtain();

        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);

        RemoteIntent remoteIntent = RemoteIntent.CREATOR.createFromParcel(parcel);
        return remoteIntent.getIntent();
    }

    public static final Creator<RemoteIntent> CREATOR = new Creator<RemoteIntent>() {
        @Override
        public RemoteIntent createFromParcel(Parcel in) {
            return new RemoteIntent(in);
        }

        @Override
        public RemoteIntent[] newArray(int size) {
            return new RemoteIntent[size];
        }
    };

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeString(intent.getAction());

        Uri data = intent.getData();
        if (data != null) {
            out.writeString(data.toString());
        } else {
            out.writeString(null);
        }

        out.writeString(intent.getType());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            out.writeString(intent.getIdentifier());
        } else {
            out.writeString(null);
        }

        out.writeInt(intent.getFlags());
        out.writeString(intent.getPackage());

        ComponentName.writeToParcel(intent.getComponent(), out);

        Rect sourceBounds = intent.getSourceBounds();
        if (sourceBounds != null) {
            out.writeInt(1);
            sourceBounds.writeToParcel(out, 0);
        } else {
            out.writeInt(0);
        }

        Set<String> categories = intent.getCategories();
        if (categories != null) {
            final int N = categories.size();
            out.writeInt(N);
            for (String c : categories) {
                out.writeString(c);
            }
        } else {
            out.writeInt(0);
        }

        Intent selector = intent.getSelector();
        if (selector != null) {
            out.writeInt(1);
            RemoteIntent remoteSelector = new RemoteIntent(selector);
            remoteSelector.writeToParcel(out, flags);
        } else {
            out.writeInt(0);
        }

//        ClipData clipData = intent.getClipData();
//        if (clipData != null) {
//            out.writeInt(1);
//            clipData.writeToParcel(out, 0);
//        } else {
        // TODO: Add support for ClipData object.
            out.writeInt(0);
//        }

        RemoteBundle remoteBundle = new RemoteBundle(intent.getExtras());
        remoteBundle.writeToParcel(out, flags);
    }

    private void readFromParcel(@NonNull Parcel in) {
        intent.setAction(in.readString());

        String uriString = in.readString();
        if (uriString != null) {
            intent.setData(Uri.parse(uriString));
        }

        intent.setType(in.readString());

        String identifier = in.readString();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            intent.setIdentifier(identifier);
        }

        intent.setFlags(in.readInt());
        intent.setPackage(in.readString());

        intent.setComponent(ComponentName.readFromParcel(in));

        if (in.readInt() != 0) {
            intent.setSourceBounds(Rect.CREATOR.createFromParcel(in));
        }

        int N = in.readInt();
        if (N > 0) {
            int i;
            for (i=0; i<N; i++) {
                intent.addCategory(in.readString());
            }
        }

        if (in.readInt() != 0) {
            RemoteIntent remoteSelector = RemoteIntent.CREATOR.createFromParcel(in);
            intent.setSelector(remoteSelector.getIntent());
        }

        if (in.readInt() != 0) {
            intent.setClipData(ClipData.CREATOR.createFromParcel(in));
        }

        RemoteBundle remoteBundle = RemoteBundle.CREATOR.createFromParcel(in);
        intent.replaceExtras(remoteBundle.getBundle());
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
