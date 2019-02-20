package com.smart.lock.ui.setting;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ToggleButton;

import com.smart.lock.R;
import com.smart.lock.ui.login.LockScreenActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.SharedPreferenceUtil;
import com.smart.lock.widget.ToggleSwitchDefineView;


public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView ivBack;
    private int mConfirmNum;
    private ToggleSwitchDefineView mNumPwdSwitchTv;
    private ToggleButton mNumPwdSwitchLight;
    private ToggleSwitchDefineView mFingersPrintSwitchTv;
    private ToggleButton mFingersPrintSwitchLight;

    private Dialog mPromptDialog;
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
                if (v.getId() == R.id.iv_switch_light) {
                            if (mNumPwdSwitchLight.isChecked()) {
                                mPromptDialog = DialogUtils.createPromptDialog(SettingsActivity.this,
                                        "您还未添加密码信息，是否立即设置？",
                                        LockScreenActivity.class);
                                mPromptDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        mNumPwdSwitchLight.setChecked(false);
                                    }
                                });
                                if (!mPromptDialog.isShowing()) {
                                    mPromptDialog.show();
                                }
                            }else {
                            Intent intent = new Intent(SettingsActivity.this, LockScreenActivity.class);
                            intent.putExtra(ConstantUtil.IS_RETURN, true);
                            SettingsActivity.this.startActivityForResult(intent.putExtra(ConstantUtil.TYPE, ConstantUtil.LOGIN_PASSWORD), 1);
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

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if(resultCode ==RESULT_OK){
            switch (data.getExtras().getInt(ConstantUtil.CONFIRM)){
                case 1:
                    SharedPreferenceUtil.getInstance(SettingsActivity.this).
                            writeBoolean(ConstantUtil.NUM_PWD_CHECK,true);
                    mNumPwdSwitchLight.setChecked(true);
                    break;
                case -1:
                    SharedPreferenceUtil.getInstance(SettingsActivity.this).
                            writeBoolean(ConstantUtil.NUM_PWD_CHECK,false);
                    mNumPwdSwitchLight.setChecked(false);
                    break;
                default:
                        break;
            }
        }else {
            SharedPreferenceUtil.getInstance(SettingsActivity.this).
                    writeBoolean(ConstantUtil.NUM_PWD_CHECK,true);
            mNumPwdSwitchLight.setChecked(true);
        }
    }
}
