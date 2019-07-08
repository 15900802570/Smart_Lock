package com.smart.lock.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.smart.lock.db.bean.DeviceInfo;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.ui.fragment.ServerPagerFragment;

import java.util.ArrayList;

public class ServerPagerAdapter extends FragmentPagerAdapter {

    private ArrayList<ServerPagerFragment> mSeverPagerList;
    private int currentItem = Integer.MAX_VALUE;

    public ServerPagerAdapter(FragmentManager fragmentManager, Context context, ArrayList<DeviceInfo> deviceInfos) {
        super(fragmentManager);
        mSeverPagerList = new ArrayList<>();
        for (int i = 0; i < deviceInfos.size(); i++) {
            mSeverPagerList.add(new ServerPagerFragment());
            if (deviceInfos.get(i).getDeviceDefault()) {
                currentItem = i;
            }
        }
        if (currentItem == Integer.MAX_VALUE && deviceInfos.size() != 0) {
            deviceInfos.get(0).setDeviceDefault(true);
            DeviceInfoDao.getInstance(context).updateDeviceInfo(deviceInfos.get(0));
            currentItem = 0;
        }
    }

    public void updateDevices(Context context, ArrayList<DeviceInfo> deviceInfos) {
        mSeverPagerList = new ArrayList<>();
        for (int i = 0; i < deviceInfos.size(); i++) {
            mSeverPagerList.add(new ServerPagerFragment());
            if (deviceInfos.get(i).getDeviceDefault()) {
                currentItem = i;
            }
        }
        if (currentItem == Integer.MAX_VALUE && deviceInfos.size() != 0) {
            deviceInfos.get(0).setDeviceDefault(true);
            DeviceInfoDao.getInstance(context).updateDeviceInfo(deviceInfos.get(0));
            currentItem = 0;
        }
    }

    public int getCurrentItem() {
        return currentItem;
    }

    @Override
    public int getCount() {
        return mSeverPagerList.size();
    }

    @Override
    public Fragment getItem(int i) {
        currentItem = i;
        return mSeverPagerList.get(i);
    }


}
