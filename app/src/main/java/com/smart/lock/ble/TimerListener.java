package com.smart.lock.ble;

import com.smart.lock.ble.provider.TimerProvider;

/**
 * 超时时间监听器
 *
 */
public interface TimerListener{
	/**
	 * 超时时调用
	 * @param t 
	 * @see TimerProvider
	 */
	public void onTimeout(TimerProvider t);
}
