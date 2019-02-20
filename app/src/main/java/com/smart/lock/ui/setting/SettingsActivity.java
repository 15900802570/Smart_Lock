package com.smart.lock.ui.setting;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ToggleButton;

import com.smart.lock.R;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.SharedPreferenceUtil;
import com.smart.lock.widget.ToggleSwitchDefineView;


public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView ivBack;
    private ToggleSwitchDefineView mNumPwdSwitchTv;
    private ToggleButton mNumPwdSwitchLight;
    private ToggleSwitchDefineView mFingersPrintSwitchTv;
    private ToggleButton mFingersPrintSwitchLight;

    @Override
    protected void onCreate(Bundle savedInstancesState) {
        super.onCreate(savedInstancesState);
            setContentView(R.layout.activity_setting);
        initView();
        initData();
        initEvent();
    }

    public void initView(){
        ivBack = findViewById(R.id.iv_back);
        mNumPwdSwitchTv = this.findViewById(R.id.switch_password);
        mNumPwdSwitchLight = mNumPwdSwitchTv.getIv_switch_light();
        mFingersPrintSwitchTv = this.findViewById(R.id.switch_fingerprint);
        mFingersPrintSwitchLight = mFingersPrintSwitchTv.getIv_switch_light();
        mNumPwdSwitchTv.setDes("密码验证");
        mFingersPrintSwitchTv.setDes("指纹验证");
    }

    public void initData(){
        try{
            if(SharedPreferenceUtil.getInstance(this).readBoolean(ConstantUtil.NUM_PWD_CHECK)){
                mNumPwdSwitchLight.setChecked(true);
            }else {
                mNumPwdSwitchLight.setChecked(false);
            }
        }catch (NullPointerException e){
             mNumPwdSwitchLight.setChecked(false);
        }
    }

    public void initEvent(){
        ivBack.setOnClickListener(this);
        mNumPwdSwitchLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.iv_switch_light){
                    if(mNumPwdSwitchLight.isChecked()){
                        SharedPreferenceUtil.getInstance(SettingsActivity.this).
                                writeBoolean(ConstantUtil.NUM_PWD_CHECK,true);
                    }else {
                        SharedPreferenceUtil.getInstance(SettingsActivity.this).
                                writeBoolean(ConstantUtil.NUM_PWD_CHECK,false);
                    }
                }
            }
        });
    }

    @Override
    public void onClick(View v){
        if (v.getId() == R.id.iv_back) {
            finish();
            return ;
        } else {
            return ;
        }
    }

}
