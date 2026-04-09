package net.wrlu.remotelogin.callback;

import android.content.Intent;

public interface ActivityStartInterceptor {

    /**
     * @param intent 启动的 Intent
     * @return true 表示拦截本次启动，false 表示放行
     */
    boolean onInterceptStartActivity(Intent intent);
}