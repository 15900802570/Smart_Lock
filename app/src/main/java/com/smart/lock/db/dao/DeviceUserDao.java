
package com.smart.lock.db.dao;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.smart.lock.R;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceStatus;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.helper.DtDatabaseHelper;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

public class DeviceUserDao {
    private final String TAG = DeviceUserDao.class.getSimpleName();

    private DtDatabaseHelper mHelper;
    private Dao<DeviceUser, Integer> dao;
    private Context mContext;
    private static DeviceUserDao instance;

    public static String DEVICE_NODE_ID = "dev_node_id";

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

    public DeviceUser queryFirstData(String key, Object value) {
        DeviceUser deviceUser = null;
        try {
            deviceUser = dao.queryBuilder().where().eq(key, value).queryForFirst();
            if (deviceUser != null) {
                return deviceUser;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void insert(DeviceUser deviceUser) {

        try {
            dao.create(deviceUser);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void insert(ArrayList<DeviceUser> beanArrayList) {
        try {
            dao.create(beanArrayList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void updateDeviceUser(DeviceUser info) {
        try {
            dao.update(info);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


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

    public synchronized int delete(DeviceUser info) {
        try {
            if (info.getQrPath() != null) {
                File delQr = new File(info.getQrPath());
                if (delQr.exists()) {
                    LogUtil.d(TAG, "isFile = " + delQr.isFile());
                    boolean result = delQr.delete();
                    if (result) {
                        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + info.getQrPath())));
                    }
                    LogUtil.d(TAG, "result = " + result + " isFile = " + delQr.isFile());
                } else
                    LogUtil.d(TAG, "删除文件失败");
            }


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

    public ArrayList<DeviceUser> queryId(int id) {
        ArrayList<DeviceUser> list = new ArrayList<>();

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


    public ArrayList<DeviceUser> queryAll() {
        ArrayList<DeviceUser> list = new ArrayList<>();
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


    public ArrayList<DeviceUser> queryKey(String key, Object valus) {
        ArrayList<DeviceUser> list = new ArrayList<>();
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


    public synchronized DeviceUser queryUser(Object nodeId, Object userId) {
        DeviceUser deviceUser = new DeviceUser();
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

    public synchronized DeviceUser queryOrCreateByNodeId(Object nodeId, short userId, String authCode) {
        DeviceUser deviceUser = null;
        try {
            deviceUser = dao.queryBuilder().where().eq(DEVICE_NODE_ID, nodeId).and().eq("user_id", userId).queryForFirst();
            if (deviceUser != null) {
                return deviceUser;
            } else if (authCode != null) {
                deviceUser = new DeviceUser();
                deviceUser.setDevNodeId((String) nodeId);
                deviceUser.setAuthCode(authCode);
                deviceUser.setUserId(userId);
                deviceUser.setCreateTime(System.currentTimeMillis() / 1000);
                if (userId < 101) {
                    deviceUser.setUserPermission(ConstantUtil.DEVICE_MASTER);
                    deviceUser.setUserName(mContext.getString(R.string.administrator) + userId);
                } else if (userId < 201) {
                    deviceUser.setUserPermission(ConstantUtil.DEVICE_MEMBER);
                    deviceUser.setUserName(mContext.getString(R.string.members) + userId);
                } else {
                    deviceUser.setUserPermission(ConstantUtil.DEVICE_TEMP);
                    deviceUser.setUserName(mContext.getString(R.string.tmp_user) + userId);
                }
                dao.create(deviceUser);
                deviceUser = dao.queryBuilder().where().eq(DEVICE_NODE_ID, nodeId).and().eq("user_id", userId).queryForFirst();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return deviceUser;
    }


    public ArrayList<DeviceUser> queryUsers(Object nodeId, Object permission) {
        ArrayList<DeviceUser> list = new ArrayList<>();
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


    public ArrayList<DeviceUser> queryDeviceUsers(Object nodeId) {
        ArrayList<DeviceUser> list = new ArrayList<>();
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


    public ArrayList<Short> queryDeviceUserIds(Object nodeId) {
        ArrayList<Short> list = new ArrayList<>();
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
    public long getUserStatus(String nodeId, int num) {
        long ret = 0 & 0xFFFFFFFF;
        long index = 0;
        ArrayList<Short> userIds = queryDeviceUserIds(nodeId);
        ArrayList<Long> indexs = new ArrayList<>();

        if (userIds != null && !userIds.isEmpty()) {
            for (Short userId : userIds) {
                if (userId > 0 && userId <= 5) { //管理员编号
                    index = userId - 1;
                } else if (userId > 100 && userId <= 200) { //普通用户
                    index = userId - (101 - (ConstantUtil.ADMIN_USR_NUM + ConstantUtil.TMP_USR_NUM));
                } else if (userId > 200 && userId <= 300) { //临时用户
                    index = userId - (201 - (ConstantUtil.ADMIN_USR_NUM));
                }
                synchronized (this) {
                    indexs.add(index);
                }
            }
        }
        if (!indexs.isEmpty()) {
            Collections.sort(indexs);
            for (long i : indexs) {
                if ((num - 1) * 32 <= i && i < num * 32) {
                    i = i - (num - 1) * 32;
                    ret |= 1L << i;
                }

            }
        }

        return ret;
    }


    /**
     * 更新用户状态
     */
    public synchronized void checkUserState(String nodeId, byte[] status) {
        LogUtil.d(TAG,"status : " + StringUtil.bytesToHexString(status,":"));
        int index = 0;
        ArrayList<DeviceUser> users = queryDeviceUsers(nodeId);

        if (users != null && !users.isEmpty()) {
            for (DeviceUser user : users) {
                short id = user.getUserId();
                if (id > 0 && id <= 5) { //管理员编号
                    index = id - 1;
                } else if (id > 100 && id <= 200) { //普通用户
                    index = id - (101 - (ConstantUtil.ADMIN_USR_NUM + ConstantUtil.TMP_USR_NUM)); //91
                } else if (id > 200 && id <= 300) { //临时用户
                    index = id - (201 - (ConstantUtil.ADMIN_USR_NUM)); //196
                }

                if (index >= status.length) {
                    return;
                }
                LogUtil.d(TAG, "id : " + id + " index : " + index + " status : " + status[index]);
                if (status[index] != user.getUserStatus()) {
                    user.setUserStatus(status[index]);
                    updateDeviceUser(user);
                }
            }

        }
    }

    /**
     * 检查状态字，实现用户同步功能
     *
     * @param status 秘钥状态字
     */
    public ArrayList<Short> checkUserStatus(long status, String nodeId, int num) {
        ArrayList<Short> diffIds = new ArrayList<>();
        long ret = 0;
        long tmp = 0;
        long userStatus = getUserStatus(nodeId, num);  //本地用户状态字
        Log.d(TAG, "userStatus = " + userStatus);

        ret = status ^ (userStatus & 0xFFFFFFFF);  //异或得出差异

        synchronized (this) {
            if (ret != 0) {
                for (int i = (num - 1) * 32; i < 32 * num; i++) {
                    tmp = ret & (1 << i);  //位移得出index指引的数是否为1,1即存在用户。
                    if (tmp != 0) {
                        if (i < ConstantUtil.ADMIN_USR_NUM) { //管理员
                            diffIds.add((short) (i + 1));
                        } else if (i < (ConstantUtil.ADMIN_USR_NUM + ConstantUtil.TMP_USR_NUM)) {
                            diffIds.add((short) (i + (201 - (ConstantUtil.ADMIN_USR_NUM))));
                        } else if (i < (ConstantUtil.ADMIN_USR_NUM + ConstantUtil.TMP_USR_NUM + ConstantUtil.COMMON_USR_NUM)) {
                            diffIds.add((short) (i + (101 - (ConstantUtil.ADMIN_USR_NUM + ConstantUtil.TMP_USR_NUM))));
                        }
                    }

                }
            }
        }

        return diffIds; //返回不同的用户ID。
    }


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

    public void registerObserver(Dao.DaoObserver ob) {
        dao.registerObserver(ob);
    }

    public void unregisterObserver(Dao.DaoObserver ob) {
        dao.unregisterObserver(ob);
    }

}
