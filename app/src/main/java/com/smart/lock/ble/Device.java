package com.smart.lock.ble;

public class Device {
    public static final String DEV_NAME = "NAME";
    public static final String DEV_MAC = "ADDRESS";

    public static final int DFU_CHAR_EXISTS = 0x01;
    public static final int DFU_FW_LOADED   = 0x02;
    public static final int DFU_READY       = DFU_CHAR_EXISTS | DFU_FW_LOADED;
    public static final int DFU_CHAR_DISCONNECTED   = 0x04;
    public static final int DFU_FW_UNLOADED   = 0x08;
}
