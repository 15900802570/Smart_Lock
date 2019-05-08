
package com.smart.lock.ble.message;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import java.io.Serializable;
import java.lang.reflect.Field;


/**
 * Ble消息
 */
public class Message implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -4468357650430213677L;
    /**
     * 日志标签
     */
    public static final String TAG = Message.class.getSimpleName();

    /**
     * 异常状态： 无异常状态
     */
    public static final byte EXCEPTION_NORMAL = 0x00;
    /**
     * 异常状态： 超时（由事务管理器处理）
     */
    public static final byte EXCEPTION_TIMEOUT = 0x01;
    /**
     * 异常状态： 发送失败
     */
    public static final byte EXCEPTION_SEND_FAIL = 0x02;
    /**
     * 异常状态： 返回ERROR
     */
    public static final byte EXCEPTION_RETURN_ERROR = 0x03;

    /**
     * 消息池大小
     */
    private static int sPoolSize = 0;
    /**
     * 消息创建计数
     */
    private static int createCount = 0;
    /**
     * 同步对象
     */
    private static final Object sPoolSync = new Object();
    /**
     * 消息池
     */
    private static Message sPool;
    /**
     * 消息池最大消息数
     */
    private static final int MAX_POOL_SIZE = 50;
    /**
     * 调试日志开关
     */
    private static final boolean debug = true;
    /**
     * 消息类型标签映射表
     */
    private static final SparseArray<String> mMsgTypeTagArray;

    /**
     * 会话秘钥
     */
    private byte[] AK = new byte[]{};

    /**
     * MSG 01 是智能锁接收到的建立安全连接的第一条指令
     */
    public static final byte TYPE_BLE_SEND_CMD_01 = 0x01;


    /**
     * APK通知智能锁进行锁体密钥录入的消息
     */
    public static final byte TYPE_BLE_RECEIVER_CMD_02 = 0x02;

    /**
     * MSG 03是建立安全连接的最后一个数据包
     */
    public static final byte TYPE_BLE_SEND_CMD_03 = 0x03;

    /**
     * MSG 04是智能锁对在安全连接后主动发给APK的消息，带有电池电量、用户同步字和用户状态字信息
     */
    public static final byte TYPE_BLE_RECEIVER_CMD_04 = 0x04;

    //MSG 05是没有安全连接直接设置SN/MAC/IMEI的消息，调试使用
    public static final byte TYPE_BLE_SEND_CMD_05 = 0x05;

    /**
     * MSG 0E APK与智能锁连接失败后的错误消息， 明文传输
     */
    public static final byte TYPE_BLE_RECEIVER_CMD_0E = 0x0E;

    /**
     * MSG 12是智能锁回复给APK的带有授权码的信息，对于通过扫描绑定新增的用户默认状态就是启用，其他通过管理员创建的用户默认状态都是非启用，只有该新建用户和智能锁连接过后状态才是启用。
     */
    public static final byte TYPE_BLE_RECEIVER_CMD_12 = 0x12;

    /**
     * MSG 11是APK发给智能锁的激活消息
     */
    public static final byte TYPE_BLE_SEND_CMD_11 = 0x11;

    /**
     * MSG 13用来删除特定用户群。
     */
    public static final byte TYPE_BLE_SEND_CMD_13 = 0x13;

    /**
     * APK通知智能锁进行锁体密钥录入的消息
     */
    public static final byte TYPE_BLE_SEND_CMD_15 = 0x15;

    /**
     * MSG 16是智能锁将录入的锁体密钥上报给APK。
     */
    public static final byte TYPE_BLE_RECEIVER_CMD_16 = 0x16;

    /**
     * MSG 16是智能锁录入前提供给APK的超时时间。
     */
    public static final byte TYPE_BLE_RECEIVER_CMD_18 = 0x18;

    /**
     * MSG 17是APK用来删除锁体密钥信息的消息
     */
    public static final byte TYPE_BLE_SEND_CMD_17 = 0x17;

    /**
     * APK配置智能锁临时用户的时效类型
     */
    public static final byte TYPE_BLE_SEND_CMD_1B = 0x1B;

    /**
     * MSG19是APK给智能锁下发的同步查询指令
     */
    public static final byte TYPE_BLE_SEND_CMD_19 = 0x19;
    /**
     * MSG1D是APK给智能锁 回锁时间设置
     */
    public static final byte TYPE_BLE_SEND_CMD_1D = 0x1D;
    /**
     * MSG 1E 是智能锁在配置基本信息过程中给服务器上报的消息
     */
    public static final byte TYPE_BLE_RECEIVER_CMD_1E = 0x1E;

    /**
     * MSG 1A 是APK收到该状态字后和APK自身存储的状态字比较
     */
    public static final byte TYPE_BLE_RECEIVER_CMD_1A = 0x1A;

//    /**
//     * OTA升级
//     */
//    public static final byte TYPE_BLE_SEND_CMD_OTA = 0x1B;

    /**
     * 设备版本信息
     */
    public static final byte TYPE_BLE_RECEIVER_CMD_1C = 0x1C;
    /**
     * 远程开锁
     */
    public static final byte TYPE_BLE_SEND_CMD_21 = 0x21;

    /**
     * 查询用户信息
     */
    public static final byte TYPE_BLE_SEND_CMD_25 = 0x25;

    /**
     * MSG 26是智能锁回复给APK的用户相关信息，其中密钥及临时用户有效时间段如果没有全部设置，则默认为0。
     */
    public static final byte TYPE_BLE_RECEIVER_CMD_26 = 0x26;

    /**
     * APK配置智能锁临时用户的生命周期，不配置则生命周期一直存在，同普通用户，通过MSG2E返回结果
     */
    public static final byte TYPE_BLE_SEND_CMD_29 = 0x29;

    /**
     * APK配置智能锁省电时间段
     */
    public static final byte TYPE_BLE_SEND_CMD_2D = 0x2D;

    /**
     * 日志查询
     */
    public static final byte TYPE_BLE_SEND_CMD_31 = 0x31;

    /**
     * MSG 2E 是智能锁在远程开锁命令后的回应。
     */
    public static final byte TYPE_BLE_RECEIVER_CMD_2E = 0x2E;

    /**
     * 智能锁使用MSG 32推送日志给APK，每次只推送一条
     */
    public static final byte TYPE_BLE_RECEIVER_CMD_32 = 0x32;

    /**
     * MSG 33是APK从智能锁查删除日志，通过MSG3E回复结果
     */
    public static final byte TYPE_BLE_SEND_CMD_33 = 0x33;

    /**
     * MSG 3E是智能锁在log传输过程中上报的消息
     */
    public static final byte TYPE_BLE_RECEIVER_CMD_3E = 0x3E;

    /**
     * OTA升级命令
     */
    public static final byte TYPE_BLE_SEND_OTA_CMD = 0x50;

    /**
     * OTA升级数据
     */
    public static final byte TYPE_BLE_SEND_OTA_DATA = 0x51;

    static {

        // 初始化消息类型标签映射表
        mMsgTypeTagArray = new SparseArray<String>();

        for (Field field : Message.class.getDeclaredFields()) {
            String name = field.getName();

            // 消息类型必须以TYPE_开头，且不能重复
            if (!TextUtils.isEmpty(name) && name.startsWith("TYPE_")) {
                try {
                    int type = field.getInt(null);

                    if (mMsgTypeTagArray.indexOfKey(type) < 0) {
                        mMsgTypeTagArray.put(field.getInt(null), name);
                    } else {
                        throw new IllegalArgumentException("Message Type is repeat");
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * 下一个对象
     */
    private Message next;
    /**
     * 消息异常状态
     */
    protected int exception = EXCEPTION_NORMAL;
    /**
     * 消息类型
     */
    protected int type;
    /**
     * 是否使用ota接口发送指令
     */
    protected boolean isOta = false;
    /**
     * 指令发送接口超时时间
     */
    protected int timeout = 30;
    /**
     * 消息参数
     */
    protected Bundle data;
    /**
     * 异步Ble接口强制返回状态, 状态为true发送失败或者成功都返回状态， 状态为false发送失败时返回状态
     */
    protected boolean forceReturnStatus = false;
    /**
     * 监听时使用的Key，用于回调时找到调用的消息定时器
     */
    protected String key = null;

    /**
     * 消息参数
     *
     * @return
     */
    public Bundle getData() {
        if (data == null) {
            data = new Bundle();
        }
        return data;
    }

    /**
     * 返回消息类型
     *
     * @return
     */
    public int getType() {
        return type;
    }

    /**
     * 设置消息类型
     *
     * @param type
     */
    public void setType(int type) {
        if (debug) {
            Log.d(TAG, "Message : " + hashCode() + " type : " + type);
        }
        this.type = type;
    }

    /**
     * 设置会话秘钥
     *
     * @param ak 会话秘钥
     */
    public void setAk(byte[] ak) {
        AK = ak;
    }

    /**
     * 返回会话秘钥
     */
    public byte[] getAK() {
        return AK;
    }

    /**
     * 获取异常状态
     *
     * @return
     */
    public int getException() {
        return exception;
    }

    /**
     * 设置异常状态
     *
     * @param exception
     */
    public void setException(int exception) {
        this.exception = exception;
    }

    /**
     * 是否为指令OTA接口
     *
     * @return
     */
    public boolean isOta() {
        return isOta;
    }

    /**
     * 设置指令同步
     *
     * @param isOta
     */
    public void setOta(boolean isOta) {
        this.isOta = isOta;
    }

    /**
     * 获取AT指令发送接口超时时间
     *
     * @return
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * 设置AT指令发送接口超时时间
     *
     * @param timeout
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean isForceReturnStatus() {
        return forceReturnStatus;
    }

    public void setForceReturnStatus(boolean forceReturnStatus) {
        this.forceReturnStatus = forceReturnStatus;
    }

    /**
     * 回收消息
     */
    public void recycle() {
        synchronized (sPoolSync) {
            exception = EXCEPTION_NORMAL;

            if (sPoolSize < MAX_POOL_SIZE) {
                Message msg = sPool;

                sPoolSize++;
                next = msg;
                sPool = this;
            } else {
                Log.w(TAG, "PoolSize is full");
            }

            clear();

            if (debug) {
                Log.d(TAG, "recycle " + hashCode() + " sPoolSize : " + sPoolSize);
            }
        }
    }

    /**
     * @return
     * @see Message#key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key
     * @see Message#key
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * 清空消息状态
     */
    private void clear() {
        exception = EXCEPTION_NORMAL;
        type = 0;
        isOta = false;
        timeout = 30;
        forceReturnStatus = false;
        key = null;
        if (data != null) {
            data.clear();
            data = null;
        }
    }

    /**
     * 获取对象
     */
    public static Message obtain() {
        synchronized (sPoolSync) {
            Message msg = sPool;

            if (msg == null) {
                if (debug) {
                    Log.d(TAG, "new count " + createCount);
                    createCount++;
                }

                msg = new Message();
            } else {
                sPoolSize--;
                sPool = msg.next;
                msg.next = null;
            }

            if (debug) {
                Log.d(TAG, "obtain " + msg.hashCode() + " sPoolSize " + sPoolSize);
            }
            return msg;
        }
    }

    /**
     * 获取消息对象池大小
     *
     * @return 消息对象池大小
     */
    public static int getsPoolSize() {
        return sPoolSize;
    }

    /**
     * 获取消息类型标签
     *
     * @param type 消息类型
     * @return 消息类型标签
     */
    public static String getMessageTypeTag(int type) {
        return mMsgTypeTagArray.get(type);
    }


    @Override
    public String toString() {
        return "Message [exception=" + exception + ", type=" + type + ", isSyn=" + isOta + ", bundle=" + getData().toString() + "]";
    }

}
