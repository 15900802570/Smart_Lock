package com.smart.lock.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.JsonObject;
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

public class HelpListActivity extends MainBaseActivity implements View.OnClickListener {
    private ImageView mBack;
    private ListView mHelpList;
    private List<ProblemModel> mProblemList = new ArrayList<>();
    private String[] mQuestions;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_list);
        mBack = findViewById(R.id.iv_back);
        mBack.setOnClickListener(this);
        mHelpList = findViewById(R.id.help_list);
        parseProblem();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mQuestions);
        mHelpList.setAdapter(adapter);
        mHelpList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Bundle bundle = new Bundle();
                ProblemModel model = mProblemList.get(position);
                bundle.putSerializable(ConstantUtil.KEY_PROBLEM, model);
                Intent intent = new Intent();
                intent.setClass(HelpListActivity.this, QuestionActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
    }

    /**
     * 解析问题列表
     */
    private void parseProblem() {
        String fileName = "problem.json";

        String modelStr = GetJsonDataUtil.getJson(this, fileName);

        try {
            JSONObject jsonObject = new JSONObject(modelStr);

            JSONArray jsonArray = jsonObject.getJSONArray("probleModel");

            for (int i = 0; i < jsonArray.length(); i++) {
                ProblemModel model = new ProblemModel();
                JSONObject temp = (JSONObject) jsonArray.get(i);
                model.question = temp.getString("question");
                model.answer = temp.getString("answer");
                mProblemList.add(model);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        mQuestions = new String[mProblemList.size()];
        for (int i = 0; i < mProblemList.size(); i++) {
            mQuestions[i] = mProblemList.get(i).question;
        }
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
