
package com.smart.lock;

import android.app.Application;
import android.content.Context;

import com.smart.lock.ble.BleManagerHelper;


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





}
