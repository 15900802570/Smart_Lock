package com.smart.lock.ble.creator;

import android.os.Bundle;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;

/**
 * MSG 13用来删除特定用户群
 */
public class BleCmd13Creator implements BleCreator {


    private static final String TAG = BleCmd13Creator.class.getSimpleName();

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public byte[] create(Message message) {

        Bundle extra = message.getData();

        if (extra == null) throw new RuntimeException("AK is error!");

        byte type = extra.getByte(BleMsg.KEY_CMD_TYPE);

        short cmdLen = 16;
        byte[] cmd = new byte[128];

        cmd[0] = 0x13;
        byte[] buf = new byte[32];

        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        byte[] cmdBuf = new byte[16];
        byte[] userIdBuf = new byte[2];

        cmdBuf[0] = type;

        Arrays.fill(cmdBuf, 1, 16, (byte) 15);

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
