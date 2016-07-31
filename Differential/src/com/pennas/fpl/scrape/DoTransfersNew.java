package com.pennas.fpl.scrape;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pennas.fpl.App;
import com.pennas.fpl.util.SquadUtil;
import com.pennas.fpl.util.SquadUtil.Squad;
import com.pennas.fpl.util.SquadUtil.SquadPlayer;
import com.pennas.fpl.util.URLUtil;
import com.pennas.fpl.util.URLUtil.UrlObj;

public class DoTransfersNew {
	
	private static final String URL_GET_TRANSFER_IDS = "http://fantasy.premierleague.com/transfers/";
	private static final String URL_FPL_CHECK = "http://fantasy.premierleague.com/transfers/confirm/";
	private static final String URL_FPL_CONFIRM = "http://fantasy.premierleague.com/transfers/confirm/";
	private static final String URL_SELECTION_POST = "http://fantasy.premierleague.com/my-team/";
	
	private int[] players_old_fpl_id = new int[SquadUtil.SQUAD_SIZE];
	private int[] players_old_out_price = new int[SquadUtil.SQUAD_SIZE];
	private int season = App.season;
	private int squad_id = App.my_squad_id;
	private int gameweek = App.next_gameweek;
	private SQLiteDatabase dbGen = App.dbGen;
	
	// do transfers, up to stage of confirmation
	// - return FPLs own confirmation text for user to approve.
	// - return null if no transfers required
	// - squad param is NEW SQUAD
	public TransferConfirm initTransfers(Squad squad) {
		TransferConfirm res = new TransferConfirm();
		res.doTransfers = false;
		res.error = true;
		
		res.token = getSquadTransferIDs();
		
		if (res.token == null) {
			App.log("couldn't get squad transfer IDs");
			res.fplMessage = "Failed getting current squad from FPL";
			return res;
		}
		
		ArrayList<TransferPlayer> p_out = new ArrayList<TransferPlayer>();
		ArrayList<TransferPlayer> p_in = new ArrayList<TransferPlayer>();
		int[] pos_out_count = new int[5];
		int[] pos_in_count = new int[5];
		for (int i=1; i<=4; i++) {
			pos_out_count[i] = 0;
			pos_in_count[i] = 0;
		}
		
		// which players need transferring in
		// - loop through NEW squad to see who is not in OLD squad
		for (int i=0; i<SquadUtil.SQUAD_SIZE; i++) {
			boolean transfer_in = true;
			SquadPlayer p = squad.players[i];
			// check if player is in FPLs squad list
			innerloop:
			for (int old=0; old<SquadUtil.SQUAD_SIZE; old++) {
				if (players_old_fpl_id[old] == p.fpl_id) {
					// player in FPL list, no transfer required
					transfer_in = false;
					break innerloop;
				}
			}
			if (transfer_in) {
				TransferPlayer tp = new TransferPlayer();
				tp.fpl_id = p.fpl_id;
				tp.position = p.position;
				p_in.add(tp);
				pos_in_count[p.position]++;
				App.log("in: " + tp.fpl_id + "(" + tp.position + ")");
			}
		}
		
		// which players need transferring out
		// - loop through OLD squad (from FPL) and check which are not in our NEW squad
		for (int old=0; old<SquadUtil.SQUAD_SIZE; old++) {
			boolean transfer_out = true;
			int cur = players_old_fpl_id[old];
			// loop NEW
			innerloop:
			for (int i=0; i<SquadUtil.SQUAD_SIZE; i++) {
				if (squad.players[i].fpl_id == cur) {
					// player in new list, no transfer required
					transfer_out = false;
					break innerloop;
				}
			}
			if (transfer_out) {
				TransferPlayer tp = new TransferPlayer();
				tp.fpl_id = cur;
				tp.position = getPosition(cur);
				
				// if this player is also in the "in" list, remove from that and don't add to this
				if (p_in.contains(tp)) {
					p_in.remove(tp);
					pos_in_count[tp.position]--;
					App.log("WARNING removed from in+out: " + tp.fpl_id + "(" + tp.position + ")");
				// normal: add to "out" list
				} else {
					p_out.add(tp);
					pos_out_count[tp.position]++;
					App.log("out: " + tp.fpl_id + "(" + tp.position + ")");
				}
			}
		}
		
		// selection string. do here where element ids are available
		StringBuffer post_select = new StringBuffer();
		post_select.append("csrfmiddlewaretoken=" + res.token);
		post_select.append("&pick_formset-TOTAL_FORMS=15");
		post_select.append("&pick_formset-INITIAL_FORMS=15");
		post_select.append("&pick_formset-MAX_NUM_FORMS=15");
		// build player selection grid
		int[] pos_sel = { 0, 0, 0, 0, 0, 0 };
		SquadPlayer[][] pos_players = new SquadPlayer[6][5];
		// loop players in squad, build position lists/counts
		for (int i=0; i<SquadUtil.SQUAD_SIZE; i++) {
			SquadPlayer p = squad.players[i];
			// increment selection count for position
			if (p.selected == SquadUtil.SEL_SELECTED) {
				pos_players[p.position-1][pos_sel[p.position-1]] = p;
				pos_sel[p.position-1]++;
			} else {
				if (p.position == 1) {
					pos_players[4][pos_sel[4]] = p;
					pos_sel[4]++;
				} else {
					// subs in order according to position
					pos_players[5][p.selected - SquadUtil.SEL_SUB_1] = p;
					pos_sel[5]++;
				}
			}
		}
		// loop squad grid to build post string
		int squad_pos = 1;
		int capt_i = 0;
		int v_capt_i = 0;
		for (int p_pos=0; p_pos<pos_sel.length; p_pos++) {
			for (int i=0; i<pos_sel[p_pos]; i++) {
				SquadPlayer p = pos_players[p_pos][i];
				String capt = "False";
				String vc = "False";
				if (p.captain) {
					capt = "True";
					capt_i = squad_pos;
				}
				if (p.vice_captain) {
					vc = "True";
					v_capt_i = squad_pos;
				}
				
				int formset = squad_pos - 1;
				post_select.append("&pick_formset-" + formset + "-is_captain=" + capt);
				post_select.append("&pick_formset-" + formset + "-is_vice_captain=" + vc);
				post_select.append("&pick_formset-" + formset + "-position=" + squad_pos);
				post_select.append("&pick_formset-" + formset + "-id=" + fpl_id2element.get(p.fpl_id));
				
				squad_pos++;
			}
		}
		// and finally
		if ( (capt_i == 0) || (v_capt_i == 0) ) {
			App.log("no capt or vc error");
			res.error = true;
			return res;
		}
		post_select.append("&ismCptSelect=" + capt_i);
		post_select.append("&ismVCptSelect=" + v_capt_i);
		
		//App.log("select post: " + post_select);
		res.selectPost = post_select.toString();
		
		if ( (p_out.size() == 0) && (p_out.size() == 0) ) {
			res.error = false;
			App.log("no transfers");
			return res;
		}
		
		// sanity check on transfers in/out
		// - check each position has equal in/out
		for (int i=1; i<=4; i++) {
			if (pos_out_count[i] != pos_in_count[i]) {
				App.log("transfer in/out mismatch error position " + i);
				res.fplMessage = "Error computing transfers";
				return res;
			}
		}
		
		// everything looks sane
		// new list for new squad in correct id order etc
		int players_new[] = new int[SquadUtil.SQUAD_SIZE+1];
		System.arraycopy(players_old_fpl_id, 0, players_new, 0, SquadUtil.SQUAD_SIZE);
		
		ArrayList<TransferItem> transfers = new ArrayList<TransferItem>();
		
		// loop through players going out
		while(!p_out.isEmpty()) {
			TransferPlayer out = p_out.get(0);
			// find in old team
			for (int old=0; old<SquadUtil.SQUAD_SIZE; old++) {
				if (players_old_fpl_id[old] == out.fpl_id) {
					// remove from out list
					p_out.remove(0);
					// find in player in same position
					Iterator<TransferPlayer> iter = p_in.iterator();
					innerloop:
					while(iter.hasNext()) {
						TransferPlayer in = iter.next();
						if (in.position == out.position) {
							// remove in player
							iter.remove();
							// set in id
							players_new[old] = in.fpl_id;
							// add to log
							TransferItem ti = new TransferItem();
							ti.out = out.fpl_id;
							ti.in = in.fpl_id;
							transfers.add(ti);
							break innerloop;
						}
					}
				}
			}
		}
		
		if (p_in.size() > 0) {
			App.log("left in ERROR");
		}
		if (p_out.size() > 0) {
			App.log("left out ERROR");
		}
		
		// load FPL confirmation page for these transfers
		StringBuffer post_check = new StringBuffer();
		StringBuffer post_confirm = new StringBuffer();
		post_check.append("csrfmiddlewaretoken=" + res.token);
		post_check.append("&transfers_form-event_id=" + gameweek);
		post_check.append("&transfers_form-confirmed=True");
		post_check.append("&transfers_form-wildcard=");
		post_check.append("&pick_formset-TOTAL_FORMS=15");
		post_check.append("&pick_formset-INITIAL_FORMS=15");
		post_check.append("&pick_formset-MAX_NUM_FORMS=15");
		for (int i=0; i<SquadUtil.SQUAD_SIZE; i++) {
			for (int pi=0; pi<SquadUtil.SQUAD_SIZE; pi++) {
				SquadPlayer p_new = squad.players[pi];
				if (p_new.fpl_id == players_new[i]) {
					post_check.append("&pick_formset-" + i + "-element=" + players_new[i]);
					post_check.append("&pick_formset-" + i + "-purchase_price=");
					if (players_new[i] != players_old_fpl_id[i]) {
						int purchase_price = (int) (p_new.price * 10);
						post_check.append(purchase_price);
					}
					post_check.append("&pick_formset-" + i + "-selling_price=" + players_old_out_price[i]);
					//int element_id = i + min_element_id;
					int element_id = fpl_id2element.get(players_old_fpl_id[i]);
					post_check.append("&pick_formset-" + i + "-id=" + element_id);
				}
			}
		}
		
		post_confirm.append(post_check);
		post_confirm.append("&confirm_transfers=1");
		
		App.log("new");
		for (int i=0; i<SquadUtil.SQUAD_SIZE; i++) {
			App.log(i + ": " + players_new[i]);
		}
		
		//App.log("final verification post: " + post_check);
		
		res.fplMessage = loadFplConfirmation(post_check.toString(), res.token);
		if (res.fplMessage == null) {
			App.log("no FPL message");
			return res;
		}
		
		res.confirmPost = post_confirm.toString();
		res.doTransfers = true;
		res.error = false;
		res.transfers = transfers;
		
		return res;
	}
	
	private static final String f_fpl_player_id = "fpl_player_id";
	
	public TransferConfirm confirmTransfers(TransferConfirm transfers) {
		UrlObj con = null;
		String proc_name = "confirm transfers";
		boolean ok = false;
		
		App.log("starting " + proc_name + " get");
		long startTime = System.currentTimeMillis();
		
		try {
			con = URLUtil.getUrlStream(URL_FPL_CONFIRM, false, URLUtil.AUTH_FULL, transfers.confirmPost, transfers.token, true, false, false, false);
			if (con.ok) {
				App.log("final url: " + con.final_url);
				ok = true;
			} else {
				App.log(proc_name + ": url failed");
				if (con.updating) {
					transfers.fplMessage = "the game is updating";
				} else {
					transfers.fplMessage = "url failed";
				}
			}
		} catch (IOException e) {
			App.exception(e);
			transfers.fplMessage = e.toString();
			ok = false;
		} finally {
			try {
				if (con!=null && con.stream != null) con.stream.close();
			} catch (Exception e) {
				App.exception(e);
			}
			if (con != null) con.closeCon();
		}
		
		Long timeDiff = System.currentTimeMillis() - startTime;
		float timeSecs = (float)timeDiff / 1000;
		App.log("done " + proc_name + " in " + timeSecs + " seconds");
		
		if (ok) {
			transfers.error = false;
			transfers.doTransfers = true;
			
			ContentValues updateVals = new ContentValues();
			final String t_squad_gameweek_player = "squad_gameweek_player";
			final String t_transfer = "transfer";
			final String f_squad_id = "squad_id";
			final String f_gameweek = "gameweek";
			final String f_player_fpl_id = "player_fpl_id";
			final String f_t_in = "t_in";
			final String t_where_sgp = "squad_id = " + squad_id + " AND gameweek = " + gameweek + " AND fpl_player_id = ?";
			String[] t_wArgs = { "0" };
			
			// update database
			Iterator<TransferItem> iter = transfers.transfers.iterator();
			while (iter.hasNext()) {
				TransferItem ti = iter.next();
				t_wArgs[0] = String.valueOf(ti.out);
				
				// write to squad
				updateVals.put(f_fpl_player_id, ti.in);
				App.log("transfer db: " + ti.out + " -> " + ti.in);
				dbGen.update(t_squad_gameweek_player, updateVals, t_where_sgp, t_wArgs);
				
				// write out transfer
				ContentValues ins = new ContentValues();
				ins.put(f_squad_id, squad_id);
				ins.put(f_gameweek, gameweek);
				ins.put(f_player_fpl_id, ti.out);
				App.dbGen.replace(t_transfer, null, ins);
				
				// re-use cv for in transfer
				ins.put(f_player_fpl_id, ti.in);
				ins.put(f_t_in, 1);
				App.dbGen.replace(t_transfer, null, ins);
			}
		} else {
			transfers.error = true;
			transfers.doTransfers = false;
		}
		
		return transfers;
	}
	
	private static final String f_selected = "selected";
	private static final String f_captain = "captain";
	
	// send selection to FPL
	// return null if success, message if failed
	public String sendSelection(Squad squad, String post, String token) {
		String proc_name = "fpl send selection";
		App.log("starting " + proc_name + " get");
		long startTime = System.currentTimeMillis();
		
		UrlObj con = null;
		
		if (post ==  null) return "errror no post supplied";
		
		App.log("token: " + token);
		
		boolean success = false;
		try {
			// open URL, passing selection details using POST
			con = URLUtil.getUrlStream(URL_SELECTION_POST, false, URLUtil.AUTH_FULL, post, token, true, false, false, false);
			if (con.ok) {
				success = true;
			}
		} catch (IOException e) {
			App.exception(e);
		} finally {
			try {
				if (con!=null && con.stream != null) con.stream.close();
			} catch (Exception e) {
				App.exception(e);
			}
			if (con != null) con.closeCon();
		}
		
		if (!success) {
			return "failed to post selection to FPL";
		}
		
		ContentValues updateVals = new ContentValues();
		final String t_squad_gameweek_player = "squad_gameweek_player";
		final String t_where_sgp = "squad_id = " + squad_id + " AND gameweek = " + gameweek + " AND fpl_player_id = ?";
		String[] t_wArgs = { "0" };
		
		// update db
		// iterate squad
		for (int i=0; i<SquadUtil.SQUAD_SIZE; i++) {
			SquadPlayer p = squad.players[i];
			t_wArgs[0] = String.valueOf(p.fpl_id);
			// populate values
			updateVals.put(f_selected, p.selected);
			int val_capt;
			if (p.captain) {
				val_capt = SquadUtil.CAPTAIN;
			} else if (p.vice_captain) {
				val_capt = SquadUtil.VICE_CAPTAIN;
			} else {
				val_capt = 0;
			}
			updateVals.put(f_captain, val_capt);
			// run update
			dbGen.update(t_squad_gameweek_player, updateVals, t_where_sgp, t_wArgs);
		}
		
		Long timeDiff = System.currentTimeMillis() - startTime;
		float timeSecs = (float)timeDiff / 1000;
		App.log("done " + proc_name + " in " + timeSecs + " seconds");
		
		return null;
	}
	
	private int getPosition(int fplId) {
		Cursor cur = dbGen.rawQuery("SELECT position FROM player_season WHERE season = " + season + " AND fpl_id = " + fplId, null);
		cur.moveToFirst();
		int pos = cur.getInt(0);
		cur.close();
		return pos;
	}
	
	private static class TransferPlayer {
		int fpl_id;
		int position;
	}
	public static class TransferConfirm {
		public boolean doTransfers;
		public boolean error;
		public String fplMessage;
		public String confirmPost;
		public String selectPost;
		public ArrayList<TransferItem> transfers;
		public String token;
		public Squad squad;
	}
	public static class TransferItem {
		public int out;
		public int in;
	}
	
	private static final String REGEX_CONFIRM = "<img src=\"http://cdn.ismfg.net/static/plfpl/img/icons/(up|down).png\" height=\"12\" width=\"12\""
		+ " alt=\"\"/>\\W+(.+?)\\W+</td>";
	private static final String UP = "up";
	
	private static final String REGEX_HITS = "these transfers will deduct ([0-9]+) points from your total score";
	
	// load FPL (pre-)confirmation page, extract text for display to user
	private String loadFplConfirmation(String post, String token) {
		UrlObj con = null;
		String proc_name = "fpl confirmation info";
		App.log("starting " + proc_name + " get");
		long startTime = System.currentTimeMillis();
		Scanner scan = null;
		String points_hits = "0";
		
		ArrayList<String> confirm_out = new ArrayList<String>();
		ArrayList<String> confirm_in = new ArrayList<String>();
		
		try {
			con = URLUtil.getUrlStream(URL_FPL_CHECK, false, URLUtil.AUTH_FULL, post, token, false, false, false, false);
			if (con.ok) {
				scan = new Scanner(con.stream);
				
				// team (player scores inside match)
				Pattern p = Pattern.compile(REGEX_CONFIRM, Pattern.MULTILINE);
				while (scan.findWithinHorizon(p, 0) != null) {
					MatchResult m = scan.match();
					String up_down = m.group(1);
					String player = m.group(2);
					if (up_down.equals(UP)) {
						confirm_in.add(player);
						App.log("in: " + player);
					} else {
						confirm_out.add(player);
						App.log("out: " + player);
					}
				}
				
				// team (player scores inside match)
				Pattern hits = Pattern.compile(REGEX_HITS);
				while (scan.findWithinHorizon(hits, 0) != null) {
					MatchResult m = scan.match();
					App.log("hits match: " + m.group());
					points_hits = m.group(1);
				}
			} else {
				App.log(proc_name + ": url failed");
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
		
		Long timeDiff = System.currentTimeMillis() - startTime;
		float timeSecs = (float)timeDiff / 1000;
		App.log("done " + proc_name + " in " + timeSecs + " seconds");
		
		StringBuffer message = new StringBuffer();
		message.append("Players In:");
		boolean first = true;
		for (String p : confirm_in) {
			if (first) {
				first = false;
			} else {
				message.append(",");
			}
			message.append(" " + p);
		}
		
		message.append("\nPlayers Out:");
		first = true;
		for (String p : confirm_out) {
			if (first) {
				first = false;
			} else {
				message.append(",");
			}
			message.append(" " + p);
		}
		
		message.append("\nChanges: " + confirm_in.size());
		
		if ( (confirm_in.size() == 0) || (confirm_out.size() == 0)
			 || (confirm_in.size() != confirm_out.size()) ) {
			 App.log("Error: in/out don't look correct");
			 return null;
		}
		
		message.append("\nPoints Hit: " + points_hits);
		
		return message.toString().replace(APOST, APOST_REP);
	}
	
	public static final String APOST = "&#39;";
	public static final String APOST_REP = "'";
	
	//<input id="id_pick_formset-0-element" name="pick_formset-0-element" type="hidden" value="104" />
	private static final String REGEX_ELEMENT_FPL_ID = "pick_formset-([0-9]+)-element\" type=\"hidden\" value=\"([0-9]+)\"";
	private static final String REGEX_ELEMENT_ELEMENT_ID = "pick_formset-[0-9]+-id\" type=\"hidden\" value=\"([0-9]+)\"";
	private static final String REGEX_ELEMENT_SELL_PRICE = "pick_formset-[0-9]+-selling_price\" type=\"hidden\" value=\"([0-9]+)\"";
	private static final String REGEX_TOKEN = "value='([A-Za-z0-9]+)'";
	private static final String FPL_ID_PREFIX = "                        <input id=\"id_pick_formset-";
	private static final String TOKEN_PREFIX = "            <input type='hidden' name='csrfmiddlewaretoken'";
	//int min_element_id = 0;
	//private static final int MAX_ELEMENT_ID = 11025;
	
	private HashMap<Integer, Integer> fpl_id2element;
	
	// get current squad from FPL transfer page, with 1-15 IDs needed for transfers...
	private String getSquadTransferIDs() {
		BufferedReader reader = null;
		InputStreamReader iread = null;
		UrlObj con = null;
		boolean ok = false;
		String line="";
		String proc_name = "transfer squad ids";
		
		fpl_id2element = new HashMap<Integer, Integer>();
		
		Pattern p_element_fpl = Pattern.compile(REGEX_ELEMENT_FPL_ID);
		Pattern p_element_element = Pattern.compile(REGEX_ELEMENT_ELEMENT_ID);
		Pattern p_element_sell_price = Pattern.compile(REGEX_ELEMENT_SELL_PRICE);
		Pattern p_token = Pattern.compile(REGEX_TOKEN);
		int element_fpl_id = 0;
		int element_sell_price = 0;
		int index = -1;
		String token = null;
		
		try {
			App.log("starting " + proc_name + " get");
			long startTime = System.currentTimeMillis();
			
			con = URLUtil.getUrlStream(URL_GET_TRANSFER_IDS, false, URLUtil.AUTH_FULL);
			if (con.ok) {
				iread = new InputStreamReader(con.stream, "UTF-8");
			    reader = new BufferedReader(iread, 1024);
				
				for (; (line = reader.readLine()) != null;) {
					// get element ids for each player in squad
					if (line.startsWith(FPL_ID_PREFIX)) {
			        	// first get fpl player id. store
					    Matcher m = p_element_fpl.matcher(line);
					    if (m.find()) {
					    	index = Integer.parseInt(m.group(1));
					    	element_fpl_id = Integer.parseInt(m.group(2));
					    // else try to find element id, and update db
					    } else {
					    	if (index > -1) {
						    	m = p_element_element.matcher(line);
						    	if (m.find()) {
							    	int element_element_id = Integer.parseInt(m.group(1));
							    	//if (min_element_id == 0) min_element_id = element_element_id;
							    	if ( (element_fpl_id != 0) && (element_sell_price != 0) ) {
							    		//int index = element_element_id - min_element_id;
							    		App.log("fpl id: " + element_fpl_id + " element: " + element_element_id + " index: " + index + " sell price: " + element_sell_price);
							    		players_old_fpl_id[index] = element_fpl_id;
							    		players_old_out_price[index] = element_sell_price;
							    		fpl_id2element.put(element_fpl_id, element_element_id);
							    	}
							    	// reset
							    	element_fpl_id = 0;
							    	element_sell_price = 0;
							    // selling price
							    } else {
							    	m = p_element_sell_price.matcher(line);
							    	if (m.find()) {
							    		element_sell_price = Integer.parseInt(m.group(1));
							    		App.log("sell price: " + element_sell_price);
							    	}
							    }
					    	}
					    }
					// get fpl token for transfer confirm
					} else if (line.startsWith(TOKEN_PREFIX)) {
						Matcher m = p_token.matcher(line);
				    	if (m.find()) {
				    		token = m.group(1);
				    		App.log("token: " + token);
				    	}
					}
					
				}
				
				ok = true;
				for (int i=0; i<SquadUtil.SQUAD_SIZE; i++) {
					if (!(players_old_fpl_id[i] > 0)) {
						App.log("!(players_old[i] > 0");
						ok = false;
					}
					if (!(players_old_out_price[i] > 0)) {
						App.log("!(players_old_out_price[i] > 0)");
						ok = false;
					}
				}
				if (token == null) {
					App.log("token == null");
					ok = false;
				}
				
				Long timeDiff = System.currentTimeMillis() - startTime;
				float timeSecs = (float)timeDiff / 1000;
				App.log("done " + proc_name + " in " + timeSecs + " seconds");
			} else {
				App.log(proc_name + ": url failed");
			}
		} catch (IOException e) {
			App.exception(e);
			ok = false;
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
		}
		
		if (ok) {
			return token;
		} else {
			return null;
		}
		
	}
	
}
