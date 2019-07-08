
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smart.lock.MainActivity;
import com.smart.lock.R;
import com.smart.lock.adapter.LockManagerAdapter;
import com.smart.lock.adapter.ServerPagerAdapter;
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
import com.smart.lock.scan.ScanQRResultInterface;
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
import com.smart.lock.widget.NoScrollViewPager;
import com.smart.lock.widget.TimePickerWithDateDefineDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;


public class HomeFragment extends BaseFragment implements
        View.OnClickListener, ViewPager.OnPageChangeListener{
    private static final String TAG = HomeFragment.class.getSimpleName();
    private Toolbar mToolbar;
    private View mHomeView;
    private ViewPager mViewPager;
    private LinearLayout mAddLockLl;
    private RelativeLayout mNewsVpRL;
    private Button mAddLockBt;
    private ImageView mInstructionBtn;
    private ImageView mScanQrIv;
    private List<BaseFragment> mServerPagerList;
    private NoScrollViewPager mServerPager;
    private ServerPagerAdapter mServerAdapter;

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


    private boolean mOpenTest = false; // 测试连接的开关

    private int mBattery = 0;

    private int mImageIds[]; //主界面图片
    private int mImageIdsNor[];
    private Device mDevice;
    private Context mCtx;

    private ArrayList<DeviceInfo> deviceInfoArraysList;

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
        mAddLockLl = mHomeView.findViewById(R.id.rl_add_lock);
        mScanQrIv = mHomeView.findViewById(R.id.iv_scan_qr);
        mAddLockBt = mHomeView.findViewById(R.id.btn_add_lock);
        mInstructionBtn = mActivity.findViewById(R.id.one_click_unlock_ib);
        mServerPager = mHomeView.findViewById(R.id.server_viewpager);
        initEvent();
        return mHomeView;
    }

    @SuppressLint("UseSparseArrays")
    private void initEvent() {
        mAddLockBt.setOnClickListener((View.OnClickListener) mActivity);
        mInstructionBtn.setOnClickListener(this);
        mScanQrIv.setOnClickListener((View.OnClickListener) mActivity);
        mServerPagerList = new ArrayList<>();

        setViewPager();
    }

    void setViewPager() {
        deviceInfoArraysList = DeviceInfoDao.getInstance(mActivity).queryAll();
        mServerAdapter = new ServerPagerAdapter(this.getChildFragmentManager(), mActivity, deviceInfoArraysList);
        mServerPager.setAdapter(mServerAdapter);
        mServerPager.setCurrentItem(mServerAdapter.getCurrentItem());
        mServerPager.addOnPageChangeListener(this);
        LogUtil.d(TAG, "INDEX ServerPage=" + mServerPager.getChildCount());

    }

    @SuppressLint("HandlerLeak")
    @Override
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
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

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
        mDefaultDevice = DeviceInfoDao.getInstance(mCtx).queryFirstData("device_default", true);
        mDevice = Device.getInstance(mCtx);
        if (mDefaultDevice == null) {
            refreshView(UNBIND_DEVICE);
        } else {
            refreshView(BIND_DEVICE);
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

            case BIND_DEVICE:
                LogUtil.d(TAG, "onResume bind");
                if (mNewsVpRL.getLayoutParams() != null)
                    mNewsVpRL.getLayoutParams().height = (int) mCtx.getResources().getDimension(R.dimen.y366dp);
                mAdapter.setImageIds(mImageIdsNor);
                mAdapter.notifyDataSetChanged();
                mAddLockLl.setVisibility(View.GONE);
                mServerPager.setVisibility(View.VISIBLE);
                deviceInfoArraysList = DeviceInfoDao.getInstance(mActivity).queryAll();
                if (deviceInfoArraysList.size() != mServerAdapter.getCount()) {
                    mServerAdapter.updateDevices(mActivity, deviceInfoArraysList);
                    mServerAdapter.notifyDataSetChanged();
                    mServerPager.setCurrentItem(mServerAdapter.getCurrentItem());
                }
                break;
            case UNBIND_DEVICE:
                LogUtil.d(TAG, "onResume Unbind");
                if (mNewsVpRL.getLayoutParams() != null)
                    mNewsVpRL.getLayoutParams().height = (int) getResources().getDimension(R.dimen.y536dp);
                mAddLockLl.setVisibility(View.VISIBLE);
                mServerPager.setVisibility(View.GONE);
                mInstructionBtn.setVisibility(View.GONE);
                mAdapter.setImageIds(mImageIds);
                mAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }


    public void onResume() {
        super.onResume();
//        updateViewPage();
        ArrayList<DeviceInfo> newDeviceInfo = DeviceInfoDao.getInstance(mCtx).queryAll();

        if (newDeviceInfo.size() == 0) {
            mDevice = null;
            refreshView(UNBIND_DEVICE);
            LogUtil.d(TAG, "onResume");
        } else {
            refreshView(BIND_DEVICE);
        }
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

    @Override
    public void onPageScrolled(int i, float v, int i1) {

    }

    @Override
    public void onPageSelected(int i) {
        DeviceInfoDao.getInstance(mActivity).setNoDefaultDev();
        mBleManagerHelper.getBleCardService().disconnect();
        deviceInfoArraysList = DeviceInfoDao.getInstance(mActivity).queryAll();
        deviceInfoArraysList.get(i).setDeviceDefault(true);
        Device.getInstance(mActivity).halt();
        DeviceInfoDao.getInstance(mActivity).updateDeviceInfo(deviceInfoArraysList.get(i));

    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

//    @Override
    public void onSelectDev(DeviceInfo deviceInfo) {
        LogUtil.d(TAG,"setCurrentItem");
        deviceInfoArraysList = DeviceInfoDao.getInstance(mActivity).queryAll();
        for (int i = 0; i < deviceInfoArraysList.size(); i++) {
            if (deviceInfo.getId() == deviceInfoArraysList.get(i).getId()) {
                mServerAdapter.notifyDataSetChanged();
                mServerPager.setCurrentItem(i);
                break;
            }
        }
    }
}
