package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadMoreListener;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;
import com.scwang.smartrefresh.layout.listener.OnRefreshLoadMoreListener;
import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceLog;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceLogDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class EventsActivity extends BaseListViewActivity implements View.OnClickListener {
    private static final String TAG = "EventsActivity";

    private EventsAdapter mEventAdapter;
    private int mBaseCount = 10;

    private LinearLayoutManager mLinearManager;
    /**
     * 日志集合
     */
    private ArrayList<DeviceLog> mLogs;

    private DeviceUser mDeviceUser; //当前设备用户
    private DeviceLog mDelDeVLog; //删除的日志


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTitle.setText(R.string.title_event);

        LocalBroadcastManager.getInstance(this).registerReceiver(eventReceiver, intentFilter());

        initData();

        mRefreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                refreshLayout.finishRefresh(5000/*,false*/);//传入false表示刷新失败
                Log.d(TAG, "setOnRefreshListener");
            }

        });

    }

    private void setFooterView(RecyclerView view) {
        View footer = LayoutInflater.from(this).inflate(R.layout.footer_item, view, false);
        mEventAdapter.setFooterView(footer);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        mBack.setVisibility(View.VISIBLE);
        mDefaultDevice = (DeviceInfo) getIntent().getSerializableExtra(BleMsg.KEY_DEFAULT_DEVICE);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mDeviceUser = DeviceUserDao.getInstance(this).queryUser(mNodeId, mDefaultDevice.getUserId());

        mBleManagerHelper = BleManagerHelper.getInstance(this, false);

        DeviceLogDao.getInstance(this).deleteAll();

        mLoadDialog = DialogUtils.createLoadingDialog(EventsActivity.this, EventsActivity.this.getResources().getString(R.string.data_loading));
        mLoadDialog.show();
        closeDialog(60);

        LogUtil.i(TAG, "mDeviceUser = " + mDeviceUser.getUserPermission());

        if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MASTER) {
            mBleManagerHelper.getBleCardService().sendCmd31((byte) 1, mDefaultDevice.getUserId());
        } else if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MEMBER) {
            mBleManagerHelper.getBleCardService().sendCmd31((byte) 0, mDefaultDevice.getUserId());
        }

        mLogs = new ArrayList<>();
        mEventAdapter = new EventsAdapter(this, mLogs);
        mLinearManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mListView.setLayoutManager(mLinearManager);
        mListView.setItemAnimator(new DefaultItemAnimator());
        mListView.setAdapter(mEventAdapter);

        mSelectCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mTipTv.setText(R.string.cancel);
                    for (DeviceLog log : mEventAdapter.mLogList) {
                        mEventAdapter.mDeleteLogs.add(log);
                        Log.d(TAG, "add = " + log.getLogId());
                    }
                } else {
                    mTipTv.setText(R.string.all_election);
                    mEventAdapter.mDeleteLogs.clear();
                }
                mEventAdapter.chioseALLDelete(isChecked);
                mEventAdapter.notifyDataSetChanged();
            }
        });

    }

    private static IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.STR_RSP_SERVER_DATA);
        intentFilter.addAction(BleMsg.STR_RSP_MSG3E_ERROR);
        intentFilter.addAction(BleMsg.STR_RSP_MSG32_LOG);
        return intentFilter;
    }

    int count = 0;
    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BleMsg.STR_RSP_MSG32_LOG)) {
                final byte[] log = intent.getByteArrayExtra(BleMsg.KEY_LOG);
                count++;
                LogUtil.d(TAG, "receiver " + count + " log!");

                byte[] userId = new byte[2];
                byte[] logId = new byte[4];
                byte[] time = new byte[4];
                byte[] nodeId = new byte[8];
                byte[] state = new byte[1];
                byte[] type = new byte[1];
                byte[] lockId = new byte[1];

                System.arraycopy(log, 0, userId, 0, 2);
                System.arraycopy(log, 2, logId, 0, 4);
                System.arraycopy(log, 6, time, 0, 4);
                System.arraycopy(log, 10, nodeId, 0, 8);
                System.arraycopy(log, 18, state, 0, 1);
                System.arraycopy(log, 19, type, 0, 1);
                System.arraycopy(log, 20, lockId, 0, 1);

                StringUtil.exchange(nodeId);

                LogUtil.d(TAG, "userId = " + Arrays.toString(userId));
                LogUtil.d(TAG, "logId = " + Arrays.toString(logId));
                LogUtil.d(TAG, "time = " + Arrays.toString(time));
                LogUtil.d(TAG, "nodeId = " + Arrays.toString(nodeId));
                LogUtil.d(TAG, "state = " + Arrays.toString(state));
                LogUtil.d(TAG, "type = " + Arrays.toString(type));
                LogUtil.d(TAG, "lockId = " + Arrays.toString(lockId));

                DeviceLog devLog = new DeviceLog();
                devLog.setUserId(Short.parseShort(StringUtil.bytesToHexString(userId), 16));
                devLog.setLogId(Integer.parseInt(StringUtil.bytesToHexString(logId), 16));
                devLog.setLogTime(Long.parseLong(StringUtil.bytesToHexString(time), 16));
                devLog.setNodeId(StringUtil.bytesToHexString(nodeId));
                devLog.setLogState(state[0]);
                String keyName = "";

                devLog.setLogType(type[0]);
                if (type[0] == 0) {
                    keyName = EventsActivity.this.getResources().getString(R.string.password);
                } else if (type[0] == 1) {
                    keyName = EventsActivity.this.getResources().getString(R.string.fingerprint);
                } else if (type[0] == 2) {
                    keyName = "NFC";
                } else {
                    keyName = EventsActivity.this.getResources().getString(R.string.remote);
                }

                devLog.setLockId(String.valueOf(lockId[0]));
                DeviceKey deviceKey = DeviceKeyDao.getInstance(EventsActivity.this).queryByLockId(StringUtil.bytesToHexString(nodeId), StringUtil.bytesToHexString(userId), String.valueOf(lockId[0]), type[0]);
                if (deviceKey != null) {
                    devLog.setKeyName(deviceKey.getKeyName());
                } else
                    devLog.setKeyName(keyName + String.valueOf(lockId[0]));
                LogUtil.d(TAG, "devLog = " + devLog.toString());

                DeviceLogDao.getInstance(EventsActivity.this).insert(devLog);
            }

            if (action.equals(BleMsg.STR_RSP_MSG3E_ERROR)) {

                final byte[] errCode = intent.getByteArrayExtra(BleMsg.KEY_ERROR_CODE);
                Log.d(TAG, "errCode[3] = " + errCode[3]);

                if (errCode[3] == 0x00) {
                    if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MASTER) {
                        mLogs = DeviceLogDao.getInstance(EventsActivity.this).queryKey("node_id", mNodeId);
                    } else if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MEMBER) {
                        mLogs = DeviceLogDao.getInstance(EventsActivity.this).queryUserLog(mNodeId, mDefaultDevice.getUserId());
                    }
                    mEventAdapter.setDataSource(mLogs);
                    mEventAdapter.notifyDataSetChanged();
                    DialogUtils.closeDialog(mLoadDialog);
                    mHandler.removeCallbacks(mRunnable);

                } else if (errCode[3] == 0x01) {

                    mDelDeVLog = (DeviceLog) intent.getSerializableExtra(BleMsg.KEY_SERIALIZABLE);
                    LogUtil.d(TAG, "mDelDeVLog = " + mDelDeVLog.toString());
                    if (mDelDeVLog != null) {
                        DeviceLogDao.getInstance(EventsActivity.this).delete(mDelDeVLog);
                        int position = mEventAdapter.mLogList.indexOf(mDelDeVLog);
                        mEventAdapter.mLogList.remove(position);
                        mEventAdapter.notifyItemRemoved(position);
                        mEventAdapter.mDeleteLogs.remove(mDelDeVLog);
                    }

                    if (mEventAdapter.mDeleteLogs.size() == 0) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mHandler.removeCallbacks(mRunnable);
                    }

                } else if (errCode[3] == 0x02) {
                    showMessage(EventsActivity.this.getString(R.string.del_log_failed));
                    DialogUtils.closeDialog(mLoadDialog);
                    mHandler.removeCallbacks(mRunnable);
                } else if (errCode[3] == 0x03) {
                    showMessage(EventsActivity.this.getString(R.string.no_authority));
                    DialogUtils.closeDialog(mLoadDialog);
                    mHandler.removeCallbacks(mRunnable);
                }

            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back_sysset:
                finish();
                break;
            case R.id.edit_tv:
                if (mEditTv.getText().toString().equals(getString(R.string.edit))) {
                    changeVisible(true);
                    mEventAdapter.mDeleteLogs.clear();
                    mEventAdapter.notifyDataSetChanged();
                } else {
                    changeVisible(false);
                    mEventAdapter.mDeleteLogs.clear();
                    mEventAdapter.chioseALLDelete(false);
                    mSelectCb.setChecked(false);

                    if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MASTER) {
                        mLogs = DeviceLogDao.getInstance(EventsActivity.this).queryKey("node_id", mNodeId);
                    } else if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MEMBER) {
                        mLogs = DeviceLogDao.getInstance(EventsActivity.this).queryUserLog(mNodeId, mDefaultDevice.getUserId());
                    }

                    mEventAdapter.setDataSource(mLogs);
                    mEventAdapter.notifyDataSetChanged();
                }

                break;
            case R.id.del_tv:
                if (mEventAdapter.mDeleteLogs.size() != 0) {
                    DialogUtils.closeDialog(mLoadDialog);
                    mLoadDialog.show();
                    for (DeviceLog devLog : mEventAdapter.mDeleteLogs) {
                        int logId = devLog.getLogId();
                        LogUtil.d(TAG, "logId = " + logId);
                        mCt = mBleManagerHelper.getBleCardService().sendCmd33((byte) 2, mDefaultDevice.getUserId(), logId, devLog);
                    }
                    closeDialog(10);
                } else {
                    showMessage(getString(R.string.plz_choise_del_log));
                }
                break;

            default:
                break;
        }
    }

    /**
     * 状态显示控件
     *
     * @param delete 是否删除
     */
    private void changeVisible(boolean delete) {
        mEventAdapter.chioseItemDelete(delete);
        if (delete) {
            mEditTv.setText(R.string.edit_back);
            mSelectEventRl.setVisibility(View.VISIBLE);
        } else {
            mEditTv.setText(R.string.edit);
            mSelectEventRl.setVisibility(View.GONE);

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

    /**
     * 事件适配器
     */
    public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.MyViewHolder> {
        private Context mContext;
        public ArrayList<DeviceLog> mLogList;
        private Boolean mVisiBle = false;
        public ArrayList<DeviceLog> mDeleteLogs = new ArrayList<>();
        public boolean mAllDelete = false;
        public static final int TYPE_HEADER = 0;  //说明是带有Header的
        public static final int TYPE_FOOTER = 1;  //说明是带有Footer的
        public static final int TYPE_NORMAL = 2;  //说明是不带有header和footer的
        private View mHeaderView;
        private View mFooterView;

        public EventsAdapter(Context context, ArrayList<DeviceLog> loglist) {
            mContext = context;
            mLogList = loglist;
        }

        public void setDataSource(ArrayList<DeviceLog> logList) {
            if (!logList.isEmpty()) {
                mLogList = logList;
            }
        }

        public View getHeaderView() {
            return mHeaderView;
        }

        public void setHeaderView(View headerView) {
            mHeaderView = headerView;
            notifyItemInserted(0);
        }

        public View getFooterView() {
            return mFooterView;
        }

        public void setFooterView(View footerView) {
            mFooterView = footerView;
            LogUtil.d(TAG, "getItemCount = " + getItemCount());
            notifyItemInserted(getItemCount() - 1);
        }

        @Override
        public int getItemViewType(int position) {
            if (mHeaderView == null && mFooterView == null) {
                return TYPE_NORMAL;
            }
            if (position == 0) {
                //第一个item应该加载Header
                return TYPE_HEADER;
            }
            if (position == getItemCount() - 1) {
                //最后一个,应该加载Footer
                return TYPE_FOOTER;
            }
            return TYPE_NORMAL;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (mHeaderView != null && viewType == TYPE_HEADER) {
                return new MyViewHolder(mHeaderView);
            }
            if (mFooterView != null && viewType == TYPE_FOOTER) {
                return new MyViewHolder(mFooterView);
            }
            View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_event, parent, false);
            SwipeLayout swipeLayout = inflate.findViewById(R.id.item_ll_event);
            swipeLayout.setClickToClose(false);
            swipeLayout.setRightSwipeEnabled(false);
            return new MyViewHolder(inflate);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(final MyViewHolder viewHolder, final int position) {
            if (getItemViewType(position) == TYPE_NORMAL) {
                final DeviceLog logInfo = mLogList.get(position);
                DeviceUser user = DeviceUserDao.getInstance(mContext).queryUser(logInfo.getNodeId(), logInfo.getUserId());
                DeviceInfo devInfo = DeviceInfoDao.getInstance(mContext).queryFirstData("device_nodeId", logInfo.getNodeId());
                String logUser = mContext.getString(R.string.administrator);
                if (user != null) {
                    LogUtil.d(TAG, "user : " + user.toString());
                    logUser = user.getUserName();
                } else {
                    if (logInfo.getUserId() < 99) { //管理员编号
                        logUser = mContext.getString(R.string.administrator) + logInfo.getUserId();
                    } else if (logInfo.getUserId() >= 100 && logInfo.getUserId() < 200) { //普通用户
                        logUser = mContext.getString(R.string.members) + logInfo.getUserId();
                    } else if (logInfo.getUserId() >= 200 && logInfo.getUserId() < 300) { //临时用户
                        logUser = mContext.getString(R.string.tmp_user) + logInfo.getUserId();
                    }
                }
                if (logInfo.getLogType() == ConstantUtil.USER_PWD) {
                    viewHolder.mEventInfo.setText(logUser + mContext.getString(R.string.use) + mContext.getString(R.string.password) + mContext.getString(R.string.open) + devInfo.getDeviceName());
                } else if (logInfo.getLogType() == ConstantUtil.USER_FINGERPRINT) {
                    viewHolder.mEventInfo.setText(logUser + mContext.getString(R.string.use) + mContext.getString(R.string.fingerprint) + mContext.getString(R.string.open) + devInfo.getDeviceName());
                } else if (logInfo.getLogType() == ConstantUtil.USER_NFC) {
                    viewHolder.mEventInfo.setText(logUser + mContext.getString(R.string.use) + "NFC" + mContext.getString(R.string.open) + devInfo.getDeviceName());
                } else if (logInfo.getLogType() == ConstantUtil.USER_REMOTE) {
                    viewHolder.mEventInfo.setText(logUser + mContext.getString(R.string.use) + mContext.getString(R.string.remote) + mContext.getString(R.string.open) + devInfo.getDeviceName());
                }

                viewHolder.mTime.setText(DateTimeUtil.timeStamp2Date(String.valueOf(logInfo.getLogTime()), "yyyy-MM-dd HH:mm:ss"));

                viewHolder.mDeleteCb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (viewHolder.mDeleteCb.isChecked()) {
                            Log.d(TAG, "add logid : " + logInfo.getLogId() + " position = " + position);
                            mDeleteLogs.add(logInfo);
                        } else {
                            Log.d(TAG, "remove logid = " + logInfo.getLogId() + " position = " + position);
                            mDeleteLogs.remove(logInfo);
                        }
                    }
                });

                if (mVisiBle)
                    viewHolder.mDeleteRl.setVisibility(View.VISIBLE);
                else
                    viewHolder.mDeleteRl.setVisibility(View.GONE);

                viewHolder.mDeleteCb.setChecked(mAllDelete);
            } else if (getItemViewType(position) == TYPE_HEADER) {

                return;
            } else {
                return;
            }

        }

        @Override
        public int getItemCount() {
            if (mHeaderView == null && mFooterView == null) {
                return mLogList.size();
            } else if (mHeaderView == null && mFooterView != null) {
                return mLogList.size() + 1;
            } else if (mHeaderView != null && mFooterView == null) {
                return mLogList.size() + 1;
            } else {
                return mLogList.size() + 2;
            }
        }

        public void chioseItemDelete(boolean visible) {
            mVisiBle = visible;
        }

        public void chioseALLDelete(boolean allDelete) {
            mAllDelete = allDelete;
        }

        class MyViewHolder extends RecyclerView.ViewHolder {

            SwipeLayout mSwipeLayout;
            TextView mEventInfo;
            TextView mEventType;
            LinearLayout mDelete;
            LinearLayout mModifyLl;
            TextView mTime;
            CheckBox mDeleteCb;
            RelativeLayout mDeleteRl;

            public MyViewHolder(View itemView) {
                super(itemView);
                if (itemView == mHeaderView) {
                    return;
                }
                if (itemView == mFooterView) {
                    return;
                }
                mSwipeLayout = (SwipeLayout) itemView;
                mEventInfo = itemView.findViewById(R.id.tv_event_info);
                mEventType = itemView.findViewById(R.id.tv_type);
                mDelete = itemView.findViewById(R.id.ll_delete);
                mModifyLl = itemView.findViewById(R.id.ll_modify);
                mTime = itemView.findViewById(R.id.tv_create_time);
                mDeleteCb = itemView.findViewById(R.id.delete_locked);
                mDeleteRl = itemView.findViewById(R.id.rl_delete);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(eventReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
