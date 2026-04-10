package net.wrlu.remotelogin.hook;

import android.content.ComponentName;
import android.content.Intent;

import net.wrlu.remotelogin.Config;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import de.robv.android.xposed.XposedBridge;

public class RemoteLoginServer extends AbsRemoteLogin {
    private static final String TAG = "RemoteLoginServer";
    private transient String mLastFromDeviceId;
    private final LinkedBlockingQueue<RequestTask> mQueue = new LinkedBlockingQueue<>();
    private final Semaphore mFlowLock = new Semaphore(0);

    @Override
    public void init(ClassLoader classLoader) {
        super.init(classLoader);
        mWSClient.registerHost(Config.Role.SUPER_HOST);
        startWorkerThread();
    }

    @Override
    public boolean isTargetIntent(Intent intent) {
        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            return !componentName.getPackageName().equals("com.tencent.mm") &&
                    componentName.getClassName().endsWith(".wxapi.WXEntryActivity");
        }
        return false;
    }

    private static class RequestTask {
        final String from;
        final Intent intent;
        RequestTask(String from, Intent intent) {
            this.from = from;
            this.intent = intent;
        }
    }

    @Override
    public void onReceiveIntent(String from, Intent intent) {
        mQueue.offer(new RequestTask(from, intent));
    }

    @Override
    public boolean onInterceptStartActivity(Intent intent) {
        boolean isIntercept = super.onInterceptStartActivity(intent);
        if (isIntercept) {
            mLastFromDeviceId = null;
            mFlowLock.release();
        }

        return isIntercept;
    }

    @Override
    public String getReceiverDeviceId() {
        return mLastFromDeviceId;
    }

    private void startWorkerThread() {
        Thread worker = new Thread(() -> {
            while (true) {
                try {
                    RequestTask task = mQueue.take();
                    mLastFromDeviceId = task.from;

                    HookActivityStarter.startActivity("android", task.intent, 0);

                    // 死等回调
                    mFlowLock.acquire();
                } catch (InterruptedException e) {
                    XposedBridge.log(e);
                    break;
                }
            }
        }, "RemoteLoginServer-Worker");
        worker.setDaemon(true);
        worker.start();
    }
}
