package com.pennas.fpl.process;

import java.util.HashMap;
import java.util.Iterator;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.pennas.fpl.App;
import com.pennas.fpl.Players;
import com.pennas.fpl.Stats;
import com.pennas.fpl.Players.Stat;
import com.pennas.fpl.process.FPLService.ProcRes;
import com.pennas.fpl.process.ProcessData.TeamStat;
import com.pennas.fpl.process.ProcessFixture.Fixture;
import com.pennas.fpl.util.DbGen;

public class ProcessPlayer {
	// player
	private static class Field_player {
		private static final String f_c_team_goals = "c_team_goals";
		private static final String f_c_ppg_home = "c_ppg_home";
		private static final String f_c_ppg_away = "c_ppg_away";
		private static final String f_pred_season_pts = "pred_season_pts";
		private static final String f_c_ppg_x_mins_home = "c_ppg_x_mins_home";
		private static final String f_c_ppg_x_mins_away = "c_ppg_x_mins_away";
		private static final String f_c_ppg_x_mins = "c_ppg_x_mins";
		private static final String f_c_ppg_x_mins_rec = "c_ppg_x_mins_rec";
		private static final String f_c_games_left = "c_games_left";
		//private static final String f_unavailable = "unavailable";
		private static final String f_diff_flag = "diff_flag";
		private static final String f_ticker_string = "ticker_string";
		private static final String f_pred_left_goals = "pred_left_goals";
		private static final String f_pred_left_assists = "pred_left_assists";
		private static final String f_pred_left_bonus_pts = "pred_left_bonus_pts";
		private static final String f_pred_left_cs_pts = "pred_left_cs_pts";
		private static final String f_pred_left_pts = "pred_left_pts";
		private static final String f_c_games_x_mins = "c_games_x_mins";
		private static final String f_c_games_recent = "c_games_recent";
		private static final String f_c_games_avl_recent = "c_games_avl_recent";
		private static final String f_c_gp90 = "c_gp90";
		private static final String f_c_ap90 = "c_ap90";
		private static final String f_c_diff = "c_diff";
		private static final String f_c_emerge = "c_emerge";
		private static final String f_c_sum_error = "c_sum_error";
		private static final String f_c_bonus_per_game = "c_bonus_per_game";
		private static final String f_c_goals_perc_team = "c_goals_perc_team";
		private static final String f_c_assists_perc_team = "c_assists_perc_team";
		private static final String f_pred_left_value = "pred_left_value";
		private static final String f_c_avg_mins_recent = "c_avg_mins_recent";
		private static final String f_diff_upcom_fix = "diff_upcom_fix";
		private static final String f_diff_upcom_pred = "diff_upcom_pred";
		private static final String f_diff_value_fix = "diff_value_fix";
		private static final String f_diff_value_pred = "diff_value_pred";
		private static final String f_c_cs_perc_games = "c_cs_perc_games";
		private static final String f_c_bonus_per_win = "c_bonus_per_win";
		private static final String f_c_bonus_per_draw= "c_bonus_per_draw";
		private static final String f_c_bonus_per_loss = "c_bonus_per_loss";
		private static final String f_c_points_per_win = "c_points_per_win";
		private static final String f_c_points_per_draw= "c_points_per_draw";
		private static final String f_c_points_per_loss = "c_points_per_loss";
		private static final String f_c_perc_games_won = "c_perc_games_won";
		private static final String f_c_pp90 = "c_pp90";
		private static final String f_c_pp90_home = "c_pp90_home";
		private static final String f_c_pp90_away = "c_pp90_away";
		private static final String f_c_g_over_y = "c_g_over_y";
		private static final String f_c_g_over_z = "c_g_over_z";
		private static final String f_c_ppg_easy = "c_ppg_easy";
		private static final String f_c_ppg_med = "c_ppg_med";
		private static final String f_c_ppg_hard = "c_ppg_hard";
		private static final String f_c_value_ppg_x = "c_value_ppg_x";
		private static final String f_c_value_pp90 = "c_value_pp90";
		private static final String f_c_flat_track_bully = "c_flat_track_bully";
		private static final String f_c_fixture_proof = "c_fixture_proof";
		private static final String f_c_highest_score = "c_highest_score";
		private static final String f_fpl_id = "fpl_id";
		private static final String f_player_id = "player_id";
		private static final String f_season = "season";
		private static final String f_position = "position";
		private static final String f_goals = "goals";
		private static final String f_assists = "assists";
		private static final String f_bonus = "bonus";
		private static final String f_points = "points";
		private static final String f_minutes = "minutes";
		private static final String f_minutes_qual = "minutes_qual";
		private static final String f_c_ea_g_x = "c_ea_g_x";
		private static final String f_c_value_diff = "c_value_diff";
		private static final String f_clean_sheets = "clean_sheets";
	}
	
	// player match
	private static class Field_player_match {
		private static final String f_season = "season";
		//private static final String f_player_fpl_id = "player_fpl_id";
		private static final String f_player_player_id = "player_player_id";
		private static final String f_fixture_id = "fixture_id";
		private static final String f_opp_team_id = "opp_team_id";
		private static final String f_pl_team_id = "pl_team_id";
		private static final String f_gameweek = "gameweek";
		private static final String f_is_home = "is_home";
		private static final String f_pred_total_pts = "pred_total_pts";
	}	
	
	private static void checkPlDiff(float newVal, String field, Cursor cur) {
		if (App.checkdiff) {
			float f = cur.getFloat(cur.getColumnIndex(field));
			if (f != newVal) App.log("player: " + player_fpl_id_use_sparingly + "... " + field + ": " + f + " -> " + newVal);
		}
	}
	
	private static int player_fpl_id_use_sparingly;
	private static int player_player_id;
	public static final int SEASON_FULL_START = 11;
	public static final int ALL_SEASONS = 0;
	private static final int FIRST_SEASON = 7;
	private static final int EMERGE_MINS = 5 * 90;
	
	private static int gameweek;
	private static final int PROGRESS_STARTAT = 70;
	
	private static final float MAX_FORM = 1.75f;
	private static final float MIN_FORM = 0.45f;
	private static final int USE_FULL_FORM_FOR_GWS = 3;
	
	/*
	private static final int[] debug_players = { 178, 574, 638, 27, 764, 372, 384, 256, 79 };
	private static HashMap<Integer, String> playerNames = new HashMap<Integer, String>();
	private static void debug(String s, int player) {
		for (int i=0; i<debug_players.length; i++) {
			if (player == debug_players[i]) {
				String name = playerNames.get(player);
				if (name == null) {
					Cursor cur = App.dbGen.rawQuery("SELECT name FROM player WHERE _id = " + player, null);
					cur.moveToFirst();
					name = cur.getString(0);
					cur.close();
				}
				App.log(name + ": " + s);
			}
		}
	}
	*/
	
	static void processPlayer(ProcessData parent, int season_proc, int p_gw, ProcRes result) {
		SQLiteDatabase db = parent.dbGen;
		gameweek = p_gw;
		boolean current_season = false;
		boolean full_proc = false;
		boolean all_seasons = false;
		if (season_proc == App.season) current_season = true;
		if ( (season_proc >= SEASON_FULL_START) || (season_proc == ALL_SEASONS) ) full_proc = true;
		if (season_proc == ALL_SEASONS) all_seasons = true;
		
		int season_from = season_proc;
		int season_to = season_proc;
		if (season_proc == ALL_SEASONS) {
			season_from = FIRST_SEASON;
			season_to = App.season;
		}
		
		App.log("Processing players.. season: " + season_proc + " gameweek: " + gameweek + " (from season: " + season_from + " to season: " + season_to + ")");
		//Debug.startMethodTracing("players2", 15000000);
		
		long startTime = System.currentTimeMillis();
		
		ContentValues updateVals = new ContentValues();
		ContentValues updateValsP = new ContentValues();
		ContentValues updatePM = new ContentValues();
		final String t_player_season = "player_season";
		final String t_player = "player";
		final String t_player_match = "player_match";
		final String t_where_player_season = "player_id = ? AND season = " + season_proc;
		final String t_where_player = "_id = ?";
		String[] t_wArgs = { "0" };
		TeamStat team;
		
		// funky join....
		// out on pso player_season gets the values for change comparison (specially for ALL_SEASONS)
		// where ps gets this season's basics for general use. they will usually be the same season
		Cursor curPl = db.rawQuery("SELECT pso.*, p.team_id nn_team_id, p.fpl_red_flag nn_fpl_red_flag, p._id nn_player_id, ps.fpl_id nn_fpl_id, ps.position nn_position, ps.price nn_price"
					+ " FROM player_season ps, player p LEFT OUTER JOIN player_season pso ON pso.season = " + season_proc + " AND pso.player_id = ps.player_id"
					+ " WHERE ps.season = " + season_to + " AND ps.player_id = p._id ", null);
		curPl.moveToFirst();
		float plCount = curPl.getCount();
		float i=0;
		
		result.setMaxName(plCount + PROGRESS_STARTAT, "ProcPlayer");
		
		App.log("a");
		long indTime = System.currentTimeMillis();
		
		// create mapping from player to basics row
		HashMap<Integer, Integer> basicsHash = new HashMap<Integer, Integer>();
		Cursor curBasics = db.rawQuery("SELECT player_id, SUM(goals) goals, SUM(assists) assists, SUM(bonus) bonus, SUM(minutes) minutes, SUM(points) points, SUM(clean_sheets) clean_sheets, SUM(ea_ppi) ea_ppi FROM player_season WHERE season >= " + season_from + " AND season <= " + season_to + " GROUP BY player_id", null);
		curBasics.moveToFirst();
		int p = 0;
		while (!curBasics.isAfterLast()) {
			basicsHash.put(curBasics.getInt(0), p);
			curBasics.moveToNext();
			p++;
		}
		
		result.setProgress(5);
		long indDiff = System.currentTimeMillis() - indTime;
		indTime = System.currentTimeMillis();
		App.log("a2: " + indDiff);
		
		
		// create mapping from player to sum row
		HashMap<Integer, Integer> sumHash = new HashMap<Integer, Integer>();
		Cursor curSum = db.rawQuery("SELECT player_player_id, SUM(team_goals_on_pitch) c_team_goals, SUM(goals) c_sum_goals, SUM(assists) c_sum_assists, SUM(bonus) c_sum_bonus, SUM(minutes) c_sum_minutes, SUM(total) c_sum_points FROM player_match WHERE season >= " + season_from + " AND season <= " + season_to + " GROUP BY player_player_id", null);
		curSum.moveToFirst();
		p = 0;
		while (!curSum.isAfterLast()) {
			sumHash.put(curSum.getInt(0), p);
			curSum.moveToNext();
			p++;
		}
		
		result.setProgress(10);
		indDiff = System.currentTimeMillis() - indTime;
		indTime = System.currentTimeMillis();
		App.log("b: " + indDiff);
		
		// home stats
		HashMap<Integer, Integer> homeHash = new HashMap<Integer, Integer>();
		Cursor curHome = db.rawQuery("SELECT player_player_id, SUM(total) res_points_home, COUNT(*) res_games_home, SUM(minutes) res_minutes_home FROM player_match WHERE season >= " + season_from + " AND season <= " + season_to + " AND is_home = 1 AND minutes > 0 GROUP BY player_player_id", null);
		curHome.moveToFirst();
		p = 0;
		while (!curHome.isAfterLast()) {
			homeHash.put(curHome.getInt(0), p);
			curHome.moveToNext();
			p++;
		}
		
		result.setProgress(15);
		indDiff = System.currentTimeMillis() - indTime;
		indTime = System.currentTimeMillis();
		App.log("c: " + indDiff);
		
		// away stats
		HashMap<Integer, Integer> awayHash = new HashMap<Integer, Integer>();
		Cursor curAway = db.rawQuery("SELECT player_player_id, SUM(total) res_points_away, COUNT(*) res_games_away, SUM(minutes) res_minutes_away FROM player_match WHERE season >= " + season_from + " AND season <= " + season_to + " AND is_home IS NULL AND minutes > 0 GROUP BY player_player_id", null);
		curAway.moveToFirst();
		p = 0;
		while (!curAway.isAfterLast()) {
			awayHash.put(curAway.getInt(0), p);
			curAway.moveToNext();
			p++;
		}
		
		result.setProgress(20);
		indDiff = System.currentTimeMillis() - indTime;
		indTime = System.currentTimeMillis();
		App.log("d: " + indDiff);
		
		// home x min stats
		HashMap<Integer, Integer> homeXHash = new HashMap<Integer, Integer>();
		Cursor curXHome = db.rawQuery("SELECT player_player_id, SUM(total) res_points_x_home, COUNT(*) res_games_x_home FROM player_match WHERE season >= " + season_from + " AND season <= " + season_to + " AND minutes >= " + ProcessData.x_minutes + " AND is_home = 1 GROUP BY player_player_id", null);
		curXHome.moveToFirst();
		p = 0;
		while (!curXHome.isAfterLast()) {
			homeXHash.put(curXHome.getInt(0), p);
			curXHome.moveToNext();
			p++;
		}
		
		result.setProgress(25);
		indDiff = System.currentTimeMillis() - indTime;
		indTime = System.currentTimeMillis();
		App.log("e: " + indDiff);
		
		// away x min stats
		HashMap<Integer, Integer> awayXHash = new HashMap<Integer, Integer>();
		Cursor curXAway = db.rawQuery("SELECT player_player_id, SUM(total) res_points_x_away, COUNT(*) res_games_x_away FROM player_match WHERE season >= " + season_from + " AND season <= " + season_to + " AND minutes >= " + ProcessData.x_minutes + " AND is_home IS NULL GROUP BY player_player_id", null);
		curXAway.moveToFirst();
		p = 0;
		while (!curXAway.isAfterLast()) {
			awayXHash.put(curXAway.getInt(0), p);
			curXAway.moveToNext();
			p++;
		}
		
		result.setProgress(30);
		indDiff = System.currentTimeMillis() - indTime;
		indTime = System.currentTimeMillis();
		App.log("f: " + indDiff);
		
		int gw_from = gameweek - ProcessData.form_num_games;
		
		// mins/games in last (form) gameweeks
		HashMap<Integer, Integer> formHash = new HashMap<Integer, Integer>();
		Cursor curForm = null;
		if (current_season) {
			curForm = db.rawQuery("SELECT player_player_id, SUM(minutes) total_mins, COUNT(*) total_games FROM player_match WHERE season = " + season_proc + " AND gameweek >= " + gw_from + " AND gameweek <= " + gameweek + " AND minutes > 0 GROUP BY player_player_id", null);
			curForm.moveToFirst();
			p = 0;
			while (!curForm.isAfterLast()) {
				formHash.put(curForm.getInt(0), p);
				curForm.moveToNext();
				p++;
			}
		}
		
		result.setProgress(35);
		indDiff = System.currentTimeMillis() - indTime;
		indTime = System.currentTimeMillis();
		App.log("g: " + indDiff);
		
		// form X
		HashMap<Integer, Integer> formXHash = new HashMap<Integer, Integer>();
		Cursor curXForm = null;
		if (current_season) {
			curXForm = db.rawQuery("SELECT player_player_id, SUM(total) res_points_x_rec, COUNT(*) res_games_x_rec FROM player_match WHERE season = " + season_proc + " AND minutes >= " + ProcessData.x_minutes + " AND gameweek >= " + gw_from + " AND gameweek <= " + gameweek + " GROUP BY player_player_id", null);
			curXForm.moveToFirst();
			p = 0;
			while (!curXForm.isAfterLast()) {
				formXHash.put(curXForm.getInt(0), p);
				curXForm.moveToNext();
				p++;
			}
		}
		
		result.setProgress(40);
		indDiff = System.currentTimeMillis() - indTime;
		indTime = System.currentTimeMillis();
		App.log("h: " + indDiff);
		
		// games over y points
		HashMap<Integer, Integer> yHash = new HashMap<Integer, Integer>();
		Cursor curY = db.rawQuery("SELECT player_player_id, COUNT(*) num_games FROM player_match WHERE season >= " + season_from + " AND season <= " + season_to + " AND total >= " + ProcessData.y_points + " GROUP BY player_player_id", null);
		curY.moveToFirst();
		while (!curY.isAfterLast()) {
			yHash.put(curY.getInt(0), curY.getInt(1));
			curY.moveToNext();
		}
		curY.close();
		
		result.setProgress(45);
		indDiff = System.currentTimeMillis() - indTime;
		indTime = System.currentTimeMillis();
		App.log("j: " + indDiff);
		
		// games over z points
		HashMap<Integer, Integer> zHash = new HashMap<Integer, Integer>();
		Cursor curZ = db.rawQuery("SELECT player_player_id, COUNT(*) num_games FROM player_match WHERE season >= " + season_from + " AND season <= " + season_to + " AND total >= " + ProcessData.z_points + " GROUP BY player_player_id", null);
		curZ.moveToFirst();
		while (!curZ.isAfterLast()) {
			zHash.put(curZ.getInt(0), curZ.getInt(1));
			curZ.moveToNext();
		}
		curZ.close();
		
		result.setProgress(50);
		indDiff = System.currentTimeMillis() - indTime;
		indTime = System.currentTimeMillis();
		App.log("k: " + indDiff);
		
		// win/draw/loss
		HashMap<Integer, PlayerWDL> wdlHash = new HashMap<Integer, PlayerWDL>();
		Cursor curWDL = db.rawQuery("SELECT player_player_id, result_points, COUNT(*) num_games, SUM(total) points, SUM(bonus) bonus FROM player_match WHERE season >= " + season_from + " AND season <= " + season_to + " AND minutes > 0 GROUP BY player_player_id, result_points ORDER BY player_player_id ASC", null);
		curWDL.moveToFirst();
		int pid = -1;
		PlayerWDL plWDL = null;
		// loop stat records (multiple per player)
		while (!curWDL.isAfterLast()) {
			int cur_pid = curWDL.getInt(0);
			// if new player, create object and add to hash
			if (cur_pid != pid) {
				plWDL = new PlayerWDL();
				wdlHash.put(cur_pid, plWDL);
				pid = cur_pid;
			}
			// add data to relevant part of aggregate record
			switch (curWDL.getInt(1)) {
				case 3:
					plWDL.w = curWDL.getFloat(2);
					plWDL.w_pts = curWDL.getFloat(3);
					plWDL.w_bonus = curWDL.getFloat(4);
					break;
				case 1:
					plWDL.d = curWDL.getFloat(2);
					plWDL.d_pts = curWDL.getFloat(3);
					plWDL.d_bonus = curWDL.getFloat(4);
					break;
				case 0:
					plWDL.l = curWDL.getFloat(2);
					plWDL.l_pts = curWDL.getFloat(3);
					plWDL.l_bonus = curWDL.getFloat(4);
					break;
			}
			curWDL.moveToNext();
		}
		curWDL.close();
		
		result.setProgress(55);
		indDiff = System.currentTimeMillis() - indTime;
		indTime = System.currentTimeMillis();
		App.log("l: " + indDiff);
		
		// points v difficulty
		HashMap<Integer, PlayerPointsDiff> pdHash = new HashMap<Integer, PlayerPointsDiff>();
		Cursor curPointDiff = db.rawQuery("SELECT player_player_id, CASE WHEN ratio < " + (ProcessData.diff_hard * 100) + " THEN 0 WHEN ratio >= " + (ProcessData.diff_hard * 100) + " AND ratio < " + (ProcessData.diff_med * 100) + " THEN 1 ELSE 2 END ratio_group, SUM(total) points, COUNT(total) games FROM ( SELECT pm.player_player_id player_player_id, CASE WHEN pm.is_home=1 THEN f.pred_ratio_home ELSE f.pred_ratio_away END ratio, total FROM player_match pm, fixture f WHERE pm.season >= " + season_from + " AND pm.season <= " + season_to + " AND f._id = pm.fixture_id AND pm.minutes > 0 ) GROUP BY player_player_id, ratio_group ORDER BY player_player_id ASC", null);
		curPointDiff.moveToFirst();
		App.log("l+");
		pid = -1;
		PlayerPointsDiff plPD = null;
		// loop stat records (multiple per player)
		while (!curPointDiff.isAfterLast()) {
			int cur_pid = curPointDiff.getInt(0);
			// if new player, create object and add to hash
			if (cur_pid != pid) {
				plPD = new PlayerPointsDiff();
				pdHash.put(cur_pid, plPD);
				pid = cur_pid;
			}
			// add data to relevant part of aggregate record
			switch (curPointDiff.getInt(1)) {
				case 0:
					plPD.hard_points = curPointDiff.getFloat(2);
					plPD.hard_games = curPointDiff.getFloat(3);
					break;
				case 1:
					plPD.med_points = curPointDiff.getFloat(2);
					plPD.med_games = curPointDiff.getFloat(3);
					break;
				case 2:
					plPD.easy_points = curPointDiff.getFloat(2);
					plPD.easy_games = curPointDiff.getFloat(3);
					break;
			}
			curPointDiff.moveToNext();
		}
		curPointDiff.close();
		
		result.setProgress(60);
		indDiff = System.currentTimeMillis() - indTime;
		indTime = System.currentTimeMillis();
		App.log("m: " + indDiff);
		
		// games over y points
		HashMap<Integer, Integer> maxHash = new HashMap<Integer, Integer>();
		Cursor curMax = db.rawQuery("SELECT player_player_id, MAX(total) max_score FROM player_match WHERE season >= " + season_from + " AND season <= " + season_to + " GROUP BY player_player_id", null);
		curMax.moveToFirst();
		while (!curMax.isAfterLast()) {
			maxHash.put(curMax.getInt(0), curMax.getInt(1));
			curMax.moveToNext();
		}
		curMax.close();
		
		result.setProgress(65);
		indDiff = System.currentTimeMillis() - indTime;
		indTime = System.currentTimeMillis();
		App.log("n: " + indDiff);
		
		// previous season stats
		HashMap<Integer, Integer> prevHash = new HashMap<Integer, Integer>();
		Cursor curPrev = db.rawQuery("SELECT player_id, c_goals_perc_team, c_assists_perc_team, c_bonus_per_win, c_bonus_per_draw, c_bonus_per_loss, minutes, c_pp90, c_ppg_x_mins FROM player_season WHERE season = " + (season_proc - 1), null);
		curPrev.moveToFirst();
		p = 0;
		while (!curPrev.isAfterLast()) {
			prevHash.put(curPrev.getInt(0), p);
			curPrev.moveToNext();
			p++;
		}
		
		result.setProgress(70);
		indDiff = System.currentTimeMillis() - indTime;
		App.log("o (last): " + indDiff);
		
		Integer hash;
		float c_team_goals;
		int c_sum_goals;
		int c_sum_assists;
		int c_sum_bonus;
		int c_sum_minutes;
		int c_sum_points;
		
		float res_points_home;
		float res_games_home;
		float res_minutes_home;
		float res_points_away;
		float res_games_away;
		float res_minutes_away;
		float res_points_x_home;
		float res_games_x_home;
		float res_points_x_away;
		float res_games_x_away;
		float res_total_mins;
		float res_total_games_recent;
		float res_points_x_rec;
		float res_games_x_rec;
		
		float c_ppg_home;
		float c_ppg_away;
		float c_ppg_x_mins_home;
		float c_ppg_x_mins_away;
		float c_ppg_x_mins;
		float c_ppg_x_mins_rec;
		
		float c_pp90;
		float c_pp90_home;
		float c_pp90_away;
		float c_ap90;
		float c_gp90;
		float c_g_over_y = 0;
		float c_g_over_z = 0;
		
		float res_goals;
		float res_assists;
		float res_bonus;
		float res_minutes;
		float res_points;
		float res_ea_ppi;
		int c_sum_error;
		
		float c_goals_perc_team;
		float c_assists_perc_team;
		float c_bonus_per_game;
		
		int c_avg_mins_recent;
		int c_diff_flag;
		float c_games_x_mins;
		int c_highest_score;
		
		float c_fact_x_home;
		float c_fact_x_away;
		float c_fact_form;
		float res_clean_sheets;
		float c_cs_perc_games;
		
		float c_bonus_per_win;
		float c_bonus_per_draw;
		float c_bonus_per_loss;
		float c_points_per_win;
		float c_points_per_draw;
		float c_points_per_loss;
		float c_perc_games_won;
		float c_ppg_easy;
		float c_ppg_med;
		float c_ppg_hard;
		float c_flat_track_bully;
		float c_fixture_proof;
		float c_value_ppg_x;
		float c_value_pp90;
		float c_emerge;
		float c_diff;
		float c_value_diff;
		float c_prev_goals_perc_team;
		float c_prev_assists_perc_team;
		float c_prev_bonus_per_win;
		float c_prev_bonus_per_draw;
		float c_prev_bonus_per_loss;
		float c_prev_minutes;
		float c_prev_pp90;
		float c_prev_ppg_x;
		float c_ea_g_x;
		
		int player_fpl_id_ind = curPl.getColumnIndex("nn_fpl_id");
		int player_player_id_ind = curPl.getColumnIndex("nn_player_id");
		int team_id_ind = curPl.getColumnIndex("nn_team_id");
		int position_ind = curPl.getColumnIndex("nn_position");
		int red_flag_ind = curPl.getColumnIndex("nn_fpl_red_flag");
		int price_ind = curPl.getColumnIndex("nn_price");
		
		int p_team_id;
		int position;
		int red_flag;
		float p_price;
		int minutes_qual;
		
		int upcom_gw_last = parent.next_gameweek + ProcessData.next_x_games - 1;
		int gw_form_till = gameweek + USE_FULL_FORM_FOR_GWS;
		
		db.execSQL(DbGen.TRANSACTION_IMMEDIATE_BEGIN);
		
		while (!curPl.isAfterLast()) {
			player_fpl_id_use_sparingly = curPl.getInt(player_fpl_id_ind);
			player_player_id = curPl.getInt(player_player_id_ind);
			p_team_id = curPl.getInt(team_id_ind);
			p_price = curPl.getFloat(price_ind) / 10f;
			position = curPl.getInt(position_ind);
			red_flag = curPl.getInt(red_flag_ind);
			t_wArgs[0] = String.valueOf(player_player_id);
			//AppClass.log("p " + player_fpl_id);
			
			team = parent.teamStats.get(p_team_id);
			// if current season, must have team hash
			if ( (team != null) || !current_season) {
				
				hash = basicsHash.get(player_player_id);
				if (hash != null) {
					curBasics.moveToPosition(hash);
					res_goals = curBasics.getInt(1);
					res_assists = curBasics.getInt(2);
					res_bonus = curBasics.getInt(3);
					res_minutes = curBasics.getInt(4);
					res_points = curBasics.getInt(5);
					res_clean_sheets = curBasics.getInt(6);
					res_ea_ppi = curBasics.getInt(7);
				} else {
					res_goals = 0;
					res_assists = 0;
					res_bonus = 0;
					res_minutes = 0;
					res_points = 0;
					res_clean_sheets = 0;
					res_ea_ppi = 0;
				}
				
				hash = sumHash.get(player_player_id);
				if (hash != null) {
					curSum.moveToPosition(hash);
					c_team_goals = curSum.getFloat(1);
					c_team_goals = Math.round(c_team_goals);
					c_sum_goals = curSum.getInt(2);
					c_sum_assists = curSum.getInt(3);
					c_sum_bonus = curSum.getInt(4);
					c_sum_minutes = curSum.getInt(5);
					c_sum_points = curSum.getInt(6);
				} else {
					c_team_goals = 0;
					c_sum_goals = 0;
					c_sum_assists = 0;
					c_sum_bonus = 0;
					c_sum_minutes = 0;
					c_sum_points = 0;
				}
				
				minutes_qual = (int) res_minutes;
				if (all_seasons) minutes_qual = c_sum_minutes;
				
				hash = homeHash.get(player_player_id);
				if (hash != null) {
					curHome.moveToPosition(hash);
					res_points_home = curHome.getFloat(1);
					res_games_home = curHome.getFloat(2);
					res_minutes_home = curHome.getFloat(3);
				} else {
					res_points_home = 0;
					res_games_home = 0;
					res_minutes_home = 0;
				}
	
				hash = awayHash.get(player_player_id);
				if (hash != null) {
					curAway.moveToPosition(hash);
					res_points_away = curAway.getFloat(1);
					res_games_away = curAway.getFloat(2);
					res_minutes_away = curAway.getFloat(3);
				} else {
					res_points_away = 0;
					res_games_away = 0;
					res_minutes_away = 0;
				}
				
				hash = homeXHash.get(player_player_id);
				if (hash != null) {
					curXHome.moveToPosition(hash);
					res_points_x_home = curXHome.getFloat(1);
					res_games_x_home = curXHome.getFloat(2);
				} else {
					res_points_x_home = 0;
					res_games_x_home = 0;
				}
				
				hash = awayXHash.get(player_player_id);
				if (hash != null) {
					curXAway.moveToPosition(hash);
					res_points_x_away = curXAway.getFloat(1);
					res_games_x_away = curXAway.getFloat(2);
				} else {
					res_points_x_away = 0;
					res_games_x_away = 0;
				}
				
				hash = formHash.get(player_player_id);
				if (hash != null) {
					curForm.moveToPosition(hash);
					res_total_mins = curForm.getFloat(1);
					res_total_games_recent = curForm.getFloat(2);
				} else {
					res_total_mins = 0;
					res_total_games_recent = 0;
				}
				
				hash = formXHash.get(player_player_id);
				if (hash != null) {
					curXForm.moveToPosition(hash);
					res_points_x_rec = curXForm.getFloat(1);
					res_games_x_rec = curXForm.getFloat(2);
				} else {
					res_points_x_rec = 0;
					res_games_x_rec = 0;
				}
				
				hash = yHash.get(player_player_id);
				if (hash != null) {
					c_g_over_y = hash;
				} else {
					c_g_over_y = 0;
				}
				
				hash = zHash.get(player_player_id);
				if (hash != null) {
					c_g_over_z = hash;
				} else {
					c_g_over_z = 0;
				}
				
				hash = maxHash.get(player_player_id);
				if (hash != null) {
					c_highest_score = hash;
				} else {
					c_highest_score = 0;
				}
				
				c_bonus_per_win = 0;
				c_bonus_per_draw = 0;
				c_bonus_per_loss = 0;
				c_points_per_win = 0;
				c_points_per_draw = 0;
				c_points_per_loss = 0;
				c_perc_games_won = 0;
				plWDL = wdlHash.get(player_player_id);
				if (plWDL != null) {
					if (plWDL.w > 0) c_bonus_per_win = plWDL.w_bonus / plWDL.w;
					if (plWDL.d > 0) c_bonus_per_draw = plWDL.d_bonus / plWDL.d;
					if (plWDL.l > 0) c_bonus_per_loss = plWDL.l_bonus / plWDL.l;
					if (plWDL.w > 0) c_points_per_win = plWDL.w_pts / plWDL.w;
					if (plWDL.d > 0) c_points_per_draw = plWDL.d_pts / plWDL.d;
					if (plWDL.l > 0) c_points_per_loss = plWDL.l_pts / plWDL.l;
					if ((res_games_home + res_games_away) > 0) c_perc_games_won = plWDL.w / (res_games_home + res_games_away);
				}
				
				// points vs difficulty
				c_ppg_easy = 0;
				c_ppg_med = 0;
				c_ppg_hard = 0;
				c_flat_track_bully = 0;
				c_fixture_proof = 0;
				plPD = pdHash.get(player_player_id);
				if (plPD != null) {
					if (plPD.easy_games > 1) c_ppg_easy = plPD.easy_points / plPD.easy_games;
					if (plPD.med_games > 1) c_ppg_med = plPD.med_points / plPD.med_games;
					if (plPD.hard_games > 1) c_ppg_hard = plPD.hard_points / plPD.hard_games;
					if ( (plPD.hard_games > 2) && (plPD.med_games > 2) && (plPD.easy_games > 2) ) {
						c_flat_track_bully = 10 + c_ppg_easy - c_ppg_med - c_ppg_hard;
						c_fixture_proof = 10 + c_ppg_hard - c_ppg_easy - c_ppg_med;
					}
				}
				
				hash = prevHash.get(player_player_id);
				if (hash != null) {
					curPrev.moveToPosition(hash);
					c_prev_goals_perc_team = curPrev.getFloat(1) / 100f;
					c_prev_assists_perc_team = curPrev.getFloat(2) / 100f;
					c_prev_bonus_per_win = curPrev.getFloat(3) / 100f;
					c_prev_bonus_per_draw = curPrev.getFloat(4) / 100f;
					c_prev_bonus_per_loss = curPrev.getFloat(5) / 100f;
					c_prev_minutes = curPrev.getFloat(6);
					c_prev_pp90 = curPrev.getFloat(7) / 100f;
					c_prev_ppg_x = curPrev.getFloat(8) / 100f;
				} else {
					c_prev_goals_perc_team = 0;
					c_prev_assists_perc_team = 0;
					c_prev_bonus_per_win = 0;
					c_prev_bonus_per_draw = 0;
					c_prev_bonus_per_loss = 0;
					c_prev_minutes = 0;
					c_prev_pp90 = 0;
					c_prev_ppg_x = 0;
				}
				
				////////////////
				////////////////
				/// load finished
				////////////////
				////////////////
				/// do actual processing
				////////////////
				////////////////
				
				// basic home/away
				if (res_games_home > 0) {
					c_ppg_home = res_points_home / res_games_home;
				} else {
					c_ppg_home = 0;
				}
				if (res_games_away > 0) {
					c_ppg_away = res_points_away / res_games_away;
				} else {
					c_ppg_away = 0;
				}
				
				// stuff per 90 minutes
				if (res_minutes > 0) {
					float m90 = res_minutes / 90;
					// ppm - use res for past
					c_pp90 = res_points / m90;
					// gp90
					c_gp90 = res_goals / m90;
					// ap90
					c_ap90 = res_assists / m90;
				} else {
					c_pp90 = 0;
					c_gp90 = 0;
					c_ap90 = 0;
				}
				
				// ppm home
				if (res_minutes_home > 0) {
					c_pp90_home = res_points_home / (res_minutes_home / 90);
				} else {
					c_pp90_home = 0;
				}
				// ppm away
				if (res_minutes_away > 0) {
					c_pp90_away = res_points_away / (res_minutes_away / 90);
				} else {
					c_pp90_away = 0;
				}
				
				// x mins home/away
				if (res_games_x_home > 0) {
					c_ppg_x_mins_home = res_points_x_home / res_games_x_home;
				} else {
					c_ppg_x_mins_home = 0;
				}
				if (res_games_x_away > 0) {
					c_ppg_x_mins_away = res_points_x_away / res_games_x_away;
				} else {
					c_ppg_x_mins_away = 0;
				}
				
				// x mins total
				c_games_x_mins = res_games_x_home + res_games_x_away;
				if (c_games_x_mins > 0) {
					c_ppg_x_mins = (res_points_x_home + res_points_x_away) / c_games_x_mins;
					c_cs_perc_games = res_clean_sheets / c_games_x_mins;
					//AppClass.log("res_clean_sheets = " + res_clean_sheets + "; c_games_x_mins  = " + c_games_x_mins + "; c_cs_perc_games = " + c_cs_perc_games);
				} else {
					c_ppg_x_mins = 0;
					c_cs_perc_games = 0;
				}
				
				// check for mismatches
				c_sum_error = 0;
				if (full_proc && !all_seasons) {
					if (res_goals != c_sum_goals) {
						c_sum_error = 1;
					} else if (res_assists != c_sum_assists) {
						c_sum_error = 1;
					} else if (res_bonus != c_sum_bonus) {
						c_sum_error = 1;
					} else if (res_minutes != c_sum_minutes) {
						c_sum_error = 1;
					} else if (res_points != c_sum_points) {
						c_sum_error = 1;
					}
				}
				
				// stats as a percentage of team while on pitch - goals
				// use c_sum_goals to exclude hist (no team goals stats for hist)
				if (c_team_goals > 0) {
					c_goals_perc_team = c_sum_goals / c_team_goals;
					if (c_goals_perc_team > 1) {
						c_goals_perc_team = 1;
					}
				} else {
					c_goals_perc_team = 0;
				}
				
				// assists
				// ditto on c_sum_assists
				if (c_team_goals > 0) {
					c_assists_perc_team = c_sum_assists / c_team_goals;
					if (c_assists_perc_team > 1) {
						c_assists_perc_team = 1;
					}
				} else {
					c_assists_perc_team = 0;
				}
				
				// bonus
				if ((res_games_x_home + res_games_x_away) > 0) {
					c_bonus_per_game = c_sum_bonus / (res_games_x_home + res_games_x_away);
				} else {
					c_bonus_per_game = 0;
				}
				
				// avg mins per game in last (form) gameweeks: c_avg_mins_recent
				if (res_total_games_recent > 0) {
					c_avg_mins_recent = (int) ProcessData.trunc(res_total_mins / res_total_games_recent);
				} else {
					c_avg_mins_recent = 0;
				}
				
				// set diff unavailable flag
				c_diff_flag = 0;
				if (current_season) {
					if ( (res_total_games_recent == 0) && (gameweek > 1) ) c_diff_flag = 1;
				}
				
				// current form ppg x
				if (res_games_x_rec > 0) {
					c_ppg_x_mins_rec = res_points_x_rec / res_games_x_rec;
				} else {
					c_ppg_x_mins_rec = 0;
				}
				
				// ea ppi per game x
				if ( (c_games_x_mins > 0) && (res_ea_ppi > 0) ) {
					c_ea_g_x = res_ea_ppi / c_games_x_mins;
				} else {
					c_ea_g_x = 0;
				}
				
				// values
				c_value_ppg_x = 0;
				c_value_pp90 = 0;
				if (p_price > 0) {
					c_value_ppg_x = c_ppg_x_mins / p_price;
					c_value_pp90 = c_pp90 / p_price;
				}
				
				// emerge 
				c_emerge = 0;
				// calc emerge...
				if (!all_seasons) {
					if ( (res_minutes >= EMERGE_MINS) && (c_prev_minutes >= EMERGE_MINS) ) {
						c_emerge = c_pp90 + c_ppg_x_mins - c_prev_pp90 - c_prev_ppg_x;
					}
				}
				
				c_fact_form = 1;
				if ( (c_ppg_x_mins_rec > 0) && (res_games_x_rec >= 3) ) c_fact_form = c_ppg_x_mins_rec / c_ppg_x_mins;
				float c_pred_left_pts = 0;
				
				if (c_fact_form > MAX_FORM) {
					c_fact_form = MAX_FORM;
				} else if (c_fact_form < MIN_FORM) {
					c_fact_form = MIN_FORM;
				}
				
				//debug("c_fact_form = " + c_fact_form, player_player_id);
				
				if (current_season) {
					//
					//
					//
					// init totals for PLAYER MATCH PROCESSING
					//
					//
					//
					float c_pred_left_goals = 0;
					float c_pred_left_assists = 0;
					float c_pred_left_bonus_pts = 0;
					float c_pred_left_cs_pts = 0;
					int c_games_left = 0;
					
					c_fact_x_home = 1;
					if ( (c_ppg_x_mins > 0) && (res_games_x_home > 4) ) c_fact_x_home = c_ppg_x_mins_home / c_ppg_x_mins;
					c_fact_x_away = 1;
					if ( (c_ppg_x_mins > 0) && (res_games_x_away > 4) ) c_fact_x_away = c_ppg_x_mins_away / c_ppg_x_mins;
					
					//debug("c_fact_x_home = " + c_fact_x_home, player_player_id);
					//debug("c_fact_x_away = " + c_fact_x_away, player_player_id);
					
					Iterator<Fixture> iterator = team.fixtures.iterator();
					Fixture f;
					boolean first = true;
					float score_fact;
					float pred_goals;
					float pred_assists;
					float pred_bonus_pts;
					float pred_save_pts;
					float pred_goal_pts;
					float pred_assist_pts;
					float pred_cs_pts;
					float conc_factor;
					float pred_total_pts;
					float diff_upcom_fix_total = 0;
					float diff_upcom_pred_total = 0;
					
					// player stats used for predictions
					float c_use_goals_perc_team = season_mix(c_goals_perc_team, c_prev_goals_perc_team);
					float c_use_assists_perc_team = season_mix(c_assists_perc_team, c_prev_assists_perc_team);
					float c_use_bonus_per_win = season_mix(c_bonus_per_win, c_prev_bonus_per_win);
					float c_use_bonus_per_draw = season_mix(c_bonus_per_draw, c_prev_bonus_per_draw);
					float c_use_bonus_per_loss = season_mix(c_bonus_per_loss, c_prev_bonus_per_loss);
					
					StringBuffer ticker_string = new StringBuffer();
					boolean ticker_first = true;
					// iterate upcoming fixtures for player (in order)
					while (iterator.hasNext()){
						f = iterator.next();
						
						if (f.home) {
							score_fact = c_fact_x_home;
							//debug(" vs " + f.team_opp_id + " (h) GW " + f.gameweek, player_player_id);
						} else {
							score_fact = c_fact_x_away;
							//debug(" vs " + f.team_opp_id + " (a) GW " + f.gameweek, player_player_id);
						}
						
						// goals/assists
						pred_goals = f.pred_goals_for * c_use_goals_perc_team;
						pred_assists = f.pred_goals_against * c_use_assists_perc_team;
						
						// bonus points
						if (f.pred_points == 0) {
							pred_bonus_pts = c_use_bonus_per_loss;
						} else if (f.pred_points == 1) {
							pred_bonus_pts = c_use_bonus_per_draw;
						} else {
							pred_bonus_pts = c_use_bonus_per_win;
						}
						
						// keeper saves
						if (position == 1) {
							// 0.8 because it's harder to reach x3 threshold for a save point
							pred_save_pts = f.pred_goals_against * 0.8f;
						} else {
							pred_save_pts = 0;
						}
						
						// goal/assist points
						if (position <= 2) {
							pred_goal_pts = pred_goals * 6f;
						} else if (position == 3) {
							pred_goal_pts = pred_goals * 5f;
						} else {
							pred_goal_pts = pred_goals * 4f;
						}
						pred_assist_pts = pred_assists * 3f;
						
						// clean sheet points
						pred_cs_pts = 0;
						if (position <= 3) {
							// no chance
							if (f.pred_goals_against > 2) {
								pred_cs_pts = 0;
							// some chance
							} else {
								pred_cs_pts = (2 - f.pred_goals_against) * 2;
								// for midfielder, %4
								if (position == 3) pred_cs_pts = pred_cs_pts / 4;
							}
						}
						
						// goals conceded points (goes into cs points)
						if (position <= 2) {
							// no chance
							if (f.pred_goals_against < 1) {
								// no lost points
							// some chance
							} else {
								// 1 point lost for every 2 goals conceded
								conc_factor = f.pred_goals_against / 2;
								pred_cs_pts -= conc_factor;
							}
						}
						
						// sum total points
						pred_total_pts = 2 + pred_goal_pts + pred_assist_pts + pred_cs_pts + pred_bonus_pts + pred_save_pts;
						
						//debug("  base total = " + pred_total_pts, player_player_id);
						
						// if not played minutes recently, reduce score by ratio of mins played
						if (gameweek >= ProcessTeam.CROSSOVER_GW) {
							float tmp_avg_mins_recent = c_avg_mins_recent;
							if (tmp_avg_mins_recent < 20) tmp_avg_mins_recent = 20;
						
							if (tmp_avg_mins_recent < ProcessData.recent_mins_thresh) {
								pred_total_pts = pred_total_pts * (tmp_avg_mins_recent / 90);
							} else if (c_games_x_mins < 4) {
								pred_total_pts = pred_total_pts * 0.75f;
							} else if (res_total_games_recent <= 2) {
								pred_total_pts = pred_total_pts * 0.75f;
							}
						}
						
						// if player is unavailable, then 0 points
						if ( (red_flag == 1) && first ) pred_total_pts = 0;
						
						//debug("  reduced = " + pred_total_pts, player_player_id);
						
						float c_use_form_factor;
						if (f.gameweek <= gw_form_till) {
							c_use_form_factor = c_fact_form;
						} else if (f.gameweek == (gw_form_till + 1) ) {
							c_use_form_factor = (1 + c_fact_form + c_fact_form) / 3;
						} else if (f.gameweek == (gw_form_till + 2) ) {
							c_use_form_factor = (1 + 1 + c_fact_form) / 3;
						} else {
							c_use_form_factor = 1;
						}
						
						//debug("   use form factor " + c_use_form_factor, player_player_id);
						
						// comment/uncomment: factor in player home/away ppg + ppg_rec
						//
						//
						//
						//
						pred_total_pts = (pred_total_pts + pred_total_pts + (pred_total_pts * score_fact)
								+ (pred_total_pts * c_use_form_factor)) / 4;
						//
						//
						
						//debug("  final = " + pred_total_pts, player_player_id);
						
						updatePM.put(Field_player_match.f_fixture_id, f.fix_id);
						updatePM.put(Field_player_match.f_gameweek, f.gameweek);
						if (f.home) {
							updatePM.put(Field_player_match.f_is_home, 1);
						} else {
							updatePM.remove(Field_player_match.f_is_home);
						}
						updatePM.put(Field_player_match.f_opp_team_id, f.team_opp_id);
						updatePM.put(Field_player_match.f_pl_team_id, f.team_pl_id);
						updatePM.put(Field_player_match.f_player_player_id, player_player_id);
						int pred_total_pts_int = (int) (pred_total_pts * 100);
						updatePM.put(Field_player_match.f_pred_total_pts, pred_total_pts_int);
						updatePM.put(Field_player_match.f_season, season_proc);
						
						// insert or update
						db.replace(t_player_match, null, updatePM);
						
						// add to player totals
						c_pred_left_goals += pred_goals;
						c_pred_left_assists += pred_assists;
						c_pred_left_bonus_pts += pred_bonus_pts;
						c_pred_left_cs_pts += pred_cs_pts;
						c_pred_left_pts += pred_total_pts;
						c_games_left ++;
						
						// add to upcoming stats if applicable
						if ( (f.gameweek > gameweek) && (f.gameweek <= upcom_gw_last) ) {
							diff_upcom_fix_total += f.ratio;
							diff_upcom_pred_total += pred_total_pts;
							
							// ticker string build
							if (ticker_first) {
								ticker_first = false;
							} else {
								ticker_string.append(",");
							}
							// gw
							ticker_string.append(f.gameweek + "=");
							// home/away
							if (f.home) {
								ticker_string.append("1");
							} else {
								ticker_string.append("0");
							}
							// fix diff
							ticker_string.append("=" + f.ratio);
							// pred score
							ticker_string.append("=" + pred_total_pts);
							// pred cs
							ticker_string.append("=" + f.pred_goals_against);
						}
						
						first = false;
					} // loop fix
					
					//
					//
					//
					// end PLAYER MATCH PROCESSING
					//
					//
					//
				
					// work out "upcoming x gameweeks" scores
					float num_gameweeks_upcom = Math.min(ProcessData.num_gameweeks - gameweek, ProcessData.next_x_games);
					float diff_upcom_fix_value = 0;
					float diff_upcom_pred_value = 0;
					if (num_gameweeks_upcom > 0) {
						diff_upcom_fix_total = (diff_upcom_fix_total / num_gameweeks_upcom) * 100;
						diff_upcom_pred_total = (diff_upcom_pred_total / num_gameweeks_upcom) * 100;
						diff_upcom_fix_value = diff_upcom_fix_total / p_price;
						diff_upcom_pred_value = diff_upcom_pred_total / p_price;
					} else {
						diff_upcom_fix_total = 0;
						diff_upcom_pred_total = 0;
					}
					
					updateVals.put(Field_player.f_diff_upcom_fix, (int) diff_upcom_fix_total);
					checkPlDiff((int) diff_upcom_fix_total, Field_player.f_diff_upcom_fix, curPl);
					updateVals.put(Field_player.f_diff_upcom_pred, (int) diff_upcom_pred_total);
					checkPlDiff((int) diff_upcom_pred_total, Field_player.f_diff_upcom_pred, curPl);
					updateVals.put(Field_player.f_diff_value_fix, (int) diff_upcom_fix_value);
					checkPlDiff((int) diff_upcom_fix_value, Field_player.f_diff_value_fix, curPl);
					updateVals.put(Field_player.f_diff_value_pred, (int) diff_upcom_pred_value);
					checkPlDiff((int) diff_upcom_pred_value, Field_player.f_diff_value_pred, curPl);
					
					updateValsP.put(Field_player.f_ticker_string, DbGen.compress(ticker_string.toString()));
					
					updateVals.put(Field_player.f_c_games_left, c_games_left);
					checkPlDiff(c_games_left, Field_player.f_c_games_left, curPl);
					int val = (int) (c_pred_left_assists * 100);
					updateVals.put(Field_player.f_pred_left_assists, val);
					checkPlDiff(val, Field_player.f_pred_left_assists, curPl);
					val = (int) (c_pred_left_bonus_pts * 100);
					updateVals.put(Field_player.f_pred_left_bonus_pts, val);
					checkPlDiff(val, Field_player.f_pred_left_bonus_pts, curPl);
					val = (int) (c_pred_left_cs_pts * 100);
					updateVals.put(Field_player.f_pred_left_cs_pts, val);
					checkPlDiff(val, Field_player.f_pred_left_cs_pts, curPl);
					val = (int) (c_pred_left_goals * 100);
					updateVals.put(Field_player.f_pred_left_goals, val);
					checkPlDiff(val, Field_player.f_pred_left_goals, curPl);
					val = (int) (c_pred_left_pts * 100);
					updateVals.put(Field_player.f_pred_left_pts, val);
					checkPlDiff(val, Field_player.f_pred_left_pts, curPl);
					
					val = (int) ((c_pred_left_pts * 100) / p_price);
					updateVals.put(Field_player.f_pred_left_value, val);
					checkPlDiff(val, Field_player.f_pred_left_value, curPl);
					val = (int) ((c_pred_left_pts + res_points) * 100);
					updateVals.put(Field_player.f_pred_season_pts, val);
					checkPlDiff(val, Field_player.f_pred_season_pts, curPl);
					
					// delete past gw player_match records where player did not play
					// (0 minutes)
					db.execSQL("DELETE FROM player_match WHERE season = " + season_proc
							+ " AND gameweek < " + gameweek + " AND (minutes is null or minutes < 1)"
							+ " AND player_player_id = " + player_player_id
							+ " AND (SELECT f.got_bonus FROM fixture f WHERE f._id = fixture_id) = 2");
				} // big if on pred stuff
				
				// diff
				float cs = 0;
				float gw = gameweek;
				if (position <= 2) cs = c_cs_perc_games;
				float perc = c_assists_perc_team + c_goals_perc_team + cs;
				float pred = 0;
				if (current_season && (gw < ProcessData.num_gameweeks) ) {
					float gw_left = ProcessData.num_gameweeks - gw;
					pred = c_pred_left_pts / (gw_left / 2);
				}
				float fact = (c_pp90 * 5) + c_g_over_y + (c_g_over_z * 2.5f) + (c_team_goals / 8) + (c_ppg_x_mins * 5) + pred + (c_emerge / 2) + c_ea_g_x;
				float res = perc * fact;
				c_diff = res;
				if (current_season) c_diff *= c_fact_form;
				if (gameweek > 5) {
					float ratio = c_games_x_mins / gw;
					if (ratio < 0.75) {
						if (ratio < 0.2) {
							c_diff *= 0.2;
						} else {
							c_diff *= ratio;
						}
					}
				}
				c_diff = ProcessData.trunc(c_diff);
				
				// values
				c_value_diff = 0;
				if (p_price > 0) {
					c_value_diff = c_diff / p_price;
				}
				
				int val;
				if (full_proc) {
					updateVals.put(Field_player.f_c_team_goals, c_team_goals);
					checkPlDiff(c_team_goals, Field_player.f_c_team_goals, curPl);
					val = (int) (c_ppg_home * 100);
					updateVals.put(Field_player.f_c_ppg_home, val);
					checkPlDiff(val, Field_player.f_c_ppg_home, curPl);
					val = (int) (c_ppg_away * 100);
					updateVals.put(Field_player.f_c_ppg_away, val);
					checkPlDiff(val, Field_player.f_c_ppg_away, curPl);
					val = (int) (c_ppg_x_mins_home * 100);
					updateVals.put(Field_player.f_c_ppg_x_mins_home, val);
					checkPlDiff(val, Field_player.f_c_ppg_x_mins_home, curPl);
					val = (int) (c_ppg_x_mins_away * 100);
					updateVals.put(Field_player.f_c_ppg_x_mins_away, val);
					checkPlDiff(val, Field_player.f_c_ppg_x_mins_away, curPl);
					val = (int) (c_ppg_x_mins * 100);
					updateVals.put(Field_player.f_c_ppg_x_mins, val);
					checkPlDiff(val, Field_player.f_c_ppg_x_mins, curPl);
				}
				if (current_season) {
					val = (int) (c_ppg_x_mins_rec * 100);
					updateVals.put(Field_player.f_c_ppg_x_mins_rec, val);
					checkPlDiff(val, Field_player.f_c_ppg_x_mins_rec, curPl);
				}
				updateVals.put(Field_player.f_c_games_x_mins, c_games_x_mins);
				checkPlDiff(c_games_x_mins, Field_player.f_c_games_x_mins, curPl);
				if (current_season) {
					updateVals.put(Field_player.f_c_games_recent, res_total_games_recent);
					checkPlDiff(res_total_games_recent, Field_player.f_c_games_recent, curPl);
					updateVals.put(Field_player.f_c_games_avl_recent, team.res_num_games_rec);
					checkPlDiff(team.res_num_games_rec, Field_player.f_c_games_avl_recent, curPl);
				}
				updateVals.put(Field_player.f_minutes_qual, minutes_qual);
				checkPlDiff(minutes_qual, Field_player.f_minutes_qual, curPl);
				if (full_proc) {
					updateVals.put(Field_player.f_c_sum_error, c_sum_error);
					checkPlDiff(c_sum_error, Field_player.f_c_sum_error, curPl);
					val = (int) (c_bonus_per_game * 100);
					updateVals.put(Field_player.f_c_bonus_per_game, val);
					checkPlDiff(val, Field_player.f_c_bonus_per_game, curPl);
					val = (int) (c_goals_perc_team * 100);
					updateVals.put(Field_player.f_c_goals_perc_team, val);
					checkPlDiff(val, Field_player.f_c_goals_perc_team, curPl);
					val = (int) (c_assists_perc_team * 100);
					updateVals.put(Field_player.f_c_assists_perc_team, val);
					checkPlDiff(val, Field_player.f_c_assists_perc_team, curPl);
				}
				if (current_season) {
					updateVals.put(Field_player.f_c_avg_mins_recent, c_avg_mins_recent);
					//checkPlDiff(c_avg_mins_recent, Field_player.f_c_avg_mins_recent, curPl);
				}
				
				if (full_proc) {
					val = (int) (c_cs_perc_games * 100);
					updateVals.put(Field_player.f_c_cs_perc_games, val);
					checkPlDiff(val, Field_player.f_c_cs_perc_games, curPl);
					val = (int) (c_bonus_per_win * 100);
					updateVals.put(Field_player.f_c_bonus_per_win, val);
					checkPlDiff(val, Field_player.f_c_bonus_per_win, curPl);
					val = (int) (c_bonus_per_draw * 100);
					updateVals.put(Field_player.f_c_bonus_per_draw, val);
					checkPlDiff(val, Field_player.f_c_bonus_per_draw, curPl);
					val = (int) (c_bonus_per_loss * 100);
					updateVals.put(Field_player.f_c_bonus_per_loss, val);
					checkPlDiff(val, Field_player.f_c_bonus_per_loss, curPl);
					
					val = (int) (c_points_per_win * 100);
					updateVals.put(Field_player.f_c_points_per_win, val);
					checkPlDiff(val, Field_player.f_c_points_per_win, curPl);
					val = (int) (c_points_per_draw * 100);
					updateVals.put(Field_player.f_c_points_per_draw, val);
					checkPlDiff(val, Field_player.f_c_points_per_draw, curPl);
					val = (int) (c_points_per_loss * 100);
					updateVals.put(Field_player.f_c_points_per_loss, val);
					checkPlDiff(val, Field_player.f_c_points_per_loss, curPl);
					val = (int) (c_perc_games_won * 100);
					updateVals.put(Field_player.f_c_perc_games_won, val);
					checkPlDiff(val, Field_player.f_c_perc_games_won, curPl);
					if (c_ea_g_x > 0) {
						val = (int) (c_ea_g_x * 100);
						updateVals.put(Field_player.f_c_ea_g_x, val);
						checkPlDiff(val, Field_player.f_c_ea_g_x, curPl);
					}
					val = (int) (c_diff * 100);
					updateVals.put(Field_player.f_c_diff, val);
					checkPlDiff(val, Field_player.f_c_diff, curPl);
					val = (int) (c_value_diff * 100);
					updateVals.put(Field_player.f_c_value_diff, val);
					checkPlDiff(val, Field_player.f_c_value_diff, curPl);
				}
				
				val = (int) (c_pp90 * 100);
				updateVals.put(Field_player.f_c_pp90, val);
				checkPlDiff(val, Field_player.f_c_pp90, curPl);
				val = (int) (c_gp90 * 100);
				updateVals.put(Field_player.f_c_gp90, val);
				checkPlDiff(val, Field_player.f_c_gp90, curPl);
				val = (int) (c_ap90 * 100);
				updateVals.put(Field_player.f_c_ap90, val);
				checkPlDiff(val, Field_player.f_c_ap90, curPl);
				if (full_proc) {
					val = (int) (c_pp90_home * 100);
					updateVals.put(Field_player.f_c_pp90_home, val);
					checkPlDiff(val, Field_player.f_c_pp90_home, curPl);
					val = (int) (c_pp90_away * 100);
					updateVals.put(Field_player.f_c_pp90_away, val);
					checkPlDiff(val, Field_player.f_c_pp90_away, curPl);
					updateVals.put(Field_player.f_c_g_over_y, c_g_over_y);
					checkPlDiff(c_g_over_y, Field_player.f_c_g_over_y, curPl);
					updateVals.put(Field_player.f_c_g_over_z, c_g_over_z);
					checkPlDiff(c_g_over_z, Field_player.f_c_g_over_z, curPl);
					val = (int) (c_value_ppg_x * 100);
					updateVals.put(Field_player.f_c_value_ppg_x, val);
					checkPlDiff(val, Field_player.f_c_value_ppg_x, curPl);
				}
				val = (int) (c_value_pp90 * 100);
				updateVals.put(Field_player.f_c_value_pp90, val);
				checkPlDiff(val, Field_player.f_c_value_pp90, curPl);
				
				if (full_proc) {
					val = (int) (c_ppg_easy * 100);
					updateVals.put(Field_player.f_c_ppg_easy, val);
					checkPlDiff(val, Field_player.f_c_ppg_easy, curPl);
					val = (int) (c_ppg_med * 100);
					updateVals.put(Field_player.f_c_ppg_med, val);
					checkPlDiff(val, Field_player.f_c_ppg_med, curPl);
					val = (int) (c_ppg_hard * 100);
					updateVals.put(Field_player.f_c_ppg_hard, val);
					checkPlDiff(val, Field_player.f_c_ppg_hard, curPl);
					val = (int) (c_flat_track_bully * 100);
					updateVals.put(Field_player.f_c_flat_track_bully, val);
					checkPlDiff(val, Field_player.f_c_flat_track_bully, curPl);
					val = (int) (c_fixture_proof * 100);
					updateVals.put(Field_player.f_c_fixture_proof, val);
					checkPlDiff(val, Field_player.f_c_fixture_proof, curPl);
					updateVals.put(Field_player.f_c_highest_score, c_highest_score);
					checkPlDiff(c_highest_score, Field_player.f_c_highest_score, curPl);
				}
				
				if (all_seasons) {
					updateVals.put(Field_player.f_goals, res_goals);
					checkPlDiff(res_goals, Field_player.f_goals, curPl);
					updateVals.put(Field_player.f_assists, res_assists);
					checkPlDiff(res_assists, Field_player.f_assists, curPl);
					updateVals.put(Field_player.f_bonus, res_bonus);
					checkPlDiff(res_bonus, Field_player.f_bonus, curPl);
					updateVals.put(Field_player.f_points, res_points);
					checkPlDiff(res_points, Field_player.f_points, curPl);
					updateVals.put(Field_player.f_minutes, res_minutes);
					checkPlDiff(res_minutes, Field_player.f_minutes, curPl);
					updateVals.put(Field_player.f_clean_sheets, res_clean_sheets);
					checkPlDiff(res_clean_sheets, Field_player.f_clean_sheets, curPl);
				} else {
					val = (int) c_emerge;
					updateVals.put(Field_player.f_c_emerge, val);
					checkPlDiff(val, Field_player.f_c_emerge, curPl);
				}
				
				// record may not yet exists for all seasons
				if (all_seasons) {
					// as with other old seasons, use player_id as fpl_id
					updateVals.put(Field_player.f_fpl_id, player_player_id);
					updateVals.put(Field_player.f_player_id, player_player_id);
					updateVals.put(Field_player.f_season, season_proc);
					updateVals.put(Field_player.f_position, position);
					db.replace(t_player_season, null, updateVals);
				} else {
					db.update(t_player_season, updateVals, t_where_player_season, t_wArgs);
				}
				
				if (current_season) updateValsP.put(Field_player.f_diff_flag, c_diff_flag);
				
				if (current_season) db.update(t_player, updateValsP, t_where_player, t_wArgs);
			}
			
			curPl.moveToNext();
			i++;
			
			result.setProgress(i + PROGRESS_STARTAT);
		} // team loop
		
		try {
			db.execSQL(DbGen.TRANSACTION_END);
		} catch (SQLException e) {
			App.exception(e);
		} catch (IllegalStateException e) {
			App.exception(e);
		}
		
		curBasics.close();
		curSum.close();
		curHome.close();
		curAway.close();
		curXHome.close();
		curXAway.close();
		curPrev.close();
		if (current_season) curForm.close();
		if (current_season) curXForm.close();
		curPl.close();
		
		// process max value/player for each stat
		Players.loadStats();
		final String f_season = "season";
		final String f_stat_id = "stat_id";
		final String f_min_games = "min_games";
		final String f_max_value = "max_value";
		final String f_player_id = "player_id";
		final String f_plus = "plus";
		final String t_stat_season = "stat_season";
		// for each stat, run query
		App.log("computing stat maxes");
		
		db.execSQL(DbGen.TRANSACTION_IMMEDIATE_BEGIN);
		
		for (Stat s : Players.playerstats_array) {
	 		// pre-compute for each possible "min games" value
 			if (s.show_in_prefs) {
				for (int min=0; min<Stats.game_nums.length; min++) {
	 				String and_min_games;
	 	    		// build query
	 				int min_games = Stats.game_nums[min];
	 	    		if (min_games == 0) {
	 	    			and_min_games = "";
	 	    		} else {
	 	    			and_min_games = " AND minutes_qual >= " + min_games;
	 	    		}
	 				//Cursor cur = db.rawQuery("SELECT player_id, " + s.db_field + " FROM player_season WHERE " + s.db_field + " = (SELECT MAX(" + s.db_field + ") FROM player_season WHERE season = " + season_proc + and_min_games + ") AND season = " + season_proc + and_min_games, null);
	    		 	Cursor cur = db.rawQuery("SELECT player_id, " + s.db_field
	    		 							+ " FROM player_season"
	    		 							+ " WHERE season = " + season_proc + and_min_games
	    		 							+ " ORDER BY " + s.db_field + " DESC"
	    		 							+ " LIMIT 2", null);
	 	    		cur.moveToFirst();
	    		 	// store top entry, and mark if there are more with same value
	    		 	if (!cur.isAfterLast()) {
	    		 		ContentValues c = new ContentValues();
	    		 		float value = cur.getFloat(1);
	    		 		c.put(f_season, season_proc);
	    		 		c.put(f_stat_id, s.db_field);
	    		 		c.put(f_min_games, min_games);
	    		 		c.put(f_max_value, value);
	    		 		c.put(f_player_id, cur.getInt(0));
	    		 		int plus = 0;
	    		 		if (!cur.isLast()) {
	    		 			cur.moveToNext();
	    		 			float sec_val = cur.getFloat(1);
	    		 			if (sec_val == value) plus = 1;
	    		 		}
	    		 		c.put(f_plus, plus);
	    		 		db.replace(t_stat_season, null, c);
	    		 	}
	    		 	cur.close();
	 			}
 			}
		}
		
		try {
			db.execSQL(DbGen.TRANSACTION_END);
		} catch (SQLException e) {
			App.exception(e);
		}
		
		//Debug.stopMethodTracing();
		Long timeDiff = System.currentTimeMillis() - startTime;
		float timeSecs = (float)timeDiff / 1000;
		App.log("Finished processing " + i + " players in " + timeSecs + " seconds");
	}
	
	private static float season_mix(float newVal, float oldVal) {
		// if near start of season, use old stats, less and less as going into season
		if (gameweek == 0) {
			return oldVal;
		} else if (gameweek < ProcessTeam.CROSSOVER_GW) {
			float xold = ProcessTeam.CROSSOVER_GW - gameweek;
			float xnew = gameweek;
			return ((oldVal * xold) + (newVal * xnew)) / ProcessTeam.CROSSOVER_GW;
		} else {
			return newVal;
		}
	}
	
	private static class PlayerWDL {
		public float w;
		public float w_pts;
		public float w_bonus;
		public float d;
		public float d_pts;
		public float d_bonus;
		public float l;
		public float l_pts;
		public float l_bonus;
	}
	
	private static class PlayerPointsDiff {
		public float easy_points;
		public float easy_games;
		public float med_points;
		public float med_games;
		public float hard_points;
		public float hard_games;
	}
	
}
