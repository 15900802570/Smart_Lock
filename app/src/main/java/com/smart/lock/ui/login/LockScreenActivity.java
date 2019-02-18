package com.smart.lock.ui.login;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import com.smart.lock.MainActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.R;
import com.smart.lock.utils.SharedPreferenceUtil;

public class LockScreenActivity extends Activity implements View.OnClickListener {

    private TextView mTvDelete;
    private TextView mTvForgetPwd;
    // 数字键盘布局
    private NumericKeyboard nk;
    // 密码框
    private MyPasswordTextView et_pwd1, et_pwd2, et_pwd3, et_pwd4;
    private int type;
    private TextView tv_info;//提示信息
    //声明字符串保存每一次输入的密码
    private String input;
    private StringBuffer fBuffer = new StringBuffer();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //取消设置透明状态栏,使 ContentView 内容不再覆盖状态栏
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //需要设置这个 flag 才能调用 setStatusBarColor 来设置状态栏颜色
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        //设置状态栏颜色
        getWindow().setStatusBarColor(getResources().getColor(R.color.bg_color));

        setContentView(R.layout.activity_lock);
        //获取界面传递的值
        type = getIntent().getIntExtra("type", 1);
        initView();
        initListener();// 事件处理

    }

    private void initView() {
        nk = findViewById(R.id.nk);// 数字键盘
        // 密码框
        et_pwd1 = findViewById(R.id.et_pwd1);
        et_pwd2 = findViewById(R.id.et_pwd2);
        et_pwd3 = findViewById(R.id.et_pwd3);
        et_pwd4 = findViewById(R.id.et_pwd4);
        tv_info = findViewById(R.id.tv_info);//提示信息
        mTvDelete = findViewById(R.id.tv_delete);
        mTvForgetPwd = findViewById(R.id.tv_forget_pwd);
    }

    /**
     * 事件处理
     */
    private void initListener() {
        // 设置点击的按钮回调事件
        nk.setOnNumberClick(new NumericKeyboard.OnNumberClick() {
            @Override
            public void onNumberReturn(int number) {
                // 设置显示密码
                setText(String.valueOf(number));
            }
        });
        et_pwd1.setOnMyTextChangedListener(new MyPasswordTextView.OnMyTextChangedListener() {
            @Override
            public void textChanged(String content) {
                if (TextUtils.isEmpty(content)) {
                    mTvDelete.setVisibility(View.GONE);
                } else {
                    mTvDelete.setVisibility(View.VISIBLE);
                }
            }
        });
        //监听最后一个密码框的文本改变事件回调
        et_pwd4.setOnMyTextChangedListener(new MyPasswordTextView.OnMyTextChangedListener() {
            @Override
            public void textChanged(String content) {
                input = et_pwd1.getTextContent() + et_pwd2.getTextContent() +
                        et_pwd3.getTextContent() + et_pwd4.getTextContent();
                if (type == ConstantUtil.SETTING_PASSWORD) {//设置密码
                    //重新输入密码
                    tv_info.setText(getString(R.string.please_input_pwd_again));
                    type = ConstantUtil.SURE_SETTING_PASSWORD;
                    fBuffer.append(input);//保存第一次输入的密码
                    startTimer();
                } else if (type == ConstantUtil.LOGIN_PASSWORD) {//登录
                    if (!input.equals(SharedPreferenceUtil.getInstance(LockScreenActivity.this).readString("password"))) {
                        shakes();
                    } else {
                        startActivity(new Intent(LockScreenActivity.this, MainActivity.class));
                        finish();
                    }
                } else if (type == ConstantUtil.SURE_SETTING_PASSWORD) {//确认密码
                    //判断两次输入的密码是否一致
                    if (input.equals(fBuffer.toString())) {//一致
                        //保存密码到文件中
                        SharedPreferenceUtil.getInstance(LockScreenActivity.this).initSharedPreferences(LockScreenActivity.this);
                        SharedPreferenceUtil.getInstance(LockScreenActivity.this).writeString("password", input);
                        tv_info.setText(getString(R.string.please_input_pwd));
                        type = ConstantUtil.LOGIN_PASSWORD;
                    } else {//不一致
                        shakes();
                    }
                    startTimer();
                }
            }
        });
        mTvDelete.setOnClickListener(this);
        mTvForgetPwd.setOnClickListener(this);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            clearText();
        }
    };

    private void startTimer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.sendEmptyMessage(0);
            }
        }).start();
    }

    /**
     * 设置显示的密码
     *
     * @param text:String
     */
    private void setText(String text) {
        // 从左往右依次显示
        if (TextUtils.isEmpty(et_pwd1.getTextContent())) {
            et_pwd1.setTextContent(text);
        } else if (TextUtils.isEmpty(et_pwd2.getTextContent())) {
            et_pwd2.setTextContent(text);
        } else if (TextUtils.isEmpty(et_pwd3.getTextContent())) {
            et_pwd3.setTextContent(text);
        } else if (TextUtils.isEmpty(et_pwd4.getTextContent())) {
            et_pwd4.setTextContent(text);
        }
    }

    /**
     * 清除输入的内容--重输
     */
    private void clearText() {
        et_pwd1.setTextContent("");
        et_pwd2.setTextContent("");
        et_pwd3.setTextContent("");
        et_pwd4.setTextContent("");
        mTvDelete.setVisibility(View.GONE);
    }

    /**
     * 震动&抖动
     */
    private void shakes(){
        Vibrator vibrator = (Vibrator)LockScreenActivity.this.getSystemService(LockScreenActivity.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(300);
        }
        Animation shake = AnimationUtils.loadAnimation(this,R.anim.shake);
        findViewById(R.id.ll_pass).startAnimation(shake);

    }

    /**
     * 删除刚刚输入的内容
     */
    private void deleteText() {
        // 从右往左依次删除
        if (!TextUtils.isEmpty(et_pwd4.getTextContent())) {
            et_pwd4.setTextContent("");
        } else if (!TextUtils.isEmpty(et_pwd3.getTextContent())) {
            et_pwd3.setTextContent("");
        } else if (!TextUtils.isEmpty(et_pwd2.getTextContent())) {
            et_pwd2.setTextContent("");
        } else if (!TextUtils.isEmpty(et_pwd1.getTextContent())) {
            et_pwd1.setTextContent("");
            mTvDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        //判断点击的按钮
        switch (view.getId()) {
            case R.id.tv_forget_pwd://忘记密码?
                finish();
                break;
            case R.id.tv_delete://删除
                deleteText();//删除刚刚输入的内容
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearText();
    }
}
