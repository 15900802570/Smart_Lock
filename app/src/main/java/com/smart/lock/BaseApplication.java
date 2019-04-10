
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
import com.smart.lock.ble.sdk.BleService;
import com.smart.lock.ble.sdk.IBle;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.utils.LogUtil;


public class BaseApplication extends Application {
    private final static String TAG = "BaseApplication";
    private static Context mContext;
    private DeviceInfo mDefaultDevice; //默认设备
    protected BleManagerHelper mBleManagerHelper; //蓝牙服务
    private boolean mIsConnected = false; //服务连接标志

    private BleService mService;
    private IBle mBle;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        mBleManagerHelper = BleManagerHelper.getInstance(mContext, false);

        mDefaultDevice = DeviceInfoDao.getInstance(this).queryFirstData("device_default", true);
        if (mDefaultDevice == null) {
            return;
        }
        mIsConnected = mBleManagerHelper.getServiceConnection();
        if (!mIsConnected) {
            LogUtil.d(TAG, "ble get Service connection() : " + mIsConnected);
            MessageCreator.setSk(mDefaultDevice);
            Bundle bundle = new Bundle();
            bundle.putShort(BleMsg.KEY_USER_ID, mDefaultDevice.getUserId());
            bundle.putString(BleMsg.KEY_BLE_MAC, mDefaultDevice.getBleMac());
            mBleManagerHelper.connectBle((byte) 1, bundle);
        }
//        Intent bindIntent = new Intent(this, BleService.class);
//        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder rawBinder) {
            mService = ((BleService.LocalBinder) rawBinder).getService();
            mBle = mService.getBle();
            if (mBle != null && !mBle.adapterEnabled()) {


            }
        }

        @Override
        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };

    public BleManagerHelper getmBleManagerHelper() {
        return mBleManagerHelper;
    }

    public IBle getIBle() {
        return mBle;
    }
}
