package com.pennas.fpl.ui;

import android.appwidget.AppWidgetManager;
import android.content.Context;

public class WidgetSmall extends WidgetNormal {
	
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		onUpdateGeneric(context, appWidgetManager, appWidgetIds, TYPE_SMALL_4x1);
	}
  
}
