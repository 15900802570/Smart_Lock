package com.smart.lock.utils;

import android.content.Context;
import android.widget.Toast;

public class ToastUtil {
    public static boolean isShow = true;

    private ToastUtil() {
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    public static void show(Context paramContext, int paramInt1, int paramInt2) {
        if (isShow) {
            Toast.makeText(paramContext, paramInt1, paramInt2).show();
        }
    }

    public static void show(Context paramContext, CharSequence paramCharSequence, int paramInt) {
        if (isShow) {
            Toast.makeText(paramContext, paramCharSequence, paramInt).show();
        }
    }

    public static void showLong(Context paramContext, int paramInt) {
        if (isShow) {
            Toast.makeText(paramContext, paramInt, Toast.LENGTH_LONG).show();
        }
    }

    public static void showLong(Context paramContext, CharSequence paramCharSequence) {
        if (isShow) {
            Toast.makeText(paramContext, paramCharSequence, Toast.LENGTH_LONG).show();
        }
    }

    public static void showShort(Context paramContext, int paramInt) {
        if (isShow) {
            Toast.makeText(paramContext, paramInt, Toast.LENGTH_SHORT).show();
        }
    }

    public static void showShort(Context paramContext, CharSequence paramCharSequence) {
        if (isShow) {
            Toast.makeText(paramContext, paramCharSequence, Toast.LENGTH_SHORT).show();
        }
    }

}
