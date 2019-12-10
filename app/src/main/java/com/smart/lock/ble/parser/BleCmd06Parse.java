
package com.smart.lock.ble.parser;


import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;

import java.util.Arrays;

import static com.smart.lock.ble.message.MessageCreator.mIs128Code;

/**
 * MSG 06是智能锁在配置基本信息过程中给服务器上报的消息。
 */
public class BleCmd06Parse implements BleCommandParse {

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
            if (mIs128Code)
                AES_ECB_PKCS7.AES128Decode(pdu, buf, MessageCreator.m128AK);
            else
                AES_ECB_PKCS7.AES256Decode(pdu, buf, MessageCreator.m256AK);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] minPwdLen =  Arrays.copyOfRange(buf, 0, 1);
        byte[] maxPwdLen = Arrays.copyOfRange(buf, 1, 2);

        return MessageCreator.getCmd06Message(getParseKey(), minPwdLen[0], maxPwdLen[0]);
    }

    @Override
    public byte getParseKey() {
        return Message.TYPE_BLE_RECEIVER_CMD_06;
    }

}
