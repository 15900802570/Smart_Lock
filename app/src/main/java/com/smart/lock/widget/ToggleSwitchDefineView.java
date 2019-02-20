
package com.smart.lock.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.smart.lock.R;

public class ToggleSwitchDefineView extends RelativeLayout {
    public static OnCustomClickListener onCustomClickListener;
    private ToggleButton iv_switch_light;
    private TextView tv_switch_des;
    private View view;

    public interface OnCustomClickListener {
        void click(View view, View view2);
    }

    public ToggleSwitchDefineView(Context context) {
        super(context);
        init();
    }

    public ToggleSwitchDefineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ToggleSwitchDefineView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        this.view = View.inflate(getContext(), R.layout.toggle_switch_define, this);
        this.tv_switch_des = (TextView) view.findViewById(R.id.tv_switch_des);
        this.iv_switch_light = (ToggleButton) view.findViewById(R.id.iv_switch_light);
    }

    public static void setOnCustomClickListener(OnCustomClickListener onCustomClickListener) {
        ToggleSwitchDefineView.onCustomClickListener = onCustomClickListener;
    }

    public void setDes(String des) {
        tv_switch_des.setText(des);
    }

    public void setChecked(boolean status) {
        iv_switch_light.setChecked(status);
    }

    public ToggleButton getIv_switch_light() {
        return iv_switch_light;
    }

    public boolean isChecked() {
        return iv_switch_light.isChecked();
    }

    public void setOnClick() {
        iv_switch_light.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

            }
        });
    }
}
