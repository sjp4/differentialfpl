package com.pennas.fpl.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.pennas.fpl.App;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class Screenshot {
	
	// suppress warning about getExternalFilesDir method (appropriate checks in place below)
	@SuppressLint("NewApi")
	private static File get_folder(Context c) {
    	File path;
		if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
			path = new File(Environment.getExternalStorageDirectory() + "/DifferentialFPL");
			path.mkdirs();
		} else {
			path = c.getExternalFilesDir(null);
		}
		return path;
    }
	
	private static final int MAX_LISTVIEW_ITEMS_DEFAULT = 15;
	public static final int NOT_SET = -1;
	
	private static Bitmap take_listview_screenshot(ListView lv, int max_items) throws OutOfMemoryError {
		Bitmap bigbitmap = null;
		int max = MAX_LISTVIEW_ITEMS_DEFAULT;
		if (max_items != NOT_SET) {
			max = max_items;
		}
		App.log("take_listview_screenshot max = " + max);
		try {
			ListAdapter adapter   = lv.getAdapter(); 
		    int itemscount        = adapter.getCount();
		    int allitemsheight    = 0;
		    List<View> childViews = new ArrayList<View>();
		    
		    int visibleChildCount = (lv.getLastVisiblePosition() - lv.getFirstVisiblePosition()) + 1;
		    
		    int start;
		    int end;
		    if (itemscount > max) {
		    	if (visibleChildCount > max) {
		    		// can't really process individually, so use standard view screenshot mechanism
		    		App.log("visibleChildCount > MAX_LISTVIEW_ITEMS; using standard screenshot mechanism");
		    		return null;
		    	} else {
		    		// use some items
		    		int spare = max - visibleChildCount;
		    		boolean ok = true;
		    		start = lv.getFirstVisiblePosition();
		    		end = lv.getLastVisiblePosition();
		    		while ( (spare > 0) && ok) {
		    			ok = false;
		    			if ( (start > 0) && (spare > 0) ) {
		    				spare--;
		    				start--;
		    				ok = true;
		    			}
		    			if ( (end < (itemscount-1)) && (spare > 0) ) {
		    				spare--;
		    				end++;
		    				ok = true;
		    			}
		    		}
		    		App.log("use " + start + " -> " + end + " (visible: " + lv.getFirstVisiblePosition() + "->" + lv.getLastVisiblePosition() + ")");
		    	}
		    } else {
		    	App.log("screenshot listview: use all child items");
		    	// use all items
		    	start = 0;
		    	end = itemscount - 1;
		    }
		    
		    for (int i=start; i<=end; i++) {
		        View childView = adapter.getView(i, null, lv);
		        childView.measure(MeasureSpec.makeMeasureSpec(lv.getWidth(), MeasureSpec.EXACTLY), 
		                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		        childView.layout(0, 0, childView.getMeasuredWidth(), childView.getMeasuredHeight());
		        childViews.add(childView);
		        allitemsheight += childView.getMeasuredHeight();
		    }
		    
		    bigbitmap = Bitmap.createBitmap(lv.getMeasuredWidth(), allitemsheight + TEXT_SPACE, Bitmap.Config.ARGB_8888);
		    Canvas bigcanvas = new Canvas(bigbitmap);
		    Paint paint = new Paint();
		    int iHeight = 0;
		    
		    for (View childView : childViews) {
		    	Bitmap sc = take_screenshot(childView, NOT_SET, true);
		        bigcanvas.drawBitmap(sc, 0, iHeight, paint);
		        iHeight += sc.getHeight();
		        sc.recycle();
		    }
		    
		    return bigbitmap;
		} catch (OutOfMemoryError e) {
			//App.exception(e);
			App.log("failed creating full listview bmp - out of memory");
			if (bigbitmap != null) {
				bigbitmap.recycle();
			}
		} catch (NullPointerException e) {
			//App.exception(e);
			App.log("failed creating full listview bmp - NPE");
			if (bigbitmap != null) {
				bigbitmap.recycle();
			}
		}
		
		return null;
	}
	
	/*
	private static Bitmap take_screenshot(View v) {
		v.setDrawingCacheEnabled(true);
		v.setDrawingCacheBackgroundColor(0xff000000);
	    Bitmap screenshot = v.getDrawingCache();
	    Bitmap bitmap = null;
	    if (screenshot != null) {
		    bitmap = Bitmap.createBitmap(screenshot);
	    }
	    v.setDrawingCacheBackgroundColor(0);
	    v.destroyDrawingCache();
	    v.setDrawingCacheEnabled(false);
	    return bitmap;
	}
	*/
	
	private static Bitmap take_screenshot(View v, int max_listview, boolean recursive) {
		if (v instanceof ListView) {
			App.log("creating ful listview bitmap for screenshot");
			Bitmap b = take_listview_screenshot((ListView) v, max_listview);
			if (b != null) {
				return b;
			}
		} 
		int width = v.getMeasuredWidth();
		int height = v.getMeasuredHeight();
		if (!recursive) {
			height += TEXT_SPACE;
		}
		Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);                
		Canvas c = new Canvas(b);
		v.draw(c);
		return b;
	}
	
	public static void screenshot(Activity a, View v, String s, int max_listview) {
		App.log("take_screenshot: " + a.getClass().getName() + " / " + s);
		Bitmap bitmap = take_screenshot(v, max_listview, false);
	    if (bitmap != null) {
		    File path = get_folder(a);
			File file = new File(path + "/differential_fpl_" + App.currentUTCTime() + ".jpg");
			FileOutputStream fos = null;
		    try {
		        add_text(bitmap);
		    	
		    	fos = new FileOutputStream(file);
		        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, fos);
	            fos.flush();
	            fos.close();
	            
	            Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
	    		//emailIntent.setType("text/html");
	            //emailIntent.setType("plain/text");
	    		emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Fantasy Premier League");  
	    		emailIntent.putExtra(Intent.EXTRA_TEXT, s + "\n\nSent from Differential FPL for Android - https://play.google.com/store/apps/details?id=com.pennas.fpl");  
	    		emailIntent.setType("image/jpeg");
	    		emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+file.getAbsolutePath()));
	    		a.startActivity(Intent.createChooser(emailIntent, "Share..."));
	    		
	    		return;
		    } catch (FileNotFoundException e) {
		    	App.exception(e);
		    } catch (IOException e) {
				App.exception(e);
			}
	    } else {
	    	App.log("screenshot null");
	    }
	    
	    // failed
	    Toast.makeText(a, "Image creation failed; please try again later", Toast.LENGTH_LONG).show();
	}
	
	private static final int TEXT_SIZE = 18;
	private static final int TEXT_SPACE = TEXT_SIZE + 4;
	
	private static void add_text(Bitmap b) {
		Canvas c = new Canvas(b);
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.FILL);
		paint.setAntiAlias(true);
		paint.setTextAlign(Paint.Align.RIGHT);
		paint.setColor(Color.WHITE);
        paint.setTextSize(TEXT_SIZE);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setShadowLayer(3, 4, -4, Color.BLACK);
        
        c.drawText("Sent from Differential FPL for Android", b.getWidth() - 2, b.getHeight() - 2, paint);
	}
	
	public static void cleanup_screenshots() {
		new Thread(new Runnable() {
	        public void run() {
	        	File path = get_folder(App.appContext);
	        	if (path != null){ //check if dir is not null
	        		File[] files = path.listFiles();
	        		// extra null check, which happens if it's not a valid folder
	        		if (files != null) {
		        		for (File tmpf : files){ 
		        			tmpf.delete();
		        		}
	        		}
	        	}
	        }
	    }).start();
	}
}
