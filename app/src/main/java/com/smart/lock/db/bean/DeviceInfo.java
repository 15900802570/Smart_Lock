
package com.smart.lock.db.bean;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

@DatabaseTable(tableName = "tb_device_info")
public class DeviceInfo implements Serializable {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(columnName = "device_id")
    private int deviceId;

    @DatabaseField(columnName = "device_name")
    private String deviceName;

    @DatabaseField(columnName = "device_location")
    private String deviceLocation;

    @DatabaseField(columnName = "device_type")
    private String deviceType;

    @DatabaseField(columnName = "support_net")
    private boolean supportNet;

    @DatabaseField(columnName = "device_date")
    private long deviceDate;

    @DatabaseField(columnName = "device_nodeId")
    private String deviceNodeId;

    @DatabaseField(columnName = "geteway_nodeId")
    private String getewayNodeId;

    @DatabaseField(columnName = "device_sn")
    private String deviceSn;

    @DatabaseField(columnName = "device_version")
    private String deviceVersion;

    @DatabaseField(columnName = "device_status")
    private String deviceStatus;

    @DatabaseField(columnName = "activited_time")
    private long activitedTime;

    @DatabaseField(columnName = "device_description")
    private String description;

    @DatabaseField(columnName = "device_topic")
    private String topic;

    @DatabaseField(columnName = "device_user")
    private String deviceUser;

    @DatabaseField(columnName = "device_default")
    private boolean deviceDefault;

    @DatabaseField(columnName = "device_key")
    private String productKey;

    @DatabaseField(columnName = "device_secret")
    private String deviceSecret;

    @DatabaseField(columnName = "node_type")
    private String nodeType;

    @DatabaseField(columnName = "ble_mac")
    private String bleMac;

    @DatabaseField(columnName = "unlock_time")
    private String unLockTime;

    @DatabaseField(columnName = "lock_battery")
    private int lockBattery;

    public int getLockBattery() {
        return lockBattery;
    }

    public void setLockBattery(int lockBattery) {
        this.lockBattery = lockBattery;
    }

    public String getUnLockTime() {
        return unLockTime;
    }

    public void setUnLockTime(String unLockTime) {
        this.unLockTime = unLockTime;
    }

    public String getBleMac() {
        return bleMac;
    }

    public void setBleMac(String bleMac) {
        this.bleMac = bleMac;
    }

    public boolean getDeviceDefault() {
        return deviceDefault;
    }

    public void setDeviceDefault(boolean deviceDefault) {
        this.deviceDefault = deviceDefault;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public void setnodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getProductKey() {
        return productKey;
    }

    public void setProductKey(String productKey) {
        this.productKey = productKey;
    }

    public String getDeviceSecret() {
        return deviceSecret;
    }

    public void setDeviceSecret(String deviceSecret) {
        this.deviceSecret = deviceSecret;
    }

    public String getDeviceUser() {
        return deviceUser;
    }

    public void setDeviceUser(String deviceUser) {
        this.deviceUser = deviceUser;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(String deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public String getDeviceVersion() {
        return deviceVersion;
    }

    public void setDeviceVersion(String deviceVersion) {
        this.deviceVersion = deviceVersion;
    }

    /**
     * 设备显示优先级
     */
    @DatabaseField(columnName = "device_index")
    private int deviceIndex;

    public long getActivitedTime() {
        return activitedTime;
    }

    public void setActivitedTime(long activitedTime) {
        this.activitedTime = activitedTime;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceLocation() {
        return deviceLocation;
    }

    public void setDeviceLocation(String deviceLocation) {
        this.deviceLocation = deviceLocation;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public boolean getConnectType() {
        return supportNet;
    }

    public void setConnectType(boolean connectType) {
        this.supportNet = connectType;
    }

    public long getDeviceDate() {
        return deviceDate;
    }

    public void setDeviceDate(long deviceDate) {
        this.deviceDate = deviceDate;
    }

    public String getDeviceNodeId() {
        return deviceNodeId;
    }

    public void setDeviceNodeId(String deviceNodeId) {
        this.deviceNodeId = deviceNodeId;
    }

    public String getDeviceSn() {
        return deviceSn;
    }

    public void setDeviceSn(String deviceSn) {
        this.deviceSn = deviceSn;
    }

    public int getDeviceIndex() {
        return deviceIndex;
    }

    public void setDeviceIndex(int deviceIndex) {
        this.deviceIndex = deviceIndex;
    }

    public String getGetewayNodeId() {
        return getewayNodeId;
    }

    public void setGetewayNodeId(String getewayNodeId) {
        this.getewayNodeId = getewayNodeId;
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "id=" + id +
                ", deviceId=" + deviceId +
                ", deviceName='" + deviceName + '\'' +
                ", deviceLocation='" + deviceLocation + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", supportNet=" + supportNet +
                ", deviceDate=" + deviceDate +
                ", deviceNodeId='" + deviceNodeId + '\'' +
                ", getewayNodeId='" + getewayNodeId + '\'' +
                ", deviceSn='" + deviceSn + '\'' +
                ", deviceVersion='" + deviceVersion + '\'' +
                ", deviceStatus='" + deviceStatus + '\'' +
                ", activitedTime=" + activitedTime +
                ", description='" + description + '\'' +
                ", topic='" + topic + '\'' +
                ", deviceUser='" + deviceUser + '\'' +
                ", deviceDefault=" + deviceDefault +
                ", productKey='" + productKey + '\'' +
                ", deviceSecret='" + deviceSecret + '\'' +
                ", nodeType='" + nodeType + '\'' +
                ", bleMac='" + bleMac + '\'' +
                ", unLockTime='" + unLockTime + '\'' +
                ", lockBattery=" + lockBattery +
                ", deviceIndex=" + deviceIndex +
                '}';
    }

}
