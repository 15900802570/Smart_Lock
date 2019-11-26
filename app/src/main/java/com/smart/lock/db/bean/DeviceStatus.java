package com.smart.lock.db.bean;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

@DatabaseTable(tableName = "tb_device_status")
public class DeviceStatus implements Serializable {

    @DatabaseField(generatedId = true, columnName = "_id")
    private int id;

    //设备ID
    @DatabaseField(columnName = "dev_node_id", canBeNull = false)
    private String devNodeId;

    //智能锁芯
    @DatabaseField(columnName = "intelligent_lock_core", defaultValue = "0")
    private boolean intelligentLockCore;

    //防撬报警
    @DatabaseField(columnName = "anti_prizing_alarm", defaultValue = "0")
    private boolean antiPrizingAlarm;

    //组合开锁
    @DatabaseField(columnName = "combination_lock", defaultValue = "0")
    private boolean combinationLock;

    //常开功能
    @DatabaseField(columnName = "normally_open", defaultValue = "0")
    private boolean normallyOpen;

    //语言提示
    @DatabaseField(columnName = "voice_prompt", defaultValue = "1")
    private boolean voicePrompt;

    //蓝牙广播
    @DatabaseField(columnName = "broadcastNormallyOpen", defaultValue = "0")
    private boolean broadcastNormallyOpen;

    //M1卡 NFC卡类型
    @DatabaseField(columnName = "support_m1", defaultValue = "0")
    private boolean m1Support;

    //是否禁止智能锁芯 1为禁用 0为启用
    @DatabaseField(columnName = "invalid_intelligent_lock", defaultValue = "1")
    private boolean invalidIntelligentLock;

    //回锁时间
    @DatabaseField(columnName = "rolled_back_time", defaultValue = "5")
    private int rolledBackTime;

    //省电开始时间
    @DatabaseField(columnName = "power_saving_start_time", defaultValue = "2300")
    private int powerSavingStartTime;

    //省电结束时间
    @DatabaseField(columnName = "power_saving_end_time", defaultValue = "700")
    private int powerSavingEndTime;

    //电池
    @DatabaseField(columnName = "battery", defaultValue = "0")
    private int battery;

    //信息更新时间
    @DatabaseField(columnName = "status_update_time")
    private long updateTime;

    //自动开门状态 0为禁用 1为启用
    @DatabaseField(columnName = "autoCloseEnable", defaultValue = "0")
    private boolean autoCloseEnable;

    //红外状态 0为禁用 1为启用
    @DatabaseField(columnName = "infraredEnable", defaultValue = "0")
    private boolean infraredEnable;

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public int getBattery() {
        return battery;
    }

    public void setBattery(int battery) {
        this.battery = battery;
    }

    public String getDevNodeId() {
        return devNodeId;
    }

    public void setDevNodeId(String devNodeId) {
        this.devNodeId = devNodeId;
    }

    public boolean isIntelligentLockCore() {
        return intelligentLockCore;
    }

    public void setIntelligentLockCore(boolean intelligentLockCore) {
        this.intelligentLockCore = intelligentLockCore;
    }

    public boolean isAntiPrizingAlarm() {
        return antiPrizingAlarm;
    }

    public void setAntiPrizingAlarm(boolean antiPrizingAlarm) {
        this.antiPrizingAlarm = antiPrizingAlarm;
    }

    public boolean isCombinationLock() {
        return combinationLock;
    }

    public void setCombinationLock(boolean combinationLock) {
        this.combinationLock = combinationLock;
    }

    public boolean isNormallyOpen() {
        return normallyOpen;
    }

    public void setNormallyOpen(boolean normallyOpen) {
        this.normallyOpen = normallyOpen;
    }

    public boolean isVoicePrompt() {
        return voicePrompt;
    }

    public void setVoicePrompt(boolean voicePrompt) {
        this.voicePrompt = voicePrompt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRolledBackTime() {
        return rolledBackTime;
    }

    public void setRolledBackTime(int rolledBackTime) {
        this.rolledBackTime = rolledBackTime;
    }

    public boolean isM1Support() {
        return m1Support;
    }

    public void setM1Support(boolean m1Support) {
        this.m1Support = m1Support;
    }

    public int getPowerSavingEndTime() {
        return powerSavingEndTime;
    }

    public void setPowerSavingEndTime(int powerSavingEndTime) {
        this.powerSavingEndTime = powerSavingEndTime;
    }

    public int getPowerSavingStartTime() {
        return powerSavingStartTime;
    }

    public void setPowerSavingStartTime(int powerSavingStartTime) {
        this.powerSavingStartTime = powerSavingStartTime;
    }

    public boolean isBroadcastNormallyOpen() {
        return broadcastNormallyOpen;
    }

    public void setBroadcastNormallyOpen(boolean broadcastNormallyOpen) {
        this.broadcastNormallyOpen = broadcastNormallyOpen;
    }

    public boolean isInvalidIntelligentLock() {
        return invalidIntelligentLock;
    }

    public void setInvalidIntelligentLock(boolean invalidIntelligentLock) {
        this.invalidIntelligentLock = invalidIntelligentLock;
    }

    public boolean isAutoCloseEnable() {
        return autoCloseEnable;
    }

    public void setAutoCloseEnable(boolean autoCloseEnable) {
        this.autoCloseEnable = autoCloseEnable;
    }

    public String toString() {
        return "DeviceStatus{" +
                "id=" + id +
                ", nodeId='" + devNodeId + '\''+
                '}';
    }

    public boolean isInfraredEnable() {
        return infraredEnable;
    }

    public void setInfraredEnable(boolean infraredEnable) {
        this.infraredEnable = infraredEnable;
    }
}
