
package com.smart.lock.ble.parser;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;

import java.util.Arrays;

/**
 * MSG 18是智能锁录入前提供给APK的超时时间。
 */
public class BleCmd18Parse implements BleCommandParse {

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

        byte[] timeOut = Arrays.copyOfRange(buf, 0, 1);

//        byte[] count = Arrays.copyOfRange(buf, 1, 2);
//        Log.d(getTag(), "count = " + count);

        return MessageCreator.getCmd18Message(getParseKey(), timeOut);
    }

    @Override
    public byte getParseKey() {
        return Message.TYPE_BLE_RECEIVER_CMD_18;
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
