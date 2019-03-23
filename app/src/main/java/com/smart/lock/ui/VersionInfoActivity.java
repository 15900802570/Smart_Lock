package com.smart.lock.ui;

import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.smart.lock.R;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.DeviceInfo;

public class VersionInfoActivity extends AppCompatActivity {

    private String TAG = "VersionInfoActivity";
    private DeviceInfo mDefaultDevice;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_version_info);

        try {
            mDefaultDevice = (DeviceInfo) getIntent().getSerializableExtra(BleMsg.KEY_DEFAULT_DEVICE);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        initView();
        initEvent();
    }

    private void initView() {

        TextView mSerialNumTv = findViewById(R.id.tv_serial_num);
        TextView mImeiTv = findViewById(R.id.tv_imei);
        TextView mBTMacTv = findViewById(R.id.tv_bt_mac_ad);
        TextView mSoftVersionTv = findViewById(R.id.tv_software_version);
        TextView mHardVersionTv = findViewById(R.id.tv_hardware_version);

        if (mDefaultDevice.getDeviceSn() != null){
            mSerialNumTv.setText(mDefaultDevice.getDeviceSn());
        }

        if (mDefaultDevice.getDeviceNodeId() != null){
            mImeiTv.setText(mDefaultDevice.getDeviceNodeId());
        }

        if (mDefaultDevice.getBleMac() != null){
            mBTMacTv.setText(mDefaultDevice.getBleMac());
        }

        if (mDefaultDevice.getDeviceSwVersion() != null) {
            mSoftVersionTv.setText(mDefaultDevice.getDeviceSwVersion());
        }
        if (mDefaultDevice.getDeviceHwVersion() != null) {
            mHardVersionTv.setText(mDefaultDevice.getDeviceHwVersion());
        }

    }
    private void initEvent(){
        ImageView imageView = findViewById(R.id.iv_version_info_back);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

}
