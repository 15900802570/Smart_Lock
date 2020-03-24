package com.smart.lock.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.MainBaseActivity;
import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.entity.Device;
import com.smart.lock.utils.StringUtil;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class LpcdTestActivity extends MainBaseActivity implements View.OnClickListener, UiListener {

    private TextView mContent;
    private Button mQueryBtn, mCancelBtn;
    private ImageView mBackIv;

    private Device mDevice;
    private BleManagerHelper mBleManagerHelper;

    private ArrayList<String> mTypes;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lpcd);
        initView();
        initData();
    }


    private void initData() {
        mDevice = Device.getInstance(this);
        mBleManagerHelper = BleManagerHelper.getInstance(this);
        mBleManagerHelper.addUiListener(this);
        mTypes = new ArrayList<>();
        mTypes.add("lpcd_Timer1");
        mTypes.add("lpcd_Timer2");
        mTypes.add("lpcd_Timer3");
        mTypes.add("lpcd_Timer3_Div");
        mTypes.add("lpcd_Timer3_Offset");
        mTypes.add("lpcd_TimerVmid");
        mTypes.add("lpcd_Fullscale_Value");
        mTypes.add("lpcd_Value");
        mTypes.add("lpcd_Irq");

        mTypes.add("calibration_Reference");
        mTypes.add("calibration_Value");
        mTypes.add("calibration_Threshold_Max");
        mTypes.add("calibration_Threshold_Min");
        mTypes.add("calibration_Gain_Index");
        mTypes.add("calibration_Driver_Index");
        mTypes.add("calibration_Range_L");
        mTypes.add("calibration_Range_H");

        mTypes.add("calibration_backup_Reference");
        mTypes.add("calibration_backup_Gain_Index");
        mTypes.add("calibration_backup_Driver_Index");
    }

    private void initView() {
        mContent = findViewById(R.id.content);
        mQueryBtn = findViewById(R.id.btn_query);
        mCancelBtn = findViewById(R.id.btn_clear);
        mBackIv = findViewById(R.id.iv_back);
        mQueryBtn.setOnClickListener(this);
        mCancelBtn.setOnClickListener(this);
        mBackIv.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_query:
                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    mBleManagerHelper.getBleCardService().sendCmd61(BleMsg.INT_DEFAULT_TIMEOUT);
                } else {
                    showMessage(getString(R.string.disconnect_ble));
                }
                break;
            case R.id.btn_clear:
                mContent.setText("");
                break;
            case R.id.iv_back:
                finish();
                break;
            default:
                break;
        }

    }

    protected void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void deviceStateChange(Device device, int state) {

    }

    @Override
    public void dispatchUiCallback(Message msg, Device device, int type) {
        mDevice = device;
        Bundle bundle = msg.getData();
        switch (msg.getType()) {

            case Message.TYPE_BLE_RECEIVER_CMD_62:
                byte[] extra = bundle.getByteArray(BleMsg.RECEIVER_DATA);

                if (extra != null) {
                    String content = StringUtil.bytesToHexString(extra, ":");
                    String[] res = content.split(":");
                    for (int i = 0; i < res.length; i++) {
                        String ret = res[i];
                        mContent.append(mTypes.get(i));
                        mContent.append(":");
                        mContent.append(" " + ret);
                        mContent.append("\n");
                    }
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

    }

    @Override
    public void addUserSuccess(Device device) {

    }

    @Override
    public void scanDevFailed() {

    }
}
