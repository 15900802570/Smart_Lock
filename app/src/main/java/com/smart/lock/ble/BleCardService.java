
package com.smart.lock.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.smart.lock.ble.listener.BleMessageListenerImpl;
import com.smart.lock.ble.listener.ClientTransaction;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.provider.BleProvider;
import com.smart.lock.ble.provider.BleReceiver;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceLog;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.SystemUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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
    private BleChannel mBleChannel;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private static boolean sIsWriting = false;

    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID FIRMWARE_REVISON_UUID = UUID
            .fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID DIS_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID RX_SERVICE_UUID = UUID
            .fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

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
     * 要删除的LockId
     */
    public ArrayList<String> mDeleteLockIds;

    /**
     * 蓝牙消息分发器
     */
    private BleMessageListenerImpl mBleMessageListenerImpl;

    private boolean mActiveDisConnect = false;

    public static final int ATT_MTU_MAX = 517;
    public static final int ATT_MTU_MIN = 23;

    private long mStartTime = 0, mEndTime = 0;
    private long mStartTime2 = 0, mEndTime2 = 0;
    private Context mCtx;
    private static BleCardService mInstance;

    public BleCardService(Context context) {
        mCtx = context;
    }

    /**
     * 构造方法
     *
     * @param context
     * @return
     */
    public static BleCardService getInstance(Context context) {
        synchronized (BleCardService.class) {
            if (mInstance == null) {
                mInstance = new BleCardService(context);
            }
        }
        return mInstance;
    }

    // Implements callback methods for GATT events that the app cares about. For
    // example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mStartTime = System.currentTimeMillis();
                mConnectionState = STATE_CONNECTED;
                mEndTime2 = System.currentTimeMillis();
                LogUtil.d(TAG, "connect to success : " + (mEndTime2 - mStartTime2));
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
                mBleChannel.notifyData(BleMsg.ACTION_GATT_CONNECTED);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "refresh ble :" + refreshDeviceCache());
                mConnectionState = STATE_DISCONNECTED;
                mBleChannel.changeChannelState(mBleChannel.STATUS_CHANNEL_WAIT);
                Log.i(TAG, "Disconnected from GATT server.");
                mBleChannel.notifyData(BleMsg.ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "mBluetoothGatt = " + mBluetoothGatt);
                mEndTime = System.currentTimeMillis();
                LogUtil.d(TAG, "connect to disconnect : " + (mEndTime - mStartTime));
                mBleChannel.notifyData(BleMsg.ACTION_GATT_SERVICES_DISCOVERED);

                LogUtil.d(TAG, "mBluetoothGatt = " + gatt.hashCode() + "gatt service size is " + gatt.getServices().size());
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.w(TAG, "onCharacteristicRead() status = " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (TX_CHAR_UUID.equals(characteristic.getUuid())) {
                    Log.d(TAG, "onCharacteristicRead() " + characteristic.getValue());
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.w(TAG, "onCharacteristicChanged()");

            if (TX_CHAR_UUID.equals(characteristic.getUuid())) {
                LogUtil.d(TAG, "characteristic.getValue() length : " + characteristic.getValue().length);
                mBleProvider.onReceiveBle(characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite() status = " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBleChannel.changeChannelState(BleChannel.STATUS_CHANNEL_WAIT);
                mBleChannel.notifyData(BleMsg.ACTION_CHARACTERISTIC_WRITE);
            } else {
                Log.e(TAG, "onCharacteristicWrite() status = " + status);
            }
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
        // For API level 18 and above, get a reference to BluetoothAdapter
        // through
        // BluetoothManager.
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
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The
     * connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(String address) {
        address = StringUtil.checkBleMac(address);
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device. Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        Log.d(TAG, "deivce : " + device.getAddress() + " hashcode : " + device.hashCode());
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.
        mStartTime2 = System.currentTimeMillis();
        Log.d(TAG, "mBluetoothGatt is : " + ((mBluetoothGatt == null) ? true : mBluetoothGatt.hashCode()));
        mBluetoothGatt = device.connectGatt(mCtx, false, mGattCallback);

        if (null != mBluetoothGatt) {
            mBleChannel = new BleChannel(mCtx, mBluetoothGatt);

            mBleProvider = new BleProvider(true, mBluetoothGatt, mBleChannel);

            mBleMessageListenerImpl = new BleMessageListenerImpl(mCtx, this);

            mBleProvider.registerMessageCallBack(mBleMessageListenerImpl);

            mBleReceiver = new BleReceiver(mBleProvider);

            mBleReceiver.registerReceiver(mCtx, mKeyResult, mBleActions);

            mBleProvider.start();

            mDeleteLockIds = new ArrayList<>();

            Log.d(TAG, "Trying to create a new connection.");
        }

        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        LogUtil.d(TAG, "service disconnect!");
        mActiveDisConnect = true;
        mBluetoothGatt.disconnect();
        mBleChannel.Close();
    }

    //判断是主动断开
    public boolean isActiveDisConnect() {
        return mActiveDisConnect;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        Log.w(TAG, "mBluetoothGatt closed");
        mBluetoothDeviceAddress = null;
        Log.w(TAG, "mBluetoothGatt : " + (mBluetoothGatt == null));
        mBluetoothGatt.close();
        mBluetoothGatt = null;

        mBleReceiver.unregisterReceiver(mCtx);
        mBleProvider.unRegisterclear();
        mBleProvider.halt();
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enable Notification on TX characteristic
     *
     * @return
     */
    public void enableTXNotification() {
        BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
        if (RxService == null) {
            showMessage("Rx service not found!");
            mBleChannel.notifyData(BleMsg.ACTION_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(TX_CHAR_UUID);
        if (TxChar == null) {
            showMessage("Tx charateristic not found!");
            mBleChannel.notifyData(BleMsg.ACTION_DOES_NOT_SUPPORT_UART);
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(TxChar, true);

        BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);

    }

    private void showMessage(String msg) {
        Log.e(TAG, msg);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;

        return mBluetoothGatt.getServices();
    }

    /**
     * MSG 01
     */
    public boolean sendCmd01(byte cmdType, short userId) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_01);
        Bundle mBundle = msg.getData();
        mBundle.putByte(BleMsg.KEY_CMD_TYPE, cmdType);
        mBundle.putShort(BleMsg.KEY_USER_ID, userId);
        return mBleProvider.send(msg);
    }

    /**
     * MSG 03
     */
    public boolean sendCmd03(byte[] random) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_03);

        Bundle bundle = msg.getData();

        if (random != null && random.length != 0) {
            bundle.putByteArray(BleMsg.KEY_RANDOM, random);
        }

        return mBleProvider.send(msg);
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
    public ClientTransaction sendCmd11(final byte cmdType, final short userId, int timeOut) {
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

        ClientTransaction ct = new ClientTransaction(msg, new BleMessageListenerImpl(mCtx, this), mBleProvider);
        ct.request();
        return ct;

    }

    /**
     * MSG 17
     */
    public boolean sendCmd13(final byte cmdType) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_13);

        Bundle bundle = msg.getData();
        bundle.putByte(BleMsg.KEY_CMD_TYPE, cmdType);

        return mBleProvider.send(msg);
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
    public ClientTransaction sendCmd15(byte cmdType, byte keyType, short userId, byte lockId, String pwd, int timeOut) {
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

        ClientTransaction ct = new ClientTransaction(msg, new BleMessageListenerImpl(mCtx, this), mBleProvider);
        ct.request();
        return ct;
    }

    /**
     * MSG 17
     */
    public boolean sendCmd17(final byte cmdType, final short userId) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_17);

        Bundle bundle = msg.getData();
        bundle.putByte(BleMsg.KEY_CMD_TYPE, cmdType);
        bundle.putShort(BleMsg.KEY_USER_ID, userId);

        return mBleProvider.send(msg);
    }

    /**
     * MSG 1B
     */
    public boolean sendCmd1B(final byte cmdType, final short userId, byte[] unlockTime) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_1B);

        Bundle bundle = msg.getData();
        bundle.putByte(BleMsg.KEY_CMD_TYPE, cmdType);
        bundle.putShort(BleMsg.KEY_USER_ID, userId);

        if (unlockTime != null && unlockTime.length != 0) {
            bundle.putByteArray(BleMsg.KEY_UNLOCK_IMEI, unlockTime);
        }

        return mBleProvider.send(msg);
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
    public boolean sendCmd21(final byte[] nodeId) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_21);

        Bundle bundle = msg.getData();
        if (nodeId != null && nodeId.length != 0) {
            bundle.putByteArray(BleMsg.KEY_NODE_ID, nodeId);
        }

        return mBleProvider.send(msg);
    }

    /**
     * MSG25是apk发给智能锁查询某用户所有用户相关信息的消息，通过MSG26返回查询结果。管理员可以查询所有用户，普通用户可以查询自己，其他情况返回MSG2E错误。
     *
     * @param userId 用户编号
     * @return 是否发送成功
     */
    public ClientTransaction sendCmd25(final short userId, int timeOut) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_25);
        msg.setKey(Message.TYPE_BLE_SEND_CMD_25 + "#" + "single");
        msg.setTimeout(timeOut);

        Bundle bundle = msg.getData();
        bundle.putShort(BleMsg.KEY_USER_ID, userId);
        bundle.putSerializable(BleMsg.KEY_SERIALIZABLE, userId);

        ClientTransaction ct = new ClientTransaction(msg, new BleMessageListenerImpl(mCtx, this), mBleProvider);
        ct.request();
        return ct;
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
    public ClientTransaction sendCmd33(final byte cmdType, final short userId, int logId, DeviceLog delLog, int timeOut) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_33);
        msg.setKey(Message.TYPE_BLE_SEND_CMD_33 + "#" + "single");
        msg.setTimeout(timeOut);
        Bundle bundle = msg.getData();
        bundle.putByte(BleMsg.KEY_CMD_TYPE, cmdType);

        bundle.putShort(BleMsg.KEY_USER_ID, userId);

        bundle.putInt(BleMsg.KEY_LOG_ID, logId);

        bundle.putSerializable(BleMsg.KEY_SERIALIZABLE, delLog);

        ClientTransaction ct = new ClientTransaction(msg, new BleMessageListenerImpl(mCtx, this), mBleProvider);
        ct.request();
        return ct;
    }

}
