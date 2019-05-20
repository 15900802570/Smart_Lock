package com.smart.lock.ble.creator;

import android.os.Bundle;
import android.util.Log;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;
import java.util.Random;

/**
 * apk->设备,通知智能锁进行锁体秘钥录入
 */
public class BleCmd01Creator implements BleCreator {


    private static final String TAG = BleCmd01Creator.class.getSimpleName();

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public byte[] create(Message message) {

        Bundle data = message.getData();

        byte apk = 0x00;
        byte type = data.getByte(BleMsg.KEY_CMD_TYPE);
        short userId = data.getShort(BleMsg.KEY_USER_ID);
        String authCode = data.getString(BleMsg.KEY_AUTH_CODE);

        byte[] authCodeBuf = new byte[30];

        if (!authCode.equals("0")) {
            authCodeBuf = StringUtil.hexStringToBytes(authCode);
        } else Arrays.fill(authCodeBuf, 0, 30, (byte) 0x00);

        short cmdLen = 66;
        byte[] buf = new byte[48];

        byte[] cmd = new byte[128];

        cmd[0] = 0x01;

        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);
        cmd[3] = apk;
        cmd[4] = type;

        byte[] msgBuf = new byte[48];

        StringUtil.short2Bytes(userId, buf);
        System.arraycopy(buf, 0, msgBuf, 0, 2);

        long time = System.currentTimeMillis() / 1000;
        StringUtil.int2Bytes((int) time, buf);
        System.arraycopy(buf, 0, msgBuf, 2, 4);

        System.arraycopy(authCodeBuf, 0, msgBuf, 6, 30); //authCode

        Arrays.fill(msgBuf, 36, 48, (byte) 0x0c);

        try {
            if (MessageCreator.mIs128Code)
                AES_ECB_PKCS7.AES128Encode(msgBuf, buf, MessageCreator.m128SK);
            else
                AES_ECB_PKCS7.AES256Encode(msgBuf, buf, MessageCreator.m256SK);
        } catch (Exception e) {
            e.printStackTrace();
        }

        LogUtil.d(TAG, "code : " + Arrays.toString(MessageCreator.m128SK));

        System.arraycopy(buf, 0, cmd, 5, 48);

        for (int i = 0; i < MessageCreator.pwdRandom.length; i++) {
            MessageCreator.pwdRandom[i] = (byte) new Random().nextInt(10);
        }

        try {
            if (MessageCreator.mIs128Code)
                AES_ECB_PKCS7.AES128Encode(MessageCreator.pwdRandom, buf, MessageCreator.m128SK);
            else
                AES_ECB_PKCS7.AES256Encode(MessageCreator.pwdRandom, buf, MessageCreator.m256SK);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.arraycopy(buf, 0, cmd, 53, 16);

        short crc = StringUtil.crc16(cmd, 69);

        StringUtil.short2Bytes(crc, buf);
        System.arraycopy(buf, 0, cmd, 69, 2);
        byte[] bleCmd = new byte[71];
        System.arraycopy(cmd, 0, bleCmd, 0, 71);

        return bleCmd;
    }

}
