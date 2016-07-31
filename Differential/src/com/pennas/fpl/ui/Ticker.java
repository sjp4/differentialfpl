package com.pennas.fpl.ui;

import java.util.Arrays;
import java.util.Comparator;

import com.pennas.fpl.App;
import com.pennas.fpl.Settings;
import com.pennas.fpl.process.ProcessTeam;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

public class Ticker extends View {
	
	private int ticker_weeks;
	private int start_gameweek;
	
	private static final int max_gw_matches = 4;
	
	public static Paint paint;
	
	// keep gradients static to save object creation
	//public static LinearGradient linGrad[] = new LinearGradient[num_bars];
	private int currentHeight = 0;
	private int currentWidth = 0;
	
	private int[] left;
	private int[] right;
	
	private TickerData data;
	
	private static final int BORDER = 3;
	private static final int DGW_LINE_WIDTH = 4;
	private static final int HOME_AWAY_WIDTH = 3;
	
	public Ticker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public Ticker(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public Ticker(Context context) {
        super(context);
    }
    
    public static void clean() {
    	paint = null;
    }
    
    private void init() {
    	if (paint == null) {
	    	paint = new Paint();
	    	paint.setStyle(Paint.Style.FILL);
	    	paint.setAntiAlias(true);
    	}
    	
    	if ( (getHeight() != currentHeight) || (getWidth() != currentWidth) ) {
    		currentHeight = getHeight();
        	currentWidth = getWidth();
        	
        	float segment_width = (float) currentWidth / (float) ticker_weeks;
        	
        	left = new int[ticker_weeks];
        	right = new int[ticker_weeks];
        	
        	for (int i=0; i<ticker_weeks; i++) {
        		left[i] = (int) (segment_width * i) + BORDER;
        		right[i] = (int) (left[i] + segment_width) - (BORDER * 2);
        	}
        	
    	}
    }
    
    public static class TickerData {
    	public float[]    rating;
    	public int[] 	  gameweek;
    	public boolean[]  home;
    	public boolean[]  top;
    	public float 	  rating_high;
    	public float      rating_low;
    }
    
    private Integer[] sortOrder;
    private int[] matchesInGW;
    
    public void setData(TickerData t, int num_weeks, int start_week) {
    	data = t;
    	ticker_weeks = num_weeks;
    	start_gameweek = start_week;
    	
    	if (data == null) {
    		invalidate();
    		return;
    	}
    	
    	matchesInGW = new int[num_weeks];
    	for (int i=0; i<num_weeks; i++){
    		matchesInGW[i] = 0;
    	}
    	
    	sortOrder = new Integer[data.gameweek.length];
    	for (int i=0; i<sortOrder.length; i++){
            int gw = data.gameweek[i];
            int rel_gw = gw - start_week;
            sortOrder[i] = i;
	        if ( (rel_gw < num_weeks) && (rel_gw >= 0) ) {
	    	    matchesInGW[rel_gw]++;
            }
        }
    	Arrays.sort(sortOrder,new Comparator<Integer>() {   
            public int compare(Integer a, Integer b){
                return data.gameweek[a]-data.gameweek[b];
            }});
    	
    	invalidate();
    }
    
    public static TickerData parseTickerData(String in, float rating_high, float rating_low
    		, boolean cs, int valField, boolean hasTop) {
    	return parseTickerData(in, rating_high, rating_low, cs, valField, hasTop, false);
    }
    public static TickerData parseTickerData(String in, float rating_high, float rating_low
    		, boolean cs, int valField, boolean hasTop, boolean divideVal) {
    	TickerData data = new TickerData();
    	final String a = "a";
    	
    	if (in == null) return null;
    	if (in == "") return null;
    	
    	String[] split = in.split(",");
    	
    	//AppClass.log(in);
    	
    	data.rating = new float[split.length];
    	data.gameweek = new int[split.length];
    	data.home = new boolean[split.length];
    	if (hasTop) data.top = new boolean[split.length];
    	
    	for (int i=0; i<split.length; i++) {
    		String[] keyVal = split[i].split("=");
    		data.gameweek[i] = Integer.valueOf(keyVal[0]);
    		data.home[i] = (Integer.valueOf(keyVal[1]) == 1);
    		data.rating[i] = Float.valueOf(keyVal[1 + valField]);
    		if (divideVal) {
    			data.rating[i] /= 100f;
    		}
    		//if (invert) data.rating[i] = (rating_high - data.rating[i]) + rating_low;
    		if (cs) data.rating[i] = ProcessTeam.getCSPerc(data.rating[i]);
    		if (data.rating[i] < rating_low) data.rating[i] = rating_low;
    		if (data.rating[i] > rating_high) data.rating[i] = rating_high;
    		if (hasTop) data.top[i] = (keyVal[2 + valField].equals(a));
    	}
    	
    	data.rating_high = rating_high;
    	data.rating_low = rating_low;
    	
    	return data;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
    	if (data != null) {
    		init();
    		
    		int m = 0;
    		
    		int alpha = 165;
    		int red, green, blue;
    		float diff;
    		
    		float[] top = new float[max_gw_matches];
    		float[] bottom = new float[max_gw_matches];
    		
    		float half = data.rating_high - data.rating_low;
			half = data.rating_low + (half / 2);
			
			/*
			AppClass.log("-----");
			for (int i=0; i<sortOrder.length; i++) {
				AppClass.log("id: " + sortOrder[i] + " / gw: " + data.gameweek[sortOrder[i]] + " / rating: " + data.rating[sortOrder[i]] + " / home: " + data.home[sortOrder[i]]);
			}
			*/
    		
    		for (int i=0; i<ticker_weeks; i++) {
    			int gw = start_gameweek + i;
    			
    			if (matchesInGW[i] > 0) {
    				while( ((m+1) < data.gameweek.length) && (data.gameweek[sortOrder[m]] < gw) ) {
        				m++;
        			}
    				
    				// dgw handling - set top/bottoms
    				if (matchesInGW[i] == 1) {
    					top[0] = 0;
    					bottom[0] = currentHeight;
    				} else {
	    				float size = currentHeight / matchesInGW[i];
    					for (int q=0; q<matchesInGW[i]; q++) {
	    					top[q] = size * q;
	    					bottom[q] = size * (q + 1);
	    				}
    					bottom[0] = currentHeight;
    				}
    				
    				int y = 0;
    				// for all matches in gw
    				while (data.gameweek[sortOrder[m]] == gw) {
        				// derive colour
    					if (Settings.getBoolPref(Settings.PREF_ALTERNATE_TICKER_COLOURS)) {
    						float val = (data.rating[sortOrder[m]] - data.rating_low) / (data.rating_high - data.rating_low) * 255;
    						blue = (int) val;
    						red = blue;
    						green = blue;
    					} else {
	    					if (data.rating[sortOrder[m]] < half) {
	    	    				diff = data.rating[sortOrder[m]] / half;
	    	    				blue = 0;
    	    					red = 255;
	    	    				green = (int) (255 * diff);
	    	    			} else {
	    	    				diff = half - (data.rating[sortOrder[m]] - half); 
	    	    				blue = 0;
    	    					green = 255;
	    	    				red = (int) (255 * diff);
	    	    			}
    					}
    	    			
    	    			App.log("rating: " + data.rating[sortOrder[m]] + " / green: " + green + " / red: " + red);
    					
    					// slightly transparent thin border (for when above image etc)
    	    			paint.setColor(0xcc000000);
    	    			paint.setStrokeWidth(1);
    	    			paint.setShadowLayer(0, 0, 0, 0x00ffffff);
    	    			paint.setStyle(Style.STROKE);
    	    			canvas.drawRect(left[i], top[y], right[i], bottom[y], paint);
    	    			
    	    			// main colour fill
    	    			paint.setStyle(Style.FILL);
    	    			paint.setColor(Color.argb(alpha, red, green, blue));
    	    			canvas.drawRect(left[i]+1, top[y]+1, right[i]-1, bottom[y]-1, paint);
    	    			
    	    			// if 2nd or above match in gw, draw dgw line at top
    	    			if (y > 0) {
    	    				paint.setColor(0xaa222222);
    	    				paint.setStyle(Style.FILL);
        	    			paint.setStrokeWidth(DGW_LINE_WIDTH);
    	    				paint.setShadowLayer(0, 0, 0, 0x00ffffff);
    		    			canvas.drawLine(left[i], top[y]+1, right[i], top[y]+1, paint);
    	    			}
    	    			
    	    			// home/away draw line left/right
    	    			paint.setColor(0xffffffff);
    	    			paint.setStrokeWidth(HOME_AWAY_WIDTH);
    	    			paint.setShadowLayer(0, 0, 0, 0x00ffffff);
    	    			paint.setStyle(Style.STROKE);
    	    			int homeAwayX;
    	    			if (data.home[sortOrder[m]]) {
    	    				homeAwayX = left[i] + (HOME_AWAY_WIDTH/2);
    	    			} else {
    	    				homeAwayX = right[i] - (HOME_AWAY_WIDTH/2);
    	    			}
    	    			canvas.drawLine(homeAwayX, top[y]+1, homeAwayX, bottom[y]-1, paint);
    	    			
    	    			// rotation team indicator draw line top/bottom
    	    			if (data.top != null) {
	    	    			paint.setColor(0xff1034a6);
	    	    			paint.setStrokeWidth(HOME_AWAY_WIDTH + 2);
	    	    			paint.setShadowLayer(0, 0, 0, 0x00ffffff);
	    	    			paint.setStyle(Style.STROKE);
	    	    			int rotationY;
	    	    			if (data.top[sortOrder[m]]) {
	    	    				rotationY = (int) (top[y] + 1);
	    	    			} else {
	    	    				rotationY = (int) (bottom[y] - 1);
	    	    			}
	    	    			canvas.drawLine(left[i] + 1, rotationY, right[i] - 1, rotationY, paint);
    	    			}
    	    			
    	    			m++;
    	    			y++;
        				if (m >= data.gameweek.length) break;
        			}
    			} else {
    				// no matches
    				if (gw <= App.num_gameweeks) {
	    				paint.setColor(0xff000000);
	    				paint.setStyle(Style.FILL);
		    			paint.setTextSize(currentHeight);
	    				paint.setShadowLayer(2, 3, -3, 0xaaffffff);
		    			canvas.drawText("X", left[i]+3, currentHeight-2, paint);
	    				paint.setColor(0xffffffff);
	    				paint.setShadowLayer(0, 0, 0, 0x00ffffff);
		    			canvas.drawText("X", left[i]+1, currentHeight, paint);
    				}
    			}
    			
	    	} // outerloop
	    	
    	} // if data!=null
    } // sub

}
