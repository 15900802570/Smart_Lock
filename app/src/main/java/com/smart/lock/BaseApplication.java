
package com.smart.lock;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.entity.Device;
import com.smart.lock.utils.LogUtil;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class BaseApplication extends Application {
    private final static String TAG = "BaseApplication";
    private Context mContext;
    protected BleManagerHelper mBleManagerHelper; //蓝牙服务
    public static int MLOOP_INTERVAL_SECS = 30;
    /**
     * 定时任务工具类
     */
    public static Timer mTimer;
    private Device mDevice;

    private boolean mActive;
    private Object mStateLock = new Object();

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();

        mBleManagerHelper = BleManagerHelper.getInstance(mContext);
        mDevice = Device.getInstance(this);
//        startLoop();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {

            }

            @Override
            public synchronized void onActivityStarted(Activity activity) {
                LogUtil.v(TAG, ">>>>>>>>>>>>>>>>>>>切到前台");
                synchronized (mStateLock) {
                    if (mTimer != null && mActive) {
                        mActive = false;
                        mTimer.cancel();
                        mTimer = null;
                    }
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public synchronized void onActivityStopped(Activity activity) {
                LogUtil.v(TAG, ">>>>>>>>>>>>>>>>>>>切到后台");
                synchronized (mStateLock) {
                    if (!mActive) {
                        startLoop();
                    }
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    public BleManagerHelper getmBleManagerHelper() {
        return mBleManagerHelper;
    }

    /**
     * 需要权限:android.permission.GET_TASKS
     *
     * @param context
     * @return
     */
    public boolean isApplicationBroughtToBackground(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (tasks != null && !tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            LogUtil.i(TAG, "topActivity:" + topActivity.flattenToString());
            if (!topActivity.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 启动轮询检测是否在前台
     */
    private void startLoop() {
        mActive = true;
        if (mTimer == null) {
            mTimer = new Timer();
        }
        LogUtil.d(TAG, "mTimer : " + mTimer.hashCode());
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {

                if (mBleManagerHelper.getBleCardService() != null && mDevice.getState() != Device.BLE_DISCONNECTED) {
                    mDevice.setBackGroundConnect(true);
                    mBleManagerHelper.getBleCardService().disconnect();
                    mDevice.setState(Device.BLE_DISCONNECTED);
                    LogUtil.d(TAG, "mDevice.getState() : " + mDevice.getState());
                }
            }
        }, MLOOP_INTERVAL_SECS * 1000);
    }


}
