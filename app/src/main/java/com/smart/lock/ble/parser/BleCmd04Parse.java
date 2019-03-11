
package com.smart.lock.ble.parser;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;

import java.util.Arrays;

/**
 * 是智能锁对MSG01消息的响应
 */
public class BleCmd04Parse implements BleCommandParse {

    @Override
    public String getTag() {
        return this.getClass().getName();
    }

    @Override
    public Message parse(byte[] cmd) {

        //计算命令长度
        int packetLen = (cmd[1]) * 256 + (cmd[2] + 5);
        byte[] pdu = Arrays.copyOfRange(cmd, 3, packetLen - 2);

        byte[] buf = new byte[160];

        try {
            AES_ECB_PKCS7.AES256Decode(pdu, buf, MessageCreator.mAK);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] batPerscent = new byte[1];
        byte[] syncUsers = new byte[16];
        byte[] userStatus = new byte[1];
        byte[] stStatus = new byte[1];
        byte[] unLockTime = new byte[1];

        byte[] tmpPwdSk = new byte[32 * 4];

        System.arraycopy(buf, 0, batPerscent, 0, 1);
        System.arraycopy(buf, 1, syncUsers, 0, 16);
        System.arraycopy(buf, 17, userStatus, 0, 1);
        System.arraycopy(buf, 18, stStatus, 0, 1);
        System.arraycopy(buf, 19, unLockTime, 0, 1);
        System.arraycopy(buf, 20, tmpPwdSk, 0, 128);

        return MessageCreator.getCmd04Message(getParseKey(), batPerscent[0], syncUsers, userStatus[0], stStatus[0], unLockTime[0], tmpPwdSk);
    }

    @Override
    public byte getParseKey() {
        return Message.TYPE_BLE_RECEV_CMD_04;
    }

    /**
     * 比较两个byte数组数据是否相同,相同返回 true
     *
     * @param data1
     * @param data2
     * @param len
     * @return
     */
    public static boolean memcmp(byte[] data1, byte[] data2, int len) {
        if (data1 == null && data2 == null) {
            return true;
        }
        if (data1 == null || data2 == null) {
            return false;
        }
        if (data1 == data2) {
            return true;
        }

        boolean bEquals = true;
        int i;
        for (i = 0; i < data1.length && i < data2.length && i < len; i++) {
            if (data1[i] != data2[i]) {
                bEquals = false;
                break;
            }
        }

        return bEquals;
    }

}
