package com.smart.lock.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
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
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.db.dao.UserProfileDao;
import com.smart.lock.entity.Device;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.SpacesItemDecoration;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class MemberFragment extends BaseFragment implements View.OnClickListener, UiListener {
    private final static String TAG = MemberFragment.class.getSimpleName();

    private View mMemberView;
    private RecyclerView mUsersRv;
    private MemberAdapter mMemberAdapter;
    private TextView mAddUserTv;
    private RelativeLayout mSelectDeleteRl;
    private CheckBox mSelectCb;
    private TextView mTipTv;
    private TextView mDeleteTv;
    private Context mCtx;
    private Device mDevice;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_add:
                ArrayList<DeviceUser> users = DeviceUserDao.getInstance(mCtx).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_MEMBER);
                LogUtil.d(TAG, "users size : " + users.size());
                if (users.size() >= 90) {
                    showMessage(mCtx.getResources().getString(R.string.members) + mCtx.getResources().getString(R.string.add_user_tips));
                    return;
                }

                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_CONNECT_ADD_MUMBER, (short) 0, BleMsg.INT_DEFAULT_TIMEOUT);
                } else showMessage(getString(R.string.disconnect_ble));
                break;
//            case R.id.cb_selete_user:
//                LogUtil.d(TAG, "choise user delete : " + mSelectBtn.getText().toString());
//
//                if ((int) mSelectBtn.getTag() == R.string.all_election) {
//
//                    mSelectBtn.setText(R.string.cancel);
//                    mSelectBtn.setTag(R.string.cancel);
//                } else if ((int) mSelectBtn.getTag() == R.string.cancel) {
//                    mMemberAdapter.mDeleteUsers.clear();
//                    mSelectBtn.setText(R.string.all_election);
//                    mMemberAdapter.chioseALLDelete(false);
//                    mSelectBtn.setTag(R.string.all_election);
//                }
//                mMemberAdapter.notifyDataSetChanged();
//                break;
            case R.id.del_tv:
                if (mMemberAdapter.mDeleteUsers.size() != 0) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    for (DeviceUser devUser : mMemberAdapter.mDeleteUsers) {
                        LogUtil.d(TAG, "devUser = " + devUser.getUserId());
                        mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_DELETE_USER, devUser.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                    }
                } else {
                    showMessage(getString(R.string.plz_choise_del_user));
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
        mMemberAdapter.chioseALLDelete(false);
        mMemberAdapter.chioseItemDelete(choise);
        mMemberAdapter.notifyDataSetChanged();
    }


    public void refreshView() {
        mMemberAdapter.setDataSource();
        mMemberAdapter.notifyDataSetChanged();
    }

    @Override
    public View initView() {
        mMemberView = View.inflate(mActivity, R.layout.fragment_user_manager, null);
        mUsersRv = mMemberView.findViewById(R.id.rv_users);
        mAddUserTv = mMemberView.findViewById(R.id.tv_add);
        mSelectDeleteRl = mMemberView.findViewById(R.id.rl_select_delete);
        mSelectCb = mMemberView.findViewById(R.id.cb_selete_user);
        mDeleteTv = mMemberView.findViewById(R.id.del_tv);
        mTipTv = mMemberView.findViewById(R.id.tv_tips);
        return mMemberView;
    }

    public void initDate() {
        mCtx = mMemberView.getContext();
        mDefaultDevice = DeviceInfoDao.getInstance(mCtx).queryFirstData("device_default", true);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mDefaultUser = DeviceUserDao.getInstance(mCtx).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId());
        mBleManagerHelper = BleManagerHelper.getInstance(mCtx);
        mBleManagerHelper.addUiListener(this);
        mDevice = Device.getInstance(mCtx);
        mMemberAdapter = new MemberAdapter(mCtx);
        LinearLayoutManager linerLayoutManager = new LinearLayoutManager(mCtx, LinearLayoutManager.VERTICAL, false);
        mUsersRv.setLayoutManager(linerLayoutManager);
        mUsersRv.setItemAnimator(new DefaultItemAnimator());
        mUsersRv.setAdapter(mMemberAdapter);
        mUsersRv.addItemDecoration(new SpacesItemDecoration(getResources().getDimensionPixelSize(R.dimen.y5dp)));

        mAddUserTv.setText(R.string.create_user);
        mSelectDeleteRl.setVisibility(View.GONE);
        mLoadDialog = DialogUtils.createLoadingDialog(mCtx, getResources().getString(R.string.data_loading));
        initEvent();
    }

    private void initEvent() {
        mAddUserTv.setOnClickListener(this);
        mSelectCb.setOnClickListener(this);
        mDeleteTv.setOnClickListener(this);
        mSelectCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ArrayList<DeviceUser> deleteUsers = DeviceUserDao.getInstance(mCtx).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_MEMBER);
                mMemberAdapter.mDeleteUsers.clear();
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
                    mMemberAdapter.mDeleteUsers.addAll(deleteUsers);
                    mMemberAdapter.chioseALLDelete(true);
                } else {
                    mTipTv.setText(R.string.all_election);
                    mMemberAdapter.chioseALLDelete(false);
                }
                mMemberAdapter.notifyDataSetChanged();
            }
        });

    }

    @Override
    public void deviceStateChange(Device device, int state) {
        mDevice = device;
        LogUtil.i(TAG, "deviceStateChange : state is " + state);
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
        if (serializable != null && !(serializable instanceof DeviceUser || serializable instanceof Short)) {
            return;
        }
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_1E:
                DeviceUser user = (DeviceUser) serializable;
                if (user != null) {
                    DeviceUser delUser = DeviceUserDao.getInstance(mCtx).queryUser(mNodeId, user.getUserId());
                    if (delUser == null || delUser.getUserPermission() != ConstantUtil.DEVICE_MEMBER) {
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
                if (addUser == null || addUser.getUserPermission() != ConstantUtil.DEVICE_MEMBER) {
                    DialogUtils.closeDialog(mLoadDialog);
                    return;
                }
                byte[] buf = new byte[64];
                byte[] authBuf = new byte[64];
                authBuf[0] = 0x02;

                byte[] authCode = extra.getByteArray(BleMsg.KEY_AUTH_CODE);
                if (authCode == null) return;

                byte[] userIdBuf = new byte[2];

                System.arraycopy(authCode, 0, userIdBuf, 0, 2);

                Short userId = Short.parseShort(StringUtil.bytesToHexString(userIdBuf), 16);

                byte[] timeQr = new byte[4];
                StringUtil.int2Bytes((int) (System.currentTimeMillis() / 1000 + 30 * 60), timeQr);
                System.arraycopy(timeQr, 0, authBuf, 1, 4); //二维码有效时间

                System.arraycopy(authCode, 0, authBuf, 5, 30); //鉴权码

                Arrays.fill(authBuf, 35, 64, (byte) 0x1d); //补充字节

                try {
                    AES_ECB_PKCS7.AES256Encode(authBuf, buf, MessageCreator.mQrSecret);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String path = createQRcodeImage(buf, ConstantUtil.DEVICE_MEMBER);
                Log.d(TAG, "path = " + path);
                if (path != null) {
                    DeviceUser deviceUser = createDeviceUser(userId, path, StringUtil.bytesToHexString(authCode));

                    if (deviceUser != null) {
                        mMemberAdapter.addItem(deviceUser);
                        DialogUtils.closeDialog(mLoadDialog);
                    }
                }

                break;
            case Message.TYPE_BLE_RECEIVER_CMD_26:
                short userIdTag = (short) serializable;
                if (userIdTag <= 100 || userIdTag > 200) {
                    DialogUtils.closeDialog(mLoadDialog);
                    return;
                }

                byte[] userInfo = extra.getByteArray(BleMsg.KEY_USER_MSG);
                if (userInfo != null) {
                    byte[] authTime = new byte[4];
                    System.arraycopy(userInfo, 8, authTime, 0, 4);

                    DeviceUser devUser = DeviceUserDao.getInstance(mCtx).queryUser(mDefaultDevice.getDeviceNodeId(), userIdTag);
                    setAuthCode(authTime, mDefaultDevice, devUser);

                    String qrPath = devUser.getQrPath();
                    if (StringUtil.checkNotNull(qrPath)) {
                        String qrName = StringUtil.getFileName(qrPath);
                        if (System.currentTimeMillis() - Long.parseLong(qrName) <= 30 * 60 * 60) {
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
                showMessage(mCtx.getString(R.string.add_user_success));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_ADD_USER_FAILED:
                showMessage(mCtx.getString(R.string.add_user_failed));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_DELETE_USER_SUCCESS:
                DeviceUser deleteUser = DeviceUserDao.getInstance(mCtx).queryUser(mNodeId, user.getUserId());
                Log.d(TAG, "deleteUser : " + deleteUser.toString());
                DeviceKeyDao.getInstance(mCtx).deleteUserKey(deleteUser.getUserId(), deleteUser.getDevNodeId()); //删除开锁信息
                mMemberAdapter.removeItem(deleteUser);
                if (mMemberAdapter.mDeleteUsers.size() == 0) {
                    showMessage(mCtx.getString(R.string.delete_user_success));
                    DialogUtils.closeDialog(mLoadDialog);
                } else return;
                break;
            case BleMsg.TYPE_DELETE_USER_FAILED:
                showMessage(mCtx.getString(R.string.delete_user_failed));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_PAUSE_USER_SUCCESS:
                showMessage(mCtx.getString(R.string.pause_user_success));
                DeviceUser pauseUser = DeviceUserDao.getInstance(mCtx).queryUser(mNodeId, user.getUserId());
                pauseUser.setUserStatus(ConstantUtil.USER_PAUSE);
                DeviceUserDao.getInstance(mCtx).updateDeviceUser(pauseUser);
                mMemberAdapter.changeUserState(pauseUser, ConstantUtil.USER_PAUSE);
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_PAUSE_USER_FAILED:
                showMessage(mCtx.getString(R.string.pause_user_failed));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_RECOVERY_USER_SUCCESS:
                showMessage(mCtx.getString(R.string.recovery_user_success));

                DeviceUser recoveryUser = DeviceUserDao.getInstance(mCtx).queryUser(mNodeId, user.getUserId());
                recoveryUser.setUserStatus(ConstantUtil.USER_ENABLE);
                DeviceUserDao.getInstance(mCtx).updateDeviceUser(recoveryUser);
                mMemberAdapter.changeUserState(recoveryUser, ConstantUtil.USER_ENABLE);
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_RECOVERY_USER_FAILED:
                showMessage(mCtx.getString(R.string.recovery_user_failed));
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

    private class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHoler> {
        private Context mContext;
        private ArrayList<DeviceUser> mUserList;
        private Boolean mVisiBle = false;
        public ArrayList<DeviceUser> mDeleteUsers = new ArrayList<>();
        public boolean mAllDelete = false;
        private SwipeLayout mSwipelayout;

        public MemberAdapter(Context context) {
            mContext = context;
            mUserList = DeviceUserDao.getInstance(mContext).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_MEMBER);
            LogUtil.d(TAG, "muserlist size : " + mUserList.size());
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
        public MemberViewHoler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_user, parent, false);
            mSwipelayout = inflate.findViewById(R.id.item_ll_user);
            mSwipelayout.setClickToClose(true);
            mSwipelayout.setRightSwipeEnabled(true);
            return new MemberViewHoler(inflate);
        }

        public void setDataSource() {
            mUserList = DeviceUserDao.getInstance(mContext).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_MEMBER);
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

                boolean result = mDeleteUsers.remove(del);
                Log.d(TAG, "result = " + result);
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

        @Override
        public void onBindViewHolder(final MemberViewHoler holder, final int position) {
            final DeviceUser userInfo = mUserList.get(position);
            if (userInfo != null) {
                holder.mNameTv.setText(userInfo.getUserName());
                if (userInfo.getUserStatus() == ConstantUtil.USER_UNENABLE) {
                    holder.mUserStateTv.setText(mCtx.getResources().getString(R.string.unenable));
                    mSwipelayout.setRightSwipeEnabled(false);
                    holder.mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.red));
                } else if (userInfo.getUserStatus() == ConstantUtil.USER_ENABLE) {
                    mSwipelayout.setRightSwipeEnabled(true);
                    holder.mUserStateTv.setText(mCtx.getResources().getString(R.string.normal));
                    holder.mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.blue_enable));
                    holder.mUserPause.setVisibility(View.VISIBLE);
                    holder.mUserRecovery.setVisibility(View.GONE);
                } else if (userInfo.getUserStatus() == ConstantUtil.USER_PAUSE) {
                    holder.mUserStateTv.setText(mCtx.getResources().getString(R.string.pause));
                    holder.mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.yallow_pause));
                    holder.mUserPause.setVisibility(View.GONE);
                    holder.mUserRecovery.setVisibility(View.VISIBLE);
                    mSwipelayout.setRightSwipeEnabled(true);
                }

                holder.mUserNumberTv.setText(String.valueOf(userInfo.getUserId()));

                final Dialog mEditorNameDialog = DialogUtils.createEditorDialog(mActivity, getString(R.string.modify_note_name), holder.mNameTv.getText().toString());
                //修改呢称响应事件
                mEditorNameDialog.findViewById(R.id.dialog_confirm_btn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String newName = ((EditText) mEditorNameDialog.findViewById(R.id.editor_et)).getText().toString();
                        if (!newName.isEmpty()) {
                            holder.mNameTv.setText(newName);
                            userInfo.setUserName(newName);
                            DeviceUserDao.getInstance(mContext).updateDeviceUser(userInfo);
                        } else {
                            ToastUtil.showLong(mActivity, R.string.cannot_be_empty_str);
                        }
                        mEditorNameDialog.dismiss();
                    }
                });

                holder.mEditIbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((EditText) mEditorNameDialog.findViewById(R.id.editor_et)).setText(holder.mNameTv.getText().toString());
                        mEditorNameDialog.show();
                    }
                });


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

                holder.itemView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (mDevice != null && mDevice.getState() == Device.BLE_CONNECTED) {
                            mBleManagerHelper.getBleCardService().sendCmd25(userInfo.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                        } else showMessage(mContext.getString(R.string.unconnected_device));

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

        public class MemberViewHoler extends RecyclerView.ViewHolder {
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

            public MemberViewHoler(View itemView) {
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
