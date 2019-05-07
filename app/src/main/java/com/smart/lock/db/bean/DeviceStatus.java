package com.smart.lock.db.bean;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;
@DatabaseTable(tableName = "tb_device_status")
public class DeviceStatus implements Serializable{

    @DatabaseField(generatedId = true, columnName = "_id")
    private int id;

    @DatabaseField(columnName = "dev_node_id",canBeNull = false)
    private String devNodeId;

    @DatabaseField(columnName = "intelligent_lock_core", defaultValue = "0")
    private boolean intelligentLockCore;

    @DatabaseField(columnName = "anti_prizing_alarm", defaultValue = "0")
    private boolean antiPrizingAlarm;

    @DatabaseField(columnName = "combination_lock", defaultValue = "0")
    private boolean combinationLock;

    @DatabaseField(columnName = "normally_open", defaultValue = "0")
    private boolean normallyOpen;

    @DatabaseField(columnName = "voice_prompt", defaultValue = "1")
    private boolean voicePrompt;

    @DatabaseField(columnName = "support_m1", defaultValue = "0")
    private boolean m1Support;

    @DatabaseField(columnName = "rolled_back_time",defaultValue = "5" )
    private int rolledBackTime;

    @DatabaseField(columnName = "power_saving_start_time",defaultValue = "23" )
    private int powerSavingStartTime;

    @DatabaseField(columnName = "power_saving_end_time",defaultValue = "7" )
    private int powerSavingEndTime;


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
}
