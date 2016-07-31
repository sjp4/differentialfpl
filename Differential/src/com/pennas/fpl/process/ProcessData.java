package com.pennas.fpl.process;

import java.util.ArrayList;
import java.util.HashMap;
import com.pennas.fpl.App;
import com.pennas.fpl.process.FPLService.ProcRes;
import com.pennas.fpl.process.ProcessFixture.Fixture;

import android.database.sqlite.SQLiteDatabase;

public class ProcessData {
	static final int form_num_games = 4;
	public static final int next_x_games = 6;
	static final float form_thresh = 1.15f; // if form is this factor over rating, gpg boosted
	static final float form_gpg_boost = 1.15f;
	static final int x_minutes = 60;
	static final float thrashing_thresh = 2.7f;
	static final float thrashing_boost = 1.25f;
	static final float thrashing_2thresh = 5;
	static final float thrashing_2boost = 1.3f;
	static final float draw_factor = 0.73f;
	static final float clean_sheet_thresh = 0.9f;
	static final int recent_mins_thresh = 45;
	static final int num_gameweeks = 38;
	public static final int y_points = 5;
	static final int max_cs_pts = 4;
	public static final int z_points = 10;
	static final int diff_hard = 1;
	static final float diff_med = 2.5f;
	
	int season;
	
	int gameweek;
	int next_gameweek;
	
	SQLiteDatabase dbGen = App.dbGen;
	
	HashMap<Integer, TeamStat> teamStats = new HashMap<Integer, TeamStat>();
	
	public void processSeason(ProcRes result, int p_season) {
		ProcessPlayer.processPlayer(this, p_season, App.num_gameweeks, result);
		
		result.messages.add("process all seasons");
	}
	
	public void processData(ProcRes result) {
		//Debug.startMethodTracing("process_all", 15000000);
		App.log("process data: " + App.cur_gameweek);
		season = App.season;
		gameweek = App.cur_gameweek;
		next_gameweek = App.next_gameweek;
		
		result.internalProgressTo = 10;
		ProcessTeam.processTeam(this, result);
		
		result.internalProgressFrom = 10;
		result.internalProgressTo = 20;
		ProcessFixture.processFixture(this, result);
		
		result.internalProgressFrom = 20;
		result.internalProgressTo = 85;
		ProcessPlayer.processPlayer(this, season, gameweek, result);
		
		result.internalProgressFrom = 85;
		result.internalProgressTo = 95;
		ProcessTeam.postProcessTeam(this, result);
		
		result.internalProgressFrom = 95;
		result.internalProgressTo = 99;
		ProcessSquadGeneric.processGeneric(this, result);
		
		try {
			App.dbGen.execSQL("vacuum");
		} catch (Exception e) {
			App.exception(e);
		}
		
		//Debug.stopMethodTracing();
		result.messages.add("process data");
	}
	
	static float trunc(float x) {
		return trunc(x, 2);
	}
	public static float trunc(float x, int dp) {
		double f = Math.pow(10, dp);
		return (float) (Math.round(x*f)/f);
	}
	
	static class TeamStat {
		public float c_h_gpg;
		public float c_h_gcpg;
		public float c_a_gpg;
		public float c_a_gcpg;
		public float c_h_rating;
		public float c_a_rating;
		public float c_h_form_rating;
		public float c_a_form_rating;
		public float res_num_games_rec;
		public ArrayList<Fixture> fixtures;
	}
}
