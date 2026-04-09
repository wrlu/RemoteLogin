package net.wrlu.remotelogin.transfer;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

public class RemoteBundle implements Parcelable {
    private static final String TAG = "RemoteBundle";
    static final int BUNDLE_MAGIC = 0x4C444E42; // 'B' 'N' 'D' 'L'
    private static final int BUNDLE_MAGIC_NATIVE = 0x4C444E44; // 'B' 'N' 'D' 'N'

    private static final int VAL_NULL = -1;
    private static final int VAL_STRING = 0;
    private static final int VAL_INTEGER = 1;
    private static final int VAL_MAP = 2; // length-prefixed
    private static final int VAL_BUNDLE = 3;
    private static final int VAL_PARCELABLE = 4; // length-prefixed
    private static final int VAL_SHORT = 5;
    private static final int VAL_LONG = 6;
    private static final int VAL_FLOAT = 7;
    private static final int VAL_DOUBLE = 8;
    private static final int VAL_BOOLEAN = 9;
    private static final int VAL_CHARSEQUENCE = 10;
    private static final int VAL_LIST  = 11; // length-prefixed
    private static final int VAL_SPARSEARRAY = 12; // length-prefixed
    private static final int VAL_BYTEARRAY = 13;
    private static final int VAL_STRINGARRAY = 14;
    private static final int VAL_IBINDER = 15;
    private static final int VAL_PARCELABLEARRAY = 16; // length-prefixed
    private static final int VAL_OBJECTARRAY = 17; // length-prefixed
    private static final int VAL_INTARRAY = 18;
    private static final int VAL_LONGARRAY = 19;
    private static final int VAL_BYTE = 20;
    private static final int VAL_SERIALIZABLE = 21; // length-prefixed
    private static final int VAL_SPARSEBOOLEANARRAY = 22;
    private static final int VAL_BOOLEANARRAY = 23;
    private static final int VAL_CHARSEQUENCEARRAY = 24;
    private static final int VAL_PERSISTABLEBUNDLE = 25;
    private static final int VAL_SIZE = 26;
    private static final int VAL_SIZEF = 27;
    private static final int VAL_DOUBLEARRAY = 28;
    private static final int VAL_CHAR = 29;
    private static final int VAL_SHORTARRAY = 30;
    private static final int VAL_CHARARRAY = 31;
    private static final int VAL_FLOATARRAY = 32;

    private final Bundle bundle;

    public RemoteBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public Bundle getBundle() {
        return bundle;
    }

    protected RemoteBundle(Parcel in) {
        this.bundle = new Bundle();
        readFromParcel(in);
    }

    public static final Creator<RemoteBundle> CREATOR = new Creator<RemoteBundle>() {
        @Override
        public RemoteBundle createFromParcel(Parcel in) {
            return new RemoteBundle(in);
        }

        @Override
        public RemoteBundle[] newArray(int size) {
            return new RemoteBundle[size];
        }
    };

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        writeToParcelInner(dest, flags);
    }

    /**
     * Writes the Bundle contents to a Parcel, typically in order for
     * it to be passed through an IBinder connection.
     * @param parcel The parcel to copy this bundle to.
     */
    private void writeToParcelInner(Parcel parcel, int flags) {
        // Special case for empty bundles.
        if (bundle.isEmpty()) {
            parcel.writeInt(0);
            return;
        }

        int lengthPos = parcel.dataPosition();
        parcel.writeInt(-1); // placeholder, will hold length
        parcel.writeInt(BUNDLE_MAGIC);

        int startPos = parcel.dataPosition();

        writeBundleInternal(parcel, flags);

        int endPos = parcel.dataPosition();

        // Backpatch length
        parcel.setDataPosition(lengthPos);
        int length = endPos - startPos;

        parcel.writeInt(length);
        parcel.setDataPosition(endPos);
    }

    /**
     * Flatten a Bundle into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.  The Map keys must be String objects.
     */
    private void writeBundleInternal(Parcel parcel, int flags) {
        final int N = bundle.size();
        parcel.writeInt(N);

        for (String key : bundle.keySet()) {
            parcel.writeString(key);
            writeValue(parcel, bundle.get(key), flags);
        }
    }

    private void writeValue(Parcel parcel, Object v, int flags) {
        if (v == null) {
            parcel.writeInt(VAL_NULL);
        } else if (v instanceof String) {
            parcel.writeInt(VAL_STRING);
            parcel.writeString((String) v);
        } else if (v instanceof Integer) {
            parcel.writeInt(VAL_INTEGER);
            parcel.writeInt((Integer) v);
        } else if (v instanceof Map) {
            parcel.writeInt(VAL_MAP);
            Map<?, ?> map = (Map<?, ?>) v;
            if (map instanceof Serializable) {
                parcel.writeSerializable((Serializable) map);
            } else {
                parcel.writeSerializable(null);
                Log.w(TAG, "Map is not Serializable, skipping.");
            }
        } else if (v instanceof Bundle) {
            // Must be before Parcelable
            parcel.writeInt(VAL_BUNDLE);
            parcel.writeBundle((Bundle) v);
        } else if (v instanceof PersistableBundle) {
            parcel.writeInt(VAL_PERSISTABLEBUNDLE);
            RemoteBundle remoteBundle = new RemoteBundle(new Bundle((PersistableBundle) v));
            remoteBundle.writeToParcel(parcel, flags);
        } else if (v instanceof SizeF) {
            parcel.writeInt(VAL_SIZEF);
            parcel.writeSizeF((SizeF) v);
        } else if (v instanceof Parcelable) {
            // IMPOTANT: cases for classes that implement Parcelable must
            // come before the Parcelable case, so that their specific VAL_*
            // types will be written.
            parcel.writeInt(VAL_PARCELABLE);
            parcel.writeParcelable((Parcelable) v, flags);
        } else if (v instanceof Short) {
            parcel.writeInt(VAL_SHORT);
            parcel.writeInt(((Short) v).intValue());
        } else if (v instanceof Long) {
            parcel.writeInt(VAL_LONG);
            parcel.writeLong((Long) v);
        } else if (v instanceof Character) {
            parcel.writeInt(VAL_CHAR);
            parcel.writeInt((Character) v);
        } else if (v instanceof Float) {
            parcel.writeInt(VAL_FLOAT);
            parcel.writeFloat((Float) v);
        } else if (v instanceof Double) {
            parcel.writeInt(VAL_DOUBLE);
            parcel.writeDouble((Double) v);
        } else if (v instanceof Boolean) {
            parcel.writeInt(VAL_BOOLEAN);
            parcel.writeInt((Boolean) v ? 1 : 0);
        } else if (v instanceof CharSequence) {
            // Must be after String
            parcel.writeInt(VAL_CHARSEQUENCE);
            parcel.writeString(v.toString());
        } else if (v instanceof List) {
            parcel.writeInt(VAL_LIST);
            List<?> list = (List<?>) v;
            int N = list.size();
            parcel.writeInt(N);

            for (int i = 0; i < N; i++) {
                Object item = list.get(i);
                writeValue(parcel, item, flags);
            }
        } else if (v instanceof SparseArray) {
            parcel.writeInt(VAL_SPARSEARRAY);

            SparseArray<?> sa = (SparseArray<?>) v;
            int N = sa.size();
            parcel.writeInt(N);

            for (int i = 0; i < N; i++) {
                parcel.writeInt(sa.keyAt(i));
                writeValue(parcel, sa.valueAt(i), flags);
            }
        } else if (v instanceof boolean[]) {
            parcel.writeInt(VAL_BOOLEANARRAY);
            parcel.writeBooleanArray((boolean[]) v);
        } else if (v instanceof byte[]) {
            parcel.writeInt(VAL_BYTEARRAY);
            parcel.writeByteArray((byte[]) v);
        } else if (v instanceof String[]) {
            parcel.writeInt(VAL_STRINGARRAY);
            parcel.writeStringArray((String[]) v);
        } else if (v instanceof CharSequence[]) {
            parcel.writeInt(VAL_CHARSEQUENCEARRAY);
            CharSequence[] cs = (CharSequence[]) v;
            int N = cs.length;
            parcel.writeInt(N);
            for (CharSequence c : cs) {
                parcel.writeString(c != null ? c.toString() : null);
            }
        }
        else if (v instanceof IBinder) {
            parcel.writeInt(VAL_IBINDER);
            // IBinder object cannot share between different Android devices.
            parcel.writeStrongBinder(null);
        } else if (v instanceof Parcelable[]) {
            parcel.writeInt(VAL_PARCELABLEARRAY);
            parcel.writeParcelableArray((Parcelable[]) v, 0);
        } else if (v instanceof int[]) {
            parcel.writeInt(VAL_INTARRAY);
            parcel.writeIntArray((int[]) v);
        } else if (v instanceof long[]) {
            parcel.writeInt(VAL_LONGARRAY);
            parcel.writeLongArray((long[]) v);
        } else if (v instanceof Byte) {
            parcel.writeInt(VAL_BYTE);
            parcel.writeInt((Byte) v);
        } else if (v instanceof Size) {
            parcel.writeInt(VAL_SIZE);
            parcel.writeSize((Size) v);
        } else if (v instanceof double[]) {
            parcel.writeInt(VAL_DOUBLEARRAY);
            parcel.writeDoubleArray((double[]) v);
        } else if (v instanceof float[]) {
            parcel.writeInt(VAL_FLOATARRAY);
            parcel.writeFloatArray((float[]) v);
        } else if (v instanceof short[]) {
            parcel.writeInt(VAL_SHORTARRAY);
            short[] src = (short[]) v;
            int N = src.length;

            int[] destArray = new int[N];
            for (int i = 0; i < N; i++) {
                destArray[i] = ((Short) src[i]).intValue();
            }
            parcel.writeIntArray(destArray);
        } else if (v instanceof char[]) {
            parcel.writeInt(VAL_CHARARRAY);
            parcel.writeCharArray((char[]) v);
        } else if (v instanceof Serializable) {
            parcel.writeInt(VAL_SERIALIZABLE);
            parcel.writeSerializable((Serializable) v);
        } else {
            throw new RuntimeException("Parcel: unable to marshal value " + v);
        }
    }

    private void readFromParcel(@NonNull Parcel in) {
        readFromParcelInner(in);
    }

    /**
     * Reads the Parcel contents into this Bundle, typically in order for
     * it to be passed through an IBinder connection.
     * @param parcel The parcel to overwrite this bundle from.
     */
    private void readFromParcelInner(Parcel parcel) {
        int length = parcel.readInt();
        readFromParcelInner(parcel, length);
    }

    private void readFromParcelInner(Parcel parcel, int length) {
        if (length < 0) {
            throw new RuntimeException("Bad length in parcel: " + length);
        } else if (length == 0) {
            // Empty Bundle or end of data.
            return;
        } else if (length % 4 != 0) {
            throw new IllegalStateException("Bundle length is not aligned by 4: " + length);
        }

        final int magic = parcel.readInt();
        final boolean isJavaBundle = magic == BUNDLE_MAGIC;

        if (!isJavaBundle) {
            throw new IllegalStateException("Bad magic number for Bundle: 0x"
                    + Integer.toHexString(magic));
        }
        synchronized (this) {
            initializeFromParcelLocked(parcel);
        }
    }

    private void initializeFromParcelLocked(@NonNull Parcel parcel) {
        final int count = parcel.readInt();
        if (count < 0) {
            return;
        }

        int N = count;
        while (N > 0) {
            String key = parcel.readString();
            Object value = readValue(parcel, null);
            writeToBundle(bundle, key, value);
            N--;
        }
    }

    private Object readValue(@NonNull Parcel parcel, @Nullable ClassLoader loader) {
        int type = parcel.readInt();

        switch (type) {
            case VAL_NULL:
                return null;

            case VAL_STRING:
            case VAL_CHARSEQUENCE:
                return parcel.readString();

            case VAL_INTEGER:
                return parcel.readInt();

            case VAL_SIZEF:
                return parcel.readSizeF();

            case VAL_PARCELABLE:
                return parcel.readParcelable(loader);

            case VAL_SHORT:
                return (short) parcel.readInt();

            case VAL_LONG:
                return parcel.readLong();

            case VAL_CHAR:
                return (char) parcel.readInt();

            case VAL_FLOAT:
                return parcel.readFloat();

            case VAL_DOUBLE:
                return parcel.readDouble();

            case VAL_BOOLEAN:
                return parcel.readInt() != 0;

            case VAL_LIST: {
                int N = parcel.readInt();
                if (N < 0) {
                    return null;
                }
                List<Object> l = new ArrayList<>(N);

                while (N > 0) {
                    l.add(readValue(parcel, loader));
                    N--;
                }
                return l;
            }

            case VAL_BOOLEANARRAY:
                return parcel.createBooleanArray();

            case VAL_BYTEARRAY:
                return parcel.createByteArray();

            case VAL_STRINGARRAY:
                return parcel.createStringArray();

            case VAL_CHARSEQUENCEARRAY: {
                int N = parcel.readInt();
                if (N < 0) return null;

                CharSequence[] res = new CharSequence[N];
                for (int i = 0; i < N; i++) {
                    res[i] = parcel.readString();
                }
                return res;
            }

            case VAL_IBINDER:
                return parcel.readStrongBinder();

            case VAL_INTARRAY:
                return parcel.createIntArray();

            case VAL_LONGARRAY:
                return parcel.createLongArray();

            case VAL_BYTE:
                return (byte) (parcel.readInt() & 0xff);

            case VAL_MAP:
            case VAL_SERIALIZABLE:
                return parcel.readSerializable();

            case VAL_PARCELABLEARRAY:
                return parcel.readParcelableArray(loader);

            case VAL_SPARSEARRAY: {
                int N = parcel.readInt();
                if (N < 0) {
                    return null;
                }

                SparseArray<Object> sa = new SparseArray<>(N);
                while (N > 0) {
                    int key = parcel.readInt();
                    Object value = readValue(parcel, loader);
                    sa.append(key, value);
                    N--;
                }

                return sa;
            }

            case VAL_BUNDLE:
            case VAL_PERSISTABLEBUNDLE:
                RemoteBundle rb = RemoteBundle.CREATOR.createFromParcel(parcel);
                return rb.getBundle();

            case VAL_SIZE:
                return parcel.readSize();

            case VAL_DOUBLEARRAY:
                return parcel.createDoubleArray();

            case VAL_FLOATARRAY:
                return parcel.createFloatArray();

            case VAL_SHORTARRAY:
                int[] temp = parcel.createIntArray();
                if (temp == null) return null;

                int N = temp.length;
                short[] res = new short[N];

                for (int i = 0; i < N; i++) {
                    res[i] = (short) temp[i];
                }
                return res;

            case VAL_CHARARRAY:
                return parcel.createCharArray();

            default:
                int off = parcel.dataPosition() - 4;
                throw new RuntimeException(
                        "Parcel " + parcel + ": Unmarshalling unknown type code " + type + " at offset " + off);
        }
    }

    public static void writeToBundle(Bundle bundle, String key, Object v) {
        if (v == null) {
            bundle.putString(key, null);
        } else if (v instanceof String) {
            bundle.putString(key, (String) v);
        } else if (v instanceof Integer) {
            bundle.putInt(key, (Integer) v);
        } else if (v instanceof Map) {
            bundle.putSerializable(key, (Serializable) v);
        } else if (v instanceof Bundle) {
            bundle.putBundle(key, (Bundle) v);
        } else if (v instanceof PersistableBundle) {
            bundle.putBundle(key, new Bundle((PersistableBundle) v));
        } else if (v instanceof SizeF) {
            bundle.putSizeF(key, (SizeF) v);
        } else if (v instanceof Parcelable) {
            bundle.putParcelable(key, (Parcelable) v);
        } else if (v instanceof Short) {
            bundle.putShort(key, (Short) v);
        } else if (v instanceof Long) {
            bundle.putLong(key, (Long) v);
        } else if (v instanceof Character) {
            bundle.putChar(key, (Character) v);
        } else if (v instanceof Float) {
            bundle.putFloat(key, (Float) v);
        } else if (v instanceof Double) {
            bundle.putDouble(key, (Double) v);
        } else if (v instanceof Boolean) {
            bundle.putBoolean(key, (Boolean) v);
        } else if (v instanceof List) {
            bundle.putSerializable(key, (Serializable) v);
        } else if (v instanceof CharSequence) {
            bundle.putCharSequence(key, v.toString());
        } else if (v instanceof SparseArray) {
            try {
                bundle.putSparseParcelableArray(key, (SparseArray<? extends Parcelable>) v);
            } catch (Exception e) {
                Log.e(TAG, "SparseArray at " + key + " contains non-Parcelable elements!", e);
            }
        } else if (v instanceof boolean[]) {
            bundle.putBooleanArray(key, (boolean[]) v);
        } else if (v instanceof byte[]) {
            bundle.putByteArray(key, (byte[]) v);
        } else if (v instanceof String[]) {
            bundle.putStringArray(key, (String[]) v);
        } else if (v instanceof IBinder) {
            bundle.putBinder(key, null);
        } else if (v instanceof Parcelable[]) {
            bundle.putParcelableArray(key, (Parcelable[]) v);
        } else if (v instanceof int[]) {
            bundle.putIntArray(key, (int[]) v);
        } else if (v instanceof long[]) {
            bundle.putLongArray(key, (long[]) v);
        } else if (v instanceof Byte) {
            bundle.putByte(key, (Byte) v);
        } else if (v instanceof Size) {
            bundle.putSize(key, (Size) v);
        } else if (v instanceof double[]) {
            bundle.putDoubleArray(key, (double[]) v);
        } else if (v instanceof float[]) {
            bundle.putFloatArray(key, (float[]) v);
        } else if (v instanceof short[]) {
            bundle.putShortArray(key, (short[]) v);
        } else if (v instanceof char[]) {
            bundle.putCharArray(key, (char[]) v);
        } else if (v instanceof Serializable) {
            bundle.putSerializable(key, (Serializable) v);
        } else {
            throw new RuntimeException("Parcel: unable to unmarshal value " + v);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
