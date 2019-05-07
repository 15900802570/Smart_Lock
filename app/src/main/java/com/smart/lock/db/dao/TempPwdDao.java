package com.smart.lock.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.smart.lock.db.bean.TempPwd;
import com.smart.lock.db.helper.DtDatabaseHelper;
import com.smart.lock.utils.LogUtil;

import java.sql.SQLException;
import java.util.ArrayList;

public class TempPwdDao {

    private DtDatabaseHelper mHelper;
    private Dao<TempPwd, Integer> dao;
    private Context mContext;
    private static TempPwdDao instance;
    public static String DEVICE_NODE_ID = "device_nodeId";

    private TempPwdDao(Context context) {
        this.mContext = context;
        try {
            mHelper = DtDatabaseHelper.getHelper(mContext);
            dao = mHelper.getDao(TempPwd.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static TempPwdDao getInstance(Context context) {
        if (instance == null) {
            synchronized (TempPwdDao.class) {
                if (instance == null) {
                    instance = new TempPwdDao(context);
                }
            }
        }
        return instance;
    }

    public void insert(ArrayList<TempPwd> beanArrayList) {
        try {
            dao.create(beanArrayList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insert(TempPwd TempPwd) {
        try {
            dao.create(TempPwd);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int deleteAll() {
        int num = -1;
        try {
            num = dao.deleteBuilder().delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return num;
    }

    public int deleteAllByNodeId(Object object) {
        int num = -1;
        try {
            ArrayList<TempPwd> list = (ArrayList<TempPwd>) dao.queryBuilder().where().eq(DEVICE_NODE_ID, object).query();
            for (TempPwd i : list) {
                dao.delete(i);
            }
            num = 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        LogUtil.d("num = " + num);
        return num;
    }

    public ArrayList<TempPwd> queryId(int id) {
        ArrayList<TempPwd> list = null;
        try {
            TempPwd tempPwd = dao.queryForId(id);
            if (tempPwd != null) {
                list = new ArrayList<>();
                list.add(tempPwd);
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public TempPwd queryMaxCreateTime() {
        TempPwd tempPwd = null;
        try {
            tempPwd = dao.queryBuilder().orderBy("pwd_create_time", false).queryForFirst();
            if (tempPwd != null) {
                return tempPwd;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tempPwd;
    }

    public ArrayList<TempPwd> queryAllByDevNodeId(String devNodeId) {
        ArrayList<TempPwd> list = null;
        try {
            list = (ArrayList<TempPwd>) dao.queryBuilder().
                    orderBy("pwd_create_time", false).
                    where().eq("device_nodeId", devNodeId).
                    query();
            if (list != null) {
                return list;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void delete(TempPwd info) {
        try {
            dao.delete(info);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
