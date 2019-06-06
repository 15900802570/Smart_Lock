package com.smart.lock.ble.parser;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.utils.LogUtil;

import java.util.Arrays;

/**
 * 智能锁->APK 对MSG 19 的版本信息查询
 */
public class BleCmd1CParse implements BleCommandParse {
    private final String TAG = this.getClass().getName();

    @Override
    public Message parse(byte[] cmd) {
        //计算指令长度
        int packetLen = (cmd[1]) * 256 + ((cmd[2] < 0 ? (256 + cmd[2]) : cmd[2]) + 5);
        byte[] pdu = Arrays.copyOfRange(cmd, 3, packetLen - 2);

        byte[] buf = new byte[96];
        try {
            if (MessageCreator.mIs128Code)
                AES_ECB_PKCS7.AES128Decode(pdu, buf, MessageCreator.m128AK);
            else
                AES_ECB_PKCS7.AES256Decode(pdu, buf, MessageCreator.m256AK);
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogUtil.d(TAG, Arrays.toString(buf));
        byte[] sn = new byte[18];
        int swLen = Integer.parseInt(String.valueOf(buf[18]), 10);
        int hwLen = Integer.parseInt(String.valueOf(buf[19]), 10);
        byte[] swVer = new byte[swLen];
        System.arraycopy(buf, 0, sn, 0, 18);
        System.arraycopy(buf, 20, swVer, 0, swLen);
        byte[] hwVer = new byte[hwLen];
        LogUtil.d(TAG, "swLen = " + swLen + '\n' +
                "hw_len = " + hwLen);
        if (hwLen == 0) {
            hwVer = null;
        } else {
            System.arraycopy(buf, 20 + swLen, hwVer, 0, hwLen);
        }
        LogUtil.d(TAG, "buf = " + Arrays.toString(buf));

        return MessageCreator.getCmd1CMessage(getParseKey(), sn, swVer, hwVer);
    }

    @Override
    public byte getParseKey() {
        return Message.TYPE_BLE_RECEIVER_CMD_1C;
    }

    @Override
    public String getTag() {
        return TAG;
    }
}
