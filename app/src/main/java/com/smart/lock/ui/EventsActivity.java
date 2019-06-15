package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
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
import com.smart.lock.ble.listener.UiListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceLog;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceLogDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.entity.Device;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class EventsActivity extends BaseListViewActivity implements View.OnClickListener, UiListener {
    private static final String TAG = "EventsActivity";

    private EventsAdapter mEventAdapter;
    private LinearLayoutManager mLinearManager;
    /**
     * 日志集合
     */
    private ArrayList<DeviceLog> mLogs;

    private DeviceUser mDeviceUser; //当前设备用户
    private DeviceLog mDelDeVLog; //删除的日志
    private Device mDevice;
    private int count = 0;
    private Context mCtx;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case BleMsg.DISPACTH_MSG_3E:
                    Bundle bundle = msg.getData();
                    dispatchErrorCode(bundle);
                    break;
                case BleMsg.RECEIVER_LOGS:
                    mCountTv.setText(String.valueOf(count));
                    break;
                default:
                    break;
            }

        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTitle.setText(R.string.title_event);
        initData();
        mRefreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                refreshLayout.finishRefresh(5000/*,false*/);//传入false表示刷新失败
                Log.d(TAG, "setOnRefreshListener");
            }

        });

    }

    /**
     * 初始化数据
     */
    private void initData() {
        mCtx = this;
        mBack.setVisibility(View.VISIBLE);
        mDefaultDevice = (DeviceInfo) getIntent().getSerializableExtra(BleMsg.KEY_DEFAULT_DEVICE);
        mNodeId = mDefaultDevice.getDeviceNodeId();
        mDeviceUser = DeviceUserDao.getInstance(this).queryUser(mNodeId, mDefaultDevice.getUserId());

        mBleManagerHelper = BleManagerHelper.getInstance(this);
        mBleManagerHelper.addUiListener(this);
        mDevice = Device.getInstance(this);
        DeviceLogDao.getInstance(this).deleteAll();

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
                    mEventAdapter.mDeleteLogs.addAll(mEventAdapter.mLogList);
                } else {
                    mTipTv.setText(R.string.all_election);
                    mEventAdapter.mDeleteLogs.clear();
                }
                mEventAdapter.chioseALLDelete(isChecked);
                mEventAdapter.notifyDataSetChanged();
            }
        });

        mLoadDialog = DialogUtils.createLoadingDialog(this, mCtx.getString(R.string.data_loading));

        if (mDevice.getState() == Device.BLE_CONNECTED) {
            mLoadDialog.show();
            if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MASTER) {
                mBleManagerHelper.getBleCardService().sendCmd31(BleMsg.TYPE_QUERY_ALL_USERS_LOG, mDefaultDevice.getUserId());
            } else if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MEMBER) {
                mBleManagerHelper.getBleCardService().sendCmd31(BleMsg.TYPE_QUERY_USER_LOG, mDefaultDevice.getUserId());
            }
        } else {
            showMessage(getString(R.string.disconnect_ble));
            finish();
        }
    }


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
                        mLogs = DeviceLogDao.getInstance(mCtx).queryKey("node_id", mNodeId);
                    } else if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MEMBER) {
                        mLogs = DeviceLogDao.getInstance(mCtx).queryUserLog(mNodeId, mDefaultDevice.getUserId());
                    }

                    mEventAdapter.setDataSource(mLogs);
                    mEventAdapter.notifyDataSetChanged();
                }

                break;
            case R.id.del_tv:
                if (mDevice.getState() == Device.BLE_CONNECTED) {
                    if (mEventAdapter.mDeleteLogs.size() != 0) {
                        DialogUtils.closeDialog(mLoadDialog);
                        mLoadDialog.show();
                        for (DeviceLog devLog : mEventAdapter.mDeleteLogs) {
                            int logId = devLog.getLogId();
                            mBleManagerHelper.getBleCardService().sendCmd33(BleMsg.TYPE_DELETE_LOG, mDefaultDevice.getUserId(), logId, devLog, BleMsg.INT_DEFAULT_TIMEOUT);
                        }
                    } else {
                        showMessage(getString(R.string.plz_choise_del_log));
                    }
                } else {
                    showMessage(getString(R.string.disconnect_ble));
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

    @Override
    public void deviceStateChange(Device device, int state) {
        mDevice = device;
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
    public void dispatchUiCallback(Message msg, Device device, int type) {
        mDevice = device;
        Bundle bundle = msg.getData();
        switch (msg.getType()) {
            case Message.TYPE_BLE_RECEIVER_CMD_3E:
                final byte[] errCode = msg.getData().getByteArray(BleMsg.KEY_ERROR_CODE);
                if (errCode != null) {
                    bundle.putByte(BleMsg.KEY_ERROR_CODE, errCode[3]);
                    dispatchErrorCode(bundle);
                }
                break;
            case Message.TYPE_BLE_RECEIVER_CMD_32:
                count++;
                LogUtil.d(TAG, "receiver " + count + " log!");
                android.os.Message message = new android.os.Message();
                message.what = type;
                mHandler.sendMessage(message);
                break;
            default:
                break;

        }
    }

    //分发MSG 3E
    private void dispatchErrorCode(Bundle bundle) {
        byte errCode = bundle.getByte(BleMsg.KEY_ERROR_CODE);
        LogUtil.i(TAG, "errCode : " + errCode);
        switch (errCode) {
            case BleMsg.TYPE_RECEIVER_LOGS_OVER:
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                DialogUtils.closeDialog(mLoadDialog);
                if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MASTER) {
                    mLogs = DeviceLogDao.getInstance(mCtx).queryKey("node_id", mNodeId);
                } else if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MEMBER) {
                    mLogs = DeviceLogDao.getInstance(mCtx).queryUserLog(mNodeId, mDefaultDevice.getUserId());
                }

                mEventAdapter.setDataSource(mLogs);
                mEventAdapter.notifyDataSetChanged();
                break;
            case BleMsg.TYPE_DELETE_LOG_SUCCESS:
                mDelDeVLog = (DeviceLog) bundle.getSerializable(BleMsg.KEY_SERIALIZABLE);
                if (mDelDeVLog != null) {
                    mEventAdapter.removeItem(mDelDeVLog);
                }

                if (mEventAdapter.mDeleteLogs.size() == 0) {

                    changeVisible(false);
                    mEventAdapter.mDeleteLogs.clear();
                    mEventAdapter.chioseALLDelete(false);
                    mSelectCb.setChecked(false);

                    if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MASTER) {
                        mLogs = DeviceLogDao.getInstance(mCtx).queryKey("node_id", mNodeId);
                    } else if (mDeviceUser.getUserPermission() == ConstantUtil.DEVICE_MEMBER) {
                        mLogs = DeviceLogDao.getInstance(mCtx).queryUserLog(mNodeId, mDefaultDevice.getUserId());
                    }

                    mEventAdapter.setDataSource(mLogs);
                    mEventAdapter.notifyDataSetChanged();
                    DialogUtils.closeDialog(mLoadDialog);

                }
                break;
            case BleMsg.TYPE_DELETE_LOG_FAILED:
                showMessage(mCtx.getString(R.string.del_log_failed));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            case BleMsg.TYPE_NO_AUTHORITY_3E:
                showMessage(mCtx.getString(R.string.no_authority));
                DialogUtils.closeDialog(mLoadDialog);
                break;
            default:
                break;

        }
    }

    @Override
    public void scanDevFailed() {

    }

    @Override
    public void reConnectBle(Device device) {

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

        public void removeItem(DeviceLog delLog) {

            int index = -1;
            int delIndex = -1;
            for (DeviceLog log : mLogList) {
                if (log.getLogId() == delLog.getLogId()) {
                    index = mLogList.indexOf(log);
                }
            }
            if (index != -1) {
                DeviceLog del = mLogList.remove(index);

                mDeleteLogs.remove(del);
                DeviceLogDao.getInstance(mCtx).delete(del);
                notifyItemRemoved(index);
            }

            for (DeviceLog log : mDeleteLogs) {
                if (log.getLogId() == delLog.getLogId()) {
                    delIndex = mDeleteLogs.indexOf(log);
                }
            }

            if (delIndex != -1) {
                mDeleteLogs.remove(delIndex);
            }

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
                    logUser = user.getUserName();
                } else {
                    if (logInfo.getUserId() == 0) {
                        logUser = mContext.getString(R.string.administrator);
                    } else if (logInfo.getUserId() < 99) { //管理员编号
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
                    viewHolder.mEventInfo.setText(logUser + mContext.getString(R.string.use) + mContext.getString(R.string.card) + mContext.getString(R.string.open) + devInfo.getDeviceName());
                } else if (logInfo.getLogType() == ConstantUtil.USER_REMOTE) {
                    viewHolder.mEventInfo.setText(logUser + mContext.getString(R.string.use) + mContext.getString(R.string.remote) + mContext.getString(R.string.open) + devInfo.getDeviceName());
                } else if (logInfo.getLogType() == ConstantUtil.USER_TEMP_PWD) {
                    viewHolder.mEventInfo.setText(mContext.getString(R.string.temp_pwd) + mContext.getString(R.string.open) + devInfo.getDeviceName());
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
        mBleManagerHelper.removeUiListener(this);
        DialogUtils.closeDialog(mLoadDialog);
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
