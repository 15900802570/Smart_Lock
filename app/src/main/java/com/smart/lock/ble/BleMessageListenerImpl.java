
package com.smart.lock.ble;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.provider.BleProvider;

/**
 * 根据message消息类型进行具体的业务处理
 */
public class BleMessageListenerImpl implements BleMessageListener {
    private static final String TAG = "BleMessageListenerImpl";
    private Context mContext;
    private BleProvider mATProvider;

    public BleMessageListenerImpl(Context context, BleProvider bleProvider) {
        mContext = context;
        mATProvider = bleProvider;
    }

    @Override
    public void onReceive(BleProvider provider, Message message) {
        try {
            int type = message.getType();

            Bundle extra = message.getData();

            Log.i(TAG, "onReceive Message type : "
                    + Message.getMessageTypeTag(type));

            if (type == Message.TYPE_BLE_RECEV_CMD_02) {

                notifyData(BleMsg.EXTRA_DATA_MSG_02, extra);

            } else if (type == Message.TYPE_BLE_RECEV_CMD_04) {

                notifyData(BleMsg.EXTRA_DATA_MSG_04, extra);

            } else if (type == Message.TYPE_BLE_RECEV_CMD_12) {

                notifyData(BleMsg.EXTRA_DATA_MSG_12, extra);

            } else if (type == Message.TYPE_BLE_RECEV_CMD_1A) {

                notifyData(BleMsg.STR_RSP_MSG1A_STATUS, extra);

            } else if (type == Message.TYPE_BLE_RECEV_CMD_1E) {

                notifyData(BleMsg.STR_RSP_MSG1E_ERRCODE, extra);

            } else if (type == Message.TYPE_BLE_RECEV_CMD_16) {

                notifyData(BleMsg.STR_RSP_MSG16_LOCKID, extra);

            } else if (type == Message.TYPE_BLE_RECEV_CMD_18) {

                notifyData(BleMsg.STR_RSP_MSG18_TIMEOUT, extra);

            } else if (type == Message.TYPE_BLE_RECEV_CMD_2E) {

                notifyData(BleMsg.STR_RSP_MSG2E_ERRCODE, extra);

            }else if (type == Message.TYPE_BLE_RECEV_CMD_26) {

                notifyData(BleMsg.STR_RSP_MSG26_USERINFO, extra);

            } else if (type == Message.TYPE_BLE_RECEV_CMD_32) {

                notifyData(BleMsg.STR_RSP_MSG32_LOG, extra);

            } else if (type == Message.TYPE_BLE_RECEV_CMD_3E) {

                notifyData(BleMsg.STR_RSP_MSG3E_ERROR, extra);

            } else {
                Log.w(TAG, "Message type : " + type + " can not be handler");
                return;
            }

        } finally {
            message.recycle();
        }

    }

    /**
     * @param action 广播标识
     * @param extra  bundle
     */
    public void notifyData(final String action, Bundle extra) {

        final Intent intent = new Intent(action);
        intent.putExtras(extra);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }


    @Override
    public String getListenerKey() {
        return TAG;
    }

    @Override
    public void halt() {

    }

}
