package com.smart.lock.ble.creator;

import android.os.Bundle;

import com.smart.lock.ble.message.Message;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;

/**
 * MSG51 测试NFC阈值
 */
public class BleCmd61Creator implements BleCreator {


    private static final String TAG = BleCmd61Creator.class.getSimpleName();

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

        cmd[0] = 0x61;
        byte[] buf = new byte[16];
        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        //写入0
        Arrays.fill(buf, 0, 16, (byte) 0);

        //写入F1
        System.arraycopy(buf, 0, cmd, 3, 16);

        short crc = StringUtil.crc16(cmd, 19);
        StringUtil.short2Bytes(crc, buf);
        System.arraycopy(buf, 0, cmd, 19, 2);

        byte[] bleCmd = new byte[21];
        System.arraycopy(cmd, 0, bleCmd, 0, 21);

        LogUtil.d(TAG, "bleCmd : " + StringUtil.bytesToHexString(bleCmd, ":"));
        return bleCmd;
    }

}
