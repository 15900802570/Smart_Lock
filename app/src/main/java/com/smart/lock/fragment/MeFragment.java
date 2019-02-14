
package com.smart.lock.fragment;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.smart.lock.R;

public class MeFragment extends BaseFragment {
    private View mMeView;
    private Toolbar mToolbar;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public View initView() {
        mMeView = View.inflate(mActivity, R.layout.me_fragment, null);
        Log.d(TAG,"initView");
        return mMeView;
    }


    public void initDate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {  //版本检测
            mToolbar = mMeView.findViewById(R.id.tb_toolbar);
            ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);  //将ToolBar设置成ActionBar
        }
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
    }


    @Override
    public void onResume() {
        super.onResume();
    }
}
