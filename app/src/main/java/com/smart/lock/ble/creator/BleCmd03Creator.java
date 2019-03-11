package com.smart.lock.ble.creator;

import android.os.Bundle;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.utils.StringUtil;

/**
 * apk->设备,通知智能锁进行锁体秘钥录入
 */
public class BleCmd03Creator implements BleCreator {


    private static final String TAG = BleCmd03Creator.class.getSimpleName();

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public byte[] create(Message message) {

        Bundle extra = message.getData();

        if (extra == null) throw new RuntimeException("AK is error!");

        short cmdLen = 20;
        byte[] cmd = new byte[128];


        cmd[0] = 0x3;
        byte[] buf = new byte[16];
        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        try {
            AES_ECB_PKCS7.AES256Encode(extra.getByteArray(BleMsg.KEY_RANDOM), buf, extra.getByteArray(BleMsg.KEY_AK));
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
