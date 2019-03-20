
package com.smart.lock.ui.fragment;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.ui.LockDetectingActivity;
import com.smart.lock.ui.setting.DeviceManagementActivity;
import com.smart.lock.ui.setting.SystemSettingsActivity;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.MeDefineView;
import com.yzq.zxinglibrary.android.CaptureActivity;
import com.yzq.zxinglibrary.bean.ZxingConfig;
import com.yzq.zxinglibrary.common.Constant;

import static android.app.Activity.RESULT_OK;

public class MeFragment extends BaseFragment implements View.OnClickListener {
    private View mMeView;
    private Toolbar mToolbar;
    private MeDefineView mSystemSetTv;
    private MeDefineView mScanQrMv;
    private MeDefineView mDevManagementTv;
    private TextView mNameTv;

    protected String mSn; //设备SN
    protected String mNodeId; //设备IMEI
    protected String mBleMac; //蓝牙地址

    private static final int REQUEST_CODE_SCAN = 0;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public View initView() {
        mMeView = View.inflate(mActivity, R.layout.me_fragment, null);
        mSystemSetTv = mMeView.findViewById(R.id.system_set);
        mScanQrMv = mMeView.findViewById(R.id.mv_scan_qr);
        mDevManagementTv = mMeView.findViewById(R.id.mc_manage);
        mNameTv = mMeView.findViewById(R.id.me_center_head_name);
        mDefaultDevice = DeviceInfoDao.getInstance(mMeView.getContext()).queryFirstData("device_default", true);
        if (mDefaultDevice != null) {
            mDefaultUser = DeviceUserDao.getInstance(mMeView.getContext()).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId());
        }
        Log.d(TAG, "initView");
        initEvent();
        return mMeView;
    }


    public void initDate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {  //版本检测
            mToolbar = mMeView.findViewById(R.id.tb_toolbar);
            ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);  //将ToolBar设置成ActionBar
        }
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
        mSystemSetTv.setDes(mMeView.getContext().getResources().getString(R.string.system_setting));
        mScanQrMv.setDes(mMeView.getContext().getResources().getString(R.string.scan_qr));
        mDevManagementTv.setDes(mMeView.getResources().getString(R.string.device_management));
        if (mDefaultUser != null) {
            mNameTv.setText(mDefaultUser.getUserName());
        }

    }

    private void initEvent() {
        mSystemSetTv.setOnClickListener(this);
        mScanQrMv.setOnClickListener(this);
        mDevManagementTv.setOnClickListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 扫描二维码/条码回传
        if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK) {
            if (data != null) {
                String content = data.getStringExtra(Constant.CODED_CONTENT);
                LogUtil.d(TAG, "content = " + content);
                String[] dvInfo = content.split(",");
                if (dvInfo.length == 3 && dvInfo[0].length() == 12 && dvInfo[1].length() == 12 && dvInfo[2].length() == 15) {
                    mSn = dvInfo[0];
                    mBleMac = dvInfo[1];
                    mNodeId = dvInfo[2];

                    Bundle bundle = new Bundle();
                    bundle.putString(BleMsg.KEY_BLE_MAC, mBleMac);
                    bundle.putString(BleMsg.KEY_NODE_SN, mSn);
                    bundle.putString(BleMsg.KEY_NODE_ID, mNodeId);

                    startIntent(LockDetectingActivity.class, bundle);
                } else {
                    ToastUtil.show(mMeView.getContext(), getString(R.string.plz_scan_correct_qr), Toast.LENGTH_LONG);
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mv_scan_qr:
                scanQr();
                break;
            case R.id.self_message:
                Log.e("self_message", "0012012");
                break;
            case R.id.mc_manage:
                Bundle bundle = new Bundle();
                bundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);
                Intent devManageInstant = new Intent(this.mActivity, DeviceManagementActivity.class);
                this.startActivity(devManageInstant, bundle);
                break;
            case R.id.sent_repair:
                Log.e("sent_repair", "0012012");
                break;
            case R.id.system_set:
                Intent intent = new Intent(this.mActivity, SystemSettingsActivity.class);
                this.startActivity(intent);
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    /**
     * 打开第三方二维码扫描库
     */
    private void scanQr() {
        Intent newIntent = new Intent(mMeView.getContext(), CaptureActivity.class);
        ZxingConfig config = new ZxingConfig();
        config.setPlayBeep(true);//是否播放扫描声音 默认为true
        config.setShake(true);//是否震动  默认为true
        config.setDecodeBarCode(false);//是否扫描条形码 默认为true
        config.setReactColor(R.color.colorAccent);//设置扫描框四个角的颜色 默认为淡蓝色
        config.setFrameLineColor(R.color.colorAccent);//设置扫描框边框颜色 默认无色
        config.setFullScreenScan(false);//是否全屏扫描  默认为true  设为false则只会在扫描框中扫描
        newIntent.putExtra(Constant.INTENT_ZXING_CONFIG, config);
        startActivityForResult(newIntent, REQUEST_CODE_SCAN);
    }
}
