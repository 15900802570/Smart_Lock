
package com.smart.lock.model;

/**
 */
public class VersionModel {

    // 拓展TYPE，多版本类型下载服务
    public String type;
    // apk名称
    public String appName;
    // 内部版本号
    public int versionCode;
    // 无需更新 ，可选更新 ，必须更新
    public String unUpdateCode;

    // 强制更新
    public boolean forceUpdate;
    // 用户版本号
    public String versionName;
    // 下载路径
    public String path;
    // 下载附带消息，可用于版本更新说明
    public String msg;
    //更新时间
    public String updateDate;
    //版本拓展名
    public String extension;
    //下载校验
    public String md5;

}
