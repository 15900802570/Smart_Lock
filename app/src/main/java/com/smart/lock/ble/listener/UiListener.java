package com.smart.lock.ble.listener;

import com.smart.lock.ble.message.Message;
import com.smart.lock.entity.Device;

public interface UiListener {

    void deviceStateChange(Device device, int state);

    void dispatchUiCallback(Message msg, Device device, int type);

    void reConnectBle(Device device); //尝试重连

    void sendFailed(Message msg);

    void addUserSuccess(Device device); //添加用户成功

    void scanDevFialed(); //搜索失败
}
