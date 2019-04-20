
package com.smart.lock.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.smart.lock.R;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.helper.DtDatabaseHelper;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.LogUtil;

import java.sql.SQLException;
import java.util.ArrayList;

public class DeviceKeyDao {

    private static final String TAG = DeviceKeyDao.class.getSimpleName();
    private DtDatabaseHelper mHelper;
    private Dao<DeviceKey, Integer> dao;
    private Context mContext;
    private static DeviceKeyDao instance;

    public static String DEVICE_NODE_ID = "device_nodeId";

    protected DeviceKeyDao(Context context) {
        this.mContext = context;
        try {
            mHelper = DtDatabaseHelper.getHelper(mContext);
            dao = mHelper.getDao(DeviceKey.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static DeviceKeyDao getInstance(Context context) {
        if (instance == null) {
            synchronized (DeviceKeyDao.class) {
                if (instance == null) {
                    instance = new DeviceKeyDao(context);
                }
            }

        }
        return instance;
    }


    public void insert(DeviceKey DeviceKey) {

        try {
            dao.create(DeviceKey);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insert(ArrayList<DeviceKey> beanArrayList) {
        try {
            dao.create(beanArrayList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void updateDeviceKey(DeviceKey info) {
        try {
            dao.update(info);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteByKey(String key, String values) {
        ArrayList<DeviceKey> list = null;
        try {
            list = (ArrayList<DeviceKey>) dao.queryForEq(key, values);
            if (list != null) {
                for (DeviceKey bean : list) {
                    dao.delete(bean);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int delete(DeviceKey info) {
        try {
            return dao.delete(info);
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
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
    public ArrayList<DeviceKey> queryId(int id) {
        ArrayList<DeviceKey> list = null;

        try {
            DeviceKey DeviceKey = dao.queryForId(id);
            if (DeviceKey != null) {
                list = new ArrayList<>();
                list.add(DeviceKey);
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<DeviceKey> queryByConnectType(String connectType) {
        ArrayList<DeviceKey> list = null;

        try {
            ArrayList<DeviceKey> devicesInfo = (ArrayList<DeviceKey>) dao
                    .queryForEq("devices_connect_type", connectType);
            list = devicesInfo;
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<DeviceKey> queryAll() {
        ArrayList<DeviceKey> list = null;
        try {
            list = (ArrayList<DeviceKey>) dao.queryForAll();

            if (list != null) {
                return list;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }


    public ArrayList<DeviceKey> queryKey(String key, Object valus) {
        ArrayList<DeviceKey> list = null;
        try {
            list = (ArrayList<DeviceKey>) dao.queryForEq(key, valus);
            if (list != null) {
                return list;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public DeviceKey queryFirstData(String key, Object valus) {
        DeviceKey deviceInfo = null;
        try {
            deviceInfo = dao.queryBuilder().where().eq(key, valus).queryForFirst();
            if (deviceInfo != null) {
                return deviceInfo;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return deviceInfo;
    }

    public ArrayList<DeviceKey> queryDeviceKey(Object nodeId, Object userId, Object type) {
        ArrayList<DeviceKey> list = new ArrayList<DeviceKey>();
        try {
            list = (ArrayList<DeviceKey>) dao.queryBuilder().where().eq("device_nodeId", nodeId).and().eq("user_id", userId).
                    and().eq("key_type", type).query();
            if (list != null) {
                return list;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public DeviceKey queryByLockId(Object nodeId, Object userId, Object lockId, Object type) {
        DeviceKey deviceKey = new DeviceKey();
        try {
            deviceKey = dao.queryBuilder().where().eq("device_nodeId", nodeId).and().eq("user_id", userId).
                    and().eq("lock_id", lockId).and().eq("key_type", type).queryForFirst();
            if (deviceKey != null) {
                return deviceKey;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return deviceKey;
    }

    /**
     * 同步用户开锁信息
     *
     * @param nodeId
     * @param userId
     * @param key
     * @param type
     * @param lockId
     */
    public void checkDeviceKey(Object nodeId, short userId, byte key, byte type, String lockId) {
        if (key != 0) {
            DeviceKey deviceKey = queryByLockId(nodeId, userId, lockId, type);
            LogUtil.d(TAG, "deviceKey =  " + (deviceKey == null ? true : deviceKey.toString()));
            if (deviceKey == null) {
                deviceKey = new DeviceKey();
                deviceKey.setDeviceNodeId((String) nodeId);
                deviceKey.setUserId(userId);
                deviceKey.setKeyActiveTime(System.currentTimeMillis() / 1000);
                if (type == ConstantUtil.USER_PWD) {
                    deviceKey.setKeyName(mContext.getResources().getString(R.string.me) + mContext.getResources().getString(R.string.password));
                } else if (type == ConstantUtil.USER_FINGERPRINT)
                    deviceKey.setKeyName(mContext.getResources().getString(R.string.me) + mContext.getResources().getString(R.string.fingerprint) + lockId);
                else if (type == ConstantUtil.USER_NFC)
                    deviceKey.setKeyName(mContext.getResources().getString(R.string.me) + mContext.getResources().getString(R.string.card));

                deviceKey.setKeyType(type);
                deviceKey.setLockId(lockId);

                insert(deviceKey);
            }
        } else {
            DeviceKey deviceKey = queryByLockId(nodeId, userId, lockId, type);
            if (deviceKey != null) {
                delete(deviceKey);
            }
        }

    }


    public ArrayList<DeviceKey> queryKeyByImei(String key, String valus, String imei) {
        ArrayList<DeviceKey> list = new ArrayList<DeviceKey>();
        try {
            ArrayList<DeviceKey> allList = (ArrayList<DeviceKey>) dao
                    .queryForEq(key, valus);

            for (DeviceKey info : allList) {
                if (info.getDeviceNodeId().equals(imei)) {
                    list.add(info);
                }
            }

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
