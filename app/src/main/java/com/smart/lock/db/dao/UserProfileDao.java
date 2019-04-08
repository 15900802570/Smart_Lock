
package com.smart.lock.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.smart.lock.db.bean.UserProfile;
import com.smart.lock.db.helper.DtDatabaseHelper;

import java.sql.SQLException;
import java.util.ArrayList;

public class UserProfileDao  {

    private DtDatabaseHelper mHelper;
    private Dao<UserProfile, Integer> dao;
    private Context mContext;
    private static UserProfileDao instance;

    protected UserProfileDao(Context context) {
        this.mContext = context;
        try {
            mHelper = DtDatabaseHelper.getHelper(mContext);
            dao = mHelper.getDao(UserProfile.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static UserProfileDao getInstance(Context context) {
        if (instance == null) {
            synchronized (UserProfileDao.class) {
                if (instance == null) {
                    instance = new UserProfileDao(context);
                }
            }

        }
        return instance;
    }

    
    public void insert(UserProfile userProfile) {

        try {
            dao.create(userProfile);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    
    public void insert(ArrayList<UserProfile> beanArrayList) {
        try {
            dao.create(beanArrayList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    
    public void updateUserName(String name, String pram) {
        ArrayList<UserProfile> list = null;
        try {
            list = (ArrayList<UserProfile>) dao.queryForEq(name, pram);
            if (list != null) {
                for (UserProfile bean : list) {
                    bean.setUserName(pram);
                    dao.update(bean);
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    
    public void updateUserSex(String name, String pram) {
        ArrayList<UserProfile> list = null;
        try {
            list = (ArrayList<UserProfile>) dao.queryForEq(name, pram);
            if (list != null) {
                for (UserProfile bean : list) {
                    bean.setSex(pram);
                    dao.update(bean);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    
    public void updateUserBirthday(String name, String pram) {
        ArrayList<UserProfile> list = null;
        try {
            list = (ArrayList<UserProfile>) dao.queryForEq(name, pram);
            if (list != null) {
                for (UserProfile bean : list) {
                    bean.setBirthday(pram);
                    dao.update(bean);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    
    public void updateUserAddress(String name, String pram) {
        ArrayList<UserProfile> list = null;
        try {
            list = (ArrayList<UserProfile>) dao.queryForEq(name, pram);
            if (list != null) {
                for (UserProfile bean : list) {
                    bean.setAddress(pram);
                    dao.update(bean);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    
    public void updateUserMobile(String name, String pram) {
        ArrayList<UserProfile> list = null;
        try {
            list = (ArrayList<UserProfile>) dao.queryForEq(name, pram);
            if (list != null) {
                for (UserProfile bean : list) {
                    bean.setMobile(pram);
                    dao.update(bean);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    
    public void update(UserProfile userProfile)  {
        try {
            dao.update(userProfile);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void delete(String name) {
        ArrayList<UserProfile> list = null;
        try {
            list = (ArrayList<UserProfile>) dao.queryForEq("name", name);
            if (list != null) {
                for (UserProfile bean : list) {
                    dao.delete(bean);
                }
            }
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

            // dao.deleteBuilder().where().eq("name", "记").reset();//????
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
    
    public ArrayList<UserProfile> queryId(int id) {
        ArrayList<UserProfile> list = null;

        try {
            UserProfile UserProfile = dao.queryForId(id);
            if (UserProfile != null) {
                list = new ArrayList<>();
                list.add(UserProfile);
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    
    public UserProfile queryById(int id) {
        try {
            UserProfile userProfile = dao.queryForId(id);
            return userProfile;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    
    public ArrayList<UserProfile> queryAll() {
        ArrayList<UserProfile> list = null;
        try {
            list = (ArrayList<UserProfile>) dao.queryForAll();

            if (list != null) {
                return list;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean delteTables(Context context, String DBname) {
        // ?????
        return false;
    }

    /**
     * 这个方法可以的
     */
    public boolean delteDatabases(Context context, String DBname) {
        return context.deleteDatabase(DBname);
    }

    
    public ArrayList<UserProfile> queryParm(String name, String parm) {
        ArrayList<UserProfile> list = null;
        try {
            list = (ArrayList<UserProfile>) dao
                    .queryForEq(name, parm);
            if (list != null) {
                return list;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
