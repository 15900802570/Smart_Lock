package com.smart.lock.db.bean;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

/**
 * <p>文件描述：<p>
 * <p>作者：chenrong<p>
 * <p>创建时间：2018/9/7<p>
 * <p>更改时间：2018/9/7<p>
 * <p>版本号：1<p>
 */
@DatabaseTable(tableName = "tb_device_log")
public class DeviceLog implements Serializable{
    @DatabaseField(generatedId = true, columnName = "_id")
    private int id;

    @DatabaseField(columnName = "node_id")
    private String nodeId;

    @DatabaseField(columnName = "user_id")
    private short userId;

    //操作秘钥的备注名
    @DatabaseField(columnName = "key_name")
    private String keyName;

    //设备类型
    @DatabaseField(columnName = "device_type")
    private String deviceType;

    //日志类型
    @DatabaseField(columnName = "log_type")
    private byte logType;

    //日志主动删除状态
    @DatabaseField(columnName = "log_state")
    private int logState;

    //秘钥编号
    @DatabaseField(columnName = "lock_id")
    private String lockId;

    //日志时间
    @DatabaseField(columnName = "log_time")
    private long logTime;

    //日志编号
    @DatabaseField(columnName = "log_id")
    private int logId;

    public long getLogTime() {
        return logTime;
    }

    public void setLogTime(long logTime) {
        this.logTime = logTime;
    }

    public short getUserId() {
        return userId;
    }

    public void setUserId(short userId) {
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public byte getLogType() {
        return logType;
    }

    public void setLogType(byte logType) {
        this.logType = logType;
    }

    public int getLogState() {
        return logState;
    }

    public void setLogState(int logState) {
        this.logState = logState;
    }

    public String getLockId() {
        return lockId;
    }

    public void setLockId(String lockId) {
        this.lockId = lockId;
    }

    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    @Override
    public String toString() {
        return "DeviceLog{" +
                "id=" + id +
                ", nodeId='" + nodeId + '\'' +
                ", userId='" + userId + '\'' +
                ", keyName='" + keyName + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", logType='" + logType + '\'' +
                ", logState=" + logState +
                ", lockId='" + lockId + '\'' +
                ", logTime='" + logTime + '\'' +
                ", logId='" + logId + '\'' +
                '}';
    }
}
