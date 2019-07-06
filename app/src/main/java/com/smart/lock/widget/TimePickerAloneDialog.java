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
import android.widget.TextView;

import com.smart.lock.R;
import com.smart.lock.utils.ConstantUtil;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.ToastUtil;

import java.util.Objects;

@SuppressLint("ValidFragment")
public class TimePickerAloneDialog extends DialogFragment {

    private int[] value = {0, 1};
    private TextView mDialogTitleTv;
    private NumberPicker mHourNP;
    private NumberPicker mMinuteNP;
    private Button mCancelBtn;
    private Button mConfirmBtn;
    private Button mCloseBtn;

    private View mView;
    private boolean isWithClose;
    private int requestCode;
    private String mTitleStr;

    /**
     * 初始化
     *
     * @param value       int[] 初始化时间
     * @param isWithClose boolean 是否需要关闭btn
     * @param title       String dialog title
     * @param requestCode int Dialog请求参数
     */
    @SuppressLint("ValidFragment")
    public TimePickerAloneDialog(int[] value, boolean isWithClose, String title, int requestCode) {
        if (value.length >= 2) {
            this.value[0] = value[0];
            this.value[1] = value[1];
        }
        this.isWithClose = isWithClose;
        this.mTitleStr = title;
        this.requestCode = requestCode;
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
        mView = inflater.inflate(R.layout.dialog_set_time, container, false);
        mDialogTitleTv = mView.findViewById(R.id.time_picker_dialog_title_tv);
        mHourNP = mView.findViewById(R.id.start_hour_np);
        mMinuteNP = mView.findViewById(R.id.start_minute_np);
        mCancelBtn = mView.findViewById(R.id.time_picker_dialog_cancel_btn);
        mConfirmBtn = mView.findViewById(R.id.time_picker_dialog_confirm_btn);
        mCloseBtn = mView.findViewById(R.id.time_picker_dialog_close_btn);

        mHourNP.setDisplayedValues(ConstantUtil.HOUR);
        mMinuteNP.setDisplayedValues(ConstantUtil.MINUTE);

        mHourNP.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mMinuteNP.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        mHourNP.setMinValue(1);
        mMinuteNP.setMinValue(1);

        mHourNP.setMaxValue(ConstantUtil.HOUR.length);
        mMinuteNP.setMaxValue(ConstantUtil.MINUTE.length);

        setCloseBtnVisible(isWithClose); //设置是否需要关闭Btn
        setDialogTitleTv(mTitleStr);    //设置Title
        return mView;
    }

    private void initData() {
        mHourNP.setValue(value[0] + 1);
        mMinuteNP.setValue(value[1] + 1);
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
                setValue();
                if (value[0] == value[1]) {
                    ToastUtil.showShort(getActivity(), Objects.requireNonNull(getActivity()).getString(R.string.set_time_error));
                } else {
                    LogUtil.d("mConfirmBtn","mConfirmBtn");
                    if (getActivity() instanceof onTimePickerListener) {
                        ((onTimePickerListener) getActivity()).onTimePickerClickConfirm(getLastValue(), requestCode);
                    }
                    dismiss();
                }
            }
        });

        mCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getActivity() instanceof onTimePickerListener) {
                    ((onTimePickerListener) getActivity()).onTimePickerClickConfirm(null, requestCode);
                }
                dismiss();
            }
        });
    }

    private void setValue() {
        value[0] = mHourNP.getValue() - 1;
        value[1] = mMinuteNP.getValue() - 1;
    }

    private int[] getLastValue() {
        return value;
    }

    private void setCloseBtnVisible(boolean value) {
        if (value) {
            mCloseBtn.setVisibility(View.VISIBLE);
        } else {
            mCloseBtn.setVisibility(View.GONE);
        }
    }

    private void setDialogTitleTv(String titleTv) {
        mDialogTitleTv.setText(titleTv);
    }

    /**
     * 回调函数，用于获取设置的时间段，用请求参数区分不同的Dialog
     */
    public interface onTimePickerListener {
        void onTimePickerClickConfirm(int[] value, int requestCode);
    }
}
