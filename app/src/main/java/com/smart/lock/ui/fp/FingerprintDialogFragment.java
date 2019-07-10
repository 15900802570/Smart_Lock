package com.smart.lock.ui.fp;

import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.utils.LogUtil;

import java.util.Objects;

import javax.crypto.Cipher;

@TargetApi(23)
public class FingerprintDialogFragment extends DialogFragment {

    private FingerprintManager mFPM;
    private CancellationSignal mCancellationSignal;
    private Cipher mCipher;
    private TextView mErrorTv;

    private BaseFPActivity mActivity;

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
    public void onStart() {
        super.onStart();
        Window win = getDialog().getWindow();
        // 一定要设置Background，如果不设置，window属性设置无效
        assert win != null;
        win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        DisplayMetrics dm = new DisplayMetrics();
        Objects.requireNonNull(getActivity()).getWindowManager().getDefaultDisplay().getMetrics(dm);

//        WindowManager.LayoutParams params = win.getAttributes();
//        // 使用ViewGroup.LayoutParams，以便Dialog 宽度充满整个屏幕
//        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
//        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
//        win.setAttributes(params);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFPM = getContext().getSystemService(FingerprintManager.class);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_AppCompat_Light_Dialog_Alert);

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
        mCancellationSignal = new CancellationSignal();
        mFPM.authenticate(new FingerprintManager.CryptoObject(mCipher),
                mCancellationSignal,
                0,
                new FingerprintManager.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        LogUtil.i(TGA, mActivity.getString(R.string.fp_verification_failed) + errorCode);
                        if (errorCode == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT) {
                            Toast.makeText(mActivity, mActivity.getString(R.string.upper_limit_of_fp_verification_times), Toast.LENGTH_SHORT).show();
                            dismiss();
                            mActivity.onFingerprintAuthenticationError(errorCode);
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
                        LogUtil.i(TGA, mActivity.getString(R.string.fp_verification_succeeded));
                        mActivity.onFingerprintAuthenticationSucceeded();
                        dismiss();
                        stopFPListening();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        LogUtil.i(TGA, mActivity.getString(R.string.fp_verification_failed));
                        mErrorTv.setText(mActivity.getString(R.string.fp_verification_failed));
                        shakes();
                    }
                },
                null);
    }

    private void shakes() {
        Vibrator vibrator = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(300);
        }
        Animation shake = AnimationUtils.loadAnimation(mActivity, R.anim.shake);
        mErrorTv.startAnimation(shake);
    }

    private void stopFPListening() {
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }

}
