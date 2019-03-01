package com.smart.lock.db.impl;

import com.smart.lock.db.bean.TempPwd;

import java.util.ArrayList;

public interface TempPwdImpl {

    void insert (ArrayList<TempPwd> beanArrayList);

    void insert(TempPwd myBean);

    int deleteAll();

    ArrayList<TempPwd> queryId(int id);

    ArrayList<TempPwd> queryAll();

    void delete(TempPwd info);
}
