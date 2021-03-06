package com.smart.lock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.entity.Device;
import com.smart.lock.scan.ScanQRHelper;
import com.smart.lock.scan.ScanQRResultInterface;
import com.smart.lock.ui.fragment.BaseFragment;
import com.smart.lock.ui.fragment.HomeFragment;
import com.smart.lock.ui.fragment.MeFragment;
import com.smart.lock.ui.fragment.ServerPagerFragment;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.SharedPreferenceUtil;
import com.smart.lock.utils.SystemUtils;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.NoScrollViewPager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class MainActivity extends AppCompatActivity implements
        RadioGroup.OnCheckedChangeListener,
        View.OnClickListener,
        MeFragment.OnFragmentInteractionListener,
        ScanQRResultInterface {

    private static final String TAG = MainActivity.class.getSimpleName();
    //back time
    private long mBackPressedTime;

    private NoScrollViewPager mTabVg;
    private RadioGroup mTabRg;

    //fragment list
    private List<BaseFragment> mPagerList;
    private HomeFragment mHomeFragment;

    private ScanQRHelper mScanQRHelper;
    /**
     * 蓝牙
     */
    protected BleManagerHelper mBleManagerHelper;


    private int mHeight;
    private Device mDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHeight = this.getResources().getDisplayMetrics().heightPixels;

        initView();
        initDate();
        initEvent();

        SystemUtils.getScreen(this);
    }

    private void initView() {
        mTabVg = findViewById(R.id.fragment_vg);
        mTabRg = findViewById(R.id.tab_rg);
        ImageView mOneClickOpen = findViewById(R.id.one_click_unlock_ib);
        mOneClickOpen.setPadding(0, 0, 0, (int) (mHeight * 0.025));
        findViewById(R.id.rl_home).getLayoutParams().height = (int) (mHeight * 0.085);
    }

    private void initEvent() {
        mTabRg.setOnCheckedChangeListener(this);
        mTabRg.check(R.id.home_rd);
    }

    private void initDate() {
        mBleManagerHelper = BleManagerHelper.getInstance(this);
        mDevice = Device.getInstance(this);
        mPagerList = new ArrayList<>();
        mHomeFragment = new HomeFragment();
        mPagerList.add(mHomeFragment);
        mPagerList.add(new MeFragment());
        mTabVg.setAdapter(new FragmentPagerAdapter(this.getSupportFragmentManager()) {
            public Fragment getItem(int i) {
                return mPagerList.get(i);
            }

            @NonNull
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                return super.instantiateItem(container, position);
            }

            public int getCount() {
                return mPagerList.size();
            }
        });
        mTabVg.setNoScroll(true);

        mScanQRHelper = new ScanQRHelper(this, this);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.home_rd:
                if (DeviceInfoDao.getInstance(this).queryFirstData("device_default", true) != null) {
                    findViewById(R.id.one_click_unlock_ib).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.one_click_unlock_ib).setVisibility(View.GONE);
                }
                mTabVg.setCurrentItem(0, false);
                break;
            case R.id.me_rd:
                findViewById(R.id.one_click_unlock_ib).setVisibility(View.GONE);
                mTabVg.setCurrentItem(1, false);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManagerHelper.getInstance(this).stopService();
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

    public void onBackPressed() {
        long curTime = SystemClock.uptimeMillis();
        if (curTime - mBackPressedTime < 3000) {
            finish();
            return;
        }
        mBackPressedTime = curTime;
        ToastUtil.showShort(this, getString(R.string.back_message));
    }

    public void onResume() {
        super.onResume();
        if (mTabVg.getCurrentItem() != 1) {
            if (DeviceInfoDao.getInstance(this).queryFirstData("device_default", true) != null) {
                findViewById(R.id.one_click_unlock_ib).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.one_click_unlock_ib).setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case ConstantUtil.SCAN_QRCODE_REQUEST_CODE:
                    mScanQRHelper.ScanDoCode(data);
                    break;
                case ConstantUtil.SETTING_PWD_REQUEST_CODE:
                    if (Objects.requireNonNull(data.getExtras()).getInt(ConstantUtil.CONFIRM) == 1) {
                        SharedPreferenceUtil.getInstance(this).writeBoolean(ConstantUtil.NUM_PWD_CHECK, true);
                        ToastUtil.showLong(this, getResources().getString(R.string.pwd_setting_successfully));
                    } else {
                        ToastUtil.showLong(this, getResources().getString(R.string.pwd_setting_failed));
                    }
                    break;
                case BleManagerHelper.REQUEST_OPEN_BT_CODE:
                    if (mBleManagerHelper.getBleCardService() != null && mDevice != null) {
                        mBleManagerHelper.getBleCardService().disconnect();
                        mDevice.setDisconnectBle(false);
                        LogUtil.d(TAG, "is unenable!");
                    }
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            switch (requestCode) {
                case BleManagerHelper.REQUEST_OPEN_BT_CODE:
                    if (mBleManagerHelper.getBleCardService() != null && mDevice != null) {
                        mDevice.setDisconnectBle(true);
                        mBleManagerHelper.getBleCardService().disconnect();
                        LogUtil.d(TAG, "is unenable!");
                    }
                    break;
            }
        }

    }

    /**
     * 添加设备成功响应函数
     *
     * @param deviceInfo 新设备信息
     */
    @Override
    public void onAuthenticationSuccess(DeviceInfo deviceInfo) {
        if (mTabVg.getCurrentItem() != 1) {
            if (DeviceInfoDao.getInstance(this).queryFirstData("device_default", true) != null) {
                findViewById(R.id.one_click_unlock_ib).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.one_click_unlock_ib).setVisibility(View.GONE);
            }
        }
        mHomeFragment.onAuthenticationSuccess();
        mScanQRHelper.halt();
    }

    /**
     * 添加失败响应函数
     */
    @Override
    public void onAuthenticationFailed() {
        if (mTabVg.getCurrentItem() != 1) {
            if (DeviceInfoDao.getInstance(this).queryFirstData("device_default", true) != null) {
                findViewById(R.id.one_click_unlock_ib).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.one_click_unlock_ib).setVisibility(View.GONE);
            }
        }
        mHomeFragment.onAuthenticationFailed();
    }

    /**
     * HomeFragment中的扫描点击事件
     *
     * @param view View
     */
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_add_lock || view.getId() == R.id.iv_scan_qr) {
            mScanQRHelper.scanQr();
        }
    }

    /**
     * MeFragment中的回调函数
     */
    @Override
    public void onScanQrCode() {
        mScanQRHelper.scanQr();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mScanQRHelper.getPermissionHelper().requestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }


}
