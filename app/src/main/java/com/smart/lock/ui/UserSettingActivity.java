package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.entity.Device;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.TimePickerAloneDialog;
import com.smart.lock.widget.TimePickerDefineDialog;
import com.smart.lock.widget.TimePickerWithDateDefineDialog;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class UserSettingActivity extends BaseActivity implements View.OnClickListener, UiListener, TimePickerWithDateDefineDialog.onTimeAndDatePickerListener, TimePickerAloneDialog.onTimePickerListener {
    private final static String TAG = UserSettingActivity.class.getSimpleName();

    private Toolbar mUsetSetTb;
    private TextView mTitleTv;
    private EditText mEtMome;
    private TextView mStartDateTv;
    private TextView mEndDateTv;
    private Button mConfirmBtn;

    private Dialog mLoadDialog;
    private Handler mHandler;
    private Calendar mCalendar;
    private DeviceUser mTempUser;

    private CheckBox mLifeCb;
    private CheckBox mUnlockTimeCb1;
    private CheckBox mUnlockTimeCb2;
    private CheckBox mUnlockTimeCb3;

    private TextView mFirstStartTime;
    private TextView mFirstEndTime;
    private TextView mSecondStartTime;
    private TextView mSecondEndTime;
    private TextView mThirdStartTime;
    private TextView mThirtEndTime;

    private static final int TEMP_USER_START_DATE = 1;
    private static final int TEMP_USER_END_DATE = 2;

    private long mStartDate, mEndDate;

    /**
     * 蓝牙
     */
    private BleManagerHelper mBleManagerHelper;
    private Device mDevice;  //当前连接设备信息

    private DeviceInfo mDefaultDevice; //默认设备
    private DeviceKeyDao mDeviceKeyDao;//设备信息管理者

    private static final int TEMP_KEY_FIRST_START_TIME = 1;
    private static final int TEMP_KEY_FIRST_END_TIME = 2;
    private static final int TEMP_KEY_SECOND_START_TIME = 3;
    private static final int TEMP_KEY_SECOND_END_TIME = 4;
    private static final int TEMP_KEY_THIRD_START_TIME = 5;
    private static final int TEMP_KEY_THIRD_END_TIME = 6;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_setting);
        initView();
        initData();
        initActionBar();
        initEvent();
    }

    @SuppressLint("WrongViewCast")
    private void initView() {
        mUsetSetTb = findViewById(R.id.tb_temp_user_set);
        mTitleTv = findViewById(R.id.tv_title);
        mEtMome = findViewById(R.id.et_memo);
        mStartDateTv = findViewById(R.id.tv_start_date);
        mEndDateTv = findViewById(R.id.tv_end_date);
        mConfirmBtn = findViewById(R.id.btn_confirm);

        mLifeCb = findViewById(R.id.cb_life);
        mUnlockTimeCb1 = findViewById(R.id.cb_unlock_time);
        mUnlockTimeCb2 = findViewById(R.id.cb_unlock_time2);
        mUnlockTimeCb3 = findViewById(R.id.cb_unlock_time3);

        mFirstStartTime = findViewById(R.id.tv_start_time);
        mFirstEndTime = findViewById(R.id.tv_end_time);
        mSecondStartTime = findViewById(R.id.tv_start_time2);
        mSecondEndTime = findViewById(R.id.tv_end_time2);
        mThirdStartTime = findViewById(R.id.tv_start_time3);
        mThirtEndTime = findViewById(R.id.tv_end_time3);
    }

    @SuppressLint("SetTextI18n")
    private void initData() {
        mDefaultDevice = DeviceInfoDao.getInstance(this).queryFirstData("device_default", true);
        mDeviceKeyDao = DeviceKeyDao.getInstance(this);
        mHandler = new Handler();
        mBleManagerHelper = BleManagerHelper.getInstance(this);
        mBleManagerHelper.addUiListener(this);
        mDevice = Device.getInstance(this);
        mCalendar = Calendar.getInstance();
        mTempUser = (DeviceUser) getIntent().getExtras().getSerializable(BleMsg.KEY_TEMP_USER);

        if (mTempUser.getStTsBegin() == null)
            mFirstStartTime.setText("08:00");
        else {
            mFirstStartTime.setText(mTempUser.getStTsBegin());
            mUnlockTimeCb1.setChecked(true);
        }
        mLoadDialog = DialogUtils.createLoadingDialog(this, getResources().getString(R.string.data_loading));
        initTime();
        initDate();
    }

    private void initTime() {
        if ((mTempUser.getStTsBegin() == null && mTempUser.getStTsEnd() == null) || (mTempUser.getStTsBegin().equals("00:00") && mTempUser.getStTsEnd().equals("00:00"))) {
            mFirstStartTime.setText("08:00");
            mFirstEndTime.setText("09:00");
            mUnlockTimeCb1.setChecked(false);
        } else {
            mFirstStartTime.setText(mTempUser.getStTsBegin());
            mFirstEndTime.setText(mTempUser.getStTsEnd());
            mUnlockTimeCb1.setChecked(true);
        }

        if ((mTempUser.getNdTsBegin() == null && mTempUser.getNdTsend() == null) || (mTempUser.getNdTsBegin().equals("00:00") && mTempUser.getNdTsend().equals("00:00"))) {
            mSecondStartTime.setText("11:00");
            mSecondEndTime.setText("12:00");
            mUnlockTimeCb2.setChecked(false);
        } else {
            mSecondStartTime.setText(mTempUser.getNdTsBegin());
            mSecondEndTime.setText(mTempUser.getNdTsend());
            mUnlockTimeCb2.setChecked(true);
        }

        if ((mTempUser.getThTsBegin() == null && mTempUser.getThTsEnd() == null) || (mTempUser.getThTsBegin().equals("00:00") && mTempUser.getThTsEnd().equals("00:00"))) {
            mThirdStartTime.setText("17:00");
            mThirtEndTime.setText("18:00");
            mUnlockTimeCb3.setChecked(false);
        } else {
            mThirdStartTime.setText(mTempUser.getThTsBegin());
            mThirtEndTime.setText(mTempUser.getThTsEnd());
            mUnlockTimeCb3.setChecked(true);
        }

    }

    private void initEvent() {
        mStartDateTv.setOnClickListener(this);
        mEndDateTv.setOnClickListener(this);
        mConfirmBtn.setOnClickListener(this);

        mFirstStartTime.setOnClickListener(this);
        mFirstEndTime.setOnClickListener(this);
        mSecondStartTime.setOnClickListener(this);
        mSecondEndTime.setOnClickListener(this);
        mThirdStartTime.setOnClickListener(this);
        mThirtEndTime.setOnClickListener(this);
    }

    private void initActionBar() {
        mTitleTv.setText(R.string.user_setting);
        mUsetSetTb.setNavigationIcon(R.mipmap.btn_back);
        setSupportActionBar(mUsetSetTb);
        mUsetSetTb.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBleManagerHelper.removeUiListener(UserSettingActivity.this);
                finish();
            }
        });
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    @SuppressLint("SetTextI18n")
    private void initDate() {
        if (StringUtil.checkNotNull(mTempUser.getUserName())) {
            mEtMome.setText(mTempUser.getUserName());
        }
        String day = mCalendar.get(Calendar.YEAR) + "-" + (mCalendar.get(Calendar.MONTH) + 1) + "-" + mCalendar.get(Calendar.DAY_OF_MONTH);
        if ((mTempUser.getLcBegin() == null && mTempUser.getLcEnd() == null) || (mTempUser.getLcBegin().equals("0000") && mTempUser.getLcEnd().equals("0000"))) {
            mStartDateTv.setText(day + " 00:00");
            mEndDateTv.setText(day + " 23:59");
            try {
                mStartDate = DateTimeUtil.dateToStamp(day + " 00:00:00") / 1000;
                mEndDate = DateTimeUtil.dateToStamp(day + " 23:59:00") / 1000;
            } catch (ParseException e) {
                e.printStackTrace();
            }
            mLifeCb.setChecked(false);
        } else {
            mStartDateTv.setText(DateTimeUtil.stampToDate(mTempUser.getLcBegin() + "000").substring(0, 16));
            mLifeCb.setChecked(true);
            mStartDate = Long.valueOf(mTempUser.getLcBegin());
            mEndDateTv.setText(DateTimeUtil.stampToDate(mTempUser.getLcEnd() + "000").substring(0, 16));
            mEndDate = Long.valueOf(mTempUser.getLcEnd());
        }

    }

    private void showTimePickerDialog(final int tag) {
        mCalendar = Calendar.getInstance();

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, AlertDialog.THEME_HOLO_LIGHT,
                new TimePickerDialog.OnTimeSetListener() {
//                    boolean flag = false;

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
//                        LogUtil.d(TAG, "flag = " + flag);
//                        if (!flag) {
//                            flag = true;
//                            return;
//                        }

                        String minuteStr = String.valueOf(minute).length() > 1 ? String.valueOf(minute) : "0" + minute;
                        LogUtil.d(TAG, "minuteStr : " + minuteStr);
                        String hour = "0";
                        if (hourOfDay < 10) {
                            hour = "0" + hourOfDay;
                        } else
                            hour = String.valueOf(hourOfDay);
                        switch (tag) {
                            case TEMP_KEY_FIRST_START_TIME:
                                mFirstStartTime.setText(hour + getString(R.string.colon) + minuteStr);
                                break;
                            case TEMP_KEY_FIRST_END_TIME:
                                if (!timeCompare(mFirstStartTime.getText().toString(), hour + getString(R.string.colon) + minuteStr)) {
                                    showMessage("开始时间大于或等于结束时间");
                                    showTimePickerDialog(TEMP_KEY_FIRST_END_TIME);
                                } else
                                    mFirstEndTime.setText(hour + getString(R.string.colon) + minuteStr);
                                break;
                            case TEMP_KEY_SECOND_START_TIME:
                                mSecondStartTime.setText(hour + getString(R.string.colon) + minuteStr);
                                break;
                            case TEMP_KEY_SECOND_END_TIME:
                                if (!timeCompare(mSecondStartTime.getText().toString(), hour + getString(R.string.colon) + minuteStr)) {
                                    showMessage("开始时间大于或等于结束时间");
                                    showTimePickerDialog(TEMP_KEY_SECOND_END_TIME);
                                } else
                                    mSecondEndTime.setText(hour + getString(R.string.colon) + minuteStr);
                                break;
                            case TEMP_KEY_THIRD_START_TIME:
                                mThirdStartTime.setText(hour + getString(R.string.colon) + minuteStr);
                                break;
                            case TEMP_KEY_THIRD_END_TIME:
                                if (!timeCompare(mSecondStartTime.getText().toString(), hour + getString(R.string.colon) + minuteStr)) {
                                    showMessage("开始时间大于或等于结束时间");
                                    showTimePickerDialog(TEMP_KEY_THIRD_END_TIME);
                                } else {
                                    mThirtEndTime.setText(hour + getString(R.string.colon) + minuteStr);
                                }
                                break;
                            default:
                                break;
                        }

                    }
                }, mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE), true);

        timePickerDialog.show();
    }


    /**
     * 时间选择器
     *
     * @param tag 标签
     */
    private void showTimePickerDefineDialog(int tag) {
        mCalendar = Calendar.getInstance();
        int[] defaultTime = {mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE)};
        TimePickerAloneDialog timePickerDialog = new TimePickerAloneDialog(
                defaultTime, false,
                getString(R.string.set_start_time),
                tag
        );

        timePickerDialog.show(this.getSupportFragmentManager(), "TimePicker");
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

        if (value != null) {
            String minuteStr = String.valueOf(value[1]).length() > 1 ? String.valueOf(value[1]) : "0" + value[1];
            String hour = "0";
            if (value[0] < 10) {
                hour = "0" + value[0];
            } else
                hour = String.valueOf(value[0]);
            switch (requestCode) {
                case TEMP_KEY_FIRST_START_TIME:
                    mFirstStartTime.setText(hour + getString(R.string.colon) + minuteStr);
                    break;
                case TEMP_KEY_FIRST_END_TIME:
                    mFirstEndTime.setText(hour + getString(R.string.colon) + minuteStr);
                    break;
                case TEMP_KEY_SECOND_START_TIME:
                    mSecondStartTime.setText(hour + getString(R.string.colon) + minuteStr);
                    break;
                case TEMP_KEY_SECOND_END_TIME:
                    mSecondEndTime.setText(hour + getString(R.string.colon) + minuteStr);
                    break;
                case TEMP_KEY_THIRD_START_TIME:
                    mThirdStartTime.setText(hour + getString(R.string.colon) + minuteStr);
                    break;
                case TEMP_KEY_THIRD_END_TIME:
                    mThirtEndTime.setText(hour + getString(R.string.colon) + minuteStr);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 日期选择器
     *
     * @param tag 标签
     */
    private void showDatePickerDialog(int tag) throws ParseException {
        TimePickerWithDateDefineDialog timePickerDialog;

        if (tag == TEMP_USER_START_DATE) {
            timePickerDialog = new TimePickerWithDateDefineDialog(
                    this,
                    getString(R.string.set_start_time),
                    mStartDate,
                    0,
                    TEMP_USER_START_DATE
            );
        } else {
            timePickerDialog = new TimePickerWithDateDefineDialog(
                    this,
                    getString(R.string.set_end_time),
                    mEndDate,
                    mStartDate,
                    TEMP_USER_END_DATE
            );
        }
        timePickerDialog.show(this.getSupportFragmentManager(), "TimePicker");
    }


    /**
     * 吐司提示
     *
     * @param msg 提示信息
     */
    protected void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        mTempUser = DeviceUserDao.getInstance(this).queryUser(mTempUser.getDevNodeId(), mTempUser.getUserId());//更新状态
        switch (v.getId()) {
            case R.id.tv_start_date:
                try {
                    showDatePickerDialog(TEMP_USER_START_DATE);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.tv_end_date:
                try {
                    showDatePickerDialog(TEMP_USER_END_DATE);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.tv_start_time:
                showTimePickerDefineDialog(TEMP_KEY_FIRST_START_TIME);
                break;
            case R.id.tv_end_time:
                showTimePickerDefineDialog(TEMP_KEY_FIRST_END_TIME);
                break;
            case R.id.tv_start_time2:
                showTimePickerDefineDialog(TEMP_KEY_SECOND_START_TIME);
                break;
            case R.id.tv_end_time2:
                showTimePickerDefineDialog(TEMP_KEY_SECOND_END_TIME);
                break;
            case R.id.tv_start_time3:
                showTimePickerDefineDialog(TEMP_KEY_THIRD_START_TIME);
                break;
            case R.id.tv_end_time3:
                showTimePickerDefineDialog(TEMP_KEY_THIRD_END_TIME);
                break;

            case R.id.btn_confirm:
                if (StringUtil.checkIsNull(mEtMome.getText().toString())) {
                    showMessage("备注名不能为空！");
                    return;
                }
//                if (mLifeCb.isChecked() || mUnlockTimeCb1.isChecked() || mUnlockTimeCb2.isChecked() || mUnlockTimeCb3.isChecked()) {
//
//                    if (mLifeCb.isChecked()) {
//                        if (mStartDate < mEndDate) {
//                            if (checkLifeCycle()) {
//                                mEndDateTv.setError(getString(R.string.life_cycle_not_changed));
//                                return;
//                            }
//
//                        } else {
//                            mEndDateTv.setError(getString(R.string.set_date_error));
//                            return;
//                        }
//                    }

                if (mUnlockTimeCb1.isChecked() && !timeCompare(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString())) {
                    showMessage("开始时间大于或等于结束时间");
                    showTimePickerDefineDialog(TEMP_KEY_FIRST_END_TIME);
                    return;
                }

                if (mUnlockTimeCb2.isChecked() && !timeCompare(mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString())) {
                    showMessage("开始时间大于或等于结束时间");
                    showTimePickerDefineDialog(TEMP_KEY_SECOND_END_TIME);
                    return;
                }

                if (mUnlockTimeCb3.isChecked() && !timeCompare(mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString())) {
                    showMessage("开始时间大于或等于结束时间");
                    showTimePickerDefineDialog(TEMP_KEY_SECOND_END_TIME);
                    return;
                }
                sendUnlocktime();
//                } else
//                    showMessage("请选择需要设置有效期或开锁时段！");

                break;
            default:
                break;
        }
    }

    private boolean checkLifeCycle() {
        boolean ret = false;

        if (StringUtil.checkNotNull(mTempUser.getLcBegin()) && StringUtil.checkNotNull(mTempUser.getLcEnd())) {
            try {
                boolean beginCompare = Long.valueOf(mTempUser.getLcBegin()) == (DateTimeUtil.dateToStampDay(mStartDateTv.getText().toString()) / 1000);
                boolean end = Long.valueOf(mTempUser.getLcEnd()) == (DateTimeUtil.dateToStampDay(mEndDateTv.getText().toString()) / 1000);

                if (beginCompare && end) {
                    ret = true;
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return ret;
    }

    private byte[] getLifeCycle() {
        byte[] lifeCycle = new byte[8];

        if (mLifeCb.isChecked()) {
            int beginTime = (int) mStartDate;
            int endTime = (int) mEndDate;
            LogUtil.d(TAG, "beginTime = " + beginTime + " ;endTime = " + endTime);
            byte[] timeBuf = new byte[4];
            StringUtil.int2Bytes(beginTime, timeBuf);
            System.arraycopy(timeBuf, 0, lifeCycle, 0, 4);
            StringUtil.int2Bytes(endTime, timeBuf);
            System.arraycopy(timeBuf, 0, lifeCycle, 4, 4);

        } else {
            Arrays.fill(lifeCycle, 0, lifeCycle.length, (byte) 0x00);
        }

        LogUtil.d(TAG, "life cycle : " + StringUtil.bytesToHexString(lifeCycle, ":"));
        return lifeCycle;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBleManagerHelper.removeUiListener(this);
    }

    @Override
    public void deviceStateChange(Device device, int state) {
        mDevice = device;
        switch (state) {
            case BleMsg.STATE_DISCONNECTED:
                DialogUtils.closeDialog(mLoadDialog);
                showMessage(getString(R.string.ble_disconnect));
                break;
            case BleMsg.STATE_CONNECTED:

                break;
            case BleMsg.GATT_SERVICES_DISCOVERED:

                break;
            default:
                break;
        }
    }

    @Override
    public void dispatchUiCallback(Message msg, Device device, int type) {
        mDevice = device;
        Bundle extra = msg.getData();
        Serializable serializable = extra.getSerializable(BleMsg.KEY_SERIALIZABLE);
        if (serializable != null && !(serializable instanceof DeviceUser || serializable instanceof Short)) {
            return;
        }
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_1E:
            case Message.TYPE_BLE_RECEIVER_CMD_2E:
                final byte[] errCode = msg.getData().getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode != null)
                    dispatchErrorCode(errCode[3]);
                break;

            default:
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
                DialogUtils.closeDialog(mLoadDialog);
                LogUtil.e(msg.getType() + " can't receiver msg!");
                break;
            case Message.EXCEPTION_SEND_FAIL:
                DialogUtils.closeDialog(mLoadDialog);
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
            case BleMsg.TYPE_NO_AUTHORITY:
                showMessage(getString(R.string.no_authority));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_USER_LIFE_UPDATE_SUCCESS:
                if (mLifeCb.isChecked()) {
                    mTempUser.setLcBegin(String.valueOf(mStartDate));
                    mTempUser.setLcEnd(String.valueOf(mEndDate));
                } else {
//                    mTempUser.setLcBegin(String.valueOf(mStartDate));
//                    mTempUser.setLcEnd(String.valueOf(mEndDate));

                    mTempUser.setLcBegin("0000");
                    mTempUser.setLcEnd("0000");
                }
                DeviceUserDao.getInstance(this).updateDeviceUser(mTempUser);

//                if (!(mUnlockTimeCb1.isChecked() || mUnlockTimeCb2.isChecked() || mUnlockTimeCb3.isChecked())) {
//                    showMessage(getString(R.string.set_life_cycle_success));
//
//                    DialogUtils.closeDialog(mLoadDialog);
//                }

                break;

            case BleMsg.TYPE_SET_USER_LIFE_FAILED:
                showMessage("设置失败,用户已暂停!");
                break;
            case BleMsg.TYPE_SET_USER_LIFE_SUCCESS:
                if (mUnlockTimeCb1.isChecked()) {
                    mTempUser.setStTsBegin(mFirstStartTime.getText().toString());
                    mTempUser.setStTsEnd(mFirstEndTime.getText().toString());
                } else {
                    mTempUser.setStTsBegin("00:00");
                    mTempUser.setStTsEnd("00:00");
                }
                if (mUnlockTimeCb2.isChecked()) {
                    mTempUser.setNdTsBegin(mSecondStartTime.getText().toString());
                    mTempUser.setNdTsend(mSecondEndTime.getText().toString());
                } else {
                    mTempUser.setNdTsBegin("00:00");
                    mTempUser.setNdTsend("00:00");
                }
                if (mUnlockTimeCb3.isChecked()) {
                    mTempUser.setThTsBegin(mThirdStartTime.getText().toString());
                    mTempUser.setThTsEnd(mThirtEndTime.getText().toString());
                } else {
                    mTempUser.setThTsBegin("00:00");
                    mTempUser.setThTsEnd("00:00");
                }
                DialogUtils.closeDialog(mLoadDialog);
                mTempUser.setUserName(mEtMome.getText().toString().trim());
                DeviceUserDao.getInstance(this).updateDeviceUser(mTempUser);
                showMessage("设置成功!");
                break;
            default:
                break;
        }

    }

    @Override
    public void onTimeAndDatePickerClickConfirm(long timeStamp, int requestCode) {
        String timeStr = DateTimeUtil.timeStamp2Date(String.valueOf(timeStamp), "yyyy-MM-dd HH:mm");
        switch (requestCode) {
            case TEMP_USER_START_DATE:
                mStartDateTv.setText(timeStr);
                mStartDate = timeStamp;
                break;
            case TEMP_USER_END_DATE:
                mEndDateTv.setText(timeStr);
                mEndDate = timeStamp;
                break;
            default:
                break;
        }

        LogUtil.d(TAG, "startTime = " + mStartDate + '\n' +
                "endTime = " + mEndDate);
    }

    /**
     * 比较时间的大小
     *
     * @param startTime
     * @param endTime
     * @return
     */
    private boolean timeCompare(String startTime, String endTime) {
        SimpleDateFormat CurrentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date1 = mCalendar.get(Calendar.YEAR) + "-" + mCalendar.get(Calendar.MONTH) + "-" + mCalendar.get(Calendar.HOUR_OF_DAY) + " " + startTime + ":00";
        String date2 = mCalendar.get(Calendar.YEAR) + "-" + mCalendar.get(Calendar.MONTH) + "-" + mCalendar.get(Calendar.HOUR_OF_DAY) + " " + endTime + ":00";
        try {

            Date beginDate = CurrentTime.parse(date1);
            Date endDate = CurrentTime.parse(date2);
            if ((endDate.getTime() - beginDate.getTime()) > 0) {
                return true;
            } else {
                return false;
            }

        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.item_confirm:

                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 发送时段命令
     */
    private void sendUnlocktime() {
        if (mUnlockTimeCb1.isChecked() && mUnlockTimeCb2.isChecked() && mUnlockTimeCb3.isChecked()) {
            boolean ret = compareDate(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
                    mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString(),
                    mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString());
            if (ret) {
                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    if (mLifeCb.isChecked()) {
                        if (mStartDate >= mEndDate) {
//                            if (checkLifeCycle()) {
//                                showMessage(getString(R.string.life_cycle_not_changed));
//                                return;
//                            }
//                        } else {
                            showMessage(getString(R.string.set_date_error));
                            return;
                        }
                    }

                    if (!mLoadDialog.isShowing()) {
                        mLoadDialog.show();
                    }
                    mBleManagerHelper.getBleCardService().sendCmd29(mTempUser.getUserId(), getLifeCycle());
                    mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_THREE_UNLOCK_TIME, mTempUser.getUserId(), getUnlockTime(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
                            mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString(),
                            mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()), BleMsg.INT_DEFAULT_TIMEOUT);
                } else {
                    showMessage(getString(R.string.disconnect_ble));
                }

            } else {
                showMessage("时间段重复，请检查！");
            }
        } else if (mUnlockTimeCb1.isChecked() && mUnlockTimeCb2.isChecked() && !mUnlockTimeCb3.isChecked()) {
            if (!DateTimeUtil.compareDate(DateTimeUtil.checkDate(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString()),
                    DateTimeUtil.checkDate(mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString()))) {

                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    if (mLifeCb.isChecked()) {
                        if (mStartDate >= mEndDate) {
//                            if (checkLifeCycle()) {
//                                showMessage(getString(R.string.life_cycle_not_changed));
//                                return;
//                            }
//                        } else {
                            showMessage(getString(R.string.set_date_error));
                            return;
                        }
                    }
                    if (!mLoadDialog.isShowing()) {
                        mLoadDialog.show();
                    }
                    mBleManagerHelper.getBleCardService().sendCmd29(mTempUser.getUserId(), getLifeCycle());
                    mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_TWO_UNLOCK_TIME, mTempUser.getUserId(),
                            getUnlockTime(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
                                    mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString(),
                                    null, null), BleMsg.INT_DEFAULT_TIMEOUT);
                } else {
                    showMessage(getString(R.string.disconnect_ble));
                }

            } else {
                showMessage("时间段重复，请检查！");
            }

        } else if (mUnlockTimeCb1.isChecked() && !mUnlockTimeCb2.isChecked() && !mUnlockTimeCb3.isChecked()) {
            if (mDevice.getState() == Device.BLE_CONNECTED) {

                if (mLifeCb.isChecked()) {
                    if (mStartDate >= mEndDate) {
//                            if (checkLifeCycle()) {
//                                showMessage(getString(R.string.life_cycle_not_changed));
//                                return;
//                            }
//                        } else {
                        showMessage(getString(R.string.set_date_error));
                        return;
                    }
                }
                if (!mLoadDialog.isShowing()) {
                    mLoadDialog.show();
                }
                mBleManagerHelper.getBleCardService().sendCmd29(mTempUser.getUserId(), getLifeCycle());
                mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_ONE_UNLOCK_TIME, mTempUser.getUserId(),
                        getUnlockTime(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
                                null, null,
                                null, null), BleMsg.INT_DEFAULT_TIMEOUT);
            } else {
                showMessage(getString(R.string.disconnect_ble));
            }

        } else if (!mUnlockTimeCb1.isChecked() && mUnlockTimeCb2.isChecked() && !mUnlockTimeCb3.isChecked()) {
            if (mDevice.getState() == Device.BLE_CONNECTED) {
                if (mLifeCb.isChecked()) {
                    if (mStartDate >= mEndDate) {
//                            if (checkLifeCycle()) {
//                                showMessage(getString(R.string.life_cycle_not_changed));
//                                return;
//                            }
//                        } else {
                        showMessage(getString(R.string.set_date_error));
                        return;
                    }
                }
                if (!mLoadDialog.isShowing()) {
                    mLoadDialog.show();
                }
                mBleManagerHelper.getBleCardService().sendCmd29(mTempUser.getUserId(), getLifeCycle());
                mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_ONE_UNLOCK_TIME, mTempUser.getUserId(),
                        getUnlockTime(null, null,
                                mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString(),
                                null, null), BleMsg.INT_DEFAULT_TIMEOUT);
            } else {
                showMessage(getString(R.string.disconnect_ble));
            }

        } else if (!mUnlockTimeCb1.isChecked() && !mUnlockTimeCb2.isChecked() && mUnlockTimeCb3.isChecked()) {
            if (mDevice.getState() == Device.BLE_CONNECTED) {

                if (mLifeCb.isChecked()) {
                    if (mStartDate >= mEndDate) {
//                            if (checkLifeCycle()) {
//                                showMessage(getString(R.string.life_cycle_not_changed));
//                                return;
//                            }
//                        } else {
                        showMessage(getString(R.string.set_date_error));
                        return;
                    }
                }
                if (!mLoadDialog.isShowing()) {
                    mLoadDialog.show();
                }
                mBleManagerHelper.getBleCardService().sendCmd29(mTempUser.getUserId(), getLifeCycle());
                mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_ONE_UNLOCK_TIME, mTempUser.getUserId(),
                        getUnlockTime(null, null,
                                null, null, mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()), BleMsg.INT_DEFAULT_TIMEOUT);
            } else {
                showMessage(getString(R.string.disconnect_ble));
            }
        } else if (!mUnlockTimeCb1.isChecked() && mUnlockTimeCb2.isChecked() && mUnlockTimeCb3.isChecked()) {

            if (!DateTimeUtil.compareDate(DateTimeUtil.checkDate(mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString()),
                    DateTimeUtil.checkDate(mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()))) {

                if (mDevice.getState() == Device.BLE_CONNECTED) {

                    if (mLifeCb.isChecked()) {
                        if (mStartDate >= mEndDate) {
//                            if (checkLifeCycle()) {
//                                showMessage(getString(R.string.life_cycle_not_changed));
//                                return;
//                            }
//                        } else {
                            showMessage(getString(R.string.set_date_error));
                            return;
                        }
                    }
                    if (!mLoadDialog.isShowing()) {
                        mLoadDialog.show();
                    }
                    mBleManagerHelper.getBleCardService().sendCmd29(mTempUser.getUserId(), getLifeCycle());
                    mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_TWO_UNLOCK_TIME, mTempUser.getUserId(),
                            getUnlockTime(null, null,
                                    mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
                                    mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()), BleMsg.INT_DEFAULT_TIMEOUT);
                } else {
                    showMessage(getString(R.string.disconnect_ble));
                }

            } else {
                showMessage("时间段重复，请检查！");
            }

        } else if (mUnlockTimeCb1.isChecked() && !mUnlockTimeCb2.isChecked() && mUnlockTimeCb3.isChecked()) {
            if (!DateTimeUtil.compareDate(DateTimeUtil.checkDate(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString()),
                    DateTimeUtil.checkDate(mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()))) {

                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    if (mLifeCb.isChecked()) {
                        if (mStartDate >= mEndDate) {
//                            if (checkLifeCycle()) {
//                                showMessage(getString(R.string.life_cycle_not_changed));
//                                return;
//                            }
//                        } else {
                            showMessage(getString(R.string.set_date_error));
                            return;
                        }
                    }
                    mLoadDialog.show();
                    mBleManagerHelper.getBleCardService().sendCmd29(mTempUser.getUserId(), getLifeCycle());
                    mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_TWO_UNLOCK_TIME, mTempUser.getUserId(),
                            getUnlockTime(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
                                    null, null,
                                    mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()), BleMsg.INT_DEFAULT_TIMEOUT);
                } else {
                    showMessage(getString(R.string.disconnect_ble));
                }

            } else {
                showMessage("时间段重复，请检查！");
            }
        } else {
            if (mDevice.getState() == Device.BLE_CONNECTED) {
                DialogUtils.closeDialog(mLoadDialog);

                if (mLifeCb.isChecked()) {
                    if (mStartDate >= mEndDate) {
//                            if (checkLifeCycle()) {
//                                showMessage(getString(R.string.life_cycle_not_changed));
//                                return;
//                            }
//                        } else {
                        showMessage(getString(R.string.set_date_error));
                        return;
                    }
                }
                mLoadDialog.show();
                mBleManagerHelper.getBleCardService().sendCmd29(mTempUser.getUserId(), getLifeCycle());
                mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_TWO_UNLOCK_TIME, mTempUser.getUserId(),
                        getUnlockTime(null, null, null, null, null, null), BleMsg.INT_DEFAULT_TIMEOUT);
            } else {
                showMessage(getString(R.string.disconnect_ble));
            }
        }
//        } else {
//
//            if (mUnlockTimeCb1.isChecked() && mUnlockTimeCb2.isChecked() && mUnlockTimeCb3.isChecked()) {
//                boolean ret = compareDate(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
//                        mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString(),
//                        mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString());
//                if (ret) {
//                    if (mDevice.getState() == Device.BLE_CONNECTED) {
//                        if (!mLoadDialog.isShowing()) {
//                            mLoadDialog.show();
//                        }
//                        mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_THREE_UNLOCK_TIME, mTempUser.getUserId(), getUnlockTime(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
//                                mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString(),
//                                mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()), BleMsg.INT_DEFAULT_TIMEOUT);
//                    } else {
//                        showMessage(getString(R.string.disconnect_ble));
//                    }
//
//                } else {
//                    showMessage("时间段重复，请检查！");
//                }
//            } else if (mUnlockTimeCb1.isChecked() && mUnlockTimeCb2.isChecked() && !mUnlockTimeCb3.isChecked()) {
//                if (!DateTimeUtil.compareDate(DateTimeUtil.checkDate(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString()),
//                        DateTimeUtil.checkDate(mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString()))) {
//
//                    if (mDevice.getState() == Device.BLE_CONNECTED) {
//                        if (!mLoadDialog.isShowing()) {
//                            mLoadDialog.show();
//                        }
//                        mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_TWO_UNLOCK_TIME, mTempUser.getUserId(),
//                                getUnlockTime(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
//                                        mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString(),
//                                        null, null), BleMsg.INT_DEFAULT_TIMEOUT);
//                    } else {
//                        showMessage(getString(R.string.disconnect_ble));
//                    }
//
//                } else {
//                    showMessage("时间段重复，请检查！");
//                }
//
//
//            } else if (mUnlockTimeCb1.isChecked() && !mUnlockTimeCb2.isChecked() && !mUnlockTimeCb3.isChecked()) {
//                if (mDevice.getState() == Device.BLE_CONNECTED) {
//                    if (!mLoadDialog.isShowing()) {
//                        mLoadDialog.show();
//                    }
//                    mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_ONE_UNLOCK_TIME, mTempUser.getUserId(),
//                            getUnlockTime(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
//                                    null, null,
//                                    null, null), BleMsg.INT_DEFAULT_TIMEOUT);
//                } else {
//                    showMessage(getString(R.string.disconnect_ble));
//                }
//
//            } else if (!mUnlockTimeCb1.isChecked() && mUnlockTimeCb2.isChecked() && !mUnlockTimeCb3.isChecked()) {
//                if (mDevice.getState() == Device.BLE_CONNECTED) {
//                    if (!mLoadDialog.isShowing()) {
//                        mLoadDialog.show();
//                    }
//                    mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_ONE_UNLOCK_TIME, mTempUser.getUserId(),
//                            getUnlockTime(null, null,
//                                    mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString(),
//                                    null, null), BleMsg.INT_DEFAULT_TIMEOUT);
//                } else {
//                    showMessage(getString(R.string.disconnect_ble));
//                }
//
//            } else if (!mUnlockTimeCb1.isChecked() && !mUnlockTimeCb2.isChecked() && mUnlockTimeCb3.isChecked()) {
//                if (mDevice.getState() == Device.BLE_CONNECTED) {
//                    if (!mLoadDialog.isShowing()) {
//                        mLoadDialog.show();
//                    }
//                    mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_ONE_UNLOCK_TIME, mTempUser.getUserId(),
//                            getUnlockTime(null, null,
//                                    null, null, mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()), BleMsg.INT_DEFAULT_TIMEOUT);
//                } else {
//                    showMessage(getString(R.string.disconnect_ble));
//                }
//            } else if (!mUnlockTimeCb1.isChecked() && mUnlockTimeCb2.isChecked() && mUnlockTimeCb3.isChecked()) {
//
//                if (!DateTimeUtil.compareDate(DateTimeUtil.checkDate(mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString()),
//                        DateTimeUtil.checkDate(mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()))) {
//
//                    if (mDevice.getState() == Device.BLE_CONNECTED) {
//                        if (!mLoadDialog.isShowing()) {
//                            mLoadDialog.show();
//                        }
//                        mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_TWO_UNLOCK_TIME, mTempUser.getUserId(),
//                                getUnlockTime(null, null,
//                                        mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
//                                        mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()), BleMsg.INT_DEFAULT_TIMEOUT);
//                    } else {
//                        showMessage(getString(R.string.disconnect_ble));
//                    }
//
//                } else {
//                    showMessage("时间段重复，请检查！");
//                }
//
//            } else if (mUnlockTimeCb1.isChecked() && !mUnlockTimeCb2.isChecked() && mUnlockTimeCb3.isChecked()) {
//                if (!DateTimeUtil.compareDate(DateTimeUtil.checkDate(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString()),
//                        DateTimeUtil.checkDate(mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()))) {
//
//                    if (mDevice.getState() == Device.BLE_CONNECTED) {
//                        mLoadDialog.show();
//                        mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_TWO_UNLOCK_TIME, mTempUser.getUserId(),
//                                getUnlockTime(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
//                                        null, null,
//                                        mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()), BleMsg.INT_DEFAULT_TIMEOUT);
//                    } else {
//                        showMessage(getString(R.string.disconnect_ble));
//                    }
//
//                } else {
//                    showMessage("时间段重复，请检查！");
//                }
//            }
//        }
    }

    /**
     * 检测三个时段是否有重合
     *
     * @param firstStartTime
     * @param firstEndTime
     * @param secondStartTime
     * @param secondEndTime
     * @param thirtStartTime
     * @param thirtEndTime
     * @return
     */
    private boolean compareDate(String firstStartTime, String firstEndTime, String secondStartTime, String secondEndTime, String thirtStartTime, String thirtEndTime) {
        boolean ret = DateTimeUtil.compareDate(DateTimeUtil.checkDate(firstStartTime, firstEndTime), DateTimeUtil.checkDate(secondStartTime, secondEndTime));
        LogUtil.d(TAG, " ret1 = " + ret);
        if (ret) {
            return false;
        } else if (DateTimeUtil.compareDate(DateTimeUtil.checkDate(thirtStartTime, thirtEndTime), DateTimeUtil.checkDate(secondStartTime, secondEndTime))) {
            return false;
        } else if (DateTimeUtil.compareDate(DateTimeUtil.checkDate(firstStartTime, firstEndTime), DateTimeUtil.checkDate(thirtStartTime, thirtEndTime))) {
            return false;
        }
        return true;
    }

    /**
     * 获取时间段字符串
     *
     * @param firstBegin
     * @param firstEnd
     * @param secondBegin
     * @param secondEnd
     * @param thirdBegin
     * @param thirdEnd
     * @return
     */
    private byte[] getUnlockTime(String firstBegin, String firstEnd, String secondBegin, String secondEnd, String thirdBegin, String thirdEnd) {
        byte[] unlockTime = new byte[24];
        SimpleDateFormat currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (firstBegin != null && firstEnd != null) {
            String firstBeginStr = mCalendar.get(Calendar.YEAR) + "-" + mCalendar.get(Calendar.MONTH) + "-" + mCalendar.get(Calendar.HOUR_OF_DAY) + " " + firstBegin + ":00";
            String firstEndDateStr = mCalendar.get(Calendar.YEAR) + "-" + mCalendar.get(Calendar.MONTH) + "-" + mCalendar.get(Calendar.HOUR_OF_DAY) + " " + firstEnd + ":00";
            LogUtil.d(TAG, "firstBeginStr = " + firstBeginStr + " ;firstEndDateStr = " + firstEndDateStr);
            try {
                Date firstBeginDate = currentTime.parse(firstBeginStr);
                Date firstEndDate = currentTime.parse(firstEndDateStr);

                int firstBeginTime = Long.valueOf((firstBeginDate.getTime() / 1000)).intValue();
                int firstEndTime = Long.valueOf((firstEndDate.getTime() / 1000)).intValue();
                LogUtil.d(TAG, "firstBeginTime = " + firstBeginTime + " ;firstEndTime = " + firstEndTime);
                byte[] firstTimeBuf = new byte[4];
                StringUtil.int2Bytes(firstBeginTime, firstTimeBuf);
                System.arraycopy(firstTimeBuf, 0, unlockTime, 0, 4);
                StringUtil.int2Bytes(firstEndTime, firstTimeBuf);
                System.arraycopy(firstTimeBuf, 0, unlockTime, 4, 4);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            Arrays.fill(unlockTime, 0, 8, (byte) 0);
        }

        if (secondBegin != null && secondEnd != null) {
            String secondBeginStr = mCalendar.get(Calendar.YEAR) + "-" + mCalendar.get(Calendar.MONTH) + "-" + mCalendar.get(Calendar.HOUR_OF_DAY) + " " + secondBegin + ":00";
            String secondEndDateStr = mCalendar.get(Calendar.YEAR) + "-" + mCalendar.get(Calendar.MONTH) + "-" + mCalendar.get(Calendar.HOUR_OF_DAY) + " " + secondEnd + ":00";
            LogUtil.d(TAG, "secondBeginStr = " + secondBeginStr + " ;secondEndDateStr = " + secondEndDateStr);
            try {
                Date secondBeginDate = currentTime.parse(secondBeginStr);
                Date secondEndDate = currentTime.parse(secondEndDateStr);

                int secondBeginTime = Long.valueOf((secondBeginDate.getTime() / 1000)).intValue();
                int secondEndTime = Long.valueOf((secondEndDate.getTime() / 1000)).intValue();
                LogUtil.d(TAG, "secondBeginTime = " + secondBeginTime + " ;secondEndTime = " + secondEndTime);
                byte[] secondTimeBuf = new byte[4];
                StringUtil.int2Bytes(secondBeginTime, secondTimeBuf);
                System.arraycopy(secondTimeBuf, 0, unlockTime, 8, 4);
                StringUtil.int2Bytes(secondEndTime, secondTimeBuf);
                System.arraycopy(secondTimeBuf, 0, unlockTime, 12, 4);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            Arrays.fill(unlockTime, 8, 16, (byte) 0);
        }

        if (thirdBegin != null && thirdEnd != null) {
            String thirdBeginStr = mCalendar.get(Calendar.YEAR) + "-" + mCalendar.get(Calendar.MONTH) + "-" + mCalendar.get(Calendar.HOUR_OF_DAY) + " " + thirdBegin + ":00";
            String thirdEndDateStr = mCalendar.get(Calendar.YEAR) + "-" + mCalendar.get(Calendar.MONTH) + "-" + mCalendar.get(Calendar.HOUR_OF_DAY) + " " + thirdEnd + ":00";
            LogUtil.d(TAG, "thirdBeginStr = " + thirdBeginStr + " ;thirdEndDateStr = " + thirdEndDateStr);
            try {
                Date thirdBeginDate = currentTime.parse(thirdBeginStr);
                Date thirdEndDate = currentTime.parse(thirdEndDateStr);

                int thirdBeginTime = Long.valueOf((thirdBeginDate.getTime() / 1000)).intValue();
                int thirdEndTime = Long.valueOf((thirdEndDate.getTime() / 1000)).intValue();
                LogUtil.d(TAG, "thirdBeginTime = " + thirdBeginTime + " ;thirdEndTime = " + thirdEndTime);
                byte[] thirdTimeBuf = new byte[4];
                StringUtil.int2Bytes(thirdBeginTime, thirdTimeBuf);
                System.arraycopy(thirdTimeBuf, 0, unlockTime, 16, 4);
                StringUtil.int2Bytes(thirdEndTime, thirdTimeBuf);
                System.arraycopy(thirdTimeBuf, 0, unlockTime, 20, 4);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            Arrays.fill(unlockTime, 16, 24, (byte) 0);
        }
        LogUtil.d(TAG, "unlockTime : " + Arrays.toString(unlockTime));
        return unlockTime;
    }
}
