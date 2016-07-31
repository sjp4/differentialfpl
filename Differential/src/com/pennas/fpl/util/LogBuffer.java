package com.pennas.fpl.util;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Activity;

import com.crashlytics.android.Crashlytics;
import com.pennas.fpl.App;
import com.pennas.fpl.App.GWFixture;
import com.pennas.fpl.Settings;
import com.pennas.fpl.process.FPLService;
import com.pennas.fpl.process.UpdateManager;

public class LogBuffer {
	private static final int MAX_ENTRIES = 800;
	
	private ConcurrentLinkedQueue<String> buffer = new ConcurrentLinkedQueue<String>();
	private int size = 0;
	private long lastentrytime = 0;
	private SimpleDateFormat formatDate;
	
	public LogBuffer() {
		formatDate = new SimpleDateFormat("EEE d MMM HH:mm:ss");
		formatDate.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	public void add(String message) {
		if (size >= MAX_ENTRIES) {
			buffer.poll();
			size--;
		}
		long sysTime = System.currentTimeMillis() / 1000;
		if (sysTime != lastentrytime) {
			lastentrytime = sysTime;
			buffer.add("<" + formatDate.format(App.currentUkTime() * 1000) + "> " + message);
		} else {
			buffer.add(message);
		}
		size++;
	}
	
	public String dump() {
		StringBuffer ret = new StringBuffer();
		boolean first = true;
		for (String s : buffer) {
			ret.append(s);
			if (first) {
				first = false;
			} else {
				ret.append("\n");
			}
		}
		
		return ret.toString();
	}
	
	public String generateDiags() {
		StringBuffer diag = new StringBuffer();
		
		diag.append("UTC: " + App.printDate(App.currentUTCTime()));
		diag.append("\n");
		diag.append("Local: " + App.printDate(App.utcToLocal(App.currentUTCTime())));
		diag.append("\n");
		diag.append("UK: " + App.printDate(App.currentUkTime()));
		
		diag.append("\n");
		for (int proc=0; proc<FPLService.NUM_PROCS; proc++) {
			diag.append("\n");
			diag.append("proc: " + proc + " (" + App.buttonLabel[proc]
			         + ") updated: " + App.printDate(UpdateManager.updatedTimestamps[proc])
			         + " prev: " + App.printDate(UpdateManager.prev[proc])
			         + " next: " + App.printDate(UpdateManager.next[proc])
			         + " fresh: " + UpdateManager.fresh[proc]);
		}
		
		diag.append("\n");
		diag.append("installed_on_sd: " + App.installed_on_sd);
		diag.append("\n");
		diag.append("cur_gameweek: " + App.cur_gameweek);
		diag.append("\n");
		diag.append("gw_score: " + App.gw_score);
		diag.append("\n");
		diag.append("total_score: " + App.total_score);
		diag.append("\n");
		diag.append("my_squad_id: " + App.my_squad_id);
		diag.append("\n");
		diag.append("logged_in: " + App.logged_in);
		diag.append("\n");
		diag.append("FPLService.cycles: " + FPLService.cycles);
		diag.append("\n");
		diag.append("FPLService.fails: " + FPLService.fails);
		diag.append("\n");
		diag.append("FPLService.sincefail: " + FPLService.sincefail);
		diag.append("\n");
		diag.append("Alarm: " + App.printDate(UpdateManager.alarmTime));
		
		diag.append("\n\nGW Fixtures:-");
		if (App.gwFixtures != null) {
			for (GWFixture f : App.gwFixtures.values()) {
				diag.append("\n");
				diag.append("id: " + f.id + " kickoff_datetime: " + App.printDate(f.kickoff_datetime)
						 + " team_away_id: " + f.team_away_id + " score: " + f.goals_home
						 + " team_home_id: " + f.team_home_id + " score: " + f.goals_away  + " complete: " + f.complete);
			}
		}
		
		diag.append("\n\nMy Squad Cur: ");
		boolean first = true;
		if (App.mySquadCur != null) {
			for (int p : App.mySquadCur) {
				if (first) {
					first = false;
				} else {
					diag.append(" / ");
				}
				diag.append(p);
			}
		}
		
		diag.append("\n\n");
		diag.append("User Comments:-\n");
		diag.append(user_feedback);
		
		diag.append("\n\n");
		diag.append("Log Entries:-\n");
		diag.append(dump());
		
		String diag_string = diag.toString();
		diag_string.replace(",", ";");
		
		if (!diag_mode) {
			App.setFailed(true, App.appContext);
			App.log("crash mode");
		} else {
			App.log("diags mode");
		}
		
		return diag_string;
	}
	
	private String user_feedback;
	private boolean diag_mode = false;
	
	public void createDiagnostics(String feedback) {
		user_feedback = feedback;
		diag_mode = true;
		//ErrorReporter.getInstance().handleSilentException(new Throwable("Diagnostics"));
		Crashlytics.logException(new Throwable("Diagnostics: " + feedback));
		diag_mode = false;
	}
	
	public void sendDiagnostics(Activity c) {
		c.showDialog(Settings.DIALOG_DIAG);
	}
}
