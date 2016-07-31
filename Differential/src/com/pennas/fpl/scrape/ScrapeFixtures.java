package com.pennas.fpl.scrape;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.pennas.fpl.App;
import com.pennas.fpl.Vidiprinter;
import com.pennas.fpl.Vidiprinter.VidiEntry;
import com.pennas.fpl.process.FPLService.ProcRes;
import com.pennas.fpl.process.UpdateManager;
import com.pennas.fpl.util.DbGen;
import com.pennas.fpl.util.SquadUtil;
import com.pennas.fpl.util.URLUtil;
import com.pennas.fpl.util.URLUtil.UrlObj;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;

public class ScrapeFixtures {
	//http://www.premierleague.com/ajax/site-header/ajax-all-results.json
	//http://www.premierleague.com/ajax/site-header/ajax-all-fixtures.json
	
	private static final String t_fixture = "fixture";
	private static final String t_where_fixture = "_id = ?";
	private String[] t_wArgs = { "0" };
	
	//private static final String URL_FIXTURES = "http://192.168.1.5/";
	private static final String URL_FIXTURES = "http://fantasy.premierleague.com/fixtures/";
	
	//private boolean changed;
	
	private Pattern p_fix = Pattern.compile("<tr class=\"ismFixture( ismResult)?\">(.+?)</tr>", Pattern.MULTILINE | Pattern.DOTALL);
	
	// match is in present/past
	/* <td>13 Aug 15:00</td>
                        <td class="ismHomeTeam">Blackburn</td>
                        <td><img alt="Blackburn" src="http://cdn.ismfg.net/static/plfpl/img/badges/badge_3.png" class="ismBadge"></td>
                        <td class="ismScore">1 - 2</td>
                        <td><img alt="Wolves" src="http://cdn.ismfg.net/static/plfpl/img/badges/badge_20.png" class="ismBadge"></td>
                        <td class="ismAwayTeam">Wolves</td> */
	//
	/* <td>20 Aug 17:30</td>
                        <td class="ismHomeTeam">Chelsea</td>
                        <td><img alt="Chelsea" src="/static/plfpl/img/badges/badge_5.png" class="ismBadge"></td>
                        <td class="ismScore">2 - 1</td>
([0-9]+) - ([0-9]+)
                        <td><img alt="West Brom" src="/static/plfpl/img/badges/badge_18.png" class="ismBadge"></td> */
	
	//<tr class="ismFixture ismResult"><td>18 Aug 15:00</td><td class="ismHomeTeam">Arsenal</td>
	//<td><img alt="Arsenal" src="http://cdn.ismfg.net/static/plfpl/img/badges/badge_1.png" class="ismBadge">
	//</td><td class="ismScore">0 - 0</td><td><img alt="Sunderland" src="http://cdn.ismfg.net/static/plfpl/img/badges/badge_15.png"
	//class="ismBadge"></td><td class="ismAwayTeam">Sunderland</td></tr>
	private static Pattern p_fix_past = Pattern.compile("<td>([0-9]+) ([A-Z][a-z]+) ([0-9][0-9]):([0-9][0-9])</td>\\W*?"
			+ "<td class=\"ismHomeTeam\">[\\w\\s]+</td>\\W*?"
			+ "<td><img alt=\"[\\w\\s]+\" src=\"http://cdn.ismfg.net/static/plfpl/img/badges/badge_([0-9]+)\\.png\" class=\"ismBadge\"></td>\\W*?"
			+ "<td class=\"ismScore\">([0-9]+) - ([0-9]+)</td>\\W*?"
			+ "<td><img alt=\"[\\w\\s]+\" src=\"http://cdn.ismfg.net/static/plfpl/img/badges/badge_([0-9]+)\\.png\"", Pattern.MULTILINE);
	
	// match is in future
	/* <td>21 Aug 13:30</td>
                    <td class="ismHomeTeam">Norwich</td>
                    <td><img alt="Norwich" src="/static/plfpl/img/badges/badge_12.png" class="ismBadge"></td>
                    <td class="ismScore">v</td>
                    <td><img alt="Stoke City" src="/static/plfpl/img/badges/badge_14.png" class="ismBadge"></td> */
	/* <td>21 Aug 16:00</td>
                    <td class="ismHomeTeam">Bolton</td>
                    <td><img alt="Bolton" src="/static/plfpl/img/badges/badge_4.png" class="ismBadge"></td>
                    <td class="ismScore">v</td>
                    <td><img alt="Man City" src="/static/plfpl/img/badges/badge_9.png" class="ismBadge"></td> */
	
	//<tr class="ismFixture"><td>25 Aug 15:00</td><td class="ismHomeTeam">Norwich</td><td><img alt="Norwich"
	//src="http://cdn.ismfg.net/static/plfpl/img/badges/badge_10.png" class="ismBadge"></td>
	//<td class="ismScore">v</td><td><img alt="QPR" src="http://cdn.ismfg.net/static/plfpl/img/badges/badge_11.png"
	//class="ismBadge"></td><td class="ismAwayTeam">QPR</td></tr>
	private static Pattern p_fix_future = Pattern.compile("<td>([0-9]+) ([A-Z][a-z]+) ([0-9][0-9]):([0-9][0-9])</td>\\W*?"
	+ "<td class=\"ismHomeTeam\">[\\w\\s]+</td>\\W*?"
	+ "<td><img alt=\"[\\w\\s]+\" src=\"http://cdn.ismfg.net/static/plfpl/img/badges/badge_([0-9]+)\\.png\" class=\"ismBadge\"></td>\\W*?"
	+ "<td class=\"ismScore\">v</td>\\W*?"
	+ "<td><img alt=\"[\\w\\s]+\" src=\"http://cdn.ismfg.net/static/plfpl/img/badges/badge_([0-9]+)\\.png\"", Pattern.MULTILINE);
	
	private int season = App.season;
	private int gameweek;
	private boolean dataChanged = false;
	private ProcRes result;
	
	public void updateAllFixtures(ProcRes pResult) {
		result = pResult;
		result.setMaxName(App.num_gameweeks, "get fix");
		
		for (int gw=1; gw<=App.num_gameweeks; gw++) {
			boolean update = true;
			// don't do all prev gws if can avoid
			if (gw < App.cur_gameweek) {
				Cursor curFix = App.dbGen.rawQuery("SELECT EXISTS (SELECT _id FROM fixture"
						+ " WHERE season = " + season + " AND gameweek = " + gw + " AND got_bonus < 1)", null);
				curFix.moveToFirst();
				if (curFix.getInt(0) == 0) {
					update = false;
				}
				curFix.close();
			}
			
			if (update) {
				updateFixtures(gw);
				if (result.updating) {
					App.log("updating... break");
					break;
				}
			}
			result.setProgress(gw);
		}
		
		// update in-memory fixtures list, then stale markers
		if (dataChanged) {
			App.init_data_gameweek();
			UpdateManager.setStaleTimes();
		}
		
		result.setDataChanged(dataChanged);
		result.setComplete();
	}
	
	private boolean changes_made;
	
	private void updateFixtures(int p_gameweek) {
		Scanner scan = null;
		UrlObj con = null;
		gameweek = p_gameweek;
		changes_made = false;
		//boolean processed_data = false;
		try {
			//if (gameweek == 1) Debug.startMethodTracing("fpl_scrape_fixtures", 15000000);
			App.log("starting fixtures scrape");
			long startTime = System.currentTimeMillis();
			
			con = URLUtil.getUrlStream(URL_FIXTURES + gameweek + "/", true, URLUtil.NO_AUTH);
			
			if (con.ok) {
			
				scan = new Scanner(con.stream);
				
				processedFix = new ArrayList<String>();
				
				// team (player scores inside match)
				boolean foundSomething = false;
				while (scan.findWithinHorizon(p_fix, 0) != null) {
					processGWFixture(scan.match());
					foundSomething = true;
					//processed_data = true;
				}
				
				// remove any fixtures that are no longer in this gameweek - we have a floater
				if (foundSomething) {
				    Cursor curRem = App.dbGen.rawQuery("SELECT _id, team_home_id, team_away_id FROM fixture WHERE season = " + season + " AND gameweek = " + gameweek, null);
				    curRem.moveToFirst();
				    while (!curRem.isAfterLast()) {
				    	String id = curRem.getString(0);
				    	if (!processedFix.contains(id)) {
				    		startTransaction();
							App.dbGen.execSQL("UPDATE fixture SET datetime = null, gameweek = null, res_goals_home = null, res_goals_away = null, got_bonus = 0 WHERE _id = " + id);
				    		App.log("Fixture " + id + " removed from GW " + gameweek);
				    		
				    		changes_made = true;
				    		
				    		VidiEntry e = new VidiEntry();
				    		e.category = Vidiprinter.CAT_FIXTURE_MOVED;
				    		e.gameweek = App.cur_gameweek;
				    		int team_home_id = curRem.getInt(1);
				    		int team_away_id = curRem.getInt(2);
				    		String team_home = App.id2team.get(team_home_id);
				    		String team_away = App.id2team.get(team_away_id);
				    		e.message = team_home + " v " + team_away + " postponed from gameweek " + gameweek;
				    		Vidiprinter.addVidi(e, result.context);
				    	}
				    	curRem.moveToNext();
				    }
				    curRem.close();
				} else {
					App.log("Didn't find top level fixture pattern - not applying removals from gw");
				}
				
				Long timeDiff = System.currentTimeMillis() - startTime;
				float timeSecs = (float)timeDiff / 1000;
				App.log("done fixtures scrape in " + timeSecs + " seconds");
			} else {
				App.log("fixtures: url failed");
				result.setError("url failed (" + p_gameweek + ")");
				if (con.updating) {
					result.updating = true;
				}
			}
		} catch (IOException e) {
			App.exception(e);
			result.setError("1 " + e);
		} finally {
			if (scan != null) scan.close();
			try {
				if (con!=null && con.stream != null) con.stream.close();
			} catch (Exception e) {
				App.exception(e);
			}
			if (con != null) con.closeCon();
			endTransaction();
		}
		
		// if changed any fixtures affecting current week, recalc squad scores
		if (changes_made && (gameweek == App.cur_gameweek) ) {
			if (squadUtil == null) {
				squadUtil = new SquadUtil();
			}
			squadUtil.updateSquadGameweekScores(App.cur_gameweek, true);
		}
		
	}
	
	private SquadUtil squadUtil;
	
	ArrayList<String> processedFix;
	
	private void processGWFixture(MatchResult match) {
		String gameweek_match = match.group();
		//App.log("gameweek match: " + gameweek_match);
		
		boolean found_method_1 = false;
		FixtureData f = new FixtureData();
		
		// method 1
		Matcher m = p_fix_future.matcher(gameweek_match);
	    while (m.find()) {
	    	//App.log("HIT 1");
	    	f.dateday = m.group(1);
	    	f.datemon = m.group(2);
	    	f.timehour = m.group(3);
	    	f.timemin = m.group(4);
	    	f.hometeam = Integer.parseInt(m.group(5));
	    	f.awayteam = Integer.parseInt(m.group(6));
	    	f.gw = String.valueOf(gameweek);
	    	
	    	// change fpl team season id to base team _id
	    	f.hometeam = App.team_fpl2id.get(f.hometeam);
	    	f.awayteam = App.team_fpl2id.get(f.awayteam);
	    	
			processFixture(f);
			found_method_1 = true;
	    }
	    
	    if (!found_method_1) {
		    // method 1
			m = p_fix_past.matcher(gameweek_match);
		    while (m.find()) {
		    	//App.log("HIT 2");
		    	f.dateday = m.group(1);
		    	f.datemon = m.group(2);
		    	f.timehour = m.group(3);
		    	f.timemin = m.group(4);
		    	f.hometeam = Integer.parseInt(m.group(5));
		    	f.awayteam = Integer.parseInt(m.group(8));
		    	f.gw = String.valueOf(gameweek);
		    	//f.fplid = 
		    	f.homescore = m.group(6);
		    	f.awayscore = m.group(7);
		    	
		    	// change fpl team season id to base team _id
		    	f.hometeam = App.team_fpl2id.get(f.hometeam);
		    	f.awayteam = App.team_fpl2id.get(f.awayteam);
		    	
		    	processFixture(f);
		    }
		}
	    
	}
	
	private static final String[] month_year1 = { "Aug", "Sep", "Oct", "Nov", "Dec" };
	
	public static long convertDate(String day, String month, String hours, String minutes, int p_season) throws java.text.ParseException {
		boolean year1 = false;
		for (int i=0; i<month_year1.length; i++) {
			if (month_year1[i].equals(month)) {
				year1 = true;
				break;
			}
		}
		
		int year = 2000 + p_season;
		if (year1) {
			year -= 1;
		}
		
		String toParse = day + " " + month + " " + year;
		// Use US locale - default locale may not parse date correctly
		SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy", Locale.US);
		// parse using GMT. just to avoid any off-setting
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date date= df.parse(toParse);
		
		long time = date.getTime() / 1000;
		int i_hours = Integer.parseInt(hours);
		int i_mins = Integer.parseInt(minutes);
		
		time += (i_hours * 60 * 60);
		time += (i_mins * 60);
		
		//AppClass.log(toParse + " / " + time);
        return time;
	}
	
	private static final String f_datetime = "datetime";
	private static final String f_res_goals_home = "res_goals_home";
	private static final String f_res_goals_away = "res_goals_away";
	private static final String f_fpl_id = "fpl_id";
	private static final String f_res_points_home = "res_points_home";
	private static final String f_res_points_away = "res_points_away";
	private static final String f_gameweek = "gameweek";
	private static final String f_team_home_id = "team_home_id";
	private static final String f_team_away_id = "team_away_id";
	private static final String f_season = "season";
	private static final String f_stats_datetime = "stats_datetime";
	private static final String f_got_bonus = "got_bonus";
	boolean doUpdate;
	ContentValues updateVals;
	private boolean transaction = false;
	
	private void startTransaction() {
		if (!transaction) {
			App.dbGen.execSQL(DbGen.TRANSACTION_IMMEDIATE_BEGIN);
			transaction = true;
			dataChanged = true;
		}
	}
	private void endTransaction() {
		if (transaction) {
			try {
				App.dbGen.execSQL(DbGen.TRANSACTION_END);
			} catch (SQLException e) {
				App.exception(e);
			} catch (IllegalStateException e) {
				App.exception(e);
			}
			transaction = false;
		}
	}
	
	private void processFixture(FixtureData f_new) {
		try {
			f_new.date_utc = convertDate(f_new.dateday, f_new.datemon, f_new.timehour, f_new.timemin, season);
		} catch (ParseException e) {
			if (f_new.homescore == null) {
				App.exception(e);
				return;
			}
		}
		
		Cursor cur = App.dbGen.rawQuery("SELECT * FROM fixture WHERE season = " + season + " AND team_home_id = " + f_new.hometeam + " AND team_away_id = " + f_new.awayteam, null);
		cur.moveToFirst();
		if (cur.isAfterLast()) {
			// insert
			ContentValues insertVals = new ContentValues();
			insertVals.put(f_datetime, f_new.date_utc);
			insertVals.put(f_team_home_id, f_new.hometeam);
			insertVals.put(f_team_away_id, f_new.awayteam);
			insertVals.put(f_gameweek, f_new.gw);
			insertVals.put(f_season, season);
			insertVals.put(f_stats_datetime, 0);
			insertVals.put(f_got_bonus, 0);
			
			if (f_new.homescore != null) insertVals.put(f_res_goals_home, f_new.homescore);
			if (f_new.awayscore != null) insertVals.put(f_res_goals_away, f_new.awayscore);
			if (f_new.homepoints != null) insertVals.put(f_res_points_home, f_new.homepoints);
			if (f_new.awaypoints != null) insertVals.put(f_res_points_away, f_new.awaypoints);
			if (f_new.fplid != null) insertVals.put(f_fpl_id, f_new.fplid);
			
			String team_home = App.id2team.get(f_new.hometeam);
    		String team_away = App.id2team.get(f_new.awayteam);
    		
			App.log("insert " + f_new.hometeam + " (" + team_home + ") v "
					+ f_new.awayteam + " (" + team_away + ") gw " + f_new.gw);
			
			VidiEntry e = new VidiEntry();
    		e.category = Vidiprinter.CAT_FIXTURE_MOVED;
    		e.gameweek = App.cur_gameweek;
    		e.message = team_home + " v " + team_away + " rescheduled for " + App.printDate(f_new.date_utc)
    			+ " in gameweek " + gameweek;
    		Vidiprinter.addVidi(e, result.context);
			
			startTransaction();
			long id = App.dbGen.insert(t_fixture, null, insertVals);
			processedFix.add(String.valueOf(id));
			
			changes_made = true;
		} else {
			// check/update
			t_wArgs[0] = cur.getString(cur.getColumnIndex("_id"));
			FixtureData f_old = new FixtureData();
			f_old.awayscore = cur.getString(cur.getColumnIndex("res_goals_away"));
			f_old.date_utc = cur.getLong(cur.getColumnIndex("datetime"));
			f_old.homescore = cur.getString(cur.getColumnIndex("res_goals_home"));
			f_old.fplid = cur.getString(cur.getColumnIndex("fpl_id"));
			f_old.homepoints = cur.getString(cur.getColumnIndex("res_points_home"));
			f_old.awaypoints = cur.getString(cur.getColumnIndex("res_points_away"));
			f_old.gw = cur.getString(cur.getColumnIndex("gameweek"));
			if (f_old.gw == null) {
				f_old.gw = "";
			}
			
			updateVals = new ContentValues();
			doUpdate = false;
			
			String team_home = App.id2team.get(f_new.hometeam);
    		String team_away = App.id2team.get(f_new.awayteam);
    		
			// date all within try above
			if (f_new.date_utc != f_old.date_utc) {
				if (f_old.gw.equals(f_new.gw)) {
					updateVals.put(f_datetime, f_new.date_utc);
					doUpdate = true;
					App.log("datetime: " + f_old.date_utc + " -> " + f_new.date_utc);
					
					// if in future, reset score to null
					if (f_new.date_utc > App.currentUTCTime()) {
						updateVals.putNull(f_res_points_home);
						updateVals.putNull(f_res_points_away);
						updateVals.put(f_got_bonus, 0);
					}
					
					changes_made = true;
					
					VidiEntry e = new VidiEntry();
		    		e.category = Vidiprinter.CAT_FIXTURE_MOVED;
		    		e.gameweek = App.cur_gameweek;
		    		e.message = team_home + " v " + team_away + " moved to "
		    			+ App.printDate(f_new.date_utc) + " GMT (within gameweek " + gameweek + ")";
		    		Vidiprinter.addVidi(e, result.context);
				}
			}
			
			processedFix.add(t_wArgs[0]);
			
			if ((f_new.homescore != null) && (f_new.awayscore != null)) {
				int homeS = Integer.parseInt(f_new.homescore);
				int awayS = Integer.parseInt(f_new.awayscore);
				String points_home = null;
				String points_away = null;
				if (homeS > awayS) {
					points_home = "3";
					points_away = "0";
				} else if (awayS > homeS) {
					points_home = "0";
					points_away = "3";
				} else {
					points_home = "1";
					points_away = "1";
				}
				checkUpdate(f_old.homepoints, points_home, f_res_points_home);
				checkUpdate(f_old.awaypoints, points_away, f_res_points_away);
				
				// if past GW and have score/result, then mark as complete (got bonus)
				if (Integer.parseInt(f_new.gw) < App.cur_gameweek) {
					updateVals.put(f_got_bonus, GOT_BONUS);
					doUpdate = true;
				}
				
				// update player_match.result_points
				App.dbGen.execSQL("UPDATE player_match SET result_points = " + points_home
						+ " WHERE fixture_id = " + t_wArgs[0] + " AND is_home = 1");
				App.dbGen.execSQL("UPDATE player_match SET result_points = " + points_away
						+ " WHERE fixture_id = " + t_wArgs[0] + " AND is_home IS NULL");
			}
			
			checkUpdate(f_old.gw, f_new.gw, f_gameweek);
			if (!f_old.gw.equals(f_new.gw)) {
				VidiEntry e = new VidiEntry();
	    		e.category = Vidiprinter.CAT_FIXTURE_MOVED;
	    		e.gameweek = App.cur_gameweek;
	    		e.message = team_home + " v " + team_away + " rescheduled for " + App.printDate(f_new.date_utc)
    			+ " in gameweek " + gameweek;
	    		Vidiprinter.addVidi(e, result.context);
	    		
	    		if (f_new.date_utc != f_old.date_utc) {
					updateVals.put(f_datetime, f_new.date_utc);
					// if in future, reset score to null
					if (f_new.date_utc > App.currentUTCTime()) {
						updateVals.putNull(f_res_points_home);
						updateVals.putNull(f_res_points_away);
						updateVals.put(f_got_bonus, 0);
					}
					doUpdate = true;
					App.log("datetime: " + f_old.date_utc + " -> " + f_new.date_utc);
					changes_made = true;
	    		}
			}
			if (f_new.fplid != null) checkUpdate(f_old.fplid, f_new.fplid, f_fpl_id);
			if (f_new.homescore != null) checkUpdate(f_old.homescore, f_new.homescore, f_res_goals_home);
			if (f_new.awayscore != null) checkUpdate(f_old.awayscore, f_new.awayscore, f_res_goals_away);
			
			if (doUpdate) {
				startTransaction();
				App.dbGen.update(t_fixture, updateVals, t_where_fixture, t_wArgs);
			}
			
		}
		cur.close();
	}
	
	private static final int GOT_BONUS = 2;
	
	private void checkUpdate(String oldVal, String newVal, String field) {
		if ( (oldVal==null && newVal != null) || !oldVal.equals(newVal) ) {
			updateVals.put(field, newVal);
			App.log(field + ": '" + oldVal + "' -> '" + newVal + "'");
			doUpdate = true;
		}
	}
	
	private class FixtureData {
		int hometeam;
		String fplid;
		String homescore;
		String awayscore;
		int awayteam;
		String dateday;
		String datemon;
		String timehour;
		String timemin;
		String gw;
		String homepoints;
		String awaypoints;
		long date_utc;
	}
	
}
