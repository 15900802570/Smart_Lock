package com.smart.lock.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
    private Boolean mIsHint = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_add:
                ArrayList<DeviceUser> users = DeviceUserDao.getInstance(mCtx).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_MEMBER);
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
//                    mMemberAdapter.choiceALLDelete(false);
//                    mSelectBtn.setTag(R.string.all_election);
//                }
//                mMemberAdapter.notifyDataSetChanged();
//                break;
            case R.id.del_tv:
                if (mMemberAdapter.mDeleteUsers.size() != 0) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    for (DeviceUser devUser : mMemberAdapter.mDeleteUsers) {
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
        mMemberAdapter.choiceALLDelete(false);
        mMemberAdapter.choiceItemDelete(choise);
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
                if (mLoadDialog != null && mLoadDialog.isShowing()) {
                    return;
                }
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
                    mMemberAdapter.choiceALLDelete(true);
                } else {
                    mTipTv.setText(R.string.all_election);
                    mMemberAdapter.choiceALLDelete(false);
                }
                mMemberAdapter.notifyDataSetChanged();
            }
        });

    }

    @Override
    public void deviceStateChange(Device device, int state) {
        mDevice = device;
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
    public void setUserVisibleHint(boolean isVisibleToUser) {
        mIsHint = isVisibleToUser;
        super.setUserVisibleHint(isVisibleToUser);
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
                break;

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
                    mMemberAdapter.removeItem(deleteUser);
                    if (mMemberAdapter.mDeleteUsers.size() == 0) {
                        showMessage(mCtx.getString(R.string.delete_user_success));
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
                    DeviceUserDao.getInstance(mCtx).updateDeviceUser(pauseUser);
                    mMemberAdapter.changeUserState(pauseUser, ConstantUtil.USER_PAUSE);
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
                    DeviceUserDao.getInstance(mCtx).updateDeviceUser(recoveryUser);
                    mMemberAdapter.changeUserState(recoveryUser, ConstantUtil.USER_ENABLE);
                    DialogUtils.closeDialog(mLoadDialog);
                }
                break;
            case BleMsg.TYPE_RECOVERY_USER_FAILED:
                showMessage(mCtx.getString(R.string.recovery_user_failed));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_USER_FULL:
                showMessage(mCtx.getString(R.string.add_user_full));
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

    private class MemberAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_HEAD = 0;
        private static final int TYPE_BODY = 1;
        private static final int TYPE_FOOT = 2;
        private int countHead = 0;
        private int countFoot = 2;
        private Context mContext;
        public ArrayList<DeviceUser> mUserList;
        private Boolean mVisiBle = false;
        public ArrayList<DeviceUser> mDeleteUsers = new ArrayList<>();
        public boolean mAllDelete = false;
//        private SwipeLayout mSwipelayout;

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

        public MemberAdapter(Context context) {
            mContext = context;
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
                    return new MemberViewHolder(inflate);
                case TYPE_FOOT:
                    return new FootViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_recycle_foot, parent, false));
                default:
                    return new FootViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_recycle_foot, parent, false));
            }
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

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
            if (holder instanceof MemberViewHolder) {
                final DeviceUser userInfo = mUserList.get(position - countHead);
                if (userInfo != null) {
                    ((MemberViewHolder) holder).mNameTv.setText(userInfo.getUserName());
                    if (userInfo.getUserStatus() == ConstantUtil.USER_UNENABLE) {
                        ((MemberViewHolder) holder).mUserStateTv.setText(mCtx.getResources().getString(R.string.unenable));
                        ((MemberViewHolder) holder).mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.red));
                        ((MemberViewHolder) holder).mSwipeLayout.setRightSwipeEnabled(false);
                    } else if (userInfo.getUserStatus() == ConstantUtil.USER_ENABLE) {
                        ((MemberViewHolder) holder).mUserStateTv.setText(mCtx.getResources().getString(R.string.normal));
                        ((MemberViewHolder) holder).mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.blue_enable));
                        ((MemberViewHolder) holder).mUserPause.setVisibility(View.VISIBLE);
                        ((MemberViewHolder) holder).mUserRecovery.setVisibility(View.GONE);
                        ((MemberViewHolder) holder).mSwipeLayout.setRightSwipeEnabled(true);
                    } else if (userInfo.getUserStatus() == ConstantUtil.USER_PAUSE) {
                        ((MemberViewHolder) holder).mUserStateTv.setText(mCtx.getResources().getString(R.string.pause));
                        ((MemberViewHolder) holder).mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.yallow_pause));
                        ((MemberViewHolder) holder).mUserPause.setVisibility(View.GONE);
                        ((MemberViewHolder) holder).mUserRecovery.setVisibility(View.VISIBLE);
                        ((MemberViewHolder) holder).mSwipeLayout.setRightSwipeEnabled(true);
                    }
                    ((MemberViewHolder) holder).mUserNumberTv.setText(String.valueOf(userInfo.getUserId()));

                    final Dialog mEditorNameDialog = DialogUtils.createEditorDialog(mActivity, getString(R.string.modify_note_name), ((MemberViewHolder) holder).mNameTv.getText().toString());
                    final EditText editText = mEditorNameDialog.findViewById(R.id.editor_et);
                    editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});

                    //修改呢称响应事件
                    mEditorNameDialog.findViewById(R.id.dialog_confirm_btn).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final String newName = editText.getText().toString();
                            if (!newName.isEmpty()) {
                                ((MemberViewHolder) holder).mNameTv.setText(newName);
                                userInfo.setUserName(newName);
                                DeviceUserDao.getInstance(mContext).updateDeviceUser(userInfo);
                            } else {
                                ToastUtil.showLong(mActivity, R.string.cannot_be_empty_str);
                            }
                            mEditorNameDialog.dismiss();
                        }
                    });

                    ((MemberViewHolder) holder).mEditIbtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            editText.setText(((MemberViewHolder) holder).mNameTv.getText().toString());
                            mEditorNameDialog.show();
                        }
                    });


                    ((MemberViewHolder) holder).mUserPause.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mDevice.getState() == Device.BLE_CONNECTED) {
                                DialogUtils.closeDialog(mLoadDialog);
                                mLoadDialog.show();
                                mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_PAUSE_USER, userInfo.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                            } else showMessage(getString(R.string.disconnect_ble));
                        }
                    });

                    ((MemberViewHolder) holder).mUserRecovery.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mDevice.getState() == Device.BLE_CONNECTED) {
                                DialogUtils.closeDialog(mLoadDialog);
                                mLoadDialog.show();
                                mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_RECOVERY_USER, userInfo.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                            } else showMessage(getString(R.string.disconnect_ble));
                        }
                    });


                    ((MemberViewHolder) holder).mDeleteCb.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (((MemberViewHolder) holder).mDeleteCb.isChecked()) {
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

                    ((MemberViewHolder) holder).mUserContent.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            if (mDevice != null && mDevice.getState() == Device.BLE_CONNECTED) {
                                mBleManagerHelper.getBleCardService().sendCmd25(userInfo.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                            } else showMessage(mContext.getString(R.string.unconnected_device));

                        }
                    });

                    if (mVisiBle)
                        ((MemberViewHolder) holder).mDeleteRl.setVisibility(View.VISIBLE);
                    else
                        ((MemberViewHolder) holder).mDeleteRl.setVisibility(View.GONE);
                    ((MemberViewHolder) holder).mDeleteCb.setChecked(mAllDelete);
                }
            }
        }


        @Override
        public int getItemCount() {
            return mUserList.size() + countHead + countFoot;
        }

        class MemberViewHolder extends RecyclerView.ViewHolder {
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

            public MemberViewHolder(View itemView) {
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

        class FootViewHolder extends RecyclerView.ViewHolder {
            private FootViewHolder(View itemView) {
                super(itemView);
            }
        }

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mBleManagerHelper.removeUiListener(this);
    }
}
