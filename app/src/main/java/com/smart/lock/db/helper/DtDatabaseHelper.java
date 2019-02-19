
package com.smart.lock.db.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.utils.LogUtil;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DtDatabaseHelper extends OrmLiteSqliteOpenHelper {

    public static final String DB_NAME = "smart_lock.db";
    public static final int DB_VERSION = 1;

    public DtDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, DeviceInfo.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource,
            int oldVersion, int newVersion) {
        LogUtil.d("MyDatabaseHelper.onUpgrade oldVersion=" + oldVersion + "  newVersion="
                + newVersion);
    }

    private static DtDatabaseHelper instance;

    /**
     * 单例获取该Helper
     * 
     * @param context
     * @return
     */
    public static DtDatabaseHelper getHelper(Context context) {
        if (instance == null) {
            synchronized (DtDatabaseHelper.class) {
                if (instance == null)
                    instance = new DtDatabaseHelper(context);
            }
        }
        return instance;
    }

    private Map<String, Dao> daos = new HashMap<>();

    public synchronized Dao getDao(Class clazz) throws SQLException {
        Dao dao = null;
        String className = clazz.getSimpleName();
        if (daos.containsKey(className)) {
            dao = daos.get(clazz);
        }
        if (dao == null) {
            dao = super.getDao(clazz);
            daos.put(className, dao);
        }
        return dao;
    }

    @Override
    public void close() {
        super.close();
        for (String key : daos.keySet()) {
            Dao dao = daos.get(key);
            dao = null;
        }
    }

}
