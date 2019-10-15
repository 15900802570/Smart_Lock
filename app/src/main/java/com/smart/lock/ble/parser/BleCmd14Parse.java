
package com.smart.lock.ble.parser;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;

/**
 * 是智能锁对MSG14消息的响应
 */
public class BleCmd14Parse implements BleCommandParse {

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

        if (MessageCreator.mIs128Code)
            AES_ECB_PKCS7.AES128Decode(pdu, buf, MessageCreator.m128AK);
        else
            AES_ECB_PKCS7.AES256Decode(pdu, buf, MessageCreator.m256AK);
        byte[] authCode = new byte[30];
        System.arraycopy(buf, 0, authCode, 0, 30);
        LogUtil.d(getTag(), "TEST >>> receiver 14 :" + StringUtil.bytesToHexString(cmd));


        return MessageCreator.getCmd14Message(getParseKey(), authCode);
    }

    @Override
    public byte getParseKey() {
        return Message.TYPE_BLE_RECEIVER_CMD_14;
    }

}
