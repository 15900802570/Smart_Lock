package com.smart.lock.transfer;


/***
 *  HTTP 响应码以及对照信息
 *
 * @author smz
 *
 */
public class HttpCodeHelper {

    public static final String SIGN_ERROR = "1";

    public static final String HTTP_TIME_OUT = "连接超时";
    public static final String RESPONSE_SUCCESS = "successful";
    public static final String RESPONSE_FAILED = "failed";
    public static final String ERROR = "-1";
    public static final String NO_PSAM = "3";
    public static final String VERSION_ERROR = "4";//软件版本错误

    public static final String AUTHENTICATION_ERROR = "400";

    public static final String SERVER_ERROR = "500";

    public static final String USER_OR_PWD_ERROR = "401";

    public static final String ILLEGAL_EQUIPMENT = "10000";

    public static final String DEV_BOUND = "10001";

    public static final String DEV_NOT_BOUND = "10002";

    public static final String NOT_AUTHORITY = "10003";

    public static final String DEVICES_ACTIVITED = "10004";

    public static final String UNLOCK_INFO = "20000";

    public static final String DEV_INFO_ALREADY_EXISTS = "20001";

    public static final String INFORMATION_NOT_EXIST = "20002";

    public static final String NOT_DEV = "30000";


    public static final String LOG_ALREADY_EXISTS = "30001";

    public static final String LOG_INVALID = "30002";

    public static final String ADD_LOG_ERROR = "30003";

    public static final String ILLEGAL_GATEWAY = "40000";
    public static final String NOT_GATEWAY = "40001";
    public static final String UNBOUND_GATEWAY = "40002";
    public static final String UNBIND_ONESELF = "40003";
    public static final String DEVICE_BIND_ONE_GATEWAY = "40004";

    public static final String HTTP_REQUEST_ERROR = "12";

    public static final String TIME_OUT = "13";

    public static final String SERVER_CLOSE = "502";

    public static String getMessage(String respCode) {
        if (RESPONSE_SUCCESS.equals(respCode)) {
            return "成功";
        } else if (AUTHENTICATION_ERROR.equals(respCode)) {
            return "请求参数错误";
        } else if (SERVER_ERROR.equals(respCode)) {
            return "服务器错误，访问方式错误";
        } else if (USER_OR_PWD_ERROR.equals(respCode)) {
            return "用户名和密码错误";
        } else if (ILLEGAL_EQUIPMENT.equals(respCode)) {
            return "非法设备";
        } else if (DEV_BOUND.equals(respCode)) {
            return "设备已绑定，请确认设备ID重新绑定";
        } else if (DEV_NOT_BOUND.equals(respCode)) {
            return "该设备未绑定";
        } else if (NOT_AUTHORITY.equals(respCode)) {
            return "权限不够,无法操作该设备";
        } else if (DEV_INFO_ALREADY_EXISTS.equals(respCode)) {
            return "该信息已存在，请确认在发送";
        } else if (NOT_DEV.equals(respCode)) {
            return "无该设备，设备key与设备ID不匹配";
        } else if (LOG_ALREADY_EXISTS.equals(respCode)) {
            return "Log已存在";
        } else if (LOG_INVALID.equals(respCode)) {
            return "Log无效";
        } else if (ADD_LOG_ERROR.equals(respCode)) {
            return "Log添加失败";
        } else if (SERVER_CLOSE.equals(respCode)) {
            return "服务器已关闭，请联系服务商";
        } else if (HTTP_REQUEST_ERROR.equals(respCode)) {
            return "其他请求异常错误";
        } else if (HTTP_TIME_OUT.equals(respCode)) {
            return "连接超时";
        }else if (DEVICES_ACTIVITED.equals(respCode)) {
            return "设备已激活！";
        }else if (UNLOCK_INFO.equals(respCode)) {
            return "开锁信息不存在！";
        }else if (ILLEGAL_GATEWAY.equals(respCode)) {
            return "非法网关！";
        }else if (NOT_GATEWAY.equals(respCode)) {
            return "非网关设备";
        }else if (UNBOUND_GATEWAY.equals(respCode)) {
            return "网关与设备未绑定";
        }else if (UNBIND_ONESELF.equals(respCode)) {
            return "不能与自己绑定";
        }else if (DEVICE_BIND_ONE_GATEWAY.equals(respCode)) {
            return "同一个设备只能绑定一个网关";
        }

        return "未知错误";
    }
}