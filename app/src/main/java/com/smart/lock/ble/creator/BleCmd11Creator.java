package com.smart.lock.ble.creator;

import android.os.Bundle;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;

/**
 * apk->设备,是APK发给智能锁的新增用户消息，如果是在扫描绑定时发送的，智能锁需提示用户按键确认才能回复，其他情况可直接创建新用户后回复。
 */
public class BleCmd11Creator implements BleCreator {


    private static final String TAG = BleCmd11Creator.class.getSimpleName();

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public byte[] create(Message message) {

        Bundle extra = message.getData();

        if (extra == null) throw new RuntimeException("AK is error!");

        int type = extra.getInt(BleMsg.KEY_CMD_TYPE);
        int userId = extra.getInt(BleMsg.KEY_USER_ID);

        short cmdLen = 32;
        byte[] cmd = new byte[128];

        cmd[0] = 0x11;
        byte[] buf = new byte[32];

        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        byte[] cmdBuf = new byte[16];
        byte[] typeBuf = new byte[4];
        byte[] userIdBuf = new byte[4];

        StringUtil.int2Bytes(type, typeBuf);
        System.arraycopy(typeBuf, 0, cmdBuf, 0, 1);

        StringUtil.int2Bytes(userId, userIdBuf);
        System.arraycopy(userIdBuf, 0, cmdBuf, 1, 2);

        Arrays.fill(cmdBuf, 3, 16, (byte) 13);

        try {
            AES_ECB_PKCS7.AES256Encode(cmdBuf, buf, MessageCreator.mAK);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.arraycopy(buf, 0, cmd, 3, 16);

        short crc =  StringUtil.crc16(cmd, 19);
        StringUtil.short2Bytes(crc, buf);
        System.arraycopy(buf, 0, cmd, 19, 2);

        byte[] bleCmd = new byte[21];
        System.arraycopy(cmd, 0, bleCmd, 0, 21);

        return bleCmd;
    }

}
