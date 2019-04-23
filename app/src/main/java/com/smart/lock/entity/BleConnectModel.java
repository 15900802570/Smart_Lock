package com.smart.lock.entity;

import android.content.Context;

import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.utils.ConstantUtil;

import java.util.ArrayList;
import java.util.Arrays;

import static com.smart.lock.ble.message.MessageCreator.mIs128Code;

public class BleConnectModel {

    private static BleConnectModel instance;
    private Context mContext;
    public static final int BLE_CONNECTED = 20; //已连接
    public static final int BLE_CONNECTION = 21; //正在连接
    public static final int BLE_DISCONNECTED = 22; //未连接

    public static final byte BLE_SCAN_QR_CONNECT_TYPE = 0;
    public static final byte BLE_OTHER_CONNECT_TYPE = 1;
    public static final byte BLE_SET_DEVICE_INFO_CONNECT_INFO = 2;

    private DeviceInfo mDevInfo; //连接设备实例
    private int state = BLE_DISCONNECTED;  //连接状态
    private int battery = 0; //设备当前电量
    private int userStatus = 0; //当前用户状态，1字节，0-未启用，1-启用，2-暂停
    private int stStatus = 0; //设置状态字，1字节，低位第一位表示常开功能是否开启，低位第二位表示语音提示是否开启
    private int unLockTime = 0; //回锁时间，1字节，5s，8s，10s
    private byte[] syncUsers = new byte[16]; //同步状态字，16字节
    private byte[] allStatus = new byte[100];//所有用户状态
    private byte[] tempSecret = new byte[4 * (mIs128Code ? 16 : 32)]; //临时秘钥储存

//    public BleConnectModel(Context context, DeviceInfo deviceInfo) {
//        mContext = context;
//        mDevInfo = deviceInfo;
//    }
//
//    public static BleConnectModel getInstance(Context context, DeviceInfo deviceInfo) {
//        if (instance == null) {
//            synchronized (BleConnectModel.class) {
//                if (instance == null) {
//                    instance = new BleConnectModel(context, deviceInfo);
//                }
//            }
//        }
//        return instance;
//    }

    public BleConnectModel(Context context) {
        mContext = context;

    }

    public static BleConnectModel getInstance(Context context) {
        if (instance == null) {
            synchronized (BleConnectModel.class) {
                if (instance == null) {
                    instance = new BleConnectModel(context);
                }
            }
        }
        return instance;
    }

    public DeviceInfo getDevInfo() {
        return mDevInfo;
    }

    public void setDevInfo(DeviceInfo mDevInfo) {
        this.mDevInfo = mDevInfo;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getBattery() {
        return battery;
    }

    public void setBattery(int battery) {
        this.battery = battery;
    }

    public int getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(int userStatus) {
        this.userStatus = userStatus;
    }

    public int getStStatus() {
        return stStatus;
    }

    public void setStStatus(int stStatus) {
        this.stStatus = stStatus;
    }

    public int getUnLockTime() {
        return unLockTime;
    }

    public void setUnLockTime(int unLockTime) {
        this.unLockTime = unLockTime;
    }

    public byte[] getSyncUsers() {
        return syncUsers;
    }

    public void setSyncUsers(byte[] syncUsers) {
        this.syncUsers = syncUsers;
    }

    public byte[] getAllStatus() {
        return allStatus;
    }

    public void setAllStatus(byte[] allStatus) {
        this.allStatus = allStatus;
    }

    public byte[] getTempSecret() {
        return tempSecret;
    }

    public void setTempSecret(byte[] tempSecret) {
        this.tempSecret = tempSecret;
    }

    public void halt() {
        state = BLE_DISCONNECTED;
        instance = null;
    }

    @Override
    public String toString() {
        return "BleConnectModel{" +
                ", mDevInfo=" + mDevInfo.toString() +
                ", state=" + state +
                ", battery=" + battery +
                ", userStatus=" + userStatus +
                ", stStatus=" + stStatus +
                ", unLockTime=" + unLockTime +
                ", syncUsers=" + Arrays.toString(syncUsers) +
                ", allStatus=" + Arrays.toString(allStatus) +
                ", tempSecret=" + Arrays.toString(tempSecret) +
                '}';
    }
}
