package com.smart.lock.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smart.lock.R;

public class NextActivityDefineView extends RelativeLayout {

    private View view;
    private TextView mTextViewDes;

    public NextActivityDefineView(Context context) {
        super(context);
        init();
    }

    public NextActivityDefineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NextActivityDefineView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }
    private void init(){
        this.view = View.inflate(getContext(), R.layout.next_activity_define, this);
        this.mTextViewDes = view.findViewById(R.id.tv_next_des);
    }

    public void setDes(String des){
        mTextViewDes.setText(des);
    }
}
