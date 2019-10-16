package com.smart.lock.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.smart.lock.MainActivity;
import com.smart.lock.R;
import com.smart.lock.ble.listener.DeviceListener;
import com.smart.lock.ble.listener.MainEngine;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceStatus;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.entity.BluetoothDev;
import com.smart.lock.entity.Device;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;

public class BleManagerHelper {
    private static final String TAG = BleManagerHelper.class.getSimpleName();

    private static Context mContext;

    /**
     * 蓝牙服务
     */
    private BleCardService mService = null;

    /**
     * 蓝牙适配器
     */
    private BluetoothAdapter mBtAdapter = null;

    /**
     * 设备蓝牙地址
     */
    private String mBleMac;

    private String mNodeId; //设备IMEI
    private String mSn; //设备SN号

    /**
     * 蓝牙帮助类
     */
    private static BleManagerHelper instance;


    private Handler mHandler;

    /**
     * 服务连接标志
     */
    private boolean mIsConnected = false;


    /**
     * 连接方式 0-扫描二维码 1-普通安全连接,2-设置设备信息
     */
    private byte mConnectType = 0;

    /**
     * 用户编号，mConnectType =0 时，该值为0，否则非0
     */
    private short mUserId = 0;

    private long mStartTime = 0;
    private long mEndTime = 0;

    private static final long SCAN_PERIOD = 20000;
    private Device mDevice; //蓝牙连接状态实例

    private DeviceInfo mDefaultDevice; //默认设备
    private DeviceUser mDefaultUser; //默认用户
    private DeviceStatus mDefaultStatus; //用户状态
    //    private Dialog mLoadDialog;
    private Object mState = new Object();

    private DeviceListener mDeviceListener;

    private final ArrayList<UiListener> mUiListeners = new ArrayList(); //Ui监听集合

    public static final int REQUEST_OPEN_BT_CODE = 100;

    private ArrayList<BluetoothDev> mBleDevList;

    private Runnable mRunnable = new Runnable() {
        public void run() {
            synchronized (mState) {
                if (mDevice.getState() == Device.BLE_CONNECTION) {
                    mHandler.removeCallbacks(mRunnable);
                    mDevice.setState(Device.BLE_DISCONNECTED);
                    mBtAdapter.stopLeScan(mLeScanCallback);
                }
                LogUtil.d(TAG, "mState" + mState);
                synchronized (mUiListeners) {
                    ArrayList<UiListener> uiListeners = new ArrayList();
                    uiListeners.addAll(mUiListeners);
                    for (UiListener mUiListener : uiListeners) {
                        mUiListener.scanDevFailed();
                    }
                    uiListeners.clear();
                }
            }

            mBtAdapter.startDiscovery();
        }
    };

    public BleManagerHelper(Context context) {
        mContext = context;
        mHandler = new Handler();
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        mBleDevList = new ArrayList<>();

        mService = BleCardService.getInstance(mContext);
        mService.initialize();
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(mContext, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        }
    }

    public static BleManagerHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (BleManagerHelper.class) {
                if (instance == null) {
                    instance = new BleManagerHelper(context);
                }
            }
        }
        return instance;
    }

    /**
     * 连接蓝牙
     *
     * @return
     */
    public void connectBle(final byte type, Bundle bundle, Context context) {
        Device dev = getDevice(type, bundle, context);
        LogUtil.d(TAG, "dev : " + dev.toString());
        mConnectType = type;

        mBleMac = StringUtil.checkBleMac(mBleMac);

        if (StringUtil.checkIsNull(mBleMac)) {
            mStartTime = System.currentTimeMillis();
            return;
        }
        if (mConnectType == 2) {
            mContext = context;
            mNodeId = bundle.getString(BleMsg.KEY_NODE_ID);
            mSn = bundle.getString(BleMsg.KEY_NODE_SN);
        } else {
            mUserId = bundle.getShort(BleMsg.KEY_USER_ID);
            mDevice.setTempAuthCode(bundle.getByteArray(BleMsg.KEY_AUTH_CODE));
        }
        LogUtil.d(TAG, "mUserId = " + mUserId);

        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (context instanceof MainActivity) {
                AppCompatActivity activity = (AppCompatActivity) context;
                activity.startActivityForResult(enableIntent, REQUEST_OPEN_BT_CODE);
            } else {
                enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(enableIntent);
            }

        } else {
            if (mDevice.getState() == Device.BLE_DISCONNECTED) {
                mDevice.setState(Device.BLE_CONNECTION);
                closeDialog((int) (SCAN_PERIOD / 1000));
                mBleDevList.clear();
                mBtAdapter.startLeScan(mLeScanCallback);
            } else {
                LogUtil.d(TAG, " mService is null : " + (mService == null));
                if (mService != null) {
                    mService.disconnect();
                }
                getDevice(type, bundle, context);
                mDevice.setState(Device.BLE_CONNECTION);
                closeDialog((int) (SCAN_PERIOD / 1000));
                mBleDevList.clear();
                mBtAdapter.startLeScan(mLeScanCallback);
            }
        }
    }

    public Device getDevice(byte type, Bundle bundle, Context ctx) {
        if (bundle == null) {
            LogUtil.e(TAG, "register dev is null!");
            return null;
        }
        mDevice = Device.getInstance(ctx);
        DeviceInfo devInfo = null;
        mBleMac = bundle.getString(BleMsg.KEY_BLE_MAC);
        switch (type) {
            case Device.BLE_RETRIEVE_CONNECT:
            case Device.BLE_SCAN_QR_CONNECT_TYPE:
                devInfo = new DeviceInfo();
                devInfo.setUserId((short) 0);
                devInfo.setBleMac(mBleMac);
                mDevice.setDevInfo(devInfo);
                mDevice.setConnectType(type);
                break;
            case Device.BLE_OTHER_CONNECT_TYPE:
                devInfo = DeviceInfoDao.getInstance(ctx).queryFirstData("device_default", true);
                if (devInfo == null) {
                    devInfo = new DeviceInfo();
                }
                if (bundle.getShort(BleMsg.KEY_USER_ID) != 0 && StringUtil.checkNotNull(bundle.getString(BleMsg.KEY_NODE_ID))) {
                    devInfo.setUserId(bundle.getShort(BleMsg.KEY_USER_ID));
                    devInfo.setDeviceNodeId(bundle.getString(BleMsg.KEY_NODE_ID));
                    devInfo.setBleMac(mBleMac);
                }
                mDevice.setDevInfo(devInfo);
                mDevice.setConnectType(type);
                break;
            case Device.BLE_SCAN_AUTH_CODE_CONNECT:
                Device.getInstance(mContext).setDisconnectBle(true);
                devInfo = new DeviceInfo();
                if (bundle.getShort(BleMsg.KEY_USER_ID) != 0 && StringUtil.checkNotNull(bundle.getString(BleMsg.KEY_NODE_ID))) {
                    devInfo.setUserId(bundle.getShort(BleMsg.KEY_USER_ID));
                    devInfo.setDeviceNodeId(bundle.getString(BleMsg.KEY_NODE_ID));
                    devInfo.setBleMac(bundle.getString(BleMsg.KEY_BLE_MAC));
                }
                mDevice.setDevInfo(devInfo);
                mDevice.setConnectType(type);
                break;
            case Device.BLE_SEARCH_DEV_CONNECT:
            case Device.BLE_SET_DEVICE_INFO_CONNECT_TYPE:
                String mac = bundle.getString(BleMsg.KEY_BLE_MAC);
                devInfo = new DeviceInfo();
                if (StringUtil.checkNotNull(mac) && mac.length() == 12) {
                    devInfo.setBleMac(StringUtil.getMacAdr(mac));
                } else {
                    devInfo.setBleMac(mac);
                }
                devInfo.setDeviceNodeId(bundle.getString(BleMsg.KEY_NODE_ID));
                devInfo.setDeviceSn(bundle.getString(BleMsg.KEY_NODE_SN));
                mDevice.setDevInfo(devInfo);
                mDevice.setConnectType(type);
                break;
            default:
                devInfo = new DeviceInfo();
                devInfo.setUserId(bundle.getShort(BleMsg.KEY_USER_ID));
                devInfo.setBleMac(mBleMac);
                mDevice.setDevInfo(devInfo);
                mDevice.setConnectType(type);
                break;
        }
        return mDevice;
    }

    public void stopScan() {
        synchronized (mState) {
//            for (UiListener uiListener : mUiListeners) {
//                uiListener.scanDevFailed();
//            }
            synchronized (mUiListeners) {
                ArrayList<UiListener> uiListeners = new ArrayList();
                uiListeners.addAll(mUiListeners);
                for (UiListener mUiListener : uiListeners) {
                    mUiListener.scanDevFailed();
                }
                uiListeners.clear();
            }
            mHandler.removeCallbacks(mRunnable);
            if (mBtAdapter == null) {
                mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            mBtAdapter.stopLeScan(mLeScanCallback);
        }
    }

    public void addDeviceListener(DeviceListener listener) {
        mDeviceListener = listener;
    }

    public void deleteDefaultDev() {
        mDeviceListener.deleteDev();
        mHandler.removeCallbacks(mRunnable);
        mBtAdapter.stopLeScan(mLeScanCallback);
    }

    public ArrayList<BluetoothDev> getBleDevList() {
        return mBleDevList;
    }

    /**
     * 蓝牙搜索结果回调
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {


            if (StringUtil.checkIsNull(mBleMac)) {
                mHandler.removeCallbacks(mRunnable);
                mBtAdapter.stopLeScan(mLeScanCallback);
                return;
            }
            if (mConnectType == 2) {
                if (StringUtil.checkNotNull(device.getName()) && device.getName().equals(ConstantUtil.LOCK_DEFAULT_NAME)) {
                    LogUtil.d(TAG, "dev rssi = " + rssi);
                    mHandler.removeCallbacks(mRunnable);
                    mBtAdapter.stopLeScan(mLeScanCallback);
                    if (!mIsConnected && mService != null) {
                        mService.connect(mDevice, device.getAddress());
                    }
                }
            } else {
                LogUtil.d(TAG, "mBleMac :" + mBleMac + "\n" + "device " + device.getAddress());
                if (!mBleMac.equals(device.getAddress()) && StringUtil.checkNotNull(device.getName()) && device.getName().equals(ConstantUtil.LOCK_DEFAULT_NAME)) {
                    BluetoothDev dev = new BluetoothDev();
                    dev.setDevMac(device.getAddress());
                    dev.setDevName(device.getName());
                    dev.setRssi(rssi);
                    int index = -1;
                    for (BluetoothDev info : mBleDevList) {
                        if (info.getDevMac().equals(dev.getDevMac())) {
                            index = mBleDevList.indexOf(info);
                        }
                    }

                    if (index == -1) {
                        mBleDevList.add(dev);
                    }
                }
                if (device.getAddress().equals(mBleMac)) {
                    LogUtil.d(TAG, "scanRecord 2: " + StringUtil.bytesToHexString(scanRecord));
                    mHandler.removeCallbacks(mRunnable);
                    mBtAdapter.stopLeScan(mLeScanCallback);
                    if (!mIsConnected && mService != null) {
                        mService.connect(mDevice, device.getAddress());
                    }
                }
            }
        }
    };

    /**
     * 设置秘钥
     */
    public static void setSk(String sBleMac, String sDevSecret) {
        String mac = sBleMac.replace(mContext.getString(R.string.colon), "");

        byte[] macByte = StringUtil.hexStringToBytes(mac);

        if (MessageCreator.mIs128Code) {
            byte[] code = new byte[10];
            if (sDevSecret == null || sDevSecret.equals("0")) {
                System.arraycopy(StringUtil.hexStringToBytes("5A6B7C8D9E"), 0, MessageCreator.m128SK, 0, 5);
                System.arraycopy(macByte, 0, MessageCreator.m128SK, 5, 6); //写入MAC
                System.arraycopy(StringUtil.hexStringToBytes("A5B6C7D8E9"), 0, MessageCreator.m128SK, 11, 5);
            } else {
                System.arraycopy(macByte, 0, MessageCreator.m128SK, 0, 6); //写入MAC
                code = StringUtil.hexStringToBytes(sDevSecret);
                System.arraycopy(code, 0, MessageCreator.m128SK, 6, 10); //写入secretCode
            }
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
        }
    }

    /**
     * 设置秘钥 --modify 01   NPD
     */
//    public static void setSk(String sBleMac, String sDevSecret) {
//        String mac = sBleMac.replace(mContext.getString(R.string.colon), "");
//
//        byte[] macByte = StringUtil.hexStringToBytes(mac);
//
//        if (MessageCreator.mIs128Code) {
//            byte[] code = new byte[10];
//            if (sDevSecret == null || sDevSecret.equals("0")) {
//                Arrays.fill(MessageCreator.m128SK, 0, 8, (byte) 0x0A);
//                Arrays.fill(MessageCreator.m128SK, 8, 16, (byte) 0x05);
//            } else {
//                code = StringUtil.hexStringToBytes(sDevSecret);
//                System.arraycopy(code, 0, MessageCreator.m128SK, 0, 10); //写入secretCode
//                Arrays.fill(MessageCreator.m128SK, 10, 16, (byte) 0x00);
//            }
//        } else {
//            System.arraycopy(macByte, 0, MessageCreator.m256SK, 0, 6); //写入MAC
//            byte[] code = new byte[10];
//            if (sDevSecret == null || sDevSecret.equals("0")) {
//                Arrays.fill(MessageCreator.m256SK, 6, 16, (byte) 0);
//            } else {
//                code = StringUtil.hexStringToBytes(sDevSecret);
//                System.arraycopy(code, 0, MessageCreator.m256SK, 6, 10); //写入secretCode
//                Arrays.fill(MessageCreator.m256SK, 16, 32, (byte) 0);
//            }
//        }
//        LogUtil.d(TAG,"MessageCreator.m128SK : " + StringUtil.bytesToHexString(MessageCreator.m128SK,":"));
//    }

//    /**
//     * 设置秘钥 --modify 01
//     *
//     * */
//    public static void setSk(String sBleMac, String sDevSecret) {
//        String mac = sBleMac.replace(mContext.getString(R.string.colon), "");
//
//        byte[] macByte = StringUtil.hexStringToBytes(mac);
//
//        if (MessageCreator.mIs128Code) {
//            byte[] code = new byte[10];
//            if (sDevSecret == null || sDevSecret.equals("0")) {
//                Arrays.fill(MessageCreator.m128SK, 0, 10, (byte) 0x0A);
//            } else {
//                code = StringUtil.hexStringToBytes(sDevSecret);
//                System.arraycopy(code, 0, MessageCreator.m128SK, 0, 10); //写入secretCode
//            }
//            System.arraycopy(macByte, 0, MessageCreator.m128SK, 10, 6); //写入MAC
//        } else {
//            System.arraycopy(macByte, 0, MessageCreator.m256SK, 0, 6); //写入MAC
//            byte[] code = new byte[10];
//            if (sDevSecret == null || sDevSecret.equals("0")) {
//                Arrays.fill(MessageCreator.m256SK, 6, 16, (byte) 0);
//            } else {
//                code = StringUtil.hexStringToBytes(sDevSecret);
//                System.arraycopy(code, 0, MessageCreator.m256SK, 6, 10); //写入secretCode
//                Arrays.fill(MessageCreator.m256SK, 16, 32, (byte) 0);
//            }
//        }
//        LogUtil.d(TAG,"MessageCreator.m128SK : " + StringUtil.bytesToHexString(MessageCreator.m128SK,":"));
//    }
    public BleCardService getBleCardService() {
        return mService;
    }

    public synchronized void addUiListener(UiListener uiListener) {
        if (mUiListeners.contains(uiListener)) {
            LogUtil.d(TAG, "uiListener is contains!~");
            return;
        }
//        ListIterator<UiListener> iterable = mUiListeners.listIterator();
//        iterable.add(uiListener);
        mUiListeners.add(uiListener);
        if (mService == null) {
            LogUtil.e(TAG, "service is null");
            return;
        }

        MainEngine mainEngine = mService.getMainEngine();
        if (mainEngine == null) {
            LogUtil.e(TAG, "mainEngine is null");
            return;
        }
        mainEngine.addUiListener(uiListener);
    }

    //移除UI监听
    public synchronized void removeUiListener(UiListener uiListener) {
        synchronized (mUiListeners) {
            ListIterator<UiListener> iterable = mUiListeners.listIterator();
            while (iterable.hasNext()) {
                UiListener tempIterable = iterable.next();
                if (tempIterable == uiListener) {
                    iterable.remove();
                }
            }
        }
        if (mService == null) {
            LogUtil.e(TAG, "service is null");
            return;
        }

        MainEngine mainEngine = mService.getMainEngine();
        if (mainEngine == null) {
            LogUtil.e(TAG, "mainEngine is null");
            return;
        }

        mainEngine.removeUiListener(uiListener);
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
     * 获取会话秘钥
     *
     * @return 会话秘钥
     */
    public byte[] getAK() {
        return MessageCreator.mIs128Code ? MessageCreator.m128AK : MessageCreator.m256AK;
    }

    /**
     * 停止蓝牙服务
     */
    public void stopService() {
        if (mService != null) {
            mService.disconnect();
            mService.close();
//                mService.stopSelf();
            MainEngine mianEngine = mService.getMainEngine();
            if (mianEngine != null) {
                mianEngine.close();
            }
            mService = null;
            mBleMac = null;
            mConnectType = 0;
            mUserId = 0;
            if (mDevice != null) {
                mDevice.halt();
            }

            instance = null;
        }
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
