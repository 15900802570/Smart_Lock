package com.smart.lock.ble.listener;

import android.content.Context;

import com.smart.lock.ble.BleCardService;
import com.smart.lock.ble.Device;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.ble.provider.BleProvider;
import com.smart.lock.ble.provider.TimerProvider;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.entity.DeviceEngine;
import com.smart.lock.utils.LogUtil;

public class BleRegisterAgent implements MsgExceptionListener {
    private static final String TAG = BleRegisterAgent.class.getSimpleName();
    private Context mCtx;
    private DeviceEngine mEngine;
    private BleCardService mService;

    public BleRegisterAgent(Context context, DeviceEngine engine, BleCardService service) {
        mCtx = context;
        mEngine = engine;
        mService = service;
    }

//    public void register(DeviceInfo info){
//        mService.sendCmd01(info.)
//    }

    @Override
    public void msgSendFail(Message msg) {

    }

    @Override
    public void msgSendTimeOut(Message msg) {

    }
}
