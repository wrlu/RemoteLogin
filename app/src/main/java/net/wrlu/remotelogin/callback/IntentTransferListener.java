package net.wrlu.remotelogin.callback;

import android.content.Intent;

public interface IntentTransferListener {
    void onReceiveIntent(String from, Intent intent);
}
