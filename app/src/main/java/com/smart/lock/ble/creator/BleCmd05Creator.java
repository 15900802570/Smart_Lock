package com.smart.lock.ble.creator;

import android.os.Bundle;

import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;
import java.util.Random;

/**
 * apk->设备,通知智能锁进行锁体秘钥录入
 */
public class BleCmd05Creator implements BleCreator {


    private static final String TAG = BleCmd05Creator.class.getSimpleName();

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public byte[] create(Message message) {

        Bundle data = message.getData();

        String sn = data.getString(BleMsg.KEY_NODE_SN);
        String nodeId = data.getString(BleMsg.KEY_NODE_ID);
        String mac = data.getString(BleMsg.KEY_BLE_MAC);
        LogUtil.d(TAG, "mac = " + mac);
        byte[] snBuf = new byte[18];
        byte[] nodeIdBuf = new byte[8];
        byte[] macBuf = new byte[6];
        if (StringUtil.checkNotNull(sn) && sn.getBytes().length == 18) {
            snBuf = sn.getBytes();
            LogUtil.d(TAG, "snBuf = " + Arrays.toString(snBuf));
        }
        if (StringUtil.checkNotNull(nodeId) && nodeId.getBytes().length == 15) {
            nodeId = "0" + nodeId;
            nodeIdBuf = StringUtil.hexStringToBytes(nodeId);
            StringUtil.exchange(nodeIdBuf);
            LogUtil.d(TAG, "nodeIdBuf = " + Arrays.toString(nodeIdBuf));
        }

        if (StringUtil.checkNotNull(mac) && mac.getBytes().length == 12) {
            macBuf = StringUtil.hexStringToBytes(mac);
            LogUtil.d(TAG, "macBuf = " + Arrays.toString(macBuf));
        }

        short cmdLen = 32;

        byte[] cmd = new byte[128];

        cmd[0] = 0x05;

        byte[] buf = new byte[16];
        StringUtil.short2Bytes(cmdLen, buf);
        System.arraycopy(buf, 0, cmd, 1, 2);

        System.arraycopy(snBuf, 0, cmd, 3, 18);
        System.arraycopy(macBuf, 0, cmd, 21, 6);
        System.arraycopy(nodeIdBuf, 0, cmd, 27, 8);

        short crc = StringUtil.crc16(cmd, 35);

        StringUtil.short2Bytes(crc, buf);
        System.arraycopy(buf, 0, cmd, 35, 2);
        byte[] bleCmd = new byte[37];
        System.arraycopy(cmd, 0, bleCmd, 0, 37);
        LogUtil.d(TAG, "bleCmd = " + Arrays.toString(bleCmd));
        return bleCmd;
    }

}
