package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.entity.Device;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.ToastUtil;

import java.util.Arrays;

@SuppressLint("Registered")
public class SelfCheckActivity extends AppCompatActivity implements View.OnClickListener, UiListener {


    private String TAG = "SelfCheckActivity";
    private ImageView mBackIv;
    private ImageView mKeyIv;
    private ImageView mFPIv;
    private ImageView mNFCIv;

    private Button mCheckBtn;
    private Button mRepairBtn;

    private BleManagerHelper mBleManagerHelper;

    private Dialog mWaitingDialog = null;
    private Dialog mWaitingDialogWithRepair = null;

    private int mErrorCounter = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_self_check);
        initView();
        initData();
        initEvent();
    }

    private void initView() {
        mBackIv = findViewById(R.id.iv_self_check_back);
        mKeyIv = findViewById(R.id.iv_self_check_key);
        mFPIv = findViewById(R.id.iv_self_check_fp);
        mNFCIv = findViewById(R.id.iv_self_check_nfc);
        mCheckBtn = findViewById(R.id.btn_self_check_check);
        mRepairBtn = findViewById(R.id.btn_self_repair);
    }

    private void initData() {
        mBleManagerHelper = BleManagerHelper.getInstance(this);
        mBleManagerHelper.addUiListener(this);
        mWaitingDialog = DialogUtils.createLoadingDialog(this, getString(R.string.checking));
        mWaitingDialogWithRepair = DialogUtils.createLoadingDialog(this, getString(R.string.repair_));
    }

    private void initEvent() {
        mBackIv.setOnClickListener(this);
        mCheckBtn.setOnClickListener(this);
        mRepairBtn.setOnClickListener(this);
        mRepairBtn.setVisibility(View.GONE);
        if (Device.getInstance(this).getState() == Device.BLE_CONNECTED) {
            sendCmd19(BleMsg.TYPE_DETECTION_LOCK_EQUIPMENT);
        } else {
            ToastUtil.showLong(this, getString(R.string.ble_disconnect));
        }
    }

    private void sendCmd19(byte type) {
        LogUtil.d(TAG, Byte.toString(type));
        mBleManagerHelper.getBleCardService().sendCmd19(type);
        DialogUtils.closeDialog(mWaitingDialogWithRepair);
        DialogUtils.closeDialog(mWaitingDialog);
        if (!this.isFinishing() && type == BleMsg.TYPE_SELF_REPAIR) {
            if (!mWaitingDialogWithRepair.isShowing())
                mWaitingDialogWithRepair.show();
        } else {
            if (!mWaitingDialog.isShowing())
                mWaitingDialog.show();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_self_check_back:
                finish();
                break;
            case R.id.btn_self_check_check:
                if (Device.getInstance(this).getState() == Device.BLE_CONNECTED) {
                    sendCmd19(BleMsg.TYPE_DETECTION_LOCK_EQUIPMENT);
                } else {
                    ToastUtil.showLong(this, getString(R.string.ble_disconnect));
                }
                break;

            case R.id.btn_self_repair:
                if (Device.getInstance(this).getState() == Device.BLE_CONNECTED) {
                    sendCmd19(BleMsg.TYPE_SELF_REPAIR);
                } else {
                    ToastUtil.showLong(this, getString(R.string.ble_disconnect));
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void deviceStateChange(Device device, int state) {
        switch (state) {
            case BleMsg.STATE_DISCONNECTED:
                DialogUtils.closeDialog(mWaitingDialog);
                DialogUtils.closeDialog(mWaitingDialogWithRepair);
                break;
            case BleMsg.STATE_CONNECTED:

                break;
            case BleMsg.GATT_SERVICES_DISCOVERED:
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

    @SuppressLint("SetTextI18n")
    @Override
    public void dispatchUiCallback(Message msg, Device device, int type) {
        Bundle extra = msg.getData();

        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_1A:

                byte key = extra.getByte(BleMsg.KEY_TYPE);
                byte[] status = extra.getByteArray(BleMsg.KEY_STATUS);
                LogUtil.d(TAG, "key = " + key+ '\n' +
                        "status " + Arrays.toString(status));
                if (key == 0x00 && status != null) {
                    mErrorCounter = 0;
                    if ((status[3] & 0x01) == 1) {
                        mKeyIv.setImageResource(R.mipmap.icon_problem);
                        mErrorCounter++;
                    } else {
                        mKeyIv.setImageResource(R.mipmap.icon_ok);
                    }
                    if ((status[3] & 0x02) == 2) {
                        mNFCIv.setImageResource(R.mipmap.icon_problem);
                        mErrorCounter++;
                    } else {
                        mNFCIv.setImageResource(R.mipmap.icon_ok);
                    }
                    if ((status[3] & 0x04) == 4) {
                        mFPIv.setImageResource(R.mipmap.icon_problem);
                        mErrorCounter++;
                    } else {
                        mFPIv.setImageResource(R.mipmap.icon_ok);
                    }
                }
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_1E:
                final byte[] errCode = extra.getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode != null && errCode[3] == BleMsg.TYPE_SELF_REPAIR_COMPLETE) {
                    sendCmd19(BleMsg.TYPE_DETECTION_LOCK_EQUIPMENT);
                }
                break;
            default:
                break;
        }
        DialogUtils.closeDialog(mWaitingDialog);
        DialogUtils.closeDialog(mWaitingDialogWithRepair);
        if (mErrorCounter != 0) {
            ((TextView) findViewById(R.id.tv_self_check_tips)).setText(
                    mErrorCounter + getString(R.string.exception) + "," +
                            (3 - mErrorCounter) + getString(R.string.one_normal));
            mRepairBtn.setVisibility(View.VISIBLE);
        } else {
            ((TextView) findViewById(R.id.tv_self_check_tips)).setText(getString(R.string.all_parts_are_working_properly));
            mRepairBtn.setVisibility(View.GONE);
        }

    }

    @Override
    public void reConnectBle(Device device) {

    }

    @Override
    public void sendFailed(Message msg) {

    }

    @Override
    public void addUserSuccess(Device device) {

    }

    @Override
    public void scanDevFailed() {

    }

}
