package com.smart.lock.ui.fp;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.LogUtil;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public abstract class BaseFPActivity extends AppCompatActivity {

    public static String TGA = "BaseFPActivity";
    protected int mIsFP = 0; //是否支持指纹，0 1 不支持 2 3 未设置 4 支持
    protected FingerprintManager mFPM;
    private CancellationSignal mCancellationSignal;
    private Cipher mCipher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsFP = isSupportFP();
    }

    public abstract void onFingerprintAuthenticationSucceeded();

    public abstract void onFingerprintCancel();

    public abstract void onFingerprintAuthenticationError(int errorCode);

    public void onFingerprintAuthenticationFailed() {

    }

    /**
     * 检测设备是否支持指纹
     *
     * @return 0 系统不支持 1 手机不支持 2 未设置锁屏 3 未设置指纹 4 支持指纹
     */
    protected int isSupportFP() {
        if (Build.VERSION.SDK_INT < 23) {
            LogUtil.i(TGA, "系统版本低,不支持指纹功能");
            return ConstantUtil.FP_LOW_VERSION;
        } else {
            KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
            mFPM = getSystemService(FingerprintManager.class);
            try {
                if (!mFPM.isHardwareDetected()) {
                    LogUtil.i(TGA, "手机不支持指纹");
                    return ConstantUtil.FP_NO_HARDWARE;
                } else if (!keyguardManager.isKeyguardSecure()) {
                    LogUtil.i(TGA, "未设置锁屏，需设置锁屏并添加指纹");
                    return ConstantUtil.FP_NO_KEYGUARDSECURE;
                } else if (!mFPM.hasEnrolledFingerprints()) {
                    LogUtil.i(TGA, "系统中至少需要添加一个指纹");
                    return ConstantUtil.FP_NO_FINGERPRINT;
                }
            } catch (NullPointerException e) {
                LogUtil.i(TGA, "手机不支持指纹");
                return ConstantUtil.FP_NO_HARDWARE;
            }
        }
        LogUtil.i(TGA, "支持指纹");
        return ConstantUtil.FP_SUPPORT;
    }

    @TargetApi(23)
    protected void initFP() {
        KeyStore mKeyStore = null;
        //初始化key
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
            mKeyStore.load(null);
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(DEVICE_POLICY_SERVICE,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
            keyGenerator.init(builder.build());
            keyGenerator.generateKey();
        } catch (Exception e) {
            LogUtil.e(TGA, "初始化key出错" + e);
        }
        // 初始化Cipher
        if (mKeyStore != null) {
            try {
                SecretKey key = (SecretKey) mKeyStore.getKey(DEVICE_POLICY_SERVICE, null);
                mCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + '/'
                        + KeyProperties.BLOCK_MODE_CBC + '/'
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7);
                mCipher.init(Cipher.ENCRYPT_MODE, key);
            } catch (Exception e) {
                LogUtil.e(TGA, "初始化Cipher出错" + e);
            }
        }
    }

    /**
     * 启动指纹监听事件
     */
    @TargetApi(23)
    protected void onStartFPListening() {
        mCancellationSignal = new CancellationSignal();
        mFPM.authenticate(new FingerprintManager.CryptoObject(mCipher),
                mCancellationSignal,
                0,
                new FingerprintManager.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        onFingerprintAuthenticationError(errorCode);
                    }

                    @Override
                    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                        super.onAuthenticationHelp(helpCode, helpString);
                    }

                    @Override
                    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        onFingerprintAuthenticationSucceeded();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        onFingerprintAuthenticationFailed();
                    }
                },
                null);
    }

    protected void onStopFPListening() {
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }

    /**
     * 开启指纹验证Dialog
     */
    @TargetApi(23)
    protected void doFingerprintDialog() {
        initFP();
        showFingerprintDialog(mCipher);
    }

    @TargetApi(23)
    private void showFingerprintDialog(Cipher cipher) {
        FingerprintDialogFragment fingerprintDialogFragment = new FingerprintDialogFragment();
        fingerprintDialogFragment.setCipher(cipher);
        fingerprintDialogFragment.show(getFragmentManager(), "FINGERPRINT");
    }
}
