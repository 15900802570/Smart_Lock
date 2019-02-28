
package com.smart.lock.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.helper.DtDatabaseHelper;
import com.smart.lock.db.impl.DeviceUserImpl;

import java.sql.SQLException;
import java.util.ArrayList;

public class DeviceUserDao implements DeviceUserImpl {

    private DtDatabaseHelper mHelper;
    private Dao<DeviceUser, Integer> dao;
    private Context mContext;
    private static DeviceUserDao instance;

    protected DeviceUserDao(Context context) {
        this.mContext = context;
        try {
            mHelper = DtDatabaseHelper.getHelper(mContext);
            dao = mHelper.getDao(DeviceUser.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static DeviceUserDao getInstance(Context context) {
        if (instance == null) {
            synchronized (DeviceUserDao.class) {
                if (instance == null) {
                    instance = new DeviceUserDao(context);
                }
            }

        }
        return instance;
    }

    @Override
    public void insert(DeviceUser DeviceUser) {

        try {
            dao.create(DeviceUser);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void insert(ArrayList<DeviceUser> beanArrayList) {
        try {
            dao.create(beanArrayList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateDeviceUser(DeviceUser info) {
        try {
            dao.update(info);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteByKey(String key, String values) {
        ArrayList<DeviceUser> list = null;
        try {
            list = (ArrayList<DeviceUser>) dao.queryForEq(key, values);
            if (list != null) {
                for (DeviceUser bean : list) {
                    dao.delete(bean);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(DeviceUser info) {
        try {
            dao.delete(info);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * @return -1:删除数据异常 0：无数据
     */
    @Override
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
    @Override
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
    @Override
    public ArrayList<DeviceUser> queryId(int id) {
        ArrayList<DeviceUser> list = null;

        try {
            DeviceUser DeviceUser = dao.queryForId(id);
            if (DeviceUser != null) {
                list = new ArrayList<>();
                list.add(DeviceUser);
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public ArrayList<DeviceUser> queryAll() {
        ArrayList<DeviceUser> list = null;
        try {
            list = (ArrayList<DeviceUser>) dao.queryForAll();

            if (list != null) {
                return list;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }


    @Override
    public ArrayList<DeviceUser> queryKey(String key, Object valus) {
        ArrayList<DeviceUser> list = null;
        try {
            list = (ArrayList<DeviceUser>) dao.queryForEq(key, valus);
            if (list != null) {
                return list;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public DeviceUser queryUser(Object nodeId, Object userId) {
        DeviceUser deviceUser = null;
        try {
            deviceUser = dao.queryBuilder().where().eq("dev_node_id", nodeId).and().eq("user_id", userId).queryForFirst();
            if (deviceUser != null) {
                return deviceUser;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return deviceUser;
    }


    /**
     * 这个方法可以的
     */
    public boolean delteDatabases(Context context, String DBname) {
        return context.deleteDatabase(DBname);
    }


}
