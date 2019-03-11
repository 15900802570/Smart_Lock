package com.smart.lock.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.smart.lock.R;
import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class MumberFragment extends BaseFragment implements View.OnClickListener {
    private final static String TAG = MumberFragment.class.getSimpleName();

    private View mMumberView;
    private RecyclerView mUsersRv;
    private MumberAdapter mMumberAdapter;
    private LinearLayoutManager mLinerLayoutManager;
    private Button mAddUserBtn;
    private RelativeLayout mSelectDeleteRl;
    private TextView mChoiseMumTv;
    private Button mSelectBtn;
    private Button mDeleteBtn;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_add:
                ArrayList<DeviceUser> users = DeviceUserDao.getInstance(mMumberView.getContext()).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_MEMBER);
                if (users != null && users.size() >= 90) {
                    showMessage(mMumberView.getContext().getResources().getString(R.string.members) + mMumberView.getContext().getResources().getString(R.string.add_user_tips));
                    return;
                }
                DialogUtils.closeDialog(mLoadDialog);
                mLoadDialog = DialogUtils.createLoadingDialog(mMumberView.getContext(), getResources().getString(R.string.data_loading));
                closeDialog(15);
                if (mBleManagerHelper.getServiceConnection()) {
                    mBleManagerHelper.getBleCardService().sendCmd11((byte) 2, (short) 0);
                }
                break;
            case R.id.btn_select_all:
                LogUtil.d(TAG, "choise user delete : " + mSelectBtn.getText().toString());

                if ((int) mSelectBtn.getTag() == R.string.all_election) {
                    ArrayList<DeviceUser> deleteUsers = DeviceUserDao.getInstance(mMumberView.getContext()).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_MEMBER);
                    mMumberAdapter.mDeleteUsers.clear();
                    int index = -1;
                    for (DeviceUser user : deleteUsers) {
                        if (user.getUserId().equals(mDefaultUser.getUserId())) {
                            index = deleteUsers.indexOf(user);
                        }
                    }
                    if (index != -1) {
                        deleteUsers.remove(index);
                    }
                    mMumberAdapter.mDeleteUsers.addAll(deleteUsers);
                    mMumberAdapter.chioseALLDelete(true);
                    mSelectBtn.setText(R.string.cancel);
                    mSelectBtn.setTag(R.string.cancel);
                } else if ((int) mSelectBtn.getTag() == R.string.cancel) {
                    mMumberAdapter.mDeleteUsers.clear();
                    mSelectBtn.setText(R.string.all_election);
                    mMumberAdapter.chioseALLDelete(false);
                    mSelectBtn.setTag(R.string.all_election);
                }
                mChoiseMumTv.setText(String.valueOf(mMumberAdapter.mDeleteUsers.size()));
                mMumberAdapter.notifyDataSetChanged();
                break;
            case R.id.btn_delete:

                if (mMumberAdapter.mDeleteUsers.size() != 0) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog = DialogUtils.createLoadingDialog(mMumberView.getContext(), getString(R.string.data_loading));
                    for (DeviceUser devUser : mMumberAdapter.mDeleteUsers) {
                        Short userId = Short.parseShort(devUser.getUserId());
                        LogUtil.d(TAG, "logId = " + userId);
                        mBleManagerHelper.getBleCardService().sendCmd11((byte) 4, userId);
                    }
                    closeDialog(10);
                } else {
                    showMessage(getString(R.string.plz_choise_del_log));
                }

                break;
            default:
                break;
        }
    }

    public void selectDelete(boolean choise) {
        if (choise) {
            mSelectDeleteRl.setVisibility(View.VISIBLE);
        } else {
            mSelectDeleteRl.setVisibility(View.GONE);
        }
        mMumberAdapter.chioseALLDelete(false);
        mMumberAdapter.chioseItemDelete(choise);
        mMumberAdapter.notifyDataSetChanged();
    }


    public void refreshView() {
        mMumberAdapter.setDataSource();
        mMumberAdapter.notifyDataSetChanged();
    }

    @Override
    public View initView() {
        mMumberView = View.inflate(mActivity, R.layout.fragment_user_manager, null);
        mUsersRv = mMumberView.findViewById(R.id.rv_users);
        mAddUserBtn = mMumberView.findViewById(R.id.btn_add);
        mSelectDeleteRl = mMumberView.findViewById(R.id.rl_select_delete);
        mChoiseMumTv = mMumberView.findViewById(R.id.tv_select_num);
        mSelectBtn = mMumberView.findViewById(R.id.btn_select_all);
        mSelectBtn.setTag(R.string.all_election);
        mDeleteBtn = mMumberView.findViewById(R.id.btn_delete);
        return mMumberView;
    }

    public void initDate() {
        mDefaultDevice = DeviceInfoDao.getInstance(mMumberView.getContext()).queryFirstData("device_default", true);
        LogUtil.d(TAG, "mDefaultDevice = " + mDefaultDevice);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mDefaultUser = DeviceUserDao.getInstance(mActivity).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getDeviceUser());
        mBleManagerHelper = BleManagerHelper.getInstance(mMumberView.getContext(), mNodeId, false);
        mMumberAdapter = new MumberAdapter(mMumberView.getContext());
        mLinerLayoutManager = new LinearLayoutManager(mMumberView.getContext(), LinearLayoutManager.VERTICAL, false);
        mUsersRv.setLayoutManager(mLinerLayoutManager);
        mUsersRv.setItemAnimator(new DefaultItemAnimator());
        mUsersRv.setAdapter(mMumberAdapter);

        LocalBroadcastManager.getInstance(mMumberView.getContext()).registerReceiver(mumberUserReciver, intentFilter());
        mAddUserBtn.setText(R.string.create_user);
        mSelectDeleteRl.setVisibility(View.GONE);
        initEvent();
    }

    private void initEvent() {
        mAddUserBtn.setOnClickListener(this);
        mSelectBtn.setOnClickListener(this);
        mDeleteBtn.setOnClickListener(this);
    }

    private static IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.EXTRA_DATA_MSG_12);
        intentFilter.addAction(BleMsg.STR_RSP_MSG1E_ERRCODE);
        intentFilter.addAction(BleMsg.STR_RSP_SET_TIMEOUT);
        intentFilter.addAction(BleMsg.STR_RSP_OPEN_TEST);
        return intentFilter;
    }

    /**
     * 广播接收
     */
    private final BroadcastReceiver mumberUserReciver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // 4.2.3 MSG 12
            if (action.equals(BleMsg.EXTRA_DATA_MSG_12)) {

                DeviceUser user = (DeviceUser) intent.getSerializableExtra(BleMsg.KEY_SERIALIZABLE);
                if (user == null || user.getUserPermission() != ConstantUtil.DEVICE_MEMBER) {
                    mHandler.removeCallbacks(mRunnable);
                    DialogUtils.closeDialog(mLoadDialog);
                    return;
                }
                byte[] buf = new byte[32];
                byte[] authBuf = new byte[32];
                authBuf[0] = 0x01;
                System.arraycopy(intent.getByteArrayExtra(BleMsg.KEY_USER_ID), 0, authBuf, 1, 2);
                System.arraycopy(intent.getByteArrayExtra(BleMsg.KEY_NODE_ID), 0, authBuf, 3, 8);
                System.arraycopy(intent.getByteArrayExtra(BleMsg.KEY_BLE_MAC), 0, authBuf, 11, 6);

                byte[] timeBuf = new byte[4];
                StringUtil.int2Bytes((int) (System.currentTimeMillis() / 1000 + 30 * 60), timeBuf);
                LogUtil.d(TAG, "time = " + Arrays.toString(timeBuf));
                System.arraycopy(timeBuf, 0, authBuf, 17, 4);

                Arrays.fill(authBuf, 21, 32, (byte) 0x11);

                String userId = StringUtil.bytesToHexString(intent.getByteArrayExtra(BleMsg.KEY_USER_ID));

                LogUtil.d(TAG, "authBuf = " + Arrays.toString(authBuf));

                try {
                    AES_ECB_PKCS7.AES256Encode(authBuf, buf, MessageCreator.mQrSecret);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                LogUtil.d(TAG, "buf = " + Arrays.toString(buf));

                String path = createQRcodeImage(buf);
                Log.d(TAG, "path = " + path);
                if (path != null) {
                    mMumberAdapter.addItem(createDeviceUser(String.valueOf(Integer.parseInt(userId, 16)), path, ConstantUtil.DEVICE_MEMBER));
                }


                mHandler.removeCallbacks(mRunnable);
                DialogUtils.closeDialog(mLoadDialog);
            }

            //MSG1E 设备->apk，返回信息
            if (action.equals(BleMsg.STR_RSP_MSG1E_ERRCODE)) {
                DeviceUser user = (DeviceUser) intent.getSerializableExtra(BleMsg.KEY_SERIALIZABLE);
                if (user != null) {
                    DeviceUser delUser = DeviceUserDao.getInstance(mMumberView.getContext()).queryUser(mNodeId, user.getUserId());
                    if (user == null || delUser == null || delUser.getUserPermission() != ConstantUtil.DEVICE_MEMBER) {
                        mHandler.removeCallbacks(mRunnable);
                        DialogUtils.closeDialog(mLoadDialog);
                        return;
                    }
                } else return;

                final byte[] errCode = intent.getByteArrayExtra(BleMsg.KEY_ERROR_CODE);

                Log.d(TAG, "errCode[3] = " + errCode[3]);

                if (errCode[3] == 0x2) {
                    showMessage(mMumberView.getContext().getString(R.string.add_user_success));
                } else if (errCode[3] == 0x3) {
                    showMessage(mMumberView.getContext().getString(R.string.add_user_failed));
                } else if (errCode[3] == 0x4) {
                    showMessage(mMumberView.getContext().getString(R.string.delete_user_success));

                    DeviceUser deleteUser = DeviceUserDao.getInstance(mMumberView.getContext()).queryUser(mNodeId, user.getUserId());
                    Log.d(TAG, "deleteUser = " + deleteUser.toString());
                    mMumberAdapter.removeItem(deleteUser);

                } else if (errCode[3] == 0x5) {
                    showMessage(mMumberView.getContext().getString(R.string.delete_user_failed));
                } else if (errCode[3] == 0x00) {
                    showMessage(mMumberView.getContext().getString(R.string.pause_user_success));

                    DeviceUser pauseUser = DeviceUserDao.getInstance(mMumberView.getContext()).queryUser(mNodeId, user.getUserId());
                    pauseUser.setUserStatus(ConstantUtil.USER_PAUSE);
                    DeviceUserDao.getInstance(mMumberView.getContext()).updateDeviceUser(pauseUser);
                    mMumberAdapter.changeUserState(pauseUser, ConstantUtil.USER_PAUSE);

                } else if (errCode[3] == 0x01) {
                    showMessage(mMumberView.getContext().getString(R.string.pause_user_failed));
                } else if (errCode[3] == 0x06) {
                    showMessage(mMumberView.getContext().getString(R.string.recovery_user_success));

                    DeviceUser pauseUser = DeviceUserDao.getInstance(mMumberView.getContext()).queryUser(mNodeId, user.getUserId());
                    pauseUser.setUserStatus(ConstantUtil.USER_ENABLE);
                    DeviceUserDao.getInstance(mMumberView.getContext()).updateDeviceUser(pauseUser);
                    mMumberAdapter.changeUserState(pauseUser, ConstantUtil.USER_ENABLE);
                } else if (errCode[3] == 0x07) {
                    showMessage(mMumberView.getContext().getString(R.string.recovery_user_failed));
                }

                mHandler.removeCallbacks(mRunnable);
                DialogUtils.closeDialog(mLoadDialog);
            }

        }
    };

    private class MumberAdapter extends RecyclerView.Adapter<MumberAdapter.MumberViewHoler> {
        private Context mContext;
        private ArrayList<DeviceUser> mUserList;
        private Boolean mVisiBle = false;
        public ArrayList<DeviceUser> mDeleteUsers = new ArrayList<>();
        public boolean mAllDelete = false;
        private SwipeLayout mSwipelayout;

        public MumberAdapter(Context context) {
            mContext = context;
            mUserList = DeviceUserDao.getInstance(mContext).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_MEMBER);
            int index = -1;
            for (DeviceUser user : mUserList) {
                if (user.getUserId().equals(mDefaultUser.getUserId())) {
                    index = mUserList.indexOf(user);
                }
            }
            if (index != -1) {
                mUserList.remove(index);
            }
        }

        @NonNull
        @Override
        public MumberViewHoler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_user, parent, false);
            mSwipelayout = inflate.findViewById(R.id.item_ll_user);
            mSwipelayout.setClickToClose(true);
            mSwipelayout.setRightSwipeEnabled(true);
            return new MumberViewHoler(inflate);
        }

        public void setDataSource() {
            mUserList = DeviceUserDao.getInstance(mContext).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_MEMBER);
            int index = -1;
            for (DeviceUser user : mUserList) {
                if (user.getUserId().equals(mDefaultUser.getUserId())) {
                    index = mUserList.indexOf(user);
                }
            }
            if (index != -1) {
                mUserList.remove(index);
            }
        }

        public void chioseItemDelete(boolean visible) {
            mVisiBle = visible;
        }


        public void chioseALLDelete(boolean allDelete) {
            mAllDelete = allDelete;
        }

        public void changeUserState(DeviceUser changeUser, int state) {
            int index = -1;
            for (DeviceUser user : mUserList) {
                if (user.getUserId().equals(changeUser.getUserId())) {
                    index = mUserList.indexOf(user);
                    user.setUserStatus(state);
                }
                notifyItemChanged(index);
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
                if (user.getUserId().equals(delUser.getUserId())) {
                    index = mUserList.indexOf(user);
                }
            }
            if (index != -1) {
                DeviceUser del = mUserList.remove(index);

                boolean result = mDeleteUsers.remove(del);
                Log.d(TAG, "result = " + result);
                DeviceUserDao.getInstance(mMumberView.getContext()).delete(del);
                notifyItemRemoved(index);
            }

            for (DeviceUser deleteUser : mDeleteUsers) {
                if (deleteUser.getUserId().equals(delUser.getUserId())) {
                    delIndex = mDeleteUsers.indexOf(deleteUser);
                }
            }

            if (delIndex != -1) {
                mDeleteUsers.remove(delIndex);
            }
            mChoiseMumTv.setText(String.valueOf(mMumberAdapter.mDeleteUsers.size()));

        }

        @Override
        public void onBindViewHolder(@NonNull final MumberViewHoler holder, final int position) {
            final DeviceUser userInfo = mUserList.get(position);
            if (userInfo != null) {
                holder.mNameEt.setText(userInfo.getUserName());
                if (userInfo.getUserStatus() == ConstantUtil.USER_UNENABLE) {
                    holder.mUserStateTv.setText(mMumberView.getContext().getResources().getString(R.string.unenable));
                    mSwipelayout.setClickToClose(false);
                } else if (userInfo.getUserStatus() == ConstantUtil.USER_ENABLE) {
                    holder.mUserStateTv.setText(mMumberView.getContext().getResources().getString(R.string.normal));
                    holder.mUserPause.setVisibility(View.VISIBLE);
                    holder.mUserRecovery.setVisibility(View.GONE);
                } else if (userInfo.getUserStatus() == ConstantUtil.USER_PAUSE) {
                    holder.mUserStateTv.setText(mMumberView.getContext().getResources().getString(R.string.pause));
                    holder.mUserPause.setVisibility(View.GONE);
                    holder.mUserRecovery.setVisibility(View.VISIBLE);
                } else
                    holder.mUserStateTv.setText(mMumberView.getContext().getResources().getString(R.string.invalid));
                mSwipelayout.setRightSwipeEnabled(false);
                holder.mUserNumberTv.setText(userInfo.getUserId());

                holder.mEditIbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        userInfo.setUserName(holder.mNameEt.getText().toString().trim());
                        DeviceUserDao.getInstance(mMumberView.getContext()).updateDeviceUser(userInfo);
                    }
                });

                holder.mUserPause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog = DialogUtils.createLoadingDialog(mMumberView.getContext(), getResources().getString(R.string.data_loading));
                        closeDialog(15);
                        if (mBleManagerHelper.getServiceConnection()) {
                            mBleManagerHelper.getBleCardService().sendCmd11((byte) 5, Short.parseShort(userInfo.getUserId()));
                        }
                    }
                });

                holder.mUserRecovery.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog = DialogUtils.createLoadingDialog(mMumberView.getContext(), getResources().getString(R.string.data_loading));
                        closeDialog(15);
                        if (mBleManagerHelper.getServiceConnection()) {
                            mBleManagerHelper.getBleCardService().sendCmd11((byte) 6, Short.parseShort(userInfo.getUserId()));
                        }
                    }
                });


                holder.mDeleteCb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (holder.mDeleteCb.isChecked()) {
                            mDeleteUsers.add(userInfo);

                        } else {
                            mDeleteUsers.remove(userInfo);
                        }
                        mChoiseMumTv.setText(String.valueOf(mMumberAdapter.mDeleteUsers.size()));
                    }
                });

                holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {
                        String path = userInfo.getQrPath();
                        Log.d(TAG, "path = " + path);
                        if (StringUtil.checkNotNull(path)) {
                            String qrName = StringUtil.getFileName(path);
                            if (System.currentTimeMillis() - Long.parseLong(qrName) > 30 * 60 * 60) {
                                Log.d(TAG, "qrName = " + qrName);
                                displayImage(path);
                            } else {
                                String newPath = createQr(userInfo);
                                Log.d(TAG, "newPath = " + newPath);
                            }

                        } else {
                            String newPath = createQr(userInfo);
                            Log.d(TAG, "newPath = " + newPath);
                        }
                        return true;
                    }
                });

                if (mVisiBle)
                    holder.mDeleteRl.setVisibility(View.VISIBLE);
                else
                    holder.mDeleteRl.setVisibility(View.GONE);
                holder.mDeleteCb.setChecked(mAllDelete);
            }
        }


        @Override
        public int getItemCount() {
            return mUserList.size();
        }

        public class MumberViewHoler extends RecyclerView.ViewHolder {
            RelativeLayout mDeleteRl;
            TextView mUserStateTv;
            ImageButton mEditIbtn;
            SwipeLayout mSwipeLayout;
            EditText mNameEt;
            TextView mUserNumberTv;
            LinearLayout mUserRecovery;
            LinearLayout mUserPause;
            CheckBox mDeleteCb;
            LinearLayout mUserContent;

            public MumberViewHoler(View itemView) {
                super(itemView);
                mNameEt = itemView.findViewById(R.id.et_username);
                mDeleteRl = itemView.findViewById(R.id.rl_delete);
                mUserStateTv = itemView.findViewById(R.id.tv_status);
                mEditIbtn = itemView.findViewById(R.id.ib_edit);
                mSwipeLayout = (SwipeLayout) itemView;
                mUserNumberTv = itemView.findViewById(R.id.tv_user_number);
                mUserRecovery = itemView.findViewById(R.id.ll_recovey);
                mUserPause = itemView.findViewById(R.id.ll_pause);
                mDeleteCb = itemView.findViewById(R.id.delete_locked);
                mUserContent = itemView.findViewById(R.id.ll_content);
            }


        }

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(mMumberView.getContext()).unregisterReceiver(mumberUserReciver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
    }
}
