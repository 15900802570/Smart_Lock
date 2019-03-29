package com.smart.lock.ui.login;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import com.smart.lock.R;
import com.smart.lock.utils.SystemUtils;

public class NumericKeyboard extends View {
    private float first_x = 0;// 绘制1的x坐标
    private float first_y = 0;// 绘制1的y坐标
    private float[] xs = new float[3];//声明数组保存每一列的圆心横坐标
    private float[] ys = new float[4];//声明数组保存每一排的圆心纵坐标
    private float circle_x, circle_y;//点击处的圆心坐标
    private int number = -1;//点击的数字
    private OnNumberClick onNumberClick;//数字点击事件
    Shader mShader, tShader;
    Paint mPaint,
            tPaint = new Paint(),
         cPaint = new Paint();
    /*
     * 判断刷新数据
     * -1 不进行数据刷新
     * 0  按下刷新
     * 1  弹起刷新
     */
    private int type = -1;

    /**
     * 构造方法
     *
     * @param context 上下文
     */
    public NumericKeyboard(Context context) {
        super(context);
        initData(context);// 初始化数据
    }

    public NumericKeyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
        initData(context);// 初始化数据
    }

    /**
     * 设置数字点击事件
     *
     * @param onNumberClick 数字点击事件
     */
    public void setOnNumberClick(OnNumberClick onNumberClick) {
        this.onNumberClick = onNumberClick;
    }

    // 初始化数据
    private void initData(Context context) {
        // 获取屏幕的宽度
        int screen_width = SystemUtils.getSystemDisplay(context)[0];
        // 获取绘制1的x坐标
        first_x = (float) screen_width / 4;
        // 获取绘制1的y坐标
        first_y = 50 + (float)(SystemUtils.getSystemDisplay(context)[1] - SystemUtils.getSystemDisplay(context)[1] / 3) / 4;
        //添加每一排的横坐标
        xs[0] = first_x + 10;
        xs[1] = first_x * 2 + 10;
        xs[2] = first_x * 3 + 10;
        //添加每一列的纵坐标
        ys[0] = 40 + first_y - 15;
        ys[1] = 40 + first_y + first_x - 15;
        ys[2] = 40 + first_y + first_x * 2 - 15;
        ys[3] = 40 + first_y + first_x * 3 - 15;

        mShader = new LinearGradient(
                first_x,
                first_y,
                first_x * 3 + 10,
                40 + first_y + first_x * 3 - 15,
                Color.rgb(194, 164, 255),
                Color.rgb(105, 174, 254),
                Shader.TileMode.MIRROR);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 创建画笔对象
        // 绘制文本,注意是从坐标开始往上绘制
        mPaint = new Paint();
        mPaint.setColor(Color.rgb(100,174,254));
        mPaint.setTextSize(40);// 设置字体大小
        mPaint.setStrokeWidth(2);
        mPaint.setAntiAlias(true);//设置抗锯齿
        mPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        // 这里较难的就是算坐标
        // 绘制第一排1,2,3
        canvas.drawText("1", first_x, 40 + first_y, mPaint);
        canvas.drawText("2", first_x * 2, 40 + first_y, mPaint);
        canvas.drawText("3", first_x * 3, 40 + first_y, mPaint);
        // 绘制第2排4,5,6
        canvas.drawText("4", first_x, 40 + first_y + first_x, mPaint);
        canvas.drawText("5", first_x * 2, 40 + first_y + first_x, mPaint);
        canvas.drawText("6", first_x * 3, 40 + first_y + first_x, mPaint);
        // 绘制第3排7,8,9
        canvas.drawText("7", first_x, 40 + first_y + first_x * 2, mPaint);
        canvas.drawText("8", first_x * 2, 40 + first_y + first_x * 2, mPaint);
        canvas.drawText("9", first_x * 3, 40 + first_y + first_x * 2, mPaint);
        // 绘制第4排0
        canvas.drawText("0", first_x * 2, 40 + first_y + first_x * 3, mPaint);
        //为每一个数字绘制一个圆
        //设置绘制空心圆
        mPaint.setShader(mShader);
        mPaint.setStyle(Paint.Style.STROKE);
        //依次绘制第一排的圆
        canvas.drawCircle(first_x + 10, 40 + first_y - 15, 70, mPaint);
        canvas.drawCircle(first_x * 2 + 10, 40 + first_y - 15, 70, mPaint);
        canvas.drawCircle(first_x * 3 + 10, 40 + first_y - 15, 70, mPaint);
        //依次绘制第2排的圆
        canvas.drawCircle(first_x + 10, 40 + first_y + first_x - 15, 70, mPaint);
        canvas.drawCircle(first_x * 2 + 10, 40 + first_y + first_x - 15, 70, mPaint);
        canvas.drawCircle(first_x * 3 + 10, 40 + first_y + first_x - 15, 70, mPaint);
        //依次绘制第3排的圆
        canvas.drawCircle(first_x + 10, 40 + first_y + first_x * 2 - 15, 70, mPaint);
        canvas.drawCircle(first_x * 2 + 10, 40 + first_y + first_x * 2 - 15, 70, mPaint);
        canvas.drawCircle(first_x * 3 + 10, 40 + first_y + first_x * 2 - 15, 70, mPaint);
        //绘制最后一个圆
        canvas.drawCircle(first_x * 2 + 10, 40 + first_y + first_x * 3 - 15, 70, mPaint);

        //判断是否点击数字(点击数字产生的渐变效果)

        tPaint.setTextSize(40);// 设置字体大小
        tPaint.setStrokeWidth(2);
        tPaint.setAntiAlias(true);//设置抗锯齿
        tPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        tPaint.setColor(Color.WHITE);
        if (circle_x > 0 && circle_y > 0) {
            if (type == 0) {//按下刷新

                cPaint.setShader(tShader);
                cPaint.setStyle(Paint.Style.FILL_AND_STROKE);//按下的时候绘制实心圆
                canvas.drawCircle(circle_x, circle_y, 70, cPaint);//绘制圆
                canvas.drawText(String.valueOf(number), circle_x - 10, circle_y +15, tPaint);

            } else if (type == 1) {//弹起刷新
                mPaint.setColor(Color.WHITE);//设置画笔颜色
                mPaint.setStyle(Paint.Style.STROKE);//弹起的时候再绘制空心圆
                canvas.drawCircle(circle_x, circle_y, 70, mPaint);//绘制圆
                //绘制完成后,重置
                circle_x = 0;
                circle_y = 0;
            }
        }
    }

    /**
     * 获取触摸点击事件
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //事件判断
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN://按下
                //判断点击的坐标位置
                float x = event.getX();//按下时的X坐标
                float y = event.getY();//按下时的Y坐标
                //判断点击的是哪一个数字圆
                handleDown(x, y);
                tShader=new LinearGradient(
                        circle_x - 50,
                        circle_y - 50,
                        circle_x + 50,
                        circle_y + 50,
                        Color.rgb(194, 164, 255),
                        Color.rgb(105, 174, 254),
                        Shader.TileMode.MIRROR);
                return true;
            case MotionEvent.ACTION_UP://弹起
                type = 1;//弹起刷新
                invalidate();//刷新界面
                //返回点击的数字
                if (onNumberClick != null && number != -1) {
                    onNumberClick.onNumberReturn(number);
                }
                setDefault();//恢复默认
                //发送辅助事件
                sendAccessEvent(R.string.numeric_keyboard_up);
                return true;
            case MotionEvent.ACTION_CANCEL://取消
                //恢复默认值
                setDefault();
                return true;
        }
        return false;
    }

    /*
     * 恢复默认值
     */
    private void setDefault() {
        circle_x = 0;
        circle_y = 0;
        type = -1;
        number = -1;
        sendAccessEvent(R.string.numeric_keyboard_cancel);
    }

    /*
     * 设置辅助功能描述
     */
    private void sendAccessEvent(int resId) {
        //设置描述
        setContentDescription(getContext().getString(resId));
        //发送辅助事件
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        setContentDescription(null);
    }

    /*
     * 判断点击的是哪一个数字圆
     */
    private void handleDown(float x, float y) {
        //判断点击的是那一列的数据
        if (xs[0] - 70 <= x && x <= xs[0] + 70) {//第一列
            //获取点击处的圆心横坐标
            circle_x = xs[0];
            //判断点击的是哪一排
            if (ys[0] - 70 <= y && ys[0] + 70 >= y) {//第1排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[0];
                number = 1;//设置点击的数字
            } else if (ys[1] - 70 <= y && ys[1] + 70 >= y) {//第2排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[1];
                number = 4;//设置点击的数字
            } else if (ys[2] - 70 <= y && ys[2] + 70 >= y) {//第3排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[2];
                number = 7;//设置点击的数字
            }
        } else if (xs[1] - 70 <= x && x <= xs[1] + 70) {//第2列
            //获取点击处的圆心横坐标
            circle_x = xs[1];
            //判断点击的是哪一排
            if (ys[0] - 70 <= y && ys[0] + 70 >= y) {//第1排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[0];
                number = 2;//设置点击的数字
            } else if (ys[1] - 70 <= y && ys[1] + 70 >= y) {//第2排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[1];
                number = 5;//设置点击的数字
            } else if (ys[2] - 70 <= y && ys[2] + 70 >= y) {//第3排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[2];
                number = 8;//设置点击的数字
            } else if (ys[3] - 70 <= y && ys[3] + 70 >= y) {//第4排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[3];
                number = 0;//设置点击的数字
            }
        } else if (xs[2] - 70 <= x && x <= xs[2] + 70) {//第3列
            //获取点击处的圆心横坐标
            circle_x = xs[2];
            //判断点击的是哪一排
            if (ys[0] - 70 <= y && ys[0] + 70 >= y) {//第1排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[0];
                number = 3;//设置点击的数字
            } else if (ys[1] - 70 <= y && ys[1] + 70 >= y) {//第2排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[1];
                number = 6;//设置点击的数字
            } else if (ys[2] - 70 <= y && ys[2] + 70 >= y) {//第3排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[2];
                number = 9;//设置点击的数字
            }
        }
        sendAccessEvent(R.string.numeric_keyboard_down);
        type = 0;//按下刷新
        //绘制点击时的背景圆
        invalidate();
    }

    /**
     * 数字点击事件
     */
    public interface OnNumberClick {
        /**
         * 返回点击的数字
         *
         * @param number 返回数字
         */
        void onNumberReturn(int number);
    }
}
