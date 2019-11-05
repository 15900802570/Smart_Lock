package com.smart.lock.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
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
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.SpacesItemDecoration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class FaceFragment extends BaseFragment implements View.OnClickListener, UiListener {
    private final static String TAG = FaceFragment.class.getSimpleName();

    private View mFaceView;
    private RecyclerView mListView;
    protected TextView mAddTv;

    private FaceManagerAdapter mFaceAdapter;
    private String mLockId = null;
    private Context mCtx;
    private Device mDevice;
    private Dialog mCancelDialog;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_add:
                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    int count = DeviceKeyDao.getInstance(mCtx).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_FACE).size();
                    if (count >= 0 && count < 1) {
                        mBleManagerHelper.getBleCardService().sendCmd15(BleMsg.CMD_TYPE_CREATE, BleMsg.TYPE_FACE, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), (byte) 0, String.valueOf(0), BleMsg.INT_DEFAULT_TIMEOUT);
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog = DialogUtils.createLoadingDialog(mCtx, mCtx.getResources().getString(R.string.plz_input) + mCtx.getResources().getString(R.string.face_info));
                        mLoadDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

                            @Override
                            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_BACK) {
                                    LogUtil.d(TAG, "按了返回键");
                                    mCancelDialog = DialogUtils.createTipsDialogWithConfirmAndCancel(mCtx, getString(R.string.cancel_warning) + getString(R.string.face_info) + getString(R.string.input));

                                    mCancelDialog.findViewById(R.id.dialog_confirm_btn).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            mBleManagerHelper.getBleCardService().cancelCmd(Message.TYPE_BLE_SEND_CMD_15 + "#" + "single");
                                            mBleManagerHelper.getBleCardService().sendCmd15(BleMsg.CMD_TYPE_CANCEL_CREATE, BleMsg.TYPE_FACE, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), (byte) 0, String.valueOf(0), BleMsg.INT_DEFAULT_TIMEOUT);
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
                        LogUtil.d(TAG,"Send cmd15");
                    } else {
                        showMessage(getResources().getString(R.string.add_face_tips));
                    }
                } else showMessage(getString(R.string.ble_disconnect));
                break;

            default:
                break;
        }
    }

    @Override
    public View initView() {
        mFaceView = View.inflate(mActivity, R.layout.fragment_device_key, null);
        mAddTv = mFaceView.findViewById(R.id.tv_add);
        Drawable top = getResources().getDrawable(R.mipmap.btn_face_add);
        mAddTv.setCompoundDrawablesWithIntrinsicBounds(null, top, null, null);
        mListView = mFaceView.findViewById(R.id.rv_key);
        return mFaceView;
    }

    public void setTempUser(DeviceUser tempUser) {
        LogUtil.d(TAG, "tempUser = " + (tempUser == null ? true : tempUser.toString()));
        if (tempUser != null) {
            mTempUser = tempUser;
        } else
            LogUtil.d(TAG, "临时用户为空");

    }

    public void initDate() {
        mCtx = mFaceView.getContext();
        mDefaultDevice = DeviceInfoDao.getInstance(mCtx).queryFirstData("device_default", true);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mBleManagerHelper = BleManagerHelper.getInstance(mCtx);
        mBleManagerHelper.addUiListener(this);
        mDevice = Device.getInstance(mCtx);
        mFaceAdapter = new FaceManagerAdapter(mCtx);
        mListView.setLayoutManager(new LinearLayoutManager(mCtx, LinearLayoutManager.VERTICAL, false));
        mListView.setItemAnimator(new DefaultItemAnimator());
        mListView.setAdapter(mFaceAdapter);
        mListView.addItemDecoration(new SpacesItemDecoration(getResources().getDimensionPixelSize(R.dimen.y5dp)));
        LogUtil.d(TAG, "tempUser 1= " + (mTempUser == null ? true : mTempUser.toString()));

        mAddTv.setVisibility(View.VISIBLE);
        mAddTv.setText(R.string.add_face);

        initEvent();
    }

    private void initEvent() {
        mAddTv.setOnClickListener(this);
    }

    public int getCounter() {
        return mFaceAdapter.getItemCount();
    }

    @Override
    public void deviceStateChange(Device device, int state) {
        switch (state) {
            case BleMsg.STATE_DISCONNECTED:
                DialogUtils.closeDialog(mLoadDialog);
                DialogUtils.closeDialog(mCancelDialog);
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
                LogUtil.d(TAG, "key = " + ((key == null) ? true : key.toString()));
                if (key == null || (key.getKeyType() != ConstantUtil.USER_FACE)) {
                    DialogUtils.closeDialog(mLoadDialog);
                    return;
                }

                final byte[] lockId = extra.getByteArray(BleMsg.KEY_LOCK_ID);
                mLockId = String.valueOf(lockId[0]);
                DeviceKey deviceKey = new DeviceKey();
                deviceKey.setDeviceNodeId(mDefaultDevice.getDeviceNodeId());
                deviceKey.setUserId(mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId());
                deviceKey.setKeyActiveTime(System.currentTimeMillis() / 1000);
                deviceKey.setKeyName(mCtx.getString(R.string.face_manager));
                deviceKey.setKeyType(ConstantUtil.USER_FACE);
                deviceKey.setLockId(mLockId);
                DeviceKeyDao.getInstance(mCtx).insert(deviceKey);
                if (mTempUser != null && mTempUser.getUserStatus() == ConstantUtil.USER_UNENABLE) {
                    mTempUser.setUserStatus(ConstantUtil.USER_ENABLE);
                    DeviceUserDao.getInstance(mCtx).updateDeviceUser(mTempUser);
                }
                mFaceAdapter.addItem(deviceKey);
                DialogUtils.closeDialog(mLoadDialog);
                break;
            default:
                break;

        }
    }

    private void dispatchErrorCode(byte errCode) {
        LogUtil.i(TAG, "errCode : " + errCode);
        switch (errCode) {
            case BleMsg.TYPE_ENTER_OR_MODIFY_FACE_FAILED:
                showMessage(mCtx.getResources().getString(R.string.add_face_failed));
                mFaceAdapter.notifyDataSetChanged();
                break;
            case BleMsg.TYPE_MODIFY_FACE_SUCCESS:
                showMessage(mCtx.getResources().getString(R.string.modify_face_success));
                mFaceAdapter.notifyDataSetChanged();
                break;
            case BleMsg.TYPE_DELETE_FACE_SUCCESS:
                showMessage(mCtx.getResources().getString(R.string.delete_face_success));
                mFaceAdapter.removeItem(mFaceAdapter.positionDelete);
                break;
            case BleMsg.TYPE_DELETE_FACE_FAILED:
                showMessage(mCtx.getResources().getString(R.string.delete_face_failed));
                break;
            case BleMsg.TYPE_DEV_KEY_REPETITION:
            case BleMsg.TYPE_FACE_EXIST:
                showMessage(getString(R.string.face_exist));
                mFaceAdapter.notifyDataSetChanged();
                break;
            case BleMsg.TYPE_OPEN_SLIDE:
                if (mBleManagerHelper.getBleCardService() != null)
                    mBleManagerHelper.getBleCardService().cancelCmd(Message.TYPE_BLE_SEND_CMD_15 + "#" + "single");
                showMessage(getString(R.string.plz_open_slide));
                break;
            default:
                break;
        }
        DialogUtils.closeDialog(mCancelDialog);
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

    public class FaceManagerAdapter extends RecyclerView.Adapter<FaceManagerAdapter.ViewHolder> {
        private Context mContext;
        public ArrayList<DeviceKey> mFaceList;
        public int positionDelete = -1;
        public int positionModify = -1;

        public FaceManagerAdapter(Context context) {
            mContext = context;
            mFaceList = DeviceKeyDao.getInstance(mCtx).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_FACE);
        }

        public void setDataSource(ArrayList<DeviceKey> faceList) {
            mFaceList = faceList;
        }

        public void addItem(DeviceKey key) {
            mFaceList.add(mFaceList.size(), key);
            notifyItemInserted(mFaceList.size());
        }

        public void removeItem(int index) {
            if (index != -1 && !mFaceList.isEmpty()) {
                DeviceKey del = mFaceList.remove(index);

                DeviceKeyDao.getInstance(mCtx).delete(del);
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
            final DeviceKey faceInfo = mFaceList.get(position);
            if (faceInfo != null) {
                viewHolder.mNameTv.setText(faceInfo.getKeyName());
                viewHolder.mType.setImageResource(R.mipmap.icon_face);
                viewHolder.mCreateTime.setText(DateTimeUtil.timeStamp2Date(String.valueOf(faceInfo.getKeyActiveTime()), "yyyy-MM-dd HH:mm:ss"));
                viewHolder.mEditorNameDialog = DialogUtils.createEditorDialog(getContext(), getString(R.string.modify_name), faceInfo.getKeyName());
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
                            mBleManagerHelper.getBleCardService().sendCmd15(BleMsg.CMD_TYPE_DELETE, BleMsg.TYPE_FACE, faceInfo.getUserId(), Byte.parseByte(faceInfo.getLockId()), String.valueOf(0), BleMsg.INT_DEFAULT_TIMEOUT);
                        } else showMessage(getString(R.string.disconnect_ble));
                    }
                });

                viewHolder.mModifyLl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mDevice.getState() == Device.BLE_CONNECTED) {
                            DialogUtils.closeDialog(mLoadDialog);
                            mLoadDialog = DialogUtils.createLoadingDialog(mCtx, mCtx.getResources().getString(R.string.plz_modify) + mCtx.getResources().getString(R.string.face_info));
                            mLoadDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

                                @Override
                                public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                                    if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_BACK) {
                                        LogUtil.d(TAG, "按了返回键");
                                        mCancelDialog = DialogUtils.createTipsDialogWithConfirmAndCancel(mCtx, getString(R.string.cancel_warning) + getString(R.string.face_info) + getString(R.string.modify));

                                        mCancelDialog.findViewById(R.id.dialog_confirm_btn).setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                mBleManagerHelper.getBleCardService().cancelCmd(Message.TYPE_BLE_SEND_CMD_15 + "#" + "single");
                                                mBleManagerHelper.getBleCardService().sendCmd15(BleMsg.CMD_TYPE_CANCEL_CREATE, BleMsg.TYPE_FACE, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), (byte) 0, String.valueOf(0), BleMsg.INT_DEFAULT_TIMEOUT);
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
                            positionModify = position;
                            mBleManagerHelper.getBleCardService().sendCmd15(BleMsg.CMD_TYPE_MODIFY, BleMsg.TYPE_FACE, faceInfo.getUserId(), Byte.parseByte(faceInfo.getLockId()), String.valueOf(0), BleMsg.INT_DEFAULT_TIMEOUT);
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
                            faceInfo.setKeyName(newName);
                            DeviceKeyDao.getInstance(mActivity).updateDeviceKey(faceInfo);
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
            return mFaceList.size();
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
                mCreateTime = itemView.findViewById(R.id.tv_create_time);
                mModifyLl = itemView.findViewById(R.id.ll_modify);
                mModifyLl.setVisibility(View.VISIBLE);
                mEditIbtn = itemView.findViewById(R.id.ib_edit);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        mFaceAdapter.setDataSource(DeviceKeyDao.getInstance(mCtx).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_FACE));
        mFaceAdapter.notifyDataSetChanged();

        mLoadDialog = DialogUtils.createLoadingDialog(mCtx, mCtx.getResources().getString(R.string.data_loading));

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBleManagerHelper.removeUiListener(this);
    }

}
