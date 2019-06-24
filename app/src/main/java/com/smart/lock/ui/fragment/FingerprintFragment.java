package com.smart.lock.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.listener.ClientTransaction;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.entity.Device;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.SpacesItemDecoration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class FingerprintFragment extends BaseFragment implements View.OnClickListener, UiListener {
    private final static String TAG = FingerprintFragment.class.getSimpleName();

    private View mFpView;
    private RecyclerView mListView;
    protected TextView mAddTv;
    private Device mDevice;
    private FpManagerAdapter mFpAdapter;
    private Context mCtx;
    private Dialog mCancelDialog;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_add:
                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    int count = DeviceKeyDao.getInstance(mCtx).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_FINGERPRINT).size();
                    if (count >= 0 && count < 5) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog = DialogUtils.createLoadingDialog(mCtx, mCtx.getResources().getString(R.string.plz_input) + mCtx.getResources().getString(R.string.fingerprint));
                        mLoadDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

                            @Override
                            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_BACK) {
                                    LogUtil.d(TAG, "按了返回键");
                                    mCancelDialog = DialogUtils.createTipsDialogWithConfirmAndCancel(mCtx, getString(R.string.cancel_warning) + getString(R.string.fingerprint) + getString(R.string.input));

                                    mCancelDialog.findViewById(R.id.dialog_confirm_btn).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            mBleManagerHelper.getBleCardService().cancelCmd(Message.TYPE_BLE_SEND_CMD_15 + "#" + "single");
                                            mBleManagerHelper.getBleCardService().sendCmd15(BleMsg.CMD_TYPE_CANCEL_CREATE, BleMsg.TYPE_FINGERPRINT, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), (byte) 0, String.valueOf(0), BleMsg.INT_DEFAULT_TIMEOUT);
                                            mCancelDialog.cancel();
                                        }
                                    });
                                    if (!mCancelDialog.isShowing()) {
                                        mCancelDialog.show();
                                    }

                                    return true;
                                }
                                return false;
                            }

                        });
                        mLoadDialog.show();
                        mBleManagerHelper.getBleCardService().sendCmd15(BleMsg.CMD_TYPE_CREATE, BleMsg.TYPE_FINGERPRINT, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), (byte) 0, String.valueOf(0), BleMsg.INT_DEFAULT_TIMEOUT);
                    } else {
                        showMessage(getResources().getString(R.string.add_fp_tips));
                    }
                } else showMessage(getString(R.string.ble_disconnect));

                break;

            default:
                break;
        }
    }

    @Override
    public View initView() {
        mFpView = View.inflate(mActivity, R.layout.fragment_device_key, null);
        mAddTv = mFpView.findViewById(R.id.tv_add);
        Drawable top = getResources().getDrawable(R.mipmap.btn_add_fingerprint);
        mAddTv.setCompoundDrawablesWithIntrinsicBounds(null, top, null, null);
        mListView = mFpView.findViewById(R.id.rv_key);
        return mFpView;
    }

    public void setTempUser(DeviceUser tempUser) {
        LogUtil.d(TAG, "tempUser = " + (tempUser == null ? true : tempUser.toString()));
        if (tempUser != null) {
            mTempUser = tempUser;
        } else
            LogUtil.d(TAG, "临时用户为空");

    }

    public void initDate() {
        mCtx = mFpView.getContext();
        mDefaultDevice = DeviceInfoDao.getInstance(mCtx).queryFirstData("device_default", true);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mBleManagerHelper = BleManagerHelper.getInstance(mCtx);
        mBleManagerHelper.addUiListener(this);
        mDevice = Device.getInstance(mCtx);
        mFpAdapter = new FpManagerAdapter(mCtx);
        mListView.setLayoutManager(new LinearLayoutManager(mCtx, LinearLayoutManager.VERTICAL, false));
        mListView.setItemAnimator(new DefaultItemAnimator());
        mListView.setAdapter(mFpAdapter);
        mListView.addItemDecoration(new SpacesItemDecoration(getResources().getDimensionPixelSize(R.dimen.y5dp)));
        LogUtil.d(TAG, "tempUser 1= " + (mTempUser == null ? true : mTempUser.toString()));

        mAddTv.setVisibility(View.VISIBLE);
        mAddTv.setText(R.string.add_fingerprint);

        initEvent();

        mLoadDialog = DialogUtils.createLoadingDialog(mCtx, mCtx.getResources().getString(R.string.data_loading));
    }

    private void initEvent() {
        mAddTv.setOnClickListener(this);
    }

    public int getCounter() {
        return mFpAdapter.getItemCount();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
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
        Bundle extra = msg.getData();
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_1E:
                final byte[] errCode = extra.getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode != null)
                    dispatchErrorCode(errCode[3]);
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_16:
                Serializable serializable = extra.getSerializable(BleMsg.KEY_SERIALIZABLE);
                if (serializable != null && !(serializable instanceof DeviceKey)) {
                    return;
                }
                DeviceKey key = (DeviceKey) serializable;
                if (key == null || (key.getKeyType() != ConstantUtil.USER_FINGERPRINT)) {
                    DialogUtils.closeDialog(mLoadDialog);
                    return;
                }

                final byte[] lockId = extra.getByteArray(BleMsg.KEY_LOCK_ID);
                String mLockId = String.valueOf(lockId[0]);
                DeviceKey deviceKey = new DeviceKey();
                deviceKey.setDeviceNodeId(mDefaultDevice.getDeviceNodeId());
                deviceKey.setUserId(mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId());
                deviceKey.setKeyActiveTime(System.currentTimeMillis() / 1000);
                deviceKey.setKeyName(mCtx.getResources().getString(R.string.fingerprint) + (Integer.parseInt(mLockId)));
                deviceKey.setKeyType(ConstantUtil.USER_FINGERPRINT);
                deviceKey.setLockId(mLockId);
                DeviceKeyDao.getInstance(mCtx).insert(deviceKey);
                if (mTempUser != null && mTempUser.getUserStatus() == ConstantUtil.USER_UNENABLE) {
                    mTempUser.setUserStatus(ConstantUtil.USER_ENABLE);
                    DeviceUserDao.getInstance(mCtx).updateDeviceUser(mTempUser);
                }

                mFpAdapter.addItem(deviceKey);
                DialogUtils.closeDialog(mLoadDialog);
                break;
            default:
                break;

        }
    }

    private void dispatchErrorCode(byte errCode) {
        LogUtil.i(TAG, "errCode : " + errCode);
        switch (errCode) {
            case BleMsg.TYPE_ENTER_OR_MODIFY_FP_FAILED:
                if (mFpAdapter.positionModify != -1) {
                    showMessage(mCtx.getResources().getString(R.string.modify_fp_failed));
                    mFpAdapter.positionModify = -1;
                } else {
                    showMessage(mCtx.getResources().getString(R.string.add_fp_failed));
                }
                mFpAdapter.notifyDataSetChanged();
                break;
            case BleMsg.TYPE_MODIFY_FP_SUCCESS:
                showMessage(mCtx.getResources().getString(R.string.modify_fp_success));
                mFpAdapter.notifyDataSetChanged();
                break;
            case BleMsg.TYPE_DELETE_FP_SUCCESS:
                showMessage(getString(R.string.delete_fp_success));
                mFpAdapter.removeItem(mFpAdapter.positionDelete);
                break;
            case BleMsg.TYPE_DELETE_FP_FAILED:
                showMessage(mCtx.getResources().getString(R.string.delete_fp_failed));
                mFpAdapter.notifyDataSetChanged();
                break;
            case BleMsg.TYPE_FP_FULL:
                showMessage(mCtx.getResources().getString(R.string.fp_full));
                break;
            case BleMsg.TYPE_FINGERPRINT_EXIST:
                showMessage(mCtx.getResources().getString(R.string.fp_exist));
                mFpAdapter.notifyDataSetChanged();
                break;
            case BleMsg.TYPE_EQUIPMENT_BUSY:
                showMessage(mCtx.getResources().getString(R.string.device_busy));
                break;
            default:
                break;
        }

        DialogUtils.closeDialog(mLoadDialog);
    }

    @Override
    public void reConnectBle(Device device) {

    }

    @Override
    public void sendFailed(Message msg) {
        int exception = msg.getException();
        switch (exception) {
            case Message.EXCEPTION_TIMEOUT:
                DialogUtils.closeDialog(mLoadDialog);
                LogUtil.e(TAG, msg.getType() + " can't receiver msg!");
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

    public class FpManagerAdapter extends RecyclerView.Adapter<FpManagerAdapter.ViewHolder> {
        private Context mContext;
        public ArrayList<DeviceKey> mFpList;
        public int positionDelete = -1;
        public int positionModify = -1;

        public FpManagerAdapter(Context context) {
            mContext = context;
            mFpList = DeviceKeyDao.getInstance(mCtx).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_FINGERPRINT);
        }

        public void setDataSource(ArrayList<DeviceKey> cardList) {
            mFpList = cardList;
        }

        public void addItem(DeviceKey key) {
            mFpList.add(mFpList.size(), key);
            notifyItemInserted(mFpList.size());
        }

        public void removeItem(int index) {
            LogUtil.d(TAG, "mFpList = " + mFpList.toString());
            if (index != -1 && !mFpList.isEmpty()) {
                DeviceKey del = mFpList.remove(index);

                DeviceKeyDao.getInstance(mContext).delete(del);
                notifyDataSetChanged();
            }

        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_recycler, parent, false);
            SwipeLayout swipeLayout = inflate.findViewById(R.id.item_ll_recycler);
            swipeLayout.setClickToClose(true);
            swipeLayout.setRightSwipeEnabled(true);
            return new ViewHolder(inflate);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder viewHolder, @SuppressLint("RecyclerView") final int position) {
            final DeviceKey fpInfo = mFpList.get(position);
            LogUtil.d(TAG, "fpInfo = " + fpInfo.toString());
            viewHolder.mNameTv.setText(fpInfo.getKeyName());
            viewHolder.mType.setImageResource(R.mipmap.icon_fingerprint);
            viewHolder.mCreateTime.setText(DateTimeUtil.timeStamp2Date(String.valueOf(fpInfo.getKeyActiveTime()), "yyyy-MM-dd HH:mm:ss"));

            viewHolder.mEditorNameDialog = DialogUtils.createEditorDialog(getContext(), getString(R.string.modify_name), fpInfo.getKeyName());
            final EditText editText = viewHolder.mEditorNameDialog.findViewById(R.id.editor_et);
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});

            viewHolder.mDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mDevice.getState() == Device.BLE_CONNECTED) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog = DialogUtils.createLoadingDialog(mCtx, mCtx.getResources().getString(R.string.data_loading));
                        mLoadDialog.show();
                        positionDelete = position;
                        mBleManagerHelper.getBleCardService().sendCmd15(BleMsg.CMD_TYPE_DELETE, BleMsg.TYPE_FINGERPRINT, fpInfo.getUserId(), Byte.parseByte(fpInfo.getLockId()), String.valueOf(0), BleMsg.INT_DEFAULT_TIMEOUT);
                    } else showMessage(getString(R.string.disconnect_ble));
                }
            });
            viewHolder.mModifyLl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mDevice.getState() == Device.BLE_CONNECTED) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog = DialogUtils.createLoadingDialog(mCtx, mCtx.getResources().getString(R.string.plz_modify) + mCtx.getResources().getString(R.string.fingerprint));
                        mLoadDialog.show();
                        positionModify = position;
                        mBleManagerHelper.getBleCardService().sendCmd15(BleMsg.CMD_TYPE_MODIFY, BleMsg.TYPE_FINGERPRINT, fpInfo.getUserId(), Byte.parseByte(fpInfo.getLockId()), String.valueOf(0), BleMsg.INT_DEFAULT_TIMEOUT);
                    } else showMessage(getString(R.string.disconnect_ble));
                }
            });

            viewHolder.mEditIbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editText.setText(viewHolder.mNameTv.getText());
                    viewHolder.mEditorNameDialog.show();
                }
            });

            viewHolder.mEditorNameDialog.findViewById(R.id.dialog_confirm_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final String newName = editText.getText().toString();
                    if (!newName.isEmpty()) {
                        viewHolder.mNameTv.setText(newName);
                        fpInfo.setKeyName(newName);
                        DeviceKeyDao.getInstance(mActivity).updateDeviceKey(fpInfo);
                    } else {
                        ToastUtil.showLong(mActivity, R.string.cannot_be_empty_str);
                    }
                    viewHolder.mEditorNameDialog.dismiss();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mFpList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            SwipeLayout mSwipeLayout;
            TextView mNameTv;
            ImageView mType;
            ImageButton mEditIbtn;
            TextView mCreateTime;
            LinearLayout mDelete;
            LinearLayout mModifyLl;
            Dialog mEditorNameDialog;

            public ViewHolder(View itemView) {
                super(itemView);

                mSwipeLayout = (SwipeLayout) itemView;
                mType = itemView.findViewById(R.id.iv_type);
                mNameTv = itemView.findViewById(R.id.tv_username);
                mDelete = itemView.findViewById(R.id.ll_delete);
                mCreateTime = itemView.findViewById(R.id.tv_create_time);
                mEditIbtn = itemView.findViewById(R.id.ib_edit);
                mModifyLl = itemView.findViewById(R.id.ll_modify);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mFpAdapter.setDataSource(DeviceKeyDao.getInstance(mCtx).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_FINGERPRINT));
        mFpAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBleManagerHelper.removeUiListener(this);
    }


}
