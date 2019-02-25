package com.smart.lock.ui.login;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import com.smart.lock.MainActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.R;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.SharedPreferenceUtil;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class LockScreenActivity extends Activity implements View.OnClickListener {

    private static String TGA ="LockScreenActivity";

    private TextView mDeleteTv;
    private TextView mForgetPwdTv;
    private TextView mInfoTv; //提示信息


    private NumericKeyboard nk; // 数字键盘布局
    private PasswordTextView num_pwd1, num_pwd2, num_pwd3, num_pwd4; // 密码框

    private int type;
    private boolean isReturn;
    private String input; //输入字段
    private StringBuffer fBuffer = new StringBuffer();

    private boolean isFP;
    private boolean isFPRequired;
    private FingerprintManager mFPM;
    private CancellationSignal mCancellationSignal;
    private Cipher mCipher;
    private KeyStore mKeyStore;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lock);
        //获取界面传递的值
        type = getIntent().getIntExtra("type", 1);
        isReturn = getIntent().getBooleanExtra(ConstantUtil.IS_RETURN, false);
        initView();
        initListener();// 事件处理

    }

    /**
     *  检测设备是否支持指纹
     * @return 是否支持指纹
     */
    private boolean supportFP(){
        if(Build.VERSION.SDK_INT<23){
            LogUtil.i(TGA,"系统版本低,不支持指纹功能");
            return false;
        }else {
            KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
            mFPM = getSystemService(FingerprintManager.class);
            if(!mFPM.isHardwareDetected()){
                LogUtil.i(TGA,"手机不支持指纹");
                return false;
            }else if(!keyguardManager.isKeyguardSecure()){
                LogUtil.i(TGA,"未设置锁屏，需设置锁屏并添加指纹");
                return false;
            }else if(!mFPM.hasEnrolledFingerprints()){
                LogUtil.i(TGA,"系统中至少需要添加一个指纹");
                return false;
            }
        }
        LogUtil.i(TGA,"支持指纹");
        return true;
    }

    private void initView() {
        nk = findViewById(R.id.num_kb);// 数字键盘
        // 密码框
        num_pwd1 = findViewById(R.id.num_pwd1);
        num_pwd2 = findViewById(R.id.num_pwd2);
        num_pwd3 = findViewById(R.id.num_pwd3);
        num_pwd4 = findViewById(R.id.num_pwd4);
        mInfoTv = findViewById(R.id.info_tv);//提示信息
        mDeleteTv = findViewById(R.id.tv_delete);
        mForgetPwdTv = findViewById(R.id.tv_forget_pwd);

        /**
         * 初始化指纹
         */
        isFP=supportFP();
        try{
           isFPRequired = SharedPreferenceUtil.getInstance(this).readBoolean(ConstantUtil.FINGERPRINT_CHECK);
        }catch (NullPointerException e){
            isFPRequired = false;
            LogUtil.e(TGA,"获取是否开启指纹验证信息失败");
        }
        if(isFP && isFPRequired && !isReturn){
            mInfoTv.setText("指纹 / 输入密码");
            initFP();
        }
    }

    @TargetApi(23)
    private void initFP(){
        //初始化key
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
            mKeyStore.load(null);
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            KeyGenParameterSpec.Builder builder= new KeyGenParameterSpec.Builder(DEVICE_POLICY_SERVICE,
                    KeyProperties.PURPOSE_ENCRYPT|
                    KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
            keyGenerator.init(builder.build());
            keyGenerator.generateKey();
        }catch (Exception e){
            LogUtil.e(TGA,"初始化key出错"+e);
        }
        // 初始化Cipher
        try{
            SecretKey key = (SecretKey)mKeyStore.getKey(DEVICE_POLICY_SERVICE,null);
            mCipher=Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES+'/'
                    +KeyProperties.BLOCK_MODE_CBC+'/'
                    +KeyProperties.ENCRYPTION_PADDING_PKCS7);
            mCipher.init(Cipher.ENCRYPT_MODE,key);
        }catch ( Exception e){
            LogUtil.e(TGA,"初始化Cipher出错"+e);
        }
        //启动监听事件
    }

    @TargetApi(23)
    private void startFPListening(){
        mCancellationSignal =  new CancellationSignal();
        mFPM.authenticate(new FingerprintManager.CryptoObject(mCipher),
                mCancellationSignal,
                0,
                new FingerprintManager.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        LogUtil.i(TGA, "指纹验证错误");
                        if(errorCode == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT){
                            mInfoTv.setText("指纹验证失败达到上限，请输入密码解锁");
                        }
                    }

                    @Override
                    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                        super.onAuthenticationHelp(helpCode, helpString);
                    }

                    @Override
                    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        LogUtil.i(TGA,"指纹验证成功");
                        setAllText();
                        onAuthenticated();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        LogUtil.i(TGA, "指纹验证失败");
                        setAllText();
                        shakes();
                        startTimer();
                    }
                },
                null);
    }

    @TargetApi(23)
    private void stopFPListening(){
        if(mCancellationSignal != null){
            mCancellationSignal.cancel();
            mCancellationSignal=null;
        }
    }

    public void onAuthenticated(){
        if( isReturn) {
            setResult(RESULT_OK , new Intent().putExtra(ConstantUtil.CONFIRM, -1));
        }else {
            startActivity(new Intent(LockScreenActivity.this, MainActivity.class));
        }
        finish();
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
                input = num_pwd1.getTextContent() + num_pwd2.getTextContent() +
                        num_pwd3.getTextContent() + num_pwd4.getTextContent();
                if (type == ConstantUtil.SETTING_PASSWORD) {//设置密码
                    //重新输入密码
                    mInfoTv.setText(getString(R.string.please_input_pwd_again));
                    type = ConstantUtil.SURE_SETTING_PASSWORD;
                    fBuffer.append(input);//保存第一次输入的密码
                    startTimer();
                } else if (type == ConstantUtil.LOGIN_PASSWORD) {//登录
                    if (!input.equals(SharedPreferenceUtil.getInstance(LockScreenActivity.this).readString(ConstantUtil.NUM_PWD))) {
                        shakes();
                    } else {
                        onAuthenticated();
                    }
                } else if (type == ConstantUtil.SURE_SETTING_PASSWORD) {//确认密码
                    //判断两次输入的密码是否一致
                    if (input.equals(fBuffer.toString())) {//一致
                        //保存密码到文件中
                        SharedPreferenceUtil.getInstance(LockScreenActivity.this).initSharedPreferences(LockScreenActivity.this);
                        SharedPreferenceUtil.getInstance(LockScreenActivity.this).writeString(ConstantUtil.NUM_PWD, input);
                        mInfoTv.setText(getString(R.string.please_input_pwd));
                        if (isReturn){
                            setResult(RESULT_OK, new Intent().putExtra(ConstantUtil.CONFIRM, 1));
                            finish();
                        }
                    } else {//不一致
                        shakes();
                    }
                    startTimer();
                }
            }
        });
        mDeleteTv.setOnClickListener(this);
        mForgetPwdTv.setOnClickListener(this);
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
    private void setAllText(){
        setText("1");
        setText("2");
        setText("3");
        setText("4");
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
    private void shakes(){
        Vibrator vibrator = (Vibrator)LockScreenActivity.this.getSystemService(LockScreenActivity.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(300);
        }
        Animation shake = AnimationUtils.loadAnimation(this,R.anim.shake);
        findViewById(R.id.num_pwd_tv).startAnimation(shake);
        clearText();
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
            case R.id.tv_forget_pwd://忘记密码?
                finish();
                break;
            case R.id.tv_delete://删除
                deleteText();//删除刚刚输入的内容
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isFP){
        startFPListening();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(isFP){
        stopFPListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isFP){
            stopFPListening();
        }
        clearText();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(isReturn) {
            setResult(RESULT_CANCELED, new Intent().putExtra(ConstantUtil.CONFIRM, 0));
        }
    }
}
