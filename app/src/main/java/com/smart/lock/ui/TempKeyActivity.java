package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.fragment.AdminFragment;
import com.smart.lock.fragment.BaseFragment;
import com.smart.lock.fragment.CardFragment;
import com.smart.lock.fragment.FingerprintFragment;
import com.smart.lock.fragment.MumberFragment;
import com.smart.lock.fragment.PwdFragment;
import com.smart.lock.fragment.TempFragment;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.widget.NoScrollViewPager;

import java.util.ArrayList;

public class TempKeyActivity extends AppCompatActivity implements View.OnClickListener, ViewPager.OnPageChangeListener {
    private final static String TAG = TempKeyActivity.class.getSimpleName();

    private TabLayout mUserPermissionTl;
    private NoScrollViewPager mUserPermissionVp;
    private Toolbar mUsetSetTb;
    private TextView mTitleTv;

    private boolean mDeleteMode = false;

    private ArrayList<String> mTitleList;
    private ArrayList<BaseFragment> mUsersList;
    private UserPagerAdapter mUserPagerAdapter;
    private int mVpPosition = 0;
    private Dialog mLoadDialog;
    private Handler mHandler;
    private DeviceUser mTempUser;
    /**
     * 蓝牙
     */
    private BleManagerHelper mBleManagerHelper;

    /**
     * 超时提示框启动器
     */
    private Runnable mRunnable = new Runnable() {
        public void run() {
            if (mLoadDialog != null && mLoadDialog.isShowing()) {

                DialogUtils.closeDialog(mLoadDialog);

                Toast.makeText(TempKeyActivity.this, getString(R.string.plz_reconnect), Toast.LENGTH_LONG).show();
            }

        }
    };

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
        mUsetSetTb = findViewById(R.id.tb_user_set);
        mTitleTv = findViewById(R.id.tv_title);
    }

    private void initData() {
        mTitleList = new ArrayList<>();
        mTitleList.add(getString(R.string.password));
        mTitleList.add(getString(R.string.fingerprint));
        mTitleList.add("NFC");

        mTempUser = (DeviceUser) getIntent().getExtras().getSerializable(BleMsg.KEY_TEMP_USER);
        mDefaultDevice = DeviceInfoDao.getInstance(this).queryFirstData("device_default", true);
        mBleManagerHelper = BleManagerHelper.getInstance(this, mDefaultDevice.getDeviceNodeId(), false);

        mUsersList = new ArrayList<>();
        PwdFragment pwdFragment = new PwdFragment();
        pwdFragment.setTempUser(mTempUser);
        FingerprintFragment fpFragment = new FingerprintFragment();
        fpFragment.setTempUser(mTempUser);
        CardFragment cardFragment = new CardFragment();
        cardFragment.setTempUser(mTempUser);

        mUsersList.add(pwdFragment);
        mUsersList.add(fpFragment);
        mUsersList.add(cardFragment);
        mUserPagerAdapter = new UserPagerAdapter(getSupportFragmentManager());
        mUserPermissionVp.setAdapter(mUserPagerAdapter);

        initTabLayout();
        mUserPermissionVp.setOffscreenPageLimit(2);
        mUserPermissionVp.setNoScroll(false);
        mHandler = new Handler();

    }

    private void initEvent() {
        mUserPermissionVp.addOnPageChangeListener(this);
    }

    private void initActionBar() {
        mTitleTv.setText(R.string.unlock_key);

        mUsetSetTb.setNavigationIcon(R.mipmap.icon_arrow_blue_left_45_45);
        mUsetSetTb.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        setSupportActionBar(mUsetSetTb);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
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
        mUserPermissionTl.setSelectedTabIndicatorColor(getResources().getColor(android.R.color.holo_blue_dark));
        mUserPermissionTl.setSelectedTabIndicatorHeight((int) getResources().getDimension(R.dimen.tablayout_indicator_height));
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

    /**
     * 超时提醒
     *
     * @param seconds
     */
    protected void closeDialog(final int seconds) {

        mHandler.removeCallbacks(mRunnable);

        mHandler.postDelayed(mRunnable, seconds * 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
