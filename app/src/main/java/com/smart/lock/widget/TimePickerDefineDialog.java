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

import com.smart.lock.R;
import com.smart.lock.utils.ConstantUtil;

import java.util.Objects;

@SuppressLint("ValidFragment")
public class TimePickerDefineDialog extends DialogFragment {

    private int[] value = {0, 1, 2, 3};
    private NumberPicker mStartHourNP;
    private NumberPicker mStartMinuteNP;
    private NumberPicker mEndHourNP;
    private NumberPicker mEndMinuteNP;
    private Button mCancelBtn;
    private Button mConfirmBtn;
    private Button mCloseBtn;

    private View mView;
    private boolean isWithClose;
    private int requestCode;

    @SuppressLint("ValidFragment")
    public TimePickerDefineDialog(int[] value, boolean isWithClose, int requestCode) {
        if (value.length != 4) {
            return;
        }
        this.value[0] = value[0];
        this.value[1] = value[1];
        this.value[2] = value[2];
        this.value[3] = value[3];
        this.isWithClose = isWithClose;
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
        mView = inflater.inflate(R.layout.dialog_set_time_period, container, false);
        mStartHourNP = mView.findViewById(R.id.start_hour_np);
        mStartMinuteNP = mView.findViewById(R.id.start_minute_np);
        mEndHourNP = mView.findViewById(R.id.end_hour_np);
        mEndMinuteNP = mView.findViewById(R.id.end_minute_np);
        mCancelBtn = mView.findViewById(R.id.time_picker_dialog_cancel_btn);
        mConfirmBtn = mView.findViewById(R.id.time_picker_dialog_confirm_btn);
        mCloseBtn = mView.findViewById(R.id.time_picker_dialog_close_btn);

        mStartHourNP.setDisplayedValues(ConstantUtil.HOUR);
        mStartMinuteNP.setDisplayedValues(ConstantUtil.MINUTE);
        mEndHourNP.setDisplayedValues(ConstantUtil.HOUR);
        mEndMinuteNP.setDisplayedValues(ConstantUtil.MINUTE);

        mStartHourNP.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mStartMinuteNP.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mEndHourNP.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mEndMinuteNP.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        mStartHourNP.setMinValue(1);
        mStartMinuteNP.setMinValue(1);
        mEndHourNP.setMinValue(1);
        mEndMinuteNP.setMinValue(1);

        mStartHourNP.setMaxValue(ConstantUtil.HOUR.length);
        mStartMinuteNP.setMaxValue(ConstantUtil.MINUTE.length);
        mEndHourNP.setMaxValue(ConstantUtil.HOUR.length);
        mEndMinuteNP.setMaxValue(ConstantUtil.MINUTE.length);

        setCloseBtnVisible(isWithClose);
        return mView;
    }

    private void initData() {
        mStartHourNP.setValue(value[0]);
        mStartMinuteNP.setValue(value[1]);
        mEndHourNP.setValue(value[2]);
        mEndMinuteNP.setValue(value[3]);
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
                if (getActivity() instanceof onTimePickerListener) {
                    ((onTimePickerListener) getActivity()).onTimePickerClickConfirm(getLastValue(), requestCode);
                }
                dismiss();
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
        value[0] = mStartHourNP.getValue();
        value[1] = mStartMinuteNP.getValue();
        value[2] = mEndHourNP.getValue();
        value[3] = mEndMinuteNP.getValue();
    }

    public int[] getLastValue() {
        return value;
    }

    public void setCloseBtnVisible(boolean value) {
        if (value) {
            mCloseBtn.setVisibility(View.VISIBLE);
        } else {
            mCloseBtn.setVisibility(View.GONE);
        }
    }

    public interface onTimePickerListener {
        void onTimePickerClickConfirm(int[] value, int requestCode);
    }
}
