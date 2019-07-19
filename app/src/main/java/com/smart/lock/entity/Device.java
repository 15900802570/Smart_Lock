package com.smart.lock.entity;

import android.content.Context;

import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.utils.LogUtil;

import java.util.Arrays;
import java.util.UUID;

import static com.smart.lock.ble.message.MessageCreator.mIs128Code;

public class Device {
    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID FIRMWARE_REVISON_UUID = UUID
            .fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID DIS_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    private static final String TAG = "Device";
    private static Device instance;
    private Context mContext;
    public static final int BLE_CONNECTED = 20; //已连接
    public static final int BLE_CONNECTION = 21; //正在连接
    public static final int BLE_DISCONNECTED = 22; //未连接

    private boolean mDisconnectBle = false;//主动断开蓝牙

    public boolean isBackGroundConnect() {
        return mBackGroundConnect;
    }

    public void setBackGroundConnect(boolean mBackGroundConnect) {
        this.mBackGroundConnect = mBackGroundConnect;
    }

    private boolean mBackGroundConnect = false;//

    public static final byte BLE_SCAN_QR_CONNECT_TYPE = 0;
    public static final byte BLE_OTHER_CONNECT_TYPE = 1;
    public static final byte BLE_SET_DEVICE_INFO_CONNECT_TYPE = 2;
    public static final byte BLE_SEARCH_DEV_CONNECT = 3;
    public static final byte BLE_SCAN_AUTH_CODE_CONNECT = 4;
    public static final byte BLE_CONNECT_TYPE = BLE_SCAN_QR_CONNECT_TYPE;

    public static final int DFU_CHAR_EXISTS = 0x01;
    public static final int DFU_FW_LOADED = 0x02;
    public static final int DFU_READY = DFU_CHAR_EXISTS | DFU_FW_LOADED;
    public static final int DFU_CHAR_DISCONNECTED = 0x04;
    public static final int DFU_FW_UNLOADED = 0x08;

    //连接方式 0-扫描二维码 1-普通安全连接,2-设置设备信息
    private byte connectType = Device.BLE_SCAN_QR_CONNECT_TYPE;

    private DeviceInfo mDevInfo; //连接设备实例
    private int state = BLE_DISCONNECTED;  //连接状态
    private int battery = 0; //设备当前电量
    private int userStatus = 0; //当前用户状态，1字节，0-未启用，1-启用，2-暂停
    private int stStatus = 0; //设置状态字，1字节，低位第一位表示常开功能是否开启，低位第二位表示语音提示是否开启
    private int unLockTime = 0; //回锁时间，1字节，5s，8s，10s
    private byte[] syncUsers = new byte[16]; //同步状态字，16字节
    private byte[] allStatus = new byte[100];//所有用户状态
    private byte[] tempSecret = new byte[4 * (mIs128Code ? 16 : 32)]; //临时秘钥储存
    private byte[] tempAuthCode = new byte[30];

    public Device(Context context) {
        mContext = context;
    }

    public static Device getInstance(Context context) {
        if (instance == null) {
            synchronized (Device.class) {
                if (instance == null) {
                    instance = new Device(context);
                }
            }
        }
        return instance;
    }

    public boolean isDisconnectBle() {
        return mDisconnectBle;
    }

    public void setDisconnectBle(boolean mDisconnectBle) {
        this.mDisconnectBle = mDisconnectBle;
    }

    public byte getConnectType() {
        return connectType;
    }

    public void setConnectType(byte connectType) {
        this.connectType = connectType;
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
        LogUtil.d(TAG, "halt");
        state = BLE_DISCONNECTED;
        battery = 0; //设备当前电量
        userStatus = 0; //当前用户状态，1字节，0-未启用，1-启用，2-暂停
        stStatus = 0; //设置状态字，1字节，低位第一位表示常开功能是否开启，低位第二位表示语音提示是否开启
        unLockTime = 0; //回锁时间，1字节，5s，8s，10s
        syncUsers = new byte[16]; //同步状态字，16字节
        allStatus = new byte[100];//所有用户状态
        tempSecret = new byte[4 * (mIs128Code ? 16 : 32)]; //临时秘钥储存
        tempAuthCode = new byte[30];

        mDevInfo = null;
    }

    public void exchangeConnect(DeviceInfo deviceInfo){
        state = BLE_DISCONNECTED;
        mDevInfo = deviceInfo;
    }

    public byte[] getTempAuthCode() {
        return tempAuthCode;
    }

    public void setTempAuthCode(byte[] tempAuthCode) {
        this.tempAuthCode = tempAuthCode;
    }

    @Override
    public String toString() {
        return "Device{" +
                ", mDisconnectBle=" + mDisconnectBle +
                ", mBackGroundConnect=" + mBackGroundConnect +
                ", connectType=" + connectType +
                ", mDevInfo=" + mDevInfo +
                ", state=" + state +
                ", battery=" + battery +
                ", userStatus=" + userStatus +
                ", stStatus=" + stStatus +
                ", unLockTime=" + unLockTime +
                ", syncUsers=" + Arrays.toString(syncUsers) +
                ", allStatus=" + Arrays.toString(allStatus) +
                ", tempSecret=" + Arrays.toString(tempSecret) +
                ", tempAuthCode=" + Arrays.toString(tempAuthCode) +
                '}';
    }
}
