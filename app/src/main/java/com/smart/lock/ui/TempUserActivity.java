package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
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
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class TempUserActivity extends BaseActivity implements View.OnClickListener, DatePickerDialog.OnDateSetListener, UiListener {
    private final static String TAG = TempUserActivity.class.getSimpleName();

    private Toolbar mUsetSetTb;
    private TextView mTitleTv;
    private EditText mEtMome;
    private TextView mStartDate;
    private TextView mEndDate;
    private Button mConfirmBtn;

    private Dialog mLoadDialog;
    private Handler mHandler;
    private Calendar mCalendar;
    private DeviceUser mTempUser;

    private static final int TEMP_USER_START_DATE = 1;
    private static final int TEMP_USER_END_DATE = 2;

    /**
     * 蓝牙
     */
    private BleManagerHelper mBleManagerHelper;
    private Device mDevice;  //当前连接设备信息

    private DeviceInfo mDefaultDevice; //默认设备
    private DeviceKeyDao mDeviceKeyDao;//设备信息管理者

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp_setting);
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
        mStartDate = findViewById(R.id.tv_start_date);
        mEndDate = findViewById(R.id.tv_end_date);
        mConfirmBtn = findViewById(R.id.btn_confirm);
    }

    private void initData() {
        mDefaultDevice = DeviceInfoDao.getInstance(this).queryFirstData("device_default", true);
        mDeviceKeyDao = DeviceKeyDao.getInstance(this);
        mHandler = new Handler();
        mBleManagerHelper = BleManagerHelper.getInstance(this);
        mBleManagerHelper.addUiListener(this);
        mDevice = Device.getInstance(this);
        mCalendar = Calendar.getInstance();
        mTempUser = (DeviceUser) getIntent().getExtras().getSerializable(BleMsg.KEY_TEMP_USER);
        mLoadDialog = DialogUtils.createLoadingDialog(this, getResources().getString(R.string.data_loading));
        String day = mCalendar.get(Calendar.YEAR) + "-" + (mCalendar.get(Calendar.MONTH) + 1) + "-" + mCalendar.get(Calendar.DAY_OF_MONTH);
        mStartDate.setText(mTempUser.getLcBegin() == null ? day : mTempUser.getLcBegin());
        mEndDate.setText(mTempUser.getLcEnd() == null ? day : mTempUser.getLcEnd());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.temp_manager_setting, menu);
        return true;
    }

    private void initEvent() {
        mStartDate.setOnClickListener(this);
        mEndDate.setOnClickListener(this);
        mConfirmBtn.setOnClickListener(this);
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

    /**
     * 日期选择器
     *
     * @param tag 标签
     */
    private void showDatePickerDialog(int tag) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, AlertDialog.THEME_HOLO_LIGHT, this,
                mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setTag(tag);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        if (tag == TEMP_USER_START_DATE) {
            datePickerDialog.setTitle("设置起始日期");
        } else
            datePickerDialog.setTitle("设置结束日期");
        datePickerDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(BleMsg.KEY_TEMP_USER, mTempUser);
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.item_set_unlock_time:
                startIntent(UnlockTimeActivity.class, bundle, -1);
                break;
            case R.id.item_set_unlock_key:
                bundle.putInt(BleMsg.KEY_CURRENT_ITEM, 0);
                startIntent(DeviceKeyActivity.class, bundle, -1);
                break;
            default:
                break;
        }
        return true;
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
            case R.id.tv_start_date:
                showDatePickerDialog(TEMP_USER_START_DATE);
                break;
            case R.id.tv_end_date:
                showDatePickerDialog(TEMP_USER_END_DATE);
                break;
            case R.id.btn_confirm:
                if (StringUtil.checkIsNull(mEtMome.getText().toString())) {
                    showMessage("备注名不能为空！");
                    return;
                }
                if (mStartDate.getText().toString().equals(getString(R.string._1970_01_01)) || mEndDate.getText().toString().equals(getString(R.string._1970_01_01))) {
                    showMessage(getString(R.string.plz_set_right_life_cycle));
                    return;
                }

                if (DateTimeUtil.isDateOneBigger(mStartDate.getText().toString(), mEndDate.getText().toString())) {
                    if (mDevice.getState() == Device.BLE_CONNECTED) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog.show();
                        mBleManagerHelper.getBleCardService().sendCmd29(mTempUser.getUserId(), getLifyCycle(mStartDate.getText().toString(), mEndDate.getText().toString()));
                    } else {
                        showMessage(getString(R.string.disconnect_ble));
                    }

                } else {
                    showMessage("起始日期不能大于或等于结束日期！");
                }

                break;
            default:
                break;
        }
    }

    private byte[] getLifyCycle(String begin, String end) {
        byte[] lifeCycle = new byte[8];
        SimpleDateFormat currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String BeginStr = begin + " " + "00" + getString(R.string.colon) + "00" + ":00";
        String endStr = end + " " + "00" + getString(R.string.colon) + "00" + ":00";

        try {
            Date beginDate = currentTime.parse(BeginStr);
            Date EndDate = currentTime.parse(endStr);

            int beginTime = Long.valueOf((beginDate.getTime() / 1000)).intValue();
            int endTime = Long.valueOf((EndDate.getTime() / 1000)).intValue();

            LogUtil.d(TAG, "beginTime = " + beginTime + " ;endTime = " + endTime);
            byte[] timeBuf = new byte[4];
            StringUtil.int2Bytes(beginTime, timeBuf);
            System.arraycopy(timeBuf, 0, lifeCycle, 0, 4);
            StringUtil.int2Bytes(endTime, timeBuf);
            System.arraycopy(timeBuf, 0, lifeCycle, 4, 4);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return lifeCycle;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Log.d(TAG, "view.getTag() = " + view.getTag());
        switch ((int) view.getTag()) {
            case TEMP_USER_START_DATE:
                mStartDate.setText(year + "-" + (month + 1) + "-" + dayOfMonth);
                break;
            case TEMP_USER_END_DATE:
                mEndDate.setText(year + "-" + (month + 1) + "-" + dayOfMonth);
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
                LogUtil.e(TAG, "state : " + state + "is can not handle");
                break;
        }
    }

    @Override
    public void dispatchUiCallback(Message msg, Device device, int type) {
        LogUtil.i(TAG, "dispatchUiCallback!");
        mDevice = device;
        Bundle extra = msg.getData();
        Serializable serializable = extra.getSerializable(BleMsg.KEY_SERIALIZABLE);
        if (serializable != null && !(serializable instanceof DeviceUser || serializable instanceof Short)) {
            return;
        }
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_2E:
                final byte[] errCode = msg.getData().getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode != null)
                    dispatchErrorCode(errCode[3]);
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
                break;
            case BleMsg.TYPE_TEMP_USER_LIFE_UPDATE_SUCCESS:
                mTempUser.setLcBegin(mStartDate.getText().toString());
                mTempUser.setLcEnd(mEndDate.getText().toString());
                mTempUser.setUserName(mEtMome.getText().toString().trim());
                DeviceUserDao.getInstance(this).updateDeviceUser(mTempUser);
                showMessage(getString(R.string.set_life_cycle_success));
                break;
            default:
                break;
        }
        DialogUtils.closeDialog(mLoadDialog);
    }
}
