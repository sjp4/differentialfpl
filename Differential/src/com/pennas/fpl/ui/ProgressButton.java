package com.pennas.fpl.ui;

import com.pennas.fpl.App;
import com.pennas.fpl.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class ProgressButton extends View {
	
	public static final int STATE_OK = 0;
	public static final int STATE_OLD = 1;
	public static final int STATE_PROC = 2;
	
	private static final int[] colours = { 0xff00ff00, 0xffff0000, 0xffffff00 };
	private static final int[] endColours = { 0xff006600, 0xff660000, 0xff666600 };
	
	private static final String  maxTextLen = "Processing xx";
	private static float maxTextWidth = 0;
	
	private int state = STATE_OLD;
	private int progress = 0;
	private String label;
	private String status;
	private String desc;
	private static int currentHeight = 0;
	private static int currentWidth = 0;
	
	private static Paint paintLabel;
	private static Paint paint;
	
	private static float barLeft;
	private static float barRight;
	private static float barWidth;
	private static float barTop;
	private static float barBottom;
	private static float textY;
	private static float textX;
	private static float labelTextSize;
	private static float stateTextSize;
	
	public static LinearGradient linGrad[] = new LinearGradient[colours.length];
	
	public ProgressButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ProgressButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgressButton(Context context) {
        super(context);
    }
    
    public void setProgress(int p) {
    	progress = p;
    	state = STATE_PROC;
    	invalidate();
    }
    
    public void setState(int s) {
    	state = s;
    	invalidate();
    }
    
    public void setLabel(String s) {
    	label = s;
    	invalidate();
    }
    
    public void setStatus(String s) {
    	status = s;
    	invalidate();
    }
    
    public void setDesc(String s) {
    	desc = s;
    	invalidate();
    }

    @SuppressLint("DrawAllocation")
	@Override
    protected void onDraw(Canvas canvas) {
    	int height = getHeight();
    	int width = getWidth();
    	
    	if (paint == null) {
    		paint = new Paint();
    		paint.setStyle(Paint.Style.FILL);
    		paint.setAntiAlias(true);
    	}
    	if (paintLabel == null) {
    		paintLabel = new Paint();
    		paintLabel.setStyle(Paint.Style.FILL);
    		paintLabel.setColor(0xffffffff);
    		paintLabel.setAntiAlias(true);
    	}
    	if (currentHeight != height) {
    		barTop = 6;
    		textX = 5;
    		labelTextSize = height / 2;
    		textY = labelTextSize + 3;
    		stateTextSize = App.appContext.getResources().getDimension(R.dimen.sync_desc_state_size);
    		barBottom = ((float) (height)) - (stateTextSize * 1.5f);
    	}
    	for (int i=0; i<colours.length; i++) {
    		if ( (linGrad[i] == null) || (currentHeight != height) ) {
    			linGrad[i] = new LinearGradient(0, 0, 0, height, colours[i], endColours[i], Shader.TileMode.MIRROR);
    		}
    	}
    	
    	paintLabel.setTextSize(labelTextSize);
		if (maxTextWidth == 0) maxTextWidth = paintLabel.measureText(maxTextLen);
    	
    	if (currentWidth != width) {
    		barLeft = maxTextWidth + 5;
    		barRight = width - 7;
    		barWidth = barRight - barLeft;
    	}
    	
    	currentWidth = width;
    	currentHeight = height;
    	
    	// label
    	if (label != null) {
    		paint.setShader(null);
        	paint.setColor(0x44000000);
        	canvas.drawRect(textX - 1, 3, textX + paintLabel.measureText(label) + 2, textY + 5, paint);
    		canvas.drawText(label, textX, textY, paintLabel);
    	}
    	
    	paint.setShader(linGrad[state]);
    	paint.setColor(colours[state]);
    	canvas.drawRect(barLeft, barTop, barRight, barBottom, paint);
    	
	    if (state == STATE_PROC) {
	    	paint.setShader(linGrad[STATE_OK]);
	    	paint.setColor(colours[STATE_OK]);
	    	
	    	float progRight = barLeft + (((float)progress/(float)100)*barWidth);
	    	
	    	// progress bar
	    	canvas.drawRect(barLeft, barTop, progRight, barBottom, paint);
    	}
	    
	    // inset effect
	    paint.setShader(null);
    	paint.setColor(0xf5444444);
	    canvas.drawRect(barLeft, barTop, barRight, barTop + 6, paint);
	    canvas.drawRect(barLeft, barTop + 6, barLeft + 6, barBottom, paint);
	    
	    if (status != null) {
	    	paintLabel.setTextSize(stateTextSize);
	    	float statusX = width - paintLabel.measureText(status) - 10;
	    	canvas.drawText(status, statusX, barBottom - 4, paintLabel);
	    }
	    
	    if (desc != null) {
	    	paintLabel.setTextSize(stateTextSize);
	    	canvas.drawText(desc, 8, height - 7, paintLabel);
	    }
	    
	    // divider
	    paint.setShader(null);
    	paint.setColor(0x99dddddd);
	    paint.setShadowLayer(2, 2, 1, 0xccdddddd);
	    canvas.drawLine(10, height - 2, width - 10, height - 1, paint);
	    paint.setShadowLayer(0, 0, 0, 0xccdddddd);
    }

}
