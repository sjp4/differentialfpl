package com.pennas.fpl.process;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.pennas.fpl.App;
import com.pennas.fpl.process.FPLService.ProcRes;
import com.pennas.fpl.process.ProcessFixture.Fixture;
import com.pennas.fpl.process.ProcessData.TeamStat;
import com.pennas.fpl.util.DbGen;
import com.pennas.fpl.util.SquadUtil;

public class ProcessTeam {
	private static int team_id;
	
	private static final int MIN_TEAM_RATING = 100;
	
	// teams
	private static class Field_team {
		private static final String f_c_sum_h_gs = "c_sum_h_gs";
		private static final String f_c_sum_h_gc = "c_sum_h_gc";
		private static final String f_c_sum_h_w = "c_sum_h_w";
		private static final String f_c_sum_h_d = "c_sum_h_d";
		private static final String f_c_sum_h_l = "c_sum_h_l";
		private static final String f_c_h_wd_perc = "c_h_wd_perc";
		private static final String f_c_h_w_perc = "c_h_w_perc";
		private static final String f_c_h_rating = "c_h_rating";
		private static final String f_c_h_form_rating = "c_h_form_rating";
		private static final String f_c_h_gpg = "c_h_gpg";
		private static final String f_c_h_gcpg = "c_h_gcpg";
		//private static final String f_c_sum_error = "c_sum_error";
		private static final String f_c_sum_games_played = "c_sum_games_played";
		private static final String f_c_sum_goal_diff = "c_sum_goal_diff";
		private static final String f_c_sum_points = "c_sum_points";
		private static final String f_c_sum_a_gs = "c_sum_a_gs";
		private static final String f_c_sum_a_gc = "c_sum_a_gc";
		private static final String f_c_sum_a_w = "c_sum_a_w";
		private static final String f_c_sum_a_d = "c_sum_a_d";
		private static final String f_c_sum_a_l = "c_sum_a_l";
		private static final String f_c_sum_gs = "c_sum_gs";
		private static final String f_c_sum_gc = "c_sum_gc";
		private static final String f_c_a_wd_perc = "c_a_wd_perc";
		private static final String f_c_a_w_perc = "c_a_w_perc";
		private static final String f_c_a_rating = "c_a_rating";
		private static final String f_c_a_form_rating = "c_a_form_rating";
		private static final String f_c_a_gpg = "c_a_gpg";
		private static final String f_c_a_gcpg = "c_a_gcpg";
		private static final String f_c_bonus_per_game = "c_bonus_per_game";
		private static final String f_c_gpg = "c_gpg";
		private static final String f_c_gcpg = "c_gcpg";
		private static final String f_c_cs_perc = "c_cs_perc";
		private static final String f_c_cs_home = "c_cs_home";
		private static final String f_c_cs_away = "c_cs_away";
		private static final String f_c_cs = "c_cs";
		private static final String f_games_played = "games_played";
		private static final String f_goal_diff = "goal_diff";
		private static final String f_points = "points";
		private static final String gk_player_id = "gk_player_id";
	}
	
	// teams post
	private static class Field_postTeam {
		private static final String f_pred_games = "pred_games";
		private static final String f_pred_points = "pred_points";
		private static final String f_pred_goal_diff = "pred_goal_diff";
	}
	
	public static final int CROSSOVER_GW = 9;
	public static final int NUM_TEAMS = 20;
	
	private static final int NONE = -1;
	private static final int FIRST_CHOICE_KEEPER_NUM_WEEKS = 10;
	
	static void processTeam(ProcessData parent, ProcRes result) {
		SQLiteDatabase db = parent.dbGen;
		int season = parent.season;
		int gameweek = parent.gameweek;
		int gw_from = gameweek - ProcessData.form_num_games;
		// add to this:
		//
		//
		// take last season into account
		// - weighted; more so when near start of season
		// - ie within first couple of games, heavily weighted towards last season
		//
		// - form spans seasons
		//
		//
		
		//Debug.startMethodTracing("proc_team2", 15000000);
		App.log("Processing teams..");
		long startTime = System.currentTimeMillis();
		Cursor cur;
		Cursor curTeam = db.rawQuery("SELECT * FROM team_season WHERE season = " + season, null);
		curTeam.moveToFirst();
		
		float teamCount = curTeam.getCount();
		
		result.setMaxName(teamCount, "ProcTeam");
		
		//int games_played;
		//int goal_diff;
		//int points;
		
		float c_sum_h_gs = 0;
		float c_sum_h_gc = 0;
		float c_sum_a_gs = 0;
		float c_sum_a_gc = 0;
		float c_sum_gs = 0;
		float c_sum_gc = 0;
		
		float c_sum_h_l = 0;
		float c_sum_h_d = 0;
		float c_sum_h_w = 0;
		
		float c_sum_a_l = 0;
		float c_sum_a_d = 0;
		float c_sum_a_w = 0;
		
		float c_h_games = 0;
		float c_a_games = 0;
		float c_h_wd_perc = 0;
		float c_a_wd_perc = 0;
		float c_h_w_perc = 0;
		float c_a_w_perc = 0;
		
		float c_h_gpg = 0;
		float c_a_gpg = 0;
		float c_h_gcpg = 0;
		float c_a_gcpg = 0;
		float c_gpg = 0;
		float c_gcpg = 0;
		
		float c_sum_games_played = 0;
		float c_sum_goal_diff = 0;
		float c_sum_points = 0;
		
		float c_h_rating = 0;
		float c_a_rating = 0;
		float c_h_form_rating = 0;
		float c_a_form_rating = 0;
		float c_bonus_per_game = 0;
		
		float c_cs_perc = 0;
		float c_cs_home = 0;
		float c_cs_away = 0;
		float c_cs = 0;
		
		//int c_sum_error = 0;
		
		float i = 0;
		
		db.execSQL(DbGen.TRANSACTION_IMMEDIATE_BEGIN);
		
		final String query_home_goals_lim = "SELECT SUM(a.res_goals_home) c_sum_h_gs, SUM(a.res_goals_away) c_sum_h_gc FROM (SELECT * FROM fixture WHERE season = " + season + " AND team_home_id = ? AND res_points_home IS NOT NULL ORDER BY datetime DESC LIMIT " + ProcessData.form_num_games + ") a";
		final String query_away_goals_lim = "SELECT SUM(a.res_goals_away) c_sum_a_gs, SUM(a.res_goals_home) c_sum_a_gc FROM (SELECT * FROM fixture WHERE season = " + season + " AND team_away_id = ? AND res_points_away IS NOT NULL ORDER BY datetime DESC LIMIT " + ProcessData.form_num_games + ") a";
		final String query_home_res_lim = "SELECT res_points_home, COUNT(*) num_res FROM (SELECT * FROM fixture WHERE season = " + season + " AND team_home_id = ? AND res_points_home IS NOT NULL ORDER BY datetime DESC LIMIT " + ProcessData.form_num_games + ") a GROUP BY res_points_home";
		final String query_away_res_lim = "SELECT res_points_away, COUNT(*) num_res FROM (SELECT * FROM fixture WHERE season = " + season + " AND team_away_id = ? AND res_points_away IS NOT NULL ORDER BY datetime DESC LIMIT " + ProcessData.form_num_games + ") a GROUP BY res_points_away";
		
		HashMap<Integer, Team> teamHash = new HashMap<Integer, Team>();
		
		Cursor curAll = db.rawQuery("SELECT team_id, c_h_rating, c_h_gpg, c_h_gcpg, c_a_rating, c_a_gpg, c_a_gcpg FROM team_season WHERE season = " + (season - 1), null);
		curAll.moveToFirst();
		while (!curAll.isAfterLast()) {
			Team t = new Team();
			t.c_prev_h_rating = curAll.getFloat(1);
			t.c_prev_h_gpg = curAll.getFloat(2);
			t.c_prev_h_gcpg = curAll.getFloat(3);
			t.c_prev_a_rating = curAll.getFloat(4);
			t.c_prev_a_gpg = curAll.getFloat(5);
			t.c_prev_a_gcpg = curAll.getFloat(6);
			teamHash.put(curAll.getInt(0), t);
			curAll.moveToNext();
		}
		curAll.close();
		
		curAll = db.rawQuery("SELECT team_home_id, SUM(res_goals_home) c_sum_h_gs, SUM(res_goals_away) c_sum_h_gc FROM fixture WHERE season = " + season + " AND res_points_home IS NOT NULL GROUP BY team_home_id", null);
		curAll.moveToFirst();
		while (!curAll.isAfterLast()) {
			Team t = teamHash.get(curAll.getInt(0));
			t.c_sum_h_gs = curAll.getInt(1);
			t.c_sum_h_gc = curAll.getInt(2);
			curAll.moveToNext();
		}
		curAll.close();
		
		curAll = db.rawQuery("SELECT team_away_id, SUM(res_goals_away) c_sum_a_gs, SUM(res_goals_home) c_sum_a_gc FROM fixture WHERE season = " + season + " AND res_points_away IS NOT NULL GROUP BY team_away_id", null);
		curAll.moveToFirst();
		while (!curAll.isAfterLast()) {
			Team t = teamHash.get(curAll.getInt(0));
			t.c_sum_a_gs = curAll.getInt(1);
			t.c_sum_a_gc = curAll.getInt(2);
			curAll.moveToNext();
		}
		curAll.close();
		
		curAll = db.rawQuery("SELECT team_home_id, res_points_home, COUNT(*) num_res FROM fixture WHERE season = " + season + " AND res_points_home IS NOT NULL GROUP BY team_home_id, res_points_home ORDER BY team_home_id ASC", null);
		curAll.moveToFirst();
		int id = -1;
		Team t = null;
		while (!curAll.isAfterLast()) {
			int idnew = curAll.getInt(0);
			if (idnew != id) {
				t = teamHash.get(idnew);
				id = idnew;
			}
			int res_points_indiv = curAll.getInt(1);
			int res_count = curAll.getInt(2);
			if (res_points_indiv == 0) {
				t.c_sum_h_l = res_count;
			} else if (res_points_indiv == 1) {
				t.c_sum_h_d = res_count;
			} else {
				t.c_sum_h_w = res_count;
			}
			curAll.moveToNext();
		}
		curAll.close();
		
		curAll = db.rawQuery("SELECT team_away_id, res_points_away, COUNT(*) num_res FROM fixture WHERE season = " + season + " AND res_points_away IS NOT NULL GROUP BY team_away_id, res_points_away ORDER BY team_away_id ASC", null);
		curAll.moveToFirst();
		id = -1;
		t = null;
		
		while (!curAll.isAfterLast()) {
			int idnew = curAll.getInt(0);
			if (idnew != id) {
				t = teamHash.get(idnew);
				id = idnew;
			}
			int res_points_indiv = curAll.getInt(1);
			int res_count = curAll.getInt(2);
			if (res_points_indiv == 0) {
				t.c_sum_a_l = res_count;
			} else if (res_points_indiv == 1) {
				t.c_sum_a_d = res_count;
			} else {
				t.c_sum_a_w = res_count;
			}
			curAll.moveToNext();
		}
		curAll.close();
		
		curAll = db.rawQuery("SELECT team_id, COUNT(*) num_games_rec FROM (SELECT team_home_id team_id FROM fixture WHERE season = " + season + " AND gameweek >= " + gw_from + " AND gameweek <= " + gameweek + " AND res_points_home IS NOT NULL UNION ALL SELECT team_away_id team_id FROM fixture WHERE season = " + season + " AND gameweek >= " + gw_from + " AND gameweek <= " + gameweek + " AND res_points_away IS NOT NULL) GROUP BY team_id", null);
		curAll.moveToFirst();
		while (!curAll.isAfterLast()) {
			t = teamHash.get(curAll.getInt(0));
			t.res_num_games_rec = curAll.getInt(1);
			curAll.moveToNext();
		}
		curAll.close();
		
		curAll = db.rawQuery("SELECT pl_team_id, SUM(bonus) bonus FROM player_match WHERE season = " + season + " AND result_points IS NOT NULL GROUP BY pl_team_id", null);
		curAll.moveToFirst();
		while (!curAll.isAfterLast()) {
			t = teamHash.get(curAll.getInt(0));
			t.bonus_points = curAll.getInt(1);
			curAll.moveToNext();
		}
		curAll.close();
		
		curAll = db.rawQuery("SELECT team_home_id, COUNT(*) clean_sheets FROM fixture WHERE season = " + season + " AND res_points_home IS NOT NULL AND res_goals_away = 0 GROUP BY team_home_id", null);
		curAll.moveToFirst();
		while (!curAll.isAfterLast()) {
			t = teamHash.get(curAll.getInt(0));
			t.clean_sheets_home = curAll.getInt(1);
			curAll.moveToNext();
		}
		curAll.close();
		
		curAll = db.rawQuery("SELECT team_away_id, COUNT(*) clean_sheets FROM fixture WHERE season = " + season + " AND res_points_away IS NOT NULL AND res_goals_home = 0 GROUP BY team_away_id", null);
		curAll.moveToFirst();
		while (!curAll.isAfterLast()) {
			t = teamHash.get(curAll.getInt(0));
			t.clean_sheets_away = curAll.getInt(1);
			curAll.moveToNext();
		}
		curAll.close();
		
		ContentValues updateVals = new ContentValues();
		final String t_team_season = "team_season";
		final String t_where = "team_id = ? AND season = " + season;
		String[] t_wArgs = { "0" };
		
		int team_id_ind = curTeam.getColumnIndex("team_id");
		//int games_played_ind = curTeam.getColumnIndex("games_played");
		//int goal_diff_ind = curTeam.getColumnIndex("goal_diff");
		//int points_ind = curTeam.getColumnIndex("points");
		
		// loop teams
		while (!curTeam.isAfterLast()) {
			team_id = curTeam.getInt(team_id_ind);
			//games_played = curTeam.getInt(games_played_ind);
			//goal_diff = curTeam.getInt(goal_diff_ind);
			//points = curTeam.getInt(points_ind);
			t_wArgs[0] = String.valueOf(team_id);
			//App.log("team " + team_id);
			
			t = teamHash.get(team_id);
			if (t != null) {
				// loop modes: form and total
				for (int mode = 1; mode<=2; mode++) {
					//App.log("mode " + mode);
					// home goals scored/conceded for completed games
					if (mode == 1) {
						cur = db.rawQuery(query_home_goals_lim, t_wArgs);
						cur.moveToFirst();
						c_sum_h_gs = cur.getInt(0);
						c_sum_h_gc = cur.getInt(1);
						cur.close();
					} else {
						c_sum_h_gs = t.c_sum_h_gs;
						c_sum_h_gc = t.c_sum_h_gc;
					}
					checkDiff(c_sum_h_gs, Field_team.f_c_sum_h_gs, curTeam, mode);
					checkDiff(c_sum_h_gc, Field_team.f_c_sum_h_gc, curTeam, mode);
					
					// away goals scored/conceded for completed games
					if (mode == 1) {
						cur = db.rawQuery(query_away_goals_lim, t_wArgs);
						cur.moveToFirst();
						c_sum_a_gs = cur.getInt(0);
						c_sum_a_gc = cur.getInt(1);
						cur.close();
					} else {
						c_sum_a_gs = t.c_sum_a_gs;
						c_sum_a_gc = t.c_sum_a_gc;
					}
					checkDiff(c_sum_a_gs, Field_team.f_c_sum_a_gs, curTeam, mode);
					checkDiff(c_sum_a_gc, Field_team.f_c_sum_a_gc, curTeam, mode);
					
					// home w/d/l/points for completed games
					if (mode == 1) {
						cur = db.rawQuery(query_home_res_lim, t_wArgs);
						cur.moveToFirst();
						c_sum_h_l = 0;
						c_sum_h_d = 0;
						c_sum_h_w = 0;
						while (!cur.isAfterLast()) {
							int res_points_indiv = cur.getInt(0);
							int res_count = cur.getInt(1);
							
							if (res_points_indiv == 0) {
								c_sum_h_l = res_count;
							} else if (res_points_indiv == 1) {
								c_sum_h_d = res_count;
							} else {
								c_sum_h_w = res_count;
							}
							
							cur.moveToNext();
						}
						cur.close();
					} else {
						c_sum_h_w = t.c_sum_h_w;
						c_sum_h_d = t.c_sum_h_d;
						c_sum_h_l = t.c_sum_h_l;
					}
					checkDiff(c_sum_h_w, Field_team.f_c_sum_h_w, curTeam, mode);
					checkDiff(c_sum_h_d, Field_team.f_c_sum_h_d, curTeam, mode);
					checkDiff(c_sum_h_l, Field_team.f_c_sum_h_l, curTeam, mode);
					
					// away w/d/l/points for completed games
					if (mode == 1) {
						cur = db.rawQuery(query_away_res_lim, t_wArgs);
						cur.moveToFirst();
						c_sum_a_l = 0;
						c_sum_a_d = 0;
						c_sum_a_w = 0;
						while (!cur.isAfterLast()) {
							int res_points_indiv = cur.getInt(0);
							int res_count = cur.getInt(1);
							
							if (res_points_indiv == 0) {
								c_sum_a_l = res_count;
							} else if (res_points_indiv == 1) {
								c_sum_a_d = res_count;
							} else {
								c_sum_a_w = res_count;
							}
							
							cur.moveToNext();
						}
						cur.close();
					} else {
						c_sum_a_w = t.c_sum_a_w;
						c_sum_a_d = t.c_sum_a_d;
						c_sum_a_l = t.c_sum_a_l;
					}
					checkDiff(c_sum_a_w, Field_team.f_c_sum_a_w, curTeam, mode);
					checkDiff(c_sum_a_d, Field_team.f_c_sum_a_d, curTeam, mode);
					checkDiff(c_sum_a_l, Field_team.f_c_sum_a_l, curTeam, mode);
					
					c_h_games = c_sum_h_w + c_sum_h_d + c_sum_h_l;
					c_a_games = c_sum_a_w + c_sum_a_d + c_sum_a_l;
					
					if (c_h_games > 0) {
						c_h_wd_perc = ProcessData.trunc((c_sum_h_w + c_sum_h_d) / c_h_games);
					} else {
						c_h_wd_perc = 0;
					}
					if (c_a_games > 0) {
						c_a_wd_perc = ProcessData.trunc((c_sum_a_w + c_sum_a_d) / c_a_games);
					} else {
						c_a_wd_perc = 0;
					}
					checkDiff(c_h_wd_perc, Field_team.f_c_h_wd_perc, curTeam, mode);
					checkDiff(c_a_wd_perc, Field_team.f_c_a_wd_perc, curTeam, mode);
					
					if (c_h_games > 0) {
						c_h_w_perc = ProcessData.trunc(c_sum_h_w / c_h_games);
					} else {
						c_h_w_perc = 0;
					}
					if (c_a_games > 0) {
						c_a_w_perc = ProcessData.trunc(c_sum_a_w / c_a_games);
					} else {
						c_a_w_perc = 0;
					}
					checkDiff(c_h_w_perc, Field_team.f_c_h_w_perc, curTeam, mode);
					checkDiff(c_a_w_perc, Field_team.f_c_a_w_perc, curTeam, mode);
					
					if (c_h_games > 0) {
						c_h_gpg = c_sum_h_gs / c_h_games;
					} else {
						c_h_gpg = 0;
					}
					if (c_a_games > 0) {
						c_a_gpg = c_sum_a_gs / c_a_games;
					} else {
						c_a_gpg = 0;
					}
					
					if (c_h_games > 0) {
						c_h_gcpg = c_sum_h_gc / c_h_games;
					} else {
						c_h_gcpg = 0;
					}
					if (c_a_games > 0) {
						c_a_gcpg = c_sum_a_gc / c_a_games;
					} else {
						c_a_gcpg = 0;
					}
					
					c_sum_games_played = c_h_games + c_a_games;
					c_sum_goal_diff = c_sum_h_gs + c_sum_a_gs - c_sum_h_gc - c_sum_a_gc;
					c_sum_points = (c_sum_h_w * 3) + (c_sum_h_d * 1) + (c_sum_a_w * 3) + (c_sum_a_d * 1);
					checkDiff(c_sum_games_played, Field_team.f_c_sum_games_played, curTeam, mode);
					checkDiff(c_sum_goal_diff, Field_team.f_c_sum_goal_diff, curTeam, mode);
					checkDiff(c_sum_points, Field_team.f_c_sum_points, curTeam, mode);
					
					// if near start of season, use old stats, less and less as going into season
					if (gameweek == 0) {
						c_h_gpg = t.c_prev_h_gpg;
						c_h_gcpg = t.c_prev_h_gcpg;
						c_a_gpg = t.c_prev_a_gpg;
						c_a_gcpg = t.c_prev_a_gcpg;
					} else if (gameweek < CROSSOVER_GW) {
						float xold = CROSSOVER_GW - gameweek;
						float xnew = gameweek;
						c_h_gpg = ((t.c_prev_h_gpg * xold) + (c_h_gpg * xnew)) / CROSSOVER_GW;
						c_h_gcpg = ((t.c_prev_h_gcpg * xold) + (c_h_gcpg * xnew)) / CROSSOVER_GW;
						c_a_gpg = ((t.c_prev_a_gpg * xold) + (c_a_gpg * xnew)) / CROSSOVER_GW;
						c_a_gcpg = ((t.c_prev_a_gcpg * xold) + (c_a_gcpg * xnew)) / CROSSOVER_GW;
					}
					
					c_h_gpg = ProcessData.trunc(c_h_gpg);
					c_h_gcpg = ProcessData.trunc(c_h_gcpg);
					c_a_gpg = ProcessData.trunc(c_a_gpg);
					c_a_gcpg = ProcessData.trunc(c_a_gcpg);
					
					c_h_rating = (c_h_w_perc * 3) + c_h_wd_perc + c_h_gpg - c_h_gcpg;
					c_h_rating = (c_h_rating * 180) + 300;
					c_a_rating = (c_a_w_perc * 3) + c_a_wd_perc + c_a_gpg - c_a_gcpg;
					c_a_rating = (c_a_rating * 180) + 300;
					
					// if near start of season, use old stats, less and less as going into season
					if (gameweek == 0) {
						c_h_rating = t.c_prev_h_rating;
						c_a_rating = t.c_prev_a_rating;
					} else if (gameweek < CROSSOVER_GW) {
						float xold = CROSSOVER_GW - gameweek;
						float xnew = gameweek;
						c_h_rating = ((t.c_prev_h_rating * xold) + (c_h_rating * xnew)) / CROSSOVER_GW;
						c_a_rating = ((t.c_prev_a_rating * xold) + (c_a_rating * xnew)) / CROSSOVER_GW;
					}
					
					int c_h_rating_trunc = (int) c_h_rating;
					if (c_h_rating_trunc < MIN_TEAM_RATING) c_h_rating_trunc = MIN_TEAM_RATING;
					
					int c_a_rating_trunc = (int) c_a_rating;
					if (c_a_rating_trunc < MIN_TEAM_RATING) c_a_rating_trunc = MIN_TEAM_RATING;
					checkDiff(c_h_rating_trunc, Field_team.f_c_h_rating, curTeam, mode);
					checkDiff(c_a_rating_trunc, Field_team.f_c_a_rating, curTeam, mode);
					
					if (mode == 1) {
						c_h_form_rating = c_h_rating_trunc;
						c_a_form_rating = c_a_rating_trunc;
						checkDiff(c_h_form_rating, Field_team.f_c_h_form_rating, curTeam);
						checkDiff(c_a_form_rating, Field_team.f_c_a_form_rating, curTeam);
					}
					
				} // mode loop
				
				// check for mismatches
				/*
				c_sum_error = 0;
				if (games_played != c_sum_games_played) {
					App.log("error gp");
					c_sum_error = 1;
				} else if (goal_diff != c_sum_goal_diff) {
					App.log("error gd");
					c_sum_error = 1;
				} else if (points != c_sum_points) {
					App.log("error p");
					c_sum_error = 1;
				}
				checkDiff(c_sum_error, Field_team.f_c_sum_error, curTeam);
				*/
				
				c_sum_gs = c_sum_h_gs + c_sum_a_gs;
				c_sum_gc = c_sum_h_gc + c_sum_a_gc;
				checkDiff(c_sum_gs, Field_team.f_c_sum_gs, curTeam);
				checkDiff(c_sum_gc, Field_team.f_c_sum_gc, curTeam);
				
				c_gpg = ProcessData.trunc(c_sum_gs / c_sum_games_played);
				c_gcpg = ProcessData.trunc(c_sum_gc / c_sum_games_played);
				checkDiff(c_gpg, Field_team.f_c_gpg, curTeam);
				checkDiff(c_gcpg, Field_team.f_c_gcpg, curTeam);
				
				// form fiddle on gpg/gcpg (post ratings calc..since this is based on that..)
				float tmp = c_h_form_rating / c_h_rating;
				if (tmp > ProcessData.form_thresh) {
					c_h_gpg = ProcessData.trunc(c_h_gpg * ProcessData.form_gpg_boost);
					c_h_gcpg = ProcessData.trunc(c_h_gcpg * (1/ProcessData.form_gpg_boost));
				}
				tmp = c_a_form_rating / c_a_rating;
				if (tmp > ProcessData.form_thresh) {
					c_a_gpg = ProcessData.trunc(c_a_gpg * ProcessData.form_gpg_boost);
					c_a_gcpg = ProcessData.trunc(c_a_gcpg * (1/ProcessData.form_gpg_boost));
				}
				// and for bad form
				tmp = c_h_form_rating / c_h_rating;
				if (tmp < (1/ProcessData.form_thresh)) {
					c_h_gpg = ProcessData.trunc(c_h_gpg * (1/ProcessData.form_gpg_boost));
					c_h_gcpg = ProcessData.trunc(c_h_gcpg * ProcessData.form_gpg_boost);
				}
				tmp = c_a_form_rating / c_a_rating;
				if (tmp < (1/ProcessData.form_thresh)) {
					c_a_gpg = ProcessData.trunc(c_a_gpg * (1/ProcessData.form_gpg_boost));
					c_a_gcpg = ProcessData.trunc(c_a_gcpg * ProcessData.form_gpg_boost);
				}
				
				checkDiff(c_h_gpg, Field_team.f_c_h_gpg, curTeam);
				checkDiff(c_a_gpg, Field_team.f_c_a_gpg, curTeam);
				checkDiff(c_h_gcpg, Field_team.f_c_h_gcpg, curTeam);
				checkDiff(c_a_gcpg, Field_team.f_c_a_gcpg, curTeam);
				
				c_cs_home = t.clean_sheets_home;
				c_cs_away = t.clean_sheets_away;
				c_cs = c_cs_home + c_cs_away;
				c_cs_perc = ProcessData.trunc(c_cs / c_sum_games_played);
				checkDiff(c_cs_home, Field_team.f_c_cs_home, curTeam);
				checkDiff(c_cs_away, Field_team.f_c_cs_away, curTeam);
				checkDiff(c_cs, Field_team.f_c_cs, curTeam);
				checkDiff(c_cs_perc, Field_team.f_c_cs_perc, curTeam);
				
				TeamStat ts = new TeamStat();
				ts.c_a_form_rating = c_a_form_rating;
				ts.c_a_gcpg = c_a_gcpg;
				ts.c_a_gpg = c_a_gpg;
				ts.c_a_rating = c_a_rating;
				ts.c_h_form_rating = c_h_form_rating;
				ts.c_h_gcpg = c_h_gcpg;
				ts.c_h_gpg = c_h_gpg;
				ts.c_h_rating = c_h_rating;
				ts.res_num_games_rec = t.res_num_games_rec;
				ts.fixtures = new ArrayList<Fixture>();
				parent.teamStats.put(team_id, ts);
				
				// lookup most player goalkeeper recently
				int first_choice_gk = NONE;
				Cursor curGk = db.rawQuery("select pm.player_player_id, sum(pm.minutes) mins"
										+ " from player_match pm, player_season ps"
										+ " where pm.season = " + season + " and ps.season=" + season
										+ " and pm.player_player_id = ps.player_id"
										+ " and ps.position=" + SquadUtil.POS_KEEP + " and pm.pl_team_id=" + team_id
										+ " and pm.gameweek > (" + gameweek + " - " + FIRST_CHOICE_KEEPER_NUM_WEEKS + ")"
										+ " group by pm.player_player_id"
										+ " order by mins desc"
										+ " limit 1", null);
				curGk.moveToFirst();
				if (!curGk.isAfterLast()) {
					if (curGk.getInt(1) > 0) {
						first_choice_gk = curGk.getInt(0);
					}
				}
				curGk.close();
				
				// if that failed (ie start of season) then use most expensive
				if (first_choice_gk == NONE) {
					curGk = db.rawQuery("SELECT ps.player_id FROM player_season ps, player p"
							+ " WHERE ps.season = " + season + " AND ps.player_id = p._id"
							+ " AND ps.position=" + SquadUtil.POS_KEEP + " AND p.team_id=" + team_id
							+ " ORDER BY ps.price DESC limit 1", null);
					curGk.moveToFirst();
					if (!curGk.isAfterLast()) {
						first_choice_gk = curGk.getInt(0);
					}
					curGk.close();
				}
				
				checkDiff(first_choice_gk, Field_team.gk_player_id, curTeam);
				
				c_bonus_per_game = ProcessData.trunc(t.bonus_points / c_sum_games_played);
				checkDiff(c_bonus_per_game, Field_team.f_c_bonus_per_game, curTeam);
				
				int c_h_rating_trunc = (int) c_h_rating;
				if (c_h_rating_trunc < MIN_TEAM_RATING) c_h_rating_trunc = MIN_TEAM_RATING;
				
				int c_a_rating_trunc = (int) c_a_rating;
				if (c_a_rating_trunc < MIN_TEAM_RATING) c_a_rating_trunc = MIN_TEAM_RATING;
				
				updateVals.put(Field_team.f_c_sum_h_gs, c_sum_h_gs);
				updateVals.put(Field_team.f_c_sum_h_gc, c_sum_h_gc);
				updateVals.put(Field_team.f_c_sum_h_w, c_sum_h_w);
				updateVals.put(Field_team.f_c_sum_h_d, c_sum_h_d);
				updateVals.put(Field_team.f_c_sum_h_l, c_sum_h_l);
				updateVals.put(Field_team.f_c_h_wd_perc, c_h_wd_perc);
				updateVals.put(Field_team.f_c_h_w_perc, c_h_w_perc);
				updateVals.put(Field_team.f_c_h_rating, c_h_rating_trunc);
				updateVals.put(Field_team.f_c_h_form_rating, c_h_form_rating);
				updateVals.put(Field_team.f_c_h_gpg, c_h_gpg);
				updateVals.put(Field_team.f_c_h_gcpg, c_h_gcpg);
				//updateVals.put(Field_team.f_c_sum_error, c_sum_error);
				updateVals.put(Field_team.f_c_sum_games_played, c_sum_games_played);
				updateVals.put(Field_team.f_c_sum_goal_diff, c_sum_goal_diff);
				updateVals.put(Field_team.f_c_sum_points, c_sum_points);
				updateVals.put(Field_team.f_games_played, c_sum_games_played);
				updateVals.put(Field_team.f_goal_diff, c_sum_goal_diff);
				updateVals.put(Field_team.f_points, c_sum_points);
				updateVals.put(Field_team.f_c_sum_a_gs, c_sum_a_gs);
				updateVals.put(Field_team.f_c_sum_a_gc, c_sum_a_gc);
				updateVals.put(Field_team.f_c_sum_a_w, c_sum_a_w);
				updateVals.put(Field_team.f_c_sum_a_d, c_sum_a_d);
				updateVals.put(Field_team.f_c_sum_a_l, c_sum_a_l);
				updateVals.put(Field_team.f_c_a_wd_perc, c_a_wd_perc);
				updateVals.put(Field_team.f_c_a_w_perc, c_a_w_perc);
				updateVals.put(Field_team.f_c_a_rating, c_a_rating_trunc);
				updateVals.put(Field_team.f_c_a_form_rating, c_a_form_rating);
				updateVals.put(Field_team.f_c_a_gpg, c_a_gpg);
				updateVals.put(Field_team.f_c_a_gcpg, c_a_gcpg);
				updateVals.put(Field_team.f_c_bonus_per_game, c_bonus_per_game);
				updateVals.put(Field_team.f_c_gpg, c_gpg);
				updateVals.put(Field_team.f_c_gcpg, c_gcpg);
				updateVals.put(Field_team.f_c_sum_gs, c_sum_gs);
				updateVals.put(Field_team.f_c_sum_gc, c_sum_gc);
				updateVals.put(Field_team.f_c_cs_home, c_cs_home);
				updateVals.put(Field_team.f_c_cs_away, c_cs_away);
				updateVals.put(Field_team.f_c_cs, c_cs);
				updateVals.put(Field_team.f_c_cs_perc, c_cs_perc);
				updateVals.put(Field_team.gk_player_id, first_choice_gk);
				
				db.update(t_team_season, updateVals, t_where, t_wArgs);
			}
			
			i++;
			result.setProgress(i);
			
			curTeam.moveToNext();
		} // team loop
		
		curTeam.close();
		
		try {
			db.execSQL(DbGen.TRANSACTION_END);
		} catch (SQLException e) {
			App.exception(e);
		} catch (IllegalStateException e) {
			App.exception(e);
		}
		
		//Debug.stopMethodTracing();
		Long timeDiff = System.currentTimeMillis() - startTime;
		float timeSecs = (float)timeDiff / 1000;
		App.log("Finished processing teams in " + timeSecs + " seconds");
	}
	
	static void postProcessTeam(ProcessData parent, ProcRes result) {
		SQLiteDatabase db = parent.dbGen;
		int season = parent.season;
		int next_gameweek = parent.next_gameweek;
		
		//Debug.startMethodTracing("proc_team2", 15000000);
		App.log("Post-processing teams..");
		long startTime = System.currentTimeMillis();
		
		int games_played;
		float goal_diff;
		int points;
		
		// create mapping from player to sum row
		HashMap<Integer, Integer> homeHash = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> awayHash = new HashMap<Integer, Integer>();
		
		Cursor curPredHome = db.rawQuery("SELECT team_home_id, SUM(pred_points_home) pred_points, SUM(pred_goals_home - pred_goals_away) pred_goal_diff, COUNT(*) pred_games FROM fixture WHERE season = " + season + " AND res_goals_home IS NULL GROUP BY team_home_id", null);
		curPredHome.moveToFirst();
		while (!curPredHome.isAfterLast()) {
			homeHash.put(curPredHome.getInt(0), curPredHome.getPosition());
			curPredHome.moveToNext();
		}
		Cursor curPredAway = db.rawQuery("SELECT team_away_id, SUM(pred_points_away) pred_points, SUM(pred_goals_away - pred_goals_home) pred_goal_diff, COUNT(*) pred_games FROM fixture WHERE season = " + season + " AND res_goals_away IS NULL GROUP BY team_away_id", null);
		curPredAway.moveToFirst();
		while (!curPredAway.isAfterLast()) {
			awayHash.put(curPredAway.getInt(0), curPredAway.getPosition());
			curPredAway.moveToNext();
		}
		
		ContentValues updateVals = new ContentValues();
		final String t_team_season = "team_season";
		final String t_where = "team_id = ? AND season = " + season;
		String[] t_wArgs = { "0" };
		
		Cursor curTeam = db.rawQuery("SELECT * FROM team_season WHERE season = " + season, null);
		curTeam.moveToFirst();
		float teamCount = curTeam.getCount();
		
		result.setMaxName(teamCount, "PPTeam");
		
		db.execSQL(DbGen.TRANSACTION_IMMEDIATE_BEGIN);
		
		float i = 0;
		Integer h;
		Integer a;
		
		// loop teams
		while (!curTeam.isAfterLast()) {
			team_id = curTeam.getInt(curTeam.getColumnIndex("team_id"));
			games_played = curTeam.getInt(curTeam.getColumnIndex("games_played"));
			goal_diff = curTeam.getInt(curTeam.getColumnIndex("goal_diff"));
			points = curTeam.getInt(curTeam.getColumnIndex("points"));
			
			t_wArgs[0] = String.valueOf(team_id);
			
			h = homeHash.get(team_id);
			a = awayHash.get(team_id);
			if (h != null) {
				curPredHome.moveToPosition(h);
				games_played += curPredHome.getInt(3);
				points += curPredHome.getInt(1);
				goal_diff += (curPredHome.getFloat(2) / 100);
			}
			if (a != null) {
				curPredAway.moveToPosition(a);
				games_played += curPredAway.getInt(3);
				points += curPredAway.getInt(1);
				goal_diff += (curPredAway.getFloat(2) / 100);
			}
			
			goal_diff = ProcessData.trunc(goal_diff);
			
			updateVals.put(Field_postTeam.f_pred_games, games_played);
			checkDiff(games_played, Field_postTeam.f_pred_games, curTeam);
			
			updateVals.put(Field_postTeam.f_pred_points, points);
			checkDiff(points, Field_postTeam.f_pred_points, curTeam);
			
			updateVals.put(Field_postTeam.f_pred_goal_diff, goal_diff);
			checkDiff(goal_diff, Field_postTeam.f_pred_goal_diff, curTeam);
			
			db.update(t_team_season, updateVals, t_where, t_wArgs);
			
			i++;
			result.setProgress(i);
			
			curTeam.moveToNext();
		} // team loop
		
		curTeam.close();
		curPredHome.close();
		curPredAway.close();
		
		App.log("calculating team rotations...");
		
		db.execSQL("DELETE FROM team_rotation");
		
		HashMap<Integer, Float> team2gkprice = new HashMap<Integer, Float>();
		
		// work out defensive rotations
		// ... first load first-choice keepers for each team
		Cursor curGK = db.rawQuery("SELECT ts.team_id, ps.price FROM team_season ts, player_season ps WHERE ts.season = " + season + " AND ps.season = " + season + " AND ps.player_id = ts.gk_player_id", null);
		curGK.moveToFirst();
		while (!curGK.isAfterLast()) {
			float price = curGK.getFloat(1) / 10f;
			team2gkprice.put(curGK.getInt(0), price);
			curGK.moveToNext();
		}
		curGK.close();
		
		final String a_string = "a";
		final String b_string = "b";
		
		// populate list of teams competing this season
		int[] team_ids = new int[NUM_TEAMS];
		int team_i = 0;
		for (int team : App.team_fpl2id.values()) {
			team_ids[team_i] = team;
			team_i++;
		}
		
		// now loop team pairings
		for (int t_i_a=0; t_i_a<NUM_TEAMS; t_i_a++) {
			for (int t_i_b=t_i_a+1; t_i_b<NUM_TEAMS; t_i_b++) {
				int team_a = team_ids[t_i_a];
				int team_b = team_ids[t_i_b];
				
				ArrayList<Fixture> fix_a = parent.teamStats.get(team_a).fixtures;
				ArrayList<Fixture> fix_b = parent.teamStats.get(team_b).fixtures;
				
				float cs_perc = 0;
				int gameweeks_home = 0;
				float num_gameweeks = 0;
				int games_below_x = 0;
				float cost = team2gkprice.get(team_a) + team2gkprice.get(team_b);
				
				StringBuffer ticker_string = new StringBuffer();
				boolean ticker_first = true;
				
				for (int gw=next_gameweek; gw<=App.num_gameweeks; gw++) {
					GWRet cs_a = getGwCS(fix_a, gw);
					GWRet cs_b = getGwCS(fix_b, gw);
					
					boolean home = cs_a.has_home;
					float max = cs_a.cs_perc;
					int below_x = cs_a.below_x;
					String which_team = a_string;
					if (cs_b.cs_perc > max) {
						max = cs_b.cs_perc;
						home = cs_b.has_home;
						below_x = cs_b.below_x;
						which_team = b_string;
					}
					cs_perc += max;
					games_below_x += below_x;
					
					if (cs_a.cs_perc > cs_b.cs_perc) {
						if (cs_a.below_x < cs_b.below_x) App.log("err rot a");
					} else if (cs_a.cs_perc < cs_b.cs_perc) {
						if (cs_a.below_x > cs_b.below_x) App.log("err rot b");
					}
					
					if (cs_a.has_home || cs_b.has_home) gameweeks_home++;
					num_gameweeks ++;
					
					// ticker string build
					if (ticker_first) {
						ticker_first = false;
					} else {
						ticker_string.append(",");
					}
					// gw
					ticker_string.append(gw + "=");
					// home/away
					if (home) {
						ticker_string.append("1");
					} else {
						ticker_string.append("0");
					}
					// pred cs
					ticker_string.append("=" + ProcessData.trunc(max));
					// team a or b
					ticker_string.append("=" + which_team);
				}
				
				cs_perc = ProcessData.trunc(cs_perc / num_gameweeks);
				
				//AppClass.log(AppClass.id2team.get(team_a) + " + " + AppClass.id2team.get(team_b) + " cs_perc: " + cs_perc + " gameweeks_home: " + gameweeks_home + " games_below_x: " + games_below_x);
				ContentValues ins = new ContentValues();
				ins.put(Field_team_rotation.f_team_a, team_a);
				ins.put(Field_team_rotation.f_team_b, team_b);
				ins.put(Field_team_rotation.f_cs_perc, cs_perc);
				ins.put(Field_team_rotation.f_ticker_string, DbGen.compress(ticker_string.toString()));
				ins.put(Field_team_rotation.f_gameweeks_home, gameweeks_home);
				ins.put(Field_team_rotation.f_clean_sheets, games_below_x);
				ins.put(Field_team_rotation.f_cost, cost);
				db.insert(t_team_rotation, null, ins);
			}
		}
		
		try {
			db.execSQL(DbGen.TRANSACTION_END);
		} catch (SQLException e) {
			App.exception(e);
		} catch (IllegalStateException e) {
			App.exception(e);
		}
		
		//Debug.stopMethodTracing();
		Long timeDiff = System.currentTimeMillis() - startTime;
		float timeSecs = (float)timeDiff / 1000;
		App.log("Finished post-processing teams in " + timeSecs + " seconds");
	}
	
	private static final float CS_MAX = 1.65f;
	
	// teams post
	private static class Field_team_rotation {
		private static final String f_team_a = "team_a";
		private static final String f_team_b = "team_b";
		private static final String f_cs_perc = "cs_perc";
		private static final String f_ticker_string = "ticker_string";
		private static final String f_clean_sheets = "clean_sheets";
		private static final String f_cost = "cost";
		private static final String f_gameweeks_home = "gameweeks_home";
	}
	private static final String t_team_rotation = "team_rotation";
	
	private static class GWRet {
		boolean has_home;
		float cs_perc;
		int below_x;
	}
	
	private static GWRet getGwCS(ArrayList<Fixture> fix, int gw) {
		GWRet ret = new GWRet();
		ret.cs_perc = 0;
		ret.has_home = false;
		ret.below_x = 0;
		
		for (Fixture f : fix) {
			if (f.gameweek == gw) {
				float perc = getCSPerc(f.pred_goals_against);
				ret.cs_perc += perc;
				if (f.home) ret.has_home = true;
				if (f.pred_goals_against < ProcessData.clean_sheet_thresh) ret.below_x++;
			}
		}
		
		return ret;
	}
	
	public static float getCSPerc(float goals_against) {
		if (goals_against <= CS_MAX) {
			return (CS_MAX - goals_against) / CS_MAX;
		} else {
			return 0;
		}
	}
	
	private static void checkDiff(float newVal, String field, Cursor cur, int mode) {
		if (App.checkdiff) {
			float f = cur.getFloat(cur.getColumnIndex(field));
			if ((mode == 2) && (f != newVal)) App.log("team " + team_id + "... " + field + ": " + f + " -> " + newVal);
		}
	}
	private static void checkDiff(float newVal, String field, Cursor cur) {
		if (App.checkdiff) {
			checkDiff(newVal, field, cur, 2);
		}
	}
	
	private static class Team {
		public float c_sum_h_gs;
		public float c_sum_a_gs;
		public float c_sum_h_gc;
		public float c_sum_a_gc;
		public float c_sum_h_w;
		public float c_sum_h_d;
		public float c_sum_h_l;
		public float c_sum_a_w;
		public float c_sum_a_d;
		public float c_sum_a_l;
		public float res_num_games_rec;
		public float bonus_points;
		public float clean_sheets_home;
		public float clean_sheets_away;
		
		public float c_prev_h_gpg;
		public float c_prev_h_gcpg;
		public float c_prev_a_gpg;
		public float c_prev_a_gcpg;
		public float c_prev_h_rating;
		public float c_prev_a_rating;
	}
	
}
