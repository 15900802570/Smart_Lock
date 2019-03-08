package com.smart.lock.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.smart.lock.db.bean.TempPwd;
import com.smart.lock.db.helper.DtDatabaseHelper;
import com.smart.lock.db.impl.TempPwdImpl;

import java.sql.SQLDataException;
import java.sql.SQLException;
import java.util.ArrayList;

public class TempPwdDao implements TempPwdImpl {

    private DtDatabaseHelper mHelper;
    private Dao<TempPwd,Integer> dao;
    private Context mContext;
    private static TempPwdDao instance;

    protected TempPwdDao(Context context){
        this.mContext = context;
        try {
            mHelper=DtDatabaseHelper.getHelper(mContext);
            dao = mHelper.getDao(TempPwd.class);
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public static TempPwdDao getInstance(Context context){
        if(instance == null){
            synchronized (TempPwdDao.class){
                if(instance == null){
                    instance = new TempPwdDao(context);
                }
            }
        }
        return instance;
    }
    @Override
    public void insert(ArrayList<TempPwd> beanArrayList) {
        try {
            dao.create(beanArrayList);
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public void insert(TempPwd TempPwd) {
        try {
            dao.create(TempPwd);
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public int deleteAll() {
        int num = -1;
        try {
            num = dao.deleteBuilder().delete();
        }catch (SQLException e){
            e.printStackTrace();
        }
        return num;

    }

    @Override
    public ArrayList<TempPwd> queryId(int id) {
        ArrayList<TempPwd> list = null;
        try {
            TempPwd tempPwd=dao.queryForId(id);
            if(tempPwd != null){
                list = new ArrayList<>();
                list.add(tempPwd);
            }
            return list;
        }catch (SQLException e){
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public ArrayList<TempPwd> queryAllByDevNodeId(String devNodeId) {
        ArrayList<TempPwd> list = null;
        try {
            list = (ArrayList<TempPwd>)dao.queryBuilder().
                    orderBy("pwd_create_time",false).
                    where().eq("device_nodeId",devNodeId).
                    query();
            if(list != null){
                return list;
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void delete(TempPwd info) {
        try {
            dao.delete(info);
        }catch (SQLException e){
            e.printStackTrace();
        }
    }
}
