package com.smart.lock.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.EditText;
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
import com.smart.lock.ble.message.MessageCreator;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.ui.CardManagerActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class CardFragment extends BaseFragment implements View.OnClickListener {
    private final static String TAG = CardFragment.class.getSimpleName();


    private View mCardView;
    private RecyclerView mListView;
    protected Button mAddBtn;

    private CardManagerAdapter mCardAdapter;
    private String mLockId = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_add:
                int count = DeviceKeyDao.getInstance(mCardView.getContext()).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_NFC).size();
                if (count >= 0 && count <= 5) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog = DialogUtils.createLoadingDialog(mCardView.getContext(), getResources().getString(R.string.data_loading));
                    closeDialog(15);
                    mBleManagerHelper.getBleCardService().sendCmd15((byte) 0, (byte) 2, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), (byte) 0, 0);
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
        mCardView = View.inflate(mActivity, R.layout.fragment_user_manager, null);
        mAddBtn = mCardView.findViewById(R.id.btn_add);
        mListView = mCardView.findViewById(R.id.rv_users);
        return mCardView;
    }

    public void setTempUser(DeviceUser tempUser) {
        LogUtil.d(TAG, "tempUser = " + (tempUser == null ? true : tempUser.toString()));
        if (tempUser != null) {
            mTempUser = tempUser;
        } else
            LogUtil.d(TAG, "临时用户为空");

    }

    public void initDate() {
        mDefaultDevice = DeviceInfoDao.getInstance(mCardView.getContext()).queryFirstData("device_default", true);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mBleManagerHelper = BleManagerHelper.getInstance(mCardView.getContext(), mNodeId, false);

        mCardAdapter = new CardManagerAdapter(mCardView.getContext());
        mListView.setLayoutManager(new LinearLayoutManager(mCardView.getContext(), LinearLayoutManager.VERTICAL, false));
        mListView.setItemAnimator(new DefaultItemAnimator());
        mListView.setAdapter(mCardAdapter);
        LogUtil.d(TAG, "tempUser 1= " + (mTempUser == null ? true : mTempUser.toString()));

        mAddBtn.setVisibility(View.VISIBLE);
        mAddBtn.setText(R.string.add_card);

        initEvent();

        LocalBroadcastManager.getInstance(mCardView.getContext()).registerReceiver(cardReceiver, intentFilter());
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

    private final BroadcastReceiver cardReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //MSG1E 设备->apk，返回信息
            if (action.equals(BleMsg.STR_RSP_MSG1E_ERRCODE)) {
                final byte[] errCode = intent.getByteArrayExtra(BleMsg.KEY_ERROR_CODE);
                DialogUtils.closeDialog(mLoadDialog);
                mHandler.removeCallbacks(mRunnable);
                Log.d(TAG, "errCode[3] = " + errCode[3]);

                if (errCode[3] == 0x0e) {
                    showMessage(mCardView.getContext().getResources().getString(R.string.add_nfc_failed));
                } else if (errCode[3] == 0x0f) {
                    showMessage(mCardView.getContext().getResources().getString(R.string.modify_nfc_success));
                } else if (errCode[3] == 0x10) {
                    showMessage(mCardView.getContext().getResources().getString(R.string.delete_nfc_success));
                    DeviceKeyDao.getInstance(mCardView.getContext()).delete(mCardAdapter.mCardList.get(mCardAdapter.positionDelete));
                    mCardAdapter.setDataSource(DeviceKeyDao.getInstance(mCardView.getContext()).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_NFC));
                    mCardAdapter.notifyDataSetChanged();
                }

            }

            if (action.equals(BleMsg.STR_RSP_MSG16_LOCKID)) {
                DeviceKey key = (DeviceKey) intent.getExtras().getSerializable(BleMsg.KEY_SERIALIZABLE);
                if (key == null || (key.getKeyType() != 2)) {
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
                deviceKey.setKeyName("NFC" + (Integer.parseInt(mLockId)));
                deviceKey.setKeyType(ConstantUtil.USER_NFC);
                deviceKey.setLockId(mLockId);
                DeviceKeyDao.getInstance(mCardView.getContext()).insert(deviceKey);
                mCardAdapter.setDataSource(DeviceKeyDao.getInstance(mCardView.getContext()).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_NFC));
                mCardAdapter.notifyDataSetChanged();
                DialogUtils.closeDialog(mLoadDialog);
                mHandler.removeCallbacks(mRunnable);
            }

            if (action.equals(BleMsg.STR_RSP_MSG1A_STATUS)) {

            }

            if (action.equals(BleMsg.STR_RSP_MSG18_TIMEOUT)) {
                Log.d(TAG, "STR_RSP_MSG18_TIMEOUT");
                byte[] seconds = intent.getByteArrayExtra(BleMsg.KEY_TIME_OUT);
                Log.d(TAG, "seconds = " + Arrays.toString(seconds));
                closeDialog((int) seconds[0]);
            }
        }
    };

    public class CardManagerAdapter extends RecyclerView.Adapter<CardManagerAdapter.ViewHolder> {
        private Context mContext;
        public ArrayList<DeviceKey> mCardList;
        public int positionDelete = -1;
        public int positionModify = -1;

        public CardManagerAdapter(Context context) {
            mContext = context;
            mCardList = DeviceKeyDao.getInstance(mCardView.getContext()).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_NFC);
        }

        public void setDataSource(ArrayList<DeviceKey> cardList) {
            mCardList = cardList;
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
                        mLoadDialog = DialogUtils.createLoadingDialog(mCardView.getContext(), mCardView.getContext().getResources().getString(R.string.data_loading));
                        closeDialog(10);
                        positionDelete = position;
                        mBleManagerHelper.getBleCardService().sendCmd15((byte) 1, (byte) 2, cardInfo.getUserId(), Byte.parseByte(cardInfo.getLockId()), 0);
                    }
                });

                viewHolder.mModifyLl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog = DialogUtils.createLoadingDialog(mCardView.getContext(), mCardView.getContext().getResources().getString(R.string.data_loading));
                        closeDialog(10);
                        positionModify = position;
                        mBleManagerHelper.getBleCardService().sendCmd15((byte) 2, (byte) 2, cardInfo.getUserId(), Byte.parseByte(cardInfo.getLockId()), 0);
                    }
                });

                final AlertDialog editDialog = DialogUtils.showEditKeyDialog(mContext, mContext.getString(R.string.modify_note_name), cardInfo);
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
                            viewHolder.mNameTv.setText(DeviceKeyDao.getInstance(mContext).queryByLockId(cardInfo.getDeviceNodeId(), cardInfo.getUserId(), cardInfo.getLockId()).getKeyName());
                        }
                    });
                }
            }

        }

        @Override
        public int getItemCount() {
            return mCardList.size();
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

        mCardAdapter.setDataSource(DeviceKeyDao.getInstance(mCardView.getContext()).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_NFC));
        mCardAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(mCardView.getContext()).unregisterReceiver(cardReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
    }
}
