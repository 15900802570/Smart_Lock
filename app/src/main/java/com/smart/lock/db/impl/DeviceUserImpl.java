
package com.smart.lock.db.impl;

import com.smart.lock.db.bean.DeviceUser;

import java.util.ArrayList;

public interface DeviceUserImpl {

    void insert(ArrayList<DeviceUser> beanArrayList);

    void insert(DeviceUser myBean);

    void deleteByKey(String key, String values);

    int deleteAll();

    long queryCount();

    ArrayList<DeviceUser> queryId(int id);

    ArrayList<DeviceUser> queryAll();

    ArrayList<DeviceUser> queryKey(String key, Object value);

    void updateDeviceUser(DeviceUser info);

    DeviceUser queryUser(Object nodeId, Object userId);

    ArrayList<DeviceUser> queryUsers(Object nodeId, Object permission);

    ArrayList<DeviceUser> queryDeviceUsers(Object nodeId);

    ArrayList<String> queryDeviceUserIds(Object nodeId);

    void delete(DeviceUser info);

    DeviceUser queryDefaultUsers(Object nodeId);

}
