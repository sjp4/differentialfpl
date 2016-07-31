package com.pennas.fpl.scrape;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.pennas.fpl.App;
import com.pennas.fpl.Settings;
import com.pennas.fpl.Vidiprinter;
import com.pennas.fpl.App.GWFixture;
import com.pennas.fpl.Vidiprinter.VidiEntry;
import com.pennas.fpl.process.FPLService.ProcRes;
import com.pennas.fpl.scrape.ScrapePlayerLiveStats.LiveStatsRet;
import com.pennas.fpl.util.DbGen;
import com.pennas.fpl.util.SquadUtil;
import com.pennas.fpl.util.URLUtil;
import com.pennas.fpl.util.URLUtil.UrlObj;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class ScrapeMatchScores_New {
	//private static final int NAME = 1;
	private static final int MINS = 2;
	private static final int GOALS = 3;
	private static final int ASS = 4;
	//private static final int CS = 5;
	private static final int CONC = 6;
	private static final int OG = 7;
	private static final int PENSAV = 8;
	private static final int PENMIS = 9;
	private static final int YEL = 10;
	private static final int RED = 11;
	private static final int SAV = 12;
	private static final int BON = 13;
	//private static final int EA = 14;
	private static final int BPS = 15;
	private static final int TOT = 16;
	private static final int TEAM_GOALS_ON_PITCH = 17;
	
	// future ones have teams/date, so can store all IDs for whole season.
	// do this automated so that next year is less of a pain...
	//http://fantasy.premierleague.com/fixture/21/
	//http://fantasy.premierleague.com/fixture/300/
	private static final String URL_MATCH_STATS = "http://fantasy.premierleague.com/fixture/";
		
	private int season = App.season;
	private boolean changed;
	
	private static final String t_player_match = "player_match";
	private final String t_where = "season = " + season + " AND player_player_id = ? AND fixture_id = ?";
	private String[] t_wArgs = new String[2];
	private static final String f_season = "season";
	private static final String f_player_player_id = "player_player_id";
	private static final String f_fixture_id = "fixture_id";
	private static final String f_gameweek = "gameweek";
	private static final String f_is_home = "is_home";
	private static final String f_opp_team_id = "opp_team_id";
	private static final String f_pl_team_id = "pl_team_id";
	
	private static String[] fields = { "", "", "minutes", "goals", "assists", "", "conceded", "own_goals", "pen_sav"
		, "pen_miss", "yellow", "red", "saves", "bonus", "", "bps", "total", "team_goals_on_pitch" };
	
	// be safe on locale
	public static SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy", Locale.US);
	
	SQLiteDatabase db = App.dbGen;
	private ContentValues updateVals = null;
	
	private ProcRes result;
	public static final int GOT_BONUS = 2;
	private int gameweek;
	
	private boolean toProcessFix(long ukTime, GWFixture f) {
		if (ukTime >= f.kickoff_datetime) {
			if (!f.complete || !f.got_bonus) {
				return true;
			}
		}
		return false;
	}
	
	public void getPoints(ProcRes res, int p_gameweek) {
		gameweek = p_gameweek;
		result = res;
		
		String proc_name = "fpl gameweek stats " + gameweek;
		App.log("starting " + proc_name + " scrape");
		//Debug.startMethodTracing("fpl_points", 5000000);
		long startTime = System.currentTimeMillis();
			
		int countFixToProcess = 0;
		long ukTime = App.currentUkTime();
		for (GWFixture f : App.gwFixtures.values()) {
    		if (toProcessFix(ukTime, f)) {
    			countFixToProcess++;
    		}
    	}
		result.setMaxName(countFixToProcess + 2, "get player scores");	
		
		db.execSQL(DbGen.TRANSACTION_IMMEDIATE_BEGIN);
		changed = false;
		
		int fixProcessed = 0;
		for (GWFixture f : App.gwFixtures.values()) {
    		if (!result.updating) {
    			if (toProcessFix(ukTime, f)) {
					processFixture(f);
					fixProcessed++;
					result.setProgress(fixProcessed);
    			}
    		}
    	}
		
		// init fixture IDs at start of season gw0
		// TO DO
		// STOP SHIP
		/*GWFixture gf = new GWFixture();
		for (int i=1; i<=380; i++) {
			gf.fpl_id = i;
			processFixture(gf);
		}/*
		
		/*
		// single fixture test
		GWFixture gf = new GWFixture();
		gf.fpl_id = 4;
		Cursor c = db.rawQuery("SELECT _id FROM fixture WHERE season = 13 AND fpl_id = " + gf.fpl_id, null);
		c.moveToFirst();
		gf.id = c.getInt(0);
		c.close();
		processFixture(gf);
		*/
		
		if (changed) {
			SquadUtil squadUtil = new SquadUtil();
			squadUtil.updateSquadGameweekScores(gameweek, false);
		}
		result.setProgress(fixProcessed + 1);
		
		try {
			db.execSQL(DbGen.TRANSACTION_END);
		} catch (SQLException e) {
			App.exception(e);
		} catch (IllegalStateException e) {
			App.exception(e);
		}
		
		Long timeDiff = System.currentTimeMillis() - startTime;
		float timeSecs = (float)timeDiff / 1000;
		App.log("done " + res.procname + " in " + timeSecs + " seconds");
		
		//Debug.stopMethodTracing();
		result.setDataChanged(changed);
		result.setComplete();
	}
	
	private static final Pattern p_header = Pattern.compile(
			"<h2 id=\"ismFixtureDetailTitle\">"
		  + "\\W+([A-Za-z\\s]+?)"
		  + "\\W+([0-9]+|None)?\\s?-\\s?([0-9]+|None)?\\W+"
		  + "([A-Za-z\\s]+?):", Pattern.MULTILINE);
	
	private static final Pattern p_team = Pattern.compile(
			"<table class=\"ismJsStatsGrouped ismTable ismDataView ismFixtureDetailTable\">"
          + "(.+?)</table>", Pattern.MULTILINE | Pattern.DOTALL);
	
	private static final Pattern p_player = Pattern.compile(
			  "<tr>"
			+ "\\W+<td>\\W+([\\w\\s'-\\.]+?)\\W+</td>"
			+ "\\W+<td>\\W+?([0-9]+)\\W+</td>"
			+ "\\W+<td>\\W+?([0-9]+)\\W+</td>"
			+ "\\W+<td>\\W+?([0-9]+)\\W+</td>"
			+ "\\W+<td>\\W+?([0-9]+)\\W+</td>"
			+ "\\W+<td>\\W+?([0-9]+)\\W+</td>"
			+ "\\W+<td>\\W+?([0-9]+)\\W+</td>"
			+ "\\W+<td>\\W+?([0-9]+)\\W+</td>"
			+ "\\W+<td>\\W+?([0-9]+)\\W+</td>"
			+ "\\W+<td>\\W+?([0-9]+)\\W+</td>"
			+ "\\W+<td>\\W+?([0-9]+)\\W+</td>"
			+ "\\W+<td>\\W+?([0-9]+)\\W+</td>"
			+ "\\W+<td>\\W+?([0-9]+)\\W+</td>"
			+ "\\W+<td>\\W+?([0-9]+)\\W+</td>"
			+ "\\W+<td>\\W+?(-?[0-9]+)\\W+</td>"
		    + "\\W+<td>\\W+?(-?[0-9]+)\\W+</td>"
		    + "\\W+</tr>", Pattern.MULTILINE);
	
	private static final String NONE = "None";
	
	public void processFixture(GWFixture f) {
		Scanner scan = null;
		UrlObj con = null;
		int fix_bonus_pts = 0;
		try {
			App.log("starting match stats scrape f._id: " + f.id + " f.fpl_id: " + f.fpl_id);
			long startTime = System.currentTimeMillis();
			
			con = URLUtil.getUrlStream(URL_MATCH_STATS + f.fpl_id + "/", true, URLUtil.NO_AUTH);
			if (con.ok) {
				scan = new Scanner(con.stream);
				
				// header
				int home_score = 0;
				int away_score = 0;
				String home_name = App.id2team.get(f.team_home_id);
				String away_name = App.id2team.get(f.team_away_id);
				if (scan.findWithinHorizon(p_header, 0) != null) {
					MatchResult m = scan.match();
					String home = m.group(1);
					String away = m.group(4);
					String home_score_string = m.group(2);
					String away_score_string = m.group(3);
					if (home_score_string != null) {
						if (home_score_string.equals(NONE)) {
							App.log("home score = None; fixture appears to be postponed");
						} else {
							home_score = Integer.parseInt(home_score_string);
						}
					}
					if (away_score_string != null) {
						if (home_score_string.equals(NONE)) {
							App.log("away score = None; fixture appears to be postponed");
						} else {
							away_score = Integer.parseInt(away_score_string);
						}
					}
					App.log("Home: '" + home + "' " + home_score
							+ " Away: '" + away + "' " + away_score);
					int home_id = App.team_2id.get(home);
					int away_id = App.team_2id.get(away);
					App.log("Home id: " + home_id + " Away id: " + away_id);
					if ( (home_id != f.team_home_id) || (away_id != f.team_away_id) ) {
						f.team_home_id = home_id;
						f.team_away_id = away_id;
						App.log("setting fixture fpl id");
						db.execSQL("UPDATE fixture SET fpl_id = " + f.fpl_id
								+ " WHERE season = " + season
								+ " AND team_home_id = " + home_id
								+ " AND team_away_id = " + away_id);
						changed = true;
					}
				}
				
				loadPlayers(f);
				boolean fix_90_mins = false;
				
				// init live bonus
				HashMap<Integer, LinkedList<PlayerData>> bonus_map = new HashMap<Integer, LinkedList<PlayerData>>();
				LinkedList<PlayerData> players_to_update = new LinkedList<PlayerData>();
				
				// teams
				int team_count = 0;
				while (scan.findWithinHorizon(p_team, 0) != null) {
					// setup data
					team_count++;
					boolean home = (team_count == 1);
					App.log("home = " + home);
					int team_id, opp_id;
					int team_goals;
					String opp;
					HashMap<String, PlayerData> playerCache;
					if (home) {
						team_id = f.team_home_id;
						opp_id = f.team_away_id;
						opp = away_name;
						team_goals = home_score;
						playerCache = players_home;
					} else {
						team_id = f.team_away_id;
						opp_id = f.team_home_id;
						opp = home_name;
						team_goals = away_score;
						playerCache = players_away;
					}
					
					// players
					Matcher players = p_player.matcher(scan.match().group(1).replace(DoTransfersNew.APOST, DoTransfersNew.APOST_REP));
					int player_count = 0;
					while (players.find()) {
						player_count++;
						String name = players.group(1);
						
						// pre-calc some stats
						int points = Integer.parseInt(players.group(TOT));
						int mins = Integer.parseInt(players.group(MINS));
						if (mins >= 90) {
							fix_90_mins = true;
						}
						int p_goals = Integer.parseInt(players.group(GOALS));
						int team_goals_on_pitch = ScrapeMatchScoresCatchup.calc_team_goals_on_pitch(team_goals, mins, p_goals);
						int bonus = Integer.parseInt(players.group(BON));
						fix_bonus_pts += bonus;
						int bps = Integer.parseInt(players.group(BPS));
						
						// find player
						PlayerData p = playerCache.get(name);
						if (p == null) {
							App.log("player_match record not found for " + name + " (team_id: " + team_id
									+ ") vs " + opp + ". inserting...");
							Cursor findP = db.rawQuery("SELECT p._id"
								+ " FROM player p"
								+ " WHERE p.team_id = " + team_id + " AND p.name = \"" + name + "\" AND NOT EXISTS"
								+ "  (SELECT pm.total FROM player_match pm"
								+ "    WHERE pm.season = " + season + " AND pm.fixture_id = " + f.id
								+ "    AND pm.player_player_id = p._id)", null);
							findP.moveToFirst();
							if (findP.isAfterLast()) {
								App.log("can't find correct player to insert..!");
							} else {
								ContentValues c = new ContentValues();
								// keys
								c.put(f_season, season);
								int pid = findP.getInt(0);
								App.log("insert p._id: " + pid);
								c.put(f_player_player_id, pid);
								c.put(f_fixture_id, f.id);
								c.put(f_gameweek, gameweek);
								if (home) {
									c.put(f_is_home, 1);
								} /* else {
									c.remote(f_is_home);
								} */
								c.put(f_opp_team_id, opp_id);
								c.put(f_pl_team_id, team_id);
								// match stats
								c.put(fields[MINS], mins);
								if (p_goals != 0) c.put(fields[GOALS], p_goals);
								int ass = Integer.parseInt(players.group(ASS));
								if (ass != 0) c.put(fields[ASS], ass);
								int conc = Integer.parseInt(players.group(CONC));
								if (conc != 0) c.put(fields[CONC], conc);
								int pensav = Integer.parseInt(players.group(PENSAV));
								if (pensav != 0) c.put(fields[PENSAV], pensav);
								int penmis = Integer.parseInt(players.group(PENMIS));
								if (penmis != 0) c.put(fields[PENMIS], penmis);
								int yel = Integer.parseInt(players.group(YEL));
								if (yel != 0) c.put(fields[YEL], yel);
								int red = Integer.parseInt(players.group(RED));
								if (red != 0) c.put(fields[RED], red);
								int sav = Integer.parseInt(players.group(SAV));
								if (sav != 0) c.put(fields[SAV], sav);
								if (bonus != 0) c.put(fields[BON], bonus);
								int og = Integer.parseInt(players.group(OG));
								if (og != 0) c.put(fields[OG], og);
								c.put(fields[BPS], bps);
								c.put(fields[TOT], points);
								c.put(fields[TEAM_GOALS_ON_PITCH], team_goals_on_pitch);
								db.insert(t_player_match, null, c);
								changed = true;
							}
							findP.close();
						} else {
							// if multiple results (players with same name for same club)
							// then can but try to get the right one..
							if (p.other != null) {
								App.log(">> Not last player with this name (" + p.name + " / " + p.fpl_id + ") points = " + points);
								// if player count = 1 (first player on fpl team sheet) and not a GK, then skip
								if ( (player_count == 1) && (p.position != SquadUtil.POS_KEEP) ) {
									App.log(">>> First player on fpl team sheet, but not a keeper; skipping..");
									p = p.other;
								// if player count > 2 and a GK, then skip
								} else if ( (player_count > 1) && (p.position == SquadUtil.POS_KEEP) ) {
									App.log(">>> Not first player on fpl team sheet, but a keeper; skipping..");
									p = p.other;
								// the joe/carlton cole case: both outfielders, exact same name
								// - use player api to get current match score and see if it matches
								// - BUT will this use (say) carlton cole for both?
								} else {
									if (p.livestatsret == null) {
										p.livestatsret = ScrapePlayerLiveStats.getPlayerLiveStats(p.fpl_id, result, gameweek, opp_id);
									}
									if (p.livestatsret.found) {
										App.log(">>> Got this player_match from API.. points = " + p.livestatsret.ptot);
										if (!p.used && (p.livestatsret.ptot == points) ) {
											App.log(">>> points match; sticking");
										} else {
											App.log(">>> points do not match; checking p.other... (" + p.other.fpl_id + ")");
											if (p.other.livestatsret == null) {
												p.other.livestatsret = ScrapePlayerLiveStats.getPlayerLiveStats(p.other.fpl_id, result, gameweek, opp_id);
											}
											App.log(">>> Got OTHER player_match from API.. points = " + p.other.livestatsret.ptot);
											if (!p.other.used && (p.other.livestatsret.ptot == points) ) {
												p = p.other;
												App.log(">>> OTHER points match; switching to " + p.fpl_id);
											} else {
												p = null;
												App.log(">>> neither points match; skipping entirely");
											}
										} //else
									} // found
								} // else: check points
							} // p.other != null
							
							if (p != null) {
								players_to_update.add(p);
								PlayerData pnew = new PlayerData();
								p.pnew = pnew;
								
								p.used = true;
								p.opp = opp;
								
								//App.log("Player " + player_count + ": '" + name + "'" + " id: " + p.id + " fpl id: " + p.fpl_id + " points: " + points);
								pnew.minutes = mins;
								pnew.goals = p_goals;
								pnew.assists = Integer.parseInt(players.group(ASS));
								pnew.conceded = Integer.parseInt(players.group(CONC));
								pnew.pen_sav = Integer.parseInt(players.group(PENSAV));
								pnew.pen_miss = Integer.parseInt(players.group(PENMIS));
								pnew.yellow = Integer.parseInt(players.group(YEL));
								pnew.red = Integer.parseInt(players.group(RED));
								pnew.saves = Integer.parseInt(players.group(SAV));
								pnew.bonus = bonus;
								pnew.own_goals = Integer.parseInt(players.group(OG));
								pnew.bps = bps;
								pnew.total = points;
								pnew.team_goals_on_pitch = team_goals_on_pitch;
								
								// process bps for live bonus
								LinkedList<PlayerData> bonus_players = bonus_map.get(bps);
								// no players with this number of bps yet
								if (bonus_players == null) {
									bonus_players = new LinkedList<PlayerData>();
									bonus_map.put(bps, bonus_players);
								}
								bonus_players.add(p);
							} // p != null
						} // found player
					} // players
				} // teams
				
				boolean fix_got_bonus = (fix_bonus_pts > 0);
				
				// live bonus..
				// only process if actual bonus not set
				if (!fix_got_bonus && Settings.getBoolPref(Settings.PREF_LIVE_BONUS)) {
					int current_bonus = 3;
					while (current_bonus > 0) {
						// find highest remaining bps value
						int highest = 0;
						for (int bps : bonus_map.keySet()) {
							if (bps > highest) {
								highest = bps;
							}
						}
						
						// nothing worth giving for 1 bps early in game
						if (highest <= 1) {
							break;
						}
						
						// assign bonus to players (save value for all players because of decrement)
						int bonus_to_use_for_players = current_bonus;
						for (PlayerData p : bonus_map.get(highest)) {
							// always update db here; as total will always need changing (after main code updated it without bonus)
							p.pnew.total += bonus_to_use_for_players;
							p.pnew.bonus = bonus_to_use_for_players;
							App.log("live bonus: " + p.name + " (" + highest + " bps) = " + bonus_to_use_for_players + " (total pts = " + p.pnew.total + ")");
							
							// decrement remaining bonus for each player assigned
							current_bonus--;
						}
						
						// remove this key for next iteration
						bonus_map.remove(highest);
					}
				} // live bonus processing
				
				// do actual updates
				for (PlayerData p : players_to_update) {
					checkDiff(p.minutes, p.pnew.minutes, MINS, p.name, p.fpl_id, p.opp);
					checkDiff(p.goals, p.pnew.goals, GOALS, p.name, p.fpl_id, p.opp);
					checkDiff(p.assists, p.pnew.assists, ASS, p.name, p.fpl_id, p.opp);
					checkDiff(p.conceded, p.pnew.conceded, CONC, p.name, p.fpl_id, p.opp);
					checkDiff(p.pen_sav, p.pnew.pen_sav, PENSAV, p.name, p.fpl_id, p.opp);
					checkDiff(p.pen_miss, p.pnew.pen_miss, PENMIS, p.name, p.fpl_id, p.opp);
					checkDiff(p.yellow, p.pnew.yellow, YEL, p.name, p.fpl_id, p.opp);
					checkDiff(p.red, p.pnew.red, RED, p.name, p.fpl_id, p.opp);
					checkDiff(p.saves, p.pnew.saves, SAV, p.name, p.fpl_id, p.opp);
					checkDiff(p.bonus, p.pnew.bonus, BON, p.name, p.fpl_id, p.opp);
					checkDiff(p.own_goals, p.pnew.own_goals, OG, p.name, p.fpl_id, p.opp);
					checkDiff(p.bps, p.pnew.bps, BPS, p.name, p.fpl_id, p.opp);
					checkDiff(p.total, p.pnew.total, TOT, p.name, p.fpl_id, p.opp);
					checkDiff(p.team_goals_on_pitch, p.pnew.team_goals_on_pitch, TEAM_GOALS_ON_PITCH, p.name, p.fpl_id, p.opp);
					
					if (updateVals != null) {
						//AppClass.log("updating " + p.playername + " v " + AppClass.id2team.get(p.opp_team_id) + " gw " + p.gameweek);
						t_wArgs[0] = String.valueOf(p.id);
						t_wArgs[1] = String.valueOf(f.id);
						App.dbGen.update(t_player_match, updateVals, t_where, t_wArgs);
						updateVals = null;
						changed = true;
					}
				}
				
				updateFixture(f, home_score, away_score, home_name, away_name, fix_bonus_pts, fix_90_mins);
				
				Long timeDiff = System.currentTimeMillis() - startTime;
				float timeSecs = (float)timeDiff / 1000;
				App.log("done match stats " + f.fpl_id + " scrape in " + timeSecs + " seconds");
			} else {
				App.log("match stats: url failed");
				result.setError("stats url failed (" + f.fpl_id + ")");
				if (con.updating) {
					result.updating = true;
				}
			}
		} catch (IOException e) {
			App.exception(e);
			result.setError(e.toString());
		} finally {
			if (scan != null) scan.close();
			try {
				if (con!=null && con.stream != null) con.stream.close();
			} catch (Exception e) {
				App.exception(e);
			}
			if (con != null) con.closeCon();
		}
	}
	
	private static class PlayerData {
		public int id, fpl_id, position, team_id;
		public String name, opp;
		public PlayerData other;
		public boolean used;
		public LiveStatsRet livestatsret;
		public int minutes, goals, assists, conceded, pen_sav, pen_miss, yellow;
		public int red, saves, bonus, own_goals, total, team_goals_on_pitch, bps;
		public PlayerData pnew;
	}
	private HashMap<String, PlayerData> players_home;
	private HashMap<String, PlayerData> players_away;
	
	private void loadPlayers(GWFixture f) {
		players_home = new HashMap<String, PlayerData>();
		players_away = new HashMap<String, PlayerData>();
		
		App.log("loadPlayers..");
		Cursor curP = db.rawQuery("SELECT p._id, ps.fpl_id, ps.position, p.name, p.team_id"
			    //5
			+ " , pm.minutes, pm.goals, pm.assists, pm.conceded, pm.pen_sav, pm.pen_miss, pm.yellow"
			    //12
			+ " , pm.red, pm.saves, pm.bonus, pm.own_goals, pm.total, pm.team_goals_on_pitch, pm.bps"
			+ " FROM player p, player_season ps, player_match pm"
			+ " WHERE pm.season = " + season + " AND pm.fixture_id = " + f.id + " AND pm.player_player_id = p._id"
			+ " AND p._id = pm.player_player_id AND ps.player_id = pm.player_player_id AND ps.season = " + season, null);
		curP.moveToFirst();
		while (!curP.isAfterLast()) {
			PlayerData p = new PlayerData();
			p.id = curP.getInt(0);
			p.fpl_id = curP.getInt(1);
			p.position = curP.getInt(2);
			p.name = curP.getString(3);
			p.team_id = curP.getInt(4);
			p.minutes = curP.getInt(5);
			p.goals = curP.getInt(6);
			p.assists = curP.getInt(7);
			p.conceded = curP.getInt(8);
			p.pen_sav = curP.getInt(9);
			p.pen_miss = curP.getInt(10);
			p.yellow = curP.getInt(11);
			p.red = curP.getInt(12);
			p.saves = curP.getInt(13);
			p.bonus = curP.getInt(14);
			p.own_goals = curP.getInt(15);
			p.total = curP.getInt(16);
			p.team_goals_on_pitch = curP.getInt(17);
			p.bps = curP.getInt(18);
			
			HashMap<String, PlayerData> players;
			if (p.team_id == f.team_home_id) {
				players = players_home;
			} else {
				players = players_away;
			}
			
			// check for duplicate names
			PlayerData o = players.get(p.name);
			if (o == null) {
				players.put(p.name, p);
			} else {
				// duplicate name. add to chain
				while (o.other != null) {
					o = o.other;
				}
				o.other = p;
			}
			
			curP.moveToNext();
		}
		curP.close();
	}
	
	protected static final int FINISHED_THRESH_MINS = 90 + 45 + 45;
	private static final int GOT_90_MINUTES_FINISHED = 10;
	
	private void updateFixture(GWFixture f, int home_score, int away_score, String home_team
			, String away_team, int fix_bonus_pts, boolean fix_90_mins) {
		// if already marked as fully complete, skip
		if (f.got_bonus) {
			return;
		}
		
		boolean got_bonus_now = (fix_bonus_pts > 0);
		
		if (f.complete && got_bonus_now) {
			// have bonus
			App.log("Marking " + home_team + " v " + away_team + " got bonus pts");
			App.dbGen.execSQL("UPDATE fixture SET got_bonus = " + GOT_BONUS + " WHERE _id = " + f.id);
			f.got_bonus = true;
			changed = true;
		}
		
		if (f.complete) {
			// no further processing required
			return;
		}
		
		boolean fixture_finished = false;
		
		if (got_bonus_now) {
			App.log("marking fixture finished by got bonus points " + f.id + " (" + home_team + " v " + away_team + ")");
			fixture_finished = true;
		}
		
		// check if match finished based on time elapsed since kickoff
		long now = App.currentUkTime();
		
		// if fpl marked minutes_played at 90, then after a certain number of minutes, marked fixture as complete
		if (!fixture_finished && fix_90_mins) {
			if (f.marked_90_datetime == 0) {
				App.log("marking got 90 for fixture " + f.id + " (" + home_team + " v " + away_team + ")");
				f.marked_90_datetime = now;
			} else {
				long diff = now - f.marked_90_datetime;
				if (diff >= (GOT_90_MINUTES_FINISHED * 60) ) {
					App.log("marking fixture finished by got 90 " + f.id + " (" + home_team + " v " + away_team + ")");
					fixture_finished = true;
				}
			}
		}
		
		if (!fixture_finished) {
			long timediff = now - f.kickoff_datetime;
			if (timediff > (FINISHED_THRESH_MINS * 60) ) {
				App.log("marking fixture " + f.id + " (" + home_team + " v " + away_team + ") as finished based on time since kickoff");
				fixture_finished = true;
			}
		}
		
		if (fixture_finished) {
			String points_home = null;
			String points_away = null;
			if (home_score > away_score) {
				points_home = "3";
				points_away = "0";
			} else if (away_score > home_score) {
				points_home = "0";
				points_away = "3";
			} else {
				points_home = "1";
				points_away = "1";
			}
			
			int got_bonus_set = 1;
			if (got_bonus_now) {
				got_bonus_set = GOT_BONUS;
				f.got_bonus = true;
				App.log("marking as finished (with bonus)");
			} else {
				App.log("marking as finished (no bonus)");
			}
			
			// set result and points for fixture
			App.dbGen.execSQL("UPDATE fixture SET got_bonus = " + got_bonus_set
					+ ", res_points_home = " + points_home
					+ ", res_points_away = " + points_away
					+ ", res_goals_home = " + home_score
					+ ", res_goals_away = " + away_score
					+ " WHERE _id = " + f.id
					+ " AND got_bonus < 1");
			
			// update player_match.result_points
			App.dbGen.execSQL("UPDATE player_match SET result_points = " + points_home
					+ " WHERE fixture_id = " + f.id + " AND is_home = 1");
			App.dbGen.execSQL("UPDATE player_match SET result_points = " + points_away
					+ " WHERE fixture_id = " + f.id + " AND is_home IS NULL");
			
			f.complete = true;
			f.goals_home = home_score;
			f.goals_away = away_score;
			f.marked_finished_datetime = App.currentUkTime();
			
			// notify game finished
			VidiEntry e = new VidiEntry();
			e.category = Vidiprinter.CAT_FINAL_WHISTLE;
			e.message = "Final Whistle " + home_team + " " + home_score + " - "
			 		+ " " + away_team + " " + away_score;
			e.gameweek = gameweek;
			// set fixture id to player id, but negative
			e.player_fpl_id = 0 - f.id;
			Vidiprinter.addVidi(e, result.context);
			
			changed = true;
		} else {
			if ( (f.goals_home != home_score) || (f.goals_away != away_score)
				|| ((f.goals_home == 0) && (f.goals_away == 0)) ) {
				App.dbGen.execSQL("UPDATE fixture SET res_goals_home = " + home_score
						+ ", res_goals_away = " + away_score
						+ " WHERE _id = " + f.id);
				
				f.goals_home = home_score;
				f.goals_away = away_score;
				
				changed = true;
			}
		}
	}
	
	private void checkDiff(int old, int newVal, int field, String name, int fpl_id, String oppname) {
		if (old != newVal) {
			// update dbGen
			if (updateVals == null) updateVals = new ContentValues();
			updateVals.put(fields[field], newVal);
			if (App.checkdiff || (newVal < old) ) {
				App.log(name + " v " + oppname + ": " + fields[field] + ": " + old + " -> " + newVal);
			}
			
			// vidiprinter
			if (field==GOALS || field==ASS || field==PENSAV || field==PENMIS || field==RED || field==OG) { //|| field==BON
				VidiEntry e = new VidiEntry();
				e.player_fpl_id = fpl_id;
				e.gameweek = gameweek;
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
							e.message = name + " goal v " + oppname; break;
						case ASS:
							e.message = name + " assist v " + oppname; break;
						case PENSAV:
							e.message = name + " saved a penalty v " + oppname; break;
						case PENMIS:
							e.message = name + " missed a penalty v " + oppname; break;
						case RED:
							e.message = name + " was sent off v " + oppname; break;
						case OG:
							e.message = name + " scored an own goal v " + oppname; break;
						/*case BON:
							e.message = name + " was awarded " + diff + " bonus point(s) v " + oppname; break;*/
					}
				} else {
					// correction
					switch (field) {
						case GOALS:
							e.message = "CORRECTION: " + name + " goal removed v " + oppname; break;
						case ASS:
							e.message = "CORRECTION: " + name + " assist removed v " + oppname; break;
						case PENSAV:
							e.message = "CORRECTION: " + name + " penalty save removed v " + oppname; break;
						case PENMIS:
							e.message = "CORRECTION: " + name + " penalty miss removed v " + oppname; break;
						case RED:
							e.message = "CORRECTION: " + name + " red card removed v " + oppname; break;
						case OG:
							e.message = "CORRECTION: " + name + " own goal removed v " + oppname; break;
						/*case BON:
							e.message = "CORRECTION: " + name + " bonus points removed v " + oppname; break;*/
					}
				}
				
				Vidiprinter.addVidi(e, result.context);
			}
		}
	}
	
}
