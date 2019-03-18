package com.smart.lock.ble.creator;

import android.os.Bundle;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;

/**
 * apk->设备,APK配置智能锁临时用户的时效类型
 */
public class BleCmd1BCreator implements BleCreator {


    private static final String TAG = BleCmd1BCreator.class.getSimpleName();

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public byte[] create(Message message) {

        Bundle data = message.getData();

        short userId = data.getShort(BleMsg.KEY_USER_ID);
        byte type = data.getByte(BleMsg.KEY_CMD_TYPE);
        byte[] unlockTime = data.getByteArray(BleMsg.KEY_UNLOCK_IMEI);

        short cmdLen = 32;
        byte[] cmd = new byte[128];

        cmd[0] = 0x1B;
        byte[] buf = new byte[32];
        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        byte[] cmdBuf = new byte[32];
        byte[] userIdBuf = new byte[2];

        StringUtil.short2Bytes(userId, userIdBuf);
        System.arraycopy(userIdBuf, 0, cmdBuf, 0, 2);

        cmdBuf[2] = type;

        if (unlockTime != null) {
            System.arraycopy(unlockTime, 0, cmdBuf, 3, 24);
            Arrays.fill(cmdBuf, 27, 32, (byte) 5);
            LogUtil.d(getTag(), "cmdBuf = " + Arrays.toString(cmdBuf));
        }

        try {
            AES_ECB_PKCS7.AES256Encode(cmdBuf, buf, MessageCreator.mAK);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.arraycopy(buf, 0, cmd, 3, 32);

        short crc = StringUtil.crc16(cmd, 35);
        StringUtil.short2Bytes(crc, buf);
        System.arraycopy(buf, 0, cmd, 35, 2);

        byte[] bleCmd = new byte[37];
        System.arraycopy(cmd, 0, bleCmd, 0, 37);

        return bleCmd;
    }

}
