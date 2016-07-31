package com.pennas.fpl.util;

import java.util.HashMap;
import java.util.HashSet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapCache {
	private HashMap<Integer, Bitmap> id2img = new HashMap<Integer, Bitmap>();
	Context c;
	boolean reflection;
	GraphicUtil g;
	
	public BitmapCache(Context context) {
		this(context, false);
	}
		
	public BitmapCache(Context context, boolean p_reflection) {
		c = context;
		reflection = p_reflection;
		if (reflection) {
			g = new GraphicUtil(context, false);
		}
	}
	
	public Bitmap getBitmap(int resource) {
		Bitmap b = id2img.get(resource);
		if (b == null) {
			if (reflection) {
				b = g.getReflection(resource, null, false, null, true, false, false);
			} else {
				b = BitmapFactory.decodeResource(c.getResources(), resource);
			}
			id2img.put(resource, b);
		}
		return b;
	}
	
	public void clear() {
		id2img.clear();
	}
	
	public void refreshWith(HashSet<Integer> newPics) {
		HashSet<Integer> toRemove = new HashSet<Integer>();
		
		for (int i : id2img.keySet()) {
			if (!newPics.contains(i)) {
				toRemove.add(i);
			}
		}
		
		for (int i : toRemove) {
			id2img.remove(i);
		}
		
		for (int i : newPics) {
			getBitmap(i);
		}
	}
}
