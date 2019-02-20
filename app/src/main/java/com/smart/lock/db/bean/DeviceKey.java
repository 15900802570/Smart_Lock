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
    private String keyType;

    @DatabaseField(columnName = "device_user_id")
    private String deviceUserId;

    @DatabaseField(columnName = "lock_id")
    private String lockId;

    @DatabaseField(columnName = "key_name")
    private String keyName;

    @DatabaseField(columnName = "key_active_time")
    private long keyActiveTime;

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

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public String getDeviceUserId() {
        return deviceUserId;
    }

    public void setDeviceUserId(String deviceUserId) {
        this.deviceUserId = deviceUserId;
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
                ", deviceUserId='" + deviceUserId + '\'' +
                ", lockId='" + lockId + '\'' +
                ", keyName='" + keyName + '\'' +
                ", keyActiveTime=" + keyActiveTime +
                '}';
    }
}
