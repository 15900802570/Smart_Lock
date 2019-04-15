package com.smart.lock.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.ble.listener.ClientTransaction;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.widget.SpacesItemDecoration;

public class BaseListViewActivity extends AppCompatActivity implements View.OnClickListener {
    protected RecyclerView mListView;
    protected TextView mTitle;
    protected ImageView mBack;
    private static String TAG = BaseListViewActivity.class.getSimpleName();

    protected CheckBox mSelectCb;
    protected TextView mTipTv;
    protected TextView mDelTv;
    protected TextView mEditTv;
    protected RelativeLayout mSelectEventRl;
    protected SmartRefreshLayout mRefreshLayout;

    /**
     * 等待框
     */
    protected Dialog mLoadDialog;

    protected Handler mHandler;

    protected String mNodeId;
    protected DeviceInfo mDefaultDevice; //默认设备
    /**
     * 超时提示框启动器
     */
    protected Runnable mRunnable = new Runnable() {
        public void run() {
            if (mLoadDialog != null && mLoadDialog.isShowing()) {
                DialogUtils.closeDialog(mLoadDialog);
                Toast.makeText(BaseListViewActivity.this, BaseListViewActivity.this.getResources().getString(R.string.plz_reconnect), Toast.LENGTH_LONG).show();
            }

        }
    };

    protected ClientTransaction mCt;

    /**
     * 蓝牙
     */
    protected BleManagerHelper mBleManagerHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_listview);
        initView();

        mHandler = new Handler();

        Bundle bundle = getIntent().getExtras();
        mNodeId = bundle.getString(BleMsg.KEY_NODE_ID);
        mListView.addItemDecoration(new SpacesItemDecoration(getResources().getDimensionPixelSize(R.dimen.y16dp)));
    }


    private static IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleMsg.ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }

    /**
     * 初始化控件
     */
    protected void initView() {
        mListView = findViewById(R.id.list_view);
        mTitle = findViewById(R.id.tv_message_title);
        mBack = findViewById(R.id.iv_back_sysset);

        mSelectCb = findViewById(R.id.delete_locked);
        mTipTv = findViewById(R.id.tv_tips);
        mDelTv = findViewById(R.id.del_tv);
        mEditTv = findViewById(R.id.edit_tv);
        mSelectEventRl = findViewById(R.id.rl_select_delete);
        mSelectEventRl.setVisibility(View.GONE);
        mRefreshLayout = findViewById(R.id.refreshLayout);
        mRefreshLayout.setEnableRefresh(false);

        mBack.setOnClickListener(this);
        mDelTv.setOnClickListener(this);
        mSelectCb.setOnClickListener(this);
        mEditTv.setOnClickListener(this);
    }

    /**
     * 超时提醒
     *
     * @param seconds
     */
    protected void closeDialog(final int seconds, Handler handler, Runnable runnable) {

        handler.removeCallbacks(runnable);

        handler.postDelayed(runnable, seconds * 1000);
    }

    protected void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back_sysset:
                finish();
                break;
            default:
                break;
        }
    }

    /**
     * 新界面
     *
     * @param cls    新Activity
     * @param bundle 数据包
     */
    protected void startIntent(Class<?> cls, Bundle bundle) {
        Intent intent = new Intent();
        if (bundle != null) {
            intent.putExtras(bundle);
        }

        intent.setClass(this, cls);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
