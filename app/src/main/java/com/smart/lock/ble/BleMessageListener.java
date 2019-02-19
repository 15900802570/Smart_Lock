package com.smart.lock.ble;

import com.smart.lock.ble.message.Message;
import com.smart.lock.ble.provider.BleProvider;

/**
 * Ble指令消息监听器
 *
 */
public interface BleMessageListener {

	/**
	 * 当触发监听事件是回调
	 * @param provider Ble Provider
	 * @param message Ble消息
	 * @see Message
	 */
	public void onReceive(BleProvider provider, Message message);
	
	/**
	 * 获取监听关键字
	 * @return 监听关键字
	 */
	public String getListenerKey();

	/**
	 * 关闭监听器
	 */
	public void halt();
}
