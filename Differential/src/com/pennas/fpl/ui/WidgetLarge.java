package com.pennas.fpl.ui;

import android.appwidget.AppWidgetManager;
import android.content.Context;

public class WidgetLarge extends WidgetNormal {
	
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		onUpdateGeneric(context, appWidgetManager, appWidgetIds, TYPE_LARGE_4x4);
	}
  
}
