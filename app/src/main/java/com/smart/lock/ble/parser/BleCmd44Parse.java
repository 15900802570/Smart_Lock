package com.smart.lock.ble.parser;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.utils.LogUtil;

import java.util.Arrays;

/**
 * 智能锁使用MSG 44 人脸OTA命令响应；
 */
public class BleCmd44Parse implements BleCommandParse {
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

        int majorLen = Integer.parseInt(String.valueOf(buf[0]), 10);
        int nCpuLen = Integer.parseInt(String.valueOf(buf[1 + majorLen]), 10);
        int sCpuLen = Integer.parseInt(String.valueOf(buf[2 + majorLen + nCpuLen]), 10);
        int moduleLen = Integer.parseInt(String.valueOf(buf[3 + majorLen + nCpuLen + sCpuLen]), 10);
        byte[] majorVer = new byte[majorLen];
        byte[] nCpuVer = new byte[nCpuLen];
        byte[] sCpuVer = new byte[sCpuLen];
        byte[] moduleVer = new byte[moduleLen];
        System.arraycopy(buf, 1, majorVer, 0, majorLen);
        System.arraycopy(buf, 2 + majorLen, nCpuVer, 0, nCpuLen);
        System.arraycopy(buf, 3 + majorLen + nCpuLen, sCpuVer, 0, sCpuLen);
        System.arraycopy(buf, 4 + majorLen + nCpuLen + sCpuLen, moduleVer, 0, moduleLen);


//        System.arraycopy(buf, 0, moduleType, 0, 1);
//        System.arraycopy(buf, 2, swVer, 0, swLen);
        LogUtil.d("CheckOta","receive 44");

        return MessageCreator.getCmd44Message(getParseKey(), majorVer, nCpuVer,sCpuVer,moduleVer);
    }

    @Override
    public byte getParseKey() {
        return Message.TYPE_BLE_RECEIVER_CMD_44;
    }
}
