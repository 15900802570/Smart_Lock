package com.smart.lock.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;

import java.util.List;
import java.util.UUID;

public class Device extends Peripheral {

    public Device(BluetoothDevice device, byte[] scanRecord, int rssi) {
        super(device, scanRecord, rssi);
    }

    @Override
    protected void onConnect() {
        super.onConnect();
    }

    @Override
    protected void onDisconnect() {
        super.onDisconnect();
    }

    @Override
    protected void onServicesDiscovered(List<BluetoothGattService> services) {
        super.onServicesDiscovered(services);
    }

    @Override
    protected void onNotify(byte[] data, UUID serviceUUID, UUID characteristicUUID, Object tag) {
        super.onNotify(data, serviceUUID, characteristicUUID, tag);
    }
}
