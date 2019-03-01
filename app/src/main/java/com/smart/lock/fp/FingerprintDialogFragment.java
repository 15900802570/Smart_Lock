package com.smart.lock.fp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ui.login.LockScreenActivity;
import com.smart.lock.utils.LogUtil;
import com.yzq.zxinglibrary.common.Constant;

import javax.crypto.Cipher;

@TargetApi(23)
public class FingerprintDialogFragment extends DialogFragment {

    private FingerprintManager mFPM;
    private CancellationSignal mCancellationSignal;
    private Cipher mCipher;
    private TextView mErrorTv;

    private BaseFPActivity mActivity;
    private boolean isSelfCancel;

    private static String TGA = "FingerprintDialogFragment";

    public void setCipher(Cipher cipher) {
        mCipher = cipher;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (BaseFPActivity) getActivity();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFPM = getContext().getSystemService(FingerprintManager.class);
        setStyle(DialogFragment.STYLE_NO_TITLE,R.style.Theme_AppCompat_Light_Dialog_Alert);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_fingerprint, container, false);
        mErrorTv = v.findViewById(R.id.tv_fingerprint_error);
        TextView cancelTv = v.findViewById(R.id.tv_fp_cancel);
        cancelTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                stopFPListening();
                mActivity.onFingerprintCancel();
            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        startFPListening();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopFPListening();
    }

    private void startFPListening() {
        isSelfCancel = false;
        mCancellationSignal = new CancellationSignal();
        mFPM.authenticate(new FingerprintManager.CryptoObject(mCipher),
                mCancellationSignal,
                0,
                new FingerprintManager.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        LogUtil.i(TGA, "指纹验证错误"+errorCode);
                        if(errorCode==FingerprintManager.FINGERPRINT_ERROR_LOCKOUT){
                            Toast.makeText(mActivity,"指纹验证次数达到上限",Toast.LENGTH_SHORT).show();
                            dismiss();
                            mActivity.onFingerprintAuthenticationError();
                        }
                        mActivity.onFingerprintCancel();

                    }

                    @Override
                    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                        super.onAuthenticationHelp(helpCode, helpString);
                    }

                    @Override
                    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        LogUtil.i(TGA, "指纹验证成功");
                        mActivity.onFingerprintAuthentication();
                        dismiss();
                        stopFPListening();
                    }
                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        LogUtil.i(TGA, "指纹验证失败");
                        mErrorTv.setText("指纹认证失败，请再试一次");
                        shakes();
                    }
                },
                null);
    }

    private void shakes(){
        Vibrator vibrator = (Vibrator) mActivity.getSystemService(mActivity.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(300);
        }
        Animation shake = AnimationUtils.loadAnimation(mActivity,R.anim.shake);
        mErrorTv.startAnimation(shake);
    }

    private void stopFPListening() {
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }

}
