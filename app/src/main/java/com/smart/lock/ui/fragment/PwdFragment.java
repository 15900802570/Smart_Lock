package com.smart.lock.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.ui.PwdSetActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.SpacesItemDecoration;

import java.util.ArrayList;
import java.util.Arrays;

public class PwdFragment extends BaseFragment implements View.OnClickListener {
    private final static String TAG = PwdFragment.class.getSimpleName();

    private View mPwdView;
    private RecyclerView mListView;
    protected TextView mAddTv;

    private PwdManagerAdapter mPwdAdapter;
    private boolean mIsVisibleFragment = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_add:
                int count = DeviceKeyDao.getInstance(mPwdView.getContext()).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_PWD).size();
                if (count >= 0 && count < 1) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);
                    bundle.putSerializable(BleMsg.KEY_TEMP_USER, mTempUser);
                    bundle.putString(BleMsg.KEY_CMD_TYPE, ConstantUtil.CREATE);
                    startIntent(PwdSetActivity.class, bundle);
                } else {
                    showMessage(getResources().getString(R.string.add_pwd_tips));
                }
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
        mDefaultDevice = DeviceInfoDao.getInstance(mPwdView.getContext()).queryFirstData("device_default", true);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mBleManagerHelper = BleManagerHelper.getInstance(mPwdView.getContext(), false);

        mPwdAdapter = new PwdManagerAdapter(mPwdView.getContext());
        mListView.setLayoutManager(new LinearLayoutManager(mPwdView.getContext(), LinearLayoutManager.VERTICAL, false));
        mListView.setItemAnimator(new DefaultItemAnimator());
        mListView.setAdapter(mPwdAdapter);
        mListView.addItemDecoration(new SpacesItemDecoration(getResources().getDimensionPixelSize(R.dimen.y5dp)));
        LogUtil.d(TAG, "tempUser 1= " + (mTempUser == null ? true : mTempUser.toString()));

        mAddTv.setVisibility(View.VISIBLE);
        mAddTv.setText(R.string.add_password);

        initEvent();

        mLoadDialog = DialogUtils.createLoadingDialog(mPwdView.getContext(), mPwdView.getContext().getResources().getString(R.string.data_loading));

        LocalBroadcastManager.getInstance(mPwdView.getContext()).registerReceiver(pwdReceiver, intentFilter());
    }

    private void initEvent() {
        mAddTv.setOnClickListener(this);
    }

    private static IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.STR_RSP_SERVER_DATA);
        intentFilter.addAction(BleMsg.STR_RSP_MSG1E_ERRCODE);
        intentFilter.addAction(BleMsg.STR_RSP_MSG16_LOCKID);
        intentFilter.addAction(BleMsg.STR_RSP_MSG1A_STATUS);
        intentFilter.addAction(BleMsg.STR_RSP_MSG18_TIMEOUT);
        return intentFilter;
    }

    private final BroadcastReceiver pwdReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //MSG1E 设备->apk，返回信息
            if (action.equals(BleMsg.STR_RSP_MSG1E_ERRCODE)) {

                final byte[] errCode = intent.getByteArrayExtra(BleMsg.KEY_ERROR_CODE);
                Log.d(TAG, "errCode[3] = " + errCode[3]);

                if (errCode[3] == 0x0d) {
                    showMessage(mPwdView.getContext().getResources().getString(R.string.delete_pwd_success));

                    mPwdAdapter.removeItem(mPwdAdapter.positionDelete);
                }

                DialogUtils.closeDialog(mLoadDialog);
                mHandler.removeCallbacks(mRunnable);

            }

            if (action.equals(BleMsg.STR_RSP_MSG1A_STATUS)) {

            }

            if (action.equals(BleMsg.STR_RSP_MSG18_TIMEOUT)) {
                Log.d(TAG, "STR_RSP_MSG18_TIMEOUT");
                byte[] seconds = intent.getByteArrayExtra(BleMsg.KEY_TIME_OUT);
                if (mIsVisibleFragment) {
                    Log.d(TAG, "seconds = " + Arrays.toString(seconds));
                    mCt.reSetTimeOut(seconds[0] * 1000);
                    if (!mLoadDialog.isShowing()) {
                        mLoadDialog.show();
                    }
                    closeDialog((int) seconds[0]);
                }

            }
        }
    };

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        mIsVisibleFragment = isVisibleToUser;
        LogUtil.d(TAG, "pwd isVisibleToUser = " + isVisibleToUser);
    }

    public class PwdManagerAdapter extends RecyclerView.Adapter<PwdManagerAdapter.ViewHolder> {
        private Context mContext;
        public ArrayList<DeviceKey> mPwdList;
        public int positionDelete;

        public PwdManagerAdapter(Context context) {
            mContext = context;
            mPwdList = DeviceKeyDao.getInstance(mPwdView.getContext()).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_PWD);
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
            LogUtil.d(TAG, "pwdInfo = " + pwdInfo.toString());
            if (pwdInfo != null) {
                viewHolder.mNameTv.setText(pwdInfo.getKeyName());
                viewHolder.mType.setImageResource(R.mipmap.icon_pwd);
                viewHolder.mCreateTime.setText(DateTimeUtil.timeStamp2Date(String.valueOf(pwdInfo.getKeyActiveTime()), "yyyy-MM-dd HH:mm:ss"));
                viewHolder.mEditorNameDialog = DialogUtils.createEditorDialog(getContext(), getString(R.string.modify_name), pwdInfo.getKeyName());
                viewHolder.mDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog.show();
                        closeDialog(10);
                        positionDelete = position;
                        mCt = mBleManagerHelper.getBleCardService().sendCmd15((byte) 1, (byte) 0, pwdInfo.getUserId(), Byte.parseByte(pwdInfo.getLockId()), String.valueOf(0));
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
        ArrayList<DeviceKey> pwdList = DeviceKeyDao.getInstance(mPwdView.getContext()).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_PWD);
        LogUtil.d(TAG, "pwdList = " + pwdList);
        mPwdAdapter.setDataSource(pwdList);
        mPwdAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(mPwdView.getContext()).unregisterReceiver(pwdReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
    }
}
