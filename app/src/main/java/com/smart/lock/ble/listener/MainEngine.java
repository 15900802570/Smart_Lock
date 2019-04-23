package com.smart.lock.ble.listener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.smart.lock.ble.BleCardService;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.provider.TimerProvider;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.entity.BleConnectModel;
import com.smart.lock.utils.LogUtil;

import java.util.Arrays;

public class MainEngine implements BleMessageListener {

    private static final String TAG = MainEngine.class.getSimpleName();
    private Context mCtx; //上下文
    private BleCardService mService;
    private BleConnectModel mBleModel;
    private DeviceInfo mDevInfo;

    /**
     * 连接方式 0-扫描二维码 1-普通安全连接,2-设置设备信息
     */
    private byte mConnectType = BleConnectModel.BLE_SCAN_QR_CONNECT_TYPE;

    public MainEngine(Context context, BleCardService service) {
        mCtx = context;
        mService = service;
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
                    break;
                case Message.EXCEPTION_SEND_FAIL:
                    break;
                default:
                    break;
            }

            switch (type) {
                case Message.TYPE_BLE_RECEV_CMD_02:
                    final byte[] random = extra.getByteArray(BleMsg.KEY_RANDOM);
                    if (random != null && random.length != 0) {
                        mService.sendCmd03(random);
                    }
//                    notifyData(BleMsg.EXTRA_DATA_MSG_02, extra);
                    break;
                case Message.TYPE_BLE_RECEV_CMD_04:
                    break;
                case Message.TYPE_BLE_RECEV_CMD_12:
                    break;
                case Message.TYPE_BLE_RECEV_CMD_1A:
                    break;
                case Message.TYPE_BLE_RECEV_CMD_1C:
                    break;
                case Message.TYPE_BLE_RECEV_CMD_1E:
                    break;
                case Message.TYPE_BLE_RECEV_CMD_16:
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
                    break;
                case Message.TYPE_BLE_RECEV_CMD_26:
                    break;
                case Message.TYPE_BLE_RECEV_CMD_32:
                    break;
                case Message.TYPE_BLE_RECEV_CMD_3E:
                    break;
                default:
                    Log.w(TAG, "Message type : " + type + " can not be handler");
                    break;
            }

        } finally {
            message.recycle();
        }
    }

    @Override
    public String getListenerKey() {
        return null;
    }

    @Override
    public void halt() {

    }


    /**
     * 扫描二维码注册
     *
     * @param connectModel 注册信息
     * @return 发送结果
     */
    public boolean scanQrRegister(BleConnectModel connectModel) {
        LogUtil.d(TAG, "scan qr register ble !");
        mBleModel = connectModel;
        if (mBleModel == null) return false;
        if (mDevInfo == null) return false;
        mDevInfo = mBleModel.getDevInfo();
        changeRegisterType(BleConnectModel.BLE_SCAN_QR_CONNECT_TYPE);
        return mService.sendCmd01(BleConnectModel.BLE_SCAN_QR_CONNECT_TYPE, mDevInfo.getUserId());
    }

    /**
     * 普通连接方式注册
     *
     * @param connectModel 注册信息
     * @return 发送结果
     */
    public boolean otherRegister(BleConnectModel connectModel) {
        LogUtil.d(TAG, "other register ble !");
        mBleModel = connectModel;
        if (mBleModel == null) return false;
        if (mDevInfo == null) return false;
        mDevInfo = mBleModel.getDevInfo();
        changeRegisterType(BleConnectModel.BLE_OTHER_CONNECT_TYPE);
        return mService.sendCmd01(BleConnectModel.BLE_OTHER_CONNECT_TYPE, mDevInfo.getUserId());
    }

    /**
     * 普通连接方式注册
     *
     * @param connectModel 注册信息
     * @return 发送结果
     */
    public boolean setInfoRegister(BleConnectModel connectModel) {
        LogUtil.d(TAG, "other register ble !");
        mBleModel = connectModel;
        if (mBleModel == null) return false;
        if (mDevInfo == null) return false;
        mDevInfo = mBleModel.getDevInfo();
        changeRegisterType(BleConnectModel.BLE_OTHER_CONNECT_TYPE);
        return mService.sendCmd01(BleConnectModel.BLE_OTHER_CONNECT_TYPE, mDevInfo.getUserId());
    }

    /**
     * 切换注册类型
     * @param type 切换类型
     */
    private void changeRegisterType(byte type) {
        LogUtil.d(TAG, "change connect ble type from : " + mConnectType + " to " + type + "!");
        mConnectType = type;
    }

    /**
     * 获取注册类型
     * @return 注册类型
     */
    public byte getRegisterType(){
        return mConnectType;
    }



}
