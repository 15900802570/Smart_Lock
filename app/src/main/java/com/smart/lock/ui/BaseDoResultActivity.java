package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.ToastUtil;
import com.yzq.zxinglibrary.common.Constant;

import java.util.Arrays;

@SuppressLint("Registered")
public class BaseDoResultActivity extends AppCompatActivity {

    public String TAG = "BaseDoResultActivity";

    private String mUserId;
    private String mNodeId;
    private String mSn;
    private String mBleMac;
    private String mRandCode;
    private String mTime;
    private Dialog mLoadDialog;
    protected Handler mHandler = new Handler();
    protected DeviceInfo mNewDevice;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void ScanDoCode(Intent data) {
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
                mSn = dvInfo[0];
                mBleMac = dvInfo[1];
                mNodeId = dvInfo[2];

                if (DeviceInfoDao.getInstance(this).queryByField(DeviceInfoDao.NODE_ID, "0" + mNodeId) == null) {
                    Bundle bundle = new Bundle();
                    bundle.putString(BleMsg.KEY_BLE_MAC, mBleMac);
                    bundle.putString(BleMsg.KEY_NODE_SN, mSn);
                    bundle.putString(BleMsg.KEY_NODE_ID, mNodeId);

                    startIntent(LockDetectingActivity.class, bundle);
                } else {
                    ToastUtil.show(this, getString(R.string.device_has_been_added), Toast.LENGTH_LONG);
                }

            } else {
                ToastUtil.show(this, getString(R.string.plz_scan_correct_qr), Toast.LENGTH_LONG);
            }
        } else {
            Dialog alterDialog = DialogUtils.createTipsDialogWithCancel(this, content);
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

    /**
     * 新界面
     *
     * @param cls    新Activity
     * @param bundle 数据包
     */
    protected void startIntent(Class<?> cls, Bundle bundle) {
        Intent intent = new Intent();
        if (bundle != null) {
            intent.putExtras(bundle);
        }

        intent.setClass(this, cls);
        startActivity(intent);
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
        mUserId = StringUtil.bytesToHexString(copyNumBytes);
        StringUtil.exchange(ImeiBytes);
        mNodeId = StringUtil.bytesToHexString(ImeiBytes);
        mBleMac = StringUtil.bytesToHexString(bleMACBytes).toUpperCase();
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

    private void addDev() {
        if ((Long.valueOf(mTime)) < System.currentTimeMillis() / 1000) {
            Dialog alterDialog = DialogUtils.createTipsDialogWithCancel(this, "授权码已过期，请重新请求");
            alterDialog.show();
        } else if (DeviceInfoDao.getInstance(this).queryByField(DeviceInfoDao.NODE_ID, mNodeId) != null) {
            ToastUtil.show(this, this.getString(R.string.device_has_been_added), Toast.LENGTH_LONG);
        } else {
            if (this.getIntent().getExtras() != null) {
                DeviceInfo deviceDev = (DeviceInfo) this.getIntent().getExtras().getSerializable(BleMsg.KEY_DEFAULT_DEVICE);
                if (BleManagerHelper.getInstance(this, false).getBleCardService() != null && BleManagerHelper.getInstance(this, false).getServiceConnection()) {
                    BleManagerHelper.getInstance(this, false).getBleCardService().disconnect();
                }

            }
            LocalBroadcastManager.getInstance(this).registerReceiver(devCheckReceiver, intentFilter());
            mLoadDialog = DialogUtils.createLoadingDialog(this, this.getString(R.string.data_loading));
            mLoadDialog.show();
            closeDialog(10);
            BleManagerHelper.setSk(mBleMac, mRandCode);
            Bundle bundle = new Bundle();
            bundle.putShort(BleMsg.KEY_USER_ID, Short.parseShort(mUserId, 16));
            bundle.putString(BleMsg.KEY_BLE_MAC, getMacAdr(mBleMac));
            BleManagerHelper.getInstance(this, false).connectBle((byte) 1, bundle);
        }
    }

    protected static IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.STR_RSP_SECURE_CONNECTION);
        intentFilter.addAction(BleMsg.STR_RSP_SET_TIMEOUT);
        return intentFilter;
    }

    /**
     * 广播接受
     */
    private final BroadcastReceiver devCheckReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //4.2.3 MSG 04
            if (action.equals(BleMsg.STR_RSP_SECURE_CONNECTION)) {
                LogUtil.d(TAG, "array = " + Arrays.toString(intent.getByteArrayExtra(BleMsg.KEY_STATUS)));
                createDeviceUser(Short.parseShort(mUserId, 16));
                createDevice();
                mHandler.removeCallbacks(mRunnable);
                DialogUtils.closeDialog(mLoadDialog);
                onAuthenticationSuccess();
            }
            if (action.equals(BleMsg.STR_RSP_SET_TIMEOUT)) {
                onAuthenticationFailed();
            }
        }
    };

    protected void onAuthenticationSuccess() {
        ToastUtil.showLong(this, getResources().getString(R.string.toast_add_lock_success));
        if(this instanceof OnRefreshView){
            ((OnRefreshView) BaseDoResultActivity.this).onRefreshView();
        }
    }

    public interface OnRefreshView {
        void onRefreshView();
    }

    protected void onAuthenticationFailed() {
        ToastUtil.showLong(this, getResources().getString(R.string.toast_add_lock_falied));
    }

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
            user.setUserName(this.getString(R.string.administrator) + userId);
        } else if (userIdInt < 201) {
            user.setUserPermission(ConstantUtil.DEVICE_MEMBER);
            user.setUserName(this.getString(R.string.members) + userId);
        } else {
            user.setUserPermission(ConstantUtil.DEVICE_MEMBER);
            user.setUserName(this.getString(R.string.members) + userId);
        }

        user.setUserStatus(ConstantUtil.USER_UNENABLE);

        DeviceUserDao.getInstance(this).insert(user);
    }

    /**
     * 创建设备
     */
    private void createDevice() {
        DeviceInfo oldDevice = DeviceInfoDao.getInstance(this).queryFirstData("device_nodeId", mNodeId);
        if (oldDevice == null) {
            DeviceInfo defaultDevice = DeviceInfoDao.getInstance(this).queryFirstData("device_default", true);
            LogUtil.d(TAG, "newDevice: " + "BLEMAC =" + mBleMac);
            mNewDevice = new DeviceInfo();
            mNewDevice.setActivitedTime(Long.valueOf(mTime));
            mNewDevice.setBleMac(getMacAdr(mBleMac));
            mNewDevice.setConnectType(false);
            mNewDevice.setUserId(Short.parseShort(mUserId, 16));
            mNewDevice.setDeviceNodeId(mNodeId);
            mNewDevice.setNodeType(ConstantUtil.SMART_LOCK);
            mNewDevice.setDeviceDate(System.currentTimeMillis() / 1000);
            if (defaultDevice != null) mNewDevice.setDeviceDefault(false);
            else mNewDevice.setDeviceDefault(true);
            mNewDevice.setDeviceSn("");
            mNewDevice.setDeviceName(this.getResources().getString(R.string.lock_default_name));
            mNewDevice.setDeviceSecret(mRandCode);
            DeviceInfoDao.getInstance(this).insert(mNewDevice);
        }
    }

    /**
     * 转成标准MAC地址
     *
     * @param str 未加：的MAC字符串
     * @return 标准MAC字符串
     */
    protected static String getMacAdr(String str) {
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

    /**
     * 超时提醒
     *
     * @param seconds 时间
     */
    private void closeDialog(final int seconds) {

        mHandler.removeCallbacks(mRunnable);

        mHandler.postDelayed(mRunnable, seconds * 1000);
    }

    /**
     * 超时提示框启动器
     */
    protected Runnable mRunnable = new Runnable() {
        public void run() {
            if (mLoadDialog != null && mLoadDialog.isShowing()) {

                DialogUtils.closeDialog(mLoadDialog);

//                mBleManagerHelper = BleManagerHelper.getInstance(BaseListViewActivity.this, mDefaultDevice.getBleMac(), false);
//                mBleManagerHelper.getBleCardService().sendCmd19(mBleManagerHelper.getAK());

                Toast.makeText(BaseDoResultActivity.this, BaseDoResultActivity.this.getResources().getString(R.string.plz_reconnect), Toast.LENGTH_LONG).show();
            }

        }
    };
}
