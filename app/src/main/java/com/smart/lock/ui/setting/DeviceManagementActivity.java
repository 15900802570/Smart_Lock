package com.smart.lock.ui.setting;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.helper.DtComFunHelper;
import com.smart.lock.entity.Device;
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
    private Context mCtx;
    private BleManagerHelper mBleManagerHelper;

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
        mCtx = this;
        mBleManagerHelper = BleManagerHelper.getInstance(this);
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
        mDevManagementAdapter.refreshList();
        LogUtil.d(TAG, "NewDevice = " + deviceInfo);
        mDevManagementAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAuthenticationFailed() {
        mAddNewDevDialog = DialogUtils.createTipsDialogWithConfirmAndCancel(mCtx, getString(R.string.disconnect_ble_first));
        mAddNewDevDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mScanQRHelper.getPermissionHelper().requestPermissionsResult(requestCode, permissions, grantResults);
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

    private class DevManagementAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_HEAD = 0;
        private static final int TYPE_BODY = 1;
        private static final int TYPE_FOOT = 2;
        private int countHead = 0;
        private int countFoot = 2;
        private Context mContext;
        private ArrayList<DeviceInfo> mDevList;
        private DeviceInfo mDefaultInfo;
        int mDefaultPosition;


        private int getBodySize() {
            return mDevList.size();
        }

        private boolean isHead(int position) {
            return countHead != 0 && position < countHead;
        }

        private boolean isFoot(int position) {
            return countFoot != 0 && (position >= (getBodySize() + countHead));
        }

        public int getItemViewType(int position) {
            if (isHead(position)) {
                return TYPE_HEAD;
            } else if (isFoot(position)) {
                return TYPE_FOOT;
            } else {
                return TYPE_BODY;
            }
        }

        private DevManagementAdapter(Context context) {
            mContext = context;
            mDevList = DeviceInfoDao.getInstance(mCtx).queryAll();
        }

        private void addItem(DeviceInfo deviceInfo) {
            if (mDevList.indexOf(deviceInfo) == -1) {
                mDevList.add(0, deviceInfo);
            }
        }

        private void refreshList() {
            mDevList = DeviceInfoDao.getInstance(mCtx).queryAll();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            switch (viewType) {
                case TYPE_HEAD:
                    return new FootViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_recycle_foot, viewGroup, false));

                case TYPE_BODY:
                    View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_recycler_dev_management, viewGroup, false);
                    SwipeLayout swipeLayout = inflate.findViewById(R.id.item_ll_dev_management);
                    swipeLayout.setClickToClose(true);
                    swipeLayout.setRightSwipeEnabled(true);
                    return new MyViewHolder(inflate);
                case TYPE_FOOT:
                    return new FootViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_recycle_foot, viewGroup, false));
                default:
                    return new FootViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_recycle_foot, viewGroup, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, @SuppressLint("RecyclerView") final int position) {
            if (viewHolder instanceof MyViewHolder) {
                final DeviceInfo deviceInfo = mDevList.get(position - countHead);
                if (deviceInfo != null) {
                    try {
                        ((MyViewHolder) viewHolder).mLockNameTv.setText(deviceInfo.getDeviceName());
                        ((MyViewHolder) viewHolder).mLockNumTv.setText(String.valueOf(deviceInfo.getBleMac()));
                    } catch (NullPointerException e) {
                        LogUtil.d(TAG, deviceInfo.getDeviceName() + "  " + deviceInfo.getDeviceIndex());
                    }
                    if (deviceInfo.getDeviceDefault()) {
                        ((MyViewHolder) viewHolder).mDefaultFlagIv.setImageResource(R.drawable.ic_selected);
                        ((MyViewHolder) viewHolder).mDefaultTv.setVisibility(View.VISIBLE);
                        mDefaultInfo = deviceInfo;
                        mDefaultPosition = position;
                    } else {
                        ((MyViewHolder) viewHolder).mDefaultFlagIv.setImageResource(R.drawable.ic_select);
                        ((MyViewHolder) viewHolder).mDefaultTv.setVisibility(View.INVISIBLE);
                    }

                    ((MyViewHolder) viewHolder).mLockNameTv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final Dialog modifyNameDialog = DialogUtils.createEditorDialog(DeviceManagementActivity.this, getString(R.string.modify_name), deviceInfo.getDeviceName());
                            ((EditText) modifyNameDialog.findViewById(R.id.editor_et)).setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
                            modifyNameDialog.findViewById(R.id.dialog_confirm_btn).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    String newName = ((EditText) modifyNameDialog.findViewById(R.id.editor_et)).getText().toString();
                                    if (!newName.isEmpty()) {
                                        ((MyViewHolder) viewHolder).mLockNameTv.setText(newName);
                                        deviceInfo.setDeviceName(newName);
                                        DeviceInfoDao.getInstance(DeviceManagementActivity.this).updateDeviceInfo(deviceInfo);
                                    } else {
                                        ToastUtil.showLong(DeviceManagementActivity.this, R.string.cannot_be_empty_str);
                                    }
                                    modifyNameDialog.dismiss();
                                }
                            });
                            modifyNameDialog.show();

                        }
                    });

                    ((MyViewHolder) viewHolder).mSetDefaultLl.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // 判断是否更换默认设备
                            if (mDefaultInfo == null) {
                                DeviceInfoDao.getInstance(mContext).setNoDefaultDev();
                                deviceInfo.setDeviceDefault(true);
                                DeviceInfoDao.getInstance(mCtx).updateDeviceInfo(deviceInfo);
                            } else if (!mDefaultInfo.getBleMac().equals(deviceInfo.getBleMac())) {
                                DeviceInfoDao.getInstance(mContext).setNoDefaultDev();
                                deviceInfo.setDeviceDefault(true);
                                DeviceInfoDao.getInstance(mCtx).updateDeviceInfo(deviceInfo);
                                Device.getInstance(DeviceManagementActivity.this).exchangeConnect(deviceInfo);
                                mBleManagerHelper.getBleCardService().disconnect();
                                Device.getInstance(DeviceManagementActivity.this).setDisconnectBle(false);
                                LogUtil.d(TAG, "设置为默认设备");
                            }
                            mDevList = DeviceInfoDao.getInstance(mCtx).queryAll();
                            mDevManagementAdapter.notifyDataSetChanged();
                        }
                    });
                    ((MyViewHolder) viewHolder).mUnbindLl.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
//                        if (deviceInfo.getDeviceDefault() && DeviceInfoDao.getInstance(mCtx).queryFirstData(DeviceInfoDao.DEVICE_DEFAULT, false) != null) {
//                            ToastUtil.showLong(mCtx, R.string.default_dev_delete_failed);
//                            ((MyViewHolder) viewHolder).mSwipeLayout.close();
//                        } else {
                            DtComFunHelper.restoreFactorySettings(DeviceManagementActivity.this, deviceInfo);
                            if (Device.getInstance(DeviceManagementActivity.this).getState() == Device.BLE_CONNECTED) {
                                mBleManagerHelper.getBleCardService().disconnect();
                            }
                            Device.getInstance(DeviceManagementActivity.this).halt();
                            mDevList = DeviceInfoDao.getInstance(mCtx).queryAll();
                            mDevManagementAdapter.notifyDataSetChanged();

//                        }
                        }
                    });
                }
            }
        }

        @Override
        public int getItemCount() {
            return mDevList.size() + countFoot + countHead;
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

        class FootViewHolder extends RecyclerView.ViewHolder {
            private FootViewHolder(View itemView) {
                super(itemView);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDevManagementAdapter.refreshList();
        mDevManagementAdapter.notifyDataSetChanged();
    }
}
