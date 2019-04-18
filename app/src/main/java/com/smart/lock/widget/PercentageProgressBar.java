package com.smart.lock.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import com.smart.lock.R;
import com.smart.lock.utils.LogUtil;

public class PercentageProgressBar extends ProgressBar {
    private static final String TAG = PercentageProgressBar.class.getSimpleName();
    String text;
    Paint mPaint;
    private Context mContext;

    public PercentageProgressBar(Context context) {
        super(context);
        mContext = context;
        System.out.println("1");
        initText();
    }

    public PercentageProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        System.out.println("2");
        mContext = context;
        initText();
    }

    public PercentageProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        System.out.println("3");
        mContext = context;
        initText();
    }

    @Override
    public synchronized void setProgress(int progress) {
        setText(progress);
        super.setProgress(progress);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //this.setText();
        Rect rect = new Rect();
        this.mPaint.getTextBounds(this.text, 0, this.text.length(), rect);

        int x = (getWidth() / 2) - rect.centerX();
        int y = (getHeight() / 2) - rect.centerY();
        LogUtil.d(TAG, "x : " + x + " y : " + y);
        canvas.drawText(this.text, x, y, this.mPaint);
    }

    //初始化，画笔  
    private void initText() {
        this.mPaint = new Paint();
        this.mPaint.setColor(mContext.getResources().getColor(R.color.yellow_selete));
        mPaint.setTextSize(mContext.getResources().getDimension(R.dimen.d18sp));
    }

//  private void setText() {  
//      setText(this.getProgress());  
//  }  

    //设置文字内容  
    private void setText(int progress) {
//        int i = (progress * 1) / this.getMax();
        this.text = String.valueOf(progress) + "%";
    }
} 