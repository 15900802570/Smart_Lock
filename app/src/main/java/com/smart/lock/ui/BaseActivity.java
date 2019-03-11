package com.smart.lock.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

public class BaseActivity extends Activity {

    protected String mSn; //设备SN
    protected String mNodeId; //设备IMEI
    protected String mBleMac; //蓝牙地址
    protected Integer mUserType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * 转成标准MAC地址
     *
     * @param str 未加：的MAC字符串
     * @return 标准MAC字符串
     */
    protected static String getMacAdr(String str) {
        StringBuilder result = new StringBuilder("");
        for (int i = 1; i <= 12; i++) {
            result.append(str.charAt(i - 1));
            if (i % 2 == 0) {
                result.append(":");
            }
        }
        return result.substring(0, 17);
    }

    /**
     * 吐司提示
     *
     * @param msg 提示信息
     */
    protected void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 新界面
     *
     * @param cls    新Activity
     * @param bundle 数据包
     */
    protected void startIntent(Class<?> cls, Bundle bundle, int flag) {
        Intent intent = new Intent();
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        if (flag != -1)
            intent.addFlags(flag);
        intent.setClass(this, cls);
        startActivity(intent);
    }
}
