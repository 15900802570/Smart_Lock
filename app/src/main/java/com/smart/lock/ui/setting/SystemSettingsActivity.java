package com.smart.lock.ui.setting;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
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
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.smart.lock.R;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.entity.Device;
import com.smart.lock.scan.ScanQRHelper;
import com.smart.lock.scan.ScanQRResultInterface;
import com.smart.lock.ui.LanguageActivity;
import com.smart.lock.ui.LockDetectingActivity;
import com.smart.lock.ui.LpcdTestActivity;
import com.smart.lock.ui.fp.BaseFPActivity;
import com.smart.lock.ui.login.LockScreenActivity;
import com.smart.lock.utils.CheckVersionThread;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.SharedPreferenceUtil;
import com.smart.lock.utils.SystemUtils;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.DialogFactory;
import com.smart.lock.widget.NextActivityDefineView;
import com.smart.lock.widget.ToggleSwitchDefineView;
import com.yzq.zxinglibrary.common.Constant;


public class SystemSettingsActivity extends BaseFPActivity implements View.OnClickListener, ScanQRResultInterface {

    private static String TAG = "SystemSettingsActivity";

    private ImageView ivBack;
    private ToggleSwitchDefineView mNumPwdSwitchTv;
    private ToggleButton mNumPwdSwitchLight;
    private ToggleSwitchDefineView mFingersPrintSwitchTv;
    private ToggleButton mFingersPrintSwitchLight;
    private ToggleSwitchDefineView mOpenTestTv;
    private ToggleSwitchDefineView mOpenUnlockDownloadTv;
    private ToggleButton mOpenTestTb;
    private ToggleButton mNumPwdSwitchLightTBtn;
    private ToggleSwitchDefineView mFingerprintSwitchTv;
    private ToggleButton mFingerprintSwitchLightTbtn;

    private ToggleSwitchDefineView mCheckSnTv; //型号检测

    private NextActivityDefineView mCheckVersionNv;

    private NextActivityDefineView mModifyPwdNv;

    private NextActivityDefineView mSetDevInfoNv;

    private NextActivityDefineView mQueryLpcdNv;

    private NextActivityDefineView mMultiLanguageNv;

    private Dialog mPromptDialog;

    private Dialog mFingerprintDialog;
    private TextView mTitleTv;


    private boolean mIsFPRequired = false;
    private boolean mIsPwdRequired = false;
    private CheckVersionThread mCheckVersionThread;
    protected DialogFactory mDialog;


    private int REQUEST_CODE_PASSWORD = 2;

    private EditText mNumPwd1Et;
    private EditText mNumPwd2Et;
    private EditText mNumPwd3Et;
    private EditText mNumPwd4Et;

    private String mSn; //设备SN
    private String mNodeId; //设备IMEI
    private String mBleMac; //蓝牙地址

    private Context mCtx;

    private SharedPreferenceUtil myPrefs; //preference

    private String[] mPermission = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private int REQUESTCODE = 0;
    private static final int REQUEST_CODE_SCAN = 1;

    private ScanQRHelper mScanQRHelper;
    private int mCount = 0;

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
        mTitleTv = findViewById(R.id.tv_title);
        mQueryLpcdNv = findViewById(R.id.next_query_lpcd);
        mOpenUnlockDownloadTv = findViewById(R.id.tw_open_unlock_download);
        mCheckSnTv = findViewById(R.id.tw_check_sn);
        mMultiLanguageNv = findViewById(R.id.next_multi_language);

        mOpenTestTb = mOpenTestTv.getIv_switch_light();
        mNumPwdSwitchTv.setDes(getString(R.string.password_verification));
        mFingersPrintSwitchTv.setDes(getString(R.string.fingerprint_verification));
        mOpenTestTv.setDes(getString(R.string.test_auto_conn));
        mOpenTestTv.setVisibility(View.GONE);
        mModifyPwdNv.setDes(getResources().getString(R.string.modify_pwd));
        mNumPwdSwitchLightTBtn = mNumPwdSwitchTv.getIv_switch_light();

        mMultiLanguageNv.setDes(getString(R.string.multi_language));
        mMultiLanguageNv.setVisibility(View.GONE);

        mCheckVersionNv.setDes(getString(R.string.check_app_version));
        mSetDevInfoNv.setDes(getString(R.string.set_dev_info));
        mQueryLpcdNv.setDes(getString(R.string.threshold));
        mOpenUnlockDownloadTv.setDes("OTA内部测试");
        mOpenUnlockDownloadTv.setVisibility(View.GONE);
        mSetDevInfoNv.setVisibility(View.GONE);
        mQueryLpcdNv.setVisibility(View.GONE);

        mCheckSnTv.setDes("不检测设备");
        mCheckSnTv.setVisibility(View.GONE);

        //指纹设置
        mFingerprintSwitchTv = this.findViewById(R.id.system_set_switch_fingerprint);

        if (mIsFP > 1) {
            mFingerprintSwitchTv.setDes(getString(R.string.fingerprint_verification));
            mFingerprintSwitchLightTbtn = mFingerprintSwitchTv.getIv_switch_light();
        } else {
            mFingerprintSwitchTv.setVisibility(View.GONE);
        }

    }


    public void initData() {
        myPrefs = SharedPreferenceUtil.getInstance(this);

        try {
            if (myPrefs.readBoolean(ConstantUtil.NUM_PWD_CHECK)) {
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
                if (myPrefs.readBoolean(ConstantUtil.FINGERPRINT_CHECK)) {
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
            if (myPrefs.readBoolean(ConstantUtil.OPEN_TEST)) {
                mOpenTestTb.setChecked(true);
            } else {
                mOpenTestTb.setChecked(false);
            }
        } catch (NullPointerException e) {
            LogUtil.e(TAG, "初始化自动连接失败" + e);
            mOpenTestTb.setChecked(false);
        }
        mCtx = this;
        mDialog = DialogFactory.getInstance(this);
        mScanQRHelper = new ScanQRHelper(this, this);
        mOpenUnlockDownloadTv.setChecked(myPrefs.readBoolean(ConstantUtil.IS_DMT_TEST));
        mCheckSnTv.setChecked(myPrefs.readBoolean(ConstantUtil.CHECK_DEVICE_SN));
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
        mTitleTv.setOnClickListener(this);
        mQueryLpcdNv.setOnClickListener(this);

        mOpenUnlockDownloadTv.getIv_switch_light().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doOnClick(R.id.tw_open_unlock_download);
            }
        });

        mCheckSnTv.getIv_switch_light().setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                doOnClick(R.id.tw_check_sn);
            }
        });
    }

    @Override
    public void onFingerprintAuthenticationSucceeded() {
        LogUtil.i(TGA, "指纹验证成功");
        myPrefs.
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
            case R.id.tv_title:
                mCount++;
                if (mCount >= 5) {
                    mCount = 0;
                    if (mSetDevInfoNv.getVisibility() == View.GONE) {
                        mSetDevInfoNv.setVisibility(View.VISIBLE);
                        mQueryLpcdNv.setVisibility(View.VISIBLE);
                        mOpenUnlockDownloadTv.setChecked(myPrefs.readBoolean(ConstantUtil.IS_DMT_TEST));
                        mOpenUnlockDownloadTv.setVisibility(View.VISIBLE);
                        mCheckSnTv.setVisibility(View.VISIBLE);
                    }
                }
                break;
            case R.id.system_set_modify_pwd:
                Intent intent = new Intent(SystemSettingsActivity.this, LockScreenActivity.class);
                intent.putExtra(ConstantUtil.IS_RETURN, true);
                SystemSettingsActivity.this.startActivityForResult(intent.
                        putExtra(ConstantUtil.TYPE, ConstantUtil.MODIFY_PASSWORD), REQUEST_CODE_PASSWORD);
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
                mScanQRHelper.scanQr();
                break;
            case R.id.next_query_lpcd:
                Intent lpcdIntent = new Intent(this, LpcdTestActivity.class);
                startActivity(lpcdIntent);
                break;
            case R.id.next_multi_language:
                Intent langnageIntent = new Intent(this, LanguageActivity.class);
                startActivity(langnageIntent);
                break;
            default:
                doOnClick(v.getId());
                break;
        }
    }


    private void doOnClick(@IdRes int idRes) {
        switch (idRes) {
            case R.id.system_set_switch_password:
                if (!myPrefs.readBoolean(ConstantUtil.NUM_PWD_CHECK)) {
                    mPromptDialog = DialogUtils.createTipsDialogWithConfirmAndCancel(SystemSettingsActivity.this,
                            getString(R.string.pwd_not_set));
                    if (!mPromptDialog.isShowing()) {
                        mPromptDialog.show();
                    }
                    mPromptDialog.findViewById(R.id.dialog_cancel_btn).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mNumPwdSwitchLightTBtn.setChecked(false);
                            mPromptDialog.cancel();
                        }
                    });
                    mPromptDialog.findViewById(R.id.dialog_confirm_btn).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(SystemSettingsActivity.this, LockScreenActivity.class);
                            intent.putExtra(ConstantUtil.IS_RETURN, true);
                            SystemSettingsActivity.this.startActivityForResult(intent.
                                    putExtra(ConstantUtil.TYPE, ConstantUtil.SETTING_PASSWORD), REQUEST_CODE_PASSWORD);
                            mPromptDialog.cancel();
                        }
                    });
                } else {
                    Intent intent = new Intent(SystemSettingsActivity.this, LockScreenActivity.class);
                    intent.putExtra(ConstantUtil.IS_RETURN, true);
                    SystemSettingsActivity.this.startActivityForResult(intent.
                            putExtra(ConstantUtil.TYPE, ConstantUtil.LOGIN_PASSWORD), REQUEST_CODE_PASSWORD);
                }
                break;

            case R.id.system_set_switch_fingerprint:
                if (!myPrefs.readBoolean(ConstantUtil.FINGERPRINT_CHECK)) {
                    if (!mIsPwdRequired) {
                        mFingerprintDialog = DialogUtils.createTipsDialogWithCancel(SystemSettingsActivity.this,
                                getString(R.string.pwd_not_set_yet));
                        mFingerprintDialog.show();
                        mFingerprintDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                mFingerprintSwitchLightTbtn.setChecked(false);
                            }
                        });
                    } else if (mIsFP == ConstantUtil.FP_NO_KEYGUARDSECURE) {
                        mFingerprintDialog = DialogUtils.createTipsDialogWithCancel(SystemSettingsActivity.this,
                                getString(R.string.pwd_screen_not_set));
                        mFingerprintDialog.show();
                        mFingerprintDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                mFingerprintSwitchLightTbtn.setChecked(false);
                            }
                        });
                    } else if (mIsFP == ConstantUtil.FP_NO_FINGERPRINT) {
                        mFingerprintDialog = DialogUtils.createTipsDialogWithCancel(SystemSettingsActivity.this,
                                getString(R.string.fp_not_set));
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
                myPrefs.writeBoolean(ConstantUtil.OPEN_TEST, mOpenTestTb.isChecked());
                Intent result = new Intent();
                result.putExtra(ConstantUtil.OPEN_TEST, mOpenTestTb.isChecked());
                result.setAction(BleMsg.STR_RSP_OPEN_TEST);
                LocalBroadcastManager.getInstance(SystemSettingsActivity.this).sendBroadcast(result);
                break;
            case R.id.tw_open_unlock_download:
                if (myPrefs.readBoolean(ConstantUtil.IS_DMT_TEST)) {
                    mOpenUnlockDownloadTv.setChecked(false);
                } else {
                    mOpenUnlockDownloadTv.setChecked(true);
                }
                myPrefs.writeBoolean(ConstantUtil.IS_DMT_TEST, mOpenUnlockDownloadTv.getIv_switch_light().isChecked());
                break;
            case R.id.tw_check_sn:
                if (myPrefs.readBoolean(ConstantUtil.CHECK_DEVICE_SN)) {
                    mCheckSnTv.setChecked(false);
                } else {
                    mCheckSnTv.setChecked(true);
                }
                myPrefs.writeBoolean(ConstantUtil.CHECK_DEVICE_SN, mCheckSnTv.getIv_switch_light().isChecked());
                break;
        }
    }

    @Override
    protected void onResume() {
        if (myPrefs.readBoolean(ConstantUtil.NUM_PWD_CHECK) &&
                myPrefs.readString(ConstantUtil.NUM_PWD) != "") {
            myPrefs.writeBoolean(ConstantUtil.NUM_PWD_CHECK, true);
            mNumPwdSwitchLightTBtn.setChecked(true);
            mIsPwdRequired = true;
        } else {
            myPrefs.writeBoolean(ConstantUtil.NUM_PWD_CHECK, false);
            mNumPwdSwitchLightTBtn.setChecked(false);
            mIsPwdRequired = false;
        }
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {

            if (requestCode == ConstantUtil.SCAN_QRCODE_REQUEST_CODE) {
                String content = data.getStringExtra(Constant.CODED_CONTENT);
                LogUtil.d(TAG, "content = " + content);
                String[] dvInfo = content.split(",");
                if (dvInfo.length == 3 && dvInfo[0].length() == 18 && dvInfo[1].length() == 12 && dvInfo[2].length() == 15) {
                    mSn = dvInfo[0];
                    mBleMac = dvInfo[1];
                    mNodeId = dvInfo[2];

                    Bundle bundle = new Bundle();
                    bundle.putString(BleMsg.KEY_BLE_MAC, mBleMac);
                    bundle.putString(BleMsg.KEY_NODE_SN, mSn);
                    bundle.putString(BleMsg.KEY_NODE_ID, mNodeId);
                    LogUtil.d(TAG, "mac = " + mBleMac + '\n' +
                            " sn = " + mSn + "\n" +
                            "mNodeId = " + mNodeId);
                    bundle.putByte(BleMsg.KEY_BLE_CONNECT_TYPE, Device.BLE_SET_DEVICE_INFO_CONNECT_TYPE);
                    Intent intent = new Intent();
                    intent.setClass(this, LockDetectingActivity.class);
                    intent.putExtras(bundle);
                    startActivity(intent);
                } else {
                    ToastUtil.show(this, getString(R.string.plz_scan_correct_qr), Toast.LENGTH_LONG);
                }
            } else if (requestCode == REQUEST_CODE_PASSWORD) {
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
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mScanQRHelper.getPermissionHelper().requestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUESTCODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        || !shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    if (!SystemUtils.isNetworkAvailable(this)) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        ToastUtil.show(this, getString(R.string.plz_open_wifi), Toast.LENGTH_LONG);
                        return;
                    }
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

    /**
     * 添加设备成功响应函数
     *
     * @param deviceInfo 新设备信息
     */
    @Override
    public void onAuthenticationSuccess(DeviceInfo deviceInfo) {
    }

    /**
     * 添加失败响应函数
     */
    @Override
    public void onAuthenticationFailed() {
    }

}

