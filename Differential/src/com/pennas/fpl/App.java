
package com.pennas.fpl;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TimeZone;

import com.actionbarsherlock.view.Window;
import com.crashlytics.android.Crashlytics;
import com.pennas.fpl.Vidiprinter.VidiEntry;
import com.pennas.fpl.process.FPLService;
import com.pennas.fpl.process.FPLService.ProcRes;
import com.pennas.fpl.process.UpdateManager;
import com.pennas.fpl.ui.ProgressButton;
import com.pennas.fpl.ui.WidgetNormal;
import com.pennas.fpl.util.DbGen;
import com.pennas.fpl.util.DbMoi;
import com.pennas.fpl.util.FPLActivity;
import com.pennas.fpl.util.LogBuffer;
import com.pennas.fpl.util.Refreshable;
import com.pennas.fpl.util.Screenshot;
import com.pennas.fpl.util.URLUtil;
import com.pennas.fpl.util.SquadUtil.GwPoints;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Build.VERSION;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SlidingDrawer;
import android.widget.Toast;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;
import android.widget.TextView;

public class App extends Application {
	public static boolean running_in_sdk = false;
	public static final boolean diags = true;
	public static final boolean checkdiff = false;
	
	// replace once using JB4.2 SDK
	public final static int JELLY_BEAN_4_2 = 17;
	
	public static SQLiteDatabase dbGen;
	
	// hard-coded parameters
	public static final int season = 16;
	public static final int num_gameweeks = 38;
	public static boolean live;
	
	// for widget
	public static GwPoints gw_score = new GwPoints();
	public static int total_score = 0;
	
	// user's squad id
	public static int my_squad_id = 0;
	public static String my_squad_name;
	
	// used everywhere
	public static int cur_gameweek;
	public static int next_gameweek;
	public static long next_deadline;
	public static long last_deadline;
	
	// key for ACRA crash reporting spreadsheet form
	public static final String formKey = "dDVCWWxGdEE1YkpOQWYxUkFJR3lJeWc6MQ";
	// crypto key
	public static final String appThing = "dDVCWgukgkgyf49083lJeWc6MQ";
	
	// current active activity - callback to refresh content
	public static FPLActivity active;
	
	// progress buttons
	public static final int[] buttonId = { R.id.genScores, R.id.genData, R.id.genProc, R.id.genSelection, R.id.genFixtures };
	public static ProgressButton[] buttonButton = new ProgressButton[FPLService.NUM_PROCS];
	public static final String[] buttonLabel = { "Scores", "Players", "Leagues", "Fixtures", "Processing"
		, "", "", "", "", "", "Watchlist", "Check Version", "", "", "", ""};
	public static final String[] buttonDesc = { 
		  "Points/scores. Updates leagues/squad totals"
		, "Player prices/stats/news + My selection/watchlist"
		, "Minileagues and rivals' selection for current GW"
		, "Fixtures for season"
		, "Calculate statistics/ratings and predictions" };
	public static final int[] buttonState = new int[FPLService.NUM_PROCS];
	public static int overallState = ProgressButton.STATE_OLD;
	
	public static boolean drawerOpen = false;
	
	// is a background proc currently running?
	public static boolean procRunning = false;
	public static int     procId;
	
	// commonly used (and cached) data
	public static HashSet<Integer> watchlist;
	public static HashSet<Integer> mySquadCur;
	public static HashSet<Integer> mySquadNext;
	public static HashMap<Integer, String> id2team;
	public static HashMap<Integer, Integer> team_fpl2id;
	public static HashMap<String, Integer> team_short_2id;
	public static HashMap<String, Integer> team_prem_2id;
	public static HashMap<String, Integer> team_2id;
	
	private static NotificationManager mNM;
	private static int notId = 0;
	public static SlidingDrawer slide;
	
	public static Context appContext;
	
	private static Queue<Integer[]> procQueue = new LinkedList<Integer[]>();
	
	private static SimpleDateFormat formatDate;
    
    public static boolean logged_in = false;
    
    public static boolean installed_on_sd;
    public static boolean need_warn_sd_card = false;
    
    public static LogBuffer logger;
    
    // when not null, notify user at next opportunity then reset to null
    public static String notify_new_version = null;
    public static int notify_new_version_version = 0;
    public static String notice = null;
    
    public static final int DEFAULT_STYLE = R.style.Theme_FPL_Dark;
    public static int style = R.style.Theme_FPL_Dark;
    
    @Override
	public void onCreate() {
    	if (!running_in_sdk) {
    		Crashlytics.start(this);
    	}
    	
    	// crash reporting
		super.onCreate();
		
		if ( "google_sdk".equals(Build.PRODUCT) || "sdk".equals(Build.PRODUCT) || "sdk_x86".equals(Build.PRODUCT) ) {
			running_in_sdk = true;
		}
		
		formatDate = new SimpleDateFormat("EEE d MMM HH:mm");
		formatDate.setTimeZone(TimeZone.getTimeZone("GMT"));
		//logger = new LogBuffer();
	    
		log("Application object being created");
		
		appContext = this;
		
		Settings.setDefaults(this);
		
		// check for API level 8 and higher
	    installed_on_sd = isInstalledOnSdCard();
	    log("installed_on_sd = " + installed_on_sd);
	    if (installed_on_sd) {
	    	if (!Settings.getBoolPref(Settings.PREF_WARNED_SD)) {
	    		log("will warn about SD card");
	    		need_warn_sd_card = true;
	    	}
	    } else {
	    	// not on sdcard, ensure that pref shows "not warned" in
	    	// preparation for next time it moved to sd
	    	Settings.setBoolPref(Settings.PREF_WARNED_SD, false, this);
	    }
		
	    // do any version upgrade (non-db-structure) stuff
	    // (including deleting dbMoi if required)
		processUpgrades_pre_db();
		
		// delete debug copy db to sd card for loadup
		DbGen.delete_db_from_sd_card(this);
		
		// create/load databases
		getDB(this);
		
		// dbGen upgrade stuff
		processUpgrades_post_db();
		
		my_squad_id = Settings.getIntPref(Settings.PREF_FPL_SQUADID);
		String email = Settings.getStringPref(Settings.PREF_FPL_EMAIL);
		if (email != null) logged_in = true;
		
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		// initialise basic data (gameweek etc)
		init_data();
		
		// start service
		this.startService(new Intent(this, FPLService.class));
		
		// delete any old temp screenshot bitmaps
		Screenshot.cleanup_screenshots();
	}
    
    // process any required version upgrade stuff
    private void processUpgrades_pre_db() {
    	try {
    		PackageManager manager = getPackageManager();
    		PackageInfo info;
    		info = manager.getPackageInfo(getPackageName(), 0);
    		int curVer = info.versionCode;
    		String curVerString = info.versionName;
    		
    		App.log("pre-db version " + curVer + " (" + curVerString + ")");
    		
    		int oldVer = Settings.getIntPref(Settings.INSTALLED_APP_VERSION);
    		
    		if (curVer > oldVer) {
    			App.log(" old version " + oldVer);
    			App.log("Resetting fail count..");
    			App.setFailed(false, appContext);
    		}
    		
    		if (oldVer == 0) {
    			App.log("no upgrades from 0");
    		} else {
    			// version upgrades (v16 onwards)
    			
    			// 2015/16: delete dbMoi from previous season
    			if (oldVer < 56) {
    				boolean res = deleteDatabase(DbMoi.DATABASE_NAME);
    				App.log("v56 delete dbMoi. result: " + res);
    				logOut(this);
    			}

    		}
		} catch (NameNotFoundException e) {
			exception(e);
		}
    }
    
    // process any required version upgrade stuff
    private void processUpgrades_post_db() {
    	try {
    		PackageManager manager = getPackageManager();
    		PackageInfo info;
    		info = manager.getPackageInfo(getPackageName(), 0);
    		int curVer = info.versionCode;
    		String curVerString = info.versionName;
    		
    		App.log("post-db version " + curVer + " (" + curVerString + ")");
    		
    		int oldVer = Settings.getIntPref(Settings.INSTALLED_APP_VERSION);
    		
    		if (curVer > oldVer) {
    			App.log(" old version " + oldVer);
    			App.log("Resetting fail count..");
    			App.setFailed(false, appContext);
    		}
    		
    		if (oldVer == 0) {
    			App.log("no upgrades from 0");
    		} else {
    			// version upgrades (v16 onwards)

    		}
    		
    		Settings.setIntPref(Settings.INSTALLED_APP_VERSION, curVer, this);
		} catch (NameNotFoundException e) {
			exception(e);
		}
    }
    
    // log out
    public static void logOut(Context c) {
    	App.logged_in = false;
		App.my_squad_id = 0;
		App.my_squad_name = null;
		if (App.mySquadCur != null) {
			App.mySquadCur.clear();
		}
		if (App.mySquadNext != null) {
			App.mySquadNext.clear();
		}
		App.gw_score = new GwPoints();
		WidgetNormal.setSquadScore();
		URLUtil.logOut();
		Settings.setStringPref(Settings.PREF_FPL_EMAIL, null, c);
		Settings.setStringPref(Settings.PREF_FPL_PASSWORD, null, c);
		Settings.setIntPref(Settings.PREF_FPL_SQUADID, 0, c);
		// could delete dbMoi stuff here...
		//
		// (but there is some cleanup stuff in squads scrape now isn't there?)
    }
    
    @SuppressLint({ "SdCardPath", "InlinedApi" })
	private boolean isInstalledOnSdCard() {

        // check for API level 8 and higher
        if (VERSION.SDK_INT > android.os.Build.VERSION_CODES.ECLAIR_MR1) {
        	PackageManager pm = getPackageManager();
        	try {
        		PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
        		ApplicationInfo ai = pi.applicationInfo;
        		return ( (ai.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) == ApplicationInfo.FLAG_EXTERNAL_STORAGE);
        	} catch (NameNotFoundException e) {
        		exception(e);
        	}
        }

        // check for API level <=7 - check files dir
        try {
        	String filesDir = getFilesDir().getAbsolutePath();
        	if (filesDir.startsWith("/data/")) {
        		return false;
        	} else if (filesDir.contains("/mnt/")) {
        		return true;
          	} else if (filesDir.contains("/sdcard/")) {
        		return true;
          	}
        } catch (Throwable e) {
        	exception(e);
        }

        return false;
    }
    
    private static int progress;

	// called by background process to update progress to GUI
	public static void setProgress(int pProgress) {
		progress = pProgress;
		updateProgressDisplay();
		
		if (login_task) {
			if (login_task_ref == null) {
				log("ERROR null ref for Home login progress callback");
			} else {
				login_task_ref.updateLoginText(progress);
			}
		}
	}
	
	public static void updateProgressDisplay() {
		if (drawerOpen) {
			if (procId < FPLService.NUM_PROCS) {
				if (buttonButton[procId] != null) buttonButton[procId].setProgress(progress);
			}
		}
		if (procId != FPLService.CHECK_VERSION) {
			if (active != null) {
				int progressAB = (Window.PROGRESS_END - Window.PROGRESS_START) / 100 * progress;
				active.setSupportProgress(progressAB);
				active.setSupportProgressBarVisibility(true);
			}
		}
	}
	
	public static long dataUpdatedTime = 0;
	
	public static void setFailed(boolean failed, Context c) {
		if (failed) {
			if (FPLService.fails == 0) {
				FPLService.fails = 1;
			} else {
				FPLService.fails *= 2;
			}
			FPLService.sincefail = 0;
		} else {
			FPLService.fails = 0;
		}
		Settings.setIntPref(Settings.PREF_FAILS, FPLService.fails, c);
	}
	
	// called after completion of background process to update GUI (and possibly refresh)
	public void setCompleted(ProcRes res) {
		if (res == null) {
			return;
		}
		if (active != null) {
			// callback to current activity
			if (res.dataChanged) {
				if (active instanceof Refreshable) {
					Refreshable ref = (Refreshable) active;
					ref.dataChanged();
				}
				// set all activities to refresh when they resume
				dataUpdatedTime = currentUTCTime();
			}
			
			active.setSupportProgressBarVisibility(false);
		}
		
		setFailed(res.failed, this);
		
		procRunning = false;
		
		if (procId < FPLService.NUM_PROCS) {
			buttonState[procId] = ProgressButton.STATE_OK;
			App.log(buttonLabel[procId] + " finished with error = " + res.failed + " dataChanged = " + res.dataChanged);
			// if data changed
			if (!res.failed) {
				UpdateManager.updatedData(procId);
			} else {
				buttonState[procId] = ProgressButton.STATE_OLD;
			}
		}
		// call this outside of if, so that sync icon is removed for watchlist etc
		updateButtonStatus();
		
		if (res.updating) {
			if (active != null) {
				Context c = (Context) active;
				String toast = "The FPL website is updating; sync could not be completed";
				Toast.makeText(c, toast, Toast.LENGTH_LONG).show();
			}
		} else if ( (res.messages.size() > 0) && (active != null) ) {
			if (procId != FPLService.CHECK_VERSION) {
				StringBuffer toast = new StringBuffer();
				
				if (res.failed) {
					toast.append("Error: ");
				} else {
					toast.append("Finished: ");
				}
				
				boolean first = true;
				for (String s : res.messages) {
					if (!first) toast.append(", ");
					if (s != null) {
						String str = s.replaceAll("http[a-zA-Z0-9\\-./:]+", "URL");
						toast.append(str);
						App.log("toast: " + str);
						first = false;
					}
				}
				
				if (active != null) {
					Context c = (Context) active;
					Toast.makeText(c, toast, Toast.LENGTH_LONG).show();
				}
			}
		}
		
		// if more stuff queued... run it
		if (procQueue.size() > 0) {
			initProc(true, procQueue.poll());
		}
		
		if (login_task) {
			if ( (res.procType == FPLService.PROC_SCRAPE_PLAYERS) 
			  || (res.procType == FPLService.PROC_SCRAPE_SQUADS) ) {
				if (login_task_ref == null) {
					log("ERROR null ref for Home login callback");
				} else {
					login_task_ref.callBack(res);
				}
			}
		}
	}
	
	// create databases if needed, then open
    public static void getDB(Context c) {
    	if (dbGen == null) {
	    	DbGen myDbGen = new DbGen(c);
	        try {
	        	dbGen = myDbGen.createDataBase(c);
		 	} catch (IOException ioe) {
		 		log("create dbGen failed:");
		 		exception(ioe);
		 	}
    	}
    	
    	DbGen.enable_write_ahead();
    	
    	// try to avoid database being automatically closed
    	dbGen.acquireReference();
    }
    
    private static final String TAG = "FPL_Diff";
    
    // logging util
	public static void log(String s) {
		if (running_in_sdk) {
			Log.i(TAG, s);
		} else {
			Crashlytics.log(s);
		}
	}
	public static void exception(Throwable e) {
    	String s = e.toString();
    	
    	if (running_in_sdk) {
    		Log.e(TAG, s);
    	}
    	if (diags) {
    		Crashlytics.logException(e);
    	}
    	
    	StringWriter sw = new StringWriter();
    	e.printStackTrace(new PrintWriter(sw));
    	s = sw.toString();
    	if (running_in_sdk) {
    		Log.e(TAG, s);
    	}
    	if (diags) {
    		//logger.add(s);
    	}
    	//logFile(e.toString(), false);
    	//logFile(sw.toString(), true);
    }
	
	//private static BufferedWriter out;
	//private static long flushTime;
	//private static final int flushPer = 10000;
	
	// additional logging to file if enabled
	/*
	private static void logFile(String s, boolean flush) {
		try {
		    if (out == null) {
		    	File root = Environment.getExternalStorageDirectory();
			    if (root.canWrite()){
			        File gpxfile = new File(root, "differential.log");
			        FileWriter gpxwriter = new FileWriter(gpxfile);
			        out = new BufferedWriter(gpxwriter);
			    }
		    }
		    String date = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date());
			out.write(date + ": " + s + "\r\n");
			if (flush || ((System.currentTimeMillis() - flushTime) >  flushPer) ) {
				out.flush();
			} else {
				flushTime = System.currentTimeMillis();
			}
		} catch (IOException e) {
		    Log.e(TAG, "Could not write log file " + e.getMessage());
		}
	}
	*/
	
	// close log file
	/*public static void close() {
		try {
			out.close();
		} catch (IOException e) {
			exception(e);
		}
	}*/
    
	// create an Android notification
    public static void notify(Notification notification) {
		notId++;
		try {
			mNM.notify(notId, notification);
		} catch (SecurityException e) {
			exception(e);
			// toast can cause NPE..
			//Toast.makeText(appContext, "Failed to generate notification: " + notification.tickerText, Toast.LENGTH_LONG).show();
		}
	}
    
    // notification helper
    public static void notification(int icon, String tickerText, String contentTitle, String contentText
    		, Intent intent, Context c, boolean sound) {
    	Notification notification = new Notification(icon, tickerText, System.currentTimeMillis());
		
    	Context context = c.getApplicationContext();
    	
    	PendingIntent contentIntent = PendingIntent.getActivity(c, 0, intent, 0);

    	notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
    	notification.flags |= Notification.FLAG_AUTO_CANCEL;
    	if (sound) {
    		notification.defaults |= Notification.DEFAULT_SOUND;
    	}
    	
		notify(notification);
    }
    
    public static HashMap<Integer, GWFixture> gwFixtures;
	public static class GWFixture {
		public int id;
		public int fpl_id;
		public long kickoff_datetime;
		public boolean complete;
		public boolean got_bonus;
		public int team_home_id;
		public int team_away_id;
		public boolean notified;
		public long marked_finished_datetime;
		public long marked_90_datetime;
		public int goals_home;
		public int goals_away;
	}
	
	// initialise basic gameweek data
	public static void init_data_gameweek() {
    	long unixTime = currentUkTime();
    	
    	App.log("init data. uk date: " + printDate(unixTime));
    	
    	Cursor gw = dbGen.rawQuery("SELECT num, end_datetime, start_datetime FROM gameweek WHERE season = " + season + " AND start_datetime <= " + unixTime + " AND end_datetime >= " + unixTime, null);
    	gw.moveToFirst();
	 	if (gw.isAfterLast()) {
	 		cur_gameweek = 0;
	 		next_gameweek = 1;
	 		//next_deadline = FPLService.NOT_SET;
	 		//last_deadline = FPLService.NOT_SET;
	 		Cursor gw2 = dbGen.rawQuery("SELECT start_datetime FROM gameweek WHERE season = " + season + " AND num = 1", null);
	    	gw2.moveToFirst();
	    	if (!gw2.isAfterLast()) {
	    		last_deadline = 0;
	    		next_deadline = gw2.getLong(0);
	    	}
	    	gw2.close();
	 	} else {
	 		cur_gameweek = gw.getInt(0);
	 		next_deadline = gw.getLong(1) + 1;
	 		last_deadline = gw.getLong(2);
	 		
	 		if (cur_gameweek < num_gameweeks) {
	 			next_gameweek = cur_gameweek + 1;
	 		} else {
	 			next_gameweek = 0;
	 		}
	 	}
	 	gw.close();
	 	
	 	/*cur_gameweek = 0;
 		next_gameweek = 1;
 		next_deadline = 2587593600l;*/
	 	
	 	gwFixtures = new HashMap<Integer, GWFixture>();
	 	Cursor fix = dbGen.rawQuery("SELECT _id, datetime, got_bonus, team_home_id, team_away_id, fpl_id, res_goals_home, res_goals_away"
	 			+ " FROM fixture WHERE season = " + season + " AND gameweek = " + cur_gameweek, null);
	 	fix.moveToFirst();
	 	while (!fix.isAfterLast()) {
	 		GWFixture f = new GWFixture();
	 		f.id = fix.getInt(0);
	 		f.kickoff_datetime = fix.getLong(1);
	 		f.complete = (fix.getInt(2)>=1);
	 		f.got_bonus = (fix.getInt(2)>=2);
	 		f.team_home_id = fix.getInt(3);
	 		f.team_away_id = fix.getInt(4);
	 		f.fpl_id = fix.getInt(5);
	 		f.goals_home = fix.getInt(6);
	 		f.notified = !fix.isNull(6);
	 		f.goals_away = fix.getInt(7);
	 		gwFixtures.put(f.id, f);
	 		fix.moveToNext();
	 	}
	 	fix.close();
	 	
	 	// load scores for my squad
		if (my_squad_id > 0) {
			// current gw score                        0       1         2            3
			Cursor curCur = dbGen.rawQuery("SELECT points, c_points, c_p_playing, c_p_to_play"
					//    4           5        6          7     8           9
					+ " , c_complete, c_goals, c_assists, c_cs, points_hit, c_bonus"
					+ " FROM squad_gameweek"
					+ " WHERE squad_id = " + my_squad_id + " AND gameweek = " + cur_gameweek, null);
		 	curCur.moveToFirst();
		 	if (!curCur.isAfterLast()) {
		 		int gwp = curCur.getInt(0);
		 		int gwc_p = curCur.getInt(1);
		 		if (gwc_p > 0) {
		 			gw_score.points = gwc_p;
		 		} else {
		 			gw_score.points = gwp;
		 		}
		 		gw_score.p_playing = curCur.getInt(2);
		 		gw_score.p_to_play = curCur.getInt(3);
		 		gw_score.complete = (curCur.getInt(4) == 1);
		 		gw_score.goals = curCur.getInt(5);
		 		gw_score.assists = curCur.getInt(6);
		 		gw_score.clean_sheets = curCur.getInt(7);
		 		gw_score.points_hit = curCur.getInt(8);
		 		gw_score.bonus = curCur.getInt(9);
		 		WidgetNormal.setSquadScore();
		 	}
		 	curCur.close();
		}
    }
	
	// initialise basic data
    public static void init_data() {
    	init_data_gameweek();
    	
    	watchlist = new HashSet<Integer>();
	 	
	 	// get watchlist from db
	 	Cursor wl = dbGen.rawQuery("SELECT player_fpl_id FROM watchlist", null);
	 	wl.moveToFirst();
	 	while (!wl.isAfterLast()) {
	 		watchlist.add(wl.getInt(0));
	 		//AppClass.log("wl add: " + wl.getInt(0));
	 		wl.moveToNext();
	 	}
	 	wl.close();
	 	
	 	mySquadCur = new HashSet<Integer>();
	 	
	 	// get current week squad from db
	 	Cursor squadCur = dbGen.rawQuery("SELECT fpl_player_id FROM squad_gameweek_player WHERE squad_id = " + my_squad_id + " AND gameweek = " + cur_gameweek, null);
	 	squadCur.moveToFirst();
	 	while (!squadCur.isAfterLast()) {
	 		mySquadCur.add(squadCur.getInt(0));
	 		//AppClass.log("squad add cur: " + squadCur.getInt(0));
	 		squadCur.moveToNext();
	 	}
	 	squadCur.close();
	 	
	 	mySquadNext = new HashSet<Integer>();
	 	
	 	// get next week squad from db
	 	Cursor squadNext = dbGen.rawQuery("SELECT fpl_player_id FROM squad_gameweek_player WHERE squad_id = " + my_squad_id + " AND gameweek = " + next_gameweek, null);
	 	squadNext.moveToFirst();
	 	while (!squadNext.isAfterLast()) {
	 		mySquadNext.add(squadNext.getInt(0));
	 		//AppClass.log("squad add next: " + squadNext.getInt(0));
	 		squadNext.moveToNext();
	 	}
	 	squadNext.close();
	 	
	 	// load scores for my squad
		if (my_squad_id > 0) {
			// overall score
		 	Cursor curGw = dbGen.rawQuery("SELECT points, c_points, name FROM squad WHERE _id = " + my_squad_id, null);
		 	curGw.moveToFirst();
		 	if (!curGw.isAfterLast()) {
		 		int totp = curGw.getInt(0);
		 		int totc_p = curGw.getInt(1);
		 		if (totc_p > 0) {
		 			total_score = totc_p;
		 		} else {
		 			total_score = totp;
		 		}
		 		my_squad_name = DbGen.decompress(curGw.getBlob(2));
		 	}
		 	curGw.close();
		}
		
		id2team = new HashMap<Integer, String>();
		team_short_2id = new HashMap<String, Integer>();
		team_prem_2id = new HashMap<String, Integer>();
		team_2id = new HashMap<String, Integer>();
		
		// get all team names from db
	 	Cursor teams = dbGen.rawQuery("SELECT _id, name, short_name, prem_name FROM team", null);
	 	teams.moveToFirst();
	 	while (!teams.isAfterLast()) {
	 		int id = teams.getInt(0);
	 		String name = teams.getString(1);
	 		String short_name = teams.getString(2);
	 		String prem_name = teams.getString(3);
	 		
	 		id2team.put(id, name);
	 		team_short_2id.put(short_name, id);
	 		team_prem_2id.put(prem_name, id);
	 		team_2id.put(name, id);
	 		teams.moveToNext();
	 	}
	 	teams.close();
	 	
	 	team_fpl2id = new HashMap<Integer, Integer>();
	 	
		// get mapping from season team fpl id to base team _id
	 	Cursor team_ids = dbGen.rawQuery("SELECT team_id, fpl_id FROM team_season WHERE season = " + season, null);
	 	team_ids.moveToFirst();
	 	while (!team_ids.isAfterLast()) {
	 		int team_id = team_ids.getInt(0);
	 		int fpl_id = team_ids.getInt(1);
	 		
	 		team_fpl2id.put(fpl_id, team_id);
	 		team_ids.moveToNext();
	 	}
	 	team_ids.close();
	 	
	 	UpdateManager.initData();
	 	// init each state
    	for (int i=0; i<FPLService.NUM_PROCS; i++) {
    		if (UpdateManager.fresh[i]) {
    			buttonState[i] = ProgressButton.STATE_OK;
    		} else {
    			buttonState[i] = ProgressButton.STATE_OLD;
    		}
	    }
    	
    	Cursor vidi = dbGen.rawQuery("SELECT datetime, message, gameweek, category, player_fpl_id"
    			+ " FROM vidiprinter ORDER BY gameweek DESC, datetime DESC"
    			+ " LIMIT " + WidgetNormal.MAX_VIDI_ENTRIES, null);
    	vidi.moveToLast();
    	while (!vidi.isBeforeFirst()) {
    		VidiEntry e = new VidiEntry();
    		e.datetime = vidi.getInt(0);
    		e.message = DbGen.decompress(vidi.getBlob(1));
    		e.gameweek = vidi.getInt(2);
    		e.category = vidi.getInt(3);
    		e.player_fpl_id = vidi.getInt(4);
    		WidgetNormal.addVidiEntry(e);
    		vidi.moveToPrevious();
    	}
    	vidi.close();
    	
    	Cursor curMyVidi = dbGen.rawQuery("SELECT v.datetime, v.message, v.gameweek, v.category, v.player_fpl_id"
				+ " FROM vidiprinter v, squad_gameweek_player sgp WHERE sgp.squad_id = " + my_squad_id
				+ " AND v.gameweek = sgp.gameweek AND v.player_fpl_id = sgp.fpl_player_id"
				+ " ORDER BY v.gameweek DESC, v.datetime DESC LIMIT " + WidgetNormal.MAX_VIDI_ENTRIES, null);
		curMyVidi.moveToLast();
		while (!curMyVidi.isBeforeFirst()) {
    		VidiEntry e = new VidiEntry();
    		e.datetime = curMyVidi.getInt(0);
    		e.message = DbGen.decompress(curMyVidi.getBlob(1));
    		e.gameweek = curMyVidi.getInt(2);
    		e.category = curMyVidi.getInt(3);
    		e.player_fpl_id = curMyVidi.getInt(4);
    		WidgetNormal.addMyVidiEntry(e);
    		curMyVidi.moveToPrevious();
    	}
		curMyVidi.close();
    	
    	WidgetNormal.updateWidgets(appContext);
    }
    
    // define on-click listener for buttons used in initBottom
	private static OnClickListener ocl = new OnClickListener() {
		public void onClick(View v) {
			int id = v.getId();
			for (int i=0; i<FPLService.NUM_PROCS; i++) {
		    	if (id == buttonId[i]) App.initProc(false, i);
		    }
		}
    };
    
    private static OnDrawerOpenListener drawerOpenListener = new OnDrawerOpenListener() {
    	public void onDrawerOpened() {
    		drawerOpen = true;
    		if (procRunning) {
    			updateProgressDisplay();
    		}
    	}
    };
    private static OnDrawerCloseListener drawerClosedListener = new OnDrawerCloseListener() {
    	public void onDrawerClosed() {
    		drawerOpen = false;
    	}
    };
    
    private static TextView gb_gameweek;
    private static TextView gb_countdown;
    private static void destroyBottomWidgets() {
    	gb_gameweek = null;
    	gb_countdown = null;
    }
    //private static ImageView background_processing;
    
    // initialise the genericBottom (progress drawer etc) section of an activity.
    // set listeners/callback refs etc
    public static void initBottom(FPLActivity a) {
    	slide = (SlidingDrawer) a.findViewById(R.id.drawer);
    	
    	// init each button
    	for (int i=0; i<FPLService.NUM_PROCS; i++) {
	    	buttonButton[i] = (ProgressButton) a.findViewById(buttonId[i]);
	    	buttonButton[i].setOnClickListener(ocl);
	    	buttonButton[i].setLabel(buttonLabel[i]);
	    	buttonButton[i].setDesc(buttonDesc[i]);
	    }
    	
    	gb_gameweek = (TextView) a.findViewById(R.id.drawer_gameweek);
    	gb_countdown = (TextView) a.findViewById(R.id.drawer_countdown);
    	
    	active = a;
	    drawerOpen = false;
	    
	    slide.setOnDrawerOpenListener(drawerOpenListener);
	    slide.setOnDrawerCloseListener(drawerClosedListener);
	    
	    if (procRunning) {
	    	updateProgressDisplay();
	    } else {
	    	active.setSupportProgressBarVisibility(false);
	    }
	    
	    updateButtonStatus();
	    updateBottomDynamic();
    }
    
    public static void updateBottomDynamic() {
    	if (gb_gameweek != null) {
    		String gw = String.valueOf(cur_gameweek);
    		gb_gameweek.setText(gw);
    	}
    	if (gb_countdown != null) {
	    	long diff = App.next_deadline - App.currentUkTime();
	    	boolean soon = (diff < FPLService.COUNTDOWN_THRESH);
	    	gb_countdown.setText(App.printDate(App.next_deadline) + " (" + App.printTimeDiff(diff, soon) + ")");
    	}
    }
    
    // update status of progress buttons / drawer button based on current proc state
    public static void updateButtonStatus() {
    	boolean old = false;
    	boolean proc = false;
    	for (int i=0; i<FPLService.NUM_PROCS; i++) {
    		if (buttonButton[i] != null) {
	    		buttonButton[i].setState(buttonState[i]);
	    		
	    		boolean this_queued = false;
	    		for (Integer[] pp : procQueue) {
	    			if (pp[0] == i) this_queued = true;
	    		}
	    		if (procRunning && (i == procId) ) {
	    			buttonButton[i].setStatus("Running..");
    			} else if (this_queued) {
	    			buttonButton[i].setStatus("Queued..");
	    		} else if (UpdateManager.updatedTimestamps[i] != UpdateManager.NEVER_UPDATED) {
	    			long updated = UpdateManager.updatedTimestamps[i];
	    			if (updated == UpdateManager.NEVER_UPDATED) {
	    				buttonButton[i].setStatus("Not Updated");
	    			} else {
	    				buttonButton[i].setStatus(printDate(utcToLocal(updated)));
	    			}
	    		} else {
	    			buttonButton[i].setStatus("");
	    		}
    		}
    		if (buttonState[i] == ProgressButton.STATE_OLD) old = true;
    		if (buttonState[i] == ProgressButton.STATE_PROC) proc = true;
    	}
    	
    	if (proc) {
    		overallState = ProgressButton.STATE_PROC;
    	} else if (old) {
    		overallState = ProgressButton.STATE_OLD;
    	} else {
    		overallState = ProgressButton.STATE_OK;
    	}
    	
    	if (active != null) {
			if (procRunning) {
	    		if (procId == FPLService.CHECK_VERSION) {
					active.stopSyncAnimate();
				} else {
	    			active.startSyncAnimate();
				}
			} else {
				active.stopSyncAnimate();
			}
    	}
    }
    
    public static boolean login_task;
    public static Home login_task_ref;
    
    // run a background process
    public static void initProc(boolean auto, Integer... params) {
    	if ( (login_task == false) && !logged_in) {
    		if (active != null) {
    			if (!auto) {
    				Context c = (Context) active;
    				Toast.makeText(c, "Please log in", Toast.LENGTH_LONG).show();
    			} 
			}
    		return;
    	}
    	if (procRunning) {
    		// if this proc is running or queued, don't run again
    		boolean this_queued = false;
    		// check if this proc is queued
    		for (Integer[] pp : procQueue) {
    			if (pp[0] == params[0]) this_queued = true;
    		}
    		if (this_queued || (procId == params[0]) ) {
    			App.log(buttonLabel[params[0]] + " already queued/running");
    		} else {
    			// queue this proc
    			App.log("proc running; queueing " + buttonLabel[params[0]]);
    			procQueue.add(params);
    			updateButtonStatus();
    		}
    	} else {
	    	procRunning = true;
	    	procId = params[0];
	    	FPLService.initProc(params);
	    	if (params[0]<FPLService.NUM_PROCS) buttonState[params[0]] = ProgressButton.STATE_PROC;
	    	setProgress(0);
	    	updateButtonStatus();
    	}
    }
    
    // Application shutting down
    protected void onDestroy() {
        FPLService.client = null;
        log("Application object being destroyed");
        //close();
    }
    
    // remove references to activity
    public static void destroyActivity() {
    	//log("Application: destroy activity");
    	for (int i=0; i<FPLService.NUM_PROCS; i++) {
    		buttonButton[i] = null;
    	}
    	active = null;
    	destroyBottomWidgets();
    	slide = null;
    	drawerOpen = false;
    }
    
    private static TimeZone zone_utc = TimeZone.getTimeZone("UTC");
    private static TimeZone zone_london = TimeZone.getTimeZone("Europe/London");
    private static TimeZone zone_local = TimeZone.getDefault();
    
    public static long currentUTCTime() {
    	Calendar calUtc = Calendar.getInstance(zone_utc);
    	return calUtc.getTimeInMillis() / 1000;
    }
    
    public static long utcToLocal(long utc) {
    	long localtime = (utc * 1000) + zone_local.getOffset(utc * 1000);
    	return localtime / 1000;
    }
    
    public static long currentUkTime() {
    	long utc = currentUTCTime();
    	long londontime = (utc * 1000) + zone_london.getOffset(utc * 1000);
    	return londontime / 1000;
    }
    
    public static long ukToLocal(long uk) {
    	long utc = (uk * 1000) - zone_london.getOffset(uk * 1000);
    	long localtime = utc + zone_local.getOffset(utc);
    	return localtime / 1000;
    }
    
    public static long ukToUtc(long uk) {
    	long utc = (uk * 1000) - zone_london.getOffset(uk * 1000);
    	return utc / 1000;
    }
    
    public static long ukPrevMidnight() {
    	long ukTime = currentUkTime();
    	//log("ukTime: " + ukTime + " = " + printDate(ukTime, false));
    	
    	Calendar calUtc = Calendar.getInstance(zone_utc);
    	calUtc.setTimeInMillis(ukTime * 1000);
    	//log("hours: " + calUtc.get(Calendar.HOUR_OF_DAY) + " mins: " + calUtc.get(Calendar.MINUTE) 
    	//		+ " secs: " + calUtc.get(Calendar.SECOND));
    	
    	long ukMidnight = ukTime - (calUtc.get(Calendar.HOUR_OF_DAY) * 60 * 60) - (calUtc.get(Calendar.MINUTE) * 60)
    	        - calUtc.get(Calendar.SECOND);
    	//log("ukMidnight: " + ukMidnight + " = " + App.printDate(ukMidnight, false));
    	return ukMidnight;
    }
    
    public static String printDate(long dateSeconds) { 
    	long date = dateSeconds * 1000;
    	
    	synchronized(formatDate){
			return formatDate.format(date);
		}
    }
    
    public static String printTimeDiff(long diffSeconds, boolean minsSecs) {
    	StringBuffer diff = new StringBuffer();
    	boolean prev = false;
    	
    	int days = (int) (diffSeconds / 86400);
    	if (days > 0) {
    		diff.append(days + " day");
    		if (days > 1) diff.append("s");
    		prev = true;
    	}
    	
    	long newdiff = diffSeconds - (86400 * days);
    	int hours = (int) (newdiff / (60 * 60));
    	if (hours > 0) {
    		if (prev) diff.append(" ");
    		diff.append(hours + " hour");
    		if (hours > 1) diff.append("s");
    		prev = true;
    	}
    	
    	if (minsSecs) {
	    	newdiff = newdiff - ((60 * 60) * hours);
	    	int minutes = (int) (newdiff / 60);
	    	if (minutes > 0) {
	    		if (prev) diff.append(" ");
	    		diff.append(minutes + " min");
	    		if (minutes > 1) diff.append("s");
	    		prev = true;
	    	}
	    	
	    	int seconds = (int) (newdiff - (60 * minutes));
	    	if (seconds > 0) {
	    		if (prev) diff.append(" ");
	    		diff.append(seconds + " sec");
	    		if (seconds > 1) diff.append("s");
	    	}
    	}
    	
    	return diff.toString();
    }

}

