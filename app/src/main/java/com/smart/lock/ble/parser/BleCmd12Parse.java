
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
        int packetLen = (cmd[1]) * 256 + ((cmd[2] < 0 ? (256 + cmd[2]) : cmd[2]) + 5);
        byte[] pdu = Arrays.copyOfRange(cmd, 3, packetLen - 2);

        byte[] buf = new byte[64];

        if (MessageCreator.mIs128Code)
            AES_ECB_PKCS7.AES128Decode(pdu, buf, MessageCreator.m128AK);
        else
            AES_ECB_PKCS7.AES256Decode(pdu, buf, MessageCreator.m256AK);

        byte[] userId = new byte[2];
        byte[] nodeId = new byte[8];
        byte[] bleMac = new byte[6];
        byte[] randCode = new byte[10];
        byte[] time = new byte[4];

        System.arraycopy(buf, 0, userId, 0, 2);
        System.arraycopy(buf, 2, nodeId, 0, 8);
        System.arraycopy(buf, 10, bleMac, 0, 6);
        System.arraycopy(buf, 16, randCode, 0, 10);
        System.arraycopy(buf, 26, time, 0, 4);

        return MessageCreator.getCmd12Message(getParseKey(), userId, nodeId, bleMac, randCode, time);
    }

    @Override
    public byte getParseKey() {
        return Message.TYPE_BLE_RECEIVER_CMD_12;
    }

}
