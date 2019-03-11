
package com.smart.lock.ble.message;

import android.os.Bundle;

import com.smart.lock.ble.BleMsg;
import com.smart.lock.utils.LogUtil;

import java.util.Arrays;

/**
 * 该方法类由传入的type生成对应的message消息
 */
public class MessageCreator {

    public static final String TAG = "MessageCreator";

    /**
     * 固定秘钥，由板卡出厂时写入，APK扫描二维码获得，调试写死，是会话秘钥生成的必要参数
     */
    public static final byte sk1[] = {
            '1', '2', '3', '4', '*', '*', '1', '2', '3', '4', 0x06, 0x06, 0x06, 0x06,
            0x06,
            0x06,
            0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F,
            0x0F, 0x0F,
            0x0F, 0x0F
    };

    public static byte mSK[] = new byte[32];

    /**
     * 账号密码随机数，调试写死
     */
    public static byte pwdRandom[] = new byte[16];
    public static byte pwdRandom1[] = {
            '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5',
            '6'
    };

    /**
     * 会话秘钥
     */
    public static byte mAK[] = new byte[32];

    /**
     * APP二维码加密
     */
    public static byte mQrSecret[] = {
            'D', 'T', 'S', '1', '5', '8', '6', 0x09, 0x09, 0x09, 0x09, 0x09, 0x09, 0x09, 0x09, 0x09,
            0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F
    };

    /**
     * 获取消息Message.TYPE_BLE_RECEV_CMD_02
     *
     * @param type   消息类型
     * @param ak     会话秘钥
     * @param random 设备回复的随机数
     * @return
     */
    public static Message getCmd02Message(byte type, byte[] ak, byte[] random) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        Bundle mBundle = mMessage.getData();

        if (ak != null && ak.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_AK, ak);
        }

        if (random != null && random.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_RANDOM, random);
        }
        return mMessage;
    }

    /**
     * 获取消息Message.TYPE_BLE_RECEV_CMD_04
     *
     * @param type        消息类型
     * @param batPerscent 电池电量
     * @param syncUsers   用户同步状态字
     * @param userStatus  用户状态
     * @param stStatus    设置状态字
     * @param unLockTime  回锁时间
     * @return
     */
    public static Message getCmd04Message(byte type, byte batPerscent, byte[] syncUsers, byte userStatus, byte stStatus, byte unLockTime, byte[] tmpPwdSk, byte[] userState) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        Bundle mBundle = mMessage.getData();
        LogUtil.d(TAG, "batPerscent = " + batPerscent);
        LogUtil.d(TAG, "syncUsers = " + Arrays.toString(syncUsers));
        LogUtil.d(TAG, "userStatus = " + userStatus);
        LogUtil.d(TAG, "stStatus = " + stStatus);
        LogUtil.d(TAG, "unLockTime = " + unLockTime);

        mBundle.putByte(BleMsg.KEY_BAT_PERSCENT, batPerscent);
        mBundle.putByte(BleMsg.KEY_USER_STATUS, userStatus);
        mBundle.putByte(BleMsg.KEY_SETTING_STATUS, stStatus);
        mBundle.putByte(BleMsg.KEY_UNLOCK_TIME, unLockTime);
        if (tmpPwdSk != null && tmpPwdSk.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_TMP_PWD_SK, tmpPwdSk);
        }
        if (syncUsers != null && syncUsers.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_SYNC_USERS, syncUsers);
        }

        if (userState != null && userState.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_USERS_STATE, userState);
        }

        return mMessage;
    }

    /**
     * 获取消息Message.TYPE_BLE_RECEV_CMD_12
     *
     * @param type     消息类型
     * @param userId   用户编号
     * @param nodeId   设备编号
     * @param bleMac   蓝牙地址
     * @param randCode 设备动态码
     * @param time     设备时间
     * @return
     */
    public static Message getCmd12Message(byte type, byte[] userId, byte[] nodeId, byte[] bleMac, byte[] randCode, byte[] time) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        mMessage.setKey(Message.TYPE_BLE_SEND_CMD_11 + "#" + "single");
        Bundle mBundle = mMessage.getData();

        if (userId != null && userId.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_USER_ID, userId);
        }
        if (nodeId != null && nodeId.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_NODE_ID, nodeId);
        }
        if (bleMac != null && bleMac.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_BLE_MAC, bleMac);
        }
        if (randCode != null && randCode.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_RAND_CODE, randCode);
        }

        if (time != null && time.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_LOCK_TIME, time);
        }
        return mMessage;
    }

    /**
     * 获取消息Message.TYPE_BLE_RECEV_CMD_1E
     *
     * @param type    消息类型
     * @param errCode 设备回复的错误编码
     * @return
     */
    public static Message getCmd1EMessage(byte type, byte[] errCode) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        Bundle mBundle = mMessage.getData();
        mMessage.setKey(Message.TYPE_BLE_SEND_CMD_11 + "#" + "single");

        if (errCode != null && errCode.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_ERROR_CODE, errCode);
        }
        return mMessage;
    }

    /**
     * 获取消息Message.TYPE_BLE_RECEV_CMD_2E
     *
     * @param type    消息类型
     * @param errCode 设备回复的错误编码
     * @return
     */
    public static Message getCmd2EMessage(byte type, byte[] errCode) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        Bundle mBundle = mMessage.getData();


        if (errCode != null && errCode.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_ERROR_CODE, errCode);
        }
        return mMessage;
    }

    /**
     * 获取消息Message.TYPE_BLE_RECEV_CMD_16
     *
     * @param type   消息类型
     * @param lockId 锁体密钥编号
     * @return
     */
    public static Message getCmd16Message(byte type, byte[] lockId) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        Bundle mBundle = mMessage.getData();


        if (lockId != null && lockId.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_LOCK_ID, lockId);
        }
        return mMessage;
    }

    /**
     * 获取消息Message.TYPE_BLE_RECEV_CMD_1A
     *
     * @param type   消息类型
     * @param status 智能锁本地密钥状态字
     * @return
     */
    public static Message getCmd1AMessage(byte type, byte[] status) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        Bundle mBundle = mMessage.getData();


        if (status != null && status.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_STATUS, status);
        }
        return mMessage;
    }

    /**
     * 获取消息Message.TYPE_BLE_RECEV_CMD_18
     *
     * @param type    消息类型
     * @param timeOut 智能锁密钥录入超时时间
     * @return
     */
    public static Message getCmd18Message(byte type, byte[] timeOut) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        Bundle mBundle = mMessage.getData();


        if (timeOut != null && timeOut.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_TIME_OUT, timeOut);
        }

//        if (count != null && count.length != 0) {
//            mBundle.putByteArray(BleMsg.KEY_TIME_OUT, count);
//        }
        return mMessage;
    }

    /**
     * 获取消息Message.TYPE_BLE_RECEV_CMD_16
     *
     * @param type 消息类型
     * @param msg  信息
     * @return
     */
    public static Message getCmd25Message(byte type, byte[] msg) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        mMessage.setKey(Message.TYPE_BLE_SEND_CMD_25 + "#" + "single");
        Bundle mBundle = mMessage.getData();

        if (msg != null && msg.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_USER_MSG, msg);
        }
        return mMessage;
    }

    /**
     * 获取消息Message.TYPE_BLE_RECEV_CMD_18
     *
     * @param type 消息类型
     * @param log  日志信息
     * @return
     */
    public static Message getCmd32Message(byte type, byte[] log) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        Bundle mBundle = mMessage.getData();

        if (log != null && log.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_LOG, log);
        }

//        if (count != null && count.length != 0) {
//            mBundle.putByteArray(BleMsg.KEY_TIME_OUT, count);
//        }
        return mMessage;
    }

    /**
     * 获取消息Message.TYPE_BLE_RECEV_CMD_3E
     *
     * @param type    消息类型
     * @param errCode 设备回复的错误编码
     * @return
     */
    public static Message getCmd3EMessage(byte type, byte[] errCode) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        Bundle mBundle = mMessage.getData();
        mMessage.setKey(Message.TYPE_BLE_SEND_CMD_33 + "#" + "single");

        if (errCode != null && errCode.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_ERROR_CODE, errCode);
        }
        return mMessage;
    }

}
