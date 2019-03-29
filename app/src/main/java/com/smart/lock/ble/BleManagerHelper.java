package com.smart.lock.ble;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.ui.BaseListViewActivity;
import com.smart.lock.ui.LockDetectingActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.ToastUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class BleManagerHelper {
    private static final String TAG = BleManagerHelper.class.getSimpleName();

    private static Context mContext;

    /**
     * 蓝牙服务
     */
    private BleCardService mService = null;

    /**
     * 蓝牙卡连接成功
     */
    public static final int UART_PROFILE_CONNECTED = 20;

    /**
     * 蓝牙卡未连接
     */
    public static final int UART_PROFILE_DISCONNECTED = 21;

    /**
     * 蓝牙状态
     */
    private int mState = UART_PROFILE_DISCONNECTED;

    /**
     * 蓝牙适配器
     */
    private BluetoothAdapter mBtAdapter = null;

    /**
     * 设备蓝牙地址
     */
    private String mBleMac;

    /**
     * 蓝牙帮助类
     */
    private static BleManagerHelper instance;


    private Handler mHandler;


    /**
     * 服务注册标志
     */
    private boolean mIsBind = false;

    /**
     * 服务连接标志
     */
    private boolean mIsConnected = false;

    /**
     * OTA升级模式
     */
    private int mOtaMode = 0;

    /**
     * 临时储存的OTA模式
     */
    private boolean mTempMode = false;

    /**
     * 等待提示框
     */
//    private Dialog mLoadDialog;

    /**
     * 连接方式 0-扫描二维码 1-普通安全连接
     */
    private byte mConnectType = 0;

    /**
     * 用户编号，mConnectType =0 时，该值为0，否则非0
     */
    private short mUserId = 0;

    private long mStartTime = 0;
    private long mEndTime = 0;

    private static final long SCAN_PERIOD = 10000;
    private boolean mScanning;

    private Runnable mRunnable = new Runnable() {
        public void run() {
//            DialogUtils.closeDialog(mLoadDialog);
            if (mScanning) {
                mScanning = false;
                mBtAdapter.stopLeScan(mLeScanCallback);
            }
            Toast.makeText(mContext, R.string.retry_connect, Toast.LENGTH_LONG).show();
            Intent intent = new Intent();
            intent.setAction(BleMsg.STR_RSP_SET_TIMEOUT);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        }
    };


    public BleManagerHelper(Context context, Boolean isOtaMode) {
        mContext = context;
        mTempMode = isOtaMode;
        mHandler = new Handler();
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        serviceInit();
        LocalBroadcastManager.getInstance(mContext).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(mContext, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        }
    }

    public static BleManagerHelper getInstance(Context context, Boolean isOtaMode) {
        if (instance == null) {
            synchronized (BleManagerHelper.class) {
                if (instance == null) {
                    instance = new BleManagerHelper(context, isOtaMode);
                }
            }
        } else {
            instance.setTempMode(isOtaMode);
        }
        return instance;
    }

    /**
     * 连接蓝牙
     *
     * @return
     */
    public void connectBle(final byte type, final short userId, String bleMac) {
        mConnectType = type;
        mUserId = userId;
        mBleMac = bleMac;
        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(enableIntent);
        } else {
            LogUtil.d(TAG, "mScanning = " + mScanning);
            if (!mScanning) {
                mScanning = true;
                closeDialog((int) (SCAN_PERIOD / 1000));
                mBtAdapter.startLeScan(mLeScanCallback);
            }
        }


    }

    /**
     * 蓝牙搜索结果回调
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            LogUtil.d(TAG, "ble mac = " + device.getAddress());
            LogUtil.d(TAG, "dev mac = " + mBleMac);

            if (device.getAddress().equals(mBleMac)) {
                LogUtil.d(TAG, "dev rssi = " + rssi);
                mBtAdapter.stopLeScan(mLeScanCallback);
                LogUtil.d(TAG, "mIsConnected = " + mIsConnected);
                if (!mIsConnected && mService != null) {
                    boolean result = mService.connect(mBleMac);
                    LogUtil.d(TAG, "result = " + result);

                }
            }
        }
    };

    /**
     * 设置秘钥
     */
    public static void setSk(String sBleMac, String sDevSecret) {
        String mac = sBleMac.replace(":", "");

        byte[] macByte = StringUtil.hexStringToBytes(mac);

        if (MessageCreator.mIs128Code) {
            System.arraycopy(macByte, 0, MessageCreator.m128SK, 0, 6); //写入MAC
            byte[] code = new byte[10];
            if (sDevSecret == null || sDevSecret.equals("0")) {
                Arrays.fill(MessageCreator.m128SK, 6, 16, (byte) 0);
            } else {
                code = StringUtil.hexStringToBytes(sDevSecret);
                System.arraycopy(code, 0, MessageCreator.m128SK, 6, 10); //写入secretCode
            }
            LogUtil.d(TAG, "m128SK = " + Arrays.toString(MessageCreator.m128SK));
        } else {
            System.arraycopy(macByte, 0, MessageCreator.m256SK, 0, 6); //写入MAC
            byte[] code = new byte[10];
            if (sDevSecret == null || sDevSecret.equals("0")) {
                Arrays.fill(MessageCreator.m256SK, 6, 16, (byte) 0);
            } else {
                code = StringUtil.hexStringToBytes(sDevSecret);
                System.arraycopy(code, 0, MessageCreator.m256SK, 6, 10); //写入secretCode
                Arrays.fill(MessageCreator.m256SK, 16, 32, (byte) 0);
            }
            LogUtil.d(TAG, "m256AK = " + Arrays.toString(MessageCreator.m256SK));
        }

    }

    public void setTempMode(Boolean isOtaMode) {
        mTempMode = isOtaMode;
    }

    public void setTempMode(Boolean isOtaMode, int otaMode) {
        mTempMode = isOtaMode;
        mOtaMode = otaMode;
    }


    /**
     * 初始化蓝牙服务
     */
    public void serviceInit() {
        Intent bindIntent = new Intent(mContext, BleCardService.class);
        LogUtil.d(TAG, "mIsBind = " + mIsBind);
        if (!mIsBind) {
            mStartTime = System.currentTimeMillis();
            mIsBind = mContext.bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * 打开搜索界面
     */
    public void startScanDevice() {

        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mContext.startActivity(enableIntent);
        } else {
            if (mOtaMode == 0) {
//                Intent newIntent = new Intent(mContext, DeviceScanActivity.class);
//                newIntent.putExtra(ConstantUtil.NODE_ID, mNodeId);
//                mContext.startActivity(newIntent);
            } else {
                Intent result = new Intent();
                result.setAction(BleMsg.STR_RSP_OTA_MODE);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(result);
            }
        }

    }


    /**
     * 蓝牙连接状态回调
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((BleCardService.LocalBinder) rawBinder).getService();
            Log.e(TAG, "mService is connection");
            mEndTime = System.currentTimeMillis();
            LogUtil.d("connecting ble time : " + (mEndTime - mStartTime));
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };

    /**
     * @return
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleMsg.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleMsg.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleMsg.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BleMsg.STR_RSP_SERVER_DATA);
        intentFilter.addAction(BleMsg.ACTION_DOES_NOT_SUPPORT_UART);
        intentFilter.addAction(BleMsg.EXTRA_DATA_MSG_02);
        intentFilter.addAction(BleMsg.EXTRA_DATA_MSG_04);
        intentFilter.addAction(BleMsg.EXTRA_DATA_MSG_12);
        intentFilter.addAction(BleMsg.STR_RSP_SCANED);
        return intentFilter;
    }

    /**
     * 广播接收
     */
    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BleMsg.ACTION_GATT_CONNECTED)) {
                Log.d(TAG, "UART_CONNECT_MSG");
                mState = UART_PROFILE_CONNECTED;
            }

            if (action.equals(BleMsg.ACTION_GATT_DISCONNECTED)) {
                Log.d(TAG, "UART_DISCONNECT_MSG");
                mState = UART_PROFILE_DISCONNECTED;
                MessageCreator.m128AK = null;
                MessageCreator.m256AK = null;
                mService.disconnect();
                mService.close();
                mScanning = false;
                mIsConnected = false;
                mOtaMode = mTempMode ? 1 : 0;
                mConnectType = 0;
                mUserId = 0;
                if (mOtaMode == 1) {
                    startScanDevice();
                }
//
            }

            if (action.equals(BleMsg.ACTION_GATT_SERVICES_DISCOVERED)) {
                if (mService != null) {
                    Log.d(TAG, "ACTION_GATT_SERVICES_DISCOVERED");
                    if (mOtaMode == 0) {
                        mService.enableTXNotification();
                        new Handler().postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                mService.sendCmd01(mConnectType, mUserId);
                            }
                        }, 1000);
                    } else {
                        Intent result = new Intent();
                        result.putExtra(BleMsg.KEY_AK, (byte[]) null);
                        result.setAction(BleMsg.STR_RSP_SECURE_CONNECTION_OTA);
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(result);
                    }
                }

                mHandler.removeCallbacks(mRunnable);
            }

            if (action.equals(BleMsg.ACTION_DOES_NOT_SUPPORT_UART)) {
                Log.d(TAG, "ACTION_DOES_NOT_SUPPORT_UART");
                showMessage("Device doesn't support UART. Disconnecting");
//                mService.disconnect();
//                mIsConnected = false;
            }
            // 4.2.3 MSG 03
            if (action.equals(BleMsg.EXTRA_DATA_MSG_02)) {
                final byte[] random = intent.getByteArrayExtra(BleMsg.KEY_RANDOM);

                if (random != null && random.length != 0) {
                    mService.sendCmd03(random);
                } else {
                    showMessage(mContext.getResources().getString(R.string.bt_connect_failed));
                }
            }

            // 4.2.3 MSG 04
            if (action.equals(BleMsg.EXTRA_DATA_MSG_04)) {
                mIsConnected = true;
                mHandler.removeCallbacks(mRunnable);
//                DialogUtils.closeDialog(mLoadDialog);
                Bundle extra = intent.getExtras();
                Intent result = new Intent();
                result.putExtras(extra);
                result.setAction(BleMsg.STR_RSP_SECURE_CONNECTION);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(result);
            }

            if (action.equals(BleMsg.STR_RSP_SCANED)) {


            }
        }
    };

    public BleCardService getBleCardService() {
        return mService;
    }

    /**
     * 获取蓝牙地址
     *
     * @return 蓝牙地址
     */
    public String getBleMac() {
        return mBleMac;
    }

    /**
     * 获取蓝牙连接状态
     *
     * @return
     */
    public boolean getServiceConnection() {
        return mIsConnected;
    }

    /**
     * 获取会话秘钥
     *
     * @return 会话秘钥
     */
    public byte[] getAK() {
        return MessageCreator.mIs128Code == true ? MessageCreator.m128AK : MessageCreator.m256AK;
    }

    /**
     * 停止蓝牙服务
     */
    public void stopService() {
        instance = null;
        try {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }

        if (mIsBind) {
            if (mService != null) {
                mService.disconnect();
                mService.close();
                mService.stopSelf();
                mService = null;
                mBleMac = null;
                mConnectType = 0;
                mUserId = 0;
            }

            mContext.unbindService(mServiceConnection);
            mIsBind = false;
        }


    }

    /**
     * 吐司提示
     *
     * @param msg 提示信息
     */
    private void showMessage(String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 超时提示框
     *
     * @param seconds 时间
     */
    private void closeDialog(int seconds) {
        mHandler.removeCallbacks(mRunnable);
        mHandler.postDelayed(mRunnable, seconds * 1000);
    }

}
