package com.smart.lock.ble.parser;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;

import java.util.Arrays;

/**
 * 智能锁使用MSG 41 人脸OTA命令响应；
 */
public class BleCmd42Parse implements BleCommandParse {
    @Override
    public String getTag() {
        return this.getClass().getName();
    }

    @Override
    public Message parse(byte[] cmd) {

        //计算命令长度
        int packetLen = (cmd[1]) * 256 + ((cmd[2] < 0 ? (256 + cmd[2]) : cmd[2]) + 5);
        byte[] pdu = Arrays.copyOfRange(cmd, 3, packetLen - 2);

        byte[] buf = new byte[packetLen - 5];
        try {
            if (MessageCreator.mIs128Code)
                AES_ECB_PKCS7.AES128Decode(pdu, buf, MessageCreator.m128AK);
            else
                AES_ECB_PKCS7.AES256Decode(pdu, buf, MessageCreator.m256AK);
        } catch (Exception e) {
            e.printStackTrace();
        }


        byte[] rsp = new byte[1];
        byte[] moduleType = new byte[1];
        System.arraycopy(buf, 0, rsp, 0, 1);
        System.arraycopy(buf, 1, moduleType, 1, 2);


        return MessageCreator.getCmd42Message(getParseKey(), rsp[0], moduleType[0]);
    }

    @Override
    public byte getParseKey() {
        return Message.TYPE_BLE_RECEIVER_CMD_42;
    }
}
