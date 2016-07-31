package com.pennas.fpl.scrape;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

import com.pennas.fpl.App;
import com.pennas.fpl.App.GWFixture;
import com.pennas.fpl.Vidiprinter;
import com.pennas.fpl.Vidiprinter.VidiEntry;
import com.pennas.fpl.process.FPLService;
import com.pennas.fpl.process.FPLService.ProcRes;
import com.pennas.fpl.process.ProcessData;
import com.pennas.fpl.process.UpdateManager;
import com.pennas.fpl.util.DbGen;
import com.pennas.fpl.util.SquadUtil;
import com.pennas.fpl.util.URLUtil;
import com.pennas.fpl.util.URLUtil.UrlObj;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class ScrapePlayers {
	//private static final String STATUS_AVAIL = "a";
	private static final String STATUS_INJURED = "i";
	private static final String STATUS_DOUBT = "d";
	private static final String STATUS_GONE = "u";
	private static final String STATUS_SUSPENDED = "s";
	private static final String STATUS_NOT_AVAILABLE = "n";
	
	//http://www.regular-expressions.info/reference.html
	private static final String ID = "id";
	private static final String STATUS = "status";
	private static final String IMGCODE = "code";
	//private static final int FIRSTNAME = 4;
	private static final String SURNAME = "web_name";
	//private static final int ADDED = 6;
	//private static final int SQUADNUMBER = 7;
	private static final String NEWS = "news";
	private static final String ORIGINALCOST = "original_cost";
	private static final String NOWCOST = "now_cost";
	//private static final int MAXCOST = 11;
	//private static final int MINCOST = 12;
	//private static final int EVENTCOST = 13;
	//private static final int TRANSFERSOUT = 14;
	//private static final int TRANSFERSIN = 15;
	//private static final int TRANSFERSBALANCE = 16;
	private static final String TRANSFERSOUTEVENT = "transfers_out_event";
	private static final String TRANSFERSINEVENT = "transfers_in_event";
	//private static final int LASTSEASONPTS = 19;
	private static final String TOTALPTS = "total_points";
	//private static final int EVENTPTS = 21;
	private static final String SELECTED_BY_PERCENT = "selected_by_percent";
	private static final String FORM = "form";
	private static final String PPG = "points_per_game";
	//private static final int NEWSRETURN = 25;
	//private static final int NEWSADDED = 26;
	//private static final int NEWSUPDATED = 27;
	//private static final int INDREAMTEAM = 28;
	//private static final int DREAMTEAMCOUNT = 29;
	//private static final int VALUEFORM = 30;
	//private static final int VALUESEASON = 31;
	private static final String MINUTES = "minutes";
	private static final String GOALSSCORED = "goals_scored";
	private static final String ASSISTS = "assists";
	private static final String CLEANSHEETS = "clean_sheets";
	//private static final int GOALSCONCEDED = 36;
	//private static final int OWNGOALS = 37;
	//private static final int PENSAV = 38;
	//private static final int PENMIS = 39;
	private static final String YELLOW = "yellow_cards";
	//private static final int RED = 41;
	//private static final int SAVES = 42;
	private static final String BONUS = "bonus";
	private static final String EAINDEX = "ea_index";
	//private static final int FANRATING = 45;
	private static final String POSITION = "element_type_id";
	private static final String TEAMID = "team_id";
	//private static final int COST_CHANGE_START = 48;
	//private static final int COST_CHANGE_START_FALL = 49;
	//private static final int COST_CHANGE_EVENT = 50;
	//private static final int COST_CHANGE_EVENT_FALL = 51;
	
	/*
	, "id": 0
	1
	, "status": 1
	"a"
	, "code": 2
	231795
	, "first_name": 3
	"Manuel"
	, "second_name": 4
	"Almunia"
	, "web_name": 5
	"Almunia"
	, "added": 6
	"2011-07-14 10:25:53"
	, "squad_number": 7
	null
	, "news": 8
	""
	, "original_cost": 9
	55
	, "now_cost": 10
	55
	, "max_cost": 11
	55
	, "min_cost": 12
	55
	, "event_cost": 13
	55
	, "transfers_out": 14
	0
	, "transfers_in": 15
	0
	, "transfers_balance": 16
	0
	, "transfers_out_event": 17
	0
	, "transfers_in_event": 18
	0
	, "last_season_points": 19
	0
	, "total_points": 20
	28
	, "event_points": 21
	0
	, "selected_by_percent": 23
	56
	, "form": 24
	"0.0"
	, "points_per_game": 25
	"3.5"
	, "news_return": 25
	null
	, "news_added": 27
	null
	, "news_updated": 28
	null
	, "in_dreamteam": 29
	false
	, "dreamteam_count": 30
	0
	, "value_form": 31
	"0.0"
	, "value_season": 32
	"0.0"
	, "minutes": 37
	720
	, "goals_scored": 38
	0
	, "assists": 39
	0
	, "clean_sheets": 40
	2
	, "goals_conceded": 41
	9
	, "own_goals": 42
	0
	, "penalties_saved": 38
	1
	, "penalties_missed": 39
	0
	, "yellow_cards": 40
	1
	, "red_cards": 41
	0
	, "penalties_saved": 43
	0
	, "penalties_missed": 44
	0
	, "saves": 47
	15
	, "bonus": 48
	0
	, "ea_index": 49
	0
	, "fan_rating": 50
	"0.0"
	, "element_type_id": 51
	1
	, "team_id": 52
	1
	, "cost_change_start": 43
	0
	, "cost_change_start_fall": 54
	0
	, "cost_change_event": 55
	0
	, "cost_change_event_fall": 56
	0
	 */
	//{"transfers_out": 14, "yellow_cards": 40, "code": 2, "goals_conceded": 36, "saves": 42, "event_points": 21, "transfers_balance": 16, "last_season_points": 19, "event_cost": 13, "news_added": 26, "goals_scored": 33, "web_name": 5, "value_season": 31, "in_dreamteam": 28, "id": 0, "first_name": 3, "transfers_out_event": 17, "own_goals": 37, "form": 23, "max_cost": 11, "cost_change_event_fall": 51, "selected": 22, "min_cost": 12, "cost_change_start_fall": 49, "total_points": 20, "penalties_missed": 39, "transfers_in": 15, "status": 1, "added": 6, "element_type_id": 46, "squad_number": 7, "bonus": 43, "dreamteam_count": 29, "fan_rating": 45, "now_cost": 10, "points_per_game": 24, "clean_sheets": 35, "assists": 34, "value_form": 30, "news": 8, "original_cost": 9, "ea_index": 44, "penalties_saved": 38, "cost_change_start": 48, "news_updated": 27, "minutes": 32, "team_id": 47, "transfers_in_event": 18, "news_return": 25, "red_cards": 41, "second_name": 4, "cost_change_event": 50}

	/*
	[1, "a", 231795, "Manuel", "Almunia", "Almunia", "2011-07-14 10:25:53", null, "", 55, 55, 55, 55, 55, 0, 0, 0, 0, 0, 0, 28, 0, 56, "0.0", "3.5", null, null, null, false, 0, "0.0", "0.0", 720, 0, 0, 2, 9, 0, 1, 0, 1, 0, 15, 0, 0, "0.0", 1, 1]
	[1, "a", 231795, "Manuel", "Almunia", "Almunia", "2011-07-14 10:25:53", null, "", 55, 55, 55, 55, 55, 1551, 157, -1118, 458, 57, 0, 0, 0, 13000, "0.0", "0.0", null, null, null, false, 0, "0.0", "0.0", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "0.0", 1, 1, 0, 0, 0, 0]
	[1, "a", 231795, "Manuel", "Almunia", "Almunia", "2011-07-14 10:25:53", null, "", 55, 53, 55, 53, 53, 5453, 725, -205, 60, 36, 0, 0, 0, "0.4", "0.4", "0.0", "0.0", "2011-10-31 13:30:09", "2011-10-14 14:15:35", null, false, 0, "0.0", "0.0", -2, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "0.0", 1, 1, -2, 2, 0, 0],
	[1, "a", 59936, "Wojciech", "Szczesny", "Szczesny", null, "", "2014-07-16T10:43:02Z", 55, 55, 55, 55, 55, null, null, null, null, null, "0.0", "28.5", 0, 0, 0, 0, false, 0, 41053, "9.7", "0.0", 0, 0, 0, 0, 0, 0, 157, 0, "0.0", null, "3.1", false, 3330, 0, 0, 16, 41, 0, 1, 0, 2, 0, 113, 4, 475, 194, 1, 1, 0, 0, 0, 0]
	 */
	
	// actually 62 at moment, but add a couple to avoid AIOBE
	private static final int numCommas = 75;
	
	private int[] 	a_pid;
	private String[] a_pname;
	private int[]	a_ppos;
	private int[]	a_pteam;
	private int[]	a_pimgcode;
	private int[] 	a_pgoal;
	private int[] 	a_pass;
	private int[] 	a_pbon;
	private int[] a_pprice;
	private int[] 	a_ppts;
	private int[] 	a_pminutes;
	private int[] a_pform;
	private int[] a_pppg;
	private int[] 	a_pyel;
	private int[] 	a_punavail;
	private int[] 	a_pdoubt;
	private String[] a_pnews;
	private int[]	a_player_id;
	private int[]   a_pprice_start;
	private int[]	a_pea_ppi;
	private int[]	a_p_cs;
	private int[]	a_p_owned;
	private int[]	a_ti_gw;
	private int[]	a_to_gw;
	private int[]	a_nti_gw;
	
	private static final String f_name = "name";
	private static final String f_team_id = "team_id";
	private static final String f_fpl_yellow_flag = "fpl_yellow_flag";
	private static final String f_fpl_red_flag = "fpl_red_flag";
	private static final String f_fpl_news = "fpl_news";
	//private static final String f_picture_code = "picture_code";
	private static final String f_code_14 = "code_14";
	
	private static final String f_position = "position";
	private static final String f_goals = "goals";
	private static final String f_assists = "assists";
	private static final String f_bonus = "bonus";
	private static final String f_price = "price";
	private static final String f_points = "points";
	private static final String f_minutes = "minutes";
	private static final String f_form = "form";
	private static final String f_ppg = "ppg";
	private static final String f_yellow = "yellow";
	private static final String f_fpl_id = "fpl_id";
	private static final String f_player_id = "player_id";
	private static final String f_season = "season";
	private static final String f_price_start = "price_start";
	private static final String f_clean_sheets = "clean_sheets";
	private static final String f_red = "red";
	private static final String f_ea_ppi = "ea_ppi";
	private static final String f_owned_perc = "owned_perc";
	private static final String f_ti_gw = "ti_gw";
	private static final String f_to_gw = "to_gw";
	private static final String f_nti_gw = "nti_gw";
	
	private static final String t_squad_gameweek_player = "squad_gameweek_player";
	
	private static final String vidi_up_txt = " went up in price, from ";
	private static final String vidi_down_txt = " went down in price, from ";
	private static final String vidi_to_txt = "M to ";
	private static final String vidi_to_txt_after = "M";
	
	public static final String URL_PLAYERS = "http://fantasy.premierleague.com/transfers/";
	//private static final String URL_PLAYERS = "http://192.168.1.5/transfers.html";
	static final String URL_PLAYER_HISTORY = "http://fantasy.premierleague.com/web/api/elements/";
	
	private int[] a_map_ids;
	
	private PlayerDetails scP;
	
	private int players_scraped;
	private int changes;
	private int inserts;
	
	private ContentValues player_update;
	private ContentValues player_season_update;
	
	private int[] commaLoc = new int[numCommas + 2];
	
	private SQLiteDatabase dbGen = App.dbGen;
	
	private String player = null;
	private int season = App.season;
	private int gameweek = App.cur_gameweek;
	
	private static final String PLAYER_DATA_PREFIX = "        <script type=\"application/json\">{\"maxTypes\": {\"1\": 2, \"3\": 5, \"2\": 5";
	private static final String BANK_PREFIX = "                                    <div class=\"ismSBValue ismSBPrimary\" id=\"ismToSpend\">";
	
	private static final String REGEX_PLAYERS = "\\[[0-9]+, \".\", .*?]";
	private static final String REGEX_WATCHLIST = "watchlist\": \\[(.*?)\\]";
	private static final String REGEX_TEAMPRICES = "\"elid\": ([0-9]+), \"sell\": [0-9]+, \"paid\": ([0-9]+)\\}";
	private static final String REGEX_STAT_WRAP = "\"elStat\": \\{(.+?)\\}";
	private static final String REGEX_STATS = "\"([a-z_]+)\": ([0-9]+)";
	
	private static final String REGEX_BANK = "<div class=\"ismSBValue ismSBPrimary\" id=\"ismToSpend\">([0-9]+\\.[0-9]+)</div>";
	
	private HashMap<String, Integer> stat2id = new HashMap<String, Integer>();
	
	public void updatePlayers(ProcRes result) {
		//Debug.startMethodTracing("fpl_scrape_p2", 15000000);
		App.log("starting NEW players scrape");
		long startTime = System.currentTimeMillis();
		
		scP = new PlayerDetails();
		
		Cursor curMax = dbGen.rawQuery("SELECT MAX(fpl_id) maxid FROM player_season WHERE season = " + season, null);
		curMax.moveToFirst();
		final int max_fpl_id = curMax.getInt(0);
		curMax.close();
		
		a_map_ids = new int[max_fpl_id+1];
		for (int a=0; a<=max_fpl_id; a++) {
			a_map_ids[a] = -1;
		}
		
		Cursor curAll = dbGen.rawQuery("SELECT p.name, ps.position, p.team_id, p.picture_code, ps.goals, ps.assists, ps.bonus"
				+ ", ps.price, ps.points, ps.minutes, ps.form, ps.ppg, ps.yellow, p.fpl_yellow_flag, p.fpl_red_flag, p.fpl_news"
				+ ", p._id, ps.fpl_id, ps.price_start, ps.ea_ppi, ps.clean_sheets, ps.owned_perc, ps.ti_gw, ps.to_gw, ps.nti_gw, p.code_14"
				+ " FROM player_season ps, player p"
				+ " WHERE ps.season = " + season + " AND p._id = ps.player_id", null);
		curAll.moveToFirst();
		int numPlayers = curAll.getCount();
		a_pid = new int[numPlayers];
		a_pname = new String[numPlayers];
		a_ppos = new int[numPlayers];
		a_pteam = new int[numPlayers];
		a_pimgcode = new int[numPlayers];
		a_pgoal = new int[numPlayers];
		a_pass = new int[numPlayers];
		a_pbon = new int[numPlayers];
		a_pprice = new int[numPlayers];
		a_ppts = new int[numPlayers];
		a_pminutes = new int[numPlayers];
		a_pform = new int[numPlayers];
		a_pppg = new int[numPlayers];
		a_pyel = new int[numPlayers];
		a_punavail = new int[numPlayers];
		a_pdoubt = new int[numPlayers];
		a_pnews = new String[numPlayers];
		a_player_id = new int[numPlayers];
		a_pprice_start = new int[numPlayers];
		a_pea_ppi = new int[numPlayers];
		a_p_cs = new int[numPlayers];
		a_p_owned = new int[numPlayers];
		a_ti_gw = new int[numPlayers];
		a_to_gw = new int[numPlayers];
		a_nti_gw = new int[numPlayers];
		
		result.setMaxName(numPlayers, "players update");
		
		int i = 0;
		while (!curAll.isAfterLast()) {
			a_pid[i] = curAll.getInt(curAll.getColumnIndex(f_fpl_id));
			a_pname[i] = curAll.getString(curAll.getColumnIndex(f_name));
			a_ppos[i] = curAll.getInt(curAll.getColumnIndex(f_position));
			a_pteam[i] = curAll.getInt(curAll.getColumnIndex(f_team_id));
			a_pimgcode[i] = curAll.getInt(curAll.getColumnIndex(f_code_14));
			a_pgoal[i] = curAll.getInt(curAll.getColumnIndex(f_goals));
			a_pass[i] = curAll.getInt(curAll.getColumnIndex(f_assists));
			a_pbon[i] = curAll.getInt(curAll.getColumnIndex(f_bonus));
			a_pprice[i] = curAll.getInt(curAll.getColumnIndex(f_price));
			a_ppts[i] = curAll.getInt(curAll.getColumnIndex(f_points));
			a_pminutes[i] = curAll.getInt(curAll.getColumnIndex(f_minutes));
			a_pform[i] = curAll.getInt(curAll.getColumnIndex(f_form));
			a_pppg[i] = curAll.getInt(curAll.getColumnIndex(f_ppg));
			a_pyel[i] = curAll.getInt(curAll.getColumnIndex(f_yellow));
			a_pdoubt[i] = curAll.getInt(curAll.getColumnIndex(f_fpl_yellow_flag));
			a_punavail[i] = curAll.getInt(curAll.getColumnIndex(f_fpl_red_flag));
			a_p_cs[i] = curAll.getInt(curAll.getColumnIndex(f_clean_sheets));
			a_p_owned[i] = curAll.getInt(curAll.getColumnIndex(f_owned_perc));
			a_ti_gw[i] = curAll.getInt(curAll.getColumnIndex(f_ti_gw));
			a_to_gw[i] = curAll.getInt(curAll.getColumnIndex(f_to_gw));
			a_nti_gw[i] = curAll.getInt(curAll.getColumnIndex(f_nti_gw));
			
			byte[] ticker_compressed = curAll.getBlob(curAll.getColumnIndex(f_fpl_news));
			a_pnews[i] = DbGen.decompress(ticker_compressed);
			//a_pnews[i] = curAll.getString(curAll.getColumnIndex(f_fpl_news));
			
			a_player_id[i] = curAll.getInt(curAll.getColumnIndex("_id"));
			a_pea_ppi[i] = curAll.getInt(curAll.getColumnIndex(f_ea_ppi));
			a_pprice_start[i] = curAll.getInt(curAll.getColumnIndex(f_price_start));
			
			a_map_ids[a_pid[i]] = i;
			
			i++;
			curAll.moveToNext();
		}
		curAll.close();
		
		players_scraped = 0;
		changes = 0;
		inserts = 0;
		int lines = 0;
		String line="";
		boolean doInsert;
		
		Pattern p_player;
		Pattern p_stats;
		Pattern p_stat_wrap;
		UrlObj con = null;
		BufferedReader reader = null;
		InputStreamReader iread = null;
		
		Pattern p_bank = Pattern.compile(REGEX_BANK);
		String whereArgs[] = new String[1];
		
		loadCodes();
		
	    dbGen.execSQL(DbGen.TRANSACTION_IMMEDIATE_BEGIN);
		
		try {
			con = URLUtil.getUrlStream(URL_PLAYERS, false, URLUtil.AUTH_FULL);
			
			if (con.ok) {
				iread = new InputStreamReader(con.stream, "UTF-8");
			    reader = new BufferedReader(iread, 1024);
			    p_player = Pattern.compile(REGEX_PLAYERS);
			    p_stats = Pattern.compile(REGEX_STATS);
			    p_stat_wrap = Pattern.compile(REGEX_STAT_WRAP, Pattern.DOTALL);
			    
			    for (; (line = reader.readLine()) != null;) {
			        lines++;
			        
			        // current bank
					if (line.startsWith(BANK_PREFIX)) {
			        	Matcher m = p_bank.matcher(line);
					    if (m.find()) {
					    	String bank = m.group(1);
					    	App.log("bank: " + bank);
					    	dbGen.execSQL("UPDATE squad SET bank = " + bank + " WHERE _id = " + App.my_squad_id);
					    }
					// main data line for players etc
			        } else if (line.startsWith(PLAYER_DATA_PREFIX)) {
			    		int k;
					    int comma;
					    
					    Matcher m = p_stat_wrap.matcher(line);
					    if (m.find()) {
					    	m = p_stats.matcher(m.group(1));
						    while (m.find()) {
						    	//App.log(m.group(1) + " - " + m.group(2));
						    	stat2id.put(m.group(1), Integer.parseInt(m.group(2)));
						    }
					    }
					    
					    m = p_player.matcher(line);
					    while (m.find()) {
							player = m.group();
							//AppClass.log(player);
					    	
							comma = 0;
			    		    k = 0;
			    		    
			    		    //boolean debug = player.contains("Saha");
			    		    //boolean debug = true;
							
							commaLoc[0] = -1;
			    		    //App.log(player);
			    		    while ( (comma = player.indexOf(',', comma)) > 0) {
			    		    	k++;
			    		    	if (k < commaLoc.length) {
			    		    		// prev entry started with a "
			    		    		// must look for ending " before looking for a ,
			    		    		// in case there are commas in string
			    		    		if ( (k > 1) && (player.charAt(commaLoc[k-1] + 2) == '"') ) { 
			    		    			// find ending "
			    		    			//App.log("quoted string");
			    		    			boolean y = true;
			    		    			int q = commaLoc[k-1] + 2;
			    		    			// loop until found a quote without a \ in front of it
			    		    			while (y) {
			    		    				q = player.indexOf('"', q + 1);
			    		    				y = (player.charAt(q-1) == '\\');
			    		    				//if (debug) App.log(player.substring(q-1, q+1));
			    		    			}
			    		    			// find ' after ending "
			    		    			commaLoc[k] = player.indexOf(',', q);
			    		    			comma = commaLoc[k];
			    		    		} else {
			    		    			commaLoc[k] = comma;
			    		    		}
			    		    		//if (debug && k>1) App.log(player.substring(commaLoc[k-1], commaLoc[k]));
			    		    		//App.log(k + " = " + comma);
			    		    	} else {
			    		    		//App.log(k + "!!!!" + comma);
			    		    	}
			    		    	comma++;
			    		    }
			    		    commaLoc[numCommas + 1] = player.length() - 1;
			    		    
			    		    scP.pid = getInt(ID);
			    		    scP.pname = getString(SURNAME, true);
			    		    //App.log("name = '" + scP.pname + "'");
				        	scP.ppos = getInt(POSITION);
				        	scP.pteam = getInt(TEAMID);
				        	// convert team id from fpl season id to base team _id
				        	//App.log("pteam = " + scP.pteam);
				        	scP.pteam = App.team_fpl2id.get(scP.pteam);
				        	scP.pimgcode = getInt(IMGCODE);
				        	String status = getString(STATUS, true);
				        	if (status.equals(STATUS_DOUBT)) {
				        		scP.punavail = 0;
				        		scP.pdoubt = 1;
				        	} else if ( status.equals(STATUS_INJURED) || status.equals(STATUS_GONE)
				        			 || status.equals(STATUS_NOT_AVAILABLE) || status.equals(STATUS_SUSPENDED) ) {
				        		scP.punavail = 1;
			        			scP.pdoubt = 0;
					    	} else {
					    		scP.punavail = 0;
			        			scP.pdoubt = 0;
					    	}
				        	scP.pnews = getString(NEWS, true);
				        	if (scP.pnews.equals("")) {
				        		scP.pnews = null;
				        	}
				        	scP.ti_gw = getInt(TRANSFERSINEVENT);
				        	scP.to_gw = getInt(TRANSFERSOUTEVENT);
				        	scP.nti_gw = scP.ti_gw - scP.to_gw;
				        	
				        	// fpl shows last season's stats as this during before start of GW1
				        	if (gameweek >= 1) {
					        	scP.ppts = getInt(TOTALPTS);
					        	scP.pminutes = getInt(MINUTES);
					        	scP.pgoal = getInt(GOALSSCORED);
					        	scP.pass = getInt(ASSISTS);
					        	scP.pyel = getInt(YELLOW);
					        	scP.pbon = getInt(BONUS);
					        	scP.pform = (int) (getFloat(FORM) * 100);
					        	scP.pppg = (int) (getFloat(PPG) * 100);
					        	scP.ea_ppi = getInt(EAINDEX);
					        	scP.p_cs = getInt(CLEANSHEETS);
				        	}
				        	try {
				        		scP.owned_perc = (int) (getFloat(SELECTED_BY_PERCENT) * 10);
				        	// before season starts
				        	} catch (NumberFormatException e) {
				        		scP.owned_perc = 0;
				        	}
				        	scP.pprice = getInt(NOWCOST);
				        	scP.pprice_start = getInt(ORIGINALCOST);
				        	//scP.pfirstname = getString(FIRSTNAME, true);
				        	
				        	//AppClass.log("id: " + scP.pid + " name: " + scP.pname + " pos: " + scP.ppos + " team: " + scP.pteam);
				        	
				        	player_update = new ContentValues();
				        	player_season_update = new ContentValues();
				        	doInsert = false;
				        	
				        	try {
				            	int d = a_map_ids[scP.pid];
				            	if (d == -1) {
				            		doInsert = true;
				            	} else {
				            		changeCheckLog(f_position, scP.ppos, a_ppos[d], true);
				            		changeCheckLog(f_team_id, scP.pteam, a_pteam[d], false);
					            	// if team changed, delete "future" player_match records
					            	if (scP.pteam != a_pteam[d]) {
					            		dbGen.execSQL("DELETE FROM player_match WHERE season = " + season
					            			  + " AND player_player_id = " + a_player_id[d]
					            		      + " AND (minutes = 0 OR minutes IS NULL)");
					            		VidiEntry e = new VidiEntry();
					            		e.message = scP.pname + " moved from " + App.id2team.get(a_pteam[d]) + " to " + App.id2team.get(scP.pteam);
					            		e.player_fpl_id = a_pid[d];
					            		e.category = Vidiprinter.CAT_CLUB_CHANGE;
					            		e.gameweek = App.cur_gameweek;
					            		Vidiprinter.addVidi(e, result.context);
					            	}
					    			changeCheckLog(f_code_14, scP.pimgcode, a_pimgcode[d], false);
					            	changeCheckLog(f_goals, scP.pgoal, a_pgoal[d], true);
					            	changeCheckLog(f_assists, scP.pass, a_pass[d], true);
					            	changeCheckLog(f_bonus, scP.pbon, a_pbon[d], true);
					            	changeCheckLog(f_ea_ppi, scP.ea_ppi, a_pea_ppi[d], true);
					            	changeCheckLog(f_clean_sheets, scP.p_cs, a_p_cs[d], true);
					            	changeCheckLog(f_owned_perc, scP.owned_perc, a_p_owned[d], true);
					            	
					            	if (scP.pprice != a_pprice[d]) {
					            		changeLog(f_price, String.valueOf(scP.pprice), String.valueOf(a_pprice[d]), true);
					            		VidiEntry e = new VidiEntry();
					            		if (scP.pprice > a_pprice[d]) {
					            			e.message = scP.pname + vidi_up_txt + ((float)a_pprice[d] / 10) + vidi_to_txt + ((float)scP.pprice / 10) + vidi_to_txt_after;
					            		} else {
					            			e.message = scP.pname + vidi_down_txt + ((float)a_pprice[d] / 10) + vidi_to_txt + ((float)scP.pprice / 10) + vidi_to_txt_after;
					            		}
					            		e.player_fpl_id = a_pid[d];
					            		e.category = Vidiprinter.CAT_PRICE_CHANGE;
					            		e.gameweek = App.cur_gameweek;
					            		Vidiprinter.addVidi(e, result.context);
					            	}
					            	
					            	changeCheckLog(f_points, scP.ppts, a_ppts[d], true);
					            	changeCheckLog(f_minutes, scP.pminutes, a_pminutes[d], true);
					            	changeCheckLog(f_form, scP.pform, a_pform[d], true);
					            	changeCheckLog(f_ppg, scP.pppg, a_pppg[d], true);
					            	changeCheckLog(f_yellow, scP.pyel, a_pyel[d], true);
					            	changeCheckLog(f_fpl_red_flag, scP.punavail, a_punavail[d], false);
					            	changeCheckLog(f_fpl_yellow_flag, scP.pdoubt, a_pdoubt[d], false);
					            	changeCheckLog(f_price_start, scP.pprice_start, a_pprice_start[d], true);
					            	changeCheckLog(f_ti_gw, scP.ti_gw, a_ti_gw[d], true);
					            	changeCheckLog(f_to_gw, scP.to_gw, a_to_gw[d], true);
					            	changeCheckLog(f_nti_gw, scP.nti_gw, a_nti_gw[d], true);
					            	
					            	if (!scP.pname.equals(a_pname[d])) {
					            		changeLog(f_name, scP.pname, a_pname[d], false);
					            	}
					            	if ( ((scP.pnews==null) && (a_pnews[d]!=null))
					            	  || ((scP.pnews!=null) && (a_pnews[d]==null))
					            	  || ((scP.pnews!=null) && (a_pnews[d]!=null) && (!scP.pnews.equals(a_pnews[d])) ) ) {
					            		changeLog(f_fpl_news, scP.pnews, a_pnews[d], false);
					            		if ( (scP.pnews != null) && !scP.pnews.equals("") ) {
						            		VidiEntry e = new VidiEntry();
						            		e.message = scP.pname + ": " + scP.pnews;
						            		e.player_fpl_id = a_pid[d];
						            		e.category = Vidiprinter.CAT_PLAYER_NEWS;
						            		e.gameweek = App.cur_gameweek;
						            		Vidiprinter.addVidi(e, result.context);
					            		}
					            	}
					            	
					            	boolean changed = false;
					            	if (player_update.size() > 0) {
					            		updArgsPlayer[0] = String.valueOf(a_player_id[d]);
					        			dbGen.update(t_player, player_update, updWherePlayer, updArgsPlayer);
					        			changed = true;
					            	}
					            	
					            	if (player_season_update.size() > 0) {
					            		updArgsPlayerSeason[0] = String.valueOf(a_pid[d]);
					        			dbGen.update(t_player_season, player_season_update, updWherePlayerSeason, updArgsPlayerSeason);
					        			changed = true;
					            	}
					            	
					            	if (changed) changes++;
				            	}
				        	} catch (ArrayIndexOutOfBoundsException e) {
				        		doInsert = true;
				        	}
				        	
				        	if (doInsert) {
				        		inserts++;
				        		App.log("Insert ps " + scP.pname + " " + scP.pid);
				        		
				        		// check whether player maps to old record. create one if not
				        		long new_player_id = getOrInsertPid();
				        		
				        		ContentValues newRec = new ContentValues();
				        		newRec.put(f_position, scP.ppos);
				        		newRec.put(f_goals, scP.pgoal);
				        		newRec.put(f_assists, scP.pass);
				        		newRec.put(f_bonus, scP.pbon);
				        		newRec.put(f_price, scP.pprice);
				        		newRec.put(f_points, scP.ppts);
				        		newRec.put(f_minutes, scP.pminutes);
				        		newRec.put(f_form, scP.pform);
				        		newRec.put(f_ppg, scP.pppg);
				        		newRec.put(f_yellow, scP.pyel);
				        		newRec.put(f_season, season);
				        		newRec.put(f_fpl_id, scP.pid);
				        		newRec.put(f_player_id, new_player_id);
				        		newRec.put(f_price_start, scP.pprice_start);
				        		newRec.put(f_ea_ppi, scP.ea_ppi);
				        		newRec.put(f_clean_sheets, scP.p_cs);
				        		newRec.put(f_owned_perc, scP.owned_perc);
				        		newRec.put(f_ti_gw, scP.ti_gw);
				        		newRec.put(f_to_gw, scP.to_gw);
				        		newRec.put(f_nti_gw, scP.nti_gw);
				        		dbGen.insert(t_player_season, null, newRec);
				        		
				        		VidiEntry e = new VidiEntry();
				        		float price = scP.pprice;
				        		price /= 10;
			            		e.message = scP.pname + " added to game, at " + price + "M";
			            		e.player_fpl_id = scP.pid;
			            		e.category = Vidiprinter.CAT_NEW_PLAYER;
			            		e.gameweek = App.cur_gameweek;
			            		Vidiprinter.addVidi(e, result.context);
				        	}
				        	
				        	players_scraped++;
				        	
				        	result.setProgress(players_scraped);
			            	
						} // player loop
					    
					    // watchlist
					    Pattern p_watchlist = Pattern.compile(REGEX_WATCHLIST);
					    m = p_watchlist.matcher(line);
					    final String t_watchlist = "watchlist";
						final String f_player_fpl_id = "player_fpl_id";
						while (m.find()) {
							String watchlist = m.group(1);
							if (watchlist.length() > 0) {
								String[] items = watchlist.split(", ");
								// wipe db watchlist
								dbGen.execSQL("DELETE FROM watchlist");
								// wipe memory watchlist
								App.watchlist.clear();
								// add each entry from fpl to db/memory
								for (String wl : items) {
									int wl_int = Integer.parseInt(wl);
									App.log("wl item: " + wl_int);
									App.watchlist.add(wl_int);
									ContentValues ins = new ContentValues();
									ins.put(f_player_fpl_id, wl_int);
									dbGen.insert(t_watchlist, null, ins);
								}
							}
					    }
						
						final String whereSquadGameweek = "squad_id = " + App.my_squad_id + " and gameweek = " + App.next_gameweek + " and fpl_player_id = ?";
					    
					    // squad sell prices
						if (App.my_squad_id > 0) {
						    Pattern p_squad_prices = Pattern.compile(REGEX_TEAMPRICES);
						    m = p_squad_prices.matcher(line);
						    final String f_bought_price = "bought_price";
						    while (m.find()) {
								String player_fpl_id = m.group(1);
								float bought_price = Float.parseFloat(m.group(2)) / 10f;
								App.log("player_fpl_id: " + player_fpl_id + " bought_price: " + bought_price);
								
								// update database for "next" gameweek
								ContentValues ins = new ContentValues();
								ins.put(f_bought_price, bought_price);
								whereArgs[0] = player_fpl_id;
								dbGen.update(t_squad_gameweek_player, ins, whereSquadGameweek, whereArgs);
						    }
						}
					    
					    break;
			    	} // prefix check
			    } // line loop
				
				Long timeDiff = System.currentTimeMillis() - startTime;
				float timeSecs = (float)timeDiff / 1000;
				App.log("done " + changes + " changes on " + players_scraped + " players (plus " + inserts + " inserts) over " + lines + " lines, in " + timeSecs + " seconds");
			} else {
				App.log("players: url failed");
				result.setError("url failed");
				if (con.updating) {
					result.updating = true;
				}
			}
		} catch (UnsupportedEncodingException e) {
			App.exception(e);
			result.setError("1 " + e);
		} catch (IOException e) {
			App.exception(e);
			result.setError("2 " + e);
		} finally {
			try {
				dbGen.execSQL(DbGen.TRANSACTION_END);
			} catch (SQLException e) {
				App.exception(e);
			} catch (IllegalStateException e) {
				App.exception(e);
			}
			if (reader != null) {
		    	try { 
		    		reader.close();
		    		iread.close();
		    	} catch (IOException e) {
		    		App.exception(e);
		    	}
		    }
			if (con != null) con.closeCon();
			if (result.failed) return;
		}
		
		// get past season history for any player we don't have history for
		//
		// OR fill in current season match hist if it doesn't match my records
		// (out join for if no pm records present)
		Cursor curHist = dbGen.rawQuery("SELECT sub.*, p.name, p.got_history, p.team_id FROM ("
				+ " SELECT ps.points points, ifnull(sum(pm.total), 0) tot, ps.player_id player_id"
				+ " , ps.fpl_id fpl_id, ps.position position, ifnull(sum(pm.minutes),0) totmins, ps.minutes minutes"
				+ " FROM player_season ps"
				+ " left outer join player_match pm on pm.player_player_id = ps.player_id and pm.season = " + season
				+ " WHERE ps.season = " + season
				+ " GROUP BY ps.player_id"
				+ " ) sub, player p"
				+ " WHERE (points != tot OR got_history IS NULL OR minutes != totmins)"
				+ " AND p._id = sub.player_id", null);
		curHist.moveToFirst();
		dbGen.execSQL(DbGen.TRANSACTION_IMMEDIATE_BEGIN);
		ScrapeMatchScoresCatchup smsn = new ScrapeMatchScoresCatchup();
		boolean done_match_hist = false;
		HashSet<Integer> gw_to_process = new HashSet<Integer>();
		while(!curHist.isAfterLast()) {
			if (getPlayerHist(curHist, smsn, result, gw_to_process)) {
				done_match_hist = true;
			}
			curHist.moveToNext();
		}
		// calc squad scores
		if (done_match_hist) {
			for (int gw : gw_to_process) {
				if (squadUtil == null) {
					squadUtil = new SquadUtil();
				}
				squadUtil.updateSquadGameweekScores(gw, false);
				checkFixtureBonusPts(gw, season);
			}
		}
		try {
			dbGen.execSQL(DbGen.TRANSACTION_END);
		} catch (SQLException e) {
			App.exception(e);
		}
		curHist.close();
		
		//Debug.stopMethodTracing();
		if (changes > 0 || inserts > 0) {
			result.setDataChanged(true);
		}
		result.setComplete();
	}
	
	// check all fixtures this gw (which aren't marked as having bonus pts)
	// to see whether they have bonus pts yet
	public static void checkFixtureBonusPts(int gw, int season) {
		Cursor curFix = App.dbGen.rawQuery("SELECT _id, team_home_id, team_away_id FROM fixture WHERE season = " + season
				+ " AND gameweek = " + gw + " AND got_bonus = 1", null);
		curFix.moveToFirst();
		while(!curFix.isAfterLast()) {
			int fixture_id = curFix.getInt(0);
			int team_home_id = curFix.getInt(1);
			int team_away_id = curFix.getInt(2);
			Cursor curBon = App.dbGen.rawQuery("SELECT SUM(bonus) bonus FROM player_match"
						+ " WHERE fixture_id = " + fixture_id, null);
			curBon.moveToFirst();
			if (!curBon.isAfterLast()) {
				int bonus_pts_sum = curBon.getInt(0);
				if (bonus_pts_sum > 0) {
					// have bonus
					App.log("Marking " + App.id2team.get(team_home_id) + " v " + App.id2team.get(team_away_id) + " got bonus pts");
					App.dbGen.execSQL("UPDATE fixture SET got_bonus = " + ScrapeMatchScores_New.GOT_BONUS + " WHERE _id = " + fixture_id);
				}
			}
			curBon.close();
			
			curFix.moveToNext();
		}
		curFix.close();
	}
	
	private SquadUtil squadUtil;
	
	private static final String updWherePlayer = "_id = ?";
	private final String updWherePlayerSeason = "season = " + season + " AND fpl_id = ?";
	private String[] updArgsPlayer = new String[1];
	private String[] updArgsPlayerSeason = new String[1];
	private static final String t_player = "player";
	private static final String t_player_season = "player_season";
	
	private HashMap<Integer, Integer> newpic2id;
	private void loadCodes() {
		newpic2id = new HashMap<Integer, Integer>();
		Cursor curCode = dbGen.rawQuery("SELECT _id, code_14 FROM player WHERE NOT EXISTS (SELECT fpl_id FROM player_season WHERE season = " + season + " AND player_id = player._id)", null);
		curCode.moveToFirst();
		while (!curCode.isAfterLast()) {
			newpic2id.put(curCode.getInt(1), curCode.getInt(0));
			curCode.moveToNext();
		}
		curCode.close();
	}
	
	// check if this player_season record can be mapped to an existing player record
	//
	// if not then create a player record
	private long getOrInsertPid() {
		Integer pid = newpic2id.get(scP.pimgcode);
		
		// already have record with new id
		if (pid != null) {
			// update existing PLAYER record
			updArgsPlayer[0] = String.valueOf(pid);
			ContentValues upd = new ContentValues();
			upd.put(f_name, scP.pname);
			upd.put(f_team_id, scP.pteam);
			upd.put(f_fpl_red_flag, scP.punavail);
			upd.put(f_fpl_yellow_flag, scP.pdoubt);
			if (scP.pnews != null) {
				upd.put(f_fpl_news, DbGen.compress(scP.pnews));
			}
			
			dbGen.update(t_player, upd, updWherePlayer, updArgsPlayer);
			return pid;
		}
		
		// find existing player and update with new 13/14 id
		/*Cursor c = dbGen.rawQuery("SELECT p._id FROM player p, player_season ps WHERE p.name = \"" + scP.pname + "\" AND ps.player_id = p._id"
				+ " AND ps.season = 13 AND ps.points = " + scP.ppts, null);
		c.moveToFirst();
		if (!c.isAfterLast()) {
			int id = c.getInt(0);
			App.log("found existing player NAME/PTS (id " + id + ") to assign new id for " + scP.pname);
			dbGen.execSQL("UPDATE player SET code_14 = " + scP.pimgcode + " WHERE _id = " + id);
			if (c.getCount() > 1) {
				App.log(" (HAS MORE MATCHES)");
			}
			c.close();
			return id;
		}
		c.close();*/
		
		// find existing player and update with new 13/14 id
		Cursor c = dbGen.rawQuery("SELECT _id FROM player WHERE name = \"" + scP.pname + "\" AND team_id = " + scP.pteam + " AND code_14 IS NULL", null);
		c.moveToFirst();
		if (!c.isAfterLast()) {
			int id = c.getInt(0);
			App.log("found existing player NAME/TEAM (id " + id + ") to assign new id for " + scP.pname);
			dbGen.execSQL("UPDATE player SET code_14 = " + scP.pimgcode + " WHERE _id = " + id);
			if (c.getCount() > 1) {
				App.log(" (HAS MORE MATCHES)");
			}
			c.close();
			return id;
		}
		c.close();
		
		App.log("inserting new p.. id: " + scP.pid + " name: " + scP.pname + " pos: " + scP.ppos + " team: " + scP.pteam);
		
		// insert new
		ContentValues newRec = new ContentValues(); 
		newRec.put(f_name, scP.pname);
		newRec.put(f_team_id, scP.pteam);
		newRec.put(f_fpl_yellow_flag, scP.pdoubt);
		newRec.put(f_fpl_red_flag, scP.punavail);
		newRec.put(f_fpl_news, scP.pnews);
		newRec.put(f_code_14, scP.pimgcode);
		return dbGen.insert(t_player, null, newRec);
	}
	
	private Pattern p_hist = Pattern.compile("\\[\\W+\"[0-9]+/([0-9]+)\",\\W+([0-9]+),\\W+([0-9]+),\\W+([0-9]+),\\W+([0-9]+),\\W+([0-9]+),\\W+([0-9]+),\\W+([0-9]+),"
            + "\\W+([0-9]+),\\W+([0-9]+),\\W+([0-9]+),\\W+([0-9]+),\\W+([0-9]+),\\W+([0-9]+),\\W+(-?[0-9]+),\\W+([0-9]+),\\W+(-?[0-9]+)\\W+]", Pattern.MULTILINE);
	private static final int H_SEASON = 1;
	private static final int H_MINS = 2;
	private static final int H_GOALS = 3;
	private static final int H_ASSISTS = 4;
	private static final int H_CS = 5;
	//private static final int H_CONC = 6;
	//private static final int H_OG = 7;
	//private static final int H_PENSAV = 8;
	//private static final int H_PENMISS = 9;
	private static final int H_YELLOW = 10;
	private static final int H_RED = 11;
	//private static final int H_SAVES = 12;
	private static final int H_BONUS = 13;
	//private static final int H_EASPORTS = 14;
	//private static final int H_BPS = 15;
	private static final int H_PRICE = 16;
	private static final int H_POINTS = 17;
	private static final int BEFORE_SEASON = 11;
	private static final String PS_WHERE_CLAUSE = "season = " + BEFORE_SEASON + " AND player_id = ?";
	private static final String P_WHERE_CLAUSE = "_id = ?";
	private String[] whereArgsHist = new String[1];
	private static final String f_got_history = "got_history";
	
	private static int get(MatchResult m, int index) {
		return Integer.valueOf(m.group(index));
	}
	
	private static final int WAIT_TIME_AFTER_FIXTURE_FINISHED = 60 * 15;
	
	public boolean getPlayerHist(Cursor cur, ScrapeMatchScoresCatchup smsn, ProcRes result, HashSet<Integer> gw_to_process) {
		int fpl_id = cur.getInt(cur.getColumnIndex("fpl_id"));
		int player_id = cur.getInt(cur.getColumnIndex("player_id"));
		int position = cur.getInt(cur.getColumnIndex("position"));
		int got_history = cur.getInt(cur.getColumnIndex("got_history"));
		int ps_points = cur.getInt(cur.getColumnIndex("points"));
		int pm_points = cur.getInt(cur.getColumnIndex("tot"));
		int team_id = cur.getInt(cur.getColumnIndex("team_id"));
		String name = cur.getString(cur.getColumnIndex("name"));
		int ps_minutes = cur.getInt(cur.getColumnIndex("minutes"));
		int pm_minutes = cur.getInt(cur.getColumnIndex("totmins"));
		
		boolean get_season_hist = (got_history != 1);
		boolean get_match_hist = (ps_points != pm_points) || (ps_minutes != pm_minutes);
		
		long currentUkTime = App.currentUkTime();
		if (get_match_hist) {
			for (GWFixture f : App.gwFixtures.values()) {
				if ( (f.team_home_id == team_id) || (f.team_away_id == team_id) ) {
					if (f.kickoff_datetime <= currentUkTime) {
						if (!f.complete) {
							App.log("Playing at the moment - won't do hist: " + name);
							get_match_hist = false;
							if (!get_season_hist) {
								return false;
							}
						// after a fixture is marked as finished, wait a period before updating player stats from it
						} else if ( (currentUkTime - f.marked_finished_datetime) < WAIT_TIME_AFTER_FIXTURE_FINISHED) { 
							App.log("Finished playing too recently - won't do hist: " + name);
							get_match_hist = false;
							if (!get_season_hist) {
								return false;
							}
						}
					}
				}
			}
		}
		
		boolean got_scores_since_install = (UpdateManager.updatedTimestamps[FPLService.PROC_SCRAPE_SCORES] != UpdateManager.NEVER_UPDATED);
		if (!got_scores_since_install && !get_season_hist) {
			App.log("not getting match hist as scores not got since install");
			return false;
		}
		
		String proc_name = "get player hist: " + fpl_id + " (" + name + ")" + " get_season_hist = " + get_season_hist
						+ " get_match_hist = " + get_match_hist;
		App.log("starting " + proc_name);
		long startTime = System.currentTimeMillis();
		Scanner scan = null;
		UrlObj con = null;
		
		boolean done_hist = false;
		
		try {
			con = URLUtil.getUrlStream(URL_PLAYER_HISTORY + fpl_id + "/", false, URLUtil.NO_AUTH);
			
			if (con.ok) {
				scan = new Scanner(con.stream);
				
				if (get_season_hist) {
					// team (player scores inside match)
					while (scan.findWithinHorizon(p_hist, 0) != null) {
						MatchResult m = scan.match();
						int s_season = get(m, H_SEASON);
						
						if (s_season < BEFORE_SEASON) {
							ContentValues newRec = new ContentValues();
							newRec.put(f_season, s_season);
							// for old seasons, store fpl_id as player_id (prevent pk violation)
			        		newRec.put(f_fpl_id, player_id);
			        		newRec.put(f_player_id, player_id);
			        		newRec.put(f_minutes, get(m, H_MINS));
			        		newRec.put(f_goals, get(m, H_GOALS));
			        		newRec.put(f_assists, get(m, H_ASSISTS));
			        		newRec.put(f_clean_sheets, get(m, H_CS));
			        		newRec.put(f_yellow, get(m, H_YELLOW));
			        		newRec.put(f_red, get(m, H_RED));
			        		newRec.put(f_bonus, get(m, H_BONUS));
			        		float s_price = ProcessData.trunc((float)get(m, H_PRICE) / 10, 1);
			        		newRec.put(f_price, s_price);
			        		newRec.put(f_points, get(m, H_POINTS));
			        		newRec.put(f_position, position);
			        		dbGen.insert(t_player_season, null, newRec);
			        	// 10/11 season; update red/cs
						} else if (s_season == BEFORE_SEASON) {
							ContentValues updRec = new ContentValues();
							updRec.put(f_red, get(m, H_RED));
							updRec.put(f_clean_sheets, get(m, H_CS));
							whereArgsHist[0] = String.valueOf(player_id);
							dbGen.update(t_player_season, updRec, PS_WHERE_CLAUSE, whereArgsHist);
						}
					}
					
					// mark player record as "got history"
					ContentValues updRec = new ContentValues();
					updRec.put(f_got_history, 1);
					whereArgsHist[0] = String.valueOf(player_id);
					dbGen.update(t_player, updRec, P_WHERE_CLAUSE, whereArgsHist);
				}
				
				if (get_match_hist) {
					smsn.getPlayerMatchHistCore(name, player_id, fpl_id, scan, result.context, gw_to_process);
					done_hist = true;
				}
			} else {
				App.log("player hist: url failed");
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
		
		Long timeDiff = System.currentTimeMillis() - startTime;
		float timeSecs = (float)timeDiff / 1000;
		App.log("done " + proc_name + " in " + timeSecs + " seconds");
		
		return done_hist;
	}
	
	
	//[1, 31513, 13531]
	// commaloc = 0, 2, 9, 17
	// 0 : 1-2
	// 1 : 4-9
	// 2 : 11-16
	private int getInt(String field) { 
		return Integer.parseInt(getString(field, false)); 
	}
	
	private float getFloat(String field) { 
		float res = 0;
		try {
			res = Float.parseFloat(getString(field, false));
		} catch (NumberFormatException e) {
			//
		}
		return res; 
	}
	
	private String getString(String field, boolean quotes) {
		//App.log("field: " + field);
		int index = stat2id.get(field);
		int from = commaLoc[index] + 2;
		int to = commaLoc[index+1];
		if (quotes) {
			from++;
			to--;
		}
		String s = player.substring(from, to);
		s = StringEscapeUtils.unescapeJava(s);
		//App.log(player.substring(from, to));
		return s;
	}
	
	private void changeLog(String field, String sc, String db, boolean ps) {
		if (ps) {
			player_season_update.put(field, sc);
		} else {
			if (field.equals(f_fpl_news) && (sc != null) ) {
				player_update.put(field, DbGen.compress(sc.toString()));
			} else {
				player_update.put(field, sc);
			}
		}
		App.log("---- " + scP.pname + " (" + scP.pid + ") -------" + field + ": '" + db + "' -> '" + sc + "'");
	}
	private void changeCheckLog(String field, int sc, int db, boolean ps) {
		if (sc != db) {
			if (ps) {
				player_season_update.put(field, sc);
			} else {
				player_update.put(field, sc);
			}
			App.log("---- " + scP.pname + " (" + scP.pid + ") -------" + field + ": '" + db + "' -> '" + sc + "'");
		}
	}
	/*
	private void changeCheckLog(String field, float sc, float db, boolean ps) {
		if (sc != db) {
			if (ps) {
				player_season_update.put(field, sc);
			} else {
				player_update.put(field, sc);
			}
			App.log("---- " + scP.pname + " (" + scP.pid + ") -------" + field + ": '" + db + "' -> '" + sc + "'");
		}
	}
	*/
	
	public static class PlayerDetails {
		public int pid;
		public String pname;
		//public String pfirstname;
		public int ppos;
		public int pteam;
		public int pimgcode;
		public int pgoal;
		public int pass;
		public int pbon;
		public int pprice;
		public int ppts;
		public int pminutes;
		public int pform;
		public int pppg;
		public int pyel;
		public int punavail;
		public int pdoubt;
		public String pnews;
		public int pprice_start;
		public int ea_ppi;
		public int p_cs;
		public int owned_perc;
		public int ti_gw;
		public int to_gw;
		public int nti_gw;
	}
	
}
