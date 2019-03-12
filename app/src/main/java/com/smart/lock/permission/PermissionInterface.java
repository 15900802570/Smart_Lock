package com.smart.lock.permission;

public interface PermissionInterface {

    /**
     * 请求权限成功回调
     */
    void requestPermissionsSuccess(int callBackCode);

    /**
     * 请求权限失败回调
     */
    void requestPermissionsFail(int callBackCode);

}
