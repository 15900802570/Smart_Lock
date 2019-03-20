
package com.smart.lock;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.dao.DeviceInfoDao;


public class BaseApplication extends Application {
    private final static String TAG = "BaseApplication";
    public static Context mContext;
    private DeviceInfo mDefaultDevice; //默认设备

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        mDefaultDevice = DeviceInfoDao.getInstance(this).queryFirstData("device_default", true);
        if (mDefaultDevice != null) {
            BleManagerHelper.getInstance(this, mDefaultDevice.getBleMac(), false);
        }
    }


    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.d(TAG, "onTerminate");
    }

}
