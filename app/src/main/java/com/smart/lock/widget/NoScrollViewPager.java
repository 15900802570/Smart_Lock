package com.smart.lock.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class NoScrollViewPager extends LazyViewPager {
    public NoScrollViewPager(Context context) {
        super(context);
    }

    public NoScrollViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean onInterceptHoverEvent(MotionEvent event) {
        return false;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }
}