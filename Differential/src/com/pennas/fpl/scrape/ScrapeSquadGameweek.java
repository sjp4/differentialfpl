package com.pennas.fpl.scrape;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
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

public class ScrapeSquadGameweek {
	private static final String t_squad_gameweek_player = "squad_gameweek_player";
	private static final String t_squad_gameweek = "squad_gameweek";
	private static final String t_squad = "squad";
	private static final String t_transfer = "transfer";
	private static final String f_squad_id = "squad_id";
	private static final String f_gameweek = "gameweek";
	private static final String f_fpl_player_id = "fpl_player_id";
	private static final String f_selected = "selected";
	private static final String f_captain = "captain";
	private static final String f_points = "points";
	private static final String f_transfers_next = "transfers_next";
	private static final String f_points_hit = "points_hit";
	private static final String f_player_fpl_id = "player_fpl_id";
	private static final String f_t_in = "t_in";
	private static final String f_not_started = "not_started";
	private static final String f_cup_opp = "cup_opp";
	private static final String f_chip = "chip";
	
	private static final String chip_bench_boost = "Bench Boost";
	private static final String chip_triple_captain = "Triple Captain";
	private static final String chip_all_out_attack = "All Out Attack";
	
	public static final String[] chip_strings = { "", chip_bench_boost, chip_triple_captain, chip_all_out_attack };
	
	public static final int c_chip_none = 0;
	public static final int c_chip_bench_boost = 1;
	public static final int c_chip_triple_captain = 2;
	public static final int c_chip_all_out_attack = 3;
	
	/*
	private static Pattern pat_player = Pattern.compile("\\{\"is_captain\": (true|false), \"pos\": [0-9]+, \"sub\": ([0-4]), \"played\": [0-9]+, \"type\": [0-9]+, "
			+ "\"is_vice_captain\": (true|false), \"team\": [0-9]+, \"m\": [0-9]+, \"id\": ([0-9]+), \"out\": 1\\}");
	*/
	// LEAP OF FAITH - this page format is not known until GW1 starts, but I am assuming that it changes 
	// in the same way that the selection page did for 2012/13 (2011/12 regex was exactly the same)
	private static final Pattern pat_player = Pattern.compile("\"id\": ([0-9]+), \"sub\": ([0-4]), \"m\": [0-9]+"
			+ ", \"copnr\": [0-9A-Za-z]+, \"is_captain\": (true|false), \"team\": [0-9]+"
			+ ", \"is_vice_captain\": (true|false)"
	);
	
	private static final Pattern pat_transfers = Pattern.compile("<dt>Transfers</dt>\\W+<dd>\\W+([0-9]+)\\W+\\(?-?([0-9]+)?"
			, Pattern.MULTILINE|Pattern.DOTALL);
	
	private static final Pattern pat_overall_points = Pattern.compile("<dt>Overall Points</dt>\\W+<dd>([0-9,]+)</dd>"
			, Pattern.MULTILINE|Pattern.DOTALL);
	
	//<div class="ism-scoreboard-panel__points ism-scoreboard-panel__points--active-chip">69<sub>pts</sub></div>
	//<div class="ism-scoreboard-panel__points">17<sub>pts</sub></div>
	private static final Pattern pat_gw_points = Pattern.compile("<div class=\"ism-scoreboard-panel__points(?:\\Wism-scoreboard-panel__points--active-chip)?\">([0-9]+)<sub>pts</sub>"
			, Pattern.DOTALL);
	
	private static final Pattern pat_player_name = Pattern.compile("<h1 class=\"ismSection2 ismWrapText\">(.+?)</h1>", Pattern.DOTALL);
	private static final Pattern pat_squad_name  = Pattern.compile("<h2 class=\"ismSection3\">(.+?)</h2>", Pattern.DOTALL);
	
	private static final Pattern pat_autosubs = Pattern.compile(
			"<h3>Automatic substitutions</h3>"
			+ "\\W+<table class=\"ismTable\">"
			+ "\\W+<colgroup>"
			+ "\\W+<col class=\"ism1\"></col>"
			+ "\\W+<col class=\"ism2\"></col>"
			+ "\\W+</colgroup>"
			+ "\\W+<thead>"
			+ "\\W+<tr>"
			+ "\\W+<th class=\"ism1\">Out</th>"
			+ "\\W+<th class=\"ism2\">In</th>"
			+ "\\W+</tr>"
			+ "\\W+</thead>"
			+ "\\W+<tbody>(.+?)</tbody>"
			+ "\\W+</table>", Pattern.MULTILINE|Pattern.DOTALL);
	
	private static final Pattern pat_autosub_pair = Pattern.compile(
			"<tr>"
			+ "\\W+<td class=\"ismCol1\">(.+?)</td>"
			+ "\\W+<td class=\"ismCol2\">(.+?)</td>"
			+ "\\W+</tr>", Pattern.MULTILINE|Pattern.DOTALL);
	
	private static final Pattern pat_chip = Pattern.compile(
			"<div class=\"ism-scoreboard__chip-status\">"
            + "\\W+(.+?) played"
            + "\\W+</div>");
	
	//<dt>In the bank</dt>
    //<dd>£1.9m</dd>
	private static final Pattern pat_bank = Pattern.compile("bank</dt>\\W+?<dd>£([0-9]+\\.[0-9]+)m", Pattern.MULTILINE|Pattern.DOTALL);
	
	private static class Player {
		int fpl_player_id;
		int selected;
		int captain;
	}
	
	public static boolean updateSquad(ProcRes result, int squad_id, int gameweek) {
		Scanner scan = null;
		UrlObj con = null;
		SQLiteDatabase dbGen = App.dbGen;
		App.log("updateSquad " + squad_id + " gw " + gameweek);
		
		// previous stored squad for current gw
		HashSet<Integer> existing_cur_week_squad = new HashSet<Integer>();
		Cursor cur = dbGen.rawQuery("SELECT fpl_player_id FROM squad_gameweek_player WHERE squad_id = " + squad_id
				+ " AND gameweek = " + gameweek, null);
		cur.moveToFirst();
		while (!cur.isAfterLast()) {
			existing_cur_week_squad.add(cur.getInt(0));
			cur.moveToNext();
		}
		cur.close();
		
		/*
		// already got, can't change
		if (existing_cur_week_squad.size() == SquadUtil.SQUAD_SIZE) {
			App.log("already got squad for gw");
			return false;
		}
		*/
		
		cur = dbGen.rawQuery("SELECT points_hit, not_started FROM squad_gameweek WHERE squad_id = " + squad_id
				+ " AND gameweek = " + gameweek + " AND ((points_hit IS NOT NULL) OR (not_started = 1))", null);
		cur.moveToFirst();
		// got correct size squad already AND marked num transfers, then don't get
		if (!cur.isAfterLast()) {
			// marked as not started by this week (ie team entered after this gameweek)
			if (cur.getInt(1) == 1) {
				App.log("Marked as not started");
				cur.close();
				return false;
			// otherwise if present then must be non-null points hit
			} else if (existing_cur_week_squad.size() == SquadUtil.SQUAD_SIZE) {
				//App.log("already got squad for gw");
				cur.close();
				return false;
			}
		}
		cur.close();
		
		Pattern pat_gw_points_this_week = Pattern.compile("<dd><a href=\"/entry/"
				+ squad_id + "/event-history/" + gameweek + "/\">([0-9]+)</a></dd>");
		
		// previous week squad
		HashSet<Integer> prev_week_squad = new HashSet<Integer>();
		cur = dbGen.rawQuery("SELECT fpl_player_id FROM squad_gameweek_player WHERE squad_id = " + squad_id
				+ " AND gameweek = " + (gameweek - 1), null);
		cur.moveToFirst();
		while (!cur.isAfterLast()) {
			prev_week_squad.add(cur.getInt(0));
			cur.moveToNext();
		}
		cur.close();
		
		// build new squad for this gw
		HashSet<Integer> new_cur_week_squad = new HashSet<Integer>();
		
		// get squad name
		cur = dbGen.rawQuery("SELECT name, player_name FROM squad WHERE _id = " + squad_id, null);
		cur.moveToFirst();
		boolean found = false;
		String squad_name_stored = null;
		String player_name_stored = null;;
		if (!cur.isAfterLast()) {
			squad_name_stored = DbGen.decompress(cur.getBlob(0));
			player_name_stored = DbGen.decompress(cur.getBlob(1));
			found = true;
		}
		cur.close();
		
		try {
			dbGen.execSQL(DbGen.TRANSACTION_IMMEDIATE_BEGIN);
			
			App.log("starting squad " + squad_id + " gameweek " + gameweek + " scrape ");
			long startTime = System.currentTimeMillis();
			
			// delete existing swg/sgwp recs
			dbGen.execSQL("DELETE FROM squad_gameweek_player WHERE squad_id = " + squad_id + " AND gameweek = " + gameweek);
			
			con = URLUtil.getUrlStream("http://fantasy.premierleague.com/entry/" + squad_id + "/event-history/" + gameweek + "/"
					, false, URLUtil.AUTH_FULL);
			if (con.ok) {
				scan = new Scanner(con.stream);
				
				// get gw points baseline
				if (scan.findWithinHorizon(pat_gw_points, 0) == null) {
					App.log(" pat_gameweek_points not found... exiting");
					try {
						dbGen.execSQL(DbGen.TRANSACTION_END);
					} catch (SQLException e) {
						App.exception(e);
					} catch (IllegalStateException e) {
						App.exception(e);
					}
					return false;
				}
				MatchResult mr_gw = scan.match();
				String gw_points_string = mr_gw.group(1);
				int gw_points = Integer.parseInt(gw_points_string);
				
				int chip = c_chip_none;
				// did they play any chips?
				if (scan.findWithinHorizon(pat_chip, 0) != null) {
					MatchResult mr_chip = scan.match();
					String chipPlayed = mr_chip.group(1);
					App.log("played chip: '" + chipPlayed + "'");
					if (chip_bench_boost.equalsIgnoreCase(chipPlayed)) {
						App.log(" = bench boost...");
						chip = c_chip_bench_boost;
					} else if (chip_triple_captain.equalsIgnoreCase(chipPlayed)) {
						App.log(" = triple captain...");
						chip = c_chip_triple_captain;
					} else if (chip_all_out_attack.equalsIgnoreCase(chipPlayed)) {
						App.log(" = all out attack...");
						chip = c_chip_all_out_attack;
					}
				}
				
				// get transfers/hits
				if (scan.findWithinHorizon(pat_transfers, 0) == null) {
					App.log(" pat_transfers not found... exiting");
					try {
						dbGen.execSQL(DbGen.TRANSACTION_END);
					} catch (SQLException e) {
						App.exception(e);
					}
					return false;
				}
				MatchResult mr_trans = scan.match();
				int transfers = Integer.parseInt(mr_trans.group(1));
				int hits = 0;
				if (mr_trans.group(2) != null) {
					hits = Integer.parseInt(mr_trans.group(2));
				}
				
				HashMap<Integer, Player> players = new HashMap<Integer, Player>();
				HashMap<String, Player> players_str_for_subs = new HashMap<String, Player>();
				
				// find team members + selection details
				while (scan.findWithinHorizon(pat_player, 0) != null) {
					MatchResult p = scan.match();
					boolean capt = Boolean.parseBoolean(p.group(3));
					int sub = Integer.parseInt(p.group(2));
					boolean vice_capt = Boolean.parseBoolean(p.group(4));
					int fpl_player_id = Integer.parseInt(p.group(1));
					
					// get name from db
					String name = null;
					Cursor curName = dbGen.rawQuery("SELECT p.name FROM player p, player_season ps"
							+ " WHERE ps.season = " + App.season
							+ " AND   ps.fpl_id = " + fpl_player_id
							+ " AND   p._id = ps.player_id", null);
					curName.moveToFirst();
					if (!curName.isAfterLast()) {
						name = curName.getString(0);
					}
					curName.close();
					
					App.log("capt: " + capt + " sub: " + sub + " vice_capt: " + vice_capt
							+ " fpl_id: " + fpl_player_id + " name: " + name);
					
					// new captain rules
					int captain = 0;
					if (capt) {
						captain = SquadUtil.CAPTAIN;
					}
					if (vice_capt) {
						captain = SquadUtil.VICE_CAPTAIN;
					}
					
					Player pl = new Player();
					pl.fpl_player_id = fpl_player_id;
					pl.captain = captain;
					pl.selected = sub;
					players.put(fpl_player_id, pl);
					players_str_for_subs.put(name, pl);
					
					// add to set for transfers
					new_cur_week_squad.add(fpl_player_id);
					
					// we know this is the last player
					if (sub == 4) {
						break;
					}
				}
				
				// get autosubs for original player selection
				if (scan.findWithinHorizon(pat_autosubs, 0) != null) {
					MatchResult mr_autos = scan.match();
					String autosubs = mr_autos.group(1);
					Matcher m_sub = pat_autosub_pair.matcher(autosubs);
				    while (m_sub.find()) {
				    	String out = m_sub.group(1);
				    	String in  = m_sub.group(2);
				    	App.log("Out: " + out);
				    	App.log("In: " + in);
				    	
				    	Player p_out = players_str_for_subs.get(out);
				    	Player p_in = players_str_for_subs.get(in);
				    	
				    	if ( (p_out != null) && (p_in != null) ) {
				    		int out_sel = p_out.selected;
				    		p_out.selected = p_in.selected;
				    		p_in.selected = out_sel;
				    	} else {
				    		App.log("autosubs not processed (null)");
				    	}
				    	
				    }
				}
				
				// write to db now autosubs processing done
				for (Player p : players.values()) {
					// insert player details into db
					ContentValues ins = new ContentValues();
					ins.put(f_gameweek, gameweek);
					ins.put(f_fpl_player_id, p.fpl_player_id);
					ins.put(f_selected, p.selected);
					ins.put(f_captain, p.captain);
					ins.put(f_squad_id, squad_id);
					dbGen.insert(t_squad_gameweek_player, null, ins);
				}
				
				// get player name
				if (scan.findWithinHorizon(pat_player_name, 0) == null) {
					App.log(" pat_player_name not found... exiting");
					try {
						dbGen.execSQL(DbGen.TRANSACTION_END);
					} catch (SQLException e) {
						App.exception(e);
					}
					return false;
				}
				MatchResult mr_playername = scan.match();
				String player_name = mr_playername.group(1).replace(DoTransfersNew.APOST, DoTransfersNew.APOST_REP);
				App.log("player name: " + player_name);
				
				// get squad name
				if (scan.findWithinHorizon(pat_squad_name, 0) == null) {
					App.log(" pat_squad_name not found... exiting");
					try {
						dbGen.execSQL(DbGen.TRANSACTION_END);
					} catch (SQLException e) {
						App.exception(e);
					}
					return false;
				}
				MatchResult mr_squadname = scan.match();
				String squad_name = mr_squadname.group(1).replace(DoTransfersNew.APOST, DoTransfersNew.APOST_REP);
				App.log("squad name: " + squad_name);
				
				if (found) {
					if (!squad_name_stored.equals(squad_name)) {
						App.log("Updating squad name '" + squad_name_stored + "' -> '" + squad_name + "'");
						ContentValues upd = new ContentValues();
						upd.put("name", DbGen.compress(squad_name));
						String[] whereArgs = { String.valueOf(squad_id) };
						dbGen.update(t_squad, upd, "_id = ?", whereArgs);
					}
					if (!player_name_stored.equals(player_name)) {
						App.log("Updating player name '" + player_name_stored + "' -> '" + player_name + "'");
						ContentValues upd = new ContentValues();
						upd.put("player_name", DbGen.compress(player_name));
						String[] whereArgs = { String.valueOf(squad_id) };
						dbGen.update(t_squad, upd, "_id = ?", whereArgs);
					}
				} else {
					App.log("inserting squad record: " + squad_name + " / " + player_name);
					squad_name_stored = squad_name;
					player_name_stored = player_name;
					
					ContentValues ins = new ContentValues();
					ins.put("_id", squad_id);
					ins.put("name", DbGen.compress(squad_name));
					ins.put("player_name", DbGen.compress(player_name));
					dbGen.insert(t_squad, null, ins);
				}
				
				// get overall points
				if (scan.findWithinHorizon(pat_overall_points, 0) == null) {
					App.log(" pat_overall_points not found... exiting");
					try {
						dbGen.execSQL(DbGen.TRANSACTION_END);
					} catch (SQLException e) {
						App.exception(e);
					}
					return false;
				}
				MatchResult mr_overall = scan.match();
				String overall_points_string = mr_overall.group(1);
				int overall_points = Integer.parseInt(overall_points_string.replace(",", ""));
				
				// if scraping current GW, then use the right-panel GW score instead of the top-left 
				// live-updated one (more likely to be in sync with the total, for maths later)
				if (gameweek == App.cur_gameweek) {
					if (scan.findWithinHorizon(pat_gw_points_this_week, 0) == null) {
						App.log("pat_gw_points_this_week not found... exiting");
						try {
							dbGen.execSQL(DbGen.TRANSACTION_END);
						} catch (SQLException e) {
							App.exception(e);
						}
						return false;
					}
					mr_gw = scan.match();
					gw_points_string = mr_gw.group(1);
					gw_points = Integer.parseInt(gw_points_string);
					App.log("cur gw right-pan for gw points");
				}
				App.log("gw points: " + gw_points);
				
				// hack: if gw points is zero then points hit probably has not been applied to gw totals
				if (gw_points == 0) {
					overall_points -= hits;
					App.log("reducing total pts by gw hits (" + hits + ") as hack");
				}
				
				/*
				 <tr>
                        <td>GW21</td>
                        
                            <td>
                                <a href="/entry/234950/event-history/21/">Rainham Steel</a>
				*/
				Pattern pat_cup = Pattern.compile(
						"<tr>"
						+ "\\W+<td>GW" + gameweek + "</td>"
						+ "\\W+<td>"
						+ "\\W+<a href=\"/entry/([0-9]+)/event-history/" + gameweek + "/\">(.+?)</a>"
						, Pattern.MULTILINE|Pattern.DOTALL);
				int cup_opp = 0;
				
				if (scan.findWithinHorizon(pat_cup, 0) != null) {
					mr_gw = scan.match();
					String cup_string = mr_gw.group(1);
					String cup_opp_name = mr_gw.group(2);
					cup_opp = Integer.parseInt(cup_string);
					App.log("cup opp: " + cup_opp + " / '" + cup_opp_name + "'");
				} else {
					App.log("cup opp not found");
				}
				
				if (gameweek == App.cur_gameweek) {
					float bank = 0;
					if (scan.findWithinHorizon(pat_bank, 0) != null) {
						mr_gw = scan.match();
						String bank_string = mr_gw.group(1);
						bank = Float.parseFloat(bank_string);
						App.log("in the bank: " + bank);
					} else {
						App.log("bank not found");
					}
				
					App.log("overall points: " + overall_points);
					String update_points = "UPDATE squad SET points = " + overall_points;
					String update_bank = "";
					if (squad_id != App.my_squad_id) {
						update_bank = ", bank = " + bank;
					}
					dbGen.execSQL(update_points + update_bank
							+ " WHERE _id = " + squad_id);
				}
				
				// squad_gameweek
				// delete existing squad gameweek records for next gw
				Cursor check = dbGen.rawQuery("SELECT squad_id FROM squad_gameweek WHERE squad_id = " + squad_id
						+ " AND gameweek = " + gameweek, null);
				check.moveToFirst();
				if (check.isAfterLast()) {
					ContentValues insSquadGw = new ContentValues();
					insSquadGw.put(f_squad_id, squad_id);
					insSquadGw.put(f_gameweek, gameweek);
					insSquadGw.put(f_transfers_next, transfers);
					insSquadGw.put(f_points_hit, hits);
					insSquadGw.put(f_points, gw_points);
					if (cup_opp > 0) {
						insSquadGw.put(f_cup_opp, cup_opp);
					}
					insSquadGw.put(f_chip, chip);
					dbGen.insert(t_squad_gameweek, null, insSquadGw);
				} else {
					String cup_opp_update = "";
					if (cup_opp > 0) {
						cup_opp_update = ", cup_opp = " + cup_opp;
					}
					dbGen.execSQL("UPDATE squad_gameweek SET transfers_next = " + transfers
							+ ", points_hit = " + hits + ", points = " + gw_points + ", chip = " + chip
							+ cup_opp_update
							+ " WHERE squad_id = " + squad_id + " AND gameweek = " + gameweek);
				}
				check.close();
				
				Long timeDiff = System.currentTimeMillis() - startTime;
				float timeSecs = (float)timeDiff / 1000;
				App.log("done squad scrape in " + timeSecs + " seconds");
			} else {
				App.log("scrapesquadgameweek: url failed");
				if (con.updating) {
					result.updating = true;
				}
			}
		} catch (IOException e) {
			App.exception(e);
			if (e instanceof FileNotFoundException) {
				if (gameweek == App.cur_gameweek) {
					// squad doesn't exist for current gameweek. this is not right - must be a delayed 
					// deadline or other error
					//
					// mark as error
					App.log("squad did not exist for CURRENT gw - late deadline?");
					result.setError("rival sync failed for current gw");
				} else {
					// squad did not exist for this past gw - avoid failing every time by marking down as 0
					App.log("squad did not exist for past gw; mark as got");
					
					Cursor check = dbGen.rawQuery("SELECT squad_id FROM squad_gameweek WHERE squad_id = " + squad_id
							+ " AND gameweek = " + gameweek, null);
					check.moveToFirst();
					if (check.isAfterLast()) {
						App.log("(ins)");
						ContentValues insSquadGw = new ContentValues();
						insSquadGw.put(f_squad_id, squad_id);
						insSquadGw.put(f_gameweek, gameweek);
						insSquadGw.put(f_not_started, 1);
						dbGen.insert(t_squad_gameweek, null, insSquadGw);
					} else {
						App.log("(upd)");
						dbGen.execSQL("UPDATE squad_gameweek SET not_started = 1"
								+ " WHERE squad_id = " + squad_id + " AND gameweek = " + gameweek);
					}
					check.close();
				}
			}
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
		}
		
		// add vidiprinter/transfer hist for trasnfers
		if (gameweek > 1) {
			if (new_cur_week_squad.size() == SquadUtil.SQUAD_SIZE) {
				if (prev_week_squad.size() == SquadUtil.SQUAD_SIZE) {
					for (int fpl_id : new_cur_week_squad) {
						if (!prev_week_squad.contains(fpl_id)) {
							// get player name
							cur = App.dbGen.rawQuery("SELECT p.name FROM player p, player_season ps"
									+ " WHERE ps.fpl_id = " + fpl_id + " AND ps.season = " + App.season
									+ " AND p._id = ps.player_id", null);
							cur.moveToFirst();
							String player_name = "?";
							if (!cur.isAfterLast()) {
								player_name = cur.getString(0);
							}
							cur.close();
							
							// only add vidiprinter for current week
							if (gameweek == App.cur_gameweek) {
								VidiEntry e = new VidiEntry();
								e.category = Vidiprinter.CAT_TRANSFER;
								e.gameweek = gameweek;
								e.message = squad_name_stored + ": transferred in " + player_name;
								e.player_fpl_id = fpl_id;
								
								Vidiprinter.addVidi(e, result.context);
							}
							
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
					for (int fpl_id : prev_week_squad) {
						if (!new_cur_week_squad.contains(fpl_id)) {
							// get player name
							cur = App.dbGen.rawQuery("SELECT p.name FROM player p, player_season ps"
									+ " WHERE ps.fpl_id = " + fpl_id + " AND ps.season = " + App.season
									+ " AND p._id = ps.player_id", null);
							cur.moveToFirst();
							String player_name = cur.getString(0);
							cur.close();
							
							// only add vidiprinter for current week
							if (gameweek == App.cur_gameweek) {
								VidiEntry e = new VidiEntry();
								e.category = Vidiprinter.CAT_TRANSFER;
								e.gameweek = gameweek;
								e.message = squad_name_stored + ": transferred out " + player_name;
								e.player_fpl_id = fpl_id;
							
								Vidiprinter.addVidi(e, result.context);
							}
							
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
		
		return true;
	}
	
	private static final Pattern pat_gw38_squad_id = Pattern.compile("/entry/([0-9]+)/event-history");
	
	public static void gw38login(ProcRes result) {
		Scanner scan = null;
		UrlObj con = null;
		SQLiteDatabase dbGen = App.dbGen;
		App.log("gw38login");
		
		try {
			dbGen.execSQL(DbGen.TRANSACTION_IMMEDIATE_BEGIN);
			long startTime = System.currentTimeMillis();
			
			// will redirect to gw38 points page for user's team
			con = URLUtil.getUrlStream(ScrapePlayers.URL_PLAYERS, false, URLUtil.AUTH_FULL, null, true, true);
			if (con.ok) {
				scan = new Scanner(con.stream);
				// extract team ID from con.final_url
				App.log("gw38login final url: " + con.final_url);
				Matcher m = pat_gw38_squad_id.matcher(con.final_url);
				int squad_id;
				if (m.find()) {
					squad_id = Integer.parseInt(m.group(1));
					App.log("found squad id: " + squad_id);
				} else {
					App.log("didn't find squad_id");
					try {
						dbGen.execSQL(DbGen.TRANSACTION_END);
					} catch (SQLException e) {
						App.exception(e);
					}
					result.setError("didn't find squad_id");
					return;
				}
				
				// get player name
				if (scan.findWithinHorizon(pat_player_name, 0) == null) {
					App.log(" pat_player_name not found... exiting");
					try {
						dbGen.execSQL(DbGen.TRANSACTION_END);
					} catch (SQLException e) {
						App.exception(e);
					}
					result.setError("didn't find player name");
					return;
				}
				MatchResult mr_playername = scan.match();
				String player_name = mr_playername.group(1).replace(DoTransfersNew.APOST, DoTransfersNew.APOST_REP);
				App.log("player name: " + player_name);
				
				// get squad name
				if (scan.findWithinHorizon(pat_squad_name, 0) == null) {
					App.log(" pat_squad_name not found... exiting");
					try {
						dbGen.execSQL(DbGen.TRANSACTION_END);
					} catch (SQLException e) {
						App.exception(e);
					}
					result.setError("didn't find squad name");
					return;
				}
				MatchResult mr_squadname = scan.match();
				String squad_name = mr_squadname.group(1).replace(DoTransfersNew.APOST, DoTransfersNew.APOST_REP);
				App.log("squad name: " + squad_name);
				
				// store in preference and memory
				Settings.setIntPref(Settings.PREF_FPL_SQUADID, squad_id, result.context);
				App.my_squad_id = squad_id;
				App.my_squad_name = squad_name;
				
				// do actual inserts after squad id is known!
				// first squad... check if exists already
				ContentValues insSquad = new ContentValues();
				insSquad.put("name", DbGen.compress(squad_name));
				insSquad.put("player_name", DbGen.compress(player_name));
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
				
				Long timeDiff = System.currentTimeMillis() - startTime;
				float timeSecs = (float)timeDiff / 1000;
				App.log("done gw38login in " + timeSecs + " seconds");
			} else {
				App.log("gw38login: url failed");
				if (con.updating) {
					result.updating = true;
				}
				try {
					dbGen.execSQL(DbGen.TRANSACTION_END);
				} catch (SQLException e) {
					App.exception(e);
				}
				result.setError("url failed");
			}
		} catch (IOException e) {
			App.exception(e);
			result.setError("gw38 login failed");
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
		}
	}
	
}
