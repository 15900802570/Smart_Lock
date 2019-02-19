package com.smart.lock.ble.creator;

import android.os.Bundle;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;

/**
 * MSG 21是APK/GW给智能锁下发的远程开锁消息。
 */
public class BleCmd21Creator implements BleCreator {


    private static final String TAG = BleCmd21Creator.class.getSimpleName();

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

        cmd[0] = 0x21;
        byte[] buf = new byte[16];
        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        byte[] nodeId = new byte[16];
        //写入nodeid
        System.arraycopy(extra.getByteArray(BleMsg.KEY_NODE_ID), 0, nodeId, 0, 8);
        //写入0
        Arrays.fill(nodeId, 8, 16, (byte) 8);

        try {
            AES_ECB_PKCS7.AES256Encode(nodeId, buf, extra.getByteArray(BleMsg.KEY_AK));
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
