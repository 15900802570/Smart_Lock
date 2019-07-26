package com.smart.lock.utils;

import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.ArrayMap;

import com.smart.lock.R;
import com.smart.lock.ble.BleCardService;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.listener.DeviceStateCallback;

public class SendOTAData implements DeviceStateCallback, Handler.Callback {

    private byte[] cmd;
    private int index = Integer.MAX_VALUE;
    private BleManagerHelper mBleManagerHelper;
    private int type;

    private Context mActivity;

    private ArrayMap<Integer, byte[]> dataList = new ArrayMap<>();
    private Handler mHandler;
    private String TAG = "SendOTAData";
    private final Object mState = new Object();

    public SendOTAData(Context context, BleManagerHelper mBleManagerHelper, int type) {
        this.mActivity = context;
        this.mBleManagerHelper = mBleManagerHelper;
        this.type = type;
        mHandler = new Handler(this);
    }

    public void start(byte[] data) {
        this.cmd = data;
        int mRspRead = 0;
        int bufLen = 0;

        if (cmd.length > 20) {
            bufLen = cmd.length / 20;

            if (cmd.length % 20 != 0)
                bufLen++;

            for (int len = 0; len < bufLen; len++) {
                byte[] mRspBuf = new byte[20];
                mRspRead = len * 20;
                for (int i = 0; i < 20; i++) {
                    if ((mRspRead + i) >= cmd.length) {
                        break;
                    }
                    mRspBuf[i] = cmd[mRspRead + i];
                }
                synchronized (mState) {
                    dataList.put(len, mRspBuf);
                    mRspBuf = null;
                }
            }

            index = 0;
        }
        send(index++);
    }

    private void send(int index) {
        LogUtil.d(TAG, "index : " + index);
        if (dataList.get(index) != null) {
            mBleManagerHelper.getBleCardService().sendCmdOtaData(dataList.get(index), type);
        } else {
            this.cmd = null;
            this.dataList.clear();
            this.index = Integer.MAX_VALUE;
            /**
             * 通知发送下一个包
             */
            if (mActivity instanceof OnSendingListener) {
                ((OnSendingListener) mActivity).onSending();
            }
        }
    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onServicesDiscovered(int state) {

    }

    @Override
    public void onGattStateChanged(int state, int type) {
        if (state == BluetoothGatt.GATT_SUCCESS && index != Integer.MAX_VALUE) {
            sendMessage(type, null, 0);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case BleCardService.READ:
                LogUtil.d(TAG, "READ");
                send(index);
                break;
            case BleCardService.WRITE:
//                int count = index++;
//                LogUtil.d(TAG, "count : " + count);
//                boolean ret = mBleManagerHelper.getBleCardService().validateOta(this.type, count);
//
//                LogUtil.d(TAG, "WRITE" + ret);
//                if (!ret) {
//                    send(count);
//                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                send(index++);
                break;
        }
        return true;
    }

    public interface OnSendingListener {
        void onSending();
    }

    private void sendMessage(int type, Bundle bundle, long time) {
        android.os.Message msg = new android.os.Message();
        if (bundle != null) {
            msg.setData(bundle);
        }
        msg.what = type;
        if (time != 0) {
            mHandler.sendMessageDelayed(msg, time);
        } else mHandler.sendMessage(msg);

    }
}
