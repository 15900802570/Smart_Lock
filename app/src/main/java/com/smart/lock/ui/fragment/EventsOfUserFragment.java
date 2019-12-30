package com.smart.lock.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.smart.lock.R;
import com.smart.lock.adapter.EventsAdapter;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceLog;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.dao.DeviceLogDao;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.widget.SpacesItemDecoration;

import java.util.ArrayList;

public class EventsOfUserFragment extends BaseFragment {
    private final static String TAG = EventsOfUserFragment.class.getSimpleName();

    private View mEventsOfUserView;
    private RecyclerView mListView;

    private ArrayList<DeviceLog> mLogs;

    private EventsAdapter mEventsOfUserAdapter;
    private Context mCtx;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View initView() {
        mEventsOfUserView = View.inflate(mActivity, R.layout.fragment_events, null);
        mListView = mEventsOfUserView.findViewById(R.id.rv_events);
        return mEventsOfUserView;
    }

    @Override
    public void initDate() {
        mCtx = mEventsOfUserView.getContext();

        mLogs = new ArrayList<>();
        mEventsOfUserAdapter = new EventsAdapter(mCtx, mLogs);
        mBleManagerHelper = BleManagerHelper.getInstance(mCtx);

        mListView.setLayoutManager(new LinearLayoutManager(mCtx, LinearLayoutManager.VERTICAL, false));
        mListView.setItemAnimator(new DefaultItemAnimator());
        mListView.setAdapter(mEventsOfUserAdapter);
        mListView.addItemDecoration(new SpacesItemDecoration(getResources().getDimensionPixelSize(R.dimen.y5dp)));
        mLoadDialog = DialogUtils.createLoadingDialog(mCtx, mCtx.getResources().getString(R.string.data_loading));

    }

    public void editDelete() {
        mEventsOfUserAdapter.mDeleteLogs.clear();
        mEventsOfUserAdapter.chooseItemDelete(true);
        mEventsOfUserAdapter.notifyDataSetChanged();
    }

    public void doDelete(short userId) {
        if (mEventsOfUserAdapter.mDeleteLogs.size() != 0) {
//            if (mEventsOfUserAdapter.mAllDelete) {
//                mBleManagerHelper.getBleCardService().sendCmd33(BleMsg.TYPE_DELETE_ALL_USER_LOGS, userId, 0, BleMsg.INT_DEFAULT_TIMEOUT);
//            } else {
            for (DeviceLog devLog : mEventsOfUserAdapter.mDeleteLogs) {
                int logId = devLog.getLogId();
                mBleManagerHelper.getBleCardService().sendCmd33(BleMsg.TYPE_DELETE_SINGLE_USER_LOG, userId, logId, devLog, BleMsg.INT_DEFAULT_TIMEOUT);
            }
//            }
            mEventsOfUserAdapter.mDeleteLogs.clear();
            mEventsOfUserAdapter.chooseItemDelete(true);
            mEventsOfUserAdapter.notifyDataSetChanged();
        } else {
            showMessage(getString(R.string.plz_choise_del_log));
        }
    }

    public void cancelDelete() {
        mEventsOfUserAdapter.mDeleteLogs.clear();
        mEventsOfUserAdapter.chooseItemDelete(false);
        mEventsOfUserAdapter.notifyDataSetChanged();
    }

    public void selectedAll() {
        mEventsOfUserAdapter.mDeleteLogs.addAll(mEventsOfUserAdapter.mLogList);
        mEventsOfUserAdapter.chooseALLDelete(true);
        mEventsOfUserAdapter.notifyDataSetChanged();
    }

    public void cancelSelectedAll() {
        mEventsOfUserAdapter.mDeleteLogs.clear();
        mEventsOfUserAdapter.chooseALLDelete(false);
        mEventsOfUserAdapter.notifyDataSetChanged();
    }

    public int getCounter() {
        return mEventsOfUserAdapter.getItemCount();
    }

    public void getLogsOver(String nodeId, DeviceInfo deviceInfo, DeviceUser deviceUser) {

        if (deviceUser.getUserPermission() == ConstantUtil.DEVICE_MASTER) {
            mLogs = DeviceLogDao.getInstance(mCtx).queryKeyUserEvent("node_id", nodeId);
        } else if (deviceUser.getUserPermission() == ConstantUtil.DEVICE_MEMBER) {
            mLogs = DeviceLogDao.getInstance(mCtx).queryUserLogUserEvent(nodeId, deviceInfo.getUserId());
        }
        LogUtil.d(TAG, "receiver size = " + mLogs.size());
        mEventsOfUserAdapter.setDataSource(mLogs);
        mEventsOfUserAdapter.notifyDataSetChanged();

    }

    public boolean delLogSuccess(DeviceUser deviceUser, Bundle bundle) {
        DeviceLog mDelDeVLog = (DeviceLog) bundle.getSerializable(BleMsg.KEY_SERIALIZABLE);
        if (mDelDeVLog != null) {
            mEventsOfUserAdapter.removeItem(mDelDeVLog);
        }
        if (mEventsOfUserAdapter.mDeleteLogs.size() == 0) {
            if (deviceUser.getUserPermission() == ConstantUtil.DEVICE_MASTER) {
                mLogs = DeviceLogDao.getInstance(mCtx).queryKeyUserEvent("node_id", mNodeId);
            } else if (deviceUser.getUserPermission() == ConstantUtil.DEVICE_MEMBER) {
                mLogs = DeviceLogDao.getInstance(mCtx).queryUserLogLockEvent(mNodeId, deviceUser.getUserId());
            }

            mEventsOfUserAdapter.setDataSource(mLogs);
            mEventsOfUserAdapter.notifyDataSetChanged();
            DialogUtils.closeDialog(mLoadDialog);
            return true;
        } else {
            return false;
        }
    }
}
