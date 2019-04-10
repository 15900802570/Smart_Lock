
package com.smart.lock.ble.listener;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.provider.BleProvider;
import com.smart.lock.utils.LogUtil;

/**
 * 根据message消息类型进行具体的业务处理
 */
public class BleMessageListenerImpl implements BleMessageListener {
    private static final String TAG = "BleMessageListenerImpl";
    private Context mContext;
    private BleProvider mBleProvider;

    public BleMessageListenerImpl(Context context, BleProvider bleProvider) {
        mContext = context;
        mBleProvider = bleProvider;
    }

    @Override
    public void onReceive(BleProvider provider, Message message) {
        try {
            int type = message.getType();

            Bundle extra = message.getData();
            Log.i(TAG, "onReceive Message type : " + Message.getMessageTypeTag(type));

            int exception = message.getException();
            if (exception == Message.EXCEPTION_TIMEOUT) {
                LogUtil.d(TAG, "msg exception : " + message.toString());
            }

            switch (type) {
                case Message.TYPE_BLE_RECEV_CMD_02:
                    notifyData(BleMsg.EXTRA_DATA_MSG_02, extra);
                    break;
                case Message.TYPE_BLE_RECEV_CMD_04:
                    notifyData(BleMsg.EXTRA_DATA_MSG_04, extra);
                    break;
                case Message.TYPE_BLE_RECEV_CMD_12:
                    notifyData(BleMsg.EXTRA_DATA_MSG_12, extra);
                    break;
                case Message.TYPE_BLE_RECEV_CMD_1A:
                    notifyData(BleMsg.STR_RSP_MSG1A_STATUS, extra);
                    break;
                case Message.TYPE_BLE_RECEV_CMD_1C:
                    notifyData(BleMsg.STR_RSP_MSG1C_VERSION, extra);
                    break;
                case Message.TYPE_BLE_RECEV_CMD_1E:
                    notifyData(BleMsg.STR_RSP_MSG1E_ERRCODE, extra);
                    break;
                case Message.TYPE_BLE_RECEV_CMD_16:
                    notifyData(BleMsg.STR_RSP_MSG16_LOCKID, extra);
                    break;
                case Message.TYPE_BLE_RECEV_CMD_18:
                    notifyData(BleMsg.STR_RSP_MSG18_TIMEOUT, extra);
                    break;
                case Message.TYPE_BLE_RECEV_CMD_2E:
                    notifyData(BleMsg.STR_RSP_MSG2E_ERRCODE, extra);
                    break;
                case Message.TYPE_BLE_RECEV_CMD_26:
                    notifyData(BleMsg.STR_RSP_MSG26_USERINFO, extra);
                    break;
                case Message.TYPE_BLE_RECEV_CMD_32:
                    notifyData(BleMsg.STR_RSP_MSG32_LOG, extra);
                    break;
                case Message.TYPE_BLE_RECEV_CMD_3E:
                    notifyData(BleMsg.STR_RSP_MSG3E_ERROR, extra);
                    break;
                default:
                    Log.w(TAG, "Message type : " + type + " can not be handler");
                    break;
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
