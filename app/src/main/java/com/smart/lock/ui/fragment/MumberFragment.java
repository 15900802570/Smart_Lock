package com.smart.lock.ui.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
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
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.smart.lock.db.bean.DeviceLog;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.entity.Device;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.widget.SpacesItemDecoration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class MumberFragment extends BaseFragment implements View.OnClickListener, UiListener {
    private final static String TAG = MumberFragment.class.getSimpleName();

    private View mMumberView;
    private RecyclerView mUsersRv;
    private MumberAdapter mMumberAdapter;
    private LinearLayoutManager mLinerLayoutManager;
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
                if (users.size() >= 90) {
                    showMessage(mCtx.getResources().getString(R.string.members) + mCtx.getResources().getString(R.string.add_user_tips));
                    return;
                }
                DialogUtils.closeDialog(mLoadDialog);
                mLoadDialog.show();
                if (mDevice.getState() == Device.BLE_CONNECTED)
                    mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_CONNECT_ADD_MUMBER, (short) 0, BleMsg.INT_DEFAULT_TIMEOUT);
                else showMessage(getString(R.string.disconnect_ble));
                break;
//            case R.id.cb_selete_user:
//                LogUtil.d(TAG, "choise user delete : " + mSelectBtn.getText().toString());
//
//                if ((int) mSelectBtn.getTag() == R.string.all_election) {
//
//                    mSelectBtn.setText(R.string.cancel);
//                    mSelectBtn.setTag(R.string.cancel);
//                } else if ((int) mSelectBtn.getTag() == R.string.cancel) {
//                    mMumberAdapter.mDeleteUsers.clear();
//                    mSelectBtn.setText(R.string.all_election);
//                    mMumberAdapter.chioseALLDelete(false);
//                    mSelectBtn.setTag(R.string.all_election);
//                }
//                mMumberAdapter.notifyDataSetChanged();
//                break;
            case R.id.del_tv:
                if (mMumberAdapter.mDeleteUsers.size() != 0) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    for (DeviceUser devUser : mMumberAdapter.mDeleteUsers) {
                        LogUtil.d(TAG, "devUser = " + devUser.getUserId());
                        mBleManagerHelper.getBleCardService().sendCmd11((byte) 4, devUser.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                    }
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
        mAddUserTv = mMumberView.findViewById(R.id.tv_add);
        mSelectDeleteRl = mMumberView.findViewById(R.id.rl_select_delete);
        mSelectCb = mMumberView.findViewById(R.id.cb_selete_user);
        mDeleteTv = mMumberView.findViewById(R.id.del_tv);
        mTipTv = mMumberView.findViewById(R.id.tv_tips);
        return mMumberView;
    }

    public void initDate() {
        mCtx = mMumberView.getContext();
        mDefaultDevice = DeviceInfoDao.getInstance(mCtx).queryFirstData("device_default", true);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mDefaultUser = DeviceUserDao.getInstance(mCtx).queryUser(mDefaultDevice.getDeviceNodeId(), mDefaultDevice.getUserId());
        mBleManagerHelper = BleManagerHelper.getInstance(mCtx, false);
        mBleManagerHelper.addUiListener(this);
        mDevice = mBleManagerHelper.getBleCardService().getDevice();
        mMumberAdapter = new MumberAdapter(mCtx);
        mLinerLayoutManager = new LinearLayoutManager(mCtx, LinearLayoutManager.VERTICAL, false);
        mUsersRv.setLayoutManager(mLinerLayoutManager);
        mUsersRv.setItemAnimator(new DefaultItemAnimator());
        mUsersRv.setAdapter(mMumberAdapter);
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
                mMumberAdapter.mDeleteUsers.clear();
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
                    mMumberAdapter.mDeleteUsers.addAll(deleteUsers);
                    mMumberAdapter.chioseALLDelete(true);
                } else {
                    mTipTv.setText(R.string.all_election);
                    mMumberAdapter.chioseALLDelete(false);
                }
                mMumberAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void deviceStateChange(Device device, int state) {
        mDevice = device;
    }

    @Override
    public void dispatchUiCallback(Message msg, Device device, int type) {
        LogUtil.i(TAG, "dispatchUiCallback : " + msg.getType());
        mDevice = device;
        Bundle extra = msg.getData();
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEV_CMD_1E:
                DeviceUser user = (DeviceUser) extra.getSerializable(BleMsg.KEY_SERIALIZABLE);
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
            case Message.TYPE_BLE_RECEV_CMD_12:
                DeviceUser addUser = (DeviceUser) extra.getSerializable(BleMsg.KEY_SERIALIZABLE);
                if (addUser == null || addUser.getUserPermission() != ConstantUtil.DEVICE_MEMBER) {
                    DialogUtils.closeDialog(mLoadDialog);
                    return;
                }
                byte[] buf = new byte[64];
                byte[] authBuf = new byte[64];
                authBuf[0] = 0x02;
                System.arraycopy(extra.getByteArray(BleMsg.KEY_USER_ID), 0, authBuf, 1, 2);
                System.arraycopy(extra.getByteArray(BleMsg.KEY_NODE_ID), 0, authBuf, 3, 8);
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

                LogUtil.d(TAG, "buf = " + Arrays.toString(buf));

                String path = createQRcodeImage(buf);
                Log.d(TAG, "path = " + path);
                DeviceUser deviceUser;
                if (path != null) {
                    deviceUser = createDeviceUser(Short.parseShort(userId, 16), path, ConstantUtil.DEVICE_MEMBER);

                    if (deviceUser != null) {
                        mMumberAdapter.addItem(deviceUser);
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
                showMessage(mCtx.getString(R.string.add_user_success));
                break;
            case BleMsg.TYPE_ADD_USER_FAILED:
                showMessage(mCtx.getString(R.string.add_user_failed));
                break;
            case BleMsg.TYPE_DELETE_USER_SUCCESS:
                showMessage(mCtx.getString(R.string.delete_user_success));
                DeviceUser deleteUser = DeviceUserDao.getInstance(mCtx).queryUser(mNodeId, user.getUserId());
                Log.d(TAG, "deleteUser : " + deleteUser.toString());
                mMumberAdapter.removeItem(deleteUser);
                if (mMumberAdapter.mDeleteUsers.size() == 0) {
                    DialogUtils.closeDialog(mLoadDialog);
                } else return;
                break;
            case BleMsg.TYPE_DELETE_USER_FAILED:
                showMessage(mCtx.getString(R.string.delete_user_failed));
                break;
            case BleMsg.TYPE_PAUSE_USER_SUCCESS:
                showMessage(mCtx.getString(R.string.pause_user_success));
                DeviceUser pauseUser = DeviceUserDao.getInstance(mCtx).queryUser(mNodeId, user.getUserId());
                pauseUser.setUserStatus(ConstantUtil.USER_PAUSE);
                DeviceUserDao.getInstance(mCtx).updateDeviceUser(pauseUser);
                mMumberAdapter.changeUserState(pauseUser, ConstantUtil.USER_PAUSE);
                break;
            case BleMsg.TYPE_PAUSE_USER_FAILED:
                showMessage(mCtx.getString(R.string.pause_user_failed));
                break;
            case BleMsg.TYPE_RECOVERY_USER_SUCCESS:
                showMessage(mCtx.getString(R.string.recovery_user_success));

                DeviceUser recoveryUser = DeviceUserDao.getInstance(mCtx).queryUser(mNodeId, user.getUserId());
                recoveryUser.setUserStatus(ConstantUtil.USER_ENABLE);
                DeviceUserDao.getInstance(mCtx).updateDeviceUser(recoveryUser);
                mMumberAdapter.changeUserState(recoveryUser, ConstantUtil.USER_ENABLE);
                break;
            case BleMsg.TYPE_RECOVERY_USER_FAILED:
                showMessage(mCtx.getString(R.string.recovery_user_failed));
                break;
            default:
                break;
        }
        DialogUtils.closeDialog(mLoadDialog);
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
        public void onBindViewHolder(@NonNull final MumberViewHoler holder, final int position) {
            final DeviceUser userInfo = mUserList.get(position);
            if (userInfo != null) {
                holder.mNameTv.setText(userInfo.getUserName());
                if (userInfo.getUserStatus() == ConstantUtil.USER_UNENABLE) {
                    holder.mUserStateTv.setText(mCtx.getResources().getString(R.string.unenable));
                    mSwipelayout.setClickToClose(false);
                } else if (userInfo.getUserStatus() == ConstantUtil.USER_ENABLE) {
                    holder.mUserStateTv.setText(mCtx.getResources().getString(R.string.normal));
                    holder.mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.blue_enable));
                    holder.mUserPause.setVisibility(View.VISIBLE);
                    holder.mUserRecovery.setVisibility(View.GONE);
                    mSwipelayout.setRightSwipeEnabled(true);
                } else if (userInfo.getUserStatus() == ConstantUtil.USER_PAUSE) {
                    holder.mUserStateTv.setText(mCtx.getResources().getString(R.string.pause));
                    holder.mUserStateTv.setTextColor(mContext.getResources().getColor(R.color.yallow_pause));
                    holder.mUserPause.setVisibility(View.GONE);
                    holder.mUserRecovery.setVisibility(View.VISIBLE);
                    mSwipelayout.setRightSwipeEnabled(true);
                } else
                    holder.mUserStateTv.setText(mCtx.getResources().getString(R.string.invalid));
                mSwipelayout.setRightSwipeEnabled(false);
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
                        if (mDevice.getState() == Device.BLE_CONNECTED) {
                            mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_PAUSE_USER, userInfo.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                        }else showMessage(getString(R.string.disconnect_ble));
                    }
                });

                holder.mUserRecovery.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog.show();
                        if (mDevice.getState() == Device.BLE_CONNECTED) {
                            mBleManagerHelper.getBleCardService().sendCmd11(BleMsg.TYPT_RECOVERY_USER, userInfo.getUserId(), BleMsg.INT_DEFAULT_TIMEOUT);
                        }else showMessage(getString(R.string.disconnect_ble));
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
        public int getItemCount() {
            return mUserList.size();
        }

        public class MumberViewHoler extends RecyclerView.ViewHolder {
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

            public MumberViewHoler(View itemView) {
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
