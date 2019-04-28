package com.smart.lock.ble;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.listener.MainEngine;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceStatus;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceStatusDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.entity.Device;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class BleManagerHelper {
    private static final String TAG = BleManagerHelper.class.getSimpleName();

    private static Context mContext;

    /**
     * 蓝牙服务
     */
    private BleCardService mService = null;

    /**
     * 蓝牙适配器
     */
    private BluetoothAdapter mBtAdapter = null;

    /**
     * 设备蓝牙地址
     */
    private String mBleMac;

    private String mNodeId; //设备IMEI
    private String mSn; //设备SN号

    /**
     * 蓝牙帮助类
     */
    private static BleManagerHelper instance;


    private Handler mHandler;

    /**
     * 服务连接标志
     */
    private boolean mIsConnected = false;

    /**
     * 0:正常连接模式，1:OTA升级模式，2:设备参数写入
     */
    private int mMode = 0;

    /**
     * 临时储存的OTA模式
     */
    private boolean mTempMode = false;

    /**
     * 等待提示框
     */
//    private Dialog mLoadDialog;

    /**
     * 连接方式 0-扫描二维码 1-普通安全连接,2-设置设备信息
     */
    private byte mConnectType = 0;

    /**
     * 用户编号，mConnectType =0 时，该值为0，否则非0
     */
    private short mUserId = 0;

    private long mStartTime = 0;
    private long mEndTime = 0;

    private static final long SCAN_PERIOD = 30000;
    private Device mDevice; //蓝牙连接状态实例

    private DeviceInfo mDefaultDevice; //默认设备
    private DeviceUser mDefaultUser; //默认用户
    private DeviceStatus mDefaultStatus; //用户状态
//    private Dialog mLoadDialog;

    private ArrayList<UiListener> mUiListeners = new ArrayList(); //Ui监听集合

    private Runnable mRunnable = new Runnable() {
        public void run() {
            if (mDevice.getState() == Device.BLE_CONNECTION) {
                mHandler.removeCallbacks(mRunnable);
                mDevice.setState(Device.BLE_DISCONNECTED);
                mBtAdapter.stopLeScan(mLeScanCallback);
            }
            for (UiListener uiListener : mUiListeners) {
                uiListener.scanDevFialed();
            }
            Intent intent = new Intent();
            intent.setAction(BleMsg.STR_RSP_SET_TIMEOUT);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        }
    };

    public BleManagerHelper(Context context, Boolean isOtaMode) {
        mContext = context;
        mTempMode = isOtaMode;
        mHandler = new Handler();
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

//        serviceInit();
        mService = BleCardService.getInstance(mContext);
        mService.initialize();
//        LocalBroadcastManager.getInstance(mContext).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(mContext, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        }
    }

    public static BleManagerHelper getInstance(Context context, Boolean isOtaMode) {
        LogUtil.d(TAG, "instance is " + (instance == null));
        if (instance == null) {
            synchronized (BleManagerHelper.class) {
                if (instance == null) {
                    instance = new BleManagerHelper(context, isOtaMode);
                }
            }
        } else {
            instance.setTempMode(isOtaMode);
        }
        return instance;
    }

    /**
     * 连接蓝牙
     *
     * @return
     */
    public void connectBle(final byte type, Bundle bundle, Context context) {
        getDevice(type, bundle, context);
        mConnectType = type;
        mBleMac = bundle.getString(BleMsg.KEY_BLE_MAC);
        if (StringUtil.checkIsNull(mBleMac)) {
            mStartTime = System.currentTimeMillis();
            return;
        }
        if (mConnectType == 2) {
            mContext = context;
            mNodeId = bundle.getString(BleMsg.KEY_NODE_ID);
            mSn = bundle.getString(BleMsg.KEY_NODE_SN);
        } else
            mUserId = bundle.getShort(BleMsg.KEY_USER_ID);

        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(enableIntent);
        } else {
            LogUtil.d(TAG, "ble state : " + mDevice.getState());
            if (mDevice.getState() == Device.BLE_DISCONNECTED) {
                mDevice.setState(Device.BLE_CONNECTION);
                closeDialog((int) (SCAN_PERIOD / 1000));
                mBtAdapter.startLeScan(mLeScanCallback);
            }
        }

    }

    public Device getDevice(byte type, Bundle bundle, Context ctx) {
        if (bundle == null) {
            LogUtil.e(TAG, "register dev is null!");
            return null;
        }
        mDevice = Device.getInstance(ctx);
        DeviceInfo devInfo = new DeviceInfo();
        String mac = bundle.getString(BleMsg.KEY_BLE_MAC);
        if (StringUtil.checkNotNull(mac) && mac.length() == 12) {
            devInfo.setBleMac(StringUtil.getMacAdr(mac));
        } else {
            devInfo.setBleMac(mac);
        }

        switch (type) {
            case Device.BLE_SCAN_QR_CONNECT_TYPE:
                devInfo.setUserId(bundle.getShort(BleMsg.KEY_USER_ID));
                mDevice.setDevInfo(devInfo);
                mDevice.setConnectType(type);
                break;
            case Device.BLE_OTHER_CONNECT_TYPE:
                devInfo = DeviceInfoDao.getInstance(ctx).queryFirstData("device_default", true);
                mDevice.setDevInfo(devInfo);
                mDevice.setConnectType(type);
                break;
            case Device.BLE_SET_DEVICE_INFO_CONNECT_TYPE:
                devInfo.setDeviceNodeId(bundle.getString(BleMsg.KEY_NODE_ID));
                devInfo.setDeviceSn(bundle.getString(BleMsg.KEY_NODE_SN));
                mDevice.setDevInfo(devInfo);
                mDevice.setConnectType(type);
                break;
            default:
                devInfo.setUserId(bundle.getShort(BleMsg.KEY_USER_ID));
                mDevice.setDevInfo(devInfo);
                mDevice.setConnectType(type);
                break;
        }
        return mDevice;
    }


    /**
     * 蓝牙搜索结果回调
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            if (StringUtil.checkIsNull(mBleMac)) {
                mHandler.removeCallbacks(mRunnable);
                mBtAdapter.stopLeScan(mLeScanCallback);
                return;
            }
            if (mConnectType == 2) {
                if (StringUtil.checkNotNull(device.getName()) && device.getName().equals(ConstantUtil.LOCK_DEFAULT_NAME)) {
                    LogUtil.d(TAG, "dev rssi = " + rssi);
                    mHandler.removeCallbacks(mRunnable);
                    mBtAdapter.stopLeScan(mLeScanCallback);
                    if (!mIsConnected && mService != null) {
                        boolean result = mService.connect(mDevice, device.getAddress());
                        LogUtil.d(TAG, "result = " + result);
                    }
                }
            } else {
                if (device.getAddress().equals(mBleMac)) {
                    LogUtil.d(TAG, "dev rssi = " + rssi);
                    mHandler.removeCallbacks(mRunnable);
                    mBtAdapter.stopLeScan(mLeScanCallback);
                    if (!mIsConnected && mService != null) {
                        boolean result = mService.connect(mDevice, device.getAddress());
                        LogUtil.d(TAG, "result = " + result);
                    }
                }
            }

        }

//        }
    };

    /**
     * 设置秘钥
     */
    public static void setSk(String sBleMac, String sDevSecret) {
        String mac = sBleMac.replace(":", "");

        byte[] macByte = StringUtil.hexStringToBytes(mac);

        if (MessageCreator.mIs128Code) {
            System.arraycopy(macByte, 0, MessageCreator.m128SK, 0, 6); //写入MAC
            byte[] code = new byte[10];
            if (sDevSecret == null || sDevSecret.equals("0")) {
                Arrays.fill(MessageCreator.m128SK, 6, 16, (byte) 0);
            } else {
                code = StringUtil.hexStringToBytes(sDevSecret);
                System.arraycopy(code, 0, MessageCreator.m128SK, 6, 10); //写入secretCode
            }
        } else {
            System.arraycopy(macByte, 0, MessageCreator.m256SK, 0, 6); //写入MAC
            byte[] code = new byte[10];
            if (sDevSecret == null || sDevSecret.equals("0")) {
                Arrays.fill(MessageCreator.m256SK, 6, 16, (byte) 0);
            } else {
                code = StringUtil.hexStringToBytes(sDevSecret);
                System.arraycopy(code, 0, MessageCreator.m256SK, 6, 10); //写入secretCode
                Arrays.fill(MessageCreator.m256SK, 16, 32, (byte) 0);
            }
        }

    }

    public void setTempMode(Boolean isOtaMode) {
        mTempMode = isOtaMode;
    }

    public void setTempMode(Boolean isOtaMode, int mode) {
        mTempMode = isOtaMode;
        mMode = mode;
    }

    /**
     * 打开搜索界面
     */
    public void startScanDevice() {

        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mContext.startActivity(enableIntent);
        } else {
            if (mMode == 0) {
//                Intent newIntent = new Intent(mContext, DeviceScanActivity.class);
//                newIntent.putExtra(ConstantUtil.NODE_ID, mNodeId);
//                mContext.startActivity(newIntent);
            } else {
                Intent result = new Intent();
                result.setAction(BleMsg.STR_RSP_OTA_MODE);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(result);
            }
        }

    }

    /**
     * @return
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleMsg.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleMsg.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleMsg.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BleMsg.STR_RSP_SERVER_DATA);
        intentFilter.addAction(BleMsg.ACTION_DOES_NOT_SUPPORT_UART);
        intentFilter.addAction(BleMsg.EXTRA_DATA_MSG_02);
        intentFilter.addAction(BleMsg.EXTRA_DATA_MSG_04);
        intentFilter.addAction(BleMsg.EXTRA_DATA_MSG_12);
        intentFilter.addAction(BleMsg.STR_RSP_MSG26_USERINFO);
        return intentFilter;
    }

    /**
     * 广播接收
     */
    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BleMsg.ACTION_GATT_CONNECTED)) {
                Log.d(TAG, "UART_CONNECT_MSG");
                mDevice.setState(Device.BLE_CONNECTED);
            }

            if (action.equals(BleMsg.ACTION_GATT_DISCONNECTED)) {
                mHandler.removeCallbacks(mRunnable);

                Log.d(TAG, "UART_DISCONNECT_MSG");
                MessageCreator.m128AK = null;
                MessageCreator.m256AK = null;
                mService.disconnect();
                mService.close();
                mDevice.setState(Device.BLE_DISCONNECTED);
                mIsConnected = false;
                mMode = mTempMode ? 1 : 0;
                mUserId = 0;
                if (mMode == 1) {
                    startScanDevice();
                }
                mDefaultDevice = null;
                mDefaultUser = null;
                mDefaultStatus = null;
                if (mConnectType == 2) {
                    mConnectType = 0;
                    return;
                }

               /* LogUtil.d(TAG, "active ble : " + mService.isActiveDisConnect());
                if (StringUtil.checkNotNull(mBleMac) && mDevice.getState() == Device.BLE_DISCONNECTED) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mService.connect(mBleMac);
                        }
                    }, 5000);
                }
                */
            }

            if (action.equals(BleMsg.ACTION_GATT_SERVICES_DISCOVERED)) {
                if (mConnectType == 2) {
                    mHandler.removeCallbacks(mRunnable);
                }
                if (mService != null) {
                    if (mMode == 0) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mDefaultDevice = DeviceInfoDao.getInstance(mContext).queryFirstData("device_default", true);
                                if (mDefaultDevice == null && mConnectType != 2) {
                                    mConnectType = 0;
                                }
                                LogUtil.d(TAG, "mConnectType : " + mConnectType + " mUserId : " + mUserId);
                                if (mConnectType == 2)
                                    mService.sendCmd05(mBleMac, mNodeId, mSn);
//                                else
//                                    mService.sendCmd01(mConnectType, mUserId);
                            }
                        }, 1000);
                    } else {
                        Intent result = new Intent();
                        result.putExtra(BleMsg.KEY_AK, (byte[]) null);
                        result.setAction(BleMsg.STR_RSP_SECURE_CONNECTION_OTA);
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(result);
                    }
                }
                mHandler.removeCallbacks(mRunnable);
            }

            if (action.equals(BleMsg.ACTION_DOES_NOT_SUPPORT_UART)) {
                Log.d(TAG, "ACTION_DOES_NOT_SUPPORT_UART");
                showMessage("Device doesn't support UART. Disconnecting");
//                mService.disconnect();
//                mIsConnected = false;
            }
            // 4.2.3 MSG 03
            if (action.equals(BleMsg.EXTRA_DATA_MSG_02)) {
                final byte[] random = intent.getByteArrayExtra(BleMsg.KEY_RANDOM);

                if (random != null && random.length != 0) {
//                    mService.sendCmd03(random);
                } else {
                    showMessage(mContext.getResources().getString(R.string.bt_connect_failed));
                }
            }

            // 4.2.3 MSG 04
            if (action.equals(BleMsg.EXTRA_DATA_MSG_04)) {
                mIsConnected = true;
                mHandler.removeCallbacks(mRunnable);
                Bundle extra = intent.getExtras();

                int battery = intent.getByteExtra(BleMsg.KEY_BAT_PERSCENT, (byte) 0);
                int userStatus = intent.getByteExtra(BleMsg.KEY_USER_STATUS, (byte) 0);
                int stStatus = intent.getByteExtra(BleMsg.KEY_SETTING_STATUS, (byte) 0);
                int unLockTime = intent.getByteExtra(BleMsg.KEY_UNLOCK_TIME, (byte) 0);
                byte[] syncUsers = intent.getByteArrayExtra(BleMsg.KEY_SYNC_USERS);
                byte[] userState = intent.getByteArrayExtra(BleMsg.KEY_USERS_STATE);
                byte[] tempSecret = intent.getByteArrayExtra(BleMsg.KEY_TMP_PWD_SK);

                mDevice.setBattery(battery);
                mDevice.setUserStatus(userStatus);
                mDevice.setStStatus(stStatus);
                mDevice.setUnLockTime(unLockTime);
                mDevice.setSyncUsers(syncUsers);
                mDevice.setTempSecret(tempSecret);

                LogUtil.d(TAG, "battery = " + battery + "\n" + "userStatus = " + userStatus + "\n" + " stStatus = " + stStatus + "\n" + " unLockTime = " + unLockTime);
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
                mDefaultDevice = DeviceInfoDao.getInstance(mContext).queryFirstData("device_default", true);

                if (mDefaultDevice != null) {
                    checkUserId(DeviceUserDao.getInstance(mContext).checkUserStatus(status1, mDefaultDevice.getDeviceNodeId(), 1)); //第一字节状态字
                    checkUserId(DeviceUserDao.getInstance(mContext).checkUserStatus(status2, mDefaultDevice.getDeviceNodeId(), 2));//第二字节状态字
                    checkUserId(DeviceUserDao.getInstance(mContext).checkUserStatus(status3, mDefaultDevice.getDeviceNodeId(), 3));//第三字节状态字
                    checkUserId(DeviceUserDao.getInstance(mContext).checkUserStatus(status4, mDefaultDevice.getDeviceNodeId(), 4));//第四字节状态字
                    DeviceUserDao.getInstance(mContext).checkUserState(mDefaultDevice.getDeviceNodeId(), userState); //开锁信息状态字

                    mDefaultDevice.setTempSecret(StringUtil.bytesToHexString(tempSecret));
                    DeviceInfoDao.getInstance(mContext).updateDeviceInfo(mDefaultDevice);

                    mService.sendCmd25(mDefaultDevice.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);

                    mDefaultUser = DeviceUserDao.getInstance(mContext).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId());
                    mDefaultStatus = DeviceStatusDao.getInstance(mContext).queryOrCreateByNodeId(mDefaultDevice.getDeviceNodeId());
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
                    DeviceStatusDao.getInstance(mContext).updateDeviceStatus(mDefaultStatus);
                }

                Intent result = new Intent();
                assert extra != null;
                result.putExtras(extra);
                result.setAction(BleMsg.STR_RSP_SECURE_CONNECTION);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(result);
            }

            if (action.equals(BleMsg.STR_RSP_MSG26_USERINFO)) {
                short userId = (short) intent.getSerializableExtra(BleMsg.KEY_SERIALIZABLE);
                if (userId == mDefaultDevice.getUserId()) {
                    byte[] userInfo = intent.getByteArrayExtra(BleMsg.KEY_USER_MSG);
                    LogUtil.d(TAG, "userInfo = " + Arrays.toString(userInfo));
                    mDefaultUser.setUserStatus(userInfo[0]);
                    DeviceUserDao.getInstance(mContext).updateDeviceUser(mDefaultUser);

                    DeviceKeyDao.getInstance(mContext).checkDeviceKey(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId(), userInfo[1], ConstantUtil.USER_PWD, "1");
                    DeviceKeyDao.getInstance(mContext).checkDeviceKey(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId(), userInfo[2], ConstantUtil.USER_NFC, "1");
                    DeviceKeyDao.getInstance(mContext).checkDeviceKey(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId(), userInfo[3], ConstantUtil.USER_FINGERPRINT, "1");
                    DeviceKeyDao.getInstance(mContext).checkDeviceKey(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId(), userInfo[4], ConstantUtil.USER_FINGERPRINT, "2");
                    DeviceKeyDao.getInstance(mContext).checkDeviceKey(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId(), userInfo[5], ConstantUtil.USER_FINGERPRINT, "3");
                    DeviceKeyDao.getInstance(mContext).checkDeviceKey(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId(), userInfo[6], ConstantUtil.USER_FINGERPRINT, "4");
                    DeviceKeyDao.getInstance(mContext).checkDeviceKey(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId(), userInfo[7], ConstantUtil.USER_FINGERPRINT, "5");

                    mDefaultDevice.setMixUnlock(userInfo[8]);
                    DeviceInfoDao.getInstance(mContext).updateDeviceInfo(mDefaultDevice);
                    mEndTime = System.currentTimeMillis();
                    LogUtil.d(TAG, "mStartTime - mEndTime = " + (mEndTime - mStartTime));
                }

            }
        }
    };

    public BleCardService getBleCardService() {
        return mService;
    }

    public void addUiListener(UiListener uiListener) {
        mUiListeners.add(uiListener);
        if (mService == null) {
            LogUtil.e(TAG, "service is null");
            return;
        }

        MainEngine mainEngine = mService.getMainEngine();
        if (mainEngine == null) {
            LogUtil.e(TAG, "mainEngine is null");
            return;
        }
        LogUtil.i(TAG, "add uilistenr !");
        mainEngine.addUiListener(uiListener);
    }

    //移除UI监听
    public void removeUiListener(UiListener uiListener) {
        mUiListeners.remove(uiListener);
        if (mService == null) {
            LogUtil.e(TAG, "service is null");
            return;
        }

        MainEngine mainEngine = mService.getMainEngine();
        if (mainEngine == null) {
            LogUtil.e(TAG, "mainEngine is null");
            return;
        }

        mainEngine.removeUiListener(uiListener);
    }

    /**
     * 获取蓝牙地址
     *
     * @return 蓝牙地址
     */
    public String getBleMac() {
        return mBleMac;
    }

    /**
     * 获取蓝牙连接状态
     *
     * @return
     */
    public boolean getServiceConnection() {
        return mIsConnected;
    }

    /**
     * 获取会话秘钥
     *
     * @return 会话秘钥
     */
    public byte[] getAK() {
        return MessageCreator.mIs128Code ? MessageCreator.m128AK : MessageCreator.m256AK;
    }

    /**
     * 停止蓝牙服务
     */
    public void stopService() {
        if (mService != null) {
            mService.disconnect();
            mService.close();
//                mService.stopSelf();
            mService = null;
            mBleMac = null;
            mConnectType = 0;
            mUserId = 0;
            mDevice.halt();
//                mContext.unbindService(mServiceConnection);
//                LogUtil.d(TAG, "mServiceConnection = " + (mServiceConnection.hashCode()));
            instance = null;
        }

    }

    //同步用户数据
    private void checkUserId(ArrayList<Short> userIds) {
        ArrayList<DeviceUser> users = DeviceUserDao.getInstance(mContext).queryDeviceUsers(mDefaultDevice.getDeviceNodeId());
        if (!userIds.isEmpty()) {
            for (DeviceUser user : users) {
                if (userIds.contains(user.getUserId())) {
                    DeviceUserDao.getInstance(mContext).delete(user);
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
     * 创建用户
     *
     * @param userId
     */
    private synchronized DeviceUser createDeviceUser(short userId, String path, int permission) {
        DeviceUser user = new DeviceUser();
        user.setDevNodeId(mDefaultDevice.getDeviceNodeId());
        user.setCreateTime(System.currentTimeMillis() / 1000);
        user.setUserId(userId);
        user.setUserPermission(permission);
        user.setUserStatus(ConstantUtil.USER_UNENABLE);
        if (permission == ConstantUtil.DEVICE_MASTER) {
            user.setUserName(mContext.getString(R.string.administrator) + userId);
        } else if (permission == ConstantUtil.DEVICE_MEMBER) {
            user.setUserName(mContext.getString(R.string.members) + userId);
        } else {
            user.setUserName(mContext.getString(R.string.tmp_user) + userId);
        }

        user.setQrPath(path);
        Log.d(TAG, "user = " + user.toString());
        DeviceUserDao.getInstance(mContext).insert(user);
        return user;
    }

    /**
     * 吐司提示
     *
     * @param msg 提示信息
     */
    private void showMessage(String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
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
