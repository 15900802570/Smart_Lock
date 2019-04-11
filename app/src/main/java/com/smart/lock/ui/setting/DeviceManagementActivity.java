package com.smart.lock.ui.setting;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceStatusDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.scan.ScanQRHelper;
import com.smart.lock.scan.ScanQRResultInterface;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.SharedPreferenceUtil;
import com.smart.lock.utils.ToastUtil;

import java.util.ArrayList;


public class DeviceManagementActivity extends AppCompatActivity implements ScanQRResultInterface {

    private static String TAG = "DeviceManagementActivity";

    private RecyclerView mDevManagementRv;
    private DevManagementAdapter mDevManagementAdapter;

    private Dialog mAddNewDevDialog;
    private ScanQRHelper mScanQRHelper;


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case ConstantUtil.SCAN_QRCODE_REQUEST_CODE:
                    mScanQRHelper.ScanDoCode(data);
                    break;
                case ConstantUtil.SETTING_PWD_REQUEST_CODE:
                    if (data.getExtras().getInt(ConstantUtil.CONFIRM) == 1) {
                        SharedPreferenceUtil.getInstance(this).
                                writeBoolean(ConstantUtil.NUM_PWD_CHECK, true);
                        ToastUtil.showLong(this,
                                getResources().getString(R.string.pwd_setting_successfully));
                        finish();

                    } else {
                        ToastUtil.showLong(this,
                                getResources().getString(R.string.pwd_setting_failed));
                        finish();
                    }
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.d(TAG, "Device");
        setContentView(R.layout.activity_device_manager);
        initView();
        initData();
        initEvent();
    }

    private void initView() {
        mDevManagementRv = findViewById(R.id.dev_management_list_view);
    }

    private void initData() {
        mDevManagementAdapter = new DevManagementAdapter(this);
        mDevManagementRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mDevManagementRv.setItemAnimator(new DefaultItemAnimator());
        mDevManagementRv.setAdapter(mDevManagementAdapter);
        mScanQRHelper = new ScanQRHelper(this, this);
    }

    private void initEvent() {

    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_dev_management_back:
                finish();
                break;
            case R.id.btn_dev_management_add_new_lock:
                mScanQRHelper.scanQr();
                break;
            default:
                break;
        }
    }

    public void tipsOnClick(View view) {
        switch (view.getId()) {
            case R.id.dialog_cancel_btn:
                mAddNewDevDialog.cancel();
                break;
            case R.id.dialog_confirm_btn:
                mAddNewDevDialog.cancel();
                mScanQRHelper.scanQr();
                break;
        }
    }

    @Override
    public void onAuthenticationSuccess(DeviceInfo deviceInfo) {
        mDevManagementAdapter.addItem(deviceInfo);
        mDevManagementAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAuthenticationFailed() {
        mAddNewDevDialog = DialogUtils.createTipsDialogWithConfirmAndCancel(DeviceManagementActivity.this, getString(R.string.disconnect_ble_first));
        mAddNewDevDialog.show();
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

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private class DevManagementAdapter extends RecyclerView.Adapter<DevManagementAdapter.MyViewHolder> {

        private Context mContext;
        private ArrayList<DeviceInfo> mDevList;
        private DeviceInfo mDefaultInfo;
        int mDefaultPosition;

        private DevManagementAdapter(Context context) {
            mContext = context;
            mDevList = DeviceInfoDao.getInstance(DeviceManagementActivity.this).queryAll();
        }

        private void addItem(DeviceInfo deviceInfo) {
            mDevList.add(0, deviceInfo);
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_recycler_dev_management, viewGroup, false);
            SwipeLayout swipeLayout = inflate.findViewById(R.id.item_ll_dev_management);
            swipeLayout.setClickToClose(true);
            swipeLayout.setRightSwipeEnabled(true);
            return new MyViewHolder(inflate);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, @SuppressLint("RecyclerView") final int position) {
            final DeviceInfo deviceInfo = mDevList.get(position);
            if (deviceInfo != null) {
                try {
                    myViewHolder.mLockNameTv.setText(deviceInfo.getDeviceName());
                    myViewHolder.mLockNumTv.setText(String.valueOf(deviceInfo.getBleMac()));
                } catch (NullPointerException e) {
                    LogUtil.d(TAG, deviceInfo.getDeviceName() + "  " + deviceInfo.getDeviceIndex());
                }
                if (deviceInfo.getDeviceDefault()) {
                    myViewHolder.mDefaultFlagIv.setImageResource(R.drawable.ic_selected);
                    myViewHolder.mDefaultTv.setVisibility(View.VISIBLE);
                    mDefaultInfo = deviceInfo;
                    mDefaultPosition = position;
                } else {
                    myViewHolder.mDefaultFlagIv.setImageResource(R.drawable.ic_select);
                    myViewHolder.mDefaultTv.setVisibility(View.INVISIBLE);
                }

                myViewHolder.mSetDefaultLl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDefaultInfo.setDeviceDefault(false);
                        DeviceInfoDao.getInstance(DeviceManagementActivity.this).updateDeviceInfo(mDefaultInfo);
                        deviceInfo.setDeviceDefault(true);
                        DeviceInfoDao.getInstance(DeviceManagementActivity.this).updateDeviceInfo(deviceInfo);
                        mDevList = DeviceInfoDao.getInstance(DeviceManagementActivity.this).queryAll();
                        mDevManagementAdapter.notifyDataSetChanged();
                        LogUtil.d(TAG, "设置为默认设备");
                    }
                });
                myViewHolder.mUnbindLl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (deviceInfo.getDeviceDefault()) {
                            BleManagerHelper.getInstance(DeviceManagementActivity.this, false).getBleCardService().disconnect();
                        }
                        DeviceUserDao.getInstance(DeviceManagementActivity.this).
                                deleteByKey(DeviceUserDao.DEVICE_NODE_ID, deviceInfo.getDeviceNodeId());
                        DeviceKeyDao.getInstance(DeviceManagementActivity.this).
                                deleteByKey(DeviceKeyDao.DEVICE_NODE_ID, deviceInfo.getDeviceNodeId());
                        DeviceStatusDao.getInstance(DeviceManagementActivity.this).
                                deleteByKey(DeviceStatusDao.DEVICE_NODEID, deviceInfo.getDeviceNodeId());
                        DeviceInfoDao.getInstance(DeviceManagementActivity.this).
                                delete(deviceInfo);
                        mDevList.remove(position);
                        mDevManagementAdapter.notifyDataSetChanged();
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return mDevList.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            SwipeLayout mSwipeLayout;
            private TextView mLockNameTv;
            private TextView mLockNumTv;
            private TextView mDefaultTv;
            private ImageView mDefaultFlagIv;
            private LinearLayout mSetDefaultLl;

            private LinearLayout mUnbindLl;

            private MyViewHolder(View itemView) {
                super(itemView);
                mSwipeLayout = (SwipeLayout) itemView;
                mLockNameTv = itemView.findViewById(R.id.tv_dev_management_dev_name);
                mLockNumTv = itemView.findViewById(R.id.tv_dev_management_dev_num);
                mDefaultTv = itemView.findViewById(R.id.tv_dev_management_default);
                mDefaultFlagIv = itemView.findViewById(R.id.iv_dev_management_default_flag);
                mSetDefaultLl = itemView.findViewById(R.id.ll_dev_management_set_default);
                mUnbindLl = itemView.findViewById(R.id.ll_unbind);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        long l = DeviceInfoDao.getInstance(DeviceManagementActivity.this).queryCount();
        LogUtil.d(TAG,"type = "+ l +'\n'+
                "count = " + mDevManagementAdapter.getItemCount());
        if (l > mDevManagementAdapter.getItemCount()) {
            mDevManagementAdapter.addItem(DeviceInfoDao.getInstance(DeviceManagementActivity.this).getNewDeviceInfo());
            mDevManagementAdapter.notifyDataSetChanged();
        }
    }
}
