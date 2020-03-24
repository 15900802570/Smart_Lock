package com.smart.lock.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.smart.lock.MainBaseActivity;
import com.smart.lock.R;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.entity.ProblemModel;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.GetJsonDataUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class QuestionActivity extends MainBaseActivity implements View.OnClickListener {
    private ImageView mBack;
    private ProblemModel mProblemModel; //问题实例
    private TextView mQuestionTv; //问题
    private TextView mAnswerTv; //回答


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);
        mBack = findViewById(R.id.iv_back);
        mBack.setOnClickListener(this);
        mQuestionTv = findViewById(R.id.tv_question);
        mAnswerTv = findViewById(R.id.tv_answer);
        mProblemModel = (ProblemModel) getIntent().getSerializableExtra(ConstantUtil.KEY_PROBLEM);
        Log.d("", "mProblemModel : " +  mProblemModel.toString());
        if (mProblemModel != null) initData();
    }


    private void initData() {
        mQuestionTv.setText(mProblemModel.question);
        mAnswerTv.setText(mProblemModel.answer);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_back:
                finish();
                break;
            default:
                break;
        }
    }
}
