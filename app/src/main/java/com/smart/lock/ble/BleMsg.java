package com.smart.lock.ble;


/**
 * Ble Message Class.
 */
public class BleMsg {
    /*BLE event.*/
    public final static String ACTION_GATT_CONNECTED = "com.datang.uart.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.datang.uart.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.datang.uart.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.datang.uart.ACTION_DATA_AVAILABLE";
    public final static String ACTION_DOES_NOT_SUPPORT_UART = "com.datang.uart.DEVICE_DOES_NOT_SUPPORT_UART";
    public final static String ACTION_CHARACTERISTIC_WRITE = "com.datang.uart.ACTION_CHARACTERISTIC_WRITE";

    public final static String EXTRA_DATA_BYTE = "com.datang.uart.EXTRA_DATA_BYTE";
    public final static String EXTRA_DATA_INT = "com.datang.uart.EXTRA_DATA_INT";
    public final static String EXTRA_DATA_MSG_02 = "com.datang.uart.EXTRA_DATA_MSG_02";
    public final static String EXTRA_DATA_MSG_04 = "com.datang.uart.EXTRA_DATA_MSG_04";
    /**
     * MSG 12是智能锁回复给APK的带有授权码的信息
     */
    public final static String EXTRA_DATA_MSG_12 = "com.datang.uart.EXTRA_DATA_MSG_12";

    public static final int ID_EVENT_SEND_DATA = 0x100;
    public static final int ID_EVENT_RECV_DATA = 0x200;

    /*Ble message ID & RSP.*/
    public static final int INT_TAG_CMD_START_SHAKE = 0x01;
    public static final int INT_TAG_CMD_BATTERY_LEVEL = 0x02;
    public static final int INT_TAG_CMD_SET_TIMEOUT = 0x03;
    public static final int INT_TAG_CMD_CHECK_CARD = 0x04;
    public static final int INT_TAG_CMD_READ_CARD = 0x05;
    public static final int INT_TAG_CMD_WRITE_CARD = 0x06;
    public static final int INT_TAG_CMD_RESET = 0x07;
    public static final int INT_TAG_CMD_CLOSE = 0x08;
    public static final int INT_TAG_CMD_POWEROFF = 0x09;
    public static final int INT_TAG_CMD_TRANSIMIT = 0x10;

    public static final String STR_RSP_BATTERY_LEVEL = "com.datang.uart.rsp.BATTERY_LEVEL";
    public static final String STR_RSP_SET_TIMEOUT = "com.datang.uart.rsp.SET_TIMEOUT";
    public static final String STR_RSP_CHECK_CARD = "com.datang.uart.rsp.CHECK_CARD";
    public static final String STR_RSP_READ_CARD = "com.datang.uart.rsp.READ_CARD";
    public static final String STR_RSP_WRITE_CARD = "com.datang.uart.rsp.WRITE_CARD";
    public static final String STR_RSP_RESET = "com.datang.uart.rsp.RESET";
    public static final String STR_RSP_CLOSE = "com.datang.uart.rsp.CLOSE";
    public static final String STR_RSP_POWEROFF = "com.datang.uart.rsp.POWEROFF";
    public static final String STR_RSP_TRANSIMIT = "com.datang.uart.rsp.TRANSIMIT";
    public static final String STR_RSP_SERVER_DATA = "com.datang.uart.rsp.SERVER_DATA";

    public static final String STR_RSP_SCANED = "com.datang.uart.rsp.SCANED";

    public static final String STR_RSP_SECURE_CONNECTION = "com.datang.uart.rsp.SECURE_CONNECTION";
    public static final String STR_RSP_SECURE_CONNECTION_OTA = "com.datang.uart.rsp.SECURE_CONNECTION_OTA";


    public static final String STR_RSP_OPEN_TEST = "com.datang.uart.rsp.OPEN_TEST";

    public static final String STR_RSP_OTA_MODE = "com.datang.uart.rsp.OTA_MODE";

    public static final String OTA_SEND_CMD = "OTA_SEND_CMD";

    public static final String OTA_SEND_DATA = "OTA_SEND_DATA";


    public static final int INT_DEFAULT_TIMEOUT = 10;

    /**
     * 设备->APK，IK校验失败
     */
    public static final String STR_RSP_IK_ERR = "com.datang.uart.rsp.IK_ERR";

    /**
     * 设备->APK ,连接错误码
     */
    public static final String STR_RSP_MSG0E_ERRCODE = "com.datang.uart.rsp.MSG0E_ERRCODE";

    /**
     * 设备->APK，校验类型
     */
    public static final String STR_RSP_MSG1E_ERRCODE = "com.datang.uart.rsp.MSG1E_ERRCODE";
    /**
     * 设备->APK，锁体秘钥上报
     */
    public static final String STR_RSP_MSG16_LOCKID = "com.datang.uart.rsp.MSG16_LOCKID";

    /**
     * 设备->APK，智能锁将录入的锁体密钥上报给APK
     */
    public static final String STR_RSP_MSG1A_STATUS = "com.datang.uart.rsp.MSG1A_STATUS";

    /**
     * 设备->APK,智能锁将设备信息上报给APK
     */
    public static final String STR_RSP_MSG1C_VERSION = "com.datang.uart.rsp.MSG1C_VERSION";

    /**
     * 设备->APK，智能锁录入前提供给APK的超时时间
     */
    public static final String STR_RSP_MSG18_TIMEOUT = "com.datang.uart.rsp.MSG18_TIMEOUT";

    /**
     * 设备->APK，用户信息查询
     */
    public static final String STR_RSP_MSG26_USERINFO = "com.datang.uart.rsp.MSG26_USERINFO";

    /**
     * 设备->APK，智能锁对APK或网关远程开锁的命令回应。
     */
    public static final String STR_RSP_MSG2E_ERRCODE = "com.datang.uart.rsp.MSG2E_ERRCODE";

    /**
     * 设备->APK，推送日志，每次只推送一条
     */
    public static final String STR_RSP_MSG32_LOG = "com.datang.uart.rsp.MSG32_LOG";

    /**
     * 智能锁在log传输过程中上报的消息
     */
    public static final String STR_RSP_MSG3E_ERROR = "com.datang.uart.rsp.MSG3E_ERROR";

    /**
     * 消息02回应的随机数
     */
    public static final String KEY_RANDOM = "random";

    /**
     * 会话秘钥
     */
    public static final String KEY_AK = "AK";

    /**
     * 用户名
     */
    public static final String KEY_USER = "user";

    /**
     * 账户秘钥
     */
    public static final String KEY_PWD = "pwd";

    /**
     * 命令类型
     */
    public static final String KEY_CMD_TYPE = "cmdType";

    /**
     * 秘钥类型
     */
    public static final String KEY_TYPE = "keyType";

    /**
     * 设备回复的错误编码
     */
    public static final String KEY_ERROR_CODE = "errCode";

    /**
     * 锁体密钥编号
     */
    public static final String KEY_LOCK_ID = "lockId";


    /**
     * 用户信息
     */
    public static final String KEY_USER_MSG = "userMsg";

    /**
     * 智能锁本地密钥状态字
     */
    public static final String KEY_STATUS = "status";

    /**
     * 智能锁密钥录入超时时间
     */
    public static final String KEY_TIME_OUT = "timeOut";

    /**
     * 智能锁密钥最大个数
     */
    public static final String KEY_MAX_COUNT = "maxCount";

    /**
     * 设备编号
     */
    public static final String KEY_NODE_ID = "nodeId";

    /**
     * 设备蓝牙编号
     */
    public static final String KEY_BLE_MAC = "bleMac";

    /**
     * 设备老地址
     */
    public static final String KEY_OLD_MAC = "oldMAC";

    /**
     * 设备SN号
     */
    public static final String KEY_NODE_SN = "nodeSn";

    /**
     * 设备用户编号
     */
    public static final String KEY_USER_ID = "userId";
    /**
     * 设备用户类型
     */
    public static final String KEY_USER_TYPE = "userType";

    /**
     * 设备电量百分比
     */
    public static final String KEY_BAT_PERSCENT = "batPerscent";

    /**
     * 设备用户状态字，用以同步用户数
     */
    public static final String KEY_SYNC_USERS = "syncUsers";

    /**
     * 设备用户状态，用以同步用户状态
     */
    public static final String KEY_USERS_STATE = "userState";

    /**
     * 设备用户状态,1字节，0-未启用，1-启用，2-暂停，用户创建后默认未启用，创建开锁信息后才是启用状态
     */
    public static final String KEY_USER_STATUS = "userStatus";

    /**
     * 设备电设置状态字，1字节，低位第一位表示常开功能是否开启，低位第二位表示语音提示是否开启
     */
    public static final String KEY_SETTING_STATUS = "stStatus";

    /**
     * 设备回锁时间
     */
    public static final String KEY_UNLOCK_TIME = "unLockTime";

    /**
     * 临时密码加密秘钥
     */
    public static final String KEY_TMP_PWD_SK = "tmpPwdSk";

    /**
     * 设备时间
     */
    public static final String KEY_LOCK_TIME = "LockTime";

    /**
     * 设备动态码
     */
    public static final String KEY_RAND_CODE = "randomCode";

    /**
     * 默认设备
     */
    public static final String KEY_DEFAULT_DEVICE = "defaultDevice";

    /**
     * 当前临时用户
     */
    public static final String KEY_TEMP_USER = "tempUser";


    /**
     * 守护数据
     */
    public static final String KEY_SERIALIZABLE = "serializable";

    /**
     * 修改的秘钥
     */
    public static final String KEY_MODIFY_DEVICE_KEY = "modifyDeviceKey";

    /**
     * 日志信息
     */
    public static final String KEY_LOG = "log";

    /**
     * 日志编号
     */
    public static final String KEY_LOG_ID = "logId";

    /**
     * 开锁时段
     */
    public static final String KEY_UNLOCK_IMEI = "unLockTime";
    /**
     * 软件版本信息
     */
    public static final String KEY_SW_VER = "swVer";

    /**
     * 开锁时段
     */
    public static final String KEY_LIFE_CYCLE = "lefeCycle";
    /**
     * 硬件版本信息
     */
    public static final String KEY_HW_VER = "hwVER";

    /**
     * 设置默认页
     */
    public static final String KEY_CURRENT_ITEM = "currentItem";
}
