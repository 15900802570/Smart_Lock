package com.smart.lock.ui.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.smart.lock.R;
import com.smart.lock.ble.AES_ECB_PKCS7;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.entity.Device;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.widget.SpacesItemDecoration;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class AdminFragment extends BaseFragment implements View.OnClickListener, UiListener {
    private final static String TAG = AdminFragment.class.getSimpleName();

    private View mAdminView;
    private RecyclerView mUsersRv;
    private AdminAdapter mAdminAdapter;
    private LinearLayoutManager mLinerLayoutManager;
    private TextView mAddUserTv;
    private RelativeLayout mSelectDeleteRl;
    private CheckBox mSelectCb;
    private TextView mTipTv;
    private TextView mDeleteTv;
    private Device mDevice;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_add:
                ArrayList<DeviceUser> users = DeviceUserDao.getInstance(mAdminView.getContext()).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_MASTER);
                if (users != null && users.size() >= 5) {
                    showMessage(mAdminView.getContext().getResources().getString(R.string.administrator) + mAdminView.getContext().getResources().getString(R.string.add_user_tips));
                    return;
                }

                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_CONNECT_ADD_MASTER, (short) 0, BleMsg.INT_DEFAULT_TIMEOUT);
                } else showMessage(getString(R.string.disconnect_ble));
                break;
            case R.id.del_tv:
                if (mAdminAdapter.mDeleteUsers.size() != 0) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    for (DeviceUser devUser : mAdminAdapter.mDeleteUsers) {
                        mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_DELETE_USER, devUser.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                    }
                } else {
                    showMessage(getString(R.string.plz_choise_del_user));
                }

                if (mActivity instanceof MemberFragment.OnFragmentInteractionListener) {
                    ((MemberFragment.OnFragmentInteractionListener) mActivity).changeVisible();
                }

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
        } else {
            mSelectDeleteRl.setVisibility(View.GONE);
        }
        mSelectCb.setChecked(false);
        mAdminAdapter.chioseALLDelete(false);
        mAdminAdapter.chioseItemDelete(choise);
        mAdminAdapter.notifyDataSetChanged();
    }

    public void refreshView() {
        mAdminAdapter.setDataSource();
        mAdminAdapter.notifyDataSetChanged();
    }


    @Override
    public View initView() {
        mAdminView = View.inflate(mActivity, R.layout.fragment_user_manager, null);
        mUsersRv = mAdminView.findViewById(R.id.rv_users);
        mAddUserTv = mAdminView.findViewById(R.id.tv_add);
        mSelectDeleteRl = mAdminView.findViewById(R.id.rl_select_delete);
        mSelectCb = mAdminView.findViewById(R.id.cb_selete_user);
        mDeleteTv = mAdminView.findViewById(R.id.del_tv);
        mTipTv = mAdminView.findViewById(R.id.tv_tips);
        return mAdminView;
    }

    public void initDate() {
        mDefaultDevice = DeviceInfoDao.getInstance(mAdminView.getContext()).queryFirstData("device_default", true);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mDefaultUser = DeviceUserDao.getInstance(mActivity).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId());
        mBleManagerHelper = BleManagerHelper.getInstance(mAdminView.getContext(), false);
        mBleManagerHelper.addUiListener(this);
        mDevice = mBleManagerHelper.getBleCardService().getDevice();
        mAdminAdapter = new AdminAdapter(mAdminView.getContext());
        LinearLayoutManager mLinerLayoutManager = new LinearLayoutManager(mAdminView.getContext(), LinearLayoutManager.VERTICAL, false);
        mUsersRv.setLayoutManager(mLinerLayoutManager);
        mUsersRv.setItemAnimator(new DefaultItemAnimator());
        mUsersRv.setAdapter(mAdminAdapter);
        mUsersRv.addItemDecoration(new SpacesItemDecoration(getResources().getDimensionPixelSize(R.dimen.y5dp)));

        mAddUserTv.setText(R.string.create_user);
        mSelectDeleteRl.setVisibility(View.GONE);

        mLoadDialog = DialogUtils.createLoadingDialog(mAdminView.getContext(), getResources().getString(R.string.data_loading));

        initEvent();
    }

    private void initEvent() {
        mAddUserTv.setOnClickListener(this);
        mSelectCb.setOnClickListener(this);
        mDeleteTv.setOnClickListener(this);
        mSelectCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ArrayList<DeviceUser> deleteUsers = DeviceUserDao.getInstance(mAdminView.getContext()).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_MASTER);
                mAdminAdapter.mDeleteUsers.clear();
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
                    mAdminAdapter.mDeleteUsers.addAll(deleteUsers);
                    mAdminAdapter.chioseALLDelete(true);
                } else {
                    mTipTv.setText(R.string.all_election);
                    mAdminAdapter.chioseALLDelete(false);
                }
                mAdminAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void deviceStateChange(Device device, int state) {
        LogUtil.i(TAG, "deviceStateChange : state is " + state);
        switch (state) {
            case BleMsg.STATE_DISCONNECTED:
                DialogUtils.closeDialog(mLoadDialog);
                showMessage(mAdminView.getContext().getString(R.string.ble_disconnect));
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
        LogUtil.i(TAG, "dispatchUiCallback : " + msg.getType());
        mDevice = device;
        Bundle extra = msg.getData();
        Serializable serializable = extra.getSerializable(BleMsg.KEY_SERIALIZABLE);
        if (serializable != null && !(serializable instanceof DeviceUser)) {
            return;
        }
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_1E:
                DeviceUser user = (DeviceUser) serializable;
                if (user != null) {
                    DeviceUser delUser = DeviceUserDao.getInstance(mAdminView.getContext()).queryUser(mNodeId, user.getUserId());
                    if (delUser == null || delUser.getUserPermission() != ConstantUtil.DEVICE_MASTER) {
                        DialogUtils.closeDialog(mLoadDialog);
                        return;
                    }
                } else return;
                final byte[] errCode = extra.getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode != null)
                    dispatchErrorCode(errCode[3], user);
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_12:
                DeviceUser addUser = (DeviceUser) serializable;
                if (addUser == null || addUser.getUserPermission() != ConstantUtil.DEVICE_MASTER) {
                    DialogUtils.closeDialog(mLoadDialog);
                    return;
                }

                byte[] buf = new byte[64];
                byte[] authBuf = new byte[64];
                authBuf[0] = 0x01;
                byte[] nodeIdBuf = extra.getByteArray(BleMsg.KEY_NODE_ID);
                StringUtil.exchange(nodeIdBuf);
                System.arraycopy(extra.getByteArray(BleMsg.KEY_USER_ID), 0, authBuf, 1, 2);
                System.arraycopy(nodeIdBuf, 0, authBuf, 3, 8);
                System.arraycopy(extra.getByteArray(BleMsg.KEY_BLE_MAC), 0, authBuf, 11, 6);
                System.arraycopy(extra.getByteArray(BleMsg.KEY_RAND_CODE), 0, authBuf, 17, 10);

                byte[] timeBuf = new byte[4];
                StringUtil.int2Bytes((int) (System.currentTimeMillis() / 1000 + 30 * 60), timeBuf);
                System.arraycopy(timeBuf, 0, authBuf, 27, 4);

                Arrays.fill(authBuf, 31, 32, (byte) 0x01);

                String userId = StringUtil.bytesToHexString(extra.getByteArray(BleMsg.KEY_USER_ID));
                try {
                    AES_ECB_PKCS7.AES256Encode(authBuf, buf, MessageCreator.mQrSecret);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String path = createQRcodeImage(buf, ConstantUtil.DEVICE_MASTER);
                Log.d(TAG, "path = " + path);
                DeviceUser deviceUser;
                if (path != null) {
                    deviceUser = createDeviceUser(Short.parseShort(userId, 16), path, ConstantUtil.DEVICE_MASTER);

                    if (deviceUser != null) {
                        mAdminAdapter.addItem(deviceUser);
                        DialogUtils.closeDialog(mLoadDialog);
                    }
                }
                break;
            default:
                LogUtil.e(TAG, "Message type : " + msg.getType() + " can not be handler");
                break;

        }
    }

    private void dispatchErrorCode(byte errCode, DeviceUser user) {
        LogUtil.i(TAG, "errCode : " + errCode);

        switch (errCode) {
            case BleMsg.TYPE_ADD_USER_SUCCESS:
                showMessage(mAdminView.getContext().getString(R.string.add_user_success));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_ADD_USER_FAILED:
                showMessage(mAdminView.getContext().getString(R.string.add_user_failed));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_DELETE_USER_SUCCESS:
                showMessage(mAdminView.getContext().getString(R.string.delete_user_success));
                DeviceUser deleteUser = DeviceUserDao.getInstance(mAdminView.getContext()).queryUser(mNodeId, user.getUserId());
                DeviceKeyDao.getInstance(mAdminView.getContext()).deleteUserKey(deleteUser.getUserId(), deleteUser.getDevNodeId()); //删除开锁信息

                mAdminAdapter.removeItem(deleteUser);
                if (mAdminAdapter.mDeleteUsers.size() == 0) {
                    DialogUtils.closeDialog(mLoadDialog);
                } else return;
                break;
            case BleMsg.TYPE_DELETE_USER_FAILED:
                showMessage(mAdminView.getContext().getString(R.string.delete_user_failed));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_PAUSE_USER_SUCCESS:
                showMessage(mAdminView.getContext().getString(R.string.pause_user_success));
                DeviceUser pauseUser = DeviceUserDao.getInstance(mAdminView.getContext()).queryUser(mNodeId, user.getUserId());
                pauseUser.setUserStatus(ConstantUtil.USER_PAUSE);
                DeviceUserDao.getInstance(mAdminView.getContext()).updateDeviceUser(pauseUser);
                mAdminAdapter.changeUserState(pauseUser, ConstantUtil.USER_PAUSE);
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_PAUSE_USER_FAILED:
                showMessage(mAdminView.getContext().getString(R.string.pause_user_failed));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_RECOVERY_USER_SUCCESS:
                showMessage(mAdminView.getContext().getString(R.string.recovery_user_success));

                DeviceUser recoveryUser = DeviceUserDao.getInstance(mAdminView.getContext()).queryUser(mNodeId, user.getUserId());
                recoveryUser.setUserStatus(ConstantUtil.USER_ENABLE);
                DeviceUserDao.getInstance(mAdminView.getContext()).updateDeviceUser(recoveryUser);
                mAdminAdapter.changeUserState(recoveryUser, ConstantUtil.USER_ENABLE);
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_RECOVERY_USER_FAILED:
                showMessage(mAdminView.getContext().getString(R.string.recovery_user_failed));
                DialogUtils.closeDialog(mLoadDialog);
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
    public void scanDevFialed() {

    }

    private class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.AdminViewHolder> {
        private Context mContext;
        private ArrayList<DeviceUser> mUserList;
        private Boolean mVisiBle = false;
        public ArrayList<DeviceUser> mDeleteUsers = new ArrayList<>();
        public boolean mAllDelete = false;
        private SwipeLayout mSwipelayout;

        public AdminAdapter(Context context) {
            mContext = context;
            mUserList = DeviceUserDao.getInstance(mContext).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_MASTER);
            int index = -1;
            for (DeviceUser user : mUserList) {
                LogUtil.d(TAG, "user : " + user.getUserId());
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
        public AdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_user, parent, false);
            mSwipelayout = inflate.findViewById(R.id.item_ll_user);
            mSwipelayout.setClickToClose(true);
            mSwipelayout.setRightSwipeEnabled(true);
            return new AdminViewHolder(inflate);
        }

        public void setDataSource() {
            mUserList = DeviceUserDao.getInstance(mContext).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_MASTER);
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
                DeviceUserDao.getInstance(mAdminView.getContext()).delete(del);
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

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull final AdminViewHolder holder, final int position) {
            final DeviceUser userInfo = mUserList.get(position);
            if (userInfo != null) {
                holder.mNameTv.setText(userInfo.getUserName());
                if (userInfo.getUserStatus() == ConstantUtil.USER_UNENABLE) {
                    holder.mUserStateTv.setText(mContext.getString(R.string.unenable));
                    mSwipelayout.setRightSwipeEnabled(false);
                    holder.mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.red));
                } else if (userInfo.getUserStatus() == ConstantUtil.USER_ENABLE) {
                    holder.mUserStateTv.setText(mContext.getString(R.string.normal));
                    holder.mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.blue_enable));
                    holder.mUserPause.setVisibility(View.VISIBLE);
                    holder.mUserRecovery.setVisibility(View.GONE);
                    mSwipelayout.setRightSwipeEnabled(true);
                } else if (userInfo.getUserStatus() == ConstantUtil.USER_PAUSE) {
                    holder.mUserStateTv.setText(mContext.getString(R.string.pause));
                    holder.mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.yallow_pause));
                    holder.mUserPause.setVisibility(View.GONE);
                    holder.mUserRecovery.setVisibility(View.VISIBLE);
                    mSwipelayout.setRightSwipeEnabled(true);
                } else {
                    holder.mUserStateTv.setText(mAdminView.getContext().getResources().getString(R.string.invalid));
                    holder.mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.dark_gray));
                    mSwipelayout.setRightSwipeEnabled(false);
                }
                holder.mUserNumberTv.setText("00" + String.valueOf(userInfo.getUserId()));

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
                        if (mDevice.getState() == Device.BLE_CONNECTED) {
                            DialogUtils.closeDialog(mLoadDialog);
                            mLoadDialog.show();
                            mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_PAUSE_USER, userInfo.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                        } else showMessage(getString(R.string.disconnect_ble));
                    }
                });

                holder.mUserRecovery.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mDevice.getState() == Device.BLE_CONNECTED) {
                            DialogUtils.closeDialog(mLoadDialog);
                            mLoadDialog.show();
                            mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_RECOVERY_USER, userInfo.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                        } else showMessage(getString(R.string.disconnect_ble));
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
                        String path = userInfo.getQrPath();
                        Log.d(TAG, "path = " + path);
                        if (StringUtil.checkNotNull(path)) {
                            String qrName = StringUtil.getFileName(path);
                            if (System.currentTimeMillis() - Long.parseLong(qrName) <= 30 * 60 * 60) {
                                Log.d(TAG, "qrName = " + qrName);
                                displayImage(path);
                            } else {
                                File delQr = new File(path);
                                boolean result = delQr.delete();
                                if (result) {
                                    mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + path)));
                                }
                                String newPath = createQr(userInfo);
                                Log.d(TAG, "newPath = " + newPath);
                            }

                        } else {
                            String newPath = createQr(userInfo);
                            Log.d(TAG, "newPath = " + newPath);
                        }
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
        public void onViewAttachedToWindow(@NonNull AdminViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            holder.mNameTv.setEnabled(false);
            holder.mNameTv.setEnabled(true);
        }

        @Override
        public int getItemCount() {
            return mUserList.size();
        }

        class AdminViewHolder extends RecyclerView.ViewHolder {
            RelativeLayout mDeleteRl;
            TextView mUserStateTv;
            ImageButton mEditIbtn;
            SwipeLayout mSwipeLayout;
            TextView mNameTv;
            TextView mUserNumberTv;
            LinearLayout mUserRecovery;
            LinearLayout mUserPause;
            CheckBox mDeleteCb;
            LinearLayout mUserContent;

            AdminViewHolder(View itemView) {
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
        mBleManagerHelper.removeUiListener(this);
    }
}
