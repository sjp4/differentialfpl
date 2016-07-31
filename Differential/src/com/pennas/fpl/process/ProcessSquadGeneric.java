package com.pennas.fpl.process;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.pennas.fpl.App;
import com.pennas.fpl.App.GWFixture;
import com.pennas.fpl.process.FPLService.ProcRes;
import com.pennas.fpl.util.DbGen;
import com.pennas.fpl.util.SquadUtil;

public class ProcessSquadGeneric {
	// fixtures
	private static final String f_season = "season";
	private static final String f_id = "id";
	private static final String f_value = "value";
	
	private static final String BONUS_W = "bonus_w";
	private static final String BONUS_D = "bonus_d";
	private static final String BONUS_L = "bonus_l";
	private static final String BONUS_GK = "bonus_gk";
	private static final String BONUS_DF = "bonus_df";
	private static final String BONUS_MF = "bonus_mf";
	private static final String BONUS_ST = "bonus_st";
	private static final String CS_PERC_HOME = "cs_perc_home";
	private static final String CS_PERC_AWAY = "cs_perc_away";
	private static final String CS_PERC = "cs_perc";
	
	private static final int SQUAD_FORM_WEEKS = 5;
	private static final int MAX_POINTS = 9999;
	
	static void processGeneric(ProcessData parent, ProcRes result) {
		SQLiteDatabase dbGen = parent.dbGen;
		int season = parent.season;
		
		App.log("Processing generic/squad..");
		
		long startTime = System.currentTimeMillis();
		
		dbGen.execSQL(DbGen.TRANSACTION_IMMEDIATE_BEGIN);
		
		// use stats from current gw if all matches have finished
		int gw_to = App.cur_gameweek;
		boolean gw_complete = true;
		for (GWFixture f : App.gwFixtures.values()) {
			if (!f.complete) {
				gw_complete = false;
				break;
			}
		}
		if (gw_complete) {
			gw_to++;
		}
		App.log("Rivals proc for < gw " + gw_to);
		
		Cursor curSquad = dbGen.rawQuery("SELECT _id, points, c_points FROM squad WHERE rival = 1 OR _id = " + App.my_squad_id, null);
		int base = curSquad.getCount();
		result.setMaxName(base + 10, "ProcGen");
		curSquad.moveToFirst();
		int i = 0;
		while (!curSquad.isAfterLast()) {
			int squadId = curSquad.getInt(0);
			float points = curSquad.getInt(2);
			if (points == 0) {
				points = curSquad.getInt(1);
			}
			App.log("squad: " + squadId);
			
			float form_points = 0;
			float form_weeks = 0;
			float weeks = 0;
			int hits = 0;
			int transfers = 0;
			int bench = 0;
			int goals = 0;
			int assists = 0;
			int cs = 0;
			int min_points = MAX_POINTS;
			int max_points = 0;
			
			int form_gw_from = gw_to - SQUAD_FORM_WEEKS;
			// 0
			Cursor curDetails = dbGen.rawQuery("SELECT gameweek, points, c_points, points_hit, transfers_next"
					// 5
					+ ", c_bench, c_goals, c_assists, c_cs"
					+ " FROM squad_gameweek"
					+ " WHERE squad_id = " + squadId + " AND gameweek < " + gw_to, null);
			curDetails.moveToFirst();
			while(!curDetails.isAfterLast()) {
				// gw points
				int gw_points = curDetails.getInt(2);
				if (gw_points == 0) {
					gw_points = curDetails.getInt(1);
				}
				//App.log("gw " + curDetails.getInt(0) + " pts " + gw_points);
				
				// form
				if (curDetails.getInt(0) >= form_gw_from) {
					form_points += gw_points;
					form_weeks++;
				}
				
				hits += curDetails.getInt(3);
				transfers += curDetails.getInt(4);
				bench += curDetails.getInt(5);
				goals += curDetails.getInt(6);
				assists += curDetails.getInt(7);
				cs += curDetails.getInt(8);
				
				if (gw_points < min_points) {
					min_points = gw_points;
				}
				if (gw_points > max_points) {
					max_points = gw_points;
				}
				
				weeks++;
				curDetails.moveToNext();
			}
			curDetails.close();
			
			        // 0
			Cursor curStats = dbGen.rawQuery("SELECT sgp.selected, sgp.captain, sgp.autosub, sgp.vc"
					// 4
					+ ", SUM(pm.total) points, sgp.gameweek"
					+ " FROM squad_gameweek_player sgp, player_match pm, player_season ps"
					+ " WHERE sgp.squad_id = " + squadId
					+ "   AND sgp.gameweek < " + gw_to
					+ "   AND pm.season = " + App.season
					+ "   AND pm.gameweek = sgp.gameweek"
					+ "   AND pm.player_player_id = ps.player_id"
					+ "   AND ps.season = " + App.season
					+ "   AND ps.fpl_id = sgp.fpl_player_id"
					+ " GROUP BY sgp.gameweek, sgp.fpl_player_id"
					+ " ORDER BY sgp.gameweek ASC", null);
			int gw = 0;
			int gw_highest = 0;
			float max_capt_total = 0;
			float captain_points = 0;
			int autosubs = 0;
			int autosub_points = 0;
			curStats.moveToFirst();
			while (!curStats.isAfterLast()) {
				int thisGw = curStats.getInt(5);
				if (thisGw != gw) {
					max_capt_total += gw_highest;
					//App.log("gw " + gw + " highest " + gw_highest);
					gw_highest = 0;
					gw = thisGw;
				}
				
				int p_points = curStats.getInt(4);
				
				int selected = curStats.getInt(0);
	            boolean autosub = (curStats.getInt(2) == 1);
	            if (autosub) {
	            	if (selected == SquadUtil.SEL_SELECTED) {
	            		selected = SquadUtil.SEL_SELECTED + 1;
	            	} else {
	            		selected = SquadUtil.SEL_SELECTED;
	            		autosubs++;
	            		autosub_points += p_points;
	            	}
	            }
	            
	            if (selected == SquadUtil.SEL_SELECTED) {
	            	if (p_points > gw_highest) {
	            		gw_highest = p_points;
	            	}
	            }
	            
	            int captain = curStats.getInt(1);
            	boolean vc = (curStats.getInt(3) == 1);
	            
            	if (vc) {
            		if (captain == SquadUtil.VICE_CAPTAIN) {
            			captain = SquadUtil.CAPTAIN;
            		}
            	}
            	
            	if (captain == SquadUtil.CAPTAIN) {
            		captain_points += p_points;
            	}
				
				curStats.moveToNext();
			}
			// add final gw
			max_capt_total += gw_highest;
			curStats.close();
			
			// gains in first week following each transfer (takin hits into account)
			Cursor curGain = dbGen.rawQuery("SELECT t_in, SUM(gw_pts) gw_pts FROM ("
					+ " SELECT DISTINCT t.t_in t_in, p.name, p.team_id, t.gameweek"
					+ " , (SELECT SUM(pm.total) points FROM player_match pm WHERE pm.season = " + App.season
					+ " AND pm.gameweek = t.gameweek AND pm.player_player_id = p._id) gw_pts"
					+ " FROM transfer t, player_season ps, player p"
					+ " WHERE t.squad_id = " + squadId
					+ " AND ps.season = " + App.season + " AND ps.fpl_id = t.player_fpl_id"
					+ " AND p._id = ps.player_id"
					+ " ) GROUP BY t_in", null);
			curGain.moveToFirst();
			int transfer_gains = 0 - hits;
			while(!curGain.isAfterLast()) {
				int t_in = curGain.getInt(0);
				int pts = curGain.getInt(1);
				if (t_in == 1) {
					transfer_gains += pts;
				} else {
					transfer_gains -= pts;
				}
				curGain.moveToNext();
			}
			curGain.close();
			
			int form = (int) ((form_points / form_weeks) * 100);
			int average_score = (int) ((points / weeks) * 100);
			//App.log("captain " + captain_points + " max " + max_capt_total);
			App.log("min " + min_points + " max " + max_points);
			int captain_percent = (int) ((captain_points / max_capt_total) * 100 * 100);
			
			dbGen.execSQL("UPDATE squad SET c_form = " + form
					+ ", c_hits = " + hits
					+ ", c_transfers = " + transfers
					+ ", c_bench = " + bench
					+ ", c_goals = " + goals
					+ ", c_assists = " + assists
					+ ", c_cs = " + cs
					+ ", c_lowest_week = " + min_points
					+ ", c_highest_week = " + max_points
					+ ", c_captain_points = " + captain_points
					+ ", c_captain_percent = " + captain_percent
					+ ", c_avg_week = " + average_score
					+ ", c_autosubs = " + autosubs
					+ ", c_autosub_points = " + autosub_points
					+ ", c_transfer_gains = " + transfer_gains
					+ " WHERE _id = " + squadId);
			
			i++;
			result.setProgress(i);
			
			curSquad.moveToNext();
		}
		curSquad.close();
		
		try {
			dbGen.execSQL(DbGen.TRANSACTION_END);
		} catch (SQLException e) {
			App.exception(e);
		} catch (IllegalStateException e) {
			App.exception(e);
		}
		
		////////
		processGen(parent, result, dbGen, season, 0);
		
		//Debug.stopMethodTracing();
		Long timeDiff = System.currentTimeMillis() - startTime;
		float timeSecs = (float)timeDiff / 1000;
		App.log("Finished processing generic in " + timeSecs + " seconds");
	}
	
	private static void processGen(ProcessData parent, ProcRes result, SQLiteDatabase dbGen, int season, int base) {
		App.log("Processing generic/generic..");
		
		ContentValues updateVals = new ContentValues();
		updateVals.put(f_season, season);
		final String t_generic_stat = "generic_stat";
		
		dbGen.execSQL(DbGen.TRANSACTION_IMMEDIATE_BEGIN);
		
		int bonus_w = 0;
		int bonus_d = 0;
		int bonus_l = 0;
		
		// bonus points for W/D/L
		Cursor cur = dbGen.rawQuery("SELECT result_points, SUM(bonus) bonus FROM player_match WHERE season = "
				+ season + " AND result_points IS NOT NULL GROUP BY result_points", null);
		cur.moveToFirst();
		while(!cur.isAfterLast()) {
			int pts = cur.getInt(0);
			int bon = cur.getInt(1);
			switch(pts) {
				case 0:
					bonus_l = bon;
					break;
				case 1:
					bonus_d = bon;
					break;
				case 3:
					bonus_w = bon;
					break;
			}
			cur.moveToNext();
		}
		cur.close();
		
		int bonus_gk = 0;
		int bonus_df = 0;
		int bonus_mf = 0;
		int bonus_st = 0;
		
		// bonus points by position
		Cursor curPos = dbGen.rawQuery("SELECT ps.position, SUM(pm.bonus) bonus"
				+ " FROM player_season ps, player_match pm"
				+ " WHERE ps.season = " + App.season + " AND pm.season = " + App.season + " AND pm.player_player_id = ps.player_id"
				+ " GROUP BY ps.position", null);
		curPos.moveToFirst();
		while(!curPos.isAfterLast()) {
			int pos = curPos.getInt(0);
			int bon = curPos.getInt(1);
			switch(pos) {
				case SquadUtil.POS_KEEP:
					bonus_gk = bon;
					break;
				case SquadUtil.POS_DEF:
					bonus_df = bon;
					break;
				case SquadUtil.POS_MID:
					bonus_mf = bon;
					break;
				case SquadUtil.POS_FWD:
					bonus_st = bon;
					break;
			}
			curPos.moveToNext();
		}
		curPos.close();
		
		result.setProgress(base + 2);
		
		updateVals.put(f_id, BONUS_W);
		updateVals.put(f_value, bonus_w);
		dbGen.replace(t_generic_stat, null, updateVals);
		
		updateVals.put(f_id, BONUS_D);
		updateVals.put(f_value, bonus_d);
		dbGen.replace(t_generic_stat, null, updateVals);
		
		updateVals.put(f_id, BONUS_L);
		updateVals.put(f_value, bonus_l);
		dbGen.replace(t_generic_stat, null, updateVals);
		
		updateVals.put(f_id, BONUS_GK);
		updateVals.put(f_value, bonus_gk);
		dbGen.replace(t_generic_stat, null, updateVals);
		
		updateVals.put(f_id, BONUS_DF);
		updateVals.put(f_value, bonus_df);
		dbGen.replace(t_generic_stat, null, updateVals);
		
		updateVals.put(f_id, BONUS_MF);
		updateVals.put(f_value, bonus_mf);
		dbGen.replace(t_generic_stat, null, updateVals);
		
		updateVals.put(f_id, BONUS_ST);
		updateVals.put(f_value, bonus_st);
		dbGen.replace(t_generic_stat, null, updateVals);
		
		result.setProgress(base + 4);
		
		// clean sheets - games played total
		cur = dbGen.rawQuery("SELECT COUNT(*) matches_played FROM fixture WHERE season = " + season + " AND got_bonus >= 1", null);
		cur.moveToFirst();
		float games_played = cur.getFloat(0);
		cur.close();
		
		// cs home
		cur = dbGen.rawQuery("SELECT COUNT(*) cs_home FROM fixture WHERE season = " + season + " AND got_bonus >= 1 AND res_goals_away = 0", null);
		cur.moveToFirst();
		float cs_h = cur.getFloat(0);
		cur.close();
		
		// cs away
		cur = dbGen.rawQuery("SELECT COUNT(*) cs_away FROM fixture WHERE season = " + season + " AND got_bonus >= 1 AND res_goals_home = 0", null);
		cur.moveToFirst();
		float cs_a = cur.getFloat(0);
		cur.close();
		
		result.setProgress(base + 6);
		
		float cs = cs_h + cs_a;
		
		float cs_perc_home = 0;
		float cs_perc_away = 0;
		float cs_perc = 0;
		if (games_played > 0) {
			cs_perc_home = ProcessData.trunc(cs_h / games_played);
			cs_perc_away = ProcessData.trunc(cs_a / games_played);
			cs_perc = ProcessData.trunc(cs / (games_played * 2f) );
		}
		
		updateVals.put(f_id, CS_PERC_HOME);
		updateVals.put(f_value, cs_perc_home);
		dbGen.replace(t_generic_stat, null, updateVals);
		
		updateVals.put(f_id, CS_PERC_AWAY);
		updateVals.put(f_value, cs_perc_away);
		dbGen.replace(t_generic_stat, null, updateVals);
		
		updateVals.put(f_id, CS_PERC);
		updateVals.put(f_value, cs_perc);
		dbGen.replace(t_generic_stat, null, updateVals);
		
		result.setProgress(base + 8);
		
		try {
			dbGen.execSQL(DbGen.TRANSACTION_END);
		} catch (SQLException e) {
			App.exception(e);
		} catch (IllegalStateException e) {
			App.exception(e);
		}
		
	}
	
}
