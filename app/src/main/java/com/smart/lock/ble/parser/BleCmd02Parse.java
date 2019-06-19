
package com.smart.lock.ble.parser;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;

/**
 * 是智能锁对MSG01消息的响应
 */
public class BleCmd02Parse implements BleCommandParse {

    private static final String TAG = BleCmd02Parse.class.getSimpleName();

    @Override
    public String getTag() {
        return this.getClass().getName();
    }

    @Override
    public Message parse(byte[] cmd) {
        LogUtil.d(TAG, "TEST >>> receiver 02 :" + StringUtil.bytesToHexString(cmd));
        //计算命令长度
        int packetLen = (cmd[1]) * 256 + ((cmd[2] < 0 ? (256 + cmd[2]) : cmd[2]) + 5);
        byte[] pdu = Arrays.copyOfRange(cmd, 3, packetLen - 2);

        byte[] buf = new byte[packetLen - 5];

        try {
            if (MessageCreator.mIs128Code)
                AES_ECB_PKCS7.AES128Decode(pdu, buf, MessageCreator.m128SK);
            else
                AES_ECB_PKCS7.AES256Decode(pdu, buf, MessageCreator.m256SK);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] random = new byte[16];
        byte[] revRandom = new byte[16];
        System.arraycopy(buf, 0, random, 0, 16);

        try {
            if (MessageCreator.mIs128Code)
                AES_ECB_PKCS7.AES128Decode(random, revRandom, MessageCreator.m128SK);
            else
                AES_ECB_PKCS7.AES256Decode(random, revRandom, MessageCreator.m256SK);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return MessageCreator.getCmd02Message(getParseKey(), revRandom, buf);
    }

    @Override
    public byte getParseKey() {
        return Message.TYPE_BLE_RECEIVER_CMD_02;
    }


}
