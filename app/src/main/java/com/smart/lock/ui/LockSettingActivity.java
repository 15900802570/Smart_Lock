package com.smart.lock.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceStatus;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceStatusDao;
import com.smart.lock.db.helper.DtComFunHelper;
import com.smart.lock.entity.Device;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.SystemUtils;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.BtnSettingDefineView;
import com.smart.lock.widget.NextActivityDefineView;
import com.smart.lock.widget.TimePickerDefineDialog;
import com.smart.lock.widget.ToggleSwitchDefineView;

import java.text.ParseException;
import java.util.Arrays;

public class LockSettingActivity extends AppCompatActivity implements UiListener, TimePickerDefineDialog.onTimePickerListener {

    private ToggleSwitchDefineView mIntelligentLockTs;
    private ToggleSwitchDefineView mAntiPrizingAlarmTs;
    private ToggleSwitchDefineView mCombinationLockTs;
    private ToggleSwitchDefineView mNormallyOpenTs;
    private ToggleSwitchDefineView mVoicePromptTs;
    private ToggleSwitchDefineView mLogEnableTs;

    private BtnSettingDefineView mSetRolledBackTimeBs;
    private BtnSettingDefineView mSetSupportCardTypeBs;
    private BtnSettingDefineView mSetPowerSavingTimeBs;

    private String TAG = "LockSettingActivity";

    private DeviceInfo mDefaultDevice;
    private short mUserID;
    private DeviceStatus mDeviceStatus;

    private BleManagerHelper mBleManagerHelper;

    private BottomSheetDialog mSetTimesBottomSheetDialog; //时间设置弹框
    private BottomSheetDialog mSetSupportCardBottomDialog;
    private boolean mSetSupportOrdinaryCards = false;
    private boolean mTempSetSupportCards = false;
    private Dialog mWarningDialog;
    private Dialog mWaitingDialog;

    private int mSetTime;
    private boolean mRestore = false;

    private TextView mSetRolledBackTime5sTv;
    private TextView mSetRolledBackTime8sTv;
    private TextView mSetRolledBackTime10sTv;
    private TextView mTitleTv;
    private NextActivityDefineView mFactoryResetNa;

    private TextView mSetSafetyCardTv;
    private TextView mSetOrdinaryCardTv;


    private String[] mPermission = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private int REQUEST_CODE = 0;

    private boolean mVisibility = true;
    private int[] mTimePickerValue = {23, 0, 7, 0};
    private int[] mTempTimePickerValue = {12, 12, 12, 12};
    private int TIME_PICKER_CODE = 1;

    private Device mDevice;
    private int mCount = 0; //打开测试条例

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
        mLogEnableTs = findViewById(R.id.ts_log_enable);
        mTitleTv = findViewById(R.id.tv_title);

        mSetRolledBackTimeBs = findViewById(R.id.bs_rolled_back_time);
        mSetSupportCardTypeBs = findViewById(R.id.bs_support_card_type);
        mSetPowerSavingTimeBs = findViewById(R.id.bs_set_power_saving_time);

        NextActivityDefineView mVersionInfoNa = findViewById(R.id.next_version_info);
        NextActivityDefineView mSelfCheckNa = findViewById(R.id.next_self_check);
        NextActivityDefineView mOtaUpdateNa = findViewById(R.id.next_ota_update);
        mFactoryResetNa = findViewById(R.id.next_factory_reset);

        mIntelligentLockTs.setDes(getResources().getString(R.string.intelligent_lock));
        mAntiPrizingAlarmTs.setDes(getResources().getString(R.string.anti_prizing_alarm));
        mCombinationLockTs.setDes(getResources().getString(R.string.combination_lock));
        mNormallyOpenTs.setDes(getResources().getString(R.string.normally_open));
        mVoicePromptTs.setDes(getResources().getString(R.string.voice_prompt));
        mLogEnableTs.setDes(getString(R.string.lock_log));
        mLogEnableTs.setVisibility(View.GONE); //测试使用

        mSetRolledBackTimeBs.setDes(getResources().getString(R.string.rolled_back_time));
        mSetSupportCardTypeBs.setDes(getString(R.string.support_types_of_card));
        mSetPowerSavingTimeBs.setDes(getString(R.string.power_saving_time_period));

        mVersionInfoNa.setDes(getResources().getString(R.string.version_info));
        mSelfCheckNa.setDes(getResources().getString(R.string.self_check));
        mOtaUpdateNa.setDes(getResources().getString(R.string.ota_update));
        mOtaUpdateNa.setVisibility(View.VISIBLE);
        mFactoryResetNa.setDes(getResources().getString(R.string.restore_the_factory_settings));

        mSetTimesBottomSheetDialog = DialogUtils.createBottomSheetDialog(this, R.layout.bottom_sheet_set_unlock_time, R.id.design_bottom_sheet);
        mSetRolledBackTime5sTv = mSetTimesBottomSheetDialog.findViewById(R.id.set_time_5s);
        mSetRolledBackTime8sTv = mSetTimesBottomSheetDialog.findViewById(R.id.set_time_8s);
        mSetRolledBackTime10sTv = mSetTimesBottomSheetDialog.findViewById(R.id.set_time_10s);

        mSetSupportCardBottomDialog = DialogUtils.createBottomSheetDialog(this, R.layout.bottom_sheet_set_support_card, R.id.design_bottom_sheet);
        mSetSafetyCardTv = mSetSupportCardBottomDialog.findViewById(R.id.set_support_safety_card);
        mSetOrdinaryCardTv = mSetSupportCardBottomDialog.findViewById(R.id.set_support_ordinary_card);

    }

    private void initData() {
        try {
            mDefaultDevice = (DeviceInfo) getIntent().getSerializableExtra(BleMsg.KEY_DEFAULT_DEVICE);
            mUserID = getIntent().getShortExtra(BleMsg.KEY_USER_ID, (short) 101);
            LogUtil.d(TAG, "Default = " + mDefaultDevice);
            mBleManagerHelper = BleManagerHelper.getInstance(this);
            mBleManagerHelper.addUiListener(this);
            mDevice = Device.getInstance(this);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        // 查询或者创建状态表
        mDeviceStatus = DeviceStatusDao.getInstance(this).queryOrCreateByNodeId(mDefaultDevice.getDeviceNodeId());
        setStatus();
        if (mUserID > 0 & mUserID < 100) {
            mFactoryResetNa.setVisibility(View.VISIBLE);
            mSetSupportCardTypeBs.setVisibility(View.VISIBLE);
        } else {
            mFactoryResetNa.setVisibility(View.GONE);
            mSetSupportCardTypeBs.setVisibility(View.GONE);
        }
    }

    private void enableTest() {
        mLogEnableTs.setVisibility(View.VISIBLE);
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
            mSetRolledBackTimeBs.setBtnDes(mSetTime + getResources().getString(R.string.s));

            mSetSupportOrdinaryCards = mDeviceStatus.isM1Support();
            mSetSupportCardTypeBs.setBtnDes(mSetSupportOrdinaryCards ? getString(R.string.ordinary_card) : getString(R.string.safety_card));

            int startTimeInt = mDeviceStatus.getPowerSavingStartTime();
            int endTimeInt = mDeviceStatus.getPowerSavingEndTime();
            if (startTimeInt == ConstantUtil.INVALID_POWER_SAVE_TIME || endTimeInt == ConstantUtil.INVALID_POWER_SAVE_TIME) {
                mSetPowerSavingTimeBs.setBtnDes(getString(R.string.close));
            } else if (startTimeInt >= 0 && endTimeInt < 3000) {
                mTimePickerValue[0] = startTimeInt / 100;
                mTimePickerValue[1] = startTimeInt % 100;
                mTimePickerValue[2] = endTimeInt / 100;
                mTimePickerValue[3] = endTimeInt % 100;
                String startTime = ConstantUtil.HOUR[mTimePickerValue[0]] + ":" + ConstantUtil.MINUTE[mTimePickerValue[1]];
                String endTime = ConstantUtil.HOUR[mTimePickerValue[2]] + ":" + ConstantUtil.MINUTE[mTimePickerValue[3]];
                mSetPowerSavingTimeBs.setBtnDes(startTime + " -- " + endTime);
            } else {
                mSetPowerSavingTimeBs.setBtnDes(getString(R.string.close));
            }
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

        //门锁日志
        mLogEnableTs.getIv_switch_light().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doClick(R.string.lock_log);
            }
        });

        //门锁抬头
        mTitleTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doClick(R.string.lock_settings);
            }
        });

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


    public void onClick(View view) {
        if (mDevice.getState() == Device.BLE_CONNECTED || view.getId() == R.id.iv_back) {
            switch (view.getId()) {
                case R.id.iv_back:
                    finish();
                    break;
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
                    mSetTimesBottomSheetDialog.show();      //显示弹窗
                    break;
                case R.id.bs_support_card_type:
                    if (mSetSupportOrdinaryCards) {
                        mSetSafetyCardTv.setTextColor(getResources().getColor(R.color.gray1));
                        mSetOrdinaryCardTv.setTextColor(getResources().getColor(R.color.lite_blue));
                    } else {
                        mSetSafetyCardTv.setTextColor(getResources().getColor(R.color.lite_blue));
                        mSetOrdinaryCardTv.setTextColor(getResources().getColor(R.color.gray1));
                    }
                    mSetSupportCardBottomDialog.show();
                    break;
                case R.id.bs_set_power_saving_time:     //设置蓝牙省电时间段
                    //    private Dialog mTimePickerDialog;
                    TimePickerDefineDialog mTimePickerDefineDialog = new TimePickerDefineDialog(mTimePickerValue,
                            true,
                            this.getString(R.string.setting_power_saving_time_period),
                            TIME_PICKER_CODE);
                    mTimePickerDefineDialog.show(this.getSupportFragmentManager(), "timePicker");
                    break;
                case R.id.next_version_info:        //查看版本信息
                    mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_CHECK_VERSION);
                    break;
                case R.id.next_self_check:
                    Intent selfCheckIntent = new Intent(this, SelfCheckActivity.class);
                    Bundle selfCheckBundle = new Bundle();
                    selfCheckBundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);
                    selfCheckIntent.putExtras(selfCheckBundle);
                    startActivity(selfCheckIntent);
                    break;
                case R.id.next_ota_update:
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        if (!SystemUtils.isNetworkAvailable(this)) {
                            ToastUtil.show(this, getString(R.string.plz_open_wifi), Toast.LENGTH_LONG);
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                            return;
                        }
                        if (mDefaultDevice != null && mDevice.getState() == Device.BLE_CONNECTED) {
                            Intent intent = new Intent(this, OtaUpdateActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);
                            intent.putExtras(bundle);
                            startActivity(intent);
                        } else
                            Toast.makeText(this, getString(R.string.plz_reconnect), Toast.LENGTH_LONG).show();
                    } else {
                        ActivityCompat.requestPermissions(this, mPermission, REQUEST_CODE);
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
        if (mDevice.getState() == Device.BLE_CONNECTED) {
            switch (value) {
                case R.string.lock_settings:
                    mCount++;
                    if (mCount >= 5) {
                        enableTest();
                    }
                    break;
                case R.string.intelligent_lock: //智能锁芯
                    if (mDeviceStatus.isIntelligentLockCore()) {
                        mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_INTELLIGENT_LOCK_CORE_CLOSE);
                    } else {
                        mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_INTELLIGENT_LOCK_CORE_OPEN);
                    }
                    break;

                case R.string.anti_prizing_alarm:   //防撬报警
                    if (mDeviceStatus.isAntiPrizingAlarm()) {
                        mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_ANTI_PRYING_ALARM_CLOSE);
                    } else {
                        mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_ANTI_PRYING_ALARM_OPEN);
                    }
                    break;

                case R.string.combination_lock:     //组合开锁
                    if (mDeviceStatus.isCombinationLock()) {
                        mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_UNENABLE_COMBINATION_UNLOCK);
                    } else {
                        mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_ENABLE_COMBINATION_UNLOCK);
                    }
                    break;

                case R.string.normally_open:    //常开功能
                    if (mDeviceStatus.isNormallyOpen()) {
                        mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_NORMALLY_CLOSE);
                    } else {
                        mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_NORMALLY_OPEN);
                    }
                    break;
                case R.string.voice_prompt:     //语言提示
                    if (mDeviceStatus.isVoicePrompt()) {
                        mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_VOICE_PROMPT_CLOSE);
                    } else {
                        mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_VOICE_PROMPT_OPEN);
                    }
                    break;

                case R.string.lock_log: //门锁日志
                    if (mLogEnableTs.isChecked()) {
                        mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_LOCK_LOG_ENABLE);
                    } else {
                        mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_LOCK_LOG_UNENABLE);
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
            case R.id.set_support_ordinary_card:
                LogUtil.d(TAG, "set_ordinary_card");
                mTempSetSupportCards = true;
                mBleManagerHelper.getBleCardService().sendCmd19((byte) 16);
                break;
            case R.id.set_support_safety_card:
                LogUtil.d(TAG, "set_safety_card");
                mTempSetSupportCards = false;
                mBleManagerHelper.getBleCardService().sendCmd19((byte) 17);
                break;
            default:
                break;
        }
        if (mSetSupportCardBottomDialog.isShowing()) {
            mSetSupportCardBottomDialog.cancel();
        }
        if (mSetTimesBottomSheetDialog.isShowing()) {
            mSetTimesBottomSheetDialog.cancel();
        }
    }

    public void warningOnClick(View view) {
        switch (view.getId()) {
            case R.id.warning_cancel_btn:

                break;
            case R.id.warning_confirm_btn:
                mWaitingDialog = DialogUtils.createLoadingDialog(this, getResources().getString(R.string.lock_reset));
                mWaitingDialog.show();
                mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_RESTORE_FACTORY_SETTINGS);
                if (DtComFunHelper.restoreFactorySettings(this, mDefaultDevice)) {
                    mRestore = true;
                } else {
                    ToastUtil.show(this, R.string.restore_the_factory_settings_failed, Toast.LENGTH_LONG);
                }
                break;
        }
        DialogUtils.closeDialog(mWarningDialog);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
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
        super.onDestroy();
        mBleManagerHelper.removeUiListener(this);
    }

    /**
     * 自定义TimePickerDialog回调函数
     *
     * @param value       int[] 选择参数
     * @param requestCode int 请求参数
     */
    @Override
    public void onTimePickerClickConfirm(int[] value, int requestCode) {

        LogUtil.d(TAG, "requestCode = " + requestCode);
        if (requestCode == TIME_PICKER_CODE) {
            if (value != null) {
                LogUtil.d(TAG, "value = " +
                        value[0] + "\n" +
                        value[1] + '\n' +
                        value[2] + "\n" +
                        value[3]);
                mTempTimePickerValue = value;
                mBleManagerHelper.getBleCardService().sendCmd2D(getPowerSavingTime(value));
            } else {
                mBleManagerHelper.getBleCardService().sendCmd2D(new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
                Arrays.fill(mTempTimePickerValue, 0, 4, (byte) 12);
            }
        }
    }

    private byte[] getPowerSavingTime(int[] time) {
        StringBuilder nowTime = new StringBuilder(DateTimeUtil.stampToDate(String.valueOf(System.currentTimeMillis())));
        nowTime.replace(17, 19, "00");
        String startTime = nowTime.replace(
                11,
                16,
                ConstantUtil.HOUR[time[0]] + ":" + ConstantUtil.MINUTE[time[1]]).toString();
        String endTime = nowTime.replace(
                11,
                16,
                ConstantUtil.HOUR[time[2]] + ":" + ConstantUtil.MINUTE[time[3]]).toString();
        // 获取时间戳 s
        try {
            long startStamp = DateTimeUtil.dateToStamp(startTime) / 1000;
            long endStamp = DateTimeUtil.dateToStamp(endTime) / 1000;
            if (startStamp > endStamp) {
                endStamp += 86400;
            }
            byte[] startTimeBytes = new byte[4];
            byte[] endTimeBytes = new byte[4];
            byte[] TimeBytes = new byte[8];
            StringUtil.int2Bytes((int) startStamp, startTimeBytes);
            StringUtil.int2Bytes((int) endStamp, endTimeBytes);
            System.arraycopy(startTimeBytes, 0, TimeBytes, 0, 4);
            System.arraycopy(endTimeBytes, 0, TimeBytes, 4, 4);
            LogUtil.d(TAG, "startStamp = " + (int) startStamp + '\n' +
                    "endStamp = " + (int) endStamp + '\n' +
                    "TimeBytes = " + Arrays.toString(TimeBytes));
            return TimeBytes;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

    }

    @Override
    public void deviceStateChange(Device device, int state) {
        mDevice = device;
    }

    @Override
    public void dispatchUiCallback(Message msg, Device device, int type) {
        LogUtil.i(TAG, "dispatchUiCallback!");
        mDevice = device;
        Bundle extra = msg.getData();
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_1E:
                final byte[] errCode = extra.getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode != null)
                    dispatchErrorCode(errCode[3]);
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_1C:
                if (!mVisibility) {
                    return;
                }
                String sn = StringUtil.asciiDeBytesToCharString(extra.getByteArray(BleMsg.KEY_NODE_SN));
                String swVer = StringUtil.asciiDeBytesToCharString(extra.getByteArray(BleMsg.KEY_SW_VER));
                String hwVer = StringUtil.asciiDeBytesToCharString(extra.getByteArray(BleMsg.KEY_HW_VER));
                LogUtil.d(TAG, "SW VERSION = " + swVer + '\n' +
                        "HW VERSION = " + hwVer + '\n' +
                        "SN = " + sn);
                mDefaultDevice.setDeviceSn(sn);
                mDefaultDevice.setDeviceSwVersion(swVer);
                mDefaultDevice.setDeviceHwVersion(hwVer);
                DeviceInfoDao.getInstance(LockSettingActivity.this).updateDeviceInfo(mDefaultDevice);
                Intent mIntent = new Intent(LockSettingActivity.this, VersionInfoActivity.class);
                startActivity(mIntent);
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_2E:
                final byte[] errCode2E = msg.getData().getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode2E != null) {
                    String startTime;
                    String endTime;
                    int[] closeByte = {12, 12, 12, 12};
                    int[] initByte = {23, 0, 7, 0};
                    switch (errCode2E[3]) {
                        case BleMsg.TYPE_SET_POWER_SAVE_SUCCESS:
                            if (Arrays.equals(mTempTimePickerValue, closeByte)) {
                                mTimePickerValue = initByte;
                            } else {
                                mTimePickerValue = mTempTimePickerValue;
                            }
                        case BleMsg.TYPE_SET_POWER_SAVE_FAILED:
                            if (Arrays.equals(mTempTimePickerValue, closeByte) &&
                                    Arrays.equals(mTimePickerValue, initByte)) {
                                mDeviceStatus.setPowerSavingStartTime(ConstantUtil.INVALID_POWER_SAVE_TIME);
                                mDeviceStatus.setPowerSavingEndTime(ConstantUtil.INVALID_POWER_SAVE_TIME);
                                mSetPowerSavingTimeBs.setBtnDes(getString(R.string.close));
                            } else {
                                startTime = ConstantUtil.HOUR[mTimePickerValue[0]] + ":" + ConstantUtil.MINUTE[mTimePickerValue[1]];
                                endTime = ConstantUtil.HOUR[mTimePickerValue[2]] + ":" + ConstantUtil.MINUTE[mTimePickerValue[3]];
                                mSetPowerSavingTimeBs.setBtnDes(startTime + " -- " + endTime);
                                mDeviceStatus.setPowerSavingStartTime((mTimePickerValue[0]) * 100 + mTimePickerValue[1]);
                                mDeviceStatus.setPowerSavingEndTime((mTimePickerValue[2]) * 100 + mTimePickerValue[3]);
                            }
                            DeviceStatusDao.getInstance(this).updateDeviceStatus(mDeviceStatus);
                            break;
                        default:
                            break;
                    }
                }
                break;
            default:
                LogUtil.e(TAG, "Message type : " + msg.getType() + " can not be handler");
                break;
        }

    }

    @Override
    public void reConnectBle(Device device) {
        mDevice = device;
    }

    @Override
    public void sendFailed(Message msg) {
        int exception = msg.getException();
        switch (exception) {
            case Message.EXCEPTION_TIMEOUT:
                LogUtil.e(msg.getType() + " can't receiver msg!");
                break;
            case Message.EXCEPTION_SEND_FAIL:
                LogUtil.e(msg.getType() + " send failed!");
                LogUtil.e(TAG, "msg exception : " + msg.toString());
                break;
            default:
                break;
        }
    }

    @Override
    public void addUserSuccess(Device device) {

    }

    @Override
    public void scanDevFailed() {

    }

    private void dispatchErrorCode(byte errCode) {
        LogUtil.i(TAG, "errCode : " + errCode);
        switch (errCode) {
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
                mSetRolledBackTimeBs.setBtnDes(mSetTime + LockSettingActivity.this.getResources().getString(R.string.s));
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
                mWaitingDialog.cancel();
                LogUtil.d(TAG, "恢复出厂设置成功");
                break;
            case 0x26: //设置卡片类型成功
                mSetSupportOrdinaryCards = mTempSetSupportCards;
            case 0x27: //设置卡片类型失败
                if (mSetSupportOrdinaryCards) {
                    mSetSupportCardTypeBs.setBtnDes(getString(R.string.ordinary_card));
                } else {
                    mSetSupportCardTypeBs.setBtnDes(getString(R.string.safety_card));
                }
                mDeviceStatus.setM1Support(mSetSupportOrdinaryCards);
                break;
            case 0x28:  //log打印设置成功
                showMessage(getString(R.string.lock_log_set_success));
                break;
            case 0x29:  //log打印设置失败
                showMessage(getString(R.string.lock_log_set_failed));
                break;
            default:
                break;
        }
        DeviceStatusDao.getInstance(LockSettingActivity.this).updateDeviceStatus(mDeviceStatus);
    }

    /**
     * 吐司提示
     *
     * @param msg 提示信息
     */
    protected void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
