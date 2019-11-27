
package com.smart.lock.db.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.bean.DeviceKey;
import com.smart.lock.db.bean.DeviceLog;
import com.smart.lock.db.bean.DeviceStatus;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.bean.UserProfile;
import com.smart.lock.db.bean.TempPwd;
import com.smart.lock.db.dao.DeviceKeyDao;
import com.smart.lock.utils.LogUtil;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DtDatabaseHelper extends OrmLiteSqliteOpenHelper {

    public static final String DB_NAME = "smart_lock.db";
    public static final int DB_VERSION = 2;
    private Context mCtx;

    public DtDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mCtx = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, DeviceInfo.class);
            TableUtils.createTable(connectionSource, DeviceKey.class);
            TableUtils.createTable(connectionSource, DeviceUser.class);
            TableUtils.createTable(connectionSource, DeviceLog.class);
            TableUtils.createTable(connectionSource, UserProfile.class);
            TableUtils.createTable(connectionSource, TempPwd.class);
            TableUtils.createTable(connectionSource, DeviceStatus.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource,
                          int oldVersion, int newVersion) {
        LogUtil.d("onUpgrade oldVersion=" + oldVersion + "  newVersion=" + newVersion);
        try {
            for (int i = oldVersion; i < newVersion; i++) {
                switch (i) {
                    case 1://数据库版本1 升级到 版本2
                        //新增设备启动状态 NFC 与FACE ，
                        // tb_device_info enableNFC=0 时，表示NFC启用，
                        String info1 = "ALTER TABLE tb_device_info add unable_nfc INTEGER DEFAULT 0";
                        // tb_device_info enableFace=1 时， 表示FACE启动   ***NFC与FACE刚好相反***
                        String info2 = "ALTER TABLE tb_device_info add enable_face INTEGER DEFAULT 0";
                        //enable_infrared 是否支持红外
                        String info3 = "ALTER TABLE tb_device_info add enable_infrared INTEGER DEFAULT 0";
                        //enable_pwd 是否支持密码长度可变
                        String info4 = "ALTER TABLE tb_device_info add enable_variable_pwd INTEGER DEFAULT 0";
                        // SCPU
                        String info5 = "ALTER TABLE tb_device_info add face_scpu_version TEXT";
                        // NCPU
                        String info6 = "ALTER TABLE tb_device_info add face_ncpu_version TEXT";
                        //DataModule
                        String info7 = "ALTER TABLE tb_device_info add face_module_version TEXT";
                        getDao(DeviceInfo.class).executeRawNoArgs(info1);
                        getDao(DeviceInfo.class).executeRawNoArgs(info2);
                        getDao(DeviceInfo.class).executeRawNoArgs(info3);
                        getDao(DeviceInfo.class).executeRawNoArgs(info4);
                        getDao(DeviceInfo.class).executeRawNoArgs(info5);
                        getDao(DeviceInfo.class).executeRawNoArgs(info6);
                        getDao(DeviceInfo.class).executeRawNoArgs(info7);

                        //tb_device_status autoOpen 自动开锁开关
                        String status1 = "ALTER TABLE tb_device_status add auto_close_enable INTEGER DEFAULT 0";
                        //tb_device_status 红外开关
                        String status2 = "ALTER TABLE tb_device_status add infrared_enable INTEGER DEFAULT 0";
                        getDao(DeviceStatus.class).executeRawNoArgs(status1);
                        getDao(DeviceStatus.class).executeRawNoArgs(status2);
                        break;
                    case 2://数据库版本2 升级到 版本3
                        break;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

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
