package com.smart.lock;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import com.smart.lock.ui.fragment.BaseFragment;
import com.smart.lock.ui.fragment.HomeFragment;
import com.smart.lock.ui.fragment.MeFragment;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.NoScrollViewPager;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {

    //back time
    private long mBackPressedTime;

    private NoScrollViewPager mTabVg;
    private RadioGroup mTabRg;

    //fragment list
    private List<BaseFragment> mPagerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initDate();
        initEvent();
    }

    private void initView() {
        mTabVg = findViewById(R.id.fragment_vg);
        mTabRg = findViewById(R.id.tab_rg);
    }

    private void initEvent() {
        mTabRg.setOnCheckedChangeListener(this);
        mTabRg.check(R.id.home_rd);
    }

    private void initDate() {
        mPagerList = new ArrayList();
        mPagerList.add(new HomeFragment());
        mPagerList.add(new MeFragment());
        mTabVg.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            public Fragment getItem(int i) {
                return mPagerList.get(i);
            }

            public Object instantiateItem(ViewGroup container, int position) {
                return super.instantiateItem(container, position);
            }

            public int getCount() {
                return mPagerList.size();
            }
        });
        mTabVg.setNoScroll(true);
    }


    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.home_rd:
                mTabVg.setCurrentItem(0, false);
                break;
            case R.id.me_rd:
                mTabVg.setCurrentItem(1, false);
                break;
            default:
                break;
        }
    }

    public void onBackPressed() {
        long curTime = SystemClock.uptimeMillis();
        if (curTime - mBackPressedTime < 3000) {
            finish();
            return;
        }
        mBackPressedTime = curTime;
        ToastUtil.showShort(this, getString(R.string.back_message));
    }
}
