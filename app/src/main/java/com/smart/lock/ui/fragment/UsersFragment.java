package com.smart.lock.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.entity.Device;
import com.smart.lock.ui.DeviceKeyActivity;
import com.smart.lock.ui.UserSettingActivity;
import com.smart.lock.ui.login.LockScreenActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.FileUtil;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.SpacesItemDecoration;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.ListIterator;

public class UsersFragment extends BaseFragment implements View.OnClickListener, UiListener {
    private final static String TAG = UsersFragment.class.getSimpleName();

    private View mUserView;
    private RecyclerView mUsersRv;
    private UserAdapter mUserAdapter;
    private TextView mAddUserTv;
    private RelativeLayout mSelectDeleteRl;
    private CheckBox mSelectCb;
    private TextView mTipTv;
    private TextView mDeleteTv;
    private Device mDevice;
    private Boolean mIsHint = false;
    private BottomSheetDialog mFunctionDialog; //功能选择
    private BottomSheetDialog mCreateUserDialog; //创建用户选择

    private TextView mShareTv;
    private TextView mGroupDeleteTv;
    private TextView mUserSettingTv;
    private TextView mUserNameTv;
    private TextView mEditUserNameTv;
    private TextView mSingleDeleteTv;

    private TextView mCreateAdminTv;
    private TextView mCreateMembersTv;
    private TextView mCreateCancelTv;
    private View mLine;
    private TextView mDeleteCancelTV;
    private int mCountUsers = 0;

    private DeviceUser mSettingUser;
    private boolean mSingleMode = false;

    private Context mCtx;

    private boolean mShowQR = false;
    private static final int CHECK_USERS_STATE_TIME_OUT = 100;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case CHECK_USERS_STATE_TIME_OUT:
                    DialogUtils.closeDialog(mLoadDialog);
                    break;
                default:
                    break;
            }
        }
    };

    private ArrayList<DeviceUser> mCheckUsers = new ArrayList<>();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_add:
                mCreateUserDialog.show();
                break;
            case R.id.tv_del_tv:
                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    if (mUserAdapter.mDeleteUsers.size() != 0) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog = DialogUtils.createLoadingDialog(mCtx, getString(R.string.data_loading));
                        mLoadDialog.show();
                        for (DeviceUser devUser : mUserAdapter.mDeleteUsers) {
                            mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_DELETE_USER, devUser.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                        }
                    } else {
                        showMessage(getString(R.string.plz_choise_del_user));
                    }

                    if (mActivity instanceof UsersFragment.OnFragmentInteractionListener) {
                        ((UsersFragment.OnFragmentInteractionListener) mActivity).changeVisible();
                    }
                } else showMessage(getString(R.string.disconnect_ble));
                break;
            case R.id.share_tv:
                if (mDevice != null && mDevice.getState() == Device.BLE_CONNECTED) {
                    LogUtil.d(TAG, "send25");
                    mShowQR = true;
                    mBleManagerHelper.getBleCardService().sendCmd25(mSettingUser.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                } else showMessage(mCtx.getString(R.string.unconnected_device));
                mFunctionDialog.cancel();
                break;
            case R.id.group_delete_tv:
                selectDelete(true);
                mFunctionDialog.cancel();
                break;
            case R.id.del_cancel:
                selectDelete(false);
                break;
            case R.id.user_setting_tv:
                if (mSettingUser.getUserStatus() != ConstantUtil.USER_PAUSE) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(BleMsg.KEY_TEMP_USER, mSettingUser);
                    startIntent(UserSettingActivity.class, bundle);
                } else
                    showMessage(getString(R.string.user_pause_warning));
                mFunctionDialog.cancel();
                break;
            case R.id.edit_user_name:
                mUserAdapter.editUserName();
                mFunctionDialog.cancel();
                break;
            case R.id.single_delete_tv:
                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    mSingleMode = true;
                    mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_DELETE_USER, mSettingUser.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                } else showMessage(getString(R.string.disconnect_ble));
                mFunctionDialog.cancel();
                break;
            case R.id.create_admin_tv:
                ArrayList<DeviceUser> admins = DeviceUserDao.getInstance(mCtx).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_MASTER);
                if (admins != null && admins.size() >= ConstantUtil.ADMIN_USR_NUM) {
                    showMessage(mCtx.getResources().getString(R.string.administrator) + mCtx.getResources().getString(R.string.add_user_tips));
                    mCreateUserDialog.cancel();
                    return;
                }

                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog = DialogUtils.createLoadingDialog(mCtx, getString(R.string.data_loading));
                    mLoadDialog.show();
                    mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_CONNECT_ADD_MASTER, (short) 0, BleMsg.INT_DEFAULT_TIMEOUT);
                } else showMessage(getString(R.string.disconnect_ble));
                break;
            case R.id.create_members_tv:
                ArrayList<DeviceUser> members = DeviceUserDao.getInstance(mCtx).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_MEMBER);
                if (members != null && members.size() >= ConstantUtil.COMMON_USR_NUM) {
                    showMessage(mCtx.getResources().getString(R.string.members) + mCtx.getResources().getString(R.string.add_user_tips));
                    mCreateUserDialog.cancel();
                    return;
                }

                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog = DialogUtils.createLoadingDialog(mCtx, getString(R.string.data_loading));
                    mLoadDialog.show();
                    mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_CONNECT_ADD_MUMBER, (short) 0, BleMsg.INT_DEFAULT_TIMEOUT);
                } else showMessage(getString(R.string.disconnect_ble));
                break;
            case R.id.create_cancel:
                mCreateUserDialog.cancel();
                break;
            default:
                break;
        }
    }

    /**
     * 调用UserManagerActivity中的函数
     */
    public interface OnFragmentInteractionListener {
        void changeVisible();
    }

    public void selectDelete(boolean choise) {
        if (choise) {
            mSelectDeleteRl.setVisibility(View.VISIBLE);
            mAddUserTv.setVisibility(View.GONE);
            mDeleteTv.setVisibility(View.VISIBLE);
            mTipTv.setText(R.string.all_election);
        } else {
            mDeleteTv.setVisibility(View.GONE);
            mAddUserTv.setVisibility(View.VISIBLE);
            mSelectDeleteRl.setVisibility(View.GONE);
        }
        mSelectCb.setChecked(false);
        mUserAdapter.choiceALLDelete(false);
        mUserAdapter.choiceItemDelete(choise);
        mUserAdapter.notifyDataSetChanged();
    }

    /**
     * 刷新view
     */
    public void refreshView() {
        if (mUserAdapter != null) {
            mUserAdapter.setDataSource();
            mUserAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View initView() {
        mUserView = View.inflate(mActivity, R.layout.fragment_user_manager, null);
        mCtx = mUserView.getContext();
        mUsersRv = mUserView.findViewById(R.id.rv_users);
        mAddUserTv = mUserView.findViewById(R.id.tv_add);
        mSelectDeleteRl = mUserView.findViewById(R.id.rl_select_delete);
        mSelectCb = mUserView.findViewById(R.id.cb_selete_user);
        mDeleteTv = mUserView.findViewById(R.id.tv_del_tv);
        mTipTv = mUserView.findViewById(R.id.tv_tips);
        mFunctionDialog = DialogUtils.createBottomSheetDialog(mCtx, R.layout.bottom_sheet_user_function, R.id.design_bottom_sheet);
        mCreateUserDialog = DialogUtils.createBottomSheetDialog(mCtx, R.layout.bottom_create_user, R.id.design_bottom_sheet);
        mShareTv = mFunctionDialog.findViewById(R.id.share_tv);
        mGroupDeleteTv = mFunctionDialog.findViewById(R.id.group_delete_tv);
        mUserSettingTv = mFunctionDialog.findViewById(R.id.user_setting_tv);
        mUserNameTv = mFunctionDialog.findViewById(R.id.user_name_tv);
        mEditUserNameTv = mFunctionDialog.findViewById(R.id.edit_user_name);
        mSingleDeleteTv = mFunctionDialog.findViewById(R.id.single_delete_tv);

        mLine = mFunctionDialog.findViewById(R.id.line);
        mCreateAdminTv = mCreateUserDialog.findViewById(R.id.create_admin_tv);
        mCreateMembersTv = mCreateUserDialog.findViewById(R.id.create_members_tv);
        mCreateCancelTv = mCreateUserDialog.findViewById(R.id.create_cancel);
        mDeleteCancelTV = mUserView.findViewById(R.id.del_cancel);
        return mUserView;
    }

    public void initDate() {
        mDefaultDevice = DeviceInfoDao.getInstance(mCtx).queryFirstData("device_default", true);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mDefaultUser = DeviceUserDao.getInstance(mActivity).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId());
        mBleManagerHelper = BleManagerHelper.getInstance(mCtx);
        mBleManagerHelper.addUiListener(this);
        mDevice = Device.getInstance(mCtx);
        mLoadDialog = DialogUtils.createLoadingDialog(mCtx, getString(R.string.data_loading));
        mUserAdapter = new UserAdapter(mCtx);

        LinearLayoutManager mLinerLayoutManager = new LinearLayoutManager(mCtx, LinearLayoutManager.VERTICAL, false);
        mUsersRv.setLayoutManager(mLinerLayoutManager);
        mUsersRv.setItemAnimator(new DefaultItemAnimator());
        mUsersRv.setAdapter(mUserAdapter);
        mUsersRv.addItemDecoration(new SpacesItemDecoration(getResources().getDimensionPixelSize(R.dimen.y5dp)));

        mAddUserTv.setText(R.string.create_user);
        mSelectDeleteRl.setVisibility(View.GONE);

        initEvent();
    }

    private void initEvent() {
        mAddUserTv.setOnClickListener(this);
        mSelectCb.setOnClickListener(this);
        mDeleteTv.setOnClickListener(this);
        mUserSettingTv.setOnClickListener(this);
        mGroupDeleteTv.setOnClickListener(this);
        mEditUserNameTv.setOnClickListener(this);
        mShareTv.setOnClickListener(this);
        mCreateAdminTv.setOnClickListener(this);
        mCreateMembersTv.setOnClickListener(this);
        mCreateCancelTv.setOnClickListener(this);
        mDeleteCancelTV.setOnClickListener(this);
        mSingleDeleteTv.setOnClickListener(this);

        mSelectCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mLoadDialog != null && mLoadDialog.isShowing()) {
                    return;
                }
                ArrayList<DeviceUser> deleteUsers = DeviceUserDao.getInstance(mCtx).queryDeviceUsers(mDefaultDevice.getDeviceNodeId());
                mUserAdapter.mDeleteUsers.clear();
                if (isChecked) {
                    mTipTv.setText(R.string.cancel);
                    int index = -1;
                    for (DeviceUser user : deleteUsers) {
                        if (user.getUserId() == mDefaultUser.getUserId()) {
                            index = deleteUsers.indexOf(user);
                        }
                    }
                    if (index != -1) {
                        deleteUsers.remove(index);
                    }
                    mUserAdapter.mDeleteUsers.addAll(deleteUsers);
                    mUserAdapter.choiceALLDelete(true);
                } else {
                    mTipTv.setText(R.string.all_election);
                    mUserAdapter.choiceALLDelete(false);
                }
                mUserAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        mIsHint = isVisibleToUser;
        super.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public void deviceStateChange(Device device, int state) {
        switch (state) {
            case BleMsg.STATE_DISCONNECTED:
                DialogUtils.closeDialog(mLoadDialog);
                showMessage(mCtx.getString(R.string.ble_disconnect));
                break;
            case BleMsg.STATE_CONNECTED:

                break;
            case BleMsg.GATT_SERVICES_DISCOVERED:
                break;
            default:
                break;
        }
    }

    @Override
    public void dispatchUiCallback(Message msg, Device device, int type) {
        mDevice = device;
        Bundle extra = msg.getData();
        Serializable serializable = extra.getSerializable(BleMsg.KEY_SERIALIZABLE);
        if (!mIsHint || serializable == null) {
            DialogUtils.closeDialog(mLoadDialog);
            return;
        }
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_1E:

                final byte[] errCode = extra.getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode != null)
                    dispatchErrorCode(errCode[3], serializable);
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_12:

                byte[] buf = new byte[64];
                byte[] authBuf = new byte[64];
                authBuf[0] = 0x01;

                byte[] authCode = extra.getByteArray(BleMsg.KEY_AUTH_CODE);
                if (authCode == null) return;

                byte[] userIdBuf = new byte[2];

                System.arraycopy(authCode, 0, userIdBuf, 0, 2);

                Short userId = Short.parseShort(StringUtil.bytesToHexString(userIdBuf), 16);

//                byte[] timeQr = new byte[4];
//                StringUtil.int2Bytes((int) (System.currentTimeMillis() / 1000 + 30 * 60), timeQr);
//                System.arraycopy(timeQr, 0, authBuf, 1, 4); //二维码有效时间
//
//                System.arraycopy(authCode, 0, authBuf, 5, 30); //鉴权码
//
//                Arrays.fill(authBuf, 35, 64, (byte) 0x1d); //补充字节
//
//                try {
//                    AES_ECB_PKCS7.AES256Encode(authBuf, buf, MessageCreator.mQrSecret);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//
//                String path = createQRcodeImage(buf, ConstantUtil.DEVICE_MASTER);
//                if (path != null) {
                DeviceUser deviceUser = createDeviceUser(userId, null, StringUtil.bytesToHexString(authCode));

                if (deviceUser != null) {
                    mUserAdapter.addItem(deviceUser);
                    DialogUtils.closeDialog(mLoadDialog);
                }
//                }
                mCreateUserDialog.cancel();
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_26:
                short userIdTag = (short) serializable;
                LogUtil.i(TAG, "receiver msg 26 : " + userIdTag);
                if (userIdTag <= 0 || userIdTag > 200) {
                    DialogUtils.closeDialog(mLoadDialog);
                    return;
                }
                LogUtil.d(TAG, "mCount   " + mCountUsers + '\n' + mCheckUsers.size());
                // 记录同步数据，同步完成后关闭Dialog
                if (++mCountUsers == mCheckUsers.size() - 1) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mCountUsers = 0;
                }
                byte[] userInfo = extra.getByteArray(BleMsg.KEY_USER_MSG);
                if (userInfo != null) {
                    DeviceUser devUser = DeviceUserDao.getInstance(mCtx).queryUser(mDefaultDevice.getDeviceNodeId(), userIdTag);

                    devUser.setUserStatus(userInfo[0]);

                    if (devUser.getUserPermission() == ConstantUtil.DEVICE_MEMBER)
                        checKMembers(userInfo, devUser);

                    DeviceKeyDao.getInstance(mCtx).checkDeviceKey(devUser.getDevNodeId(), devUser.getUserId(), userInfo[1], ConstantUtil.USER_PWD, "1");
                    DeviceKeyDao.getInstance(mCtx).checkDeviceKey(devUser.getDevNodeId(), devUser.getUserId(), userInfo[2], ConstantUtil.USER_NFC, "1");
                    DeviceKeyDao.getInstance(mCtx).checkDeviceKey(devUser.getDevNodeId(), devUser.getUserId(), userInfo[3], ConstantUtil.USER_FINGERPRINT, "1");
                    DeviceKeyDao.getInstance(mCtx).checkDeviceKey(devUser.getDevNodeId(), devUser.getUserId(), userInfo[4], ConstantUtil.USER_FINGERPRINT, "2");
                    DeviceKeyDao.getInstance(mCtx).checkDeviceKey(devUser.getDevNodeId(), devUser.getUserId(), userInfo[5], ConstantUtil.USER_FINGERPRINT, "3");
                    DeviceKeyDao.getInstance(mCtx).checkDeviceKey(devUser.getDevNodeId(), devUser.getUserId(), userInfo[6], ConstantUtil.USER_FINGERPRINT, "4");
                    DeviceKeyDao.getInstance(mCtx).checkDeviceKey(devUser.getDevNodeId(), devUser.getUserId(), userInfo[7], ConstantUtil.USER_FINGERPRINT, "5");

                    DeviceUserDao.getInstance(mCtx).updateDeviceUser(devUser);
                    LogUtil.d(TAG, "mShowQR =" + mShowQR);
                    if (mShowQR) {
                        mShowQR = false;
                        byte[] authTime = new byte[4];
                        System.arraycopy(userInfo, 8, authTime, 0, 4);

                        setAuthCode(authTime, mDefaultDevice, devUser);

                        String qrPath = devUser.getQrPath();
                        if (StringUtil.checkNotNull(qrPath)) {
                            String qrName = StringUtil.getFileName(qrPath);
                            if (System.currentTimeMillis() - Long.parseLong(qrName) <= 30 * 60 * 60) {
                                Log.d(TAG, "qrName = " + qrName);
                                displayImage(qrPath);
                            } else {
                                File delQr = new File(qrPath);
                                boolean result = delQr.delete();
                                if (result) {
                                    mCtx.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + qrPath)));
                                }
                                String newPath = createQr(devUser);
                                Log.d(TAG, "newPath = " + newPath);
                            }

                        } else {
                            String newPath = createQr(devUser);
                            Log.d(TAG, "newPath = " + newPath);
                        }
                    }

                    int index = -1;
                    for (DeviceUser member : mCheckUsers) {
                        if (member.getUserId() == devUser.getUserId()) {
                            index = mCheckUsers.indexOf(member);
                        }
                    }
                    if (index != -1) {
                        mCheckUsers.remove(index);
                    }
                    if (mCheckUsers.size() == 0) {
                        FileUtil.clearQr(mCtx, ".jpg");
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        DialogUtils.closeDialog(mLoadDialog);
                        if (mHandler.hasMessages(CHECK_USERS_STATE_TIME_OUT)) {
                            mHandler.removeMessages(CHECK_USERS_STATE_TIME_OUT);
                        }
                        mUserAdapter.setDataSource();
                        mUserAdapter.notifyDataSetChanged();
                    }
                }
                break;
            default:
                break;

        }
    }

    private void checKMembers(byte[] userInfo, DeviceUser user) {

        if (userInfo != null) {
            LogUtil.e(TAG, "userInfo : " + StringUtil.bytesToHexString(userInfo, ":"));
            byte[] stTsBegin = new byte[4];
            System.arraycopy(userInfo, 12, stTsBegin, 0, 4); //第一起始时间

            byte[] stTsEnd = new byte[4];
            System.arraycopy(userInfo, 16, stTsEnd, 0, 4); //第一结束时间

            byte[] ndTsBegin = new byte[4];
            System.arraycopy(userInfo, 20, ndTsBegin, 0, 4); //第二起始时间

            byte[] ndTsEnd = new byte[4];
            System.arraycopy(userInfo, 24, ndTsEnd, 0, 4); //第二结束时间

            byte[] thTsBegin = new byte[4];
            System.arraycopy(userInfo, 28, thTsBegin, 0, 4); //第三结束时间

            byte[] thTsEnd = new byte[4];
            System.arraycopy(userInfo, 32, thTsEnd, 0, 4); //第三结束时间

            byte[] lcTsBegin = new byte[4];
            System.arraycopy(userInfo, 36, lcTsBegin, 0, 4); //生命周期开始时间

            byte[] lcTsEnd = new byte[4];
            System.arraycopy(userInfo, 40, lcTsEnd, 0, 4); //生命周期结束时间

            LogUtil.d(TAG, "lcTsBegin : " + StringUtil.bytesToHexString(lcTsBegin, ":") + " lcTsEnd : " + StringUtil.bytesToHexString(lcTsEnd, ":"));

            String stBegin = StringUtil.byte2Int(stTsBegin);
            if (!stBegin.equals("0000")) {
                user.setStTsBegin(DateTimeUtil.stampToMinute(stBegin + "000"));
            } else {
                user.setStTsBegin("00:00");
            }

            String stEnd = StringUtil.byte2Int(stTsEnd);
            if (!stEnd.equals("0000")) {
                user.setStTsEnd(DateTimeUtil.stampToMinute(stEnd + "000"));
            } else
                user.setStTsEnd("00:00");

            String ndBegin = StringUtil.byte2Int(ndTsBegin);
            if (!ndBegin.equals("0000")) {
                user.setNdTsBegin(DateTimeUtil.stampToMinute(ndBegin + "000"));
            } else
                user.setNdTsBegin("00:00");

            String ndEnd = StringUtil.byte2Int(ndTsEnd);
            if (!ndEnd.equals("0000")) {
                user.setNdTsend(DateTimeUtil.stampToMinute(ndEnd + "000"));
            } else
                user.setNdTsend("00:00");

            String thBegin = StringUtil.byte2Int(thTsBegin);
            if (!thBegin.equals("0000")) {
                user.setThTsBegin(DateTimeUtil.stampToMinute(thBegin + "000"));
            } else
                user.setThTsBegin("00:00");

            String thEnd = StringUtil.byte2Int(thTsEnd);
            if (!thEnd.equals("0000")) {
                user.setThTsEnd(DateTimeUtil.stampToMinute(thEnd + "000"));
            } else user.setThTsEnd("00:00");

            String lcBegin = StringUtil.byte2Int(lcTsBegin);
            String lcEnd = StringUtil.byte2Int(lcTsEnd);

            user.setLcBegin(lcBegin);
            user.setLcEnd(lcEnd);

            LogUtil.d(TAG, "stBegin : " + stBegin + "\n" +
                    "stEnd : " + stEnd + "\n" +
                    "ndBegin : " + ndBegin + "\n" +
                    "ndEnd : " + ndEnd + "\n" +
                    "thBegin : " + thBegin + "\n" +
                    "thEnd : " + thEnd + "\n" +
                    "lcBegin : " + lcBegin + "\n" +
                    "lcEnd : " + lcEnd + "\n");
        }


    }

    private void dispatchErrorCode(byte errCode, Serializable serializable) {
        LogUtil.i(TAG, "errCode : " + errCode);
        switch (errCode) {
            case BleMsg.TYPE_ADD_USER_SUCCESS:
                showMessage(mCtx.getString(R.string.add_user_success));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_ADD_USER_FAILED:
                showMessage(mCtx.getString(R.string.add_user_failed));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_DELETE_USER_SUCCESS:
                if (serializable instanceof DeviceUser) {
                    DeviceUser user = (DeviceUser) serializable;
                    DeviceUser deleteUser = DeviceUserDao.getInstance(mCtx).queryUser(mNodeId, user.getUserId());
                    DeviceKeyDao.getInstance(mCtx).deleteUserKey(deleteUser.getUserId(), deleteUser.getDevNodeId()); //删除开锁信息

                    mUserAdapter.removeItem(deleteUser);
                    if (mUserAdapter.mDeleteUsers.size() == 0 || mSingleMode) {
                        mSingleMode = false;
                        showMessage(mCtx.getString(R.string.delete_user_success));
                        selectDelete(false);
                        DialogUtils.closeDialog(mLoadDialog);
                    } else return;
                }
                break;
            case BleMsg.TYPE_DELETE_USER_FAILED:
                showMessage(mCtx.getString(R.string.delete_user_failed));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_PAUSE_USER_SUCCESS:
                if (serializable instanceof DeviceUser) {
                    DeviceUser user = (DeviceUser) serializable;
                    showMessage(mCtx.getString(R.string.pause_user_success));
                    DeviceUser pauseUser = DeviceUserDao.getInstance(mCtx).queryUser(mNodeId, user.getUserId());
                    pauseUser.setUserStatus(ConstantUtil.USER_PAUSE);
                    pauseUser.setActivePause(true);
                    DeviceUserDao.getInstance(mCtx).updateDeviceUser(pauseUser);
                    mUserAdapter.changeUserState(pauseUser, ConstantUtil.USER_PAUSE);
                    DialogUtils.closeDialog(mLoadDialog);
                }
                break;
            case BleMsg.TYPE_PAUSE_USER_FAILED:
                showMessage(mCtx.getString(R.string.pause_user_failed));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_RECOVERY_USER_SUCCESS:
                if (serializable instanceof DeviceUser) {
                    DeviceUser user = (DeviceUser) serializable;
                    showMessage(mCtx.getString(R.string.recovery_user_success));
                    DeviceUser recoveryUser = DeviceUserDao.getInstance(mCtx).queryUser(mNodeId, user.getUserId());
                    recoveryUser.setUserStatus(ConstantUtil.USER_ENABLE);
                    recoveryUser.setActivePause(false);
                    DeviceUserDao.getInstance(mCtx).updateDeviceUser(recoveryUser);
                    mUserAdapter.changeUserState(recoveryUser, ConstantUtil.USER_ENABLE);
                    DialogUtils.closeDialog(mLoadDialog);
                }
                break;
            case BleMsg.TYPE_RECOVERY_USER_FAILED:
                showMessage(mCtx.getString(R.string.recovery_user_failed));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_USER_FULL:
                if (serializable instanceof DeviceUser) {
                    DeviceUser user = (DeviceUser) serializable;
                    if (user.getUserPermission() == ConstantUtil.DEVICE_MASTER) { //管理员编号
                        showMessage(getString(R.string.administrator) + getString(R.string.add_user_full));
                    } else if (user.getUserPermission() == ConstantUtil.DEVICE_MEMBER) { //普通用户
                        showMessage(getString(R.string.members) + getString(R.string.add_user_full));
                    } else { //临时用户
                        showMessage(getString(R.string.tmp_user) + getString(R.string.add_user_full));
                    }
                }

                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_OPEN_SLIDE:
                if (mBleManagerHelper.getBleCardService() != null)
                    mBleManagerHelper.getBleCardService().cancelCmd(Message.TYPE_BLE_SEND_CMD_11 + "#" + "single");
                showMessage(getString(R.string.plz_open_slide));
                break;
            default:
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
                LogUtil.e(msg.getType() + " can't receiver msg!");
                break;
            case Message.EXCEPTION_SEND_FAIL:
                DialogUtils.closeDialog(mLoadDialog);
                LogUtil.e(msg.getType() + " send failed!");
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

    private class UserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_HEAD = 0;
        private static final int TYPE_BODY = 1;
        private static final int TYPE_FOOT = 2;
        private int countHead = 0;
        private int countFoot = 2;
        private Context mContext;
        private ArrayList<DeviceUser> mUserList;
        private Boolean mVisiBle = false;
        public ArrayList<DeviceUser> mDeleteUsers = new ArrayList<>();
        public boolean mAllDelete = false;

        private int getBodySize() {
            return mUserList.size();
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


        public UserAdapter(Context context) {
            mContext = context;
            mUserList = DeviceUserDao.getInstance(mCtx).queryDeviceUsers(mDefaultDevice.getDeviceNodeId());
            int index = -1;

            ListIterator<DeviceUser> userListIterator = mUserList.listIterator();

            while (userListIterator.hasNext()) {
                DeviceUser user = userListIterator.next();
                if (user.getUserId() == mDefaultUser.getUserId() || user.getUserId() == 1 && mDefaultUser.getUserId() != 1) {
                    userListIterator.remove();
                } else mCheckUsers.add(user);
            }
            mUserList.add(0, mDefaultUser);

            if (mCheckUsers.size() > 0) {
                DialogUtils.closeDialog(mLoadDialog);
                mLoadDialog = DialogUtils.createLoadingDialog(mCtx, getString(R.string.sync_data));
                mLoadDialog.show();
                android.os.Message msg = android.os.Message.obtain();
                msg.what = CHECK_USERS_STATE_TIME_OUT;
                mHandler.sendMessageDelayed(msg, 5 * 1000);
            }
            for (DeviceUser user : mCheckUsers) {
                if (mDevice != null && mDevice.getState() == Device.BLE_CONNECTED) {
                    LogUtil.d(TAG, "mCount send25 " + user.getUserId());
                    mBleManagerHelper.getBleCardService().sendCmd25(user.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                } else showMessage(getString(R.string.unconnected_device));
            }

        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            switch (viewType) {
                case TYPE_HEAD:
                    return new FootViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_recycle_foot, parent, false));
                case TYPE_BODY:
                    View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_user, parent, false);
                    SwipeLayout swipelayout = inflate.findViewById(R.id.item_ll_user);
                    swipelayout.setClickToClose(true);
                    swipelayout.setRightSwipeEnabled(true);
                    return new UserViewHolder(inflate);
                case TYPE_FOOT:
                    return new FootViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_recycle_foot, parent, false));
                default:
                    return new FootViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_recycle_foot, parent, false));
            }
        }

        public void setDataSource() {
            mUserList = DeviceUserDao.getInstance(mCtx).queryDeviceUsers(mDefaultDevice.getDeviceNodeId());
            int index = -1;

            ListIterator<DeviceUser> userListIterator = mUserList.listIterator();

            while (userListIterator.hasNext()) {
                DeviceUser user = userListIterator.next();
                if (user.getUserId() == mDefaultUser.getUserId() || (user.getUserId() == 1 && mDefaultUser.getUserId() != 1)) {
                    userListIterator.remove();
                } else mCheckUsers.add(user);
            }
            mDefaultUser = DeviceUserDao.getInstance(mActivity).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId());
            mUserList.add(0, mDefaultUser); //将默认用户调整至最上方
        }

        public void choiceItemDelete(boolean visible) {
            mVisiBle = visible;
        }

        public void choiceALLDelete(boolean allDelete) {
            mAllDelete = allDelete;
        }

        public void changeUserState(DeviceUser changeUser, int state) {
            int index = -1;
            for (DeviceUser user : mUserList) {
                if (user.getUserId() == changeUser.getUserId()) {
                    index = mUserList.indexOf(user);
                    user.setUserStatus(state);
                    if (index != -1) {
                        notifyItemChanged(index);
                    }
                }
            }
        }

        public void addItem(DeviceUser user) {
            mUserList.add(mUserList.size(), user);
            notifyItemInserted(mUserList.size());
        }

        public void removeItem(DeviceUser delUser) {

            int index = -1;
            int delIndex = -1;
            for (DeviceUser user : mUserList) {
                if (user.getUserId() == delUser.getUserId()) {
                    index = mUserList.indexOf(user);
                }
            }
            if (index != -1) {
                DeviceUser del = mUserList.remove(index);

                mDeleteUsers.remove(del);
                DeviceUserDao.getInstance(mCtx).delete(del);
                notifyItemRemoved(index);
            }

            for (DeviceUser deleteUser : mDeleteUsers) {
                if (deleteUser.getUserId() == delUser.getUserId()) {
                    delIndex = mDeleteUsers.indexOf(deleteUser);
                }
            }

            if (delIndex != -1) {
                mDeleteUsers.remove(delIndex);
            }
        }

        private void editUserName() {
            final Dialog mEditorNameDialog = DialogUtils.createEditorDialog(mActivity, getString(R.string.modify_note_name), mSettingUser.getUserName());

            final EditText editText = mEditorNameDialog.findViewById(R.id.editor_et);
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});

            //修改呢称响应事件
            mEditorNameDialog.findViewById(R.id.dialog_confirm_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final String newName = editText.getText().toString();
                    if (!newName.isEmpty()) {
                        mSettingUser.setUserName(newName);
                        DeviceUserDao.getInstance(mContext).updateDeviceUser(mSettingUser);
                        int index = -1;
                        for (DeviceUser user : mUserList) {
                            if (user.getUserId() == mSettingUser.getUserId()) {
                                index = mUserList.indexOf(user);
                                if (index != -1) {
                                    notifyItemChanged(index);
                                }
                            }
                        }
                    } else {
                        ToastUtil.showLong(mActivity, R.string.cannot_be_empty_str);
                    }
                    mEditorNameDialog.dismiss();
                }
            });

            editText.setText(mSettingUser.getUserName());
            mEditorNameDialog.show();
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
            if (holder instanceof UserViewHolder) {
                final DeviceUser userInfo = mUserList.get(position - countHead);
                if (userInfo != null) {
//                holder.mEditIbtn.setVisibility(View.GONE);

                    if (userInfo.getUserId() == mDefaultUser.getUserId()) {
                        ((UserViewHolder) holder).mNameTv.setText(userInfo.getUserName() + "(我)");
//                        ((UserViewHolder) holder).mSwipeLayout.setRightSwipeEnabled(false);

                        if (userInfo.getUserPermission() == ConstantUtil.DEVICE_MASTER)
                            ((UserViewHolder) holder).mUserNumberTv.setText("00" + String.valueOf(userInfo.getUserId()));
                        else if (userInfo.getUserPermission() == ConstantUtil.DEVICE_MEMBER)
                            ((UserViewHolder) holder).mUserNumberTv.setText(String.valueOf(userInfo.getUserId()));

//                        ((UserViewHolder) holder).mUserStateTv.setText(mContext.getString(R.string.normal));
//                        ((UserViewHolder) holder).mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.color_green));
//                        refreshStatus(userInfo.getUserStatus(), ((UserViewHolder) holder));
//                        ((UserViewHolder) holder).mUserContent.setOnLongClickListener(null);
                        ((UserViewHolder) holder).mDeleteRl.setVisibility(View.GONE);
                        ((UserViewHolder) holder).mUserContent.setOnLongClickListener(new View.OnLongClickListener() {

                            @Override
                            public boolean onLongClick(View v) {
                                showBottomDialog(userInfo);
                                return true;
                            }
                        });

                        ((UserViewHolder) holder).mDeleteCb.setChecked(false);
                    } else if (userInfo.getUserId() == 1 && mDefaultUser.getUserId() != 1) { //第一个绑定的用户
                        ((UserViewHolder) holder).mNameTv.setText(userInfo.getUserName() + "(主)");
//                        ((UserViewHolder) holder).mSwipeLayout.setRightSwipeEnabled(false);

                        if (userInfo.getUserPermission() == ConstantUtil.DEVICE_MASTER)
                            ((UserViewHolder) holder).mUserNumberTv.setText("00" + String.valueOf(userInfo.getUserId()));
                        else if (userInfo.getUserPermission() == ConstantUtil.DEVICE_MEMBER)
                            ((UserViewHolder) holder).mUserNumberTv.setText(String.valueOf(userInfo.getUserId()));

//                        ((UserViewHolder) holder).mUserStateTv.setText(mContext.getString(R.string.normal));
//                        ((UserViewHolder) holder).mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.color_green));
//                        refreshStatus(userInfo.getUserStatus(), ((UserViewHolder) holder));
//                        ((UserViewHolder) holder).mUserContent.setOnLongClickListener(null);
                        ((UserViewHolder) holder).mDeleteRl.setVisibility(View.GONE);

                        ((UserViewHolder) holder).mDeleteCb.setChecked(false);
                        ((UserViewHolder) holder).mUserContent.setOnLongClickListener(new View.OnLongClickListener() {

                            @Override
                            public boolean onLongClick(View v) {
                                showBottomDialog(userInfo);
                                return true;
                            }
                        });

                    } else {
                        ((UserViewHolder) holder).mSwipeLayout.setRightSwipeEnabled(true);
                        ((UserViewHolder) holder).mNameTv.setText(userInfo.getUserName());

//                        refreshStatus(userInfo.getUserStatus(), ((UserViewHolder) holder)); //刷新用户状态

                        if (userInfo.getUserPermission() == ConstantUtil.DEVICE_MASTER)
                            ((UserViewHolder) holder).mUserNumberTv.setText("00" + String.valueOf(userInfo.getUserId()));
                        else if (userInfo.getUserPermission() == ConstantUtil.DEVICE_MEMBER)
                            ((UserViewHolder) holder).mUserNumberTv.setText(String.valueOf(userInfo.getUserId()));


                        ((UserViewHolder) holder).mUserStatus.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                doClick(((UserViewHolder) holder), userInfo.getUserId());
                            }
                        });

                        ((UserViewHolder) holder).mDeleteCb.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (((UserViewHolder) holder).mDeleteCb.isChecked()) {
                                    mDeleteUsers.add(userInfo);
                                } else {
                                    int delIndex = -1;

                                    for (DeviceUser deleteUser : mDeleteUsers) {
                                        if (deleteUser.getUserId() == userInfo.getUserId()) {
                                            delIndex = mDeleteUsers.indexOf(deleteUser);
                                        }
                                    }

                                    if (delIndex != -1) {
                                        mDeleteUsers.remove(delIndex);
                                    }
                                }
                            }
                        });

                        ((UserViewHolder) holder).mUserContent.setOnLongClickListener(new View.OnLongClickListener() {

                            @Override
                            public boolean onLongClick(View v) {
                                showBottomDialog(userInfo);
                                return true;
                            }
                        });

                        if (mVisiBle)
                            ((UserViewHolder) holder).mDeleteRl.setVisibility(View.VISIBLE);
                        else
                            ((UserViewHolder) holder).mDeleteRl.setVisibility(View.GONE);

                        ((UserViewHolder) holder).mDeleteCb.setChecked(mAllDelete);

                    }
                    ((UserViewHolder) holder).mUserMore.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showBottomDialog(userInfo);
                        }
                    });
                    refreshStatus(userInfo, ((UserViewHolder) holder));
                    LogUtil.d(TAG, "id= " + userInfo.getUserId() + "\n" + "usrInfo=" + userInfo.getUserStatus());
                    ((UserViewHolder) holder).mUserContent.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            Bundle bundle = new Bundle();
                            bundle.putSerializable(BleMsg.KEY_TEMP_USER, userInfo);
                            bundle.putInt(BleMsg.KEY_CURRENT_ITEM, 0);
                            startIntent(DeviceKeyActivity.class, bundle, -1);
                        }
                    });
                }
            }
        }

        /**
         * 刷新用户状态
         *
         * @param userInfo 用户状态标志
         */
        private void refreshStatus(DeviceUser userInfo, UserViewHolder holder) {

            switch (userInfo.getUserStatus()) {
                case ConstantUtil.USER_UNENABLE:
                    holder.mUserStateTv.setText(mContext.getString(R.string.unenable));
                    holder.mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.gray1));
                    holder.mUserStatus.setVisibility(View.GONE);
                    break;
                case ConstantUtil.USER_ENABLE:
                    holder.mUserStateTv.setText(mContext.getString(R.string.normal));
                    holder.mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.color_green));
                    holder.mUserStatus.setVisibility(View.VISIBLE);
                    holder.mUserStatus.setBackgroundResource(R.color.color_red);
                    holder.mSetStateTv.setText(getString(R.string.pause));
                    holder.mSetStateTv.setTag(R.string.pause);
                    break;
                case ConstantUtil.USER_PAUSE:
                    holder.mUserStateTv.setText(mContext.getString(R.string.pause));
                    holder.mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.color_red));
                    holder.mUserStatus.setVisibility(View.VISIBLE);
                    holder.mUserStatus.setBackgroundResource(R.color.color_green);
                    holder.mSetStateTv.setText(getString(R.string.recovery));
                    holder.mSetStateTv.setTag(R.string.recovery);
                    break;
            }
            if (userInfo.getUserId() == mDefaultUser.getUserId()) {
                holder.mUserStatus.setVisibility(View.GONE);
            }
        }

        /**
         * 用户状态切换
         *
         * @param holder item
         * @param userId 用户编号
         */
        private void doClick(UserViewHolder holder, short userId) {
            if (mDevice.getState() == Device.BLE_CONNECTED) {
                DialogUtils.closeDialog(mLoadDialog);
                mLoadDialog = DialogUtils.createLoadingDialog(mCtx, getString(R.string.data_loading));
                mLoadDialog.show();
                switch ((int) holder.mSetStateTv.getTag()) {
                    case R.string.pause:
                        mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_PAUSE_USER, userId, BleMsg.INT_DEFAULT_TIMEOUT);
                        break;
                    case R.string.recovery:
                        mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_RECOVERY_USER, userId, BleMsg.INT_DEFAULT_TIMEOUT);
                        break;
                }
            } else showMessage(getString(R.string.disconnect_ble));
        }

        private void showBottomDialog(DeviceUser userInfo) {
//            shakes();
            mSettingUser = userInfo;
            mUserNameTv.setText(userInfo.getUserName());
            if (userInfo.getUserId() == mDefaultUser.getUserId()) {
                mUserSettingTv.setVisibility(View.GONE);
                mLine.setVisibility(View.GONE);
                mSingleDeleteTv.setVisibility(View.GONE);
            } else if (userInfo.getUserPermission() == ConstantUtil.DEVICE_MASTER) {
                mUserSettingTv.setVisibility(View.GONE);
                mLine.setVisibility(View.GONE);
                mSingleDeleteTv.setVisibility(View.VISIBLE);
            } else {
                mUserSettingTv.setVisibility(View.VISIBLE);
                mLine.setVisibility(View.VISIBLE);
                mSingleDeleteTv.setVisibility(View.VISIBLE);
            }
            mFunctionDialog.show();
        }

        /**
         * 震动
         */
        private void shakes() {
            Vibrator vibrator = (Vibrator) mCtx.getSystemService(LockScreenActivity.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(300);
            }
        }

        @Override
        public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            if (holder instanceof UserViewHolder) {
                ((UserViewHolder) holder).mNameTv.setEnabled(false);
                ((UserViewHolder) holder).mNameTv.setEnabled(true);
            }
        }

        @Override
        public int getItemCount() {
            return mUserList.size() + countFoot + countHead;
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            RelativeLayout mDeleteRl;
            TextView mUserStateTv;
            ImageButton mEditIbtn;
            SwipeLayout mSwipeLayout;
            TextView mNameTv;
            TextView mUserNumberTv;
            LinearLayout mUserMore;
            LinearLayout mUserStatus;
            CheckBox mDeleteCb;
            LinearLayout mUserContent;
            TextView mSetStateTv;

            UserViewHolder(View itemView) {
                super(itemView);
                mNameTv = itemView.findViewById(R.id.tv_username);
                mDeleteRl = itemView.findViewById(R.id.rl_delete);
                mUserStateTv = itemView.findViewById(R.id.tv_status);
                mEditIbtn = itemView.findViewById(R.id.ib_edit);
                mSwipeLayout = (SwipeLayout) itemView;
                mUserNumberTv = itemView.findViewById(R.id.tv_user_number);
                mUserMore = itemView.findViewById(R.id.ll_more);
                mUserStatus = itemView.findViewById(R.id.ll_status);
                mSetStateTv = itemView.findViewById(R.id.tv_set_status);
                mDeleteCb = itemView.findViewById(R.id.delete_locked);
                mUserContent = itemView.findViewById(R.id.content_ll);
            }
        }

        class FootViewHolder extends RecyclerView.ViewHolder {
            private FootViewHolder(View itemView) {
                super(itemView);
            }
        }
    }

    /**
     * 新界面
     *
     * @param cls    新Activity
     * @param bundle 数据包
     */
    protected void startIntent(Class<?> cls, Bundle bundle, int flag) {
        Intent intent = new Intent();
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        if (flag != -1)
            intent.addFlags(flag);
        intent.setClass(mCtx, cls);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mIsHint) {
            ArrayList<DeviceUser> list = DeviceUserDao.getInstance(mCtx).queryUsers(mNodeId, ConstantUtil.DEVICE_MEMBER);
            for (DeviceUser user : list) {
                StringUtil.checkTempUserStatus(mCtx, user);
            }
        }
        refreshView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBleManagerHelper.removeUiListener(this);
    }
}
