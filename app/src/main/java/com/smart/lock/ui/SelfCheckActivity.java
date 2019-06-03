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
    private int mBleStatus;

    private BleManagerHelper mBleManagerHelper;

    private Dialog mWaitingDialog;

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
    }

    private void initData() {
        mBleManagerHelper = BleManagerHelper.getInstance(this);
        mBleManagerHelper.addUiListener(this);
        mWaitingDialog = DialogUtils.createLoadingDialog(this, this.getString(R.string.checking));
    }

    private void initEvent() {
        mBackIv.setOnClickListener(this);
        mCheckBtn.setOnClickListener(this);
        if (Device.getInstance(this).getState() == Device.BLE_CONNECTED) {
            sendCmd19();
        } else {
            ToastUtil.showLong(this, getString(R.string.ble_disconnect));
        }
    }

    private void sendCmd19() {
        LogUtil.d(TAG, "sendCmd19 0");
        mBleManagerHelper.getBleCardService().sendCmd19(BleMsg.TYPE_DETECTION_LOCK_EQUIPMENT);
        mWaitingDialog.show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_self_check_back:
                finish();
                break;
            case R.id.btn_self_check_check:
                if (Device.getInstance(this).getState() == Device.BLE_CONNECTED) {
                    sendCmd19();
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
        if (state != Device.BLE_CONNECTED) {
            DialogUtils.closeDialog(mWaitingDialog);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void dispatchUiCallback(Message msg, Device device, int type) {
        Bundle extra = msg.getData();

        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_1A:

                byte key = extra.getByte(BleMsg.KEY_TYPE);
                byte[] status = extra.getByteArray(BleMsg.KEY_STATUS);
                LogUtil.d(TAG, "key = " + String.valueOf(key) + '\n' +
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
            default:
                break;
        }
        if (mErrorCounter != 0) {
            ((TextView) findViewById(R.id.tv_self_check_tips)).setText(
                    mErrorCounter + getString(R.string.exception) + "," +
                            (3 - mErrorCounter) + getString(R.string.one_normal));
        } else {
            ((TextView) findViewById(R.id.tv_self_check_tips)).setText(getString(R.string.all_parts_are_working_properly));
        }
        DialogUtils.closeDialog(mWaitingDialog);
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
