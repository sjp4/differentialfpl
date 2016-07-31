package com.pennas.fpl.process;


import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;

import com.pennas.fpl.App;
import com.pennas.fpl.Home;
import com.pennas.fpl.R;
import com.pennas.fpl.Selection;
import com.pennas.fpl.Settings;
import com.pennas.fpl.Vidiprinter;
import com.pennas.fpl.App.GWFixture;
import com.pennas.fpl.Vidiprinter.VidiEntry;
import com.pennas.fpl.scrape.ScrapeFixtures;
import com.pennas.fpl.scrape.ScrapeLeagues;
import com.pennas.fpl.scrape.ScrapeMatchScores_New;
import com.pennas.fpl.scrape.ScrapeMySquad;
import com.pennas.fpl.scrape.ScrapePlayers;
import com.pennas.fpl.scrape.ScrapeSquadGameweek;
import com.pennas.fpl.scrape.ScrapeVersion;
import com.pennas.fpl.ui.WidgetNormal;
import com.pennas.fpl.util.FPLAsyncTask;
import com.pennas.fpl.util.Refreshable;
import com.pennas.fpl.util.URLUtil;
import com.pennas.fpl.util.URLUtil.UrlObj;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

public class FPLService extends Service {
	private static final int RUN_EVERY_X_SECONDS = 30;
	
	NotificationManager mNM;
	private Handler mHandler = new Handler();

	public static App client;
    
	public static final int PROC_SCRAPE_SCORES = 0;
    public static final int PROC_SCRAPE_PLAYERS = 1;
    public static final int PROC_SCRAPE_SQUADS = 2;
    public static final int PROC_SCRAPE_FIXTURES = 3;
    public static final int PROC_PROCESS_DATA = 4;
    
    // num_procs is just for bottom-bar procs
    public static final int NUM_PROCS = 5;
    
    public static final int UPDATE_WATCHLIST = 10;
    public static final int CHECK_VERSION = 11;
    public static final int UPDATE_NOTES = 12;
    public static final int JOIN_LEAGUE = 13;
    
    private static final String NOTIFY_GOAL = "NOTIFY_GOAL";
    
    // 2 hours
    public static final int COUNTDOWN_THRESH = 60 * 60 * 2;
    
    // hours
    public static final int VERSION_CHECK_FREQ = 3600 * 4;
    
    public static FPLService service;
    
    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
        client = (App) App.appContext;
        service = this;
        
        App.log("service onCreate");
        
        fails = Settings.getIntPref(Settings.PREF_FAILS);
        
        mHandler.removeCallbacks(mUpdateTimeTask);
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }
    
    public static class ProcRes {
    	public int procType;
    	public boolean failed = false;
    	public boolean updating = false;
    	public boolean dataChanged = false;
    	public ArrayList<String> messages = new ArrayList<String>();
    	public ProcTask procTask;
    	public Context context;
    	
    	// allowable progress range of this call from FPLService
    	public float progressFrom = 0;
    	public float progressTo = 99;
    	
    	// allowable progress range, assuming multiple calls within procressing task.
    	// sum of these calls should be 100
    	public float internalProgressFrom = 0;
    	public float internalProgressTo = 99;
    	
    	private float max;
    	
    	public String procname;
    	
    	public void setMaxName(float maximum, String name) {
    		max = maximum;
    		procname = name;
    		setProgress(0);
    	}
    	
    	public void setDataChanged(boolean changed) {
    		if (changed) dataChanged = true;
    	}
    	
    	public void setError(String error) {
    		failed = true;
    		messages.add(procname + ": " + error);
    	}
    	public void setComplete() {
    		messages.add(procname);
    	}
    	
    	private int progress = 0;
    	
    	public void setProgress(float currentCount) {
    		float newProgress = (currentCount / max) * 100;
    		// internal
    		//
    		// how much progress can be made by this proc?
    		float internalProgressAvailable = internalProgressTo - internalProgressFrom;
    		// progress added to baseline progress
    		float internalProgress = internalProgressFrom + ((newProgress/100)*internalProgressAvailable);
    		
    		// overall
    		float progressAvailable = progressTo - progressFrom;
    		float overallProgress = progressFrom + ((internalProgress/100)*progressAvailable);
    		int newTot = (int) overallProgress;
    		if (newTot > 100) newTot = 100;
    		if (newTot > (progress+1)) {
    			progress = newTot;
    			procTask.extProgress(progress);
    		}
    	}
    }
    
    // async task for all scrape/processing jobs
    public static class ProcTask extends FPLAsyncTask<Integer, Integer, ProcRes> {
        protected ProcRes doInBackground(Integer... params) {
        	ProcRes res = new ProcRes();
        	res.procTask = this;
        	res.context = client;
        	res.procType = params[0];
        	
    		switch (res.procType) {
        		case PROC_SCRAPE_PLAYERS:
        			// only scrape my squad/players if there is another gameweek to come..
        			if (App.cur_gameweek < App.num_gameweeks) {
        				ScrapeMySquad ss = new ScrapeMySquad();
	        			res.progressTo = 20;
						ss.updateSquad(res);
						
						if (res.failed) {
							break;
						}
						
						// players not available gw38 either
						ScrapePlayers s = new ScrapePlayers();
	        			res.progressFrom = 20;
	        			res.progressTo = 99;
	        			s.updatePlayers(res);
	        		// gw38. to allow login, scrape squad gameweek using transfers url
	        		// and store team id etc
        			} else if (App.cur_gameweek == App.num_gameweeks) {
        				App.log("gw 38; don't run ScrapeMySquad or ScrapePlayers. Runing special gw38login..");
        				ScrapeSquadGameweek.gw38login(res);
        			}
					break;
        		case PROC_PROCESS_DATA:
        			ProcessData p = new ProcessData();
        			//for (int i=7; i<=11; i++) p.processSeason(res, i);
        			
        			res.progressTo = 30;
        			p.processSeason(res, ProcessPlayer.ALL_SEASONS);
        			if (res.failed) break;
        			
        			res.progressFrom = 30;
        			res.progressTo = 99;
        			p.processData(res);
        			
        			// already refresh after processing
        			res.dataChanged = true;
        			break;
        		case PROC_SCRAPE_SCORES:
        			// TODO GW0 set fixture IDs uncomment the > 0 check
        			if (App.cur_gameweek > 0) {
	        			ScrapeMatchScores_New sm = new ScrapeMatchScores_New();
	        			sm.getPoints(res, App.cur_gameweek);
        			}
        			break;
        		case PROC_SCRAPE_FIXTURES:
        			ScrapeFixtures sf = new ScrapeFixtures();
        			sf.updateAllFixtures(res);
        			break;
        		case UPDATE_WATCHLIST:
        			updateWL(res, params[1], params[2], params[3]);
        			break;
        		case UPDATE_NOTES:
        			updateNotes(res);
        			break;
        		case CHECK_VERSION:
        			ScrapeVersion sv = new ScrapeVersion();
        			sv.checkVersion(res);
        			break;
        		case PROC_SCRAPE_SQUADS:
        			ScrapeLeagues sl = new ScrapeLeagues();
        			boolean justRivals = false;
        			if (params.length > 1) justRivals = true;
        			sl.updateLeagues(res, justRivals);
        			break;
        		case JOIN_LEAGUE:
        			joinLeague(res, DIFFERENTIAL_LEAGUE_CODE);
        			break;
        	}
        	return res;
        }

        protected void onProgressUpdate(Integer... progress) {
            sendProgress(progress[0]);
        }

        protected void onPostExecute(ProcRes result) {
        	WidgetNormal.updateWidgets(client);
        	sendCompleted(result);
        	
        	if ( (result.procType == JOIN_LEAGUE) && (!result.failed) ) {
        		if (Settings.getBoolPref(Settings.PREF_UPDATE_DATA)) {
        			App.initProc(true, FPLService.PROC_SCRAPE_SQUADS);
        		}
        	}
        }
        
        public void extProgress(int progress) {
        	publishProgress(progress);
        }
    }
    
    /*
    public static void dumpUrl(String url, boolean xmlRequest, int auth, String filename) {
    	try {
			UrlObj con = URLUtil.getUrlStream(url, xmlRequest, auth);
			if (con.ok) {
				File SDCardRoot = Environment.getExternalStorageDirectory();
				File file = new File(SDCardRoot, filename);
				FileOutputStream fileOutput = new FileOutputStream(file);
				byte[] buffer = new byte[1024];
				int bufferLength = 0; //used to store a temporary size of the buffer
				while ( (bufferLength = con.stream.read(buffer)) > 0 ) {
					fileOutput.write(buffer, 0, bufferLength);
				}
				fileOutput.close();
				con.stream.close();
			} else {
				App.log("ext url get failed");
				if (con.auth_fail) App.log("ext AUTH FAIL");
			}
		} catch (IOException e) {
			App.exception(e);
		}
    }
    */
    
    /*
    public static void printUrl(String url, int auth) {
    	try {
			UrlObj con = URLUtil.getUrlStream(url, auth);
			if (con.ok) {
				byte[] buffer = new byte[1024];
				int bufferLength = 0; //used to store a temporary size of the buffer
				while ( (bufferLength = con.stream.read(buffer)) > 0 ) {
					//AppClass.log(buffer);
				}
				con.stream.close();
			} else {
				AppClass.log("ext url get failed");
				if (con.auth_fail) AppClass.log("ext AUTH FAIL");
			}
		} catch (IOException e) {
			App.exception(e);
		}
    }
    */
    
    // 3 minutes
    private static final int DEADLINE_TIMER_REFRESH_THRESH = 60 * 3;
    private boolean done_deadline_notif = (Settings.getIntPref(Settings.PREF_DEADLINE_NOTIF_DONE) == App.next_gameweek);
    private static final int DEADLINE_NOTIF_MINS = 30;
    private static final int KICKOFF_NOTIF_MINS = 15;
    public static int fails = 0;
    public static int sincefail = 0;
    public static int cycles = 0;
    
    private static final int DEADLINE_WAIT = 60 * 5;
    public static final int NOT_SET = 0;
    
    public void doServiceStuff() {
    	long ukTime = App.currentUkTime();
    	
    	cycles++;
		   
    	UpdateManager.setFlags();
	    
    	// reset data for new GW
    	if (ukTime >= App.next_deadline) {
    		App.log("deadline passed: refresh data");
    		App.init_data();
    		// refresh current screen
    		refreshGeneric();
    		done_deadline_notif = false;
    	} else {
    		refreshHome();
    	}
       
    	App.updateBottomDynamic();
       
    	// are any fixtures live?
    	boolean prevLive = App.live;
    	App.live = false;
    	for (GWFixture f : App.gwFixtures.values()) {
    		if (f.kickoff_datetime <= ukTime) {
    			if (!f.complete) {
    				App.live = true;
    			    
    				// notify kick-off
    				if (!f.notified) {
    					// but only if started recently (don't want repeated entries if app stops/starts)
    					long sinceStart = ukTime - f.kickoff_datetime;
    					if (sinceStart < (KICKOFF_NOTIF_MINS * 60)) {
    						VidiEntry e = new VidiEntry();
    						e.category = Vidiprinter.CAT_KICK_OFF;
    						e.message = App.id2team.get(f.team_home_id) + " v "
							 		 + App.id2team.get(f.team_away_id) + " Kicked Off";
    						e.gameweek = App.cur_gameweek;
    						// set fixture id to player id, but negative
    						e.player_fpl_id = 0 - f.id;
    						Vidiprinter.addVidi(e, client);
    						f.notified = true;
    					} else {
    					   	f.notified = true;
    					}
    				}
    			} // !complete
    		} // started
    	}
    	if (prevLive != App.live) {
    		refreshHome();
    	}
    	
    	// sync data
    	long sinceDeadline = ukTime - App.last_deadline;
    	// don't do any syncs straight after deadline
    	if (sinceDeadline > DEADLINE_WAIT) {
	    	sincefail++;
			if (sincefail > fails) {
				if (Settings.getBoolPref(Settings.PREF_UPDATE_DATA)) {
					for (int proc=0; proc<NUM_PROCS; proc++) {
						// auto-run sync if enabled. always run processing
						if (!UpdateManager.fresh[proc]) {
							App.log("auto running: " + App.buttonLabel[proc]);
							App.initProc(true, proc);
						}
					}
				}
				
				// version/messages check
				if (!App.login_task) {
					if (Settings.getBoolPref(Settings.PREF_CHECK_FOR_UPDATES)) {
			    		long last_check_ver = Settings.getLongPref(Settings.LAST_CHECKED_NEW_VERSION);
			        	long diff = ukTime - last_check_ver;
			        	if (diff > VERSION_CHECK_FREQ) {
			        		App.initProc(true, CHECK_VERSION);
			        	}
			    	}
				}
			} // sincefail > fails
    	} // deadline wait
       
    	// deadline notification
    	if (!done_deadline_notif) {
    		if (Settings.getBoolPref(Settings.PREF_NOTIF_DEADLINE)) {
    			long notif_time = App.next_deadline - (DEADLINE_NOTIF_MINS * 60);
    			if (ukTime >= notif_time) {
    				long diff = App.next_deadline - ukTime;
    				String message = "Gameweek deadline is in " + App.printTimeDiff(diff, true);
    				Intent notificationIntent = new Intent(FPLService.this, Home.class);
    				boolean sound = Settings.getBoolPref(Settings.PREF_NOTIF_SOUND_ALERTS);
    				App.notification(R.drawable.differential, message, "FPL Deadline", message, notificationIntent
    						, FPLService.this, sound);
    				done_deadline_notif = true;
    				Settings.setIntPref(Settings.PREF_DEADLINE_NOTIF_DONE, App.next_gameweek, client);
    			}
    		}
    	}
    	
    	if (cycles == 1) {
    		// force widget init
    		WidgetNormal.forceInit(client);
    	}
    }
        
    // service proc - called every x seconds
    private Runnable mUpdateTimeTask = new Runnable() {
	    public void run() {
	       //App.log("service running.. password: " + Settings.getStringPref(Settings.PREF_FPL_PASSWORD));
	       //App.log("service running..");
		   
		   long ukTime = App.currentUkTime();
		   int delay;
		   long tillDeadline = App.next_deadline - ukTime;
		   if (tillDeadline <= DEADLINE_TIMER_REFRESH_THRESH) {
			   delay = 500;
		   } else {
			   delay = RUN_EVERY_X_SECONDS * 1000;
		   }
		   
	       mHandler.postDelayed(this, delay);
	       
	       doServiceStuff();
	   }
	};
	
	private static void refreshHome() {
		if (App.active != null) {
			if (App.active instanceof Home) {
				Home h = (Home) App.active;
		    	h.dataChanged();
		    }
		}
	}
	
	private static void refreshGeneric() {
		if (App.active != null) {
   	       	if (App.active instanceof Refreshable) {
   	       		Refreshable ref = (Refreshable) App.active;
   	       		ref.dataChanged();
   	       	}
		}
	}
	
	//private static long last_updated_widget;
	//private static final long WIDGET_UPDATE_FREQ_SECS = 40;
	
	// send current proc progress to AppClass for UI
    private static void sendProgress(int progress) {
    	App.setProgress(progress);
    	
    	/*
    	// update widget ocassionally during procs.
    	// limit at 99 because widget updated at end of proc anyway
    	if (progress <= 99) {
	    	long time = App.currentUTCTime();
	    	long diff = time - last_updated_widget;
	    	if (diff >= WIDGET_UPDATE_FREQ_SECS) {
	    		last_updated_widget = time;
	    		WidgetNormal.updateWidgets(client);
	    	}
    	}
    	*/
    }
    
    // send proc completed message to AppClass for UI
    private static void sendCompleted(ProcRes result) {
    	if (client == null) return;
    	client.setCompleted(result);
    }
    
    // start new proc... async
    public static void initProc(Integer... params) {
    	App.log("FPLService initProc: " + params[0]);
    	new ProcTask().fplExecute(params);
    }
    
    private static final String WL_URL_STUB = "http://fantasy.premierleague.com/web/api/watchlist/element/";
    private static final String WL_ADD = "/post/";
    private static final String WL_REMOVE = "/delete/";
    
    // core watchlist update (done within async)
    private static void updateWL(ProcRes result, int player_fpl_id, int player_pa_id, int newWL) {
    	UrlObj con = null;
    	result.setMaxName(1, "update watchlist");
    	
    	if (newWL == 1) {
    		try {
    			con = URLUtil.getUrlStream(WL_URL_STUB + player_pa_id + WL_ADD, false, URLUtil.AUTH_FULL);
    			App.dbGen.execSQL("INSERT INTO watchlist (player_fpl_id) VALUES (" + player_fpl_id + ")");
			} catch (IOException e) {
				App.exception(e);
				result.setError("add " + e);
			} catch (SQLiteException e) {
				App.exception(e);
			}
    	} else {
    		App.dbGen.execSQL("DELETE FROM watchlist WHERE player_fpl_id = " + player_fpl_id);
    		try {
    			con = URLUtil.getUrlStream(WL_URL_STUB + player_pa_id + WL_REMOVE, false, URLUtil.AUTH_FULL);
			} catch (IOException e) {
				App.exception(e);
			}
    	}
    	
    	if (con != null) {
    		if (!con.ok) result.setError("url failed");
    		con.closeCon();
    		if (con.updating) {
				result.updating = true;
			}
    	}
    	result.setComplete();
    }
    
    // core notes update (done within async)
    private static void updateNotes(ProcRes result) {
    	UrlObj con = null;
    	result.setMaxName(1, "update notes");
    	
    	try {
    		String post_encoded = "notes=" + URLEncoder.encode(Selection.notes_for_upload, "utf-8");
    		String token = URLUtil.getToken();
    		con = URLUtil.getUrlStream(ScrapeMySquad.URL_NOTES, true, URLUtil.AUTH_FULL, post_encoded, token, false, false, false, true);
			Settings.setStringPref(Settings.PREF_NOTES, Selection.notes_for_upload, result.context);
		} catch (IOException e) {
			App.exception(e);
			result.setError("notes: " + e);
		}
    	
    	if (con != null) {
    		if (!con.ok) result.setError("url failed");
    		con.closeCon();
    		if (con.updating) {
				result.updating = true;
			}
    	}
    	result.setComplete();
    	
    	Selection.notes_for_upload = null;
    }
    
    private static final String URL_JOIN_LEAGUE = "http://fantasy.premierleague.com/my-leagues/create-join-leagues/";
    private static final String DIFFERENTIAL_LEAGUE_CODE = "480340-565642";
    
    // core minileague join update (done within async)
    private static void joinLeague(ProcRes result, String league_code) {
    	UrlObj con = null;
    	result.setMaxName(1, "join league");
    	
    	try {
    		String token = URLUtil.getToken();
    		String post_encoded = "csrfmiddlewaretoken=" + URLEncoder.encode(token, "utf-8")
    			+ "&join_private_league_form-code=" + URLEncoder.encode(league_code, "utf-8")
    			+ "&action=" + URLEncoder.encode("join_private", "utf-8");
    		con = URLUtil.getUrlStream(URL_JOIN_LEAGUE, true, URLUtil.AUTH_FULL, post_encoded, token, false, false, false, false);
		} catch (IOException e) {
			App.exception(e);
			result.setError("join league: " + e);
		} catch (NullPointerException e) {
			App.exception(e);
			result.setError("join league: error");
		}
    	
    	if (con != null) {
    		if (!con.ok) result.setError("url failed");
    		con.closeCon();
    		if (con.updating) {
				result.updating = true;
			}
    	}
    	result.setComplete();
    }
    
    @Override
    public void onDestroy() {
        // cancel handler
        mHandler.removeCallbacks(mUpdateTimeTask);
        
        // Tell the user we stopped.
        Toast.makeText(this, "Differential service stopped", Toast.LENGTH_SHORT).show();
    }
    
    @Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
