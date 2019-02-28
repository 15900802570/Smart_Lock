package com.smart.lock.ble.creator;

import android.os.Bundle;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;

/**
 * MSG19是APK给智能锁下发的同步查询指令
 */
public class BleCmd19Creator implements BleCreator {


    private static final String TAG = BleCmd19Creator.class.getSimpleName();

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public byte[] create(Message message) {

        Bundle extra = message.getData();

        if (extra == null) throw new RuntimeException("AK is error!");

        short cmdLen = 16;
        byte[] cmd = new byte[128];

        cmd[0] = 0x19;
        byte[] buf = new byte[16];
        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        byte[] cmdbuf = new byte[16];
        Arrays.fill(cmdbuf, 0, 16, (byte) 0);

        try {
            AES_ECB_PKCS7.AES256Encode(cmdbuf, buf, MessageCreator.mAK);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //写入F1
        System.arraycopy(buf, 0, cmd, 3, 16);

        short crc = StringUtil.crc16(cmd, 19);
        StringUtil.short2Bytes(crc, buf);
        System.arraycopy(buf, 0, cmd, 19, 2);

        byte[] bleCmd = new byte[21];
        System.arraycopy(cmd, 0, bleCmd, 0, 21);

        return bleCmd;
    }

}
