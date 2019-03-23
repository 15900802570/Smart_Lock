package com.smart.lock.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.smart.lock.db.bean.DeviceStatus;
import com.smart.lock.db.helper.DtDatabaseHelper;

import java.sql.SQLException;
import java.util.ArrayList;

public class DeviceStatusDao {
    private DtDatabaseHelper mHelper;
    private Dao<DeviceStatus, Integer> dao;
    private Context mContext;
    private static DeviceStatusDao instance;

    public static String DEVICE_NODEID = "dev_node_id";
    public static String INTELLIGENT_LOCK_CORE = "intelligent_lock_core";
    public static String AntiPrizingAlarm = "anti_prizing_alarm";
    public static String CombinationLock = "combination_lock";
    public static String NormallyOpen = "normally_open";
    public static String VoicePrompt = "voice_prompt";

    protected DeviceStatusDao(Context context) {
        this.mContext = context;
        try {
            mHelper = DtDatabaseHelper.getHelper(mContext);
            dao = mHelper.getDao(DeviceStatus.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static DeviceStatusDao getInstance(Context context) {
        if (instance == null) {
            synchronized ((DeviceStatusDao.class)) {
                if (instance == null) {
                    instance = new DeviceStatusDao(context);
                }
            }
        }
        return instance;
    }

    public DeviceStatus queryOrCreateByNodeId(String value) {
        DeviceStatus deviceStatus = null;
        try {
            deviceStatus = dao.queryBuilder().where().eq(DEVICE_NODEID, value).queryForFirst();
            if (deviceStatus != null) {
                return deviceStatus;
            } else {
                deviceStatus = new DeviceStatus();
                deviceStatus.setDevNodeId(value);
                dao.create(deviceStatus);
                deviceStatus = dao.queryBuilder().where().eq(DEVICE_NODEID, value).queryForFirst();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return deviceStatus;
    }

    public void deleteByKey(String key, String values) {
        ArrayList<DeviceStatus> list = null;
        try {
            list = (ArrayList<DeviceStatus>) dao.queryForEq(key, values);
            if (list != null) {
                for (DeviceStatus bean : list) {
                    dao.delete(bean);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public synchronized void insert(DeviceStatus deviceStatus) {
        try {
            dao.create(deviceStatus);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateDeviceStatus(DeviceStatus deviceStatus) {
        try {
            dao.update(deviceStatus);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(DeviceStatus deviceStatus) {
        try {
            dao.delete(deviceStatus);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * @return -1:删除数据异常 0：无数据
     */
    public int deleteAll() {
        int number = -1;
        try {
            number = dao.deleteBuilder().delete();// 返回删除的数据条数
            // 例如：删除1条数据，返回1，依次类推。


        } catch (SQLException e) {
            e.printStackTrace();
        }
        return number;
    }
}
