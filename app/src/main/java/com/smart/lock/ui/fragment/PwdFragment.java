package com.smart.lock.ui.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import java.util.ArrayList;
import java.util.Arrays;

public class PwdFragment extends BaseFragment implements View.OnClickListener {
    private final static String TAG = PwdFragment.class.getSimpleName();

    private View mPwdView;
    private RecyclerView mListView;
    protected Button mAddBtn;

    private PwdManagerAdapter mPwdAdapter;
    private boolean mIsVisibleFragment = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_add:
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
        mAddBtn = mPwdView.findViewById(R.id.btn_add);
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
        LogUtil.d(TAG, "tempUser 1= " + (mTempUser == null ? true : mTempUser.toString()));

        mAddBtn.setVisibility(View.VISIBLE);
        mAddBtn.setText(R.string.add_password);

        initEvent();

        mLoadDialog = DialogUtils.createLoadingDialog(mPwdView.getContext(), mPwdView.getContext().getResources().getString(R.string.data_loading));

        LocalBroadcastManager.getInstance(mPwdView.getContext()).registerReceiver(pwdReceiver, intentFilter());
    }

    private void initEvent() {
        mAddBtn.setOnClickListener(this);
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
            LogUtil.d(TAG, "mPwdList = " + mPwdList.toString());
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
                viewHolder.mType.setImageResource(R.mipmap.record_pwd);
                viewHolder.mCreateTime.setText(DateTimeUtil.timeStamp2Date(String.valueOf(pwdInfo.getKeyActiveTime()), "yyyy-MM-dd HH:mm:ss"));
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

                final AlertDialog editDialog = DialogUtils.showEditKeyDialog(mContext, mContext.getString(R.string.modify_note_name), pwdInfo);
                viewHolder.mEditIbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editDialog.show();
                    }
                });

                if (editDialog != null) {
                    editDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            viewHolder.mNameTv.setText(DeviceKeyDao.getInstance(mContext).queryByLockId(pwdInfo.getDeviceNodeId(), pwdInfo.getUserId(), pwdInfo.getLockId(), ConstantUtil.USER_PWD).getKeyName());
                        }
                    });
                }

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
