
package com.smart.lock.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.MainActivity;
import com.smart.lock.R;
import com.smart.lock.adapter.LockManagerAdapter;
import com.smart.lock.adapter.ViewPagerAdapter;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.ClientTransaction;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceStatus;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceStatusDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.permission.PermissionHelper;
import com.smart.lock.ui.AddDeviceActivity;
import com.smart.lock.ui.DeviceKeyActivity;
import com.smart.lock.ui.EventsActivity;
import com.smart.lock.ui.LockDetectingActivity;
import com.smart.lock.ui.LockSettingActivity;
import com.smart.lock.ui.TempPwdActivity;
import com.smart.lock.ui.UserManagerActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.MyGridView;
import com.yzq.zxinglibrary.android.CaptureActivity;
import com.yzq.zxinglibrary.bean.ZxingConfig;
import com.yzq.zxinglibrary.common.Constant;

import java.util.ArrayList;
import java.util.Arrays;


public class HomeFragment extends BaseFragment implements View.OnClickListener, AdapterView.OnItemClickListener {
    private static final String TAG = HomeFragment.class.getSimpleName();
    private Toolbar mToolbar;
    private View mHomeView;
    private ViewPager mViewPager;
    private MyGridView mMyGridView;
    private LinearLayout mAddLockLl;
    private RelativeLayout mNewsVpRL;
    private RelativeLayout mLockManagerRl;
    private Button mAddLockBt;
    private TextView mLockNameTv;
    private LinearLayout mLockSettingLl;
    private ImageView mEqIv; //电量的图片
    private TextView mEqTv; //电量的显示
    private TextView mUpdateTimeTv;
    private TextView mShowTimeTv;
    private TextView mLockStatusTv;
    private ImageView mBleConnectIv;
    private ImageView mInstructionBtn;
    private ImageView mScanQrIv;

    private ViewPagerAdapter mAdapter; //news adapter
    private LockManagerAdapter mLockAdapter; //gridView adapter
    private DeviceInfo mDefaultDevice; //默认设备
    private DeviceUser mDefaultUser; //默认用户
    private DeviceStatus mDefaultStatus; //用户状态
    private ArrayList<DeviceInfo> mDeviceInfos; //设备集合
    private ArrayList<View> mDots; //spot list

    private int mOldPosition = 0;// 记录上一次点的位置
    private int mCurrentItem; // 当前页面

    public static final int BIND_DEVICE = 0; //用户已添加设备
    public static final int UNBIND_DEVICE = 1;//未添加设备

    public static final int DEVICE_CONNECTING = 2;//未添加设备

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
    private int REQUESTCODE = 0;
    private String[] mPermission = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private int mImageIds[];

    /**
     * 服务连接标志
     */
    private boolean mIsConnected = false;
    private long mStartTime = 0, mEndTime = 0;

    private int mHeight;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String mSn;
        String mBleMac;
        // 扫描二维码/条码回传
        if (requestCode == Activity.RESULT_FIRST_USER && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String content = data.getStringExtra(Constant.CODED_CONTENT);
                LogUtil.d(TAG, "content = " + content);
                String[] dvInfo = content.split(",");
                if (dvInfo.length == 3 && dvInfo[0].length() == 18 && dvInfo[1].length() == 12 && dvInfo[2].length() == 15) {
                    mSn = dvInfo[0];
                    mBleMac = dvInfo[1];
                    mNodeId = dvInfo[2];

                    if (DeviceInfoDao.getInstance(getContext()).queryByField(DeviceInfoDao.NODE_ID, "0" + mNodeId) == null) {
                        Bundle bundle = new Bundle();
                        bundle.putString(BleMsg.KEY_BLE_MAC, mBleMac);
                        bundle.putString(BleMsg.KEY_NODE_SN, mSn);
                        bundle.putString(BleMsg.KEY_NODE_ID, mNodeId);

                        startIntent(LockDetectingActivity.class, bundle);
                    } else {
                        ToastUtil.show(mHomeView.getContext(), getString(R.string.device_has_been_added), Toast.LENGTH_LONG);
                    }

                } else {
                    ToastUtil.show(mHomeView.getContext(), getString(R.string.plz_scan_correct_qr), Toast.LENGTH_LONG);
                }
            }
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHeight = this.getResources().getDisplayMetrics().heightPixels;
    }

    public View initView() {
        mHomeView = View.inflate(mActivity, R.layout.home_fragment, null);
        mNewsVpRL = mHomeView.findViewById(R.id.rl_news_vp);
        mViewPager = mHomeView.findViewById(R.id.news_vp);
        mMyGridView = mHomeView.findViewById(R.id.gv_lock);
        mAddLockLl = mHomeView.findViewById(R.id.rl_add_lock);
        mLockManagerRl = mHomeView.findViewById(R.id.ll_lock_manager);
        mAddLockBt = mHomeView.findViewById(R.id.btn_add_lock);
        mLockNameTv = mHomeView.findViewById(R.id.tv_lock_name);
        mLockSettingLl = mHomeView.findViewById(R.id.ll_setting);
        mEqIv = mHomeView.findViewById(R.id.iv_electric_quantity);
        mEqTv = mHomeView.findViewById(R.id.tv_electric_quantity);
        mUpdateTimeTv = mHomeView.findViewById(R.id.tv_update_time);
        mShowTimeTv = mHomeView.findViewById(R.id.tv_update);
        mLockStatusTv = mHomeView.findViewById(R.id.tv_status);
        mBleConnectIv = mHomeView.findViewById(R.id.iv_connect);
        mScanQrIv = mHomeView.findViewById(R.id.iv_scan_qr);
        initEvent();
        LogUtil.d(TAG, "mHeight = " + mHeight);
        //设置界面比例
        RelativeLayout.LayoutParams mLockManagerRlParams = (RelativeLayout.LayoutParams) mLockManagerRl.getLayoutParams();
        mLockManagerRlParams.setMargins(0, -(int) (mHeight * 0.12), 0, 0);

        mMyGridView.getLayoutParams().height = (int) (mHeight * 0.3);
        mHomeView.findViewById(R.id.ll_management).getLayoutParams().height = (int) (mHeight * 0.14);

        mHomeView.findViewById(R.id.ll_gv_lock).setPadding(0, (int) (mHeight * 0.025), 0, 0);

        return mHomeView;
    }

    public void setmInstructionBtn(View view) {
        mInstructionBtn = (ImageView) view;
    }

    public void setTestMode(boolean openTest) {
        mOpenTest = openTest;
        if (mOpenTest) {
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    if (mDefaultDevice != null && !mBleManagerHelper.getServiceConnection()) {
                        setSk();
                        mBleManagerHelper.connectBle((byte) 1, mDefaultUser.getUserId(), mDefaultDevice.getBleMac());
                    } else
                        mBleManagerHelper.getBleCardService().disconnect();
                }
            }, 2000);
        }
    }

    private void initEvent() {
        mAddLockBt.setOnClickListener(this);
        mLockSettingLl.setOnClickListener(this);
        mMyGridView.setOnItemClickListener(this);
        mBleConnectIv.setOnClickListener(this);
        mInstructionBtn.setOnClickListener(this);
        mScanQrIv.setOnClickListener(this);
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

                mDots.get(mOldPosition).setBackgroundResource(R.drawable.dot_focused);
                mDots.get(position).setBackgroundResource(R.drawable.dot_normal);

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

        mImageIds = new int[]{
                R.mipmap.homepage_adv3,
                R.mipmap.homepage_adv4
        };

        mBleManagerHelper = BleManagerHelper.getInstance(mHomeView.getContext(), false);
        LocalBroadcastManager.getInstance(mHomeView.getContext()).registerReceiver(deviceReciver, intentFilter());
    }

    /**
     * 打开第三方二维码扫描库
     */
    private void scanQr() {
        Intent newIntent = new Intent(mHomeView.getContext(), CaptureActivity.class);
        ZxingConfig config = new ZxingConfig();
        config.setPlayBeep(true);//是否播放扫描声音 默认为true
        config.setShake(true);//是否震动  默认为true
        config.setDecodeBarCode(false);//是否扫描条形码 默认为true
        config.setReactColor(R.color.colorAccent);//设置扫描框四个角的颜色 默认为淡蓝色
        config.setFrameLineColor(R.color.colorAccent);//设置扫描框边框颜色 默认无色
        config.setFullScreenScan(true);//是否全屏扫描  默认为true  设为false则只会在扫描框中扫描
        newIntent.putExtra(Constant.INTENT_ZXING_CONFIG, config);
        startActivityForResult(newIntent, Activity.RESULT_FIRST_USER);
    }

    /**
     * 刷新显示界面
     */
    private void refreshView(int status) {
        switch (status) {
            case DEVICE_CONNECTING:
                LogUtil.d(TAG, "DEVICE_CONNECTING");
                mNewsVpRL.getLayoutParams().height = (int) (mHeight * 0.41);
                mAddLockLl.setVisibility(View.GONE);
                mLockManagerRl.setVisibility(View.VISIBLE);
                mLockStatusTv.setText(R.string.bt_connecting);
                mBleConnectIv.setClickable(false);
                mBleConnectIv.setImageResource(R.mipmap.icon_bluetooth_nor);
                refreshView(BATTER_UNKNOW);
                if (mDefaultDevice != null) {
                    mDefaultUser = DeviceUserDao.getInstance(mHomeView.getContext()).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId());
                    LogUtil.d(TAG, "mDefaultUser = " + mDefaultUser.toString());
                    mNodeId = mDefaultDevice.getDeviceNodeId();
                    mLockAdapter = new LockManagerAdapter(mHomeView.getContext(), mMyGridView, mDefaultUser.getUserPermission());
                    mDefaultStatus = DeviceStatusDao.getInstance(mHomeView.getContext()).queryOrCreateByNodeId(mNodeId);
                    mMyGridView.setAdapter(mLockAdapter);
                    mAdapter.setImageIds(mImageIds);
                    mAdapter.notifyDataSetChanged();
                }
                break;
            case BIND_DEVICE:
                mNewsVpRL.getLayoutParams().height = (int) (mHeight * 0.41);
                mAddLockLl.setVisibility(View.GONE);
                mLockManagerRl.setVisibility(View.VISIBLE);
                LogUtil.d(TAG, "mIsConnected = " + mIsConnected);

                if (mIsConnected) {
                    mLockStatusTv.setText(R.string.bt_connect_success);
                    mBleConnectIv.setClickable(false);
                    mBleConnectIv.setImageResource(R.mipmap.icon_bluetooth_nor);
                    if (mDefaultDevice != null)
                        mLockNameTv.setText(mDefaultDevice.getDeviceName());

                    refreshBattery(mBattery);
                } else {
                    mLockStatusTv.setText(R.string.bt_connect_failed);
                    mBleConnectIv.setClickable(true);
                    mBleConnectIv.setImageResource(R.mipmap.icon_bluetooth);
                    refreshView(BATTER_UNKNOW);
                }
                mDefaultDevice = DeviceInfoDao.getInstance(mHomeView.getContext()).queryFirstData("device_default", true);
                if (mDefaultDevice != null) {
                    mDefaultUser = DeviceUserDao.getInstance(mHomeView.getContext()).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId());
                    mNodeId = mDefaultDevice.getDeviceNodeId();
                    Log.d(TAG, "mDefaultUser = " + mDefaultDevice.getUserId());
                    mLockAdapter = new LockManagerAdapter(mHomeView.getContext(), mMyGridView, mDefaultUser.getUserPermission());
                    mDefaultStatus = DeviceStatusDao.getInstance(mHomeView.getContext()).queryOrCreateByNodeId(mNodeId);
                    mMyGridView.setAdapter(mLockAdapter);
                    mAdapter.setImageIds(mImageIds);
                    mAdapter.notifyDataSetChanged();
                }
                break;
            case UNBIND_DEVICE:
                mNewsVpRL.getLayoutParams().height = (int) (mHeight * 0.61);
                mAddLockLl.setVisibility(View.VISIBLE);
                mLockManagerRl.setVisibility(View.GONE);
                mAdapter.setImageIds(mAdapter.imageIds);
                mAdapter.notifyDataSetChanged();
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
        mUpdateTimeTv.setText(DateTimeUtil.timeStamp2Date(String.valueOf(System.currentTimeMillis() / 1000), "MM-dd HH:mm"));
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

    private void checkUserId(ArrayList<Short> userIds) {
        ArrayList<DeviceUser> users = DeviceUserDao.getInstance(mActivity).queryDeviceUsers(mDefaultDevice.getDeviceNodeId());
        if (!userIds.isEmpty()) {
            for (DeviceUser user : users) {
                if (userIds.contains(user.getUserId())) {
                    DeviceUserDao.getInstance(mActivity).delete(user);
                    userIds.remove((Short) user.getUserId());
                }
            }
            for (Short userId : userIds) {
                if (userId > 0 && userId <= 100) { //管理员
                    createDeviceUser(userId, null, ConstantUtil.DEVICE_MASTER);
                } else if (userId > 200 && userId <= 300) {
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
                if (mBleManagerHelper.getServiceConnection() && mDefaultDevice != null) {
                    mStartTime = System.currentTimeMillis();
                    mBleManagerHelper.getBleCardService().sendCmd25(mDefaultDevice.getUserId());
                }
                mBattery = intent.getByteExtra(BleMsg.KEY_BAT_PERSCENT, (byte) 0);
                int userStatus = intent.getByteExtra(BleMsg.KEY_USER_STATUS, (byte) 0);
                int stStatus = intent.getByteExtra(BleMsg.KEY_SETTING_STATUS, (byte) 0);
                int unLockTime = intent.getByteExtra(BleMsg.KEY_UNLOCK_TIME, (byte) 0);
                byte[] syncUsers = intent.getByteArrayExtra(BleMsg.KEY_SYNC_USERS);
                byte[] userState = intent.getByteArrayExtra(BleMsg.KEY_USERS_STATE);
                byte[] tempSecret = intent.getByteArrayExtra(BleMsg.KEY_TMP_PWD_SK);

                LogUtil.d(TAG, "battery = " + mBattery + "\n" + "userStatus = " + userStatus + "\n" + " stStatus = " + stStatus + "\n" + " unLockTime = " + unLockTime);
                LogUtil.d(TAG, "syncUsers = " + Arrays.toString(syncUsers));
                LogUtil.d(TAG, "userState = " + Arrays.toString(userState));
                LogUtil.d(TAG, "tempSecret = " + Arrays.toString(tempSecret));
                byte[] buf = new byte[4];
                System.arraycopy(syncUsers, 0, buf, 0, 4);
                long status1 = Long.parseLong(StringUtil.bytesToHexString(buf), 16);
                LogUtil.d(TAG, "status1 = " + status1);

                System.arraycopy(syncUsers, 4, buf, 0, 4);
                long status2 = Long.parseLong(StringUtil.bytesToHexString(buf), 16);
                LogUtil.d(TAG, "status2 = " + status2);

                System.arraycopy(syncUsers, 8, buf, 0, 4);
                long status3 = Long.parseLong(StringUtil.bytesToHexString(buf), 16);
                LogUtil.d(TAG, "status3 = " + status3);

                System.arraycopy(syncUsers, 12, buf, 0, 4);
                long status4 = Long.parseLong(StringUtil.bytesToHexString(buf), 16);
                LogUtil.d(TAG, "status4 = " + status4);

                if (mDefaultDevice != null) {
                    checkUserId(DeviceUserDao.getInstance(mActivity).checkUserStatus(status1, mDefaultDevice.getDeviceNodeId(), 1)); //第一字节状态字
                    checkUserId(DeviceUserDao.getInstance(mActivity).checkUserStatus(status2, mDefaultDevice.getDeviceNodeId(), 2));//第二字节状态字
                    checkUserId(DeviceUserDao.getInstance(mActivity).checkUserStatus(status3, mDefaultDevice.getDeviceNodeId(), 3));//第三字节状态字
                    checkUserId(DeviceUserDao.getInstance(mActivity).checkUserStatus(status4, mDefaultDevice.getDeviceNodeId(), 4));//第四字节状态字
                    DeviceUserDao.getInstance(mActivity).checkUserState(mDefaultDevice.getDeviceNodeId(), userState); //开锁信息状态字

                    mDefaultDevice.setTempSecret(StringUtil.bytesToHexString(tempSecret));
                    DeviceInfoDao.getInstance(mHomeView.getContext()).updateDeviceInfo(mDefaultDevice);
                }
                if (mDefaultStatus != null) {
                    switch (stStatus) {
                        case 0:
                            mDefaultStatus.setVoicePrompt(false);
                            mDefaultStatus.setNormallyOpen(false);
                            break;
                        case 1:
                            mDefaultStatus.setVoicePrompt(false);
                            mDefaultStatus.setNormallyOpen(true);
                            break;
                        case 2:
                            mDefaultStatus.setVoicePrompt(true);
                            mDefaultStatus.setNormallyOpen(false);
                            break;
                        case 3:
                            mDefaultStatus.setVoicePrompt(true);
                            mDefaultStatus.setNormallyOpen(true);
                            break;
                        default:
                            break;
                    }
                    mDefaultStatus.setRolledBackTime(unLockTime);
                    DeviceStatusDao.getInstance(mHomeView.getContext()).updateDeviceStatus(mDefaultStatus);
                }

                mIsConnected = true;
                refreshView(BIND_DEVICE);
                refreshBattery(mBattery);

                if (mOpenTest) {
                    new Handler().postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            mBleManagerHelper.getBleCardService().disconnect();
                        }
                    }, 5000);
                }

            }

            if (action.equals(BleMsg.STR_RSP_MSG26_USERINFO)) {
                mEndTime = System.currentTimeMillis();
                LogUtil.d(TAG, "mStartTime - mEndTime = " + (mEndTime - mStartTime));
                short userId = (short) intent.getSerializableExtra(BleMsg.KEY_SERIALIZABLE);
                if (userId == mDefaultDevice.getUserId()) {
                    byte[] userInfo = intent.getByteArrayExtra(BleMsg.KEY_USER_MSG);
                    LogUtil.d(TAG, "userInfo = " + Arrays.toString(userInfo));
                    mDefaultUser.setUserStatus(userInfo[0]);
                    DeviceUserDao.getInstance(mHomeView.getContext()).updateDeviceUser(mDefaultUser);

                    DeviceKeyDao.getInstance(mHomeView.getContext()).checkDeviceKey(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId(), userInfo[1], ConstantUtil.USER_PWD, "1");
                    DeviceKeyDao.getInstance(mHomeView.getContext()).checkDeviceKey(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId(), userInfo[2], ConstantUtil.USER_NFC, "1");
                    DeviceKeyDao.getInstance(mHomeView.getContext()).checkDeviceKey(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId(), userInfo[3], ConstantUtil.USER_FINGERPRINT, "1");
                    DeviceKeyDao.getInstance(mHomeView.getContext()).checkDeviceKey(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId(), userInfo[4], ConstantUtil.USER_FINGERPRINT, "2");
                    DeviceKeyDao.getInstance(mHomeView.getContext()).checkDeviceKey(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId(), userInfo[5], ConstantUtil.USER_FINGERPRINT, "3");
                    DeviceKeyDao.getInstance(mHomeView.getContext()).checkDeviceKey(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId(), userInfo[6], ConstantUtil.USER_FINGERPRINT, "4");
                    DeviceKeyDao.getInstance(mHomeView.getContext()).checkDeviceKey(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId(), userInfo[7], ConstantUtil.USER_FINGERPRINT, "5");

                    mDefaultDevice.setMixUnlock(userInfo[8]);
                    DeviceInfoDao.getInstance(mHomeView.getContext()).updateDeviceInfo(mDefaultDevice);
                }

            }

            if (action.equals(BleMsg.ACTION_GATT_DISCONNECTED)) {
                mIsConnected = false;
                refreshView(BIND_DEVICE);
            }

            if (action.equals(BleMsg.STR_RSP_SET_TIMEOUT)) {
                if (mDefaultDevice != null) {
                    refreshView(BIND_DEVICE);
                } else refreshView(UNBIND_DEVICE);


            }

            if (action.equals(BleMsg.STR_RSP_OPEN_TEST)) {
                setTestMode(intent.getBooleanExtra(ConstantUtil.OPEN_TEST, false));
            }

            if (action.equals(BleMsg.STR_RSP_MSG2E_ERRCODE)) {
                final byte[] errCode = intent.getByteArrayExtra(BleMsg.KEY_ERROR_CODE);
                Log.d(TAG, "errCode[3] = " + errCode[3]);
                if (errCode[3] == 0x00) {
                    showMessage(getString(R.string.remote_unlock_success));
                }
                mHandler.removeCallbacks(mRunnable);
                DialogUtils.closeDialog(mLoadDialog);
            }
        }
    };


    protected static IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.STR_RSP_SECURE_CONNECTION);
        intentFilter.addAction(BleMsg.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleMsg.STR_RSP_SET_TIMEOUT);
        intentFilter.addAction(BleMsg.STR_RSP_OPEN_TEST);
        intentFilter.addAction(BleMsg.STR_RSP_MSG26_USERINFO);
        intentFilter.addAction(BleMsg.STR_RSP_MSG2E_ERRCODE);
        return intentFilter;
    }

    public void onResume() {
        super.onResume();
        mDefaultDevice = DeviceInfoDao.getInstance(mHomeView.getContext()).queryFirstData("device_default", true);
        if (mDefaultDevice == null) {
            refreshView(UNBIND_DEVICE);
            return;
        }
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mIsConnected = mBleManagerHelper.getServiceConnection();
        if (!mIsConnected) {
            LogUtil.d(TAG, "ble get Service connection() : " + mIsConnected);
            setSk();
            refreshView(DEVICE_CONNECTING);
            mBleManagerHelper.connectBle((byte) 1, mDefaultDevice.getUserId(), mDefaultDevice.getBleMac());
            mDefaultStatus = DeviceStatusDao.getInstance(mHomeView.getContext()).queryOrCreateByNodeId(mDefaultDevice.getDeviceNodeId());
        }
    }

    public void onPause() {
        super.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG, "onDestroy");
        try {
            LocalBroadcastManager.getInstance(mHomeView.getContext()).unregisterReceiver(deviceReciver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        mIsConnected = false;

    }


    @Override
    public void onClick(View v) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);
        switch (v.getId()) {
            case R.id.btn_add_lock:
//                startIntent(AddDeviceActivity.class, null);
                scanQr();
                break;
            case R.id.iv_scan_qr:
                scanQr();
                break;
            case R.id.iv_connect:
                refreshView(DEVICE_CONNECTING);
                setSk();
                mBleManagerHelper.connectBle((byte) 1, mDefaultDevice.getUserId(), mDefaultDevice.getBleMac());
                break;
            case R.id.ll_setting:
                if (mIsConnected) {
                    startIntent(LockSettingActivity.class, bundle);
                } else {
                    showMessage(mHomeView.getContext().getString(R.string.unconnected_device));
                }
                break;
            case R.id.one_click_unlock_ib:

                if (mNodeId.getBytes().length == 15) {
                    mNodeId = "0" + mNodeId;
                }
                LogUtil.d(TAG, "nodeId = " + mNodeId);

                byte[] nodeId = StringUtil.hexStringToBytes(mNodeId);

                StringUtil.exchange(nodeId);

                if (mBleManagerHelper.getServiceConnection()) {
                    mBleManagerHelper.getBleCardService().sendCmd21(nodeId);
                } else
                    showMessage("未连接");
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

        byte[] nodeId = StringUtil.hexStringToBytes(defaultNodeId);

        StringUtil.exchange(nodeId);

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
        LogUtil.d(TAG, "mDefaultDevice = " + mDefaultDevice.toString());
        switch (((Integer) view.getTag()).intValue()) {
            case R.mipmap.icon_password:
                if (mIsConnected) {
                    bundle.putInt(BleMsg.KEY_CURRENT_ITEM, 0);
                    startIntent(DeviceKeyActivity.class, bundle);
                } else
                    showMessage(mHomeView.getContext().getString(R.string.unconnected_device));
                break;
            case R.mipmap.icon_nfc:
                if (mIsConnected) {
                    bundle.putInt(BleMsg.KEY_CURRENT_ITEM, 2);
                    startIntent(DeviceKeyActivity.class, bundle);
                } else
                    showMessage(mHomeView.getContext().getString(R.string.unconnected_device));
                break;
            case R.mipmap.icon_fingerprint:
                if (mIsConnected) {
                    bundle.putInt(BleMsg.KEY_CURRENT_ITEM, 1);
                    startIntent(DeviceKeyActivity.class, bundle);
                } else
                    showMessage(mHomeView.getContext().getString(R.string.unconnected_device));
                break;
            case R.mipmap.icon_events:
                if (mIsConnected)
                    startIntent(EventsActivity.class, bundle);
                else
                    showMessage(mHomeView.getContext().getString(R.string.unconnected_device));
                break;
            case R.mipmap.icon_userguanl:
                if (mIsConnected) {
                    if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        startIntent(UserManagerActivity.class, bundle);
                    } else {
                        ActivityCompat.requestPermissions(mActivity, mPermission, REQUESTCODE);
                    }
                } else
                    showMessage(mHomeView.getContext().getString(R.string.unconnected_device));
                break;
            case R.mipmap.icon_temporarypassword:
                startIntent(TempPwdActivity.class, bundle);
                break;
            default:
                break;

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
        AlertDialog.Builder builder = new AlertDialog.Builder(mHomeView.getContext());
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
                intent.setData(Uri.parse("package:" + mActivity.getPackageName())); // 根据包名打开对应的设置界面
                startActivity(intent);
            }
        });
        builder.create().show();
    }

}
