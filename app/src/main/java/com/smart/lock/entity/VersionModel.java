
package com.smart.lock.entity;

import java.io.Serializable;

/**
 */
public class VersionModel implements Serializable {

    // 拓展TYPE，多版本类型下载服务
    public String type;
    // 文件名称
    public String fileName;
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

    //sha码用于检测文件完整性
    public String sha1;

    //文件分区,与请求zone相反
    public String zone;

    /*
    face 固件专用
     */
    public String mainVersion;
    //nCPU
    public String nCpuFilename;
    public String nCpuVersion;
    public String nCpuSHA1;
    public String nCpuPath;
    //sCPU
    public String sCpuFilename;
    public String sCpuVersion;
    public String sCpuSHA1;
    public String sCpuPath;
    //module
    public String moduleFilename;
    public String moduleVersion;
    public String moduleSHA1;
    public String modulePath;

}
