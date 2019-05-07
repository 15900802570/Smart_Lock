
package com.smart.lock.ble.parser;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.utils.LogUtil;

import java.util.Arrays;

import static com.smart.lock.ble.message.MessageCreator.mIs128Code;

/**
 * 是智能锁对MSG01消息的响应
 */
public class BleCmd04Parse implements BleCommandParse {

    private static final String TAG = BleCmd04Parse.class.getSimpleName();

    @Override
    public String getTag() {
        return this.getClass().getName();
    }

    @Override
    public Message parse(byte[] cmd) {

        //计算命令长度
        int packetLen = (cmd[1]) * 256 + ((cmd[2] < 0 ? (256 + cmd[2]) : cmd[2]) + 5);
        byte[] pdu = Arrays.copyOfRange(cmd, 3, packetLen - 2);

        byte[] buf = new byte[256];

        try {
            if (mIs128Code)
                AES_ECB_PKCS7.AES128Decode(pdu, buf, MessageCreator.m128AK);
            else
                AES_ECB_PKCS7.AES256Decode(pdu, buf, MessageCreator.m256AK);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] batPerscent = new byte[1];
        byte[] syncUsers = new byte[16];
        byte[] userStatus = new byte[1];
        byte[] stStatus = new byte[1];
        byte[] unLockTime = new byte[1];
        byte[] tmpPwdSk;
        byte[] userState;
        System.arraycopy(buf, 0, batPerscent, 0, 1);
        System.arraycopy(buf, 1, syncUsers, 0, 16);
        System.arraycopy(buf, 17, userStatus, 0, 1);
        System.arraycopy(buf, 18, stStatus, 0, 1);
        System.arraycopy(buf, 19, unLockTime, 0, 1);
        if (mIs128Code) {
            tmpPwdSk = new byte[16 * 4];
            userState = new byte[100];
            System.arraycopy(buf, 20, tmpPwdSk, 0, 64);
            System.arraycopy(buf, 84, userState, 0, 100);
        } else {
            tmpPwdSk = new byte[32 * 4];
            userState = new byte[100];
            System.arraycopy(buf, 20, tmpPwdSk, 0, 128);
            System.arraycopy(buf, 148, userState, 0, 100);
        }
        return MessageCreator.getCmd04Message(getParseKey(), batPerscent[0], syncUsers, userStatus[0], stStatus[0], unLockTime[0], tmpPwdSk, userState);
    }

    @Override
    public byte getParseKey() {
        return Message.TYPE_BLE_RECEIVER_CMD_04;
    }

}
