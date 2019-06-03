package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.entity.Device;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class UnlockTimeActivity extends AppCompatActivity implements View.OnClickListener, UiListener {
    private final static String TAG = UnlockTimeActivity.class.getSimpleName();

    private Toolbar mUsetSetTb;
    private TextView mTitleTv;
    private TextView mFirstStartTime;
    private TextView mFirstEndTime;
    private TextView mSecondStartTime;
    private TextView mSecondEndTime;
    private TextView mThirdStartTime;
    private TextView mThirtEndTime;
    private TextView mAddUnlockTimeTv;
    private LinearLayout mFirstUnlockTimeLl;
    private LinearLayout mSecondUnlockTimeLl;
    private LinearLayout mThirtUnlockTimeLl;
    private TextView mDelFirstTv;
    private TextView mDelSecondTv;
    private TextView mDelThirdTv;

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

    private Device mDevice;

    /**
     * 蓝牙
     */
    private BleManagerHelper mBleManagerHelper;

    private DeviceInfo mDefaultDevice; //默认设备

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp_unlock_time);
        initView();
        initData();
        initActionBar();
        initEvent();
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
        mAddUnlockTimeTv = findViewById(R.id.tv_add_unlock_time);
        mFirstUnlockTimeLl = findViewById(R.id.ll_unlock_time1);
        mSecondUnlockTimeLl = findViewById(R.id.ll_unlock_time2);
        mThirtUnlockTimeLl = findViewById(R.id.ll_unlock_time3);
        mDelFirstTv = findViewById(R.id.tv_del_time1);
        mDelSecondTv = findViewById(R.id.tv_del_time2);
        mDelThirdTv = findViewById(R.id.tv_del_time3);
    }

    private void initData() {
        mDefaultDevice = DeviceInfoDao.getInstance(this).queryFirstData("device_default", true);
        mHandler = new Handler();
        mBleManagerHelper = BleManagerHelper.getInstance(this);
        mBleManagerHelper.addUiListener(this);
        mDevice = Device.getInstance(this);
        mCalendar = Calendar.getInstance();

        mTempUser = (DeviceUser) getIntent().getExtras().getSerializable(BleMsg.KEY_TEMP_USER);
        if (mTempUser.getStTsBegin() == null) {
            mFirstUnlockTimeLl.setVisibility(View.GONE);
        }
        if (mTempUser.getNdTsBegin() == null) {
            mSecondUnlockTimeLl.setVisibility(View.GONE);
        }
        if (mTempUser.getThTsBegin() == null) {
            mThirtUnlockTimeLl.setVisibility(View.GONE);
        }
        LogUtil.d(TAG, "mTempUser = " + mTempUser.toString());
        mFirstStartTime.setText(mTempUser.getStTsBegin() == null ? "08:00" : mTempUser.getStTsBegin());
        mFirstEndTime.setText(mTempUser.getStTsEnd() == null ? "09:00" : mTempUser.getStTsEnd());

        mSecondStartTime.setText(mTempUser.getNdTsBegin() == null ? "11:00" : mTempUser.getNdTsBegin());
        mSecondEndTime.setText(mTempUser.getNdTsend() == null ? "12:00" : mTempUser.getNdTsend());

        mThirdStartTime.setText(mTempUser.getThTsBegin() == null ? "17:00" : mTempUser.getThTsBegin());
        mThirtEndTime.setText(mTempUser.getThTsEnd() == null ? "18:00" : mTempUser.getThTsEnd());
        mLoadDialog = DialogUtils.createLoadingDialog(this, getResources().getString(R.string.data_loading));
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
        mAddUnlockTimeTv.setOnClickListener(this);
        mDelFirstTv.setOnClickListener(this);
        mDelSecondTv.setOnClickListener(this);
        mDelThirdTv.setOnClickListener(this);
    }

    private void initActionBar() {
        mTitleTv.setText(R.string.temp_user_setting);

        mUsetSetTb.setNavigationIcon(R.mipmap.btn_back);
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
                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    mLoadDialog.show();
                    mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_THREE_UNLOCK_TIME, mTempUser.getUserId(), getUnlockTime(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
                            mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString(),
                            mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()), BleMsg.INT_DEFAULT_TIMEOUT);
                } else {
                    showMessage(getString(R.string.disconnect_ble));
                }

            } else {
                showMessage("时间段重复，请检查！");
            }
        } else if (mFirstUnlockTimeLl.getVisibility() == View.VISIBLE && mSecondUnlockTimeLl.getVisibility() == View.VISIBLE && mThirtUnlockTimeLl.getVisibility() == View.GONE) {
            if (DateTimeUtil.compareDate(DateTimeUtil.checkDate(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString()),
                    DateTimeUtil.checkDate(mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString()))) {

                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    mLoadDialog.show();
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


        } else if (mFirstUnlockTimeLl.getVisibility() == View.VISIBLE && mSecondUnlockTimeLl.getVisibility() == View.GONE && mThirtUnlockTimeLl.getVisibility() == View.GONE) {
            if (mDevice.getState() == Device.BLE_CONNECTED) {
                mLoadDialog.show();
                mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_ONE_UNLOCK_TIME, mTempUser.getUserId(),
                        getUnlockTime(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString(),
                                null, null,
                                null, null), BleMsg.INT_DEFAULT_TIMEOUT);
            } else {
                showMessage(getString(R.string.disconnect_ble));
            }

        } else if (mSecondUnlockTimeLl.getVisibility() == View.VISIBLE && mFirstUnlockTimeLl.getVisibility() == View.GONE && mThirtUnlockTimeLl.getVisibility() == View.GONE) {
            if (mDevice.getState() == Device.BLE_CONNECTED) {
                mLoadDialog.show();
                mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_ONE_UNLOCK_TIME, mTempUser.getUserId(),
                        getUnlockTime(null, null,
                                mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString(),
                                null, null), BleMsg.INT_DEFAULT_TIMEOUT);
            } else {
                showMessage(getString(R.string.disconnect_ble));
            }

        } else if (mThirtUnlockTimeLl.getVisibility() == View.VISIBLE && mSecondUnlockTimeLl.getVisibility() == View.GONE && mFirstUnlockTimeLl.getVisibility() == View.GONE) {
            if (mDevice.getState() == Device.BLE_CONNECTED) {
                mLoadDialog.show();
                mBleManagerHelper.getBleCardService().sendCmd1B(BleMsg.TYPE_SET_USER_ONE_UNLOCK_TIME, mTempUser.getUserId(),
                        getUnlockTime(null, null,
                                null, null, mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()), BleMsg.INT_DEFAULT_TIMEOUT);
            } else {
                showMessage(getString(R.string.disconnect_ble));
            }
        } else if (mSecondUnlockTimeLl.getVisibility() == View.VISIBLE && mThirtUnlockTimeLl.getVisibility() == View.VISIBLE && mFirstUnlockTimeLl.getVisibility() == View.GONE) {

            if (DateTimeUtil.compareDate(DateTimeUtil.checkDate(mSecondStartTime.getText().toString(), mSecondEndTime.getText().toString()),
                    DateTimeUtil.checkDate(mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()))) {

                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    mLoadDialog.show();
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

        } else if (mFirstUnlockTimeLl.getVisibility() == View.VISIBLE && mThirtUnlockTimeLl.getVisibility() == View.VISIBLE && mSecondUnlockTimeLl.getVisibility() == View.GONE) {
            if (DateTimeUtil.compareDate(DateTimeUtil.checkDate(mFirstStartTime.getText().toString(), mFirstEndTime.getText().toString()),
                    DateTimeUtil.checkDate(mThirdStartTime.getText().toString(), mThirtEndTime.getText().toString()))) {

                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    mLoadDialog.show();
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
        } else
            showMessage("未设置时间段!");

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
            case R.id.tv_del_time1:
                if (mSecondUnlockTimeLl.getVisibility() == View.GONE && mThirtUnlockTimeLl.getVisibility() == View.GONE) {
                    showMessage("至少设置一个时间段");
                } else
                    mFirstUnlockTimeLl.setVisibility(View.GONE);
                break;
            case R.id.tv_del_time2:
                if (mFirstUnlockTimeLl.getVisibility() == View.GONE && mThirtUnlockTimeLl.getVisibility() == View.GONE) {
                    showMessage("至少设置一个时间段");
                } else
                    mSecondUnlockTimeLl.setVisibility(View.GONE);
                break;
            case R.id.tv_del_time3:
                if (mFirstUnlockTimeLl.getVisibility() == View.GONE && mSecondUnlockTimeLl.getVisibility() == View.GONE) {
                    showMessage("至少设置一个时间段");
                } else
                    mThirtUnlockTimeLl.setVisibility(View.GONE);
                break;

            case R.id.tv_add_unlock_time:
                mTempUser = DeviceUserDao.getInstance(this).queryUser(mTempUser.getDevNodeId(), mTempUser.getUserId());//更新状态
                if (mFirstUnlockTimeLl.getVisibility() == View.GONE) {
                    mFirstStartTime.setText(mTempUser.getStTsBegin() == null ? "08:00" : mTempUser.getStTsBegin());
                    mFirstEndTime.setText(mTempUser.getStTsEnd() == null ? "09:00" : mTempUser.getStTsEnd());
                    mFirstUnlockTimeLl.setVisibility(View.VISIBLE);
                    return;
                }
                if (mSecondUnlockTimeLl.getVisibility() == View.GONE) {
                    mSecondStartTime.setText(mTempUser.getNdTsBegin() == null ? "11:00" : mTempUser.getNdTsBegin());
                    mSecondEndTime.setText(mTempUser.getNdTsend() == null ? "12:00" : mTempUser.getNdTsend());
                    mSecondUnlockTimeLl.setVisibility(View.VISIBLE);
                    return;
                }
                if (mThirtUnlockTimeLl.getVisibility() == View.GONE) {
                    mThirdStartTime.setText(mTempUser.getThTsBegin() == null ? "17:00" : mTempUser.getThTsBegin());
                    mThirtEndTime.setText(mTempUser.getThTsEnd() == null ? "18:00" : mTempUser.getThTsEnd());
                    mThirtUnlockTimeLl.setVisibility(View.VISIBLE);
                    return;
                }
                showMessage("最多可设置3个开锁时段!");
                break;
            default:
                break;
        }
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
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_1E:
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
            case BleMsg.TYPE_SET_TEMP_USER_LIFE_SUCCESS:
                if (mFirstUnlockTimeLl.getVisibility() == View.VISIBLE) {
                    mTempUser.setStTsBegin(mFirstStartTime.getText().toString());
                    mTempUser.setStTsEnd(mFirstEndTime.getText().toString());
                } else {
                    mTempUser.setStTsBegin(null);
                    mTempUser.setStTsEnd(null);
                }
                if (mSecondUnlockTimeLl.getVisibility() == View.VISIBLE) {
                    mTempUser.setNdTsBegin(mSecondStartTime.getText().toString());
                    mTempUser.setNdTsend(mSecondEndTime.getText().toString());
                } else {
                    mTempUser.setNdTsBegin(null);
                    mTempUser.setNdTsend(null);
                }
                if (mThirtUnlockTimeLl.getVisibility() == View.VISIBLE) {
                    mTempUser.setThTsBegin(mThirdStartTime.getText().toString());
                    mTempUser.setThTsEnd(mThirtEndTime.getText().toString());
                } else {
                    mTempUser.setThTsBegin(null);
                    mTempUser.setThTsEnd(null);
                }

                DeviceUserDao.getInstance(this).updateDeviceUser(mTempUser);
                showMessage(getString(R.string.set_unlock_time_success));
                break;
            case BleMsg.TYPE_NO_AUTHORITY_1E:
                showMessage(getString(R.string.no_authority));
                break;
            default:
                break;
        }
        DialogUtils.closeDialog(mLoadDialog);
    }


}
