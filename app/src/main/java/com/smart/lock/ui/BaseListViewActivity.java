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
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.utils.DialogUtils;

public class BaseListViewActivity extends AppCompatActivity implements View.OnClickListener {
    protected RecyclerView mListView;
    protected TextView mTitle;
    protected ImageView mBack;
    protected TextView mSyncTv;
    protected ImageButton mReturnBtn;
    protected ImageButton mDeleteBtn;
    protected CheckBox mSelectCb;
    private static String TAG = BaseListViewActivity.class.getSimpleName();
    protected boolean mStatusTag = false;

    protected Button mAddBtn;

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

//                mBleManagerHelper = BleManagerHelper.getInstance(BaseListViewActivity.this, mDefaultDevice.getBleMac(), false);
//                mBleManagerHelper.getBleCardService().sendCmd19(mBleManagerHelper.getAK());

                Toast.makeText(BaseListViewActivity.this, BaseListViewActivity.this.getResources().getString(R.string.plz_reconnect), Toast.LENGTH_LONG).show();
            }

        }
    };

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
        mSyncTv = findViewById(R.id.tv_sync);

        mAddBtn = findViewById(R.id.btn_add);
        mAddBtn.setVisibility(View.GONE);

        mReturnBtn = findViewById(R.id.return_btn);
        mDeleteBtn = findViewById(R.id.delete_btn);
        mSelectCb = findViewById(R.id.select_all);

        mBack.setOnClickListener(this);
        mAddBtn.setOnClickListener(this);
        mReturnBtn.setOnClickListener(this);
        mDeleteBtn.setOnClickListener(this);
        mSyncTv.setOnClickListener(this);
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
