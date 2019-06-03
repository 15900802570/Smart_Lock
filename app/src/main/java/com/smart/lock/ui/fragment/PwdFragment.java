package com.smart.lock.ui.fragment;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.entity.Device;
import com.smart.lock.ui.PwdSetActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.SpacesItemDecoration;

import java.util.ArrayList;
import java.util.Arrays;

public class PwdFragment extends BaseFragment implements View.OnClickListener, UiListener {
    private final static String TAG = PwdFragment.class.getSimpleName();

    private View mPwdView;
    private RecyclerView mListView;
    protected TextView mAddTv;
    private Device mDevice;

    private PwdManagerAdapter mPwdAdapter;
    private Context mCtx;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_add:
                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    int count = DeviceKeyDao.getInstance(mCtx).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_PWD).size();
                    if (count >= 0 && count < 1) {
                        Bundle bundle = new Bundle();
                        bundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);
                        bundle.putSerializable(BleMsg.KEY_TEMP_USER, mTempUser);
                        bundle.putString(BleMsg.KEY_CMD_TYPE, ConstantUtil.CREATE);
                        startIntent(PwdSetActivity.class, bundle);
                    } else {
                        showMessage(getResources().getString(R.string.add_pwd_tips));
                    }
                } else showMessage(getString(R.string.ble_disconnect));
                break;

            default:
                break;
        }
    }

    @Override
    public View initView() {
        mPwdView = View.inflate(mActivity, R.layout.fragment_device_key, null);
        mAddTv = mPwdView.findViewById(R.id.tv_add);
        Drawable top = getResources().getDrawable(R.mipmap.btn_add_password);
        mAddTv.setCompoundDrawablesWithIntrinsicBounds(null, top, null, null);
        mListView = mPwdView.findViewById(R.id.rv_key);
        return mPwdView;
    }

    public void setTempUser(DeviceUser tempUser) {
        LogUtil.d(TAG, "tempUser = " + (tempUser == null ? true : tempUser.toString()));
        if (tempUser != null) {
            mTempUser = tempUser;
        } else
            LogUtil.d(TAG, "临时用户为空");

    }

    public void initDate() {
        mCtx = mPwdView.getContext();
        mDefaultDevice = DeviceInfoDao.getInstance(mCtx).queryFirstData("device_default", true);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mBleManagerHelper = BleManagerHelper.getInstance(mCtx);
        mBleManagerHelper.addUiListener(this);
        mDevice = Device.getInstance(mCtx);
        mPwdAdapter = new PwdManagerAdapter(mCtx);
        mListView.setLayoutManager(new LinearLayoutManager(mCtx, LinearLayoutManager.VERTICAL, false));
        mListView.setItemAnimator(new DefaultItemAnimator());
        mListView.setAdapter(mPwdAdapter);
        mListView.addItemDecoration(new SpacesItemDecoration(getResources().getDimensionPixelSize(R.dimen.y5dp)));
        LogUtil.d(TAG, "tempUser 1= " + (mTempUser == null ? true : mTempUser.toString()));

        mAddTv.setVisibility(View.VISIBLE);
        mAddTv.setText(R.string.add_password);
        initEvent();
        mLoadDialog = DialogUtils.createLoadingDialog(mCtx, mCtx.getResources().getString(R.string.data_loading));
    }

    private void initEvent() {
        mAddTv.setOnClickListener(this);
    }

    public int getCounter() {
        return mPwdAdapter.getItemCount();
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
            default:
                break;

        }
    }

    private void dispatchErrorCode(byte errCode) {
        LogUtil.i(TAG, "errCode : " + errCode);
        switch (errCode) {
            case BleMsg.TYPE_DELETE_PASSWORD_SUCCESS:
                showMessage(getString(R.string.delete_pwd_success));
                mPwdAdapter.removeItem(mPwdAdapter.positionDelete);
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

    public class PwdManagerAdapter extends RecyclerView.Adapter<PwdManagerAdapter.ViewHolder> {
        private Context mContext;
        public ArrayList<DeviceKey> mPwdList;
        public int positionDelete;

        public PwdManagerAdapter(Context context) {
            mContext = context;
            mPwdList = DeviceKeyDao.getInstance(mCtx).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_PWD);
        }

        public void setDataSource(ArrayList<DeviceKey> pwdList) {
            mPwdList = pwdList;
        }

        public void addItem(DeviceKey key) {
            mPwdList.add(mPwdList.size(), key);
            notifyItemInserted(mPwdList.size());
        }

        public void removeItem(int index) {
            if (index != -1 && !mPwdList.isEmpty()) {
                DeviceKey del = mPwdList.remove(index);

                DeviceKeyDao.getInstance(mContext).delete(del);
                notifyDataSetChanged();
            }

        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_recycler, parent, false);
            SwipeLayout swipeLayout = inflate.findViewById(R.id.item_ll_recycler);
            swipeLayout.setClickToClose(true);
            swipeLayout.setRightSwipeEnabled(true);
            return new ViewHolder(inflate);
        }

        @Override
        public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
            final DeviceKey pwdInfo = mPwdList.get(position);
            if (pwdInfo != null) {
                LogUtil.d(TAG, "pwdInfo = " + pwdInfo.toString());
                viewHolder.mNameTv.setText(pwdInfo.getKeyName());
                viewHolder.mType.setImageResource(R.mipmap.icon_pwd);
                viewHolder.mCreateTime.setText(DateTimeUtil.timeStamp2Date(String.valueOf(pwdInfo.getKeyActiveTime()), "yyyy-MM-dd HH:mm:ss"));
                viewHolder.mEditorNameDialog = DialogUtils.createEditorDialog(getContext(), getString(R.string.modify_name), pwdInfo.getKeyName());
                viewHolder.mDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog.show();
                        positionDelete = position;
                        mBleManagerHelper.getBleCardService().sendCmd15(BleMsg.CMD_TYPE_DELETE, BleMsg.TYPE_PASSWORD, pwdInfo.getUserId(), Byte.parseByte(pwdInfo.getLockId()), String.valueOf(0), BleMsg.INT_DEFAULT_TIMEOUT);
                    }
                });
                viewHolder.mModifyLl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle bundle = new Bundle();
                        bundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);
                        bundle.putSerializable(BleMsg.KEY_MODIFY_DEVICE_KEY, pwdInfo);
                        bundle.putString(BleMsg.KEY_CMD_TYPE, ConstantUtil.MODIFY);
                        startIntent(PwdSetActivity.class, bundle);

                    }
                });

                viewHolder.mEditIbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((EditText) viewHolder.mEditorNameDialog.findViewById(R.id.editor_et)).setText(viewHolder.mNameTv.getText());
                        viewHolder.mEditorNameDialog.show();
                    }
                });

                viewHolder.mEditorNameDialog.findViewById(R.id.dialog_confirm_btn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String newName = ((EditText) viewHolder.mEditorNameDialog.findViewById(R.id.editor_et)).getText().toString();
                        if (!newName.isEmpty()) {
                            viewHolder.mNameTv.setText(newName);
                            pwdInfo.setKeyName(newName);
                            DeviceKeyDao.getInstance(mActivity).updateDeviceKey(pwdInfo);
                        } else {
                            ToastUtil.showLong(mActivity, R.string.cannot_be_empty_str);
                        }
                        viewHolder.mEditorNameDialog.dismiss();
                    }
                });

            }

        }

        @Override
        public int getItemCount() {
            return mPwdList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            SwipeLayout mSwipeLayout;
            TextView mNameTv;
            ImageView mType;
            TextView mCreateTime;
            LinearLayout mDelete;
            LinearLayout mModifyLl;
            ImageButton mEditIbtn;
            Dialog mEditorNameDialog;

            public ViewHolder(View itemView) {
                super(itemView);

                mSwipeLayout = (SwipeLayout) itemView;
                mType = itemView.findViewById(R.id.iv_type);
                mNameTv = itemView.findViewById(R.id.tv_username);
                mDelete = itemView.findViewById(R.id.ll_delete);
                mModifyLl = itemView.findViewById(R.id.ll_modify);
                mCreateTime = itemView.findViewById(R.id.tv_create_time);
                mEditIbtn = itemView.findViewById(R.id.ib_edit);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ArrayList<DeviceKey> pwdList = DeviceKeyDao.getInstance(mCtx).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_PWD);
        LogUtil.d(TAG, "pwdList = " + pwdList);
        mPwdAdapter.setDataSource(pwdList);
        mPwdAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBleManagerHelper.removeUiListener(this);
    }
}
