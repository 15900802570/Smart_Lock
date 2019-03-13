package com.smart.lock.permission;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.smart.lock.utils.LogUtil;

import java.security.Permission;

import static android.content.ContentValues.TAG;

public class PermissionHelper {
    private final String TAG = PermissionHelper.class.getSimpleName();

    private Activity mActivity;
    private PermissionInterface mPermissionInterface;
    private String mPermission;
    private int mCallBackCode;


    public PermissionHelper(Activity activity, PermissionInterface permissionInterface) {
        mActivity = activity;
        mPermissionInterface = permissionInterface;
    }

    /**
     * 弹出对话框请求权限
     *
     * @param permissions
     * @param requestCode
     */
    public void requestPermissions(String[] permissions, int requestCode) {
        mCallBackCode = requestCode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                if (hasPermission(mActivity, permission)) {
                    mPermissionInterface.requestPermissionsSuccess(mCallBackCode);
                } else {
                    ActivityCompat.requestPermissions(mActivity, new String[]{permission}, mCallBackCode);
                }
            }

        }
    }

    /**
     * 判断是否有某个权限
     *
     * @param context
     * @param permission
     * @return
     */
    public  boolean hasPermission(Context context, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6.0判断，6.0以下跳过。在清单文件注册即可，不用动态请求，这里直接视为有权限
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 请求权限
     *
     * @param permission   权限名字
     * @param callBackCode 回调code
     */
    public void requestPermissions(String permission, int callBackCode) {
        mPermission = permission;
        mCallBackCode = callBackCode;
        if (hasPermission(mActivity, permission)) {
            mPermissionInterface.requestPermissionsSuccess(callBackCode);
        } else {
            ActivityCompat.requestPermissions(mActivity, new String[]{permission}, callBackCode);
        }

    }

    /**
     * 在Activity中的onRequestPermissionsResult中调用,用来接收结果判断
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    public void requestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == mCallBackCode) {
            for (int result : grantResults) {
                LogUtil.d(TAG, "result = " + result);
                if (result == PackageManager.PERMISSION_GRANTED) {
                    mPermissionInterface.requestPermissionsSuccess(mCallBackCode);
                } else {
                    mPermissionInterface.requestPermissionsFail(mCallBackCode);
                }
            }
        }
    }


}
