package com.smart.dt.widget;
/**
 * @version 创建时间：2014-4-9 下午4:20:38
 * 说明：
 */

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.Button;

public class RedButton extends Button {

    public RedButton(Context context) {
        super(context);
        init(context);
    }

    public RedButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RedButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context){
    	
    	setTypeface(Typeface.DEFAULT_BOLD);
    }
    
}
