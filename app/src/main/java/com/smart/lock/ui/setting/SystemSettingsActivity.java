package com.smart.lock.ui.setting;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.view.View;
import android.widget.ImageView;
import android.widget.ToggleButton;

import com.smart.lock.R;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.fp.BaseFPActivity;
import com.smart.lock.fp.FingerprintDialogFragment;
import com.smart.lock.ui.login.LockScreenActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.SharedPreferenceUtil;
import com.smart.lock.widget.ToggleSwitchDefineView;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;


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

    private int mIsFP = 0; //是否支持指纹，0 1 不支持 2 3 未设置 4 支持
    private FingerprintManager mFPM;
    private CancellationSignal mCancellationSignal;
    private KeyStore mKeyStore;

    private boolean mIsFPRequired = false;
    private boolean mIsPwdRequired = false;


    @Override
    protected void onCreate(Bundle savedInstancesState) {
        super.onCreate(savedInstancesState);
        setContentView(R.layout.activity_system_setting);
        initView();
        initData();
        initEvent();
    }

    public void initView() {
        ivBack = findViewById(R.id.iv_back);
        mNumPwdSwitchTv = this.findViewById(R.id.switch_password);
        mNumPwdSwitchLight = mNumPwdSwitchTv.getIv_switch_light();
        mFingersPrintSwitchTv = this.findViewById(R.id.switch_fingerprint);
        mFingersPrintSwitchLight = mFingersPrintSwitchTv.getIv_switch_light();
        mOpenTestTv = findViewById(R.id.tw_open_test);
        mOpenTestTb = mOpenTestTv.getIv_switch_light();
        mNumPwdSwitchTv.setDes("密码验证");
        mFingersPrintSwitchTv.setDes("指纹验证");
        mOpenTestTv.setDes("测试自动连接");
        mNumPwdSwitchLightTBtn = mNumPwdSwitchTv.getIv_switch_light();

        //指纹设置
        mFingerprintSwitchTv = this.findViewById(R.id.switch_fingerprint);
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
                if (v.getId() == R.id.iv_switch_light) {

                    if (mNumPwdSwitchLightTBtn.isChecked()) {
                        mPromptDialog = DialogUtils.createPromptDialog(SystemSettingsActivity.this,
                                "您还未添加密码信息，是否立即设置？",
                                LockScreenActivity.class);
                        mPromptDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                mNumPwdSwitchLightTBtn.setChecked(false);
                            }
                        });
                        if (!mPromptDialog.isShowing()) {
                            mPromptDialog.show();
                        }
                    } else {
                        Intent intent = new Intent(SystemSettingsActivity.this, LockScreenActivity.class);
                        intent.putExtra(ConstantUtil.IS_RETURN, true);
                        SystemSettingsActivity.this.startActivityForResult(intent.putExtra(ConstantUtil.TYPE, ConstantUtil.LOGIN_PASSWORD), 1);
                    }
                }
            }
        });
        if (mIsFP > 1) {
            mFingerprintSwitchLightTbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v.getId() == R.id.iv_switch_light) {
                        if (mFingerprintSwitchLightTbtn.isChecked()) {
                            if (!mIsPwdRequired) {
                                mFingerprintDialog = DialogUtils.createAlertDialog(SystemSettingsActivity.this,
                                        "您未开启密码验证，请先开启密码验证");
                                mFingerprintDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        mFingerprintSwitchLightTbtn.setChecked(false);
                                    }
                                });
                            } else if (mIsFP == ConstantUtil.FP_NO_KEYGUARDSECURE) {
                                mFingerprintDialog = DialogUtils.createAlertDialog(SystemSettingsActivity.this,
                                        "您未设置锁屏，请设置锁屏并添加指纹");
                                mFingerprintDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        mFingerprintSwitchLightTbtn.setChecked(false);
                                    }
                                });
                            } else if (mIsFP == ConstantUtil.FP_NO_FINGERPRINT) {
                                mFingerprintDialog = DialogUtils.createAlertDialog(SystemSettingsActivity.this,
                                        "您至少在系统设置中添加一个指纹");
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
                    }
                }
            });
        }

        mOpenTestTb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferenceUtil.getInstance(SystemSettingsActivity.this).writeBoolean(ConstantUtil.OPEN_TEST, mOpenTestTb.isChecked());
                Intent result = new Intent();
                result.putExtra(ConstantUtil.OPEN_TEST, mOpenTestTb.isChecked());
                result.setAction(BleMsg.STR_RSP_OPEN_TEST);
                LocalBroadcastManager.getInstance(SystemSettingsActivity.this).sendBroadcast(result);
            }
        });
    }


    /**
     * 检测设备是否支持指纹
     *
     * @return 0 系统不支持 1 手机不支持 2 未设置锁屏 3 未设置指纹 4 支持指纹
     */
    private int supportFP() {
        if (Build.VERSION.SDK_INT < 23) {
            LogUtil.i(TGA, "系统版本低,不支持指纹功能");
            return ConstantUtil.FP_LOW_VERSION;
        } else {
            KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
            mFPM = getSystemService(FingerprintManager.class);
            if (!mFPM.isHardwareDetected()) {
                LogUtil.i(TGA, "手机不支持指纹");
                return ConstantUtil.FP_NO_HARDWARE;
            } else if (!keyguardManager.isKeyguardSecure()) {
                LogUtil.i(TGA, "未设置锁屏，需设置锁屏并添加指纹");
                return ConstantUtil.FP_NO_KEYGUARDSECURE;
            } else if (!mFPM.hasEnrolledFingerprints()) {
                LogUtil.i(TGA, "系统中至少需要添加一个指纹");
                return ConstantUtil.FP_NO_FINGERPRINT;
            }
        }
        LogUtil.i(TGA, "支持指纹");
        return ConstantUtil.FP_SUPPORT;
    }

    /**
     * 开启指纹验证
     */
    @TargetApi(23)
    private void doFingerprintDialog() {
        Cipher lCipher;
        //初始化key
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
            mKeyStore.load(null);
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(DEVICE_POLICY_SERVICE,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
            keyGenerator.init(builder.build());
            keyGenerator.generateKey();
        } catch (Exception e) {
            LogUtil.e(TGA, "初始化key出错" + e);
        }
        // 初始化Cipher
        try {
            SecretKey key = (SecretKey) mKeyStore.getKey(DEVICE_POLICY_SERVICE, null);
            lCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + '/'
                    + KeyProperties.BLOCK_MODE_CBC + '/'
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            lCipher.init(Cipher.ENCRYPT_MODE, key);
            showFingerprintDialog(lCipher);
        } catch (Exception e) {
            LogUtil.e(TGA, "初始化Cipher出错" + e);
        }

    }

    @TargetApi(23)
    private void showFingerprintDialog(Cipher cipher) {
        FingerprintDialogFragment fingerprintDialogFragment = new FingerprintDialogFragment();
        fingerprintDialogFragment.setCipher(cipher);
        fingerprintDialogFragment.show(getFragmentManager(), "FINGERPRINT");
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
        if (v.getId() == R.id.iv_back) {
            finish();
            return;
        } else {
            return;
        }
    }

    @Override
    protected void onResume() {
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
                    mNumPwdSwitchLightTBtn.setChecked(false);
                    mIsPwdRequired = false;
                    SharedPreferenceUtil.getInstance(SystemSettingsActivity.this).
                            writeBoolean(ConstantUtil.FINGERPRINT_CHECK, false);
                    mFingerprintSwitchLightTbtn.setChecked(false);
                    mIsFPRequired = false;
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
