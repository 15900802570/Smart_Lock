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

        Bundle data = message.getData();

        byte cmdType = data.getByte(BleMsg.KEY_CMD_TYPE); //0x00：新增 0x01：删除 0x02：修改

        byte keyType = data.getByte(BleMsg.KEY_TYPE);  //0x00：密码 0x01：指纹 0x02：NFC卡

        short userId = data.getShort(BleMsg.KEY_USER_ID);

        byte lockId = data.getByte(BleMsg.KEY_LOCK_ID);

        int pwd = data.getInt(BleMsg.KEY_PWD);

        short cmdLen = 16;
        byte[] cmd = new byte[128];

        cmd[0] = 0x15;
        byte[] buf = new byte[16];
        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        byte[] cmdBuf = new byte[16];
        byte[] userIdBuf = new byte[2];

        cmdBuf[0] = cmdType;
        cmdBuf[1] = keyType;

        StringUtil.short2Bytes(userId, userIdBuf);
        System.arraycopy(userIdBuf, 0, cmdBuf, 2, 2);

        cmdBuf[4] = lockId;

        byte[] pwdBuf = getPsw(cmdType, keyType, pwd);

        LogUtil.d(getTag(), "cmdBuf = " + Arrays.toString(cmdBuf));
        LogUtil.d(getTag(), "cmdType = " + cmdType);
        LogUtil.d(getTag(), "keyType = " + keyType);
        LogUtil.d(getTag(), "userIdBuf = " + Arrays.toString(userIdBuf));
        LogUtil.d(getTag(), "lockId = " + lockId);

        if (pwdBuf != null) {
            System.arraycopy(pwdBuf, 0, cmdBuf, 5, 6);
            Arrays.fill(cmdBuf, 11, 16, (byte) 5);
            LogUtil.d(getTag(), "pwdBuf = " + Arrays.toString(pwdBuf));
        } else {
            Arrays.fill(cmdBuf, 5, 16, (byte) 11);
            LogUtil.d(getTag(), "pwdBuf = " + Arrays.toString(pwdBuf));
        }

        try {
            AES_ECB_PKCS7.AES256Encode(cmdBuf, buf, MessageCreator.mAK);
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
     * @param cmdType 命令类型
     * @param keyType 秘钥类型
     * @param pwd     密码
     * @return
     */
    private byte[] getPsw(int cmdType, int keyType, int pwd) {
        byte[] psw = null;
        if ((cmdType == 0 || cmdType == 2) && keyType == 0) {
            if (String.valueOf(pwd).getBytes().length == 6) {
                psw = String.valueOf(pwd).getBytes();
            } else if (String.valueOf(pwd).getBytes().length == 1){
                psw = new byte[6];
                Arrays.fill(psw, 0, 6, (byte) 0x0);
            }
            return psw;
        }

        return psw;
    }

}
