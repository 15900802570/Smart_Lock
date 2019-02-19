
package com.smart.lock.ble.parser;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;

import java.util.Arrays;

/**
 * MSG 1E是智能锁在配置基本信息过程中给服务器上报的消息。
 */
public class BleCmd1EParse implements BleCommandParse {

    @Override
    public String getTag() {
        return this.getClass().getName();
    }

    @Override
    public Message parse(byte[] cmd) {
        //计算命令长度
        int packetLen = (cmd[1]) * 256 + (cmd[2] + 5);
        byte[] pdu = Arrays.copyOfRange(cmd, 3, packetLen - 2);

        byte[] ik = Arrays.copyOfRange(cmd, packetLen - 2, packetLen);

//        if (mCrc != byte2short(ik)) {
//            Log.d(TAG, "ik = " + ik);
//            notifyData(BleMsg.STR_RSP_IK_ERR);
//        }

        byte[] buf = new byte[16];
        try {
            AES_ECB_PKCS7.AES256Decode(pdu, buf, MessageCreator.mSK);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] errCode = Arrays.copyOfRange(buf, 0, 4);

        return MessageCreator.getCmd1EMessage(getParseKey(), errCode);
    }

    @Override
    public byte getParseKey() {
        return Message.TYPE_BLE_RECEV_CMD_1E;
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
