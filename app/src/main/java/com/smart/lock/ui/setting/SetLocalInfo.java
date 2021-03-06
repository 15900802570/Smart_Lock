package com.smart.lock.ui.setting;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.swipe.SwipeLayout;
import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ui.BaseActivity;
import com.smart.lock.ui.LockDetectingActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.util.ArrayList;

public class SetLocalInfo extends BaseActivity implements View.OnClickListener {

    private String TAG = "SetLocalInfo";

    private LinearLayout mRescanLl;
    private LinearLayout mTipsLl;
    private Button mRescanBtn;
    private LinearLayout mRefreshDevLl;
    private ProgressBar mScanDevBar;
    private RecyclerView mDevList;
    private BleAdapter mBleAdapter;

    private BluetoothAdapter mBluetoothAdapter;//蓝牙适配器
    private boolean mScanning;
    private Handler mHandler;
    private Dialog mLoadDialog;
    private BleManagerHelper mBleManagerHelper;
    private int REQUEST_ENABLE_BT = 100;

    private Runnable mRunnable = new Runnable() {
        public void run() {
            DialogUtils.closeDialog(mLoadDialog);
            Toast.makeText(SetLocalInfo.this, R.string.retry_connect, Toast.LENGTH_LONG).show();
            mRescanLl.setVisibility(View.VISIBLE);
            mTipsLl.setVisibility(View.VISIBLE);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_set_info);
        initView();
        initDate();
        initEvent();
    }

    private void initView() {
        mRescanLl = findViewById(R.id.ll_rescanning);
        mTipsLl = findViewById(R.id.ll_tips);
        mRescanBtn = findViewById(R.id.btn_rescanning);
        mRefreshDevLl = findViewById(R.id.refresh_dev_ll);
        mScanDevBar = findViewById(R.id.pb_scan_ble_dev);
        mDevList = findViewById(R.id.dev_list_rv);
    }

    private void initDate() {
        mHandler = new Handler();
        ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
        mBleAdapter = new BleAdapter(this, deviceList);

        deviceList = new ArrayList<>();
        mBleAdapter = new BleAdapter(this, deviceList);
        mDevList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mDevList.setItemAnimator(new DefaultItemAnimator());
        mDevList.setAdapter(mBleAdapter);
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mScanning = true;
        mLoadDialog = DialogUtils.createLoadingDialog(SetLocalInfo.this, SetLocalInfo.this.getString(R.string.search_lock));
        scanLeDevice(true);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mBleMac = StringUtil.getMacAdr(extras.getString(BleMsg.KEY_BLE_MAC));
            mSn = extras.getString(BleMsg.KEY_NODE_SN);
            mNodeId = extras.getString(BleMsg.KEY_NODE_ID);
        }

    }

    private void initEvent() {
        findViewById(R.id.iv_back).setOnClickListener(this);
        mRescanBtn.setOnClickListener(this);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_back:
                finish();
                break;
            case R.id.btn_rescanning:
                if (mScanning)
                    scanLeDevice(false);
                else
                    scanLeDevice(true);
                break;
        }
    }

    /**
     * 搜索附近门锁
     *
     * @param enable
     */
    private void scanLeDevice(final boolean enable) {

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (enable) {
                mRefreshDevLl.setVisibility(View.VISIBLE);
                mBleAdapter.mBluetoothDevList.clear();
                mBleAdapter.notifyDataSetChanged();
                mRescanLl.setVisibility(View.VISIBLE);
                mRescanBtn.setText(R.string.stop_scan);
                startRefresh();
                mScanning = true;
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                mRescanBtn.setText(R.string.rescanning);
                stopRefresh();
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }
    }

    /**
     * 开始刷新动画
     */
    public void startRefresh() {
        mScanDevBar.setIndeterminateDrawable(getResources().getDrawable(
                R.drawable.progressbar_loading));
        mScanDevBar.setProgressDrawable(getResources().getDrawable(
                R.drawable.progressbar_loading));
        mScanDevBar.setVisibility(View.VISIBLE);
    }

    /**
     * 停止刷新动画
     */
    public void stopRefresh() {
        mScanDevBar.setIndeterminateDrawable(getResources().getDrawable(
                R.mipmap.dialog_loading_img));
        mScanDevBar.setProgressDrawable(getResources().getDrawable(
                R.mipmap.dialog_loading_img));
        mScanDevBar.setVisibility(View.GONE);
    }


    public class BleAdapter extends RecyclerView.Adapter<BleAdapter.ViewHolder> {
        private Context mContext;
        public ArrayList<BluetoothDevice> mBluetoothDevList;

        public BleAdapter(Context context, ArrayList<BluetoothDevice> devList) {
            mContext = context;
            mBluetoothDevList = devList;
        }

        public void setDataSource(ArrayList<BluetoothDevice> devList) {
            mBluetoothDevList = devList;
        }

        public void addItem(BluetoothDevice bleDev) {
            int index = mBluetoothDevList.indexOf(bleDev);
            LogUtil.d(TAG, "result = " + index);
            if (index == -1) {
                mBluetoothDevList.add(mBluetoothDevList.size(), bleDev);
                LogUtil.d(TAG, "mBluetoothDevList = " + mBluetoothDevList.size());
                notifyItemInserted(mBluetoothDevList.size());
            }

        }

        @Override
        public BleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_dev, parent, false);
            SwipeLayout swipeLayout = inflate.findViewById(R.id.item_ll_dev);
            swipeLayout.setClickToClose(false);
            swipeLayout.setRightSwipeEnabled(false);
            return new BleAdapter.ViewHolder(inflate);
        }

        @Override
        public void onBindViewHolder(final BleAdapter.ViewHolder viewHolder, final int position) {
            final BluetoothDevice dev = mBluetoothDevList.get(position);
            if (dev != null) {
                viewHolder.mDevName.setText(dev.getName());
                viewHolder.mDevMac.setText(dev.getAddress());

                viewHolder.mDevContent.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (mScanning) {
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        }
                        mHandler.removeCallbacks(mRunnable);
                        DialogUtils.closeDialog(mLoadDialog);
                        Bundle bundle = new Bundle();
                        bundle.putString(BleMsg.KEY_BLE_MAC, StringUtil.deleteString(mBleMac,':'));
                        bundle.putString(BleMsg.KEY_NODE_SN, mSn);
                        bundle.putString(BleMsg.KEY_NODE_ID, mNodeId);
                        bundle.putString(BleMsg.KEY_OLD_MAC, dev.getAddress());
                        LogUtil.d(TAG, "mac = " + mBleMac + '\n' +
                                " sn = " + mSn + "\n" +
                                "mNodeId = " + mNodeId+'\n'+
                                "oldMAC = " + dev.getAddress());
                        BleManagerHelper.getInstance(SetLocalInfo.this).connectBle((byte) 2, bundle, SetLocalInfo.this);
                    }
                });
            }

        }

        @Override
        public int getItemCount() {
            return mBluetoothDevList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            SwipeLayout mSwipeLayout;
            TextView mDevMac;
            TextView mDevName;
            LinearLayout mDevContent;

            public ViewHolder(View itemView) {
                super(itemView);

                mSwipeLayout = (SwipeLayout) itemView;
                mDevMac = itemView.findViewById(R.id.tv_dev_mac);
                mDevName = itemView.findViewById(R.id.tv_dev_name);
                mDevContent = itemView.findViewById(R.id.ll_content);
            }
        }
    }

    private void addDevice(BluetoothDevice device) {
        mBleAdapter.addItem(device);
    }

    /**
     * 蓝牙搜索结果回调
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (StringUtil.checkNotNull(device.getName()) && device.getName().equals(ConstantUtil.LOCK_DEFAULT_NAME)) {
                        Log.d(TAG, "device.getName() = " + device.getName());
                        addDevice(device);
                    }

                }
            });
        }
    };

    /**
     * 超时提示框
     *
     * @param seconds 时间
     */
    private void closeDialog(int seconds) {
        mHandler.removeCallbacks(mRunnable);

        mHandler.postDelayed(mRunnable, seconds * 1000);
    }

}
