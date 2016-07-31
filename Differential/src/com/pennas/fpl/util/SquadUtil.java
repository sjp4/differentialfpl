package com.pennas.fpl.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import com.pennas.fpl.App;
import com.pennas.fpl.Players;
import com.pennas.fpl.R;
import com.pennas.fpl.Selection;
import com.pennas.fpl.Players.Stat;
import com.pennas.fpl.scrape.DoTransfersNew;
import com.pennas.fpl.scrape.DoTransfersNew.TransferConfirm;
import com.pennas.fpl.scrape.DoTransfersNew.TransferItem;
import com.pennas.fpl.scrape.ScrapeSquadGameweek;
import com.pennas.fpl.ui.WidgetNormal;

import android.database.Cursor;
import android.database.SQLException;
import android.widget.Toast;

public class SquadUtil {
	public static final int SQUAD_SIZE = 15;
	public static final int NUM_SUBS = 3;
	
	public static final int POS_KEEP = 1;
	public static final int POS_DEF = 2;
	public static final int POS_MID = 3;
	public static final int POS_FWD = 4;
	
	public static final int SEL_SELECTED = 0;
	public static final int SEL_GK_NOT_SELECTED = 1;
	public static final int SEL_SUB_1 = 2;
	public static final int SEL_SUB_2 = 3;
	public static final int SEL_SUB_3 = 4;
	
	public static final int[] MIN_SEL_DEFAULT = { 0, 1, 3, 3, 1 };
	public static final int[] MIN_SEL_ALL_OUT_ATTACK = { 0, 1, 2, 3, 1 };
	public static final int[] MAX_SEL = { 0, 1, 5, 5, 3 };
	
	private static int[] get_min_sel(Squad squad) {
		if (squad.gameweek_chip == ScrapeSquadGameweek.c_chip_all_out_attack) {
			return MIN_SEL_ALL_OUT_ATTACK;
		} else {
			return MIN_SEL_DEFAULT;
		}
	}
	
	public static final int CAPTAIN = 1;
	public static final int VICE_CAPTAIN = 2;
	
	/*public static final int[] SHIRT_IMAGES = { 0, R.drawable.shirt_1, R.drawable.shirt_2, 0, 0
		, 0, 0, R.drawable.shirt_7, R.drawable.shirt_8, 0
		, R.drawable.shirt_10, R.drawable.shirt_11, R.drawable.shirt_12, R.drawable.shirt_13, R.drawable.shirt_14
		, R.drawable.shirt_15, R.drawable.shirt_16, R.drawable.shirt_17, R.drawable.shirt_18, 0
		, 0, R.drawable.shirt_21, 0, R.drawable.shirt_23, R.drawable.shirt_24
		, 0, 0, R.drawable.shirt_27, 0, 0, R.drawable.shirt_30
		, R.drawable.shirt_31, R.drawable.shirt_32};*/
	// Needs real shirt assets
	public static final int[] SHIRT_IMAGES = { 0, 0, 0, 0, 0
			, 0, 0, 0, 0, 0
			, 0, 0, 0, 0, 0
			, 0, 0, 0, 0, 0
			, 0, 0, 0, 0, 0
			, 0, 0, 0, 0, 0, 0
			, 0, 0};
	
	public static int getShirtResource(int team_id) {
		return SHIRT_IMAGES[team_id];
	}
	
	public static void arrangeSquad(SquadPlayer[] squad, SquadPlayer[][] grid) {
		int[] pos_sel = { 0, 0, 0, 0, 0, 0 };
		int[][] pos_players = new int[6][5];
		
		// loop players in squad, build position lists/counts
		for (int i=0; i<SQUAD_SIZE; i++) {
			SquadPlayer p = squad[i];
			// increment selection count for position
			if (p.selected == SquadUtil.SEL_SELECTED) {
				pos_players[p.position-1][pos_sel[p.position-1]] = i;
				pos_sel[p.position-1]++;
			} else {
				if (p.position == 1) {
					pos_players[4][pos_sel[4]] = i;
					pos_sel[4]++;
				} else {
					// subs in order according to position
					pos_players[5][p.selected - SEL_SUB_1] = i;
					pos_sel[5]++;
				}
			}
		}
		
		// loop positions, assigning players to grid
		for (int p=0; p<4; p++) {
			switch(pos_sel[p]){ 
				case 1:
					grid[p][0] = null;
					grid[p][1] = null;
					grid[p][2] = squad[pos_players[p][0]];
					grid[p][3] = null;
					grid[p][4] = null;
					break;
				case 2:
					grid[p][0] = null;
					grid[p][1] = squad[pos_players[p][0]];
					grid[p][2] = null;
					grid[p][3] = squad[pos_players[p][1]];
					grid[p][4] = null;
					break;
				case 3:
					grid[p][0] = null;
					grid[p][1] = squad[pos_players[p][0]];
					grid[p][2] = squad[pos_players[p][1]];
					grid[p][3] = squad[pos_players[p][2]];
					grid[p][4] = null;
					break;
				case 4:
					grid[p][0] = squad[pos_players[p][0]];
					grid[p][1] = squad[pos_players[p][1]];
					grid[p][2] = null;
					grid[p][3] = squad[pos_players[p][2]];
					grid[p][4] = squad[pos_players[p][3]];
					break;
				case 5:
					grid[p][0] = squad[pos_players[p][0]];
					grid[p][1] = squad[pos_players[p][1]];
					grid[p][2] = squad[pos_players[p][2]];
					grid[p][3] = squad[pos_players[p][3]];
					grid[p][4] = squad[pos_players[p][4]];
					break;
			}
		}
		
		grid[4][0] = squad[pos_players[4][0]];
		
		grid[4][2] = squad[pos_players[5][0]];
		grid[4][3] = squad[pos_players[5][1]];
		grid[4][4] = squad[pos_players[5][2]];
	}
	
	public static void dumpSquad(SquadPlayer[] squad) {
		for (int i=0; i<SQUAD_SIZE; i++) {
			App.log(squad[i].name + " sel: " + squad[i].selected + " cap: " + squad[i].captain + " pos: " + squad[i].position + " opp: " + squad[i].opp);
		}
	}
	
	private HashMap<Integer, SquadPlayer> gwPlayers;
	private boolean useGwPlayers = false;
	
	public static final int CS_MINS = 60;
	
	private static SquadPlayer loadPlayerBasics(int fpl_id, boolean load_all, int gameweek) {
		SquadPlayer p = new SquadPlayer();
		
		if (load_all) {
			Cursor pCur = App.dbGen.rawQuery("SELECT p.name, p.team_id, p.fpl_yellow_flag, p.fpl_red_flag, p.fpl_news, p.ticker_string"
					+ ", p.code_14, ps.position, ps.price, p._id, p.diff_flag, ps.points FROM player p, player_season ps WHERE p._id = ps.player_id AND ps.fpl_id = " + fpl_id + " AND ps.season = " + App.season, null);
	        pCur.moveToFirst();
	        if (pCur.isAfterLast()){
	        	App.log("player details cursor no rows for fpl id: " + fpl_id);
	        	return null;
	        }
			p.name = pCur.getString(pCur.getColumnIndex("name"));
		    p.team_id = pCur.getInt(pCur.getColumnIndex("team_id"));
		    p.fpl_yellow_flag = (pCur.getInt(pCur.getColumnIndex("fpl_yellow_flag")) != 0);
		    p.fpl_red_flag = (pCur.getInt(pCur.getColumnIndex("fpl_red_flag")) != 0);
		    p.picture_code = pCur.getInt(pCur.getColumnIndex("code_14"));
		    p.position = pCur.getInt(pCur.getColumnIndex("position"));
		    p.price = pCur.getFloat(pCur.getColumnIndex("price")) / 10f;
		    p.player_id = pCur.getInt(pCur.getColumnIndex("_id"));
		    p.diff_flag = (pCur.getInt(pCur.getColumnIndex("diff_flag")) != 0);
		    p.total_score = pCur.getInt(pCur.getColumnIndex("points"));
		    //p.ticker_string = pCur.getString(pCur.getColumnIndex("ticker_string"));
		    byte[] ticker_compressed = pCur.getBlob(pCur.getColumnIndex("ticker_string"));
		    p.ticker_string = DbGen.decompress(ticker_compressed);
		    pCur.close();
		    
		    // stats for selection screen; use last season if GW0
		    int stats_season = App.season;
		    if (App.cur_gameweek < 1) stats_season --;
		    pCur = App.dbGen.rawQuery("SELECT * FROM player_season WHERE player_id = " + p.player_id + " AND season = " + stats_season, null);
	        pCur.moveToFirst();
	        // only load stats if available (won't be for new players during gw0)
	        if (!pCur.isAfterLast()) {
	        	Players.loadStats();
			    p.stat2value = new HashMap<String, String>();
			    for (Stat s : Players.playerstats_array) {
			 		if (s.show_value) {
			 			String val;
				 		if (s.divide > 0) {
				 			float num = pCur.getFloat(pCur.getColumnIndex(s.db_field)) / s.divide;
				 			val = String.valueOf(num);
				 		} else {
				 			val = pCur.getString(pCur.getColumnIndex(s.db_field));
				 		}
			 			p.stat2value.put(s.db_field, val);
			 		}
			 	}
			    p.stat2value.put("points", pCur.getString(pCur.getColumnIndex("points")));
	        }
		    pCur.close();
		} else {
			// don't do extra db work if not needed.... (generally when just calculating squad total score)
			Cursor pCur = App.dbGen.rawQuery("SELECT position, player_id, price FROM player_season WHERE fpl_id = " + fpl_id + " AND season = " + App.season, null);
	        pCur.moveToFirst();
	        if (pCur.isAfterLast()){
	        	App.log("player details cursor (basic) no rows for fpl id: " + fpl_id);
	        	return null;
	        }
	        p.position = pCur.getInt(0);
			p.player_id = pCur.getInt(1);
			p.price = pCur.getFloat(2) / 10f;
		    pCur.close();
		}
		
		p.status_played = false;
		p.status_gw_complete = true;
		p.status_gw_got_all_bonus = true;
		p.status_playing_now = false;
		p.status_any_match_complete = false;
	    
        Cursor pmCur = App.dbGen.rawQuery("SELECT pm.is_home, pm.opp_team_id, pm.total, pm.minutes, f.got_bonus, f.datetime, pm.goals"
            + ", pm.assists, pm.bonus, pm.conceded, pm.yellow, pm.red, pm.pen_sav, pm.pen_miss, pm.saves, pm.own_goals, pm.bps"
            + " FROM player_match pm, fixture f WHERE pm.season = " + App.season
            + " AND pm.player_player_id = " + p.player_id + " AND f._id = pm.fixture_id AND pm.gameweek = " + gameweek
            + " AND f.gameweek = " + gameweek
            + " ORDER BY f.datetime ASC", null);
        pmCur.moveToFirst();
        boolean dgw = (pmCur.getCount() > 1);
        SquadPlayer dgw_match = null;
        if (dgw) {
        	p.dgw_matches = new LinkedList<SquadPlayer>();
        }
        // loop match info
        while (pmCur.isAfterLast() == false) {
        	// stuff not needed by totalising calculator
        	if (load_all) {
        		// home/away
    			String ha;
        		if (pmCur.getInt(pmCur.getColumnIndex("is_home")) == 1) {
	        		p.opp_home = true;
	        		ha = " (h)";
	            } else {
	            	p.opp_home = false;
	            	ha = " (a)";
	            }
	        	// opp name
	        	String opp_name = App.id2team.get(pmCur.getInt(pmCur.getColumnIndex("opp_team_id"))) + ha;
	        	if (p.opp == null) {
	        		p.opp = opp_name;
	        	} else {
	        		p.opp += "\n" + opp_name;
	        	}
	        	
	        	int gw_yellow = pmCur.getInt(pmCur.getColumnIndex("yellow"));
	        	int gw_red = pmCur.getInt(pmCur.getColumnIndex("red"));
	        	int gw_pen_miss = pmCur.getInt(pmCur.getColumnIndex("pen_miss"));
	        	int gw_pen_save = pmCur.getInt(pmCur.getColumnIndex("pen_sav"));
	        	int gw_saves = pmCur.getInt(pmCur.getColumnIndex("saves"));
	        	int gw_own_goals = pmCur.getInt(pmCur.getColumnIndex("own_goals"));
	        	int gw_bps = pmCur.getInt(pmCur.getColumnIndex("bps"));
	        	
	        	p.gw_yellow += gw_yellow;
	        	p.gw_red += gw_red;
	        	p.gw_pen_miss += gw_pen_miss;
	        	p.gw_pen_save += gw_pen_save;
	        	p.gw_saves += gw_saves;
	        	p.gw_own_goals += gw_own_goals;
	        	p.gw_bps += gw_bps;
	        	
	        	if (dgw) {
	        		dgw_match = new SquadPlayer();
	        		dgw_match.name = opp_name;
	        		dgw_match.gw_yellow = gw_yellow;
	        		dgw_match.gw_red = gw_red;
	        		dgw_match.gw_pen_miss = gw_pen_miss;
	        		dgw_match.gw_pen_save = gw_pen_save;
	        		dgw_match.gw_saves = gw_saves;
	        		dgw_match.gw_own_goals = gw_own_goals;
	        		dgw_match.gw_bps = gw_bps;
	        	}
    		}
        	
        	// points
        	int gw_bonus = pmCur.getInt(pmCur.getColumnIndex("bonus"));
        	int gw_goals = pmCur.getInt(pmCur.getColumnIndex("goals"));
        	int gw_assists = pmCur.getInt(pmCur.getColumnIndex("assists"));
        	int gw_score = pmCur.getInt(pmCur.getColumnIndex("total"));
        	p.gw_goals += gw_goals;
        	p.gw_assists += gw_assists;
        	p.gw_score += gw_score;
        	p.gw_bonus += gw_bonus;
        	
        	int minutes = pmCur.getInt(pmCur.getColumnIndex("minutes"));
        	int conc = pmCur.getInt(pmCur.getColumnIndex("conceded"));
        	p.gw_conceded += conc;
        	if ( (conc == 0) && (minutes >= CS_MINS) && (p.position <= SquadUtil.POS_DEF) ) {
        		p.gw_cs++;
        	}
        	
        	// if played any minutes, mark played
        	if (minutes > 0) {
        		p.status_played = true;
        	}
        	p.gw_minutes += minutes;
        	
        	int got_bonus = pmCur.getInt(pmCur.getColumnIndex("got_bonus"));
        	// if match not complete then mark "all matches" incomplete
        	if (got_bonus == 0) {
        		p.status_gw_complete = false;
        		p.status_gw_got_all_bonus = false;
        		// if match is in progress (past start time), then mark that also!
        		long unixTime = App.currentUkTime();
        		if (pmCur.getLong(pmCur.getColumnIndex("datetime")) <= unixTime) {
        			p.status_playing_now = true;
        		} else {
        			p.status_gw_matches_left++;
        		}
        	} else if (got_bonus == 1) {
        		p.status_gw_got_all_bonus = false;
        	} else {
        		// match complete
        		p.status_any_match_complete = true;
        	}
        	
        	if (load_all && dgw) {
        		dgw_match.gw_goals = gw_goals;
        		dgw_match.gw_assists = gw_assists;
        		dgw_match.gw_score = gw_score;
        		dgw_match.gw_conceded = conc;
        		dgw_match.gw_minutes = minutes;
        		dgw_match.gw_bonus = gw_bonus;
        		if (conc > 0) {
        			dgw_match.gw_cs = 1;
        		}
        		if (got_bonus > 0) {
        			dgw_match.status_gw_complete = true;
        			dgw_match.status_played = true;
        			if (got_bonus >= 2) {
        				dgw_match.status_gw_got_all_bonus = true;
        			}
        		}
        		dgw_match.status_playing_now = p.status_playing_now;
        		p.dgw_matches.add(dgw_match);
        	}
        	
            pmCur.moveToNext();
        }
        pmCur.close();
		
		return p;
	}
	
	// only public for Selection to use when transfering in one new player. otherwise, helper for loadSquad
	public SquadPlayer loadPlayer(int gameweek, int selected, int captain, int fpl_id, float bought_price, int i, boolean load_all) {
		SquadPlayer p = null;
		if (useGwPlayers) {
			if (gwPlayers != null) {
				p = gwPlayers.get(fpl_id);
			}
		}
		if (p == null) {
			p = loadPlayerBasics(fpl_id, load_all, gameweek);
			if (useGwPlayers) {
				if (gwPlayers != null) {
					gwPlayers.put(fpl_id, p);
				}
			}
		}
		
		if (p == null) {
			return null;
		}
		
		p.selected = selected;
		p.fpl_id = fpl_id;
		if (captain == CAPTAIN) {
 			p.captain = true;
 			p.vice_captain = false;
 			//App.log(p.fpl_id + " capt");
 		} else if (captain == VICE_CAPTAIN) {
 			p.vice_captain = true;
 			p.captain = false;
 			//App.log(p.fpl_id + " vc");
 		} else {
 			// necessary because of player caching
 			p.captain = false;
 			p.vice_captain = false;
 		}
 		p.i = i;
		// used for actual transfers in Selection
		p.transferred_in = false;
 		
		if (load_all) {
		    p.sell_price = calc_sell_price(bought_price, p.price);
	        p.bought_price = bought_price;
	        p.outgoing_player_sell_price = p.sell_price;
	        //App.log(p.name + " price: " + p.price + " bought_price: " + bought_price + " sell_price: " + p.sell_price);
		}
		
		return p;
	}
	
	public static float calc_sell_price(float bought_price, float cur_price) {
		int price = (int) (cur_price * 10);
	    int bought_price_int = (int) (bought_price * 10);
	    float sell_price;
	    // work out sell price
        if (price < bought_price_int) {
        	sell_price = price;
        } else {
        	int diff = price - bought_price_int;
        	int rise = 0;
        	//App.log("diff " + price + " - " + bought_price_int + " = " + diff);
        	
        	if (diff > 1) {
        		rise = diff / 2;
        		//App.log("rise " + diff + " / 2 = " + rise);
        	}
        	sell_price = (float) (bought_price_int + rise);
        	//App.log("sell price () = " + bought_price_int + " + " + rise + " = " + p.sell_price);
        }
        sell_price /= 10;
        
        return sell_price;
	}
	
	// called from either loader or bg. should not do updates, because loader may be called at same time as bg
	public Squad loadSquad(int p_squad_id, int p_gameweek, boolean p_load_all, boolean do_subs) {
		Squad newSquad = new Squad();
		
		Cursor cur;
		cur = App.dbGen.rawQuery("SELECT _id, name, bank, points, c_points FROM squad WHERE _id = " + p_squad_id, null);
		cur.moveToFirst();
		if (cur.isAfterLast()) {
			cur.close();
			App.log("loadSquad err 1");
			return null;
		}
		newSquad.id = cur.getInt(0);
		newSquad.team_name = DbGen.decompress(cur.getBlob(1));
		newSquad.bank = cur.getFloat(2);
		newSquad.total_points = cur.getInt(3);
		newSquad.c_total_points = cur.getInt(4);
		cur.close();
		
		cur = App.dbGen.rawQuery("SELECT points, points_hit, transfers_next, chip FROM squad_gameweek"
				+ " WHERE gameweek = " + p_gameweek + " AND squad_id = " + p_squad_id, null);
		cur.moveToFirst();
		if (cur.isAfterLast()) {
			cur.close();
			App.log("loadSquad err 2");
			return null;
		}
		newSquad.gameweek_score = cur.getInt(0);
		newSquad.gw_hits = cur.getInt(1);
		newSquad.gw_transfers = cur.getInt(2);
		newSquad.gameweek_chip = cur.getInt(3);
		cur.close();
		
		cur = App.dbGen.rawQuery("SELECT selected, captain, fpl_player_id, bought_price FROM squad_gameweek_player"
				+ " WHERE gameweek = " + p_gameweek + " AND squad_id = " + p_squad_id, null);
		cur.moveToFirst();
		if (cur.isAfterLast()) {
			cur.close();
			App.log("loadSquad err 3");
			return null;
		}
		SquadPlayer[] players = new SquadPlayer[SQUAD_SIZE];
	 	int i = 0;
	 	
	 	float value = 0;
	 	float sell_value = 0;
	 	
	 	// loop to get player info
	 	while (cur.isAfterLast() == false) {
            int fpl_id = cur.getInt(2);
	        float bought_price = cur.getFloat(3);
            int selected = cur.getInt(0);
            int captain = cur.getInt(1);
            
            players[i] = loadPlayer(p_gameweek, selected, captain, fpl_id, bought_price, i, p_load_all);
            if (players[i] == null) {
            	App.log("null player, returning null squad for " + p_squad_id);
            	return null;
            }
            
            value += players[i].price;
            sell_value += players[i].sell_price;
            
            cur.moveToNext();
            i++;
        }
        cur.close();
        
        // squad didn't have enough players in it
        if (i != SQUAD_SIZE) {
        	App.log(i + " players in squad " + p_squad_id + " is wrong; returning null");
        	return null;
        }
        
        newSquad.value = value;
        newSquad.bankvalue = newSquad.bank + newSquad.value;
        newSquad.sell_value = sell_value;
        newSquad.players = players;
        
        // do automatic subs (sets player selection
        if (do_subs) {
        	processSubsStatusScore(newSquad, p_gameweek);
        }
        
        return newSquad;
	}
	
	// part of loadSquad. should not do updates
	private static void processSubsStatusScore(Squad squad, int p_gameweek) {
		// setup possible subs
		SquadPlayer gk_sub = null;
		SquadPlayer[] outfield_sub = new SquadPlayer[NUM_SUBS];
		ArrayList<SquadPlayer> to_be_subbed = new ArrayList<SquadPlayer>();
		int[] sel = { 0, 0, 0, 0, 0 };
		
		SquadPlayer capt = null;
		SquadPlayer vc = null;
		
		for (int i=0; i<SQUAD_SIZE; i++) {
			SquadPlayer p = squad.players[i];
			if (p != null) {
				if (p.selected == SEL_SELECTED) {
					if (p.status_gw_complete && !p.status_played) {
						to_be_subbed.add(p);
					}
					sel[p.position]++;
				} else if (p.selected >= SEL_SUB_1) {
					if (p.status_gw_complete && !p.status_played) {
						outfield_sub[p.selected - SEL_SUB_1] = null;
					} else {
						outfield_sub[p.selected - SEL_SUB_1] = p;
					}
				} else if (p.selected == SEL_GK_NOT_SELECTED) {
					if (p.status_gw_complete && !p.status_played) {
						gk_sub = null;
					} else {
						gk_sub = p;
					}
				}
				
				if (p.captain) capt = p;
				if (p.vice_captain) vc = p;
			}
		}
		
		boolean bench_boost = squad.gameweek_chip == ScrapeSquadGameweek.c_chip_bench_boost;
		
		// go throuch players who can be subbed out
		if (!bench_boost) {
			for (SquadPlayer p : to_be_subbed) {
				// keeper
				if (p != null) {
					if (p.position == POS_KEEP) {
						if (gk_sub != null) {
							doAutoSub(p, gk_sub, squad, p_gameweek);
						}
					// outfield
					} else {
						// players on bench
						innerloop:
						for (int i=0; i<NUM_SUBS; i++) {
							// if outfield player can come on
							if (outfield_sub[i] != null) {
								// check if this obeys selection rules
								boolean doSub = false;
								int out_pos = p.position;
								int in_pos = outfield_sub[i].position;
								if (out_pos == in_pos) {
									doSub = true;
								} else if ( ((sel[out_pos] - 1) >= get_min_sel(squad)[out_pos])
									     && ((sel[in_pos] + 1)  <= MAX_SEL[in_pos]) ) {
									doSub = true;
								}
								if (doSub) {
									doAutoSub(p, outfield_sub[i], squad, p_gameweek);
									sel[out_pos]--;
									sel[in_pos]++;
									// this sub no longer eligible
									outfield_sub[i] = null;
									break innerloop;
								}
							}
						} // for
					} // keeper/else
				} // null
			}
		}
		
		// squad status (squad gw complete?) + score; init
		squad.c_gameweek_complete = true;
		squad.c_gameweek_score = 0;
		squad.c_gameweek_bench_score = 0;
		squad.c_gameweek_playing = 0;
		squad.c_gameweek_to_play = 0;
		boolean capt_playing = false;
		boolean capt_to_play = false;
		
		// captain swap
		if ( (capt != null) && (vc != null) ) {
			// captain didn't play
			if (capt.status_gw_complete && !capt.status_played) {
				// vice captain has played
				if (vc.status_played) {
					capt.captain = false;
					vc.captain = true;
					vc.vice_captain = false;
					
					capt.status_vc_swap = true;
					vc.status_vc_swap = true;
					//App.log("captain swap " + capt.fpl_id + "(" + capt.gw_score + ") -> " + vc.fpl_id + "(" + vc.gw_score + ")");
				} else if (!vc.status_gw_complete) {
					capt_to_play = true;
				}
			}
		}
			
		// populate status/to_play etc
		for (int i=0; i<SQUAD_SIZE; i++) {
			SquadPlayer p = squad.players[i];
			if (p != null) {
				if ( (p.selected == SEL_SELECTED || bench_boost) && (!p.status_gw_complete) ) {
					squad.c_gameweek_complete = false;
				}
				if (p.selected == SEL_SELECTED || bench_boost) {
					squad.c_gameweek_score += p.gw_score;
					//App.log(p.fpl_id + ": " + p.gw_score);
					if (p.captain) {
						squad.c_gameweek_score += p.gw_score;
						if (squad.gameweek_chip == ScrapeSquadGameweek.c_chip_triple_captain) {
							squad.c_gameweek_score += p.gw_score;
						}
						//App.log("(c)");
					}
					
					if (p.status_playing_now) {
						squad.c_gameweek_playing++;
						if (p.captain) {
							capt_playing = true;
						}
					}
					if (!p.status_gw_complete) {
						squad.c_gameweek_to_play += p.status_gw_matches_left;
						if (p.captain && (p.status_gw_matches_left > 0) ) {
							capt_to_play = true;
						}
					}
				} else {
					squad.c_gameweek_bench_score += p.gw_score;
				}
			}
		}
		
		if (capt_playing) {
			squad.c_gameweek_playing = -squad.c_gameweek_playing;
		}
		if (capt_to_play) {
			squad.c_gameweek_to_play = -squad.c_gameweek_to_play;
		}
		
		// calc new total score: this is store total from fpl, minus this gw score from fpl,
		// then add new calculated gw score
		if (p_gameweek == App.cur_gameweek) {
			App.log("total: " + squad.total_points + " - " + squad.gameweek_score + " + " + squad.c_gameweek_score);
			squad.total_points = squad.total_points - squad.gameweek_score + squad.c_gameweek_score;
		}
	}
	
	// called as part of standard loadSquad. should not do updates
	private static void doAutoSub(SquadPlayer out, SquadPlayer in, Squad squad, int p_gameweek) {
		out.status_auto_subbed_out = true;
		// takes subs place on bench
		out.selected = in.selected;
		in.status_auto_subbed_in = true;
		in.selected = SEL_SELECTED;
		App.log("autosub: " + out.name + " (" + out.fpl_id + ") " + " -> " + in.name + " (" + in.fpl_id + ")");
	}
	
	// called from various procs to update rival points after changes. always in bg thread, so can do updates
	public void updateSquadGameweekScores(int gameweek, boolean transactionRequired) {
		String proc_name = "updateSquadGameweekScores gw " + gameweek;
		App.log("starting " + proc_name);
		long startTime = System.currentTimeMillis();
		//Debug.startMethodTracing("updsqgwscores");
		
		if (transactionRequired) {
			App.dbGen.execSQL(DbGen.TRANSACTION_IMMEDIATE_BEGIN);
		}
		
		useGwPlayers = true;
		gwPlayers = new HashMap<Integer, SquadPlayer>();
		
		// first, reset c_score for all squads, so that old calculated score can't have any effect
		// why would we be doing this? for every gw?
		//App.dbGen.execSQL("UPDATE squad SET c_points = 0");
		
		Cursor cur = App.dbGen.rawQuery("SELECT DISTINCT squad_id FROM squad_gameweek_player"
				+ " WHERE gameweek = " + gameweek, null);
		cur.moveToFirst();
		while(!cur.isAfterLast()) {
			int squad_id = cur.getInt(0);
			Squad squad = loadSquad(squad_id, gameweek, false, true);
			if (squad == null) {
				App.log("squad " + squad_id + " is null.. not processing");
			} else {
				// update db with squad score + status using replace
				int complete = 0;
				if (squad.c_gameweek_complete) complete = 1;
				
				int goals = 0;
				int assists = 0;
				int cs = 0;
				int bonus = 0;
				boolean bench_boost = squad.gameweek_chip == ScrapeSquadGameweek.c_chip_bench_boost;
				for (SquadPlayer p : squad.players) {
					if (p.selected == SquadUtil.SEL_SELECTED || bench_boost) {
						goals += p.gw_goals;
						assists += p.gw_assists;
						cs += p.gw_cs;
						bonus += p.gw_bonus;
					}
					
					if (p.status_vc_swap) {
						// update db status fields for c/vc to note captain swap
						App.dbGen.execSQL("UPDATE squad_gameweek_player SET vc = 1"
								+ " WHERE squad_id = " + squad.id
								+ " AND   gameweek = " + gameweek
								+ " AND   fpl_player_id = " + p.fpl_id
								+ " AND   vc IS NULL");
					}
					
					if (p.status_auto_subbed_in || p.status_auto_subbed_out) {
						// update db status fields to note autosub
						App.dbGen.execSQL("UPDATE squad_gameweek_player SET autosub = 1"
								+ " WHERE squad_id = " + squad.id
								+ " AND   gameweek = " + gameweek
								+ " AND   fpl_player_id = " + p.fpl_id
								+ " AND   autosub IS NULL");
					}
				}
				
				App.log("squad " + squad_id + " gameweek score: " + squad.c_gameweek_score);
				App.dbGen.execSQL("UPDATE squad_gameweek SET c_points = " + squad.c_gameweek_score + ", c_bench = "
					+ squad.c_gameweek_bench_score + ", c_complete = " + complete + ", c_p_playing = " + squad.c_gameweek_playing
					+ ", c_p_to_play = " + squad.c_gameweek_to_play + ", c_goals = " + goals + ", c_assists = " + assists
					+ ", c_cs = " + cs + ", c_bonus = " + bonus
					+ " WHERE squad_id = " + squad_id
					+ " AND gameweek = " + gameweek
					/*
					// save unnecessary updates - doesn't work though... because of null != value comparison?
					+ " AND (c_points != " + squad.c_gameweek_score + " OR c_bench != "
					+ squad.c_gameweek_bench_score + " OR c_complete != " + complete + " OR c_p_playing != " + squad.c_gameweek_playing
					+ " OR c_p_to_play != " + squad.c_gameweek_to_play + " OR c_goals != " + goals + " OR c_assists != " + assists
					+ " OR c_cs != " + cs + ")"
					*/
					);
				
				if (gameweek == App.cur_gameweek) {
					//App.log("zz total score: " + squad.total_points + " value: " + squad.value);
					App.dbGen.execSQL("UPDATE squad SET c_points = " + squad.total_points
							+ ", value = " + squad.value + ", bankvalue = " + squad.bankvalue
							+ " WHERE _id = " + squad_id
							+ " AND ( (c_points IS NULL) OR (c_points != " + squad.total_points
									+ ") OR (value != " + squad.value + ") OR (bankvalue != " + squad.bankvalue + ")"
									+ " OR (value IS NULL) OR (bankvalue IS NULL) )"); 
				}
				
				// if this is my squad, update global score and possibly widget
				if ( (squad_id == App.my_squad_id) && (gameweek == App.cur_gameweek) ) {
					App.gw_score.assists = assists;
					App.gw_score.clean_sheets = cs;
					App.gw_score.complete = squad.c_gameweek_complete;
					App.gw_score.goals = goals;
					App.gw_score.p_playing = squad.c_gameweek_playing;
					App.gw_score.p_to_play = squad.c_gameweek_to_play;
					App.gw_score.points_hit = squad.gw_hits;
					App.gw_score.bonus = bonus;
					boolean updateWidget = false;
					if (App.gw_score.points != squad.c_gameweek_score) {
						updateWidget = true;
					}
					App.gw_score.points = squad.c_gameweek_score;
					if (updateWidget) {
						WidgetNormal.setSquadScore();
					}
					App.total_score = squad.total_points;
				}
				
				if (gameweek == App.cur_gameweek) {
					// update minileague totals (some might have different starting week)
					Cursor curML = App.dbGen.rawQuery("SELECT points, minileague_id, c_points, current_gw_pts FROM minileague_team"
							+ " WHERE squad_id = " + squad_id, null);
					curML.moveToFirst();
					while (!curML.isAfterLast()) {
						int ml_points = curML.getInt(0);
						int minileague_id = curML.getInt(1);
						int old_c_ml_points = curML.getInt(2);
						int ml_gw_pts = curML.getInt(3);
						int c_ml_points = ml_points - ml_gw_pts + squad.c_gameweek_score;
						//App.log("c_ml_points: " + ml_points + " - " + squad.gameweek_score + " + "
						//		+ squad.c_gameweek_score + " = " + c_ml_points);
						
						if ( (c_ml_points != ml_points) || (c_ml_points != old_c_ml_points) ) {
							App.dbGen.execSQL("UPDATE minileague_team SET c_points = " + c_ml_points
									+ " WHERE minileague_id = " + minileague_id + " AND squad_id = " + squad_id
									+ " AND (c_points IS NULL OR c_points != " + c_ml_points + ")");
						}
						
						curML.moveToNext();
					}
					curML.close();
				}
			}
			
			cur.moveToNext();
		}
		                       
		cur.close();
		
		if (transactionRequired) {
			try {
				App.dbGen.execSQL(DbGen.TRANSACTION_END);
			} catch (SQLException e) {
				App.exception(e);
			} catch (IllegalStateException e) {
				App.exception(e);
			}
		}
		
		useGwPlayers = false;
		gwPlayers = null;
		//Debug.stopMethodTracing();
		
		Long timeDiff = System.currentTimeMillis() - startTime;
		float timeSecs = (float)timeDiff / 1000;
		App.log("done " + proc_name + " in " + timeSecs + " seconds");
	}
	
	public static class SquadPlayer {
		public int fpl_id;
		public int player_id;
		public int position;
		public int selected;
		public boolean captain;
		public boolean vice_captain;
		public String name;
		public int team_id;
		public boolean fpl_yellow_flag;
		public boolean fpl_red_flag;
		public boolean diff_flag;
		public int picture_code;
		public String opp;
		public boolean opp_home;
		public float price;
		public float sell_price;
		public float bought_price;
		public float outgoing_player_sell_price;
		public int i;
		public boolean canSelect;
		public boolean transferred_in;
		
		public int gw_score;
		public int total_score;
		public String display_stat;
		
		public int     status_gw_matches_left;
		public boolean status_gw_complete;
		public boolean status_gw_got_all_bonus;
		public boolean status_playing_now;
		public boolean status_any_match_complete;
		public boolean status_played;
		
		public boolean status_auto_subbed_in;
		public boolean status_auto_subbed_out;
		public boolean status_vc_swap;
		
		public int gw_minutes;
		public int gw_goals;
		public int gw_assists;
		public int gw_bonus;
		public int gw_conceded;
		public int gw_yellow;
		public int gw_red;
		public int gw_pen_save;
		public int gw_pen_miss;
		public int gw_saves;
		public int gw_own_goals;
		public int gw_cs;
		public int gw_bps;
		
		public LinkedList<SquadPlayer> dgw_matches;
		public HashMap<String, String> stat2value;
		public String ticker_string;
	}
	
	public static class Squad {
		public int id;
		public float bank;
		public float value;
		public float bankvalue;
		public float sell_value;
		public String team_name;
		public SquadPlayer[] players;
		
		public int total_points, c_total_points;
		
		public int gameweek_score;
		public int c_gameweek_score;
		public int c_gameweek_bench_score;
		
		public boolean c_gameweek_complete;
		public int c_gameweek_to_play;
		public int c_gameweek_playing;
		
		public int gw_transfers;
		public int gw_hits;
		public int gameweek_chip;
	}
	
	// async task to load current squad IDs according to FPL, and check if any transfers are required
	public static class TransferCheckSquad extends FPLAsyncTask<Squad, Void, com.pennas.fpl.scrape.DoTransfersNew.TransferConfirm> {
        private boolean doneTransfers;
        
		public TransferCheckSquad(boolean doneTransfersP) {
        	doneTransfers = doneTransfersP;
        }
		protected com.pennas.fpl.scrape.DoTransfersNew.TransferConfirm doInBackground(Squad... params) {
			//DoTransfers sums = new DoTransfers();
			DoTransfersNew sums = new DoTransfersNew();
			TransferConfirm tc = sums.initTransfers(params[0]);
			tc.squad = params[0];
			return tc;
        }
		protected void onPreExecute() {
			if (doneTransfers) {
				FPLActivity.progress_text = "Re-checking transfers..";
			} else {
				FPLActivity.progress_text = "Checking required transfers..";
			}
			if (Selection.currentSelection != null) {
				Selection.currentSelection.showDialog(FPLActivity.DIALOG_PROGRESS_NEW);
			}
		}
        protected void onPostExecute(com.pennas.fpl.scrape.DoTransfersNew.TransferConfirm result) {
        	if (Selection.currentSelection != null) {
				try {
					Selection.currentSelection.dismissDialog(FPLActivity.DIALOG_PROGRESS_NEW);
				} catch (IllegalArgumentException e) {
					App.exception(e);
				}
        	}
        	if (result.doTransfers) {
	        	// show transfer confirmation dialog for user
        		if (Selection.currentSelection != null) {
	        		Selection.currentSelection.transfers = result;
	        		Selection.currentSelection.showFPLDialog(Selection.RESPONSE_DIALOG);
        		}
        	} else if (result.error) {
        		// report error to user
        		if (Selection.currentSelection != null) {
	        		App.log("Transfer error: " + result.fplMessage);
	        		Toast.makeText(Selection.currentSelection, "Error: " + result.fplMessage, Toast.LENGTH_LONG).show();
	        		Selection.currentSelection.confirming = false;
        		}
        	} else {
        		// no transfers required: go to selection
        		// start selection update
        		String message = "No transfers required..";
        		if (doneTransfers) {
        			message = "Transfers Confirmed..";
        		}
				new SelectionTask(message).fplExecute(result.selectPost, result.token);
        	}
        }
    }
	
	// async task to confirm transfers
	public static class TransferConfirmTask extends FPLAsyncTask<TransferConfirm, Void, TransferConfirm> {

		protected TransferConfirm doInBackground(TransferConfirm... params) {
			DoTransfersNew sums = new DoTransfersNew();
			return sums.confirmTransfers(params[0]);
        }
		protected void onPreExecute() {
			FPLActivity.progress_text = "Confirming transfers..";
			Selection.currentSelection.showDialog(FPLActivity.DIALOG_PROGRESS_NEW);
		}
        protected void onPostExecute(TransferConfirm result) {
        	Selection.currentSelection.dismissDialog(FPLActivity.DIALOG_PROGRESS_NEW);
        	if (result.doTransfers) {
	        	App.log("transfer confirmed");
	        	Iterator<TransferItem> iter = result.transfers.iterator();
				while (iter.hasNext()) {
					TransferItem ti = iter.next();
					// update AppClass
	    			App.mySquadNext.remove(ti.out);
	    	    	App.mySquadNext.add(ti.in);
				}
				// start selection update
				/*String message;
				if (result.transfers.size() > 1) {
					message = "Transfers confirmed";
				} else {
					message = "Transfer confirmed";
				}*/
				//new SelectionTask(parent, message).fplExecute(result.selectPost, null);
				new TransferCheckSquad(true).fplExecute(result.squad);
        	} else {
        		App.log("transfer failed");
        		Toast.makeText(Selection.currentSelection, "Error: " + result.fplMessage, Toast.LENGTH_LONG).show();
        		Selection.currentSelection.confirming = false;
        	}
        }
    }
	
	// async task set selection
	public static class SelectionTask extends FPLAsyncTask<String, Void, String> {
        String message;
        
		public SelectionTask(String m) {
        	message = m;
        }
		protected String doInBackground(String... params) {
			DoTransfersNew sums = new DoTransfersNew();
			if (Selection.currentSelection == null) {
				return null;
			}
			return sums.sendSelection(Selection.currentSelection.squad, params[0], params[1]);
        }
		protected void onPreExecute() {
			FPLActivity.progress_text = message + "...Sending selection";
			if (Selection.currentSelection != null) {
				Selection.currentSelection.showFPLDialog(FPLActivity.DIALOG_PROGRESS_NEW);
			}
		}
        protected void onPostExecute(String result) {
        	if (Selection.currentSelection != null) {	
	        	Selection.currentSelection.dismissDialog(FPLActivity.DIALOG_PROGRESS_NEW);
	        	if (result == null) {
		        	App.log("Selection confirmed");
		        	Toast.makeText(Selection.currentSelection, "Selection confirmed", Toast.LENGTH_LONG).show();
	        	} else {
	        		App.log("Selection confirmation failed: " + result);
		        	Toast.makeText(Selection.currentSelection, "Selection confirmation failed: " + result, Toast.LENGTH_LONG).show();
	        	}
	        	Selection.currentSelection.confirming = false;
        	}
        }
    }
	
	public static class GwPoints {
		public int points;
		public int points_hit;
		public int p_playing;
		public int p_to_play;
		public boolean complete;
		public int goals;
		public int assists;
		public int clean_sheets;
		public int bonus;
		public String points_string;
	}
	
}
