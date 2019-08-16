package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.smart.lock.MainActivity;
import com.smart.lock.R;
import com.smart.lock.adapter.LangnageAdapter;
import com.smart.lock.entity.LangnageModel;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.LanguageType;
import com.smart.lock.utils.LanguageUtil;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.SharedPreferenceUtil;

import java.util.ArrayList;
import java.util.List;

public class LanguageActivity extends AppCompatActivity {

    private final String TAG = LanguageActivity.class.getSimpleName();

    private TextView mTitleTv;
    private Toolbar mLanguageTb;
    private ListView mMulitLangnageLv;
    private String mLangnage;
    private MenuItem mSaveItem;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_language);

        initView();
        initData();
        initActionBar();
    }

    @SuppressLint("WrongViewCast")
    private void initView() {
        mLanguageTb = findViewById(R.id.tb_language_set);
        mTitleTv = findViewById(R.id.tv_title);
        mMulitLangnageLv = findViewById(R.id.lv_language);
    }

    private void initData() {
        final List<LangnageModel> langnages = new ArrayList<>();
        mLangnage = SharedPreferenceUtil.getInstance(this).readString(ConstantUtil.DEFAULT_LANGNAGE, "");

        LangnageModel chModel = new LangnageModel();
        chModel.setLangnage(LanguageType.CHINESE.getLanguage());
        if (mLangnage.equals(LanguageType.CHINESE.getLanguage())) chModel.setDefaultLangnage(true);
        else chModel.setDefaultLangnage(false);
        langnages.add(chModel);

        LangnageModel enModel = new LangnageModel();
        enModel.setLangnage(LanguageType.ENGLISH.getLanguage());
        if (mLangnage.equals(LanguageType.ENGLISH.getLanguage())) enModel.setDefaultLangnage(true);
        else enModel.setDefaultLangnage(false);
        langnages.add(enModel);

        final LangnageAdapter adapter = new LangnageAdapter(this, R.layout.item_language, langnages);

        mMulitLangnageLv.setAdapter(adapter);

        mMulitLangnageLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mLangnage = langnages.get(position).getLangnage();
                adapter.selectLangnage(position);
                String defaultLangnage = SharedPreferenceUtil.getInstance(LanguageActivity.this).readString(ConstantUtil.DEFAULT_LANGNAGE, LanguageType.CHINESE.getLanguage());
                if (defaultLangnage.equals(mLangnage)) {
                    mSaveItem.setEnabled(false);
                } else mSaveItem.setEnabled(true);
            }
        });
    }

    private void initActionBar() {
        mTitleTv.setText(R.string.multi_language);

        mLanguageTb.setNavigationIcon(R.mipmap.btn_back);
        mLanguageTb.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        setSupportActionBar(mLanguageTb);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.langnage, menu);
        mSaveItem = menu.findItem(R.id.item_save);
        mSaveItem.setEnabled(false);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.item_save:
//                mLangnage = SharedPreferenceUtil.getInstance(this).readString(ConstantUtil.DEFAULT_LANGNAGE);
                LogUtil.d(TAG, "save : " + mLangnage);
                changeLanguage(mLangnage);
                mSaveItem.setEnabled(false);
                break;
            default:
                break;
        }

        return true;
    }


    /**
     * 如果是7.0以下，我们需要调用changeAppLanguage方法，
     * 如果是7.0及以上系统，直接把我们想要切换的语言类型保存在SharedPreferences中,然后重新启动MainActivity即可
     *
     * @param language
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void changeLanguage(String language) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            LanguageUtil.changeAppLanguage(this, language);
        }
        SharedPreferenceUtil.getInstance(this).writeString(ConstantUtil.DEFAULT_LANGNAGE, language);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

}
