package com.smart.lock.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
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
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class CardManagerActivity extends BaseListViewActivity implements View.OnClickListener {
    private static final String TAG = "CardManagerActivity";

    private CardManagerAdapter mCardAdapter;
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
        mAddBtn.setText(R.string.add_card);

        initData();

    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.STR_RSP_SERVER_DATA);
        intentFilter.addAction(BleMsg.STR_RSP_MSG1E_ERRCODE);
        intentFilter.addAction(BleMsg.STR_RSP_MSG16_LOCKID);
        intentFilter.addAction(BleMsg.STR_RSP_MSG1A_STATUS);
        intentFilter.addAction(BleMsg.STR_RSP_MSG18_TIMEOUT);
        return intentFilter;
    }

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //MSG1E 设备->apk，返回信息
            if (action.equals(BleMsg.STR_RSP_MSG1E_ERRCODE)) {
                final byte[] errCode = intent.getByteArrayExtra(BleMsg.KEY_ERROR_CODE);
                DialogUtils.closeDialog(mLoadDialog);
                mHandler.removeCallbacks(mRunnable);
                Log.d(TAG, "errCode[3] = " + errCode[3]);

                if (errCode[3] == 0x0e) {
                    showMessage(CardManagerActivity.this.getResources().getString(R.string.add_nfc_failed));
                } else if (errCode[3] == 0x0f) {
                    showMessage(CardManagerActivity.this.getResources().getString(R.string.modify_nfc_success));
                } else if (errCode[3] == 0x10) {
                    showMessage(CardManagerActivity.this.getResources().getString(R.string.delete_nfc_success));
                    DeviceKeyDao.getInstance(CardManagerActivity.this).delete(mCardAdapter.mCardList.get(mCardAdapter.positionDelete));
                    mCardAdapter.setDataSource(DeviceKeyDao.getInstance(CardManagerActivity.this).queryDeviceKey(mNodeId, mDefaultDevice.getUserId(), ConstantUtil.USER_NFC));
                    mCardAdapter.notifyDataSetChanged();
                }

            }

            if (action.equals(BleMsg.STR_RSP_MSG16_LOCKID)) {
                DialogUtils.closeDialog(mLoadDialog);
                mHandler.removeCallbacks(mRunnable);
                final byte[] lockId = intent.getByteArrayExtra(BleMsg.KEY_LOCK_ID);
                LogUtil.d(TAG, "lockId = " + Arrays.toString(lockId));
                mLockId = String.valueOf(lockId[0]);
                LogUtil.d(TAG, "lockId = " + mLockId);
                DeviceKey deviceKey = new DeviceKey();
                deviceKey.setDeviceNodeId(mDefaultDevice.getDeviceNodeId());
                deviceKey.setUserId(mDefaultDevice.getUserId());
                deviceKey.setKeyActiveTime(System.currentTimeMillis() / 1000);
                deviceKey.setKeyName("NFC" + (Integer.parseInt(mLockId)));
                deviceKey.setKeyType(ConstantUtil.USER_NFC);
                deviceKey.setLockId(mLockId);
                DeviceKeyDao.getInstance(CardManagerActivity.this).insert(deviceKey);
                mCardAdapter.setDataSource(DeviceKeyDao.getInstance(CardManagerActivity.this).queryDeviceKey(mNodeId, mDefaultDevice.getUserId(), ConstantUtil.USER_NFC));
                mCardAdapter.notifyDataSetChanged();
            }

            if (action.equals(BleMsg.STR_RSP_MSG1A_STATUS)) {

            }

            if (action.equals(BleMsg.STR_RSP_MSG18_TIMEOUT)) {
                Log.d(TAG, "STR_RSP_MSG18_TIMEOUT");
                byte[] seconds = intent.getByteArrayExtra(BleMsg.KEY_TIME_OUT);
                Log.d(TAG, "seconds = " + Arrays.toString(seconds));
                if (!mLoadDialog.isShowing()) {
                    mLoadDialog.show();
                }
                closeDialog((int) seconds[0]);
            }
        }
    };

    private void initData() {
        mDefaultDevice = (DeviceInfo) getIntent().getSerializableExtra(BleMsg.KEY_DEFAULT_DEVICE);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mBleManagerHelper = BleManagerHelper.getInstance(this, false);
        mTitle.setText(R.string.card_manager);

        mCardAdapter = new CardManagerAdapter(this);
        mListView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mListView.setItemAnimator(new DefaultItemAnimator());
        mListView.setAdapter(mCardAdapter);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
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
                int count = DeviceKeyDao.getInstance(this).queryDeviceKey(mNodeId, mDefaultDevice.getUserId(), ConstantUtil.USER_NFC).size();

                if (count >= 0 && count < 1) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    closeDialog(15);
                    mBleManagerHelper.getBleCardService().sendCmd15((byte) 0, (byte) 2, mDefaultDevice.getUserId(), (byte) 0, String.valueOf(0));
                } else {
                    showMessage(getResources().getString(R.string.add_nfc_tips));
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

    public class CardManagerAdapter extends RecyclerView.Adapter<CardManagerAdapter.MyViewHolder> {
        private Context mContext;
        public ArrayList<DeviceKey> mCardList;
        public int positionDelete = -1;
        public int positionModify = -1;

        public CardManagerAdapter(Context context) {
            mContext = context;
            mCardList = DeviceKeyDao.getInstance(CardManagerActivity.this).queryDeviceKey(mNodeId, mDefaultDevice.getUserId(), ConstantUtil.USER_NFC);
        }

        public void setDataSource(ArrayList<DeviceKey> cardList) {
            mCardList = cardList;
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
            final DeviceKey cardInfo = mCardList.get(position);
            LogUtil.d(TAG, "cardInfo = " + cardInfo.toString());
            if (cardInfo != null) {
                viewHolder.mNameTv.setText(cardInfo.getKeyName());
                viewHolder.mType.setImageResource(R.mipmap.record_nfc);
                viewHolder.mCreateTime.setText(DateTimeUtil.timeStamp2Date(String.valueOf(cardInfo.getKeyActiveTime()), "yyyy-MM-dd HH:mm:ss"));
                viewHolder.mDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog.show();
                        closeDialog(10);
                        positionDelete = position;
                        mBleManagerHelper.getBleCardService().sendCmd15((byte) 1, (byte) 2, cardInfo.getUserId(), Byte.parseByte(cardInfo.getLockId()), String.valueOf(0));
                    }
                });

                viewHolder.mModifyLl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog.show();
                        closeDialog(10);
                        positionModify = position;
                        mBleManagerHelper.getBleCardService().sendCmd15((byte) 2, (byte) 2, cardInfo.getUserId(), Byte.parseByte(cardInfo.getLockId()), String.valueOf(0));
                    }
                });

                viewHolder.mEditIbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cardInfo.setKeyName(viewHolder.mNameTv.getText().toString().trim());
                        DeviceKeyDao.getInstance(CardManagerActivity.this).updateDeviceKey(cardInfo);
                    }
                });
            }

        }

        @Override
        public int getItemCount() {
            return mCardList.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {

            SwipeLayout mSwipeLayout;
            TextView mNameTv;
            ImageView mType;
            TextView mCreateTime;
            LinearLayout mDelete;
            LinearLayout mModifyLl;
            ImageButton mEditIbtn;

            public MyViewHolder(View itemView) {
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
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
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
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}

