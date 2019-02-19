
package com.smart.lock.ble.parser;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;

import java.util.Arrays;

/**
 * 是智能锁对MSG11消息的响应
 */
public class BleCmd12Parse implements BleCommandParse {

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
            AES_ECB_PKCS7.AES256Decode(pdu, buf, MessageCreator.mAK);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] userId = new byte[2];
        byte[] nodeId = new byte[8];
        byte[] bleMac = new byte[6];
        byte[] randCode = new byte[18];
        byte[] time = new byte[4];

        System.arraycopy(buf, 0, userId, 0, 2);
        System.arraycopy(buf, 2, nodeId, 0, 8);
        System.arraycopy(buf, 10, bleMac, 0, 6);
        System.arraycopy(buf, 16, randCode, 0, 18);
        System.arraycopy(buf, 34, time, 0, 4);

        return MessageCreator.getCmd12Message(getParseKey(), userId, nodeId, bleMac, randCode, time);
    }

    @Override
    public byte getParseKey() {
        return Message.TYPE_BLE_RECEV_CMD_12;
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
