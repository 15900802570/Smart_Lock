
package com.smart.lock.db.dao;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.helper.DtDatabaseHelper;
import com.smart.lock.db.impl.DeviceUserImpl;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.LogUtil;

import java.sql.SQLException;
import java.util.ArrayList;

public class DeviceUserDao implements DeviceUserImpl {
    private final String TAG = DeviceUserDao.class.getSimpleName();

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
    public synchronized void insert(ArrayList<DeviceUser> beanArrayList) {
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
    public synchronized void delete(DeviceUser info) {
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
            list = (ArrayList<DeviceUser>) dao.queryBuilder().orderBy("user_id", false).where().eq(key, valus).query();
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

    @Override
    public ArrayList<DeviceUser> queryUsers(Object nodeId, Object permission) {
        ArrayList<DeviceUser> list = null;
        try {
            list = (ArrayList<DeviceUser>) dao.queryBuilder().orderBy("user_id", true).where().eq("dev_node_id", nodeId).and().eq("user_permission", permission).query();
            if (list != null) {
                return list;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public ArrayList<DeviceUser> queryDeviceUsers(Object nodeId) {
        ArrayList<DeviceUser> list = null;
        try {
            list = (ArrayList<DeviceUser>) dao.queryBuilder().orderBy("user_id", true).where().eq("dev_node_id", nodeId).query();
            if (list != null) {
                return list;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public ArrayList<String> queryDeviceUserIds(Object nodeId) {
        ArrayList<String> list = new ArrayList<>();
        try {
            ArrayList<DeviceUser> users = (ArrayList<DeviceUser>) dao.queryBuilder().orderBy("user_id", true).where().eq("dev_node_id", nodeId).query();
            for (DeviceUser info : users) {
                list.add(info.getUserId());
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
     * 本地用户状态字
     *
     * @return 获取本地用户状态字
     */
    public long getUserStatus(String nodeId) {
        long ret = 0;
        long index = 0;
        ArrayList<String> userIds = queryDeviceUserIds(nodeId);
        if (userIds != null && !userIds.isEmpty()) {

            for (String userId : userIds) {
                long id = Long.parseLong(userId);
                Log.d(TAG, "id = " + id);
                if (id > 0 && id <= 5) { //管理员编号
                    index = id - 1;
                } else if (id > 100 && id <= 200) { //普通用户
                    index = id - 91;
                } else if (id > 200 && id <= 300) { //临时用户
                    index = id - 196;
                }
                ret |= 1 << index;
            }
        }
        return ret;
    }

    /**
     * 检查状态字，实现用户同步功能
     *
     * @param status 秘钥状态字
     */
    public ArrayList<String> checkUserStatus(long status, String nodeId) {
        ArrayList<String> diffIds = new ArrayList<>();
        long ret = 0;
        long tmp = 0;
        long userStatus = getUserStatus(nodeId);
        Log.d(TAG, "userStatus = " + userStatus);

        ret = status ^ userStatus;

        Log.d(TAG, "ret = " + ret);
        synchronized (this) {
            if (ret != 0) {
                for (int i = 0; i < 100; i++) {
                    tmp = ret & (1 << i);
                    if (tmp != 0) {
                        Log.d(TAG, "tmp = " + tmp);
                        if (i < 5) { //管理员
                            diffIds.add(String.valueOf(i + 1));
                        } else if (i < 10) {
                            diffIds.add(String.valueOf(i + 196));
                        } else {
                            diffIds.add(String.valueOf(i + 91));
                        }
                    }

                }
            }
        }

        return diffIds;
    }

    @Override
    public DeviceUser queryDefaultUsers(Object nodeId) {
        DeviceUser deviceUser = null;
        try {
            deviceUser = dao.queryBuilder().orderBy("create_time", true).where().eq("dev_node_id", nodeId).queryForFirst();
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
