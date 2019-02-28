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
 * MSG 33是APK从智能锁查删除日志，通过MSG3E回复结果。
 */
public class BleCmd33Creator implements BleCreator {


    private static final String TAG = BleCmd33Creator.class.getSimpleName();

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public byte[] create(Message message) {

        Bundle extra = message.getData();

        if (extra == null) throw new RuntimeException("AK is error!");

        byte type = extra.getByte(BleMsg.KEY_CMD_TYPE);
        short userId = extra.getShort(BleMsg.KEY_USER_ID);
        int logId = extra.getInt(BleMsg.KEY_LOG_ID);

        short cmdLen = 16;
        byte[] cmd = new byte[128];

        cmd[0] = 0x33;

        byte[] buf = new byte[16];
        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        byte[] cmdBuf = new byte[16];
        byte[] userIdBuf = new byte[2];
        byte[] logIdBuf = new byte[4];

        cmdBuf[0] = type;

        StringUtil.short2Bytes(userId, userIdBuf);
        System.arraycopy(userIdBuf, 0, cmdBuf, 1, 2);

        StringUtil.int2Bytes(logId, logIdBuf);
        System.arraycopy(logIdBuf, 0, cmdBuf, 3, 4);

        Arrays.fill(cmdBuf, 7, 16, (byte) 0x9);

        LogUtil.d(TAG, "cmdBuf = " + Arrays.toString(cmdBuf));

        try {
            AES_ECB_PKCS7.AES256Encode(cmdBuf, buf, MessageCreator.mAK);
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
