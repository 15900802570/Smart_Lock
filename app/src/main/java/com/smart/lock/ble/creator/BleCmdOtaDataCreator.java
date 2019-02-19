package com.smart.lock.ble.creator;

import android.os.Bundle;

import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;

/**
 * MSG50 是APK给智能锁下发的进入OTA模式命令
 */
public class BleCmdOtaDataCreator implements BleCreator {


    private static final String TAG = BleCmdOtaDataCreator.class.getSimpleName();

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public byte[] create(Message message) {

        Bundle extra = message.getData();

        if (extra == null) throw new RuntimeException("No data to send!");

        return extra.getByteArray(BleMsg.EXTRA_DATA_BYTE);
    }

}
