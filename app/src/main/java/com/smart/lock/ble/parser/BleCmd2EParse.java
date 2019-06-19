
package com.smart.lock.ble.parser;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;

/**
 * MSG 2E是智能锁在远程开锁命令后的回应。
 */
public class BleCmd2EParse implements BleCommandParse {

    private static final String TAG = BleCmd2EParse.class.getSimpleName();

    @Override
    public String getTag() {
        return this.getClass().getName();
    }

    @Override
    public Message parse(byte[] cmd) {
        LogUtil.d(TAG,"TEST >>> receiver 2E :" + StringUtil.bytesToHexString(cmd));
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
        LogUtil.d(TAG, "buf = " + Arrays.toString(buf));
        byte[] errCode = Arrays.copyOfRange(buf, 0, 4);

        LogUtil.d(TAG, "errCode = " + Arrays.toString(errCode));

        return MessageCreator.getCmd2EMessage(getParseKey(), errCode);
    }

    @Override
    public byte getParseKey() {
        return Message.TYPE_BLE_RECEIVER_CMD_2E;
    }

}
