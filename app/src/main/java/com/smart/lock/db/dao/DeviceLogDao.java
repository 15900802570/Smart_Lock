
package com.smart.lock.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceLog;
import com.smart.lock.db.helper.DtDatabaseHelper;

import java.sql.SQLException;
import java.util.ArrayList;

public class DeviceLogDao {

    private DtDatabaseHelper mHelper;
    private Dao<DeviceLog, Integer> dao;
    private Context mContext;
    private static DeviceLogDao instance;

    public static String DEVICE_NODE_ID = "node_id";

    protected DeviceLogDao(Context context) {
        this.mContext = context;
        try {
            mHelper = DtDatabaseHelper.getHelper(mContext);
            dao = mHelper.getDao(DeviceLog.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static DeviceLogDao getInstance(Context context) {
        if (instance == null) {
            synchronized (DeviceLogDao.class) {
                if (instance == null) {
                    instance = new DeviceLogDao(context);
                }
            }

        }
        return instance;
    }

    public void insert(DeviceLog DeviceLog) {

        try {
            dao.create(DeviceLog);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insert(ArrayList<DeviceLog> beanArrayList) {
        try {
            dao.create(beanArrayList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void updateDeviceLog(DeviceLog info) {
        try {
            dao.update(info);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteByKey(String key, String values) {
        ArrayList<DeviceLog> list = null;
        try {
            list = (ArrayList<DeviceLog>) dao.queryForEq(key, values);
            if (list != null) {
                for (DeviceLog bean : list) {
                    dao.delete(bean);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(DeviceLog info) {
        try {
            dao.delete(info);
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


    /**
     * @return 表中数据的个数
     */
    public long queryCount() {
        long number = 0;
        try {
            number = dao.queryBuilder().countOf();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return number;
    }

    /**
     * @param id 这个id 就是表中，每次插入数据，自己递增的id 字段
     */
    public ArrayList<DeviceLog> queryId(int id) {
        ArrayList<DeviceLog> list = null;

        try {
            DeviceLog DeviceLog = dao.queryForId(id);
            if (DeviceLog != null) {
                list = new ArrayList<>();
                list.add(DeviceLog);
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<DeviceLog> queryByConnectType(String connectType) {
        ArrayList<DeviceLog> list = null;

        try {
            ArrayList<DeviceLog> devicesInfo = (ArrayList<DeviceLog>) dao
                    .queryForEq("devices_connect_type", connectType);
            list = devicesInfo;
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<DeviceLog> queryAll() {
        ArrayList<DeviceLog> list = null;
        try {
            list = (ArrayList<DeviceLog>) dao.queryForAll();

            if (list != null) {
                return list;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }


    public ArrayList<DeviceLog> queryKeyUserEvent(String key, Object valus) {
        ArrayList<DeviceLog> list = new ArrayList<DeviceLog>();
        try {
            list = (ArrayList<DeviceLog>) dao.queryBuilder()
                    .orderBy("log_id", false)
                    .where()
                    .eq(key, valus)
                    .and()
                    .ge("log_state", 4)
                    .query();
            if (list != null) {
                return list;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<DeviceLog> queryKeyLockEvent(String key, Object valus) {
        ArrayList<DeviceLog> list = new ArrayList<DeviceLog>();
        try {
            list = (ArrayList<DeviceLog>) dao.queryBuilder()
                    .orderBy("log_id", false)
                    .where()
                    .eq(key, valus)
                    .and()
                    .le("log_state", 3)
                    .query();
            if (list != null) {
                return list;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<DeviceLog> queryUserLogLockEvent(Object nodeId, Object userId) {
        ArrayList<DeviceLog> list = new ArrayList<DeviceLog>();
        try {
            list = (ArrayList<DeviceLog>) dao.queryBuilder()
                    .orderBy("log_id", false)
                    .where()
                    .eq(DEVICE_NODE_ID, nodeId)
                    .and()
                    .eq("user_id", userId)
                    .and()
                    .le("log_state", 3)
                    .query();
            if (list != null) {
                return list;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<DeviceLog> queryUserLogUserEvent(Object nodeId, Object userId) {
        ArrayList<DeviceLog> list = new ArrayList<DeviceLog>();
        try {
            list = (ArrayList<DeviceLog>) dao.queryBuilder()
                    .orderBy("log_id", false)
                    .where()
                    .eq(DEVICE_NODE_ID, nodeId)
                    .and()
                    .eq("user_id", userId)
                    .and()
                    .gt("log_state", 3)
                    .query();
            if (list != null) {
                return list;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<DeviceLog> queryDeviceLog(Object nodeId, Object userId, Object type) {
        ArrayList<DeviceLog> list = new ArrayList<DeviceLog>();
        try {
            list = (ArrayList<DeviceLog>) dao.queryBuilder().where().eq("device_nodeId", nodeId).and().eq("user_id", userId).
                    and().eq("key_type", type).query();
            if (list != null) {
                return list;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }


    /**
     * 这个方法可以的
     */
    public boolean delteDatabases(Context context, String DBname) {
        return context.deleteDatabase(DBname);
    }


}
