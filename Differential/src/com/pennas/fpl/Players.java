package com.pennas.fpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.pennas.fpl.process.ProcessData;
import com.pennas.fpl.process.ProcessPlayer;
import com.pennas.fpl.ui.Pie;
import com.pennas.fpl.ui.Ticker;
import com.pennas.fpl.ui.Worm;
import com.pennas.fpl.ui.Ticker.TickerData;
import com.pennas.fpl.util.BitmapCache;
import com.pennas.fpl.util.DbGen;
import com.pennas.fpl.util.FPLAsyncTask;
import com.pennas.fpl.util.Refreshable;
import com.pennas.fpl.util.Screenshot;
import com.pennas.fpl.util.SquadUtil;
import com.pennas.fpl.util.FPLActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Players extends FPLActivity implements OnLongClickListener, Refreshable {
	// persistent cursor for listview
	protected Cursor cur;
	// filter properties
	private int player_position = 0;
	private int club = 0;
	private int sort = 0;
	private int min_games = 0;
	private int traffic = 0;
	private int team = 0;
	private float cur_max_price;
	private boolean watchlist = false;
	
	private static final int DIALOG_STAT_ID = 0;
	
	private static final int PLAYER_PIC_INITIAL_CACHE_SIZE = 18;
	
	// number of visible stats
	private static final int num_stats = 4;
	private String[] use_stats;
	private String[] use_stats_a = { "minutes", "c_avg_mins_recent", "c_ppg_x_mins", "c_pp90" };
	private String[] use_stats_b = { "c_g_over_y", "c_g_over_z", "pred_left_value", "c_value_ppg_x" };
	private int using_stats;
	private String long_clicked_stat;
	ArrayList<String> avail_stats, avail_stat_labs;
	
	private void loadStatFields() {
		String statFields = Settings.getStringPref(Settings.PREF_PLAYERS_FIELDS);
		if (statFields != null) {
			String[] split = statFields.split(",");
			for (int i=0; i<split.length; i++) {
				if (i < num_stats) {
					use_stats_a[i] = split[i];
				} else {
					use_stats_b[i - num_stats] = split[i];
				}
			}
		}
	}
	
	private void saveStatFields() {
		StringBuffer statFields = new StringBuffer();
		for (int i=0; i<num_stats; i++) {
			if (i > 0) statFields.append(",");
			statFields.append(use_stats_a[i]);
		}
		for (int i=0; i<num_stats; i++) {
			statFields.append("," + use_stats_b[i]);
		}
		Settings.setStringPref(Settings.PREF_PLAYERS_FIELDS, statFields.toString(), this);
	}
	
	// ids for xml lookup
	public final static int[] fieldIds = { R.id.stat0, R.id.stat1, R.id.stat2, R.id.stat3 };
	public final static int[] fieldLabs = { R.id.lab0, R.id.lab1, R.id.lab2, R.id.lab3 };
	public static float max_price, min_price;
	TextView seekLab;
	private boolean doPopList = false;
	private ListView listViewRef;
	
	// number of entries returned for listview
	private static final int QUERY_LIMIT = 45;
	
	private float max_score;
	
	// basic constant
	public static final int NUM_TEAMS = 20;
	private int[]    team_ids   = new int[NUM_TEAMS + 1];
	private String[] team_names = new String[NUM_TEAMS + 1];
	private static final int TEAM_ALL = -1;
	
	private float values[];
	private String value_labs[];
	
	private static final int static_sort_fields = 2;
	private String[] sort_fields = new String[static_sort_fields + num_stats + num_stats];
	private String[] sort_names  = new String[static_sort_fields + num_stats + num_stats];
	
	// intent params
	public static final String P_STATID = "com.pennas.fpl.statid";
	public static final String P_POSITION = "com.pennas.fpl.position";
	public static final String P_TRANSFER = "com.pennas.fpl.transfer";
	public static final String P_PRICE = "com.pennas.fpl.price";
	public static final String P_SQUAD_STRING = "com.pennas.fpl.squad_string";
	public static final String P_TEAMS_STRING = "com.pennas.fpl.teams_string";
	public static final String P_PLAYER_OUT = "com.pennas.fpl.player_out";
	public static final String P_SEASON = "com.pennas.fpl.season";
	public static final String P_TEAM = "com.pennas.fpl.team";
	
	private static float rating_high, rating_low;
	
	private static final int TRAFFIC_FIXDIFF = 0;
	private static final int TRAFFIC_PREDSCORE = 1;
	private static final int TRAFFIC_PREDCS = 2;
	
	// high/low values for traffic light colour processing
	public static final float RATING_LOW_PRED = -0.3f;
	public static final float RATING_HIGH_PRED = 8;
	public static final float RATING_LOW_FIX = 0;
	public static final float RATING_HIGH_FIX = 4.5f;
	
	private StringBuffer wl_string;
	private String squad_string, teams_string;
	
	private boolean c_transfer;
	public static final String[] position_text = { "G", "D", "M", "F" };
	public static final String[] position_text_exp = { "Goalkeeper", "Defender", "Midfielder", "Forward" };
	private String[] position_array;
	
	private int season = App.season;
	private int stat_season = season;
	private boolean season_minus = false;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Debug.startMethodTracing("fpl_players_6");
        
        // load stat choices from prefs
	    loadStatFields();
	    
	    actionBar.setTitle("Players");
	    
	    c_transfer = false;
        int p_position = 0;
        float p_price = 0;
        String p_player_out = null;
        // process intent params
        Bundle extras = getIntent().getExtras();
        boolean setMinDropDown = false;
	    if (extras != null) {
	        if (extras.containsKey(P_STATID)) {
		    	use_stats_a[2] = extras.getString(P_STATID);
		    	sort = 4;
		    	App.log("P_STATID " + use_stats_a[2]);
		    }
	        if (extras.containsKey(P_SEASON)) {
	        	stat_season = extras.getInt(P_SEASON);
	        	App.log("P_SEASON " + stat_season);
		    }
	        if (extras.containsKey(P_TRANSFER)) {
		    	c_transfer = extras.getBoolean(P_TRANSFER);
		    	p_position = extras.getInt(P_POSITION);
		    	p_price = extras.getFloat(P_PRICE);
		    	squad_string = extras.getString(P_SQUAD_STRING);
		    	p_player_out = extras.getString(P_PLAYER_OUT);
		    	teams_string = extras.getString(P_TEAMS_STRING);	
		    	App.log("squad_string: " + squad_string);
		    	App.log("teams_string: " + teams_string);
		    }
	        if (extras.containsKey(P_TEAM)) {
	        	team = extras.getInt(P_TEAM);
	        	App.log("P_TEAM " + team);
		    }
	    }
	    
	    if ( (stat_season == App.season) && (App.cur_gameweek < 1) ) {
	    	stat_season = season - 1;
	    	season_minus = true;
	    }
	    
        min_games = Settings.getIntPref(Settings.PREF_MIN_GAMES_FILTER);
	    if (min_games > 0) setMinDropDown = true;
	    
	    display_mode = Settings.getIntPref(Settings.PREF_PLAYERS_VIEW_TYPE);
        
	    // default set of visible stats
        use_stats = use_stats_a;
        using_stats = 1;
        
        setContentView(R.layout.players_screen);
        
        // GW0: show last seasons stats
        // season filter: populate (static)
		addFilter(FILTER_SEASON, "Season", Player.season_desc, Player.getSeasonInd(stat_season), true, false);
        
        hasTips = true;
        // transfer mode
		if (c_transfer) {
			tipsPref = Settings.PREF_TIPS_PLAYERS_TRANSFER;
			tips.add("Use the filter icon at the top to narrow down your selection");
			tips.add("Click the star to filter by your watchlist");
			tips.add("Select a player to transfer them in to your draft team");
			tips.add("Players are limited by price/team/position depending on who you are transferring out");
		// normal mode
		} else {
			tipsPref = Settings.PREF_TIPS_PLAYERS;
			tips.add("Use the filter icon at the top to narrow down your selection");
			tips.add("Click the star to filter by your watchlist");
			tips.add("'Worm' shows player points breakdown (App/CS, Goal, Assist, Bonus Pts)");
			tips.add("Select a player to view more details on them");
			tips.add("Switch to an alternative view from menu");
			tips.add("'Pie' shows breakdown of bad/good/excellent games");
			tips.add("Click on a stat on the right for a description");
			tips.add("Long-Click on a stat on the right to choose another stat to display there");
			tips.add("Switch between two sets of stats using the 'Switch' button");
			tips.add("Show different Traffic Lights (mini ticker) for player using dropdown");
			tips.add("View stats from previous seasons (not all stats available) using the dropdown");
			tips.add("Players in your current confirmed squad are highlighted in green");
			tips.add("Star icon indicates player in watchlist");
		}
        
        if (!c_transfer) {
        	RelativeLayout rl = (RelativeLayout) findViewById(R.id.transferMode);
	    	rl.setVisibility(View.GONE);
	    } else {
	    	TextView t = (TextView) findViewById(R.id.transferModeRight);
	    	t.setText(p_player_out);
	    }
        
        listViewRef = (ListView) findViewById(android.R.id.list);
        getMinMax(App.dbGen);
	 	
	 	// init list of values for price Spinner
	 	float min = min_price / 10f;
	 	float max = max_price / 10f;
	 	if (c_transfer) {
	 		float new_max = p_price;
	 		// if spending cash is less than value of most expensive player,
	 		// then set max to an increment of cash+1 but default value to cash
	 		while (new_max < max) {
	 			new_max++;
	 		}
	 		max = new_max;
	 	}
	 	
	 	if (c_transfer) {
	 		cur_max_price = p_price;
	 	} else {
	 		cur_max_price = max;
	 	}
	 	
	 	App.log("max: " + max + " min: " + min + " default: " + cur_max_price);
	 	
	 	int diff = (int)max - (int)min;
	 	values = new float[diff + 1];
	 	value_labs = new String[diff + 1];
	 	for (int i=0; i<=diff; i++) {
	 		//float val = ProcessData.trunc(max, 1);
	 		values[i] = max;
	 		//value_labs[i] = String.valueOf(max);
	 		value_labs[i] = String.format("%.1f", max);
	 		max -= 1;
	 	}
	 	
	 	int cur_max_price_int = (int) (cur_max_price * 10);
	 	// set default value for transfer mode (as current available spend)
	    int sel = 0;
	    if (c_transfer) {
	    	for (int i=0; i<values.length; i++) {
	    		int val_int = (int) (values[i] * 10);
	    		if (val_int == cur_max_price_int) {
	    			sel = i;
	    		}
	    	}
	    }
	    addFilter(FILTER_VALUE, "Price", value_labs, sel, true, false);
	 	
	    position_array = getResources().getStringArray(R.array.positions_array);
	    // posFilter: populate (static)
		if (c_transfer) {
	    	player_position = p_position;
	    } else {
	    	addFilter(FILTER_POSITION, "Position", position_array, sel, true, false);
	    }
	    
	    // trafficFilter: populate (static)
	    traffic = Settings.getIntPref(Settings.PREF_PLAYERS_TRAFFIC);
		addFilter(FILTER_TRAFFIC, "Traffic Lights", R.array.traffic_array, traffic, true, false);
	    
	    // minFilter: populate (static)
		sel = 0;
	    if (setMinDropDown) {
	    	for (int i=0; i<Stats.game_nums.length; i++) {
	    		if (Stats.game_nums[i] == min_games) {
	    			sel = i;
	    		}
	    	}
	    }
	    addFilter(FILTER_MIN, "Minimum Games", Stats.dropdown, sel, true, false);
	    
	    // clubFilter - get clubs list (add watchlist and "all" to start)
	    Cursor clubs = App.dbGen.rawQuery("SELECT _id, name FROM team WHERE active = 1 ORDER BY name ASC", null);
	    clubs.moveToFirst();
	    team_ids[0]   = TEAM_ALL;
	    team_names[0] = "All";
	    int tind = 1;
	    while (clubs.isAfterLast() == false) {
            team_ids[tind] = clubs.getInt(0);
            team_names[tind] = clubs.getString(1);
       	    
            clubs.moveToNext();
       	    tind++;
        }
	 	clubs.close();
	    sel = 0;
	    if (team > 0) {
	    	for (int i=0; i<team_names.length; i++) {
	    		if (team_ids[i] == team) {
	    			sel = i;
	    			club = i;
	    			//App.log("using team " + sel + ": " + team_names[i]);
	    		}
	    	}
	 	}
	    addFilter(FILTER_CLUB, "Team", team_names, sel, true, false);
	    
	    // load stat types, desc, etc for lookup
	    loadStats();
	    
	    // sort dropdown
	    initSortFilter();
	    
	    // main list populate (dynamic)
	    doPopList = true;
	    App.log("k");
    	popList();
	 	
	 	//Debug.stopMethodTracing();
    }
    
    // stat details
    public static class Stat {
    	public String db_field;
    	public int type;
    	public String short_lab;
    	public String desc;
    	public boolean show_value;
    	public boolean show_in_prefs;
    	public int always_show_sort;
    	public float divide;
    }
    public static HashMap<String, Stat> playerstats;
    // also store as array (ordered, for things that can't sort)
    public static ArrayList<Stat> playerstats_array;
    
    private static final String C_G_OVER_Y = "c_g_over_y";
    private static final String C_G_OVER_Z = "c_g_over_z";
    
    // load stat defs into memory
    public synchronized static void loadStats() {
    	if ( (playerstats != null) && (playerstats_array != null) ) return;
    	
    	playerstats = new HashMap<String, Stat>();
    	playerstats_array = new ArrayList<Stat>();
    	
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.appContext);
    	
    	Cursor curStat = App.dbGen.rawQuery("SELECT db_field, type, short_lab, desc, show_value, always_show_sort, divide FROM stat WHERE player_stat = 1 ORDER BY desc ASC", null);
    	curStat.moveToFirst();
    	while (!curStat.isAfterLast()) {
    		Stat s = new Stat();
    		s.db_field = curStat.getString(0);
    		s.type = curStat.getInt(1);
    		s.short_lab = curStat.getString(2);
    		s.desc = curStat.getString(3);
    		s.show_value = (curStat.getInt(4) == 1);
    		s.show_in_prefs = s.show_value;
    		s.always_show_sort = curStat.getInt(5);
    		s.divide = curStat.getInt(6);
    		
    		if (s.db_field.equals(C_G_OVER_Y)) {
    			s.short_lab = s.short_lab + ProcessData.y_points;
    			s.desc = s.desc + ProcessData.y_points;
    		}
    		if (s.db_field.equals(C_G_OVER_Z)) {
    			s.short_lab = s.short_lab + ProcessData.z_points;
    			s.desc = s.desc + ProcessData.z_points;
    		}
    		
    		// check if preferences say that this stat should be displayed
    		// PREF CHANGE LISTENER NEEDS TO SET playerstats TO NULL WHEN PREFS CHANGE
    		if (s.show_value) {
    			if (!prefs.getBoolean(Settings.PREF_PREFIX_PLAYER_STAT + s.db_field, true)) {
    				s.show_value = false;
    			}
    		}
    		
    		playerstats.put(s.db_field, s);
    		playerstats_array.add(s);
    		//AppClass.log("Add " + s.db_field + " -> " + s.desc);
    		curStat.moveToNext();
    	}
    	curStat.close();
    }
    
    private static final int SORT_PRICE = 1;
    
    // populate sort dropdown
    protected void initSortFilter () {
    	sort_fields[0] = "points";
	    sort_names[0]  = "Points";
	    sort_fields[1] = "price";
	    sort_names[1]  = "Price";
	    for (int i=0; i<num_stats; i++) {
	    	Stat s = playerstats.get(use_stats_a[i]);
	    	sort_fields[i+static_sort_fields] = s.db_field;
	    	sort_names[i+static_sort_fields] = s.desc;
	    }
	    for (int i=0; i<num_stats; i++) {
	    	Stat s = playerstats.get(use_stats_b[i]);
	    	sort_fields[i+static_sort_fields+num_stats] = s.db_field;
		    sort_names[i+static_sort_fields+num_stats] = s.desc;
	    }
	    
	    addFilter(FILTER_SORT, "Sort", sort_names, sort, true, false);
    }
    
    // listener for stat desc toasts
    public void clickStat(View v) {
    	loadStats();
    	int id = v.getId();
    	for (int i=0; i<num_stats; i++) {
    		if (id == fieldIds[i] || id == fieldLabs[i]) Toast.makeText(v.getContext(), playerstats.get(use_stats[i]).desc, Toast.LENGTH_LONG).show();
    	}
    }
    
    // find min/max season price/score, for worm/filter bounds
    private void getMinMax(SQLiteDatabase db) {
	    // get max/min price in db for slider
	 	Cursor minmax = db.rawQuery("SELECT MIN(price) min_price, MAX(price) max_price FROM player_season WHERE season = " + season, null);
	 	minmax.moveToFirst();
	 	min_price = minmax.getInt(0);
	 	max_price = minmax.getInt(1);
	 	minmax.close();
	 	
	 	// in case GW0, get points from last season
	 	minmax = db.rawQuery("SELECT MAX(points) score FROM player_season WHERE season = " + stat_season, null);
	 	minmax.moveToFirst();
	 	max_score = minmax.getFloat(0);
	 	minmax.close();
    }
    
    private BitmapCache bmpOther = new BitmapCache(this);
    private BitmapCache bmpPlayer = new BitmapCache(this, true);
    private HashSet<Integer> picsOther;
    private HashSet<Integer> picsPlayer;
    
    private StringBuilder filter_string = null;
    private void add_to_filter_string(String s) {
    	if (filter_string == null) {
    		filter_string = new StringBuilder();
    		filter_string.append(s);
    	} else {
    		filter_string.append(", " + s);
    	}
    }
    
    // async task to load data
    private class LoadTask extends FPLAsyncTask<Integer, Void, Cursor> {
    	ProgressDialog dialog;
    	
    	protected Cursor doInBackground(Integer... params) {
			SQLiteDatabase db = App.dbGen;
			int c_season = params[0];
			
			Cursor curLoad;
			getMinMax(db);
			
			// commented out: work out scores for worm bounds. currently uses constants
	 		//private static float rating_high;
			//private static float rating_low;
			/*Cursor curHighLow = null;
			if (traffic == TRAFFIC_PREDSCORE) {
				curHighLow = db.rawQuery("SELECT MIN(pred_total_pts) low, MAX(pred_total_pts) high FROM player_match WHERE season = " + season + " AND gameweek >= " + next_gameweek + " AND gameweek <= " + upcom_gw_last, null);
			} else if (traffic == TRAFFIC_FIXDIFF) {
				curHighLow = db.rawQuery("SELECT MIN(MIN(pred_ratio_home),MIN(pred_ratio_away)) low, MAX(MAX(pred_ratio_home),MAX(pred_ratio_away)) high FROM fixture WHERE season = " + season + " AND gameweek >= " + next_gameweek + " AND gameweek <= " + upcom_gw_last, null);
			} else if (traffic == TRAFFIC_PREDCS) {
				curHighLow = db.rawQuery("SELECT MIN(MIN(pred_goals_home),MIN(pred_goals_away)) low, MAX(MAX(pred_goals_home),MAX(pred_goals_away)) high FROM fixture WHERE season = " + season + " AND gameweek >= " + next_gameweek + " AND gameweek <= " + upcom_gw_last, null);
			}
			curHighLow.moveToFirst();
			rating_low = curHighLow.getFloat(0);
			rating_high = curHighLow.getFloat(1);
			curHighLow.close();*/
			
			if (cur_max_price < (max_price/10f)) {
				add_to_filter_string("£" + ProcessData.trunc(cur_max_price, 1));
			}
			
			// start to build query string
	 		StringBuffer where_clause = new StringBuffer();
	 		where_clause.append("ps.player_id = p._id AND ps.season = " + c_season);
		 	if (player_position > 0) {
		 		where_clause.append(" AND ps.position = " + player_position);
		 		add_to_filter_string(position_array[player_position]);
		 	}
		 	where_clause.append(" AND ps.price <= " + (cur_max_price * 10));
    		
		 	// watchlist
		 	if (watchlist) {
		 		where_clause.append(" AND ps.fpl_id IN (" + wl_string + ")");
		 		add_to_filter_string("watchlist");
		 	}
		 	
		 	if (club > 0) {
		 		where_clause.append(" AND p.team_id = " + team_ids[club]);
		 		add_to_filter_string(team_names[club]);
		 	}
		 	
		 	if (min_games > 0) {
		 		where_clause.append(" AND pls.minutes_qual >= " + min_games);
		 		int games = min_games / 90;
		 		add_to_filter_string(games + " games");
			}
		 	
		 	if (c_transfer) {
		 		where_clause.append(" AND ps.fpl_id NOT IN (" + squad_string + ")");
		 		if (teams_string != null) {
		 			where_clause.append(" AND p.team_id NOT IN (" + teams_string + ")");
		 		}
		 	}
		 	
		 	if (c_season != App.season) {
		 		for (int i=0; i<Player.seasons.length; i++) {
		 			if (Player.seasons[i] == c_season) {
		 				add_to_filter_string(Player.season_desc[i]);
		 				break;
		 			}
		 		}
		 	}
		 	
		 	// hack: sort by cur season if price, old/default for everything else
		 	String sort_ss = "pls";
		 	if (sort == SORT_PRICE) sort_ss = "ps";
		 	
		 	String order_by = " ORDER BY "+sort_ss+"." + sort_fields[sort] + " DESC";
		 	String select_fields = "";
		 	
		 	for (int i=0; i<num_stats; i++){
		 		Stat s = playerstats.get(use_stats_a[i]);
		 		select_fields += ", pls." + s.db_field + " " + s.db_field;
		 		
		 		s = playerstats.get(use_stats_b[i]);
		 		select_fields += ", pls." + s.db_field + " " + s.db_field;
		 	}
		 	
		 	// run query for listview
		 	String query = "SELECT p._id _id, p.name name, p.team_id team_id, ps.position position, p.fpl_yellow_flag fpl_yellow_flag"
		 		+ ", p.fpl_red_flag fpl_red_flag, p.diff_flag diff_flag, ps.price price, pls.points points"
		 		+ ", p.code_14 code_14, (pls.assists * 3) assist_pts, pls.bonus bonus_pts"
		 		+ ", pls.c_games_x_mins cgox, pls.c_g_over_y cgoy, pls.c_g_over_z cgoz"
		 		+ ", (CASE WHEN pls.position <= 2 THEN pls.goals * 6 WHEN pls.position = 3 THEN pls.goals * 5 WHEN pls.position = 4 THEN pls.goals * 4 END) goal_pts"
		 		+ select_fields + ", p.ticker_string, ps.fpl_id"
		 		+ " FROM player p, player_season ps"
		 		+ " LEFT OUTER JOIN player_season pls ON pls.season = " + stat_season + " AND pls.player_id = p._id"
		 		+ " WHERE " + where_clause
		 		+ order_by 
		 		+ " LIMIT " + QUERY_LIMIT;
		 	curLoad = db.rawQuery(query, null);
	    	
	    	// this is the step that actually takes the time - do it in async
		 	curLoad.moveToFirst();
		 	
		 	picsOther = new HashSet<Integer>();
		 	picsPlayer = new HashSet<Integer>();
		 	picsOther.add(R.drawable.btn_star_big_on);
		 	picsOther.add(R.drawable.yellow_flag);
		 	picsOther.add(R.drawable.red_flag);
		 	picsOther.add(R.drawable.diff_flag);
		 	
		 	int i_pic_code = curLoad.getColumnIndex("code_14");
		 	int i_team_id = curLoad.getColumnIndex("team_id");
		 	int i = 0;
		 	while(!curLoad.isAfterLast()) {
		 		if (display_mode == DISPLAY_FULL && (i < PLAYER_PIC_INITIAL_CACHE_SIZE) ) {
			 		int pic_res = getPlayerPic(Players.this, curLoad.getInt(i_pic_code));
			 		picsPlayer.add(pic_res);
		 		}
		 		int pic_res = SquadUtil.getShirtResource(curLoad.getInt(i_team_id));
		 		picsOther.add(pic_res);
		 		
		 		curLoad.moveToNext();
		 		i++;
		 	}
		 	
		 	curLoad.moveToFirst();
		 	
		 	return curLoad;
        }
		protected void onPreExecute() {
			dialog = new ProgressDialog(Players.this);
			dialog.setMessage("Loading..");
			dialog.setCancelable(false);
			dialog.show();
			Players.this.progress = dialog;
			loadStats();
		    if (cur != null) {
		    	cur.close();
		    }
		}
        protected void onPostExecute(Cursor result) {
        	Players.this.cur = result;
        	Players.this.populateCallback();
        	dialog.dismiss();
        	loader = null;
        }
    }
    
    private LoadTask loader;
    
    // init or redo cursor->list binding for main player list
    private void popList() {
	 	if (doPopList) {
	 		App.log("Players::popList()");
	 		
	 		// build "where in" string for watchlist
		    wl_string = new StringBuffer();
	 		Iterator<Integer> i = App.watchlist.iterator();
	 		boolean first = true;
	 		while (i.hasNext()) {
	 			if (first) {
	 				wl_string.append(i.next());
	 				first = false;
	 			} else {
	 				wl_string.append(", " + i.next());
	 			}
	 		}
	 		
	 		// run load in background (displaying progress dialog)
	 		loader = new LoadTask();
			loader.fplExecute(season, ProcessData.next_x_games, App.next_gameweek);
	 	}
    }
    
    // called when async load task finishes: populate GUI from data
    protected void populateCallback() {
    	bmpOther.refreshWith(picsOther);
    	bmpPlayer.refreshWith(picsPlayer);
    	
    	switch (traffic) {
		case TRAFFIC_PREDSCORE:
			rating_low = RATING_LOW_PRED;
			rating_high = RATING_HIGH_PRED;
			break;
		case TRAFFIC_FIXDIFF:
			rating_low = RATING_LOW_FIX;
			rating_high = RATING_HIGH_FIX;
			break;
		case TRAFFIC_PREDCS:
			rating_low = Teams.CS_LOW;
			rating_high = Teams.CS_HIGH;
			break;
    	}
	
    	PlayersCursorAdapter pAdapter = new PlayersCursorAdapter(this, cur);
		listViewRef.setAdapter(pAdapter);
		OnItemClickListener listListener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
	            if (c_transfer) {
	            	Intent resultIntent = new Intent();
	            	Selection.transferPlayerID = (int) id;
	            	setResult(Activity.RESULT_OK, resultIntent);
	            	finish();
	            } else {
	            	// load player screen for player "id"
					Intent intent = new Intent(view.getContext(), Player.class);
					intent.putExtra(Player.P_PLAYERID, (int) id);
					int season_use = stat_season;
					if (season_minus) {
						season_use++;
					}
					intent.putExtra(Player.P_SEASON, season_use);
			    	startActivity(intent);
	            }
			}
	    };
		listViewRef.setOnItemClickListener(listListener);
		
		String ss_string;
		if (filter_string == null) {
			ss_string = "";
		} else {
			ss_string = ", filtered by " + filter_string;
		}
		int max_ss_entries = SCREENSHOT_LISTVIEW_COUNT_LARGE_ITEMS;
		if (display_mode == DISPLAY_SMALLER) {
			max_ss_entries = Screenshot.NOT_SET;
		}
		setScreenshotView(listViewRef, "Players" + ss_string + " for season "
				+ Player.season_desc[Player.getSeasonInd(stat_season)] + ", sorted by " + sort_names[sort], max_ss_entries);
		
		actionBar.setSubtitle(filter_string);
		filter_string = null;
    }
    
    public static final int SCREENSHOT_LISTVIEW_COUNT_LARGE_ITEMS = 8;
    
    // dialog shown when long-click on stat: choose stat for GUI
    protected Dialog onCreateDialog(int id) {
        Dialog new_dialog;
        switch(id) {
        case DIALOG_STAT_ID:
        	avail_stats = new ArrayList<String>();
        	avail_stat_labs = new ArrayList<String>();
        	
        	boolean add;
        	// iterate all stats
        	for (Stat s : playerstats_array) {
        		if (s.show_value) {
	        		add = true;
	        		// if stat not already shown (apart from the one being replaced)
	        		for (int a=0; a<num_stats; a++) {
	        			if ( (use_stats[a].equals(s.db_field)) && (!s.db_field.equals(long_clicked_stat)) ) {
	        				add = false;
	        			}
	        		}
	        		// add to dialogue list
	        		if (add) {
	        			avail_stats.add(s.db_field);
	        			avail_stat_labs.add(s.desc);
	        		}
        		}
        	}
        	
        	// build dialogue from array
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	builder.setTitle("Pick a color");
        	String[] items = new String[avail_stat_labs.size()];
        	for (int i=0; i<avail_stat_labs.size(); i++) {
        		items[i] = avail_stat_labs.get(i);
        	}
        	// listener
        	builder.setItems(items, new DialogInterface.OnClickListener() {
        	    public void onClick(DialogInterface dialog, int item) {
        	        String stat = avail_stats.get(item);
        	    	if (stat != long_clicked_stat) {
	        	        // update stat group
        	    		if (using_stats == 1) {
	        	        	for (int i = 0; i<num_stats; i++) {
	        	        		if (use_stats_a[i] == long_clicked_stat) {
	        	        			use_stats_a[i] = stat;
	        	        		}
	        	        	}
	        	        	use_stats = use_stats_a;
	        	        } else {
	        	        	for (int i = 0; i<num_stats; i++) {
	        	        		if (use_stats_b[i] == long_clicked_stat) {
	        	        			use_stats_b[i] = stat;
	        	        		}
	        	        	}
	        	        	use_stats = use_stats_b;
	        	        }
        	    		
	        	        //int oldSort = sort;
	        	        
	        	        // repopulate stuff
        	    		doPopList = false;
	        	        initSortFilter();
	        	        //sortSpinner.setSelection(oldSort, true);
	        	        
	        	        doPopList = true;
	        	        App.log("j");
	        	        
	        	        // save choices
	        	        saveStatFields();
	        	        
	        	    	popList();
        	    	}
        	    }
        	});
        	new_dialog = builder.create();
        	new_dialog.setTitle("Select stat");
            break;
        default:
            new_dialog = null;
        }
        if (new_dialog == null) {
        	return super.onCreateDialog(id);
        }
        return new_dialog;
    }
    
    @Override
	protected void filterSelection(int id, int pos) {
		App.log("subclass");
    	
    	switch(id) {
	    	case (FILTER_CLUB):
	    		if (pos != club) {
	    			club = pos;
	    			App.log("i");
	    	    	popList();
	    		}
	    		break;
	    	case (FILTER_SORT):
	    		if (pos != sort) {
		        	  	sort = pos;
		        	  	App.log("h");
		        	  	popList();
	    		}
	    		break;
	    	case (FILTER_POSITION) :
	    		if (pos != player_position) {
	    			player_position = pos;
	    			App.log("g");
	    	    	popList();
	          	}
	    		break;
	    	case (FILTER_VALUE) :
	    		if (values[pos] != cur_max_price) {
	    			cur_max_price = values[pos];
	    			App.log("f");
	    	    	popList();
	    		}
	    		break;
	    	case (FILTER_TRAFFIC) :
	    		if (pos != traffic) {
	    			traffic = pos;
	    			Settings.setIntPref(Settings.PREF_PLAYERS_TRAFFIC, traffic, Players.this);
	    			redrawList();
	    		}
	    		break;
	    	case (FILTER_MIN) :
	    		if (min_games != Stats.game_nums[pos]) {
	        		min_games = Stats.game_nums[pos];
	        		Settings.setIntPref(Settings.PREF_MIN_GAMES_FILTER, min_games, Players.this);
	        		App.log("minFilter");
	        		popList();
	    		}
	    		break;
	    	case (FILTER_SEASON) :
	    		if (stat_season != Player.seasons[pos]) {
	    			stat_season = Player.seasons[pos];
	    			App.log("season");
	    	    	popList();
	        	}
	    		break;	
    	}
	}
    
    private static final int FILTER_CLUB = 1;
    private static final int FILTER_SORT = 2;
    private static final int FILTER_POSITION = 3;
    private static final int FILTER_VALUE = 4;
    private static final int FILTER_TRAFFIC = 5;
    private static final int FILTER_MIN = 6;
    private static final int FILTER_SEASON = 7;
    
    private static final int DISPLAY_FULL = 0;
    private static final int DISPLAY_SMALLER = 1;
    private static final int[] display_resources = { R.layout.players_screen_item, R.layout.players_screen_item_smaller };
    private int display_mode;
    
    public static int getPlayerPic(Context c, int picture_code) {
    	int res = c.getResources().getIdentifier("player" + picture_code, "drawable", c.getPackageName());
    	//App.log("getPlayerPic " + picture_code + " = " + res);
    	// blank picture
    	if (res == 0) return R.drawable.player_none;
    	return res;
    }
    
    //public static final int HIGHLIGHT_COLOUR = 0xff003300;
    //public static final int HIGHLIGHT_COLOUR_AUTOIN = 0xff002700;
    //public static final int NORMAL_COLOUR = 0xff000000;
    //public static final int BENCH_COLOUR = 0xff330000;
    
    private class PlayersCursorAdapter extends CursorAdapter {
    	private int i_goal_pts, i_bonus_pts, i_assist_pts, i_picture_code, i_fpl_yellow_flag, i_fpl_red_flag;
    	private int i_diff_flag, i_team_id, i_points, i_ticker_string, i_fpl_id, i_name, i_price, i_position;
    	private int i_cgox, i_cgoy, i_cgoz;
    	private boolean loaded_indexes;
    	
		public PlayersCursorAdapter(Context context, Cursor c) {
			super(context, c);
		}
    	
		private void loadIndexes(Cursor cursor) {
        	loaded_indexes = true;
        	i_goal_pts = cursor.getColumnIndex("goal_pts");
        	i_bonus_pts = cursor.getColumnIndex("bonus_pts");
        	i_assist_pts = cursor.getColumnIndex("assist_pts");
        	i_picture_code = cursor.getColumnIndex("code_14");
        	i_fpl_yellow_flag = cursor.getColumnIndex("fpl_yellow_flag");
        	i_fpl_red_flag = cursor.getColumnIndex("fpl_red_flag");
        	i_diff_flag = cursor.getColumnIndex("diff_flag");
        	i_team_id = cursor.getColumnIndex("team_id");
        	i_points = cursor.getColumnIndex("points");
        	i_ticker_string = cursor.getColumnIndex("ticker_string");
        	i_fpl_id = cursor.getColumnIndex("fpl_id");
        	i_name = cursor.getColumnIndex("name");
        	i_position = cursor.getColumnIndex("position");
        	i_price = cursor.getColumnIndex("price");
        	i_cgox = cursor.getColumnIndex("cgox");
        	i_cgoy = cursor.getColumnIndex("cgoy");
        	i_cgoz = cursor.getColumnIndex("cgoz");
        }
		
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// load column indexes if required
			if (!loaded_indexes) loadIndexes(cursor);
			
			TextView t;
			ImageView i;
			// shirt pic
			i = (ImageView) view.findViewById(R.id.shirtPic);
			int picResource = SquadUtil.getShirtResource(cursor.getInt(i_team_id));
			i.setImageBitmap(bmpOther.getBitmap(picResource));
			// player pic
			if (display_mode == DISPLAY_FULL) {
				i = (ImageView) view.findViewById(R.id.playerPic);
				picResource = getPlayerPic(context, cursor.getInt(i_picture_code));
				i.setImageBitmap(bmpPlayer.getBitmap(picResource));
				//i.setImageBitmap(g.getReflection(picResource, null, false, GraphicUtil.NO_SCORE, true, false));
			}
			// worm
			Worm worm = (Worm) view.findViewById(R.id.worm);
			worm.showsubscore = false;
			worm.max_pts = max_score;
        	worm.total_pts = cursor.getInt(i_points);
        	int ass_pts = cursor.getInt(i_assist_pts);
        	int bon_pts = cursor.getInt(i_bonus_pts);
        	int goal_pts = cursor.getInt(i_goal_pts);
        	int app_pts = worm.total_pts - ass_pts - bon_pts - goal_pts;
        	worm.points[0] = app_pts;
        	worm.points[1] = goal_pts;
        	worm.points[2] = ass_pts;
        	worm.points[3] = bon_pts;
        	worm.invalidate();
        	// ticker
        	Ticker tick = (Ticker) view.findViewById(R.id.ticker);
        	byte[] ticker_compressed = cursor.getBlob(i_ticker_string);
        	//String input = cursor.getString(i_ticker_string);
        	String input = DbGen.decompress(ticker_compressed);
        	if (input != null) {
        		if (input.length() > 0) {
        			TickerData data = Ticker.parseTickerData(input, rating_high, rating_low, (traffic==TRAFFIC_PREDCS), traffic + 1, false);
	        		tick.setData(data, ProcessData.next_x_games, App.next_gameweek);
        		}
        	}
        	// watchlist
        	i = (ImageView) view.findViewById(R.id.watchlisticon);
        	int fpl_id = cursor.getInt(i_fpl_id);
        	if (App.watchlist.contains(fpl_id)) {
        		//i.setImageResource(R.drawable.btn_star_big_on);
        		i.setImageBitmap(bmpOther.getBitmap(R.drawable.btn_star_big_on));
    	 	} else {
    	 		i.setImageBitmap(null);
    	 	}
        	// my squad
        	RelativeLayout rl = (RelativeLayout) view.findViewById(R.id.backing);
        	if (App.mySquadNext.contains(fpl_id)) {
        		rl.setBackgroundResource(R.drawable.list_item_gradient_green);
    	 	} else {
    	 		rl.setBackgroundResource(R.drawable.list_item_gradient);
    	 	}
        	// flags + name
        	if (display_mode == DISPLAY_FULL) i = (ImageView) view.findViewById(R.id.flag);
        	t = (TextView) view.findViewById(R.id.pName);
        	t.setText(cursor.getString(i_name));
        	int yel = cursor.getInt(i_fpl_yellow_flag);
    		int red = cursor.getInt(i_fpl_red_flag);
    		int diff = cursor.getInt(i_diff_flag);
    		if (yel == 1) {
    			if (display_mode == DISPLAY_FULL) {
    				//i.setImageResource(R.drawable.yellow_flag);
    				i.setImageBitmap(bmpOther.getBitmap(R.drawable.yellow_flag));
    			}
    			t.setTextColor(Color.YELLOW);
    		} else if (red == 1) {
    			if (display_mode == DISPLAY_FULL) {
    				//i.setImageResource(R.drawable.red_flag);
    				i.setImageBitmap(bmpOther.getBitmap(R.drawable.red_flag));
    			}
    			t.setTextColor(Color.RED);
    		} else if (diff == 1) {
    			if (display_mode == DISPLAY_FULL) {
    				//i.setImageResource(R.drawable.diff_flag);
    				i.setImageBitmap(bmpOther.getBitmap(R.drawable.diff_flag));
    			}
    			t.setTextColor(0xffdf00ff);
    		} else {
    			if (display_mode == DISPLAY_FULL) {
    				i.setImageBitmap(null);
    			}
    			t.setTextColor(Color.WHITE);
    		}
    		// stats
    		for (int ind=0; ind<num_stats; ind++){
    			boolean pop = true;
    			if (display_mode == DISPLAY_SMALLER) {
    				if (ind==1 || ind==3) pop = false;
    			}
    			if (pop) {
	        		Stat s = playerstats.get(use_stats[ind]);
	    	 		t = (TextView) view.findViewById(fieldIds[ind]);
	    	 		if (s.type <= 2) {
	    	 			t.setTextColor(Color.WHITE);
	    	 		} else if (s.type == 3) {
	    	 			t.setTextColor(Color.YELLOW);
	    	 		}
	    	 		
	    	 		String val;
			 		if (s.divide > 0) {
			 			float num = cursor.getFloat(cursor.getColumnIndex(playerstats.get(use_stats[ind]).db_field)) / s.divide;
			 			val = String.valueOf(num);
			 		} else {
			 			val = cursor.getString(cursor.getColumnIndex(playerstats.get(use_stats[ind]).db_field));
			 		}
			 		
	    	 		t.setText(val);
	    	 		t.setOnLongClickListener((Players) context);
	    	 		// stat lab
	    	 		t = (TextView) view.findViewById(fieldLabs[ind]);
	    	 		t.setText(s.short_lab);
	    	 		t.setOnLongClickListener((Players) context);
    			}
    	 	}
    		// price
    		t = (TextView) view.findViewById(R.id.value);
    		float price = cursor.getFloat(i_price) / 10f;
    		t.setText("£" + price);
    		// position
    		t = (TextView) view.findViewById(R.id.position);
    		t.setText(position_text[cursor.getInt(i_position) - 1]);
    		// pie
    		if (display_mode == DISPLAY_FULL) {
	    		Pie pie = (Pie) view.findViewById(R.id.pie);
	    		if ( (stat_season >= ProcessPlayer.SEASON_FULL_START) || (stat_season == ProcessPlayer.ALL_SEASONS) ) {
	    			pie.setVisibility(View.VISIBLE);
	    			int g_x = cursor.getInt(i_cgox);
		    		int g_o_y = cursor.getInt(i_cgoy);
		    		int g_o_z = cursor.getInt(i_cgoz);
		    		pie.games[0] = Math.max(g_x - g_o_y, 0);
		    		pie.games[1] = Math.max(g_o_y - g_o_z, 0);
		    		pie.games[2] = Math.max(g_o_z, 0);
					//App.log(cursor.getString(i_name) + " " + pie.games[0] + " " + pie.games[1] + " " + pie.games[2]);
					pie.invalidate();
	    		} else {
	    			pie.setVisibility(View.INVISIBLE);
	    		}
    		}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final View view = LayoutInflater.from(context).inflate(display_resources[display_mode], parent, false);
		    return view;
		}
    	
    }
    
    // listener - long click changes stat in position
    public boolean onLongClick(View v) {
		int id = v.getId();
    	for (int i=0; i<num_stats; i++) {
    		if (id == fieldIds[i] || id == fieldLabs[i]) {
    			//Toast.makeText(v.getContext(), "long click: " + stat_desc[use_stats[i]], Toast.LENGTH_LONG).show();
    			long_clicked_stat = use_stats[i];
    			showDialog(DIALOG_STAT_ID);
    			return true;
    		}
    	}
		return false;
	}
    
    // switch between listview layouts
    public void changeView() {
    	if (display_mode == DISPLAY_FULL) {
    		display_mode = DISPLAY_SMALLER;
    	} else {
    		display_mode = DISPLAY_FULL;
    	}
    	Settings.setIntPref(Settings.PREF_PLAYERS_VIEW_TYPE, display_mode, this);
    	redrawList();
    }
    
    // callback
    public void dataChanged() {
    	if (loader != null) {
			loader.cancel(true);
    	}
    	if (progress != null) {
    		progress.dismiss();
    	}
    	App.log("a");
    	popList();
    }
    
    // swap displayed stats
    public void clickStatSwap(View v) {
        if (using_stats == 1) {
	    	using_stats = 2;
	    	use_stats = use_stats_b;
        } else {
        	using_stats = 1;
        	use_stats = use_stats_a;
        }
    	redrawList();
    }
    
    public void toggleWatchlist() {
    	watchlist = !watchlist;
    	App.log("l");
    	popList();
    	setWatchlistIcon(watchlist);
    }
    
    // redraw list (no cursor refresh)
    private void redrawList() {
    	int pos = listViewRef.getFirstVisiblePosition();
    	populateCallback();
    	listViewRef.setSelection(pos);
    }
    
    public ProgressDialog progress;
    
    protected void onDestroy() {
    	if (loader != null) {
			loader.cancel(true);
    	}
    	if (progress != null) {
    		progress.dismiss();
    	}
    	if (cur != null) {
	    	cur.close();
	    }
    	super.onDestroy();
    }
    
}