package com.pennas.fpl.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class PageView extends View {
	
	public int num = 0;
	public int cur = 0;
	
	private Paint paint;

	public PageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public PageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PageView(Context context) {
        super(context);
        init();
    }
    
    private void init() {
    	paint = new Paint();
    	paint.setColor(0xaabbbb00);
    	paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
    	if (num > 0 && cur > 0) {
	    	int height = getHeight();
	    	int width = getWidth();
	    	
	    	int left = (width/num) * (cur-1);
	    	int right = (width/num) * (cur);
	    	
	    	canvas.drawRect(left, 0, right, height, paint);
    	}
    }

}
