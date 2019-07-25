package com.smart.lock.ble;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.entity.BluetoothDev;
import com.smart.lock.entity.Device;
import com.smart.lock.ui.LockDetectingActivity;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.ToastUtil;

import org.apache.http.auth.AUTH;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class AutoConnectBle {

    private static final String TAG = AutoConnectBle.class.getSimpleName();
    private Context mCtx;
    /**
     * 蓝牙
     */
    private BleManagerHelper mBleManagerHelper;

    private boolean mAutoConnect = true;

    private ArrayList<DeviceInfo> newDeviceInfo;
    private static AutoConnectBle mAutoConnectBle;

    public static AutoConnectBle getInstance(Context context) {
        if (mAutoConnectBle != null) return mAutoConnectBle;
        else return new AutoConnectBle(context);
    }

    private AutoConnectBle(Context context) {
        mCtx = context;
        mBleManagerHelper = BleManagerHelper.getInstance(mCtx);
        newDeviceInfo = DeviceInfoDao.getInstance(mCtx).queryAll();
    }

    public DeviceInfo getAutoDev() {
        DeviceInfo devInfo = null;
        ArrayList<BluetoothDev> devs = mBleManagerHelper.getBleDevList();
        Collections.sort(devs, new Comparator<BluetoothDev>() {

            @Override
            public int compare(BluetoothDev o1, BluetoothDev o2) {

                return (o2.getRssi() - o1.getRssi());
            }
        });

        LogUtil.d(TAG, "devs : " + devs.toString());

        for (BluetoothDev dev : devs) {
            String mac = dev.getDevMac();
            devInfo = DeviceInfoDao.getInstance(mCtx).queryByField(DeviceInfoDao.DEVICE_MAC, mac);
            if (devInfo != null) {
                break;
            }

        }
        LogUtil.d(TAG, "devInfo : " + (devInfo == null ? true : devInfo.toString()));
        return devInfo;
    }

    public void autoConnect(DeviceInfo devInfo) {
        DeviceInfoDao.getInstance(mCtx).setNoDefaultDev();
//        mBleManagerHelper.getBleCardService().disconnect();
        devInfo.setDeviceDefault(true);
        Device.getInstance(mCtx).halt();
        DeviceInfoDao.getInstance(mCtx).updateDeviceInfo(devInfo);
    }

    public boolean isAutoConnect() {
        return mAutoConnect;
    }

    public void setAutoConnect(boolean mAutoConnect) {
        this.mAutoConnect = mAutoConnect;
    }
}
