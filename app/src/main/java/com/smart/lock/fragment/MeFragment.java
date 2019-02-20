
package com.smart.lock.fragment;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.smart.lock.R;
import com.smart.lock.ui.setting.SettingsActivity;
import com.smart.lock.widget.MeDefineView;

public class MeFragment extends BaseFragment implements View.OnClickListener{
    private View mMeView;
    private Toolbar mToolbar;
    private MeDefineView mSystemSetTv;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public View initView() {
        mMeView = View.inflate(mActivity, R.layout.me_fragment, null);
        mSystemSetTv = mMeView.findViewById(R.id.system_set);
        Log.d(TAG, "initView");
        return mMeView;
    }


    public void initDate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {  //版本检测
            mToolbar = mMeView.findViewById(R.id.tb_toolbar);
            ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);  //将ToolBar设置成ActionBar
        }
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
        mSystemSetTv.setDes(mMeView.getContext().getResources().getString(R.string.system_setting));
        mSystemSetTv.setOnClickListener(this);

    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.self_message:
                Log.e("self_message","0012012");
                break;
            case R.id.mc_manage:
                Log.e("mc_manage","0012012");
                break;
            case R.id.sent_repair:
                Log.e("sent_repair","0012012");
                break;
            case R.id.system_set:
                Intent intent = new Intent( this.mActivity, SettingsActivity.class);
                this.startActivity(intent);
                break;
        }
    }
    @Override
    public void onResume() {
        super.onResume();
    }
}
