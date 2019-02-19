package com.smart.lock.ble.creator;

import android.os.Bundle;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;

/**
 * apk->设备,通知智能锁进行锁体秘钥录入
 */
public class BleCmd17Creator implements BleCreator {


    private static final String TAG = BleCmd17Creator.class.getSimpleName();

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

        cmd[0] = 0x17;
        byte[] buf = new byte[16];
        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);
        //写入LockId
        byte[] lockIdBuf = new byte[16];
        StringUtil.int2Bytes(Integer.parseInt(extra.getString(BleMsg.KEY_LOCK_ID), 10), lockIdBuf);

        System.arraycopy(lockIdBuf, 0, lockIdBuf, 0, 4);

        Arrays.fill(lockIdBuf, 4, 16, (byte) 12);

        try {
            AES_ECB_PKCS7.AES256Encode(lockIdBuf, buf, extra.getByteArray(BleMsg.KEY_AK));
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
