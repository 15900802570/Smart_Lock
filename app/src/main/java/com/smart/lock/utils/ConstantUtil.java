package com.smart.lock.utils;

import android.util.ArrayMap;

import java.util.HashSet;
import java.util.Set;

public class ConstantUtil {

    //    public static String BASE_URL = "https://api.dttsh.cn";
    public static String BASE_URL = "http://118.31.62.126:8093";
    public static final String CHECK_FIRMWARE_VERSION = BASE_URL + "/api/v1.0/firmware/update";
    public static final String CHECK_APP_VERSION = BASE_URL + "/api/v1.0/application/update";
    /**
     * 是否开启密码验证
     */
    public static final String NUM_PWD_CHECK = "isNumPwdCheck";

    /**
     * 是否开启指纹验证
     */
    public static final String FINGERPRINT_CHECK = "isFingerprintCheck";

    /**
     * 是否自动测试
     */
    public static final String OPEN_TEST = "openTest";

    /**
     * 系统太低不支持指纹
     */
    public static final int FP_LOW_VERSION = 0;

    /**
     * 手机不支持指纹
     */
    public static final int FP_NO_HARDWARE = 1;

    /**
     * 未设置锁屏，需要设置锁屏并添加指纹
     */
    public static final int FP_NO_KEYGUARDSECURE = 2;

    /**
     * 系统中没有指纹
     */
    public static final int FP_NO_FINGERPRINT = 3;

    /**
     * 支持指纹
     */
    public static final int FP_SUPPORT = 4;

    /**
     * 静态字段
     */
    public static final String NUM_PWD = "NumPwd";

    /**
     * 检测输入密码次数
     */
    public static final String NUM_COUNTER = "NumCounter";

    /**
     * 错误计时
     */
    public static final String ERROR_TIME = "ErrorTime";

    /**
     * 初始设置密码
     */
    public static final int SETTING_PASSWORD = 0;

    /**
     * 确认密码
     */
    public static final int SURE_SETTING_PASSWORD = 2;

    /**
     * 验证登录密码
     */
    public static final int LOGIN_PASSWORD = 1;

    /**
     * 修改密码
     */
    public static final int MODIFY_PASSWORD = 3;

    /**
     * 扫描请求参数
     */
    public static final int SCAN_QRCODE_REQUEST_CODE = 0;

    /**
     * 设置密码请求参数
     */
    public static final int SETTING_PWD_REQUEST_CODE = 1;

    /**
     * SharedPreferences的文件名
     */
    public static final String PREF_NAME = "numberlock";

    public static final String TYPE = "type";

    public static final String IS_RETURN = "is_return";

    public static final String NOT_CANCEL = "not_cancel";

    public static final String CONFIRM = "confirm";

    public static final String SERIALIZABLE_FP_VERSION_MODEL = "fpVersionModel";

    public static final String SERIALIZABLE_DEV_VERSION_MODEL = "devVersionModel";

    public static final String LOCK_DIR_NAME = "SmartLock_DT"; //门锁文件夹

    public static final String APP_DIR_NAME = "app"; //app下载文件夹

    public static final String DEV_DIR_NAME = "device"; //设备下载文件夹

    public static final String QR_DIR_NAME = "qr"; //二维码下载文件夹

    public static final String ICON_DIR_NAME = "icon"; //头像下载文件夹


    /**
     * 默认设备标识
     */
    public static final String DEFAULT_DEVICE = "default";

    /**
     * 智能门锁
     */
    public static final String SMART_LOCK = "smart_lock";

    /**
     * 智能门锁默认名称
     */
    public static final String LOCK_DEFAULT_NAME = "DTLOCKER";

    /**
     * 门锁软件版本
     */
    public static final String OTA_LOCK_SW_VERSION = "lockSwVersion";
    /**
     * 门锁软件版本
     */
    public static final String OTA_FP_SW_VERSION = "fingerprintSwVersion";

    /**
     * 修改
     */
    public static final String MODIFY = "modify";

    /**
     * 创建
     */
    public static final String CREATE = "create";

    /**
     * 设备管理员
     */
    public static final byte DEVICE_MASTER = 0x01;

    /**
     * 设备成员
     */
    public static final byte DEVICE_MEMBER = 0x02;

    /**
     * 临时成员
     */
    public static final byte DEVICE_TEMP = 0x03;


    /**
     * 用户状态 -未启用 -0
     */
    public static final int USER_UNENABLE = 0x00;

    /**
     * 用户状态 -启用 -1
     */
    public static final int USER_ENABLE = 0x01;

    /**
     * 用户状态 -暂停 -2
     */
    public static final int USER_PAUSE = 0x02;


    /**
     * 用户密码
     */
    public static final byte USER_PWD = 0x0;

    /**
     * 用户指纹
     */
    public static final byte USER_FINGERPRINT = 0x01;

    /**
     * 用户卡片
     */
    public static final byte USER_NFC = 0x02;

    /**
     * 远程开锁
     */
    public static final byte USER_REMOTE = 0x07;

    /**
     * 临时密码开锁
     */
    public static final byte USER_TEMP_PWD = 0x08;

    /**
     * 组合开锁
     */
    public static final byte USER_COMBINATION_LOCK = 0x09;

    /**
     *
     */
    public static final int INVALID_POWER_SAVE_TIME = Integer.MAX_VALUE;

    public static final String APPLICATION = "apk";
    public static final String BIN_EXTENSION = "bin";

    public static final int ADMIN_USR_NUM = 5; //管理员个数
    public static final int COMMON_USR_NUM = 90;//普通用户个数
    public static final int TMP_USR_NUM = 5;//临时用户个数

    public interface ParamName {
        String RESULT = "result";
        String RESD_CODE = "respCode";
        String RESD_DESC = "respDesc";
        String FILENAME = "filename";
        String DEVICE_SN = "deviceSn";
        String EXTENSION = "extension";
        String DEV_CUR_VER = "devCurVer";
        String FINGERPRINT = "fingerprint";
        String FP_TYPE = "fpType";
        String FP_CUR_VER = "fpCurVer";
        String FP_CUR_ZONE = "fpCurZone";
    }

    public static final String[] HOUR = {
            "00", "01", "02", "03", "04", "05", "06", "07", "08", "09",
            "10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
            "20", "21", "22", "23"
    };
    public static final String[] MINUTE = {
            "00", "01", "02", "03", "04", "05", "06", "07", "08", "09",
            "10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
            "20", "21", "22", "23", "24", "25", "26", "27", "28", "29",
            "30", "31", "32", "33", "34", "35", "36", "37", "38", "39",
            "40", "41", "42", "43", "44", "45", "46", "47", "48", "49",
            "50", "51", "52", "53", "54", "55", "56", "57", "58", "59"
    };
    public static final String[] NUMBER_100 = {
            "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "0A", "0B", "0C", "0D", "0E", "0F",
            "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "1A", "1B", "1C", "1D", "1E", "1F",
            "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "2A", "2B", "2C", "2D", "2E", "2F",
            "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "3A", "3B", "3C", "3D", "3E", "3F",
            "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "4A", "4B", "4C", "4D", "4E", "4F",
            "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "5A", "5B", "5C", "5D", "5E", "5F",
            "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "6A", "6B", "6C", "6D", "6E", "6F",
    };

    public static final String[] TEMP_PWD_PERIOD = {
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
            "11", "12", "13", "14", "15", "16", "17", "18", "19",
            "20", "21", "22", "23", "24"
    };
}
