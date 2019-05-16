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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.entity.Device;
import com.smart.lock.ui.fragment.AdminFragment;
import com.smart.lock.ui.fragment.BaseFragment;
import com.smart.lock.ui.fragment.MemberFragment;
import com.smart.lock.ui.fragment.TempFragment;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.widget.NoScrollViewPager;

import java.util.ArrayList;

public class UserManagerActivity extends AppCompatActivity implements View.OnClickListener, ViewPager.OnPageChangeListener, UiListener, MemberFragment.OnFragmentInteractionListener,
        AdminFragment.OnFragmentInteractionListener,
        TempFragment.OnFragmentInteractionListener {
    private final static String TAG = UserManagerActivity.class.getSimpleName();

    private TabLayout mUserPermissionTl;
    private NoScrollViewPager mUserPermissionVp;
    private Toolbar mUserSetTb;
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
    private Device mDevice;

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
        mTitleList.add(getString(R.string.administrator));
        mTitleList.add(getString(R.string.members));
        mTitleList.add(getString(R.string.tmp_user));

        mUsersList = new ArrayList<>();
        mUsersList.add(new AdminFragment());
        mUsersList.add(new MemberFragment());
        mUsersList.add(new TempFragment());
        mUserPagerAdapter = new UserPagerAdapter(getSupportFragmentManager());
        mUserPermissionVp.setAdapter(mUserPagerAdapter);
        initTabLayout();
        mUserPermissionVp.setOffscreenPageLimit(2);
        mUserPermissionVp.setNoScroll(true);
        mHandler = new Handler();
        mDefaultDevice = DeviceInfoDao.getInstance(this).queryFirstData("device_default", true);
        mBleManagerHelper = BleManagerHelper.getInstance(this, false);
        mDevice = mBleManagerHelper.getBleCardService().getDevice();
        mBleManagerHelper.addUiListener(this);

        mLoadDialog = DialogUtils.createLoadingDialog(this, getString(R.string.data_loading));
    }

    private void initEvent() {
        mUserPermissionVp.addOnPageChangeListener(this);
    }

    private void initActionBar() {
        mTitleTv.setText(R.string.permission_manager);

        mUserSetTb.setNavigationIcon(R.mipmap.btn_back);
        mUserSetTb.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        setSupportActionBar(mUserSetTb);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_manager_setting, menu);
        mDeleteItem = menu.findItem(R.id.item_edit);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.item_edit:
                changeVisible();
                break;
            case R.id.del_all_pwd:
                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    mBleManagerHelper.getBleCardService().sendCmd17(BleMsg.TYPE_DELETE_OTHER_USER_PASSWORD, mDefaultDevice.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                } else showMessage(getString(R.string.disconnect_ble));
                break;
            case R.id.del_all_fp:
                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    mBleManagerHelper.getBleCardService().sendCmd17(BleMsg.TYPE_DELETE_OTHER_USER_FINGERPRINT, mDefaultDevice.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                } else showMessage(getString(R.string.disconnect_ble));
                break;
            case R.id.del_all_card:
                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    mBleManagerHelper.getBleCardService().sendCmd17(BleMsg.TYPE_DELETE_OTHER_USER_CARD, mDefaultDevice.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                } else showMessage(getString(R.string.disconnect_ble));
                break;
            case R.id.del_all_user:
                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    mBleManagerHelper.getBleCardService().sendCmd13(BleMsg.TYPE_DELETE_ALL_USER, BleMsg.INT_DEFAULT_TIMEOUT);
                } else showMessage(getString(R.string.disconnect_ble));
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 吐司提示
     *
     * @param msg 提示信息
     */
    protected void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public void changeVisible() {
        if (mDeleteMode) {
            mDeleteItem.setTitle(getString(R.string.edit));
            mDeleteMode = false;
        } else {
            mDeleteItem.setTitle(getString(R.string.edit_back));
            mDeleteMode = true;
        }
        BaseFragment framentView = mUserPagerAdapter.getItem(mVpPosition);
        if (framentView instanceof AdminFragment) {
            AdminFragment adminFragment = (AdminFragment) framentView;
            adminFragment.selectDelete(mDeleteMode);
        } else if (framentView instanceof MemberFragment) {
            MemberFragment mumberFragment = (MemberFragment) framentView;
            mumberFragment.selectDelete(mDeleteMode);
        } else if (framentView instanceof TempFragment) {
            TempFragment tempFragment = (TempFragment) framentView;
            tempFragment.selectDelete(mDeleteMode);
        }
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
        mDeleteItem.setTitle(getString(R.string.edit));
        mDeleteMode = false;
        for (int i = 0; i < mTitleList.size(); i++) {
            BaseFragment framentView = mUserPagerAdapter.getItem(i);
            if (framentView instanceof AdminFragment) {
                AdminFragment adminFragment = (AdminFragment) framentView;
                adminFragment.selectDelete(false);
            } else if (framentView instanceof MemberFragment) {
                MemberFragment mumberFragment = (MemberFragment) framentView;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBleManagerHelper.removeUiListener(this);
    }

    @Override
    public void deviceStateChange(Device device, int state) {
        mDevice = device;
        switch (state) {
            case BleMsg.STATE_DISCONNECTED:
                DialogUtils.closeDialog(mLoadDialog);
                showMessage(getString(R.string.ble_disconnect));
                break;
            case BleMsg.STATE_CONNECTED:

                break;
            case BleMsg.GATT_SERVICES_DISCOVERED:
                break;
            default:
                LogUtil.e(TAG, "state : " + state + "is can not handle");
                break;
        }
    }

    @Override
    public void dispatchUiCallback(Message msg, Device device, int type) {
        LogUtil.i(TAG, "dispatchUiCallback!");
        mDevice = device;
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_1E:
                final byte[] errCode = msg.getData().getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode != null)
                    dispatchErrorCode(errCode[3]);
                break;
            default:
                LogUtil.e(TAG, "Message type : " + msg.getType() + " can not be handler");
                break;
        }

    }

    @Override
    public void reConnectBle(Device device) {
        mDevice = device;
    }

    @Override
    public void sendFailed(Message msg) {
        int exception = msg.getException();
        switch (exception) {
            case Message.EXCEPTION_TIMEOUT:
                DialogUtils.closeDialog(mLoadDialog);
                showMessage(msg.getType() + " can't receiver msg!");
                break;
            case Message.EXCEPTION_SEND_FAIL:
                DialogUtils.closeDialog(mLoadDialog);
                showMessage(msg.getType() + " send failed!");
                LogUtil.e(TAG, "msg exception : " + msg.toString());
                break;
            default:
                break;
        }
    }

    @Override
    public void addUserSuccess(Device device) {

    }

    @Override
    public void scanDevFailed() {

    }

    private void dispatchErrorCode(byte errCode) {
        LogUtil.i(TAG, "errCode : " + errCode);
        switch (errCode) {
            case BleMsg.TYPE_GROUP_DELETE_KEY_SUCCESS:
                showMessage(getString(R.string.delete_key_success));
                break;
            case BleMsg.TYPE_GROUP_DELETE_KEY_FAILED:
                showMessage(getString(R.string.delete_key_failed));
                break;
            case BleMsg.TYPE_DELETE_FP_SUCCESS:
                showMessage(getString(R.string.delete_key_success));
                break;
            case BleMsg.TYPE_DELETE_FP_FAILED:
                showMessage(getString(R.string.delete_key_failed));
                break;
            case BleMsg.TYPE_GROUP_DELETE_USER_SUCCESS:
                ArrayList<DeviceUser> users = DeviceUserDao.getInstance(this).queryDeviceUsers(mDefaultDevice.getDeviceNodeId());
                LogUtil.d(TAG, "mDefaultDevice.getUserId() : " + mDefaultDevice.getUserId());
                for (DeviceUser user : users) {
                    LogUtil.d(TAG, "user id : " + user.getUserId());
                    if (user.getUserId() != mDefaultDevice.getUserId()) {

                        DeviceUserDao.getInstance(this).delete(user);
                        DeviceKeyDao.getInstance(this).deleteUserKey(user.getUserId(), user.getDevNodeId()); //删除开锁信息
                    }
                }

                for (int i = 0; i < mTitleList.size(); i++) {
                    BaseFragment framentView = mUserPagerAdapter.getItem(i);
                    if (framentView instanceof AdminFragment) {
                        AdminFragment adminFragment = (AdminFragment) framentView;
                        adminFragment.refreshView();
                    } else if (framentView instanceof MemberFragment) {
                        MemberFragment mumberFragment = (MemberFragment) framentView;
                        mumberFragment.refreshView();
                    } else if (framentView instanceof TempFragment) {
                        TempFragment tempFragment = (TempFragment) framentView;
                        tempFragment.refreshView();
                    }
                }
                showMessage(getString(R.string.delete_users_success));
                break;
            case BleMsg.TYPE_GROUP_DELETE_USER_FAILED:
                showMessage(getString(R.string.delete_users_failed));
                break;
            default:
                break;
        }
        DialogUtils.closeDialog(mLoadDialog);
    }
}
