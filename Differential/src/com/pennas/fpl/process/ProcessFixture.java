package com.pennas.fpl.process;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.pennas.fpl.App;
import com.pennas.fpl.process.FPLService.ProcRes;
import com.pennas.fpl.process.ProcessData.TeamStat;
import com.pennas.fpl.util.DbGen;

public class ProcessFixture {
	private static String fixture_id;
	
	// fixtures
	private static class Field_fix {
		private static final String f_pred_goals_home = "pred_goals_home";
		private static final String f_pred_goals_away = "pred_goals_away";
		private static final String f_pred_ratio_home = "pred_ratio_home";
		private static final String f_pred_ratio_away = "pred_ratio_away";
		private static final String f_pred_points_home = "pred_points_home";
		private static final String f_pred_points_away = "pred_points_away";
	}
	
	private static void checkFixDiff(float newVal, String field, Cursor cur) {
		if (App.checkdiff) {
			float f = cur.getFloat(cur.getColumnIndex(field));
			if (f != newVal) App.log("fixture " + fixture_id + "... " + field + ": " + f + " -> " + newVal);
		}
	}
	
	static void processFixture(ProcessData parent, ProcRes result) {
		SQLiteDatabase db = parent.dbGen;
		int season = parent.season;
		
		App.log("Processing fixtures..");
		
		long startTime = System.currentTimeMillis();
		int home_id;
		int away_id;
		float home_final_rating;
		float away_final_rating;
		float ratio_away;
		float ratio_home;
		float ratio_diff;
		float c_pred_home_goals;
		float c_pred_away_goals;
		int c_pred_home_points;
		int c_pred_away_points;
		TeamStat home;
		TeamStat away;
		ContentValues updateVals = new ContentValues();
		final String t_fixture = "fixture";
		final String t_where = "_id = ?";
		String[] t_wArgs = { "0" };
		
		Cursor curFix = db.rawQuery("SELECT * FROM fixture WHERE season = " + season + " AND res_goals_home IS NULL ORDER BY datetime ASC", null);
		curFix.moveToFirst();
		float fixCount = curFix.getCount();
		float i=0;
		
		result.setMaxName(fixCount, "ProcFix");
		
		db.execSQL(DbGen.TRANSACTION_IMMEDIATE_BEGIN);
		
		int fixture_id_ind = curFix.getColumnIndex("_id");
		int home_id_ind = curFix.getColumnIndex("team_home_id");
		int away_id_ind = curFix.getColumnIndex("team_away_id");
		int datetime_ind = curFix.getColumnIndex("datetime");
		int gw_ind = curFix.getColumnIndex("gameweek");
		
		while (!curFix.isAfterLast()) {
			fixture_id = curFix.getString(fixture_id_ind);
			home_id = curFix.getInt(home_id_ind);
			away_id = curFix.getInt(away_id_ind);
			
			home = parent.teamStats.get(home_id);
			away = parent.teamStats.get(away_id);
			
			if ( (home != null) && (away != null) ) {
				home_final_rating = (home.c_h_rating + home.c_h_form_rating) / 2;
				away_final_rating = (away.c_a_rating + away.c_a_form_rating) / 2;
				
				// predicted goals
				c_pred_home_goals = (home.c_h_gpg + away.c_a_gcpg) / 2;
				c_pred_away_goals = (home.c_h_gcpg + away.c_a_gpg) / 2;
				
				// boost for thrashing (home)
				ratio_home = ProcessData.trunc(home_final_rating / away_final_rating);
				if (ratio_home > ProcessData.thrashing_thresh) {
					c_pred_home_goals = c_pred_home_goals * ProcessData.thrashing_boost;
					//c_pred_away_goals = c_pred_away_goals * (1/ProcessData.thrashing_boost);
				}
				if (ratio_home > ProcessData.thrashing_2thresh) {
					c_pred_home_goals = c_pred_home_goals * ProcessData.thrashing_2boost;
					c_pred_away_goals = c_pred_away_goals * (1/ProcessData.thrashing_2boost);
				}
				// away
				ratio_away = ProcessData.trunc(away_final_rating / home_final_rating);
				if (ratio_away > ProcessData.thrashing_thresh) {
					//c_pred_home_goals = c_pred_home_goals * (1/ProcessData.thrashing_boost);
					c_pred_away_goals = c_pred_away_goals * ProcessData.thrashing_boost;
				}
				if (ratio_away > ProcessData.thrashing_2thresh) {
					c_pred_home_goals = c_pred_home_goals * (1/ProcessData.thrashing_2boost);
					c_pred_away_goals = c_pred_away_goals * ProcessData.thrashing_2boost;
				}
				c_pred_home_goals = ProcessData.trunc(c_pred_home_goals);
				c_pred_away_goals = ProcessData.trunc(c_pred_away_goals);
				
				// calculate result
				ratio_diff = Math.abs(ratio_home - ratio_away);
				if (ratio_diff < ProcessData.draw_factor) {
					c_pred_home_points = 1;
					c_pred_away_points = 1;
				} else if (ratio_home > ratio_away) {
					c_pred_home_points = 3;
					c_pred_away_points = 0;
				} else {
					c_pred_home_points = 0;
					c_pred_away_points = 3;
				}
				
				int val = (int) (c_pred_home_goals * 100);
				updateVals.put(Field_fix.f_pred_goals_home, val);
				checkFixDiff(val, Field_fix.f_pred_goals_home, curFix);
				val = (int) (c_pred_away_goals * 100);
				updateVals.put(Field_fix.f_pred_goals_away, val);
				checkFixDiff(val, Field_fix.f_pred_goals_away, curFix);
				
				val = (int) (ratio_home * 100);
				updateVals.put(Field_fix.f_pred_ratio_home, val);
				checkFixDiff(val, Field_fix.f_pred_ratio_home, curFix);
				val = (int) (ratio_away * 100);
				updateVals.put(Field_fix.f_pred_ratio_away, val);
				checkFixDiff(val, Field_fix.f_pred_ratio_away, curFix);
				
				updateVals.put(Field_fix.f_pred_points_home, c_pred_home_points);
				checkFixDiff(c_pred_home_points, Field_fix.f_pred_points_home, curFix);
				updateVals.put(Field_fix.f_pred_points_away, c_pred_away_points);
				checkFixDiff(c_pred_away_points, Field_fix.f_pred_points_away, curFix);
				
				t_wArgs[0] = fixture_id;
				db.update(t_fixture, updateVals, t_where, t_wArgs);
				
				// add fixture to each team hash
				Fixture f = new Fixture();
				f.datetime = curFix.getLong(datetime_ind);
				f.gameweek = curFix.getInt(gw_ind);
				f.fix_id = fixture_id;
				f.pred_goals_against = c_pred_away_goals;
				f.pred_goals_for = c_pred_home_goals;
				f.team_opp_id = away_id;
				f.team_pl_id = home_id;
				f.home = true;
				f.ratio = ratio_home;
				f.pred_points = c_pred_home_points;
				home.fixtures.add(f);
				
				Fixture g = new Fixture();
				g.datetime = f.datetime;
				g.gameweek = f.gameweek;
				g.fix_id = fixture_id;
				g.pred_goals_against = c_pred_home_goals;
				g.pred_goals_for = c_pred_away_goals;
				g.team_opp_id = home_id;
				g.team_pl_id = away_id;
				g.home = false;
				g.ratio = ratio_away;
				g.pred_points = c_pred_away_points;
				away.fixtures.add(g);
			}
			
			curFix.moveToNext();
			i++;
			
			result.setProgress(i);
		} // team loop
		
		try {
			db.execSQL(DbGen.TRANSACTION_END);
		} catch (SQLException e) {
			App.exception(e);
		}
		
		curFix.close();
		
		//Debug.stopMethodTracing();
		Long timeDiff = System.currentTimeMillis() - startTime;
		float timeSecs = (float)timeDiff / 1000;
		App.log("Finished processing " + i + " fixtures in " + timeSecs + " seconds");
	}
	
	static class Fixture {
		public String fix_id;
		public int    team_opp_id;
		public int    team_pl_id;
		public float  pred_goals_for;
		public float  pred_goals_against;
		public int    gameweek;
		public long   datetime;
		public boolean home;
		public float  ratio;
		public int    pred_points;
	}
	
}
