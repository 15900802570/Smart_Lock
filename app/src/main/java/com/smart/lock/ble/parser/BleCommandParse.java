package com.smart.lock.ble.parser;

import com.smart.lock.ble.message.Message;

/**
 * AT指令解析器
 */
public interface BleCommandParse {
    /**
     * 命令错误时，默认显示
     */
    public static final String ERROR_UNKNOW = "Unknow";
    /**
     * 错误命令标志
     */
    public static final String ERROR = "ERROR";
    /**
     * 成功命令标志
     */
    public static final String OK = "OK";

    /**
     * 解析命令
     *
     * @param cmd ble命令
     * @return
     */
    public Message parse(byte[] cmd);

    /**
     * 获取解析器关键字
     *
     * @return
     */
    public byte getParseKey();


    public String getTag();
}
