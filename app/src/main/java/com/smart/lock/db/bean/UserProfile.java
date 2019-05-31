
package com.smart.lock.db.bean;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

@DatabaseTable(tableName = "tb_user_profile")
public class UserProfile implements Serializable {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(columnName = "user_address")
    public String address;

    @DatabaseField(columnName = "user_birthday")
    public String birthday;

    @DatabaseField(columnName = "user_desc")
    public String desc;

    @DatabaseField(columnName = "user_email")
    public String email;

    @DatabaseField(columnName = "user_mobile")
    public String mobile;

    @DatabaseField(columnName = "user_sex")
    private String sex;

    @DatabaseField(columnName = "user_userName")
    public String userName;

    @DatabaseField(columnName = "user_account")
    public String user;

    @DatabaseField(columnName = "pass_word")
    public String password;

    @DatabaseField(columnName = "phote_path")
    public String photoPath;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBirthday() {
        return this.birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getSex() {
        return this.sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getMobile() {
        return this.mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "UserProfile [id=" + id + ", address=" + address + ", birthday=" + birthday
                + ", desc=" + desc + ", email=" + email + ", mobile=" + mobile + ", sex=" + sex
                + ", userName=" + userName + ", user=" + user + ", password=" + password + "]";
    }

}
