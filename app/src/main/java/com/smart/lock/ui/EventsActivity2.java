package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
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
import com.smart.lock.db.dao.DeviceLogDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.entity.Device;
import com.smart.lock.ui.fragment.BaseFragment;
import com.smart.lock.ui.fragment.EventsOfLockFragment;
import com.smart.lock.ui.fragment.EventsOfUserFragment;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.widget.DialogFactory;
import com.smart.lock.widget.NoScrollViewPager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;


public class EventsActivity2 extends AppCompatActivity implements View.OnClickListener, ViewPager.OnPageChangeListener, UiListener {
    private final static String TAG = EventsActivity2.class.getSimpleName();

    private TabLayout mEventsTl;
    private NoScrollViewPager mEventsVp;
    private Toolbar mEventsTb;
    private TextView mTitleTv;
    protected String mNodeId;

    private ArrayList<String> mTitleList;
    private ArrayList<BaseFragment> mEventsList;
    private EventsPagerAdapter mEventsPagerAdapter;
    private Dialog mLoadDialog;

    private EventsOfUserFragment mEventsOfUserFragment;
    private EventsOfLockFragment mEventsOfLockFragment;

    private BleManagerHelper mBleManagerHelper;
    private Device mDevice;

    private DeviceInfo mDefaultDevice; //默认设备
    private DeviceUser mDeviceUser; //当前设备用户

    protected RelativeLayout mSelectEventRl;

    private DialogFactory mTipsDialog; //删除提示框

    private final static int RECEIVER_LOG_TIME_OUT = 15;
    private int countTimeOut = 0;
    private int mDialogCount = 0;
    protected CheckBox mSelectCb;
    protected TextView mTipTv;
    protected TextView mBack;
    protected TextView mDelTv;


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            LogUtil.d(TAG, "receiver handle type=" + msg.what);
            switch (msg.what) {
                case BleMsg.DISPACTH_MSG_3E:
                    Bundle bundle = msg.getData();
                    dispatchErrorCode(bundle);
                    break;
                case BleMsg.RECEIVER_LOGS:
//                    mCountTv.setText(String.valueOf(count));
                    break;
                case RECEIVER_LOG_TIME_OUT:
//                    mHandler.removeMessages(RECEIVER_LOG_TIME_OUT);
                    if (mHandler.hasMessages(RECEIVER_LOG_TIME_OUT)) {
                        mHandler.removeMessages(RECEIVER_LOG_TIME_OUT);
                    }
                    DialogUtils.closeDialog(mLoadDialog);
                    LogUtil.d(TAG, "receiver log time out!");
                    DeviceLogDao.getInstance(EventsActivity2.this).deleteAll();
                    if (mDevice.getState() == Device.BLE_CONNECTED && ++countTimeOut < 2) {
                        mLoadDialog.show();
                        android.os.Message logMsg = android.os.Message.obtain();
                        logMsg.what = RECEIVER_LOG_TIME_OUT;
                        mHandler.sendMessageDelayed(logMsg, 10 * 1000);
                        if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MASTER) {
                            mBleManagerHelper.getBleCardService().sendCmd31(BleMsg.TYPE_QUERY_ALL_LOCK_LOG, mDefaultDevice.getUserId());
                        } else if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MEMBER) {
                            mBleManagerHelper.getBleCardService().sendCmd31(BleMsg.TYPE_QUERY_LOCK_LOG, mDefaultDevice.getUserId());
                        }
                    } else if (countTimeOut >= 2) {
                        countTimeOut = 0;
                        showMessage(getString(R.string.synchronization_timeout));
                    } else {
                        showMessage(getString(R.string.disconnect_ble));
                    }
                    break;
                default:
                    break;
            }

        }
    };


    protected void onCreate(@NonNull Bundle savedInstancesState) {
        super.onCreate(savedInstancesState);
        setContentView(R.layout.activity_events);
        initView();
        initData();
        initActionBar();
        initEvent();
    }

    private void initView() {
        mEventsTl = findViewById(R.id.tl_events_manager);
        mEventsVp = findViewById(R.id.vp_events_manager);
        mEventsTb = findViewById(R.id.tb_events_set);
        mTitleTv = findViewById(R.id.tv_events_title);
        mSelectCb = findViewById(R.id.delete_locked);
        mTipTv = findViewById(R.id.tv_tips);
        mDelTv = findViewById(R.id.del_tv);
        mBack = findViewById(R.id.back_tv);

        mSelectEventRl = findViewById(R.id.rl_select_delete);
        mSelectEventRl.setVisibility(View.GONE);

        mBack.setOnClickListener(this);
        mDelTv.setOnClickListener(this);
        mSelectCb.setOnClickListener(this);
    }

    private void initData() {

        mDefaultDevice = DeviceInfoDao.getInstance(this).queryFirstData("device_default", true);
        mDevice = Device.getInstance(this);
        mBleManagerHelper = BleManagerHelper.getInstance(this);
        mBleManagerHelper.addUiListener(this);

        mDefaultDevice = DeviceInfoDao.getInstance(this).queryFirstData(DeviceInfoDao.DEVICE_DEFAULT, true);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mDeviceUser = DeviceUserDao.getInstance(this).queryUser(mNodeId, mDefaultDevice.getUserId());
        mDevice = Device.getInstance(this);

        DeviceLogDao.getInstance(this).deleteAll(); // 删除数据库数据

        mLoadDialog = DialogUtils.createLoadingDialog(this, getString(R.string.data_loading));
        mLoadDialog.setCancelable(true);

        mTitleList = new ArrayList<>();
        mTitleList.add(getString(R.string.event_manager));
        mTitleList.add(getString(R.string.event_of_user));

        mEventsList = new ArrayList<>();
        mEventsOfUserFragment = new EventsOfUserFragment();
        mEventsOfLockFragment = new EventsOfLockFragment();
        mEventsList.add(mEventsOfLockFragment);
        mEventsList.add(mEventsOfUserFragment);


        mEventsPagerAdapter = new EventsPagerAdapter(getSupportFragmentManager());
        mEventsVp.setAdapter(mEventsPagerAdapter);

        initTabLayout();

        mEventsVp.setOffscreenPageLimit(1);
        mEventsVp.setCurrentItem(0);
        mEventsVp.setNoScroll(true);

//        mEventsVp.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View view, boolean b) {
//                mSelectEventRl.setVisibility(View.GONE);
//                mEventsOfLockFragment.cancelDelete();
//                mEventsOfUserFragment.cancelDelete();
//            }
//        });

        mSelectCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mTipTv.setText(R.string.cancel);
                    if (mEventsVp.getCurrentItem() == 0) {
                        mEventsOfLockFragment.selectedAll();
                    } else {
                        mEventsOfUserFragment.selectedAll();
                    }
                } else {
                    mTipTv.setText(R.string.all_election);
                    if (mEventsVp.getCurrentItem() == 0) {
                        mEventsOfLockFragment.cancelSelectedAll();
                    } else {
                        mEventsOfUserFragment.cancelSelectedAll();
                    }
                }
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mEventsVp.setOnCapturedPointerListener(new View.OnCapturedPointerListener() {
                @Override
                public boolean onCapturedPointer(View view, MotionEvent motionEvent) {
                    mSelectEventRl.setVisibility(View.GONE);
                    mEventsOfLockFragment.cancelDelete();
                    mEventsOfUserFragment.cancelDelete();
                    return false;
                }
            });
        } else {
            mEventsVp.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int i, float v, int i1) {

                }

                @Override
                public void onPageSelected(int i) {
                    mSelectEventRl.setVisibility(View.GONE);
                    mEventsOfLockFragment.cancelDelete();
                    mEventsOfUserFragment.cancelDelete();
                }

                @Override
                public void onPageScrollStateChanged(int i) {
                }
            });
        }

    }

    private void initEvent() {
        mEventsVp.addOnPageChangeListener(this);

        //查询用户事件
        if (mDevice.getState() == Device.BLE_CONNECTED) {
            mLoadDialog.show();
            android.os.Message msg = android.os.Message.obtain();
            msg.what = RECEIVER_LOG_TIME_OUT;
            mHandler.sendMessageDelayed(msg, 10 * 1000);
            if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MASTER) {
                mBleManagerHelper.getBleCardService().sendCmd31(BleMsg.TYPE_QUERY_ALL_USERS_LOG, mDefaultDevice.getUserId());
            } else if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MEMBER) {
                mBleManagerHelper.getBleCardService().sendCmd31(BleMsg.TYPE_QUERY_USER_LOG, mDefaultDevice.getUserId());
            }
        } else {
            showMessage(getString(R.string.disconnect_ble));
            finish();
        }
        //查询开锁事件
        if (mDevice.getState() == Device.BLE_CONNECTED) {
            mLoadDialog.show();
            android.os.Message msg = android.os.Message.obtain();
            msg.what = RECEIVER_LOG_TIME_OUT;
            mHandler.sendMessageDelayed(msg, 10 * 1000);
            if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MASTER) {
                mBleManagerHelper.getBleCardService().sendCmd31(BleMsg.TYPE_QUERY_ALL_LOCK_LOG, mDefaultDevice.getUserId());
            } else if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MEMBER) {
                mBleManagerHelper.getBleCardService().sendCmd31(BleMsg.TYPE_QUERY_LOCK_LOG, mDefaultDevice.getUserId());
            }
        } else {
            showMessage(getString(R.string.disconnect_ble));
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mDefaultDevice.isEnableFace()) {
            getMenuInflater().inflate(R.menu.events_management_with_face, menu);
        } else {
            getMenuInflater().inflate(R.menu.events_management, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_edit:
                if (item.getTitle().toString().equals(getString(R.string.edit))) {
                    if (mEventsVp.getCurrentItem() == 0) {
                        mEventsOfLockFragment.editDelete();
                    } else if (mEventsVp.getCurrentItem() == 1) {
                        mEventsOfUserFragment.editDelete();
                    }
                    mSelectEventRl.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.show_album:
                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_OPEN_ALBUM);
                } else {
                    showMessage(getString(R.string.disconnect_ble));
                }
                break;
            default:
                break;

        }
        return true;
    }

    /**
     * 初始化tb
     */
    private void initTabLayout() {
        mEventsTl.setTabMode(TabLayout.MODE_FIXED);
        mEventsTl.setSelectedTabIndicatorColor(getResources().getColor(R.color.yellow_selete));
        mEventsTl.setSelectedTabIndicatorHeight(getResources().getDimensionPixelSize(R.dimen.y5dp));
        mEventsTl.setupWithViewPager(mEventsVp);
    }

    /**
     * 吐司提示
     *
     * @param msg 提示信息
     */
    protected void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void initActionBar() {
        mTitleTv.setText(R.string.unlock_key);

        mEventsTb.setNavigationIcon(R.mipmap.btn_back);

        setSupportActionBar(mEventsTb);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        mEventsTb.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
//                LogUtil.d(TAG, "OnBack");
            }
        });
    }


    //分发MSG 3E
    private void dispatchErrorCode(Bundle bundle) {
        byte[] errCode = bundle.getByteArray(BleMsg.KEY_ERROR_CODE);
        if (errCode == null) {
            return;
        }
        LogUtil.i(TAG, "errCode : " + errCode[3]);
        switch (errCode[3]) {
            case BleMsg.TYPE_RECEIVER_LOGS_OVER:
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (mHandler.hasMessages(RECEIVER_LOG_TIME_OUT)) {
                    mHandler.removeMessages(RECEIVER_LOG_TIME_OUT);
                }
                if (mDialogCount++ >= 1) {
                    mDialogCount = 0;
                    DialogUtils.closeDialog(mLoadDialog);
                }


                mEventsOfUserFragment.getLogsOver(mNodeId, mDefaultDevice, mDeviceUser);
                mEventsOfLockFragment.getLogsOver(mNodeId, mDefaultDevice, mDeviceUser);
                break;
            case BleMsg.TYPE_DELETE_LOG_SUCCESS:
//                mDelDeVLog = (DeviceLog) bundle.getSerializable(BleMsg.KEY_SERIALIZABLE);
//                if (mDelDeVLog != null) {
//                    mEventsOfUserAdapter.removeItem(mDelDeVLog);
//                }
                if (mEventsOfUserFragment.delLogSuccess(mDeviceUser, bundle)) {
                    DialogUtils.closeDialog(mLoadDialog);
                }
                if (mEventsOfLockFragment.delLogSuccess(mDeviceUser, bundle)) {
                    DialogUtils.closeDialog(mLoadDialog);
                }
                break;
            case BleMsg.TYPE_DELETE_LOG_FAILED:
                showMessage(this.getString(R.string.del_log_failed));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_NO_AUTHORITY_3E:
                showMessage(this.getString(R.string.no_authority));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_OPEN_ALBUM_SUCCESS:
                showMessage(this.getString(R.string.open_album_success));
                break;
            case BleMsg.TYPE_OPEN_ALBUM_FAILED:
                showMessage(this.getString(R.string.open_album_failed));
                break;
            default:
                break;

        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back_tv:
                mSelectEventRl.setVisibility(View.GONE);
                mSelectCb.setChecked(false);
                if (mEventsVp.getCurrentItem() == 0) {
                    mEventsOfLockFragment.cancelDelete();
                } else if (mEventsVp.getCurrentItem() == 1) {
                    mEventsOfUserFragment.cancelDelete();
                }
                break;
            case R.id.del_tv:
                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    if (mEventsVp.getCurrentItem() == 0) {
                        mEventsOfLockFragment.doDelete(mDeviceUser.getUserId());
                    } else if (mEventsVp.getCurrentItem() == 1) {
                        mEventsOfUserFragment.doDelete(mDeviceUser.getUserId());
                    }
                } else {
                    showMessage(getString(R.string.disconnect_ble));
                }
                break;
            case R.id.tv_open_album:
                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_OPEN_ALBUM);
                } else {
                    showMessage(getString(R.string.disconnect_ble));
                }
                break;

            default:
                break;
        }

    }

    @Override
    public void onPageScrolled(int i, float v, int i1) {

    }

    @Override
    public void onPageSelected(int i) {

    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

    @Override
    public void deviceStateChange(Device device, int state) {

    }

    @Override
    public void dispatchUiCallback(Message msg, Device device, int type) {
        mDevice = device;
        Bundle bundle = msg.getData();
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_1E:
            case Message.TYPE_BLE_RECEIVER_CMD_3E:
                LogUtil.d(TAG, "MSG = " + msg.toString());
                final byte[] errCode = msg.getData().getByteArray(BleMsg.KEY_ERROR_CODE);
                LogUtil.d(TAG, "MSG = " + Arrays.toString(errCode));
                if (errCode != null) {
                    dispatchErrorCode(bundle);
                }
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_32:
                LogUtil.d(TAG, "receiver " + 1 + " log!");
                android.os.Message message = new android.os.Message();
                message.what = type;
                LogUtil.d(TAG, "receiver type = " + type);
                mHandler.sendMessage(message);
                break;
            default:
                break;

        }

    }

    @Override
    public void reConnectBle(Device device) {

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

    private class EventsPagerAdapter extends FragmentPagerAdapter {

        public EventsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public BaseFragment getItem(int i) {
            return mEventsList.get(i);
        }

        @Override
        public int getCount() {
            return mEventsList.size();
        }

        public CharSequence getPageTitle(int position) {
            return mTitleList.get(position);
        }
    }
}
