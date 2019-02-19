/**
 *
 */
package com.smart.lock.ble.provider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.Arrays;

public class BleReceiver extends BroadcastReceiver {

    /**
     * 标签
     */
    public static final String TAG = BleReceiver.class.getSimpleName();

    /**
     * Ble指令接收器广播结果关键字
     */
    protected String mKeyResult;
    /**
     * Ble指令回调广播
     */
    protected String[] mATActions;
    /**
     * @see BleProvider
     */
    private BleProvider mProvider;

    /**
     * 构造方法
     *
     * @param provider
     * @see #mProvider
     */
    public BleReceiver(BleProvider provider) {
        super();
        this.mProvider = provider;
    }

    /**
     * 注册AT广播接收器
     *
     * @param context
     */
    public void registerReceiver(Context context, String keyResult, String[] actions) {
        IntentFilter filter = new IntentFilter();
        mATActions = actions;
        mKeyResult = keyResult;

        for (String action : actions) {
            filter.addAction(action);
        }

        context.registerReceiver(this, filter);
    }

    /**
     * 注销AT广播接收器
     *
     * @param context
     */
    public void unregisterReceiver(Context context) {
        context.unregisterReceiver(this);
    }

    /**
     * {@inheritDoc}
     *
     * @see BroadcastReceiver#onReceive(Context,
     * Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        byte[] result = intent.getByteArrayExtra(mKeyResult);

        if (result.length != 0) {
            mProvider.onReceiveBle(result);
            Log.w(TAG, "onReceive failure ble : " + Arrays.toString(result));

        }
    }

}
