
package com.smart.lock.ble.parser;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;

import java.util.Arrays;

/**
 * MSG 0E是智能锁在配置基本信息过程中给服务器上报的消息。
 */
public class BleCmd0EParse implements BleCommandParse {

    @Override
    public String getTag() {
        return this.getClass().getName();
    }

    @Override
    public Message parse(byte[] cmd) {
        //计算命令长度
        int packetLen = (cmd[1]) * 256 + ((cmd[2] < 0 ? (256 + cmd[2]) : cmd[2]) + 5);
        byte[] pdu = Arrays.copyOfRange(cmd, 3, packetLen - 2);

        byte[] ik = Arrays.copyOfRange(cmd, packetLen - 2, packetLen);

//        if (mCrc != byte2short(ik)) {
//            Log.d(TAG, "ik = " + ik);
//            notifyData(BleMsg.STR_RSP_IK_ERR);
//        }

        byte[] buf = new byte[16];
        try {
            if (MessageCreator.mIs128Code)
                AES_ECB_PKCS7.AES128Decode(pdu, buf, MessageCreator.m128AK);
            else
                AES_ECB_PKCS7.AES256Decode(pdu, buf, MessageCreator.m256AK);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] errCode = Arrays.copyOfRange(pdu, 0, 4);

        return MessageCreator.getCmd0EMessage(getParseKey(), errCode);
    }

    @Override
    public byte getParseKey() {
        return Message.TYPE_BLE_RECEIVER_CMD_0E;
    }

}
