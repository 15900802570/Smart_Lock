
package com.smart.lock.ble.creator;

import android.os.Bundle;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;

public class BleCmd41Creator implements BleCreator {
    private static final String TAG = BleCmd41Creator.class.getSimpleName();

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public byte[] create(Message message) {

        Bundle data = message.getData();

        byte cmdType = data.getByte(BleMsg.KEY_CMD_TYPE); //0x00：新增 0x01：删除 0x02：修改

        byte OTAType = data.getByte(BleMsg.KEY_FACE_OTA_MODULE);

        int size = data.getInt(BleMsg.KEY_FACE_FIRMWARE_SIZE);

        short cmdLen = 16;
        byte[] cmd = new byte[128];

        cmd[0] = 0x41;
        byte[] buf = new byte[16];
        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        byte[] cmdBuf = new byte[16];

        cmdBuf[0] = cmdType;
        cmdBuf[1] = OTAType;

        LogUtil.d(TAG, "cmd=" + cmdType + '\n' +
                "ota =" + OTAType + '\n' +
                "size =" + size);

        if (size != 0) {
            byte[] sizeBuf = new byte[4];
            StringUtil.int2Bytes(size, sizeBuf);
            System.arraycopy(sizeBuf, 0, cmdBuf, 2, 4);
//            Arrays.fill(cmdBuf, 2, 6, (byte) size);
        } else {
            Arrays.fill(cmdBuf, 2, 6, (byte) 0xffffffff);
        }


        Arrays.fill(cmdBuf, 7, 16, (byte) 0x0A);

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
