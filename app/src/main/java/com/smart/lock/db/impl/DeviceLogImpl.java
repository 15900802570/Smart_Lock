
package com.smart.lock.db.impl;

import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceLog;

import java.util.ArrayList;

public interface DeviceLogImpl {

    void insert(ArrayList<DeviceLog> beanArrayList);

    void insert(DeviceLog myBean);

    void deleteByKey(String key, String values);

    int deleteAll();

    long queryCount();

    ArrayList<DeviceLog> queryId(int id);

    ArrayList<DeviceLog> queryAll();


    ArrayList<DeviceLog> queryByConnectType(String connectType);

    ArrayList<DeviceLog> queryKey(String key, Object value);

    void updateDeviceLog(DeviceLog info);

    ArrayList<DeviceLog> queryDeviceLog(Object nodeId, Object userId, Object type);

    ArrayList<DeviceLog> queryUserLog(Object nodeId, Object userId);

    void delete(DeviceLog info);

}
