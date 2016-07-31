package com.pennas.fpl.process;

import com.pennas.fpl.App;
import com.pennas.fpl.App.GWFixture;
import com.pennas.fpl.R;
import com.pennas.fpl.Settings;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

public class UpdateManager {
	public static final int NEVER_UPDATED = -1;
	
	public static long[] updatedTimestamps = new long[FPLService.NUM_PROCS];
	
	public static long[] prev = new long[FPLService.NUM_PROCS];
	public static long[] next = new long[FPLService.NUM_PROCS];
	
	public static boolean[] fresh = new boolean[FPLService.NUM_PROCS];
	public static String[] error = new String[FPLService.NUM_PROCS];
	
	// load existing updated timestamps from db, and set fresh flags
	public static void initData() {
		//App.log("UpdateManager::initData()");
		for (int i=0; i<FPLService.NUM_PROCS; i++) {
			updatedTimestamps[i] = NEVER_UPDATED;
		}
		
		Cursor cur = App.dbGen.rawQuery("SELECT data, updated_datetime FROM updated WHERE updated_datetime IS NOT NULL", null);
		cur.moveToFirst();
		while(!cur.isAfterLast()) {
			int id = cur.getInt(0);
			updatedTimestamps[id] = cur.getLong(1);
			cur.moveToNext();
		}
		cur.close();
		
		setStaleTimes();
		setFlags();
		
		UpdateManager.resetAlarm();
	}
	
	public static void setFlags() {
		for (int i=0; i<FPLService.NUM_PROCS; i++) {
			setFlag(i);
		}
	}
	
	public static void setStaleTimes() {
		for (int i=0; i<FPLService.NUM_PROCS; i++) {
			setStale(i);
		}
	}
	
	private static final int ONE_DAY = 24 * 60 * 60;
	// 5am
	private static final int PLAYERS_UPDATE_TIME = 5 * 60 * 60;
	// time for BPs to arrive?
	private static final int SCORES_DAILY_UPDATE_TIME = 12 * 60 * 60;
	
	// set prev/next datetimes for daily event
	private static void setDatesDaily(int type) {
		long midnight = App.ukToUtc(App.ukPrevMidnight());
		prev[type] = midnight + PLAYERS_UPDATE_TIME;
		next[type] = midnight + ONE_DAY + PLAYERS_UPDATE_TIME;
		// if not reached update time yet, go back
		if (App.ukToUtc(App.currentUkTime()) < prev[type]) {
			prev[type] -= ONE_DAY;
			next[type] -= ONE_DAY;
		}
		
		// also needs to happen on GW boundaries
		if (prev[type] < App.ukToUtc(App.last_deadline)) {
			prev[type] = App.ukToUtc(App.last_deadline);
		}
		if (App.ukToUtc(App.next_deadline) < next[type]) {
			next[type] = App.ukToUtc(App.next_deadline);
		}
	}
	private static void setDatesDailyScores(int type) {
		long midnight = App.ukToUtc(App.ukPrevMidnight());
		prev[type] = midnight + SCORES_DAILY_UPDATE_TIME;
		next[type] = midnight + ONE_DAY + SCORES_DAILY_UPDATE_TIME;
		// if not reached update time yet, go back
		if (App.ukToUtc(App.currentUkTime()) < prev[type]) {
			prev[type] -= ONE_DAY;
			next[type] -= ONE_DAY;
		}
	}
	
	// set prev/next datetimes for event every gw
	private static void setDatesGW(int type) {
		prev[type] = App.ukToUtc(App.last_deadline);
		next[type] = App.ukToUtc(App.next_deadline);
	}
	
	private static void setStale(int type) {
		//App.log("setStale: " + type + " (" + App.buttonLabel[type] + ")");
		switch (type) {
			case FPLService.PROC_SCRAPE_PLAYERS:
				int players_pref = Settings.getIntPref(Settings.PREF_UPDATE_PLAYERS);
				if (players_pref == Settings.UPDATE_PLAYERS_DAILY) {
					setDatesDaily(type);
				} else if (players_pref == Settings.UPDATE_PLAYERS_GW) {
					setDatesGW(type);
				}
				break;
			case FPLService.PROC_SCRAPE_SQUADS:
				int squads_pref = Settings.getIntPref(Settings.PREF_UPDATE_SQUADS);
				if (squads_pref == Settings.UPDATE_SQUADS_GW) {
					setDatesGW(type);
				}
				break;
			case FPLService.PROC_SCRAPE_FIXTURES:
				// deadlines.. (this is all season fixture scrape)
				setDatesGW(type);
				break;
			case FPLService.PROC_PROCESS_DATA:
				// after gw deadline (to fill in tickers etc)
				setDatesGW(type);
				break;
			case FPLService.PROC_SCRAPE_SCORES:
				int scores_pref = Settings.getIntPref(Settings.PREF_UPDATE_SCORES);
				if (scores_pref > 0) {
					int update_every_x_seconds = scores_pref * 60;
					long utcTime = App.currentUTCTime();
					long ukTime = App.currentUkTime();
					// default
					setDatesDailyScores(type);
					
					GWFixture next_fix = null;
					
					// find which fixture is the earliest that hasn't yet completed
					for (GWFixture f : App.gwFixtures.values()) {
						if (!f.complete) {
							if ( (next_fix == null) || (f.kickoff_datetime < next_fix.kickoff_datetime) ) {
								next_fix = f;
							}
						}
					}
					
					// at least one fixture left..
					if (next_fix != null) {
						// match started
						if (next_fix.kickoff_datetime <= ukTime) {
							// updated recently enough
							if (updatedTimestamps[type] > (utcTime - update_every_x_seconds) ) {
								//App.log("A (alarm) prev[scores]: " + prev[type] + " (" + App.printDate(prev[type]) + ") -> " + updatedTimestamps[type] + " (" + App.printDate(updatedTimestamps[type]) + ")");
								prev[type] = updatedTimestamps[type];
							} else {
								//App.log("B (alarm) prev[scores]: " + prev[type] + " (" + App.printDate(prev[type]) + ") -> " + utcTime + " (" + App.printDate(utcTime) + ")");
								prev[type] = utcTime;
							}
							next[type] = prev[type] + update_every_x_seconds;
						// match not started
						} else {
							long nextKO = App.ukToUtc(next_fix.kickoff_datetime);
							if (nextKO < next[type]) {
								next[type] = nextKO;
							}
						}
					}
				}
				break;
		}
		
		App.log("setStale: " + type + " (" + App.buttonLabel[type] + ") prev: " + App.printDate(prev[type]) + " next: " + App.printDate(next[type]));
	}
	
	public static void resetAlarm() {
		//App.log("reset alarm... checking procs. current alarm: " + App.printDate(alarmTime));
		long utcTime = App.currentUTCTime();
		
		for (int proc = 0; proc < FPLService.NUM_PROCS; proc++) {
			if ( (nextEvent == 0) || (next[proc] < nextEvent) ) {
				if (next[proc] > utcTime) {
					// + 1 to make sure that event starts when alarm fires
					nextEvent = next[proc] + 1;
					//App.log("alarm nextEvent:" + App.buttonLabel[proc]);
				}
			}
		} // loop
		
		if ( (alarmTime == 0) || (nextEvent < alarmTime) || (utcTime > alarmTime) ) {
			if (alarmTime != nextEvent) {
				//App.log("alarm " + alarmTime + " -> " + nextEvent);
				alarmTime = nextEvent;
				setAlarm();
			}
		}
	}
	
	private static void setAlarm() {
		// only set alarms if both auto-update and wake-up are enabled
		if (Settings.getBoolPref(Settings.PREF_WAKE) && Settings.getBoolPref(Settings.PREF_UPDATE_DATA)) {
			App.log("Setting alarm for " + App.printDate(alarmTime) + "(" + alarmTime + ")");
			
			/*if (App.appContext == null) {
				App.log("null app context  alarm error UpdateManager");
				return;
			}*/
			
			AlarmManager am = (AlarmManager) App.appContext.getSystemService(Context.ALARM_SERVICE);
			
			// unset existing alarm
			if (alarmSet != null) {
				App.log("Cancelling previous alarm");
				am.cancel(alarmSet);
				alarmSet.cancel();
			}
			
			Intent intent = new Intent(DiffBroadcastReceiver.ACTION_ALARM);
			alarmSet = PendingIntent.getBroadcast(App.appContext, R.drawable.icon_bonus, intent, 0);
	
			long timeMillis = alarmTime * 1000;
			am.set(AlarmManager.RTC_WAKEUP, timeMillis, alarmSet); 
		}
	}
	
	public static long nextEvent = 0;
	public static long alarmTime = 0;
	public static PendingIntent alarmSet;
	
	private static void setFlag(int type) {
		//App.log("setFlag: " + type + " (" + App.buttonLabel[type] + ")");
		
		// type fresh to true if auto-update disabled for this data type
		switch (type) {
			case FPLService.PROC_SCRAPE_PLAYERS:
				if (Settings.getIntPref(Settings.PREF_UPDATE_PLAYERS) == Settings.UPDATE_PLAYERS_NEVER) {
					//App.log("no auto: default fresh");
					fresh[type] = true;
					return;
				}
				break;
			case FPLService.PROC_SCRAPE_SCORES:
				if (Settings.getIntPref(Settings.PREF_UPDATE_SCORES) == Settings.UPDATE_SCORES_NEVER) {
					//App.log("no auto: default fresh");
					fresh[type] = true;
					return;
				}
				break;
			case FPLService.PROC_SCRAPE_SQUADS:
				if (Settings.getIntPref(Settings.PREF_UPDATE_SQUADS) == Settings.UPDATE_SQUADS_NEVER) {
					//App.log("no auto: default fresh");
					fresh[type] = true;
					return;
				}
				break;
			case FPLService.PROC_PROCESS_DATA:
				//App.log("no auto: default fresh");
				// Done anyway when auto sync is disabled, so always default to not fresh
				break;
		}
		
		long timestamp = updatedTimestamps[type];
		if (timestamp == NEVER_UPDATED) {
			//App.log("never updated: fresh = false");
			fresh[type] = false;
		} else {
			// if we are past next milestone, recalculate milestones
			// (do this first, so new milestones are used below..)
			long now = App.currentUTCTime();
			if (now > next[type]) {
				setStale(type);
				resetAlarm();
			}
			
			// if data updated since last milestone, is ok
			fresh[type] = false;
			//App.log("prev: " + App.printDate(prev[type]) + " ts: " + App.printDate(timestamp)
			//		    + " next: " + App.printDate(next[type]));
			if (timestamp >= prev[type]) fresh[type] = true;
		}
		
		//App.log(" fresh " + type + " (" + App.buttonLabel[type] + "): " + fresh[type]);
	}
	
	private static final String t_updated = "updated";
	private static final String f_data = "data";
	private static final String f_updated_datetime = "updated_datetime";
	
	public static void updatedData(int type) {
		// stored as UTC.
		// converted to local on display
		long timestamp = App.currentUTCTime();
		ContentValues upd = new ContentValues();
		upd.put(f_data, type);
		upd.put(f_updated_datetime, timestamp);
		App.dbGen.replace(t_updated, null, upd);
		
		updatedTimestamps[type] = timestamp;
		
		setFlag(type);
		
		resetAlarm();
	}
	
}
