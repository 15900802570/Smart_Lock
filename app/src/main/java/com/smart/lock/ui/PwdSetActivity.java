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
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.listener.ClientTransaction;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.entity.Device;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.widget.Controller;

import java.util.Arrays;

public class PwdSetActivity extends BaseActivity implements View.OnClickListener, UiListener {
    private static final String TAG = "PwdCreateActivity";

    private EditText mUserNameEt;
    private EditText mFirstPwdEt;
    private EditText mSecondPwdEt;
    private Button mSetPwdBtn;
    private ImageView mBackIv;
    private TextView mTitleTv;

    private Controller controller; //提示框
    private DeviceInfo mDefaultDevice; //默认设备
    private DeviceKey mModifyDeviceKey;
    private DeviceUser mTempUser;

    private Dialog mLoadDialog;//等待框
    private Handler mHandler;
    private String mCmdType; //密码设置类型
    /**
     * 蓝牙服务者
     */
    private BleManagerHelper mBleManagerHelper;
    private String mLockId = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_pwd);
        initView();
        initData();
        initEvent();
    }

    private void initView() {
        mUserNameEt = findViewById(R.id.et_username);
        mFirstPwdEt = findViewById(R.id.et_first_pwd);
        mSecondPwdEt = findViewById(R.id.et_second_pwd);
        mSetPwdBtn = findViewById(R.id.btn_set_pwd);
        mBackIv = findViewById(R.id.iv_back);
        mTitleTv = findViewById(R.id.tv_message_title);
    }

    @SuppressLint("SetTextI18n")
    private void initData() {
        controller = Controller.getInstants();
        controller.setAcitivty(this);
        mCmdType = getIntent().getStringExtra(BleMsg.KEY_CMD_TYPE);
        mDefaultDevice = (DeviceInfo) getIntent().getSerializableExtra(BleMsg.KEY_DEFAULT_DEVICE);

        if (mCmdType.equals(ConstantUtil.CREATE)) {
            mTempUser = (DeviceUser) getIntent().getExtras().getSerializable(BleMsg.KEY_TEMP_USER);
            mNodeId = mDefaultDevice.getDeviceNodeId();
            mUserNameEt.setText(getString(R.string.me) + getString(R.string.password));
            mUserNameEt.setSelection((getString(R.string.me) + getString(R.string.password)).length());
            mTitleTv.setText(R.string.create_pwd);
        } else {
            mModifyDeviceKey = (DeviceKey) getIntent().getSerializableExtra(BleMsg.KEY_MODIFY_DEVICE_KEY);
            mUserNameEt.setText(mModifyDeviceKey.getKeyName());
            mUserNameEt.setSelection(mModifyDeviceKey.getKeyName().length());
            mNodeId = mModifyDeviceKey.getDeviceNodeId();
            mTitleTv.setText(R.string.modify_pwd);
        }

        mBleManagerHelper = BleManagerHelper.getInstance(this, false);
        mBleManagerHelper.addUiListener(this);
        mHandler = new Handler();
        mLoadDialog = DialogUtils.createLoadingDialog(this, getResources().getString(R.string.data_loading));
    }

    private void initEvent() {
        mSetPwdBtn.setOnClickListener(this);
        mBackIv.setOnClickListener(this);
    }


    /**
     * 验证密码
     */
    private void checkPwd() {
        final String firstPwd = mFirstPwdEt.getText().toString().trim();
        final String secPwd = mSecondPwdEt.getText().toString().trim();
        final String userName = mUserNameEt.getText().toString().trim();

        if (TextUtils.isEmpty(firstPwd) || firstPwd.length() != 6) {
            mFirstPwdEt.setError(getString(R.string.valid_password));
        } else if (TextUtils.isEmpty(secPwd) || secPwd.length() != 6) {
            mSecondPwdEt.setError(getString(R.string.valid_password));
        } else if (!firstPwd.equals(secPwd)) {
            mSecondPwdEt.setError(getString(R.string.pwd_twice_error));
        } else if (TextUtils.isEmpty(userName)) {
            mUserNameEt.setError(getString(R.string.plz_input_username));
        } else {
            if (mCmdType.equals(ConstantUtil.CREATE)) {
                int count = DeviceKeyDao.getInstance(this).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_PWD).size();

                if (count >= 0 && count < 1) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    if (mTempUser != null)
                        mBleManagerHelper.getBleCardService().sendCmd15(BleMsg.CMD_TYPE_CREATE, BleMsg.TYPE_PASSWORD, mTempUser.getUserId(), (byte) 0, firstPwd, BleMsg.INT_DEFAULT_TIMEOUT);
                    else
                        mBleManagerHelper.getBleCardService().sendCmd15(BleMsg.CMD_TYPE_CREATE, BleMsg.TYPE_PASSWORD, mDefaultDevice.getUserId(), (byte) 0, firstPwd, BleMsg.INT_DEFAULT_TIMEOUT);

                } else {
                    showMessage(getString(R.string.add_pwd_tips));
                }
            } else {
                DialogUtils.closeDialog(mLoadDialog);
                mLoadDialog.show();
                mBleManagerHelper.getBleCardService().sendCmd15(BleMsg.CMD_TYPE_MODIFY, BleMsg.TYPE_PASSWORD, mModifyDeviceKey.getUserId(), (byte) 0, firstPwd, BleMsg.INT_DEFAULT_TIMEOUT);
            }

        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_set_pwd:
                checkPwd();
                break;
            case R.id.iv_back:
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBleManagerHelper.removeUiListener(this);
    }

    @Override
    public void deviceStateChange(Device device, int state) {
        switch (state) {
            case BleMsg.STATE_DISCONNECTED:
                DialogUtils.closeDialog(mLoadDialog);
                showMessage(getString(R.string.ble_disconnect));
                break;
            case BleMsg.STATE_CONNECTED:

                break;
            case BleMsg.GATT_SERVICES_DISCOVERED:
                break;
            default:
                LogUtil.e(TAG, "state : " + state + "is can not handle");
                break;
        }
    }

    @Override
    public void dispatchUiCallback(Message msg, Device device, int type) {
        LogUtil.i(TAG, "dispatchUiCallback : " + msg.getType());
        Bundle extra = msg.getData();
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_1E:
                final byte[] errCode = extra.getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode != null)
                    dispatchErrorCode(errCode[3]);
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_16:
                DeviceKey key = (DeviceKey) extra.getSerializable(BleMsg.KEY_SERIALIZABLE);
                if (key == null || (key.getKeyType() != ConstantUtil.USER_PWD)) {
                    DialogUtils.closeDialog(mLoadDialog);
                    return;
                }

                final byte[] lockId = extra.getByteArray(BleMsg.KEY_LOCK_ID);
                mLockId = String.valueOf(lockId[0]);
                LogUtil.d(TAG, "lockId = " + mLockId);
                DeviceKey deviceKey = new DeviceKey();
                deviceKey.setDeviceNodeId(mDefaultDevice.getDeviceNodeId());
                deviceKey.setUserId(mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId());
                deviceKey.setKeyActiveTime(System.currentTimeMillis() / 1000);
                deviceKey.setKeyName(mUserNameEt.getText().toString().trim());
                deviceKey.setKeyType(ConstantUtil.USER_PWD);
                deviceKey.setLockId(mLockId);
                deviceKey.setPwd(mFirstPwdEt.getText().toString().trim());
                DeviceKeyDao.getInstance(this).insert(deviceKey);
                if (mTempUser != null) {
                    mTempUser.setUserStatus(ConstantUtil.USER_ENABLE);
                    DeviceUserDao.getInstance(this).updateDeviceUser(mTempUser);
                }
                showMessage(getString(R.string.set_pwd_success));
                DialogUtils.closeDialog(mLoadDialog);

                finish();
                break;
            default:
                LogUtil.e(TAG, "Message type : " + msg.getType() + " can not be handler");
                break;

        }
    }

    @Override
    public void reConnectBle(Device device) {

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
                LogUtil.e(msg.getType() + " send failed!");
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
    public void scanDevFailed() {

    }

    private void dispatchErrorCode(byte errCode) {
        LogUtil.i(TAG, "errCode : " + errCode);
        DialogUtils.closeDialog(mLoadDialog);
        switch (errCode) {
            case BleMsg.TYPE_ENTER_PASSWORD_FAILED:
                showMessage(getString(R.string.add_pwd_failed));
                break;
            case BleMsg.TYPE_ENTER_OR_MODIFY_PASSWORD_SUCCESS:
                showMessage(getString(R.string.modify_pwd_success));
                mModifyDeviceKey.setPwd(mFirstPwdEt.getText().toString().trim());
                DeviceKeyDao.getInstance(PwdSetActivity.this).updateDeviceKey(mModifyDeviceKey);
                finish();
                break;
            case BleMsg.TYPE_DELETE_PASSWORD_SUCCESS:
                showMessage(getString(R.string.delete_pwd_success));
                break;
            default:
                break;
        }
    }
}
