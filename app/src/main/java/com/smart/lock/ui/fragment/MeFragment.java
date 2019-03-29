
package com.smart.lock.ui.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.ui.AboutUsActivity;
import com.smart.lock.ui.LockDetectingActivity;
import com.smart.lock.ui.setting.DeviceManagementActivity;
import com.smart.lock.ui.setting.SystemSettingsActivity;
import com.smart.lock.utils.DialogUtils;
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
    private MeDefineView mDevManagementTv;
    private MeDefineView mAboutUsTv;
    private TextView mNameTv;
    private ImageView mEditNameIv;

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
        mDevManagementTv = mMeView.findViewById(R.id.mc_manage);
        mAboutUsTv = mMeView.findViewById(R.id.about_us);
        mNameTv = mMeView.findViewById(R.id.me_center_head_name);
        mEditNameIv = mMeView.findViewById(R.id.me_edit_name);

        mDefaultDevice = DeviceInfoDao.getInstance(mMeView.getContext()).queryFirstData("device_default", true);
        if (mDefaultDevice != null) {
            mDefaultUser = DeviceUserDao.getInstance(mMeView.getContext()).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId());
        }
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
        mDevManagementTv.setDes(mMeView.getResources().getString(R.string.device_management));
        mAboutUsTv.setDes(mMeView.getResources().getString(R.string.about_us));
        if (mDefaultUser != null) {
            mNameTv.setText(mDefaultUser.getUserName());
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_me, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                searchDev();
                break;
            case R.id.action_scan:
                scanQr();
                break;
            default:
                break;

        }
        return true;
    }

    private void initEvent() {
        mSystemSetTv.setOnClickListener(this);
        mDevManagementTv.setOnClickListener(this);
        mAboutUsTv.setOnClickListener(this);
        mEditNameIv.setOnClickListener(this);
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
                if (dvInfo.length == 3 && dvInfo[0].length() == 18 && dvInfo[1].length() == 12 && dvInfo[2].length() == 15) {
                    mSn = dvInfo[0];
                    mBleMac = dvInfo[1];
                    mNodeId = dvInfo[2];

                    if (DeviceInfoDao.getInstance(mActivity).queryByField(DeviceInfoDao.NODE_ID, "0" + mNodeId) == null) {
                        Bundle bundle = new Bundle();
                        bundle.putString(BleMsg.KEY_BLE_MAC, mBleMac);
                        bundle.putString(BleMsg.KEY_NODE_SN, mSn);
                        bundle.putString(BleMsg.KEY_NODE_ID, mNodeId);
                        LogUtil.d(TAG, "mac = " + mBleMac + '\n' +
                                " sn = " + mSn + "\n" +
                                "mNodeId = " + mNodeId);
                        startIntent(LockDetectingActivity.class, bundle);
                    } else {
                        ToastUtil.show(mActivity, getString(R.string.device_has_been_added), Toast.LENGTH_LONG);
                    }
                } else {
                    ToastUtil.show(mMeView.getContext(), getString(R.string.plz_scan_correct_qr), Toast.LENGTH_LONG);
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mc_manage:
                Bundle bundle = new Bundle();
                if (mDefaultDevice != null) {
                    bundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);
                }
                startIntent(DeviceManagementActivity.class, bundle);
                break;
            case R.id.system_set:
                Intent intent = new Intent(mMeView.getContext(), SystemSettingsActivity.class);
                this.startActivity(intent);
                break;
            case R.id.about_us:
                Intent aboutIntent = new Intent(mMeView.getContext(), AboutUsActivity.class);
                this.startActivity(aboutIntent);
                break;
            case R.id.me_edit_name:
                final AlertDialog editDialog = DialogUtils.showEditDialog(mMeView.getContext(), getString(R.string.modify_note_name), mDefaultUser);
                editDialog.show();
                if (editDialog != null) {
                    editDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            mNameTv.setText(DeviceUserDao.getInstance(mMeView.getContext()).queryUser(mDefaultUser.getDevNodeId(), mDefaultUser.getUserId()).getUserName());
                        }
                    });
                }
                break;
            default:
                break;
        }
    }

    private void searchDev() {
//        Bundle bundle = new Bundle();

        startIntent(LockDetectingActivity.class, null);
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
        config.setFullScreenScan(true);//是否全屏扫描  默认为true  设为false则只会在扫描框中扫描
        newIntent.putExtra(Constant.INTENT_ZXING_CONFIG, config);
        startActivityForResult(newIntent, REQUEST_CODE_SCAN);
    }
}
