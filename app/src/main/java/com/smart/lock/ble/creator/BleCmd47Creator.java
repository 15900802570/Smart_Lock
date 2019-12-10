
package com.smart.lock.ble.creator;

import android.os.Bundle;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;

public class BleCmd47Creator implements BleCreator {
    private static final String TAG = BleCmd47Creator.class.getSimpleName();

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public byte[] create(Message message) {

        Bundle data = message.getData();

        int timeZone = data.getInt(BleMsg.KEY_TIME_ZONE); // 当前时区

        short cmdLen = 16;
        byte[] cmd = new byte[128];

        cmd[0] = 0x47;
        byte[] buf = new byte[16];
        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        // 填充字节
        byte[] cmdBuf = new byte[16];
        byte[] timeZoneBuf = new byte[4];

        LogUtil.d(getTag(), "cmdBuf = " + Arrays.toString(cmdBuf));
        LogUtil.d(getTag(), "timeZone = " + timeZone);
        StringUtil.int2Bytes(timeZone, timeZoneBuf);
        System.arraycopy(timeZoneBuf, 0, cmdBuf, 0, 4);

        Arrays.fill(cmdBuf, 4, 16, (byte) 0x0C);

        try {
            if (MessageCreator.mIs128Code)
                AES_ECB_PKCS7.AES128Encode(cmdBuf, buf, MessageCreator.m128AK);
            else
                AES_ECB_PKCS7.AES256Encode(cmdBuf, buf, MessageCreator.m256AK);
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
