package com.smart.lock.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.smart.lock.R;

import java.util.List;

public class HomeViewPage extends Fragment {
    public Activity mActivity;
    private ViewPager mViewPage;
    private List<BaseFragment> mViewPageList;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mActivity = getActivity();
    }

//    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState){
//        mViewPage = View.inflate(mActivity, R.layout.home_viewpage, null);
//        return mViewPage;
//    }
}
