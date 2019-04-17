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
import com.smart.lock.ui.fragment.AdminFragment;
import com.smart.lock.ui.fragment.BaseFragment;
import com.smart.lock.ui.fragment.MumberFragment;
import com.smart.lock.ui.fragment.TempFragment;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.widget.NoScrollViewPager;
import com.smart.lock.widget.SpacesItemDecoration;

import java.util.ArrayList;

public class UserManagerActivity extends AppCompatActivity implements View.OnClickListener, ViewPager.OnPageChangeListener {
    private final static String TAG = UserManagerActivity.class.getSimpleName();

    private TabLayout mUserPermissionTl;
    private NoScrollViewPager mUserPermissionVp;
    private Toolbar mUsetSetTb;
    private TextView mTitleTv;
    private MenuItem mDeleteItem;
    private boolean mDeleteMode = false;

    private ArrayList<String> mTitleList;
    private ArrayList<BaseFragment> mUsersList;
    private UserPagerAdapter mUserPagerAdapter;
    private int mVpPosition = 0;
    private Dialog mLoadDialog;
    private Handler mHandler;
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

                Toast.makeText(UserManagerActivity.this, getString(R.string.plz_reconnect), Toast.LENGTH_LONG).show();
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
        LocalBroadcastManager.getInstance(this).registerReceiver(userReciver, intentFilter());
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
        mTitleList.add(getString(R.string.administrator));
        mTitleList.add(getString(R.string.members));
        mTitleList.add(getString(R.string.tmp_user));

        mUsersList = new ArrayList<>();
        mUsersList.add(new AdminFragment());
        mUsersList.add(new MumberFragment());
        mUsersList.add(new TempFragment());
        mUserPagerAdapter = new UserPagerAdapter(getSupportFragmentManager());
        mUserPermissionVp.setAdapter(mUserPagerAdapter);
        initTabLayout();
        mUserPermissionVp.setOffscreenPageLimit(2);
        mUserPermissionVp.setNoScroll(true);
        mHandler = new Handler();
        mDefaultDevice = DeviceInfoDao.getInstance(this).queryFirstData("device_default", true);
        mBleManagerHelper = BleManagerHelper.getInstance(this, false);
        mLoadDialog = DialogUtils.createLoadingDialog(this, getString(R.string.data_loading));
    }

    private void initEvent() {
        mUserPermissionVp.addOnPageChangeListener(this);
    }

    private void initActionBar() {
        mTitleTv.setText(R.string.permission_manager);

        mUsetSetTb.setNavigationIcon(R.mipmap.btn_back);
        mUsetSetTb.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        setSupportActionBar(mUsetSetTb);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_manager_setting, menu);
        mDeleteItem = menu.findItem(R.id.item_edit);
        return true;
    }

    private static IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.STR_RSP_MSG1E_ERRCODE);
        return intentFilter;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.item_edit:
                changeVisible();
                if (mDeleteMode) {
                    mDeleteItem.setIcon(R.mipmap.b_log_recents_delete);
                    mDeleteMode = false;
                } else {
                    mDeleteItem.setIcon(R.mipmap.b_log_recents_delete_reture);
                    mDeleteMode = true;
                }
                break;
            case R.id.del_all_pwd:
                DialogUtils.closeDialog(mLoadDialog);
                mLoadDialog.show();
                closeDialog(15);
                if (mBleManagerHelper.getServiceConnection()) {
                    mBleManagerHelper.getBleCardService().sendCmd17((byte) 4, mDefaultDevice.getUserId());
                }
                break;
            case R.id.del_all_fp:
                DialogUtils.closeDialog(mLoadDialog);
                mLoadDialog.show();
                closeDialog(15);
                if (mBleManagerHelper.getServiceConnection()) {
                    mBleManagerHelper.getBleCardService().sendCmd17((byte) 5, mDefaultDevice.getUserId());
                }
                break;
            case R.id.del_all_card:
                DialogUtils.closeDialog(mLoadDialog);
                mLoadDialog.show();
                closeDialog(15);
                if (mBleManagerHelper.getServiceConnection()) {
                    mBleManagerHelper.getBleCardService().sendCmd17((byte) 6, mDefaultDevice.getUserId());
                }
                break;
            case R.id.del_all_user:
                DialogUtils.closeDialog(mLoadDialog);
                mLoadDialog.show();
                closeDialog(15);
                if (mBleManagerHelper.getServiceConnection()) {
                    mBleManagerHelper.getBleCardService().sendCmd13((byte) 3);
                }
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 广播接收
     */
    private final BroadcastReceiver userReciver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BleMsg.STR_RSP_MSG1E_ERRCODE)) {
                final byte[] errCode = intent.getByteArrayExtra(BleMsg.KEY_ERROR_CODE);
                Log.d(TAG, "errCode[3] = " + errCode[3]);
                if (errCode[3] == 0x13) {
                    showMessage(getString(R.string.delete_key_success));
                } else if (errCode[3] == 0x14) {
                    showMessage(getString(R.string.delete_key_failed));
                } else if (errCode[3] == 0x11) {
                    ArrayList<DeviceUser> users = DeviceUserDao.getInstance(UserManagerActivity.this).queryDeviceUsers(mDefaultDevice.getDeviceNodeId());
                    for (DeviceUser user : users) {
                        if (!(user.getUserId() == mDefaultDevice.getUserId())) {
                            DeviceUserDao.getInstance(UserManagerActivity.this).delete(user);
                        }
                    }

                    for (int i = 0; i < mTitleList.size(); i++) {
                        BaseFragment framentView = mUserPagerAdapter.getItem(i);
                        if (framentView instanceof AdminFragment) {
                            AdminFragment adminFragment = (AdminFragment) framentView;
                            adminFragment.refreshView();
                        } else if (framentView instanceof MumberFragment) {
                            MumberFragment mumberFragment = (MumberFragment) framentView;
                            mumberFragment.refreshView();
                        } else if (framentView instanceof TempFragment) {
                            TempFragment tempFragment = (TempFragment) framentView;
                            tempFragment.refreshView();
                        }
                    }
                    showMessage(getString(R.string.delete_users_success));
                } else if (errCode[3] == 0x12) {
                    showMessage(getString(R.string.delete_users_failed));
                }
                mHandler.removeCallbacks(mRunnable);
                DialogUtils.closeDialog(mLoadDialog);
            }
        }
    };

    /**
     * 吐司提示
     *
     * @param msg 提示信息
     */
    protected void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void changeVisible() {
        BaseFragment framentView = mUserPagerAdapter.getItem(mVpPosition);
        if (framentView instanceof AdminFragment) {
            AdminFragment adminFragment = (AdminFragment) framentView;
            adminFragment.selectDelete(!mDeleteMode);
        } else if (framentView instanceof MumberFragment) {
            MumberFragment mumberFragment = (MumberFragment) framentView;
            mumberFragment.selectDelete(!mDeleteMode);
        } else if (framentView instanceof TempFragment) {
            TempFragment tempFragment = (TempFragment) framentView;
            tempFragment.selectDelete(!mDeleteMode);
        }
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
        mDeleteItem.setIcon(R.mipmap.b_log_recents_delete);
        mDeleteMode = false;
        for (int i = 0; i < mTitleList.size(); i++) {
            BaseFragment framentView = mUserPagerAdapter.getItem(i);
            if (framentView instanceof AdminFragment) {
                AdminFragment adminFragment = (AdminFragment) framentView;
                adminFragment.selectDelete(false);
            } else if (framentView instanceof MumberFragment) {
                MumberFragment mumberFragment = (MumberFragment) framentView;
                mumberFragment.selectDelete(false);
            } else if (framentView instanceof TempFragment) {
                TempFragment tempFragment = (TempFragment) framentView;
                tempFragment.selectDelete(false);
            }
        }
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
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(userReciver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
    }
}
