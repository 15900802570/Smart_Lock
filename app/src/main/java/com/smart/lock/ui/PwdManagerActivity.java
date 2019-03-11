package com.smart.lock.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class PwdManagerActivity extends BaseListViewActivity implements View.OnClickListener {
    private static final String TAG = "PwdManagerActivity";

    private PwdManagerAdapter mPwdAdapter;
    private String mNodeId;

    private String mLockId = null;

    /**
     * 蓝牙
     */
    private BleManagerHelper mBleManagerHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAddBtn.setVisibility(View.VISIBLE);
        mAddBtn.setText(R.string.add_password);

        initData();

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
                DialogUtils.closeDialog(mLoadDialog);
                mHandler.removeCallbacks(mRunnable);
                Log.d(TAG, "errCode[3] = " + errCode[3]);

                if (errCode[3] == 0x0d) {
                    showMessage(PwdManagerActivity.this.getResources().getString(R.string.delete_pwd_success));
                    DeviceKeyDao.getInstance(PwdManagerActivity.this).delete(mPwdAdapter.mPwdList.get(mPwdAdapter.positionDelete));
                    mPwdAdapter.setDataSource(DeviceKeyDao.getInstance(PwdManagerActivity.this).queryDeviceKey(mNodeId, mDefaultDevice.getDeviceUser(), "PWD"));
                    mPwdAdapter.notifyDataSetChanged();
                }

            }

            if (action.equals(BleMsg.STR_RSP_MSG1A_STATUS)) {

            }

            if (action.equals(BleMsg.STR_RSP_MSG18_TIMEOUT)) {
                Log.d(TAG, "STR_RSP_MSG18_TIMEOUT");
                byte[] seconds = intent.getByteArrayExtra(BleMsg.KEY_TIME_OUT);
                Log.d(TAG, "seconds = " + Arrays.toString(seconds));
                closeDialog((int) seconds[0], mHandler, mRunnable);
            }
        }
    };

    private void initData() {
        mDefaultDevice = (DeviceInfo) getIntent().getSerializableExtra(BleMsg.KEY_DEFAULT_DEVICE);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mBleManagerHelper = BleManagerHelper.getInstance(this, mNodeId, false);
        mTitle.setText(R.string.password_manager);

        mPwdAdapter = new PwdManagerAdapter(this);
        mListView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mListView.setItemAnimator(new DefaultItemAnimator());
        mListView.setAdapter(mPwdAdapter);

        LocalBroadcastManager.getInstance(this).registerReceiver(pwdReceiver, intentFilter());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back_sysset:
                finish();
                break;
            case R.id.tv_sync:

                break;
            case R.id.btn_add:
                int count = DeviceKeyDao.getInstance(this).queryDeviceKey(mNodeId, mDefaultDevice.getDeviceUser(), "PWD").size();

                if (count >= 0 && count < 1) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);
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

    /**
     * 超时提醒
     *
     * @param seconds
     */
    private void closeDialog(final int seconds) {

        mHandler.removeCallbacks(mRunnable);

        mHandler.postDelayed(mRunnable, seconds * 1000);
    }


    public class PwdManagerAdapter extends RecyclerView.Adapter<PwdManagerAdapter.MyViewHolder> {
        private Context mContext;
        public ArrayList<DeviceKey> mPwdList;
        public int positionDelete;

        public PwdManagerAdapter(Context context) {
            mContext = context;
            mPwdList = DeviceKeyDao.getInstance(PwdManagerActivity.this).queryDeviceKey(mNodeId, mDefaultDevice.getDeviceUser(), "PWD");
        }

        public void setDataSource(ArrayList<DeviceKey> cardList) {
            mPwdList = cardList;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_recycler, parent, false);
            SwipeLayout swipeLayout = inflate.findViewById(R.id.item_ll_recycler);
            swipeLayout.setClickToClose(true);
            swipeLayout.setRightSwipeEnabled(true);
            return new MyViewHolder(inflate);
        }

        @Override
        public void onBindViewHolder(final MyViewHolder viewHolder, final int position) {
            final DeviceKey pwdInfo = mPwdList.get(position);
            if (pwdInfo != null) {
                viewHolder.mName.setText(pwdInfo.getKeyName());
                viewHolder.mType.setImageResource(R.mipmap.record_pwd);
                viewHolder.mCreateTime.setText(DateTimeUtil.timeStamp2Date(String.valueOf(pwdInfo.getKeyActiveTime()), "yyyy-MM-dd HH:mm:ss"));
                viewHolder.mDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog = DialogUtils.createLoadingDialog(PwdManagerActivity.this, PwdManagerActivity.this.getResources().getString(R.string.data_loading));
                        closeDialog(10);
                        positionDelete = position;
                        mBleManagerHelper.getBleCardService().sendCmd15((byte) 1, (byte) 0, Short.parseShort(pwdInfo.getDeviceUserId()), Byte.parseByte(pwdInfo.getLockId()), Integer.parseInt(pwdInfo.getPwd()));
                    }
                });
                viewHolder.mModifyLl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle bundle = new Bundle();
                        bundle.putSerializable(BleMsg.KEY_MODIFY_DEVICE_KEY, pwdInfo);
                        bundle.putString(BleMsg.KEY_CMD_TYPE, ConstantUtil.MODIFY);
                        startIntent(PwdSetActivity.class, bundle);

                    }
                });

                viewHolder.mEditIbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pwdInfo.setKeyName(viewHolder.mName.getText().toString().trim());
                        DeviceKeyDao.getInstance(PwdManagerActivity.this).updateDeviceKey(pwdInfo);
                    }
                });

            }

        }

        @Override
        public int getItemCount() {
            return mPwdList.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {

            SwipeLayout mSwipeLayout;
            EditText mName;
            ImageView mType;
            TextView mCreateTime;
            LinearLayout mDelete;
            LinearLayout mModifyLl;
            ImageButton mEditIbtn;

            public MyViewHolder(View itemView) {
                super(itemView);

                mSwipeLayout = (SwipeLayout) itemView;
                mType = itemView.findViewById(R.id.iv_type);
                mName = itemView.findViewById(R.id.et_username);
                mDelete = itemView.findViewById(R.id.ll_delete);
                mModifyLl = itemView.findViewById(R.id.ll_modify);
                mCreateTime = itemView.findViewById(R.id.tv_create_time);
                mEditIbtn = itemView.findViewById(R.id.ib_edit);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(pwdReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBleManagerHelper.getServiceConnection()) {
            showMessage(getResources().getString(R.string.plz_reconnect));
            finish();
        }
        mPwdAdapter.setDataSource(DeviceKeyDao.getInstance(PwdManagerActivity.this).queryDeviceKey(mNodeId, mDefaultDevice.getDeviceUser(), "PWD"));
        mPwdAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}

