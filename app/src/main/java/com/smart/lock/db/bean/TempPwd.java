package com.smart.lock.db.bean;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

@DatabaseTable(tableName = "tb_temp_pwd")
public class TempPwd implements Serializable {

    @DatabaseField(generatedId = true, columnName = "_id")
    private int id;

    @DatabaseField(columnName = "device_nodeId")
    private String deviceNodeId;

    @DatabaseField(columnName = "temp_pwd_user",defaultValue = "User")
    private String tempPwdUser;

    @DatabaseField(columnName = "temp_pwd")
    private String tempPwd;

    @DatabaseField(columnName = "pwd_create_time")
    private  long pwdCreateTime;

    @DatabaseField(columnName = "pwd_random_num")
    private int randomNum;

    public void setId(int id) {
        this.id = id;
    }

    public int getId(){
        return id;
    }

    public void setTempPwdUser(String tempPwdUser) {
        this.tempPwdUser = tempPwdUser;
    }

    public String getTempPwdUser() {
        return tempPwdUser;
    }

    public void setTempPwd(String tempPwd) {
        this.tempPwd = tempPwd;
    }

    public String getTempPwd() {
        return tempPwd;
    }

    public long getPwdCreateTime() {
        return pwdCreateTime;
    }

    public void setPwdCreateTime(long pwdCreateTime) {
        this.pwdCreateTime = pwdCreateTime;
    }

    public String getDeviceNodeId() {
        return deviceNodeId;
    }

    public void setDeviceNodeId(String deviceNodeId) {
        this.deviceNodeId = deviceNodeId;
    }

    public int getRandomNum() {
        return randomNum;
    }

    public void setRandomNum(int randomNum) {
        this.randomNum = randomNum;
    }
    
    @Override
    public String toString(){
        return "TempPwd{"+
                "id="+id+
                ",tempPwdUser='"+tempPwdUser+'\''+
                ",tempPwd='"+tempPwd+'\''+
                ",pwdCreateTime'"+pwdCreateTime+
                "}";
    }
}
