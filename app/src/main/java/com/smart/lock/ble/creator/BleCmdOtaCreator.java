package com.smart.lock.ble.creator;

import android.os.Bundle;
import android.util.Log;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;

/**
 * MSG1B是APK给智能锁下发的进入OTA模式命令
 */
public class BleCmdOtaCreator implements BleCreator {


    private static final String TAG = BleCmdOtaCreator.class.getSimpleName();

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public byte[] create(Message message) {

        Bundle extra = message.getData();

        if (extra == null) throw new RuntimeException("AK is error!");

        short cmdLen = 16;
        byte[] cmd = new byte[128];

        cmd[0] = 0x1B;
        byte[] buf = new byte[16];
        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        byte[] nodeId = new byte[16];
        //写入nodeid
        System.arraycopy(extra.getByteArray(BleMsg.KEY_NODE_ID), 0, nodeId, 0, 8);
        //写入0
        Arrays.fill(nodeId, 8, 16, (byte) 8);

        try {
            AES_ECB_PKCS7.AES256Encode(nodeId, buf, extra.getByteArray(BleMsg.KEY_AK));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //写入F1
        System.arraycopy(buf, 0, cmd, 3, 16);

        short crc = StringUtil.crc16(cmd, 19);
        StringUtil.short2Bytes(crc, buf);
        System.arraycopy(buf, 0, cmd, 19, 2);

        byte[] bleCmd = new byte[21];
        System.arraycopy(cmd, 0, bleCmd, 0, 21);


        return bleCmd;
    }

    private byte[] String2BytesArray(String strDigit, int radix) {
        int len = strDigit.length();
        int size = 0;
        boolean makeup = false;
        if (len % 2 == 0)
            size = len / 2;
        else {
            size = len / 2 + 1;
            makeup = true;
        }
        int iByte = 0;

        byte bArray[] = new byte[size];

        Log.d(TAG, "String2BytesArray size= " + size);
        Log.d(TAG, "String2BytesArray strData= " + strDigit);
        for (int j = 0; j < len; j++) {
            Log.d(TAG, "String2BytesArray j= " + j + " makeup=" + makeup);
            String strByte;
            if (j == 0 && makeup)
                strByte = "0" + strDigit.charAt(0);
            else {
                strByte = strDigit.substring(j, j + 2);
                j++;
            }
            Log.d(TAG, "String2BytesArray strByte= " + strByte);
            bArray[iByte] = (byte) Integer.parseInt(strByte, radix);
            iByte++;
        }

        return bArray;
    }

}
