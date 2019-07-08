package com.smart.lock.widget;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.smart.lock.R;
import com.smart.lock.utils.ConstantUtil;

import java.util.Objects;

@SuppressLint("ValidFragment")
public class CreateTmpPwdDialog extends DialogFragment {

    private View mView;
    private RadioButton mSetNumOneBtn;
    private RadioButton mSetNumMoreBtn;
    private NumberPicker mSetPeriod;
    private Button mCancelBtn;
    private Button mConfirmBtn;
    private TextView mTitleTV;

    private int mRequestCode;

    @SuppressLint("ValidFragment")
    public CreateTmpPwdDialog(int requestCode) {
        mRequestCode = requestCode;
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

        WindowManager.LayoutParams params = win.getAttributes();
        // 使用ViewGroup.LayoutParams，以便Dialog 宽度充满整个屏幕
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        win.setAttributes(params);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(android.app.DialogFragment.STYLE_NO_TITLE, R.style.Theme_AppCompat_Light_Dialog_Alert);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = initView(inflater, container);
        initData();
        initEvent();
        return mView;

    }

    private View initView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        mView = inflater.inflate(R.layout.dialog_create_temp_pwd, container, false);
        mSetNumOneBtn = mView.findViewById(R.id.temp_pwd_rg_one);
        mSetNumMoreBtn = mView.findViewById(R.id.temp_pwd_rg_more);
        mSetPeriod = mView.findViewById(R.id.temp_pwd_period);
        mCancelBtn = mView.findViewById(R.id.create_temp_pwd_dialog_cancel_btn);
        mConfirmBtn = mView.findViewById(R.id.create_temp_pwd_dialog_confirm_btn);
        mTitleTV = mView.findViewById(R.id.create_temp_pwd_dialog_title_tv);
        return mView;
    }

    private void initData() {

        mSetPeriod.setDisplayedValues(ConstantUtil.TEMP_PWD_PERIOD);

//        mSetNum.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mSetPeriod.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

//        mSetNum.setMinValue(1);
        mSetPeriod.setMinValue(1);

//        mSetNum.setMaxValue(ConstantUtil.TEMP_PWD_NUMBERS.length);
        mSetPeriod.setMaxValue(ConstantUtil.TEMP_PWD_PERIOD.length);
        mSetNumOneBtn.setChecked(true);

        mTitleTV.setText(R.string.create_temp_pwd);
    }

    private void initEvent() {
        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        mConfirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean once = true;
                if (mSetNumOneBtn.isChecked()) {
                    once = true;
                } else if (mSetNumMoreBtn.isChecked()) {
                    once = false;
                }
                if (getActivity() instanceof OnCreateTmpPwdListener) {
                    ((OnCreateTmpPwdListener) getActivity()).onCreateTmpPwdConfirm(
                            once,
                            mSetPeriod.getValue(),
                            mRequestCode);
                }
                dismiss();
            }
        });
    }

    public interface OnCreateTmpPwdListener {
        void onCreateTmpPwdConfirm(boolean once, int period, int requestCode);
    }
}
