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
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class FingerprintFragment extends BaseFragment implements View.OnClickListener {
    private final static String TAG = FingerprintFragment.class.getSimpleName();

    private View mFpView;
    private RecyclerView mListView;
    protected Button mAddBtn;

    private FpManagerAdapter mFpAdapter;
    private String mLockId = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_add:
                int count = DeviceKeyDao.getInstance(mFpView.getContext()).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_FINGERPRINT).size();
                if (count >= 0 && count < 5) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    closeDialog(15);
                    mBleManagerHelper.getBleCardService().sendCmd15((byte) 0, (byte) 1, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), (byte) 0, String.valueOf(0));
                } else {
                    showMessage(getResources().getString(R.string.add_fp_tips));
                }
                break;

            default:
                break;
        }
    }

    @Override
    public View initView() {
        mFpView = View.inflate(mActivity, R.layout.fragment_device_key, null);
        mAddBtn = mFpView.findViewById(R.id.btn_add);
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
        mDefaultDevice = DeviceInfoDao.getInstance(mFpView.getContext()).queryFirstData("device_default", true);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mBleManagerHelper = BleManagerHelper.getInstance(mFpView.getContext(), false);

        mFpAdapter = new FpManagerAdapter(mFpView.getContext());
        mListView.setLayoutManager(new LinearLayoutManager(mFpView.getContext(), LinearLayoutManager.VERTICAL, false));
        mListView.setItemAnimator(new DefaultItemAnimator());
        mListView.setAdapter(mFpAdapter);
        LogUtil.d(TAG, "tempUser 1= " + (mTempUser == null ? true : mTempUser.toString()));

        mAddBtn.setVisibility(View.VISIBLE);
        mAddBtn.setText(R.string.add_fingerprint);

        initEvent();

        mLoadDialog = DialogUtils.createLoadingDialog(mFpView.getContext(), mFpView.getContext().getResources().getString(R.string.data_loading));
        LocalBroadcastManager.getInstance(mFpView.getContext()).registerReceiver(fpReceiver, intentFilter());
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

    private final BroadcastReceiver fpReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //MSG1E 设备->apk，返回信息
            if (action.equals(BleMsg.STR_RSP_MSG1E_ERRCODE)) {
                final byte[] errCode = intent.getByteArrayExtra(BleMsg.KEY_ERROR_CODE);

                Log.d(TAG, "errCode[3] = " + errCode[3]);

                if (errCode[3] == 0x08) {
                    if (mFpAdapter.positionModify != -1) {
                        showMessage(mFpView.getContext().getResources().getString(R.string.modify_fp_failed));
                        mFpAdapter.positionModify = -1;
                    } else {
                        showMessage(mFpView.getContext().getResources().getString(R.string.add_fp_failed));
                    }

                } else if (errCode[3] == 0x09) {
                    showMessage(mFpView.getContext().getResources().getString(R.string.modify_fp_success));

                } else if (errCode[3] == 0x0a) {
                    showMessage(mFpView.getContext().getResources().getString(R.string.delete_fp_success));
                    mFpAdapter.removeItem(mFpAdapter.positionDelete);
                }
                DialogUtils.closeDialog(mLoadDialog);
                mHandler.removeCallbacks(mRunnable);
            }

            if (action.equals(BleMsg.STR_RSP_MSG1A_STATUS)) {

            }

            if (action.equals(BleMsg.STR_RSP_MSG16_LOCKID)) {
                DeviceKey key = (DeviceKey) intent.getExtras().getSerializable(BleMsg.KEY_SERIALIZABLE);
                if (key == null || (key.getKeyType() != ConstantUtil.USER_FINGERPRINT)) {
                    mHandler.removeCallbacks(mRunnable);
                    DialogUtils.closeDialog(mLoadDialog);
                    return;
                }

                final byte[] lockId = intent.getByteArrayExtra(BleMsg.KEY_LOCK_ID);
                LogUtil.d(TAG, "lockId = " + Arrays.toString(lockId));
                mLockId = String.valueOf(lockId[0]);
                LogUtil.d(TAG, "lockId = " + mLockId);
                DeviceKey deviceKey = new DeviceKey();
                deviceKey.setDeviceNodeId(mDefaultDevice.getDeviceNodeId());
                deviceKey.setUserId(mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId());
                deviceKey.setKeyActiveTime(System.currentTimeMillis() / 1000);
                deviceKey.setKeyName(mFpView.getContext().getResources().getString(R.string.fingerprint) + (Integer.parseInt(mLockId)));
                deviceKey.setKeyType(ConstantUtil.USER_FINGERPRINT);
                deviceKey.setLockId(mLockId);
                DeviceKeyDao.getInstance(mFpView.getContext()).insert(deviceKey);
                if (mTempUser != null) {
                    mTempUser.setUserStatus(ConstantUtil.USER_ENABLE);
                    DeviceUserDao.getInstance(mFpView.getContext()).updateDeviceUser(mTempUser);
                }

                mFpAdapter.addItem(deviceKey);
                DialogUtils.closeDialog(mLoadDialog);
                mHandler.removeCallbacks(mRunnable);
            }

            if (action.equals(BleMsg.STR_RSP_MSG18_TIMEOUT)) {
                Log.d(TAG, "STR_RSP_MSG18_TIMEOUT");
                byte[] seconds = intent.getByteArrayExtra(BleMsg.KEY_TIME_OUT);
                Log.d(TAG, "seconds = " + Arrays.toString(seconds));
                closeDialog((int) seconds[0]);
            }


        }
    };

    public class FpManagerAdapter extends RecyclerView.Adapter<FpManagerAdapter.ViewHolder> {
        private Context mContext;
        public ArrayList<DeviceKey> mFpList;
        public int positionDelete = -1;
        public int positionModify = -1;

        public FpManagerAdapter(Context context) {
            mContext = context;
            mFpList = DeviceKeyDao.getInstance(mFpView.getContext()).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_FINGERPRINT);
        }

        public void setDataSource(ArrayList<DeviceKey> cardList) {
            mFpList = cardList;
        }

        public void addItem(DeviceKey key) {
            mFpList.add(mFpList.size(), key);
            notifyItemInserted(mFpList.size());
        }

        public void removeItem(int index) {
            if (index != -1) {
                DeviceKey del = mFpList.remove(index);

                DeviceKeyDao.getInstance(mContext).delete(del);
                notifyItemRemoved(index);
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
            final DeviceKey fpInfo = mFpList.get(position);
            LogUtil.d(TAG, "fpInfo = " + fpInfo.toString());
            if (fpInfo != null) {
                viewHolder.mNameTv.setText(fpInfo.getKeyName());
                viewHolder.mType.setImageResource(R.mipmap.record_fingerprint);
                viewHolder.mCreateTime.setText(DateTimeUtil.timeStamp2Date(String.valueOf(fpInfo.getKeyActiveTime()), "yyyy-MM-dd HH:mm:ss"));

                viewHolder.mDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog .show();
                        closeDialog(10);
                        positionDelete = position;
                        mBleManagerHelper.getBleCardService().sendCmd15((byte) 1, (byte) 1, fpInfo.getUserId(), Byte.parseByte(fpInfo.getLockId()), String.valueOf(0));
                    }
                });
                viewHolder.mModifyLl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog.show();
                        closeDialog(10);
                        positionModify = position;
                        mBleManagerHelper.getBleCardService().sendCmd15((byte) 2, (byte) 1, fpInfo.getUserId(), Byte.parseByte(fpInfo.getLockId()), String.valueOf(0));
                    }
                });

                final AlertDialog editDialog = DialogUtils.showEditKeyDialog(mContext, mContext.getString(R.string.modify_note_name), fpInfo);
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
                            viewHolder.mNameTv.setText(DeviceKeyDao.getInstance(mContext).queryByLockId(fpInfo.getDeviceNodeId(), fpInfo.getUserId(), fpInfo.getLockId(), ConstantUtil.USER_FINGERPRINT).getKeyName());
                        }
                    });
                }
            }

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
        Log.d(TAG, "onResume");

        mFpAdapter.setDataSource(DeviceKeyDao.getInstance(mFpView.getContext()).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_FINGERPRINT));
        mFpAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(mFpView.getContext()).unregisterReceiver(fpReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
    }
}
