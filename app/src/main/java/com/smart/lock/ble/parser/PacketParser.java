package com.smart.lock.ble.parser;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.lang.reflect.Array;
import java.util.Arrays;

public class PacketParser implements BleCommandParse {
    private static final String TAG = PacketParser.class.getSimpleName();

    @Override
    public Message parse(byte[] cmd) {
        return null;
    }

    @Override
    public byte getParseKey() {
        return Message.TYPE_BLE_SEND_OTA_CMD;
    }

    @Override
    public String getTag() {
        return this.getClass().getName();
    }

    private int total;
    private int index = -1;
    private byte[] data;
    private int progress;

    public void set(byte[] data, byte[] sha1) {
        this.clear();
        LogUtil.d(TAG, "data : " + data.length + " sha1 : " + sha1.length);
        this.data = new byte[data.length + sha1.length];
        System.arraycopy(sha1, 0, this.data, 0, sha1.length);
        System.arraycopy(data, 0, this.data, sha1.length, data.length);

        int length = this.data.length;
        int size = 495;

        if (length % size == 0) {
            total = length / size;
        } else {
            total = (int) Math.floor(length / size + 1);
        }
    }

    public void clear() {
        this.progress = 0;
        this.total = 0;
        this.index = -1;
        this.data = null;
    }

    public boolean hasNextPacket() {
        return this.total > 0 && (this.index + 1) < this.total;
    }

    public boolean isLast() {
        return (this.index + 1) == this.total;
    }

    public int getNextPacketIndex() {
        return this.index + 1;
    }

    public byte[] getNextPacket() {
        int index = this.getNextPacketIndex();
        byte[] packet = this.getPacket(index);
        this.index = index;

        return packet;
    }

    public byte[] getPacket(int index) {

        int length = this.data.length;
        int size = 495;
        int packetSize;

        if (length > size) {
            if ((index + 1) == this.total) {
                packetSize = length - index * size;
            } else {
                packetSize = size;
            }
        } else {
            packetSize = length;
        }
        packetSize = packetSize + 5;
        byte[] packet = new byte[500];
        packet[0] = 0x35;
        short cmdLen = 495;
        byte[] buf = new byte[48];
        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, packet, 1, 2);

        System.arraycopy(this.data, index * size, packet, 3, packetSize - 5);

        short crc = StringUtil.crc16(packet, 498);
        StringUtil.short2Bytes(crc, buf);
        System.arraycopy(buf, 0, packet, 498, 2);

        LogUtil.d("ota packet ---> index : " + index + " total : " + this.total + " content : " + StringUtil.bytesToHexString(packet, ":"));
        return packet;
    }

    private byte[] getSendPacket(byte[] val) {
        byte[] mRspBuf = new byte[20];
        int mRspRead = 0;
        int bufLen = 0;
        if (val.length > 20) {

            bufLen = val.length / 20;

            if (val.length % 20 != 0)
                bufLen++;

            for (int len = 0; len < bufLen; len++) {
                mRspRead = len * 20;
                for (int i = 0; i < 20; i++) {
                    if ((mRspRead + i) >= val.length) {
                        break;
                    }
                    mRspBuf[i] = val[mRspRead + i];
                }

            }
            return mRspBuf;
        } else {
            return mRspBuf;
        }
    }

    public int crc16(byte[] packet) {

        int length = packet.length - 2;
        short[] poly = new short[]{0, (short) 0xA001};
        int crc = 0xFFFF;
        int ds;

        for (int j = 0; j < length; j++) {

            ds = packet[j];

            for (int i = 0; i < 8; i++) {
                crc = (crc >> 1) ^ poly[(crc ^ ds) & 1] & 0xFFFF;
                ds = ds >> 1;
            }
        }

        return crc;
    }

    public boolean invalidateProgress() {

        float a = this.getNextPacketIndex();
        float b = this.total;

        int progress = (int) Math.floor((a / b * 100));

        if (progress == this.progress)
            return false;

        this.progress = progress;

        return true;
    }

    public int getProgress() {
        invalidateProgress();
        return this.progress;
    }

    public int getIndex() {
        return this.index;
    }

    public int getTotal() {
        return this.total;
    }
}
