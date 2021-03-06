package com.smart.lock.db.helper;

import android.content.Context;

import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.db.dao.DeviceStatusDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.db.dao.TempPwdDao;

public class DtComFunHelper {

    /**
     * 对单个设备进行恢复出厂设置
     *
     * @param context    Context
     * @param deviceInfo 设备信息
     * @return Boolean
     */
    public static boolean restoreFactorySettings(Context context, DeviceInfo deviceInfo) {
        try {
            String nodeId = deviceInfo.getDeviceNodeId();
            DeviceKeyDao.getInstance(context).deleteByKey(DeviceKeyDao.DEVICE_NODE_ID, nodeId);
            DeviceStatusDao.getInstance(context).deleteByKey(DeviceStatusDao.DEVICE_NODEID, nodeId);
            DeviceUserDao.getInstance(context).deleteByKey(DeviceUserDao.DEVICE_NODE_ID, nodeId);
            TempPwdDao.getInstance(context).deleteByKey(TempPwdDao.DEVICE_NODE_ID, nodeId);
            DeviceInfoDao.getInstance(context).delete(deviceInfo);
            if (DeviceInfoDao.getInstance(context).queryFirstData(DeviceInfoDao.DEVICE_DEFAULT, true) == null) {
                setNewDefault(context);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 查询其余设备并且设置为默认设备
     *
     * @param context Context
     */
    public static void setNewDefault(Context context) {
        DeviceInfo deviceInfo = DeviceInfoDao.getInstance(context).queryFirstData(DeviceInfoDao.DEVICE_DEFAULT, false);
        if (deviceInfo != null) {
            deviceInfo.setDeviceDefault(true);
            DeviceInfoDao.getInstance(context).updateDeviceInfo(deviceInfo);
        }
    }
}
