package com.smart.lock.ble.provider;

import android.util.Log;

import com.smart.lock.ble.TimerListener;


/**
 * 超时提供者
 *
 */
public class TimerProvider{

	TimerListener listener;
	long time;
	String label;
	boolean active;
	InnerTimerST innerTimer;

	void init(long t_msec, String t_label, TimerListener t_listener) {

		listener = t_listener;
		time = t_msec;
		label = t_label;
		active = false;
	}

	public TimerProvider(long t_msec, String t_label, TimerListener t_listener) {
		init(t_msec, t_label, t_listener);
	}

	public String getLabel() {
		return label;
	}

	public long getTime() {
		return time;
	}

	public void halt() {
		active = false;
		listener = null;
		if (innerTimer != null) {
			innerTimer.cancel();
		}
	}

	/** Starts the timer */
	public void start() {
		active = true;
		innerTimer = new InnerTimerST(time, this);
	}

	/** When the Timeout fires */
	public void onInnerTimeout() {
		if (active && listener != null)
			listener.onTimeout(this);
		listener = null;
		active = false;
	}

	public boolean isActive() {
		return active;
	}
}

class InnerTimerST extends java.util.TimerTask{
	static java.util.Timer single_timer = new java.util.Timer(true);

	TimerProvider listener;

	public InnerTimerST(long timeout, TimerProvider listener) {
		this.listener = listener;
		 Log.d("ClientTransaction", "InnerTimerST");
		single_timer.schedule(this, timeout);
	}

	public void run() {
	    Log.d("ClientTransaction", "single_timer is run!~");
		if (listener != null) {
			listener.onInnerTimeout();
			listener = null;
		}
	}
}
