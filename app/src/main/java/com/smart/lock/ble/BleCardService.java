
package com.smart.lock.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.smart.lock.ble.listener.ClientTransaction;
import com.smart.lock.ble.listener.DeviceStateCallback;
import com.smart.lock.ble.listener.MainEngine;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.provider.BleProvider;
import com.smart.lock.ble.provider.BleReceiver;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceLog;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.entity.Device;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.SystemUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
public class BleCardService {
    private final static String TAG = "BleCardService";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    /**
     * 蓝牙提供者
     */
    private BleProvider mBleProvider;

    /**
     * 蓝牙广播接收器
     */
    private BleReceiver mBleReceiver;

    /**
     * Ble指令回调广播
     */
    public static String[] mBleActions = new String[]{
            "android.intent.action.BleCommand.result"
    };

    /**
     * Ble指令回调广播
     */
    public static String mKeyResult = "result";

    /**
     * 蓝牙消息分发器
     */
    private MainEngine mEngine;
    private Handler mHandler;

    private Context mCtx;
    private static BleCardService mInstance;
    private DeviceStateCallback mDevStateCallback;

    private Runnable mRunnable = new Runnable() {
        public void run() {
            mDevStateCallback.onDisconnected();
        }
    };


    public BleCardService(Context context) {
        mCtx = context;
        mEngine = new MainEngine(mCtx, this);
        mHandler = new Handler();
    }

    private static ArrayList<UiListener> mUiListeners = new ArrayList<>();

    /**
     * 构造方法
     *
     * @param context
     * @return
     */
    public static BleCardService getInstance(Context context) {
        if (context instanceof UiListener) {
            mUiListeners.add((UiListener) context);
        }
        synchronized (BleCardService.class) {
            if (mInstance == null) {
                mInstance = new BleCardService(context);
            }
        }
        return mInstance;
    }

    public void registerDevStateCb(DeviceStateCallback devStateCallback) {
        mDevStateCallback = devStateCallback;
    }

    // Implements callback methods for GATT events that the app cares about. For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Attempts to discover services after successful connection.
                if (!gatt.discoverServices()) {
                    LogUtil.d("remote service discovery has been stopped status = " + newState);
                    disconnect();
                } else closeDialog(10);
                mDevStateCallback.onConnected(); //成功回调

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                Log.i(TAG, "refresh ble :" + refreshDeviceCache());
                Log.i(TAG, "Disconnected from GATT server.");
                mDevStateCallback.onDisconnected();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            LogUtil.d(TAG, "mBluetoothGatt Discovered : " + gatt.hashCode());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mHandler.removeCallbacks(mRunnable);
//                for (BluetoothGattService service: gatt.getServices()){
//                    LogUtil.d(TAG, "server uuid : " + service.getUuid().toString());
//                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()){
//                        LogUtil.d(TAG, "characteristic uuid : " + characteristic.getUuid().toString());
//                    }
//                }
                if (mBleProvider != null) {
                    mBleProvider.enableTXNotification();
                }
                mDevStateCallback.onServicesDiscovered(status);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
                disconnect();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.w(TAG, "onCharacteristicRead() status = " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (Device.TX_CHAR_UUID.equals(characteristic.getUuid())) {
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (Device.TX_CHAR_UUID.equals(characteristic.getUuid())) {
                mBleProvider.onReceiveBle(characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mDevStateCallback.onGattStateChanged(BluetoothGatt.GATT_SUCCESS);
            } else
                Log.e(TAG, "write failed, status = " + status);
        }
    };

    /**
     * Clears the internal cache and forces a refresh of the services from the
     * remote device.
     */
    public boolean refreshDeviceCache() {
        if (mBluetoothGatt != null) {
            try {
                BluetoothGatt localBluetoothGatt = mBluetoothGatt;
                Method localMethod = localBluetoothGatt.getClass().getMethod(
                        "refresh", new Class[0]);
                if (localMethod != null) {
                    boolean bool = ((Boolean) localMethod.invoke(
                            localBluetoothGatt, new Object[0])).booleanValue();
                    return bool;
                }
            } catch (Exception localException) {
                Log.i(TAG, "An exception occured while refreshing device");
            }
        }
        return false;
    }


    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mCtx.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
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

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @return Return true if the connection is initiated successfully. The
     * connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(Device device, String address) {

        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mCtx.startActivity(enableIntent);
            return false;
        }
        address = StringUtil.checkBleMac(address);
        // Previously connected device. Try to reconnect.
        if (address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.
        mBluetoothGatt = remoteDevice.connectGatt(mCtx, false, mGattCallback);
        LogUtil.d(TAG, "mBluetoothGatt : " + mBluetoothGatt.hashCode());
        if (null != mBluetoothGatt) {
            mEngine.registerDevice(device);
            mBleProvider = new BleProvider(true, mBluetoothGatt);
            mBleProvider.registerMessageCallBack(mEngine);

            mBleReceiver = new BleReceiver(mBleProvider);
            mBleReceiver.registerReceiver(mCtx, mKeyResult, mBleActions);
            mBleProvider.start();
            Log.d(TAG, "Trying to create a new connection.");
        }

        mBluetoothDeviceAddress = address;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        if (mBluetoothGatt != null) {
            LogUtil.d(TAG, "mBluetoothGatt disconnect: " + mBluetoothGatt.hashCode());
            mBluetoothGatt.disconnect();
        }

    }

    public MainEngine getMainEngine() {
        return mEngine;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public void close() {
        mBluetoothDeviceAddress = null;
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        if (mBleReceiver != null) {
            mBleReceiver.unregisterReceiver(mCtx);
            mBleProvider.unRegisterclear();
            mBleProvider.halt();
        }
    }

    /**
     * MSG 01
     */
    public boolean sendCmd01(byte cmdType, String authCode, short userId, int timeOut) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_01);
        msg.setKey(Message.TYPE_BLE_SEND_CMD_01 + "#" + "single");
        msg.setTimeout(timeOut);
        Bundle mBundle = msg.getData();
        mBundle.putByte(BleMsg.KEY_CMD_TYPE, cmdType);
        mBundle.putShort(BleMsg.KEY_USER_ID, userId);
        if (StringUtil.checkNotNull(authCode)) {
            mBundle.putString(BleMsg.KEY_AUTH_CODE, authCode);
        }

        ClientTransaction ct = new ClientTransaction(msg, mEngine, mBleProvider);
        return ct.request();
    }

    /**
     * MSG 03
     */
    public boolean sendCmd03(byte[] random, int timeOut) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_03);
        msg.setKey(Message.TYPE_BLE_SEND_CMD_03 + "#" + "single");
        msg.setTimeout(timeOut);
        Bundle bundle = msg.getData();

        if (random != null && random.length != 0) {
            bundle.putByteArray(BleMsg.KEY_RANDOM, random);
        }

        ClientTransaction ct = new ClientTransaction(msg, mEngine, mBleProvider);
        return ct.request();
    }

    /**
     * MSG 03
     */
    public boolean sendCmd05(String mac, String nodeId, String Sn) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_05);

        Bundle bundle = msg.getData();
        bundle.putString(BleMsg.KEY_BLE_MAC, mac);
        bundle.putString(BleMsg.KEY_NODE_ID, nodeId);
        bundle.putString(BleMsg.KEY_NODE_SN, Sn);

        return mBleProvider.send(msg);
    }

    /**
     * MSG 11
     */
    public boolean sendCmd11(final byte cmdType, final short userId, int timeOut) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_11);
        msg.setKey(Message.TYPE_BLE_SEND_CMD_11 + "#" + "single");
        msg.setTimeout(timeOut);
        Bundle bundle = msg.getData();
        bundle.putByte(BleMsg.KEY_CMD_TYPE, cmdType);
        bundle.putShort(BleMsg.KEY_USER_ID, userId);

        DeviceUser user = new DeviceUser();
        user.setUserPermission(cmdType);
        user.setUserId(userId);
        bundle.putSerializable(BleMsg.KEY_SERIALIZABLE, user);

        ClientTransaction ct = new ClientTransaction(msg, mEngine, mBleProvider);
        return ct.request();
    }

    /**
     * MSG 13
     */
    public boolean sendCmd13(final byte cmdType, int timeOut) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_13);
        msg.setKey(Message.TYPE_BLE_SEND_CMD_13 + "#" + "single");
        msg.setTimeout(timeOut);

        Bundle bundle = msg.getData();
        bundle.putByte(BleMsg.KEY_CMD_TYPE, cmdType);

        ClientTransaction ct = new ClientTransaction(msg, mEngine, mBleProvider);
        return ct.request();
    }

    /**
     * MSG 15是APK通知智能锁进行锁体密钥录入的消息
     *
     * @param cmdType 命令类型
     * @param keyType 秘钥类型
     * @param userId  用户编号
     * @param lockId  秘钥编号
     * @param pwd     录入密码
     * @return
     */
    public boolean sendCmd15(byte cmdType, byte keyType, short userId, byte lockId, String pwd, int timeOut) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_15);
        msg.setKey(Message.TYPE_BLE_SEND_CMD_15 + "#" + "single");
        msg.setTimeout(timeOut);

        Bundle bundle = msg.getData();
        bundle.putByte(BleMsg.KEY_CMD_TYPE, cmdType);
        bundle.putByte(BleMsg.KEY_TYPE, keyType);
        bundle.putShort(BleMsg.KEY_USER_ID, userId);
        bundle.putByte(BleMsg.KEY_LOCK_ID, lockId);
        bundle.putString(BleMsg.KEY_PWD, pwd);

        DeviceKey deviceKey = new DeviceKey();
        deviceKey.setKeyType(keyType);
        deviceKey.setUserId(userId);
        deviceKey.setLockId(String.valueOf(lockId));
        bundle.putSerializable(BleMsg.KEY_SERIALIZABLE, deviceKey);

        ClientTransaction ct = new ClientTransaction(msg, mEngine, mBleProvider);
        return ct.request();
    }

    /**
     * MSG 17
     */
    public boolean sendCmd17(final byte cmdType, final short userId, int timeOut) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_17);
        msg.setKey(Message.TYPE_BLE_SEND_CMD_17 + "#" + "single");
        msg.setTimeout(timeOut);

        Bundle bundle = msg.getData();
        bundle.putByte(BleMsg.KEY_CMD_TYPE, cmdType);
        bundle.putShort(BleMsg.KEY_USER_ID, userId);

        ClientTransaction ct = new ClientTransaction(msg, mEngine, mBleProvider);
        return ct.request();
    }

    /**
     * MSG 1B
     */
    public boolean sendCmd1B(final byte cmdType, final short userId, byte[] unlockTime, int timeOut) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_1B);
        msg.setKey(Message.TYPE_BLE_SEND_CMD_1B + "#" + "single");
        msg.setTimeout(timeOut);

        Bundle bundle = msg.getData();
        bundle.putByte(BleMsg.KEY_CMD_TYPE, cmdType);
        bundle.putShort(BleMsg.KEY_USER_ID, userId);

        if (unlockTime != null && unlockTime.length != 0) {
            bundle.putByteArray(BleMsg.KEY_UNLOCK_IMEI, unlockTime);
        }
        ClientTransaction ct = new ClientTransaction(msg, mEngine, mBleProvider);
        return ct.request();
    }

    /**
     * APK智能锁。APK给智能锁下发的进入OTA模式命令
     *
     * @param ota OTA命令秘钥
     * @return 是否发送成功
     */
    public boolean sendCmdOta(final byte[] ota, final byte[] key) {
        Message msg = Message.obtain();
//        msg.setType(Message.TYPE_BLE_SEND_CMD_OTA);

        Bundle bundle = msg.getData();
        if (ota != null && ota.length != 0) {
            bundle.putByteArray(BleMsg.KEY_NODE_ID, ota);
        }

        if (key != null && key.length != 0) {
            bundle.putByteArray(BleMsg.KEY_AK, key);
        }

        return mBleProvider.send(msg);
    }

    /**
     * APK智能锁。APK给智能锁下发的同步查询指令，智能锁通过MSG1A返回同步状态字
     *
     * @return 是否发送成功
     */
    public boolean sendCmd19(byte cmdType) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_19);

        Bundle bundle = msg.getData();

        if (cmdType != -1) {
            bundle.putByte(BleMsg.KEY_CMD_TYPE, cmdType);
        }
        return mBleProvider.send(msg);
    }

    /**
     * APK智能锁。APK给智能锁设置回锁时间
     *
     * @return 是否发送成功
     */
    public boolean sendCmd1D(byte cmdType) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_1D);

        Bundle bundle = msg.getData();

        if (cmdType != -1) {
            bundle.putByte(BleMsg.KEY_CMD_TYPE, cmdType);
        }
        return mBleProvider.send(msg);
    }

    /**
     * APK智能锁。APK给智能锁下发的进入OTA模式命令
     *
     * @param nodeId 设备编号
     * @return 是否发送成功
     */
    public boolean sendCmd21(final byte[] nodeId, int timeOut) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_21);
        msg.setKey(Message.TYPE_BLE_SEND_CMD_21 + "#" + "single");
        msg.setTimeout(timeOut);

        Bundle bundle = msg.getData();
        if (nodeId != null && nodeId.length != 0) {
            bundle.putByteArray(BleMsg.KEY_NODE_ID, nodeId);
        }

        ClientTransaction ct = new ClientTransaction(msg, mEngine, mBleProvider);
        return ct.request();
    }

    /**
     * MSG25是apk发给智能锁查询某用户所有用户相关信息的消息，通过MSG26返回查询结果。管理员可以查询所有用户，普通用户可以查询自己，其他情况返回MSG2E错误。
     *
     * @param userId 用户编号
     * @return 是否发送成功
     */
    public boolean sendCmd25(final short userId, int timeOut) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_25);
        msg.setKey(Message.TYPE_BLE_SEND_CMD_25 + "#" + "single");
        msg.setTimeout(timeOut);

        Bundle bundle = msg.getData();
        bundle.putShort(BleMsg.KEY_USER_ID, userId);
        bundle.putSerializable(BleMsg.KEY_SERIALIZABLE, userId);

        ClientTransaction ct = new ClientTransaction(msg, mEngine, mBleProvider);
        return ct.request();
    }

    /**
     * MSG 29
     */
    public boolean sendCmd29(final short userId, byte[] lifeCycle) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_29);

        Bundle bundle = msg.getData();
        bundle.putShort(BleMsg.KEY_USER_ID, userId);

        if (lifeCycle != null && lifeCycle.length != 0) {
            bundle.putByteArray(BleMsg.KEY_LIFE_CYCLE, lifeCycle);
        }

        return mBleProvider.send(msg);
    }

    /**
     * MSG 2D 设置省电时间段
     *
     * @param powerSaveTime Byte[] 省电时间段
     * @return bool
     */
    public boolean sendCmd2D(final byte[] powerSaveTime) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_2D);
        Bundle bundle = msg.getData();
        if (powerSaveTime != null && powerSaveTime.length == 8) {
            bundle.putByteArray(BleMsg.KEY_POWER_SAVE, powerSaveTime);
        } else {
            return false;
        }
        return mBleProvider.send(msg);
    }

    /**
     * APK智能锁。APK给智能锁更新版本
     *
     * @return 是否发送成功
     */
    public boolean sendCmdOtaData(final byte[] data, int type) {
        Message msg = Message.obtain();
        msg.setType(type);

        msg.setOta(true);

        Bundle bundle = msg.getData();

        if (data != null && data.length != 0) {
            bundle.putByteArray(BleMsg.EXTRA_DATA_BYTE, data);
        }

        return mBleProvider.send(msg);
    }

    /**
     * 是APK从智能锁查询开锁日志
     *
     * @param cmdType 命令类型
     * @param userId  用户编号
     * @return 是否发送成功
     */
    public boolean sendCmd31(final byte cmdType, final short userId) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_31);

        Bundle bundle = msg.getData();
        bundle.putByte(BleMsg.KEY_CMD_TYPE, cmdType);

        bundle.putShort(BleMsg.KEY_USER_ID, userId);

        return mBleProvider.send(msg);
    }

    /**
     * MSG 33是APK从智能锁查删除日志，通过MSG3E回复结果。
     *
     * @param cmdType 命令类型
     * @param userId  用户编号
     * @param logId   日志编号
     * @return 是否发送成功
     */
    public boolean sendCmd33(final byte cmdType, final short userId, int logId, DeviceLog delLog, int timeOut) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_33);
        msg.setKey(Message.TYPE_BLE_SEND_CMD_33 + "#" + "single");
        msg.setTimeout(timeOut);
        Bundle bundle = msg.getData();
        bundle.putByte(BleMsg.KEY_CMD_TYPE, cmdType);

        bundle.putShort(BleMsg.KEY_USER_ID, userId);

        bundle.putInt(BleMsg.KEY_LOG_ID, logId);

        bundle.putSerializable(BleMsg.KEY_SERIALIZABLE, delLog);

        ClientTransaction ct = new ClientTransaction(msg, mEngine, mBleProvider);
        return ct.request();
    }

    /**
     * MSG 37是APK发给智能锁的指纹固件大小。
     *
     * @param size 指纹固件大小
     * @return 是否发送成功
     */
    public boolean sendCmd37(int size, int timeOut) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_37);
        msg.setKey(Message.TYPE_BLE_SEND_CMD_37 + "#" + "single");
        msg.setTimeout(timeOut);
        Bundle bundle = msg.getData();

        bundle.putInt(BleMsg.KEY_FINGERPRINT_SIZE, size);

        ClientTransaction ct = new ClientTransaction(msg, mEngine, mBleProvider);
        return ct.request();
    }

    /**
     * MSG 51阈值测试。
     *
     * @return 是否发送成功
     */
    public boolean sendCmd61(int timeOut) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_61);
        msg.setKey(Message.TYPE_BLE_SEND_CMD_61 + "#" + "single");
        msg.setTimeout(timeOut);

        ClientTransaction ct = new ClientTransaction(msg, mEngine, mBleProvider);
        return ct.request();
    }

    public void cancelCmd(String key) {

        ClientTransaction ct = (ClientTransaction) mBleProvider.removeBleMsgListener(key);
        LogUtil.d(TAG,"cancel ct : " + ct.toString());
    }

}
