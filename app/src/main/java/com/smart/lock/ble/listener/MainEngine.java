package com.smart.lock.ble.listener;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.BleCardService;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.ble.provider.TimerProvider;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceLog;
import com.smart.lock.db.bean.DeviceStatus;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceLogDao;
import com.smart.lock.db.dao.DeviceStatusDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.entity.Device;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.SharedPreferenceUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.SystemUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Objects;

public class MainEngine implements BleMessageListener, DeviceStateCallback, Handler.Callback {

    private static final String TAG = MainEngine.class.getSimpleName();
    private Context mCtx; //上下文
    private BleCardService mService; //蓝牙服务
    private Device mDevice; //连接设备
    private DeviceInfo mDevInfo; //设备信息
    private DeviceUserDao mDeviceUserDao; //数据库用户管理者
    private DeviceInfoDao mDeviceInfoDao; //设备信息管理者
    private DeviceUser mDefaultUser; //当前用户信息
    private DeviceStatus mDefaultStatus; //设备属性信息
    private DeviceKeyDao mDeviceKeyDao;//设备信息管理者
    private ArrayList<UiListener> mUiListeners = new ArrayList(); //Ui监听集合
    private long mStartTime = 0, mEndTime = 0;//测试注册时间
    private static MainEngine mInstance;

    private static final int MSG_RECONNCT_BLE = 0; //重连
    private static final int MSG_POLLING_BLE = 4; //轮询
    private static final int MSG_ADD_USER_SUCCESS = 1; //添加设备成功
    private static final int MSG_REGISTER = 2;//注册蓝牙设备
    private static final int MSG_CHANGE_GATT_STATE = 5;//GATT State
    private static final String MSG_RECEIVER = "receiver_msg";
    private static final int RUN_ON_UI_THREAD = 3; //UI线程
    private Handler mHandler;
    private Message mCheckMsg = null;
    private final Object mStateLock = new Object();

    public MainEngine(Context context, BleCardService service) {
        mCtx = context;
        mService = service;
        mDeviceUserDao = DeviceUserDao.getInstance(context);
        mDeviceInfoDao = DeviceInfoDao.getInstance(context);
        mDeviceKeyDao = DeviceKeyDao.getInstance(context);
        mService.registerDevStateCb(this); //注册设备状态回调
        mHandler = new Handler(this);
    }


    public static MainEngine getInstance(Context context, BleCardService service) {
        if (mInstance == null) {
            synchronized (MainEngine.class) {
                if (mInstance == null) {
                    mInstance = new MainEngine(context, service);
                }
            }
        }
        return mInstance;
    }

    //添加UI监听
    public void addUiListener(UiListener uiListener) {
        ListIterator<UiListener> iterable = mUiListeners.listIterator();
        iterable.add(uiListener);
//        mUiListeners.add(uiListener);
    }

    //移除UI监听
    public void removeUiListener(UiListener uiListener) {
        Iterator<UiListener> iterable = mUiListeners.iterator();
        while (iterable.hasNext()) {
            if (iterable.next() == uiListener) {
                iterable.remove();
            }
        }
    }

    @Override
    public void onConnected() {
        sendMessage(Device.BLE_CONNECTED, null, 0);
    }

    @Override
    public void onDisconnected() {
        LogUtil.i(TAG, "onDisconnected : change dev state from : " + mDevice.getState() + " to: " + Device.BLE_DISCONNECTED);
        mDevice.setState(Device.BLE_DISCONNECTED);
        MessageCreator.m128AK = null;
        MessageCreator.m256AK = null;
//        if (mService != null) {
//            mService.disconnect();
//            mService.close();
//        }
        switch (mDevice.getConnectType()) {
            case Device.BLE_SCAN_QR_CONNECT_TYPE:
//                sendMessage(BleMsg.STATE_DISCONNECTED, null, 0);
//                break;
            case Device.BLE_SCAN_AUTH_CODE_CONNECT:
            case Device.BLE_OTHER_CONNECT_TYPE:
            case Device.BLE_SEARCH_DEV_CONNECT:
                sendMessage(BleMsg.STATE_DISCONNECTED, null, 0);

                DeviceInfo defaultDevice = mDeviceInfoDao.queryFirstData("device_default", true);
                if (defaultDevice == null) {
                    LogUtil.e(TAG, "defaultDevice is null");
                    halt();
                    sendMessage(BleMsg.STATE_DISCONNECTED, null, 0);
                    return;
                }
                if (mDevice != null && mDevice.getUserStatus() == ConstantUtil.USER_PAUSE || mDevice.isDisconnectBle() || mDevice.isBackGroundConnect()) {
                    return; //暂停的用户不需要重连
                }
                sendMessage(MSG_RECONNCT_BLE, null, 8 * 1000);
                break;
            case Device.BLE_SET_DEVICE_INFO_CONNECT_TYPE:
                sendMessage(BleMsg.STATE_DISCONNECTED, null, 0);
                halt();
                break;
//            case Device.BLE_SEARCH_DEV_CONNECT:
//                if (mDevice != null && mDevice.getUserStatus() == ConstantUtil.USER_PAUSE) {
//                    return;
//                }
//                sendMessage(BleMsg.STATE_DISCONNECTED, null, 0);
//                break;
            default:
                break;
        }

    }

    @Override
    public void onServicesDiscovered(int status) {
//        LogUtil.i(TAG, "onServicesDiscovered : device connect type is " + mDevice.getConnectType() + "\nmac =" + mDevice.getDevInfo().getBleMac());
        mDevice.setState(Device.BLE_CONNECTION);
        sendMessage(MSG_REGISTER, null, 600);
    }

    @Override
    public void onGattStateChanged(int state, int type) {
        Bundle extra = new Bundle();
        extra.putInt("gatt_state", state);
        sendMessage(MSG_CHANGE_GATT_STATE, extra, 0);
    }

    @Override
    public void onReceive(Message message, TimerProvider timer) {

        int type = message.getType();
        Log.i(TAG, "onReceive Message type : " + Message.getMessageTypeTag(type));

        Bundle bundle = new Bundle();
        bundle.putSerializable(MSG_RECEIVER, message);
        bundle.putSerializable("timer", timer);
        sendMessage(RUN_ON_UI_THREAD, bundle, 0);
    }

    @Override
    public String getListenerKey() {
        return TAG;
    }

    @Override
    public void halt() {
        mDevInfo = null;
        mDevice.halt();
        if (mCheckMsg != null) {
            mCheckMsg.recycle();
        }
    }

    public void close() {
        mDevInfo = null;
        if (mDevice != null)
            mDevice.halt();
        if (!mUiListeners.isEmpty()) {
            mUiListeners.clear();
        }
        if (mInstance != null)
            mInstance = null;
    }

    private void receiverLog(Bundle bundle) {
        final byte[] log = bundle.getByteArray(BleMsg.KEY_LOG);

        byte[] userId = new byte[2];
        byte[] logId = new byte[4];
        byte[] time = new byte[4];
        byte[] nodeId = new byte[8];
        byte[] state = new byte[1];
        byte[] type = new byte[1];
        byte[] lockId = new byte[1];

        System.arraycopy(log, 0, userId, 0, 2);
        System.arraycopy(log, 2, logId, 0, 4);
        System.arraycopy(log, 6, time, 0, 4);
        System.arraycopy(log, 10, nodeId, 0, 8);
        System.arraycopy(log, 18, state, 0, 1);
        System.arraycopy(log, 19, type, 0, 1);
        System.arraycopy(log, 20, lockId, 0, 1);

        StringUtil.exchange(nodeId);

        LogUtil.d(TAG, "userId = " + Arrays.toString(userId));
        LogUtil.d(TAG, "logId = " + Arrays.toString(logId));
        LogUtil.d(TAG, "time = " + Arrays.toString(time));
        LogUtil.d(TAG, "nodeId = " + Arrays.toString(nodeId));
        LogUtil.d(TAG, "state = " + Arrays.toString(state));
        LogUtil.d(TAG, "type = " + Arrays.toString(type));
        LogUtil.d(TAG, "lockId = " + Arrays.toString(lockId));

        DeviceLog devLog = new DeviceLog();
        devLog.setUserId(Short.parseShort(StringUtil.bytesToHexString(userId), 16));
        devLog.setLogId(Integer.parseInt(StringUtil.bytesToHexString(logId), 16));
        devLog.setLogTime(Long.parseLong(StringUtil.bytesToHexString(time), 16));
        devLog.setNodeId(StringUtil.bytesToHexString(nodeId));
        devLog.setLogState(state[0]);
        String keyName = "";

        devLog.setLogType(type[0]);
        if (type[0] == 0) {
            keyName = mCtx.getString(R.string.password);
        } else if (type[0] == 1) {
            keyName = mCtx.getString(R.string.fingerprint);
        } else if (type[0] == 2) {
            keyName = mCtx.getString(R.string.card);
        } else {
            keyName = mCtx.getString(R.string.remote);
        }

        devLog.setLockId(String.valueOf(lockId[0]));
        DeviceKey deviceKey = DeviceKeyDao.getInstance(mCtx).queryByLockId(StringUtil.bytesToHexString(nodeId), StringUtil.bytesToHexString(userId), String.valueOf(lockId[0]), type[0]);
        if (deviceKey != null)
            devLog.setKeyName(deviceKey.getKeyName());
        else if (type[0] == ConstantUtil.USER_REMOTE)
            devLog.setKeyName(keyName);
        else
            devLog.setKeyName(keyName + String.valueOf(lockId[0]));

        LogUtil.d(TAG, "devLog = " + devLog.toString());
        mDevice.setDisconnectBle(true);

        DeviceLogDao.getInstance(mCtx).insert(devLog);
    }

    /**
     * 注册当前设备
     *
     * @param device
     */
    public void registerDevice(Device device) {
        LogUtil.i(TAG, "device : " + (device == null ? true : device.toString()));
        mDevice = device;
        if (mDevice != null)
            mDevInfo = mDevice.getDevInfo();
    }

    /**
     * 扫描二维码注册
     *
     * @return 发送结果
     */
    public boolean scanQrRegister() {
        LogUtil.i(TAG, "scan qr register ble!");
        if (mDevInfo == null || mService == null) return false;
        return mService.sendCmd01(Device.BLE_SCAN_QR_CONNECT_TYPE, "0", mDevice.getDevInfo().getUserId(), mDevice.getDevInfo().getBleMac(), BleMsg.INT_DEFAULT_TIMEOUT);

    }

    /**
     * 普通连接方式注册
     *
     * @return 发送结果
     */
    public boolean otherRegister() {
        LogUtil.i(TAG, "other register ble!");
        if (mService == null) return false;
        if (mDevInfo != null) {
            String authCode = null;
            mDefaultUser = mDeviceUserDao.queryUser(mDevInfo.getDeviceNodeId(), mDevInfo.getUserId());
            if (mDefaultUser != null && mDefaultUser.getAuthCode() != null) {
                authCode = mDefaultUser.getAuthCode();
            } else if (mDevice.getTempAuthCode() != null) {
                authCode = StringUtil.bytesToHexString(mDevice.getTempAuthCode());
            }
            LogUtil.d(TAG, "authCode = " + authCode);
            if (StringUtil.checkNotNull(authCode)) {
                return mService.sendCmd01(Device.BLE_OTHER_CONNECT_TYPE, authCode, mDevInfo.getUserId(), mDevInfo.getBleMac(), BleMsg.INT_DEFAULT_TIMEOUT);
            } else {
                LogUtil.e(TAG, "authCode = null");
                return false;
            }
        }
        return false;
    }

    public boolean searchRegister() {
        LogUtil.i(TAG, "search register ble!");
        if (mService == null) return false;

        return mService.sendCmd01(Device.BLE_SCAN_QR_CONNECT_TYPE, "0", (short) 0, mDevInfo.getBleMac(), BleMsg.INT_DEFAULT_TIMEOUT); //搜索注册
    }

    /**
     * 设置设备信息注册
     *
     * @return 发送结果
     */
    public boolean setInfoRegister() {
        LogUtil.i(TAG, "set devinfo register ble!");
        if (mDevInfo == null || mService == null) return false;
        String bleMac = mDevInfo.getBleMac().replace(mCtx.getString(R.string.colon), "");
        String nodeId = mDevInfo.getDeviceNodeId();
        String sn = mDevInfo.getDeviceSn();
        return mService.sendCmd05(bleMac, nodeId, sn);
    }

    private void registerCallBack(Message msg) {
        Bundle bundle = msg.getData();
        LogUtil.i(TAG, "receiver msg 04,check lock info!");
        int battery = bundle.getByte(BleMsg.KEY_BAT_PERSCENT, (byte) 0);
        int userStatus = bundle.getByte(BleMsg.KEY_USER_STATUS, (byte) 0);
        int stStatus = bundle.getByte(BleMsg.KEY_SETTING_STATUS, (byte) 0);
        int unLockTime = bundle.getByte(BleMsg.KEY_UNLOCK_TIME, (byte) 0);
        byte[] syncUsers = bundle.getByteArray(BleMsg.KEY_SYNC_USERS);
        byte[] userState = bundle.getByteArray(BleMsg.KEY_USERS_STATE);
        byte[] tempSecret = bundle.getByteArray(BleMsg.KEY_TMP_PWD_SK);
        byte[] powerSave = StringUtil.bytesReverse(Objects.requireNonNull(bundle.getByteArray(BleMsg.KEY_POWER_SAVE))); // 字节翻转，结束时间在前
        LogUtil.d(TAG, "battery = " + battery + "\n" + "userStatus = " + userStatus + "\n" + " stStatus = " + stStatus + "\n" + " unLockTime = " + unLockTime);
        LogUtil.d(TAG, "syncUsers = " + StringUtil.bytesToHexString(syncUsers, ":"));
        LogUtil.d(TAG, "userState = " + Arrays.toString(userState));
        LogUtil.d(TAG, "tempSecret = " + Arrays.toString(tempSecret));
        LogUtil.d(TAG, "powerSave = " + Arrays.toString(powerSave));

        mDevice.setBattery(battery);
        mDevice.setUserStatus(userStatus);
        mDevice.setStStatus(stStatus);
        mDevice.setUnLockTime(unLockTime);
        mDevice.setSyncUsers(syncUsers);
        mDevice.setTempSecret(tempSecret);
        synchronized (this.mStateLock) {
            if (userStatus == ConstantUtil.USER_PAUSE) {
                if (mDefaultUser != null) {
                    mDefaultUser.setUserStatus(userStatus);
                    DeviceUserDao.getInstance(mCtx).updateDeviceUser(mDefaultUser);
                }
                for (UiListener uiListener : mUiListeners) {
                    uiListener.dispatchUiCallback(msg, mDevice, BleMsg.USER_PAUSE);
                }
                return;
            }
        }
        byte[] buf = new byte[4];
        System.arraycopy(Objects.requireNonNull(syncUsers), 0, buf, 0, 4);
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

        if (mDevInfo != null && (mDevice.getConnectType() == Device.BLE_OTHER_CONNECT_TYPE
                || mDevice.getConnectType() == Device.BLE_SCAN_AUTH_CODE_CONNECT)) {
            mDefaultUser = mDeviceUserDao.queryOrCreateByNodeId(mDevInfo.getDeviceNodeId(), mDevInfo.getUserId(), StringUtil.bytesToHexString(mDevice.getTempAuthCode()));
            mDefaultStatus = DeviceStatusDao.getInstance(mCtx).queryOrCreateByNodeId(mDevInfo.getDeviceNodeId());

            checkUserId(mDeviceUserDao.checkUserStatus(status1, mDevInfo.getDeviceNodeId(), 1)); //第一字节状态字
            checkUserId(mDeviceUserDao.checkUserStatus(status2, mDevInfo.getDeviceNodeId(), 2));//第二字节状态字
            checkUserId(mDeviceUserDao.checkUserStatus(status3, mDevInfo.getDeviceNodeId(), 3));//第三字节状态字
            checkUserId(mDeviceUserDao.checkUserStatus(status4, mDevInfo.getDeviceNodeId(), 4));//第四字节状态字
            mDeviceUserDao.checkUserState(mDevInfo.getDeviceNodeId(), userState); //开锁信息状态字

            mDevInfo.setTempSecret(StringUtil.bytesToHexString(tempSecret)); //设置临时秘钥
            mDeviceInfoDao.updateDeviceInfo(mDevInfo); //数据库更新锁信息
            getUserInfo();

            if (mDefaultStatus != null) {
                mDefaultStatus.setBattery(battery);
                mDefaultStatus.setUpdateTime(System.currentTimeMillis() / 1000);

                if ((stStatus & 1) == 1) {  //常开功能
                    mDefaultStatus.setNormallyOpen(true);
                } else {
                    mDefaultStatus.setNormallyOpen(false);
                }
                if ((stStatus & 2) == 2) {  //语音提示
                    mDefaultStatus.setVoicePrompt(true);
                } else {
                    mDefaultStatus.setVoicePrompt(false);
                }
                if ((stStatus & 4) == 4) {  //智能锁芯
                    mDefaultStatus.setIntelligentLockCore(true);
                } else {
                    mDefaultStatus.setIntelligentLockCore(false);
                }
                if ((stStatus & 8) == 8) {  //防撬开关
                    mDefaultStatus.setAntiPrizingAlarm(true);
                } else {
                    mDefaultStatus.setAntiPrizingAlarm(false);
                }
                if ((stStatus & 16) == 16) {    //组合开锁
                    mDefaultStatus.setCombinationLock(true);
                } else {
                    mDefaultStatus.setCombinationLock(false);
                }
                if ((stStatus & 32) == 32) {    //支持M1卡
                    mDefaultStatus.setM1Support(true);
                } else {
                    mDefaultStatus.setM1Support(false);
                }
                if ((stStatus & 64) == 64) {    //蓝牙广播
                    mDefaultStatus.setBroadcastNormallyOpen(true);
                } else {
                    mDefaultStatus.setBroadcastNormallyOpen(false);
                }
                mDefaultStatus.setRolledBackTime(unLockTime);
                // 获取省电时间段
                if (Arrays.equals(powerSave, new byte[]{0, 0, 0, 0, 0, 0, 0, 0})) {
                    mDefaultStatus.setPowerSavingStartTime(ConstantUtil.INVALID_POWER_SAVE_TIME); //无效时间 表示关闭
                    mDefaultStatus.setPowerSavingEndTime(ConstantUtil.INVALID_POWER_SAVE_TIME); //无效时间 表示关闭
                } else {
                    byte[] startPowerSave = new byte[4];
                    byte[] endPowerSave = new byte[4];
                    System.arraycopy(powerSave, 4, startPowerSave, 0, 4);
                    System.arraycopy(powerSave, 0, endPowerSave, 0, 4);
                    String startTimeStr = DateTimeUtil.stampToDate(StringUtil.byte2Int(startPowerSave) + "000");
                    String endTimeStr = DateTimeUtil.stampToDate(StringUtil.byte2Int(endPowerSave) + "000");
                    LogUtil.d(TAG, "powerSave = " + '\n' +
                            "startStamp = " + startTimeStr + "\n" +
                            "endStamp = " + endTimeStr);
                    mDefaultStatus.setPowerSavingStartTime(Integer.valueOf(startTimeStr.substring(11, 13)) * 100 + Integer.valueOf(startTimeStr.substring(14, 16)));
                    mDefaultStatus.setPowerSavingEndTime(Integer.valueOf(endTimeStr.substring(11, 13)) * 100 + Integer.valueOf(endTimeStr.substring(14, 16)));
                }
                DeviceStatusDao.getInstance(mCtx).updateDeviceStatus(mDefaultStatus);
            }
        } else {
            mCheckMsg = new Message();
            Bundle extra = mCheckMsg.getData();

            extra.putByte(BleMsg.KEY_BAT_PERSCENT, bundle.getByte(BleMsg.KEY_BAT_PERSCENT, (byte) 0));
            extra.putByte(BleMsg.KEY_USER_STATUS, bundle.getByte(BleMsg.KEY_USER_STATUS, (byte) 0));
            extra.putByte(BleMsg.KEY_SETTING_STATUS, bundle.getByte(BleMsg.KEY_SETTING_STATUS, (byte) 0));
            extra.putByte(BleMsg.KEY_UNLOCK_TIME, bundle.getByte(BleMsg.KEY_UNLOCK_TIME, (byte) 0));
            extra.putByteArray(BleMsg.KEY_TMP_PWD_SK, bundle.getByteArray(BleMsg.KEY_TMP_PWD_SK));
            extra.putByteArray(BleMsg.KEY_SYNC_USERS, bundle.getByteArray(BleMsg.KEY_SYNC_USERS));
            extra.putByteArray(BleMsg.KEY_USERS_STATE, bundle.getByteArray(BleMsg.KEY_USERS_STATE));
            extra.putByteArray(BleMsg.KEY_POWER_SAVE, bundle.getByteArray(BleMsg.KEY_POWER_SAVE));
        }
        mDevice.setState(Device.BLE_CONNECTED);
        ListIterator<UiListener> iterator = mUiListeners.listIterator();
        synchronized (this.mStateLock) {
            while (iterator.hasNext()) {
                iterator.next().dispatchUiCallback(msg, mDevice, BleMsg.REGISTER_SUCCESS);
            }
        }
    }

    private void checkDevInfo(Message msg) {
        Bundle bundle = msg.getData();

        if (bundle == null) return;
        LogUtil.d(TAG, "bundle : " + bundle.toString());
        LogUtil.i(TAG, "check lock info!");
        int battery = bundle.getByte(BleMsg.KEY_BAT_PERSCENT, (byte) 0);
        int stStatus = bundle.getByte(BleMsg.KEY_SETTING_STATUS, (byte) 0);
        int unLockTime = bundle.getByte(BleMsg.KEY_UNLOCK_TIME, (byte) 0);
        byte[] syncUsers = bundle.getByteArray(BleMsg.KEY_SYNC_USERS);
        byte[] userState = bundle.getByteArray(BleMsg.KEY_USERS_STATE);
        byte[] tempSecret = bundle.getByteArray(BleMsg.KEY_TMP_PWD_SK);
        byte[] powerSave = StringUtil.bytesReverse(Objects.requireNonNull(bundle.getByteArray(BleMsg.KEY_POWER_SAVE))); // 字节翻转，结束时间在前

        byte[] buf = new byte[4];
        System.arraycopy(Objects.requireNonNull(syncUsers), 0, buf, 0, 4);
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

        if (mDevInfo != null) {
            mDefaultUser = mDeviceUserDao.queryOrCreateByNodeId(mDevInfo.getDeviceNodeId(), mDevInfo.getUserId(), StringUtil.bytesToHexString(mDevice.getTempAuthCode()));
            mDefaultStatus = DeviceStatusDao.getInstance(mCtx).queryOrCreateByNodeId(mDevInfo.getDeviceNodeId());

            checkUserId(mDeviceUserDao.checkUserStatus(status1, mDevInfo.getBleMac(), 1)); //第一字节状态字
            checkUserId(mDeviceUserDao.checkUserStatus(status2, mDevInfo.getBleMac(), 2));//第二字节状态字
            checkUserId(mDeviceUserDao.checkUserStatus(status3, mDevInfo.getBleMac(), 3));//第三字节状态字
            checkUserId(mDeviceUserDao.checkUserStatus(status4, mDevInfo.getBleMac(), 4));//第四字节状态字
            mDeviceUserDao.checkUserState(mDevInfo.getBleMac(), userState); //开锁信息状态字

            mDevInfo.setTempSecret(StringUtil.bytesToHexString(tempSecret)); //设置临时秘钥
            mDeviceInfoDao.updateDeviceInfo(mDevInfo); //数据库更新锁信息
            getUserInfo();

            if (mDefaultStatus != null) {
                mDefaultStatus.setBattery(battery);
                mDefaultStatus.setUpdateTime(System.currentTimeMillis() / 1000);

                if ((stStatus & 1) == 1) {  //常开功能
                    mDefaultStatus.setNormallyOpen(true);
                } else {
                    mDefaultStatus.setNormallyOpen(false);
                }
                if ((stStatus & 2) == 2) {  //语音提示
                    mDefaultStatus.setVoicePrompt(true);
                } else {
                    mDefaultStatus.setVoicePrompt(false);
                }
                if ((stStatus & 4) == 4) {  //智能锁芯
                    mDefaultStatus.setIntelligentLockCore(true);
                } else {
                    mDefaultStatus.setIntelligentLockCore(false);
                }
                if ((stStatus & 8) == 8) {  //防撬开关
                    mDefaultStatus.setAntiPrizingAlarm(true);
                } else {
                    mDefaultStatus.setAntiPrizingAlarm(false);
                }
                if ((stStatus & 16) == 16) {    //组合开锁
                    mDefaultStatus.setCombinationLock(true);
                } else {
                    mDefaultStatus.setCombinationLock(false);
                }
                if ((stStatus & 32) == 32) {    //支持M1卡
                    mDefaultStatus.setM1Support(true);
                } else {
                    mDefaultStatus.setM1Support(false);
                }
                if ((stStatus & 64) == 64) {    //蓝牙广播
                    mDefaultStatus.setBroadcastNormallyOpen(true);
                } else {
                    mDefaultStatus.setBroadcastNormallyOpen(false);
                }
                mDefaultStatus.setRolledBackTime(unLockTime);
                // 获取省电时间段
                if (Arrays.equals(powerSave, new byte[]{0, 0, 0, 0, 0, 0, 0, 0})) {
                    mDefaultStatus.setPowerSavingStartTime(ConstantUtil.INVALID_POWER_SAVE_TIME); //无效时间 表示关闭
                    mDefaultStatus.setPowerSavingEndTime(ConstantUtil.INVALID_POWER_SAVE_TIME); //无效时间 表示关闭
                } else {
                    byte[] startPowerSave = new byte[4];
                    byte[] endPowerSave = new byte[4];
                    System.arraycopy(powerSave, 4, startPowerSave, 0, 4);
                    System.arraycopy(powerSave, 0, endPowerSave, 0, 4);
                    String startTimeStr = DateTimeUtil.stampToDate(StringUtil.byte2Int(startPowerSave) + "000");
                    String endTimeStr = DateTimeUtil.stampToDate(StringUtil.byte2Int(endPowerSave) + "000");

                    LogUtil.d(TAG, "powerSave = " + '\n' + "startStamp = " + startTimeStr + "\n" + "endStamp = " + endTimeStr);

                    mDefaultStatus.setPowerSavingStartTime(Integer.valueOf(startTimeStr.substring(11, 13)) * 100 + Integer.valueOf(startTimeStr.substring(14, 16)));
                    mDefaultStatus.setPowerSavingEndTime(Integer.valueOf(endTimeStr.substring(11, 13)) * 100 + Integer.valueOf(endTimeStr.substring(14, 16)));
                }
                DeviceStatusDao.getInstance(mCtx).updateDeviceStatus(mDefaultStatus);
            }
        }
    }

    /**
     * 获取用户信息
     */
    private void getUserInfo() {
        LogUtil.i(TAG, "send msg 25!");
        if (mDevInfo != null) {
            mService.sendCmd25(mDevInfo.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
        } else
            LogUtil.e(TAG, "devInfo is null!");

    }

    /**
     * 获取注册类型
     *
     * @return 注册类型
     */
    public byte getRegisterType() {
        return mDevice.getConnectType();
    }

    //同步用户数据
    private void checkUserId(ArrayList<Short> userIds) {
        ArrayList<DeviceUser> users = DeviceUserDao.getInstance(mCtx).queryDeviceUsers(mDevInfo.getDeviceNodeId());
        if (!userIds.isEmpty()) {
            for (DeviceUser user : users) {
                if (userIds.contains(user.getUserId())) { //数据库是否包含不同的userid，如果包含表示门锁没有该userId,删除对应的用户
                    DeviceUserDao.getInstance(mCtx).delete(user);
                    userIds.remove((Short) user.getUserId());
                }
            }
            for (Short userId : userIds) {
                createDeviceUser(userId, null, null); //其余不同则创建对应的用户
            }
        }
    }

    /**
     * 创建用户
     *
     * @param userId
     */
    private synchronized DeviceUser createDeviceUser(short userId, String path, String authCode) {
        LogUtil.d(TAG, "Create User" + userId);
        DeviceUser user = new DeviceUser();
        user.setDevNodeId(mDevInfo.getDeviceNodeId());
        user.setCreateTime(System.currentTimeMillis() / 1000);
        user.setUserId(userId);
        user.setUserStatus(ConstantUtil.USER_UNENABLE);
        user.setAuthCode(authCode);
        LogUtil.d(TAG, "userId : " + userId);
        if (userId < 101) {
            user.setUserPermission(ConstantUtil.DEVICE_MASTER);
            user.setUserName(mCtx.getString(R.string.administrator) + userId);
        } else if (userId < 201) {
            user.setUserPermission(ConstantUtil.DEVICE_MEMBER);
            user.setUserName(mCtx.getString(R.string.members) + userId);
        } else {
            user.setUserPermission(ConstantUtil.DEVICE_TEMP);
            user.setUserName(mCtx.getString(R.string.tmp_user) + userId);
        }

        user.setQrPath(path);
        DeviceUserDao.getInstance(mCtx).insert(user);
        return user;
    }

    @Override
    public boolean handleMessage(android.os.Message msg) {

        switch (msg.what) {
            case RUN_ON_UI_THREAD:
                Message message = (Message) msg.getData().getSerializable(MSG_RECEIVER);
                TimerProvider timer = (TimerProvider) msg.getData().getSerializable("timer");
                dispatchMessage(message, timer);
                break;
            case MSG_POLLING_BLE:
                LogUtil.i(TAG, "Ble polling!");
                if (mService == null || mDevice == null || mDevInfo == null) {
                    LogUtil.e(TAG, "the service or dev is null!");
                    break;
                }

                if (mDevice.getConnectType() == Device.BLE_SCAN_QR_CONNECT_TYPE && mDevInfo.getUserId() == 0) {
                    LogUtil.e(TAG, "suarch dev!");
                    break;
                }

                if (mDevice.getState() != Device.BLE_DISCONNECTED) break; //设备状态不是非连接，不需要自动连接

                boolean result = mService.connect(mDevice, mDevInfo.getBleMac());
                LogUtil.d(TAG, "result : " + result);
                if (result) mDevice.setState(Device.BLE_CONNECTION);
                for (UiListener uiListener : mUiListeners) {
                    uiListener.reConnectBle(mDevice);
                }
                sendMessage(MSG_POLLING_BLE, null, 120 * 1000);
                break;
            case MSG_RECONNCT_BLE:
                if (mService == null || mDevice == null || mDevInfo == null || mDevice.isDisconnectBle() || mDevice.isBackGroundConnect()) {
                    LogUtil.e(TAG, "the service or dev is null!");
                    if (mService == null) {
                        LogUtil.d(TAG, "mService");
                    }
                    if (mDevice == null) {
                        LogUtil.d(TAG, "mDevice");
                    }
                    if (mDevInfo == null) {
                        LogUtil.d(TAG, "mDevInfo");
                    }
                    if (mDevice.isDisconnectBle()) {
                        LogUtil.d(TAG, "true");
                    }
                    if (mDevice.isBackGroundConnect()) {
                        LogUtil.d(TAG, "Back true");
                    }
                    break;
                }

                if (mDevice.getConnectType() == Device.BLE_SCAN_QR_CONNECT_TYPE && mDevInfo.getUserId() == 0) {
                    LogUtil.e(TAG, "suarch dev!");
                    break;
                }
                LogUtil.i(TAG, "test reconnect device" + "mDevInfo.getBleMac() : " + mDevInfo.getBleMac());
                if (mDevice.getState() == Device.BLE_DISCONNECTED) { //设备状态不是非连接，不需要自动连接
                    for (UiListener uiListener : mUiListeners) {
                        uiListener.reConnectBle(mDevice);
                    }
                }
                break;
            case MSG_ADD_USER_SUCCESS:
                if (mCheckMsg != null) checkDevInfo(mCheckMsg);
                break;
            case MSG_CHANGE_GATT_STATE:
                int state = msg.getData().getInt("gatt_state", -1);
                for (UiListener uiListener : mUiListeners) {
                    uiListener.deviceStateChange(mDevice, state);
                }
                break;

            case MSG_REGISTER:
                boolean isConnect = false;
                switch (mDevice.getConnectType()) {
                    case Device.BLE_SCAN_QR_CONNECT_TYPE:
                        isConnect = scanQrRegister();
                        break;
                    case Device.BLE_OTHER_CONNECT_TYPE:
                    case Device.BLE_SCAN_AUTH_CODE_CONNECT:
                        isConnect = otherRegister();
                        break;
                    case Device.BLE_SET_DEVICE_INFO_CONNECT_TYPE:
                        isConnect = setInfoRegister();
                        break;
                    case Device.BLE_SEARCH_DEV_CONNECT:
                        isConnect = searchRegister();
                        break;
                    default:
                        isConnect = scanQrRegister();
                        break;
                }
                if (isConnect) {
                    sendMessage(BleMsg.GATT_SERVICES_DISCOVERED, null, 0);
                } else {
                    if (mService != null) mService.disconnect();
                }
                break;
            case BleMsg.STATE_DISCONNECTED:
                for (UiListener uiListener : mUiListeners) {
                    uiListener.deviceStateChange(mDevice, BleMsg.STATE_DISCONNECTED);
                }
                break;
            case BleMsg.STATE_CONNECTED:
                for (UiListener uiListener : mUiListeners) {
                    uiListener.deviceStateChange(mDevice, BleMsg.STATE_CONNECTED);
                }
                break;
            case BleMsg.GATT_SERVICES_DISCOVERED:
                for (UiListener uiListener : mUiListeners) {
                    uiListener.deviceStateChange(mDevice, BleMsg.GATT_SERVICES_DISCOVERED);
                }
                break;
            default:
                break;
        }
        return true;
    }

    private synchronized void dispatchMessage(Message message, TimerProvider timer) {
        LogUtil.d(TAG, message.toString());
        try {
            int type = message.getType();
            Bundle extra = message.getData();
            int exception = message.getException();
            if (exception != Message.EXCEPTION_NORMAL) {
                LogUtil.e(TAG, "msg exception : " + message.toString());
                if (mService != null) mService.disconnect();
                for (UiListener uiListener : mUiListeners) {
                    uiListener.sendFailed(message); //发送消息失败回调
                }
            }

            switch (type) {
                case Message.TYPE_BLE_RECEIVER_CMD_02:
                    parseMsg(message);
                    break;
//                    LogUtil.i(TAG, "receiver msg 02,send msg 03!");
//                    final byte[] random = extra.getByteArray(BleMsg.KEY_RANDOM);
//                    if (random != null && random.length != 0) {
//                        mService.sendCmd03(random, BleMsg.INT_DEFAULT_TIMEOUT);
//                    }

                case Message.TYPE_BLE_RECEIVER_CMD_04:
                    registerCallBack(message);
                    break;
                case Message.TYPE_BLE_RECEIVER_CMD_12:
                    DeviceUser addUser = (DeviceUser) extra.getSerializable(BleMsg.KEY_SERIALIZABLE);
                    if (addUser == null || !(addUser.getUserPermission() == BleMsg.TYPE_SCAN_QR_ADD_MASTER ||
                            addUser.getUserPermission() == BleMsg.TYPT_NO_SCAN_QR_ADD_USER)) {
                        for (UiListener uiListener : mUiListeners) {
                            uiListener.dispatchUiCallback(message, mDevice, -1);
                        }
                        return;
                    }
                    byte[] authCode = extra.getByteArray(BleMsg.KEY_AUTH_CODE);

                    byte[] addUserIdBuf = new byte[2];
                    byte[] nodeIdBuf = new byte[8];
                    byte[] bleMacBuf = new byte[6];
                    byte[] randCodeBuf = new byte[10];
                    byte[] timeBuf = new byte[4];

                    System.arraycopy(authCode, 0, addUserIdBuf, 0, 2);
                    System.arraycopy(authCode, 2, nodeIdBuf, 0, 8);
                    System.arraycopy(authCode, 10, bleMacBuf, 0, 6);
                    System.arraycopy(authCode, 16, randCodeBuf, 0, 10);
                    System.arraycopy(authCode, 26, timeBuf, 0, 4);


                    String addUserId = StringUtil.bytesToHexString(addUserIdBuf);
                    StringUtil.exchange(nodeIdBuf);
                    String nodeId = StringUtil.bytesToHexString(nodeIdBuf);
                    String bleMac = StringUtil.bytesToHexString(bleMacBuf);

                    String time = StringUtil.bytesToHexString(timeBuf);
                    String randCode = StringUtil.bytesToHexString(randCodeBuf);

                    mDevInfo.setActivitedTime(Long.parseLong(time, 16));
                    mDevInfo.setConnectType(false);
                    mDevInfo.setUserId(Short.parseShort(addUserId, 16));
                    mDevInfo.setDeviceNodeId(nodeId);
                    mDevInfo.setBleMac(StringUtil.getMacAdr(bleMac));
                    mDevInfo.setNodeType(ConstantUtil.SMART_LOCK);
                    mDevInfo.setDeviceDate(System.currentTimeMillis() / 1000);
                    DeviceInfoDao.getInstance(mCtx).setNoDefaultDev();
                    mDevInfo.setDeviceDefault(true);
                    mDevInfo.setDeviceName(mCtx.getString(R.string.lock_default_name) + String.valueOf(DeviceInfoDao.getInstance(mCtx).queryCount() + 1));
                    mDevInfo.setDeviceSecret(randCode);
                    mDeviceInfoDao.insert(mDevInfo);

                    short sUserId = Short.parseShort(addUserId, 16);

                    createDeviceUser(sUserId, null, StringUtil.bytesToHexString(authCode));

                    sendMessage(MSG_ADD_USER_SUCCESS, null, 0);
                    break;
                case Message.TYPE_BLE_RECEIVER_CMD_1A:
                case Message.TYPE_BLE_RECEIVER_CMD_1C:
                case Message.TYPE_BLE_RECEIVER_CMD_1E:
                case Message.TYPE_BLE_RECEIVER_CMD_16:
                case Message.TYPE_BLE_RECEIVER_CMD_0E:
                case Message.TYPE_BLE_RECEIVER_CMD_62:
                    ListIterator<UiListener> iterator = mUiListeners.listIterator();
                    synchronized (this.mStateLock) {
                        while (iterator.hasNext()) {
                            iterator.next().dispatchUiCallback(message, mDevice, -1);
                        }
                    }
                    break;
                case Message.TYPE_BLE_RECEIVER_CMD_18:
                    byte[] seconds = extra.getByteArray(BleMsg.KEY_TIME_OUT);

                    if (seconds != null && timer != null) {
                        long timeOut = seconds[0] * 1000 + 5000; //APP侧比芯片侧时长多5s，避免重复remove
                        LogUtil.i(TAG, "receiver msg 18,reset timer : " + timeOut);
                        timer.reSetTimeOut(timeOut);
                    }
                    break;
                case Message.TYPE_BLE_RECEIVER_CMD_2E:
                    iterator = mUiListeners.listIterator();
                    synchronized (this.mStateLock) {
                        while (iterator.hasNext()) {
                            iterator.next().dispatchUiCallback(message, mDevice, -1);
                        }
                    }
                    break;
                case Message.TYPE_BLE_RECEIVER_CMD_26:
                    LogUtil.i(TAG, "receiver msg 26,check device key!");
                    short userId = (short) extra.getSerializable(BleMsg.KEY_SERIALIZABLE);
                    if (userId == mDevInfo.getUserId()) {
                        byte[] userInfo = extra.getByteArray(BleMsg.KEY_USER_MSG);
                        LogUtil.d(TAG, "user info : " + Arrays.toString(userInfo));

                        mDeviceKeyDao.checkDeviceKey(mDevInfo.getDeviceNodeId(), mDevInfo.getUserId(), userInfo[1], ConstantUtil.USER_PWD, "1");
                        mDeviceKeyDao.checkDeviceKey(mDevInfo.getDeviceNodeId(), mDevInfo.getUserId(), userInfo[2], ConstantUtil.USER_NFC, "1");
                        mDeviceKeyDao.checkDeviceKey(mDevInfo.getDeviceNodeId(), mDevInfo.getUserId(), userInfo[3], ConstantUtil.USER_FINGERPRINT, "1");
                        mDeviceKeyDao.checkDeviceKey(mDevInfo.getDeviceNodeId(), mDevInfo.getUserId(), userInfo[4], ConstantUtil.USER_FINGERPRINT, "2");
                        mDeviceKeyDao.checkDeviceKey(mDevInfo.getDeviceNodeId(), mDevInfo.getUserId(), userInfo[5], ConstantUtil.USER_FINGERPRINT, "3");
                        mDeviceKeyDao.checkDeviceKey(mDevInfo.getDeviceNodeId(), mDevInfo.getUserId(), userInfo[6], ConstantUtil.USER_FINGERPRINT, "4");
                        mDeviceKeyDao.checkDeviceKey(mDevInfo.getDeviceNodeId(), mDevInfo.getUserId(), userInfo[7], ConstantUtil.USER_FINGERPRINT, "5");

                        byte[] authTime = new byte[4];
                        System.arraycopy(userInfo, 8, authTime, 0, 4);

                        String auth = setAuthCode(authTime);
//                        mDevInfo.setMixUnlock(userInfo[8]);
//                        mDeviceInfoDao.updateDeviceInfo(mDevInfo);
                        mDefaultUser = mDeviceUserDao.queryUser(mDevInfo.getDeviceNodeId(), mDevInfo.getUserId());
                        mDefaultUser.setAuthCode((auth.equals("0")) ? null : auth);
                        mDefaultUser.setUserStatus(userInfo[0]);
                        mDeviceUserDao.updateDeviceUser(mDefaultUser);
                        mEndTime = System.currentTimeMillis();
                        LogUtil.d(TAG, "mStartTime - mEndTime = " + (mEndTime - mStartTime));
                    } else {
                        for (UiListener uiListener : mUiListeners) {
                            uiListener.dispatchUiCallback(message, mDevice, -1);
                        }
                    }

                    if (mDevice.getConnectType() == Device.BLE_SCAN_QR_CONNECT_TYPE
                            || mDevice.getConnectType() == Device.BLE_SEARCH_DEV_CONNECT)
                        for (UiListener uiListener : mUiListeners) {
                            uiListener.addUserSuccess(mDevice);
                        }
                    break;
                case Message.TYPE_BLE_RECEIVER_CMD_32:
                    LogUtil.i(TAG, "receiver 32!");
                    receiverLog(extra);
//                    for (UiListener uiListener : mUiListeners) {
//                        uiListener.dispatchUiCallback(message, mDevice, BleMsg.RECEIVER_LOGS);
//                    }
                    iterator = mUiListeners.listIterator();
                    while (iterator.hasNext()) {
                        iterator.next().dispatchUiCallback(message, mDevice, -1);
                    }
                    break;
                case Message.TYPE_BLE_RECEIVER_CMD_3E:
                    LogUtil.i(TAG, "receiver 3e!");
//                    for (UiListener uiListener : mUiListeners) {
//                        uiListener.dispatchUiCallback(message, mDevice, -1);
//                    }
                    iterator = mUiListeners.listIterator();
                    while (iterator.hasNext()) {
                        iterator.next().dispatchUiCallback(message, mDevice, -1);
                    }
                    break;
                default:
                    Log.w(TAG, "Message type : " + type + " can not be handler");
                    break;
            }

        } finally {
            message.recycle();
        }
    }

    //解析MSG 02
    private void parseMsg(Message msg) {
        Bundle extra = msg.getData();
        final byte[] random = extra.getByteArray(BleMsg.KEY_RANDOM);
        final byte[] buf = extra.getByteArray(BleMsg.RECEIVER_DATA);

        LogUtil.d(TAG, "random : " + StringUtil.bytesToHexString(random));
        LogUtil.d(TAG, "MessageCreator.pwdRandom : " + StringUtil.bytesToHexString(MessageCreator.pwdRandom));

        if (StringUtil.memcmp(random, MessageCreator.pwdRandom, 16)) {
            byte[] respRandom = new byte[16];
            byte[] sn = new byte[18];
            if (buf == null) {
                return;
            }
            System.arraycopy(buf, 16, respRandom, 0, 16);
            if (MessageCreator.mIs128Code) {
                if (MessageCreator.m128AK == null) {
                    MessageCreator.m128AK = new byte[16];
                }
                System.arraycopy(buf, 32, MessageCreator.m128AK, 0, 16);
                System.arraycopy(buf, 48, sn, 0, 18);
            } else {
                if (MessageCreator.m256AK == null) {
                    MessageCreator.m256AK = new byte[32];
                }
                System.arraycopy(buf, 32, MessageCreator.m256AK, 0, 32);
                System.arraycopy(buf, 64, sn, 0, 18);
            }
            LogUtil.d(TAG, "check sn : " + SharedPreferenceUtil.getInstance(mCtx).readBoolean(ConstantUtil.CHECK_DEVICE_SN));
            if (SharedPreferenceUtil.getInstance(mCtx).readBoolean(ConstantUtil.CHECK_DEVICE_SN)) {
                mService.sendCmd03(respRandom, BleMsg.INT_DEFAULT_TIMEOUT);
            } else {
                if (compareSn(sn)) {
                    mService.sendCmd03(respRandom, BleMsg.INT_DEFAULT_TIMEOUT);
                } else {
                    showMessage(mCtx.getString(R.string.sn_warning));
                    if (mDevice.getState() != Device.BLE_DISCONNECTED)
                        mService.disconnect();
                }
            }

        } else {
            showMessage(mCtx.getString(R.string.random_error));
            if (mDevice.getState() != Device.BLE_DISCONNECTED)
                mService.disconnect();
        }
    }

    //校验产品号
    private boolean compareSn(byte[] sn) {
        String devSn = StringUtil.asciiDeBytesToCharString(sn).substring(0, 7);
        String appSn = SystemUtils.getMetaDataFromApp(mCtx);
        LogUtil.d(TAG, "devSn : " + devSn + " appSn: " + appSn);
        return devSn.equals(appSn);
    }


    /**
     * 吐司提示
     *
     * @param msg 提示信息
     */
    protected void showMessage(String msg) {
        Toast.makeText(mCtx, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 设置鉴权码到数据库
     *
     * @param authTime 鉴权码里面的时间
     * @return
     */
    private String setAuthCode(byte[] authTime) {
        byte[] authCode = new byte[30];
        if (StringUtil.checkNotNull(mDevInfo.getDeviceNodeId())) {
            byte[] userId = new byte[2];
            StringUtil.short2Bytes(mDevInfo.getUserId(), userId);
            System.arraycopy(userId, 0, authCode, 0, 2);

            byte[] nodeIdBuf = new byte[8];
            String nodeId = mDevInfo.getDeviceNodeId();
            if (nodeId.getBytes().length == 15) {
                nodeId = "0" + nodeId;
            }
            nodeIdBuf = StringUtil.hexStringToBytes(nodeId);
            StringUtil.exchange(nodeIdBuf);
            System.arraycopy(nodeIdBuf, 0, authCode, 2, 8);

            byte[] bleMacBuf = new byte[6];
            String bleMac = mDevInfo.getBleMac();
            if (StringUtil.checkNotNull(bleMac)) {
                bleMacBuf = StringUtil.hexStringToBytes(bleMac.replace(":", ""));
                System.arraycopy(bleMacBuf, 0, authCode, 10, 6);
            }

            byte[] randCodeBuf = new byte[10];
            String randCode = mDevInfo.getDeviceSecret();
            if (StringUtil.checkNotNull(randCode)) {
                randCodeBuf = StringUtil.hexStringToBytes(randCode);
                System.arraycopy(randCodeBuf, 0, authCode, 16, 10);
            }

            System.arraycopy(authTime, 0, authCode, 26, 4);
        }

        return StringUtil.bytesToHexString(authCode);
    }

    private void sendMessage(int type, Bundle bundle, long time) {
        android.os.Message msg = android.os.Message.obtain();
        if (bundle != null) {
            msg.setData(bundle);
        }
        msg.what = type;
        if (time != 0) {
            mHandler.sendMessageDelayed(msg, time);
        } else mHandler.sendMessage(msg);
    }
}
