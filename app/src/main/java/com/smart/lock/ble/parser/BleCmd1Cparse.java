package com.smart.lock.ble.parser;

import com.smart.lock.ble.message.Message;

import java.util.Arrays;

/**
 * 智能锁->APK 对MSG 19 的版本信息查询
 */
public class BleCmd1Cparse implements BleCommandParse {
    @Override
    public Message parse(byte[] cmd) {
        //计算指令长度
        int packetLen = (cmd[1]*256+(cmd[2]+5));
        byte[] pdu = Arrays.copyOfRange(cmd,3,packetLen-2);

        return null;
    }

    @Override
    public byte getParseKey() {
        return 0;
    }

    @Override
    public String getTag() {
        return this.getClass().getName();
    }
}
