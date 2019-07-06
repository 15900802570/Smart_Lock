
package com.smart.lock.ui.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.text.InputFilter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smart.lock.MainActivity;
import com.smart.lock.R;
import com.smart.lock.adapter.LockManagerAdapter;
import com.smart.lock.adapter.ViewPagerAdapter;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.listener.DeviceListener;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceStatus;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceStatusDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.db.helper.DtComFunHelper;
import com.smart.lock.entity.Device;
import com.smart.lock.ui.DeviceKeyActivity;
import com.smart.lock.ui.EventsActivity;
import com.smart.lock.ui.LockSettingActivity;
import com.smart.lock.ui.TempPwdActivity;
import com.smart.lock.ui.UserManagerActivity;
import com.smart.lock.ui.UserManagerActivity2;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.MyGridView;
import com.smart.lock.widget.TimePickerWithDateDefineDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.ListIterator;
import java.util.Objects;


public class HomeFragment extends BaseFragment implements
        View.OnClickListener,
        AdapterView.OnItemClickListener, UiListener, DeviceListener {
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
    private LinearLayout mDevStatusLl;

    private ViewPagerAdapter mAdapter; //news adapter
    private LockManagerAdapter mLockAdapter; //gridView adapter
    private DeviceInfo mDefaultDevice; //默认设备
    private DeviceUser mDefaultUser; //默认用户
    private DeviceStatus mDefaultStatus; //用户状态
    private ArrayList<DeviceInfo> mDeviceInfos; //设备集合
    private ArrayList<View> mDots; //spot list

    private int mOldPosition = 0;// 记录上一次点的位置

    public static final int BIND_DEVICE = 0; //用户已添加设备
    public static final int UNBIND_DEVICE = 1;//未添加设备
    public static final int DEVICE_CONNECTING = 2;//添加设备中
    public static final int OPEN_LOCK_SUCESS = 3;//打开门锁成功


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

    private int mImageIds[]; //主界面图片
    private int mImageIdsNor[];
    private Device mDevice;
    private Context mCtx;

    private int mAuthErrorCounter = 0;

    private boolean mIsLockBack = false;

    public void onAuthenticationSuccess() {
        refreshView(BIND_DEVICE);
    }

    public void onAuthenticationFailed() {
        refreshView(UNBIND_DEVICE);
    }

    private Runnable mRunnable = new Runnable() {
        public void run() {
//            mInstructionBtn.setEnabled(true);
            mIsLockBack = false;
        }
    };

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
        mDevStatusLl = mHomeView.findViewById(R.id.ll_status);
        initEvent();
        return mHomeView;
    }

    public void setTestMode(boolean openTest) {
        mOpenTest = openTest;
        if (mOpenTest) {
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    if (mDefaultDevice != null && mDevice.getState() != Device.BLE_CONNECTED) {
                        MessageCreator.setSk(mDefaultDevice);
                        Bundle bundle = new Bundle();
                        bundle.putShort(BleMsg.KEY_USER_ID, mDefaultUser.getUserId());
                        bundle.putString(BleMsg.KEY_BLE_MAC, mDefaultDevice.getBleMac());
                        mBleManagerHelper.connectBle(Device.BLE_OTHER_CONNECT_TYPE, bundle, mCtx);
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
        mDevStatusLl.setOnClickListener(this);
        mLockNameTv.setOnClickListener(this);
        mScanQrIv.setOnClickListener((View.OnClickListener) mActivity);
    }

    @SuppressLint("HandlerLeak")
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

        mImageIds = new int[]{
                R.mipmap.homepage_adv1,
                R.mipmap.homepage_adv5
        };
        mImageIdsNor = new int[]{
                R.mipmap.homepage_adv1_nor,
                R.mipmap.homepage_adv5_nor
        };

        mCtx = mHomeView.getContext();

        mAdapter = new ViewPagerAdapter(mCtx, mImageIds);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                // TODO Auto-generated method stub

                mDots.get(mOldPosition).setBackgroundResource(R.drawable.dot_focused);
                mDots.get(position).setBackgroundResource(R.drawable.dot_normal);

                mOldPosition = position;
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

        mBleManagerHelper = BleManagerHelper.getInstance(mCtx);
        mBleManagerHelper.addDeviceLintener(this);
        mBleManagerHelper.addUiListener(this);
        mDefaultDevice = DeviceInfoDao.getInstance(mCtx).queryFirstData("device_default", true);
        mDevice = Device.getInstance(mCtx);
        if (mDefaultDevice == null) {
            refreshView(UNBIND_DEVICE);
        } else {
            refreshView(BIND_DEVICE);
            refreshBattery(-1);
        }
        mHandler = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {

                switch (msg.what) {
                    case BIND_DEVICE:
                        refreshView(BIND_DEVICE);
                        break;
                    case UNBIND_DEVICE:
                        refreshView(UNBIND_DEVICE);
                        break;
                    case DEVICE_CONNECTING:
                        refreshView(DEVICE_CONNECTING);
                        break;
                    case OPEN_LOCK_SUCESS:
                        showMessage(getString(R.string.remote_unlock_success));
                        break;
                    case BleMsg.SCAN_DEV_FIALED:
                        if (mDevice != null && !mDevice.isDisconnectBle()) {
                            showMessage(getString(R.string.retry_connect));
                        }
                        if (mDefaultDevice != null) {
                            refreshView(BIND_DEVICE);
                        } else {
                            refreshView(UNBIND_DEVICE);
                        }
                        break;
                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        };
    }

    /**
     * 刷新显示界面
     */
    private void refreshView(int status) {
        switch (status) {
            case DEVICE_CONNECTING:
                if (mNewsVpRL.getLayoutParams() != null)
                    mNewsVpRL.getLayoutParams().height = (int) mCtx.getResources().getDimension(R.dimen.y366dp);
                mAddLockLl.setVisibility(View.GONE);
                mLockManagerRl.setVisibility(View.VISIBLE);
                mLockStatusTv.setText(R.string.bt_connecting);
                mBleConnectIv.setClickable(false);
                mBleConnectIv.setImageResource(R.mipmap.icon_bluetooth_nor);
//                refreshView(BATTER_UNKNOW);
                if (mDefaultDevice != null) {
                    mDefaultUser = DeviceUserDao.getInstance(mCtx).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId());
                    if (mDefaultUser != null) {
                        mNodeId = mDefaultDevice.getDeviceNodeId();
                        if (mDefaultUser.getUserPermission() == ConstantUtil.DEVICE_MASTER) {
                            mMyGridView.setNumColumns(3);
                        } else
                            mMyGridView.setNumColumns(2);
                        mLockAdapter = new LockManagerAdapter(mCtx, mMyGridView, mDefaultUser.getUserPermission());
                        mDefaultStatus = DeviceStatusDao.getInstance(mCtx).queryOrCreateByNodeId(mNodeId);
                        mMyGridView.setAdapter(mLockAdapter);
                        mAdapter.setImageIds(mImageIdsNor);
                        mAdapter.notifyDataSetChanged();
                    }
                }
                break;
            case BIND_DEVICE:
                if (mNewsVpRL.getLayoutParams() != null)
                    mNewsVpRL.getLayoutParams().height = (int) mCtx.getResources().getDimension(R.dimen.y366dp);

                mAddLockLl.setVisibility(View.GONE);
                mLockManagerRl.setVisibility(View.VISIBLE);

                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    mLockStatusTv.setText(R.string.bt_connect_success);
                    mBleConnectIv.setClickable(false);
                    mBleConnectIv.setImageResource(R.mipmap.icon_bluetooth_nor);
                    mBattery = mDevice.getBattery();
                    refreshBattery(mBattery);
                } else {
                    mLockStatusTv.setText(R.string.bt_unconnected);
                    mBleConnectIv.setClickable(true);
                    mBleConnectIv.setImageResource(R.mipmap.icon_bluetooth);
//                    refreshView(BATTER_UNKNOW);
                }
                if (mDefaultDevice == null) {
                    mDefaultDevice = DeviceInfoDao.getInstance(mCtx).queryFirstData("device_default", true);
                }
                if (mDefaultDevice != null) {
                    mDefaultUser = DeviceUserDao.getInstance(mCtx).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId());
                }
                if (mDefaultDevice != null && mDefaultUser != null) {
                    mNodeId = mDefaultDevice.getDeviceNodeId();
                    if (mDefaultUser.getUserPermission() == ConstantUtil.DEVICE_MASTER) {
                        mMyGridView.setNumColumns(3);
                    } else
                        mMyGridView.setNumColumns(2);
                    mLockAdapter = new LockManagerAdapter(mCtx, mMyGridView, mDefaultUser.getUserPermission());
                    mDefaultStatus = DeviceStatusDao.getInstance(mCtx).queryOrCreateByNodeId(mNodeId);
                    mMyGridView.setAdapter(mLockAdapter);
                    mAdapter.setImageIds(mImageIdsNor);
                    mLockNameTv.setText(mDefaultDevice.getDeviceName());
                    mAdapter.notifyDataSetChanged();
                }
                break;
            case UNBIND_DEVICE:
                if (mNewsVpRL.getLayoutParams() != null)
                    mNewsVpRL.getLayoutParams().height = (int) getResources().getDimension(R.dimen.y536dp);
                mAddLockLl.setVisibility(View.VISIBLE);
                mLockManagerRl.setVisibility(View.GONE);
                mInstructionBtn.setVisibility(View.GONE);
                mAdapter.setImageIds(mImageIds);
                mAdapter.notifyDataSetChanged();
                break;
            case BATTER_FULL:
                mEqIv.setBackgroundResource(R.mipmap.icon_battery_100);
                mEqTv.setText(R.string.battery_100);
                break;
            case BATTER_LOW:
                mEqIv.setBackgroundResource(R.mipmap.icon_battery_10);
                mEqTv.setText(R.string.battery_10);
                break;
            case BATTER_UNKNOW:
                mUpdateTimeTv.setVisibility(View.GONE);
                mShowTimeTv.setVisibility(View.GONE);
                if (mDefaultDevice != null)
                    mLockNameTv.setText(mDefaultDevice.getDeviceName());
                mEqIv.setBackgroundResource(R.mipmap.icon_battery_gray);
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
        if (StringUtil.checkNotNull(mNodeId)) {
            mDefaultStatus = DeviceStatusDao.getInstance(mCtx).queryOrCreateByNodeId(mNodeId);
        }
        long updateTime = System.currentTimeMillis() / 1000;
        if (mDefaultStatus != null && battery == -1) {
            battery = mDefaultStatus.getBattery();
            updateTime = mDefaultStatus.getUpdateTime();
        }
        mUpdateTimeTv.setVisibility(View.VISIBLE);
        mShowTimeTv.setVisibility(View.VISIBLE);
        mEqTv.setText(String.valueOf(battery) + "%");
        mUpdateTimeTv.setText(DateTimeUtil.timeStamp2Date(String.valueOf(updateTime), "MM-dd HH:mm"));
        switch (battery / 10) {
            case 0:
                mEqIv.setBackgroundResource(R.mipmap.icon_battery_10);
                break;
            case 1:
                mEqIv.setBackgroundResource(R.mipmap.icon_battery_10);
                break;
            case 2:
                mEqIv.setBackgroundResource(R.mipmap.icon_battery_20);
                break;
            case 3:
                mEqIv.setBackgroundResource(R.mipmap.icon_battery_30);
                break;
            case 4:
            case 5:
                mEqIv.setBackgroundResource(R.mipmap.icon_battery_50);
                break;
            case 6:
                mEqIv.setBackgroundResource(R.mipmap.icon_battery_60);
                break;
            case 7:
                mEqIv.setBackgroundResource(R.mipmap.icon_battery_70);
                break;
            case 8:
                mEqIv.setBackgroundResource(R.mipmap.icon_battery_80);
                break;
            case 9:
            case 10:
                mEqIv.setBackgroundResource(R.mipmap.icon_battery_100);
                break;
            default:
//                refreshView(BATTER_UNKNOW);
                break;
        }
    }

    public void onResume() {
        super.onResume();
        DeviceInfo newDeviceInfo = DeviceInfoDao.getInstance(mCtx).queryFirstData("device_default", true);

        if (newDeviceInfo == null) {
            mDevice = null;
            refreshView(UNBIND_DEVICE);
            return;
        }
        // 检测是否有切换默认用户，已MAC地址来判断，用于切换蓝牙连接
        if (mDefaultDevice == null || !newDeviceInfo.getBleMac().equals(mDefaultDevice.getBleMac())) {
            mDefaultDevice = newDeviceInfo;
        }
        mDefaultUser = DeviceUserDao.getInstance(mCtx).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId());
        if (mDefaultUser == null) {
            LogUtil.e(TAG, "mDefaultUser is null! ");
            return;
        }

        mLockNameTv.setText(mDefaultDevice.getDeviceName());
        mNodeId = mDefaultDevice.getDeviceNodeId();
        if (mDevice.getState() == Device.BLE_DISCONNECTED) {
            if (mDevice != null && mDevice.getUserStatus() == ConstantUtil.USER_PAUSE || mDevice.isDisconnectBle()) {
                return;
            }
            mDevice.setBackGroundConnect(false);
            MessageCreator.setSk(mDefaultDevice);
            refreshView(DEVICE_CONNECTING);
            Bundle bundle = new Bundle();
            bundle.putShort(BleMsg.KEY_USER_ID, mDefaultUser.getUserId());
            bundle.putString(BleMsg.KEY_BLE_MAC, mDefaultDevice.getBleMac());
            mBleManagerHelper.connectBle(Device.BLE_OTHER_CONNECT_TYPE, bundle, mCtx);
            mDefaultStatus = DeviceStatusDao.getInstance(mCtx).queryOrCreateByNodeId(mDefaultDevice.getDeviceNodeId());
        } else if (mDevice.getState() == Device.BLE_CONNECTION) {
            refreshView(DEVICE_CONNECTING);
        } else
            refreshView(BIND_DEVICE);
    }

    public void onPause() {
        super.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
        mBleManagerHelper.removeUiListener(this);
    }

    @Override
    public void onClick(View v) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);

        switch (v.getId()) {
            case R.id.ll_status:
                switch (mDevice.getState()) {
                    case Device.BLE_DISCONNECTED:
                        refreshView(DEVICE_CONNECTING);
                        MessageCreator.setSk(mDefaultDevice);
                        Bundle dev = new Bundle();
                        if (mDefaultUser != null) {
                            dev.putShort(BleMsg.KEY_USER_ID, mDefaultUser.getUserId());
                            dev.putString(BleMsg.KEY_BLE_MAC, mDefaultDevice.getBleMac());
                            mBleManagerHelper.connectBle(Device.BLE_OTHER_CONNECT_TYPE, dev, mCtx);
                            mDevice.setDisconnectBle(false);
                        }
                        break;
                    case Device.BLE_CONNECTED:
                        if (mBleManagerHelper.getBleCardService() != null) {
                            mDevice.setDisconnectBle(true);
                            mBleManagerHelper.getBleCardService().disconnect();
                        }
                        break;
                    case Device.BLE_CONNECTION:
                        if (mBleManagerHelper.getBleCardService() != null) {
                            mBleManagerHelper.getBleCardService().disconnect();
                            mBleManagerHelper.stopScan();
                            mDevice.setDisconnectBle(true);
                        }
//                        showMessage(getString(R.string.is_connecting));
                        break;
                }
                break;
            case R.id.ll_setting:
                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    bundle.putShort(BleMsg.KEY_USER_ID, mDefaultUser.getUserId());
                    startIntent(LockSettingActivity.class, bundle);
                } else if (mDevice.getState() == Device.BLE_CONNECTION) {
                    showMessage(mCtx.getString(R.string.bt_connecting));
                } else if (mDevice.getState() == Device.BLE_DISCONNECTED) {
                    showMessage(mCtx.getString(R.string.bt_unconnected));
                }
                break;
            case R.id.one_click_unlock_ib:
                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    if (!mIsLockBack) {
//                        mInstructionBtn.setEnabled(false);
                        if (mNodeId.getBytes().length == 15)
                            mNodeId = "0" + mNodeId;
                        byte[] nodeId = StringUtil.hexStringToBytes(mNodeId);

                        StringUtil.exchange(nodeId);
                        mBleManagerHelper.getBleCardService().sendCmd21(nodeId, BleMsg.INT_DEFAULT_TIMEOUT);

                        mIsLockBack = true;
                        mDefaultStatus = DeviceStatusDao.getInstance(mCtx).queryOrCreateByNodeId(mNodeId);
                        int unLockTime = mDefaultStatus.getRolledBackTime();
                        LogUtil.d(TAG, "unLockTime : " + unLockTime);
                        closeDialog(unLockTime);
                    } else showMessage(getString(R.string.rolled_back));
                } else if (mDevice.getState() == Device.BLE_CONNECTION)
                    showMessage(mCtx.getString(R.string.bt_connecting));
                else if (mDevice.getState() == Device.BLE_DISCONNECTED) {
                    showMessage(mCtx.getString(R.string.bt_unconnected));
                }
                break;
            case R.id.tv_lock_name:
                final Dialog modifyNameDialog = DialogUtils.createEditorDialog(mActivity, getString(R.string.modify_name), mDefaultDevice.getDeviceName());
                ((EditText) modifyNameDialog.findViewById(R.id.editor_et)).setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
                modifyNameDialog.findViewById(R.id.dialog_confirm_btn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String newName = ((EditText) modifyNameDialog.findViewById(R.id.editor_et)).getText().toString();
                        if (!newName.isEmpty()) {
                            mLockNameTv.setText(newName);
                            mDefaultDevice.setDeviceName(newName);
                            DeviceInfoDao.getInstance(mActivity).updateDeviceInfo(mDefaultDevice);
                        } else {
                            ToastUtil.showLong(mActivity, R.string.cannot_be_empty_str);
                        }
                        modifyNameDialog.dismiss();
                    }
                });
                modifyNameDialog.show();
                break;
            default:
                break;
        }

    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);
        if (mDevice.getState() == Device.BLE_DISCONNECTED && (Integer) view.getTag() != R.mipmap.icon_temporarypassword) {
            showMessage(mCtx.getString(R.string.unconnected_device));
            return;
        } else if (mDevice.getState() == Device.BLE_CONNECTION && (Integer) view.getTag() != R.mipmap.icon_temporarypassword) {
            showMessage(mCtx.getString(R.string.bt_connecting));
            return;
        }
        switch ((Integer) view.getTag()) {
            case R.mipmap.icon_password:
                bundle.putInt(BleMsg.KEY_CURRENT_ITEM, 0);
                startIntent(DeviceKeyActivity.class, bundle);
                break;
            case R.mipmap.icon_nfc:
                bundle.putInt(BleMsg.KEY_CURRENT_ITEM, 2);
                startIntent(DeviceKeyActivity.class, bundle);
                break;
            case R.mipmap.icon_fingerprint:
                bundle.putInt(BleMsg.KEY_CURRENT_ITEM, 1);
                startIntent(DeviceKeyActivity.class, bundle);
                break;
            case R.mipmap.icon_events:
                startIntent(EventsActivity.class, bundle);
                break;
            case R.mipmap.icon_userguanl:
                if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    startIntent(UserManagerActivity2.class, bundle);
                } else {
                    ActivityCompat.requestPermissions(mActivity, mExternalPermission, REQUESTCODE);
                }
                break;
            case R.mipmap.icon_temporarypassword:
                if (mDefaultUser.getUserStatus() == ConstantUtil.USER_PAUSE) { //用户被暂停
                    ToastUtil.showShort(mCtx, mCtx.getString(R.string.user_pause_contact_admin));
                } else {
                    startIntent(TempPwdActivity.class, bundle);
                }
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
//                    askForPermission();
                    Bundle bundle = new Bundle();
                    if (mDefaultDevice != null) {
                        bundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);
                        startIntent(UserManagerActivity2.class, bundle);
                    }

                }
            }

        }

    }

    private void askForPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
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

    /**
     * 0E消息分发
     *
     * @param errCode
     */
    private void dispatchOE(byte errCode) {
        LogUtil.i(TAG, "errCode : " + errCode);
        switch (errCode) {
            case BleMsg.TYPE_RAND_ERROR:
                showMessage(getString(R.string.random_error));
                break;
            case BleMsg.ERR0E_NO_AUTHORITY:
                mDevice.setState(Device.BLE_DISCONNECTED);
                mBleManagerHelper.getBleCardService().disconnect();
                showMessage(mCtx.getString(R.string.no_authority));
                break;
            case BleMsg.TYPE_DEVICE_BUSY:
                showMessage(getString(R.string.device_busy));
                break;
            // 鉴权码失败和用户不存在均视为用户已被删除
            case BleMsg.TYPE_USER_NOT_EXIST:
            case BleMsg.TYPE_AUTH_CODE_ERROR:
                userHadBeenDelete();
                break;
            default:
                break;
        }
    }

    /**
     * 1E消息分发
     *
     * @param errCode
     */
    private void dispatch1E(byte errCode) {
        LogUtil.i(TAG, "errCode : " + errCode);
        switch (errCode) {
            case BleMsg.TYPE_LONG_TIME_NO_DATA:
                switch (mDevice.getState()) {
                    case Device.BLE_CONNECTED:
                        if (mBleManagerHelper.getBleCardService() != null) {
                            mDevice.setBackGroundConnect(true);
                            mBleManagerHelper.getBleCardService().disconnect();
                        }
                        break;
                    case Device.BLE_CONNECTION:
                        if (mBleManagerHelper.getBleCardService() != null) {
                            mBleManagerHelper.getBleCardService().disconnect();
                            mBleManagerHelper.stopScan();
                            mDevice.setBackGroundConnect(true);
                        }
                        break;
                }
                break;
            default:
                break;
        }
    }

    /**
     * 2E消息分发
     *
     * @param errCode
     */
    private void dispatch2E(byte errCode) {
        LogUtil.i(TAG, "errCode : " + errCode);
        switch (errCode) {
            case BleMsg.TYPE_REMOTE_UNLOCK_SUCCESS:
                showMessage(getString(R.string.remote_unlock_success));
                break;
            default:
                break;
        }
    }

    private void userHadBeenDelete() {
        if (mBleManagerHelper.getBleCardService() != null && mDevice.getState() != Device.BLE_CONNECTED)
            mBleManagerHelper.getBleCardService().disconnect();
        if (mAuthErrorCounter++ == 1) {
            LogUtil.d(TAG, "用户已删除");
            mAuthErrorCounter = 0;
            // 删除相关数据
            if (mDefaultDevice != null) {
                DtComFunHelper.restoreFactorySettings(mActivity, mDefaultDevice);
                mDefaultDevice = null;
                mDefaultUser = null;
            }
            Device.getInstance(mCtx).halt();
            mBleManagerHelper.getBleCardService().disconnect();
            refreshView(UNBIND_DEVICE);
            DialogUtils.createTipsDialogWithCancel(mActivity, getString(R.string.the_user_delete_by_admin)).show();
        }
    }

    @Override
    public void deviceStateChange(Device device, int state) {
        mDevice = device;
        mDefaultDevice = DeviceInfoDao.getInstance(mCtx).queryFirstData("device_default", true);
        switch (state) {
            case BleMsg.STATE_DISCONNECTED:
                if (mDefaultDevice == null) {
                    sendMessage(UNBIND_DEVICE, null, 0);
                } else {
                    sendMessage(BIND_DEVICE, null, 0);
                }
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
    public void sendFailed(Message msg) {
        int exception = msg.getException();
        switch (exception) {
            case Message.EXCEPTION_TIMEOUT:
                DialogUtils.closeDialog(mLoadDialog);
                LogUtil.e(msg.getType() + " can't receiver msg!");
                break;
            case Message.EXCEPTION_SEND_FAIL:
                DialogUtils.closeDialog(mLoadDialog);
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
    public void dispatchUiCallback(Message msg, Device device, int type) {
        mDevice = device;
        if (mDevice != null && type == BleMsg.USER_PAUSE) {
            ToastUtil.showShort(mCtx, mCtx.getString(R.string.user_pause_contact_admin));
            if (mBleManagerHelper.getBleCardService() != null) {
                mBleManagerHelper.getBleCardService().disconnect();
            }
            return;
        }
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_0E:
                final byte[] errCode0E = msg.getData().getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode0E != null)
                    dispatchOE(errCode0E[3]);
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_1E:
                final byte[] errCode1E = msg.getData().getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode1E != null)
                    dispatch1E(errCode1E[3]);
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_2E:
                final byte[] errCode2E = msg.getData().getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode2E != null)
                    dispatch2E(errCode2E[3]);
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_04:
            case Message.TYPE_BLE_RECEIVER_CMD_26:
                LogUtil.i(TAG, "receiver 26!");
                mBattery = mDevice.getBattery(); //获取电池电量
                mDefaultDevice = DeviceInfoDao.getInstance(mCtx).queryFirstData("device_default", true);
                if (mDefaultDevice != null) {
                    sendMessage(BIND_DEVICE, null, 0);
                }
                break;
            default:
                break;

        }
    }

    @Override
    public void scanDevFailed() {
        LogUtil.i(TAG, "scanDevFiaed!");
        if (mDevice != null)
            mDevice.setState(Device.BLE_DISCONNECTED);
        sendMessage(BleMsg.SCAN_DEV_FIALED, null, 0);
    }

    @Override
    public void reConnectBle(Device device) {
        mDefaultDevice = DeviceInfoDao.getInstance(mCtx).queryFirstData("device_default", true);
        if (mDefaultDevice != null) {
            mDefaultUser = DeviceUserDao.getInstance(mCtx).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId());
        }
        if (mDefaultUser == null) return;
        mDevice = device;
        LogUtil.i(TAG, "reConnectBle!");

        sendMessage(DEVICE_CONNECTING, null, 0);
    }

    @Override
    public void deleteDeviceDev() {
        mDefaultDevice = DeviceInfoDao.getInstance(mCtx).queryFirstData("device_default", true);
        if (mDefaultDevice != null) {
            refreshView(BIND_DEVICE);
        } else {
            refreshView(UNBIND_DEVICE);
        }
    }

    /**
     * 超时提示框
     *
     * @param seconds 时间
     */
    private void closeDialog(int seconds) {
        mHandler.removeCallbacks(mRunnable);
        mHandler.postDelayed(mRunnable, seconds * 1000);
    }

    private void sendMessage(int type, Bundle bundle, long time) {
        android.os.Message msg = new android.os.Message();
        if (bundle != null) {
            msg.setData(bundle);
        }
        msg.what = type;
        if (time != 0) {
            mHandler.sendMessageDelayed(msg, time);
        } else mHandler.sendMessage(msg);

    }
}
