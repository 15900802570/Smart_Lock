package com.smart.lock.ui.setting;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.smart.lock.R;


public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {



    @Override
    protected void onCreate(Bundle savedInstancesState) {
        super.onCreate(savedInstancesState);
//        try {
            setContentView(R.layout.activity_setting);
//        }catch (Exception e){
//            Log.e("Setting",String.valueOf(e));
//        }


        initView();
        initDate();
        initEvent();
    }

    public void initView(){

    }

    private void initDate(){

    }

    private void initEvent(){

    }

    @Override
    public void onClick(View v){

    }
}
