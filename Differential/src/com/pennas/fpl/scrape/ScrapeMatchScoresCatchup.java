package com.pennas.fpl.scrape;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import com.pennas.fpl.App;
import com.pennas.fpl.Vidiprinter;
import com.pennas.fpl.Vidiprinter.VidiEntry;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class ScrapeMatchScoresCatchup {
	
	//private static final int NAME = 1;
	private static final int MINS = 2;
	private static final int GOALS = 3;
	private static final int ASS = 4;
	private static final int CONC = 5;
	private static final int PENSAV = 6;
	private static final int PENMIS = 7;
	private static final int YEL = 8;
	private static final int RED = 9;
	private static final int SAV = 10;
	private static final int BON = 11;
	private static final int OG = 12;
	private static final int TOT = 13;
	private static final int PRICE = 14;
	private static final int TEAM_GOALS_ON_PITCH = 15;
	private static final int BPS = 16;
	
	private int season = App.season;
	private int gameweek = App.cur_gameweek;
	
	private static final String t_player_match = "player_match";
	private final String t_where = "season = " + season + " AND player_player_id = ? AND fixture_id = ?";
	private String[] t_wArgs = new String[2];
	
	private static String[] fields = { "", "", "minutes", "goals", "assists", "conceded", "pen_sav", "pen_miss", 
		"yellow", "red", "saves", "bonus", "own_goals", "total", "price", "team_goals_on_pitch", "bps" };
	
	private ContentValues updateVals = null;
	
	private Context c;
	
	//[ "2008/09", 3256, 12, 10, 0, 19, 0, 0, 0, 3, 1, 0, 46, 0, 121, 226 ]
	//[ "20 Aug", 2, "WBA(H) 2-1", 90, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 120, 1 ]
	//["20 Aug 20:00", 1, "MUN(H) 1-0", 90, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 21, 0, 60,   7]
	//["01 Nov 13:30",11,"SUN(H) 6-2",90,0,0,0,2,0,0,0,0,0,0,0,24,17,-2417,43,2]
	static final Pattern p_match_hist = Pattern.compile("\\[\\W*\"([0-9]+) ([A-Z][a-z]+) [0-9]+:[0-9]+\",\\W*([0-9]+),\\W*([A-Z][A-Z][A-Z])\\(([A-Z])\\) ([0-9]+)-([0-9]+)\""
			+ ",\\W*([0-9]+),\\W*([0-9]+),\\W*([0-9]+),\\W*([0-9]+),\\W*([0-9]+),\\W*([0-9]+),\\W*([0-9]+),\\W*([0-9]+),\\W*([0-9]+)"
			+ ",\\W*([0-9]+),\\W*([0-9]+),\\W*([0-9]+),\\W*(-?[0-9]+),\\W*(-?[0-9]+),\\W*(-?[0-9]+),\\W*([0-9]+),\\W*(-?[0-9]+)\\W*\\]", Pattern.MULTILINE);
	private static final int H_DATE_DAY = 1;
	private static final int H_DATE_MONTH = 2;
	static final int H_GW = 3;
	static final int H_OPP = 4;
	private static final int H_HOME_AWAY = 5;
	private static final int H_TEAM_SCORE = 6;
	//private static final int H_OPP_SCORE = 7;
	private static final int H_MINS = 8;
	private static final int H_GOALS = 9;
	private static final int H_ASSISTS = 10;
	//private static final int H_CS = 11;
	private static final int H_CONC = 12;
	private static final int H_OG = 13;
	private static final int H_PENSAV = 14;
	private static final int H_PENMISS = 15;
	private static final int H_YELLOW = 16;
	private static final int H_RED = 17;
	private static final int H_SAVES = 18;
	private static final int H_BONUS = 19;
	//private static final int H_EASPORTS = 20;
	private static final int H_BPS = 21;
	//private static final int H_NET_TRANSFERS = 22;
	private static final int H_PRICE = 23;
	static final int H_POINTS = 24;
	
	static int getInt(MatchResult m, int index) {
		return Integer.parseInt(m.group(index));
	}
	static String getString(MatchResult m, int index) {
		return m.group(index);
	}
	
	private static PlayerData findMatch(ArrayList<PlayerData> stored, int opp_team_id, int gameweek) {
		for (PlayerData pm : stored) {
			if ( (pm.opp_team_id == opp_team_id) && (pm.gameweek == gameweek) ) return pm;
		}
		return null;
	}
	
	private int fpl_id;
	private int player_id;
	private int proc_match_gw;
	private String pname;
	private String oppname;
	
	public static class PlayerData {
		public int gameweek, opp_team_id, fixture_id, player_fpl_id, player_id, position, pa_id;
		public String playername;
		public boolean home, use_old_minutes;
		public int pteam_goals_on_pitch, pmins, pgoals, pass, pconc, ppensav;
		public int ppenmis, pyel, pred, psav, pbon, pog, ptot, pbps;
		// only used for catchup api scrape
		public int pprice;
		public Integer subbed_on = null;
		public Integer subbed_off = null;
		public Integer red_card = null;
		
		public int end_minutes, start_minutes;
		public boolean sub;
		// only used in new bbc scrape, where subs aren't available straight away
		public String subbed_off_for;
	}
	
	private static final String f_season = "season";
	private static final String f_player_player_id = "player_player_id";
	private static final String f_fixture_id = "fixture_id";
	private static final String f_opp_team_id = "opp_team_id";
	private static final String f_pl_team_id = "pl_team_id";
	private static final String f_gameweek = "gameweek";
	private static final String f_is_home = "is_home";
	
	public static int calc_team_goals_on_pitch(int team_goals, int mins, int player_goals) {
		// pro-rate team goals on pitch if played less than 90 mins
		int ret = team_goals;
		if (mins == 0) {
			ret = 0;
		} else if (mins < 90) {
			// cast to float+ceil
			ret = (int) Math.ceil((team_goals / 90.0) * mins);
		}
		
		// never allow more goals by player than team_goals_on_pitch
		if (player_goals > ret) {
			ret = player_goals;
		}
		return ret;
	}
	
	public void getPlayerMatchHistCore(String player_name, int player_player_id, int player_fpl_id
			, Scanner scan, Context context, HashSet<Integer> gw_to_process) {
		ArrayList<PlayerData> player_matches = new ArrayList<PlayerData>();
		c = context;
		pname = player_name;
		player_id = player_player_id;
		fpl_id = player_fpl_id;
		
		// load existing match stats for player
		//                                        0
		Cursor curPm = App.dbGen.rawQuery("SELECT minutes, goals, assists, conceded, pen_sav, pen_miss"
				//    6
				+ " , yellow, red, saves, bonus, own_goals, total, gameweek, opp_team_id"
				//    14
				+ " , fixture_id, price, team_goals_on_pitch, bps FROM player_match WHERE season = " + season
				+ " AND player_player_id = " + player_id + " AND gameweek <= " + gameweek, null);
		curPm.moveToFirst();
		while (!curPm.isAfterLast()) {
			PlayerData p = new PlayerData();
			p.pmins = curPm.getInt(0);
			p.pgoals = curPm.getInt(1);
			p.pass = curPm.getInt(2);
			p.pconc = curPm.getInt(3);
			p.ppensav = curPm.getInt(4);
			p.ppenmis = curPm.getInt(5);
			p.pyel = curPm.getInt(6);
			p.pred = curPm.getInt(7);
			p.psav = curPm.getInt(8);
			p.pbon = curPm.getInt(9);
			p.pog = curPm.getInt(10);
			p.ptot = curPm.getInt(11);
			
			p.gameweek = curPm.getInt(12);
			p.opp_team_id = curPm.getInt(13);
			p.fixture_id = curPm.getInt(14);
			p.pprice = curPm.getInt(15);
			p.pteam_goals_on_pitch = curPm.getInt(16);
			p.pbps = curPm.getInt(17);
			//AppClass.log("Adding gw " + p.gameweek + " / " + AppClass.id2team.get(p.opp_team_id));
			player_matches.add(p);
			curPm.moveToNext();
		}
		curPm.close();
		
		// team (player scores inside match)
		while (scan.findWithinHorizon(p_match_hist, 0) != null) {
			MatchResult m = scan.match();
			PlayerData scP = new PlayerData();
			String team_short_name = getString(m, H_OPP);
			
			// get data from scrape into structure
			scP.gameweek = getInt(m, H_GW);
			proc_match_gw = scP.gameweek;
			scP.opp_team_id = App.team_short_2id.get(team_short_name);
			oppname = App.id2team.get(scP.opp_team_id);
			scP.pass = getInt(m, H_ASSISTS);
			scP.pbon = getInt(m, H_BONUS);
			scP.pconc = getInt(m, H_CONC);
			scP.pgoals = getInt(m, H_GOALS);
			scP.pmins = getInt(m, H_MINS);
			scP.pog = getInt(m, H_OG);
			scP.ppenmis = getInt(m, H_PENMISS);
			scP.ppensav = getInt(m, H_PENSAV);
			scP.pred = getInt(m, H_RED);
			scP.psav = getInt(m, H_SAVES);
			scP.pbps = getInt(m, H_BPS);
			scP.ptot = getInt(m, H_POINTS);
			scP.pprice = getInt(m, H_PRICE);
			scP.pyel = getInt(m, H_YELLOW);
			scP.pteam_goals_on_pitch = getInt(m, H_TEAM_SCORE);
			
			scP.pteam_goals_on_pitch = calc_team_goals_on_pitch(scP.pteam_goals_on_pitch, scP.pmins, scP.pgoals);
			
			// find stored player match record
			PlayerData stored = findMatch(player_matches, scP.opp_team_id, scP.gameweek);
			if (stored != null) {
				checkDiff(stored.pass, scP.pass, ASS);
				checkDiff(stored.pbon, scP.pbon, BON);
				checkDiff(stored.pconc, scP.pconc, CONC);
				checkDiff(stored.pgoals, scP.pgoals, GOALS);
				checkDiff(stored.pmins, scP.pmins, MINS);
				checkDiff(stored.pog, scP.pog, OG);
				checkDiff(stored.ppenmis, scP.ppenmis, PENMIS);
				checkDiff(stored.ppensav, scP.ppensav, PENSAV);
				checkDiff(stored.pred, scP.pred, RED);
				checkDiff(stored.psav, scP.psav, SAV);
				checkDiff(stored.pbps, scP.pbps, BPS);
				checkDiff(stored.ptot, scP.ptot, TOT);
				checkDiff(stored.pyel, scP.pyel, YEL);
				checkDiff(stored.pprice, scP.pprice, PRICE);
				// only update team_goals_on_pitch if the minutes changed.
				//  - calculations here WILL be different for a lot of subs because they are estimates here
				//  - but the ones from the original scrape are "correct", if the minutes played have not changed
				if (stored.pmins != scP.pmins) {
					checkDiff(stored.pteam_goals_on_pitch, scP.pteam_goals_on_pitch, TEAM_GOALS_ON_PITCH);
				}
				
				if (updateVals != null) {
					App.log("updating " + pname + " v " + oppname + " gw " + proc_match_gw);
					t_wArgs[0] = String.valueOf(player_id);
					t_wArgs[1] = String.valueOf(stored.fixture_id);
					App.dbGen.update(t_player_match, updateVals, t_where, t_wArgs);
					updateVals = null;
					gw_to_process.add(proc_match_gw);
				}
			} else if (scP.pmins > 0) {
				App.log("inserting match record for " + pname + " v " + oppname + " in gw " + scP.gameweek);
				ContentValues insertPM = new ContentValues();
				FixDat f = findFix(getString(m, H_DATE_DAY), getString(m, H_DATE_MONTH), scP.opp_team_id, proc_match_gw);
				if (f != null) {
					insertPM.put(f_fixture_id, f.fixture_id);
					insertPM.put(f_gameweek, proc_match_gw);
					if (getString(m, H_HOME_AWAY).equals("H")) {
						insertPM.put(f_is_home, 1);
					}
					insertPM.put(f_opp_team_id, scP.opp_team_id);
					insertPM.put(f_pl_team_id, f.pl_team_id);
					insertPM.put(f_player_player_id, player_player_id);
					insertPM.put(f_season, season);
					
					if (scP.pass > 0) {
						insertPM.put(fields[ASS], scP.pass);
					}
					if (scP.pbon > 0) {
						insertPM.put(fields[BON], scP.pbon);
					}
					if (scP.pconc > 0) {
						insertPM.put(fields[CONC], scP.pconc);
					}
					if (scP.pgoals > 0) {
						insertPM.put(fields[GOALS], scP.pgoals);
					}
					if (scP.pmins > 0) {
						insertPM.put(fields[MINS], scP.pmins);
					}
					if (scP.pog > 0) {
						insertPM.put(fields[OG], scP.pog);
					}
					if (scP.ppenmis > 0) {
						insertPM.put(fields[PENMIS], scP.ppenmis);
					}
					if (scP.ppensav > 0) {
						insertPM.put(fields[PENSAV], scP.ppensav);
					}
					if (scP.pred > 0) {
						insertPM.put(fields[RED], scP.pred);
					}
					if (scP.psav > 0) {
						insertPM.put(fields[SAV], scP.psav);
					}
					if (scP.pteam_goals_on_pitch > 0) {
						insertPM.put(fields[TEAM_GOALS_ON_PITCH], scP.pteam_goals_on_pitch);
					}
					insertPM.put(fields[TOT], scP.ptot);
					insertPM.put(fields[PRICE], scP.pprice);
					if (scP.pyel > 0) {
						insertPM.put(fields[YEL], scP.pyel);
					}
					if (scP.pbps > 0) {
						insertPM.put(fields[BPS], scP.pbps);
					}
					
					// insert
					App.dbGen.insert(t_player_match, null, insertPM);
					gw_to_process.add(proc_match_gw);
				} else {
					App.log("Couldn't find fixture");
				}
			}
		} // while pattern found
			
	}
	
	private static class FixDat {
		int fixture_id;
		int pl_team_id;
	}
	
	// find a fixture in the database, given the date (not time), gw and one of teams involved
	private FixDat findFix(String date_day, String date_month, int opp, int gw) {
		FixDat f = new FixDat();
		 
		try {
			long date = ScrapeFixtures.convertDate(date_day, date_month, "15", "00", season);
			
			long date_from = date - (6 * 60 * 60);
			long date_to = date + (8 * 60 * 60);
			
			Cursor curFix = App.dbGen.rawQuery("SELECT _id, team_home_id, team_away_id"
					+ " FROM fixture WHERE season = " + season + " AND datetime >= " + date_from
					+ " AND datetime <= " + date_to + " AND gameweek = " + gw
					+ " AND (team_home_id = " + opp + " OR team_away_id = " + opp + ")", null);
			curFix.moveToFirst();
			if (curFix.isAfterLast()) return null;
			f.fixture_id = curFix.getInt(0);
			int team_home_id = curFix.getInt(1);
			int team_away_id = curFix.getInt(2);
			curFix.close();
			
			if (team_home_id == opp) {
				f.pl_team_id = team_away_id;
			} else {
				f.pl_team_id = team_home_id;
			}
			return f;
		} catch (ParseException e) {
			App.exception(e);
			return null;
		}
	}
	
	private void checkDiff(int old, int newVal, int field) {
		if (old != newVal) {
			// update dbGen
			//changed = true;
			if (updateVals == null) {
				updateVals = new ContentValues();
			}
			updateVals.put(fields[field], newVal);
			App.log(pname + " " + proc_match_gw + " v " + oppname + ": " + fields[field] + ": " + old + " -> " + newVal);
			
			// vidiprinter
			if (field==GOALS || field==ASS || field==PENSAV || field==PENMIS || field==RED || field==OG) { //|| field==BON
				VidiEntry e = new VidiEntry();
				e.player_fpl_id = fpl_id;
				e.gameweek = proc_match_gw;
				int diff = newVal - old;
				
				switch (field) {
					case GOALS:
						e.category = Vidiprinter.CAT_GOAL; break;
					case ASS:
						e.category = Vidiprinter.CAT_ASS; break;
					case PENSAV:
						e.category = Vidiprinter.CAT_PENSAV; break;
					case PENMIS:
						e.category = Vidiprinter.CAT_PENMIS; break;
					case RED:
						e.category = Vidiprinter.CAT_RED; break;
					case OG:
						e.category = Vidiprinter.CAT_OG; break;
					/*case BON:
						e.category = Vidiprinter.CAT_BON; break;*/
				}
				
				if (diff > 0) {
					switch (field) {
						case GOALS:
							e.message = pname + " goal v " + oppname; break;
						case ASS:
							e.message = pname + " assist v " + oppname; break;
						case PENSAV:
							e.message = pname + " saved a penalty v " + oppname; break;
						case PENMIS:
							e.message = pname + " missed a penalty v " + oppname; break;
						case RED:
							e.message = pname + " was sent off v " + oppname; break;
						case OG:
							e.message = pname + " scored an own goal v " + oppname; break;
						/*case BON:
							e.message = pname + " was awarded " + diff + " bonus point(s) v " + oppname; break;*/
					}
				} else {
					// correction
					switch (field) {
						case GOALS:
							e.message = "CORRECTION: " + pname + " goal removed v " + oppname; break;
						case ASS:
							e.message = "CORRECTION: " + pname + " assist removed v " + oppname; break;
						case PENSAV:
							e.message = "CORRECTION: " + pname + " penalty save removed v " + oppname; break;
						case PENMIS:
							e.message = "CORRECTION: " + pname + " penalty miss removed v " + oppname; break;
						case RED:
							e.message = "CORRECTION: " + pname + " red card removed v " + oppname; break;
						case OG:
							e.message = "CORRECTION: " + pname + " own goal removed v " + oppname; break;
						/*case BON:
							e.message = "CORRECTION: " + pname + " bonus points removed v " + oppname; break;*/
					}
				}
				
				Vidiprinter.addVidi(e, c);
			}
		}
	}
	
	
	
}
