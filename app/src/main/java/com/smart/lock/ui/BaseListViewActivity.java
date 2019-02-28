package com.smart.lock.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.BleCardService;
import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.StringUtil;

import java.util.ArrayList;

public class BaseListViewActivity extends Activity implements View.OnClickListener {
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

                mBleManagerHelper = BleManagerHelper.getInstance(BaseListViewActivity.this, mNodeId, false);
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
     * 检查状态字，实现录入秘钥同步功能
     *
     * @param status 秘钥状态字
     */
    public boolean checkStatus(int status, Handler handler, Runnable runnable, String imei, BleCardService service, byte[] ak) {
        ArrayList<String> lockIdList = null;
        ArrayList<String> devList = new ArrayList<>();
        if (lockIdList == null) {
            lockIdList = new ArrayList<>();
        }
        Log.d(TAG, "lockIdList = " + lockIdList);

        //将二进制状态字反序，便于获取lockId
        String str = new StringBuffer(Integer.toBinaryString(status)).reverse().toString();

        Log.d(TAG, "str = " + str);
        if (str != null && str.equals("0")) {
            mStatusTag = false;
            return false;
        }
        for (int i = str.length() - 1; i >= 0; i--) {

            if (str.charAt(i) == '1') {
                String lockId = StringUtil.autoGenericCode(String.valueOf(i), 4);
                devList.add(lockId);
                if (!lockIdList.contains(lockId)) {
                    Log.d(TAG, "lockId = " + lockId);
                    closeDialog(15, handler, runnable);
                    service.sendCmd17(lockId, ak);
                }
            }

        }

        for (String devLockId : devList) {
            if (!devList.contains(devLockId)) {
//                SettingInfoDao.getInstance(this).deleteSettingKey("lock_id", devLockId);
            }
        }
        mStatusTag = false;
        return true;
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

}
