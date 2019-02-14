
package com.smart.lock.fragment;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;

import com.smart.lock.R;
import com.smart.lock.adapter.LockManagerAdapter;
import com.smart.lock.adapter.ViewPagerAdapter;
import com.smart.lock.widget.MyGridView;

import java.util.ArrayList;


public class HomeFragment extends BaseFragment implements View.OnClickListener, AdapterView.OnItemClickListener{

    private Toolbar mToolbar;
    private View mHomeView;
    private ViewPager mViewPager;
    private ViewPagerAdapter mAdapter;
    private ArrayList<View> mDots;
    private int mOldPosition = 0;// 记录上一次点的位置
    private int mCurrentItem; // 当前页面

    private MyGridView mMyGridView;
    private LockManagerAdapter mLockAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public View initView() {
        mHomeView = View.inflate(mActivity, R.layout.home_fragment, null);
        mViewPager = mHomeView.findViewById(R.id.news_vp);
        mMyGridView = mHomeView.findViewById(R.id.gv_lock);
        return mHomeView;
    }


    public void onDestroy() {
        super.onDestroy();
    }



    public void initDate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {  //版本检测
            mToolbar = mHomeView.findViewById(R.id.tb_toolbar);
            ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);  //将ToolBar设置成ActionBar
        }
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);

        // 显示的点
        mDots = new ArrayList<View>();
        mDots.add((View) getView().findViewById(R.id.dot_0));
        mDots.add((View) getView().findViewById(R.id.dot_1));

        mAdapter = new ViewPagerAdapter(mHomeView.getContext());
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                // TODO Auto-generated method stub

                mDots.get(mOldPosition).setBackgroundResource(R.drawable.dot_normal);
                mDots.get(position).setBackgroundResource(R.drawable.dot_focused);

                mOldPosition = position;
                mCurrentItem = position;
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
                // TODO Auto-generated method stub

            }
        });

        mLockAdapter = new LockManagerAdapter(mHomeView.getContext(), mMyGridView);
        mMyGridView.setAdapter(mLockAdapter);
        mMyGridView.setOnItemClickListener(this);
    }

    public void onResume() {
        super.onResume();
    }

    public void onPause() {
        super.onPause();
    }


    @Override
    public void onClick(View v) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        switch (((Integer) view.getTag()).intValue()) {
            case R.mipmap.manager_pwd:
//                startIntent(PasswordManagerActivity.class, bundle);

                break;
            case R.mipmap.manager_card:
//                startIntent(CardManagerActivity.class, bundle);
                break;

            case R.mipmap.manager_finger:
//                startIntent(FingerprintManagerActivity.class, bundle);
                break;

            case R.mipmap.manager_event:
//               startIntent(LockInfoActivity.class, bundle);
                break;

            case R.mipmap.manager_permission:

                break;

            case R.mipmap.manager_token:
//               startIntent(TokenManagerActivitySwipeAdapter.class, bundle);
                break;
            default:
                break;

        }
    }
}
