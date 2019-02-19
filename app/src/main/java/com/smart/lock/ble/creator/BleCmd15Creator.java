package com.smart.lock.ble.creator;

import android.os.Bundle;
import android.util.Log;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;

/**
 * apk->设备,通知智能锁进行锁体秘钥录入
 */
public class BleCmd15Creator implements BleCreator {


    private static final String TAG = BleCmd15Creator.class.getSimpleName();

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public byte[] create(Message message) {

        int mType = message.getType();
        Bundle data = message.getData();

        byte[] keyType = data.getByteArray(BleMsg.KEY_CMD_TYPE);

        byte[] key = message.getAK();

        short cmdLen = 16;
        byte[] cmd = new byte[128];

        cmd[0] = 0x15;
        byte[] buf = new byte[16];
        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);
        //写入Type
        byte[] typeBuf = new byte[16];
        System.arraycopy(keyType, 0, typeBuf, 0, 1);
        Arrays.fill(typeBuf, 1, 16, (byte) 15);

        try {
            AES_ECB_PKCS7.AES256Encode(typeBuf, buf, key);
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

        Log.d(TAG, "cmd = " + Arrays.toString(bleCmd));
        return bleCmd;
    }

}
