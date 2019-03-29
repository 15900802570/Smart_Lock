package com.smart.lock.ble.creator;

import android.os.Bundle;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;

/**
 * MSG 31是APK从智能锁查询开锁日志，根据TYPE查询不同日志，智能锁将所查询日志分条发出，无需APK响应。
 */
public class BleCmd31Creator implements BleCreator {


    private static final String TAG = BleCmd31Creator.class.getSimpleName();

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public byte[] create(Message message) {

        Bundle extra = message.getData();

        if (extra == null) throw new RuntimeException("AK is error!");

        byte cmdType = extra.getByte(BleMsg.KEY_CMD_TYPE);
        short userId = extra.getShort(BleMsg.KEY_USER_ID);

        short cmdLen = 16;
        byte[] cmd = new byte[128];

        cmd[0] = 0x31;
        byte[] buf = new byte[16];
        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        byte[] cmdBuf = new byte[16];
        byte[] userIdBuf = new byte[2];

        StringUtil.short2Bytes(userId, userIdBuf);
        System.arraycopy(userIdBuf, 0, cmdBuf, 0, 2);

        cmdBuf[3] = cmdType;

        Arrays.fill(cmdBuf, 3, 16, (byte) 13);

        try {
            if (MessageCreator.mIs128Code)
                AES_ECB_PKCS7.AES128Encode(cmdBuf, buf, MessageCreator.m128AK);
            else
                AES_ECB_PKCS7.AES256Encode(cmdBuf, buf, MessageCreator.m256AK);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //写入F1
        System.arraycopy(buf, 0, cmd, 3, 16);

//        mCrc = crc16(cmd, 19);
//        Log.d(TAG, "mCrc = " + mCrc);
//        short2Bytes(mCrc, buf);
//        System.arraycopy(buf, 0, cmd, 19, 2);


        short crc = StringUtil.crc16(cmd, 19);
        StringUtil.short2Bytes(crc, buf);
        System.arraycopy(buf, 0, cmd, 19, 2);

        byte[] bleCmd = new byte[21];
        System.arraycopy(cmd, 0, bleCmd, 0, 21);

        return bleCmd;
    }

}
