package com.smart.lock.widget;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.smart.lock.R;

public class CustomDialog extends Dialog {
    private Context mContext;
    private int mHeight, mWidth;
    private boolean mCancelTouchout;
    private View mView;

    private CustomDialog(Builder builder) {
        super(builder.context);
        mContext = builder.context;
        mHeight = builder.height;
        mWidth = builder.width;
        mCancelTouchout = builder.cancelTouchout;
        mView = builder.view;
    }


    private CustomDialog(Builder builder, int resStyle) {
        super(builder.context, resStyle);
        mContext = builder.context;
        mHeight = builder.height;
        mWidth = builder.width;
        mCancelTouchout = builder.cancelTouchout;
        mView = builder.view;
        builder.addViewOnclick(R.id.cancel_btn, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }


    public View getCustomView() {
        return mView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(mView);

        setCanceledOnTouchOutside(mCancelTouchout);

        Window win = getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();
        lp.gravity = Gravity.CENTER;
        lp.height = mHeight;
        lp.width = mWidth;
        win.setAttributes(lp);
    }

    public static final class Builder {

        private Context context;
        private int height, width;
        private boolean cancelTouchout;
        private View view;
        private int resStyle = -1;

        public CustomDialog create() {
            CustomDialog dialog = new CustomDialog(this, resStyle);
            return dialog;
        }

        public Builder(Context context) {
            this.context = context;
        }

        public Builder view(int resView) {
            view = View.inflate(context, resView, null);
            return this;
        }

        public Builder heightpx(int val) {
            height = val;
            return this;
        }

        public Builder widthpx(int val) {
            width = val;
            return this;
        }


        public Builder heightDimenRes(int dimenRes) {
            height = context.getResources().getDimensionPixelOffset(dimenRes);
            return this;
        }

        public Builder widthDimenRes(int dimenRes) {
            width = context.getResources().getDimensionPixelOffset(dimenRes);
            return this;
        }

        public Builder style(int resStyle) {
            this.resStyle = resStyle;
            return this;
        }

        public Builder cancelTouchout(boolean val) {
            cancelTouchout = val;
            return this;
        }

        public Builder addViewOnclick(int viewRes, View.OnClickListener listener) {
            view.findViewById(viewRes).setOnClickListener(listener);
            return this;
        }


        public CustomDialog build() {
            if (resStyle != -1) {
                return new CustomDialog(this, resStyle);
            } else {
                return new CustomDialog(this);
            }
        }
    }

}
