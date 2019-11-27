
package com.smart.lock.db.bean;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.smart.lock.utils.LogUtil;

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

    @DatabaseField(columnName = "device_sw_version")
    private String deviceSwVersion;

    @DatabaseField(columnName = "device_hw_version")
    private String deviceHwVersion;

    @DatabaseField(columnName = "fp_sw_version")
    private String fpSwVersion;

    @DatabaseField(columnName = "face_scpu_version")
    private String faceSCPUVersion;

    @DatabaseField(columnName = "face_ncpu_version")
    private String faceNCPUVersion;

    @DatabaseField(columnName = "face_module_version")
    private String faceModuleVersion;

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

    /**
     * 设备显示优先级
     */
    @DatabaseField(columnName = "device_index")
    private int deviceIndex;

    //是否支持nfc 1为禁用 0为启用
    @DatabaseField(columnName = "unable_nfc", defaultValue = "0")
    private boolean unableNfc;

    //是否支持人脸 1为禁用 0为启用
    @DatabaseField(columnName = "enable_face", defaultValue = "0")
    private boolean enableFace;

    //是否支持红外 0为不支持 1为支持
    @DatabaseField(columnName = "enable_infrared", defaultValue = "0")
    private boolean enableInfrared;

    //是否支持红外 0为不支持 1为支持
    @DatabaseField(columnName = "enable_variable_pwd", defaultValue = "0")
    private boolean enableVariablePwd;


    public String getFpSwVersion() {
        return fpSwVersion;
    }

    public void setFpSwVersion(String fpSwVersion) {
        this.fpSwVersion = fpSwVersion;
    }

    public String getDeviceSwVersion() {
        return deviceSwVersion != null ? deviceSwVersion : "SMARTLOCK_1.0.1_20190813";
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
        LogUtil.d("SetDefault", "logID = " + this.getBleMac());
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

    public int getId() {
        return this.id;
    }


    public boolean isUnableNfc() {
        return unableNfc;
    }

    public void setUnableNfc(boolean unableNfc) {
        this.unableNfc = unableNfc;
    }

    public boolean isEnableFace() {
        return enableFace;
    }

    public void setEnableFace(boolean enableFace) {
        LogUtil.e("setEnable" + enableFace);
        this.enableFace = enableFace;
    }

    public boolean isEnableInfrared() {
        return enableInfrared;
    }

    public void setEnableInfrared(boolean enableInfrared) {
        this.enableInfrared = enableInfrared;
    }

    public boolean isEnableVariablePwd() {
        return enableVariablePwd;
    }

    public void setEnableVariablePwd(boolean enableVariablePwd) {
        this.enableVariablePwd = enableVariablePwd;
    }

    public String getFaceSCPUVersion() {
        return faceSCPUVersion;
    }

    public void setFaceSCPUVersion(String faceSCPUVersion) {
        this.faceSCPUVersion = faceSCPUVersion;
    }

    public String getFaceNCPUVersion() {
        return faceNCPUVersion;
    }

    public String getFaceModuleVersion() {
        return faceModuleVersion;
    }

    public void setFaceModuleVersion(String faceModuleVersion) {
        this.faceModuleVersion = faceModuleVersion;
    }

    public void setFaceNCPUVersion(String faceNCPUVersion) {
        this.faceNCPUVersion = faceNCPUVersion;
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
                ", fpSwVersion='" + fpSwVersion + '\'' +
                ", deviceStatus='" + deviceStatus + '\'' +
                ", activitedTime=" + activitedTime +
                ", description='" + description + '\'' +
                ", topic='" + topic + '\'' +
                ", userId=" + userId +
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
