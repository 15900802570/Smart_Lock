
package com.smart.dt.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.Toast;

public class NetworkUtil {

    /** 没有可用网络 */
    public static final int NO_NET_CONNECT = -1;
    /** wap网络可用 */
    public static final int WAP_CONNECTED = 0;
    /** net网 可用 */
    public static final int NET_CONNECTED = 1;
    /** wifi网络可用 */
    public static final int WIFI_CONNECT = 2;
    /** 当前网络不是WAP */
    public static final int NO_WAP_NET = 3;
    /** wifi网络可用 */
    public static final int OTHER_CONNECT = 4;

    public static int curNetWorkStatus;
    private static ConnectivityManager mConnMgr = null;

    private static WifiManager wifiManager = null;

    /**
     * @return -1: 没有可用网络; 0: wap 网络可用; 1：net 网络可用; 2：wifi 网络可用;
     */
    public static int getNetworkType(Context context) {
        mConnMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int wifiState = wifiManager.getWifiState();
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if (wifiInfo != null
                && wifiInfo.getIpAddress() != 0
                && wifiInfo.getNetworkId() != -1
                && (wifiState == WifiManager.WIFI_STATE_ENABLED || wifiState == WifiManager.WIFI_STATE_ENABLING)) {
            LogUtil.i("network", "WIFI网络可用");
            LogUtil.i("network", "WIFI网络IP=" + wifiInfo.getIpAddress());
            return WIFI_CONNECT;
        } else {
            NetworkInfo[] networkInfos = mConnMgr.getAllNetworkInfo();
            if (networkInfos != null && networkInfos.length > 0)
            {
                for (NetworkInfo networkInfo : networkInfos)
                {
                    LogUtil.i("network", "networkInfo.ExtraInfo=" + networkInfo.getExtraInfo());
                    if (networkInfo.isConnected() && networkInfo.isAvailable())
                    {
                        if (networkInfo.getExtraInfo() != null)
                        {
                            if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE
                                    && networkInfo.getExtraInfo().toLowerCase().equals("cmwap")) {
                                LogUtil.i("network", "WAP网络可用");
                                return WAP_CONNECTED;
                            } else {
                                LogUtil.i("network", "NET网络可用");
                                return NET_CONNECTED;
                            }
                        } else {
                            return OTHER_CONNECT;
                        }

                    }
                }
            } else {
                return NO_NET_CONNECT;
            }
        }
        return NO_NET_CONNECT;
    }

    public static boolean checkValue(boolean haveCheckNet, Context context) {

        if (haveCheckNet && !checkNet(context)) {
            Toast.makeText(context, "请打开网路", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    public static boolean checkNet(Context context) {
        if (NetworkUtil.getNetworkType(context) == NetworkUtil.NO_NET_CONNECT) {
            return false;
        } else {
            LogUtil.i("checkNet", "当前网络可用");
            return true;
        }
    }
}
