package com.smart.lock.db.bean;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

@DatabaseTable(tableName = "tb_device_key")
public class DeviceKey implements Serializable {

    @DatabaseField(generatedId = true, columnName = "_id")
    private int id;

    @DatabaseField(columnName = "device_nodeId")
    private String deviceNodeId;

    @DatabaseField(columnName = "key_type")
    private byte keyType;

    @DatabaseField(columnName = "user_id")
    private short userId;

    @DatabaseField(columnName = "lock_id")
    private String lockId;

    @DatabaseField(columnName = "key_name")
    private String keyName;

    @DatabaseField(columnName = "pwd")
    private String pwd;

    @DatabaseField(columnName = "key_active_time")
    private long keyActiveTime;

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDeviceNodeId() {
        return deviceNodeId;
    }

    public void setDeviceNodeId(String deviceNodeId) {
        this.deviceNodeId = deviceNodeId;
    }

    public byte getKeyType() {
        return keyType;
    }

    public void setKeyType(byte keyType) {
        this.keyType = keyType;
    }

    public short getUserId() {
        return userId;
    }

    public void setUserId(short userId) {
        this.userId = userId;
    }

    public String getLockId() {
        return lockId;
    }

    public void setLockId(String lockId) {
        this.lockId = lockId;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public long getKeyActiveTime() {
        return keyActiveTime;
    }

    public void setKeyActiveTime(long keyActiveTime) {
        this.keyActiveTime = keyActiveTime;
    }

    @Override
    public String toString() {
        return "DeviceKey{" +
                "id=" + id +
                ", deviceNodeId='" + deviceNodeId + '\'' +
                ", keyType='" + keyType + '\'' +
                ", userId=" + userId +
                ", lockId='" + lockId + '\'' +
                ", keyName='" + keyName + '\'' +
                ", pwd='" + pwd + '\'' +
                ", keyActiveTime=" + keyActiveTime +
                '}';
    }
}
