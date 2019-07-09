package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.swipe.SwipeLayout;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadMoreListener;
import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.entity.Device;
import com.smart.lock.ui.login.LockScreenActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.SharedPreferenceUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.SystemUtils;
import com.smart.lock.utils.ToastUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class LockDetectingActivity extends BaseActivity implements View.OnClickListener, UiListener {

    private static final String TAG = "LockDetectingActivity";

    private ImageView mSearchingIv;
    private ImageView mBackIv;
    private LinearLayout mRescanLl;
    private LinearLayout mTipsLl;
    private RelativeLayout mSearchingRl;
    private LinearLayout mAddLockSuccessLl;
    private Button mRescanBtn;
    private TextView mScanLockTv;
    private TextView mTitleTv;
    private Button mConfirmBtn;
    private EditText mRemarkEt;
    private LinearLayout mRefreshDevLl;
    private RecyclerView mDevList;
    private RelativeLayout mScanDev;
    private LinearLayout mScanEmpty;
    private ProgressBar mScanDevBar;
    private View mLine;

    private Animation mRotateAnimation;
    private static final long SCAN_PERIOD = 30000; //scanning for 30 seconds
    private boolean mScanning, mSearchAddDev = false; //搜索添加
    private BluetoothAdapter mBluetoothAdapter;//蓝牙适配器
    private BleManagerHelper mBleManagerHelper;

    private Dialog mLoadDialog;
    private DeviceInfo mDetectingDevice;
    private ArrayList<BluetoothDevice> deviceList;
    private BleAdapter mBleAdapter;

    private final int DETECTING_LOCK = 0x1;
    private final int SEARCH_LOCK = 0x2;
    private final int SET_DEV_INFO = 0x3;
    private int mMode = DETECTING_LOCK;
    private int REQUEST_ENABLE_BT = 100;
    private Device mDevice;
    private Context mCtx;
    private SmartRefreshLayout mRefreshLayout;

    //back time
    private long mBackPressedTime;
    /**
     * 连接方式 0-扫描二维码 1-普通安全连接,2-设置设备信息
     */
    private byte mConnectType = Device.BLE_CONNECT_TYPE;
    private Dialog mCancelDialog;

    private Runnable mStopScan = new Runnable() {
        @Override
        public void run() {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mSearchingIv.clearAnimation();
            mRescanLl.setVisibility(View.VISIBLE);
            mTipsLl.setVisibility(View.VISIBLE);
            mScanLockTv.setText(R.string.ble_scan_failed);
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {

            switch (msg.what) {
                case BleMsg.SCAN_DEV_FIALED:
                    mRescanLl.setVisibility(View.VISIBLE);
                    mTipsLl.setVisibility(View.VISIBLE);
                    mScanLockTv.setText(R.string.bt_connect_failed);
                    break;
                case BleMsg.REGISTER_SUCCESS:
                    DialogUtils.closeDialog(mLoadDialog);

                    if (mMode == SEARCH_LOCK && mSearchAddDev) {
                        mLoadDialog = DialogUtils.createLoadingDialog(mCtx, getString(R.string.plz_press_setting));
//                        mLoadDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
//
//                            @Override
//                            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
//                                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_BACK) {
//                                    LogUtil.d(TAG, "按了返回键");
//                                    mCancelDialog = DialogUtils.createTipsDialogWithConfirmAndCancel(mCtx, getString(R.string.cancel_warning) + getString(R.string.create_user));
//
//                                    mCancelDialog.findViewById(R.id.dialog_confirm_btn).setOnClickListener(new View.OnClickListener() {
//                                        @Override
//                                        public void onClick(View view) {
//                                            mBleManagerHelper.getBleCardService().cancelCmd(Message.TYPE_BLE_SEND_CMD_11 + "#" + "single");
//                                            mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_CANCEL_SCAN_QR, (short) 0, BleMsg.INT_DEFAULT_TIMEOUT);
//                                            mCancelDialog.cancel();
//                                        }
//                                    });
//                                    if (!mCancelDialog.isShowing()) {
//                                        mCancelDialog.show();
//                                    }
//
//                                    return true;
//                                }
//                                return false;
//                            }
//
//                        });
                        mLoadDialog.show();
                        mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_NO_SCAN_QR_ADD_USER, (short) 0, BleMsg.INT_DEFAULT_TIMEOUT);
                    } else if (mDevice != null && mDevice.getConnectType() == Device.BLE_SCAN_QR_CONNECT_TYPE) {
                        mLoadDialog = DialogUtils.createLoadingDialog(mCtx, getString(R.string.plz_press_key));
//                        mLoadDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
//
//                            @Override
//                            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
//                                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_BACK) {
//                                    LogUtil.d(TAG, "按了返回键");
//                                    mCancelDialog = DialogUtils.createTipsDialogWithConfirmAndCancel(mCtx, getString(R.string.cancel_warning) + getString(R.string.create_user));
//
//                                    mCancelDialog.findViewById(R.id.dialog_confirm_btn).setOnClickListener(new View.OnClickListener() {
//                                        @Override
//                                        public void onClick(View view) {
//                                            mBleManagerHelper.getBleCardService().cancelCmd(Message.TYPE_BLE_SEND_CMD_11 + "#" + "single");
//                                            mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_CANCEL_SCAN_QR, (short) 0, BleMsg.INT_DEFAULT_TIMEOUT);
//                                            mCancelDialog.cancel();
//                                        }
//                                    });
//                                    if (!mCancelDialog.isShowing()) {
//                                        mCancelDialog.show();
//                                    }
//
//                                    return true;
//                                }
//                                return false;
//                            }
//
//                        });
                        mLoadDialog.show();
                        mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPE_SCAN_QR_ADD_MASTER, (short) 0, BleMsg.INT_DEFAULT_TIMEOUT);
                    }
                    break;
                case BleMsg.USER_PAUSE:
                    if (mDevice != null && mDevice.getUserStatus() == ConstantUtil.USER_PAUSE) {
                        Dialog alterDialog = DialogUtils.createTipsDialogWithCancel(mCtx, getString(R.string.user_pause_contact_admin));
                        alterDialog.show();
                        if (mBleManagerHelper.getBleCardService() != null) {
                            mBleManagerHelper.getBleCardService().disconnect();
                        }
                    }
                    mSearchAddDev = false;
                    break;
                case BleMsg.TYPE_ADD_USER_SUCCESS:
                    DialogUtils.closeDialog(mLoadDialog);
                    DialogUtils.closeDialog(mCancelDialog);
                    mTitleTv.setText(R.string.add_lock_success);
                    mSearchingRl.setVisibility(View.GONE);
                    mAddLockSuccessLl.setVisibility(View.VISIBLE);
                    mSearchAddDev = false;
                    break;
                case BleMsg.TYPE_ADD_USER_FAILED:
                    DialogUtils.closeDialog(mLoadDialog);
                    DialogUtils.closeDialog(mCancelDialog);
                    showMessage(mCtx.getString(R.string.add_user_failed));
                    if (mDevice.getState() != Device.BLE_DISCONNECTED)
                        mBleManagerHelper.getBleCardService().disconnect();
                    mSearchAddDev = false;
                    break;
                case BleMsg.TYPE_USER_FULL:
                    DialogUtils.closeDialog(mLoadDialog);
                    showMessage(mCtx.getString(R.string.add_user_full));
                    if (mDevice.getState() != Device.BLE_DISCONNECTED)
                        mBleManagerHelper.getBleCardService().disconnect();
                    mSearchAddDev = false;
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);
        mCtx = this;
        initView();
        initDate();
        initEvent();
    }

    private void initView() {
        mSearchingIv = findViewById(R.id.iv_searching);
        mRescanLl = findViewById(R.id.ll_rescanning);
        mTipsLl = findViewById(R.id.ll_tips);
        mRescanBtn = findViewById(R.id.btn_rescanning);
        mBackIv = findViewById(R.id.iv_back);
        mScanLockTv = findViewById(R.id.tv_scan_lock);
        mTitleTv = findViewById(R.id.tv_message_title);
        mSearchingRl = findViewById(R.id.ll_searching);
        mAddLockSuccessLl = findViewById(R.id.ll_result_view);
        mConfirmBtn = findViewById(R.id.btn_confirm);
        mRemarkEt = findViewById(R.id.et_remark);
        mRefreshDevLl = findViewById(R.id.refresh_dev_ll);
        mDevList = findViewById(R.id.dev_list_rv);
        mScanDev = findViewById(R.id.fl_scan_dev);
        mScanEmpty = findViewById(R.id.scan_empty);
        mScanDevBar = findViewById(R.id.pb_scan_ble_dev);
        mLine = findViewById(R.id.line);

        mRefreshLayout = findViewById(R.id.refreshLayout);
        mRefreshLayout.setEnableRefresh(false);
    }

    private void initDate() {
        mRotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_loading);
        Bundle extras = getIntent().getExtras();
        mBleManagerHelper = BleManagerHelper.getInstance(this);
        //扫描连接
        if (extras != null) {
            mConnectType = extras.getByte(BleMsg.KEY_BLE_CONNECT_TYPE);
            if (mConnectType == Device.BLE_SET_DEVICE_INFO_CONNECT_TYPE) {
                mMode = SET_DEV_INFO;
                deviceList = new ArrayList<>();
                mTitleTv.setText(getString(R.string.search_lock));
                mBleAdapter = new BleAdapter(this, deviceList);
                mDevList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
                mDevList.setItemAnimator(new DefaultItemAnimator());
                mDevList.setAdapter(mBleAdapter);
            }
            mBleMac = StringUtil.getMacAdr(extras.getString(BleMsg.KEY_BLE_MAC));
//            mSn = extras.getString(BleMsg.KEY_NODE_SN);
//            mNodeId = extras.getString(BleMsg.KEY_NODE_ID);
            mDevice = mBleManagerHelper.getDevice(mConnectType, extras, this);
        } else {
            mMode = SEARCH_LOCK;
            deviceList = new ArrayList<>();
            mTitleTv.setText(getString(R.string.search_lock));
            mBleAdapter = new BleAdapter(this, deviceList);
            mDevList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            mDevList.setItemAnimator(new DefaultItemAnimator());
            mDevList.setAdapter(mBleAdapter);
        }

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mLoadDialog = DialogUtils.createLoadingDialog(mCtx, mCtx.getString(R.string.add_locking));

        mBleManagerHelper.addUiListener(this);
        mRefreshDevLl.setVisibility(View.GONE);
        scanLeDevice(true);
    }

    private void initEvent() {
        mRescanBtn.setOnClickListener(this);
        mBackIv.setOnClickListener(this);
        mConfirmBtn.setOnClickListener(this);
        mRemarkEt.setOnClickListener(this);
        mRefreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                refreshLayout.finishRefresh(5000/*,false*/);//传入false表示刷新失败
                Log.d(TAG, "setOnRefreshListener");
            }

        });
        int count = (int) DeviceInfoDao.getInstance(mCtx).queryCount();

        for (int i = 1; i <= count + 2; i++) {
            if (DeviceInfoDao.getInstance(mCtx).queryByField(DeviceInfoDao.DEVICE_NAME, getString(R.string.lock_default_name) + i) == null) {
                mRemarkEt.setHint(getString(R.string.lock_default_name) + i);
                break;
            }
        }
    }

    /**
     * 搜索附近门锁
     *
     * @param enable
     */
    private void scanLeDevice(final boolean enable) {

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (enable) {
                if (mMode == DETECTING_LOCK) {
                    mHandler.postDelayed(mStopScan, SCAN_PERIOD);
                    mRefreshDevLl.setVisibility(View.GONE);
                    mRescanLl.setVisibility(View.GONE);
                    mTipsLl.setVisibility(View.GONE);
                    mSearchingIv.startAnimation(mRotateAnimation);
                    mScanLockTv.setText(R.string.tv_scan_lock);
                    mLine.setVisibility(View.VISIBLE);
                } else if (mMode == SEARCH_LOCK || mMode == SET_DEV_INFO) {
                    mLine.setVisibility(View.GONE);
                    mScanLockTv.setVisibility(View.GONE);
                    mRefreshDevLl.setVisibility(View.VISIBLE);
                    mBleAdapter.mBluetoothDevList.clear();
                    mBleAdapter.notifyDataSetChanged();
                    mScanDev.setVisibility(View.GONE);
                    mScanEmpty.setVisibility(View.GONE);
                    mRescanLl.setVisibility(View.VISIBLE);
                    mRescanBtn.setText(R.string.stop_scan);
                    mTipsLl.setVisibility(View.GONE);
                    startRefresh();
                }
                mScanning = true;
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                if (mMode == DETECTING_LOCK) {
                    mSearchingIv.clearAnimation();
                } else if (mMode == SEARCH_LOCK || mMode == SET_DEV_INFO) {
                    mRescanBtn.setText(R.string.rescanning);
                    stopRefresh();
                }

                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }
    }

    /**
     * 开始刷新动画
     */
    public void startRefresh() {
        mScanDevBar.setIndeterminateDrawable(getResources().getDrawable(
                R.drawable.progressbar_loading));
        mScanDevBar.setProgressDrawable(getResources().getDrawable(
                R.drawable.progressbar_loading));
        mScanDevBar.setVisibility(View.VISIBLE);
    }

    /**
     * 停止刷新动画
     */
    public void stopRefresh() {
        mScanDevBar.setIndeterminateDrawable(getResources().getDrawable(
                R.mipmap.dialog_loading_img));
        mScanDevBar.setProgressDrawable(getResources().getDrawable(
                R.mipmap.dialog_loading_img));
        mScanDevBar.setVisibility(View.GONE);
    }

    /**
     * 蓝牙搜索结果回调
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mMode == DETECTING_LOCK) {
                        detectDevice(device);
                    } else if (mMode == SEARCH_LOCK || mMode == SET_DEV_INFO) {
                        if (StringUtil.checkNotNull(device.getName()) && device.getName().equals(ConstantUtil.LOCK_DEFAULT_NAME)) {
                            Log.d(TAG, "device.getName() = " + device.getName());
                            addDevice(device);
                        }
                    }

                }
            });
        }
    };

    private void addDevice(BluetoothDevice device) {
        mBleAdapter.addItem(device);
    }

    /**
     * 搜索结果处理
     *
     * @param device 设备信息
     */
    private void detectDevice(BluetoothDevice device) {

        if (device.getAddress().equals(mBleMac)) {
            mHandler.removeCallbacks(mStopScan);
            scanLeDevice(false);
            mRescanLl.setVisibility(View.GONE);
            mTipsLl.setVisibility(View.GONE);
            String mac = device.getAddress().replace(getString(R.string.colon), "");
            mScanLockTv.setText(getString(R.string.checking_security));
            LogUtil.d(TAG, "mac = " + mac);
            byte[] macByte = StringUtil.hexStringToBytes(mac);

            if (MessageCreator.mIs128Code) {
                System.arraycopy(StringUtil.hexStringToBytes("5A6B7C8D9E"), 0, MessageCreator.m128SK, 0, 5);
                System.arraycopy(macByte, 0, MessageCreator.m128SK, 5, 6); //写入MAC
                System.arraycopy(StringUtil.hexStringToBytes("A5B6C7D8E9"), 0, MessageCreator.m128SK, 11, 5);
            } else {
                System.arraycopy(macByte, 0, MessageCreator.m256SK, 0, 6); //写入MAC
                Arrays.fill(MessageCreator.m256SK, 6, 32, (byte) 0);
            }
            Device connDev = Device.getInstance(mCtx);
            if (connDev != null && mBleManagerHelper.getBleCardService() != null && connDev.getState() != Device.BLE_DISCONNECTED) {
                connDev.halt();
                connDev.setDisconnectBle(true);
                mBleManagerHelper.getBleCardService().disconnect();
            }
            DialogUtils.closeDialog(mLoadDialog);
            LogUtil.d(TAG, "mDevice is" + ((mDevice == null) ? true : mDevice.toString()));
            if (mDevice != null && mDevice.getConnectType() == Device.BLE_SET_DEVICE_INFO_CONNECT_TYPE) {
                mLoadDialog = DialogUtils.createLoadingDialog(mCtx, mCtx.getResources().getString(R.string.data_loading));
                mLoadDialog.show();
            }
            if (mMode == SEARCH_LOCK) {
                if (DeviceInfoDao.getInstance(this).queryByField(DeviceInfoDao.DEVICE_MAC, device.getAddress()) == null) {
                    mLoadDialog = DialogUtils.createLoadingDialog(mCtx, mCtx.getResources().getString(R.string.checking_security));
                    mLoadDialog.show();
                    Bundle bundle = new Bundle();
                    bundle.putString(BleMsg.KEY_BLE_MAC, device.getAddress());
                    mDevice = mBleManagerHelper.getDevice(Device.BLE_SEARCH_DEV_CONNECT, bundle, this);
                    mBleManagerHelper.getBleCardService().connect(mDevice, device.getAddress()); //搜索添加
                } else {
                    ToastUtil.showLong(this, getString(R.string.device_has_been_added));
                }
            } else {
                DeviceInfo devInfo = new DeviceInfo();
                devInfo.setBleMac(mac);
                mDevice.setDevInfo(devInfo);
                mBleManagerHelper.getBleCardService().connect(mDevice, mBleMac);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        mBleManagerHelper.removeUiListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_rescanning:
                if (mScanning)
                    scanLeDevice(false);
                else
                    scanLeDevice(true);
                break;
            case R.id.iv_back:
                finish();
                break;
            case R.id.btn_confirm:
                String deviceName = mRemarkEt.getText().toString().trim();
                if (mDetectingDevice != null) {
                    mDetectingDevice.setDeviceName((StringUtil.checkIsNull(deviceName) ? mRemarkEt.getHint().toString() : deviceName));
                    DeviceInfoDao.getInstance(this).updateDeviceInfo(mDetectingDevice);
                    Device connDev = Device.getInstance(mCtx);
                    connDev.setDisconnectBle(false);
                }
                if (SharedPreferenceUtil.getInstance(this).readBoolean(ConstantUtil.NUM_PWD_CHECK)) {
                    finish();
                } else {
                    Intent intent = new Intent(mCtx, LockScreenActivity.class);
                    intent.putExtra(ConstantUtil.IS_RETURN, true);
                    intent.putExtra(ConstantUtil.NOT_CANCEL, true);
                    startActivityForResult(intent.putExtra(ConstantUtil.TYPE, ConstantUtil.SETTING_PASSWORD), 1);
                }
                break;
            default:
                break;
        }
    }

    private void dispatchErrorCode(byte errCode) {
        LogUtil.i(TAG, "errCode : " + errCode);

        switch (errCode) {
            case BleMsg.TYPE_ADD_USER_SUCCESS:
                showMessage(mCtx.getString(R.string.add_user_success));
                break;
            case BleMsg.TYPE_ADD_USER_FAILED:
                android.os.Message msg = new android.os.Message();
                msg.what = BleMsg.TYPE_ADD_USER_FAILED;
                mHandler.sendMessage(msg);
                break;
            case BleMsg.TYPE_USER_FULL:
                android.os.Message msgFull = new android.os.Message();
                msgFull.what = BleMsg.TYPE_USER_FULL;
                mHandler.sendMessage(msgFull);
                break;
            default:
                break;
        }
    }

    @Override
    public void deviceStateChange(Device device, int state) {
        mDevice = device;
        mDetectingDevice = device.getDevInfo();
        switch (state) {
            case BleMsg.STATE_DISCONNECTED:
                DialogUtils.closeDialog(mLoadDialog);
                DialogUtils.closeDialog(mCancelDialog);
                mScanLockTv.setText(R.string.disconnect_ble);
                mRescanLl.setVisibility(View.VISIBLE);
                mTipsLl.setVisibility(View.VISIBLE);
                mSearchAddDev = false;
                break;
            case BleMsg.STATE_CONNECTED:

                break;
            case BleMsg.GATT_SERVICES_DISCOVERED:
                if (mConnectType == Device.BLE_SET_DEVICE_INFO_CONNECT_TYPE) {
                    DialogUtils.closeDialog(mLoadDialog);
                    showMessage("set device info success !");
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void sendFailed(Message msg) {
        int exception = msg.getException();

        if (mBleManagerHelper.getBleCardService() != null && mDevice.getState() != Device.BLE_DISCONNECTED) {
            mDevice.setState(Device.BLE_DISCONNECTED);
            mBleManagerHelper.getBleCardService().disconnect();
        }
        switch (exception) {
            case Message.EXCEPTION_TIMEOUT:
                DialogUtils.closeDialog(mLoadDialog);
                DialogUtils.closeDialog(mCancelDialog);
                LogUtil.e(msg.getType() + " can't receiver msg!");
                if (msg.getType() == Message.TYPE_BLE_SEND_CMD_01 || msg.getType() == Message.TYPE_BLE_SEND_CMD_03) {
                    mBleManagerHelper.getBleCardService().disconnect();
                }
                break;
            case Message.EXCEPTION_SEND_FAIL:
                DialogUtils.closeDialog(mLoadDialog);
                DialogUtils.closeDialog(mCancelDialog);
                LogUtil.e(msg.getType() + " send failed!");
                LogUtil.e(TAG, "msg exception : " + msg.toString());
                break;
            default:
                break;
        }

    }

    @Override
    public void dispatchUiCallback(Message msg, Device device, int type) {
        mDevice = device;
//        if (mDevice != null && type == BleMsg.USER_PAUSE) {
//            Dialog alterDialog = DialogUtils.createTipsDialogWithCancel(mCtx, getString(R.string.user_pause_contact_admin));
//            alterDialog.show();
//            if (mBleManagerHelper.getBleCardService() != null) {
//                mBleManagerHelper.getBleCardService().disconnect();
//            }
//            return;
//        }
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_1E:
                final byte[] errCode = msg.getData().getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode != null)
                    dispatchErrorCode(errCode[3]);
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_04:
                mDevice = device;
                mDetectingDevice = device.getDevInfo();
                android.os.Message message = new android.os.Message();
                message.what = type;
                mHandler.sendMessage(message);
                break;
            default:
                break;

        }
    }


    @Override
    public void scanDevFailed() {
        if (mDevice != null)
            mDevice.setState(Device.BLE_DISCONNECTED);
        android.os.Message msg = new android.os.Message();
        msg.what = BleMsg.SCAN_DEV_FIALED;
        mHandler.sendMessage(msg);
    }

    @Override
    public void reConnectBle(Device device) {

    }

    @Override
    public void addUserSuccess(Device device) {
        mDevice = device;
        mDetectingDevice = device.getDevInfo();
        android.os.Message msg = new android.os.Message();
        msg.what = BleMsg.TYPE_ADD_USER_SUCCESS;
        mHandler.sendMessage(msg);
    }


    public class BleAdapter extends RecyclerView.Adapter<BleAdapter.ViewHolder> {
        private Context mContext;
        public ArrayList<BluetoothDevice> mBluetoothDevList;

        public BleAdapter(Context context, ArrayList<BluetoothDevice> devList) {
            mContext = context;
            mBluetoothDevList = devList;
        }

        public void setDataSource(ArrayList<BluetoothDevice> devList) {
            mBluetoothDevList = devList;
        }

        public void addItem(BluetoothDevice bleDev) {
            int index = mBluetoothDevList.indexOf(bleDev);
            if (index == -1) {
                mBluetoothDevList.add(mBluetoothDevList.size(), bleDev);
                notifyItemInserted(mBluetoothDevList.size());
            }

        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_dev, parent, false);
            SwipeLayout swipeLayout = inflate.findViewById(R.id.item_ll_dev);
            swipeLayout.setClickToClose(false);
            swipeLayout.setRightSwipeEnabled(false);
            return new ViewHolder(inflate);
        }

        @Override
        public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
            final BluetoothDevice dev = mBluetoothDevList.get(position);
            if (dev != null) {
                viewHolder.mDevName.setText(dev.getName());
                viewHolder.mDevMac.setText(dev.getAddress());

                viewHolder.mDevContent.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (!mSearchAddDev) {
                            if (mScanning) {
                                scanLeDevice(false);
                            }
                            mSearchAddDev = true;
                            mBleMac = dev.getAddress();
                            detectDevice(dev);
                        } else showMessage(getString(R.string.data_loading));

                    }
                });
            }

        }

        @Override
        public int getItemCount() {
            return mBluetoothDevList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            SwipeLayout mSwipeLayout;
            TextView mDevMac;
            TextView mDevName;
            LinearLayout mDevContent;

            public ViewHolder(View itemView) {
                super(itemView);

                mSwipeLayout = (SwipeLayout) itemView;
                mDevMac = itemView.findViewById(R.id.tv_dev_mac);
                mDevName = itemView.findViewById(R.id.tv_dev_name);
                mDevContent = itemView.findViewById(R.id.ll_content);
            }
        }

    }

    public void onBackPressed() {
        long curTime = SystemClock.uptimeMillis();
        if (curTime - mBackPressedTime < 3000) {
            finish();
            return;
        }
        mBackPressedTime = curTime;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && data != null) {
            switch (data.getExtras().getInt(ConstantUtil.CONFIRM)) {
                case 1:
                    SharedPreferenceUtil.getInstance(this).
                            writeBoolean(ConstantUtil.NUM_PWD_CHECK, true);
                    ToastUtil.showLong(this, getResources().getString(R.string.pwd_setting_successfully));
                    finish();
                    break;
                default:
                    ToastUtil.showLong(this, getResources().getString(R.string.pwd_setting_failed));
                    finish();
                    break;
            }
        } else if (requestCode == REQUEST_ENABLE_BT) {
            LogUtil.d(TAG, "requestCode = " + requestCode + " resultCode :" + resultCode);
            if (resultCode == RESULT_OK) {
                scanLeDevice(true);
            } else if (resultCode == RESULT_CANCELED) {
                showMessage(getString(R.string.unenable_ble));
                finish();
            }

        }

    }
}
