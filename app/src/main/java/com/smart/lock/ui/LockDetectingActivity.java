package com.smart.lock.ui;

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
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.swipe.SwipeLayout;
import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.ui.login.LockScreenActivity;
import com.smart.lock.ui.setting.SystemSettingsActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.SharedPreferenceUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.ToastUtil;

import java.util.ArrayList;
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
    private LinearLayout mRefreshDevLl;
    private RecyclerView mDevList;
    private RelativeLayout mScanDev;
    private LinearLayout mScanEmpty;
    private ProgressBar mScanDevBar;
    private View mLine;

    private Animation mRotateAnimation;
    private static final long SCAN_PERIOD = 10000; //scanning for 10 seconds
    private Handler mHandler;
    private boolean mScanning;
    private BluetoothAdapter mBluetoothAdapter;//蓝牙适配器
    private BleManagerHelper mBleManagerHelper;

    private boolean mIsConnected = false;//服务连接标志
    private static final int UART_PROFILE_CONNECTED = 20;//蓝牙卡连接成功
    private static final int UART_PROFILE_DISCONNECTED = 21;//蓝牙卡未连接
    private int mState = UART_PROFILE_DISCONNECTED;//蓝牙状态
    private Dialog mLoadDialog;
    private DeviceInfo mDetectingDevice;
    private ArrayList<BluetoothDevice> deviceList;
    private BleAdapter mBleAdapter;

    private final int DETECTING_LOCK = 0x1;
    private final int SEARCH_LOCK = 0x2;
    private int mMode = DETECTING_LOCK;
    private int REQUEST_ENABLE_BT = 100;

    //back time
    private long mBackPressedTime;

    private Runnable mRunnable = new Runnable() {
        public void run() {
            DialogUtils.closeDialog(mLoadDialog);
            Toast.makeText(LockDetectingActivity.this, R.string.retry_connect, Toast.LENGTH_LONG).show();

            mBleManagerHelper.getBleCardService().disconnect();

            mRescanLl.setVisibility(View.VISIBLE);
            mTipsLl.setVisibility(View.VISIBLE);
            mScanLockTv.setText(R.string.bt_connect_failed);
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
        mRefreshDevLl = findViewById(R.id.refresh_dev_ll);
        mDevList = findViewById(R.id.dev_list_rv);
        mScanDev = findViewById(R.id.fl_scan_dev);
        mScanEmpty = findViewById(R.id.scan_empty);
        mScanDevBar = findViewById(R.id.pb_scan_ble_dev);
        mLine = findViewById(R.id.line);
    }

    private void initDate() {
        mRotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_loading);
        mHandler = new Handler();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mBleMac = getMacAdr(extras.getString(BleMsg.KEY_BLE_MAC));
            mSn = extras.getString(BleMsg.KEY_NODE_SN);
            mNodeId = extras.getString(BleMsg.KEY_NODE_ID);
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
        mLoadDialog = DialogUtils.createLoadingDialog(LockDetectingActivity.this, LockDetectingActivity.this.getString(R.string.add_locking));
        LocalBroadcastManager.getInstance(this).registerReceiver(detectReciver, makeGattUpdateIntentFilter());
        mBleManagerHelper = BleManagerHelper.getInstance(this, false);
        mRefreshDevLl.setVisibility(View.GONE);
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
        intentFilter.addAction(BleMsg.ACTION_GATT_DISCONNECTED);
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
                mHandler.removeCallbacks(mRunnable);
                DialogUtils.closeDialog(mLoadDialog);
                if (mMode == SEARCH_LOCK) {
                    mLoadDialog = DialogUtils.createLoadingDialog(LockDetectingActivity.this, LockDetectingActivity.this.getResources().getString(R.string.plz_press_setting));
                    mLoadDialog.show();
                    closeDialog(10);
                    mBleManagerHelper.getBleCardService().sendCmd11((byte) 7, (short) 0);
                } else {
                    mLoadDialog.show();
                    closeDialog(10);
                    mBleManagerHelper.getBleCardService().sendCmd11((byte) 0, (short) 0);
                }
            }

            // 4.2.3 MSG 12
            if (action.equals(BleMsg.EXTRA_DATA_MSG_12)) {
                int size = DeviceUserDao.getInstance(LockDetectingActivity.this).queryUsers(mNodeId, ConstantUtil.DEVICE_MASTER).size();
                if (size >= 5) {
                    showMessage(LockDetectingActivity.this.getString(R.string.add_user_tips));
                    mIsConnected = mBleManagerHelper.getServiceConnection();
                    if (mIsConnected) {
                        mBleManagerHelper.getBleCardService().disconnect();
                    }
                    return;
                }
                String userId = StringUtil.bytesToHexString(intent.getByteArrayExtra(BleMsg.KEY_USER_ID));
                LogUtil.d(TAG, "userId = " + Arrays.toString(intent.getByteArrayExtra(BleMsg.KEY_USER_ID)));
                byte[] nodeIdBuf = intent.getByteArrayExtra(BleMsg.KEY_NODE_ID);
                StringUtil.exchange(nodeIdBuf);
                String nodeId = StringUtil.bytesToHexString(nodeIdBuf);
                LogUtil.d(TAG, "nodId = " + nodeId);
                String time = StringUtil.bytesToHexString(intent.getByteArrayExtra(BleMsg.KEY_LOCK_TIME));
                String randCode = StringUtil.bytesToHexString(intent.getByteArrayExtra(BleMsg.KEY_RAND_CODE));
                LogUtil.d(TAG, "KEY_RAND_CODE = " + Arrays.toString(intent.getByteArrayExtra(BleMsg.KEY_RAND_CODE)));
                DeviceInfo defaultDevice = DeviceInfoDao.getInstance(LockDetectingActivity.this).queryFirstData("device_default", true);

                mDetectingDevice = new DeviceInfo();
                mDetectingDevice.setActivitedTime(Long.parseLong(time, 16));
                mDetectingDevice.setBleMac(mBleMac);
                mDetectingDevice.setConnectType(false);
                mDetectingDevice.setUserId(Short.parseShort(userId, 16));
                mDetectingDevice.setDeviceNodeId(nodeId);
                mDetectingDevice.setNodeType(ConstantUtil.SMART_LOCK);
                mDetectingDevice.setDeviceDate(System.currentTimeMillis() / 1000);
                if (defaultDevice != null) mDetectingDevice.setDeviceDefault(false);
                else mDetectingDevice.setDeviceDefault(true);
                mDetectingDevice.setDeviceSn(mSn);
                mDetectingDevice.setDeviceName(getString(R.string.lock_default_name));
                mDetectingDevice.setDeviceSecret(randCode);
                DeviceInfoDao.getInstance(LockDetectingActivity.this).insert(mDetectingDevice);

                mBleManagerHelper.getBleCardService().disconnect();

                createDeviceUser(Short.parseShort(userId, 16), nodeId);

                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        mTitleTv.setText(R.string.add_lock_success);
                        mSearchingRl.setVisibility(View.GONE);
                        mAddLockSuccessLl.setVisibility(View.VISIBLE);
                        mHandler.removeCallbacks(mRunnable);
                        DialogUtils.closeDialog(mLoadDialog);
                    }
                }, 2000);

            }

            //MSG1E 设备->apk，返回信息
            if (action.equals(BleMsg.STR_RSP_MSG1E_ERRCODE)) {
                final byte[] errCode = intent.getByteArrayExtra(BleMsg.KEY_ERROR_CODE);

                Log.d(TAG, "errCode[3] = " + errCode[3]);

                if (errCode[3] == 0x2) {
                    showMessage(LockDetectingActivity.this.getString(R.string.add_user_success));
                } else if (errCode[3] == 0x3) {
                    showMessage(LockDetectingActivity.this.getString(R.string.add_user_failed));
                    mIsConnected = mBleManagerHelper.getServiceConnection();
                    if (mIsConnected) {
                        mBleManagerHelper.getBleCardService().disconnect();
                    }
                }
                DialogUtils.closeDialog(mLoadDialog);
                mHandler.removeCallbacks(mRunnable);

            }

            //MSG1E 设备->apk，返回信息
            if (action.equals(BleMsg.STR_RSP_SET_TIMEOUT)) {
                mBleManagerHelper.getBleCardService().disconnect();
                mRescanLl.setVisibility(View.VISIBLE);
                mTipsLl.setVisibility(View.VISIBLE);
                mScanLockTv.setText(R.string.bt_connect_failed);
            }

            if (action.equals(BleMsg.ACTION_GATT_DISCONNECTED)) {
                mIsConnected = false;
                mScanLockTv.setText(R.string.disconnect_ble);
                mRescanLl.setVisibility(View.VISIBLE);
                mTipsLl.setVisibility(View.VISIBLE);
            }
        }
    };

    /**
     * 创建用户
     *
     * @param userId
     */
    private void createDeviceUser(short userId, String nodeId) {
        DeviceUser user = new DeviceUser();
        user.setDevNodeId(nodeId);
        user.setCreateTime(System.currentTimeMillis() / 1000);
        user.setUserId(userId);
        user.setUserPermission(ConstantUtil.DEVICE_MASTER);
        if (userId < 101) {
            user.setUserPermission(ConstantUtil.DEVICE_MASTER);
            user.setUserName(getString(R.string.administrator) + userId);
        } else if (userId < 201) {
            user.setUserPermission(ConstantUtil.DEVICE_MEMBER);
            user.setUserName(getString(R.string.members) + userId);
        } else {
            user.setUserPermission(ConstantUtil.DEVICE_MEMBER);
            user.setUserName(getString(R.string.members) + userId);
        }

        user.setUserStatus(ConstantUtil.USER_UNENABLE);
        LogUtil.d(TAG, "user = " + user.toString());
        DeviceUserDao.getInstance(this).insert(user);
    }

    /**
     * 搜索附近门锁
     *
     * @param enable
     */
    private void scanLeDevice(final boolean enable) {

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
                } else if (mMode == SEARCH_LOCK) {
                    mLine.setVisibility(View.GONE);
                    mScanLockTv.setVisibility(View.GONE);
                    mRefreshDevLl.setVisibility(View.VISIBLE);
                    mBleAdapter.mBluetoothDevlist.clear();
                    mBleAdapter.notifyDataSetChanged();
                    mScanDev.setVisibility(View.GONE);
                    mScanEmpty.setVisibility(View.GONE);
                    mRescanLl.setVisibility(View.VISIBLE);
                    mRescanBtn.setText(R.string.stop_scan);
                    startRefresh();
                }
                mScanning = true;
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                if (mMode == DETECTING_LOCK) {
                    mSearchingIv.clearAnimation();
                } else if (mMode == SEARCH_LOCK) {
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
                    } else if (mMode == SEARCH_LOCK) {
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
     * @param device
     */
    private void detectDevice(BluetoothDevice device) {

        LogUtil.d(TAG, "ble mac = " + device.getAddress());
        LogUtil.d(TAG, "dev mac = " + mBleMac);
        if (device.getAddress().equals(mBleMac)) {
            mHandler.removeCallbacks(mStopScan);
            scanLeDevice(false);
            mRescanLl.setVisibility(View.GONE);
            mTipsLl.setVisibility(View.GONE);
            mScanLockTv.setText("安全校验中");
            String mac = device.getAddress().replace(":", "");
            LogUtil.d(TAG, "mac = " + mac);
            byte[] macByte = StringUtil.hexStringToBytes(mac);

            LogUtil.d(TAG, "macByte = " + Arrays.toString(macByte));

            if (MessageCreator.mIs128Code) {
                System.arraycopy(macByte, 0, MessageCreator.m128SK, 0, 6); //写入IMEI
                Arrays.fill(MessageCreator.m128SK, 6, 16, (byte) 0);
                LogUtil.d(TAG, "sk = " + Arrays.toString(MessageCreator.m128SK));
            } else {
                System.arraycopy(macByte, 0, MessageCreator.m256SK, 0, 6); //写入MAC
                Arrays.fill(MessageCreator.m256SK, 6, 32, (byte) 0);
                LogUtil.d(TAG, "sk = " + Arrays.toString(MessageCreator.m256SK));
            }

            mIsConnected = mBleManagerHelper.getServiceConnection();
            LogUtil.d(TAG, "mIsConnected = " + mIsConnected);
            if (!mIsConnected)
                mBleManagerHelper.getBleCardService().connect(mBleMac);
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
                mDetectingDevice.setDeviceName((StringUtil.checkIsNull(deviceName) ? getString(R.string.lock_default_name) : deviceName));
                DeviceInfoDao.getInstance(this).updateDeviceInfo(mDetectingDevice);
                if (SharedPreferenceUtil.getInstance(this).readBoolean(ConstantUtil.NUM_PWD_CHECK)) {
                    finish();
                } else {
                    Intent intent = new Intent(LockDetectingActivity.this, LockScreenActivity.class);
                    intent.putExtra(ConstantUtil.IS_RETURN, true);
                    intent.putExtra(ConstantUtil.NOT_CANCEL, true);
                    LockDetectingActivity.this.startActivityForResult(intent.
                            putExtra(ConstantUtil.TYPE, ConstantUtil.SETTING_PASSWORD), 1);
                }
//                startIntent(MainActivity.class, null, Intent.FLAG_ACTIVITY_CLEAR_TOP);

                break;
            case R.id.et_remark:
                break;
            default:
                break;
        }
    }

    public class BleAdapter extends RecyclerView.Adapter<BleAdapter.ViewHolder> {
        private Context mContext;
        public ArrayList<BluetoothDevice> mBluetoothDevlist;

        public BleAdapter(Context context, ArrayList<BluetoothDevice> devList) {
            mContext = context;
            mBluetoothDevlist = devList;
        }

        public void setDataSource(ArrayList<BluetoothDevice> devList) {
            mBluetoothDevlist = devList;
        }

        public void addItem(BluetoothDevice bleDev) {
            int index = mBluetoothDevlist.indexOf(bleDev);
            LogUtil.d(TAG, "result = " + index);
            if (index == -1) {
                mBluetoothDevlist.add(mBluetoothDevlist.size(), bleDev);
                LogUtil.d(TAG, "mBluetoothDevlist = " + mBluetoothDevlist.size());
                notifyItemInserted(mBluetoothDevlist.size());
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
            final BluetoothDevice dev = mBluetoothDevlist.get(position);
            if (dev != null) {
                viewHolder.mDevName.setText(dev.getName());
                viewHolder.mDevMac.setText(dev.getAddress());

                viewHolder.mDevContent.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (mScanning) {
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        }
                        mBleMac = dev.getAddress();
                        mHandler.removeCallbacks(mRunnable);
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog = DialogUtils.createLoadingDialog(LockDetectingActivity.this, LockDetectingActivity.this.getResources().getString(R.string.checking_security));
                        mLoadDialog.show();
                        closeDialog(10);
                        detectDevice(dev);
                    }
                });
            }

        }

        @Override
        public int getItemCount() {
            return mBluetoothDevlist.size();
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


    /**
     * 超时提示框
     *
     * @param seconds 时间
     */
    private void closeDialog(int seconds) {
        mHandler.removeCallbacks(mRunnable);

        mHandler.postDelayed(mRunnable, seconds * 1000);
    }

    public void onBackPressed() {
        long curTime = SystemClock.uptimeMillis();
        if (curTime - mBackPressedTime < 3000) {
            mHandler.removeCallbacks(mRunnable);
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
