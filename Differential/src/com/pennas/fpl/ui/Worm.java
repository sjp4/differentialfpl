package com.pennas.fpl.ui;


import com.pennas.fpl.App;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class Worm extends View {
	
	public int total_pts = 0;
	public float max_pts = 0;
	
	public boolean team = false;
	public boolean squad = false;
	public boolean squad_gw = false;
	public boolean showsubscore = true;
	public boolean fadescore = false;
	public boolean showtotal = true;
	
	private static final int num_bars = 4;
	public float points[] = new float[num_bars];
	private static final String labels[] = { "App/CS ", "Goal ", "Assist", "Bonus" };
	private static final String labels_team[] = { "Home", "Away" };
	private static final String labels_squad[] = { "", "GW" };
	private static final String labels_squad_gw[] = { "", "Hit" };
	
	//private static final int colours[]    = { 0xffFE0001, 0xff4D4DFF, 0xff6FFF00, 0xffFF8105 };
	//private static final int endColours[] = { 0xff650001, 0xff1D1D66, 0xff1E6600, 0xff661F01 };
	
	private static final int startColours[] = { 0xff110001, 0xff040411, 0xff061200, 0xff131701 };
	private static final int colours[]      = { 0xffFE0001, 0xff4D4DFF, 0xff6FFF00, 0xffFF9105 };
	private static final int endColours[]   = { 0xff4a0001, 0xff1a1a3d, 0xff164000, 0xff3e2702 };
	
	private static final int textColours[] = { 0xffffffff, 0xffffffff, 0xff000000, 0xffffffff };
	
	private static final int BAR_COLOUR = 0xd7ffffff;
	//private static final int BG_COLOUR = 0xaa2f2f2f;
	
	final static private int xr = 10;
	
	public static Paint paint;
	public static Paint paintT;
	
	// keep gradients static to save object creation
	public static LinearGradient linGrad[] = new LinearGradient[num_bars];
	public static int currentHeight = 0;
	
	private RectF rec;
	
	public Worm(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public Worm(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public Worm(Context context) {
        super(context);
    }
    
    public static void clean() {
    	for (int i=0; i<num_bars; i++) {
    		linGrad[i] = null;
    	}
    	paint = null;
    	paintT = null;
    }
    
    private void init(int height) {
    	if (paint == null) {
	    	paint = new Paint();
	    	paint.setStyle(Paint.Style.FILL);
	    	paint.setAntiAlias(true);
    	}
    	
    	if (paintT == null) {
	    	paintT = new Paint();
	    	paintT.setAntiAlias(true);
	    	paintT.setStyle(Paint.Style.FILL);
    	}
    	
    	for (int i=0; i<num_bars; i++) {
    		if ( (linGrad[i] == null) || (currentHeight != height) ) {
    			//linGrad[i] = new LinearGradient(0, 0, 0, height, colours[i], endColours[i], Shader.TileMode.MIRROR);
    			int[] grad = { startColours[i], colours[i], endColours[i] };
    			linGrad[i] = new LinearGradient(0, 0, 0, height, grad, null, Shader.TileMode.MIRROR);
    		}
    	}
    	
    	currentHeight = height;
    	if (rec == null) rec = new RectF();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
    	if (max_pts > 0) {
    		int height = getHeight();
	    	int width = getWidth();
	    	
	    	init(height);
	    	
	    	// background
	    	//paint.setShader(null);
	    	//paint.setColor(BG_COLOUR);
	    	//canvas.drawRect(0, 0, width, height, paint);
	    	
	    	int[] rights = new int[num_bars];
	    	int size;
	    	
	    	paintT.setTextSize(height - 3);
	    	
	    	// get width of total... for overal spacing
	    	paintT.setTypeface(Typeface.DEFAULT_BOLD);
	    	int totalScoreWidth = (int) paintT.measureText(String.valueOf(total_pts));
	    	// set width of worm from that
	    	int wormWidth = width - totalScoreWidth - 9;
	    	// reset
	    	paintT.setTypeface(Typeface.DEFAULT);
	    	
	    	// bars
	    	float running = total_pts;
	    	for (int i=num_bars-1; i>=0; i--) {
	    		size = (int) ((running / max_pts) * wormWidth);
	    		rights[i] = size;
	    		running -= points[i];
	    	}
	    	
	    	for (int i=num_bars-1; i>=0; i--) {
	    		// bar - draw beyond canvas to avoid unwanted rounded edges
	    		if (linGrad[i]==null) App.log("grad null");
	    		paint.setShader(linGrad[i]);
	    		paint.setColor(BAR_COLOUR);
	    		
	    		rec.bottom = height * 2;
	    		rec.left = -xr;
	    		rec.right = rights[i];
	    		rec.top = 2;
	    		canvas.drawRoundRect(rec, xr, height/2, paint);
	    		
	    		// text
	    		int x;
	    		if (i==0) {
	    			x = 0;
	    		} else {
	    			x = rights[i-1];
	    		}
	    		String ps = String.valueOf((int)points[i]);
	    		float prevSize = paintT.getTextSize();
	    		paintT.setTextSize(prevSize - 3);
	    		float measText = paintT.measureText(ps);
	    		if ( (measText <= (rights[i]-x)) && showsubscore) {
		    		if ( (squad || squad_gw) && (i==0) && (points[i+1] == 0) ) {
		    			// no number if is the only part of the worm...
		    		} else {
		    			paintT.setColor(textColours[i]);
			    		canvas.drawText(ps, x+1, height-2, paintT);
		    		}
		    		measText += 3;
	    		} else {
	    			measText = 0;
	    		}
	    		paintT.setTextSize(prevSize);
		    	
	    		// label
	    		String label = null;
	    		if (team) {
	    			if (i < labels_team.length) {
	    				label = labels_team[i];
	    			}
	    		} else if (squad) {
	    			if (i < labels_squad.length) {
	    				label = labels_squad[i];
	    			}
	    		} else if (squad_gw) {
	    			if (i < labels_squad_gw.length) {
	    				label = labels_squad_gw[i];
	    			}
	    		} else {
	    			if (i < labels.length) {
	    				label = labels[i];
	    			}
	    		}
	    		if (label != null) {
		    		prevSize = paintT.getTextSize();
		    		paintT.setTextSize(prevSize - 4);
	    			if (paintT.measureText(label) <= (rights[i] - x - measText) ) {
	    				paintT.setColor(0x55ffffff);
		    			canvas.drawText(label, x+3+measText, height-2, paintT);
		    		}
	    			paintT.setTextSize(prevSize);
	    		}
	    	}
	    	
	    	if (showtotal) {
		    	// total score text
		    	if (fadescore) {
		    		paintT.setColor(0xffbbbb00);
		    	} else {
		    		paintT.setColor(0xffffff00);
		    	}
		    	paintT.setTypeface(Typeface.DEFAULT_BOLD);
		    	canvas.drawText(String.valueOf(total_pts), rights[num_bars-1]+2, height-3, paintT);
	    	}
	    	
    	} // if
    } // sub

}
