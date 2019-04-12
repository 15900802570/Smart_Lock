package com.smart.lock.scan;

import com.smart.lock.db.bean.DeviceInfo;

public interface ScanQRResultInterface {

    void onAuthenticationSuccess(DeviceInfo deviceInfo);

    void onAuthenticationFailed();
}
