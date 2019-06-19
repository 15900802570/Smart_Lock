
package com.smart.lock.ble.parser;

import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;

import java.util.Arrays;

/**
 * MSG 62。
 */
public class BleCmd62Parse implements BleCommandParse {

    @Override
    public String getTag() {
        return this.getClass().getName();
    }

    @Override
    public Message parse(byte[] cmd) {
        //计算命令长度
        int packetLen = (cmd[1]) * 256 + ((cmd[2] < 0 ? (256 + cmd[2]) : cmd[2]) + 5);
        byte[] pdu = Arrays.copyOfRange(cmd, 3, packetLen - 2);

        return MessageCreator.getCmd62Message(getParseKey(), pdu);
    }

    @Override
    public byte getParseKey() {
        return Message.TYPE_BLE_RECEIVER_CMD_62;
    }

}
