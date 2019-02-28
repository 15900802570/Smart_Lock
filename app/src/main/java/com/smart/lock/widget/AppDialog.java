package com.smart.lock.widget;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

import com.smart.lock.R;

public class AppDialog extends Dialog {
    public AppDialog(Context context, int layout, int width, int height) {
        super(context, R.style.AppDialog);
        setContentView(layout);
        Window window = getWindow();
        LayoutParams params = window.getAttributes();
        params.width = width;
        params.height = height;
        window.setAttributes(params);
    }

    public AppDialog(Context context) {
        super(context, R.style.AppDialog);
    }
}
