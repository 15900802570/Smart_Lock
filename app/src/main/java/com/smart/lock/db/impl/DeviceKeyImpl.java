
package com.smart.lock.db.impl;

import com.smart.lock.db.bean.DeviceKey;

import java.util.ArrayList;

public interface DeviceKeyImpl {

    void insert(ArrayList<DeviceKey> beanArrayList);

    void insert(DeviceKey myBean);

    void deleteByKey(String key, String values);

    int deleteAll();

    long queryCount();

    ArrayList<DeviceKey> queryId(int id);

    ArrayList<DeviceKey> queryAll();


    ArrayList<DeviceKey> queryByConnectType(String connectType);

    ArrayList<DeviceKey> queryKey(String key, Object value);


    void updateDeviceKey(DeviceKey info);

    ArrayList<DeviceKey> queryKeyByImei(String key, String valus, String imei);

    ArrayList<DeviceKey> queryDeviceKey(Object nodeId, Object userId, Object type);

    DeviceKey queryFirstData(String key, Object valus);

    void delete(DeviceKey info);

}
