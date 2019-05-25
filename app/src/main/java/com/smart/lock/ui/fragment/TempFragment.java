package com.smart.lock.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
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
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.entity.Device;
import com.smart.lock.ui.TempUserActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.SpacesItemDecoration;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class TempFragment extends BaseFragment implements View.OnClickListener, UiListener {
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
                ArrayList<DeviceUser> users = DeviceUserDao.getInstance(mCtx).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_TEMP);
                if (users != null && users.size() >= 5) {
                    showMessage(mCtx.getResources().getString(R.string.tmp_user) + mCtx.getResources().getString(R.string.add_user_tips));
                    return;
                }

                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_CONNECT_ADD_TEMP, (short) 0, BleMsg.INT_DEFAULT_TIMEOUT);
                } else showMessage(getString(R.string.disconnect_ble));
                break;
            case R.id.del_tv:
                if (mTempAdapter.mDeleteUsers.size() != 0) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    for (DeviceUser devUser : mTempAdapter.mDeleteUsers) {
                        mBleManagerHelper.getBleCardService().sendCmd11((byte) 4, devUser.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                    }
                } else {
                    showMessage(getString(R.string.plz_choise_del_user));
                }
                if (mActivity instanceof TempFragment.OnFragmentInteractionListener) {
                    ((TempFragment.OnFragmentInteractionListener) mActivity).changeVisible();
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
        mTempAdapter.chioseALLDelete(false);
        mTempAdapter.chioseItemDelete(choise);
        mTempAdapter.notifyDataSetChanged();
    }

    public void refreshView() {
        mTempAdapter.setDataSource();
        mTempAdapter.notifyDataSetChanged();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        mIsHint = isVisibleToUser;
        LogUtil.d(TAG, "mIsHint : " + mIsHint);
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
        mCtx = mTempView.getContext();
        mDefaultDevice = DeviceInfoDao.getInstance(mCtx).queryFirstData("device_default", true);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mDefaultUser = DeviceUserDao.getInstance(mCtx).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId());
        mBleManagerHelper = BleManagerHelper.getInstance(mCtx);
        mBleManagerHelper.addUiListener(this);
        mDevice = Device.getInstance(mCtx);
//        DeviceUserDao.getInstance(mCtx).registerObserver(mOb);
        mTempAdapter = new TempAdapter(mCtx);
        mLinerLayoutManager = new LinearLayoutManager(mCtx, LinearLayoutManager.VERTICAL, false);
        mUsersRv.setLayoutManager(mLinerLayoutManager);
        mUsersRv.setItemAnimator(new DefaultItemAnimator());
        mUsersRv.setAdapter(mTempAdapter);
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
                ArrayList<DeviceUser> deleteUsers = DeviceUserDao.getInstance(mCtx).queryUsers(mDefaultDevice.getDeviceNodeId(), ConstantUtil.DEVICE_TEMP);
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
    public void dispatchUiCallback(Message msg, Device device, int type) {
        Bundle extra = msg.getData();
        mDevice = device;
        Serializable serializable = extra.getSerializable(BleMsg.KEY_SERIALIZABLE);
        if (!mIsHint || serializable == null || serializable instanceof DeviceKey) {
            DialogUtils.closeDialog(mLoadDialog);
            return;
        }
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_1E:
                DeviceUser user = (DeviceUser) serializable;
                final byte[] errCode = extra.getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode != null)
                    dispatchErrorCode(errCode[3], user);
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_12:
                byte[] buf = new byte[64];
                byte[] authBuf = new byte[64];
                authBuf[0] = 0x03;

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

                String path = createQRcodeImage(buf, ConstantUtil.DEVICE_TEMP);
                if (path != null) {
                    DeviceUser deviceUser = createDeviceUser(userId, path, StringUtil.bytesToHexString(authCode));
                    if (deviceUser != null) {
                        mTempAdapter.addItem(deviceUser);
                        DialogUtils.closeDialog(mLoadDialog);
                    }
                }

                break;

            default:
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
                DeviceKeyDao.getInstance(mCtx).deleteUserKey(deleteUser.getUserId(), deleteUser.getDevNodeId()); //删除开锁信息
                mTempAdapter.removeItem(deleteUser);
                if (mTempAdapter.mDeleteUsers.size() == 0) {
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
                mTempAdapter.changeUserState(pauseUser, ConstantUtil.USER_PAUSE);
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
                mTempAdapter.changeUserState(recoveryUser, ConstantUtil.USER_ENABLE);
                DialogUtils.closeDialog(mLoadDialog);
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

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull final TempViewHoler holder, final int position) {
            final DeviceUser userInfo = mUserList.get(position);
            if (userInfo != null) {
                holder.mNameTv.setText(userInfo.getUserName());
                if (userInfo.getUserStatus() == ConstantUtil.USER_UNENABLE) {
                    holder.mUserStateTv.setText(mCtx.getResources().getString(R.string.unenable));
                    mSwipelayout.setRightSwipeEnabled(false);
                    holder.mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.red));
                } else if (userInfo.getUserStatus() == ConstantUtil.USER_ENABLE) {
                    holder.mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.blue_enable));
                    holder.mUserStateTv.setText(mCtx.getResources().getString(R.string.normal));
                    holder.mUserPause.setVisibility(View.VISIBLE);
                    holder.mUserRecovery.setVisibility(View.GONE);
                    mSwipelayout.setRightSwipeEnabled(true);
                } else if (userInfo.getUserStatus() == ConstantUtil.USER_PAUSE) {
                    holder.mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.yallow_pause));
                    holder.mUserStateTv.setText(mCtx.getResources().getString(R.string.pause));
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
                            if (userInfo.getLcBegin() != null && userInfo.getLcEnd() != null) {
                                try {
                                    Date now = new Date(System.currentTimeMillis());
                                    Date begin = new Date(DateTimeUtil.dateToStampDay(userInfo.getLcBegin()));
                                    Date end = new Date(DateTimeUtil.dateToStampDay(userInfo.getLcEnd()));
                                    boolean ret = StringUtil.isEffectiveDate(now, begin, end);
                                    LogUtil.d(TAG, "ret : " + ret);

                                    if (!ret) {
                                        showMessage("请设置有效的生命周期后恢复!");
                                        notifyItemChanged(position);
                                    } else {
                                        DialogUtils.closeDialog(mLoadDialog);
                                        mLoadDialog.show();
                                        mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_RECOVERY_USER, userInfo.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                                    }
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                DialogUtils.closeDialog(mLoadDialog);
                                mLoadDialog.show();
                                mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_RECOVERY_USER, userInfo.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                            }

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
    public void onResume() {
        super.onResume();
        LogUtil.d(TAG, "mIsHint : " + mIsHint + " onResume");
        if (mIsHint) {
            ArrayList<DeviceUser> list = DeviceUserDao.getInstance(mCtx).queryUsers(mNodeId, ConstantUtil.DEVICE_TEMP);
            for (DeviceUser user : list) {
                StringUtil.checkTempUserStatus(mCtx, user);
            }
        }
        refreshView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        DeviceUserDao.getInstance(mCtx).unregisterObserver(mOb);
        mBleManagerHelper.removeUiListener(this);
    }
}
