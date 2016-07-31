package com.pennas.fpl.ui;


import com.pennas.fpl.Settings;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class Pie extends View {
	
	public int[] games = { 0, 0, 0 };
	private static final int num_segs = 3;
	
	private static final int startColours[] = { 0xffff0000, 0xffffff00, 0xff00ff00 };
	private static final int endColours[] =   { 0x55ff0000, 0x55ffff00, 0x5500ff00 };
	private static final int startColours_alt[] = { 0xff666666, 0xffcccccc, 0xffffffff };
	private static final int endColours_alt[] =   { 0x55666666, 0x55cccccc, 0x55ffffff };
	public static Paint paint;
	
	public static RadialGradient linGrad[] = new RadialGradient[num_segs];
	public int currentHeight = 0;
	
	private RectF rec;
	
	public Pie(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public Pie(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public Pie(Context context) {
        super(context);
    }
    
    public static void clean() {
    	paint = null;
    }
    
    private void init(int height, int width) {
    	if (paint == null) {
	    	paint = new Paint();
	    	paint.setStyle(Paint.Style.FILL);
	    	paint.setAntiAlias(true);
    	}
    	
    	for (int i=0; i<num_segs; i++) {
    		if ( (linGrad[i] == null) || (currentHeight != height) ) {
    			//linGrad[i] = new LinearGradient(0, 0, 0, height, colours[i], endColours[i], Shader.TileMode.MIRROR);
    			int[] grad = new int[2];
    			if (Settings.getBoolPref(Settings.PREF_ALTERNATE_TICKER_COLOURS)) {
    				grad[0] = startColours_alt[i];
    				grad[1] = endColours_alt[i];
    			} else {
    				grad[0] = startColours[i];
    				grad[1] = endColours[i];
    			}
    			
    			int radius = height / 2;
    			linGrad[i] = new RadialGradient(radius, radius, radius, grad, null, Shader.TileMode.MIRROR);
    		}
    	}
    	
    	currentHeight = height;
    	if (rec == null) {
    		rec = new RectF();
    		rec.bottom = height;
    		rec.left = 0;
    		rec.right = width;
    		rec.top = 0;
    	}
    	
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
    	int total = games[0] + games[1] + games[2];
    	if (total > 0) {
    		int height = getHeight();
	    	int width = getWidth();
	    	
	    	init(height, width);
	    	
	    	float start = 0;
	    	
	    	for (int i=0; i<num_segs; i++) {
	    		// bar - draw beyond canvas to avoid unwanted rounded edges
	    		//paint.setColor(colours[i]);
	    		paint.setShader(linGrad[i]);
	    		
	    		float mSweep = (float) 360 * ( (float) (games[i]) / (float)total );
	            canvas.drawArc(rec, start, mSweep, true, paint);
	            start += mSweep;
	    	}
	    	
    	} // if
    } // sub

}
