
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
import android.text.TextUtils;
import android.util.Log;

import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.provider.BleProvider;
import com.smart.lock.ble.provider.BleReceiver;
import com.smart.lock.db.bean.DeviceLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.List;
//import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
public class BleCardService extends Service {
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

    //蓝牙写入队列
    private static final Queue<Object> sWriteQueue =
            new ConcurrentLinkedQueue<Object>();
    private static boolean sIsWriting = false;

    public static final UUID TX_POWER_UUID = UUID
            .fromString("00001804-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_POWER_LEVEL_UUID = UUID
            .fromString("00002a07-0000-1000-8000-00805f9b34fb");
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

    public static final int ATT_MTU_MAX = 517;
    public static final int ATT_MTU_MIN = 23;

    // Implements callback methods for GATT events that the app cares about. For
    // example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;

                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG,
                        "Attempting to start service discovery:"
                                + mBluetoothGatt.discoverServices());
                mBleChannel.notifyData(BleMsg.ACTION_GATT_CONNECTED);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
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

                mBleChannel.notifyData(BleMsg.ACTION_GATT_SERVICES_DISCOVERED);


            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            Log.w(TAG, "onCharacteristicRead() status = " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (TX_CHAR_UUID.equals(characteristic.getUuid())) {
                    Log.d(TAG, "onCharacteristicRead() " + characteristic.getValue());
                } else {
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.w(TAG, "onCharacteristicChanged()");

            if (TX_CHAR_UUID.equals(characteristic.getUuid())) {

                mBleProvider.onReceiveBle(characteristic.getValue());

            } else {
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "onCharacteristicWrite()");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBleChannel.changeChannelState(mBleChannel.STATUS_CHANNEL_WAIT);
                mBleChannel.notifyData(BleMsg.ACTION_CHARACTERISTIC_WRITE);
            } else {
                Log.e(TAG, "onCharacteristicWrite() status = " + status);
            }
        }
    };


//    @SuppressLint("NewApi")
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        startForeground(1, new Notification());
//        // 绑定建立链接
//        bindService(new Intent(this, GuardService.class),
//                mServiceConnection, Context.BIND_IMPORTANT);
//
//
//        // Do an appropriate action based on the intent.
//        if (intent.getAction().equals(ACTION_STOP) == true) {
//            stop();
//            stopSelf();
//        } else if (intent.getAction().equals(ACTION_START) == true) {
//            start();
//            getLock(mCtx);
//        } else if (intent.getAction().equals(ACTION_KEEPALIVE) == true) {
//            keepAlive();
//        } else if (intent.getAction().equals(ACTION_RECONNECT) == true) {
//            if (isNetworkAvailable()) {
//                reconnectIfNecessary();
//            }
//        }
//        return START_STICKY;
//    }

    public class LocalBinder extends Binder {
        public BleCardService getService() {
            return BleCardService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that
        // BluetoothGatt.close() is called
        // such that resources are cleaned up properly. In this particular
        // example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

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
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
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
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device. Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        Log.d(TAG, "deivce !" + device.getAddress() + device.getBondState());
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);

        if (null != mBluetoothGatt) {
            mBleChannel = new BleChannel(this, mBluetoothGatt);

            mBleProvider = new BleProvider(true, mBluetoothGatt, mBleChannel);

            mBleMessageListenerImpl = new BleMessageListenerImpl(this, mBleProvider);

            mBleProvider.registerMessageCallBack(mBleMessageListenerImpl);

            mBleReceiver = new BleReceiver(mBleProvider);

            mBleReceiver.registerReceiver(this, mKeyResult, mBleActions);

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
        mBluetoothGatt.disconnect();
        mBleChannel.Close();

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
        mBluetoothGatt.close();
        mBluetoothGatt = null;

        mBleReceiver.unregisterReceiver(this);
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

    public void writeRXCharacteristic(byte[] value) {
        BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);

        // showMessage("mBluetoothGatt null"+ mBluetoothGatt);
        if (RxService == null) {
            showMessage("Rx service not found!");
            mBleChannel.notifyData(BleMsg.ACTION_DOES_NOT_SUPPORT_UART);
            return;
        }

        BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
        if (RxChar == null) {
            showMessage("Rx charateristic not found!");
            mBleChannel.notifyData(BleMsg.ACTION_DOES_NOT_SUPPORT_UART);
            return;
        }
        RxChar.setValue(value);
        boolean status = mBluetoothGatt.writeCharacteristic(RxChar);

        Log.d(TAG, "write TXchar - status=" + status);
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
     *
     */
    public void sendHexData(byte[] value) {
        mBleChannel.sendHexData(value);
    }

    /**
     * MSG 01
     */
    public boolean sendCmd01(int cmdType, int userId) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_01);
        Bundle mBundle = msg.getData();
        mBundle.putInt(BleMsg.KEY_CMD_TYPE, cmdType);
        mBundle.putInt(BleMsg.KEY_USER_ID, userId);
        return mBleProvider.send(msg);
    }

    /**
     * MSG 03
     */
    public boolean sendCmd03(byte[] random, byte[] ak) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_03);

        Bundle bundle = msg.getData();
        if (ak != null && ak.length != 0) {
            bundle.putByteArray(BleMsg.KEY_AK, ak);
        }

        if (random != null && random.length != 0) {
            bundle.putByteArray(BleMsg.KEY_RANDOM, random);
        }

        return mBleProvider.send(msg);
    }

    /**
     * MSG 11
     */
    public boolean sendCmd11(final int cmdType, final int userId) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_11);

        Bundle bundle = msg.getData();
        bundle.putInt(BleMsg.KEY_CMD_TYPE, cmdType);

        bundle.putInt(BleMsg.KEY_USER_ID, userId);

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
    public boolean sendCmd15(byte cmdType, byte keyType, short userId, byte lockId, int pwd) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_15);

        Bundle bundle = msg.getData();
        bundle.putByte(BleMsg.KEY_CMD_TYPE, cmdType);
        bundle.putByte(BleMsg.KEY_TYPE, keyType);
        bundle.putShort(BleMsg.KEY_USER_ID, userId);
        bundle.putByte(BleMsg.KEY_LOCK_ID, lockId);
        bundle.putInt(BleMsg.KEY_PWD, pwd);

        return mBleProvider.send(msg);
    }

    /**
     * APK智能锁。用来删除锁体密钥信息的消息
     *
     * @param lockId 锁体密钥编号，4字节
     * @param key    会话秘钥
     * @return 是否发送成功
     */
    public boolean sendCmd17(String lockId, final byte[] key) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_17);

        Bundle bundle = msg.getData();
        if (!TextUtils.isEmpty(lockId)) {
            mDeleteLockIds.add(lockId);
            bundle.putString(BleMsg.KEY_LOCK_ID, lockId);
        }

        if (key != null && key.length != 0) {
            bundle.putByteArray(BleMsg.KEY_AK, key);
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
        msg.setType(Message.TYPE_BLE_SEND_CMD_OTA);

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
     * @param key 会话秘钥
     * @return 是否发送成功
     */
    public boolean sendCmd19(final byte[] key) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_19);

        Bundle bundle = msg.getData();

        if (key != null && key.length != 0) {
            bundle.putByteArray(BleMsg.KEY_AK, key);
        }

        return mBleProvider.send(msg);
    }

    /**
     * APK智能锁。APK给智能锁下发的进入OTA模式命令
     *
     * @param nodeId 设备编号
     * @return 是否发送成功
     */
    public boolean sendCmd21(final byte[] nodeId, final byte[] key) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_21);

        Bundle bundle = msg.getData();
        if (nodeId != null && nodeId.length != 0) {
            bundle.putByteArray(BleMsg.KEY_NODE_ID, nodeId);
        }

        if (key != null && key.length != 0) {
            bundle.putByteArray(BleMsg.KEY_AK, key);
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
    public boolean sendCmd33(final byte cmdType, final short userId, int logId, DeviceLog delLog) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_33);
        msg.setKey(Message.TYPE_BLE_SEND_CMD_33 + "#" + "single");

        Bundle bundle = msg.getData();
        bundle.putByte(BleMsg.KEY_CMD_TYPE, cmdType);

        bundle.putShort(BleMsg.KEY_USER_ID, userId);

        bundle.putInt(BleMsg.KEY_LOG_ID, logId);

        bundle.putSerializable(BleMsg.KEY_SERIALIZABLE, delLog);

        ClientTransaction ct = new ClientTransaction(msg, 90, new BleMessageListenerImpl(this, mBleProvider), mBleProvider);

        return ct.request();
    }

    private synchronized void write(Object o) {
        if (sWriteQueue.isEmpty() && !sIsWriting) {
            doWrite(o);
        } else {
            sWriteQueue.add(o);
        }
    }

    private synchronized void nextWrite() {
        if (!sWriteQueue.isEmpty() && !sIsWriting) {
            doWrite(sWriteQueue.poll());
        }
    }

    private synchronized void doWrite(Object o) {
        if (o instanceof BluetoothGattCharacteristic) {
            sIsWriting = true;
            mBluetoothGatt.writeCharacteristic(
                    (BluetoothGattCharacteristic) o);
        } else if (o instanceof BluetoothGattDescriptor) {
            sIsWriting = true;
            mBluetoothGatt.writeDescriptor((BluetoothGattDescriptor) o);
        } else {
            nextWrite();
        }
    }

}
