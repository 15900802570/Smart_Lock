package com.smart.lock.scan;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceStatus;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceStatusDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.entity.Device;
import com.smart.lock.ui.LockDetectingActivity;
import com.smart.lock.ui.login.LockScreenActivity;
import com.smart.lock.utils.ConstantUtil;
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

public class ScanQRHelper implements UiListener {
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

    public ScanQRHelper(Activity activity, ScanQRResultInterface scanQRResultInterface) {
        mActivity = activity;
        mScanQRResultInterface = scanQRResultInterface;
        mBleManagerHelper = BleManagerHelper.getInstance(activity, false);
        mDevice = mBleManagerHelper.getBleCardService().getDevice();
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
            addDev();
        }
    }

    private void getDevInfo(byte[] devInfo) {
        byte[] typeBytes = new byte[1];
        byte[] copyNumBytes = new byte[2];
        byte[] ImeiBytes = new byte[8];
        byte[] bleMACBytes = new byte[6];
        byte[] timeBytes = new byte[4];
        byte[] randCodeBytes = new byte[18];
        System.arraycopy(devInfo, 0, typeBytes, 0, 1);
        System.arraycopy(devInfo, 1, copyNumBytes, 0, 2);
        System.arraycopy(devInfo, 3, ImeiBytes, 0, 8);
        System.arraycopy(devInfo, 11, bleMACBytes, 0, 6);
        System.arraycopy(devInfo, 17, randCodeBytes, 0, 10);
        System.arraycopy(devInfo, 27, timeBytes, 0, 4);
        String mUserType = StringUtil.bytesToHexString(typeBytes);
        LogUtil.d(TAG, "copyNumBytes : " + Arrays.toString(copyNumBytes));
        mUserId = StringUtil.bytesToHexString(copyNumBytes);
        mNodeId = StringUtil.bytesToHexString(ImeiBytes);
        mBleMac = Objects.requireNonNull(StringUtil.bytesToHexString(bleMACBytes)).toUpperCase();
        mRandCode = StringUtil.bytesToHexString(randCodeBytes);
        mTime = StringUtil.byte2Int(timeBytes);

        LogUtil.d(TAG, "New Device : " + '\n' +
                "userType = " + mUserType + '\n' +
                "UserId = " + mUserId + '\n' +
                "NodeId = " + mNodeId + '\n' +
                "mRandCode = " + mRandCode + '\n' +
                "mBleMac = " + mBleMac + '\n' +
                "Time = " + mTime);
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

    private void addDev() {
       if ((Long.valueOf(mTime)) < System.currentTimeMillis() / 1000) {
            Dialog alterDialog = DialogUtils.createTipsDialogWithCancel(mActivity, "授权码已过期，请重新请求");
            alterDialog.show();
        } else  if (DeviceInfoDao.getInstance(mActivity).queryByField(DeviceInfoDao.NODE_ID, mNodeId) != null) {
            ToastUtil.show(mActivity, mActivity.getString(R.string.device_has_been_added), Toast.LENGTH_LONG);
        } else {
            if (mActivity.getIntent().getExtras() != null && mDevice != null) {
                DeviceInfo deviceDev = (DeviceInfo) mActivity.getIntent().getExtras().getSerializable(BleMsg.KEY_DEFAULT_DEVICE);
                if (mBleManagerHelper.getBleCardService() != null && mDevice.getState() != Device.BLE_DISCONNECTED) {
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
            mBleManagerHelper.connectBle(Device.BLE_OTHER_CONNECT_TYPE, bundle, mActivity);
        }
    }

    private void onAuthenticationSuccess() {
        ToastUtil.showLong(mActivity, mActivity.getResources().getString(R.string.toast_add_lock_success));
        mScanQRResultInterface.onAuthenticationSuccess(mNewDevice);
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
        }
        DialogUtils.closeDialog(mLoadDialog);
        ToastUtil.showLong(mActivity, mActivity.getResources().getString(R.string.toast_add_lock_falied));
        mScanQRResultInterface.onAuthenticationFailed();
    }

    /**
     * 打开第三方二维码扫描库
     */
    public void scanQr() {
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

//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if(resultCode == Activity.RESULT_OK) switch (requestCode) {
//            case ConstantUtil.SCAN_QRCODE_REQUEST_CODE:
//                mScanQRHelper.ScanDoCode(data);
//                break;
//            case ConstantUtil.SETTING_PWD_REQUEST_CODE:
//                break;
//        }
//        LogUtil.d(TAG, "测试测试测试测试");
//    }

    /**
     * 创建设备
     */
    private void createDevice() {
        DeviceInfo oldDevice = DeviceInfoDao.getInstance(mActivity).queryFirstData("device_nodeId", mNodeId);
        if (oldDevice == null) {
            DeviceInfo defaultDevice = DeviceInfoDao.getInstance(mActivity).queryFirstData("device_default", true);
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
            if (defaultDevice != null) mNewDevice.setDeviceDefault(false);
            else mNewDevice.setDeviceDefault(true);
            mNewDevice.setDeviceSn("");
            mNewDevice.setDeviceName(mActivity.getResources().getString(R.string.lock_default_name));
            mNewDevice.setDeviceSecret(mRandCode);
            DeviceInfoDao.getInstance(mActivity).insert(mNewDevice);
        }
    }

    /**
     * 创建设备状态
     */
    private void createDeviceStatus() {
        DeviceStatus deviceStatus = DeviceStatusDao.getInstance(mActivity)
                .queryOrCreateByNodeId(mNodeId);
        switch (mStatus) {
            case 0:
                deviceStatus.setVoicePrompt(false);
                deviceStatus.setNormallyOpen(false);
                break;
            case 1:
                deviceStatus.setVoicePrompt(false);
                deviceStatus.setNormallyOpen(true);
                break;
            case 2:
                deviceStatus.setVoicePrompt(true);
                deviceStatus.setNormallyOpen(false);
                break;
            case 3:
                deviceStatus.setVoicePrompt(true);
                deviceStatus.setNormallyOpen(true);
                break;
            default:
                break;
        }
        deviceStatus.setRolledBackTime(unLockTime);
        DeviceStatusDao.getInstance(mActivity).updateDeviceStatus(deviceStatus);
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
    }

    @Override
    public void dispatchUiCallback(Message msg, Device device, int type) {
        LogUtil.i(TAG, "dispatchUiCallback : " + msg.getType());
        mDevice = device;
        Bundle bundle = msg.getData();
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_0E:
                DialogUtils.closeDialog(mLoadDialog);

                onAuthenticationFailed();
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_04:
            case Message.TYPE_BLE_RECEIVER_CMD_26:
                LogUtil.d(TAG, "array = " + Arrays.toString(bundle.getByteArray(BleMsg.KEY_STATUS)));
                mStatus = bundle.getByte(BleMsg.KEY_SETTING_STATUS, (byte) 0);
                unLockTime = bundle.getByte(BleMsg.KEY_UNLOCK_TIME, (byte) 0);
                LogUtil.d(TAG, "unLockTime = " + unLockTime);
                if (unLockTime != 0) {
                    createDevice();
                    createDeviceStatus();
                    DialogUtils.closeDialog(mLoadDialog);
                    onAuthenticationSuccess();
                }
                break;
            default:
                break;

        }
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
}
