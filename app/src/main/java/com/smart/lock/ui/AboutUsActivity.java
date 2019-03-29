package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.smart.lock.R;

public class AboutUsActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView mVersionTv;
    private ImageView mBack;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);
        mVersionTv = findViewById(R.id.tv_version);
        mBack = findViewById(R.id.iv_about_us_back);
        mBack.setOnClickListener(this);
        try {
            mVersionTv.setText(getString(R.string.app_name) + " " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_about_us_back:
                finish();
                break;
            default:
                break;
        }
    }
}
