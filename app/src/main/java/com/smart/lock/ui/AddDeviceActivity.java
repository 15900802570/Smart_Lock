
package com.smart.lock.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.permission.PermissionHelper;
import com.smart.lock.permission.PermissionInterface;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.ToastUtil;
import com.yzq.zxinglibrary.android.CaptureActivity;
import com.yzq.zxinglibrary.bean.ZxingConfig;
import com.yzq.zxinglibrary.common.Constant;


public class AddDeviceActivity extends BaseActivity implements OnClickListener, PermissionInterface {
    private static final int REQUEST_CODE_SCAN = 0;
    private static final String TAG = "AddDeviceActivity";

    private Button mScanQrBt;
    private Button mManualAddBtn;
    private ImageView mBackIv;
    private PermissionHelper mPermissionHelper;
    private static final int REQ_CODE_CAMERA = 1;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                    ToastUtil.show(AddDeviceActivity.this, getString(R.string.plz_scan_correct_qr), Toast.LENGTH_LONG);
                }
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_lock_select_way);

        initView();
        initEvent();
        mPermissionHelper = new PermissionHelper(this, this);
    }

    private void initView() {
        mScanQrBt = findViewById(R.id.btn_scan_qr);
        mManualAddBtn = findViewById(R.id.btn_manual_addition);
        mBackIv = findViewById(R.id.iv_back);
    }

    private void initEvent() {
        mScanQrBt.setOnClickListener(this);
        mManualAddBtn.setOnClickListener(this);
        mBackIv.setOnClickListener(this);
    }

    protected void onDestroy() {
        super.onDestroy();
    }


    protected void onResume() {
        super.onResume();
    }


    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 4) {
            finish();
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan_qr:
                mPermissionHelper.requestPermissions(Manifest.permission.CAMERA, 1);
                break;
            case R.id.btn_manual_addition:
                break;
            case R.id.iv_back:
                finish();
                break;
            default:
                break;
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

    /**
     * 打开第三方二维码扫描库
     */
    private void scanQr() {
        Intent newIntent = new Intent(this, CaptureActivity.class);
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

    /**
     * 重写Activity的权限请求返回结果方法
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mPermissionHelper.requestPermissionsResult(requestCode, permissions, grantResults); // 接管结果判断
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public void requestPermissionsSuccess(int callBackCode) {
        if (callBackCode == REQ_CODE_CAMERA) {
            scanQr();
        }
    }

    @Override
    public void requestPermissionsFail(int callBackCode) {
        if (callBackCode == REQ_CODE_CAMERA) {
            showMessage("您拒绝了权限申请！");
        }
    }
}
