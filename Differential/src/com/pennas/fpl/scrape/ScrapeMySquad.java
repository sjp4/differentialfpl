package com.pennas.fpl.scrape;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.pennas.fpl.App;
import com.pennas.fpl.Settings;
import com.pennas.fpl.Vidiprinter;
import com.pennas.fpl.Vidiprinter.VidiEntry;
import com.pennas.fpl.process.FPLService.ProcRes;
import com.pennas.fpl.util.DbGen;
import com.pennas.fpl.util.SquadUtil;
import com.pennas.fpl.util.URLUtil;
import com.pennas.fpl.util.URLUtil.UrlObj;

public class ScrapeMySquad {
	private static final String URL_MYSQUAD = "http://fantasy.premierleague.com/my-team/";
	private int gameweek = App.next_gameweek;
	private int squad_id = App.my_squad_id;
	
	private static final String t_squad_gameweek_player = "squad_gameweek_player";
	private static final String t_transfer = "transfer";
	private static final String f_squad_id = "squad_id";
	private static final String f_gameweek = "gameweek";
	private static final String f_fpl_player_id = "fpl_player_id";
	private static final String f_selected = "selected";
	private static final String f_captain = "captain";
	private static final String f_player_fpl_id = "player_fpl_id";
	private static final String f_t_in = "t_in";
	
	private SQLiteDatabase dbGen = App.dbGen;
	
	private final Pattern pat_squad_id = Pattern.compile("href=\"/entry/([0-9]+)/history/\">View Gameweek history</a>");
	
	//"coptr": null, "played": 0, "pos": 11, "can_sub": 1, "ep_this": null, "event_points": 0, "id": 193, "sub": 0, "m": 1
	//, "copnr": null, "is_captain": false, "team": 7, "is_vice_captain": false, "type": 4, "ep_next": 3.6
	private final Pattern pat_player = Pattern.compile("\"id\": ([0-9]+), \"sub\": ([0-4]), \"m\": [0-9]+"
		+ ", \"copnr\": [0-9A-Za-z]+, \"is_captain\": (true|false), \"team\": [0-9]+"
		+ ", \"is_vice_captain\": (true|false)"
	);
	
	private final Pattern pat_manager_name = Pattern.compile("<h1 class=\"ismSection2 ismWrapText\">(.*?)</h1>");
	private final Pattern pat_squad_name = Pattern.compile("<h2 class=\"ismSection3\">(.*?)</h2>");
	
	/*<div class="inner">
                <h3>Gameweek 32</h3>
 					<p class="ismDeadline">
                      <strong>06 Apr 16:00</strong>
    */
	/*<div class="inner">
                <h3>Gameweek 1</h3>
                
                    
                        <p class="ismDeadline">
                            <strong>18 Aug 11:30</strong>
   */
	private final Pattern pat_deadline = Pattern.compile("<div class=\"inner\">\\s*"
			                                      + "<h3>Gameweek ([0-9]+)</h3>\\s*"
			                                      + "<p class=\"ismDeadline\">\\s*"
			          + "<strong>([0-9]+) ([A-Z][a-z]+) ([0-9][0-9]):([0-9][0-9])</strong>", Pattern.MULTILINE);
	
	private static final Pattern pat_bank = Pattern.compile("bank</dt>\\W+?<dd>£([0-9]+\\.[0-9]+)m", Pattern.MULTILINE|Pattern.DOTALL);
	
	private ProcRes result;
	
	public void updateSquad(ProcRes p_res) {
		Scanner scan = null;
		UrlObj con = null;
		result = p_res;
		try {
			result.setMaxName(10, "get my squad selection");
			
			// see if we already got the squad for that week (it can change)
			HashSet<Integer> old_squad = new HashSet<Integer>();
			Cursor cur = dbGen.rawQuery("SELECT fpl_player_id FROM squad_gameweek_player WHERE squad_id = " + squad_id
					+ " AND gameweek = " + gameweek, null);
			cur.moveToFirst();
			if (cur.getCount() != SquadUtil.SQUAD_SIZE) {
				// not got squad for this week already. get for last week, for comparison
				cur.close();
				cur = dbGen.rawQuery("SELECT fpl_player_id FROM squad_gameweek_player WHERE squad_id = " + squad_id
						+ " AND gameweek = " + (gameweek - 1), null);
				cur.moveToFirst();
			}
			while (!cur.isAfterLast()) {
				old_squad.add(cur.getInt(0));
				cur.moveToNext();
			}
			cur.close();
			
			// check if we actually have a valid squad for prev gameweek, for transfers comparison
			cur = dbGen.rawQuery("SELECT points_hit FROM squad_gameweek WHERE squad_id = " + squad_id
					+ " AND gameweek = " + (gameweek - 1) + " AND points_hit IS NOT NULL", null);
			cur.moveToFirst();
			boolean record_transfers = true;
			if (cur.isAfterLast()) {
				// didn't find an entry. disable transfers based on this comparison
				record_transfers = false;
			}
			cur.close();
			App.log("record_transfers: " + record_transfers);
			
			// build new squad for next gw
			HashSet<Integer> new_squad = new HashSet<Integer>();
			
			dbGen.execSQL(DbGen.TRANSACTION_IMMEDIATE_BEGIN);
			
			//Debug.startMethodTracing("fpl_scrape_p2", 15000000);
			App.log("starting my squad scrape gw " + gameweek);
			long startTime = System.currentTimeMillis();
			
			con = URLUtil.getUrlStream(URL_MYSQUAD, false, URLUtil.AUTH_FULL);
			if (con.ok) {
				scan = new Scanner(con.stream);
				
				ArrayList<ContentValues> toInsert = new ArrayList<ContentValues>();
				
				result.setProgress(2);
				
				// find gameweek deadlines
				long[] deadlines = new long[App.num_gameweeks + 1];
				while (scan.findWithinHorizon(pat_deadline, 0) != null) {
					MatchResult m = scan.match();
					
					int deadline_gw = Integer.parseInt(m.group(1));
					String dateday = m.group(2);
					String datemon = m.group(3);
					String timehour = m.group(4);
					String timemin = m.group(5);
					long date = ScrapeFixtures.convertDate(dateday, datemon, timehour, timemin, App.season);
					App.log("found deadline gw " + deadline_gw + ": " + App.printDate(date));
					
					deadlines[deadline_gw] = date;
				}
				
				// go through and check found deadlines
				// reverse order, so that end timestamp can be calculated
				Cursor curGw = dbGen.rawQuery("SELECT num, start_datetime FROM gameweek"
						+ " WHERE season = " + App.season
						+ " ORDER BY num DESC", null);
				curGw.moveToFirst();
				boolean changed_gw = false;
				while (!curGw.isAfterLast()) {
					int gw = curGw.getInt(0);
					long start = curGw.getLong(1);
					
					if ( (start != deadlines[gw]) && (deadlines[gw] > 0) ) {
						App.log("gw " + gw + " deadline from " + App.printDate(start)
						                            + " to " + App.printDate(deadlines[gw]));
						
						VidiEntry e = new VidiEntry();
						e.gameweek = gameweek;
						e.category = Vidiprinter.CAT_DEADLINE_MOVED;
						e.message = "The deadline for gameweek " + gw + " has been moved from " + App.printDate(start)
								              + " to " + App.printDate(deadlines[gw]);
						Vidiprinter.addVidi(e, result.context);
						
						dbGen.execSQL("UPDATE gameweek SET start_datetime = " + deadlines[gw]
								   + " WHERE num = " + gw);
						
						if (gw > 1) {
							int gw_prev = gw - 1;
							long prev_end = deadlines[gw] - 1;
							App.log("Also setting end_datetime of " + gw_prev + " to " + App.printDate(prev_end));
							
							dbGen.execSQL("UPDATE gameweek SET end_datetime = " + prev_end
									   + " WHERE num = " + gw_prev);
						}
						
						changed_gw = true;
					}
					
					curGw.moveToNext();
				}
				curGw.close();
				
				if (changed_gw) {
					App.init_data();
				}
				
				result.setProgress(3);
				
				// find team members + selection details
				while (scan.findWithinHorizon(pat_player, 0) != null) {
					MatchResult p = scan.match();
					boolean capt = Boolean.parseBoolean(p.group(3));
					int sub = Integer.parseInt(p.group(2));
					boolean vice_capt = Boolean.parseBoolean(p.group(4));
					int fpl_player_id = Integer.parseInt(p.group(1));
					App.log("capt: " + capt + " sub: " + sub + " vice_capt: " + vice_capt + " fpl_id: " + fpl_player_id);
					
					// new captain rules
					int captain = 0;
					if (capt) {
						captain = SquadUtil.CAPTAIN;
					}
					if (vice_capt) {
						captain = SquadUtil.VICE_CAPTAIN;
					}
					
					// insert player details into db
					ContentValues ins = new ContentValues();
					ins.put(f_gameweek, gameweek);
					ins.put(f_fpl_player_id, fpl_player_id);
					ins.put(f_selected, sub);
					ins.put(f_captain, captain);
					toInsert.add(ins);
					
					// add to set for transfers
					new_squad.add(fpl_player_id);
				}
				
				result.setProgress(4);
				
				// get manager name
				String found = scan.findWithinHorizon(pat_manager_name, 0);
				if (found == null) {
					App.log("pat_manager_name not found... exiting");
					result.setError("1");
					return;
				}
				MatchResult nm = scan.match();
				String player_name = nm.group(1);
				
				// get team name
				found = scan.findWithinHorizon(pat_squad_name, 0);
				if (found == null) {
					App.log("pat_squad_name not found... exiting");
					result.setError("2");
					return;
				}
				nm = scan.match();
				String squad_name = nm.group(1);
				
				App.log("squad name: " + squad_name + " manager name: " + player_name);
				
				// now find out my squad id
				found = scan.findWithinHorizon(pat_squad_id, 0);
				if (found == null) {
					result.setError("3");
					return;
				}
				MatchResult sqm = scan.match();
				squad_id = Integer.parseInt(sqm.group(1));
				App.log("squad id found: " + squad_id + " (" + found + ")");
				// store in preference and memory
				Settings.setIntPref(Settings.PREF_FPL_SQUADID, squad_id, result.context);
				App.my_squad_id = squad_id;
				App.my_squad_name = squad_name;
				
				float bank = 0;
				if (scan.findWithinHorizon(pat_bank, 0) != null) {
					MatchResult mr_bank = scan.match();
					String bank_string = mr_bank.group(1);
					bank = Float.parseFloat(bank_string);
					App.log("in the bank: " + bank);
				} else {
					App.log("bank not found");
				}
				
				result.setProgress(6);
				
				// delete existing swg/sgwp recs
				dbGen.execSQL("DELETE FROM squad_gameweek WHERE squad_id = " + squad_id + " AND gameweek = " + gameweek);
				dbGen.execSQL("DELETE FROM squad_gameweek_player WHERE squad_id = " + squad_id + " AND gameweek = " + gameweek);
				
				// do actual inserts after squad id is known!
				// first squad... check if exists already
				ContentValues insSquad = new ContentValues();
				insSquad.put("name", DbGen.compress(squad_name));
				insSquad.put("player_name", DbGen.compress(player_name));
				insSquad.put("bank", bank);
				Cursor curSq = dbGen.rawQuery("SELECT name FROM squad WHERE _id = " + squad_id, null);
				curSq.moveToFirst();
				if (curSq.isAfterLast()) {
					// insert
					insSquad.put("_id", squad_id);
					dbGen.insert("squad", null, insSquad);
				} else {
					dbGen.update("squad", insSquad, "_id = " + squad_id, null);
				}
				curSq.close();
				
				result.setProgress(8);
				
				// squad_gameweek
				// delete existing squad gameweek records for next gw
				ContentValues insSquadGw = new ContentValues();
				insSquadGw.put("squad_id", squad_id);
				insSquadGw.put("gameweek", gameweek);
				dbGen.insert("squad_gameweek", null, insSquadGw);
				
				// then squad_gameweek_player
				App.mySquadNext.clear();
				for (ContentValues ins : toInsert) {
					// add squad id then insert
					ins.put(f_squad_id, squad_id);
					dbGen.insert(t_squad_gameweek_player, null, ins);
					App.mySquadNext.add(ins.getAsInteger(f_fpl_player_id));
				}
				
				// add transfer records
				if ( (gameweek > 1) && record_transfers) {
					if (new_squad.size() == SquadUtil.SQUAD_SIZE) {
						if (old_squad.size() == SquadUtil.SQUAD_SIZE) {
							for (int fpl_id : new_squad) {
								if (!old_squad.contains(fpl_id)) {
									// add transfer item
									ContentValues ins = new ContentValues();
									ins.put(f_squad_id, squad_id);
									ins.put(f_gameweek, gameweek);
									ins.put(f_player_fpl_id, fpl_id);
									ins.put(f_t_in, 1);
									App.dbGen.replace(t_transfer, null, ins);
								}
							}
							
							// and transfers out
							for (int fpl_id : old_squad) {
								if (!new_squad.contains(fpl_id)) {
									// add transfer item
									ContentValues ins = new ContentValues();
									ins.put(f_squad_id, squad_id);
									ins.put(f_gameweek, gameweek);
									ins.put(f_player_fpl_id, fpl_id);
									App.dbGen.replace(t_transfer, null, ins);
								}
							}
						}
					}
				}
				
				Long timeDiff = System.currentTimeMillis() - startTime;
				float timeSecs = (float)timeDiff / 1000;
				App.log("done my squad scrape in " + timeSecs + " seconds");
			} else {
				App.log("fixtures: url failed");
				result.setError("url failed");
				if (con.updating) {
					result.updating = true;
				}
			}
		} catch (IOException e) {
			App.exception(e);
			result.setError("4 " + e);
		} catch (ParseException e) {
			App.exception(e);
			result.setError("5 " + e);
		} finally {
			try {
				dbGen.execSQL(DbGen.TRANSACTION_END);
			} catch (SQLException e) {
				App.exception(e);
			} catch (IllegalStateException e) {
				App.exception(e);
			}
			if (scan != null) scan.close();
			try {
				if (con!=null && con.stream != null) con.stream.close();
			} catch (Exception e) {
				App.exception(e);
			}
			if (con != null) con.closeCon();
			if (result.failed) return;
		}
		
		getNotes();
		
		//Debug.stopMethodTracing();
		result.setDataChanged(true);
		result.setComplete();
	}
	
	public static final String URL_NOTES = "http://fantasy.premierleague.com/web/api/notes/";
	public static final int NOTES_MAX_SIZE = 1000;
	
	private void getNotes() {
		UrlObj con = null;
		InputStreamReader iread = null;
		result.setMaxName(1, "get notes");
		
		try {
			// see if we already got the squad for that week (it can change)
			App.log("starting get notes..");
			long startTime = System.currentTimeMillis();
			
			con = URLUtil.getUrlStream(URL_NOTES, false, URLUtil.AUTH_FULL);
			if (con.ok) {
				iread = new InputStreamReader(con.stream, "UTF-8");
			    
			    char[] buffer = new char[NOTES_MAX_SIZE + 2];
			    int size = iread.read(buffer);
			    
			    StringBuilder notes = new StringBuilder();
			    notes.append(buffer, 0, size);
			    // delete leading/trailing "
			    notes.deleteCharAt(0);
			    notes.deleteCharAt(notes.length() - 1);
			    
			    //App.log("read notes: '" + notes + "'");
			    
			    String notes_formatted = notes.toString().replace("\\n", "\n");
			    notes_formatted = notes_formatted.replace("\\\"", "\"");
			    
			    //App.log("formatted: " + notes_formatted);
			    
			    String encoded = URLEncoder.encode(notes_formatted, "utf-8");
			    
			    //App.log("encoded: '" + encoded + "'");
			    
			    Settings.setStringPref(Settings.PREF_NOTES, encoded, result.context);
				
				Long timeDiff = System.currentTimeMillis() - startTime;
				float timeSecs = (float)timeDiff / 1000;
				App.log("done get notes version in " + timeSecs + " seconds");
			} else {
				App.log("notes: url failed");
				result.setError("url failed");
				if (con.updating) {
					result.updating = true;
				}
			}
		} catch (IOException e) {
			App.exception(e);
			result.setError("notes: " + e);
		} finally {
			if (iread != null) {
		    	try { 
		    		iread.close();
		    	} catch (IOException e) {
		    		App.exception(e);
		    	}
		    }
			if (con != null) con.closeCon();
		}
	}
}
