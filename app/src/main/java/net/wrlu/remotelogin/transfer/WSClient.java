package net.wrlu.remotelogin.transfer;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import net.wrlu.remotelogin.callback.IntentTransferListener;

import org.json.JSONObject;

import java.util.UUID;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WSClient {
    private static final String TAG = "WRL-WSClient";
    private static final String SERVER_URL_CN = "wss://api.wrlus.com/ws/intent/";
    private static final String SERVER_URL_GLOBAL = "wss://api.wrlu.net/ws/intent/";
    private static String sServerUrl = SERVER_URL_CN;

    private static WSClient sInstance;
    private WebSocket mWebSocket;
    private IntentTransferListener mListener;


    private final String deviceId;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    public static class Api {
        public static final String INTENT_TRANSFER = "1";
        public static final String REGISTER_HOST = "2";
        public static final String WEIXIN_LOGIN = "3";
    }

    private WSClient() {
        setServerUrlCN();
        deviceId = UUID.randomUUID().toString();
    }

    public static synchronized WSClient getInstance() {
        if (sInstance == null) sInstance = new WSClient();
        return sInstance;
    }

    public void connect() {
        if (mWebSocket != null) return;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(sServerUrl + deviceId).build();

        mWebSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                parseAndDispatch(text);
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, okhttp3.Response response) {
                mWebSocket = null;
                mMainHandler.postDelayed(() -> connect(), 5000);
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                mWebSocket = null;
                mMainHandler.postDelayed(() -> connect(), 5000);
            }
        });
    }

    public void registerHost(String type) {
        if (mWebSocket == null) return;
        try {
            JSONObject json = new JSONObject();
            json.put("api", Api.REGISTER_HOST);
            json.put("type", type);
            mWebSocket.send(json.toString());
        } catch (Exception e) {
            Log.e(TAG, "API registerHost failed", e);
        }
    }

    public void sendIntent(String deviceId, Intent intent) {
        if (mWebSocket == null) return;
        try {
            String base64 = RemoteIntent.serialize(intent);
            JSONObject json = new JSONObject();
            json.put("api", Api.INTENT_TRANSFER);
            json.put("toDeviceId", deviceId);
            json.put("data", base64);
            mWebSocket.send(json.toString());
        } catch (Exception e) {
            Log.e(TAG, "API sendIntent failed", e);
        }
    }

    private void parseAndDispatch(String text) {
        try {
            JSONObject json = new JSONObject(text);
            String api = json.optString("api");
            String from = json.optString("from");

            mMainHandler.post(() -> {
                switch (api) {
                    case Api.INTENT_TRANSFER: {
                        String toDeviceId = json.optString("toDeviceId");
                        if (!deviceId.equals(toDeviceId)) {
                            Log.w(TAG, "INTENT_TRANSFER: target is not our device id.");
                            return;
                        };

                        String data = json.optString("data");
                        Intent intent = RemoteIntent.deserialize(data);

                        if (mListener != null) {
                            mListener.onReceiveIntent(from, intent);
                        }
                    }
                    default:
                        break;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Dispatch failed", e);
        }
    }

    public void setIntentTransferListener(IntentTransferListener listener) {
        this.mListener = listener;
    }

    public void setServerUrlCN() {
        sServerUrl = SERVER_URL_CN;
    }

    public void setServerUrlGlobal() {
        sServerUrl = SERVER_URL_GLOBAL;
    }

    public String getDeviceId() {
        return deviceId;
    }
}