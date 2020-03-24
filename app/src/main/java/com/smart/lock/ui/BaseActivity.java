package com.smart.lock.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.smart.lock.MainBaseActivity;

public class BaseActivity extends MainBaseActivity {

    protected String mSn; //设备SN
    protected String mNodeId; //设备IMEI
    protected String mBleMac; //蓝牙地址
    protected String mUserType; //用户类型
    protected String mUserId;   //用户ID
    protected String mDevSecret; //设备Secret

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
