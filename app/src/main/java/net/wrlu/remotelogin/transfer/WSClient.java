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
    private final OkHttpClient mOkHttpClient;
    private WebSocket mWebSocket;
    private IntentTransferListener mListener;
    private final String mDeviceId;
    private final String mAuthToken;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean mIsConnected = false;
    private volatile boolean mIsConnecting = false;
    private int mReconnectAttempts = 0;
    private String mPendingHostType = null;

    public static class Api {
        public static final String INTENT_TRANSFER = "1";
        public static final String REGISTER_HOST = "2";
    }

    public WSClient(String token) {
        setServerUrlCN();

        mDeviceId = UUID.randomUUID().toString();
        mAuthToken = token;
        mOkHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request originalRequest = chain.request();

                    Request authRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer " + mAuthToken)
                            .build();

                    return chain.proceed(authRequest);
                })
                .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    public synchronized void connect() {
        if (mIsConnected || mIsConnecting) return;
        mIsConnecting = true;

        Request request = new Request.Builder().url(sServerUrl + mDeviceId).build();

        mWebSocket = mOkHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull okhttp3.Response response) {
                mIsConnected = true;
                mIsConnecting = false;
                mReconnectAttempts = 0;

                if (mPendingHostType != null) {
                    registerHost(mPendingHostType);
                }
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                parseAndDispatch(text);
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t,
                                  okhttp3.Response response) {
                Log.e(TAG, "WebSocket connection failed: " + t.getMessage());
                handleDisconnect();
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                Log.e(TAG,"WebSocket connection closed: " + reason);
                handleDisconnect();
            }
        });
    }

    private synchronized void handleDisconnect() {
        mIsConnected = false;
        mIsConnecting = false;
        mWebSocket = null;

        int delayMs = Math.min(2000 * (int) Math.pow(2, mReconnectAttempts), 30000);
        mReconnectAttempts++;

        Log.w(TAG,"Retry in " + delayMs + " ms, (attempt " + mReconnectAttempts + ")...");

        mMainHandler.postDelayed(this::connect, delayMs);
    }

    public void registerHost(String type) {
        // 记忆当前设备的身份，方便断线重连时自动恢复
        mPendingHostType = type;

        // 如果还没连上，什么都不用做，直接 return。
        // 因为等到 connect() -> onOpen() 被系统底层触发时，会自动读取 mPendingHostType 并执行发送。
        if (!mIsConnected || mWebSocket == null) {
            Log.w(TAG, "WebSocket disconnected. Pending registration for role [" + type + "].");
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("api", Api.REGISTER_HOST);
            json.put("type", type);
            mWebSocket.send(json.toString());
        } catch (Exception e) {
            Log.e(TAG, "registerHost", e);
        }
    }

    public void sendIntent(String deviceId, Intent intent) {
        if (!mIsConnected || mWebSocket == null) return;

        try {
            String base64 = RemoteIntent.serialize(intent);
            JSONObject json = new JSONObject();
            json.put("api", Api.INTENT_TRANSFER);
            json.put("toDeviceId", deviceId);
            json.put("data", base64);
            mWebSocket.send(json.toString());
        } catch (Exception e) {
            Log.e(TAG, "sendIntent", e);
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
                        if (!this.mDeviceId.equals(toDeviceId)) {
                            Log.w(TAG, "INTENT_TRANSFER: target is not our device id.");
                            return;
                        }

                        String data = json.optString("data");
                        Intent intent = RemoteIntent.deserialize(data);

                        if (mListener != null) {
                            mListener.onReceiveIntent(from, intent);
                        }
                        break;
                    }
                    default:
                        break;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "parseAndDispatch", e);
        }
    }

    public void setIntentTransferListener(IntentTransferListener listener) {
        this.mListener = listener;
    }

    public void setServerUrlCN() { sServerUrl = SERVER_URL_CN; }
    public void setServerUrlGlobal() { sServerUrl = SERVER_URL_GLOBAL; }
    public String getDeviceId() { return mDeviceId; }
}