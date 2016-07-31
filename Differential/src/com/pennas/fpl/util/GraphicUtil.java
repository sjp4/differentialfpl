package com.pennas.fpl.util;

import com.pennas.fpl.App;
import com.pennas.fpl.R;
import com.pennas.fpl.util.SquadUtil.SquadPlayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;

public class GraphicUtil {
	private Paint paint;
	private Paint strokePaint;
	private BitmapCache bmp;
	private Context c;
	private boolean cache;
	private float max_name_size, selection_stat_size, points_size, home_score_size, c_vc_size;
	
	public GraphicUtil(Context context) { this(context, true); }
	public GraphicUtil(Context context, boolean p_cache) {
		paint = new Paint();
		strokePaint = new Paint();
		
		paint.setStyle(Paint.Style.FILL);
		paint.setAntiAlias(true);
		paint.setTextAlign(Paint.Align.CENTER);
		
		strokePaint.setColor(Color.BLACK);
        strokePaint.setTextAlign(Paint.Align.CENTER);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(3);
        strokePaint.setAntiAlias(true);
        
        c = context;
        cache = p_cache;
        
        max_name_size = App.appContext.getResources().getDimension(R.dimen.max_name_size);
        selection_stat_size = App.appContext.getResources().getDimension(R.dimen.selection_stat_size);
        points_size = App.appContext.getResources().getDimension(R.dimen.points_size);
        home_score_size = App.appContext.getResources().getDimension(R.dimen.home_score_size);
        c_vc_size = App.appContext.getResources().getDimension(R.dimen.c_vc_size);
        
        if (cache) {
        	bmp = new BitmapCache(context);
        }
	}
	
	public void clearCache() {
		if (cache) {
			bmp.clear();
		}
	}
	
	
	public Bitmap getReflection(int resourceId, SquadPlayer p, boolean selection, boolean triple_captain) { return getReflection(resourceId, p, selection, null, false, false, triple_captain); }
	public Bitmap getReflection(int resourceId, SquadPlayer p, boolean selection, String score, boolean smaller, boolean live, boolean triple_captain) {
		//Get you bit map from drawable folder
        Bitmap originalImage;
        if (cache) {
        	originalImage = bmp.getBitmap(resourceId);
        } else {
        	originalImage = BitmapFactory.decodeResource(c.getResources(), resourceId);
        }
        
        if (originalImage == null) {
        	return null;
        }
        
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        
        if (p != null) {
        	originalImage = addOverlay(originalImage, p, selection);
        } else if ( (score != null) || (live) ) {
        	originalImage = addScoreLiveOverlay(originalImage, score, live);
        }
        
        //This will not scale but will flip on the Y axis
        Matrix matrix = new Matrix();
        matrix.preScale(1, -1);
        
        int reflectionHeight = height/2;
        if (smaller) reflectionHeight *= 0.75;
        
        //Create a Bitmap with the flip matix applied to it.
        //We only want the bottom half of the image
        Bitmap reflectionImage = Bitmap.createBitmap(originalImage, 0, (height - reflectionHeight), width, reflectionHeight, matrix, false);
        
        //Create a new bitmap with same width but taller to fit reflection
        Bitmap bitmapWithReflection = Bitmap.createBitmap(width
          , (height + reflectionHeight), Config.ARGB_8888);
        
        //Create a new Canvas with the bitmap that's big enough for
        //the image plus gap plus reflection
        Canvas canvas = new Canvas(bitmapWithReflection);
        //Draw in the original image
        canvas.drawBitmap(originalImage, 0, 0, null);
        //Draw in the gap
        Paint deafaultPaint = new Paint();
        canvas.drawRect(0, height, width, height, deafaultPaint);
        //Draw in the reflection
        canvas.drawBitmap(reflectionImage,0, height, null);
        
        //Create a shader that is a linear gradient that covers the reflection 72
        Paint paintGrad = new Paint();
        LinearGradient shader = new LinearGradient(0, originalImage.getHeight(), 0,
	     bitmapWithReflection.getHeight(), 0x92ffffff, 0x00ffffff,
	     TileMode.CLAMP);
        //Set the paint to use this shader (linear gradient)
        paintGrad.setShader(shader);
        //Set the Transfer mode to be porter duff and destination in
        paintGrad.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
        //Draw a rectangle using the paint with our linear gradient
        canvas.drawRect(0, height, width,
	     bitmapWithReflection.getHeight(), paintGrad);
        
        // add score overlay
        if (p != null ) {
        	addTextOverlay(canvas, bitmapWithReflection, p, selection, triple_captain);
        }
        
        return bitmapWithReflection;
    }
	
	private void addTextOverlay(Canvas canvas, Bitmap bitmap, SquadPlayer p, boolean selection, boolean triple_captain) {
		int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        int nameBottom = 4;
        
        float nameSize = max_name_size;
        paint.setColor(Color.WHITE);
        paint.setTextSize(nameSize);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setShadowLayer(0, 0, 0, Color.BLACK);
        paint.setTextScaleX(0.94f);
        
        while (!(paint.measureText(p.name) <= width)) {
        	nameSize--;
        	paint.setTextSize(nameSize);
        }
        
        canvas.drawText(p.name, width/2, height - nameBottom, paint);
        
        paint.setTextScaleX(1f);
        
        if ( (!selection && p.status_played) || selection ) {
	        paint.setColor(Color.WHITE);
			paint.setShadowLayer(3, 4, -4, Color.BLACK);
			paint.setTypeface(Typeface.DEFAULT);
			
			String text;
			float textsize;
			if (selection) {
				text = p.display_stat;
				textsize = selection_stat_size;
			} else {
				int p_score = p.gw_score;
				if (p.captain) {
					if (triple_captain) {
						p_score *= 3;
					} else {
						p_score *= 2;
					}
				}
				text = String.valueOf(p_score);
				textsize = points_size;
			}
			
			strokePaint.setTextSize(textsize);
	        paint.setTextSize(textsize);
			
	        if (text != null) {
				canvas.drawText(text, width/2, height - max_name_size - nameBottom, strokePaint);
				canvas.drawText(text, width/2, height - max_name_size - nameBottom, paint);
	        }
        } 
	}
	
	// for home screen
	private Bitmap addScoreLiveOverlay(Bitmap bitmap, String score, boolean live) {
		Bitmap newBmp = bitmap.copy(Config.ARGB_8888, true);
			
		Canvas canvas = new Canvas(newBmp);
		canvas.drawBitmap(bitmap, 0, 0, null);
		
		int height = newBmp.getHeight();
		int width = newBmp.getWidth();
		
		if (score != null) {
			paint.setTextSize(home_score_size);
			paint.setColor(Color.YELLOW);
			paint.setTypeface(Typeface.DEFAULT_BOLD);
			paint.setShadowLayer(3, -4, -4, Color.BLACK);
			paint.setTextAlign(Paint.Align.RIGHT);
			
			int x = (int) width - 2;
			int y = (int) (height * 0.92);
			
			canvas.drawText(score, x, y, paint);
			paint.setTextAlign(Paint.Align.CENTER);
		}
		
		if (live) {
			Bitmap status = bmp.getBitmap(R.drawable.icon_playing);
			
			int icon_width = (int) (width / 2.5);
			Rect r = new Rect(0, 0, icon_width, icon_width);
	        paint.setShadowLayer(0, 0, 0, Color.BLACK);
			canvas.drawBitmap(status, null, r, paint);
		}
		
		return newBmp;
	}
	
	private Bitmap addOverlay(Bitmap bitmap, SquadPlayer p, boolean selection) {
		if (p.captain || p.vice_captain || p.fpl_yellow_flag || p.fpl_red_flag || p.diff_flag || p.status_auto_subbed_in || p.status_auto_subbed_out
			|| p.status_gw_complete || p.status_playing_now || (p.status_gw_complete && !p.status_played) || p.status_gw_got_all_bonus ) {
			Bitmap newBmp = bitmap.copy(Config.ARGB_8888, true);
			
			Canvas canvas = new Canvas(newBmp);
			canvas.drawBitmap(bitmap, 0, 0, null);
			
			int height = newBmp.getHeight();
			int width = newBmp.getWidth();
			
			if (p.captain || p.vice_captain) {
				paint.setTextSize(c_vc_size);
				paint.setColor(Color.YELLOW);
				paint.setTypeface(Typeface.DEFAULT_BOLD);
				paint.setShadowLayer(3, -4, -4, Color.BLACK);
				paint.setTextAlign(Paint.Align.RIGHT);
				
				int x = (int) (width * 0.96);
				int y = (int) (height * 0.90);
				
				if (p.captain) {
					canvas.drawText("C", x, y, paint);
				} else {
					canvas.drawText("V", x, y, paint);
				}
				
				paint.setTextAlign(Paint.Align.CENTER);
			}
			
			if (p.fpl_yellow_flag || p.fpl_red_flag || p.diff_flag) {
				Bitmap flag;
				if (p.fpl_yellow_flag) {
					//flag = bmp.getBitmap(R.drawable.yellow_flag);
					flag = bmp.getBitmap(R.drawable.yellow_flag);
				} else if (p.fpl_red_flag) {
					flag = bmp.getBitmap(R.drawable.red_flag);
				} else {
					flag = bmp.getBitmap(R.drawable.diff_flag);
				}
				
				int top = (int) ((height/2) * 1.2);
		        Rect r = new Rect(0, top, width/2, height);
		        paint.setShadowLayer(0, 0, 0, Color.BLACK);
				canvas.drawBitmap(flag, null, r, paint);
			}
			
			int icon_width = (int) (width / 2.5);
			
			if (!selection && (p.status_auto_subbed_in || p.status_auto_subbed_out)) {
				Bitmap sub;
				if (p.status_auto_subbed_in) {
					sub = bmp.getBitmap(R.drawable.icon_up_green);
				} else {
					sub = bmp.getBitmap(R.drawable.icon_down_orange);
				}
				
				Rect r = new Rect(width - icon_width, 0, width, icon_width);
		        paint.setShadowLayer(0, 0, 0, Color.BLACK);
				canvas.drawBitmap(sub, null, r, paint);
			}
			
			if (!selection && (p.status_gw_complete || p.status_playing_now
					|| (p.status_gw_complete && !p.status_played) || p.status_gw_got_all_bonus)) {
				Bitmap status;
				if (p.status_gw_complete && !p.status_played) {
					status = bmp.getBitmap(R.drawable.icon_x);
				} else if (p.status_gw_got_all_bonus) {
					status = bmp.getBitmap(R.drawable.icon_bonus);
				} else if (p.status_gw_complete) {
					status = bmp.getBitmap(R.drawable.icon_tick);
				} else {
					status = bmp.getBitmap(R.drawable.icon_playing);
				}
				
				Rect r = new Rect(0, 0, icon_width, icon_width);
		        paint.setShadowLayer(0, 0, 0, Color.BLACK);
				canvas.drawBitmap(status, null, r, paint);
			}
			
			return newBmp;
		} else {
			return bitmap;
		}
	}
	
}
