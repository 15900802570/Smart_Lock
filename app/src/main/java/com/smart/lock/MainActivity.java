package com.smart.lock;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.smart.lock.ble.BleManagerHelper;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.ui.LockDetectingActivity;
import com.smart.lock.ui.fragment.BaseFragment;
import com.smart.lock.ui.fragment.HomeFragment;
import com.smart.lock.ui.fragment.MeFragment;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.NoScrollViewPager;
import com.yzq.zxinglibrary.android.CaptureActivity;
import com.yzq.zxinglibrary.bean.ZxingConfig;
import com.yzq.zxinglibrary.common.Constant;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    //back time
    private long mBackPressedTime;

    private NoScrollViewPager mTabVg;
    private RadioGroup mTabRg;

    //fragment list
    private List<BaseFragment> mPagerList;

    private ImageView mOneClickOpen;
    private int mHeight;

    private HomeFragment mHomeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHeight = this.getResources().getDisplayMetrics().heightPixels;

        initView();
        initDate();
        initEvent();
    }

    private void initView() {
        mTabVg = findViewById(R.id.fragment_vg);
        mTabRg = findViewById(R.id.tab_rg);
        mOneClickOpen = findViewById(R.id.one_click_unlock_ib);
        mOneClickOpen.setPadding(0, 0, 0, (int) (mHeight * 0.025));
        findViewById(R.id.rl_home).getLayoutParams().height = (int) (mHeight * 0.085);
    }

    private void initEvent() {
        mTabRg.setOnCheckedChangeListener(this);
        mTabRg.check(R.id.home_rd);
    }

    private void initDate() {
        BleManagerHelper.getInstance(this, false);
        mPagerList = new ArrayList();
        mHomeFragment = new HomeFragment();
        mHomeFragment.setmInstructionBtn(mOneClickOpen);
        mPagerList.add(mHomeFragment);
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
                if (DeviceInfoDao.getInstance(this).queryFirstData("device_default", true) != null) {
                    findViewById(R.id.one_click_unlock_ib).setVisibility(View.VISIBLE);
                }else {
                    findViewById(R.id.one_click_unlock_ib).setVisibility(View.GONE);
                }
                mTabVg.setCurrentItem(0, false);
                break;
            case R.id.me_rd:
                findViewById(R.id.one_click_unlock_ib).setVisibility(View.GONE);
                mTabVg.setCurrentItem(1, false);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        LogUtil.d(TAG, "onDestroy!");
        super.onDestroy();
        BleManagerHelper.getInstance(this, false).stopService();
    }


    /**
     * 新界面
     *
     * @param cls    新Activity
     * @param bundle 数据包
     */
    protected void startIntent(Class<?> cls, Bundle bundle) {
        Intent intent = new Intent();
        if (bundle != null) {
            intent.putExtras(bundle);
        }

        intent.setClass(this, cls);
        startActivity(intent);
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

    public void onResume(){
        super.onResume();
        if (DeviceInfoDao.getInstance(this).queryFirstData("device_default", true) != null) {
            findViewById(R.id.one_click_unlock_ib).setVisibility(View.VISIBLE);
        }else {
            findViewById(R.id.one_click_unlock_ib).setVisibility(View.GONE);
        }
        mHomeFragment.setmInstructionBtn(mOneClickOpen);
    }
}
