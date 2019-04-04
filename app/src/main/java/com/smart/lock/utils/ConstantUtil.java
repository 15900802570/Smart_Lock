package com.smart.lock.utils;

public class ConstantUtil {

    public static String BASE_URL = "https://api.dttsh.cn";
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
     * SharedPreferences的文件名
     */
    public static final String PREF_NAME = "numberlock";

    public static final String TYPE = "type";

    public static final String IS_RETURN = "is_return";

    public static final String NOT_CANCEL = "not_cancel";

    public static final String CONFIRM = "confirm";

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
    public static final int DEVICE_MASTER = 0x01;

    /**
     * 设备成员
     */
    public static final int DEVICE_MEMBER = 0x02;

    /**
     * 临时成员
     */
    public static final int DEVICE_TEMP = 0x03;


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
     * 用户卡片
     */
    public static final byte USER_REMOTE = 0x02;

    public static final String APPLICATION = "apk";
    public static final String BIN_EXTENSION = "bin";

    public interface ParamName {
        String RESULT = "result";
        String RESD_CODE = "respCode";
        String RESD_DESC = "respDesc";
        String FILENAME = "filename";
        String DEVICE_SN = "deviceSn";
        String EXTENSION = "extension";
    }

}
