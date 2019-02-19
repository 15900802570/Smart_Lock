package com.smart.lock.ble.creator;

import com.smart.lock.ble.message.Message;

/**
 * AT生成器
 */
public interface BleCreator {

    /**
     * 生成AT接口
     *
     * @param message 消息
     * @return AT指令
     */
    public byte[] create(Message message);

    public String getTag();
}
