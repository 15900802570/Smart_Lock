
package com.smart.lock.db.dao;

import android.content.Context;


import com.j256.ormlite.dao.Dao;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.helper.DtDatabaseHelper;
import com.smart.lock.utils.StringUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DeviceInfoDao {

    private DtDatabaseHelper mHelper;
    private Dao<DeviceInfo, Integer> dao;
    private Context mContext;
    private static DeviceInfoDao instance;

    protected DeviceInfoDao(Context context) {
        this.mContext = context;
        try {
            mHelper = DtDatabaseHelper.getHelper(mContext);
            dao = mHelper.getDao(DeviceInfo.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static DeviceInfoDao getInstance(Context context) {
        if (instance == null) {
            synchronized (DeviceInfoDao.class) {
                if (instance == null) {
                    instance = new DeviceInfoDao(context);
                }
            }

        }
        return instance;
    }

    public synchronized void insert(DeviceInfo DeviceInfo) {

        try {
            dao.create(DeviceInfo);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insert(ArrayList<DeviceInfo> beanArrayList) {
        try {
            dao.create(beanArrayList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateDeviceInfo(DeviceInfo info) {
        try {
            dao.update(info);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteByKey(String key, String values) {
        ArrayList<DeviceInfo> list = null;
        try {
            list = (ArrayList<DeviceInfo>) dao.queryForEq(key, values);
            if (list != null) {
                for (DeviceInfo bean : list) {
                    dao.delete(bean);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(DeviceInfo info) {
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

    public ArrayList<Boolean> queryConnectType(String pram) {
        List<DeviceInfo> list = null;
        ArrayList<Boolean> strings = null;
        try {
            list = dao.queryForEq("devices_connect_type", pram);
            if (list != null) {
                strings = new ArrayList<>();
                for (DeviceInfo DeviceInfo : list) {
                    strings.add(DeviceInfo.getConnectType());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return strings;
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
    public ArrayList<DeviceInfo> queryId(int id) {
        ArrayList<DeviceInfo> list = null;

        try {
            DeviceInfo DeviceInfo = dao.queryForId(id);
            if (DeviceInfo != null) {
                list = new ArrayList<>();
                list.add(DeviceInfo);
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<DeviceInfo> queryByConnectType(String connectType) {
        ArrayList<DeviceInfo> list = null;

        try {
            ArrayList<DeviceInfo> devicesInfo = (ArrayList<DeviceInfo>) dao
                    .queryForEq("devices_connect_type", connectType);
            list = devicesInfo;
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<DeviceInfo> queryAll() {
        ArrayList<DeviceInfo> list = null;
        try {
            list = (ArrayList<DeviceInfo>) dao.queryForAll();

            if (list != null) {
                return list;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public ArrayList<DeviceInfo> queryByUser(short userId) {
        ArrayList<DeviceInfo> list = new ArrayList<>();
        try {
            ArrayList<DeviceInfo> allList = (ArrayList<DeviceInfo>) dao.queryForAll();

            for (DeviceInfo info : allList) {
                if (info.getUserId() == userId) {
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

    public ArrayList<DeviceInfo> queryKey(String key, Object valus) {
        ArrayList<DeviceInfo> list = null;
        try {
            list = (ArrayList<DeviceInfo>) dao.queryForEq(key, valus);
            if (list != null) {
                return list;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public DeviceInfo queryFirstData(String key, Object valus) {
        DeviceInfo deviceInfo = null;
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

    public ArrayList<DeviceInfo> queryKeyByImei(String key, String valus, String imei) {
        ArrayList<DeviceInfo> list = new ArrayList<DeviceInfo>();
        try {
            ArrayList<DeviceInfo> allList = (ArrayList<DeviceInfo>) dao
                    .queryForEq(key, valus);

            for (DeviceInfo info : allList) {
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

    public ArrayList<DeviceInfo> queryKeyByUser(String key, Object valus, Object userId) {
        ArrayList<DeviceInfo> list = new ArrayList<DeviceInfo>();
        try {
            ArrayList<DeviceInfo> allList = (ArrayList<DeviceInfo>) dao
                    .queryForEq(key, valus);

            for (DeviceInfo info : allList) {
                if (info.getUserId() == (short) userId) {
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

    public ArrayList<String> queryDeviceType(String key, String valus) {
        ArrayList<DeviceInfo> list = null;
        ArrayList<String> strings = null;
        try {
            list = (ArrayList<DeviceInfo>) dao.queryForEq(key, valus);
            if (list != null) {
                strings = new ArrayList<>();
                for (DeviceInfo DeviceInfo : list) {
                    if (StringUtil.checkNotNull(DeviceInfo.getDeviceType()) && !strings.contains(DeviceInfo.getDeviceType())) {
                        strings.add(DeviceInfo.getDeviceType());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return strings;
    }

}
