
package com.smart.lock.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smart.lock.R;
import com.smart.lock.adapter.LockManagerAdapter;
import com.smart.lock.adapter.ViewPagerAdapter;
import com.smart.lock.ble.BleCardService;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.ui.AddDeviceActivity;
import com.smart.lock.ui.CardManagerActivity;
import com.smart.lock.ui.EventsActivity;
import com.smart.lock.ui.FingerPrintManagerActivity;
import com.smart.lock.ui.PwdManagerActivity;
import com.smart.lock.ui.TempPwdActivity;
import com.smart.lock.ui.UserManagerActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.widget.MyGridView;

import java.util.ArrayList;
import java.util.Arrays;


public class HomeFragment extends BaseFragment implements View.OnClickListener, AdapterView.OnItemClickListener {

    private Toolbar mToolbar;
    private View mHomeView;
    private ViewPager mViewPager;
    private MyGridView mMyGridView;
    private RelativeLayout mAddLockRl;
    private LinearLayout mLockManagerLl;
    private Button mAddLockBt;
    private TextView mLockNameTv;
    private TextView mLockSettingTv;
    private ImageView mEqIv; //电量的图片
    private TextView mEqTv; //电量的显示
    private TextView mUpdateTimeTv;
    private TextView mShowTimeTv;
    private TextView mLockStatusTv;
    private TextView mBleConnectTv;

    private ViewPagerAdapter mAdapter; //news adapter
    private LockManagerAdapter mLockAdapter; //gridView adapter
    private DeviceInfo mDefaultDevice; //默认设备
    private DeviceUser mDefaultUser; //默认用户
    private ArrayList<DeviceInfo> mDeviceInfos; //设备集合
    private ArrayList<View> mDots; //spot list

    private int mOldPosition = 0;// 记录上一次点的位置
    private int mCurrentItem; // 当前页面

    public static final int BIND_DEVICE = 0; //用户已添加设备
    public static final int UNBIND_DEVICE = 1;//未添加设备

    public static final int BATTER_0 = 0;//电量10%
    public static final int BATTER_10 = 10;//电量10%
    public static final int BATTER_20 = 20;//电量20%
    public static final int BATTER_30 = 30;//电量35%
    public static final int BATTER_50 = 50;//电量50%
    public static final int BATTER_60 = 60;//电量60%
    public static final int BATTER_70 = 70;//电量70%
    public static final int BATTER_80 = 80;//电量80%
    public static final int BATTER_100 = 100;//电量100%

    public static final int BATTER_FULL = 100;//电量充足
    public static final int BATTER_LOW = 101;//电量缺少
    public static final int BATTER_UNKNOW = 102;//电量未知

    private boolean mOpenTest = false; // 测试连接的开关

    private int mBattery = 0;

    /**
     * 服务连接标志
     */
    private boolean mIsConnected = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View initView() {
        mHomeView = View.inflate(mActivity, R.layout.home_fragment, null);
        mViewPager = mHomeView.findViewById(R.id.news_vp);
        mMyGridView = mHomeView.findViewById(R.id.gv_lock);
        mAddLockRl = mHomeView.findViewById(R.id.rl_add_lock);
        mLockManagerLl = mHomeView.findViewById(R.id.ll_lock_manager);
        mAddLockBt = mHomeView.findViewById(R.id.btn_add_lock);
        mLockNameTv = mHomeView.findViewById(R.id.tv_lock_name);
        mLockSettingTv = mHomeView.findViewById(R.id.bt_setting);
        mEqIv = mHomeView.findViewById(R.id.iv_electric_quantity);
        mEqTv = mHomeView.findViewById(R.id.tv_electric_quantity);
        mUpdateTimeTv = mHomeView.findViewById(R.id.tv_update_time);
        mShowTimeTv = mHomeView.findViewById(R.id.tv_update);
        mLockStatusTv = mHomeView.findViewById(R.id.tv_status);
        mBleConnectTv = mHomeView.findViewById(R.id.tv_connect);
        initEvent();
        return mHomeView;
    }

    public void setTestMode(boolean openTest) {
        mOpenTest = openTest;
        if (mOpenTest) {
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    if (mDefaultDevice != null && !BleManagerHelper.getInstance(mHomeView.getContext(), mDefaultDevice.getBleMac(), false).getServiceConnection()) {
                        setSk();
                        BleManagerHelper.getInstance(mHomeView.getContext(), mDefaultDevice.getBleMac(), false).connectBle((byte) 1, Short.parseShort(mDefaultDevice.getDeviceUser()));
                    } else
                        BleManagerHelper.getInstance(mHomeView.getContext(), mDefaultDevice.getBleMac(), false).getBleCardService().disconnect();
                }
            }, 2000);
        }
    }

    private void initEvent() {
        mAddLockBt.setOnClickListener(this);
        mLockSettingTv.setOnClickListener(this);
        mMyGridView.setOnItemClickListener(this);
        mBleConnectTv.setOnClickListener(this);
    }

    public void initDate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {  //版本检测
            mToolbar = mHomeView.findViewById(R.id.tb_toolbar);
            ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);  //将ToolBar设置成ActionBar
        }
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);

        // 显示的点
        mDots = new ArrayList<View>();
        mDots.add((View) getView().findViewById(R.id.dot_0));
        mDots.add((View) getView().findViewById(R.id.dot_1));

        mAdapter = new ViewPagerAdapter(mHomeView.getContext());
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                // TODO Auto-generated method stub

                mDots.get(mOldPosition).setBackgroundResource(R.drawable.dot_normal);
                mDots.get(position).setBackgroundResource(R.drawable.dot_focused);

                mOldPosition = position;
                mCurrentItem = position;
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
                // TODO Auto-generated method stub

            }
        });

        LocalBroadcastManager.getInstance(mHomeView.getContext()).registerReceiver(deviceReciver, intentFilter());

        mDefaultDevice = DeviceInfoDao.getInstance(mHomeView.getContext()).queryFirstData("device_default", true);
        LogUtil.d(TAG, "mDefaultDevice = " + mDefaultDevice);

        if (mDefaultDevice != null && !BleManagerHelper.getInstance(mHomeView.getContext(), mDefaultDevice.getBleMac(), false).getServiceConnection()) {
            setSk();
            BleManagerHelper.getInstance(mHomeView.getContext(), mDefaultDevice.getBleMac(), false).connectBle((byte) 1, Short.parseShort(mDefaultDevice.getDeviceUser()));
            refreshView(BIND_DEVICE);
        } else refreshView(UNBIND_DEVICE);

    }

    /**
     * 刷新显示界面
     */
    private void refreshView(int status) {
        switch (status) {
            case BIND_DEVICE:
                mAddLockRl.setVisibility(View.GONE);
                mLockManagerLl.setVisibility(View.VISIBLE);
                LogUtil.d(TAG, "mIsConnected = " + mIsConnected);
                if (mIsConnected) {
                    mLockStatusTv.setText(R.string.bt_connect_success);
                    mBleConnectTv.setVisibility(View.GONE);
                    if (mDefaultDevice != null)
                        mLockNameTv.setText(mDefaultDevice.getDeviceName());

                    refreshBattery(mBattery);
                } else {
                    mLockStatusTv.setText(R.string.bt_connect_failed);
                    mBleConnectTv.setVisibility(View.VISIBLE);
                    mBleConnectTv.setEnabled(true);
                    refreshView(BATTER_UNKNOW);
                }
                if (mDefaultDevice != null) {
                    mDefaultUser = DeviceUserDao.getInstance(mHomeView.getContext()).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getDeviceUser());
                    mNodeId = mDefaultDevice.getDeviceNodeId();
                    mLockAdapter = new LockManagerAdapter(mHomeView.getContext(), mMyGridView, mDefaultUser.getUserPermission());
                    mMyGridView.setAdapter(mLockAdapter);
                }
                break;
            case UNBIND_DEVICE:
                mAddLockRl.setVisibility(View.VISIBLE);
                mLockManagerLl.setVisibility(View.GONE);
                break;
            case BATTER_FULL:
                mEqIv.setBackgroundResource(R.mipmap.ic_battery_100);
                mEqTv.setText(R.string.battery_100);
                break;
            case BATTER_LOW:
                mEqIv.setBackgroundResource(R.mipmap.ic_battery_10);
                mEqTv.setText(R.string.battery_10);
                break;
            case BATTER_UNKNOW:
                mUpdateTimeTv.setVisibility(View.GONE);
                mShowTimeTv.setVisibility(View.GONE);
                if (mDefaultDevice != null)
                    mLockNameTv.setText(mDefaultDevice.getDeviceName());
                mEqIv.setBackgroundResource(R.mipmap.lock_manager_battery_unknow);
                mEqTv.setText(R.string.battery_unknow);
                break;
            default:
                break;
        }
    }

    /**
     * 电量刷新
     *
     * @param battery 电量值
     */
    private void refreshBattery(int battery) {
        mUpdateTimeTv.setVisibility(View.VISIBLE);
        mShowTimeTv.setVisibility(View.VISIBLE);
        mEqTv.setText(String.valueOf(battery) + "%");
        mUpdateTimeTv.setText(DateTimeUtil.timeStamp2Date(String.valueOf(System.currentTimeMillis() / 1000), "yyyy-MM-dd HH:mm:ss"));
        switch (battery / 10) {
            case 0:
                mEqIv.setBackgroundResource(R.mipmap.ic_battery_10);
                break;
            case 1:
                mEqIv.setBackgroundResource(R.mipmap.ic_battery_10);
                break;
            case 2:
                mEqIv.setBackgroundResource(R.mipmap.ic_battery_20);
                break;
            case 3:
                mEqIv.setBackgroundResource(R.mipmap.ic_battery_30);
                break;
            case 4:
            case 5:
                mEqIv.setBackgroundResource(R.mipmap.ic_battery_50);
                break;
            case 6:
                mEqIv.setBackgroundResource(R.mipmap.ic_battery_60);
                break;
            case 7:
                mEqIv.setBackgroundResource(R.mipmap.ic_battery_70);
                break;
            case 8:
                mEqIv.setBackgroundResource(R.mipmap.ic_battery_80);
                break;
            case 9:
            case 10:
                mEqIv.setBackgroundResource(R.mipmap.ic_battery_100);
                break;
            default:
                refreshView(BATTER_UNKNOW);
                break;
        }
    }

    private void checkUserId(ArrayList<String> userIds) {
        ArrayList<DeviceUser> users = DeviceUserDao.getInstance(mActivity).queryDeviceUsers(mDefaultDevice.getDeviceNodeId());

        if (!userIds.isEmpty()) {
            for (DeviceUser user : users) {
                Log.d(TAG, "getUserId 1= " + user.getUserId());
                if (userIds.contains(user.getUserId())) {
                    DeviceUserDao.getInstance(mActivity).delete(user);
                    userIds.remove(user.getUserId());
                }
            }
            for (String userId : userIds) {
                int i = Integer.parseInt(userId);
                if (i > 0 && i <= 100) { //管理员
                    createDeviceUser(userId, null, ConstantUtil.DEVICE_MASTER);
                } else if (i > 200 && i <= 300) {
                    createDeviceUser(userId, null, ConstantUtil.DEVICE_TEMP);
                } else {
                    createDeviceUser(userId, null, ConstantUtil.DEVICE_MEMBER);
                }

            }
        }
    }

    /**
     * 广播接收
     */
    private final BroadcastReceiver deviceReciver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // 4.2.3 MSG 04
            if (action.equals(BleMsg.STR_RSP_SECURE_CONNECTION)) {
                mBattery = intent.getByteExtra(BleMsg.KEY_BAT_PERSCENT, (byte) 0);
                int userStatus = intent.getByteExtra(BleMsg.KEY_USER_STATUS, (byte) 0);
                int stStatus = intent.getByteExtra(BleMsg.KEY_SETTING_STATUS, (byte) 0);
                int unLockTime = intent.getByteExtra(BleMsg.KEY_UNLOCK_TIME, (byte) 0);
                byte[] syncUsers = intent.getByteArrayExtra(BleMsg.KEY_SYNC_USERS);
                byte[] userState = intent.getByteArrayExtra(BleMsg.KEY_USERS_STATE);
                byte[] tempSecret = intent.getByteArrayExtra(BleMsg.KEY_TMP_PWD_SK);

                LogUtil.d(TAG, "battery = " + mBattery + "\n" + "userStatus = " + userStatus + "\n" + " stStatus = " + stStatus + "\n" + " unLockTime = " + unLockTime);
                LogUtil.d(TAG, "syncUsers = " + Arrays.toString(syncUsers));
                byte[] buf = new byte[4];
                System.arraycopy(syncUsers, 0, buf, 0, 4);
                String status1 = String.valueOf(Integer.parseInt(StringUtil.bytesToHexString(buf), 16));
                LogUtil.d(TAG, "status1 = " + status1);

                System.arraycopy(syncUsers, 4, buf, 0, 4);
                String status2 = String.valueOf(Integer.parseInt(StringUtil.bytesToHexString(buf), 16));
                LogUtil.d(TAG, "status2 = " + status2);

                System.arraycopy(syncUsers, 8, buf, 0, 4);
                String status3 = String.valueOf(Integer.parseInt(StringUtil.bytesToHexString(buf), 16));
                LogUtil.d(TAG, "status3 = " + status3);

                System.arraycopy(syncUsers, 12, buf, 0, 4);
                String status4 = String.valueOf(Integer.parseInt(StringUtil.bytesToHexString(buf), 16));
                LogUtil.d(TAG, "status4 = " + status4);

                if (mDefaultDevice != null) {
                    checkUserId(DeviceUserDao.getInstance(mActivity).checkUserStatus(Integer.parseInt(status1), mDefaultDevice.getDeviceNodeId(), 1));
                    checkUserId(DeviceUserDao.getInstance(mActivity).checkUserStatus(Integer.parseInt(status2), mDefaultDevice.getDeviceNodeId(), 2));
                    checkUserId(DeviceUserDao.getInstance(mActivity).checkUserStatus(Integer.parseInt(status3), mDefaultDevice.getDeviceNodeId(), 3));
                    checkUserId(DeviceUserDao.getInstance(mActivity).checkUserStatus(Integer.parseInt(status4), mDefaultDevice.getDeviceNodeId(), 4));
                    DeviceUserDao.getInstance(mActivity).checkUserState(mDefaultDevice.getDeviceNodeId(), userState);

                    mDefaultDevice.setTempSecret(StringUtil.bytesToHexString(tempSecret));
                }

                mIsConnected = true;
                refreshView(BIND_DEVICE);
                refreshBattery(mBattery);

                if (mOpenTest) {
                    new Handler().postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            BleManagerHelper.getInstance(mHomeView.getContext(), mDefaultDevice.getBleMac(), false).getBleCardService().disconnect();
                        }
                    }, 5000);
                }

            }

            if (action.equals(BleMsg.ACTION_GATT_DISCONNECTED)) {
                mIsConnected = false;
                refreshView(BIND_DEVICE);
                if (mDefaultDevice != null) {
                    setSk();
                    BleManagerHelper.getInstance(mHomeView.getContext(), mDefaultDevice.getBleMac(), false).connectBle((byte) 1, Short.parseShort(mDefaultDevice.getDeviceUser()));
                } else refreshView(UNBIND_DEVICE);
            }

            if (action.equals(BleMsg.STR_RSP_SET_TIMEOUT)) {
                if (mDefaultDevice != null) {
                    refreshView(BIND_DEVICE);
                } else refreshView(UNBIND_DEVICE);
            }

            if (action.equals(BleMsg.STR_RSP_OPEN_TEST)) {
                setTestMode(intent.getBooleanExtra(ConstantUtil.OPEN_TEST, false));
            }
        }
    };

    protected static IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.STR_RSP_SECURE_CONNECTION);
        intentFilter.addAction(BleMsg.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleMsg.STR_RSP_SET_TIMEOUT);
        intentFilter.addAction(BleMsg.STR_RSP_OPEN_TEST);
        return intentFilter;
    }

    public void onResume() {
        super.onResume();
        if (mDefaultDevice != null) {
            refreshView(BIND_DEVICE);
        } else refreshView(UNBIND_DEVICE);
    }

    public void onPause() {
        super.onPause();
    }

    public void onDestroy() {
        super.onDestroy();

        try {
            LocalBroadcastManager.getInstance(mHomeView.getContext()).unregisterReceiver(deviceReciver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        mIsConnected = false;

        if (mDefaultDevice != null)
            BleManagerHelper.getInstance(mHomeView.getContext(), mDefaultDevice.getBleMac(), false).stopService();
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_add_lock:
                startIntent(AddDeviceActivity.class, null);
                break;
            case R.id.tv_connect:
                mBleConnectTv.setEnabled(false);
                setSk();
                BleManagerHelper.getInstance(mHomeView.getContext(), mDefaultDevice.getBleMac(), false).connectBle((byte) 1, Short.parseShort(mDefaultDevice.getDeviceUser()));
                break;
            default:
                break;
        }

    }

    /**
     * 设置秘钥
     */
    private void setSk() {
        String mac = mDefaultDevice.getBleMac().replace(":", "");

        byte[] macByte = StringUtil.hexStringToBytes(mac);

        String defaultNodeId = mDefaultDevice.getDeviceNodeId();
        LogUtil.d(TAG, "defaultNodeId = " + defaultNodeId);

        byte[] nodeId = StringUtil.hexStringToBytes(defaultNodeId);

        StringUtil.exchange(nodeId);

        LogUtil.d(TAG, "nodeId = " + Arrays.toString(nodeId));
        LogUtil.d(TAG, "macByte = " + Arrays.toString(macByte));

        System.arraycopy(nodeId, 0, MessageCreator.mSK, 0, 8); //写入IMEI

        System.arraycopy(macByte, 0, MessageCreator.mSK, 8, 6); //写入MAC

        byte[] code = new byte[18];
        String secretCode = mDefaultDevice.getDeviceSecret();
        if (secretCode == null || secretCode.equals("0")) {
            Arrays.fill(MessageCreator.mSK, 14, 32, (byte) 0);
        } else {
            code = StringUtil.hexStringToBytes(secretCode);
            System.arraycopy(code, 0, MessageCreator.mSK, 14, 18); //写入secretCode
        }

        LogUtil.d(TAG, "sk = " + Arrays.toString(MessageCreator.mSK));

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);
        switch (((Integer) view.getTag()).intValue()) {
            case R.mipmap.manager_pwd:
                if (mIsConnected)
                    startIntent(PwdManagerActivity.class, bundle);
                else
                    showMessage(mHomeView.getContext().getString(R.string.unconnected_device));
                break;
            case R.mipmap.manager_card:
                if (mIsConnected)
                    startIntent(CardManagerActivity.class, bundle);
                else
                    showMessage(mHomeView.getContext().getString(R.string.unconnected_device));

                break;
            case R.mipmap.manager_finger:
                if (mIsConnected)
                    startIntent(FingerPrintManagerActivity.class, bundle);
                else
                    showMessage(mHomeView.getContext().getString(R.string.unconnected_device));
                break;

            case R.mipmap.manager_event:
                if (mIsConnected)
                    startIntent(EventsActivity.class, bundle);
                else
                    showMessage(mHomeView.getContext().getString(R.string.unconnected_device));
                break;
            case R.mipmap.manager_permission:
                startIntent(UserManagerActivity.class, bundle);

                break;
            case R.mipmap.manager_token:
                startIntent(TempPwdActivity.class, bundle);
                break;
            default:
                break;

        }
    }
}
