package com.smart.lock.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;

import com.smart.lock.R;

import java.lang.reflect.Field;

public class NumberPickerView extends NumberPicker {
    private Paint mSelectorWheelPaint;
    private NumberPicker mNumberPicker;
    private boolean mHasSelectorWheel;
    private int mTopSelectionDividerTop;
    private int mBottomSelectionDividerBottom;
    private int[] mSelectorIndices;
    private int mScrollState;
    private SparseArray<String> mSelectorIndexToStringCache;
    private EditText mInputText;
    private int mSelectorElementHeight;
    private int mCurrentScrollOffset;
    private boolean mHideWheelUntilFocused;
    private boolean mDecrementVirtualButtonPressed;
    private boolean mIncrementVirtualButtonPressed;
    private Drawable mSelectionDivider;
    private Drawable mVirtualButtonPressedDrawable;
    private int mSelectionDividerHeight;

    /**
     * The number of items show in the selector wheel.
     */
    private static final int SELECTOR_WHEEL_ITEM_COUNT = 3;

    /**
     * The index of the middle selector item.
     */
    private static final int SELECTOR_MIDDLE_ITEM_INDEX = SELECTOR_WHEEL_ITEM_COUNT / 2;


    public NumberPickerView(Context context) {
        super(context);
        mNumberPicker = this;
    }

    public NumberPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mNumberPicker = this;
    }

    public NumberPickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mNumberPicker = this;
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NumberPickerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void addView(View child) {
        super.addView(child);
        updateView(child);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        super.addView(child, params);
        updateView(child);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        updateView(child);
    }

    private void updateView(View view) {
        setNumberPickerDividerColor(this);
    }


    private void setNumberPickerDividerColor(NumberPicker numberPicker) {
        Field[] pickerFields = NumberPicker.class.getDeclaredFields();
        for (Field pf : pickerFields) {
            if (pf.getName().equals("mSelectionDivider")) {
                pf.setAccessible(true);
                try {
                    //设置分割线的颜色值
                    pf.set(numberPicker, new ColorDrawable(getResources().getColor(R.color.transparent)));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }

    }
/*

    @Override
    protected void onDraw(Canvas canvas) {
        int mLeft = super.getLeft();
        int mRight = super.getRight();
        int mBottom = super.getBottom();
        getMyValue();
//        float y = mCurrentScrollOffset;
//        final boolean showSelectorWheel = mHideWheelUntilFocused ? hasFocus() : true;
//        float x = (mRight - mLeft) / 2;
//        mSelectorWheelPaint.setColor(getResources().getColor(R.color.blue2));
//        int[] selectorIndices = mSelectorIndices;
//        for (int i = 0; i < selectorIndices.length; i++) {
//            int selectorIndex = selectorIndices[i];
//            String scrollSelectorValue = mSelectorIndexToStringCache.get(selectorIndex);
//            if (i != 1) {
//                mSelectorWheelPaint.setColor(getResources().getColor(R.color.black));
//                mInputText.setTextColor(getResources().getColor(R.color.black));
//                mSelectorWheelPaint.setTextSize(getResources().getDimension(R.dimen.d15sp));
//            } else {
//                mSelectorWheelPaint.setColor(getResources().getColor(R.color.blue2));
//                mSelectorWheelPaint.setTextSize(getResources().getDimension(R.dimen.d18sp));
//                mInputText.setTextColor(getResources().getColor(R.color.blue2));
//            }
//
//            if ((showSelectorWheel && i != 1) ||
//                    (i == 1 && mInputText.getVisibility() != VISIBLE)) {
//                Rect mRect = new Rect();
//                mSelectorWheelPaint.getTextBounds(scrollSelectorValue, 0, scrollSelectorValue.length(), mRect);
//                canvas.drawText(scrollSelectorValue, x, y, mSelectorWheelPaint);
//            }
//            y += mSelectorElementHeight;
//        }

        if (!mHasSelectorWheel) {
            super.onDraw(canvas);
            return;
        }
        final boolean showSelectorWheel = mHideWheelUntilFocused ? hasFocus() : true;
        float x = (mRight - mLeft) / 2;
        float y = mCurrentScrollOffset;
        int[] selectorIndices = mSelectorIndices;
        // draw the virtual buttons pressed state if needed
        if (showSelectorWheel && mVirtualButtonPressedDrawable != null
                && mScrollState == OnScrollListener.SCROLL_STATE_IDLE) {
            if (mDecrementVirtualButtonPressed) {
                mVirtualButtonPressedDrawable.setState(PRESSED_STATE_SET);
                mVirtualButtonPressedDrawable.setBounds(0, 0, mRight, mTopSelectionDividerTop);
                mVirtualButtonPressedDrawable.draw(canvas);
            }
            if (mIncrementVirtualButtonPressed) {
                mVirtualButtonPressedDrawable.setState(PRESSED_STATE_SET);
                mVirtualButtonPressedDrawable.setBounds(0, mBottomSelectionDividerBottom, mRight,
                        mBottom);
                mVirtualButtonPressedDrawable.draw(canvas);
            }
        }

        // draw the selector wheel
//        int[] selectorIndices = mSelectorIndices;
        for (int i = 0; i < selectorIndices.length; i++) {
            int selectorIndex = selectorIndices[i];
            if (i != 1) {
                mSelectorWheelPaint.setColor(getResources().getColor(R.color.black));
                mSelectorWheelPaint.setTextSize(getResources().getDimension(R.dimen.d15sp));
            } else {
//                mSelectorWheelPaint.setColor(getResources().getColor(R.color.blue2));
                mSelectorWheelPaint.setTextSize(getResources().getDimension(R.dimen.d20sp));
            }

            String scrollSelectorValue = mSelectorIndexToStringCache.get(selectorIndex);
            // Do not draw the middle item if input is visible since the input
            // is shown only if the wheel is static and it covers the middle
            // item. Otherwise, if the user starts editing the text via the
            // IME he may see a dimmed version of the old value intermixed
            // with the new one.
            if ((showSelectorWheel && i != SELECTOR_MIDDLE_ITEM_INDEX) ||
                    (i == SELECTOR_MIDDLE_ITEM_INDEX && mInputText.getVisibility() != VISIBLE)) {
                canvas.drawText(scrollSelectorValue, x, y, mSelectorWheelPaint);
            }
            y += mSelectorElementHeight;
        }

        // draw the selection dividers
        if (showSelectorWheel && mSelectionDivider != null) {
            // draw the top divider
            int topOfTopDivider = mTopSelectionDividerTop;
            int bottomOfTopDivider = topOfTopDivider + mSelectionDividerHeight;
            mSelectionDivider.setBounds(0, topOfTopDivider, mRight, bottomOfTopDivider);
            mSelectionDivider.draw(canvas);

            // draw the bottom divider
            int bottomOfBottomDivider = mBottomSelectionDividerBottom;
            int topOfBottomDivider = bottomOfBottomDivider - mSelectionDividerHeight;
            mSelectionDivider.setBounds(0, topOfBottomDivider, mRight, bottomOfBottomDivider);
            mSelectionDivider.draw(canvas);
        }
//        mInputText.setTextColor(getResources().getColor(R.color.blue2));
        mInputText.setTextSize(getResources().getDimension(R.dimen.d10sp));

    }
    private void getMyValue() {
        Field[] pickerFields = NumberPicker.class.getDeclaredFields();
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mSelectorWheelPaint")) {
                try {
                    mSelectorWheelPaint = (Paint) field.get(mNumberPicker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mHasSelectorWheel")) {
                try {
                    mHasSelectorWheel = (boolean) field.get(mNumberPicker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mSelectorElementHeight")) {
                try {
                    mSelectorElementHeight = (int) field.get(mNumberPicker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mCurrentScrollOffset")) {
                try {
                    mCurrentScrollOffset = (int) field.get(mNumberPicker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mInputText")) {
                try {
                    mInputText = (EditText) field.get(mNumberPicker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();

                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mSelectorIndexToStringCache")) {
                try {
                    mSelectorIndexToStringCache = (SparseArray<String>) field.get(mNumberPicker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mSelectorIndices")) {
                try {
                    mSelectorIndices = (int[]) field.get(mNumberPicker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mHasSelectorWheel")) {
                try {
                    mHasSelectorWheel = (boolean) field.get(mNumberPicker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mHideWheelUntilFocused")) {
                try {
                    mHideWheelUntilFocused = (boolean) field.get(mNumberPicker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mScrollState")) {
                try {
                    mScrollState = (int) field.get(mNumberPicker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mTopSelectionDividerTop")) {
                try {
                    mTopSelectionDividerTop = (int) field.get(mNumberPicker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mBottomSelectionDividerBottom")) {
                try {
                    mBottomSelectionDividerBottom = (int) field.get(mNumberPicker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mSelectionDivider")) {
                try {
                    mSelectionDivider = (Drawable) field.get(mNumberPicker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mVirtualButtonPressedDrawable")) {
                try {
                    mVirtualButtonPressedDrawable = (Drawable) field.get(mNumberPicker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mDecrementVirtualButtonPressed")) {
                try {
                    mDecrementVirtualButtonPressed = (Boolean) field.get(mNumberPicker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mIncrementVirtualButtonPressed")) {
                try {
                    mIncrementVirtualButtonPressed = (Boolean) field.get(mNumberPicker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mSelectionDividerHeight")) {
                try {
                    mSelectionDividerHeight = (int) field.get(mNumberPicker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
     */

}
