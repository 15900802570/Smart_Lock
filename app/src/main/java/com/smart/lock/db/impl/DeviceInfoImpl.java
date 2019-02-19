
package com.smart.lock.db.impl;

import com.smart.lock.db.bean.DeviceInfo;

import java.util.ArrayList;

public interface DeviceInfoImpl {

    void insert(ArrayList<DeviceInfo> beanArrayList);

    void insert(DeviceInfo myBean);

    void deleteByKey(String key, String values);

    int deleteAll();

    long queryCount();

    ArrayList<DeviceInfo> queryId(int id);

    ArrayList<DeviceInfo> queryAll();

    ArrayList<Boolean> queryConnectType(String pram);

    ArrayList<DeviceInfo> queryByConnectType(String connectType);

    ArrayList<DeviceInfo> queryKey(String key, Object value);

    ArrayList<String> queryDeviceType(String key, String valus);

    void updateDeviceInfo(DeviceInfo info);

    ArrayList<DeviceInfo> queryKeyByImei(String key, String valus, String imei);

    ArrayList<DeviceInfo> queryKeyByUser(String key, String valus, String user);

    ArrayList<DeviceInfo> queryByUser(String user);

    DeviceInfo queryFirstData(String key, Object valus);

    void delete(DeviceInfo info);

}
