package com.smart.lock.ui;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.smart.lock.R;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.DeviceInfo;

public class VersionInfoActivity extends Activity {

    private DeviceInfo mDefaultDevice;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_version_info);

        try {
            Bundle bundle = getIntent().getExtras();
            mDefaultDevice = (DeviceInfo) bundle.getSerializable(BleMsg.KEY_DEFAULT_DEVICE);
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        initView();
    }

    private void initView(){

        TextView mSerialNumTv=findViewById(R.id.tv_serial_num);
        TextView mImeiTv=findViewById(R.id.tv_imei);
        TextView mBTMacTv=findViewById(R.id.tv_bt_mac_ad);
        TextView mSoftVersionTv=findViewById(R.id.tv_software_version);
        TextView mHardVersionTv=findViewById(R.id.tv_hardware_version);

        mSerialNumTv.setText(mDefaultDevice.getDeviceSn());
        mImeiTv.setText(mDefaultDevice.getDeviceNodeId());
        mBTMacTv.setText(mDefaultDevice.getBleMac());
        if(!mDefaultDevice.getDeviceVersion().equals("")) {
            mSoftVersionTv.setText(mDefaultDevice.getDeviceVersion());
            mHardVersionTv.setText(mDefaultDevice.getDeviceVersion());
        }
    }

    public void onClick(View view){
        if(view.getId() == R.id.iv_version_info_back){
            finish();
        }
    }

}
