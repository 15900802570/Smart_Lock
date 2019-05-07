
package com.smart.lock;

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
import com.smart.lock.utils.LogUtil;


public class BaseApplication extends Application {
    private final static String TAG = "BaseApplication";
    private Context mContext;
    private DeviceInfo mDefaultDevice; //默认设备
    protected BleManagerHelper mBleManagerHelper; //蓝牙服务

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();

        mBleManagerHelper = BleManagerHelper.getInstance(mContext, false);

        mDefaultDevice = DeviceInfoDao.getInstance(this).queryFirstData("device_default", true);
        if (mDefaultDevice == null) {
            return;
        }

    }

    public BleManagerHelper getmBleManagerHelper() {
        return mBleManagerHelper;
    }

}
