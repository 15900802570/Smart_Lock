package com.smart.lock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;

import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.LanguageType;
import com.smart.lock.utils.LanguageUtil;
import com.smart.lock.utils.SharedPreferenceUtil;

@SuppressLint("Registered")
public class MainBaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        //获取我们存储的语言环境 比如 "en","zh",等等
        String language = SharedPreferenceUtil.getInstance(newBase).readString(ConstantUtil.DEFAULT_LANGUAGE, LanguageType.CHINESE.getLanguage());
        /**
         * attach对应语言环境下的context
         */
        super.attachBaseContext(LanguageUtil.attachBaseContext(newBase, language));
    }
}
