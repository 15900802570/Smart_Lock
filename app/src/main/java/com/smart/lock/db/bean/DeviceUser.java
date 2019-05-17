
package com.smart.lock.db.bean;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

@DatabaseTable(tableName = "tb_device_user")
public class DeviceUser implements Serializable {

    @DatabaseField(generatedId = true, columnName = "_id")
    private int id;

    @DatabaseField(columnName = "dev_node_id")
    private String devNodeId;

    @DatabaseField(columnName = "user_id")
    private short userId;

    @DatabaseField(columnName = "user_permission")
    private byte userPermission;

    @DatabaseField(columnName = "user_status")
    private int userStatus;

    @DatabaseField(columnName = "create_time")
    private long createTime;

    @DatabaseField(columnName = "lc_begin") //生命周期开始
    private String lcBegin;

    @DatabaseField(columnName = "lc_end") //生命周期结束
    private String lcEnd;

    @DatabaseField(columnName = "st_ts_begin") //第一开锁开始时段
    private String stTsBegin;

    @DatabaseField(columnName = "st_ts_end")//第一开锁结束时段
    private String stTsEnd;

    @DatabaseField(columnName = "nd_ts_begin")//第二开锁开始时段
    private String ndTsBegin;

    @DatabaseField(columnName = "nd_ts_end")//第二开锁结束时段
    private String ndTsend;

    @DatabaseField(columnName = "th_ts_begin")//第三开锁开始时段
    private String thTsBegin;

    @DatabaseField(columnName = "th_ts_end")//第三开锁结束时段
    private String thTsEnd;

    @DatabaseField(columnName = "user_name")
    private String userName;

    @DatabaseField(columnName = "qr_path")
    private String qrPath;

    @DatabaseField(columnName = "auth_code")
    private String authCode;

    public String getLcBegin() {
        return lcBegin;
    }

    public void setLcBegin(String lcBegin) {
        this.lcBegin = lcBegin;
    }

    public String getLcEnd() {
        return lcEnd;
    }

    public void setLcEnd(String lcEnd) {
        this.lcEnd = lcEnd;
    }

    public String getStTsBegin() {
        return stTsBegin;
    }

    public void setStTsBegin(String stTsBegin) {
        this.stTsBegin = stTsBegin;
    }

    public String getStTsEnd() {
        return stTsEnd;
    }

    public void setStTsEnd(String stTsEnd) {
        this.stTsEnd = stTsEnd;
    }

    public String getNdTsBegin() {
        return ndTsBegin;
    }

    public void setNdTsBegin(String ndTsBegin) {
        this.ndTsBegin = ndTsBegin;
    }

    public String getNdTsend() {
        return ndTsend;
    }

    public void setNdTsend(String ndTsend) {
        this.ndTsend = ndTsend;
    }

    public String getThTsBegin() {
        return thTsBegin;
    }

    public void setThTsBegin(String thTsBegin) {
        this.thTsBegin = thTsBegin;
    }

    public String getThTsEnd() {
        return thTsEnd;
    }

    public void setThTsEnd(String thTsEnd) {
        this.thTsEnd = thTsEnd;
    }

    public String getQrPath() {
        return qrPath;
    }

    public void setQrPath(String qrPath) {
        this.qrPath = qrPath;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDevNodeId() {
        return devNodeId;
    }

    public void setDevNodeId(String devNodeId) {
        this.devNodeId = devNodeId;
    }

    public short getUserId() {
        return userId;
    }

    public void setUserId(short userId) {
        this.userId = userId;
    }

    public byte getUserPermission() {
        return userPermission;
    }

    public void setUserPermission(byte userPermission) {
        this.userPermission = userPermission;
    }

    public int getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(int userStatus) {
        this.userStatus = userStatus;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "DeviceUser{" +
                "id=" + id +
                ", devNodeId='" + devNodeId + '\'' +
                ", userId=" + userId +
                ", userPermission=" + userPermission +
                ", userStatus=" + userStatus +
                ", createTime=" + createTime +
                ", lcBegin='" + lcBegin + '\'' +
                ", lcEnd='" + lcEnd + '\'' +
                ", stTsBegin='" + stTsBegin + '\'' +
                ", stTsEnd='" + stTsEnd + '\'' +
                ", ndTsBegin='" + ndTsBegin + '\'' +
                ", ndTsend='" + ndTsend + '\'' +
                ", thTsBegin='" + thTsBegin + '\'' +
                ", thTsEnd='" + thTsEnd + '\'' +
                ", userName='" + userName + '\'' +
                ", qrPath='" + qrPath + '\'' +
                ", authCode='" + authCode + '\'' +
                '}';
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

}
