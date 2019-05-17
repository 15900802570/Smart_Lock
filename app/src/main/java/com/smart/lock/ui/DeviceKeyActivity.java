package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceStatus;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceStatusDao;
import com.smart.lock.ui.fragment.BaseFragment;
import com.smart.lock.ui.fragment.CardFragment;
import com.smart.lock.ui.fragment.FingerprintFragment;
import com.smart.lock.ui.fragment.PwdFragment;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.NoScrollViewPager;

import java.util.ArrayList;
import java.util.Objects;

public class DeviceKeyActivity extends AppCompatActivity implements View.OnClickListener, ViewPager.OnPageChangeListener {
    private final static String TAG = DeviceKeyActivity.class.getSimpleName();

    private TabLayout mUserPermissionTl;
    private NoScrollViewPager mUserPermissionVp;
    private Toolbar mUserSetTb;
    private TextView mTitleTv;

    private PwdFragment mPwdFragment;
    private CardFragment mCardFragment;
    private FingerprintFragment mFPFragment;

    private boolean mDeleteMode = false;

    private ArrayList<String> mTitleList;
    private ArrayList<BaseFragment> mUsersList;
    private UserPagerAdapter mUserPagerAdapter;
    private int mVpPosition = 0;
    private DeviceUser mTempUser;
    /**
     * 蓝牙
     */
    private BleManagerHelper mBleManagerHelper;


    private DeviceInfo mDefaultDevice; //默认设备

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_manager);
        initView();
        initData();
        initActionBar();
        initEvent();
    }

    @SuppressLint("WrongViewCast")
    private void initView() {
        mUserPermissionTl = findViewById(R.id.tl_user_manager);
        mUserPermissionVp = findViewById(R.id.vp_user_manager);
        mUserSetTb = findViewById(R.id.tb_user_set);
        mTitleTv = findViewById(R.id.tv_title);
    }

    private void initData() {
        mTitleList = new ArrayList<>();
        mTitleList.add(getString(R.string.password));
        mTitleList.add(getString(R.string.fingerprint));
        mTitleList.add(getString(R.string.card));

        mTempUser = (DeviceUser) Objects.requireNonNull(getIntent().getExtras()).getSerializable(BleMsg.KEY_TEMP_USER);
        mDefaultDevice = DeviceInfoDao.getInstance(this).queryFirstData("device_default", true);
        mBleManagerHelper = BleManagerHelper.getInstance(this);

        int currentItem = getIntent().getExtras().getInt(BleMsg.KEY_CURRENT_ITEM);


        mUsersList = new ArrayList<>();
        mPwdFragment = new PwdFragment();
        mPwdFragment.setTempUser(mTempUser);
        mFPFragment = new FingerprintFragment();
        mFPFragment.setTempUser(mTempUser);
        mCardFragment = new CardFragment();
        mCardFragment.setTempUser(mTempUser);

        mUsersList.add(mPwdFragment);
        mUsersList.add(mFPFragment);
        mUsersList.add(mCardFragment);
        mUserPagerAdapter = new UserPagerAdapter(getSupportFragmentManager());
        mUserPermissionVp.setAdapter(mUserPagerAdapter);

        initTabLayout();
        mUserPermissionVp.setOffscreenPageLimit(2);
        mUserPermissionVp.setCurrentItem(currentItem);
        mUserPermissionVp.setNoScroll(true);
    }

    private void initEvent() {
        mUserPermissionVp.addOnPageChangeListener(this);
    }

    private void initActionBar() {
        mTitleTv.setText(R.string.unlock_key);

        mUserSetTb.setNavigationIcon(R.mipmap.btn_back);
        mUserSetTb.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }


    /**
     * 吐司提示
     *
     * @param msg 提示信息
     */
    protected void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 初始化tb
     */
    private void initTabLayout() {
        mUserPermissionTl.setTabMode(TabLayout.MODE_FIXED);
        mUserPermissionTl.setSelectedTabIndicatorColor(getResources().getColor(R.color.yellow_selete));
        mUserPermissionTl.setSelectedTabIndicatorHeight(getResources().getDimensionPixelSize(R.dimen.y5dp));
        mUserPermissionTl.setupWithViewPager(mUserPermissionVp);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        mVpPosition = position;
        mDeleteMode = false;
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private class UserPagerAdapter extends FragmentPagerAdapter {

        public UserPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public BaseFragment getItem(int position) {
            return mUsersList.get(position);
        }

        @Override
        public int getCount() {
            return mUsersList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitleList.get(position);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onBackPressed() {

        DeviceStatus deviceStatus = DeviceStatusDao.getInstance(this).queryOrCreateByNodeId(mDefaultDevice.getDeviceNodeId());
        if (deviceStatus.isCombinationLock()) {
            int counter = mPwdFragment.getCounter() + mCardFragment.getCounter() + mFPFragment.getCounter();
            if (counter < 2){
                DialogUtils.createTipsDialogWithConfirm(this,getString(R.string.two_or_more_unlocking_keys_must_be_set)).show();
                return;
            }
            LogUtil.d(TAG, "Counter = " + counter);
        }
        super.onBackPressed();
        finish();

    }
}
