
package com.smart.lock.db.bean;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

@DatabaseTable(tableName = "tb_device_user")
public class DeviveUser implements Serializable {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(columnName = "dev_node_id")
    public String devNodeId;

    @DatabaseField(columnName = "user_id")
    public String userId;

    @DatabaseField(columnName = "user_permission")
    public String userPermission;

    @DatabaseField(columnName = "user_status")
    public String userStatus;

    @DatabaseField(columnName = "create_time")
    public long createTime;

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

    public String getUserPermission() {
        return userPermission;
    }

    public void setUserPermission(String userPermission) {
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
        return "DeviveUser{" +
                "id=" + id +
                ", devNodeId='" + devNodeId + '\'' +
                ", userId='" + userId + '\'' +
                ", userPermission='" + userPermission + '\'' +
                ", userStatus='" + userStatus + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
