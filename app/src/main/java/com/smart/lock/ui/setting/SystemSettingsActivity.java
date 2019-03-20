package com.smart.lock.ui.setting;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.content.LocalBroadcastManager;
import android.os.CancellationSignal;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ToggleButton;

import com.smart.lock.R;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ui.fp.BaseFPActivity;
import com.smart.lock.ui.login.LockScreenActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.SharedPreferenceUtil;
import com.smart.lock.widget.ToggleSwitchDefineView;


public class SystemSettingsActivity extends BaseFPActivity implements View.OnClickListener {

    private static String TAG = "SystemSettingsActivity";

    private ImageView ivBack;
    private ToggleSwitchDefineView mNumPwdSwitchTv;
    private ToggleButton mNumPwdSwitchLight;
    private ToggleSwitchDefineView mFingersPrintSwitchTv;
    private ToggleButton mFingersPrintSwitchLight;
    private ToggleSwitchDefineView mOpenTestTv;
    private ToggleButton mOpenTestTb;
    private ToggleButton mNumPwdSwitchLightTBtn;
    private ToggleSwitchDefineView mFingerprintSwitchTv;
    private ToggleButton mFingerprintSwitchLightTbtn;

    private Dialog mPromptDialog;

    private Dialog mFingerprintDialog;

    private CancellationSignal mCancellationSignal;

    private boolean mIsFPRequired = false;
    private boolean mIsPwdRequired = false;

    private EditText mNumPwd1Et;
    private EditText mNumPwd2Et;
    private EditText mNumPwd3Et;
    private EditText mNumPwd4Et;


    @Override
    protected void onCreate(Bundle savedInstancesState) {
        super.onCreate(savedInstancesState);
        setContentView(R.layout.activity_system_setting);
        initView();
        initData();
        initEvent();
    }

    public void initView() {
        ivBack = findViewById(R.id.system_set_iv_back);
        mNumPwdSwitchTv = this.findViewById(R.id.system_set_switch_password);
        mNumPwdSwitchLight = mNumPwdSwitchTv.getIv_switch_light();
        mFingersPrintSwitchTv = this.findViewById(R.id.system_set_switch_fingerprint);
        mFingersPrintSwitchLight = mFingersPrintSwitchTv.getIv_switch_light();
        mOpenTestTv = findViewById(R.id.tw_open_test);
        mOpenTestTb = mOpenTestTv.getIv_switch_light();
        mNumPwdSwitchTv.setDes("密码验证");
        mFingersPrintSwitchTv.setDes("指纹验证");
        mOpenTestTv.setDes("测试自动连接");
        mNumPwdSwitchLightTBtn = mNumPwdSwitchTv.getIv_switch_light();

        //指纹设置
        mFingerprintSwitchTv = this.findViewById(R.id.system_set_switch_fingerprint);
        mIsFP = supportFP();
        if (mIsFP > 1) {
            mFingerprintSwitchTv.setDes("指纹验证");
            mFingerprintSwitchLightTbtn = mFingerprintSwitchTv.getIv_switch_light();
        } else {
            mFingerprintSwitchTv.setVisibility(View.GONE);
        }

    }


    public void initData() {
        try {
            if (SharedPreferenceUtil.getInstance(this).readBoolean(ConstantUtil.NUM_PWD_CHECK)) {
                mNumPwdSwitchLightTBtn.setChecked(true);
                mIsPwdRequired = true;
            } else {
                mNumPwdSwitchLightTBtn.setChecked(false);
                mIsPwdRequired = false;
            }
        } catch (NullPointerException e) {
            LogUtil.e(TAG, "初始化密码验证开关出错" + e);
            mNumPwdSwitchLightTBtn.setChecked(false);
            mIsPwdRequired = false;
        }

        if (mIsFP > 1) {
            try {
                if (SharedPreferenceUtil.getInstance(this).readBoolean(ConstantUtil.FINGERPRINT_CHECK)) {
                    mFingerprintSwitchLightTbtn.setChecked(true);
                    mIsFPRequired = true;
                } else {
                    mFingerprintSwitchLightTbtn.setChecked(false);
                    mIsFPRequired = false;
                }
            } catch (NullPointerException e) {
                LogUtil.e(TAG, "初始化指纹验证开关出错" + e);
                mFingerprintSwitchLightTbtn.setChecked(false);
                mIsFPRequired = false;
            }
        }

        try {
            if (SharedPreferenceUtil.getInstance(this).readBoolean(ConstantUtil.OPEN_TEST)) {
                mOpenTestTb.setChecked(true);
            } else {
                mOpenTestTb.setChecked(false);
            }
        } catch (NullPointerException e) {
            LogUtil.e(TAG, "初始化自动连接失败" + e);
            mOpenTestTb.setChecked(false);
        }
    }

    public void initEvent() {
        ivBack.setOnClickListener(this);
        // 数字密码拨动开关监听
        mNumPwdSwitchLightTBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doOnClick(R.id.system_set_switch_password);
            }
        });
        if (mIsFP > 1) {
            mFingerprintSwitchLightTbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doOnClick(R.id.system_set_switch_fingerprint);
                }
            });
        }

        mOpenTestTb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doOnClick(R.id.tw_open_test);
            }
        });
    }

    @Override
    public void onFingerprintAuthentication() {
        LogUtil.i(TGA, "指纹验证成功");
        SharedPreferenceUtil.getInstance(SystemSettingsActivity.this).
                writeBoolean(ConstantUtil.FINGERPRINT_CHECK, !mIsFPRequired);
        mFingerprintSwitchLightTbtn.setChecked(!mIsFPRequired);
        mIsFPRequired = !mIsFPRequired;
    }

    @Override
    public void onFingerprintAuthenticationError() {

    }

    @Override
    public void onFingerprintCancel() {
        mFingerprintSwitchLightTbtn.setChecked(mIsFPRequired);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.system_set_iv_back:
                finish();
                break;

            case R.id.system_set_modify_pwd:
                BottomSheetDialog bottomDialog = DialogUtils.createBottomSheetDialog(this, R.layout.bottom_sheet_modify_pwd, R.id.system_set_modify_pwd_ll);
                mNumPwd1Et = bottomDialog.getDelegate().findViewById(R.id.num_pwd1_et);
                mNumPwd2Et = bottomDialog.getDelegate().findViewById(R.id.num_pwd2_et);
                mNumPwd3Et = bottomDialog.getDelegate().findViewById(R.id.num_pwd3_et);
                mNumPwd4Et = bottomDialog.getDelegate().findViewById(R.id.num_pwd4_et);

                bottomDialog.show();
                break;

            default:
                doOnClick(v.getId());
                break;
        }
    }

    public void tipsOnClick(View view) {
        switch (view.getId()) {
            case R.id.dialog_cancel_btn:
                mNumPwdSwitchLightTBtn.setChecked(false);
                break;
            case R.id.dialog_confirm_btn:
                Intent intent = new Intent(SystemSettingsActivity.this, LockScreenActivity.class);
                intent.putExtra(ConstantUtil.IS_RETURN, true);
                SystemSettingsActivity.this.startActivityForResult(intent.
                        putExtra(ConstantUtil.TYPE, ConstantUtil.SETTING_PASSWORD), 1);
                break;
        }
        mPromptDialog.cancel();
    }

    private void doOnClick(@IdRes int idRes) {
        switch (idRes) {
            case R.id.system_set_switch_password:
                if (!SharedPreferenceUtil.getInstance(this).readBoolean(ConstantUtil.NUM_PWD_CHECK)) {
                    mPromptDialog = DialogUtils.createTipsDialog(SystemSettingsActivity.this,
                            "您还未添加密码信息，是否立即设置？");
                    if (!mPromptDialog.isShowing()) {
                        mPromptDialog.show();
                    }
                } else {
                    Intent intent = new Intent(SystemSettingsActivity.this, LockScreenActivity.class);
                    intent.putExtra(ConstantUtil.IS_RETURN, true);
                    SystemSettingsActivity.this.startActivityForResult(intent.
                            putExtra(ConstantUtil.TYPE, ConstantUtil.LOGIN_PASSWORD), 1);
                }
                break;

            case R.id.system_set_switch_fingerprint:
                if (!SharedPreferenceUtil.getInstance(this).readBoolean(ConstantUtil.FINGERPRINT_CHECK)) {
                    if (!mIsPwdRequired) {
                        mFingerprintDialog = DialogUtils.createAlertDialog(SystemSettingsActivity.this,
                                "您未开启密码验证，请先开启密码验证");
                        mFingerprintDialog.show();
                        mFingerprintDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                mFingerprintSwitchLightTbtn.setChecked(false);
                            }
                        });
                    } else if (mIsFP == ConstantUtil.FP_NO_KEYGUARDSECURE) {
                        mFingerprintDialog = DialogUtils.createAlertDialog(SystemSettingsActivity.this,
                                "您未设置锁屏，请设置锁屏并添加指纹");
                        mFingerprintDialog.show();
                        mFingerprintDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                mFingerprintSwitchLightTbtn.setChecked(false);
                            }
                        });
                    } else if (mIsFP == ConstantUtil.FP_NO_FINGERPRINT) {
                        mFingerprintDialog = DialogUtils.createAlertDialog(SystemSettingsActivity.this,
                                "您至少在系统设置中添加一个指纹");
                        mFingerprintDialog.show();
                        mFingerprintDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                mFingerprintSwitchLightTbtn.setChecked(false);
                            }
                        });
                    } else if (mIsFP == ConstantUtil.FP_SUPPORT) {
                        doFingerprintDialog();
                    }
                } else {
                    doFingerprintDialog();
                }
                break;

            case R.id.tw_open_test:
                SharedPreferenceUtil.getInstance(SystemSettingsActivity.this).writeBoolean(ConstantUtil.OPEN_TEST, mOpenTestTb.isChecked());
                Intent result = new Intent();
                result.putExtra(ConstantUtil.OPEN_TEST, mOpenTestTb.isChecked());
                result.setAction(BleMsg.STR_RSP_OPEN_TEST);
                LocalBroadcastManager.getInstance(SystemSettingsActivity.this).sendBroadcast(result);
                break;
        }
    }

    public void numOnClick(@NonNull View view) {
        int num = -1;
        switch (view.getId()) {
            case R.id.num_one_tv:
                num = 1;
                break;
            case R.id.num_two_tv:
                num = 2;
                break;
            case R.id.num_three_tv:
                num = 3;
                break;
            case R.id.num_four_tv:
                num = 4;
                break;
            case R.id.num_five_tv:
                num = 5;
                break;
            case R.id.num_six_tv:
                num = 6;
                break;
            case R.id.num_seven_tv:
                num = 7;
                break;
            case R.id.num_eight_tv:
                num = 8;
                break;
            case R.id.num_nine_tv:
                num = 9;
                break;
            case R.id.num_zero_tv:
                num = 0;
                break;
            case R.id.num_back_tv:
                num = -1;
                break;
        }
        if (num != -1) {
            setPwd(String.valueOf(num));
        } else {
            delPwd();
        }
        LogUtil.d(TAG, "TAG = " + mNumPwd1Et.getText());
    }

    private void setPwd(String num) {
        if (mNumPwd1Et.getText().toString().equals("")) {
            mNumPwd1Et.setText(num);
            LogUtil.d(TAG, "TAG = 1");
            mNumPwd2Et.setFreezesText(true);
        } else if (mNumPwd2Et.getText().toString().equals("")) {
            mNumPwd2Et.setText(num);
            LogUtil.d(TAG, "TAG = 2");
            mNumPwd3Et.setFreezesText(true);
        } else if (mNumPwd3Et.getText().toString().equals("")) {
            mNumPwd3Et.setText(num);
            mNumPwd4Et.setFreezesText(true);
        } else if (mNumPwd4Et.getText().toString().equals("")) {
            mNumPwd4Et.setText(num);
        }
    }

    private void delPwd() {
        if (!mNumPwd4Et.getText().toString().equals("")) {
            mNumPwd4Et.getText().delete(0, 1);
        } else if (!mNumPwd3Et.getText().toString().equals("")) {
            mNumPwd3Et.getText().delete(0, 1);
        } else if (!mNumPwd2Et.getText().toString().equals("")) {
            mNumPwd2Et.getText().delete(0, 1);
        } else if (!mNumPwd1Et.getText().toString().equals("")) {
            mNumPwd1Et.getText().delete(0, 1);
        }
    }

    @Override
    protected void onResume() {
        if (SharedPreferenceUtil.getInstance(this).readBoolean(ConstantUtil.NUM_PWD_CHECK) &&
                SharedPreferenceUtil.getInstance(this).readString(ConstantUtil.NUM_PWD) != "") {
            SharedPreferenceUtil.getInstance(this).writeBoolean(ConstantUtil.NUM_PWD_CHECK, true);
            mNumPwdSwitchLightTBtn.setChecked(true);
            mIsPwdRequired = true;
        } else {
            SharedPreferenceUtil.getInstance(this).writeBoolean(ConstantUtil.NUM_PWD_CHECK, false);
            mNumPwdSwitchLightTBtn.setChecked(false);
            mIsPwdRequired = false;
        }
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode == RESULT_OK) {
            switch (data.getExtras().getInt(ConstantUtil.CONFIRM)) {
                case 1:
                    SharedPreferenceUtil.getInstance(SystemSettingsActivity.this).
                            writeBoolean(ConstantUtil.NUM_PWD_CHECK, true);
                    mNumPwdSwitchLightTBtn.setChecked(true);
                    mIsPwdRequired = true;
                    break;
                case -1:
                    SharedPreferenceUtil.getInstance(SystemSettingsActivity.this).
                            writeBoolean(ConstantUtil.NUM_PWD_CHECK, false);
                    SharedPreferenceUtil.getInstance(SystemSettingsActivity.this).
                            writeString(ConstantUtil.NUM_PWD, "");
                    mNumPwdSwitchLightTBtn.setChecked(false);
                    mIsPwdRequired = false;
                    if (mIsFP > 1) {
                        SharedPreferenceUtil.getInstance(SystemSettingsActivity.this).
                                writeBoolean(ConstantUtil.FINGERPRINT_CHECK, false);
                        mFingerprintSwitchLightTbtn.setChecked(false);
                        mIsFPRequired = false;
                    }
                    break;
                default:
                    break;
            }
        } else {
            SharedPreferenceUtil.getInstance(SystemSettingsActivity.this).
                    writeBoolean(ConstantUtil.NUM_PWD_CHECK, true);
            mNumPwdSwitchLightTBtn.setChecked(true);
            mIsPwdRequired = true;
        }
    }
}
