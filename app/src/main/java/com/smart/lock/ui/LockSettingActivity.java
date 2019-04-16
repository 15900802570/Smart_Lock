package com.smart.lock.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceStatus;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceLogDao;
import com.smart.lock.db.dao.DeviceStatusDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.db.dao.TempPwdDao;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.SystemUtils;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.ToggleSwitchDefineView;
import com.smart.lock.widget.NextActivityDefineView;
import com.smart.lock.widget.BtnSettingDefineView;

import com.smart.lock.R;

import java.util.Arrays;

public class LockSettingActivity extends AppCompatActivity {

    private ToggleSwitchDefineView mIntelligentLockTs;
    private ToggleSwitchDefineView mAntiPrizingAlarmTs;
    private ToggleSwitchDefineView mCombinationLockTs;
    private ToggleSwitchDefineView mNormallyOpenTs;
    private ToggleSwitchDefineView mVoicePromptTs;

    private BtnSettingDefineView mRolledBackTimeBs;

    private String TAG = "LockSettingActivity";

    private DeviceInfo mDefaultDevice;
    private DeviceStatus mDeviceStatus;

    private BleManagerHelper mBleManagerHelper;

    private BottomSheetDialog mBottomSheetDialog; //时间设置弹框
    private Dialog mWarningDialog;
    private int mSetTime;
    private boolean mRestore = false;

    private TextView mSetRolledBackTime5sTv;
    private TextView mSetRolledBackTime8sTv;
    private TextView mSetRolledBackTime10sTv;

    private boolean mIsConnected = true; //蓝牙连接状态

    private String[] mPermission = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private int REQUESTCODE = 0;

    private boolean mVisibility = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_setting);
        initView();
        initData();
        initEvent();
    }

    private void initView() {
        mIntelligentLockTs = findViewById(R.id.ts_intelligent_lock);
        mAntiPrizingAlarmTs = findViewById(R.id.ts_anti_prizing_alarm);
        mCombinationLockTs = findViewById(R.id.ts_combination_lock);
        mNormallyOpenTs = findViewById(R.id.ts_normally_open);
        mVoicePromptTs = findViewById(R.id.ts_voice_prompt);

        mRolledBackTimeBs = findViewById(R.id.bs_rolled_back_time);

        NextActivityDefineView mVersionInfoNa = findViewById(R.id.next_version_info);
        NextActivityDefineView mSelfCheckNa = findViewById(R.id.next_self_check);
        NextActivityDefineView mOtaUpdateNa = findViewById(R.id.next_ota_update);
        NextActivityDefineView mFactoryResetNa = findViewById(R.id.next_factory_reset);

        mIntelligentLockTs.setDes(getResources().getString(R.string.intelligent_lock));
        mAntiPrizingAlarmTs.setDes(getResources().getString(R.string.anti_prizing_alarm));
        mCombinationLockTs.setDes(getResources().getString(R.string.combination_lock));
        mNormallyOpenTs.setDes(getResources().getString(R.string.normally_open));
        mVoicePromptTs.setDes(getResources().getString(R.string.voice_prompt));

        mRolledBackTimeBs.setDes(getResources().getString(R.string.rolled_back_time));

        mVersionInfoNa.setDes(getResources().getString(R.string.version_info));
        mSelfCheckNa.setDes(getResources().getString(R.string.self_check));
        mOtaUpdateNa.setDes(getResources().getString(R.string.ota_update));
        mOtaUpdateNa.setVisibility(View.GONE);
        mFactoryResetNa.setDes(getResources().getString(R.string.restore_the_factory_settings));

        mBottomSheetDialog = DialogUtils.createBottomSheetDialog(this, R.layout.bottom_sheet_set_unlock_time, R.id.design_bottom_sheet);
        mSetRolledBackTime5sTv = mBottomSheetDialog.findViewById(R.id.set_time_5s);
        mSetRolledBackTime8sTv = mBottomSheetDialog.findViewById(R.id.set_time_8s);
        mSetRolledBackTime10sTv = mBottomSheetDialog.findViewById(R.id.set_time_10s);
    }

    private void initData() {
        try {
            mDefaultDevice = (DeviceInfo) getIntent().getSerializableExtra(BleMsg.KEY_DEFAULT_DEVICE);
            LogUtil.d(TAG, "Default = " + mDefaultDevice);
            mBleManagerHelper = BleManagerHelper.getInstance(this, false);
            LocalBroadcastManager.getInstance(this).registerReceiver(lockSettingReceiver, intentFilter());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        // 查询或者创建状态表
        mDeviceStatus = DeviceStatusDao.getInstance(this).queryOrCreateByNodeId(mDefaultDevice.getDeviceNodeId());
        setStatus();
    }

    /**
     * 设置状态
     */
    private void setStatus() {
        if (mDeviceStatus != null) {
            mIntelligentLockTs.setChecked(mDeviceStatus.isIntelligentLockCore());
            mAntiPrizingAlarmTs.setChecked(mDeviceStatus.isAntiPrizingAlarm());
            mCombinationLockTs.setChecked(mDeviceStatus.isCombinationLock());
            mNormallyOpenTs.setChecked(mDeviceStatus.isNormallyOpen());
            mVoicePromptTs.setChecked(mDeviceStatus.isVoicePrompt());

            mSetTime = mDeviceStatus.getRolledBackTime();
            mRolledBackTimeBs.setBtnDes(String.valueOf(mSetTime) + getResources().getString(R.string.s));
        }
    }

    /**
     * 主要设置ToggleSwitchBtn的响应函数，其他响应函数在onClick中设置
     */
    private void initEvent() {

        //智能锁芯
        mIntelligentLockTs.getIv_switch_light().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doClick(R.string.intelligent_lock);
            }
        });

        //防撬报警
        mAntiPrizingAlarmTs.getIv_switch_light().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doClick(R.string.anti_prizing_alarm);
            }
        });

        //组合开锁
        mCombinationLockTs.getIv_switch_light().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doClick(R.string.combination_lock);
            }
        });

        //常开功能
        mNormallyOpenTs.getIv_switch_light().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doClick(R.string.normally_open);
            }
        });

        //语音提示
        mVoicePromptTs.getIv_switch_light().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doClick(R.string.voice_prompt);
            }
        });

    }

    private static IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.STR_RSP_SECURE_CONNECTION);
        intentFilter.addAction(BleMsg.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleMsg.STR_RSP_MSG1E_ERRCODE);
        intentFilter.addAction(BleMsg.STR_RSP_SET_TIMEOUT);
        intentFilter.addAction(BleMsg.STR_RSP_OPEN_TEST);
        intentFilter.addAction(BleMsg.STR_RSP_MSG1C_VERSION);
        return intentFilter;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVisibility = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVisibility = false;
    }

    /**
     * 广播接收
     */
    private final BroadcastReceiver lockSettingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                // 4.2.3 MSG 1C
                if (action.equals(BleMsg.STR_RSP_MSG1C_VERSION)) {
                    if (!mVisibility) {
                        return;
                    }
                    String sn = StringUtil.asciiDeBytesToCharString(intent.getByteArrayExtra(BleMsg.KEY_NODE_SN));
                    String swVer = StringUtil.asciiDeBytesToCharString(intent.getByteArrayExtra(BleMsg.KEY_SW_VER));
                    String hwVer = StringUtil.asciiDeBytesToCharString(intent.getByteArrayExtra(BleMsg.KEY_HW_VER));
                    LogUtil.d(TAG, "SW VERSION = " + swVer + '\n' +
                            "HW VERSION = " + hwVer + '\n' +
                            "SN = " + sn);
                    mDefaultDevice.setDeviceSn(sn);
                    mDefaultDevice.setDeviceSwVersion(swVer);
                    mDefaultDevice.setDeviceHwVersion(hwVer);
                    DeviceInfoDao.getInstance(LockSettingActivity.this).updateDeviceInfo(mDefaultDevice);
                    Intent mIntent = new Intent(LockSettingActivity.this, VersionInfoActivity.class);
                    startActivity(mIntent);
                }
                if (action.equals(BleMsg.STR_RSP_MSG1E_ERRCODE)) {
                    final byte[] errCode = intent.getByteArrayExtra(BleMsg.KEY_ERROR_CODE);
                    switch (errCode[3]) {
                        case 0x15: // 组合开锁设置成功
                            if (mDeviceStatus.isCombinationLock()) {
                                mCombinationLockTs.setChecked(false);
                                mDeviceStatus.setCombinationLock(false);
                            } else {
                                mCombinationLockTs.setChecked(true);
                                mDeviceStatus.setCombinationLock(true);
                            }
                            break;
                        case 0x16:  //组合开锁设置失败
                            break;

                        case 0x17:  //常开功能设置成功
                            if (mDeviceStatus.isNormallyOpen()) {
                                mNormallyOpenTs.setChecked(false);
                                mDeviceStatus.setNormallyOpen(false);
                            } else {
                                mNormallyOpenTs.setChecked(true);
                                mDeviceStatus.setNormallyOpen(true);
                            }
                            break;
                        case 0x18:  //常开功能设置失败
                            break;

                        case 0x19:  //语言提示设置成功
                            if (mDeviceStatus.isVoicePrompt()) {
                                mVoicePromptTs.setChecked(false);
                                mDeviceStatus.setVoicePrompt(false);
                            } else {
                                mVoicePromptTs.setChecked(true);
                                mDeviceStatus.setVoicePrompt(true);
                            }
                            break;
                        case 0x1a:  //语音提示设置失败
                            break;

                        case 0x1b:  //智能锁芯设置成功
                            if (mDeviceStatus.isIntelligentLockCore()) {
                                mIntelligentLockTs.setChecked(false);
                                mDeviceStatus.setIntelligentLockCore(false);
                            } else {
                                mIntelligentLockTs.setChecked(true);
                                mDeviceStatus.setIntelligentLockCore(true);
                            }
                            break;
                        case 0x1c:  //智能锁芯设置失败
                            break;

                        case 0x1d:  //防撬报警设置成功
                            if (mDeviceStatus.isAntiPrizingAlarm()) {
                                mAntiPrizingAlarmTs.setChecked(false);
                                mDeviceStatus.setAntiPrizingAlarm(false);
                            } else {
                                mAntiPrizingAlarmTs.setChecked(true);
                                mDeviceStatus.setAntiPrizingAlarm(true);
                            }
                            break;
                        case 0x1e:  //防撬报警设置失败
                            break;

                        case 0x20:  //回锁时间设置成功
                            mDeviceStatus.setRolledBackTime(mSetTime);
                            mRolledBackTimeBs.setBtnDes(String.valueOf(mSetTime) + LockSettingActivity.this.getResources().getString(R.string.s));
                            break;
                        case 0x22:  //恢复出厂设置成功
                            if (mRestore) {
                                ToastUtil.show(
                                        LockSettingActivity.this,
                                        R.string.restore_the_factory_settings_success,
                                        Toast.LENGTH_LONG);
                                mBleManagerHelper.getBleCardService().disconnect();
                                finish();
                            } else {
                                finish();
                            }
                            LogUtil.d(TAG, "恢复出厂设置成功");
                            break;
                    }
                    DeviceStatusDao.getInstance(LockSettingActivity.this).updateDeviceStatus(mDeviceStatus);
                }
                if (action.equals(BleMsg.ACTION_GATT_DISCONNECTED)) {
                    mIsConnected = false;
                }
                if (action.equals(BleMsg.ACTION_GATT_CONNECTED)) {
                    mIsConnected = true;
                }
            }
        }
    };

    public void onClick(View view) {
        if (mIsConnected) {
            switch (view.getId()) {
                case R.id.iv_back:
                    finish();

                case R.id.ts_intelligent_lock:  //智能锁芯
                    doClick(R.string.intelligent_lock);
                    break;

                case R.id.ts_anti_prizing_alarm:    //防撬报警
                    doClick(R.string.anti_prizing_alarm);
                    break;

                case R.id.ts_combination_lock:      //组合开锁
                    doClick(R.string.combination_lock);
                    break;

                case R.id.ts_normally_open:         //常开功能
                    doClick(R.string.normally_open);
                    break;

                case R.id.ts_voice_prompt:          //语言提示
                    doClick(R.string.voice_prompt);
                    break;

                case R.id.bs_rolled_back_time:      //设置回锁时间
                    switch (mSetTime) {
                        case 5:
                            mSetRolledBackTime5sTv.setTextColor(getResources().getColor(R.color.lite_blue));
                            mSetRolledBackTime8sTv.setTextColor(getResources().getColor(R.color.gray1));
                            mSetRolledBackTime10sTv.setTextColor(getResources().getColor(R.color.gray1));
                            break;
                        case 8:
                            mSetRolledBackTime5sTv.setTextColor(getResources().getColor(R.color.gray1));
                            mSetRolledBackTime8sTv.setTextColor(getResources().getColor(R.color.lite_blue));
                            mSetRolledBackTime10sTv.setTextColor(getResources().getColor(R.color.gray1));
                            break;
                        case 10:
                            mSetRolledBackTime5sTv.setTextColor(getResources().getColor(R.color.gray1));
                            mSetRolledBackTime8sTv.setTextColor(getResources().getColor(R.color.gray1));
                            mSetRolledBackTime10sTv.setTextColor(getResources().getColor(R.color.lite_blue));
                            break;
                    }
                    mBottomSheetDialog.show();      //显示弹窗
                    break;
                case R.id.next_version_info:        //查看版本信息
                    mBleManagerHelper.getBleCardService().sendCmd19((byte) 7);
                    break;
                case R.id.next_ota_update:
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        if (!SystemUtils.isNetworkAvailable(this)) {
                            ToastUtil.show(this, getString(R.string.plz_open_wifi), Toast.LENGTH_LONG);
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                            return;
                        }
                        if (mDefaultDevice != null && mBleManagerHelper.getServiceConnection()) {
                            Intent intent = new Intent(this, OtaUpdateActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);
                            intent.putExtras(bundle);
                            startActivity(intent);
                        } else
                            Toast.makeText(this, getString(R.string.plz_reconnect), Toast.LENGTH_LONG).show();
                    } else {
                        ActivityCompat.requestPermissions(this, mPermission, REQUESTCODE);
                    }//ota命令
                    break;
                case R.id.next_factory_reset:
                    mWarningDialog = DialogUtils.createWarningDialog(this, getResources().getString(R.string.restore_warning));
                    mWarningDialog.show();
                    break;
            }
        } else {
            ToastUtil.show(this, getResources().getString(R.string.ble_disconnect), Toast.LENGTH_LONG);
            setStatus();
        }
    }

    private void doClick(int value) {
        if (mIsConnected) {
            switch (value) {
                case R.string.intelligent_lock: //智能锁芯
                    if (mDeviceStatus.isIntelligentLockCore()) {
                        mBleManagerHelper.getBleCardService().sendCmd19((byte) 13);
                    } else {
                        mBleManagerHelper.getBleCardService().sendCmd19((byte) 12);
                    }
                    break;

                case R.string.anti_prizing_alarm:   //防撬报警
                    if (mDeviceStatus.isAntiPrizingAlarm()) {
                        mBleManagerHelper.getBleCardService().sendCmd19((byte) 15);
                    } else {
                        mBleManagerHelper.getBleCardService().sendCmd19((byte) 14);
                    }
                    break;

                case R.string.combination_lock:     //组合开锁
                    if (mDeviceStatus.isCombinationLock()) {
                        mBleManagerHelper.getBleCardService().sendCmd19((byte) 2);
                    } else {
                        mBleManagerHelper.getBleCardService().sendCmd19((byte) 1);
                    }
                    break;

                case R.string.normally_open:    //常开功能
                    if (mDeviceStatus.isNormallyOpen()) {
                        mBleManagerHelper.getBleCardService().sendCmd19((byte) 4);
                    } else {
                        mBleManagerHelper.getBleCardService().sendCmd19((byte) 3);
                    }
                    break;
                case R.string.voice_prompt:     //语言提示
                    if (mDeviceStatus.isVoicePrompt()) {
                        mBleManagerHelper.getBleCardService().sendCmd19((byte) 6);
                    } else {
                        mBleManagerHelper.getBleCardService().sendCmd19((byte) 5);
                    }
                    break;
            }
        } else {
            ToastUtil.show(this, getResources().getString(R.string.ble_disconnect), Toast.LENGTH_LONG);
            setStatus();
        }
    }

    public void setOnClick(View view) {
        switch (view.getId()) {
            case R.id.set_time_5s:
                LogUtil.d(TAG, "set_time_5s");
                mSetTime = 5;
                mBleManagerHelper.getBleCardService().sendCmd1D((byte) 5);
                break;
            case R.id.set_time_8s:
                LogUtil.d(TAG, "set_time_8s");
                mSetTime = 8;
                mBleManagerHelper.getBleCardService().sendCmd1D((byte) 8);
                break;
            case R.id.set_time_10s:
                LogUtil.d(TAG, "set_time_10s");
                mSetTime = 10;
                mBleManagerHelper.getBleCardService().sendCmd1D((byte) 10);
                break;
            default:
                break;
        }
        mBottomSheetDialog.cancel();
    }

    public void warningOnClick(View view) {
        switch (view.getId()) {
            case R.id.warning_cancel_btn:

                break;
            case R.id.warning_confirm_btn:
                mBleManagerHelper.getBleCardService().sendCmd19((byte) 8);
                clearAllDataOfApplication();
                break;
        }
        mWarningDialog.cancel();
    }

    /**
     * 清除应用所有的数据
     */
    public void clearAllDataOfApplication() {
        if (
                DeviceInfoDao.getInstance(this).deleteAll() != -1 &&
                        DeviceKeyDao.getInstance(this).deleteAll() != -1 &&
                        DeviceLogDao.getInstance(this).deleteAll() != -1 &&
                        DeviceStatusDao.getInstance(this).deleteAll() != -1 &&
                        DeviceUserDao.getInstance(this).deleteAll() != -1 &&
                        TempPwdDao.getInstance(this).deleteAll() != -1) {
            mRestore = true;
        } else {
            ToastUtil.show(this, R.string.restore_the_factory_settings_failed, Toast.LENGTH_LONG);
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

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(lockSettingReceiver);
        super.onDestroy();
    }
}
