
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
    private String userId;

    @DatabaseField(columnName = "user_permission")
    private int userPermission;

    @DatabaseField(columnName = "user_status")
    private String userStatus;

    @DatabaseField(columnName = "create_time")
    private long createTime;

    @DatabaseField(columnName = "user_name")
    private String userName;

    @DatabaseField(columnName = "qr_path")
    private String qrPath;

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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getUserPermission() {
        return userPermission;
    }

    public void setUserPermission(int userPermission) {
        this.userPermission = userPermission;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
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
                ", userId='" + userId + '\'' +
                ", userPermission='" + userPermission + '\'' +
                ", userStatus='" + userStatus + '\'' +
                ", createTime=" + createTime +
                ", userName='" + userName + '\'' +
                ", qrPath='" + qrPath + '\'' +
                '}';
    }
}
