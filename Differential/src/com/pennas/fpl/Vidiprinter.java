package com.pennas.fpl;

import com.pennas.fpl.ui.WidgetNormal;
import com.pennas.fpl.util.FPLActivity;
import com.pennas.fpl.util.DbGen;
import com.pennas.fpl.util.Refreshable;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class Vidiprinter extends FPLActivity implements Refreshable {
	// max number of entries to display
	public static final int limit = 100;
	
	private MyViewBinder binder = new MyViewBinder(true);
	
	private Cursor curVidi;
	private ListView list;
	
	private boolean my_squad = false;
	
	// entry categories
	private static final int CAT_ALL = 0;
	public static final int CAT_GOAL = 1;
	public static final int CAT_ASS = 2;
	public static final int CAT_PENSAV = 3;
	public static final int CAT_PENMIS = 4;
	public static final int CAT_RED = 5;
	public static final int CAT_OG = 6;
	public static final int CAT_BON = 7;
	public static final int CAT_PRICE_CHANGE = 8;
	public static final int CAT_TRANSFER = 9;
	public static final int CAT_NEW_PLAYER = 10;
	public static final int CAT_CLUB_CHANGE = 11;
	public static final int CAT_PLAYER_NEWS = 12;
	public static final int CAT_FINAL_WHISTLE = 13;
	public static final int CAT_KICK_OFF = 14;
	public static final int CAT_FIXTURE_MOVED = 15;
	public static final int CAT_DEADLINE_MOVED = 16;
	
	public static final String[] cat_labels = { "All", "Goal", "Assist", "Penalty Save", "Penalty Miss", "Red Card"
		, "Own Goal", "Bonus", "Price Change", "Transfer", "New Player", "Club Change", "Player News", "Final Whistle"
		, "Kick Off", "Fixture Moved", "Deadline Moved" };
	public static final int[] cat_bg = { 0x77000000, 0x77CC6600, 0x7700ff00, 0x776600CC, 0x77ffff00, 0x77ff0000
		, 0x7766FFFF, 0x77FFCC00, 0x77666699, 0x77FF6699, 0x77FFFF66, 0x7700CC33, 0x77FFFFFF, 0x773333FF
		, 0x7733FF99, 0x776622BB, 0x77339900 };
	public static final int[] cat_fg = { 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff
		, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xff000000, 0xffffffff
		, 0xffffffff, 0xffffffff, 0xffffffff };
	
	private int show_cat = CAT_ALL;
	
	public static final int FILTER_TYPE = 1;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    setContentView(R.layout.vidiprinter_screen);
	    
	    list = (ListView) findViewById(android.R.id.list);
	    
	    actionBar.setTitle("Vidiprinter");
	    
	    addFilter(FILTER_TYPE, "Filter", cat_labels, 0, true, true);
	    
	    popList();
	}
	
	@Override
	protected void filterSelection(int id, int pos) {
		App.log("subclass");
    	
    	switch(id) {
	    	case (FILTER_TYPE):
	    		if (show_cat != pos) {
	        		show_cat = pos;
	        		popList();
	        	}
				break;
    	}
	}
	
	// load data and populate listview
	private void popList() {
		App.log("popList()  (Vidiprinter)");
		
		if (curVidi != null) {
			curVidi.close();
		}
		
		if (my_squad) {
			String where_addition = "";
			if (show_cat != CAT_ALL) {
				where_addition = " AND category = " + show_cat;
			}
			curVidi = App.dbGen.rawQuery("SELECT v.player_fpl_id _id, v.gameweek, v.datetime, v.message, v.category"
					+ " FROM vidiprinter v, squad_gameweek_player sgp WHERE sgp.squad_id = " + App.my_squad_id
					+ " AND v.gameweek = sgp.gameweek AND v.player_fpl_id = sgp.fpl_player_id"
					+ where_addition
					+ " ORDER BY v.gameweek DESC, v.datetime DESC LIMIT " + Vidiprinter.limit, null);
		} else {
			String where_clause = "";
			if (show_cat != CAT_ALL) {
				where_clause = " WHERE category = " + show_cat;
			}
			curVidi = App.dbGen.rawQuery("SELECT player_fpl_id _id, gameweek, datetime, message, category"
					+ " FROM vidiprinter"
					+ where_clause + " ORDER BY gameweek DESC, datetime DESC LIMIT " + limit, null);
		}
		
		// the desired columns to be bound
		String[] columns = new String[] { "datetime", "message", "gameweek", "category" };
		// the XML defined views which the data will be bound to
		int[] to = new int[] { R.id.datetime, R.id.message, R.id.gameweek, R.id.cat };
		
		if (my_squad) {
			binder.setHighlight(false);
		} else {
			binder.setHighlight(true);
		}
		
		// create the adapter using the cursor pointing to the desired data as well as the layout information
		SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(this, R.layout.vidiprinter_screen_item, curVidi, columns, to);
		mAdapter.setViewBinder(binder);
		
		// set this adapter as your ListActivity's adapter
		list.setAdapter(mAdapter);
		
		OnItemClickListener playersListener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
				vidiClickAction(id, view.getContext());
			}
	    };
	    list.setOnItemClickListener(playersListener);
	    
	    setScreenshotView(list, "Vidiprinter, filter: " + cat_labels[show_cat]);
	}
	
	public static void vidiClickAction(long id, Context c) {
		// load player screen for player "id"
		if (id > 0) {
			Intent intent = new Intent(c, Player.class);
			Cursor cur = App.dbGen.rawQuery("SELECT player_id FROM player_season WHERE season = " + App.season
					+ " AND fpl_id = " + id, null);
			cur.moveToFirst();
			int player_id = cur.getInt(0);
			cur.close();
			intent.putExtra(Player.P_PLAYERID, (int) player_id);
			intent.putExtra(Player.P_SEASON, App.season);
	    	c.startActivity(intent);
		} else if (id < 0) {
			Intent intent = new Intent(c, Fixture.class);
			int fix_id = (int) (0 - id);
			App.log("fix id: " + fix_id);
			intent.putExtra(Fixture.P_FIXTURE_ID, fix_id);
			c.startActivity(intent);
		}
	}
	
	// bind cursor to main list row item
    public static class MyViewBinder implements SimpleCursorAdapter.ViewBinder {
    	private boolean highlightPlayers;
    	
    	public MyViewBinder(boolean highlight) {
    		highlightPlayers = highlight;
    	}
    	
    	public void setHighlight(boolean highlight) {
    		highlightPlayers = highlight;
    	}
    	
    	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            int viewId = view.getId();
            TextView t = (TextView) view;
            if (viewId == R.id.datetime) {
            	long localdate = App.utcToLocal(cursor.getLong(columnIndex));
            	t.setText(App.printDate(localdate));
            	
            	if (highlightPlayers) {
	            	int fpl_id = cursor.getInt(0);
	            	RelativeLayout rl = (RelativeLayout) t.getParent();
	        		if (App.mySquadCur.contains(fpl_id)) {
	            		rl.setBackgroundResource(R.drawable.list_item_gradient_green);
	            	} else {
	            		rl.setBackgroundResource(R.drawable.list_item_gradient);
	            	}
            	}
            } else if (viewId == R.id.message) {
            	t.setText(DbGen.decompress(cursor.getBlob(columnIndex)));
            } else if (viewId == R.id.cat) {
            	int cat = cursor.getInt(columnIndex);
            	t.setText(cat_labels[cat]);
            	t.setBackgroundColor(cat_bg[cat]);
            	t.setTextColor(cat_fg[cat]);
            } else {
            	t.setText(cursor.getString(columnIndex));
            }
            
            return true;
        }
    }
    
    // callback
    public void dataChanged() {
    	popList();
    }

    // data type for entry
    public static class VidiEntry {
    	public String message;
    	public int gameweek;
    	public int player_fpl_id;
    	public int category;
    	// datetime is only for the widget
    	public long datetime;
    }
    
    private static void notification(VidiEntry e, String pref, int icon, String title, String text, Context c) {
    	boolean notif = false;
    	
    	if (e.category == CAT_GOAL || e.category == CAT_ASS || e.category == CAT_RED) {
    		int setting = Settings.getIntPref(pref);
    		if (setting == Settings.NOTIF_MY_TEAM) {
        		notif = App.mySquadCur.contains(e.player_fpl_id);
        	} else if (setting == Settings.NOTIF_MY_TEAM_RIVALS) {
        		notif = App.mySquadCur.contains(e.player_fpl_id);
        		if (!notif) {
        			Cursor riv = App.dbGen.rawQuery("SELECT 1 FROM squad_gameweek_player sgp, squad s WHERE s.rival = 1"
        					+ " AND sgp.squad_id = s._id AND sgp.gameweek = " + e.gameweek
        					+ " AND sgp.fpl_player_id = " + e.player_fpl_id, null);
        			riv.moveToFirst();
        			if (!riv.isAfterLast()) notif = true;
        			riv.close();
        		}
        	} else if (setting == Settings.NOTIF_ALL_PLAYERS) {
        		notif = true;
        	}
    	} else if (e.category == CAT_PRICE_CHANGE) {
    		int setting = Settings.getIntPref(pref);
    		if (setting == Settings.NOTIF_PRICE_MY_TEAM) {
        		notif = App.mySquadNext.contains(e.player_fpl_id);
        	} else if (setting == Settings.NOTIF_PRICE_WATCHLIST) {
        		notif = App.watchlist.contains(e.player_fpl_id);
        	} else if (setting == Settings.NOTIF_PRICE_MY_TEAM_WATCHLIST) {
        		notif = App.mySquadNext.contains(e.player_fpl_id);
        		if (!notif) {
        			// check watchlist
        			notif = App.watchlist.contains(e.player_fpl_id);
        		}
        	}
    	} else {
    		notif = Settings.getBoolPref(pref);
    	}
    	
    	boolean sound = false;
    	if (e.category == CAT_GOAL || e.category == CAT_ASS || e.category == CAT_RED) {
    		sound = Settings.getBoolPref(Settings.PREF_NOTIF_SOUND_SCORING);
    	} else if ( (e.category == CAT_KICK_OFF) || (e.category == CAT_FINAL_WHISTLE) ) {
    		sound = Settings.getBoolPref(Settings.PREF_NOTIF_SOUND_ALERTS);
    	}
    	
    	if (notif) {
	    	Intent notificationIntent = new Intent(c, Vidiprinter.class);
	    	App.notification(icon, text, title, text, notificationIntent, c, sound);
    	}
    }
    
    private static final String t_vidiprinter = "vidiprinter";
    private static final String f_datetime = "datetime";
    private static final String f_message = "message";
    private static final String f_player_fpl_id = "player_fpl_id";
    private static final String f_category = "category";
    private static final String f_gameweek = "gameweek";
    
    // add a Vidiprinter entry (called by background task)
    public static void addVidi(VidiEntry e, Context c) {
    	// either add directly in this thread (for when called by background task)
    	// or start async task (for action from UI thread)
    	long unixTime = App.currentUTCTime();
    	
    	ContentValues ve = new ContentValues();
    	e.datetime = unixTime;
    	ve.put(f_datetime, e.datetime);
    	ve.put(f_message, DbGen.compress(e.message));
    	ve.put(f_player_fpl_id, e.player_fpl_id);
    	ve.put(f_category, e.category);
    	ve.put(f_gameweek, e.gameweek);
    	App.dbGen.insert(t_vidiprinter, null, ve);
    	
    	if (e.gameweek != App.cur_gameweek) return;
    	
    	// set widget
    	WidgetNormal.addVidiEntry(e);
    	
    	// and "my vidi" on widget if applicable
    	if ( (e.player_fpl_id > 0) && App.mySquadCur.contains(e.player_fpl_id)) {
    		WidgetNormal.addMyVidiEntry(e);
    	}
    	
    	// notification, if applicable
		if (e.category == CAT_GOAL) {
			notification(e, Settings.PREF_NOTIF_GOAL, R.drawable.notif_goal, "Goal", e.message, c);
    	} else if (e.category == CAT_ASS) {
    		notification(e, Settings.PREF_NOTIF_ASSIST, R.drawable.notif_assist, "Assist", e.message, c);
    	} else if (e.category == CAT_RED) {
    		notification(e, Settings.PREF_NOTIF_RED, R.drawable.notif_red, "Red Card", e.message, c);
    	} else if (e.category == CAT_PRICE_CHANGE) {
    		notification(e, Settings.PREF_NOTIF_PRICE, R.drawable.differential, "Price Change", e.message, c);
    	} else if (e.category == CAT_TRANSFER) {
    		notification(e, Settings.PREF_NOTIF_TRANSFER, R.drawable.differential, "Transfer", e.message, c);
    	} else if (e.category == CAT_KICK_OFF) {
    		notification(e, Settings.PREF_NOTIF_KICKOFF, R.drawable.differential, "Kick-Off", e.message, c);
    	} else if (e.category == CAT_FINAL_WHISTLE) {
    		notification(e, Settings.PREF_NOTIF_FINAL_WHISTLE, R.drawable.differential, "Final Whistle", e.message, c);
    	}
    }
    
    public void toggleMySquad() {
    	my_squad = !my_squad;
    	popList();
    	setMySquadIcon(my_squad);
    }
    
     protected void onDestroy() {
	    if (curVidi != null) {
	    	curVidi.close();
		}
	    super.onDestroy();
    }
    
}
