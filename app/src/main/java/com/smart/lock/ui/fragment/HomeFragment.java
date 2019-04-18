
package com.smart.lock.ui.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceStatus;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceStatusDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.entity.BleConnectModel;
import com.smart.lock.ui.DeviceKeyActivity;
import com.smart.lock.ui.EventsActivity;
import com.smart.lock.ui.LockSettingActivity;
import com.smart.lock.ui.TempPwdActivity;
import com.smart.lock.ui.UserManagerActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.widget.MyGridView;

import java.util.ArrayList;
import java.util.Objects;


public class HomeFragment extends BaseFragment implements
        View.OnClickListener,
        AdapterView.OnItemClickListener {
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

    private int mImageIds[]; //主界面图片
    private boolean mIsConnected = false; //服务连接标志
    private BleConnectModel mBleModel;

    public void onAuthenticationSuccess() {
        refreshView(BIND_DEVICE);
    }

    public void onAuthenticationFailed() {
        refreshView(UNBIND_DEVICE);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        mInstructionBtn = mActivity.findViewById(R.id.one_click_unlock_ib);
        initEvent();
        return mHomeView;
    }

    public void setTestMode(boolean openTest) {
        mOpenTest = openTest;
        if (mOpenTest) {
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    if (mDefaultDevice != null && !mBleManagerHelper.getServiceConnection()) {
                        MessageCreator.setSk(mDefaultDevice);
                        Bundle bundle = new Bundle();
                        bundle.putShort(BleMsg.KEY_USER_ID, mDefaultUser.getUserId());
                        bundle.putString(BleMsg.KEY_BLE_MAC, mDefaultDevice.getBleMac());
                        mBleManagerHelper.connectBle((byte) 1, bundle, mHomeView.getContext());
                    } else
                        mBleManagerHelper.getBleCardService().disconnect();
                }
            }, 2000);
        }
    }

    private void initEvent() {
        mAddLockBt.setOnClickListener((View.OnClickListener) mActivity);
        mLockSettingLl.setOnClickListener(this);
        mMyGridView.setOnItemClickListener(this);
        mBleConnectIv.setOnClickListener(this);
        mInstructionBtn.setOnClickListener(this);
        mScanQrIv.setOnClickListener((View.OnClickListener) mActivity);
    }

    public void initDate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {  //版本检测
            mToolbar = mHomeView.findViewById(R.id.tb_toolbar);
            ((AppCompatActivity) Objects.requireNonNull(getActivity())).setSupportActionBar(mToolbar);  //将ToolBar设置成ActionBar
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
        mDefaultDevice = DeviceInfoDao.getInstance(mHomeView.getContext()).queryFirstData("device_default", true);
        if (mDefaultDevice == null) {
            refreshView(UNBIND_DEVICE);
        } else {
            refreshView(BIND_DEVICE);
        }
    }


    /**
     * 刷新显示界面
     */
    private void refreshView(int status) {
        switch (status) {
            case DEVICE_CONNECTING:
                LogUtil.d(TAG, "DEVICE_CONNECTING");
                mNewsVpRL.getLayoutParams().height = (int) getResources().getDimension(R.dimen.y366dp);
                mAddLockLl.setVisibility(View.GONE);
                mLockManagerRl.setVisibility(View.VISIBLE);
                mLockStatusTv.setText(R.string.bt_connecting);
                mBleConnectIv.setClickable(false);
                mBleConnectIv.setImageResource(R.mipmap.icon_bluetooth_nor);
                refreshView(BATTER_UNKNOW);
                if (mDefaultDevice != null) {
                    mDefaultUser = DeviceUserDao.getInstance(mHomeView.getContext()).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId());
                    mNodeId = mDefaultDevice.getDeviceNodeId();
                    mLockAdapter = new LockManagerAdapter(mHomeView.getContext(), mMyGridView, mDefaultUser.getUserPermission());
                    mDefaultStatus = DeviceStatusDao.getInstance(mHomeView.getContext()).queryOrCreateByNodeId(mNodeId);
                    mMyGridView.setAdapter(mLockAdapter);
                    mAdapter.setImageIds(mImageIds);
                    mAdapter.notifyDataSetChanged();
                }
                break;
            case BIND_DEVICE:
                mNewsVpRL.getLayoutParams().height = (int) getResources().getDimension(R.dimen.y366dp);
                mAddLockLl.setVisibility(View.GONE);
                mLockManagerRl.setVisibility(View.VISIBLE);
                LogUtil.d(TAG, "mIsConnected = " + mIsConnected);

                if (mIsConnected) {
                    mLockStatusTv.setText(R.string.bt_connect_success);
                    mBleConnectIv.setClickable(false);
                    mBleConnectIv.setImageResource(R.mipmap.icon_bluetooth_nor);
                    if (mDefaultDevice != null)
                        mLockNameTv.setText(mDefaultDevice.getDeviceName());
                    mBleModel = BleConnectModel.getInstance(mHomeView.getContext());
                    mBattery = mBleModel.getBattery();
                    refreshBattery(mBattery);
                } else {
                    mLockStatusTv.setText(R.string.bt_unconnected);
                    mBleConnectIv.setClickable(true);
                    mBleConnectIv.setImageResource(R.mipmap.icon_bluetooth);
                    refreshView(BATTER_UNKNOW);
                }
                if (mDefaultDevice == null) {
                    mDefaultDevice = DeviceInfoDao.getInstance(mHomeView.getContext()).queryFirstData("device_default", true);
                }
                if (mDefaultDevice != null) {
                    mDefaultUser = DeviceUserDao.getInstance(mHomeView.getContext()).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId());
                }
                LogUtil.d(TAG, "Default = " + mDefaultDevice + "\n" +
                        "Default user = " + mDefaultUser);
                if (mDefaultDevice != null && mDefaultUser != null) {
                    mNodeId = mDefaultDevice.getDeviceNodeId();
                    Log.d(TAG, "mDefaultUser = " + mDefaultUser.toString());
                    mLockAdapter = new LockManagerAdapter(mHomeView.getContext(), mMyGridView, mDefaultUser.getUserPermission());
                    mDefaultStatus = DeviceStatusDao.getInstance(mHomeView.getContext()).queryOrCreateByNodeId(mNodeId);
                    mMyGridView.setAdapter(mLockAdapter);
                    mAdapter.setImageIds(mImageIds);
                    mAdapter.notifyDataSetChanged();
                    LogUtil.d(TAG, "默认设备");
                }
                break;
            case UNBIND_DEVICE:
                mNewsVpRL.getLayoutParams().height = (int) getResources().getDimension(R.dimen.y536dp);
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
    @SuppressLint("SetTextI18n")
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


    /**
     * 广播接收
     */
    private final BroadcastReceiver deviceReciver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null)
                return;
            // 4.2.3 MSG 04
            if (action.equals(BleMsg.STR_RSP_SECURE_CONNECTION)) {
                BleConnectModel mBleModel = BleConnectModel.getInstance(mHomeView.getContext());
                mBattery = mBleModel.getBattery();

                mIsConnected = true;
                refreshView(BIND_DEVICE);

                if (mOpenTest) {
                    new Handler().postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            mBleManagerHelper.getBleCardService().disconnect();
                        }
                    }, 5000);
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
        LogUtil.d(TAG, "default ble : " + mDefaultDevice.getBleMac());
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mIsConnected = mBleManagerHelper.getServiceConnection();
        if (!mIsConnected) {
            LogUtil.d(TAG, "ble get Service connection() : " + mIsConnected);
            MessageCreator.setSk(mDefaultDevice);
            refreshView(DEVICE_CONNECTING);
            Bundle bundle = new Bundle();
            bundle.putShort(BleMsg.KEY_USER_ID, mDefaultUser.getUserId());
            bundle.putString(BleMsg.KEY_BLE_MAC, mDefaultDevice.getBleMac());
            mBleManagerHelper.connectBle((byte) 1, bundle, mHomeView.getContext());
            mDefaultStatus = DeviceStatusDao.getInstance(mHomeView.getContext()).queryOrCreateByNodeId(mDefaultDevice.getDeviceNodeId());
        } else
            refreshView(BIND_DEVICE);
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
//                mScanQRHelper.scanQr();
                break;
            case R.id.iv_scan_qr:
//                mScanQRHelper.scanQr();
                break;
            case R.id.iv_connect:
                refreshView(DEVICE_CONNECTING);
                MessageCreator.setSk(mDefaultDevice);
                Bundle dev = new Bundle();
                dev.putShort(BleMsg.KEY_USER_ID, mDefaultUser.getUserId());
                dev.putString(BleMsg.KEY_BLE_MAC, mDefaultDevice.getBleMac());
                LogUtil.d(TAG, "dev = " + dev.toString());
                mBleManagerHelper.connectBle((byte) 1, dev, mHomeView.getContext());
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


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);
        switch ((Integer) view.getTag()) {
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
        LogUtil.d(TAG, "requestCode = " + requestCode);
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
