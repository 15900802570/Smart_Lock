
package com.smart.lock.ble;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.smart.lock.ble.provider.BleProvider;

import java.util.Arrays;
import java.util.UUID;

/**
 *
 */
public class BleChannel implements Runnable {
    private final static String TAG = "BleChannel";

    public final static int STATUS_CHANNEL_WAIT = 0;
    public final static int STATUS_CHANNEL_WRITE = 2;

    private static final int INT_TAG_CMD = 0x70;
    private static final int INT_TAG_RSP = 0x71;

    private static int mChannelStatus;
    private static MsgSession mMsgSession;
    private static Handler mChannelHandler;

    private Service mSrv;

    private BluetoothGatt mBleGatt;

    private String mPwd;

    private short mCrc;

    private BleProvider mBleProvider;

    /**
     * BleChannel().
     */
    public BleChannel(Service service, BluetoothGatt bleGatt) {
        mSrv = service;
        mBleGatt = bleGatt;
        mChannelStatus = STATUS_CHANNEL_WAIT;

        Thread t1 = new Thread(this);
        t1.start();

//        mBleProvider = new BleProvider(false);
    }

    @SuppressLint("HandlerLeak")
    @Override
    public void run() {
        Looper.prepare();

        mChannelHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int read_length = 0;

                if (msg.what == BleMsg.ID_EVENT_SEND_DATA) {
                    byte[] packet = new byte[20];

                    read_length = mMsgSession.readPacket(0x01, packet);

                    Log.d(TAG, "send packet = " + Arrays.toString(packet));

                    if (read_length > 0) {
                        if (!sendPacket(packet)) {
                            changeChannelState(STATUS_CHANNEL_WAIT);
                        }
                    }
                } else if (msg.what == BleMsg.ID_EVENT_RECV_DATA) {
                    byte[] buf = new byte[512];

                    read_length = mMsgSession.readData(0x02, buf, buf.length);

                    Log.i(TAG, "recevie buf = " + Arrays.toString(buf));
                    if (0 == read_length)
                        return;

                    if (read_length > 0) {
                        processDevRsp(buf);
                    }
                }
            }
        };

        mMsgSession = new MsgSession(mChannelHandler);

        Looper.loop();
    }

    /**
     * Close().
     */
    public void Close() {
        mSrv = null;
        mBleGatt = null;
        mChannelStatus = STATUS_CHANNEL_WAIT;
    }

    /**
     * notifyData(final byte[] data).
     */
    public int notifyData(final String action, final int data) {
        if (null == mSrv)
            return -1;

        if (null == action)
            return -1;

        final Intent intent = new Intent(action);
        intent.putExtra(BleMsg.EXTRA_DATA_INT, data);
        LocalBroadcastManager.getInstance(mSrv).sendBroadcast(intent);
        return 0;
    }

    /**
     * notifyData(final byte[] data).
     */
    public int notifyData(final String action, final byte[] data) {
        if (null == mSrv)
            return -1;

        if (null == data)
            return -1;

        if (null == action)
            return -1;
        final Intent intent = new Intent(action);
        intent.putExtra(BleMsg.EXTRA_DATA_BYTE, data);
        LocalBroadcastManager.getInstance(mSrv).sendBroadcast(intent);
        return 0;
    }

    /**
     * notifyData(final byte[] random,final byte[] key).
     */
    public int notifyData(final String action, final byte[] random, final byte[] key) {
        if (null == mSrv)
            return -1;

        if (null == random)
            return -1;

        if (null == key)
            return -1;

        if (null == action)
            return -1;

        final Intent intent = new Intent(action);
        intent.putExtra(BleMsg.KEY_RANDOM, random);
        intent.putExtra(BleMsg.KEY_AK, key);
        LocalBroadcastManager.getInstance(mSrv).sendBroadcast(intent);
        return 0;
    }

    /**
     * notifyData(final String action).
     */
    public int notifyData(final String action) {

        if (null == action)
            return -1;

        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(mSrv).sendBroadcast(intent);
        return 0;
    }

    /**
     * .
     */
    public void changeChannelState(int state) {
        mChannelStatus = state;
    }

    /**
     * byte2Int().
     */
    private static int byte2Int(byte[] bytes) {
        int b0 = bytes[0] & 0xFF;
        int b1 = bytes[1] & 0xFF;
        int b2 = bytes[2] & 0xFF;
        int b3 = bytes[3] & 0xFF;
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }

    /**
     * int2Bytes().
     */
    private static void int2Bytes(int i, byte[] target) {
        target[3] = (byte) (i & 0xFF);
        target[2] = (byte) (i >> 8 & 0xFF);
        target[1] = (byte) (i >> 16 & 0xFF);
        target[0] = (byte) (i >> 24 & 0xFF);
    }

    /**
     * long2Bytes
     */
    public static byte[] long2Bytes(long num, byte[] target) {
        for (int ix = 0; ix < 8; ++ix) {
            int offset = 64 - (ix + 1) * 8;
            target[ix] = (byte) ((num >> offset) & 0xff);
        }
        return target;
    }

    /**
     * short2Bytes().
     */
    private static void short2Bytes(short s, byte[] target) {
        target[0] = (byte) (s >> 8 & 0xFF);
        target[1] = (byte) (s & 0xFF);
    }

    /**
     * byte2short().
     */
    private static short byte2short(byte[] bytes) {
        byte high = bytes[0];
        byte low = bytes[1];

        return (short) ((high << 8 & 0xFF00) | (low & 0xFF));
    }

    /**
     * crc16(void)
     */
    private static short crc16(byte[] value, int len) {
        int index;
        short temp;
        short crc = (short) 0xFFFF;

        if (null == value || 0 == len)
            return (short) 0xFFFF;

        for (index = 0; index < len; index++) {
            crc = (short) (((crc & 0xFFFF) >>> 8) | (crc << 8));
            temp = (short) (value[index] & 0xFF);
            crc ^= temp;
            crc ^= (short) ((crc & 0xFF) >>> 4);
            crc ^= (short) ((crc << 8) << 4);
            crc ^= (short) (((crc & 0xFF) << 4) << 1);
        }

        return crc;
    }

    public boolean sendPacket(byte[] value) {
        final UUID RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
        final UUID RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");

        BluetoothGattService RxService = mBleGatt.getService(RX_SERVICE_UUID);

        while (mChannelStatus != STATUS_CHANNEL_WAIT) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }

        if (RxService == null) {
            notifyData(BleMsg.ACTION_DOES_NOT_SUPPORT_UART);
            return false;
        }

        BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
        if (RxChar == null) {
            notifyData(BleMsg.ACTION_DOES_NOT_SUPPORT_UART);
            return false;
        }

        changeChannelState(STATUS_CHANNEL_WRITE);

        Log.d(TAG, "value = " + Arrays.toString(value));

        RxChar.setValue(value);
        return mBleGatt.writeCharacteristic(RxChar);
    }

    public boolean sendOta(byte[] value, int type) {
        final UUID RX_SERVICE_UUID = UUID.fromString("0000ffa0-0000-1000-8000-00805f9b34fb");
        final UUID RX_CMD_UUID = UUID.fromString("0000ffa1-0000-1000-8000-00805f9b34fb");
        final UUID RX_DATA_UUID = UUID.fromString("0000ffa2-0000-1000-8000-00805f9b34fb");

        try {
            Thread.sleep(20);
        } catch (InterruptedException ex) {
        }

        BluetoothGattService RxService = mBleGatt.getService(RX_SERVICE_UUID);

        Log.d(TAG, "RxService = " + (RxService == null));
        if (RxService == null) {
            notifyData(BleMsg.ACTION_DOES_NOT_SUPPORT_UART);
            return false;
        }
        BluetoothGattCharacteristic RxChar = null;
        if (type == com.smart.lock.ble.message.Message.TYPE_BLE_SEND_OTA_CMD) {
            RxChar = RxService.getCharacteristic(RX_CMD_UUID);
        } else if (type == com.smart.lock.ble.message.Message.TYPE_BLE_SEND_OTA_DATA) {
            RxChar = RxService.getCharacteristic(RX_DATA_UUID);
        }

        if (RxChar == null) {
            notifyData(BleMsg.ACTION_DOES_NOT_SUPPORT_UART);
            return false;
        }

        changeChannelState(STATUS_CHANNEL_WRITE);

        Log.d(TAG, "value = " + Arrays.toString(value));
        RxChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        RxChar.setValue(value);
        return mBleGatt.writeCharacteristic(RxChar);
    }

    /**
     * @brief Function for changing hex data to pdu.
     */
    private boolean hex2pdu(byte[] hex, byte[] pdu) {
        byte i = 0;
        byte input_char = 0;
        byte output_char = 0;
        int len_out = 0;
        int len_in = 0;
        int len_reduce = hex.length;

        if (0 != len_reduce % 2) {
            /* The length of input must be even */
            Log.e(TAG, "hex2pdu() hex.length error.");

            return false;
        }

        if (pdu.length * 2 < len_reduce) {
            /* The length of input must be even */
            Log.e(TAG, "hex2pdu() hex.length error.");

            return false;
        }

        while (0 != len_reduce) {
            output_char = 0;

            /* Each two ASCII characters are changed into one Uint8 value */
            for (i = 0; i < 2; i++) {
                output_char = (byte) (output_char << 4);

                input_char = hex[len_in];

                if ((input_char >= 'A') && (input_char <= 'F')) {
                    output_char = (byte) (output_char + input_char - 'A' + 10);
                } else if ((input_char >= 'a') && (input_char <= 'f')) {
                    output_char = (byte) (output_char + input_char - 'a' + 10);
                } else if ((input_char >= '0') && (input_char <= '9')) {
                    output_char = (byte) (output_char + input_char - '0');
                } else {
                    Log.e(TAG, "hex2pdu() the input string is not a hex.");

                    return false;
                }

                len_in = len_in + 1;
            }

            pdu[len_out] = output_char;
            len_out = len_out + 1;
            len_reduce = len_reduce - 2;
        }

        return true;
    }

    /**
     * @brief Function for changing pdu data to hex.
     */
    private boolean pdu2hex(byte[] pdu, byte[] hex) {
        int in = 0;
        int out = 0;

        int len_pdu = pdu.length;
        int len_hex = hex.length;
        byte[] hex_val = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };

        if (len_hex < len_pdu * 2) {
            Log.e(TAG, "pdu2hex() hex.length error.");
            return false;
        }

        while (0 != len_pdu) {
            hex[out] = hex_val[pdu[in] >>> 4 & 0x0F];
            out++;
            hex[out] = hex_val[pdu[in] & 0x0F];
            out++;
            in++;
            len_pdu = len_pdu - 1;
        }

        return true;
    }

    public boolean sendHexData(byte[] data) {
        int length = data.length;

        byte[] pdu = new byte[length / 2];

        if (STATUS_CHANNEL_WAIT != mChannelStatus) {
            Log.e(TAG, "sendHexData() mChannelStatus is false.");
            return false;
        }

        if (false == mMsgSession.initSession(MsgSession.INT_SESSION_TYPE_CMD, length / 2)) {
            Log.e(TAG, "sendHexData() mMsgSession.initSession().");
            return false;
        }

        if (false == hex2pdu(data, pdu)) {
            Log.e(TAG, "sendHexData() hex2pdu() fail.");
            return false;
        }

        return mMsgSession.writeData(0x01, pdu, length / 2);
    }

    public boolean sendPduData(byte[] data) {
        int length = data.length;

        if (STATUS_CHANNEL_WAIT != mChannelStatus) {
            Log.e(TAG, "sendPduData() mChannelStatus is false.");
            return false;
        }

        if (false == mMsgSession.initSession(MsgSession.INT_SESSION_TYPE_CMD, length)) {
            Log.e(TAG, "sendPduData() mMsgSession.initSession().");
            return false;
        }

        return mMsgSession.writeData(0x01, data, length);
    }

    public boolean sendCommand(int cmd, int len, byte[] data) {
        byte[] buf = new byte[4];

        if (STATUS_CHANNEL_WAIT != mChannelStatus) {
            Log.e(TAG, "sendCommand() mChannelStatus is false");
            return false;
        }

        if (0 != (len % 16) || len > 511 || len < 16) {
            Log.e(TAG, "sendCommand() len = " + len + " is wrong length.");
            return false;
        }

        if (false == mMsgSession.initSession(MsgSession.INT_SESSION_TYPE_CMD, len + 4)) {
            Log.e(TAG, "sendCommand() mMsgSession.initSession().");
            return false;
        }

        buf[0] = (byte) cmd;
        buf[1] = (byte) (len / 16);
        mMsgSession.writeData(0x01, buf, 2);

        if (false == mMsgSession.writeData(0x01, data, len)) {
            Log.e(TAG, "sendCommand() mMsgSession.writeData.");
            return false;
        }

        buf[0] = (byte) 0x33;
        buf[1] = (byte) 0x44;
        return mMsgSession.writeData(0x01, buf, 2);
    }

    /**
     * 激活设备握手指令MSG 01
     *
     * @param context
     * @return 发送结果
     */
    public boolean sendCmd01(Context context) {
        Log.d(TAG, "sendCmd01");
        int apk = 0x01;
        int version = 0x01;
        short cmdLen = 24;
        byte[] buf = new byte[16];
        byte key[] = {
                '1', '2', '3', '4', '*', '*', '1', '2', '3', '4', 0x06, 0x06, 0x06, 0x06, 0x06,
                0x06,
                0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F,
                0x0F, 0x0F
        };

        byte random[] = {
                '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6'
        };

        byte[] cmd = new byte[128];

        waitChannelStatus();

        if (false == mMsgSession.initSession(MsgSession.INT_SESSION_TYPE_CMD, cmdLen + 5)) {
            Log.e(TAG, "sendCmd01() mMsgSession.initSession().");
            return false;
        }

        cmd[0] = 0x01;

        short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        int2Bytes(apk, buf);
        System.arraycopy(buf, 0, cmd, 3, 4);

        int2Bytes(version, buf);
        System.arraycopy(buf, 0, cmd, 7, 4);

        try {
            AES_ECB_PKCS7.AES256Encode(random, buf, key);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.arraycopy(buf, 0, cmd, 11, 16);

        short crc = crc16(cmd, 27);
        short2Bytes(crc, buf);
        System.arraycopy(buf, 0, cmd, 27, 2);

        if (false == mMsgSession.writeData(0x01, cmd, 29)) {
            Log.e(TAG, "sendCommand() mMsgSession.writeData.");
            return false;
        }

        return true;
    }

    /**
     * 等待BLE处于可发送状态
     */
    private void waitChannelStatus() {
        while (mChannelStatus != STATUS_CHANNEL_WAIT) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
    }

    /**
     * 智能锁->APK握手指令03
     *
     * @param context 上下文
     * @param random  随机数
     * @param key     会话秘钥
     * @return 发送结果
     */
    public boolean sendCmd03(Context context, final byte[] random, final byte[] key) {
        Log.d(TAG, "sendCmd03");
        short cmdLen = 20;
        byte[] cmd = new byte[128];

        waitChannelStatus();

        if (false == mMsgSession.initSession(MsgSession.INT_SESSION_TYPE_CMD, cmdLen + 5)) {
            Log.e(TAG, "sendCmd03() mMsgSession.initSession().");
            return false;
        }

        cmd[0] = 0x3;
        byte[] buf = new byte[16];
        short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        try {
            AES_ECB_PKCS7.AES256Encode(random, buf, key);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.arraycopy(buf, 0, cmd, 3, 16);
        long time = System.currentTimeMillis() / 1000;
        int2Bytes((int) time, buf);
        System.arraycopy(buf, 0, cmd, 19, 4);
        short crc = crc16(cmd, 23);
        short2Bytes(crc, buf);
        System.arraycopy(buf, 0, cmd, 23, 2);

        if (false == mMsgSession.writeData(0x01, cmd, 25)) {
            Log.e(TAG, "sendCommand() mMsgSession.writeData.");
            return false;
        }

        return true;

    }

    /**
     * 设备激活指令
     *
     * @param context 上下文
     * @param user    管理员账号
     * @param pwd     管理员密码
     * @param key     会话秘钥
     * @return 发送结果
     */
    public boolean sendCmd11(Context context, final byte[] user, final byte[] pwd, final byte[] key) {
        Log.d(TAG, "sendCmd11");
        short cmdLen = 32;
        byte[] cmd = new byte[128];

        waitChannelStatus();

        if (false == mMsgSession.initSession(MsgSession.INT_SESSION_TYPE_CMD, cmdLen + 5)) {
            Log.e(TAG, "sendCmd11() mMsgSession.initSession().");
            return false;
        }

        cmd[0] = 0x11;
        byte[] buf = new byte[32];
        short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        byte[] account = new byte[32];
        //写入管理员账号
        System.arraycopy(user, 0, account, 0, 16);
        //写入管理员密码
        System.arraycopy(pwd, 0, account, 16, 16);
        Log.d(TAG, "account = " + Arrays.toString(account));

        try {
            AES_ECB_PKCS7.AES256Encode(account, buf, key);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.arraycopy(buf, 0, cmd, 3, 32);

        mCrc = crc16(cmd, 35);
        short2Bytes(mCrc, buf);
        System.arraycopy(buf, 0, cmd, 35, 2);

        if (false == mMsgSession.writeData(0x01, cmd, 37)) {
            Log.e(TAG, "sendCommand() mMsgSession.writeData.");
            return false;
        }

        return true;
    }

    /**
     * apk->设备,通知智能锁进行锁体秘钥录入
     *
     * @param context 上下文
     * @param type    锁体密钥的类型
     * @param key     会话秘钥
     * @return 发送结果
     */
    public synchronized boolean sendCmd15(Context context, final byte[] type, final byte[] key) {
        Log.d(TAG, "sendCmd15");
        short cmdLen = 16;
        byte[] cmd = new byte[128];

        waitChannelStatus();

        while (MsgSession.hasNextPacket(MsgSession.INT_SESSION_TYPE_CMD)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (false == mMsgSession.initSession(MsgSession.INT_SESSION_TYPE_CMD, cmdLen + 5)) {
            Log.e(TAG, "sendCmd15() mMsgSession.initSession().");
            return false;
        }

        cmd[0] = 0x15;
        byte[] buf = new byte[16];
        short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);
        //写入Type
        byte[] typeBuf = new byte[16];
        System.arraycopy(type, 0, typeBuf, 0, 1);
        Arrays.fill(typeBuf, 1, 16, (byte) 15);

        try {
            AES_ECB_PKCS7.AES256Encode(typeBuf, buf, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //写入F1
        System.arraycopy(buf, 0, cmd, 3, 16);

        short crc = crc16(cmd, 19);
        Log.d(TAG, "crc = " + crc);
        short2Bytes(crc, buf);
        System.arraycopy(buf, 0, cmd, 19, 2);

        if (false == mMsgSession.writeData(0x01, cmd, 21)) {
            Log.e(TAG, "sendCommand() mMsgSession.writeData.");
            return false;
        }
        return true;
    }


    /**
     * APK智能锁。用来删除锁体密钥信息的消息
     *
     * @param context 上下文
     * @param lockId  锁体密钥编号，4字节
     * @param key     会话秘钥
     * @return 是否发送成功
     */
    public synchronized boolean sendCmd17(Context context, String lockId, final byte[] key) {
        Log.d(TAG, "sendCmd17");
        short cmdLen = 16;
        byte[] cmd = new byte[128];

        waitChannelStatus();

        if (false == mMsgSession.initSession(MsgSession.INT_SESSION_TYPE_CMD, cmdLen + 5)) {
            Log.e(TAG, "sendCmd17 mMsgSession.initSession().");
            return false;
        }

        cmd[0] = 0x17;
        byte[] buf = new byte[16];
        short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);
        //写入LockId
        byte[] lockIdBuf = new byte[16];
        int2Bytes(Integer.parseInt(lockId, 10), lockIdBuf);

        System.arraycopy(lockIdBuf, 0, lockIdBuf, 0, 4);

        Arrays.fill(lockIdBuf, 4, 16, (byte) 12);
        Log.d(TAG, "typeBuf = " + Arrays.toString(lockIdBuf));
        try {
            AES_ECB_PKCS7.AES256Encode(lockIdBuf, buf, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //写入F1
        System.arraycopy(buf, 0, cmd, 3, 16);

//        mCrc = crc16(cmd, 19);
//        Log.d(TAG, "mCrc = " + mCrc);
//        short2Bytes(mCrc, buf);
//        System.arraycopy(buf, 0, cmd, 19, 2);


        short crc = crc16(cmd, 19);
        Log.d(TAG, "crc = " + crc);
        short2Bytes(crc, buf);
        System.arraycopy(buf, 0, cmd, 19, 2);

        if (false == mMsgSession.writeData(0x01, cmd, 21)) {
            Log.e(TAG, "sendCommand() mMsgSession.writeData.");
            return false;
        }
        return true;
    }


    /**
     * APK智能锁。APK给智能锁下发的错误信息
     *
     * @param errorCode 错误编号，4字节
     *                  0X01 取消密钥录入（MSG15）
     *                  0X02 密钥录入成功
     *                  0X03 密钥录入失败
     * @param key       会话秘钥
     * @return 是否发送成功
     */
    public boolean sendCmd1F(String errorCode, final byte[] key) {
        Log.d(TAG, "sendCmd1F");
        short cmdLen = 16;
        byte[] cmd = new byte[128];

        waitChannelStatus();

        if (false == mMsgSession.initSession(MsgSession.INT_SESSION_TYPE_CMD, cmdLen + 5)) {
            Log.e(TAG, "sendCmd1F() mMsgSession.initSession().");
            return false;
        }

        cmd[0] = 0x1F;
        byte[] buf = new byte[16];
        short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);
        //写入LockId
        byte[] errorBuf = new byte[16];
        int2Bytes(Integer.parseInt(errorCode, 10), errorBuf);

        System.arraycopy(errorBuf, 0, errorBuf, 0, 4);

        Arrays.fill(errorBuf, 4, 16, (byte) 12);
        Log.d(TAG, "typeBuf = " + Arrays.toString(errorBuf));
        try {
            AES_ECB_PKCS7.AES256Encode(errorBuf, buf, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //写入F1
        System.arraycopy(buf, 0, cmd, 3, 16);
        mCrc = crc16(cmd, 19);
        Log.d(TAG, "mCrc = " + mCrc);
        short2Bytes(mCrc, buf);
        System.arraycopy(buf, 0, cmd, 19, 2);
        if (false == mMsgSession.writeData(0x01, cmd, 21)) {
            Log.e(TAG, "sendCommand() mMsgSession.writeData.");
            return false;
        }
        return true;
    }

    /**
     * APK智能锁。APK给智能锁下发的同步查询指令，智能锁通过MSG1A返回同步状态字
     *
     * @param key 会话秘钥
     * @return 是否发送成功
     */
    public boolean sendCmd19(final byte[] key) {
        Log.d(TAG, "sendCmd19");
        short cmdLen = 16;
        byte[] cmd = new byte[128];

        waitChannelStatus();

        if (false == mMsgSession.initSession(MsgSession.INT_SESSION_TYPE_CMD, cmdLen + 5)) {
            Log.e(TAG, "sendCmd19() mMsgSession.initSession().");
            return false;
        }

        cmd[0] = 0x19;
        byte[] buf = new byte[16];
        short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        byte[] cmdbuf = new byte[16];
        Arrays.fill(cmdbuf, 0, 16, (byte) 0);
        Log.d(TAG, "typeBuf = " + Arrays.toString(buf));

        try {
            AES_ECB_PKCS7.AES256Encode(cmdbuf, buf, key);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //写入F1
        System.arraycopy(buf, 0, cmd, 3, 16);

        short crc = crc16(cmd, 19);
        Log.d(TAG, "crc = " + crc);
        short2Bytes(crc, buf);
        System.arraycopy(buf, 0, cmd, 19, 2);
        if (false == mMsgSession.writeData(0x01, cmd, 21)) {
            Log.e(TAG, "sendCommand() mMsgSession.writeData.");
            return false;
        }
        return true;
    }


    public boolean recvResponse(byte[] data, int length) {
        int packet_length;

        if (true == mMsgSession.isNewSession(0x02)) {

            packet_length = (data[1] * 256) + (data[2] + 5);

            Log.i(TAG, "recvResponse() new session length = " + packet_length);

            if (packet_length > 512 || packet_length == 0) {
                Log.i(TAG, "recvResponse() length is to large.");
                return false;
            }

            mMsgSession.initSession(0x02, packet_length);

            return mMsgSession.writeData(0x02, data, length);
        } else {
            return mMsgSession.writeData(0x02, data, length);
        }
    }


    /**
     * @param value
     */
    public void processDevRsp(byte[] value) {
        byte[] c1 = Arrays.copyOfRange(value, 2, 6);

        if (INT_TAG_RSP == value[0]) {
            // response for mobile app...
            int cmd = byte2Int(c1);

            if (BleMsg.INT_TAG_CMD_BATTERY_LEVEL == cmd) {
                int batteryLevel = 0;
                byte[] p1 = Arrays.copyOfRange(value, 14, 18);
                batteryLevel = byte2Int(p1);
                notifyData(BleMsg.STR_RSP_BATTERY_LEVEL, batteryLevel);
            } else if (BleMsg.INT_TAG_CMD_SET_TIMEOUT == cmd) {
                int timeout = 0;
                byte[] p1 = Arrays.copyOfRange(value, 14, 18);
                timeout = byte2Int(p1);
                notifyData(BleMsg.STR_RSP_SET_TIMEOUT, timeout);
            } else if (BleMsg.INT_TAG_CMD_READ_CARD == cmd) {
                notifyData(BleMsg.ACTION_DATA_AVAILABLE, value);
            } else if (BleMsg.INT_TAG_CMD_RESET == cmd) {
                notifyData(BleMsg.STR_RSP_RESET);
            } else if (BleMsg.INT_TAG_CMD_CLOSE == cmd) {
                notifyData(BleMsg.STR_RSP_CLOSE);
            } else if (BleMsg.INT_TAG_CMD_POWEROFF == cmd) {
                notifyData(BleMsg.STR_RSP_POWEROFF);
            }
        } else {
            int packet_len;
            if (0x02 == value[0]) {
                Log.d(TAG, "recevie 02");
                packet_len = (value[1]) * 256 + (value[2] + 5);
                byte[] pdu = Arrays.copyOfRange(value, 3, packet_len - 2);
                Log.d(TAG, "pdu = " + Arrays.toString(pdu) + "pdu length : " + pdu.length);
                byte key[] = {
                        '1', '2', '3', '4', '*', '*', '1', '2', '3', '4', 0x06, 0x06, 0x06, 0x06,
                        0x06,
                        0x06,
                        0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F,
                        0x0F, 0x0F,
                        0x0F, 0x0F
                };

                byte random1[] = {
                        '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5',
                        '6'
                };
                Log.d(TAG, "random1 = " + Arrays.toString(random1));
                byte[] buf = new byte[64];
                try {
                    AES_ECB_PKCS7.AES256Decode(pdu, buf, key);
                    Log.d(TAG, "buf = " + Arrays.toString(buf));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                byte[] random = new byte[16];
                System.arraycopy(buf, 0, random, 0, 16);
                if (memcmp(random, random1, 16)) {
                    byte[] respRandom = new byte[16];
                    System.arraycopy(buf, 16, respRandom, 0, 16);
                    Log.d(TAG, "respRandom = " + Arrays.toString(respRandom));
                    byte[] AK = new byte[32];
                    System.arraycopy(buf, 32, AK, 0, 32);
                    Log.d(TAG, "AK = " + Arrays.toString(AK));
                    notifyData(BleMsg.EXTRA_DATA_MSG_02, respRandom, AK);
                }
                //设备->APK 设备应答信息
            } else if (0x1E == value[0]) {
                Log.d(TAG, "recevie 0x1E");
                packet_len = (value[1]) * 256 + (value[2] + 5);
                byte[] pdu = Arrays.copyOfRange(value, 3, packet_len - 2);
                Log.d(TAG, "pdu = " + Arrays.toString(pdu) + "pdu length : " + pdu.length);

                byte[] ik = Arrays.copyOfRange(value, packet_len - 2, packet_len);

                if (mCrc != byte2short(ik)) {
                    Log.d(TAG, "ik = " + ik);
                    notifyData(BleMsg.STR_RSP_IK_ERR);
                }
                byte key[] = {
                        '1', '2', '3', '4', '*', '*', '1', '2', '3', '4', 0x06, 0x06, 0x06, 0x06,
                        0x06,
                        0x06,
                        0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F,
                        0x0F, 0x0F,
                        0x0F, 0x0F
                };

                byte[] buf = new byte[16];
                try {
                    AES_ECB_PKCS7.AES256Decode(pdu, buf, key);
                    Log.d(TAG, "buf = " + Arrays.toString(buf));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                byte[] errCode = new byte[4];
                errCode = Arrays.copyOfRange(buf, 0, 4);
                Log.d(TAG, "buf = " + Arrays.toString(errCode));
                notifyData(BleMsg.STR_RSP_MSG1E_ERRCODE, errCode);

                //智能锁APK。智能锁将录入的锁体密钥上报给APK。
            } else if (0x16 == value[0]) {
                Log.d(TAG, "recevie 16");
                packet_len = (value[1]) * 256 + (value[2] + 5);
                byte[] pdu = Arrays.copyOfRange(value, 3, packet_len - 2);
                Log.d(TAG, "pdu = " + Arrays.toString(pdu) + "pdu length : " + pdu.length);

                byte[] ik = Arrays.copyOfRange(value, packet_len - 2, packet_len);

                if (mCrc != byte2short(ik)) {
                    notifyData(BleMsg.STR_RSP_IK_ERR);
                }
                byte key[] = {
                        '1', '2', '3', '4', '*', '*', '1', '2', '3', '4', 0x06, 0x06, 0x06, 0x06,
                        0x06,
                        0x06,
                        0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F,
                        0x0F, 0x0F,
                        0x0F, 0x0F
                };

                byte[] buf = new byte[16];
                try {
                    AES_ECB_PKCS7.AES256Decode(pdu, buf, key);
                    Log.d(TAG, "buf = " + Arrays.toString(buf));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                byte[] lockId = new byte[4];
                lockId = Arrays.copyOfRange(buf, 0, 4);
                notifyData(BleMsg.STR_RSP_MSG16_LOCKID, lockId);

                //智能锁APK。APK收到该状态字后和APK自身存储的状态字比较。
            } else if (0x1A == value[0]) {
                Log.d(TAG, "recevie 1A");
                packet_len = (value[1]) * 256 + (value[2] + 5);
                byte[] pdu = Arrays.copyOfRange(value, 3, packet_len - 2);

                byte[] ik = Arrays.copyOfRange(value, packet_len - 2, packet_len);

                if (mCrc != byte2short(ik)) {
                    notifyData(BleMsg.STR_RSP_IK_ERR);
                }

                byte key[] = {
                        '1', '2', '3', '4', '*', '*', '1', '2', '3', '4', 0x06, 0x06, 0x06, 0x06,
                        0x06,
                        0x06,
                        0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F,
                        0x0F, 0x0F,
                        0x0F, 0x0F
                };

                byte[] buf = new byte[16];
                try {
                    AES_ECB_PKCS7.AES256Decode(pdu, buf, key);
                    Log.d(TAG, "buf = " + Arrays.toString(buf));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                byte[] status = Arrays.copyOfRange(buf, 0, 4);
                notifyData(BleMsg.STR_RSP_MSG1A_STATUS, status);

                //是APK给智能锁下发密钥录入指令后，智能锁录入前提供给APK的超时时间，
            } else if (0x18 == value[0]) {
                Log.d(TAG, "recevie 18");
                packet_len = (value[1]) * 256 + (value[2] + 5);
                byte[] pdu = Arrays.copyOfRange(value, 3, packet_len - 2);

                byte[] ik = Arrays.copyOfRange(value, packet_len - 2, packet_len);

                if (mCrc != byte2short(ik)) {
                    notifyData(BleMsg.STR_RSP_IK_ERR);
                }

                byte key[] = {
                        '1', '2', '3', '4', '*', '*', '1', '2', '3', '4', 0x06, 0x06, 0x06, 0x06,
                        0x06,
                        0x06,
                        0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F,
                        0x0F, 0x0F,
                        0x0F, 0x0F
                };

                byte[] buf = new byte[16];
                try {
                    AES_ECB_PKCS7.AES256Decode(pdu, buf, key);
                    Log.d(TAG, "buf = " + Arrays.toString(buf));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                byte[] timeOut = Arrays.copyOfRange(buf, 0, 1);
                notifyData(BleMsg.STR_RSP_MSG18_TIMEOUT, timeOut);
            }


        }
    }

    /**
     * 比较两个byte数组数据是否相同,相同返回 true
     *
     * @param data1
     * @param data2
     * @param len
     * @return
     */
    public static boolean memcmp(byte[] data1, byte[] data2, int len) {
        if (data1 == null && data2 == null) {
            return true;
        }
        if (data1 == null || data2 == null) {
            return false;
        }
        if (data1 == data2) {
            return true;
        }

        boolean bEquals = true;
        int i;
        for (i = 0; i < data1.length && i < data2.length && i < len; i++) {
            if (data1[i] != data2[i]) {
                bEquals = false;
                break;
            }
        }

        return bEquals;
    }
}
