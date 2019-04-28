
package com.smart.lock.ble.parser;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.utils.LogUtil;

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

        //计算命令长度
        int packetLen = (cmd[1]) * 256 + ((cmd[2] < 0 ? (256 + cmd[2]) : cmd[2]) + 5);
        byte[] pdu = Arrays.copyOfRange(cmd, 3, packetLen - 2);

        byte[] buf = new byte[64];

        try {
            if (MessageCreator.mIs128Code)
                AES_ECB_PKCS7.AES128Decode(pdu, buf, MessageCreator.m128SK);
            else
                AES_ECB_PKCS7.AES256Decode(pdu, buf, MessageCreator.m256SK);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] random = new byte[16];
        System.arraycopy(buf, 0, random, 0, 16);

        try {
            if (MessageCreator.mIs128Code)
                AES_ECB_PKCS7.AES128Decode(random, random, MessageCreator.m128SK);
            else
                AES_ECB_PKCS7.AES256Decode(random, random, MessageCreator.m256SK);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] respRandom = new byte[16];
        LogUtil.d(TAG, "buf = " + Arrays.toString(buf));
        LogUtil.d(TAG, "random = " + Arrays.toString(random));
        LogUtil.d(TAG, "pwdRandom = " + Arrays.toString(MessageCreator.pwdRandom));
        if (memcmp(random, MessageCreator.pwdRandom, 16)) {

            System.arraycopy(buf, 16, respRandom, 0, 16);
            if (MessageCreator.mIs128Code) {
                if (MessageCreator.m128AK == null) {
                    MessageCreator.m128AK = new byte[16];
                }
                System.arraycopy(buf, 32, MessageCreator.m128AK, 0, 16);
            } else {
                if (MessageCreator.m256AK == null) {
                    MessageCreator.m256AK = new byte[32];
                }
                System.arraycopy(buf, 32, MessageCreator.m256AK, 0, 32);
            }


        } else
            throw new RuntimeException("Random memcmp error !");

        return MessageCreator.getCmd02Message(getParseKey(), respRandom);
    }

    @Override
    public byte getParseKey() {
        return Message.TYPE_BLE_RECEIVER_CMD_02;
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
