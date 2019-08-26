package com.smart.lock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.permission.PermissionHelper;
import com.smart.lock.permission.PermissionInterface;
import com.smart.lock.ui.login.LockScreenActivity;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.LanguageType;
import com.smart.lock.utils.LanguageUtil;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.SharedPreferenceUtil;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class WelcomeActivity extends AppCompatActivity implements PermissionInterface {

    private static final String TAG = WelcomeActivity.class.getSimpleName();
    /**
     * 设置倒计时文本
     */
    private TextView mCountdownTextView;

    private static final int MSG_COUNT_WHAT = 99;
    private static final int NUM = 3;
    private int countdownNum;//倒计时的秒数
    private static Timer timer;//计时器
    private MyHandler countdownHandle;//用于控制倒计时子线程
    private Runnable runnable;//倒计时子线程
    private PermissionHelper mPermissionHelper;
    private static final int REQ_CODE_CAMERA = 1;
    private static final int ACCESS_COARSE_LOCATION = 2;
    private static final int ACCESS_FINE_LOCATION = 3;
    private static final int READ_EXTERNAL_STORAGE = 4;
    private static final int WRITE_EXTERNAL_STORAGE = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        /*set it to be no title*/
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        /*set it to be full screen*/
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //初始化控件
        initView();

        //初始化Handler和Runnable
        initThread();
        mPermissionHelper = new PermissionHelper(this, this);

        SharedPreferenceUtil.getInstance(this).writeBoolean(ConstantUtil.CHECK_DEVICE_SN, true);
        SharedPreferenceUtil.getInstance(this).writeBoolean(ConstantUtil.IS_DMT_TEST, false);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        //获取我们存储的语言环境 比如 "en","zh",等等
        String language = SharedPreferenceUtil.getInstance(newBase).readString(ConstantUtil.DEFAULT_LANGNAGE, LanguageType.CHINESE.getLanguage());
        /**
         * attach对应语言环境下的context
         */
        super.attachBaseContext(LanguageUtil.attachBaseContext(newBase, language));
    }

    /**
     * 初始化控件
     */
    private void initView() {
        mCountdownTextView = findViewById(R.id.id_countdownTextView);
        mCountdownTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopThread();
                mPermissionHelper.requestPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, ACCESS_COARSE_LOCATION);
            }
        });
    }

    /**
     * 初始化Handler和Runnable
     */
    private void initThread() {
        //倒计时变量
        initCountdownNum();
        //handler对象
        countdownHandle = new MyHandler(this);
        //runnable
        runnable = new Runnable() {

            @Override
            public void run() {
                //执行倒计时代码
                timer = new Timer();
                TimerTask task = new TimerTask() {
                    public void run() {
                        countdownNum--;
                        if (countdownNum >= 0) {
                            Message msg = countdownHandle.obtainMessage();
                            msg.what = MSG_COUNT_WHAT;//message的what值
                            msg.arg1 = countdownNum;//倒计时的秒数
                            countdownHandle.sendMessage(msg);
                        }
                    }
                };
                timer.schedule(task, 0, 1000);
            }
        };
    }


    /**
     * 重写Activity的权限请求返回结果方法
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        LogUtil.d(TAG, "requestCode : " + requestCode);
        mPermissionHelper.requestPermissionsResult(requestCode, permissions, grantResults); // 接管结果判断
    }


    @Override
    public void requestPermissionsSuccess(int callBackCode) {
        Log.d(TAG, "success callBackCode = " + callBackCode);

        if (callBackCode == ACCESS_FINE_LOCATION) {
            openNextActivity(WelcomeActivity.this);//打开下一个界面
        } else if (callBackCode == ACCESS_COARSE_LOCATION) {
            mPermissionHelper.requestPermissions(Manifest.permission.ACCESS_FINE_LOCATION, ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void requestPermissionsFail(int callBackCode) {
        Log.d(TAG, "failed callBackCode = " + callBackCode);
        showMessage(getString(R.string.rejected_permission));
    }

    /**
     * 吐司提示
     *
     * @param msg 提示信息
     */
    protected void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public String[] getPermissions() {
        return new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
    }

    /**
     * 必须使用静态类：解决问题：This Handler class should be static or leaks might occur Android
     * http://www.cnblogs.com/jevan/p/3168828.html
     */
    private class MyHandler extends Handler {
        // WeakReference to the outer class's instance.
        private WeakReference<WelcomeActivity> mOuter;

        private MyHandler(WelcomeActivity activity) {
            mOuter = new WeakReference<>(activity);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {

            WelcomeActivity theActivity = mOuter.get();

            if (theActivity != null) {

                switch (msg.what) {
                    case MSG_COUNT_WHAT:
                        if (msg.arg1 == 0) {//表示倒计时完成

                            //在这里执行的话，不会出现-1S的情况
                            if (timer != null) {
                                timer.cancel();//销毁计时器
                            }
//                            openNextActivity(theActivity);//打开下一个界面
                            mPermissionHelper.requestPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, ACCESS_COARSE_LOCATION);

                        } else {
                            theActivity.mCountdownTextView.setText(getString(R.string.skip) + msg.arg1 + getString(R.string.s));
                        }
                        break;

                    default:
                        break;
                }
            }
        }
    }

    /*
     * Activity有三个状态：
     * 运行——当它在屏幕前台时（位于当前任务堆栈的顶部）。它是激活或运行状态。它就是相应用户操作的Activity
     * 暂停——当它失去焦点但仍然对用户可见时，它处于暂停状态
     * 停止——完全被另一个Activity覆盖时则处于停止状态。它仍然保留所有的状态和成员信息。然而对用户是不可见的，所以它的窗口将被隐藏，如果其他地方需要内存，则系统经常会杀死这个Activity。
     *
     * 运行：OnCreate——>OnStart——>OnResume
     * 暂停：OnResume——>OnPause  再次重新运行：——>OnResume
     * 停止：
     * （1）切换到其他界面或者按home键回到桌面：OnPause——>OnStop   重新执行：——>OnRestart——>OnStart——>OnResume
     * （2）退出整个应用或者finish()：OnPause——>OnStop——>OnDestroy   重新执行：——>OnCreate——>OnStart——>OnResume
     *
     * */

    //1、正常状态下，运行——倒计时——跳转到登录界面，finish欢迎界面
    //2、用户在打开应用时，按home键返回到了桌面，过了一段时间再次打开了应用
    //3、在欢迎界面，手机出现了一个其他应用的提示对话框，此时实现的是继续倒计时，所以暂未处理

    @Override
    protected void onResume() {
        //开启线程
        countdownHandle.post(runnable);
        super.onResume();
    }

    @Override
    protected void onStop() {

        initCountdownNum();//初始化倒计时的秒数，这样按home键后再次进去欢迎界面，则会重新倒计时

        stopThread();

        super.onStop();
    }

    //停止倒计时
    private void stopThread() {
        //在这里执行的话，用户点击home键后，不会继续倒计时进入登录界面
        if (timer != null) {
            timer.cancel();//销毁计时器
        }

        //将线程销毁掉
        countdownHandle.removeCallbacks(runnable);
    }

    int count = 0;

    //打开下一个界面
    private void openNextActivity(Activity mActivity) {
        //跳转到登录界面并销毁当前界面
        try {
            if (SharedPreferenceUtil.getInstance(this).readBoolean(ConstantUtil.NUM_PWD_CHECK)) {
                jLockScreenActivity(mActivity);
            } else {
                if (count == 0) {
                    jManiActivity(mActivity);
                }

            }
        } catch (NullPointerException e) {
            jManiActivity(mActivity);
        }
        mActivity.finish();
    }

    /**
     * 跳转到主界面
     *
     * @param mActivity 上下文
     */
    private void jManiActivity(Activity mActivity) {
        count++;
        Intent intent = new Intent(mActivity, MainActivity.class);
        mActivity.startActivity(intent);
    }

    /**
     * 跳转到验证界面
     *
     * @param mActivity 上下文
     */
    private void jLockScreenActivity(Activity mActivity) {
        int param;
        try {
            if (!SharedPreferenceUtil.getInstance(this).readString(ConstantUtil.NUM_PWD,"").isEmpty()) {
                param = ConstantUtil.LOGIN_PASSWORD;
            } else {
                param = ConstantUtil.SETTING_PASSWORD;
            }
        } catch (NullPointerException e) {
            param = ConstantUtil.SETTING_PASSWORD;
        }
        Intent intent = new Intent(mActivity, LockScreenActivity.class);
        mActivity.startActivity(intent.putExtra("type", param));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG, "onDestroy");
    }

    /*初始化倒计时的秒数*/
    private void initCountdownNum() {
        countdownNum = NUM;
    }
}
