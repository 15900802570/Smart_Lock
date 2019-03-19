package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.LocalActivityManager;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.daimajia.swipe.SwipeLayout;
import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.fragment.AdminFragment;
import com.smart.lock.fragment.BaseFragment;
import com.smart.lock.fragment.MumberFragment;
import com.smart.lock.fragment.TempFragment;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.widget.NoScrollViewPager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class TempUserActivity extends BaseActivity implements View.OnClickListener, DatePickerDialog.OnDateSetListener {
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

    /**
     * 超时提示框启动器
     */
    private Runnable mRunnable = new Runnable() {
        public void run() {
            if (mLoadDialog != null && mLoadDialog.isShowing()) {

                DialogUtils.closeDialog(mLoadDialog);

                Toast.makeText(TempUserActivity.this, getString(R.string.plz_reconnect), Toast.LENGTH_LONG).show();
            }
        }
    };

    private DeviceInfo mDefaultDevice; //默认设备

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp_setting);
        initView();
        initData();
        initActionBar();
        initEvent();
        LocalBroadcastManager.getInstance(this).registerReceiver(tempUserReciver, intentFilter());
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
        mHandler = new Handler();
        mBleManagerHelper = BleManagerHelper.getInstance(this, mDefaultDevice.getDeviceNodeId(), false);
        mCalendar = Calendar.getInstance();
        mTempUser = (DeviceUser) getIntent().getExtras().getSerializable(BleMsg.KEY_TEMP_USER);

        mStartDate.setText(mTempUser.getLcBegin() == null ? ("1970-01-01") : mTempUser.getLcBegin());
        mEndDate.setText(mTempUser.getLcEnd() == null ? ("1970-01-01") : mTempUser.getLcEnd());
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

    /**
     * 日期选择器
     *
     * @param tag 标签
     */
    private void showDatePickerDialog(int tag) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, AlertDialog.THEME_HOLO_LIGHT, this,
                mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setTag(tag);
        if (tag == TEMP_USER_START_DATE) {
            datePickerDialog.setTitle("设置起始日期");
        } else
            datePickerDialog.setTitle("设置结束日期");
        datePickerDialog.show();
    }

    private static IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.STR_RSP_MSG2E_ERRCODE);
        return intentFilter;
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
                startIntent(TempKeyActivity.class, bundle, -1);
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 广播接收
     */
    private final BroadcastReceiver tempUserReciver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BleMsg.STR_RSP_MSG2E_ERRCODE)) {
                final byte[] errCode = intent.getByteArrayExtra(BleMsg.KEY_ERROR_CODE);
                Log.d(TAG, "errCode[3] = " + errCode[3]);
                if (errCode[3] == 0x02) {
                    showMessage(getString(R.string.no_authority));
                } else if (errCode[3] == 0x03) {
                    mTempUser.setLcBegin(mStartDate.getText().toString());
                    mTempUser.setLcEnd(mEndDate.getText().toString());
                    mTempUser.setUserStatus(ConstantUtil.USER_ENABLE);
                    DeviceUserDao.getInstance(TempUserActivity.this).updateDeviceUser(mTempUser);
                    showMessage(getString(R.string.set_life_cycle_success));
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

                if (!DateTimeUtil.isDateOneBigger(mStartDate.getText().toString(), mEndDate.getText().toString())) {
                    if (mBleManagerHelper.getServiceConnection()) {
                        mLoadDialog = DialogUtils.createLoadingDialog(this, getResources().getString(R.string.data_loading));
                        closeDialog(15);
                        mBleManagerHelper.getBleCardService().sendCmd29(mTempUser.getUserId(), getLifyCycle(mStartDate.getText().toString(), mEndDate.getText().toString()));
                    } else {
                        showMessage("连接中断，请重试！");
                    }

                } else {
                    showMessage("起始日期不能大于结束日期！");
                }

                break;
            default:
                break;
        }
    }

    private byte[] getLifyCycle(String begin, String end) {
        byte[] lifeCycle = new byte[8];
        SimpleDateFormat currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String BeginStr = begin + " " + "00" + ":" + "00" + ":00";
        String endStr = end + " " + "00" + ":" + "00" + ":00";

        try {
            Date beginDate = currentTime.parse(BeginStr);
            Date EndDate = currentTime.parse(endStr);

            int beginTime = (int) (beginDate.getTime() / 1000);
            int endTime = (int) (EndDate.getTime() / 1000);
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
            LocalBroadcastManager.getInstance(this).unregisterReceiver(tempUserReciver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
    }
}
