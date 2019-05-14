package com.smart.lock.ble.listener;

public interface DeviceStateCallback {

    void onConnected();

    void onDisconnected();

    void onServicesDiscovered(int state);

    void onGattStateChanged(int state);
}
