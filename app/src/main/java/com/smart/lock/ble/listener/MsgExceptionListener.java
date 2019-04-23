package com.smart.lock.ble.listener;

import com.smart.lock.ble.message.Message;

public interface MsgExceptionListener {

    public void msgSendFail(Message msg);

    public void msgSendTimeOut(Message msg);

}
