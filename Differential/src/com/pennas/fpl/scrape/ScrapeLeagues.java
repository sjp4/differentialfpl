package com.pennas.fpl.scrape;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.pennas.fpl.App;
import com.pennas.fpl.process.FPLService.ProcRes;
import com.pennas.fpl.util.DbGen;
import com.pennas.fpl.util.SquadUtil;
import com.pennas.fpl.util.URLUtil;
import com.pennas.fpl.util.URLUtil.UrlObj;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class ScrapeLeagues {
	
	private static final String URL_LEAGUES = "http://fantasy.premierleague.com/my-leagues/";
	
	private Pattern p_league = Pattern.compile("<a href=\"/my-leagues/([0-9]+)/standings/\" title=\"View information about .+?\">(.+?)</a>");
	
	private SQLiteDatabase dbGen = App.dbGen;
	private HashMap<Integer, String> leagues;
	
	private static final String f_id = "_id";
	private static final String f_name = "name";
	private static final String t_minileague = "minileague";
	
	private static final String LEAGUES_PRIVATE_CLASSIC = "<table class=\"ismTable ismPrivClassicLeague\" cellspacing=\"0\">";
	private static final String LEAGUES_PUBLIC_CLASSIC = "<table class=\"ismTable ismPubClassicLeague\" cellspacing=\"0\">";
	private static final String LEAGUES_GLOBAL_CLASSIC = "<table class=\"ismTable ismGlobalLeague\" cellspacing=\"0\">";

	private static final String LEAGUES_PRIVATE_H2H = "<table class=\"ismTable ismPrivH2HLeague\" cellspacing=\"0\">";
	private static final String LEAGUES_PUBLIC_H2H = "<table class=\"ismTable ismPubH2HLeague\" cellspacing=\"0\">";
	
	private static final String LEAGUES_END = "<div id=\"ismCreateLeagueDialog\" class=\"ismSlide ismWizardWrapper\">";
	
	private static final int TYPE_NONE = 0;
	private static final int TYPE_PRIVATE_CLASSIC = 1;
	private static final int TYPE_PUBLIC_CLASSIC = 2;
	private static final int TYPE_GLOBAL_CLASSIC = 3;
	private static final int TYPE_PRIVATE_H2H = 4;
	private static final int TYPE_PUBLIC_H2H = 6;
	
	private HashSet<Integer> leagues_found = new HashSet<Integer>();
	
	public void updateLeagues(ProcRes p_result, boolean justRivals) {
		result = p_result;
		App.log("updateLeagues justRivals=" + justRivals);
		if (!justRivals) {
			UrlObj con = null;
			BufferedReader reader = null;
			InputStreamReader iread = null;
			String line="";
			
			result.internalProgressFrom = 0;
			result.internalProgressTo = 10;
			result.setMaxName(10, "get leagues");
			
			//boolean processed_data = false;
			try {
				dbGen.execSQL(DbGen.TRANSACTION_IMMEDIATE_BEGIN);
				
				// load existing leagues into mem
				leagues = new HashMap<Integer, String>();
				Cursor cur = dbGen.rawQuery("SELECT _id, name FROM minileague", null);
				cur.moveToFirst();
				while (!cur.isAfterLast()) {
					int league_id = cur.getInt(0);
					String league_name = DbGen.decompress(cur.getBlob(1));
					leagues.put(league_id, league_name);
					cur.moveToNext();
				}
				cur.close();
				
				//Debug.startMethodTracing("fpl_scrape_p2", 15000000);
				App.log("starting leagues scrape");
				long startTime = System.currentTimeMillis();
				
				con = URLUtil.getUrlStream(URL_LEAGUES, false, URLUtil.AUTH_FULL);
				
				if (con.ok) {
					iread = new InputStreamReader(con.stream, "UTF-8");
				    reader = new BufferedReader(iread, 1024);
					
					result.setProgress(3);
					
					boolean done_any = false;
					
					int league_type = TYPE_NONE;
					
					for (; (line = reader.readLine()) != null;) {
						line = line.trim();
						
						if (line.equals(LEAGUES_PRIVATE_CLASSIC)) {
							App.log("TYPE_PRIVATE_CLASSIC");
							league_type = TYPE_PRIVATE_CLASSIC;
							done_any = true;
						} else if (line.equals(LEAGUES_PUBLIC_CLASSIC)) {
							App.log("TYPE_PUBLIC_CLASSIC");
							league_type = TYPE_PUBLIC_CLASSIC;
							done_any = true;
						} else if (line.equals(LEAGUES_GLOBAL_CLASSIC)) {
							App.log("TYPE_GLOBAL_CLASSIC");
							league_type = TYPE_GLOBAL_CLASSIC;
							done_any = true;
						} else if (line.equals(LEAGUES_PRIVATE_H2H)) {
							App.log("TYPE_PRIVATE_H2H");
							league_type = TYPE_PRIVATE_H2H;
						} else if (line.equals(LEAGUES_PUBLIC_H2H)) {
							App.log("TYPE_PUBLIC_H2H");
							league_type = TYPE_PUBLIC_H2H;
						} else if (line.equals(LEAGUES_END)) {
							break;
						} else if ( (league_type == TYPE_PRIVATE_CLASSIC) || (league_type == TYPE_PUBLIC_CLASSIC) ) {
							Matcher m_league = p_league.matcher(line);
			        		if (m_league.find()) processLeague(m_league, true);
						}
					}
					
					result.setProgress(9);
					
					// remove leagues not in scrape
					if (done_any) {
						for (Integer id : leagues.keySet()) {
							if (!leagues_found.contains(id)) {
								dbGen.execSQL("DELETE FROM minileague WHERE _id = " + id);
								dbGen.execSQL("DELETE FROM minileague_team WHERE minileague_id = " + id);
								App.log("Removing league: " + id);
							}
						}
					}
					
					Long timeDiff = System.currentTimeMillis() - startTime;
					float timeSecs = (float)timeDiff / 1000;
					App.log("done leagues scrape in " + timeSecs + " seconds");
				} else {
					App.log("leagues: url failed");
					result.setError("leagues: url failed");
					if (con.updating) {
						result.updating = true;
					}
				}
			} catch (IOException e) {
				App.exception(e);
				result.setError("1 " + e);
			} finally {
				if (reader != null) {
			    	try { 
			    		reader.close();
			    		iread.close();
			    	} catch (IOException e) {
			    		App.exception(e);
			    	}
			    }
				if (con != null) con.closeCon();
				try {
					dbGen.execSQL(DbGen.TRANSACTION_END);
				} catch (SQLException e) {
					App.exception(e);
				} catch (IllegalStateException e) {
					App.exception(e);
				}
				if (result.failed) return;
			}
			
			result.internalProgressFrom = 10;
			result.internalProgressTo = 50;
			
			if (!result.updating) {
				updateLeagueTeams(result);
				
				dbGen.execSQL(DbGen.TRANSACTION_IMMEDIATE_BEGIN);
				
				// remove unreferenced squads
				Cursor curDel = dbGen.rawQuery("SELECT s._id FROM squad s WHERE s.rival != 1"
						+ " AND NOT EXISTS (SELECT mt.minileague_id FROM minileague_team mt WHERE mt.squad_id = s._id)"
						+ " AND s._id != " + App.my_squad_id
						+ " AND NOT EXISTS (SELECT sgw.cup_opp FROM squad_gameweek sgw"
						                    + " WHERE sgw.squad_id = " + App.my_squad_id + " AND sgw.cup_opp = s._id)", null);
				curDel.moveToFirst();
				while (!curDel.isAfterLast()) {
					int squad_id = curDel.getInt(0);
					if (squad_id != App.my_squad_id) {
						App.log("Removing squad " + squad_id + " (not in league/rival)");
						dbGen.execSQL("DELETE FROM squad_gameweek_player WHERE squad_id = " + squad_id);
						dbGen.execSQL("DELETE FROM squad_gameweek WHERE squad_id = " + squad_id);
						dbGen.execSQL("DELETE FROM squad WHERE _id = " + squad_id);
						dbGen.execSQL("DELETE FROM transfer WHERE squad_id = " + squad_id);
					}
					curDel.moveToNext();
				}
				curDel.close();
				
				try {
					dbGen.execSQL(DbGen.TRANSACTION_END);
				} catch (SQLException e) {
					App.exception(e);
				} catch (IllegalStateException e) {
					App.exception(e);
				}
			}
			
			result.internalProgressFrom = 50;
			result.internalProgressTo = 100;
		// just rivals
		} else {
			result.internalProgressFrom = 0;
			result.internalProgressTo = 100;
		}
		
		if (!result.updating){ 
			if (App.cur_gameweek > 0) {
				getRivalLineUps(App.cur_gameweek);
			}
		}
		
		result.setDataChanged(true);
		result.setComplete();
	}
	
	private static final String where_upd_ml = "_id = ?";
	
	private void processLeague(Matcher m, boolean classic) {
		int league_id = Integer.parseInt(m.group(1));
		String league_name = m.group(2).replace(DoTransfersNew.APOST, DoTransfersNew.APOST_REP);
		
		String stored_name = leagues.get(league_id);
		if (stored_name == null) {
			// insert
			ContentValues ins = new ContentValues();
			ins.put(f_id, league_id);
			ins.put(f_name, DbGen.compress(league_name));
			dbGen.insert(t_minileague, null, ins);
			App.log("Adding league " + league_name + " (id: " + league_id + " classic: " + classic + ")");
		} else{
			// compare
			if (!league_name.equals(stored_name)) {
				ContentValues upd = new ContentValues();
				upd.put(f_name, DbGen.compress(league_name));
				String[] args = new String[1];
				args[0] = String.valueOf(league_id);
				dbGen.update(t_minileague, upd, where_upd_ml, args);
				App.log("Updating league name to " + league_name + " (id: " + league_id + " classic: " + classic + ")");
			}
		}
		
		leagues_found.add(league_id);
	}
	
	private ProcRes result;
	
	public void updateLeagueTeams(ProcRes p_result) {
		result = p_result;
		dbGen.execSQL(DbGen.TRANSACTION_IMMEDIATE_BEGIN);
			
		// load existing leagues into mem
		Cursor cur = dbGen.rawQuery("SELECT _id, name FROM minileague", null);
		cur.moveToFirst();
		
		result.setMaxName(cur.getCount(), "get league teams");
		
		while (!cur.isAfterLast() && !result.updating) {
			int league_id = cur.getInt(0);
			
			HashSet<Integer> league_squads = new HashSet<Integer>();
			Cursor curSquads = dbGen.rawQuery("SELECT squad_id FROM minileague_team WHERE minileague_id = " + league_id, null);
			curSquads.moveToFirst();
			while (!curSquads.isAfterLast()) {
				league_squads.add(curSquads.getInt(0));
				curSquads.moveToNext();
			}
			curSquads.close();
			
			getLeagueTeams(league_id, league_squads);
			
			result.setProgress(cur.getPosition());
			cur.moveToNext();
		}
		cur.close();
		
		try {
			dbGen.execSQL(DbGen.TRANSACTION_END);
		} catch (SQLException e) {
			App.exception(e);
		} catch (IllegalStateException e) {
			App.exception(e);
		}
	}
	
	/* <td><a href="/entry/421598/event-history/18/">KingsLandingRangers</a></td>
                    <td>Craig Mackay</td>
                    <td>0</td>
                    <td>1,016</td>
     */
	//League scoring will start
	private static final Pattern p_league_gw_start = Pattern.compile("League scoring (started|will start): Gameweek ([0-9]+)");
	private static final Pattern p_league_squads = Pattern.compile("<td><a href=\"/entry/([0-9]+)/event-history/[0-9]+/\">(.+?)</a></td>"
			+ "\\W+<td>(.+?)</td>"
			+ "\\W+<td>([0-9]*)</td>"
			+ "\\W+<td>([0-9,]+)</td>", Pattern.MULTILINE|Pattern.DOTALL);
	private static final Pattern p_league_squads_gw0 = Pattern.compile("<tr>"
			+ "\\W+<td>(.+?)</td>"
			+ "\\W+<td><a href=\"/entry/([0-9]+)/history/\">(.+?)</a></td>"
			+ "\\W+</tr>");
	private static final int SQUAD_ID = 1;
	private static final int SQUAD_NAME = 2;
	private static final int SQUAD_MANAGER = 3;
	private static final int SQUAD_GW_SCORE = 4;
	private static final int SQUAD_TOT_SCORE = 5;
	
	private static final int SQUAD_ID_GW0 = 2;
	private static final int SQUAD_NAME_GW0 = 1;
	private static final int SQUAD_MANAGER_GW0 = 3;
	
	private void getLeagueTeams(int league_id, HashSet<Integer> league_squads) {
		Scanner scan = null;
		UrlObj con = null;
		//boolean processed_data = false;
		try {
			//Debug.startMethodTracing("fpl_scrape_p2", 15000000);
			App.log("starting league teams scrape for league " + league_id);
			long startTime = System.currentTimeMillis();
			
			con = URLUtil.getUrlStream("http://fantasy.premierleague.com/my-leagues/" + league_id + "/standings/", false, URLUtil.NO_AUTH);
			
			if (con.ok) {
				scan = new Scanner(con.stream);
				
				int start_scoring = 1;
				
				// league starts scoring from week...
				if (scan.findWithinHorizon(p_league_gw_start, 0) != null) {
					MatchResult m = scan.match();
					start_scoring = Integer.parseInt(m.group(2));
					App.log("start scoring: " + start_scoring);
					dbGen.execSQL("UPDATE minileague SET start_week = " + start_scoring
							+ " WHERE _id = " + league_id + " AND (start_week IS NULL OR start_week != " + start_scoring + ")");
				}
				
				// league teams
				Pattern pat_teams = p_league_squads;
				if (App.cur_gameweek == 0) {
					pat_teams = p_league_squads_gw0;
				}
				while (scan.findWithinHorizon(pat_teams, 0) != null) {
					MatchResult m = scan.match();
					processLeagueTeam(league_id, m);
					// remove from list of known squads
					int grp_squad_id = SQUAD_ID;
					if (App.cur_gameweek == 0) {
						grp_squad_id = SQUAD_ID_GW0;
					}
					int squad_id = Integer.parseInt(m.group(grp_squad_id));
					league_squads.remove(squad_id);
				}
				
				// remove squads no longer in league
				for (int sq : league_squads) {
					App.log("removing " + sq + " from " + league_id);
					dbGen.execSQL("DELETE FROM minileague_team WHERE minileague_id = " + league_id
							+ " AND squad_id = " + sq);
				}
				
				Long timeDiff = System.currentTimeMillis() - startTime;
				float timeSecs = (float)timeDiff / 1000;
				App.log("done league teams scrape in " + timeSecs + " seconds");
			} else {
				App.log("leagueteams: url failed");
				result.setError("leagueteams: url failed");
				if (con.updating) {
					result.updating = true;
				}
			}
		} catch (IOException e) {
			App.exception(e);
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
	
	private static final String t_minileague_team = "minileague_team";
	private static final String f_squad_id = "squad_id";
	private static final String f_minileague_id = "minileague_id";
	private static final String f_current_gw_pts = "current_gw_pts";
	
	private void processLeagueTeam(int league_id, MatchResult m) {
		int grp_squad_name = SQUAD_NAME;
		int grp_squad_id = SQUAD_ID;
		int grp_squad_manager = SQUAD_MANAGER;
		if (App.cur_gameweek == 0) {
			grp_squad_name = SQUAD_NAME_GW0;
			grp_squad_id = SQUAD_ID_GW0;
			grp_squad_manager = SQUAD_MANAGER_GW0;
		}
		
		String team_name = m.group(grp_squad_name).replace(DoTransfersNew.APOST, DoTransfersNew.APOST_REP);
		int team_id = Integer.parseInt(m.group(grp_squad_id));
		String manager_name = m.group(grp_squad_manager).replace(DoTransfersNew.APOST, DoTransfersNew.APOST_REP);
		int score_tot = 0, score_gw = 0;
		if (App.cur_gameweek > 0) {
			score_tot = Integer.parseInt(m.group(SQUAD_TOT_SCORE).replace(",", ""));
			score_gw = Integer.parseInt(m.group(SQUAD_GW_SCORE));
		}
		
		Cursor curLS = App.dbGen.rawQuery("SELECT squad_id, points, current_gw_pts FROM minileague_team"
				+ " WHERE minileague_id = " + league_id + " AND squad_id = " + team_id, null);
		curLS.moveToFirst();
		if (curLS.isAfterLast()) {
			App.log("insert minileague_team " + team_id + ": " + team_name + " (" + manager_name + ")");
			ContentValues ins = new ContentValues();
			ins.put(f_squad_id, team_id);
			ins.put(f_minileague_id, league_id);
			ins.put(f_points, score_tot);
			ins.put(f_current_gw_pts, score_gw);
			dbGen.insert(t_minileague_team, null, ins);
		} else {
			int stored_pts = curLS.getInt(1);
			int stored_current_gw_pts = curLS.getInt(2);
			if ( (stored_pts != score_tot) || (stored_current_gw_pts != score_gw) ) {
				dbGen.execSQL("UPDATE minileague_team SET points = " + score_tot + ", current_gw_pts = " + score_gw
						+ " WHERE minileague_id = " + league_id + " AND squad_id = " + team_id);
			}
		}
		curLS.close();
		
		processSquad(m);
	}
	
	private static final String t_squad = "squad";
	private static final String f_player_name = "player_name";
	private static final String f_points = "points";
	private static final String where_squad = "_id = ?";
	private static final String t_squad_gameweek = "squad_gameweek";
	private static final String f_gameweek = "gameweek";
	
	private void processSquad(MatchResult m) {
		int grp_squad_name = SQUAD_NAME;
		int grp_squad_id = SQUAD_ID;
		int grp_squad_manager = SQUAD_MANAGER;
		if (App.cur_gameweek == 0) {
			grp_squad_name = SQUAD_NAME_GW0;
			grp_squad_id = SQUAD_ID_GW0;
			grp_squad_manager = SQUAD_MANAGER_GW0;
		}
		
		String team_name = m.group(grp_squad_name).replace(DoTransfersNew.APOST, DoTransfersNew.APOST_REP);
		int team_id = Integer.parseInt(m.group(grp_squad_id));
		String manager_name = m.group(grp_squad_manager).replace(DoTransfersNew.APOST, DoTransfersNew.APOST_REP);
		
		int score_tot = 0, score_gw = 0;
		if (App.cur_gameweek > 0) {
			score_gw = Integer.parseInt(m.group(SQUAD_GW_SCORE));
			score_tot = Integer.parseInt(m.group(SQUAD_TOT_SCORE).replace(",", ""));
		}
		
		String[] where_args = new String[1];
		where_args[0] = m.group(SQUAD_ID);
		App.log(team_name + " gw: " + score_gw + " total: " + score_tot);
		boolean store_squad_tot = true;
		
		Cursor curSquad = App.dbGen.rawQuery("SELECT name, player_name, points, rival FROM squad WHERE _id = " + team_id, null);
		curSquad.moveToFirst();
		if (curSquad.isAfterLast()) {
			App.log("insert squad " + team_id + ": " + team_name + " (" + manager_name + ")");
			ContentValues ins = new ContentValues();
			ins.put(f_id, team_id);
			ins.put(f_name, DbGen.compress(team_name));
			ins.put(f_player_name, DbGen.compress(manager_name));
			// always store total for new teams... can't be rivals yet
			App.log("storing total: " + score_tot);
			ins.put(f_points, score_tot);
			dbGen.insert(t_squad, null, ins);
		} else {
			String stored_team_name = DbGen.decompress(curSquad.getBlob(0));
			String stored_manager_name = DbGen.decompress(curSquad.getBlob(1));
			int stored_score_tot = curSquad.getInt(2);
			int rival = curSquad.getInt(3);
			if (rival == 1) {
				store_squad_tot = false;
			}
			
			ContentValues upd = new ContentValues();
			if (!team_name.equals(stored_team_name)) {
				upd.put(f_name, DbGen.compress(team_name));
			}
			if (!manager_name.equals(stored_manager_name)) {
				upd.put(f_player_name, DbGen.compress(manager_name));
			}
			if (store_squad_tot && (score_tot != stored_score_tot) ) {
				App.log("storing total: " + score_tot);
				upd.put(f_points, score_tot);
			}
			if (upd.size() > 0) {
				dbGen.update(t_squad, upd, where_squad, where_args);
			}
		}
		curSquad.close();
		
		Cursor sgw = dbGen.rawQuery("SELECT points FROM squad_gameweek WHERE squad_id = " + team_id
				+ " AND gameweek = " + App.cur_gameweek, null);
		sgw.moveToFirst();
		if (sgw.isAfterLast()) {
			ContentValues insSquadGw = new ContentValues();
			insSquadGw.put(f_squad_id, team_id);
			insSquadGw.put(f_gameweek, App.cur_gameweek);
			if (store_squad_tot) {
				insSquadGw.put(f_points, score_gw);
			}
			dbGen.insert(t_squad_gameweek, null, insSquadGw);
		} else {
			if (store_squad_tot) {
				dbGen.execSQL("UPDATE squad_gameweek SET points = " + score_gw + " WHERE squad_id = " + team_id
						+ " AND gameweek = " + App.cur_gameweek + " AND (points IS NULL OR points != " + score_gw + ")");
			}
		}
		sgw.close();
	}
	
	private void getRivalLineUps(int gameweek) {
		Cursor curRiv = dbGen.rawQuery("SELECT _id FROM squad WHERE rival = 1 OR _id = " + App.my_squad_id, null);
		curRiv.moveToFirst();
		
		int max = curRiv.getCount();
		max *= gameweek;
		max += gameweek;

		result.setMaxName(max, "get league teams");
		
		HashSet<Integer> gw_to_process = new HashSet<Integer>();
		
		int count = 0;
		while (!curRiv.isAfterLast()) {
			int squad_id = curRiv.getInt(0);
			for (int gw = 1; gw <= gameweek; gw++) {
				if (!result.failed) {
					boolean changed = ScrapeSquadGameweek.updateSquad(result, squad_id, gw);
					if (changed) {
						gw_to_process.add(gw);
					}
				}
					
				result.setProgress(++count);
			}
			curRiv.moveToNext();
		}
		curRiv.close();
		
		// cup opponent
		Cursor curCup = dbGen.rawQuery("SELECT cup_opp FROM squad_gameweek"
				+ " WHERE squad_id = " + App.my_squad_id + " AND gameweek = " + gameweek, null);
		curCup.moveToFirst();
		if (!curCup.isAfterLast()) {
			int cup_opp = curCup.getInt(0);
			if (cup_opp > 0) {
				boolean changed = ScrapeSquadGameweek.updateSquad(result, cup_opp, gameweek);
				if (changed) {
					gw_to_process.add(gameweek);
				}
			}
		}
		curCup.close();
		
		gw_to_process.add(gameweek);
		for (int gw : gw_to_process) {
			if (squadUtil == null) {
				squadUtil = new SquadUtil();
			}
			squadUtil.updateSquadGameweekScores(gw, true);
			result.setProgress(++count);
		}
	}
	
	private SquadUtil squadUtil;
	
}
