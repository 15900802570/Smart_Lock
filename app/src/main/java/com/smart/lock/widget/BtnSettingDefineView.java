package com.smart.lock.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smart.lock.R;

public class BtnSettingDefineView extends RelativeLayout {
    public static ToggleSwitchDefineView.OnCustomClickListener onCustomClickListener;
    private View view;
    private TextView mBtnInfoDesTv;
    private TextView mBtnSettingBtn;

    public interface OnCustomClickListener {
        void click(View view, View view2);
    }
    public BtnSettingDefineView(Context context) {
        super(context);
        init();
    }

    public BtnSettingDefineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BtnSettingDefineView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }
    private void init(){
        this.view = View.inflate(getContext(), R.layout.btn_setting_define, this);
        this.mBtnInfoDesTv = view.findViewById(R.id.tv_btn_info_des);
        this.mBtnSettingBtn = view.findViewById(R.id.btn_setting_des);
    }

    public void setDes(String info){
        mBtnInfoDesTv.setText(info);
    }

    public void setBtnDes(String info){
        mBtnSettingBtn.setText(info);
    }
}
