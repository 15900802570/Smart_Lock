package com.smart.lock.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.widget.ToggleSwitchDefineView;
import com.smart.lock.widget.NextActivityDefineView;
import com.smart.lock.widget.BtnSettingDefineView;

import com.smart.lock.R;

public class LockSettingActivity extends AppCompatActivity {

    private ToggleSwitchDefineView mIntelligentLockTs;
    private ToggleSwitchDefineView mAntiPrizingAlarmTs;
    private ToggleSwitchDefineView mCombinationLockTs;
    private ToggleSwitchDefineView mNormallyOpenTs;
    private ToggleSwitchDefineView mTurnOffVoiceTs;

    private BtnSettingDefineView mRolledBackTineBs;

    private NextActivityDefineView mVersionInfoNa;
    private NextActivityDefineView mSelfCheckNa;
    private NextActivityDefineView mOtaUpdateNa;
    private NextActivityDefineView mFactoryResetNa;

    private String  TAG = "LockSettingActivity";

    private DeviceInfo mDefaultDevice;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_setting);
        initView();
        initData();
        initEvent();
    }

    private void initView(){
        mIntelligentLockTs = findViewById(R.id.ts_intelligent_lock);
        mAntiPrizingAlarmTs = findViewById(R.id.ts_anti_prizing_alarm);
        mCombinationLockTs = findViewById(R.id.ts_combination_lock);
        mNormallyOpenTs = findViewById(R.id.ts_normally_open);
        mTurnOffVoiceTs = findViewById(R.id.ts_turn_off_voice);

        mRolledBackTineBs = findViewById(R.id.bs_rolled_back_time);

        mVersionInfoNa = findViewById(R.id.next_version_info);
        mSelfCheckNa = findViewById(R.id.next_self_check);
        mOtaUpdateNa = findViewById(R.id.next_ota_update);
        mFactoryResetNa = findViewById(R.id.next_factory_reset);

        mIntelligentLockTs.setDes(getResources().getString(R.string.intelligent_lock));
        mAntiPrizingAlarmTs.setDes(getResources().getString(R.string.anti_prizing_alarm));
        mCombinationLockTs.setDes(getResources().getString(R.string.combination_lock));
        mNormallyOpenTs.setDes(getResources().getString(R.string.normally_open));
        mTurnOffVoiceTs.setDes(getResources().getString(R.string.turn_off_voice));

        mRolledBackTineBs.setDes(getResources().getString(R.string.rolled_back_time),
                getResources().getString(R.string.set_up_time));

        mVersionInfoNa.setDes(getResources().getString(R.string.version_info));
        mSelfCheckNa.setDes(getResources().getString(R.string.self_check));
        mOtaUpdateNa.setDes(getResources().getString(R.string.ota_update));
        mFactoryResetNa.setDes(getResources().getString(R.string.restore_the_factory_settings));
    }

    private void initData(){
        try {
            Bundle bundle = getIntent().getExtras();
            mDefaultDevice = (DeviceInfo) bundle.getSerializable(BleMsg.KEY_DEFAULT_DEVICE);
        }catch (NullPointerException e){
            e.printStackTrace();
        }

    }

    private void initEvent(){

        //版本信息
        mVersionInfoNa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//            mVersionInfoNa.animate().alpha(0.5f).setDuration(200).start();
                LogUtil.e(TAG,"ERROR");
                Intent intent = new Intent(LockSettingActivity.this,VersionInfoActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
    }
    public void onClick(View view){
        switch (view.getId()){
            case R.id.iv_back:
                finish();
        }
    }

}
