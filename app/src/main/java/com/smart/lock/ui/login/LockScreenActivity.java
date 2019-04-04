package com.smart.lock.ui.login;

import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.MainActivity;
import com.smart.lock.ui.fp.BaseFPActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.R;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.SharedPreferenceUtil;
import com.smart.lock.utils.ToastUtil;


public class LockScreenActivity extends BaseFPActivity implements View.OnClickListener {

    private static String TAG = "LockScreenActivity";

    private TextView mDeleteTv;
    private TextView mInfoTv; //提示信息


    private NumericKeyboard mNumKeyNk; // 数字键盘布局
    private PasswordTextView num_pwd1, num_pwd2, num_pwd3, num_pwd4; // 密码框

    private int type;
    private boolean isReturn;
    private boolean notCancel;
    private StringBuffer fBuffer = new StringBuffer();

    private boolean isFPRequired = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lock);
        //获取界面传递的值
        type = getIntent().getIntExtra("type", 1);
        isReturn = getIntent().getBooleanExtra(ConstantUtil.IS_RETURN, false);
        notCancel = getIntent().getBooleanExtra(ConstantUtil.NOT_CANCEL, false);
        LogUtil.d(TAG, "intent type = " + type + '\n' +
                "isReturn = " + isReturn);
        initView();
        initListener();// 事件处理

    }

    @Override
    public void onFingerprintAuthenticationSucceeded() {
        setAllText(true);
    }

    @Override
    public void onFingerprintCancel() {
        onStartFPListening();
    }

    @Override
    public void onFingerprintAuthenticationError(int errorCode) {
        LogUtil.i(TGA, "指纹验证错误" + errorCode);
        switch (errorCode) {
            case FingerprintManager.FINGERPRINT_ERROR_LOCKOUT:
                ToastUtil.show(this,
                        "指纹验证次数达到上限",
                        Toast.LENGTH_SHORT);
                mInfoTv.setText("输入密码");
                setAllText(false);
                break;
                case FingerprintManager.FINGERPRINT_ERROR_CANCELED:
                    break;
                    default:
                        break;
        }
    }

    public void onFingerprintAuthenticationFailed() {
        setAllText(false);
    }

    private void initView() {
        mNumKeyNk = findViewById(R.id.num_kb);// 数字键盘
        // 密码框
        num_pwd1 = findViewById(R.id.num_pwd1);
        num_pwd2 = findViewById(R.id.num_pwd2);
        num_pwd3 = findViewById(R.id.num_pwd3);
        num_pwd4 = findViewById(R.id.num_pwd4);
        mInfoTv = findViewById(R.id.info_tv);//提示信息
        mDeleteTv = findViewById(R.id.tv_delete);

        switch (type){
            case ConstantUtil.SETTING_PASSWORD:
                mInfoTv.setText(getString(R.string.please_input_pwd_first));
                break;
            case ConstantUtil.MODIFY_PASSWORD:
                mInfoTv.setText(getString(R.string.please_input_old_pwd));
                break;
            case ConstantUtil.LOGIN_PASSWORD:
                //初始化指纹
                try {
                    isFPRequired = SharedPreferenceUtil.getInstance(this).readBoolean(ConstantUtil.FINGERPRINT_CHECK);
                    LogUtil.e(TAG, "isFPRequired = " + isFPRequired);

                } catch (NullPointerException e) {
                    isFPRequired = false;
                    LogUtil.e(TAG, "获取是否开启指纹验证信息失败");
                }
                if (mIsFP == 4 && isFPRequired && !isReturn) {
                    mInfoTv.setText("指纹 / 输入密码");
                    initFP();
                }else {
                    mInfoTv.setText(getString(R.string.please_input_pwd));
                }
                break;
        }
        if(notCancel){
            DialogUtils.createTipsDialogWithConfirm(LockScreenActivity.this,getString(R.string.setting_password_for_security)).show();
        }
    }

    /**
     * 验证成功函数
     */
    public void onAuthenticatedSucceeded() {
        if (mIsFP == 4 && isFPRequired && !isReturn) {
            onStopFPListening();
        }
        if (isReturn) {
            setResult(RESULT_OK, new Intent().putExtra(ConstantUtil.CONFIRM, -1));
        } else {
            startActivity(new Intent(LockScreenActivity.this, MainActivity.class));
        }
        finish();
    }

    /**
     * 事件处理
     */
    private void initListener() {
        // 设置点击的按钮回调事件
        mNumKeyNk.setOnNumberClick(new NumericKeyboard.OnNumberClick() {
            @Override
            public void onNumberReturn(int number) {
                // 设置显示密码
                setText(String.valueOf(number));
            }
        });
        num_pwd1.setOnMyTextChangedListener(new PasswordTextView.OnMyTextChangedListener() {
            @Override
            public void textChanged(String content) {
                if (TextUtils.isEmpty(content)) {
                    mDeleteTv.setVisibility(View.GONE);
                } else {
                    mDeleteTv.setVisibility(View.VISIBLE);
                }
            }
        });
        //监听最后一个密码框的文本改变事件回调
        num_pwd4.setOnMyTextChangedListener(new PasswordTextView.OnMyTextChangedListener() {
            @Override
            public void textChanged(String content) {
                onAuthenticated();
            }
        });
        mDeleteTv.setOnClickListener(this);
    }

    /**
     * 验证函数
     */
    private void onAuthenticated(){
        //输入字段
        String input = num_pwd1.getTextContent() + num_pwd2.getTextContent() +
                num_pwd3.getTextContent() + num_pwd4.getTextContent();
        switch (type) {
            case ConstantUtil.SETTING_PASSWORD: //设置密码
                LogUtil.d("设置密码");
                //重新输入密码
                mInfoTv.setText(getString(R.string.please_input_pwd_again));
                type = ConstantUtil.SURE_SETTING_PASSWORD;
                fBuffer.append(input);//保存第一次输入的密码
                startTimer();
                break;
            case ConstantUtil.LOGIN_PASSWORD: //登录
                LogUtil.d("登录123456566");
                if (!input.equals(SharedPreferenceUtil.getInstance(LockScreenActivity.this).readString(ConstantUtil.NUM_PWD))) {
                    shakes();
                    startTimer();
                } else {
                    onAuthenticatedSucceeded();
                }
                break;
            case ConstantUtil.SURE_SETTING_PASSWORD: //确认密码
                LogUtil.d("确认密码");
                //判断两次输入的密码是否一致
                if (input.equals(fBuffer.toString())) {//一致
                    //保存密码到文件中
                    SharedPreferenceUtil.getInstance(LockScreenActivity.this).initSharedPreferences(LockScreenActivity.this);
                    SharedPreferenceUtil.getInstance(LockScreenActivity.this).writeString(ConstantUtil.NUM_PWD, input);
                    if (isReturn) {
                        setResult(RESULT_OK, new Intent().putExtra(ConstantUtil.CONFIRM, 1));
                        finish();
                    }
                } else {//不一致
                    shakes();
                }
                startTimer();
                break;
            case ConstantUtil.MODIFY_PASSWORD:
                LogUtil.d("修改密码");
                if(!input.equals(SharedPreferenceUtil.getInstance(LockScreenActivity.this).readString(ConstantUtil.NUM_PWD))){
                    shakes();
                }else {
                    type = ConstantUtil.SETTING_PASSWORD;
                    mInfoTv.setText(getString(R.string.please_input_pwd_first));
                }
                startTimer();
                break;
        }
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
                    Thread.sleep(300);
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
        if (TextUtils.isEmpty(num_pwd1.getTextContent())) {
            num_pwd1.setTextContent(text);
        } else if (TextUtils.isEmpty(num_pwd2.getTextContent())) {
            num_pwd2.setTextContent(text);
        } else if (TextUtils.isEmpty(num_pwd3.getTextContent())) {
            num_pwd3.setTextContent(text);
        } else if (TextUtils.isEmpty(num_pwd4.getTextContent())) {
            num_pwd4.setTextContent(text);
        }
    }

    /**
     * 设置所有字符
     */
    private void setAllText(boolean flag) {
        String pwd = SharedPreferenceUtil.getInstance(this).readString(ConstantUtil.NUM_PWD);
        clearText();
        if (flag) {
            setText(pwd.substring(0, 1));
            setText(pwd.substring(1, 2));
            setText(pwd.substring(2, 3));
            setText(pwd.substring(3, 4));
        } else {
            setText(pwd.substring(0, 1) + 1);
            setText(pwd.substring(1, 2) + 1);
            setText(pwd.substring(2, 3) + 1);
            setText(pwd.substring(3, 4) + 1);
            startTimer();
        }
    }

    /**
     * 清除输入的内容--重输
     */
    private void clearText() {
        num_pwd1.setTextContent("");
        num_pwd2.setTextContent("");
        num_pwd3.setTextContent("");
        num_pwd4.setTextContent("");
        mDeleteTv.setVisibility(View.GONE);
    }

    /**
     * 震动&抖动
     */
    private void shakes() {
        Vibrator vibrator = (Vibrator) LockScreenActivity.this.getSystemService(LockScreenActivity.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(300);
        }
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        findViewById(R.id.num_pwd_tv).startAnimation(shake);
        LogUtil.d("shakes");
    }

    /**
     * 删除刚刚输入的内容
     */
    private void deleteText() {
        // 从右往左依次删除
        if (!TextUtils.isEmpty(num_pwd4.getTextContent())) {
            num_pwd4.setTextContent("");
        } else if (!TextUtils.isEmpty(num_pwd3.getTextContent())) {
            num_pwd3.setTextContent("");
        } else if (!TextUtils.isEmpty(num_pwd2.getTextContent())) {
            num_pwd2.setTextContent("");
        } else if (!TextUtils.isEmpty(num_pwd1.getTextContent())) {
            num_pwd1.setTextContent("");
            mDeleteTv.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        //判断点击的按钮
        switch (view.getId()) {
            case R.id.tv_delete://删除
                deleteText();//删除刚刚输入的内容
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mIsFP == 4 && isFPRequired) {
            onStartFPListening();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIsFP == 4 && isFPRequired) {
            onStopFPListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsFP == 4 && isFPRequired) {
            onStopFPListening();
        }
        clearText();
    }

    @Override
    public void onBackPressed() {
        if(notCancel){
            ToastUtil.showLong(this,getString(R.string.setting_password_for_security));
        }else {
            super.onBackPressed();
            if (isReturn) {
                setResult(RESULT_CANCELED, new Intent().putExtra(ConstantUtil.CONFIRM, 0));
            }
        }
    }
}
