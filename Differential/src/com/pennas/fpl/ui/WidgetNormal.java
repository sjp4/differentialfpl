package com.pennas.fpl.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import com.pennas.fpl.App;
import com.pennas.fpl.Home;
import com.pennas.fpl.Leagues;
import com.pennas.fpl.R;
import com.pennas.fpl.Selection;
import com.pennas.fpl.Settings;
import com.pennas.fpl.Vidiprinter;
import com.pennas.fpl.Vidiprinter.VidiEntry;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.SparseArray;
import android.view.View;
import android.widget.RemoteViews;

public class WidgetNormal extends AppWidgetProvider {
	private static final String ACTION_WIDGET_RECEIVER = "ActionRecieverWidget";  
	private static final String ID = "com.pennas.fpl.id";  
	//private static final String CAT_SYNC = "sync";  
	private static final String CAT_HOME = "home";  
	private static final String CAT_WIDGET = "widget";  
	protected static final int NONE = -1;
    
	private static PendingIntent intent_points;
	private static PendingIntent intent_vidi;
	private static PendingIntent intent_home;
	//private static PendingIntent intent_sync;
	private static LinkedList<VidiEntry> vidi_entries = new LinkedList<VidiEntry>();
	private static LinkedList<VidiEntry> my_vidi_entries = new LinkedList<VidiEntry>();
	
	public static final int MAX_VIDI_ENTRIES = 11;
	public static final int MAX_VIDI_ENTRIES_NORMAL = 5;
	public static final int MAX_VIDI_ENTRIES_SMALL = 2;
	
	protected static final int TYPE_LARGE_4x4 = 0;
	private static final int TYPE_NORMAL_4x2 = 1;
	protected static final int TYPE_SMALL_4x1 = 2;
	private static final String[] type_desc = { "Large 4x4", "Normal 4x2", "Small 4x1" };
	private static final int[] type_max_entries = { MAX_VIDI_ENTRIES, MAX_VIDI_ENTRIES_NORMAL, MAX_VIDI_ENTRIES_SMALL };
	private static final int[] type_layout = { R.layout.widget_large, R.layout.widget, R.layout.widget_small };
	
	private static SparseArray<Bitmap> bitmaps = new SparseArray<Bitmap>();
	private static HashMap<Integer, WidgetMap> widgets = new HashMap<Integer, WidgetMap>();
	private static HashSet<Integer> widgets_mode_my;
	
	private static final int[] vidi_datetime = { R.id.datetime1, R.id.datetime2, R.id.datetime3, R.id.datetime4, 
		R.id.datetime5, R.id.datetime6, R.id.datetime7, R.id.datetime8, R.id.datetime9, R.id.datetime10, 
		R.id.datetime11 };
	private static final int[] vidi_gameweek = { R.id.gameweek1, R.id.gameweek2, R.id.gameweek3, R.id.gameweek4, 
		R.id.gameweek5, R.id.gameweek6, R.id.gameweek7, R.id.gameweek8, R.id.gameweek9, R.id.gameweek10, 
		R.id.gameweek11 };
	private static final int[] vidi_message = { R.id.message1, R.id.message2, R.id.message3, R.id.message4, 
		R.id.message5, R.id.message6, R.id.message7, R.id.message8, R.id.message9, R.id.message10, 
		R.id.message11 };
	private static final int[] vidi_cat_background = { R.id.cat_background1, R.id.cat_background2, R.id.cat_background3, R.id.cat_background4, 
		R.id.cat_background5, R.id.cat_background6, R.id.cat_background7, R.id.cat_background8, R.id.cat_background9, R.id.cat_background10, 
		R.id.cat_background11 };
	private static final int[] vidi_cat = { R.id.cat1, R.id.cat2, R.id.cat3, R.id.cat4, 
		R.id.cat5, R.id.cat6, R.id.cat7, R.id.cat8, R.id.cat9, R.id.cat10, 
		R.id.cat11 };
	
	private static class WidgetMap {
		RemoteViews main;
		int awid;
		AppWidgetManager awm;
		boolean myVidi;
		boolean update;
		boolean vidi_update;
		int type;
		//boolean animating;
	}
	
	private static boolean initialised = false;
	
	// onUpdate broadcast isn't recieved after a restart of the com.pennas.fpl process.
	// ...force an update of the widget IDs
	public static void forceInit(Context c) {
		if (initialised) {
			return;
		}
		App.log("manually init app widget manager..");
		AppWidgetManager manager = AppWidgetManager.getInstance(c);
		
		// 4x2
		ComponentName provider = new ComponentName(c, WidgetNormal.class);
		int[] ids = manager.getAppWidgetIds(provider);
		if (ids.length > 0) {
			onUpdateGeneric(c, manager, ids, TYPE_NORMAL_4x2);
		}
		
		// 4x4
		provider = new ComponentName(c, WidgetLarge.class);
		ids = manager.getAppWidgetIds(provider);
		if (ids.length > 0) {
			onUpdateGeneric(c, manager, ids, TYPE_LARGE_4x4);
		}
		
		// 4x1
		provider = new ComponentName(c, WidgetSmall.class);
		ids = manager.getAppWidgetIds(provider);
		if (ids.length > 0) {
			onUpdateGeneric(c, manager, ids, TYPE_SMALL_4x1);
		}
	}
	
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		onUpdateGeneric(context, appWidgetManager, appWidgetIds, TYPE_NORMAL_4x2);
	}
	
	public void onDeleted (Context context, int[] appWidgetIds) {
		for (int id : appWidgetIds) {
			App.log("deleting id: " + id);
			widgets.remove(id);
			
			if (widgets_mode_my != null) {
    			widgets_mode_my.remove(id);
    			Settings.setIntSetPref(Settings.PREF_WIDGETS_MODE_MY, widgets_mode_my, context);
    		}
		}
	}
	
	protected static void onUpdateGeneric(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, int type) {
		App.log("widget onUpdateGeneric()");
		initialised = true;
		
		// check whether each widget already mapped
		if (widgets_mode_my == null) {
			widgets_mode_my = Settings.getIntSetPref(Settings.PREF_WIDGETS_MODE_MY);
		}
		
		for (int id : appWidgetIds) {
			WidgetMap m = widgets.get(id);
			if (m == null) {
				App.log("adding widget id: " + id + " type: " + type_desc[type]);
				m = new WidgetMap();
				m.awid = id;
				m.awm = appWidgetManager;
				// use settings to load widget type state
				if (widgets_mode_my != null) {
					m.myVidi = widgets_mode_my.contains(m.awid);
				}
				m.type = type;
				m.update = true;
				m.vidi_update = true;
				widgets.put(id, m);
			} else {
				// send to us for a reason. update
				m.update = true;
				m.vidi_update = true;
			}
		}
		updateWidgets(context);
	}
	
	private static void createIntents(Context c) {
		if (intent_points == null) {
		    // Create an Intent to launch my squad score
		    Intent intent = new Intent(c, Selection.class);
	    	intent.putExtra(Selection.P_SELECTION, false);
	    	intent.putExtra(Selection.P_SQUADID, App.my_squad_id);
	    	intent.putExtra(Selection.P_GAMEWEEK, Selection.CURRENT_GW);
	    	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    	intent.addCategory(CAT_WIDGET);
	        intent_points = PendingIntent.getActivity(c, 0, intent, 0);
	    }
	    
	    if (intent_vidi == null) {
	    	// Create an Intent to launch vidiprinter
		    Intent vidiIntent = new Intent(c, Vidiprinter.class);
		    vidiIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    vidiIntent.addCategory(CAT_WIDGET);
	        intent_vidi = PendingIntent.getActivity(c, 0, vidiIntent, 0);
	    }
	    	
	    if (intent_home == null) {
	    	// Create an Intent to launch vidiprinter
		    Intent homeIntent = new Intent(c, Home.class);
		    homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    // set category so that android does not merge this intent with sync
		    // (and use the wrong one)
		    homeIntent.addCategory(CAT_HOME);
	        intent_home = PendingIntent.getActivity(c, 0, homeIntent, 0);
	    }
	    
	    /*
	    if (intent_sync == null) {
	    	// Create an Intent to launch vidiprinter
		    Intent intentSync = new Intent(c, Home.class);
		    intentSync.putExtra(Home.P_SHOW_SYNC, true);
		    // set category so that android does not merge this intent with home
		    // (and use the wrong one)
		    intentSync.addCategory(CAT_SYNC);
	        intent_sync = PendingIntent.getActivity(c, 0, intentSync, 0);
	    }
	    */
	}
	
	private static final StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
	
	public static void updateWidgets(Context c) {
    	App.log("Widgets updating.. ");
    	createIntents(c);
    	// Perform this loop procedure for each App Widget that belongs to this
		// provider
    	for (WidgetMap map : widgets.values()) {
			if (map.update) {
				App.log(".. updating " + map.awid + " type: " + type_desc[map.type] + " myVidi: " + map.myVidi);
				
			    // Get the layout for the App Widget and attach an on-click listener
			    // to the button
			    if (map.main == null) {
			    	map.main = new RemoteViews(c.getPackageName(), type_layout[map.type]);
			    	map.main.setOnClickPendingIntent(R.id.widgetlayout, intent_vidi);
			    	map.main.setOnClickPendingIntent(R.id.widget_infobar, intent_points);
			    	map.main.setOnClickPendingIntent(R.id.widget_gw_score, intent_points);
			    	map.main.setOnClickPendingIntent(R.id.widget_logo, intent_home);
			    	map.main.setOnClickPendingIntent(R.id.widget_title, intent_home);
			    	
			    	Intent intent = new Intent(c, WidgetNormal.class);
			        intent.setAction(ACTION_WIDGET_RECEIVER);
			        intent.putExtra(ID, map.awid);
			        // add category to make this intent unique to this widget
			        // (otherwise android will simply use an intent for a previous widget)
			        intent.addCategory(String.valueOf(map.awid));
			        PendingIntent intent_switch = PendingIntent.getBroadcast(c, 0, intent, 0);
			        
			        map.main.setOnClickPendingIntent(R.id.widget_switch, intent_switch);
					//map.main.setOnClickPendingIntent(R.id.widget_sync_img, intent_sync);
			    }
			    
			    // title
			    String title;
			    if (map.myVidi) {
			    	title = "Differential FPL: My Squad";
			    } else {
			    	title = "Differential FPL: Vidiprinter";
			    }
			    map.main.setTextViewText(R.id.widget_title, title);
			    
			    // gw points
			    map.main.setTextViewText(R.id.widget_gw_score, App.gw_score.points_string);
			    map.main.setTextViewText(R.id.widget_goals, String.valueOf(App.gw_score.goals));
			    map.main.setTextViewText(R.id.widget_assists, String.valueOf(App.gw_score.assists));
			    map.main.setTextViewText(R.id.widget_cs, String.valueOf(App.gw_score.clean_sheets));
			    map.main.setTextViewText(R.id.widget_bonus, String.valueOf(App.gw_score.bonus));
			    
			    if (App.gw_score.complete) {
			    	map.main.setViewVisibility(R.id.widget_complete_img, View.VISIBLE);
				} else {
					map.main.setViewVisibility(R.id.widget_complete_img, View.GONE);
				}
			    
			    boolean capt_playing = false;
			    int playing = App.gw_score.p_playing;
				if (playing < 0) {
					capt_playing = true;
					playing = -playing;
				}
				if (playing > 0) {
					map.main.setViewVisibility(R.id.widget_playing, View.VISIBLE);
					map.main.setViewVisibility(R.id.widget_playing_img, View.VISIBLE);
					String capt_string;
					if (capt_playing) {
						capt_string = Leagues.CAPT_STRING_C;
					} else {
						capt_string = Leagues.CAPT_STRING_BLANK;
					}
					map.main.setTextViewText(R.id.widget_playing, playing + capt_string);
				} else {
					map.main.setViewVisibility(R.id.widget_playing, View.GONE);
					map.main.setViewVisibility(R.id.widget_playing_img, View.GONE);
				}
					
				boolean capt_to_play = false;
				int to_play = App.gw_score.p_to_play;
				if (to_play < 0) {
					capt_to_play = true;
					to_play = -to_play;
				}
				if (to_play > 0) {
					map.main.setViewVisibility(R.id.widget_to_play, View.VISIBLE);
					map.main.setViewVisibility(R.id.widget_to_play_img, View.VISIBLE);
					String capt_string;
					if (capt_to_play) {
						capt_string = Leagues.CAPT_STRING_C;
					} else {
						capt_string = Leagues.CAPT_STRING_BLANK;
					}
					map.main.setTextViewText(R.id.widget_to_play, to_play + capt_string);
				} else {
					map.main.setViewVisibility(R.id.widget_to_play, View.GONE);
					map.main.setViewVisibility(R.id.widget_to_play_img, View.GONE);
				}
					
				if (map.vidi_update) {
				    App.log(".. and vidi");
				    
					// wipe vidiprinter items
					LinkedList<VidiEntry> vidi_to_use;
					if (map.myVidi) {
						vidi_to_use = my_vidi_entries;
					} else {
						vidi_to_use = vidi_entries;
					}
					int vidi_i = 0;
					int max_vidi_for_type = type_max_entries[map.type];
					synchronized(vidi_to_use) {
					    for (VidiEntry e : vidi_to_use) {
					    	// limit per type
					    	if ( (e != null) && (vidi_i < max_vidi_for_type) ) {
					    		long localdate = App.utcToLocal(e.datetime);
					    		map.main.setTextViewText(vidi_datetime[vidi_i], App.printDate(localdate));
					    		// can't set background gradient in 2.1.... so use bold
					    		if (!map.myVidi && (App.mySquadCur.contains(e.player_fpl_id)) ) {
					    			SpannableString s = new SpannableString(e.message);
					                s.setSpan(styleSpan, 0, e.message.length(), 0);
					                map.main.setTextViewText(vidi_message[vidi_i], s);
					    		} else {
					    			map.main.setTextViewText(vidi_message[vidi_i], e.message);
					    		}
					    		map.main.setTextViewText(vidi_gameweek[vidi_i], String.valueOf(e.gameweek));
					    		map.main.setTextViewText(vidi_cat[vidi_i], Vidiprinter.cat_labels[e.category]);
					    		map.main.setTextColor(vidi_cat[vidi_i], Vidiprinter.cat_fg[e.category]);
					    		
					    		// hack to be able to set background colour pre-2.2.
					    		// have to create bitmap and assign it to imageview behind textview.
					    		//     .. lame
					    		int colour = Vidiprinter.cat_bg[e.category];
					    		Bitmap bitmap = bitmaps.get(colour);
					    		if (bitmap == null) {
						    		bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Create a Bitmap
						            bitmap.setPixel(0, 0, colour);
						            bitmaps.put(colour, bitmap);
					    		}
					            
					    		map.main.setImageViewBitmap(vidi_cat_background[vidi_i], bitmap);
					    		
					    		vidi_i++;
					    	} // != null
					    } // loop vidiprinter entries
					    
					    // wipe entries owith no item if required
					    if (vidi_i < max_vidi_for_type) {
					    	for (; vidi_i<max_vidi_for_type; vidi_i++) {
					    		map.main.setTextViewText(vidi_datetime[vidi_i], "");
					    		map.main.setTextViewText(vidi_message[vidi_i], "");
					    		map.main.setTextViewText(vidi_cat[vidi_i],"");
					    	}
					    }
					} // synchronised
				} // if new/vidiprinter updated
				    
			    // Tell the AppWidgetManager to perform an update on the current app widget
			    map.awm.updateAppWidget(map.awid, map.main);
			    
			    // set this widget as updated
			    map.update = false;
			    map.vidi_update = false;
			} // if updated
		} // loop widgets
    }
	
	@Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_WIDGET_RECEIVER)) {
            int id = intent.getIntExtra(ID, -1);
            if (id >= 0) {
            	WidgetMap map = widgets.get(id);
            	if (map != null) {
            		if (widgets_mode_my != null) {
            			if (map.myVidi) {
            				widgets_mode_my.remove(id);
            			} else {
            				widgets_mode_my.add(id);
            			}
            			Settings.setIntSetPref(Settings.PREF_WIDGETS_MODE_MY, widgets_mode_my, context);
            		}
            		map.myVidi = !map.myVidi;
            		map.update = true;
            		map.vidi_update = true;
            		App.log("flipping switch id: " + id + " - now: " + map.myVidi);
            		updateWidgets(context);
            	} else {
            		App.log("ERROR: Couldn't find map for widget " + id + " for flip");
            	}
            }
        }
        super.onReceive(context, intent);
    }
	
	private static void setUpdates(boolean vidi, boolean myVidi) {
		for (WidgetMap map : widgets.values()) {
			map.update = true;
			if (map.myVidi && myVidi) {
				map.vidi_update = true;
			}
			if (!map.myVidi && vidi) {
				map.vidi_update = true;
			}
		}
	}
	
	public static void addVidiEntry(VidiEntry e) {
		synchronized (vidi_entries) {
			if (vidi_entries.size() == MAX_VIDI_ENTRIES) {
				vidi_entries.removeLast();
			}
			vidi_entries.addFirst(e);
		}
		setUpdates(true, false);
		App.log("widget update: vidiprinter");
	}
	
	public static void addMyVidiEntry(VidiEntry e) {
		synchronized (my_vidi_entries) {
			if (my_vidi_entries.size() == MAX_VIDI_ENTRIES) {
				my_vidi_entries.removeLast();
			}
			my_vidi_entries.addFirst(e);
		}
		setUpdates(false, true);
		App.log("widget update: my vidiprinter");
	}
	
	public static void setSquadScore() {
		//App.gw_score.points_string = App.gw_score.points + "-" + App.gw_score.points_hit;
		App.gw_score.points_string = String.valueOf(App.gw_score.points);
		setUpdates(false, false);
			
		App.log("widget update: points");
	}
  
}
