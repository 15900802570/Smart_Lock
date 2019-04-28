
package com.smart.lock.ble.listener;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.BleCardService;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.provider.BleProvider;
import com.smart.lock.ble.provider.TimerProvider;
import com.smart.lock.utils.LogUtil;

import java.sql.Time;
import java.util.Arrays;

/**
 * 根据message消息类型进行具体的业务处理
 */
public class BleMessageListenerImpl implements BleMessageListener {
    private static final String TAG = "BleMessageListenerImpl";
    private Context mContext;
    private MsgExceptionListener mExceptionListener;
    private BleCardService mService;

    public BleMessageListenerImpl(Context context, BleCardService service) {
        mContext = context;
        mService = service;
    }

    public void registerExceptionListener(MsgExceptionListener msgExceptionListener) {
        mExceptionListener = msgExceptionListener;
    }

    @Override
    public void onReceive(Message message, TimerProvider timer) {
        try {
            int type = message.getType();

            Bundle extra = message.getData();
            Log.i(TAG, "onReceive Message type : " + Message.getMessageTypeTag(type));

            int exception = message.getException();
            LogUtil.e(TAG, "msg exception : " + message.toString());
            switch (exception) {
                case Message.EXCEPTION_TIMEOUT:
                    mExceptionListener.msgSendTimeOut(message);
                    break;
                case Message.EXCEPTION_SEND_FAIL:
                    mExceptionListener.msgSendFail(message);
                    break;
                default:
                    break;
            }

            switch (type) {
                case Message.TYPE_BLE_RECEV_CMD_02:
                    final byte[] random = extra.getByteArray(BleMsg.KEY_RANDOM);
                    if (random != null && random.length != 0) {
                        mService.sendCmd03(random, BleMsg.INT_DEFAULT_TIMEOUT);
                    }
//                    notifyData(BleMsg.EXTRA_DATA_MSG_02, extra);
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
                    byte[] seconds = extra.getByteArray(BleMsg.KEY_TIME_OUT);
                    Log.d(TAG, "seconds : " + Arrays.toString(seconds) + " timer : " + (timer == null));
                    if (seconds != null && timer != null) {
                        long timeOut = seconds[0] * 1000 + 5; //APP侧比芯片侧时长多5s，避免重复remove
                        timer.reSetTimeOut(timeOut);
                    }

//                    notifyData(BleMsg.STR_RSP_MSG18_TIMEOUT, extra);
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

    /**
     * 吐司提示
     *
     * @param msg 提示信息
     */
    private void showMessage(String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

}
