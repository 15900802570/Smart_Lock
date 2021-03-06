package com.smart.lock.ble.provider;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.smart.lock.ble.BleCommand;
import com.smart.lock.ble.creator.BleCmd05Creator;
import com.smart.lock.ble.creator.BleCmd2DCreator;
import com.smart.lock.ble.creator.BleCmd37Creator;
import com.smart.lock.ble.creator.BleCmd61Creator;
import com.smart.lock.ble.listener.BleMessageListener;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.listener.ClientTransaction;
import com.smart.lock.ble.creator.BleCmd01Creator;
import com.smart.lock.ble.creator.BleCmd03Creator;
import com.smart.lock.ble.creator.BleCmd11Creator;
import com.smart.lock.ble.creator.BleCmd13Creator;
import com.smart.lock.ble.creator.BleCmd15Creator;
import com.smart.lock.ble.creator.BleCmd17Creator;
import com.smart.lock.ble.creator.BleCmd19Creator;
import com.smart.lock.ble.creator.BleCmd1BCreator;
import com.smart.lock.ble.creator.BleCmd1DCreator;
import com.smart.lock.ble.creator.BleCmd21Creator;
import com.smart.lock.ble.creator.BleCmd25Creator;
import com.smart.lock.ble.creator.BleCmd29Creator;
import com.smart.lock.ble.creator.BleCmd31Creator;
import com.smart.lock.ble.creator.BleCmd33Creator;
import com.smart.lock.ble.creator.BleCmdOtaDataCreator;
import com.smart.lock.ble.creator.BleCreator;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.parser.BleCmd02Parse;
import com.smart.lock.ble.parser.BleCmd04Parse;
import com.smart.lock.ble.parser.BleCmd0EParse;
import com.smart.lock.ble.parser.BleCmd12Parse;
import com.smart.lock.ble.parser.BleCmd16Parse;
import com.smart.lock.ble.parser.BleCmd18Parse;
import com.smart.lock.ble.parser.BleCmd1AParse;
import com.smart.lock.ble.parser.BleCmd1CParse;
import com.smart.lock.ble.parser.BleCmd1EParse;
import com.smart.lock.ble.parser.BleCmd26Parse;
import com.smart.lock.ble.parser.BleCmd2EParse;
import com.smart.lock.ble.parser.BleCmd32Parse;
import com.smart.lock.ble.parser.BleCmd3EParse;
import com.smart.lock.ble.parser.BleCmd62Parse;
import com.smart.lock.ble.parser.BleCommandParse;
import com.smart.lock.entity.Device;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * BLE 底层提供者
 */
public class BleProvider {
    private static final String TAG = BleProvider.class.getSimpleName();

    /**
     * 发送队列放入超时
     */
    private static final long OFFER_SEND_QUEUE_TIMEOUT = 1000 * 1;

    /**
     * 数据源最长字节数
     */
    private static final int INT_SESSION_BUF_MAX_LEN = 512;

    /**
     * 发送标识
     */
    public static final int INT_SESSION_TYPE_CMD = 0x01;

    /**
     * 接受标识
     */
    public static final int INT_SESSION_TYPE_RSP = 0x02;

    /**
     * 未初始化
     */
    public static final int STATUS_UNINITIALIZATION = 0x0;

    /**
     * 运行中
     */
    public static final int STATUS_RUNNING = 0x1;
    /**
     * 已经关闭
     */
    public static final int STATUS_HALT = 0x2;

    /**
     * 标志：假
     */
    public static final int FLAG_FALSE = 0x01;
    /**
     * 标志：真
     */
    public static final int FLAG_TRUE = 0x02;

    /**
     * 缓存广播收到的指令，异步处理
     */
    protected ReceiverThread receiverThread;

    /**
     * 发送指令
     */
    protected SendThread sendThread;

    /**
     * 缓存广播收到的指令，异步处理,上行数据
     */
    protected ArrayBlockingQueue<byte[]> bleCommandQueue;
    /**
     * 终端发送数据，下行数据
     */
    protected ArrayBlockingQueue<Object> messageQueue;

    /**
     * 消息队列中只能有一个消息的消息映射，用于维护队列中消息的唯一性
     */
    protected SparseIntArray onceSendMessageMap;

    /**
     * 监听器映射表
     */
    protected Map<String, BleMessageListener> transactionBleMsgListenerMap;

    /**
     * ble指令创建器映射表
     */
    protected SparseArray<BleCreator> bleCreatorMap;
    /**
     * 消息UA映射表
     */
    protected SparseArray<BleMessageListener> messageListenerMap;
    /**
     * ble指令解析器映射表
     */
    protected Map<Byte, BleCommandParse> bleCommandParseMap;
    /**
     * 是否处于运行
     */
    protected int status = STATUS_UNINITIALIZATION;

    /**
     * 调试开关
     */
    protected boolean debug = false;

    /**
     * Gatt
     */
    private BluetoothGatt mBleGatt;

    /**
     * 蓝牙接收数据监听器
     */
    private BleMessageListener mBleMessageListener;

    /**
     * 接受数据长度
     */
    private static int mRspLength = 0;

    /**
     * 接受数据包计算长度
     */
    private static int mPacketLength = 0;

    /**
     * 接受数据包缓存
     */
    private static byte[] mRspBuf;

    /**
     * OTA线程
     */
    private Thread sendOTA;

    private boolean mCheckTimeOut = false; //是否超时
    private ClientTransaction mCt;

    public BleProvider(boolean debug, BluetoothGatt bleGatt) {

        // 初始化队列
        bleCommandQueue = new ArrayBlockingQueue(100);
        messageQueue = new ArrayBlockingQueue(100);

        // 初始化指令回调映射表
        messageListenerMap = new SparseArray();
        transactionBleMsgListenerMap = new HashMap();

        // 初始化指令创建器和指令解析器映射表
        bleCreatorMap = new SparseArray();
        bleCommandParseMap = new HashMap();

        // 初始化队列中只能有一个的指令消息映射
        onceSendMessageMap = new SparseIntArray();

        // 初始化接收线程
        receiverThread = new ReceiverThread(TAG + "--ReceiverThread");

        // 初始化发送线程
        sendThread = new SendThread(TAG + "--SendThread");

        this.debug = debug;

        mBleGatt = bleGatt;

        mRspLength = 0;

        mRspBuf = new byte[INT_SESSION_BUF_MAX_LEN];
        mCheckTimeOut = false;
    }

    /**
     * 启动BleProvider
     */
    public void start() {
        Log.i(TAG, "start");
        // 启动线程
        if (receiverThread != null)
            receiverThread.start();
        if (sendThread != null)
            sendThread.start();
    }

    /**
     * 接口调用，提交消息
     *
     * @param tran 事务定时器
     * @return 是否提交成功
     * @see ClientTransaction
     */
    public boolean send(ClientTransaction tran) {
        return offer(tran);
    }

    /**
     * 接口调用，提交消息
     *
     * @param message 消息
     * @return 是否提交成功
     * @see Message
     */
    public boolean send(Message message) {
        return offer(message);
    }


    /**
     * 提交消息
     *
     * @param message 消息
     * @return 是否提交成功
     * @see ClientTransaction
     * @see Message
     */
    private synchronized boolean offer(Object message) {
        if (status == STATUS_RUNNING) {
            try {
                if (message instanceof Message) {
                    Message msg = (Message) message;
                    int flag = onceSendMessageMap.get(msg.getType());
                    switch (flag) {
                        case FLAG_TRUE:
                            Log.w(TAG, "Queue has message "
                                    + Message.getMessageTypeTag(msg.getType()));
                            return false;
                        case FLAG_FALSE:
                            onceSendMessageMap.put(msg.getType(), FLAG_TRUE);
                            break;
                        default:
                            break;
                    }
                }

                return messageQueue.offer(message, OFFER_SEND_QUEUE_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Log.w(TAG, e.getMessage(), e);
            }
        } else {
            Log.w(TAG, "status is not running");
        }

        return false;
    }


    /**
     * 回调、唤醒等待，响应的命令
     *
     * @param command 蓝牙指令
     */
    public synchronized void onReceiveBle(byte[] command) {

        if (status == STATUS_RUNNING) {
            try {
                bleCommandQueue.offer(command, OFFER_SEND_QUEUE_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Log.w(TAG, e.getMessage(), e);
            }
        } else {
            Log.w(TAG, "status is not running");
        }


    }

    private static void clearSession() {
        mRspLength = mPacketLength = 0;
        Arrays.fill(mRspBuf, 0, INT_SESSION_BUF_MAX_LEN, (byte) 0);
    }

    /**
     * 发送消息
     *
     * @param msg         消息
     * @param transaction 消息事务
     * @see ClientTransaction
     * @see Message
     * @see BleCommand
     */
    private void sendMessage(Message msg, ClientTransaction transaction) {
        int flag = onceSendMessageMap.get(msg.getType());

        switch (flag) {
            case FLAG_TRUE:
            case FLAG_FALSE:
                onceSendMessageMap.put(msg.getType(), FLAG_FALSE);
                break;
            default:
                break;
        }

        if (debug) {
            LogUtil.d(TAG, "send msg : " + msg.toString());
        }

        // 获取创建器
        BleCreator creator = bleCreatorMap.get(msg.getType(), null);

        if (creator == null) {
            msg.recycle();
        } else {
            try {
                byte[] cmd = creator.create(msg);

                byte[] mRspBuf = new byte[20];

                int mRspRead = 0;
                int bufLen = 0;

                if (cmd != null) {

                    if (msg.isOta()) {
                        // 调用异步接口发送指令
                        if (cmd.length > 20) {

                            bufLen = cmd.length / 20;

                            if (cmd.length % 20 != 0)
                                bufLen++;

                            for (int len = 0; len < bufLen; len++) {
                                mRspRead = len * 20;
                                for (int i = 0; i < 20; i++) {
                                    if ((mRspRead + i) >= cmd.length) {
                                        break;
                                    }
                                    mRspBuf[i] = cmd[mRspRead + i];
                                }

                                if (sendOta(mRspBuf, msg.getType())) {
                                    Arrays.fill(mRspBuf, 0, 20, (byte) 0);
                                }
                            }
                        } else {
                            sendOta(cmd, msg.getType());
                        }
                        msg.recycle();
                    } else {
                        // 调用异步接口发送Ble指令
                        if (cmd.length > 20) {

                            bufLen = cmd.length / 20;

                            if (cmd.length % 20 != 0)
                                bufLen++;

                            for (int len = 0; len < bufLen; len++) {
                                mRspRead = len * 20;
                                for (int i = 0; i < 20; i++) {
                                    if ((mRspRead + i) >= cmd.length) {
                                        break;
                                    }
                                    mRspBuf[i] = cmd[mRspRead + i];
                                }

                                if (sendPacket(mRspBuf)) {
                                    Arrays.fill(mRspBuf, 0, 20, (byte) 0);
                                } else {
                                    Log.i(TAG, "send : " + cmd + " failure");
                                    // 设置发送失败状态
                                    msg.setException(Message.EXCEPTION_SEND_FAIL);
                                    // 分发命令
                                    dispatchMessage(msg, transaction);
                                }
                            }

                        } else {
                            if (!sendPacket(cmd))
                                Log.i(TAG, "send : " + cmd + " failure");
                            // 设置发送失败状态
                            msg.setException(Message.EXCEPTION_SEND_FAIL);
                            // 分发命令
                            dispatchMessage(msg, transaction);

                        }

                        if (msg.isForceReturnStatus()) {
                            // 分发命令
                            dispatchMessage(msg, transaction);
                        }

                        // 如果是事务ble下发，启动ble事务
                        if (transaction != null) {
                            Log.i(TAG, "Transaction start : " + transaction.getListenerKey());
                            addBleMsgListener(transaction);
                            transaction.startWatch();
                        } else {
                            // 消息回收
                            msg.recycle();
                        }
                    }
                } else {
                    Log.w(TAG, "send ble cmd is empty");
                    msg.recycle();
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                msg.recycle();
            }
        }
    }

    /**
     * 解析指令
     *
     * @param command        指令
     * @param bleMsgListener 回调监听器
     * @see BleCommand
     * @see BleMessageListener
     */
    private void parseBle(byte[] command, BleMessageListener bleMsgListener) {
        if (isNewCommand()) {
            mPacketLength = (command[1] * 256) + ((command[2] < 0 ? (256 + command[2]) : command[2]) + 5);

            if (mPacketLength > INT_SESSION_BUF_MAX_LEN || mPacketLength <= 0) {
                LogUtil.e(TAG, "recvResponse length is to large.");
                clearSession();
                return;
            }
        }

        for (int i = 0; i < command.length; i++) {
            if (mRspLength + i >= INT_SESSION_BUF_MAX_LEN) {
                clearSession();
                LogUtil.e(TAG, "max recvResponse length is to large.");
                return; //防止丢包
            }
            mRspBuf[mRspLength + i] = command[i];
        }

        mRspLength += command.length;

        if (mRspLength < mPacketLength || mPacketLength <= 0) {
            return;
        }

        byte[] cmdBuf = new byte[mPacketLength];
        System.arraycopy(mRspBuf, 0, cmdBuf, 0, mPacketLength);

        if (!checkCrc(cmdBuf)) {
            LogUtil.e(TAG, "crc error!");
            return;
        }

        clearSession();

        try {

            // 获取解析器
            BleCommandParse parse = bleCommandParseMap.get(cmdBuf[0]);
            if (parse != null) {
                // 解析成消息
                Message m = parse.parse(cmdBuf);

                if (m != null) {
                    if (m.getKey() != null) {
                        // 获取事务监听器
                        ClientTransaction ct = (ClientTransaction) removeBleMsgListener(m.getKey());
                        if (ct != null) {
                            Serializable serializable = ct.getMessage().getData().getSerializable(BleMsg.KEY_SERIALIZABLE);
                            // 停止定时器
                            ct.halt();
                            if (serializable != null) {
                                m.getData().putSerializable(BleMsg.KEY_SERIALIZABLE, serializable);
                            } else {
                                Log.i(TAG,
                                        "bleMsgListener  : "
                                                + Message.getMessageTypeTag(m.getType())
                                                + " is null");
                            }
                        }
                    }
                    // 分发消息
                    dispatchMessage(m, bleMsgListener);
                } else {
                    Log.w(TAG, "ble parse : " + " message is null");
                }
            } else {
//                Log.w(TAG, "ble Command : " + bleCommand.command + " has no parse");
            }

        } catch (
                Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private boolean checkCrc(byte[] cmd) {
        int packetLen = cmd.length;
        byte[] ik = Arrays.copyOfRange(cmd, packetLen - 2, packetLen);
        short revCrc = StringUtil.byte2short(ik);

        byte[] revCmd = Arrays.copyOfRange(cmd, 0, packetLen - 2);
        short crc = StringUtil.crc16(revCmd, revCmd.length);

        LogUtil.d(TAG, "revCrc : " + revCrc + " ,crc : " + crc);

        return (revCrc == crc);
    }

    private boolean isNewCommand() {
        if (0 == mRspLength)
            return true;
        else
            return false;
    }

    /**
     * 分发消息
     *
     * @param msg            消息
     * @param bleMsgListener 回调监听器
     */
    private void dispatchMessage(Message msg, BleMessageListener bleMsgListener) {
        LogUtil.d(TAG, "msg : " + (msg == null ? true : msg.toString()));
        if (msg != null) {
            if (mCt != null) {
                bleMsgListener.onReceive(msg, mCt.getTimer());
            } else
                bleMsgListener.onReceive(msg, null);
        }
    }

    /**
     * 初始化BLE模块
     */
    private void init() {
        status = STATUS_RUNNING;

        // 填充ble指令生成器映射表
        bleCreatorMap.put(Message.TYPE_BLE_SEND_CMD_01, new BleCmd01Creator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_CMD_03, new BleCmd03Creator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_CMD_05, new BleCmd05Creator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_CMD_11, new BleCmd11Creator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_CMD_13, new BleCmd13Creator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_CMD_15, new BleCmd15Creator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_CMD_17, new BleCmd17Creator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_CMD_1B, new BleCmd1BCreator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_CMD_19, new BleCmd19Creator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_CMD_1D, new BleCmd1DCreator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_CMD_21, new BleCmd21Creator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_CMD_25, new BleCmd25Creator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_CMD_29, new BleCmd29Creator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_CMD_2D, new BleCmd2DCreator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_CMD_31, new BleCmd31Creator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_CMD_33, new BleCmd33Creator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_CMD_37, new BleCmd37Creator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_CMD_61, new BleCmd61Creator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_OTA_CMD, new BleCmdOtaDataCreator());
        bleCreatorMap.put(Message.TYPE_BLE_SEND_OTA_DATA, new BleCmdOtaDataCreator());
        bleCreatorMap.put(Message.TYPE_BLE_FP_SEND_OTA_DATA, new BleCmdOtaDataCreator());

        // 填充ble指令监听器映射表
        messageListenerMap.put(Message.TYPE_BLE_RECEIVER_CMD_02, mBleMessageListener);
        messageListenerMap.put(Message.TYPE_BLE_RECEIVER_CMD_04, mBleMessageListener);
        messageListenerMap.put(Message.TYPE_BLE_RECEIVER_CMD_0E, mBleMessageListener);
        messageListenerMap.put(Message.TYPE_BLE_RECEIVER_CMD_1A, mBleMessageListener);
        messageListenerMap.put(Message.TYPE_BLE_RECEIVER_CMD_1C, mBleMessageListener);
        messageListenerMap.put(Message.TYPE_BLE_RECEIVER_CMD_1E, mBleMessageListener);
        messageListenerMap.put(Message.TYPE_BLE_RECEIVER_CMD_12, mBleMessageListener);
        messageListenerMap.put(Message.TYPE_BLE_RECEIVER_CMD_16, mBleMessageListener);
        messageListenerMap.put(Message.TYPE_BLE_RECEIVER_CMD_18, mBleMessageListener);
        messageListenerMap.put(Message.TYPE_BLE_RECEIVER_CMD_2E, mBleMessageListener);
        messageListenerMap.put(Message.TYPE_BLE_RECEIVER_CMD_26, mBleMessageListener);
        messageListenerMap.put(Message.TYPE_BLE_RECEIVER_CMD_32, mBleMessageListener);
        messageListenerMap.put(Message.TYPE_BLE_RECEIVER_CMD_3E, mBleMessageListener);
        messageListenerMap.put(Message.TYPE_BLE_RECEIVER_CMD_62, mBleMessageListener);

        //填充ble指令接收器映射表
        bleCommandParseMap.put(Message.TYPE_BLE_RECEIVER_CMD_0E, new BleCmd0EParse());
        bleCommandParseMap.put(Message.TYPE_BLE_RECEIVER_CMD_1A, new BleCmd1AParse());
        bleCommandParseMap.put(Message.TYPE_BLE_RECEIVER_CMD_1C, new BleCmd1CParse());
        bleCommandParseMap.put(Message.TYPE_BLE_RECEIVER_CMD_1E, new BleCmd1EParse());
        bleCommandParseMap.put(Message.TYPE_BLE_RECEIVER_CMD_02, new BleCmd02Parse());
        bleCommandParseMap.put(Message.TYPE_BLE_RECEIVER_CMD_04, new BleCmd04Parse());
        bleCommandParseMap.put(Message.TYPE_BLE_RECEIVER_CMD_12, new BleCmd12Parse());
        bleCommandParseMap.put(Message.TYPE_BLE_RECEIVER_CMD_16, new BleCmd16Parse());
        bleCommandParseMap.put(Message.TYPE_BLE_RECEIVER_CMD_18, new BleCmd18Parse());
        bleCommandParseMap.put(Message.TYPE_BLE_RECEIVER_CMD_26, new BleCmd26Parse());
        bleCommandParseMap.put(Message.TYPE_BLE_RECEIVER_CMD_2E, new BleCmd2EParse());
        bleCommandParseMap.put(Message.TYPE_BLE_RECEIVER_CMD_32, new BleCmd32Parse());
        bleCommandParseMap.put(Message.TYPE_BLE_RECEIVER_CMD_3E, new BleCmd3EParse());
        bleCommandParseMap.put(Message.TYPE_BLE_RECEIVER_CMD_62, new BleCmd62Parse());
    }

    /**
     * 调用接口，发送ble指令
     */
    class SendThread extends Thread {

        public SendThread(String threadName) {
            super(threadName);
        }

        @Override
        public void run() {
            // 初始化Provider
            init();

            while (sendThread == Thread.currentThread()) {
                try {
                    if (!mCheckTimeOut) {
                        Object obj = messageQueue.take();
                        if (obj instanceof Message) {
                            // 直接发送消息
                            Message msg = (Message) obj;
                            sendMessage(msg, null);
                        } else if (obj instanceof ClientTransaction) {
                            // 直接发送消息
                            ClientTransaction transaction = (ClientTransaction) obj;
                            sendMessage(transaction.getMessage(), transaction);
                        } else {
                            // 未知类型消息
                            Log.w(TAG, "Unknow obj class : "
                                    + (obj == null ? obj : obj.getClass().getName()));
                        }
                    }
                } catch (InterruptedException e) {

                }
            }
        }
    }

    /**
     * 处理android广播接收到指令后，放到队列处的指令，进行二次处理分发
     */
    class ReceiverThread extends Thread {

        public ReceiverThread(String threadName) {
            super(threadName);
        }

        @Override
        public void run() {

            while (receiverThread == Thread.currentThread()) {
                try {
                    byte[] command = bleCommandQueue.take();
                    parseBle(command, mBleMessageListener);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                }
            }
        }

    }

    /**
     * 注册监听，供项目调用
     *
     * @param bleMessageListener
     */
    public void registerMessageCallBack(BleMessageListener bleMessageListener) {
        mBleMessageListener = bleMessageListener;
    }

    public synchronized void halt() {
        Thread t = receiverThread;
        receiverThread = null;
        if (t != null)
            t.interrupt();

        t = sendThread;
        sendThread = null;
        if (t != null)
            t.interrupt();

        bleCommandQueue.clear();
        messageQueue.clear();

        bleCreatorMap.clear();
        messageListenerMap.clear();
        bleCommandParseMap.clear();

        for (Map.Entry<String, BleMessageListener> entry : transactionBleMsgListenerMap.entrySet()) {
            BleMessageListener listener = entry.getValue();

            if (listener != null) {
                listener.halt();
            }
        }

        transactionBleMsgListenerMap.clear();
        status = STATUS_HALT;
    }

    public synchronized void clear() {
        bleCommandQueue.clear();
        unRegisterclear();

    }

    public void unRegisterclear() {
        messageQueue.clear();

        for (Map.Entry<String, BleMessageListener> entry : transactionBleMsgListenerMap.entrySet()) {
            BleMessageListener listener = entry.getValue();

            if (listener != null) {
                listener.halt();
            }
        }
        transactionBleMsgListenerMap.clear();
    }

    public boolean addBleMsgListener(BleMessageListener listener) {

        transactionBleMsgListenerMap.put(listener.getListenerKey(), listener);
        if (debug) {
            Log.d(TAG, "addbleMsgListener : " + listener.getListenerKey()
                    + " transactionBleMsgListenerMap size : "
                    + transactionBleMsgListenerMap.size());
        }
        mCt = (ClientTransaction) listener;
        mCheckTimeOut = true;
        return true;
    }

    public synchronized BleMessageListener removeBleMsgListener(String key) {
        if (key == null) {
            return null;
        }
        mCheckTimeOut = false;
        mCt = null;
        return transactionBleMsgListenerMap.remove(key);
    }

    public synchronized BleMessageListener removeBleMsgListener(BleMessageListener listener) {
        return removeBleMsgListener(listener.getListenerKey());
    }

    public synchronized boolean containsKey(String key) {
        return transactionBleMsgListenerMap.containsKey(key);
    }

    /**
     * Enable Notification on TX characteristic
     *
     * @return
     */
    public void enableTXNotification() {
        LogUtil.i(TAG, "enableTXNotification!");
        BluetoothGattService RxService = mBleGatt.getService(Device.RX_SERVICE_UUID);
        if (RxService == null) {
            LogUtil.e("Rx service not found!");
            return;
        }
        BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(Device.TX_CHAR_UUID);
        if (TxChar == null) {
            LogUtil.e("Tx charateristic not found!");
            return;
        }
        mBleGatt.setCharacteristicNotification(TxChar, true);

        BluetoothGattDescriptor descriptor = TxChar.getDescriptor(Device.CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBleGatt.writeDescriptor(descriptor);
    }

    /**
     * Enable Notification on TX characteristic
     *
     * @return
     */
    public void enableTXDev() {
        LogUtil.i(TAG, "enableTXDev!");

        UUID RX_SERVICE_UUID = UUID.fromString("00010203-0405-0607-0809-0a0b0c0d1912");
        UUID RX_CMD_UUID = UUID.fromString("00010203-0405-0607-0809-0a0b0c0d2b12");

        BluetoothGattService RxService = mBleGatt.getService(RX_SERVICE_UUID);
        if (RxService == null) {
            LogUtil.e("Rx service not found!");
            return;
        }
        BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(RX_CMD_UUID);
        if (TxChar == null) {
            LogUtil.e("Tx charateristic not found!");
            return;
        }
        mBleGatt.setCharacteristicNotification(TxChar, true);

        List<BluetoothGattDescriptor> descriptors = TxChar.getDescriptors();
        for (BluetoothGattDescriptor descriptor : descriptors) {

            boolean b1 = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            if (b1) {
                mBleGatt.writeDescriptor(descriptor);
                Log.d(TAG, "startRead: " + "监听收数据");
            }

        }
    }

    private BluetoothGattCharacteristic findNotifyCharacteristic(
            BluetoothGattService service, UUID characteristicUUID) {

        BluetoothGattCharacteristic characteristic = null;

        List<BluetoothGattCharacteristic> characteristics = service
                .getCharacteristics();

        for (BluetoothGattCharacteristic c : characteristics) {
            if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                    && characteristicUUID.equals(c.getUuid())) {
                characteristic = c;
                break;
            }
        }

        if (characteristic != null)
            return characteristic;

        for (BluetoothGattCharacteristic c : characteristics) {
            if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                    && characteristicUUID.equals(c.getUuid())) {
                characteristic = c;
                break;
            }
        }

        return characteristic;
    }

    private synchronized boolean sendPacket(byte[] value) {
        BluetoothGattService RxService = mBleGatt.getService(Device.RX_SERVICE_UUID);

        try {
            Thread.sleep(20);
        } catch (InterruptedException ex) {
        }

        if (RxService == null) {
            LogUtil.e(TAG, "RxService is null!");
            return false;
        }

        BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(Device.RX_CHAR_UUID);
        if (RxChar == null) {
            LogUtil.e(TAG, "RxChar is null!");
            return false;
        }

        RxChar.setValue(value);

        return mBleGatt.writeCharacteristic(RxChar);
    }

    private synchronized boolean sendOta(byte[] value, int type) {
        UUID RX_SERVICE_UUID = UUID.fromString("00010203-0405-0607-0809-0a0b0c0d1912");
        UUID RX_CMD_UUID = UUID.fromString("00010203-0405-0607-0809-0a0b0c0d2b12");
        switch (type) {
            case Message.TYPE_BLE_FP_SEND_OTA_DATA:
                RX_SERVICE_UUID = Device.RX_SERVICE_UUID;
                RX_CMD_UUID = Device.RX_CHAR_UUID;
                break;
            case Message.TYPE_BLE_SEND_OTA_DATA:
                RX_SERVICE_UUID = UUID.fromString("00010203-0405-0607-0809-0a0b0c0d1912");
                RX_CMD_UUID = UUID.fromString("00010203-0405-0607-0809-0a0b0c0d2b12");
                break;
        }

        BluetoothGattService RxService = mBleGatt.getService(RX_SERVICE_UUID);

        if (RxService == null) {
            Log.e(TAG, "RxService is null");
            return false;
        }
        BluetoothGattCharacteristic rxChar = RxService.getCharacteristic(RX_CMD_UUID);

        if (rxChar == null) {
            return false;
        }
        rxChar.setValue(value);
        rxChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        return mBleGatt.writeCharacteristic(rxChar);
    }

}
