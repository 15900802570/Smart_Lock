package com.smart.lock.ble.creator;

import android.os.Bundle;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;

public class BleCmd45Creator implements BleCreator {
    private static final String TAG = BleCmd45Creator.class.getSimpleName();

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public byte[] create(Message message) {

        Bundle data = message.getData();

        byte cmdType = data.getByte(BleMsg.KEY_CMD_TYPE); //0x00：新增 0x01：删除 0x02：修改


        short userId = data.getShort(BleMsg.KEY_USER_ID);

        byte lockId = data.getByte(BleMsg.KEY_LOCK_ID);

        byte pwdLen = data.getByte(BleMsg.KEY_PWD_LEN);

        String pwd = data.getString(BleMsg.KEY_PWD);

        short cmdLen = 16;
        byte[] cmd = new byte[128];

        cmd[0] = 0x45;
        byte[] buf = new byte[16];
        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        byte[] cmdBuf = new byte[16];
        byte[] userIdBuf = new byte[2];

        cmdBuf[0] = cmdType;

        StringUtil.short2Bytes(userId, userIdBuf);
        System.arraycopy(userIdBuf, 0, cmdBuf, 1, 2);

        cmdBuf[3] = lockId;
        cmdBuf[4] = pwdLen;

        byte[] pwdBuf = getPsw(pwdLen, pwd);

        LogUtil.d(getTag(), "cmdBuf = " + Arrays.toString(cmdBuf));
        LogUtil.d(getTag(), "cmdType = " + cmdType);
        LogUtil.d(getTag(), "userIdBuf = " + Arrays.toString(userIdBuf));
        LogUtil.d(getTag(), "lockId = " + lockId);

        if (pwdBuf != null) {
            System.arraycopy(pwdBuf, 0, cmdBuf, 5, (int) pwdLen);
            Arrays.fill(cmdBuf, 5 + (int) pwdLen, 16, (byte) (11 - pwdLen));
            LogUtil.d(getTag(), "pwdBuf = " + Arrays.toString(pwdBuf));
        } else {
            Arrays.fill(cmdBuf, 5, 16, (byte) 11);
            LogUtil.d(getTag(), "pwdBuf = " + Arrays.toString(pwdBuf));
        }

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

    /**
     * 获取密码
     *
     * @param pwdLen 密码长度
     * @param pwd    密码
     * @return
     */
    private byte[] getPsw(int pwdLen, String pwd) {
        byte[] psw = null;

        if (pwdLen >= 6 && pwdLen <= 10 && pwd.getBytes().length == pwdLen) {
            psw = pwd.getBytes();
        }
        return psw;
    }

}
