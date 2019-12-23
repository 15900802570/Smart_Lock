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

public class EventsOfLockFragment extends BaseFragment {

    private View mEventsOfLockView;
    private RecyclerView mListView;
    private ArrayList<DeviceLog> mLogs;
    private DeviceLog mDelDeVLog; //删除的日志

    private EventsAdapter mEventsOfLockAdapter;
    private Context mCtx;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View initView() {
        mEventsOfLockView = View.inflate(mActivity, R.layout.fragment_events, null);
        mListView = mEventsOfLockView.findViewById(R.id.rv_events);
        return mEventsOfLockView;
    }

    @Override
    public void initDate() {
        mCtx = mEventsOfLockView.getContext();

        mLogs = new ArrayList<>();
        mEventsOfLockAdapter = new EventsAdapter(mCtx, mLogs);
        mBleManagerHelper = BleManagerHelper.getInstance(mCtx);

        mListView.setLayoutManager(new LinearLayoutManager(mCtx, LinearLayoutManager.VERTICAL, false));
        mListView.setItemAnimator(new DefaultItemAnimator());
        mListView.setAdapter(mEventsOfLockAdapter);
        mListView.addItemDecoration(new SpacesItemDecoration(getResources().getDimensionPixelSize(R.dimen.y5dp)));
        mLoadDialog = DialogUtils.createLoadingDialog(mCtx, mCtx.getResources().getString(R.string.data_loading));
    }

    public int getCounter() {
        return mEventsOfLockAdapter.getItemCount();
    }

    public void doDelete(short userId) {
        if (mEventsOfLockAdapter.mDeleteLogs.size() != 0) {
            for (DeviceLog devLog : mEventsOfLockAdapter.mDeleteLogs) {
                int logId = devLog.getLogId();
                mBleManagerHelper.getBleCardService().sendCmd33(BleMsg.TYPE_DELETE_SINGLE_LOCK_LOG, userId, logId, devLog, BleMsg.INT_DEFAULT_TIMEOUT);
            }
        } else {
            showMessage(getString(R.string.plz_choise_del_log));
        }
    }

    public void cancelDelete() {
        mEventsOfLockAdapter.mDeleteLogs.clear();
        mEventsOfLockAdapter.chooseItemDelete(false);
        mEventsOfLockAdapter.notifyDataSetChanged();
    }

    public void editDelete() {
        mEventsOfLockAdapter.mDeleteLogs.clear();
        mEventsOfLockAdapter.chooseItemDelete(true);
        mEventsOfLockAdapter.notifyDataSetChanged();
    }

    public void selectedAll(){
        mEventsOfLockAdapter.mDeleteLogs.addAll(mEventsOfLockAdapter.mLogList);
        mEventsOfLockAdapter.chooseALLDelete(true);
        mEventsOfLockAdapter.notifyDataSetChanged();
    }
    public void cancelSelectedAll(){
        mEventsOfLockAdapter.mDeleteLogs.clear();
        mEventsOfLockAdapter.chooseALLDelete(false);
        mEventsOfLockAdapter.notifyDataSetChanged();
    }

    public void getLogsOver(String nodeId, DeviceInfo deviceInfo, DeviceUser deviceUser) {

        if (deviceUser.getUserPermission() == ConstantUtil.DEVICE_MASTER) {
            mLogs = DeviceLogDao.getInstance(mCtx).queryKeyLockEvent("node_id", nodeId);
        } else if (deviceUser.getUserPermission() == ConstantUtil.DEVICE_MEMBER) {
            mLogs = DeviceLogDao.getInstance(mCtx).queryUserLogLockEvent(nodeId, mDefaultDevice.getUserId());
        }
        LogUtil.d(TAG, "receiver size = " + mLogs.size());
        mEventsOfLockAdapter.setDataSource(mLogs);
        mEventsOfLockAdapter.notifyDataSetChanged();
    }

    public boolean delLogSuccess(DeviceUser deviceUser,Bundle bundle) {
        DeviceLog mDelDeVLog = (DeviceLog) bundle.getSerializable(BleMsg.KEY_SERIALIZABLE);
        if (mDelDeVLog != null) {
            mEventsOfLockAdapter.removeItem(mDelDeVLog);
        }
        if (mEventsOfLockAdapter.mDeleteLogs.size() == 0) {
            if (deviceUser.getUserPermission() == ConstantUtil.DEVICE_MASTER) {
                mLogs = DeviceLogDao.getInstance(mCtx).queryKeyUserEvent("node_id", mNodeId);
            } else if (deviceUser.getUserPermission() == ConstantUtil.DEVICE_MEMBER) {
                mLogs = DeviceLogDao.getInstance(mCtx).queryUserLogLockEvent(mNodeId, deviceUser.getUserId());
            }

            mEventsOfLockAdapter.setDataSource(mLogs);
            mEventsOfLockAdapter.notifyDataSetChanged();
            DialogUtils.closeDialog(mLoadDialog);
            return true;
        } else {
            return false;
        }
    }


}
