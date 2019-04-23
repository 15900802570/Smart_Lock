package com.smart.lock.ui.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.j256.ormlite.dao.Dao;
import com.smart.lock.R;
import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.ui.TempUserActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.widget.SpacesItemDecoration;

import java.util.ArrayList;
import java.util.Arrays;

public class TempFragment extends BaseFragment implements View.OnClickListener {
    private final static String TAG = TempFragment.class.getSimpleName();

    private View mTempView;
    private RecyclerView mUsersRv;
    private TempAdapter mTempAdapter;
    private LinearLayoutManager mLinerLayoutManager;
    private TextView mAddUserTv;
    private RelativeLayout mSelectDeleteRl;
    private CheckBox mSelectCb;
    private TextView mTipTv;
    private TextView mDeleteTv;
    /**
     * 定义一个内容观察者
     */
    private Dao.DaoObserver mOb = new Dao.DaoObserver() {
        @Override
        public void onChange() {
            mHandler.sendEmptyMessage(0);
        }
    };

    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    refreshView();
                    break;
                default:
                    break;
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_add:
                ArrayList<DeviceUser> users = DeviceUserDao.getInstance(mTempView.getContext()).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_TEMP);
                if (users != null && users.size() >= 5) {
                    showMessage(mTempView.getContext().getResources().getString(R.string.tmp_user) + mTempView.getContext().getResources().getString(R.string.add_user_tips));
                    return;
                }
                mHandler.removeCallbacks(mRunnable);
                DialogUtils.closeDialog(mLoadDialog);
                mLoadDialog.show();
                closeDialog(15);
                if (mBleManagerHelper.getServiceConnection()) {
                    mBleManagerHelper.getBleCardService().sendCmd11((byte) 3, (short) 0,BleMsg.INT_DEFAULT_TIMEOUT);
                }
                break;
//            case R.id.btn_select_all:
//                LogUtil.d(TAG, "choise user delete : " + mSelectBtn.getText().toString());
//
//                if ((int) mSelectBtn.getTag() == R.string.all_election) {
//                    ArrayList<DeviceUser> deleteUsers = DeviceUserDao.getInstance(mTempView.getContext()).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_TEMP);
//                    mTempAdapter.mDeleteUsers.clear();
//                    int index = -1;
//                    for (DeviceUser user : deleteUsers) {
//                        if (user.getUserId() == mDefaultUser.getUserId()) {
//                            index = deleteUsers.indexOf(user);
//                        }
//                    }
//                    if (index != -1) {
//                        deleteUsers.remove(index);
//                    }
//                    mTempAdapter.mDeleteUsers.addAll(deleteUsers);
//                    mTempAdapter.chooseALLDelete(true);
//                    mSelectBtn.setText(R.string.cancel);
//                    mSelectBtn.setTag(R.string.cancel);
//                } else if ((int) mSelectBtn.getTag() == R.string.cancel) {
//                    mTempAdapter.mDeleteUsers.clear();
//                    mSelectBtn.setText(R.string.all_election);
//                    mTempAdapter.chooseALLDelete(false);
//                    mSelectBtn.setTag(R.string.all_election);
//                }
//                mChoiseMumTv.setText(String.valueOf(mTempAdapter.mDeleteUsers.size()));
//                mTempAdapter.notifyDataSetChanged();
//                break;
            case R.id.del_tv:
                if (mTempAdapter.mDeleteUsers.size() != 0) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    for (DeviceUser devUser : mTempAdapter.mDeleteUsers) {
                        mBleManagerHelper.getBleCardService().sendCmd11((byte) 4, devUser.getUserId(),BleMsg.INT_DEFAULT_TIMEOUT);
                    }
                    closeDialog(10);
                } else {
                    showMessage(getString(R.string.plz_choise_del_user));
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
        mTempAdapter.chioseALLDelete(false);
        mTempAdapter.chioseItemDelete(choise);
        mTempAdapter.notifyDataSetChanged();
    }

    public void refreshView() {
        mTempAdapter.setDataSource();
        mTempAdapter.notifyDataSetChanged();
    }

    @Override
    public View initView() {
        mTempView = View.inflate(mActivity, R.layout.fragment_user_manager, null);
        mUsersRv = mTempView.findViewById(R.id.rv_users);
        mAddUserTv = mTempView.findViewById(R.id.tv_add);
        mSelectDeleteRl = mTempView.findViewById(R.id.rl_select_delete);
        mSelectCb = mTempView.findViewById(R.id.cb_selete_user);
        mDeleteTv = mTempView.findViewById(R.id.del_tv);
        mTipTv = mTempView.findViewById(R.id.tv_tips);
        return mTempView;
    }

    public void initDate() {
        mDefaultDevice = DeviceInfoDao.getInstance(mTempView.getContext()).queryFirstData("device_default", true);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mDefaultUser = DeviceUserDao.getInstance(mTempView.getContext()).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId());
        mBleManagerHelper = BleManagerHelper.getInstance(mTempView.getContext(), false);
        DeviceUserDao.getInstance(mTempView.getContext()).registerObserver(mOb);
        mTempAdapter = new TempAdapter(mTempView.getContext());
        mLinerLayoutManager = new LinearLayoutManager(mTempView.getContext(), LinearLayoutManager.VERTICAL, false);
        mUsersRv.setLayoutManager(mLinerLayoutManager);
        mUsersRv.setItemAnimator(new DefaultItemAnimator());
        mUsersRv.setAdapter(mTempAdapter);
        mUsersRv.addItemDecoration(new SpacesItemDecoration(getResources().getDimensionPixelSize(R.dimen.y5dp)));
        LocalBroadcastManager.getInstance(mTempView.getContext()).registerReceiver(userReciver, intentFilter());

        mAddUserTv.setText(R.string.create_user);
        mSelectDeleteRl.setVisibility(View.GONE);

        mLoadDialog = DialogUtils.createLoadingDialog(mTempView.getContext(), getResources().getString(R.string.data_loading));

        initEvent();
    }

    private void initEvent() {
        mAddUserTv.setOnClickListener(this);
        mSelectCb.setOnClickListener(this);
        mDeleteTv.setOnClickListener(this);
        mSelectCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ArrayList<DeviceUser> deleteUsers = DeviceUserDao.getInstance(mTempView.getContext()).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_TEMP);
                mTempAdapter.mDeleteUsers.clear();
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
                    mTempAdapter.mDeleteUsers.addAll(deleteUsers);
                    mTempAdapter.chioseALLDelete(true);
                } else {
                    mTipTv.setText(R.string.all_election);
                    mTempAdapter.chioseALLDelete(false);
                }
                mTempAdapter.notifyDataSetChanged();
            }
        });
    }

    private static IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.EXTRA_DATA_MSG_12);
        intentFilter.addAction(BleMsg.STR_RSP_MSG1E_ERRCODE);
        intentFilter.addAction(BleMsg.STR_RSP_SET_TIMEOUT);
        intentFilter.addAction(BleMsg.STR_RSP_OPEN_TEST);
        return intentFilter;
    }

    private final BroadcastReceiver userReciver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // 4.2.3 MSG 12
            if (action.equals(BleMsg.EXTRA_DATA_MSG_12)) {
                DeviceUser user = (DeviceUser) intent.getSerializableExtra(BleMsg.KEY_SERIALIZABLE);
                if (user == null || user.getUserPermission() != ConstantUtil.DEVICE_TEMP) {
                    mHandler.removeCallbacks(mRunnable);
                    DialogUtils.closeDialog(mLoadDialog);
                    return;
                }

                byte[] buf = new byte[64];
                byte[] authBuf = new byte[64];
                authBuf[0] = 0x03;
                System.arraycopy(intent.getByteArrayExtra(BleMsg.KEY_USER_ID), 0, authBuf, 1, 2);
                System.arraycopy(intent.getByteArrayExtra(BleMsg.KEY_NODE_ID), 0, authBuf, 3, 8);
                System.arraycopy(intent.getByteArrayExtra(BleMsg.KEY_BLE_MAC), 0, authBuf, 11, 6);
                System.arraycopy(intent.getByteArrayExtra(BleMsg.KEY_RAND_CODE), 0, authBuf, 17, 10);

                byte[] timeBuf = new byte[4];
                StringUtil.int2Bytes((int) (System.currentTimeMillis() / 1000 + 30 * 60), timeBuf);
                System.arraycopy(timeBuf, 0, authBuf, 27, 4);

                Arrays.fill(authBuf, 31, 32, (byte) 0x01);

                String userId = StringUtil.bytesToHexString(intent.getByteArrayExtra(BleMsg.KEY_USER_ID));

                try {
                    AES_ECB_PKCS7.AES256Encode(authBuf, buf, MessageCreator.mQrSecret);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                LogUtil.d(TAG, "buf = " + Arrays.toString(buf));

//                String path = createQRcodeImage(buf);
                Log.d(TAG, "path = " + null);

                DeviceUser deviceUser;
                deviceUser = createDeviceUser(Short.parseShort(userId, 16), null, ConstantUtil.DEVICE_TEMP);

                if (deviceUser != null) {
                    mTempAdapter.addItem(deviceUser);
                    mHandler.removeCallbacks(mRunnable);
                    DialogUtils.closeDialog(mLoadDialog);
                }
            }

            if (action.equals(BleMsg.STR_RSP_MSG1E_ERRCODE)) {
                DeviceUser user = (DeviceUser) intent.getSerializableExtra(BleMsg.KEY_SERIALIZABLE);
                if (user != null) {
                    DeviceUser delUser = DeviceUserDao.getInstance(mTempView.getContext()).queryUser(mNodeId, user.getUserId());
                    if (delUser == null || delUser.getUserPermission() != ConstantUtil.DEVICE_TEMP) {
                        mHandler.removeCallbacks(mRunnable);
                        DialogUtils.closeDialog(mLoadDialog);
                        return;
                    }
                } else return;

                final byte[] errCode = intent.getByteArrayExtra(BleMsg.KEY_ERROR_CODE);

                Log.d(TAG, "errCode[3] = " + errCode[3]);

                if (errCode[3] == 0x2) {
                    showMessage(mTempView.getContext().getString(R.string.add_user_success));
                } else if (errCode[3] == 0x3) {
                    showMessage(mTempView.getContext().getString(R.string.add_user_failed));
                } else if (errCode[3] == 0x4) {
                    showMessage(mTempView.getContext().getString(R.string.delete_user_success));

                    DeviceUser deleteUser = DeviceUserDao.getInstance(mTempView.getContext()).queryUser(mNodeId, user.getUserId());
                    Log.d(TAG, "deleteUser = " + deleteUser.toString());
                    mTempAdapter.removeItem(deleteUser);

                    if (mTempAdapter.mDeleteUsers.size() == 0) {
                        mHandler.removeCallbacks(mRunnable);
                        DialogUtils.closeDialog(mLoadDialog);
                    } else return;

                } else if (errCode[3] == 0x05) {
                    showMessage(mTempView.getContext().getString(R.string.delete_user_failed));
                } else if (errCode[3] == 0x00) {
                    showMessage(mTempView.getContext().getString(R.string.pause_user_success));

                    DeviceUser pauseUser = DeviceUserDao.getInstance(mTempView.getContext()).queryUser(mNodeId, user.getUserId());
                    pauseUser.setUserStatus(ConstantUtil.USER_PAUSE);
                    DeviceUserDao.getInstance(mTempView.getContext()).updateDeviceUser(pauseUser);
                    mTempAdapter.changeUserState(pauseUser, ConstantUtil.USER_PAUSE);

                } else if (errCode[3] == 0x01) {
                    showMessage(mTempView.getContext().getString(R.string.pause_user_failed));
                } else if (errCode[3] == 0x06) {
                    showMessage(mTempView.getContext().getString(R.string.recovery_user_success));

                    DeviceUser pauseUser = DeviceUserDao.getInstance(mTempView.getContext()).queryUser(mNodeId, user.getUserId());
                    pauseUser.setUserStatus(ConstantUtil.USER_ENABLE);
                    DeviceUserDao.getInstance(mTempView.getContext()).updateDeviceUser(pauseUser);
                    mTempAdapter.changeUserState(pauseUser, ConstantUtil.USER_ENABLE);
                } else if (errCode[3] == 0x07) {
                    showMessage(mTempView.getContext().getString(R.string.recovery_user_failed));
                }

                mHandler.removeCallbacks(mRunnable);
                DialogUtils.closeDialog(mLoadDialog);
            }

        }
    };

    private class TempAdapter extends RecyclerView.Adapter<TempAdapter.TempViewHoler> {
        private Context mContext;
        private ArrayList<DeviceUser> mUserList;
        private Boolean mVisiBle = false;
        public ArrayList<DeviceUser> mDeleteUsers = new ArrayList<>();
        public boolean mAllDelete = false;
        private SwipeLayout mSwipelayout;

        public TempAdapter(Context context) {
            mContext = context;
            mUserList = DeviceUserDao.getInstance(mContext).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_TEMP);
            int index = -1;
            for (DeviceUser user : mUserList) {
                if (user.getUserId() == mDefaultUser.getUserId()) {
                    index = mUserList.indexOf(user);
                }
            }
            if (index != -1) {
                mUserList.remove(index);
            }
        }

        @NonNull
        @Override
        public TempViewHoler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_user, parent, false);
            mSwipelayout = inflate.findViewById(R.id.item_ll_user);
            mSwipelayout.setClickToClose(true);
            mSwipelayout.setRightSwipeEnabled(true);
            return new TempViewHoler(inflate);
        }

        public void setDataSource() {
            mUserList = DeviceUserDao.getInstance(mContext).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_TEMP);
            int index = -1;
            for (DeviceUser user : mUserList) {
                if (user.getUserId() == mDefaultUser.getUserId()) {
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
                if (user.getUserId() == changeUser.getUserId()) {
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
                if (user.getUserId() == delUser.getUserId()) {
                    index = mUserList.indexOf(user);
                }
            }
            if (index != -1) {
                DeviceUser del = mUserList.remove(index);

                boolean result = mDeleteUsers.remove(del);
                Log.d(TAG, "result = " + result);
                DeviceUserDao.getInstance(mTempView.getContext()).delete(del);
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

        @Override
        public void onBindViewHolder(@NonNull final TempViewHoler holder, final int position) {
            final DeviceUser userInfo = mUserList.get(position);
            if (userInfo != null) {
                holder.mNameTv.setText(userInfo.getUserName());
                if (userInfo.getUserStatus() == ConstantUtil.USER_UNENABLE) {
                    holder.mUserStateTv.setText(mTempView.getContext().getResources().getString(R.string.unenable));
                    mSwipelayout.setRightSwipeEnabled(false);
                } else if (userInfo.getUserStatus() == ConstantUtil.USER_ENABLE) {
                    holder.mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.blue_enable));
                    holder.mUserStateTv.setText(mTempView.getContext().getResources().getString(R.string.normal));
                    holder.mUserPause.setVisibility(View.VISIBLE);
                    holder.mUserRecovery.setVisibility(View.GONE);
                    mSwipelayout.setRightSwipeEnabled(true);
                } else if (userInfo.getUserStatus() == ConstantUtil.USER_PAUSE) {
                    holder.mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.yallow_pause));
                    holder.mUserStateTv.setText(mTempView.getContext().getResources().getString(R.string.pause));
                    holder.mUserPause.setVisibility(View.GONE);
                    holder.mUserRecovery.setVisibility(View.VISIBLE);
                    mSwipelayout.setRightSwipeEnabled(true);
                } else {
                    holder.mUserStateTv.setText(mTempView.getContext().getResources().getString(R.string.invalid));
                    mSwipelayout.setRightSwipeEnabled(false);
                }
                holder.mUserNumberTv.setText(String.valueOf(userInfo.getUserId()));

                final AlertDialog editDialog = DialogUtils.showEditDialog(mContext, mContext.getString(R.string.modify_note_name), userInfo);
                holder.mEditIbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editDialog.show();
                    }
                });

                if (editDialog != null) {
                    editDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            holder.mNameTv.setText(DeviceUserDao.getInstance(mContext).queryUser(userInfo.getDevNodeId(), userInfo.getUserId()).getUserName());
                        }
                    });
                }

                holder.mUserPause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog.show();
                        closeDialog(15);
                        if (mBleManagerHelper.getServiceConnection()) {
                            mBleManagerHelper.getBleCardService().sendCmd11((byte) 5, userInfo.getUserId(),BleMsg.INT_DEFAULT_TIMEOUT);
                        }
                    }
                });

                holder.mUserRecovery.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog.show();
                        closeDialog(15);
                        if (mBleManagerHelper.getServiceConnection()) {
                            mBleManagerHelper.getBleCardService().sendCmd11((byte) 6, userInfo.getUserId(),BleMsg.INT_DEFAULT_TIMEOUT);
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
                    }
                });

                holder.mUserContent.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        LogUtil.d(TAG, "userInfo = " + userInfo.toString());
                        Bundle bundle = new Bundle();
                        bundle.putSerializable(BleMsg.KEY_TEMP_USER, userInfo);
                        startIntent(TempUserActivity.class, bundle);
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

        public class TempViewHoler extends RecyclerView.ViewHolder {
            RelativeLayout mDeleteRl;
            TextView mUserStateTv;
            ImageButton mEditIbtn;
            SwipeLayout mSwipeLayout;
            TextView mNameTv;
            TextView mUserNumberTv;
            CheckBox mDeleteCb;
            LinearLayout mUserRecovery;
            LinearLayout mUserPause;
            LinearLayout mUserContent;

            public TempViewHoler(View itemView) {
                super(itemView);
                mNameTv = itemView.findViewById(R.id.tv_username);
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
        DeviceUserDao.getInstance(mTempView.getContext()).unregisterObserver(mOb);
        try {
            LocalBroadcastManager.getInstance(mTempView.getContext()).unregisterReceiver(userReciver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
    }
}
