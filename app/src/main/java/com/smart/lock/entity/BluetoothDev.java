package com.smart.lock.entity;

public class BluetoothDev {

    private String devName;
    private int rssi;
    private String devMac;

    public String getDevName() {
        return devName;
    }

    public void setDevName(String devName) {
        this.devName = devName;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public String getDevMac() {
        return devMac;
    }

    public void setDevMac(String devMac) {
        this.devMac = devMac;
    }

    @Override
    public String toString() {
        return "BluetoothDev{" +
                "devName='" + devName + '\'' +
                ", rssi=" + rssi +
                ", devMac='" + devMac + '\'' +
                '}';
    }
}
