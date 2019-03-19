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
 * APK配置智能锁临时用户的生命周期，不配置则生命周期一直存在，同普通用户，通过MSG2E返回结果
 */
public class BleCmd29Creator implements BleCreator {


    private static final String TAG = BleCmd29Creator.class.getSimpleName();

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public byte[] create(Message message) {

        Bundle data = message.getData();

        short userId = data.getShort(BleMsg.KEY_USER_ID);
        byte[] lifeCycle = data.getByteArray(BleMsg.KEY_LIFE_CYCLE);

        short cmdLen = 16;
        byte[] cmd = new byte[128];

        cmd[0] = 0x29;
        byte[] buf = new byte[16];
        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        byte[] cmdBuf = new byte[16];
        byte[] userIdBuf = new byte[2];

        StringUtil.short2Bytes(userId, userIdBuf);
        System.arraycopy(userIdBuf, 0, cmdBuf, 0, 2);

        if (lifeCycle != null) {
            System.arraycopy(lifeCycle, 0, cmdBuf, 2, 8);
            Arrays.fill(cmdBuf, 10, 16, (byte) 6);
            LogUtil.d(getTag(), "cmdBuf = " + Arrays.toString(cmdBuf));
        }

        try {
            AES_ECB_PKCS7.AES256Encode(cmdBuf, buf, MessageCreator.mAK);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.arraycopy(buf, 0, cmd, 3, 16);

        short crc = StringUtil.crc16(cmd, 19);
        StringUtil.short2Bytes(crc, buf);
        System.arraycopy(buf, 0, cmd, 19, 2);

        byte[] bleCmd = new byte[21];
        System.arraycopy(cmd, 0, bleCmd, 0, 21);

        return bleCmd;
    }

}
