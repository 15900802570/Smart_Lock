package com.smart.lock.ui;

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
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.widget.Controller;

import java.util.Arrays;

public class PwdSetActivity extends BaseActivity implements View.OnClickListener {
    private static final String TAG = "PwdCreateActivity";

    private EditText mUserNameEt;
    private Button mModifyPwdBtn;
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
    /**
     * 超时提示框启动器
     */
    protected Runnable mRunnable = new Runnable() {
        public void run() {
            if (mLoadDialog != null && mLoadDialog.isShowing()) {

                DialogUtils.closeDialog(mLoadDialog);

//                mBleManagerHelper = BleManagerHelper.getInstance(PwdSetActivity.this, mNodeId, false);

                Toast.makeText(PwdSetActivity.this, PwdSetActivity.this.getResources().getString(R.string.plz_reconnect), Toast.LENGTH_LONG).show();
            }

        }
    };

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
        mModifyPwdBtn = findViewById(R.id.btn_edit);
        mFirstPwdEt = findViewById(R.id.et_first_pwd);
        mSecondPwdEt = findViewById(R.id.et_second_pwd);
        mSetPwdBtn = findViewById(R.id.btn_set_pwd);
        mBackIv = findViewById(R.id.iv_back);
        mTitleTv = findViewById(R.id.tv_message_title);
    }

    private void initData() {
        controller = Controller.getInstants();
        controller.setAcitivty(this);
        mCmdType = getIntent().getStringExtra(BleMsg.KEY_CMD_TYPE);
        mDefaultDevice = (DeviceInfo) getIntent().getSerializableExtra(BleMsg.KEY_DEFAULT_DEVICE);

        if (mCmdType.equals(ConstantUtil.CREATE)) {
            mTempUser = (DeviceUser) getIntent().getExtras().getSerializable(BleMsg.KEY_TEMP_USER);
            mNodeId = mDefaultDevice.getDeviceNodeId();
            mUserNameEt.setText(getResources().getString(R.string.password) + 1);
            mTitleTv.setText(R.string.create_pwd);
        } else {
            mModifyDeviceKey = (DeviceKey) getIntent().getSerializableExtra(BleMsg.KEY_MODIFY_DEVICE_KEY);
            mUserNameEt.setText(mModifyDeviceKey.getKeyName());
            mNodeId = mModifyDeviceKey.getDeviceNodeId();
            mTitleTv.setText(R.string.modify_pwd);
        }

        mBleManagerHelper = BleManagerHelper.getInstance(this, mDefaultDevice.getBleMac(), false);
        mHandler = new Handler();
        LocalBroadcastManager.getInstance(this).registerReceiver(pwdReceiver, intentFilter());
    }

    private void initEvent() {
        mModifyPwdBtn.setOnClickListener(this);
        mSetPwdBtn.setOnClickListener(this);
        mBackIv.setOnClickListener(this);
    }

    private static IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.STR_RSP_SERVER_DATA);
        intentFilter.addAction(BleMsg.STR_RSP_MSG1E_ERRCODE);
        intentFilter.addAction(BleMsg.STR_RSP_MSG16_LOCKID);
        intentFilter.addAction(BleMsg.STR_RSP_MSG1A_STATUS);
        intentFilter.addAction(BleMsg.STR_RSP_MSG18_TIMEOUT);
        return intentFilter;
    }

    private final BroadcastReceiver pwdReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //MSG1E 设备->apk，返回信息
            if (action.equals(BleMsg.STR_RSP_MSG1E_ERRCODE)) {
                final byte[] errCode = intent.getByteArrayExtra(BleMsg.KEY_ERROR_CODE);
                mHandler.removeCallbacks(mRunnable);
                DialogUtils.closeDialog(mLoadDialog);


                if (errCode[3] == 0x0b) {
                    showMessage(PwdSetActivity.this.getResources().getString(R.string.add_pwd_failed));
                } else if (errCode[3] == 0x0c) {
                    showMessage(PwdSetActivity.this.getResources().getString(R.string.modify_pwd_success));
                    mModifyDeviceKey.setPwd(mFirstPwdEt.getText().toString().trim());
                    LogUtil.d(TAG, "mModifyDeviceKey = " + mModifyDeviceKey.toString());
                    DeviceKeyDao.getInstance(PwdSetActivity.this).updateDeviceKey(mModifyDeviceKey);
                    mSetPwdBtn.setEnabled(false);
                } else if (errCode[3] == 0x0d) {
                    showMessage(PwdSetActivity.this.getResources().getString(R.string.delete_pwd_success));
                }
            }

            if (action.equals(BleMsg.STR_RSP_MSG16_LOCKID)) {
                DeviceKey key = (DeviceKey) intent.getExtras().getSerializable(BleMsg.KEY_SERIALIZABLE);
                if (key == null || (key.getKeyType() != 0)) {
                    mHandler.removeCallbacks(mRunnable);
                    DialogUtils.closeDialog(mLoadDialog);
                    return;
                }

                final byte[] lockId = intent.getByteArrayExtra(BleMsg.KEY_LOCK_ID);
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
                DeviceKeyDao.getInstance(PwdSetActivity.this).insert(deviceKey);
                if (mTempUser != null) {
                    mTempUser.setUserStatus(ConstantUtil.USER_ENABLE);
                    DeviceUserDao.getInstance(PwdSetActivity.this).updateDeviceUser(mTempUser);
                }
                showMessage(PwdSetActivity.this.getResources().getString(R.string.set_pwd_success));
                mHandler.removeCallbacks(mRunnable);
                DialogUtils.closeDialog(mLoadDialog);

                finish();
            }

            if (action.equals(BleMsg.STR_RSP_MSG1A_STATUS)) {

            }

            if (action.equals(BleMsg.STR_RSP_MSG18_TIMEOUT)) {
                Log.d(TAG, "STR_RSP_MSG18_TIMEOUT");
                byte[] seconds = intent.getByteArrayExtra(BleMsg.KEY_TIME_OUT);
                Log.d(TAG, "seconds = " + Arrays.toString(seconds));
                closeDialog((int) seconds[0]);
            }
        }
    };


    /**
     * 验证密码
     */
    private void checkPwd() {
        final String firstPwd = mFirstPwdEt.getText().toString().trim();
        final String secPwd = mSecondPwdEt.getText().toString().trim();
        final String userName = mUserNameEt.getText().toString().trim();

        if (TextUtils.isEmpty(firstPwd) || firstPwd.length() != 6) {
            try {
                controller.alertDialog(getResources().getString(R.string.valid_password));
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } else if (!TextUtils.isEmpty(secPwd) && !firstPwd.equals(secPwd)) {
            controller.alertDialog(getResources().getString(R.string.pwd_twice_error));
        } else if (TextUtils.isEmpty(userName)) {
            controller.alertDialog(getResources().getString(R.string.plz_input_username));
        } else {
            if (mCmdType.equals(ConstantUtil.CREATE)) {
                int count = DeviceKeyDao.getInstance(this).queryDeviceKey(mNodeId, mDefaultDevice.getUserId(), ConstantUtil.USER_PWD).size();

                if (count >= 0 && count < 1) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog = DialogUtils.createLoadingDialog(this, getResources().getString(R.string.data_loading));
                    closeDialog(5);
                    if (mTempUser != null)
                        mBleManagerHelper.getBleCardService().sendCmd15((byte) 0, (byte) 0, mTempUser.getUserId(), (byte) 0, Integer.parseInt(firstPwd));
                    else
                        mBleManagerHelper.getBleCardService().sendCmd15((byte) 0, (byte) 0, mDefaultDevice.getUserId(), (byte) 0, Integer.parseInt(firstPwd));

                } else {
                    showMessage(getResources().getString(R.string.add_pwd_tips));
                }
            } else {
                DialogUtils.closeDialog(mLoadDialog);
                mLoadDialog = DialogUtils.createLoadingDialog(this, getResources().getString(R.string.data_loading));
                closeDialog(5);
                mBleManagerHelper.getBleCardService().sendCmd15((byte) 2, (byte) 0, mModifyDeviceKey.getUserId(), (byte) 0, Integer.parseInt(firstPwd));
            }

        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_edit:
                break;
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
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(pwdReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
    }

    /**
     * 超时提醒
     *
     * @param seconds
     */
    private void closeDialog(final int seconds) {

        mHandler.removeCallbacks(mRunnable);

        mHandler.postDelayed(mRunnable, seconds * 1000);
    }
}
