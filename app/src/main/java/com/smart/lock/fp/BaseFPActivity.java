package com.smart.lock.fp;

import android.os.Bundle;
import android.app.Activity;
import android.support.annotation.Nullable;

public abstract class BaseFPActivity extends Activity {

    public static String TGA = "BaseFPActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public abstract void onFingerprintAuthentication();

    public abstract void onFingerprintCancel();

    public abstract void onFingerprintAuthenticationError();
}
