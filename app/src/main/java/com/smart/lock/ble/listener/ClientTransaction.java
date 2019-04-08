package com.smart.lock.ble.listener;

import android.util.Log;

import com.smart.lock.ble.TimerListener;
import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.provider.BleProvider;
import com.smart.lock.ble.provider.TimerProvider;

/**
 * 信息事务
 */
public class ClientTransaction implements TimerListener, BleMessageListener {

    private Message mMessage;
    private int mTimeout;
    private BleMessageListener listener;
    private TimerProvider timer;
    private BleProvider BleMsgProvider;
    private String listenerKey;
    private long lastupdBleetime = System.currentTimeMillis();

    private static final String TAG = ClientTransaction.class.getSimpleName();

    /**
     * @param timeout
     * @param agent
     * @param bleProvider
     */
    public ClientTransaction(Message message, int timeout, BleMessageListener agent, BleProvider bleProvider) {
        mMessage = message;
        mTimeout = timeout;
        listener = agent;
        BleMsgProvider = bleProvider;
        listenerKey = message.getKey();
    }

    /**
     * 调用Provider发送指令
     *
     * @throws Exception
     */
    public boolean request() {
        return BleMsgProvider.send(this);
    }

    public int getTimeout() {
        return mTimeout;
    }

    @Override
    public void onReceive(BleProvider bleProvide, Message message) {
        listener.onReceive(bleProvide, message);
    }

    /**
     * 启动守护，维护在指定时间内没有响应
     */
    public void startWatch() {
        // 比芯片侧超时时间多10S，保证系统超时比芯片侧长，这个为了防止芯片侧出现异常情况
        timer = new TimerProvider((mTimeout + 10) * 1000, "ClientTransaction---" + getListenerKey(), this);
        timer.start();
        Log.d(TAG, timer.getLabel() + "启动守护-----timeout=" + timer.getTime());
    }

    /**
     * 停止事务，一般由中断程序引起的
     */
    public void halt() {
        Log.d(TAG, "timer.isActive() = " + timer.isActive());
        if (timer.isActive()) {
            BleMsgProvider.removeBleMsgListener(this);
            timer.halt();
            mMessage.recycle();
        }
    }

    @Override
    public void onTimeout(TimerProvider t) {
        Log.d(ClientTransaction.class.getSimpleName(), toString() + "<<<<onTimeout");

        if (BleMsgProvider.removeBleMsgListener(this) == null) {
            Log.w(TAG, "listenerKey : " + listenerKey + " has already removed");
        } else {
            mMessage.setException(Message.EXCEPTION_TIMEOUT);
            listener.onReceive(BleMsgProvider, mMessage);
        }
    }

    @Override
    public String getListenerKey() {
        return listenerKey;
    }

    @Override
    public String toString() {
        return "ClientTransaction@" + hashCode() + ">>>" + mMessage.toString();
    }

    public long getLastupdBleetime() {
        return lastupdBleetime;
    }

    public boolean isTimeout() {
        return lastupdBleetime + mTimeout < System.currentTimeMillis();
    }

    public void setLastupdBleetime(long lastupdBleetime) {
        lastupdBleetime = lastupdBleetime;
    }

    public Message getMessage() {
        return mMessage;
    }

}