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
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.SpacesItemDecoration;

import java.util.ArrayList;
import java.util.Arrays;

public class CardFragment extends BaseFragment implements View.OnClickListener {
    private final static String TAG = CardFragment.class.getSimpleName();

    private View mCardView;
    private RecyclerView mListView;
    protected TextView mAddTv;

    private CardManagerAdapter mCardAdapter;
    private String mLockId = null;
    private boolean mIsVisibleFragment = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_add:
                int count = DeviceKeyDao.getInstance(mCardView.getContext()).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_NFC).size();
                LogUtil.d(TAG, "count = " + count);
                if (count >= 0 && count < 1) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    closeDialog(15);
                    mCt = mBleManagerHelper.getBleCardService().sendCmd15((byte) 0, (byte) 2, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), (byte) 0, String.valueOf(0),BleMsg.INT_DEFAULT_TIMEOUT);
                } else {
                    showMessage(getResources().getString(R.string.add_nfc_tips));
                }
                break;

            default:
                break;
        }
    }

    @Override
    public View initView() {
        mCardView = View.inflate(mActivity, R.layout.fragment_device_key, null);
        mAddTv = mCardView.findViewById(R.id.tv_add);
        Drawable top = getResources().getDrawable(R.mipmap.btn_add_card);
        mAddTv.setCompoundDrawablesWithIntrinsicBounds(null, top, null, null);
        mListView = mCardView.findViewById(R.id.rv_key);
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
        mBleManagerHelper = BleManagerHelper.getInstance(mCardView.getContext(), false);

        mCardAdapter = new CardManagerAdapter(mCardView.getContext());
        mListView.setLayoutManager(new LinearLayoutManager(mCardView.getContext(), LinearLayoutManager.VERTICAL, false));
        mListView.setItemAnimator(new DefaultItemAnimator());
        mListView.setAdapter(mCardAdapter);
        mListView.addItemDecoration(new SpacesItemDecoration(getResources().getDimensionPixelSize(R.dimen.y5dp)));
        LogUtil.d(TAG, "tempUser 1= " + (mTempUser == null ? true : mTempUser.toString()));

        mAddTv.setVisibility(View.VISIBLE);
        mAddTv.setText(R.string.add_card);

        initEvent();

        LocalBroadcastManager.getInstance(mCardView.getContext()).registerReceiver(cardReceiver, intentFilter());

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

    private final BroadcastReceiver cardReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //MSG1E 设备->apk，返回信息
            if (action.equals(BleMsg.STR_RSP_MSG1E_ERRCODE)) {
                final byte[] errCode = intent.getByteArrayExtra(BleMsg.KEY_ERROR_CODE);
                Log.d(TAG, "errCode[3] = " + errCode[3]);

                if (errCode[3] == 0x0e) {
                    showMessage(mCardView.getContext().getResources().getString(R.string.add_nfc_failed));
                } else if (errCode[3] == 0x0f) {
                    showMessage(mCardView.getContext().getResources().getString(R.string.modify_nfc_success));
                    mCardAdapter.notifyDataSetChanged();
                } else if (errCode[3] == 0x10) {
                    showMessage(mCardView.getContext().getResources().getString(R.string.delete_nfc_success));
                    mCardAdapter.removeItem(mCardAdapter.positionDelete);
                }

                DialogUtils.closeDialog(mLoadDialog);
                mHandler.removeCallbacks(mRunnable);
            }

            if (action.equals(BleMsg.STR_RSP_MSG16_LOCKID)) {
                DeviceKey key = (DeviceKey) intent.getExtras().getSerializable(BleMsg.KEY_SERIALIZABLE);
                LogUtil.d(TAG, "key = " + ((key == null) ? true : key.toString()));
                if (key == null || (key.getKeyType() != ConstantUtil.USER_NFC)) {
                    mHandler.removeCallbacks(mRunnable);
                    DialogUtils.closeDialog(mLoadDialog);
                    return;
                }

                final byte[] lockId = intent.getByteArrayExtra(BleMsg.KEY_LOCK_ID);
                LogUtil.d(TAG, "lockId = " + Arrays.toString(lockId));
                mLockId = String.valueOf(lockId[0]);
                DeviceKey deviceKey = new DeviceKey();
                deviceKey.setDeviceNodeId(mDefaultDevice.getDeviceNodeId());
                deviceKey.setUserId(mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId());
                deviceKey.setKeyActiveTime(System.currentTimeMillis() / 1000);
                deviceKey.setKeyName(mCardView.getContext().getString(R.string.me) + mCardView.getContext().getString(R.string.card) );
                deviceKey.setKeyType(ConstantUtil.USER_NFC);
                deviceKey.setLockId(mLockId);
                DeviceKeyDao.getInstance(mCardView.getContext()).insert(deviceKey);
                if (mTempUser != null) {
                    mTempUser.setUserStatus(ConstantUtil.USER_ENABLE);
                    DeviceUserDao.getInstance(mCardView.getContext()).updateDeviceUser(mTempUser);
                }
                mCardAdapter.addItem(deviceKey);
                DialogUtils.closeDialog(mLoadDialog);
                mHandler.removeCallbacks(mRunnable);
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
        LogUtil.d(TAG, "card isVisibleToUser = " + isVisibleToUser);
        mIsVisibleFragment = isVisibleToUser;
    }

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

        public void addItem(DeviceKey key) {
            mCardList.add(mCardList.size(), key);
            notifyItemInserted(mCardList.size());
        }

        public void removeItem(int index) {
            if (index != -1 && !mCardList.isEmpty()) {
                DeviceKey del = mCardList.remove(index);

                DeviceKeyDao.getInstance(mCardView.getContext()).delete(del);
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
            final DeviceKey cardInfo = mCardList.get(position);
            LogUtil.d(TAG, "cardInfo = " + cardInfo.toString());
            if (cardInfo != null) {
                viewHolder.mNameTv.setText(cardInfo.getKeyName());
                viewHolder.mType.setImageResource(R.mipmap.icon_card);
                viewHolder.mCreateTime.setText(DateTimeUtil.timeStamp2Date(String.valueOf(cardInfo.getKeyActiveTime()), "yyyy-MM-dd HH:mm:ss"));
                viewHolder.mEditorNameDialog = DialogUtils.createEditorDialog(getContext(), getString(R.string.modify_name), cardInfo.getKeyName());
                viewHolder.mDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog.show();
                        closeDialog(10);
                        positionDelete = position;
                        mCt = mBleManagerHelper.getBleCardService().sendCmd15((byte) 1, (byte) 2, cardInfo.getUserId(), Byte.parseByte(cardInfo.getLockId()), String.valueOf(0),BleMsg.INT_DEFAULT_TIMEOUT);
                    }
                });

                viewHolder.mModifyLl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog.show();
                        closeDialog(10);
                        positionModify = position;
                        mCt = mBleManagerHelper.getBleCardService().sendCmd15((byte) 2, (byte) 2, cardInfo.getUserId(), Byte.parseByte(cardInfo.getLockId()), String.valueOf(0),BleMsg.INT_DEFAULT_TIMEOUT);
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
                            cardInfo.setKeyName(newName);
                            DeviceKeyDao.getInstance(mActivity).updateDeviceKey(cardInfo);
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

        mCardAdapter.setDataSource(DeviceKeyDao.getInstance(mCardView.getContext()).queryDeviceKey(mNodeId, mTempUser == null ? mDefaultDevice.getUserId() : mTempUser.getUserId(), ConstantUtil.USER_NFC));
        mCardAdapter.notifyDataSetChanged();

        mLoadDialog = DialogUtils.createLoadingDialog(mCardView.getContext(), mCardView.getContext().getResources().getString(R.string.data_loading));

        mLoadDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_BACK) {
                    LogUtil.d(TAG, "按了返回键");
                    return true;
                }
                return false;
            }

        });
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
