package com.smart.lock.ui.setting;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;


import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.ui.LockDetectingActivity;
import com.smart.lock.ui.OtaUpdateActivity;

import com.smart.lock.ui.fp.BaseFPActivity;
import com.smart.lock.ui.login.LockScreenActivity;
import com.smart.lock.utils.CheckVersionThread;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.SharedPreferenceUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.SystemUtils;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.DialogFactory;
import com.smart.lock.widget.NextActivityDefineView;
import com.smart.lock.widget.ToggleSwitchDefineView;
import com.yzq.zxinglibrary.android.CaptureActivity;
import com.yzq.zxinglibrary.bean.ZxingConfig;
import com.yzq.zxinglibrary.common.Constant;


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
    private NextActivityDefineView mCheckVersionNv;

    private NextActivityDefineView mModifyPwdNv;

    private NextActivityDefineView mSetDevInfoNv;


    private Dialog mPromptDialog;

    private Dialog mFingerprintDialog;


    private boolean mIsFPRequired = false;
    private boolean mIsPwdRequired = false;
    private CheckVersionThread mCheckVersionThread;
    protected DialogFactory mDialog;


    private int REQUEST_CODE_NEW_PASSWORD = 1;
    private int REQUEST_CODE_MODIFY_PASSWORD = 1;

    private EditText mNumPwd1Et;
    private EditText mNumPwd2Et;
    private EditText mNumPwd3Et;
    private EditText mNumPwd4Et;

    private String mSn; //设备SN
    private String mNodeId; //设备IMEI
    private String mBleMac; //蓝牙地址

    private Dialog mLoadDialog;


    private String[] mPermission = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private int REQUESTCODE = 0;
    private static final int REQUEST_CODE_SCAN = 1;

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
        mNumPwdSwitchTv = findViewById(R.id.system_set_switch_password);
        mNumPwdSwitchLight = mNumPwdSwitchTv.getIv_switch_light();
        mFingersPrintSwitchTv = findViewById(R.id.system_set_switch_fingerprint);
        mFingersPrintSwitchLight = mFingersPrintSwitchTv.getIv_switch_light();
        mOpenTestTv = findViewById(R.id.tw_open_test);
        mCheckVersionNv = findViewById(R.id.next_check_version);

        mModifyPwdNv = findViewById(R.id.system_set_modify_pwd);

        mSetDevInfoNv = findViewById(R.id.next_set_info);

        mOpenTestTb = mOpenTestTv.getIv_switch_light();
        mNumPwdSwitchTv.setDes("密码验证");
        mFingersPrintSwitchTv.setDes("指纹验证");
        mOpenTestTv.setDes("测试自动连接");
        mModifyPwdNv.setDes(getResources().getString(R.string.modify_pwd));
        mNumPwdSwitchLightTBtn = mNumPwdSwitchTv.getIv_switch_light();

        mCheckVersionNv.setDes(getString(R.string.check_app_version));
        mSetDevInfoNv.setDes(getString(R.string.set_dev_info));

        //指纹设置
        mFingerprintSwitchTv = this.findViewById(R.id.system_set_switch_fingerprint);

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
                mNumPwdSwitchTv.setVisibility(View.GONE);
                mModifyPwdNv.setVisibility(View.VISIBLE);
            } else {
                mNumPwdSwitchLightTBtn.setChecked(false);
                mIsPwdRequired = false;
                mModifyPwdNv.setVisibility(View.GONE);
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

        mDialog = DialogFactory.getInstance(this);
        mLoadDialog = DialogUtils.createLoadingDialog(this, getString(R.string.data_loading));
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
        mCheckVersionNv.setOnClickListener(this);
        mSetDevInfoNv.setOnClickListener(this);
        mOpenTestTb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doOnClick(R.id.tw_open_test);
            }
        });
    }

    @Override
    public void onFingerprintAuthenticationSucceeded() {
        LogUtil.i(TGA, "指纹验证成功");
        SharedPreferenceUtil.getInstance(SystemSettingsActivity.this).
                writeBoolean(ConstantUtil.FINGERPRINT_CHECK, !mIsFPRequired);
        mFingerprintSwitchLightTbtn.setChecked(!mIsFPRequired);
        mIsFPRequired = !mIsFPRequired;
    }

    @Override
    public void onFingerprintAuthenticationError(int errorCode) {

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
                Intent intent = new Intent(SystemSettingsActivity.this, LockScreenActivity.class);
                intent.putExtra(ConstantUtil.IS_RETURN, true);
                SystemSettingsActivity.this.startActivityForResult(intent.
                        putExtra(ConstantUtil.TYPE, ConstantUtil.MODIFY_PASSWORD), REQUEST_CODE_MODIFY_PASSWORD);
                break;

            case R.id.next_check_version:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    if (!SystemUtils.isNetworkAvailable(this)) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        ToastUtil.show(this, getString(R.string.plz_open_wifi), Toast.LENGTH_LONG);
                        return;
                    }
                    mCheckVersionThread = new CheckVersionThread(this, mDialog);
                    mCheckVersionThread.setShowLoadDialog(true);
                    mCheckVersionThread.run();
                } else {
                    ActivityCompat.requestPermissions(this, mPermission, REQUESTCODE);
                }//版本检测
                break;
            case R.id.next_set_info:
                scanQr();
                break;
            default:
                doOnClick(v.getId());
                break;
        }
    }

    /**
     * 打开第三方二维码扫描库
     */
    private void scanQr() {
        Intent newIntent = new Intent(this, CaptureActivity.class);
        ZxingConfig config = new ZxingConfig();
        config.setPlayBeep(true);//是否播放扫描声音 默认为true
        config.setShake(true);//是否震动  默认为true
        config.setDecodeBarCode(false);//是否扫描条形码 默认为true
        config.setReactColor(R.color.colorAccent);//设置扫描框四个角的颜色 默认为淡蓝色
        config.setFrameLineColor(R.color.colorAccent);//设置扫描框边框颜色 默认无色
        config.setFullScreenScan(true);//是否全屏扫描  默认为true  设为false则只会在扫描框中扫描
        newIntent.putExtra(Constant.INTENT_ZXING_CONFIG, config);
        startActivityForResult(newIntent, REQUEST_CODE_SCAN);
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
                        putExtra(ConstantUtil.TYPE, ConstantUtil.SETTING_PASSWORD), REQUEST_CODE_NEW_PASSWORD);
                break;
        }
        mPromptDialog.cancel();
    }

    private void doOnClick(@IdRes int idRes) {
        switch (idRes) {
            case R.id.system_set_switch_password:
                if (!SharedPreferenceUtil.getInstance(this).readBoolean(ConstantUtil.NUM_PWD_CHECK)) {
                    mPromptDialog = DialogUtils.createTipsDialogWithConfirmAndCancel(SystemSettingsActivity.this,
                            "您还未添加密码信息，是否立即设置？");
                    if (!mPromptDialog.isShowing()) {
                        mPromptDialog.show();
                    }
                } else {
                    Intent intent = new Intent(SystemSettingsActivity.this, LockScreenActivity.class);
                    intent.putExtra(ConstantUtil.IS_RETURN, true);
                    SystemSettingsActivity.this.startActivityForResult(intent.
                            putExtra(ConstantUtil.TYPE, ConstantUtil.LOGIN_PASSWORD), REQUEST_CODE_NEW_PASSWORD);
                }
                break;

            case R.id.system_set_switch_fingerprint:
                if (!SharedPreferenceUtil.getInstance(this).readBoolean(ConstantUtil.FINGERPRINT_CHECK)) {
                    if (!mIsPwdRequired) {
                        mFingerprintDialog = DialogUtils.createTipsDialogWithCancel(SystemSettingsActivity.this,
                                "您未开启密码验证，请先开启密码验证");
                        mFingerprintDialog.show();
                        mFingerprintDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                mFingerprintSwitchLightTbtn.setChecked(false);
                            }
                        });
                    } else if (mIsFP == ConstantUtil.FP_NO_KEYGUARDSECURE) {
                        mFingerprintDialog = DialogUtils.createTipsDialogWithCancel(SystemSettingsActivity.this,
                                "您未设置锁屏，请设置锁屏并添加指纹");
                        mFingerprintDialog.show();
                        mFingerprintDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                mFingerprintSwitchLightTbtn.setChecked(false);
                            }
                        });
                    } else if (mIsFP == ConstantUtil.FP_NO_FINGERPRINT) {
                        mFingerprintDialog = DialogUtils.createTipsDialogWithCancel(SystemSettingsActivity.this,
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

        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_NEW_PASSWORD) {
            switch (data.getExtras().getInt(ConstantUtil.CONFIRM)) {
                case 1:
                    SharedPreferenceUtil.getInstance(SystemSettingsActivity.this).
                            writeBoolean(ConstantUtil.NUM_PWD_CHECK, true);
                    mNumPwdSwitchLightTBtn.setChecked(true);
                    mNumPwdSwitchTv.setVisibility(View.GONE);
                    mModifyPwdNv.setVisibility(View.VISIBLE);
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
        } else if(requestCode == REQUEST_CODE_NEW_PASSWORD) {
            SharedPreferenceUtil.getInstance(SystemSettingsActivity.this).
                    writeBoolean(ConstantUtil.NUM_PWD_CHECK, true);
            mNumPwdSwitchLightTBtn.setChecked(true);
            mIsPwdRequired = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUESTCODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        || !shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    askForPermission();
                }
            }
        }
    }

    private void askForPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Need Permission!");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName())); // 根据包名打开对应的设置界面
                startActivity(intent);
            }
        });
        builder.create().show();
    }

}

