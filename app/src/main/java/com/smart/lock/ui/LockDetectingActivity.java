package com.smart.lock.ui;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.MainActivity;
import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.Arrays;

public class LockDetectingActivity extends BaseActivity implements View.OnClickListener {

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

    private Animation mRotateAnimation;
    private static final long SCAN_PERIOD = 10000; //scanning for 10 seconds
    private Handler mHandler;
    private boolean mScanning;
    private BluetoothAdapter mBluetoothAdapter;//蓝牙适配器

    private boolean mIsConnected = false;//服务连接标志
    private static final int UART_PROFILE_CONNECTED = 20;//蓝牙卡连接成功
    private static final int UART_PROFILE_DISCONNECTED = 21;//蓝牙卡未连接
    private int mState = UART_PROFILE_DISCONNECTED;//蓝牙状态
    private Dialog mLoadDialog;
    private DeviceInfo mDetectingDevice;

    private Runnable mRunnable = new Runnable() {
        public void run() {
            DialogUtils.closeDialog(mLoadDialog);
            Toast.makeText(LockDetectingActivity.this, R.string.retry_connect, Toast.LENGTH_LONG).show();

            Intent intent = new Intent();
            intent.setAction(BleMsg.STR_RSP_SET_TIMEOUT);
            LocalBroadcastManager.getInstance(LockDetectingActivity.this).sendBroadcast(intent);
        }
    };

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);
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
    }

    private void initDate() {
        mRotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_loading);
        mHandler = new Handler();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mBleMac = getMacAdr(extras.getString(BleMsg.KEY_BLE_MAC));
            mSn = extras.getString(BleMsg.KEY_NODE_SN);
            mNodeId = extras.getString(BleMsg.KEY_NODE_ID);
            mUserType = extras.getString(BleMsg.KEY_USER_TYPE);
            mUserType = mUserType != null ? mUserType : "0";
        }

        // When you need the permission, e.g. onCreate, OnClick etc.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 255);
        } else {
            // We have already permission to use the location
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

        LocalBroadcastManager.getInstance(this).registerReceiver(detectReciver, makeGattUpdateIntentFilter());

        scanLeDevice(true);


    }

    private void initEvent() {
        mRescanBtn.setOnClickListener(this);
        mBackIv.setOnClickListener(this);
        mConfirmBtn.setOnClickListener(this);
        mRemarkEt.setOnClickListener(this);
    }


    /**
     * @return
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.STR_RSP_SECURE_CONNECTION);
        intentFilter.addAction(BleMsg.EXTRA_DATA_MSG_12);
        intentFilter.addAction(BleMsg.STR_RSP_SET_TIMEOUT);
        intentFilter.addAction(BleMsg.STR_RSP_MSG1E_ERRCODE);
        return intentFilter;
    }

    /**
     * 广播接收
     */
    private final BroadcastReceiver detectReciver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // 4.2.3 MSG 04
            if (action.equals(BleMsg.STR_RSP_SECURE_CONNECTION)) {
                mLoadDialog = DialogUtils.createLoadingDialog(LockDetectingActivity.this, LockDetectingActivity.this.getString(R.string.add_locking));
                closeDialog(10);
                BleManagerHelper.getInstance(LockDetectingActivity.this, mBleMac, false).getBleCardService().sendCmd11(Byte.valueOf(mUserType), (short) 0);
            }

            // 4.2.3 MSG 12
            if (action.equals(BleMsg.EXTRA_DATA_MSG_12)) {
                mTitleTv.setText(R.string.add_lock_success);
                mSearchingRl.setVisibility(View.GONE);
                mAddLockSuccessLl.setVisibility(View.VISIBLE);

                String userId = StringUtil.bytesToHexString(intent.getByteArrayExtra(BleMsg.KEY_USER_ID));
                LogUtil.d(TAG, "userId = " + Arrays.toString(intent.getByteArrayExtra(BleMsg.KEY_USER_ID)));
                String time = StringUtil.bytesToHexString(intent.getByteArrayExtra(BleMsg.KEY_LOCK_TIME));
                String randCode = StringUtil.bytesToHexString(intent.getByteArrayExtra(BleMsg.KEY_RAND_CODE));
                LogUtil.d(TAG, "KEY_RAND_CODE = " + Arrays.toString(intent.getByteArrayExtra(BleMsg.KEY_RAND_CODE)));
                DeviceInfo defaultDevice = DeviceInfoDao.getInstance(LockDetectingActivity.this).queryFirstData("device_default", true);

                mDetectingDevice = new DeviceInfo();
                mDetectingDevice.setActivitedTime(Long.parseLong(time, 16));
                mDetectingDevice.setBleMac(mBleMac);
                mDetectingDevice.setConnectType(false);
                mDetectingDevice.setUserId(Short.parseShort(userId, 16));
                mDetectingDevice.setDeviceNodeId(mNodeId);
                mDetectingDevice.setNodeType(ConstantUtil.SMART_LOCK);
                mDetectingDevice.setDeviceDate(System.currentTimeMillis() / 1000);
                if (defaultDevice != null) mDetectingDevice.setDeviceDefault(false);
                else mDetectingDevice.setDeviceDefault(true);
                mDetectingDevice.setDeviceSn(mSn);
                mDetectingDevice.setDeviceName(ConstantUtil.LOCK_DEFAULT_NAME);
                mDetectingDevice.setDeviceSecret(randCode);
                DeviceInfoDao.getInstance(LockDetectingActivity.this).insert(mDetectingDevice);

                createDeviceUser(Short.parseShort(userId, 16));

                mHandler.removeCallbacks(mRunnable);
                DialogUtils.closeDialog(mLoadDialog);
            }

            //MSG1E 设备->apk，返回信息
            if (action.equals(BleMsg.STR_RSP_MSG1E_ERRCODE)) {
                final byte[] errCode = intent.getByteArrayExtra(BleMsg.KEY_ERROR_CODE);
                DialogUtils.closeDialog(mLoadDialog);
                mHandler.removeCallbacks(mRunnable);
                Log.d(TAG, "errCode[3] = " + errCode[3]);

                if (errCode[3] == 0x2) {
                    showMessage(LockDetectingActivity.this.getString(R.string.add_user_success));
                } else if (errCode[3] == 0x3) {
                    showMessage(LockDetectingActivity.this.getString(R.string.add_user_failed));
                }

            }

            //MSG1E 设备->apk，返回信息
            if (action.equals(BleMsg.STR_RSP_SET_TIMEOUT)) {
                BleManagerHelper.getInstance(LockDetectingActivity.this, mBleMac, false).stopService();

                mRescanLl.setVisibility(View.VISIBLE);
                mTipsLl.setVisibility(View.VISIBLE);
                mScanLockTv.setText(R.string.bt_connect_failed);
            }
        }
    };

    /**
     * 创建用户
     *
     * @param userId
     */
    private void createDeviceUser(short userId) {
        DeviceUser user = new DeviceUser();
        int userIdInt = Integer.valueOf(userId);
        user.setDevNodeId(mNodeId);
        user.setCreateTime(System.currentTimeMillis() / 1000);
        user.setUserId(userId);
        user.setUserPermission(ConstantUtil.DEVICE_MASTER);
        if (userIdInt < 101) {
            user.setUserPermission(ConstantUtil.DEVICE_MASTER);
            user.setUserName(getString(R.string.administrator) + userId);
        } else if (userIdInt < 201) {
            user.setUserPermission(ConstantUtil.DEVICE_MEMBER);
            user.setUserName(getString(R.string.members) + userId);
        } else {
            user.setUserPermission(ConstantUtil.DEVICE_MEMBER);
            user.setUserName(getString(R.string.members) + userId);
        }

        user.setUserStatus(ConstantUtil.USER_UNENABLE);

        DeviceUserDao.getInstance(this).insert(user);
    }

    /**
     * 搜索附近门锁
     *
     * @param enable
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(mStopScan, SCAN_PERIOD);

            mRescanLl.setVisibility(View.GONE);
            mTipsLl.setVisibility(View.GONE);
            mSearchingIv.startAnimation(mRotateAnimation);
            mScanning = true;
            mScanLockTv.setText(R.string.tv_scan_lock);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mSearchingIv.clearAnimation();
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

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
                    addDevice(device, rssi, scanRecord);
                }
            });
        }
    };

    /**
     * 搜索结果处理
     *
     * @param device
     * @param rssi
     * @param scanRecord
     */
    private void addDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {

        LogUtil.d(TAG, "ble mac = " + device.getAddress());
        LogUtil.d(TAG, "dev mac = " + mBleMac);
        if (device.getAddress().equals(mBleMac)) {
            LogUtil.d(TAG, "dev rssi = " + rssi);
            mHandler.removeCallbacks(mStopScan);
            scanLeDevice(false);
            mRescanLl.setVisibility(View.GONE);
            mTipsLl.setVisibility(View.GONE);
            mScanLockTv.setText("");
            String mac = device.getAddress().replace(":", "");

            byte[] macByte = StringUtil.hexStringToBytes(mac);
            if (mNodeId.getBytes().length == 15) {
                mNodeId = "0" + mNodeId;
            }
            LogUtil.d(TAG, "nodeId = " + mNodeId);

            byte[] nodeId = StringUtil.hexStringToBytes(mNodeId);

            StringUtil.exchange(nodeId);

            LogUtil.d(TAG, "nodeId = " + Arrays.toString(nodeId));
            LogUtil.d(TAG, "macByte = " + Arrays.toString(macByte));

            System.arraycopy(nodeId, 0, MessageCreator.mSK, 0, 8); //写入IMEI

            System.arraycopy(macByte, 0, MessageCreator.mSK, 8, 6); //写入MAC

            Arrays.fill(MessageCreator.mSK, 14, 32, (byte) 0);

            LogUtil.d(TAG, "sk = " + Arrays.toString(MessageCreator.mSK));
            mIsConnected = BleManagerHelper.getInstance(this, mBleMac, false).getServiceConnection();
            if (!mIsConnected)
                BleManagerHelper.getInstance(this, mBleMac, false).connectBle((byte) 0, (short) 0);
        }

    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        BleManagerHelper.getInstance(LockDetectingActivity.this, mBleMac, false).stopService();

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(detectReciver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_rescanning:
                scanLeDevice(true);
                break;
            case R.id.iv_back:
                finish();
                break;
            case R.id.btn_confirm:
                startIntent(MainActivity.class, null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                break;
            case R.id.et_remark:
                String deviceName = mRemarkEt.getText().toString().trim();
                mDetectingDevice.setDeviceName(deviceName);
                DeviceInfoDao.getInstance(this).updateDeviceInfo(mDetectingDevice);
                break;
            default:
                break;
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
}
