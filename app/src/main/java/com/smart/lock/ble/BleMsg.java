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
     * 省电模式
     */
    public static final String KEY_POWER_SAVE = "powerSave";

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
    //用户授权码
    public static final String KEY_AUTH_CODE = "authCode";

    /**
     * 设置默认页
     */
    public static final String KEY_CURRENT_ITEM = "currentItem";
    //连接方式
    public static final String KEY_BLE_CONNECT_TYPE = "bleConnectType";


    /**
     * MSG 2E ERRCODE
     */
    public static final byte TYPE_REMOTE_UNLOCK_SUCCESS = 0x00; //远程开锁成功
    public static final byte TYPE_RAND_CODE_UPDARE_SUCCESS = 0x01; //动态码更新成功
    public static final byte TYPE_NO_AUTHORITY = 0x02; //无权限
    public static final byte TYPE_TEMP_USER_LIFE_UPDATE_SUCCESS = 0x03; //无权限
    public static final byte TYPE_SET_BROADCAST_NAME_SUCCESS = 0x04; //配置广播名成功
    public static final byte TYPE_SET_BROADCAST_NAME_FAILED = 0x05; //配置广播名失败
    public static final byte TYPE_SET_POWER_SAVE_SUCCESS = 0x06; //配置省电时段成功
    public static final byte TYPE_SET_POWER_SAVE_FAILED = 0x07; //配置省电时段失败


    /**
     * MSG 11 TYPE
     */
    public static final byte TYPE_SCAN_QR_ADD_MASTER = 0x00; //扫描绑定增加管理员，此时锁侧提示用户按键后才会增加管理员，如果超时没有按键则通过MSG1E返回失败码
    public static final byte TYPT_CONNECT_ADD_MASTER = 0x01; //普通安全连接增加管理员
    public static final byte TYPT_CONNECT_ADD_MUMBER = 0x02; //普通安全连接增加普通用户
    public static final byte TYPT_CONNECT_ADD_TEMP = 0x03; //普通安全连接增加临时用户
    public static final byte TYPT_DELETE_USER = 0x04; //删除用户
    public static final byte TYPT_PAUSE_USER = 0x05; //暂停用户
    public static final byte TYPT_RECOVERY_USER = 0x06; //重新启动用户
    public static final byte TYPT_NO_SCAN_QR_ADD_USER = 0x07; //无扫描增加管理员
    public static final byte TYPT_CANCEL_SCAN_QR = 0x08; //取消扫描

    /**
     * MSG 1E ERRCODE
     */
    public static final byte TYPE_PAUSE_USER_SUCCESS = 0x00; //暂停用户成功
    public static final byte TYPE_PAUSE_USER_FAILED = 0x01; //暂停用户失败
    public static final byte TYPE_ADD_USER_SUCCESS = 0x02; //新增用户成功
    public static final byte TYPE_ADD_USER_FAILED = 0x03; //新增用户失败
    public static final byte TYPE_DELETE_USER_SUCCESS = 0x04; //删除用户成功
    public static final byte TYPE_DELETE_USER_FAILED = 0x05; //删除用户失败
    public static final byte TYPE_RECOVERY_USER_SUCCESS = 0x06; //启用用户成功
    public static final byte TYPE_RECOVERY_USER_FAILED = 0x07; //启用用户失败
    public static final byte TYPE_ENTER_OR_MODIFY_FP_FAILED = 0x08; //录入/修改指纹失败
    public static final byte TYPE_MODIFY_FP_SUCCESS = 0x09; //修改指纹成功
    public static final byte TYPE_DELETE_FP_SUCCESS = 0x0a; //删除指纹成功
    public static final byte TYPE_ENTER_PASSWORD_FAILED = 0x0b; //录入密码失败
    public static final byte TYPE_ENTER_OR_MODIFY_PASSWORD_SUCCESS = 0x0c; //密码录入/修改成功
    public static final byte TYPE_DELETE_PASSWORD_SUCCESS = 0x0d; //删除密码成功
    public static final byte TYPE_ENTER_OR_MODIFY_NFC_FAILED = 0x0e; //录入/修改NFC卡失败
    public static final byte TYPE_MODIFY_NFC_SUCCESS = 0x0f; //修改NFC卡成功
    public static final byte TYPE_DELETE_NFC_SUCCESS = 0x10; //删除NFC卡成功
    public static final byte TYPE_GROUP_DELETE_USER_SUCCESS = 0x11; //群删用户成功
    public static final byte TYPE_GROUP_DELETE_USER_FAILED = 0x12; //群删用户失败
    public static final byte TYPE_GROUP_DELETE_KEY_SUCCESS = 0x13; //群删密钥成功
    public static final byte TYPE_GROUP_DELETE_KEY_FAILED = 0x14; //群删密钥失败
    public static final byte TYPE_SET_COMBINATION_UNLOCK_SUCCESS = 0x15; //组合开锁功能设置成功
    public static final byte TYPE_SET_COMBINATION_UNLOCK_FAILED = 0x16; //组合开锁功能设置失败
    public static final byte TYPE_KEEP_UNLOCK_SUCCESS = 0x17; //常开功能设置成功
    public static final byte TYPE_KEEP_UNLOCK_FAILED = 0x18; //常开功能设置失败
    public static final byte TYPE_SET_VOICE_PROMPT_SUCCESS = 0x19; //语音提示设置成功
    public static final byte TYPE_SET_VOICE_PROMPT_FAILED = 0x1a; //语音提示设置失败
    public static final byte TYPE_SET_LOCK_CORE_SUCCESS = 0x1b; //智能锁芯设置成功
    public static final byte TYPE_SET_LOCK_CORE_FAILED = 0x1c; //智能锁芯设置失败
    public static final byte TYPE_SET_ANTI_PRYING_ALARM_SUCCESS = 0x1d; //防撬报警设置成功
    public static final byte TYPE_SET_ANTI_PRYING_ALARM_FAILED = 0x1e; //防撬报警设置失败
    public static final byte TYPE_SET_TEMP_USER_LIFE_SUCCESS = 0x1f; //设置临时用户有效期成功
    public static final byte TYPE_SET_TEMP_USER_LIFE_FAILED = 0x20; //回锁时间设置成功
    public static final byte TYPE_NO_AUTHORITY_1E = 0x21; //无权限，对于一些需管理员权限才能操作的设置返回的错误
    public static final byte TYPE_RESTORE_FACTORY_SETTINGS_SUCCESS = 0x22; //恢复出厂设置成功
    public static final byte TYPE_DELETE_FP_FAILED = 0x23; //删除指纹失败
    public static final byte TYPE_FP_FULL = 0x24; //指纹已满
    public static final byte TYPE_EQUIPMENT_BUSY = 0x25; //设备忙
    public static final byte TYPE_USER_FULL = 0x2B; //用户已满
    public static final byte TYPE_FINGERPRINT_EXIST = 0x2A; //指纹已存在
    public static final byte TYPE_LONG_TIME_NO_DATA = 0x2C; //长时间无数据交互
    public static final byte TYPE_ALLOW_OTA_UPDATE = 0x2D; //允许ota升级
    public static final byte TYPE_REFUSE_OTA_UPDATE = 0x2E; //拒绝ota升级

    public static final int SCAN_DEV_FIALED = 100;//未搜索到设备
    public static final int STATE_DISCONNECTED = 101;//连接中断
    public static final int REGISTER_SUCCESS = 102; //注册成功
    public static final int DISPACTH_MSG_3E = 103; //分发3E
    public static final int RECEIVER_LOGS = 104; //接受log
    public static final int STATE_CONNECTED = 105; //连接成功
    public static final int GATT_SERVICES_DISCOVERED = 106; //发现服务
    public static final int USER_PAUSE = 107; //USER pause

    /**
     * msg 13 type
     */
    public static final byte TYPE_DELETE_MASTER_USER = 0x00; //管理员，除了自己
    public static final byte TYPE_DELETE_MUMBER_USER = 0x01; //普通用户
    public static final byte TYPE_DELETE_TEMP_USER = 0x02; //临时用户
    public static final byte TYPE_DELETE_ALL_USER = 0x03; //所有用户，除了管理员自己

    /**
     * msg 15 type
     */
    public static final byte TYPE_PASSWORD = 0x00; //密码
    public static final byte TYPE_FINGERPRINT = 0x01; //指纹
    public static final byte TYPE_CARD = 0x02; //NFC卡

    /**
     * msg 15 CMD
     */
    public static final byte CMD_TYPE_CREATE = 0x00; //新增
    public static final byte CMD_TYPE_DELETE = 0x01; //删除
    public static final byte CMD_TYPE_MODIFY = 0x02; //修改
    public static final byte CMD_TYPE_CANCEL_CREATE = 0x03; //取消录入

    /**
     * msg 17 type
     */
    public static final byte TYPE_DELETE_USER_PASSWORD = 0x00;//密码
    public static final byte TYPE_DELETE_USER_FINGERPRINT = 0x01;//指纹
    public static final byte TYPE_DELETE_USER_CARD = 0x02;// NFC
    public static final byte TYPE_DELETE_USER_ALL_KEY = 0x03;//所有 该情况此用户也将被删除，故应使用删除用户的消息
    public static final byte TYPE_DELETE_OTHER_USER_PASSWORD = 0x04;//删除所有其他用户密码
    public static final byte TYPE_DELETE_OTHER_USER_FINGERPRINT = 0x05;//删除所有其他用户指纹
    public static final byte TYPE_DELETE_OTHER_USER_CARD = 0x06;//删除所有其他用户NFC


    /**
     * MSG 1B type
     */
    public static final byte TYPE_SET_USER_ONE_UNLOCK_TIME = 0x01; //设置一个时间段
    public static final byte TYPE_SET_USER_TWO_UNLOCK_TIME = 0x02; //设置两个时间段
    public static final byte TYPE_SET_USER_THREE_UNLOCK_TIME = 0x03; //设置三个时间段

    /**
     * MSG 31 type
     */
    public static final byte TYPE_QUERY_USER_LOG = 0x00; //查询某个用户日志
    public static final byte TYPE_QUERY_ALL_USERS_LOG = 0x01; //查询所有用户日志，仅管理员有效，否则返回MSG3E错误

    /**
     * MSG 3E type
     */
    public static final byte TYPE_RECEIVER_LOGS_OVER = 0x00; //log传输完毕，包括无log及log传输完成
    public static final byte TYPE_DELETE_LOG_SUCCESS = 0x01; //删除log成功
    public static final byte TYPE_DELETE_LOG_FAILED = 0x02; //删除log失败
    public static final byte TYPE_NO_AUTHORITY_3E = 0x03;  //无权限

    /**
     * MSG 33 type
     */
    public static final byte TYPE_DELETE_ALL_LOGS = 0x00; //删除所有日志
    public static final byte TYPE_DELETE_USER_LOGS = 0x01; //按USR_ID删除所有日志
    public static final byte TYPE_DELETE_LOG = 0x02; //删除单条日志

    /**
     * msg 19 type
     */
    public static final byte TYPE_DETECTION_LOCK_EQUIPMENT = 0x00;//开锁设备检测，通过MSG1A返回检测结果给APK
    public static final byte TYPE_ENABLE_COMBINATION_UNLOCK = 0x01;//组合开锁功能启用
    public static final byte TYPE_UNENABLE_COMBINATION_UNLOCK = 0x02;//组合开锁功能关闭
    public static final byte TYPE_NORMALLY_OPEN = 0x03;//常开功能启用
    public static final byte TYPE_NORMALLY_CLOSE = 0x04;//常开功能关闭
    public static final byte TYPE_VOICE_PROMPT_OPEN = 0x05;//语音提示开启
    public static final byte TYPE_VOICE_PROMPT_CLOSE = 0x06;//语音提示关闭
    public static final byte TYPE_CHECK_VERSION = 0x07;//版本信息查询，通过MSG1C返回给APK
    public static final byte TYPE_RESTORE_FACTORY_SETTINGS = 0x08;//恢复出厂设置 成功后智能锁重启，无回复消息
    public static final byte TYPE_OTA_UPDATE = 0x09;//OTA升级
    public static final byte TYPE_POWER_QUERY = 0x0a;//电量查询，通过MSG1A返回检测结果给APK
    public static final byte TYPE_RESET_SYSTEM = 0x0b;//系统复位，无回复
    public static final byte TYPE_INTELLIGENT_LOCK_CORE_OPEN = 0x0c;//智能锁芯开启
    public static final byte TYPE_INTELLIGENT_LOCK_CORE_CLOSE = 0x0d;//智能锁芯关闭
    public static final byte TYPE_ANTI_PRYING_ALARM_OPEN = 0x0e;//防撬报警开启
    public static final byte TYPE_ANTI_PRYING_ALARM_CLOSE = 0x0f;//防撬报警关闭
    public static final byte TYPE_LOCK_LOG_ENABLE = 0x12;//log打印开启
    public static final byte TYPE_LOCK_LOG_UNENABLE = 0x13;//log打印关闭
    public static final byte TYPE_EXIT_OTA_UPDATE = 0x14;//退出ota升级

    /**
     * msg 0E type
     */
    public static final byte TYPE_RAND_ERROR = 0x01; //随机数校验失败
    public static final byte TYPE_USER_NOT_EXIST = 0x02; //用户不存在
    public static final byte ERR0E_NO_AUTHORITY = 0X03; //无权限
    public static final byte TYPE_DEVICE_BUSY = 0x04; //设备忙
    public static final byte TYPE_AUTH_CODE_ERROR = 0x05; //鉴权码错误
}
