
package com.smart.lock.ble.message;

import android.os.Bundle;

import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;

/**
 * 该方法类由传入的type生成对应的message消息
 */
public class MessageCreator {

    public static final String TAG = "MessageCreator";

    /**
     * APP二维码加密
     */
    public static byte mQrSecret[] = {
            'D', 'T', 'S', '1', '5', '8', '6', 0x09, 0x09, 0x09, 0x09, 0x09, 0x09, 0x09, 0x09, 0x09,
            0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F
    };

    public static byte m256SK[] = new byte[32];//sk

    public static byte m128SK[] = new byte[16]; //sk

    public static byte pwdRandom[] = new byte[16]; //随机数

    public static boolean mIs128Code = true; //false 256

    public static boolean mIsOnceForTempPwd = true; //零时密码验证次数

    public static byte m256AK[] = new byte[32];//256会话秘钥

    public static byte m128AK[] = new byte[16]; //128会话秘钥

    /**
     * 设置秘钥
     */
    public static void setSk(DeviceInfo info) {
        String mac = info.getBleMac().replace(":", "");

        byte[] macByte = StringUtil.hexStringToBytes(mac);
        LogUtil.d(TAG, "macByte = " + Arrays.toString(macByte));
        if (mIs128Code) {
            byte[] code = new byte[10];
            String secretCode = info.getDeviceSecret();
            if (secretCode == null || secretCode.equals("0")) {
                System.arraycopy(StringUtil.hexStringToBytes("5A6B7C8D9E"), 0, MessageCreator.m128SK, 0, 5);
                System.arraycopy(macByte, 0, MessageCreator.m128SK, 5, 6); //写入MAC
                System.arraycopy(StringUtil.hexStringToBytes("A5B6C7D8E9"), 0, MessageCreator.m128SK, 11, 5);
            } else {
                System.arraycopy(macByte, 0, MessageCreator.m128SK, 0, 6); //写入MAC
                code = StringUtil.hexStringToBytes(secretCode);
                System.arraycopy(code, 0, MessageCreator.m128SK, 6, 10); //写入secretCode
            }
            LogUtil.d(TAG, "m128SK = " + Arrays.toString(m128SK));
        } else {
            System.arraycopy(macByte, 0, m256SK, 0, 6); //写入MAC
            byte[] code = new byte[10];
            String secretCode = info.getDeviceSecret();
            if (secretCode == null || secretCode.equals("0")) {
                Arrays.fill(m256SK, 6, 16, (byte) 0);
            } else {
                code = StringUtil.hexStringToBytes(secretCode);
                System.arraycopy(code, 0, m256SK, 6, 10); //写入secretCode
                Arrays.fill(m256SK, 16, 32, (byte) 0);
            }
            LogUtil.d(TAG, "m256AK = " + Arrays.toString(m256SK));
        }
    }


    /**
     * 获取消息Message.TYPE_BLE_RECEIVER_CMD_02
     *
     * @param type   消息类型
     * @param random 设备回复的随机数
     * @return
     */
    public static Message getCmd02Message(byte type, byte[] random,byte[] buf) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        mMessage.setKey(Message.TYPE_BLE_SEND_CMD_01 + "#" + "single");
        Bundle mBundle = mMessage.getData();

        if (random != null && random.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_RANDOM, random);
        }

        if (buf != null && buf.length != 0) {
            mBundle.putByteArray(BleMsg.RECEIVER_DATA, buf);
        }
        return mMessage;
    }

    /**
     * 获取消息Message.TYPE_BLE_RECEIVER_CMD_04
     *
     * @param type        消息类型
     * @param batPerscent 电池电量
     * @param syncUsers   用户同步状态字
     * @param userStatus  用户状态
     * @param stStatus    设置状态字
     * @param unLockTime  回锁时间
     * @return
     */
    public static Message getCmd04Message(byte type, byte batPerscent, byte[] syncUsers, byte userStatus, byte stStatus, byte unLockTime, byte[] tmpPwdSk, byte[] userState, byte[] powerSave) {
        Message message = Message.obtain();
        message.setType(type);
        message.setKey(Message.TYPE_BLE_SEND_CMD_03 + "#" + "single");
        Bundle bundle = message.getData();

        bundle.putByte(BleMsg.KEY_BAT_PERSCENT, batPerscent);
        bundle.putByte(BleMsg.KEY_USER_STATUS, userStatus);
        bundle.putByte(BleMsg.KEY_SETTING_STATUS, stStatus);
        bundle.putByte(BleMsg.KEY_UNLOCK_TIME, unLockTime);
        if (tmpPwdSk != null && tmpPwdSk.length != 0) {
            bundle.putByteArray(BleMsg.KEY_TMP_PWD_SK, tmpPwdSk);
        }
        if (syncUsers != null && syncUsers.length != 0) {
            bundle.putByteArray(BleMsg.KEY_SYNC_USERS, syncUsers);
        }

        if (userState != null && userState.length != 0) {
            bundle.putByteArray(BleMsg.KEY_USERS_STATE, userState);
        }

        if (powerSave != null && powerSave.length != 0) {
            bundle.putByteArray(BleMsg.KEY_POWER_SAVE, powerSave);
        }

        return message;
    }

    /**
     * MSG 0E 连接错误消息
     *
     * @param type      消息类型
     * @param errorCode 错误码
     * @return Message
     */
    public static Message getCmd0EMessage(byte type, byte[] errorCode) {
        Message message = Message.obtain();
        message.setType(type);
        message.setKey(Message.TYPE_BLE_SEND_CMD_03 + "#" + "single");
        Bundle mBundle = message.getData();
        if (errorCode != null && errorCode.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_ERROR_CODE, errorCode);
        }
        return message;
    }

    /**
     * 获取消息Message.TYPE_BLE_RECEIVER_CMD_12
     *
     * @param type     消息类型
     * @param authCode 用户授权码
     * @return
     */
    public static Message getCmd12Message(byte type, byte[] authCode) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        mMessage.setKey(Message.TYPE_BLE_SEND_CMD_11 + "#" + "single");
        Bundle mBundle = mMessage.getData();

        if (authCode != null && authCode.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_AUTH_CODE, authCode);
        }

        return mMessage;
    }

    /**
     * 获取消息 MESSAGE.TYPE_BLE_RECEIVER_CMD_1C
     *
     * @param type   消息类型
     * @param sn     设备序列号
     * @param sw_ver 软件版本
     * @param hw_ver 硬件版本
     * @return
     */
    public static Message getCmd1CMessage(byte type, byte[] sn, byte[] sw_ver, byte[] hw_ver) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        Bundle mBundle = mMessage.getData();

        if (sn != null && sn.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_NODE_SN, sn);
        }
        if (sw_ver != null && sw_ver.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_SW_VER, sw_ver);
        }
        if (hw_ver != null && hw_ver.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_HW_VER, hw_ver);
        }
        return mMessage;
    }

    /**
     * 获取消息Message.TYPE_BLE_RECEIVER_CMD_1E
     *
     * @param type    消息类型
     * @param errCode 设备回复的错误编码
     * @return
     */
    public static Message getCmd1EMessage(byte type, byte[] errCode) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        Bundle mBundle = mMessage.getData();
        if (errCode == null) {
            LogUtil.e(TAG, "errCode is null!");
            mMessage.setException(Message.EXCEPTION_RETURN_ERROR);
            return mMessage;
        }
        if (errCode[3] == 0x2 || errCode[3] == 0x3 || errCode[3] == 0x4
                || errCode[3] == 0x00 || errCode[3] == 0x1 || errCode[3] == 0x6
                || errCode[3] == 0x05 || errCode[3] == 0x7 || errCode[3] == 0x2B) {
            mMessage.setKey(Message.TYPE_BLE_SEND_CMD_11 + "#" + "single");
        } else if (errCode[3] == 0x8 || errCode[3] == 0x09 || errCode[3] == 0x0a
                || errCode[3] == 0x0b || errCode[3] == 0x0c || errCode[3] == 0x0d
                || errCode[3] == 0x0e || errCode[3] == 0x0f || errCode[3] == 0x10
                || errCode[3] == 0x23 || errCode[3] == 0x24 || errCode[3] == 0x25
                || errCode[3] == 0x2A || errCode[3] == BleMsg.TYPE_DEV_KEY_REPETITION) {
            mMessage.setKey(Message.TYPE_BLE_SEND_CMD_15 + "#" + "single");
        } else if (errCode[3] == BleMsg.TYPE_SET_TEMP_USER_LIFE_SUCCESS ||
                errCode[3] == BleMsg.TYPE_NO_AUTHORITY_1E) {
            mMessage.setKey(Message.TYPE_BLE_SEND_CMD_1B + "#" + "single");
        } else if (errCode[3] == BleMsg.TYPE_GROUP_DELETE_USER_SUCCESS ||
                errCode[3] == BleMsg.TYPE_GROUP_DELETE_USER_FAILED) {
            mMessage.setKey(Message.TYPE_BLE_SEND_CMD_13 + "#" + "single");
        } else if (errCode[3] == BleMsg.TYPE_GROUP_DELETE_KEY_SUCCESS ||
                errCode[3] == BleMsg.TYPE_GROUP_DELETE_KEY_FAILED
        ) {
            mMessage.setKey(Message.TYPE_BLE_SEND_CMD_17 + "#" + "single");
        }

        mBundle.putByteArray(BleMsg.KEY_ERROR_CODE, errCode);
        return mMessage;
    }

    /**
     * 获取消息Message.TYPE_BLE_RECEIVER_CMD_2E
     *
     * @param type    消息类型
     * @param errCode 设备回复的错误编码
     * @return
     */
    public static Message getCmd2EMessage(byte type, byte[] errCode) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        Bundle mBundle = mMessage.getData();
        if (errCode[3] == 0x00) {
            mMessage.setKey(Message.TYPE_BLE_SEND_CMD_21 + "#" + "single");
        }

        mBundle.putByteArray(BleMsg.KEY_ERROR_CODE, errCode);
        return mMessage;
    }

    /**
     * 获取消息Message.TYPE_BLE_RECEIVER_CMD_16
     *
     * @param type   消息类型
     * @param lockId 锁体密钥编号
     * @return
     */
    public static Message getCmd16Message(byte type, byte[] lockId) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        mMessage.setKey(Message.TYPE_BLE_SEND_CMD_15 + "#" + "single");
        Bundle mBundle = mMessage.getData();

        if (lockId != null && lockId.length != 0) {
            mBundle.putByteArray(BleMsg.KEY_LOCK_ID, lockId);
        }
        return mMessage;
    }

    /**
     * 获取消息Message.TYPE_BLE_RECEIVER_CMD_1A
     *
     * @param type   消息类型
     * @param status 智能锁本地密钥状态字
     * @return
     */
    public static Message getCmd1AMessage(byte type, byte key, byte[] status) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        Bundle mBundle = mMessage.getData();


        if (status != null && status.length != 0) {
            mBundle.putByte(BleMsg.KEY_TYPE, key);
            mBundle.putByteArray(BleMsg.KEY_STATUS, status);
        }
        return mMessage;
    }

    /**
     * 获取消息Message.TYPE_BLE_RECEIVER_CMD_18
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
     * 获取消息Message.TYPE_BLE_RECEIVER_CMD_16
     *
     * @param type 消息类型
     * @param msg  信息
     * @return
     */
    public static Message getCmd26Message(byte type, byte[] msg) {
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
     * 获取消息Message.TYPE_BLE_RECEIVER_CMD_18
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
     * 获取消息Message.TYPE_BLE_RECEIVER_CMD_3E
     *
     * @param type    消息类型
     * @param errCode 设备回复的错误编码
     * @return
     */
    public static Message getCmd3EMessage(byte type, byte[] errCode) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        Bundle mBundle = mMessage.getData();
        if (errCode[3] == 0x04) {
            mMessage.setKey(Message.TYPE_BLE_SEND_CMD_37 + "#" + "single");
        } else
            mMessage.setKey(Message.TYPE_BLE_SEND_CMD_33 + "#" + "single");

        mBundle.putByteArray(BleMsg.KEY_ERROR_CODE, errCode);
        return mMessage;
    }

    /**
     * 获取消息Message.TYPE_BLE_RECEIVER_CMD_62
     *
     * @param type 消息类型
     * @param pdu  返回字符串
     * @return
     */
    public static Message getCmd62Message(byte type, byte[] pdu) {
        Message mMessage = Message.obtain();
        mMessage.setType(type);
        Bundle mBundle = mMessage.getData();
        mMessage.setKey(Message.TYPE_BLE_SEND_CMD_61 + "#" + "single");

        mBundle.putByteArray(BleMsg.RECEIVER_DATA, pdu);
        return mMessage;
    }

}
