package com.smart.lock.ui.fp;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.Nullable;

import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.LogUtil;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public abstract class BaseFPActivity extends Activity {

    public static String TGA = "BaseFPActivity";
    protected int mIsFP = 0; //是否支持指纹，0 1 不支持 2 3 未设置 4 支持
    protected FingerprintManager mFPM;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public abstract void onFingerprintAuthentication();

    public abstract void onFingerprintCancel();

    public abstract void onFingerprintAuthenticationError();

    /**
     * 检测设备是否支持指纹
     *
     * @return 0 系统不支持 1 手机不支持 2 未设置锁屏 3 未设置指纹 4 支持指纹
     */
    protected int supportFP() {
        if (Build.VERSION.SDK_INT < 23) {
            LogUtil.i(TGA, "系统版本低,不支持指纹功能");
            return ConstantUtil.FP_LOW_VERSION;
        } else {
            KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
            mFPM = getSystemService(FingerprintManager.class);
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
        }
        LogUtil.i(TGA, "支持指纹");
        return ConstantUtil.FP_SUPPORT;
    }

    /**
     * 开启指纹验证
     */
    @TargetApi(23)
    protected void doFingerprintDialog() {
        KeyStore mKeyStore = null;
        Cipher lCipher;
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
        if (mKeyStore!=null) {
            try {
                SecretKey key = (SecretKey) mKeyStore.getKey(DEVICE_POLICY_SERVICE, null);
                lCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + '/'
                        + KeyProperties.BLOCK_MODE_CBC + '/'
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7);
                lCipher.init(Cipher.ENCRYPT_MODE, key);
                showFingerprintDialog(lCipher);
            } catch (Exception e) {
                LogUtil.e(TGA, "初始化Cipher出错" + e);
            }

        }
    }

    @TargetApi(23)
    private void showFingerprintDialog(Cipher cipher) {
        FingerprintDialogFragment fingerprintDialogFragment = new FingerprintDialogFragment();
        fingerprintDialogFragment.setCipher(cipher);
        fingerprintDialogFragment.show(getFragmentManager(), "FINGERPRINT");
    }

}
