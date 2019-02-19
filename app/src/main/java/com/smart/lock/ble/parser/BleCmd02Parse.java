
package com.smart.lock.ble.parser;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;

import java.util.Arrays;

/**
 * 是智能锁对MSG01消息的响应
 */
public class BleCmd02Parse implements BleCommandParse {

    @Override
    public String getTag() {
        return this.getClass().getName();
    }

    @Override
    public Message parse(byte[] cmd) {

        //计算命令长度
        int packetLen = (cmd[1]) * 256 + (cmd[2] + 5);
        byte[] pdu = Arrays.copyOfRange(cmd, 3, packetLen - 2);

        byte[] buf = new byte[64];

        try {
            AES_ECB_PKCS7.AES256Decode(pdu, buf, MessageCreator.mSK);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] random = new byte[16];
        System.arraycopy(buf, 0, random, 0, 16);

        try {
            AES_ECB_PKCS7.AES256Decode(random, random, MessageCreator.mSK);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] akbuf = new byte[32];

        byte[] respRandom = new byte[16];

        if (memcmp(random, MessageCreator.pwdRandom, 16)) {

            System.arraycopy(buf, 16, respRandom, 0, 16);

            System.arraycopy(buf, 32, akbuf, 0, 32);

        } else
            throw new RuntimeException("Random memcmp error !");

        return MessageCreator.getCmd02Message(getParseKey(), akbuf, respRandom);
    }

    @Override
    public byte getParseKey() {
        return Message.TYPE_BLE_RECEV_CMD_02;
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
