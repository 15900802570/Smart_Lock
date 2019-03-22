
package com.smart.lock;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.dao.DeviceInfoDao;


public class BaseApplication extends Application {
    private final static String TAG = "BaseApplication";
    private static Context mContext;
    private BleManagerHelper mBleManagerHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        mBleManagerHelper = BleManagerHelper.getInstance(mContext, false);
    }


    public static Context getContext() {
        return mContext;
    }


}
