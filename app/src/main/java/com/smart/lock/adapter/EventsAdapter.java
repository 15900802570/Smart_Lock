package com.smart.lock.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.smart.lock.R;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceLog;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceLogDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DateTimeUtil;

import java.util.ArrayList;

public class EventsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public Context mContext;
    public ArrayList<DeviceLog> mLogList;
    private Boolean mVisible = false;
    public ArrayList<DeviceLog> mDeleteLogs = new ArrayList<>();
    private boolean mAllDelete = false;
    private static final int TYPE_HEADER = 0;  //说明是带有Header的
    private static final int TYPE_FOOTER = 1;  //说明是带有Footer的
    private static final int TYPE_NORMAL = 2;  //说明是不带有header和footer的
    private int countHead = 0;
    private int countFoot = 2;
    protected CheckBox mSelectCb;

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
            DeviceLogDao.getInstance(mContext).delete(del);
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

    private int getBodySize() {
        return mLogList.size();
    }

    private boolean isHead(int position) {
        return countHead != 0 && position < countHead;
    }

    private boolean isFoot(int position) {
        return countFoot != 0 && (position >= (getBodySize() + countHead));
    }

    @Override
    public int getItemViewType(int position) {
        if (isHead(position)) {
            return TYPE_HEADER;
        } else if (isFoot(position)) {
            return TYPE_FOOTER;
        } else {
            return TYPE_NORMAL;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_NORMAL:
                View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_event, parent, false);
                SwipeLayout swipeLayout = inflate.findViewById(R.id.item_ll_event);
                swipeLayout.setClickToClose(false);
                swipeLayout.setRightSwipeEnabled(false);
                return new MyViewHolder(inflate);
            case TYPE_FOOTER:
            case TYPE_HEADER:
            default:
                return new FootViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_recycle_foot, parent, false));
        }
    }

    @SuppressLint("SetTextI18n")
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, final int position) {
        if (getItemViewType(position) == TYPE_NORMAL) {
            final MyViewHolder mViewHolder = (MyViewHolder) viewHolder;
            final DeviceLog logInfo = mLogList.get(position);
            DeviceUser user = DeviceUserDao.getInstance(mContext).queryUser(logInfo.getNodeId(), logInfo.getUserId());
            DeviceInfo devInfo = DeviceInfoDao.getInstance(mContext).queryFirstData("device_nodeId", logInfo.getNodeId());
            // 用户名
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
            if (logInfo.getLogState() == BleMsg.STATE_ADD_LOCK_INFO) {
                mViewHolder.mEventIv.setImageResource(R.mipmap.icon_new_info);
                mViewHolder.mEventType.setTextColor(mContext.getResources().getColor(R.color.color_text));
                mViewHolder.mEventType.setText(R.string.add_unlock_info);
                if (logInfo.getLogType() == ConstantUtil.USER_PWD) {
                    mViewHolder.mEventInfo.setText(logUser + mContext.getString(R.string._de) + mContext.getString(R.string.password) + mContext.getString(R.string.add) + mContext.getString(R.string.successfully));
                } else if (logInfo.getLogType() == ConstantUtil.USER_FINGERPRINT) {
                    mViewHolder.mEventInfo.setText(logUser + mContext.getString(R.string._de) + mContext.getString(R.string.fingerprint) + mContext.getString(R.string.add) + mContext.getString(R.string.successfully));
                } else if (logInfo.getLogType() == ConstantUtil.USER_NFC) {
                    mViewHolder.mEventInfo.setText(logUser + mContext.getString(R.string._de) + mContext.getString(R.string.nfc) + mContext.getString(R.string.add) + mContext.getString(R.string.successfully));
                } else if (logInfo.getLogType() == ConstantUtil.USER_FACE) {
                    mViewHolder.mEventInfo.setText(logUser + mContext.getString(R.string._de) + mContext.getString(R.string.face_manager) + mContext.getString(R.string.add) + mContext.getString(R.string.successfully));
                } else {
                    mViewHolder.mEventInfo.setText(logUser + mContext.getString(R.string._de) + mContext.getString(R.string.unlock_key) + mContext.getString(R.string.add) + mContext.getString(R.string.successfully));
                }

            } else if (logInfo.getLogState() == BleMsg.STATE_DEL_LOCK_INFO) {
                mViewHolder.mEventIv.setImageResource(R.mipmap.icon_del_info);
                mViewHolder.mEventType.setText(R.string.del_unlock_info);
                mViewHolder.mEventType.setTextColor(mContext.getResources().getColor(R.color.red));
                if (logInfo.getLogType() == ConstantUtil.USER_PWD) {
                    mViewHolder.mEventInfo.setText(logUser + mContext.getString(R.string._de) + mContext.getString(R.string.password) + mContext.getString(R.string._deleted) + mContext.getString(R.string.successfully));
                } else if (logInfo.getLogType() == ConstantUtil.USER_FINGERPRINT) {
                    mViewHolder.mEventInfo.setText(logUser + mContext.getString(R.string._de) + mContext.getString(R.string.fingerprint) + mContext.getString(R.string._deleted) + mContext.getString(R.string.successfully));
                } else if (logInfo.getLogType() == ConstantUtil.USER_NFC) {
                    mViewHolder.mEventInfo.setText(logUser + mContext.getString(R.string._de) + mContext.getString(R.string.nfc) + mContext.getString(R.string._deleted) + mContext.getString(R.string.successfully));
                } else if (logInfo.getLogType() == ConstantUtil.USER_FACE) {
                    mViewHolder.mEventInfo.setText(logUser + mContext.getString(R.string._de) + mContext.getString(R.string.face_manager) + mContext.getString(R.string._deleted) + mContext.getString(R.string.successfully));
                } else {
                    mViewHolder.mEventInfo.setText(logUser + mContext.getString(R.string._de) + mContext.getString(R.string.unlock_key) + mContext.getString(R.string._deleted) + mContext.getString(R.string.successfully));
                }
            } else { // 门锁事件显示
                if (logInfo.getUserId() == 0 && Integer.parseInt(logInfo.getLockId()) == -3) {
                    mViewHolder.mEventIv.setImageResource(R.mipmap.icon_warning_log);
                    mViewHolder.mEventType.setText(R.string.anomalous_event);
                    mViewHolder.mEventType.setTextColor(mContext.getResources().getColor(R.color.red));
                    mViewHolder.mEventInfo.setText("门锁多次验证失败");
                } else {
                    mViewHolder.mEventIv.setImageResource(R.mipmap.icon_event);
                    mViewHolder.mEventType.setTextColor(mContext.getResources().getColor(R.color.color_text));

                    mViewHolder.mEventType.setText(R.string.unlock_event);
                    if (logInfo.getLogType() == ConstantUtil.USER_PWD) {
                        mViewHolder.mEventInfo.setText(mContext.getString(R.string.the) + logUser + mContext.getString(R.string._through_zh) + mContext.getString(R.string._pwd_zh) + mContext.getString(R.string._unlocked) + devInfo.getDeviceName() + mContext.getString(R.string.by_pwd));
                    } else if (logInfo.getLogType() == ConstantUtil.USER_FINGERPRINT) {
                        mViewHolder.mEventInfo.setText(mContext.getString(R.string.the) + logUser + mContext.getString(R.string._through_zh) + mContext.getString(R.string._fingerprint_zh) + mContext.getString(R.string._unlocked) + devInfo.getDeviceName() + mContext.getString(R.string.by_fingerprint));
                    } else if (logInfo.getLogType() == ConstantUtil.USER_NFC) {
                        mViewHolder.mEventInfo.setText(mContext.getString(R.string.the) + logUser + mContext.getString(R.string._through_zh) + mContext.getString(R.string._nfc_zh) + mContext.getString(R.string._unlocked) + devInfo.getDeviceName() + mContext.getString(R.string.by_nfc));
                    } else if (logInfo.getLogType() == ConstantUtil.USER_FACE) {
                        mViewHolder.mEventInfo.setText(mContext.getString(R.string.the) + logUser + mContext.getString(R.string._through_zh) + mContext.getString(R.string._face_zh) + mContext.getString(R.string._unlocked) + devInfo.getDeviceName() + mContext.getString(R.string.by_face));
                    } else if (logInfo.getLogType() == ConstantUtil.USER_REMOTE) {
                        mViewHolder.mEventInfo.setText(mContext.getString(R.string.the) + logUser + mContext.getString(R.string._remote_zh) + mContext.getString(R.string._unlocked) + devInfo.getDeviceName() + mContext.getString(R.string.remotely));
                    } else if (logInfo.getLogType() == ConstantUtil.USER_TEMP_PWD) {
                        mViewHolder.mEventInfo.setText(mContext.getString(R.string.temp_pwd) + " " + mContext.getString(R.string.open) + " " + devInfo.getDeviceName());
                    } else if (logInfo.getLogType() == ConstantUtil.USER_COMBINATION_LOCK) {
                        mViewHolder.mEventInfo.setText(mContext.getString(R.string.the) + logUser + mContext.getString(R.string._combination_lock_zh) + mContext.getString(R.string._unlocked) + devInfo.getDeviceName() + mContext.getString(R.string._combination_lock));
                    }
                }
            }
            mViewHolder.mTime.setText(DateTimeUtil.timeStamp2Date(String.valueOf(logInfo.getLogTime()), "yyyy-MM-dd HH:mm:ss"));

            mViewHolder.mDeleteCb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mViewHolder.mDeleteCb.isChecked()) {
                        mDeleteLogs.add(logInfo);
                    } else {
                        mDeleteLogs.remove(logInfo);
                    }
                }
            });
            if (mVisible)
                mViewHolder.mDeleteRl.setVisibility(View.VISIBLE);
            else
                mViewHolder.mDeleteRl.setVisibility(View.GONE);

            mViewHolder.mDeleteCb.setChecked(mAllDelete);
            if(mDeleteLogs!=null && mDeleteLogs.indexOf(logInfo)!=-1){
                mViewHolder.mDeleteCb.setChecked(true);
            }

        }
    }

    @Override
    public int getItemCount() {
        return mLogList.size() + countHead + countFoot;
    }

    public void chooseItemDelete(boolean visible) {
        mVisible = visible;
    }

    public void chooseALLDelete(boolean allDelete) {
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
        ImageView mEventIv;

        MyViewHolder(View itemView) {
            super(itemView);
            mEventIv = itemView.findViewById(R.id.iv_event);
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

    private class FootViewHolder extends RecyclerView.ViewHolder {
        private FootViewHolder(View itemView) {
            super(itemView);
        }
    }
}
