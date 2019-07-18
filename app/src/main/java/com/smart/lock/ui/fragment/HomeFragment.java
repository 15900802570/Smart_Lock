
package com.smart.lock.ui.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smart.lock.R;
import com.smart.lock.adapter.LockManagerAdapter;
import com.smart.lock.adapter.ServerPagerAdapter;
import com.smart.lock.adapter.ViewPagerAdapter;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceStatus;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.entity.Device;
import com.smart.lock.ui.UserManagerActivity2;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.widget.NoScrollViewPager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class HomeFragment extends BaseFragment implements
        View.OnClickListener, ViewPager.OnPageChangeListener {
    private static final String TAG = HomeFragment.class.getSimpleName();
    private Toolbar mToolbar;
    private View mHomeView;
    private ViewPager mViewPager;
    private LinearLayout mAddLockLl;
    private RelativeLayout mNewsVpRL;
    private Button mAddLockBt;
    private ImageView mInstructionBtn;
    private ImageView mScanQrIv;
    private List<BaseFragment> mServerPagerList;
    private NoScrollViewPager mServerPager;
    private ServerPagerAdapter mServerAdapter;

    private ViewPagerAdapter mAdapter; //news adapter
    private LockManagerAdapter mLockAdapter; //gridView adapter
    private DeviceInfo mDefaultDevice; //默认设备
    private DeviceUser mDefaultUser; //默认用户
    private DeviceStatus mDefaultStatus; //用户状态
    private ArrayList<DeviceInfo> mDeviceInfos; //设备集合
    private ArrayList<View> mDots; //spot list

    private int mOldPosition = 0;// 记录上一次点的位置

    public static final int BIND_DEVICE = 0; //用户已添加设备
    public static final int UNBIND_DEVICE = 1;//未添加设备
    public static final int DEVICE_CONNECTING = 2;//添加设备中
    public static final int OPEN_LOCK_SUCESS = 3;//打开门锁成功


    private boolean mOpenTest = false; // 测试连接的开关

    private int mBattery = 0;

    private int mImageIds[]; //主界面图片
    private int mImageIdsNor[];
    private Device mDevice;
    private Context mCtx;

    private ArrayList<DeviceInfo> deviceInfoArraysList;

    private DevManagementAdapter mDevManagementAdapter;
    private Dialog mBottomSheetSelectDev;

    private boolean mIsLockBack = false;

    public void onAuthenticationSuccess() {
        refreshView(BIND_DEVICE);
    }

    public void onAuthenticationFailed() {
        refreshView(UNBIND_DEVICE);
    }

    private Runnable mRunnable = new Runnable() {
        public void run() {
//            mInstructionBtn.setEnabled(true);
            mIsLockBack = false;
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View initView() {
        mHomeView = View.inflate(mActivity, R.layout.home_fragment, null);
        mNewsVpRL = mHomeView.findViewById(R.id.rl_news_vp);
        mViewPager = mHomeView.findViewById(R.id.news_vp);
        mAddLockLl = mHomeView.findViewById(R.id.rl_add_lock);
        mScanQrIv = mHomeView.findViewById(R.id.iv_scan_qr);
        mAddLockBt = mHomeView.findViewById(R.id.btn_add_lock);
        mInstructionBtn = mActivity.findViewById(R.id.one_click_unlock_ib);
        mServerPager = mHomeView.findViewById(R.id.server_viewpager);
        mBottomSheetSelectDev = DialogUtils.createBottomSheetDialog(mActivity, R.layout.bottom_sheet_select_device, R.id.design_bottom_sheet);
        mDevManagementAdapter = new DevManagementAdapter(mActivity);
        initEvent();
        return mHomeView;
    }

    @SuppressLint("UseSparseArrays")
    private void initEvent() {
        mAddLockBt.setOnClickListener((View.OnClickListener) mActivity);
        mInstructionBtn.setOnClickListener(this);
        mScanQrIv.setOnClickListener((View.OnClickListener) mActivity);
        mServerPagerList = new ArrayList<>();
        BottomSheetBehavior behavior = BottomSheetBehavior.from(mBottomSheetSelectDev.findViewById(R.id.design_bottom_sheet));
        behavior.setHideable(false);
        setViewPager();
    }

    void setViewPager() {
        deviceInfoArraysList = DeviceInfoDao.getInstance(mActivity).queryAll();
        mServerAdapter = new ServerPagerAdapter(this.getChildFragmentManager(), mActivity, deviceInfoArraysList);
        mServerPager.setAdapter(mServerAdapter);
        mServerPager.setCurrentItem(mServerAdapter.getCurrentItem());
        mServerPager.addOnPageChangeListener(this);
        LogUtil.d(TAG, "INDEX ServerPage=" + mServerPager.getChildCount());

    }

    @SuppressLint("HandlerLeak")
    @Override
    public void initDate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {  //版本检测
            mToolbar = mHomeView.findViewById(R.id.tb_toolbar);
            ((AppCompatActivity) Objects.requireNonNull(getActivity())).setSupportActionBar(mToolbar);  //将ToolBar设置成ActionBar
        }
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);

        // 显示的点
        mDots = new ArrayList<View>();
        mDots.add((View) getView().findViewById(R.id.dot_0));
        mDots.add((View) getView().findViewById(R.id.dot_1));

        mImageIds = new int[]{
                R.mipmap.homepage_adv1,
                R.mipmap.homepage_adv5
        };
        mImageIdsNor = new int[]{
                R.mipmap.homepage_adv1_nor,
                R.mipmap.homepage_adv5_nor
        };

        mCtx = mHomeView.getContext();

        mAdapter = new ViewPagerAdapter(mCtx, mImageIds);
        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                // TODO Auto-generated method stub

                mDots.get(mOldPosition).setBackgroundResource(R.drawable.dot_focused);
                mDots.get(position).setBackgroundResource(R.drawable.dot_normal);

                mOldPosition = position;
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
                // TODO Auto-generated method stub

            }
        });

        mBleManagerHelper = BleManagerHelper.getInstance(mCtx);
        mDefaultDevice = DeviceInfoDao.getInstance(mCtx).queryFirstData("device_default", true);
        mDevice = Device.getInstance(mCtx);
        if (mDefaultDevice == null) {
            refreshView(UNBIND_DEVICE);
        } else {
            refreshView(BIND_DEVICE);
        }
        mHandler = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {

                switch (msg.what) {
                    case BIND_DEVICE:
                        refreshView(BIND_DEVICE);
                        break;
                    case UNBIND_DEVICE:
                        refreshView(UNBIND_DEVICE);
                        break;
                    case DEVICE_CONNECTING:
                        refreshView(DEVICE_CONNECTING);
                        break;
                    case OPEN_LOCK_SUCESS:
                        showMessage(getString(R.string.remote_unlock_success));
                        break;
                    case BleMsg.SCAN_DEV_FIALED:
                        LogUtil.d(TAG, "SCAN_DEV_FIALED 2 " + this.hashCode());
                        if (mDevice != null && !mDevice.isDisconnectBle()) {
                            showMessage(getString(R.string.retry_connect));
                        }
                        if (mDefaultDevice != null) {
                            refreshView(BIND_DEVICE);
                        } else {
                            refreshView(UNBIND_DEVICE);
                        }
                        break;
                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        };
    }

    /**
     * 刷新显示界面
     */
    public void refreshView(int status) {
        switch (status) {

            case BIND_DEVICE:
                LogUtil.d(TAG, "onResume bind");
                if (mNewsVpRL.getLayoutParams() != null)
                    mNewsVpRL.getLayoutParams().height = (int) mCtx.getResources().getDimension(R.dimen.y366dp);
                mAdapter.setImageIds(mImageIdsNor);
                mAdapter.notifyDataSetChanged();
                mAddLockLl.setVisibility(View.GONE);
                mServerPager.setVisibility(View.VISIBLE);
                deviceInfoArraysList = DeviceInfoDao.getInstance(mActivity).queryAll();
                if (deviceInfoArraysList.size() != mServerAdapter.getCount()) {
                    mServerAdapter.updateDevices(mActivity, deviceInfoArraysList);
                    mServerAdapter.notifyDataSetChanged();
                    LogUtil.d(TAG, "onResume changed\n" + mServerAdapter.getCurrentItem());
                    mServerPager.setCurrentItem(mServerAdapter.getCurrentItem());
                }
                mDevManagementAdapter.refreshList();
                mDevManagementAdapter.notifyDataSetChanged();
                break;
            case UNBIND_DEVICE:
                LogUtil.d(TAG, "onResume Unbind");
                if (mNewsVpRL.getLayoutParams() != null)
                    mNewsVpRL.getLayoutParams().height = (int) getResources().getDimension(R.dimen.y536dp);
                mAddLockLl.setVisibility(View.VISIBLE);
                mServerPager.setVisibility(View.GONE);
                mInstructionBtn.setVisibility(View.GONE);
                deviceInfoArraysList = DeviceInfoDao.getInstance(mActivity).queryAll();
                if (deviceInfoArraysList.size() != mServerAdapter.getCount()) {
                    mServerAdapter.updateDevices(mActivity, deviceInfoArraysList);
                    mServerAdapter.notifyDataSetChanged();
                    mServerPager.setCurrentItem(mServerAdapter.getCurrentItem());
                }
                mAdapter.setImageIds(mImageIds);
                mAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }


    public void onResume() {
        super.onResume();
//        updateViewPage();
        ArrayList<DeviceInfo> newDeviceInfo = DeviceInfoDao.getInstance(mCtx).queryAll();

        if (newDeviceInfo.size() == 0) {
            mDevice = null;
            refreshView(UNBIND_DEVICE);
            LogUtil.d(TAG, "onResume  UNBIND_DEVICE");
        } else {
            refreshView(BIND_DEVICE);
            LogUtil.d(TAG, "onResume  BIND_DEVICE");
        }
    }


    @Override
    public void onClick(View v) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUESTCODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        || !shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
//                    askForPermission();
                    Bundle bundle = new Bundle();
                    if (mDefaultDevice != null) {
                        bundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);
                        startIntent(UserManagerActivity2.class, bundle);
                    }

                }
            }

        }

    }

    /**
     * 超时提示框
     *
     * @param seconds 时间
     */
    private void closeDialog(int seconds) {
        mHandler.removeCallbacks(mRunnable);
        mHandler.postDelayed(mRunnable, seconds * 1000);
    }

    private void sendMessage(int type, Bundle bundle, long time) {
        android.os.Message msg = new android.os.Message();
        if (bundle != null) {
            msg.setData(bundle);
        }
        msg.what = type;
        if (time != 0) {
            mHandler.sendMessageDelayed(msg, time);
        } else mHandler.sendMessage(msg);

    }

    @Override
    public void onPageScrolled(int i, float v, int i1) {

    }

    @Override
    public void onPageSelected(int i) {
        DeviceInfoDao.getInstance(mActivity).setNoDefaultDev();
        deviceInfoArraysList = DeviceInfoDao.getInstance(mActivity).queryAll();
        deviceInfoArraysList.get(i).setDeviceDefault(true);
        DeviceInfoDao.getInstance(mActivity).updateDeviceInfo(deviceInfoArraysList.get(i));
    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }
    public void setOnSelectDialogCancelListener(final ServerPagerFragment fragment){
        mBottomSheetSelectDev.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                fragment.closedSelectDevDialog();
            }
        });
    }

    //    @Override
    public void onSelectDev(DeviceInfo deviceInfo) {
        LogUtil.d(TAG, "setCurrentItem");
        deviceInfoArraysList = DeviceInfoDao.getInstance(mActivity).queryAll();
        for (int i = 0; i < deviceInfoArraysList.size(); i++) {
            if (deviceInfo.getId() == deviceInfoArraysList.get(i).getId()) {
                mServerAdapter.updateDevices(mActivity, deviceInfoArraysList);
                mServerAdapter.notifyDataSetChanged();
                mServerPager.setCurrentItem(i);
            }
        }
    }

    public void showDialog() {
        RecyclerView mSelectList = mBottomSheetSelectDev.findViewById(R.id.list_view_select_dev);
        assert mSelectList != null;
        mSelectList.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.VERTICAL, false));
        mSelectList.setItemAnimator(new DefaultItemAnimator());
        mDevManagementAdapter.refreshList();
        mDevManagementAdapter.notifyDataSetChanged();
        mSelectList.setAdapter(mDevManagementAdapter);
        mBottomSheetSelectDev.show();
    }

    private class DevManagementAdapter extends RecyclerView.Adapter<DevManagementAdapter.MyViewHolder> {

        private Context mContext;
        private ArrayList<DeviceInfo> mDevList;
        private DeviceInfo mDefaultInfo;
        int mDefaultPosition;

        private DevManagementAdapter(Context context) {
            mContext = context;
            mDevList = DeviceInfoDao.getInstance(mActivity).queryAll();
        }

        private void addItem(DeviceInfo deviceInfo) {
            if (mDevList.indexOf(deviceInfo) == -1) {
                mDevList.add(0, deviceInfo);
            }
        }

        private void refreshList() {
            mDevList = DeviceInfoDao.getInstance(mActivity).queryAll();
        }

        @NonNull
        @Override
        public DevManagementAdapter.MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_recycler_select_mangement, viewGroup, false);
            return new DevManagementAdapter.MyViewHolder(inflate);
        }

        @Override
        public void onBindViewHolder(@NonNull final DevManagementAdapter.MyViewHolder myViewHolder, @SuppressLint("RecyclerView") final int position) {
            final DeviceInfo deviceInfo = mDevList.get(position);
            if (deviceInfo != null) {
                try {
                    myViewHolder.mLockNameTv.setText(deviceInfo.getDeviceName());
                    myViewHolder.mLockNumTv.setText(String.valueOf(deviceInfo.getBleMac()));
                } catch (NullPointerException e) {
                    LogUtil.d(TAG, deviceInfo.getDeviceName() + "  " + deviceInfo.getDeviceIndex());
                }
                if (deviceInfo.getDeviceDefault()) {
                    myViewHolder.mDefaultFlagIv.setVisibility(View.VISIBLE);
                    mDefaultInfo = deviceInfo;
                    mDefaultPosition = position;
                } else {
                    myViewHolder.mDefaultFlagIv.setVisibility(View.INVISIBLE);
                }

                myViewHolder.mSelectDevLl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 判断是否更换默认设备
                       HomeFragment.this.mBottomSheetSelectDev.cancel();
                        if (!deviceInfo.getDeviceDefault()) {
                            if (mDefaultInfo == null) {
                                LogUtil.d(TAG, "设置为默认设备222");
                                deviceInfo.setDeviceDefault(true);
                                DeviceInfoDao.getInstance(mActivity).updateDeviceInfo(deviceInfo);
                            } else if (!mDefaultInfo.getBleMac().equals(deviceInfo.getBleMac())) {
                                mDefaultInfo.setDeviceDefault(false);
                                DeviceInfoDao.getInstance(mActivity).updateDeviceInfo(mDefaultInfo);
                                deviceInfo.setDeviceDefault(true);
                                DeviceInfoDao.getInstance(mActivity).updateDeviceInfo(deviceInfo);
                                Device.getInstance(mActivity).exchangeConnect(deviceInfo);
//                                mBleManagerHelper.getBleCardService().disconnect();
                                LogUtil.d(TAG, "设置为默认设备");
                            }
                            mDevList = DeviceInfoDao.getInstance(mActivity).queryAll();
                            mDevManagementAdapter.notifyDataSetChanged();
                            onSelectDev(deviceInfo);
                            Device.getInstance(mActivity).setDisconnectBle(false);
                        }

                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return mDevList.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            private TextView mLockNameTv;
            private TextView mLockNumTv;
            private ImageView mDefaultFlagIv;
            private LinearLayout mSelectDevLl;


            private MyViewHolder(View itemView) {
                super(itemView);
                mLockNameTv = itemView.findViewById(R.id.tv_dev_management_dev_name);
                mLockNumTv = itemView.findViewById(R.id.tv_dev_management_dev_num);
                mDefaultFlagIv = itemView.findViewById(R.id.iv_dev_management_default_flag);
                mSelectDevLl = itemView.findViewById(R.id.ll_dev_select);
            }
        }
    }

}
