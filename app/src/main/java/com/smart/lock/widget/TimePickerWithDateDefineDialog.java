package com.smart.lock.widget;

import android.annotation.SuppressLint;
import android.content.Context;
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
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.utils.DateTimeUtil;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.ToastUtil;

import java.text.ParseException;
import java.util.Objects;
import java.util.Calendar;

@SuppressLint("ValidFragment")
public class TimePickerWithDateDefineDialog extends DialogFragment {

    private static final String TAG = TimePickerWithDateDefineDialog.class.getSimpleName();
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
    private Context mActivity;


    private int mYear, mMonth, mDay, mHour, mMinute, requestCode;
    private int sYear, sMonth, sDay;

    private long minTimeStamp;
    private Calendar mCalendar;

    /**
     * 构造函数
     *
     * @param title        title
     * @param timeStamp    初始时间戳
     * @param minTimeStamp 最小时间戳，0表示没有最小时间
     * @param requestCode  请求参数
     */
    @SuppressLint("ValidFragment")
    public TimePickerWithDateDefineDialog(Context context, String title, long timeStamp, long minTimeStamp, int requestCode) {
        mActivity = context;
        String timeStr = DateTimeUtil.timeStamp2Date(String.valueOf(timeStamp), "yyyyMMddHHmm");
        LogUtil.d(TAG,"timeStr : " + timeStr);
        this.mTitleStr = title;
        this.minTimeStamp = minTimeStamp;
        this.mYear = Integer.valueOf(timeStr.substring(0, 4));
        this.mMonth = Integer.valueOf(timeStr.substring(4, 6));
        this.mDay = Integer.valueOf(timeStr.substring(6, 8));
        this.mHour = Integer.valueOf(timeStr.substring(8, 10));
        this.mMinute = Integer.valueOf(timeStr.substring(10, 12));
        this.requestCode = requestCode;

        mCalendar = Calendar.getInstance();

        this.sYear = mCalendar.get(Calendar.YEAR);
        this.sMonth = mCalendar.get(Calendar.MONTH) + 1;
        this.sDay = mCalendar.get(Calendar.DAY_OF_MONTH);
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

        mYearNp.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mMonthNp.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mDayNp.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mHourNp.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mMinuteNp.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        mYearNp.setMinValue(mCalendar.get(Calendar.YEAR));
        mYearNp.setMaxValue(2100);
        mYearNp.setValue(mCalendar.get(Calendar.YEAR));
        mYearNp.setWrapSelectorWheel(false);
        mYearNp.setOnValueChangedListener(mOnYearChangedListener);

        mMonthNp.setFormatter(formatter);
        mMonthNp.setMinValue(1);
        mMonthNp.setMaxValue(12);
        mMonthNp.setValue(mCalendar.get(Calendar.MONTH));
        mMonthNp.setOnValueChangedListener(mOnMonthChangedListener);

        judgeMonth();

        mDayNp.setFormatter(formatter);
        mDay = mCalendar.get(Calendar.DAY_OF_MONTH);
        mDayNp.setValue(mDay);


        mHourNp.setMaxValue(23);
        mHourNp.setMinValue(0);
        mHourNp.setValue(mHour);
        mHourNp.setFormatter(formatter);
        mHourNp.setEnabled(false);

        mMinuteNp.setMaxValue(59);
        mMinuteNp.setMinValue(0);
        mMinuteNp.setValue(mHour);
        mMinuteNp.setFormatter(formatter);
        mMinuteNp.setEnabled(false);

        return mView;
    }

    private void initData() {
        mDialogTitleTv.setText(mTitleStr);
        mYearNp.setValue(mYear);
        mMonthNp.setValue(mMonth);
        mDayNp.setValue(mDay);
        mHourNp.setValue(mHour);
        mMinuteNp.setValue(mMinute);
        judgeYear();
        judgeMonth();
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
                long timeStamp = getValue();
                if (timeStamp <= minTimeStamp) {
                    ToastUtil.show(mActivity, mActivity.getString(R.string.time_is_invalid), Toast.LENGTH_LONG);
                } else {
                    if (getActivity() instanceof onTimeAndDatePickerListener) {
                        ((onTimeAndDatePickerListener) getActivity()).onTimeAndDatePickerClickConfirm(getValue(), requestCode);
                    }
                    dismiss();
                }
            }

        });
    }


    private NumberPicker.OnValueChangeListener mOnYearChangedListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            mYear = mYearNp.getValue();
            judgeYear();
            judgeMonth();
        }
    };
    private NumberPicker.OnValueChangeListener mOnMonthChangedListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            mMonth = mMonthNp.getValue();
            judgeMonth();
        }
    };

    private long getValue() {
        mYear = mYearNp.getValue();
        mMonth = mMonthNp.getValue();
        mDay = mDayNp.getValue();

        mHour = mHourNp.getValue();
        mMinute = mMinuteNp.getValue();

        String timeStr = getStr(mYear) + "-" + getStr(mMonth) + "-" + getStr(mDay) + " " +
                getStr(mHour) + ":" + getStr(mMinute) + ":00";

        try {
            long timeStamp = DateTimeUtil.dateToStamp(timeStr);
            return timeStamp / 1000;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }


    /**
     * 回调函数，用于获取设置的时间段，用请求参数区分不同的Dialog
     */
    public interface onTimeAndDatePickerListener {
        void onTimeAndDatePickerClickConfirm(long timeStamp, int requestCode);
    }


    private NumberPicker.Formatter formatter = new NumberPicker.Formatter() {
        @Override
        public String format(int value) {
            return getStr(value);
        }
    };

    private String getStr(int value) {
        String Str = String.valueOf(value);
        if (value < 10) {
            Str = "0" + Str;
        }
        return Str;
    }


    private void judgeYear() {
        if (mMonth == 2) {
            if (mYear % 4 == 0 && mYear % 100 != 0 || mYear % 400 == 0) {
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
        }
        if (mYear == sYear) {
            mMonthNp.setWrapSelectorWheel(false);
            mMonthNp.setDisplayedValues(null);
            mMonthNp.setMinValue(sMonth);
            mMonthNp.setMaxValue(12);
        } else {
            mMonthNp.setWrapSelectorWheel(true);
            mMonthNp.setDisplayedValues(null);
            mMonthNp.setMinValue(1);
            mMonthNp.setMaxValue(12);
        }
        mMonth = mMonthNp.getValue();
        mDay = mDayNp.getValue();
    }

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
        if (mYear == sYear && mMonth == sMonth) {
            mDayNp.setWrapSelectorWheel(false);
            mDayNp.setMinValue(sDay);
        } else {
            mDayNp.setWrapSelectorWheel(true);
            mDayNp.setMinValue(1);
        }
//        mDay = mDayNp.getValue();

    }

}
