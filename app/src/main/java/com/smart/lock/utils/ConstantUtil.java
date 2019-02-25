package com.smart.lock.utils;

public class ConstantUtil {

	/**
     * 是否开启密码验证
     */
	public static final String NUM_PWD_CHECK = "isNumPwdCheck";

    /**
     * 是否开启指纹验证
     */
    public static final String FINGERPRINT_CHECK = "isFingerprintCheck";

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
     * SharedPreferences的文件名
     */
    public static final String PREF_NAME = "numberlock";

    public static final String TYPE = "type";

    public static final String IS_RETURN = "is_return";

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
    public static final String LOCK_DEFAULT_NAME = "智能门锁";
}
