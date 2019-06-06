
package com.smart.lock.db.bean;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;
import java.util.Arrays;

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

    @DatabaseField(columnName = "device_sw_version")
    private String deviceSwVersion;

    @DatabaseField(columnName = "device_hw_version")
    private String deviceHwVersion;

    @DatabaseField(columnName = "fp_sw_version")
    private String fpSwVersion;

    @DatabaseField(columnName = "device_status")
    private String deviceStatus;

    @DatabaseField(columnName = "activited_time")
    private long activitedTime;

    @DatabaseField(columnName = "device_description")
    private String description;

    @DatabaseField(columnName = "device_topic")
    private String topic;

    @DatabaseField(columnName = "user_id")
    private short userId;

    @DatabaseField(columnName = "device_default")
    private boolean deviceDefault;

    @DatabaseField(columnName = "device_key")
    private String productKey;

    @DatabaseField(columnName = "device_secret")
    private String deviceSecret; //板卡的动态秘钥

    @DatabaseField(columnName = "node_type")
    private String nodeType;

    @DatabaseField(columnName = "ble_mac")
    private String bleMac;

    @DatabaseField(columnName = "unlock_time")
    private String unLockTime;

    @DatabaseField(columnName = "mix_unlock")
    private int mixUnlock;

    @DatabaseField(columnName = "lock_battery")
    private int lockBattery;

    @DatabaseField(columnName = "temp_pwd_secret")
    private String tempSecret;

    public String getFpSwVersion() {
        return fpSwVersion;
    }

    public void setFpSwVersion(String fpSwVersion) {
        this.fpSwVersion = fpSwVersion;
    }

    public String getDeviceSwVersion() {
        return deviceSwVersion;
    }

    public void setDeviceSwVersion(String deviceSwVersion) {
        this.deviceSwVersion = deviceSwVersion;
    }

    public String getDeviceHwVersion() {
        return deviceHwVersion;
    }

    public void setDeviceHwVersion(String deviceHwVersion) {
        this.deviceHwVersion = deviceHwVersion;
    }

    public int getMixUnlock() {
        return mixUnlock;
    }

    public void setMixUnlock(int mixUnlock) {
        this.mixUnlock = mixUnlock;
    }

    public String getTempSecret() {
        return tempSecret;
    }

    public void setTempSecret(String tempSecret) {
        this.tempSecret = tempSecret;
    }

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

    public short getUserId() {
        return userId;
    }

    public void setUserId(short userId) {
        this.userId = userId;
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
                ", deviceSwVersion='" + deviceSwVersion + '\'' +
                ", deviceHwVersion='" + deviceHwVersion + '\'' +
                ", deviceStatus='" + deviceStatus + '\'' +
                ", activitedTime=" + activitedTime +
                ", description='" + description + '\'' +
                ", topic='" + topic + '\'' +
                ", userId='" + userId + '\'' +
                ", deviceDefault=" + deviceDefault +
                ", productKey='" + productKey + '\'' +
                ", deviceSecret='" + deviceSecret + '\'' +
                ", nodeType='" + nodeType + '\'' +
                ", bleMac='" + bleMac + '\'' +
                ", unLockTime='" + unLockTime + '\'' +
                ", mixUnlock=" + mixUnlock +
                ", lockBattery=" + lockBattery +
                ", tempSecret='" + tempSecret + '\'' +
                ", deviceIndex=" + deviceIndex +
                '}';
    }

}
