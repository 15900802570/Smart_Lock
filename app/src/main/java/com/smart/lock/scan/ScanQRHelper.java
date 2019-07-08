package com.smart.lock.scan;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.AES_ECB_PKCS7;
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
import com.smart.lock.permission.PermissionHelper;
import com.smart.lock.permission.PermissionInterface;
import com.smart.lock.ui.LockDetectingActivity;
import com.smart.lock.ui.login.LockScreenActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.SharedPreferenceUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.ToastUtil;
import com.yzq.zxinglibrary.android.CaptureActivity;
import com.yzq.zxinglibrary.bean.ZxingConfig;
import com.yzq.zxinglibrary.common.Constant;

import java.util.Arrays;
import java.util.Objects;

public class ScanQRHelper implements UiListener, PermissionInterface {
    private final String TAG = ScanQRHelper.class.getSimpleName();

    private Activity mActivity;
    private ScanQRResultInterface mScanQRResultInterface;

    private String mUserId;
    private String mNodeId;
    private String mBleMac;
    private String mRandCode;
    private String mTime;
    private int mStatus = -1;
    private int unLockTime = 0;
    private Dialog mLoadDialog;
    protected Handler mHandler = new Handler();
    private DeviceInfo mNewDevice;
    private BleManagerHelper mBleManagerHelper;
    private Device mDevice;
    private PermissionHelper mPermissionHelper;
    private static final int REQ_CODE_CAMERA = 1;
    private static final int ACCESS_COARSE_LOCATION = 2;
    private static final int ACCESS_FINE_LOCATION = 3;
    private static final int READ_EXTERNAL_STORAGE = 4;
    private static final int WRITE_EXTERNAL_STORAGE = 5;

    public ScanQRHelper(Activity activity, ScanQRResultInterface scanQRResultInterface) {
        mActivity = activity;
        mScanQRResultInterface = scanQRResultInterface;
        mBleManagerHelper = BleManagerHelper.getInstance(activity);
        mDevice = Device.getInstance(activity);
        mPermissionHelper = new PermissionHelper(mActivity, this);
    }

    /**
     * 处理扫描结果
     */
    public void ScanDoCode(Intent data) {
        String content = data.getStringExtra(Constant.CODED_CONTENT);
        LogUtil.d(TAG, "content = " + content);
        byte[] mByte = null;
        if (content.length() == 127) {
            mByte = StringUtil.hexStringToBytes('0' + content);
        } else if (content.length() == 128) {
            mByte = StringUtil.hexStringToBytes(content);
        } else if (content.length() == 47) {

            // 通过扫描绑定设备
            String[] dvInfo = content.split(",");
            if (dvInfo.length == 3 && dvInfo[0].length() == 18 && dvInfo[1].length() == 12 && dvInfo[2].length() == 15) {
                String mSn = dvInfo[0];
                mBleMac = dvInfo[1];
                mNodeId = dvInfo[2];

                if (DeviceInfoDao.getInstance(mActivity).queryByField(DeviceInfoDao.NODE_ID, "0" + mNodeId) == null) {
                    Bundle bundle = new Bundle();
                    bundle.putString(BleMsg.KEY_BLE_MAC, mBleMac);
                    bundle.putString(BleMsg.KEY_NODE_SN, mSn);
                    bundle.putString(BleMsg.KEY_NODE_ID, mNodeId);

                    // 断开老设备
                    if (mDevice != null && mDevice.getState() != Device.BLE_DISCONNECTED) {
                        mDevice.halt();
                        mBleManagerHelper.getBleCardService().disconnect();
                    }

                    startIntent(LockDetectingActivity.class, bundle);
                } else {
                    ToastUtil.show(mActivity, mActivity.getString(R.string.device_has_been_added), Toast.LENGTH_LONG);
                }

            } else {
                ToastUtil.show(mActivity, mActivity.getString(R.string.plz_scan_correct_qr), Toast.LENGTH_LONG);
            }

        } else {
            Dialog alterDialog = DialogUtils.createTipsDialogWithCancel(mActivity, mActivity.getString(R.string.plz_scan_correct_qr));
            alterDialog.show();
            return;
        }

        // 通过授权二维码添加设备
        if (mByte != null) {
            LogUtil.d(TAG, "mByte = " + Arrays.toString(mByte) + '\n' +
                    "length = " + mByte.length);
            byte[] devInfo = new byte[64];
            AES_ECB_PKCS7.AES256Decode(mByte, devInfo, MessageCreator.mQrSecret);
            LogUtil.d(TAG, "devInfo =" + Arrays.toString(devInfo));
            getDevInfo(devInfo);
        }
    }

    //验证版本
    private void compareSn() {


    }

    private void getDevInfo(byte[] devInfo) {
        byte[] typeBytes = new byte[1];
        byte[] timeBytes = new byte[4];
        byte[] userIdBytes = new byte[2];
        byte[] imeiBytes = new byte[8];
        byte[] bleMACBytes = new byte[6];
        byte[] randCodeBytes = new byte[10];
        byte[] authCode = new byte[30];
        System.arraycopy(devInfo, 0, typeBytes, 0, 1);
        System.arraycopy(devInfo, 1, timeBytes, 0, 4);
        System.arraycopy(devInfo, 5, userIdBytes, 0, 2);
        System.arraycopy(devInfo, 7, imeiBytes, 0, 8);
        System.arraycopy(devInfo, 15, bleMACBytes, 0, 6);
        System.arraycopy(devInfo, 21, randCodeBytes, 0, 10);
        System.arraycopy(devInfo, 5, authCode, 0, 30);
        String mUserType = StringUtil.bytesToHexString(typeBytes);
        LogUtil.d(TAG, "copyNumBytes : " + Arrays.toString(userIdBytes));
        mUserId = StringUtil.bytesToHexString(userIdBytes);
        mNodeId = StringUtil.bytesToHexString(StringUtil.bytesReverse(imeiBytes));
        mBleMac = Objects.requireNonNull(StringUtil.bytesToHexString(bleMACBytes)).toUpperCase();
        mRandCode = StringUtil.bytesToHexString(randCodeBytes);
        mTime = StringUtil.byte2Int(timeBytes);

        LogUtil.d(TAG, "New Device : " + '\n' +
                "userType = " + mUserType + '\n' +
                "UserId = " + mUserId + '\n' +
                "NodeId = " + mNodeId + '\n' +
                "mRandCode = " + mRandCode + '\n' +
                "mBleMac = " + mBleMac + '\n' +
                "Time = " + DateTimeUtil.stampToDate(mTime + "000") + '\n' +
                "mAuthCode = " + Arrays.toString(authCode));

        addDev(authCode);
    }

    /**
     * 新界面
     *
     * @param cls    新Activity
     * @param bundle 数据包
     */
    private void startIntent(Class<?> cls, Bundle bundle) {
        Intent intent = new Intent();
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        intent.setClass(mActivity, cls);
        mActivity.startActivity(intent);
    }

    private void addDev(byte[] authCode) {
        if ((Long.valueOf(mTime)) < System.currentTimeMillis() / 1000) {
            Dialog alterDialog = DialogUtils.createTipsDialogWithCancel(mActivity, "授权码已过期，请重新请求");
            alterDialog.show();
        } else if (DeviceInfoDao.getInstance(mActivity).queryByField(DeviceInfoDao.NODE_ID, mNodeId) != null) {
            ToastUtil.show(mActivity, mActivity.getString(R.string.device_has_been_added), Toast.LENGTH_LONG);
        } else {
            if (mActivity.getIntent().getExtras() != null && mDevice != null) {
                if (mBleManagerHelper.getBleCardService() != null && mDevice.getState() != Device.BLE_DISCONNECTED) {
                    mDevice.halt();
                    mDevice.setDisconnectBle(true);
                    LogUtil.d(TAG, "hash code 1: " + mDevice.hashCode());
                    mBleManagerHelper.getBleCardService().disconnect();
                }
            }
            mLoadDialog = DialogUtils.createLoadingDialog(mActivity, mActivity.getString(R.string.data_loading));
            mLoadDialog.show();
            mBleManagerHelper.addUiListener(this);
            BleManagerHelper.setSk(mBleMac, mRandCode);
            Bundle bundle = new Bundle();
            bundle.putShort(BleMsg.KEY_USER_ID, Short.parseShort(mUserId, 16));
            bundle.putString(BleMsg.KEY_BLE_MAC, getMacAdr(mBleMac));
            bundle.putString(BleMsg.KEY_NODE_ID, mNodeId);
            bundle.putByteArray(BleMsg.KEY_AUTH_CODE, authCode);
            mBleManagerHelper.connectBle(Device.BLE_SCAN_AUTH_CODE_CONNECT, bundle, mActivity);
        }
    }

    private void onAuthenticationSuccess(DeviceInfo deviceInfo) {
        ToastUtil.showLong(mActivity, mActivity.getResources().getString(R.string.toast_add_lock_success));
        mScanQRResultInterface.onAuthenticationSuccess(deviceInfo);
        mDevice.setDisconnectBle(false);
        if (!SharedPreferenceUtil.getInstance(mActivity).readBoolean(ConstantUtil.NUM_PWD_CHECK)) {
            Intent intent = new Intent(mActivity, LockScreenActivity.class);
            intent.putExtra(ConstantUtil.IS_RETURN, true);
            intent.putExtra(ConstantUtil.NOT_CANCEL, true);
            mActivity.startActivityForResult(intent.putExtra(ConstantUtil.TYPE, ConstantUtil.SETTING_PASSWORD), ConstantUtil.SETTING_PWD_REQUEST_CODE);
        }
    }

    private void onAuthenticationFailed() {
        if (mBleManagerHelper.getBleCardService() != null && mDevice.getState() == Device.BLE_CONNECTED) {
            mBleManagerHelper.getBleCardService().disconnect();
            mDevice.halt();
        }
        DialogUtils.closeDialog(mLoadDialog);
        ToastUtil.showLong(mActivity, mActivity.getResources().getString(R.string.toast_add_lock_falied));
        mScanQRResultInterface.onAuthenticationFailed();
    }

    /**
     * 打开第三方二维码扫描库
     */
    public void scanQr() {
        mPermissionHelper.requestPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, ACCESS_COARSE_LOCATION);
    }

    private void doScan() {
        Intent newIntent = new Intent(mActivity, CaptureActivity.class);
        ZxingConfig config = new ZxingConfig();
        config.setPlayBeep(true);//是否播放扫描声音 默认为true
        config.setShake(true);//是否震动  默认为true
        config.setDecodeBarCode(false);//是否扫描条形码 默认为true
        config.setReactColor(R.color.colorAccent);//设置扫描框四个角的颜色 默认为淡蓝色
        config.setFrameLineColor(R.color.colorAccent);//设置扫描框边框颜色 默认无色
        config.setFullScreenScan(true);//是否全屏扫描  默认为true  设为false则只会在扫描框中扫描
        newIntent.putExtra(Constant.INTENT_ZXING_CONFIG, config);
        mActivity.startActivityForResult(newIntent, ConstantUtil.SCAN_QRCODE_REQUEST_CODE);
    }

    /**
     * 创建用户
     *
     * @param userId
     */
    private synchronized void createDeviceUser(short userId, String path, String authCode) {
        DeviceUser user = new DeviceUser();
        user.setDevNodeId(mNodeId);
        user.setCreateTime(System.currentTimeMillis() / 1000);
        user.setUserId(userId);
        user.setUserStatus(ConstantUtil.USER_UNENABLE);
        user.setAuthCode(authCode);
        LogUtil.d(TAG, "userId : " + userId);
        if (userId < 101) {
            user.setUserPermission(ConstantUtil.DEVICE_MASTER);
            user.setUserName(mActivity.getString(R.string.administrator) + userId);
        } else if (userId < 201) {
            user.setUserPermission(ConstantUtil.DEVICE_MEMBER);
            user.setUserName(mActivity.getString(R.string.members) + userId);
        } else {
            user.setUserPermission(ConstantUtil.DEVICE_TEMP);
            user.setUserName(mActivity.getString(R.string.tmp_user) + userId);
        }

        user.setQrPath(path);
        DeviceUserDao.getInstance(mActivity).insert(user);
    }

    /**
     * 创建设备
     */
    private DeviceInfo createDevice() {

        LogUtil.d(TAG, "New Device2 : " + '\n' +
                "UserId = " + mUserId + '\n' +
                "NodeId = " + mNodeId + '\n' +
                "mRandCode = " + mRandCode + '\n' +
                "mBleMac = " + mBleMac + '\n' +
                "Time = " + DateTimeUtil.stampToDate(mTime + "000") + '\n');
        LogUtil.d(TAG, "newDevice: " + "BLEMAC =" + mBleMac);
        mNewDevice = new DeviceInfo();
        mNewDevice.setActivitedTime(Long.valueOf(mTime));
        mNewDevice.setBleMac(getMacAdr(mBleMac));
        mNewDevice.setConnectType(false);
        mNewDevice.setUserId(Short.parseShort(mUserId, 16));
        LogUtil.d(TAG, "mNewDevice userId : " + mNewDevice.getUserId());
        mNewDevice.setDeviceNodeId(mNodeId);
        mNewDevice.setNodeType(ConstantUtil.SMART_LOCK);
        mNewDevice.setDeviceDate(System.currentTimeMillis() / 1000);
        DeviceInfoDao.getInstance(mActivity).setNoDefaultDev();

        mNewDevice.setDeviceDefault(true);
        mNewDevice.setDeviceSn("");
        mNewDevice.setTempSecret(StringUtil.bytesToHexString(Device.getInstance(mActivity).getTempSecret()));
        int count = (int) DeviceInfoDao.getInstance(mActivity).queryCount();

        for (int i = 1; i <= count + 2; i++) {
            if (DeviceInfoDao.getInstance(mActivity).queryByField(DeviceInfoDao.DEVICE_NAME, mActivity.getString(R.string.lock_default_name) + i) == null) {
                mNewDevice.setDeviceName(mActivity.getResources().getString(R.string.lock_default_name) + i);
                break;
            }
        }

        mNewDevice.setDeviceSecret(mRandCode);
        DeviceInfoDao.getInstance(mActivity).insert(mNewDevice);
        return mNewDevice;
    }


    /**
     * 转成标准MAC地址
     *
     * @param str 未加：的MAC字符串
     * @return 标准MAC字符串
     */
    private static String getMacAdr(String str) {
        str = str.toUpperCase();
        StringBuilder result = new StringBuilder("");
        for (int i = 1; i <= 12; i++) {
            result.append(str.charAt(i - 1));
            if (i % 2 == 0) {
                result.append(":");
            }
        }
        return result.substring(0, 17);
    }


    @Override
    public void deviceStateChange(Device device, int state) {
        mDevice = device;
        switch (state) {
            case BleMsg.STATE_DISCONNECTED:
                if (mDevice.getConnectType() == Device.BLE_SCAN_AUTH_CODE_CONNECT) {
                    showMessage("添加失败,请重试!");
                    DialogUtils.closeDialog(mLoadDialog);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void dispatchUiCallback(Message msg, Device device, int type) {
        mDevice = device;
        if (mDevice != null && type == BleMsg.USER_PAUSE) {
            DialogUtils.closeDialog(mLoadDialog);
            return;
        }
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_0E:
                onAuthenticationFailed();
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_04:
                DeviceInfo deviceInfo = DeviceInfoDao.getInstance(mActivity).queryByField(DeviceInfoDao.NODE_ID, mNodeId);
                LogUtil.d(TAG, "deviceInfo = " + deviceInfo + '\n' +
                        "mNodeId = " + mNodeId);
                if (deviceInfo == null) {
                    onAuthenticationSuccess(createDevice());
                }

                break;
            default:
                break;

        }
        mBleManagerHelper.removeUiListener(this);
        DialogUtils.closeDialog(mLoadDialog);
    }

    @Override
    public void reConnectBle(Device device) {

    }

    @Override
    public void sendFailed(Message msg) {
        int exception = msg.getException();
        LogUtil.e(TAG, "msg exception : " + msg.toString());
        onAuthenticationFailed();
        mBleManagerHelper.removeUiListener(this);
        switch (exception) {
            case Message.EXCEPTION_TIMEOUT:
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case Message.EXCEPTION_SEND_FAIL:
                DialogUtils.closeDialog(mLoadDialog);
                break;
            default:
                break;
        }
    }

    @Override
    public void addUserSuccess(Device device) {

    }

    @Override
    public void scanDevFailed() {
        LogUtil.e(TAG, "scanDevFailed");
        DialogUtils.closeDialog(mLoadDialog);
        mBleManagerHelper.removeUiListener(this);
    }

    public void halt() {
        mBleManagerHelper.removeUiListener(this);
        mNewDevice = null;
    }

    @Override
    public void requestPermissionsSuccess(int callBackCode) {

        Log.d(TAG, "success callBackCode = " + callBackCode);

        if (callBackCode == WRITE_EXTERNAL_STORAGE) {
            doScan();
        } else if (callBackCode == ACCESS_COARSE_LOCATION) {
            mPermissionHelper.requestPermissions(Manifest.permission.ACCESS_FINE_LOCATION, ACCESS_FINE_LOCATION);
        } else if (callBackCode == ACCESS_FINE_LOCATION) {
            mPermissionHelper.requestPermissions(Manifest.permission.CAMERA, REQ_CODE_CAMERA);
        } else if (callBackCode == REQ_CODE_CAMERA) {
            mPermissionHelper.requestPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE);
        } else if (callBackCode == READ_EXTERNAL_STORAGE) {
            mPermissionHelper.requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE);
        }

    }

    @Override
    public void requestPermissionsFail(int callBackCode) {
        Log.d(TAG, "failed callBackCode = " + callBackCode);
        showMessage(mActivity.getString(R.string.rejected_permission));
    }

    @Override
    public String[] getPermissions() {
        return new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
    }

    /**
     * 吐司提示
     *
     * @param msg 提示信息
     */
    protected void showMessage(String msg) {
        Toast.makeText(mActivity, msg, Toast.LENGTH_SHORT).show();
    }

    public PermissionHelper getPermissionHelper() {
        return mPermissionHelper;
    }

}
