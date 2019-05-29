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
import com.smart.lock.utils.ToastUtil;

import java.util.Objects;
import java.util.Calendar;

@SuppressLint("ValidFragment")
public class TimePickerWithDateDefineDialog extends DialogFragment {

    private TextView mDialogTitleTv;
    private NumberPicker mYearNp;
    private NumberPicker mMonthNp;
    private NumberPicker mDayNp;
    private NumberPicker mHourNp;
    private NumberPicker mMinuteNp;
    private Button mConfirmBtn;
    private Button mCancelBtn;

    private View mView;

    private String mTitleStr;


    private int mYear, mMonth, mDay, mHour, mMinute, requestCode;

    /**
     *
     * @param title title
     * @param mYear 年
     * @param mMonth 月
     * @param mDay 日
     * @param mHour 时
     * @param mMinute 分
     * @param requestCode 请求参数
     */
    @SuppressLint("ValidFragment")
    public TimePickerWithDateDefineDialog(String title, int mYear, int mMonth, int mDay, int mHour, int mMinute, int requestCode) {
        this.mTitleStr = title;
        this.mYear = mYear;
        this.mMonth = mMonth;
        this.mDay = mDay;
        this.mHour = mHour;
        this.mMinute = mMinute;
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

    private View initView(LayoutInflater inflater, ViewGroup container) {
        mView = inflater.inflate(R.layout.dialog_set_date_and_time, container, false);
        mDialogTitleTv = mView.findViewById(R.id.dialog_time_with_date_picker_title_tv);
        mYearNp = mView.findViewById(R.id.year_np);
        mMonthNp = mView.findViewById(R.id.month_np);
        mDayNp = mView.findViewById(R.id.day_np);
        mHourNp = mView.findViewById(R.id.hour_np);
        mMinuteNp = mView.findViewById(R.id.minute_np);
        mCancelBtn = mView.findViewById(R.id.time_picker_with_date_dialog_cancel_btn);
        mConfirmBtn = mView.findViewById(R.id.time_picker_with_date_dialog_confirm_btn);
        Calendar mCalendar = Calendar.getInstance();

        mYearNp.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mMonthNp.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mDayNp.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mHourNp.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mMinuteNp.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        mYearNp.setMinValue(mCalendar.get(Calendar.YEAR));
        mYearNp.setMaxValue(2100);
        mYearNp.setValue(mCalendar.get(Calendar.YEAR));

        mMonthNp.setFormatter(formatter);
        mMonthNp.setMinValue(1);
        mMonthNp.setMaxValue(12);
        mMonthNp.setValue(mCalendar.get(Calendar.MONTH));

        judgeMonth();

        mDayNp.setFormatter(formatter);
        mDay = mCalendar.get(Calendar.DAY_OF_MONTH);
        mDayNp.setValue(mDay);


        mHourNp.setMaxValue(23);
        mHourNp.setMinValue(0);
        mHourNp.setValue(mHour);
        mHourNp.setFormatter(formatter);

        mMinuteNp.setMaxValue(59);
        mMinuteNp.setMinValue(0);
        mMinuteNp.setValue(mHour);
        mMinuteNp.setFormatter(formatter);

        return mView;
    }

    private void initData() {
        mDialogTitleTv.setText(mTitleStr);
        mYearNp.setValue(mYear);
        mMonthNp.setValue(mMonth);
        mDayNp.setValue(mDay);
        mHourNp.setValue(mHour);
        mMinuteNp.setValue(mMinute);
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
                if (getActivity() instanceof onTimeAndDatePickerListener) {
                    ((onTimeAndDatePickerListener) getActivity()).onTimeAndDatePickerClickConfirm(mYear, mMonth, mDay, mHour, mMinute, requestCode);
                }
                dismiss();
            }

        });
    }

    private void setValue() {
        mYear = mYearNp.getValue();
        mMonth = mMonthNp.getValue();
        mDay = mDayNp.getValue();

        mHour = mHourNp.getValue();
        mMinute = mMinuteNp.getValue();
    }

    /**
     * 回调函数，用于获取设置的时间段，用请求参数区分不同的Dialog
     */
    public interface onTimeAndDatePickerListener {
        void onTimeAndDatePickerClickConfirm(int mYear, int mMonth, int mDay, int mHour, int mMinute, int requestCode);
    }


    private NumberPicker.Formatter formatter = new NumberPicker.Formatter() {
        @Override
        public String format(int value) {
            String Str = String.valueOf(value);
            if (value < 10) {
                Str = "0" + Str;
            }
            return Str;
        }
    };

    private void judgeMonth() {
        if (mMonth == 2) {
            if (mYear % 4 == 0) {
                if (mDayNp.getMaxValue() != 29) {
                    mDayNp.setDisplayedValues(null);
                    mDayNp.setMinValue(1);
                    mDayNp.setMaxValue(29);
                }
            } else {
                if (mDayNp.getMaxValue() != 28) {
                    mDayNp.setDisplayedValues(null);
                    mDayNp.setMinValue(1);
                    mDayNp.setMaxValue(28);
                }
            }
        } else {
            switch (mMonth) {
                case 4:
                case 6:
                case 9:
                case 11:
                    if (mDayNp.getMaxValue() != 30) {
                        mDayNp.setDisplayedValues(null);
                        mDayNp.setMinValue(1);
                        mDayNp.setMaxValue(30);
                    }
                    break;
                default:
                    if (mDayNp.getMaxValue() != 31) {
                        mDayNp.setDisplayedValues(null);
                        mDayNp.setMinValue(1);
                        mDayNp.setMaxValue(31);
                    }
            }
        }
        mDay = mDayNp.getValue();

    }

}
