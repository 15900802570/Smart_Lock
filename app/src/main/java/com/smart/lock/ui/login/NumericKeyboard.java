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
    private float[] xs = new float[3];//声明数组保存每一列的圆心横坐标
    private float[] ys = new float[4];//声明数组保存每一排的圆心纵坐标
    private float circle_x, circle_y;//点击处的圆心坐标
    private float offset_x = 0, offset_y = 0; //偏移大小
    private int number = -1;//点击的数字
    private float radius = 0; //半径
    private float click_radius = 0;
    private float size = 0; //字体大小
    private OnNumberClick onNumberClick;//数字点击事件
    Shader mShader;
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
        // 绘制1的x坐标
        float first_x = (float) screen_width / 11;
        // 获取绘制1的y坐标
        // 绘制1的y坐标
        float first_y = (float) (SystemUtils.getSystemDisplay(context)[1] - SystemUtils.getSystemDisplay(context)[1] / 3) / (float) 3.5;
        radius = getResources().getDimension(R.dimen.x50dp);
        click_radius = first_x * (float) 1.5;
        size = getResources().getDimension(R.dimen.d33sp);
        offset_x = -getResources().getDimension(R.dimen.x13dp);
        offset_y = getResources().getDimension(R.dimen.x15dp);
        //添加每一排的横坐标
        xs[0] = first_x * (float) 2.5;
        xs[1] = first_x * (float) 5.5;
        xs[2] = first_x * (float) 8.5;
        //添加每一列的纵坐标
        ys[0] = first_y;
        ys[1] = first_y + first_x * 3;
        ys[2] = first_y + first_x * 6;
        ys[3] = first_y + first_x * 9;

        mShader = new LinearGradient(
                first_x,
                first_y,
                first_x * 11,
                first_y + first_x * 9,
                Color.rgb(194, 164, 255),
                Color.rgb(105, 174, 254),
                Shader.TileMode.MIRROR);
        cPaint.setShader(mShader);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 创建画笔对象
        // 绘制文本,注意是从坐标开始往上绘制
        mPaint = new Paint();
        mPaint.setShader(mShader);
        mPaint.setTextSize(size);// 设置字体大小
        mPaint.setStrokeWidth(2);
        mPaint.setAntiAlias(true);//设置抗锯齿
        mPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        // 这里较难的就是算坐标
        // 绘制第一排1,2,3
        canvas.drawText("1", xs[0] + offset_x, ys[0]+ offset_y, mPaint);
        canvas.drawText("2", xs[1] + offset_x, ys[0]+ offset_y, mPaint);
        canvas.drawText("3", xs[2] + offset_x, ys[0] + offset_y, mPaint);
        // 绘制第2排4,5,6
        canvas.drawText("4", xs[0]+ offset_x, ys[1] + offset_y, mPaint);
        canvas.drawText("5", xs[1] + offset_x, ys[1] + offset_y, mPaint);
        canvas.drawText("6", xs[2] + offset_x, ys[1] + offset_y, mPaint);
        // 绘制第3排7,8,9
        canvas.drawText("7", xs[0] + offset_x, ys[2] + offset_y, mPaint);
        canvas.drawText("8", xs[1] + offset_x, ys[2] + offset_y, mPaint);
        canvas.drawText("9", xs[2] + offset_x, ys[2] + offset_y, mPaint);
        // 绘制第4排0
        canvas.drawText("0", xs[1] + offset_x, ys[3] + offset_y, mPaint);
        //为每一个数字绘制一个圆
        //设置绘制空心圆
//        mPaint.setShader(mShader);
        mPaint.setStyle(Paint.Style.STROKE);
        //依次绘制第一排的圆
        canvas.drawCircle(xs[0], ys[0], radius, mPaint);
        canvas.drawCircle(xs[1], ys[0], radius, mPaint);
        canvas.drawCircle(xs[2], ys[0], radius, mPaint);
        //依次绘制第2排的圆
        canvas.drawCircle(xs[0], ys[1], radius, mPaint);
        canvas.drawCircle(xs[1], ys[1], radius, mPaint);
        canvas.drawCircle(xs[2], ys[1], radius, mPaint);
        //依次绘制第3排的圆
        canvas.drawCircle(xs[0], ys[2], radius, mPaint);
        canvas.drawCircle(xs[1], ys[2], radius, mPaint);
        canvas.drawCircle(xs[2], ys[2], radius, mPaint);
        //绘制最后一个圆
        canvas.drawCircle(xs[1], ys[3], radius, mPaint);

        //判断是否点击数字(点击数字产生的渐变效果)

        tPaint.setTextSize(size);// 设置字体大小
        tPaint.setStrokeWidth(2);
        tPaint.setAntiAlias(true);//设置抗锯齿
        tPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        tPaint.setColor(Color.WHITE);
        if (circle_x > 0 && circle_y > 0) {
            if (type == 0) {//按下刷新
                cPaint.setStyle(Paint.Style.FILL_AND_STROKE);//按下的时候绘制实心圆
                canvas.drawCircle(circle_x, circle_y, radius, cPaint);//绘制圆
                canvas.drawText(String.valueOf(number), circle_x + offset_x, circle_y + offset_y, tPaint);

            } else if (type == 1) {//弹起刷新
                mPaint.setColor(Color.WHITE);//设置画笔颜色
                mPaint.setStyle(Paint.Style.STROKE);//弹起的时候再绘制空心圆
                canvas.drawCircle(circle_x, circle_y, radius, mPaint);//绘制圆
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
//                tShader = new LinearGradient(
//                        circle_x - radius,
//                        circle_y - radius,
//                        circle_x + radius,
//                        circle_y + radius,
//                        Color.rgb(194, 164, 255),
//                        Color.rgb(105, 174, 254),
//                        Shader.TileMode.MIRROR);
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
        if (xs[0] - click_radius <= x && x <= xs[0] + click_radius) {//第一列
            //获取点击处的圆心横坐标
            circle_x = xs[0];
            //判断点击的是哪一排
            if (ys[0] - click_radius <= y && ys[0] + click_radius >= y) {//第1排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[0];
                number = 1;//设置点击的数字
            } else if (ys[1] - click_radius <= y && ys[1] + click_radius >= y) {//第2排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[1];
                number = 4;//设置点击的数字
            } else if (ys[2] - click_radius <= y && ys[2] + click_radius >= y) {//第3排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[2];
                number = 7;//设置点击的数字
            }
        } else if (xs[1] - click_radius <= x && x <= xs[1] + click_radius) {//第2列
            //获取点击处的圆心横坐标
            circle_x = xs[1];
            //判断点击的是哪一排
            if (ys[0] - click_radius <= y && ys[0] + click_radius >= y) {//第1排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[0];
                number = 2;//设置点击的数字
            } else if (ys[1] - click_radius <= y && ys[1] + click_radius >= y) {//第2排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[1];
                number = 5;//设置点击的数字
            } else if (ys[2] - click_radius <= y && ys[2] + click_radius >= y) {//第3排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[2];
                number = 8;//设置点击的数字
            } else if (ys[3] - click_radius <= y && ys[3] + click_radius >= y) {//第4排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[3];
                number = 0;//设置点击的数字
            }
        } else if (xs[2] - click_radius <= x && x <= xs[2] + click_radius) {//第3列
            //获取点击处的圆心横坐标
            circle_x = xs[2];
            //判断点击的是哪一排
            if (ys[0] - click_radius <= y && ys[0] + click_radius >= y) {//第1排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[0];
                number = 3;//设置点击的数字
            } else if (ys[1] - click_radius <= y && ys[1] + click_radius >= y) {//第2排
                //获取点击的数字圆的圆心纵坐标
                circle_y = ys[1];
                number = 6;//设置点击的数字
            } else if (ys[2] - click_radius <= y && ys[2] + click_radius >= y) {//第3排
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
