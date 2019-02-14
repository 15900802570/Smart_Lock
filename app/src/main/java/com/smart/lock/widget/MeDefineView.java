package com.smart.lock.widget;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smart.lock.R;

public class MeDefineView extends RelativeLayout {
    private ImageView im_me_define;
    private TextView tv_me_define;
    private View view;

    public MeDefineView(Context context) {
        super(context);
        init();
    }

    public MeDefineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MeDefineView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        this.view = View.inflate(getContext(), R.layout.me_own_define, this);
        this.im_me_define = (ImageView) this.view.findViewById(R.id.im_me_define);
        this.tv_me_define = (TextView) this.view.findViewById(R.id.tv_me_define);
    }

    public void setImage(int i) {
        this.im_me_define.setImageResource(i);
    }

    public void setDes(String des) {
        this.tv_me_define.setText(des);
    }
}