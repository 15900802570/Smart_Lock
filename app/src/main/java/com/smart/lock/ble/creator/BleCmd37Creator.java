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
 * MSG 37是APK发给智能锁的指纹固件大小。
 */
public class BleCmd37Creator implements BleCreator {


    private static final String TAG = BleCmd37Creator.class.getSimpleName();

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public byte[] create(Message message) {

        Bundle extra = message.getData();

        if (extra == null) throw new RuntimeException("AK is error!");

        int size = extra.getInt(BleMsg.KEY_FINGERPRINT_SIZE);

        short cmdLen = 16;
        byte[] cmd = new byte[128];

        cmd[0] = 0x37;

        byte[] buf = new byte[16];
        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        byte[] sizeBuf = new byte[4];
        StringUtil.int2Bytes(size, sizeBuf);
        System.arraycopy(sizeBuf, 0, buf, 0, 4);

        Arrays.fill(buf, 4, 16, (byte) 0x0C);

        LogUtil.d(TAG, "cmdBuf = " + Arrays.toString(buf));

        try {
            if (MessageCreator.mIs128Code)
                AES_ECB_PKCS7.AES128Encode(buf, buf, MessageCreator.m128AK);
            else
                AES_ECB_PKCS7.AES256Encode(buf, buf, MessageCreator.m256AK);
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
