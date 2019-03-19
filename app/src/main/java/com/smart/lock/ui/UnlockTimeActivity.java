package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class UnlockTimeActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = UnlockTimeActivity.class.getSimpleName();

    private Toolbar mUsetSetTb;
    private TextView mTitleTv;
    private TextView mFirstStartTime;
    private TextView mFirstEndTime;
    private TextView mSecondStartTime;
    private TextView mSecondEndTime;
    private TextView mThirdStartTime;
    private TextView mThirtEndTime;
    private Button mAddUnlockTimeBtn;
    private LinearLayout mFirstUnlockTimeLl;
    private LinearLayout mSecondUnlockTimeLl;
    private LinearLayout mThirtUnlockTimeLl;
    private Button mDelFirstBtn;
    private Button mDelSecondBtn;
    private Button mDelThirdBtn;

    private boolean mDeleteMode = false;

    private Dialog mLoadDialog;
    private Handler mHandler;
    private Calendar mCalendar;
    private DeviceUser mTempUser;

    private static final int TEMP_KEY_FIRST_START_TIME = 1;
    private static final int TEMP_KEY_FIRST_END_TIME = 2;
    private static final int TEMP_KEY_SECOND_START_TIME = 3;
    private static final int TEMP_KEY_SECOND_END_TIME = 4;
    private static final int TEMP_KEY_THIRD_START_TIME = 5;
    private static final int TEMP_KEY_THIRD_END_TIME = 6;

    /**
     * 蓝牙
     */
    private BleManagerHelper mBleManagerHelper;

    /**
     * 超时提示框启动器
     */
    private Runnable mRunnable = new Runnable() {
        public void run() {
            if (mLoadDialog != null && mLoadDialog.isShowing()) {

                DialogUtils.closeDialog(mLoadDialog);

                Toast.makeText(UnlockTimeActivity.this, getString(R.string.plz_reconnect), Toast.LENGTH_LONG).show();
            }
        }
    };

    private DeviceInfo mDefaultDevice; //默认设备

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp_unlock_time);
        initView();
        initData();
        initActionBar();
        initEvent();
        LocalBroadcastManager.getInstance(this).registerReceiver(userReciver, intentFilter());
    }

    @SuppressLint("WrongViewCast")
    private void initView() {
        mUsetSetTb = findViewById(R.id.tb_unlock_time);
        mTitleTv = findViewById(R.id.tv_title);
        mFirstStartTime = findViewById(R.id.tv_start_time);
        mFirstEndTime = findViewById(R.id.tv_end_time);
        mSecondStartTime = findViewById(R.id.tv_start_time2);
        mSecondEndTime = findViewById(R.id.tv_end_time2);
        mThirdStartTime = findViewById(R.id.tv_start_time3);
        mThirtEndTime = findViewById(R.id.tv_end_time3);
        mAddUnlockTimeBtn = findViewById(R.id.btn_add_unlock_time);
        mFirstUnlockTimeLl = findViewById(R.id.ll_unlock_time1);
        mSecondUnlockTimeLl = findViewById(R.id.ll_unlock_time2);
        mThirtUnlockTimeLl = findViewById(R.id.ll_unlock_time3);
        mDelFirstBtn = findViewById(R.id.btn_del_time1);
        mDelSecondBtn = findViewById(R.id.btn_del_time2);
        mDelThirdBtn = findViewById(R.id.btn_del_time3);
    }

    private void initData() {
        mDefaultDevice = DeviceInfoDao.getInstance(this).queryFirstData("device_default", true);
        mHandler = new Handler();
        mBleManagerHelper = BleManagerHelper.getInstance(this, mDefaultDevice.getDeviceNodeId(), false);
        mCalendar = Calendar.getInstance();
        mSecondUnlockTimeLl.setVisibility(View.GONE);
        mThirtUnlockTimeLl.setVisibility(View.GONE);
        mTempUser = (DeviceUser) getIntent().getExtras().getSerializable(BleMsg.KEY_TEMP_USER);

        mFirstStartTime.setText(mTempUser.getStTsBegin() == null ? "00:00" : mTempUser.getStTsBegin());
        mFirstEndTime.setText(mTempUser.getStTsEnd() == null ? "00:00" : mTempUser.getStTsEnd());

        mSecondStartTime.setText(mTempUser.getNdTsBegin() == null ? "00:00" : mTempUser.getNdTsBegin());
        mSecondEndTime.setText(mTempUser.getNdTsend() == null ? "00:00" : mTempUser.getNdTsend());

        mThirdStartTime.setText(mTempUser.getThTsBegin() == null ? "00:00" : mTempUser.getThTsBegin());
        mThirtEndTime.setText(mTempUser.getThTsEnd() == null ? "00:00" : mTempUser.getThTsEnd());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.confirm, menu);
        return true;
    }

    private void initEvent() {
        mFirstStartTime.setOnClickListener(this);
        mFirstEndTime.setOnClickListener(this);
        mSecondStartTime.setOnClickListener(this);
        mSecondEndTime.setOnClickListener(this);
        mThirdStartTime.setOnClickListener(this);
        mThirtEndTime.setOnClickListener(this);
        mAddUnlockTimeBtn.setOnClickListener(this);
        mDelFirstBtn.setOnClickListener(this);
        mDelSecondBtn.setOnClickListener(this);
        mDelThirdBtn.setOnClickListener(this);
    }

    private void initActionBar() {
        mTitleTv.setText(R.string.temp_user_setting);

        mUsetSetTb.setNavigationIcon(R.mipmap.icon_arrow_blue_left_45_45);
        mUsetSetTb.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        setSupportActionBar(mUsetSetTb);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void showTimePickerDialog(final int tag) {
        mCalendar = Calendar.getInstance();

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, AlertDialog.THEME_HOLO_LIGHT,
                new TimePickerDialog.OnTimeSetListener() {
                    boolean flag = false;

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                        if (!flag) {
                            flag = true;
                            return;
                        }
                        String hour = "0";
                        if (hourOfDay < 10) {
                            hour = "0" + hourOfDay;
                        } else
                            hour = String.valueOf(hourOfDay);
                        switch (tag) {
                            case TEMP_KEY_FIRST_START_TIME:
                                mFirstStartTime.setText(hour + ":" + minute);
                                break;
                            case TEMP_KEY_FIRST_END_TIME:
                                if (!timeCompare(mFirstStartTime.getText().toString(), hour + ":" + minute)) {
                                    showMessage("开始时间大于结束时间");
                                    showTimePickerDialog(TEMP_KEY_FIRST_END_TIME);
                                } else
                                    mFirstEndTime.setText(hour + ":" + minute);
                                break;
                            case TEMP_KEY_SECOND_START_TIME:
                                mSecondStartTime.setText(hour + ":" + minute);
                                break;
                            case TEMP_KEY_SECOND_END_TIME:
                                if (!timeCompare(mSecondStartTime.getText().toString(), hour + ":" + minute)) {
                                    showMessage("开始时间大于结束时间");
                                    showTimePickerDialog(TEMP_KEY_SECOND_END_TIME);
                                } else
                                    mSecondEndTime.setText(hour + ":" + minute);
                                break;
                            case TEMP_KEY_THIRD_START_TIME:
                                mThirdStartTime.setText(hour + ":" + minute);
                                break;
                            case TEMP_KEY_THIRD_END_TIME:
                                if (!timeCompare(mSecondStartTime.getText().toString(), hour + ":" + minute)) {
                                    showMessage("开始时间大于结束时间");
                                    showTimePickerDialog(TEMP_KEY_THIRD_END_TIME);
                                } else {
                                    mThirtEndTime.setText(hour + ":" + minute);
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

        if (DateTimeUtil.compareDate(DateTimeUtil.checkDate(firstStartTime, firstEndTime), DateTimeUtil.checkDate(secondStartTime, secondEndTime))) {
            return false;
        } else if (DateTimeUtil.compareDate(DateTimeUtil.checkDate(thirtStartTime, thirtEndTime), DateTimeUtil.checkDate(secondStartTime, secondEndTime))) {
            return false;
        } else if (DateTimeUtil.compareDate(DateTimeUtil.checkDate(firstStartTime, firstEndTime), DateTimeUtil.checkDate(thirtStartTime, thirtEndTime))) {
            return false;
        }
        return true;
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
        LogUtil.d(TAG, "date1 = " + date1);
        String date2 = mCalendar.get(Calendar.YEAR) + "-" + mCalendar.get(Calendar.MONTH) + "-" + mCalendar.get(Calendar.HOUR_OF_DAY) + " " + endTime + ":00";
        LogUtil.d(TAG, "date2 = " + date2);
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

    private static IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.STR_RSP_MSG1E_ERRCODE);
        return intentFilter;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.item_confirm:

                sendUnlocktime();
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
        if (mFirstUnlockTimeLl.getVisibility() == View.VISIBLE && mSecondUnlockTimeLl.getVisibility() == View.VISIBLE && mThirtUnlockTimeLl.getVisibility() == View.VISIBLE) {
            if (!compareDate(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
                    mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString(),
                    mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString())) {
                if (mBleManagerHelper.getServiceConnection()) {
                    mLoadDialog = DialogUtils.createLoadingDialog(this, getResources().getString(R.string.data_loading));
                    closeDialog(15);
                    mBleManagerHelper.getBleCardService().sendCmd1B((byte) 3, mTempUser.getUserId(), getUnlockTime(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
                            mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString(),
                            mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()));
                } else {
                    showMessage("连接中断，请重试！");
                }

            } else {
                showMessage("时间段重复，请检查！");
            }
        } else if (mFirstUnlockTimeLl.getVisibility() == View.VISIBLE && mSecondUnlockTimeLl.getVisibility() == View.VISIBLE && mThirtUnlockTimeLl.getVisibility() == View.GONE) {
            if (DateTimeUtil.compareDate(DateTimeUtil.checkDate(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString()),
                    DateTimeUtil.checkDate(mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString()))) {

                if (mBleManagerHelper.getServiceConnection()) {
                    mLoadDialog = DialogUtils.createLoadingDialog(this, getResources().getString(R.string.data_loading));
                    closeDialog(15);
                    mBleManagerHelper.getBleCardService().sendCmd1B((byte) 2, mTempUser.getUserId(),
                            getUnlockTime(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
                                    mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString(),
                                    null, null));
                } else {
                    showMessage("连接中断，请重试！");
                }

            } else {
                showMessage("时间段重复，请检查！");
            }


        } else if (mFirstUnlockTimeLl.getVisibility() == View.VISIBLE && mSecondUnlockTimeLl.getVisibility() == View.GONE && mThirtUnlockTimeLl.getVisibility() == View.GONE) {
            if (mBleManagerHelper.getServiceConnection()) {
                mLoadDialog = DialogUtils.createLoadingDialog(this, getResources().getString(R.string.data_loading));
                closeDialog(15);
                mBleManagerHelper.getBleCardService().sendCmd1B((byte) 1, mTempUser.getUserId(),
                        getUnlockTime(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
                                null, null,
                                null, null));
            } else {
                showMessage("连接中断，请重试！");
            }

        } else if (mSecondUnlockTimeLl.getVisibility() == View.VISIBLE && mFirstUnlockTimeLl.getVisibility() == View.GONE && mThirtUnlockTimeLl.getVisibility() == View.GONE) {
            if (mBleManagerHelper.getServiceConnection()) {
                mLoadDialog = DialogUtils.createLoadingDialog(this, getResources().getString(R.string.data_loading));
                closeDialog(15);
                mBleManagerHelper.getBleCardService().sendCmd1B((byte) 1, mTempUser.getUserId(),
                        getUnlockTime(null, null,
                                mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString(),
                                null, null));
            } else {
                showMessage("连接中断，请重试！");
            }

        } else if (mThirtUnlockTimeLl.getVisibility() == View.VISIBLE && mSecondUnlockTimeLl.getVisibility() == View.GONE && mFirstUnlockTimeLl.getVisibility() == View.GONE) {
            if (mBleManagerHelper.getServiceConnection()) {
                mLoadDialog = DialogUtils.createLoadingDialog(this, getResources().getString(R.string.data_loading));
                closeDialog(15);
                mBleManagerHelper.getBleCardService().sendCmd1B((byte) 1, mTempUser.getUserId(),
                        getUnlockTime(null, null,
                                null, null, mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()));
            } else {
                showMessage("连接中断，请重试！");
            }
        } else if (mSecondUnlockTimeLl.getVisibility() == View.VISIBLE && mThirtUnlockTimeLl.getVisibility() == View.VISIBLE && mFirstUnlockTimeLl.getVisibility() == View.GONE) {

            if (DateTimeUtil.compareDate(DateTimeUtil.checkDate(mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString()),
                    DateTimeUtil.checkDate(mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()))) {

                if (mBleManagerHelper.getServiceConnection()) {
                    mLoadDialog = DialogUtils.createLoadingDialog(this, getResources().getString(R.string.data_loading));
                    closeDialog(15);
                    mBleManagerHelper.getBleCardService().sendCmd1B((byte) 2, mTempUser.getUserId(),
                            getUnlockTime(null, null,
                                    mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
                                    mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()));
                } else {
                    showMessage("连接中断，请重试！");
                }

            } else {
                showMessage("时间段重复，请检查！");
            }

        } else if (mFirstUnlockTimeLl.getVisibility() == View.VISIBLE && mThirtUnlockTimeLl.getVisibility() == View.VISIBLE && mSecondUnlockTimeLl.getVisibility() == View.GONE) {
            if (DateTimeUtil.compareDate(DateTimeUtil.checkDate(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString()),
                    DateTimeUtil.checkDate(mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()))) {

                if (mBleManagerHelper.getServiceConnection()) {
                    mLoadDialog = DialogUtils.createLoadingDialog(this, getResources().getString(R.string.data_loading));
                    closeDialog(15);
                    mBleManagerHelper.getBleCardService().sendCmd1B((byte) 2, mTempUser.getUserId(),
                            getUnlockTime(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
                                    null, null,
                                    mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()));
                } else {
                    showMessage("连接中断，请重试！");
                }

            } else {
                showMessage("时间段重复，请检查！");
            }
        }

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

                int firstBeginTime = (int) (firstBeginDate.getTime() / 1000);
                int firstEndTime = (int) (firstEndDate.getTime() / 1000);
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
            try {
                Date secondBeginDate = currentTime.parse(secondBeginStr);
                Date secondEndDate = currentTime.parse(secondEndDateStr);

                int secondBeginTime = (int) secondBeginDate.getTime() / 1000;
                int secondEndTime = (int) secondEndDate.getTime() / 1000;
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
            try {
                Date thirdBeginDate = currentTime.parse(thirdBeginStr);
                Date thirdEndDate = currentTime.parse(thirdEndDateStr);

                int thirdBeginTime = (int) thirdBeginDate.getTime() / 1000;
                int thirdEndTime = (int) thirdEndDate.getTime() / 1000;
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

        return unlockTime;
    }

    /**
     * 广播接收
     */
    private final BroadcastReceiver userReciver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BleMsg.STR_RSP_MSG1E_ERRCODE)) {
                final byte[] errCode = intent.getByteArrayExtra(BleMsg.KEY_ERROR_CODE);
                Log.d(TAG, "errCode[3] = " + errCode[3]);
                if (errCode[3] == 0x1f) {
                    mTempUser.setStTsBegin(mFirstStartTime.getText().toString());
                    mTempUser.setStTsEnd(mFirstEndTime.getText().toString());
                    mTempUser.setNdTsBegin(mSecondStartTime.getText().toString());
                    mTempUser.setNdTsend(mSecondEndTime.getText().toString());
                    mTempUser.setThTsBegin(mThirdStartTime.getText().toString());
                    mTempUser.setThTsEnd(mThirtEndTime.getText().toString());

                    DeviceUserDao.getInstance(UnlockTimeActivity.this).updateDeviceUser(mTempUser);
                    showMessage(getString(R.string.set_unlock_time_success));
                } else if (errCode[3] == 0x21) {
                    showMessage(getString(R.string.no_authority));
                }
                mHandler.removeCallbacks(mRunnable);
                DialogUtils.closeDialog(mLoadDialog);
            }
        }
    };

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
        switch (v.getId()) {
            case R.id.tv_start_time:
                showTimePickerDialog(TEMP_KEY_FIRST_START_TIME);
                break;
            case R.id.tv_end_time:
                showTimePickerDialog(TEMP_KEY_FIRST_END_TIME);
                break;
            case R.id.tv_start_time2:
                showTimePickerDialog(TEMP_KEY_SECOND_START_TIME);
                break;
            case R.id.tv_end_time2:
                showTimePickerDialog(TEMP_KEY_SECOND_END_TIME);
                break;
            case R.id.tv_start_time3:
                showTimePickerDialog(TEMP_KEY_THIRD_START_TIME);
                break;
            case R.id.tv_end_time3:
                showTimePickerDialog(TEMP_KEY_THIRD_END_TIME);
                break;
            case R.id.btn_del_time1:
                mFirstUnlockTimeLl.setVisibility(View.GONE);
                break;
            case R.id.btn_del_time2:
                mSecondUnlockTimeLl.setVisibility(View.GONE);
                break;
            case R.id.btn_del_time3:
                mThirtUnlockTimeLl.setVisibility(View.GONE);
                break;

            case R.id.btn_add_unlock_time:
                if (mFirstUnlockTimeLl.getVisibility() == View.GONE) {
                    mFirstUnlockTimeLl.setVisibility(View.VISIBLE);
                    return;
                }
                if (mSecondUnlockTimeLl.getVisibility() == View.GONE) {
                    mSecondUnlockTimeLl.setVisibility(View.VISIBLE);
                    return;
                }
                if (mThirtUnlockTimeLl.getVisibility() == View.GONE) {
                    mThirtUnlockTimeLl.setVisibility(View.VISIBLE);
                    return;
                }
                showMessage("最多可设置3个开锁时段!");
                break;
            default:
                break;
        }
    }


    /**
     * 超时提醒
     *
     * @param seconds
     */
    protected void closeDialog(final int seconds) {

        mHandler.removeCallbacks(mRunnable);

        mHandler.postDelayed(mRunnable, seconds * 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(userReciver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
    }


}
