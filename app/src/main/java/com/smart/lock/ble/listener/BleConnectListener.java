package com.smart.lock.ble.listener;

import android.os.Bundle;
import android.util.Log;

import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.UIReceiver;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.provider.BleProvider;
import com.smart.lock.ble.provider.TimerProvider;
import com.smart.lock.utils.LogUtil;

public class BleConnectListener implements BleMessageListener {

    private static final String TAG = BleConnectListener.class.getSimpleName();
    private UIReceiver mNotify;

    public BleConnectListener() {

    }

    @Override
    public void onReceive(Message message, TimerProvider timer) {

        try {
            int type = message.getType();

            Bundle extra = message.getData();

            LogUtil.i(TAG, "onReceive Message type : " + Message.getMessageTypeTag(type));

            if (type == Message.TYPE_BLE_RECEIVER_CMD_02) {

                final byte[] random = extra.getByteArray(BleMsg.KEY_RANDOM);
                if (random != null && random.length != 0) {
//                    sendCmd03(random, provider);
                } else {
                    Log.w(TAG, "random is null!");
                }

            } else if (type == Message.TYPE_BLE_RECEIVER_CMD_04) {
                mNotify.onChanged(extra);
            } else {
                Log.w(TAG, "Message type : " + type + " can not be handler");
                return;
            }

        } finally {
            message.recycle();
        }

    }

    /**
     * MSG 03
     */
    public boolean sendCmd03(byte[] random, BleProvider provider) {
        Message msg = Message.obtain();
        msg.setType(Message.TYPE_BLE_SEND_CMD_03);

        Bundle bundle = msg.getData();

        if (random != null && random.length != 0) {
            bundle.putByteArray(BleMsg.KEY_RANDOM, random);
        }

        return provider.send(msg);
    }

    @Override
    public String getListenerKey() {
        return TAG;
    }

    @Override
    public void halt() {

    }

    /**
     * @param noty
     */
    public void registerReceiver(UIReceiver noty) {
        // TODO Auto-generated method stub
        mNotify = noty;
    }

    public void unRegisterReceiver() {
        // TODO Auto-generated method stub
        mNotify = null;
    }

}
