package com.pennas.fpl;

import java.util.ArrayList;

import com.pennas.fpl.Players.Stat;
import com.pennas.fpl.Selection.ViewPagerAdapter;
import com.pennas.fpl.process.ProcessData;
import com.pennas.fpl.ui.Ticker;
import com.pennas.fpl.ui.Worm;
import com.pennas.fpl.ui.Ticker.TickerData;
import com.pennas.fpl.util.FPLActivity;
import com.pennas.fpl.util.DbGen;
import com.pennas.fpl.util.FPLAsyncTask;
import com.pennas.fpl.util.Refreshable;
import com.pennas.fpl.util.SquadUtil;
import com.viewpagerindicator.TitlePageIndicator;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.util.FloatMath;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class Teams extends FPLActivity implements Refreshable {
	// persistent cursor for listview
	private Cursor curTeams, curRotation;
	// filter properties
	private int sort = 0;
	private int stat = 0;
	private int rotate_sort = 0;
	private int traffic = 0;
	private float rotate_cost;
	
	private int season = App.season;
	
	SimpleCursorAdapter teams_adapter;
	
	private String[] teams_adapter_from;
	private int[] teams_adapter_to;
	
	private boolean doPopList = false;
	private ListView teamsList, rotationList;
	protected ViewFlipper flipper;
	
	private int maxPoints;
	
	private String default_stat = "c_h_rating";
	private String default_sort = "points";
	
	public static final String P_SORT = "com.pennas.fpl.sort";
	
	private int next_gw = App.next_gameweek;
	
	private String[] sortNames;
    private Stat[] sortStats;
    private float values[];
	private String value_labs[];
    
	private static final String[] trafficNames = { "Overall", "Attacking", "Defensive" };
	public static final float CS_HIGH = 1;
    public static final float CS_LOW = 0;
    public static final float[] ratingLow = { Players.RATING_LOW_FIX, 0.8f, CS_LOW };
	public static final float[] ratingHigh = { Players.RATING_HIGH_FIX, 2.5f, CS_HIGH };
	public static final int TRAF_OVERALL = 0;
	public static final int TRAF_ATTACK = 1;
	public static final int TRAF_DEF = 2;
	
    private static final String[] rotate_sort_desc = { "CS Chance %", "Num Gameweeks Home", "Cost", "Clean Sheets (Guess!!)" };
    private static final String[] rotate_sort_fields = { "cs_perc", "gameweeks_home", "cost", "clean_sheets" };
    
    private static final int FILTER_SORT = 1;
    private static final int FILTER_STAT = 2;
    private static final int FILTER_TRAF = 3;
    private static final int FILTER_COST = 4;
    private static final int FILTER_ROTATE_SORT = 5;
    
    private ViewPager viewPager;
	
    private static final int TAB_TEAMS = 0;
	private static final int TAB_ROTATION = 1;
	
	/** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        actionBar.setTitle("Teams");
        
        hasTips = true;
        tipsPref = Settings.PREF_TIPS_TEAMS;
        tips.add("Use the filter icon for different stats/orders on the the Teams tab");
		tips.add("Worm shows home/away points");
		tips.add("Configurable traffic lights show upcoming fixtures");
		tips.add("White line to left/right of traffic light depicts home/away");
		tips.add("Rotation tab helps choose a goalkeeping combination");
		tips.add("Top/bottom marking on rotation traffic light item shows which 'keeper is indicated");
		
        setContentView(R.layout.teams_screen);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
	        if (extras.containsKey(P_SORT)) {
		    	default_stat = extras.getString(P_SORT);
		    	default_sort = extras.getString(P_SORT);
		    }
	    }
        
        teamsList = new ListView(this);
        rotationList = new ListView(this);
        
	 	loadStats();
	 	
	 	// sort dropdown
	 	sortNames = new String[Teams.teamstats.size()];
    	sortStats = new Stat[Teams.teamstats.size()];
    	int i = 0;
    	for (Stat s : Teams.teamstats) {
    		sortNames[i] = s.desc;
    		sortStats[i] = s;
    		if (s.db_field.equals(default_stat)) stat = i;
    		if (s.db_field.equals(default_sort)) sort = i;
    		i++;
    	}
    	
	    addFilter(FILTER_SORT, "Sort", sortNames, sort, true, false);
	    
	    addFilter(FILTER_STAT, "Stat", sortNames, stat, true, false);
	    
	    addFilter(FILTER_TRAF, "Traffic Lights", trafficNames, traffic, true, false);
	    
	    // get max/min price in db for slider
	 	Cursor minmax = App.dbGen.rawQuery("SELECT MIN(cost) min_cost, MAX(cost) max_cost FROM team_rotation", null);
	 	minmax.moveToFirst();
	 	float min_price = minmax.getFloat(0);
	 	float max_price = minmax.getFloat(1);
	 	minmax.close();
	 	
	 	// init list of values for price Spinner
	 	float min = FloatMath.ceil(min_price);
	 	float max = FloatMath.ceil(max_price);
	 	App.log(max_price + " -> " + max);
	 	
	 	rotate_cost = max;
	 	int diff = (int)max - (int)min;
	 	values = new float[diff + 1];
	 	value_labs = new String[diff + 1];
	 	for (int k=0; k<=diff; k++) {
	 		float val = ProcessData.trunc(max, 1);
	 		values[k] = val;
	 		value_labs[k] = String.valueOf(val);
	 		max -= 1;
	 	}
	    
	    addFilter(FILTER_COST, "Cost", value_labs, 0, false, false);
	    
	    addFilter(FILTER_ROTATE_SORT, "Sort", rotate_sort_desc, 0, false, false);
	    
	    viewPager = (ViewPager) findViewById(R.id.viewpager);
		final String[] titles = new String[] { "Teams", "Rotation" };
		final View[] views = new View[] { teamsList, rotationList };
		ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(titles, views);
	    TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.titles);
	    viewPager.setAdapter(pagerAdapter);
	    indicator.setViewPager(viewPager);
	    
	    indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				if(position == TAB_TEAMS) {
        	    	setFilterVisible(FILTER_SORT, true);
        	    	setFilterVisible(FILTER_STAT, true);
        	    	setFilterVisible(FILTER_TRAF, true);
        	    	setFilterVisible(FILTER_COST, false);
        	    	setFilterVisible(FILTER_ROTATE_SORT, false);
        	    } else {
        	    	setFilterVisible(FILTER_SORT, false);
        	    	setFilterVisible(FILTER_STAT, false);
        	    	setFilterVisible(FILTER_TRAF, false);
        	    	setFilterVisible(FILTER_COST, true);
        	    	setFilterVisible(FILTER_ROTATE_SORT, true);
        	    }
				
				set_screenshot_view();
			}

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

			@Override
			public void onPageScrollStateChanged(int state) { }
		});
	 	
	 	// main lists populate (dynamic)
	    doPopList = true;
	    App.log("a");
    	popList();
    }
    
    
    // find min/max season price/score, for worm/filter bounds
    public static int getMaxScore(SQLiteDatabase db, int p_season) {
	    // get max/min price in db for slider
	 	Cursor minmax = db.rawQuery("SELECT MAX(points) points FROM team_season WHERE season = " + p_season, null);
	 	minmax.moveToFirst();
	 	int max_points = minmax.getInt(0);
	 	minmax.close();
	 	return max_points;
    }
    
    // async task to load data
    private class LoadTask extends FPLAsyncTask<Integer, Void, Boolean> {
    	ProgressDialog dialog;
    	
    	protected Boolean doInBackground(Integer... params) {
			SQLiteDatabase db = App.dbGen;
			int c_season = params[0];
			int next_gameweek = params[1];
			Cursor curLoad;
			
			String order_by = "ORDER BY " + sortStats[sort].db_field + " DESC";
			String ticker_home = null;
			String ticker_away = null;
			if (traffic == TRAF_OVERALL) {
				ticker_home = "pred_ratio_home";
				ticker_away = "pred_ratio_away";
			} else if (traffic == TRAF_ATTACK) {
				ticker_home = "pred_goals_home";
				ticker_away = "pred_goals_away";
			} else if (traffic == TRAF_DEF) {
				ticker_home = "pred_goals_away";
				ticker_away = "pred_goals_home";
			}
		 	
		 	// run query for listview
		 	String query = "SELECT ts.team_id _id, ((ts.c_sum_h_w*3)+ts.c_sum_h_d) points_home,"
		 		+ " ((ts.c_sum_a_w*3)+ts.c_sum_a_d) points_away"
		 		+ ", ts." + sortStats[stat].db_field + " " + sortStats[stat].db_field
		 		+ ", (SELECT group_concat(f.gameweek||'=1='||f." + ticker_home + ") upcoming_fix_home"
		 		+ " FROM fixture f WHERE f.season = " + c_season + " AND f.gameweek >= " + next_gameweek
		 		+ " AND f.team_home_id = ts.team_id ) upcoming_fix_home"
		 		+ ", (SELECT group_concat(f.gameweek||'=0='||f." + ticker_away + ") upcoming_fix_away FROM fixture f WHERE f.season = " + c_season + " AND f.gameweek >= " + next_gameweek + " AND f.team_away_id = ts.team_id ) upcoming_fix_away"
		 		+ " FROM team_season ts WHERE season = " + c_season + " " + order_by;
		 	curLoad = db.rawQuery(query, null);
	    	
	    	// this is the step that actually takes the time - do it in async
		 	curLoad.moveToFirst();
		 	
		 	String order_by_rotate = rotate_sort_fields[rotate_sort];
		 	Cursor curRotate = db.rawQuery("SELECT tr.team_a||'-'||tr.team_b _id, tr.team_a, tr.team_b, tr.cs_perc"
		 			+ ", tr.gameweeks_home, tr.ticker_string, tr.cost, tr.clean_sheets, pa.name name_a, pb.name name_b"
		 			+ " FROM team_rotation tr, player pa, player pb, team_season ta, team_season tb"
		 			+ " WHERE tr.cost <= " + rotate_cost
		 			+ " AND ta.season = " + season + " AND tb.season = " + season
		 			+ " AND ta.team_id = tr.team_a AND tb.team_id = tr.team_b"
		 			+ " AND pa._id = ta.gk_player_id AND pb._id = tb.gk_player_id"
		 			+ " ORDER BY " + order_by_rotate + " DESC", null);
    	    curRotate.moveToFirst();
    	    
    	    curTeams = curLoad;
    	    curRotation = curRotate;
    	    
    	    return true;
        }
		protected void onPreExecute() {
			dialog = new ProgressDialog(Teams.this);
			dialog.setMessage("Loading..");
			dialog.setCancelable(false);
			dialog.show();
			Teams.this.progress = dialog;
			
		    closeCursors();
		}
        protected void onPostExecute(Boolean res) {
        	Teams.this.populateCallback();
        	dialog.dismiss();
        	loader = null;
        }
    }
    
    private void closeCursors() {
    	if (curTeams != null) {
    		curTeams.close();
    	}
	    if (curRotation != null) {
	    	curRotation.close();
	    }
    }
    
    private LoadTask loader;
    
    // init or redo cursor->list binding for main player list
    private void popList() {
	 	if (doPopList) {
	 		App.log("popList()");
	 		maxPoints = getMaxScore(App.dbGen, season);
	 		
	 		// run load in background (displaying progress dialog)
	 		loader = new LoadTask();
			loader.fplExecute(season, next_gw);
	 	}
    }
    
    boolean changed_columns = false;
    
    // called when async load task finishes: populate GUI from data
    protected void populateCallback() {
    	// column/GUI mapping (contains some hacks)
    	// _id, points, <stat field>, upcoming_fix_home, upcoming_fix_away
 		teams_adapter_from = new String[] { "_id", "points_home", "upcoming_fix_home", "_id", sortStats[stat].db_field };
 		teams_adapter_to = new int[] { R.id.shirtPic, R.id.worm, R.id.ticker, R.id.teamname, R.id.statvalue };
    	
    	// update existing cursor adapter
    	if (teams_adapter != null) {
    		if (changed_columns) {
	 			teams_adapter.changeCursorAndColumns(curTeams, teams_adapter_from, teams_adapter_to);
	 		} else {
	 			teams_adapter.changeCursor(curTeams);
	 		}
	 	// create new cursor adapter (from/to are the same as update)
	 	} else {
	 		teams_adapter = new SimpleCursorAdapter(this, R.layout.teams_screen_team_item, curTeams, teams_adapter_from, teams_adapter_to);
			teams_adapter.setViewBinder(new MyViewBinder(this));
			teamsList.setAdapter(teams_adapter);
			OnItemClickListener listListener = new OnItemClickListener() {
				public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
					// load player screen for player "id"
					Intent intent = new Intent(view.getContext(), Team.class);
					intent.putExtra(Team.P_TEAM_ID, (int) id);
			    	startActivity(intent);
				}
		    };
		    teamsList.setOnItemClickListener(listListener);
	 	}
    	
    	RotationCursorAdapter rAdapter = new RotationCursorAdapter(this, curRotation);
    	rotationList.setAdapter(rAdapter);
    	
    	set_screenshot_view();
    }
    
    private void set_screenshot_view() {
		if (viewPager.getCurrentItem() == TAB_TEAMS) {
			setScreenshotView(teamsList, "Teams, sorted by " + sortNames[sort] + ", showing stat " + sortNames[stat], 20);
		} else if (viewPager.getCurrentItem() == TAB_ROTATION) {
			setScreenshotView(rotationList, "Goalkeeper rotation, sorted by " + rotate_sort_desc[rotate_sort]);
		}
	}
    
    @Override
	protected void filterSelection(int id, int pos) {
    	App.log("subclass");
    	
    	switch(id) {
    		case FILTER_SORT:
    			if (pos != sort) {
            	  	sort = pos;
            	  	App.log("b");
            	  	popList();
        		}
    			break;
    		case FILTER_STAT:
    			if (pos != stat) {
    	        	stat = pos;
        			changed_columns = true;
    	        	App.log("c");
    	        	popList();
        		}
    			break;
    		case FILTER_TRAF:
    			if (pos != traffic) {
            	  	traffic = pos;
            	  	App.log("b3");
            	  	popList();
        		}
    			break;
    		case FILTER_COST:
    			if (values[pos] != rotate_cost) {
        			rotate_cost = values[pos];
        			App.log("d");
        	    	popList();
        		}
    			break;
    		case FILTER_ROTATE_SORT:
    			if (pos != rotate_sort) {
        			rotate_sort = pos;
  	        	  	App.log("b2");
  	        	  	popList();
        		}
    			break;
    	}
    }
    
    public static ArrayList<Stat> teamstats;
    
    // load stat defs into memory
    public static void loadStats() {
    	if (teamstats != null) return;
    	
    	teamstats = new ArrayList<Stat>();
    	
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.appContext);
    	
    	Cursor curStat = App.dbGen.rawQuery("SELECT db_field, type, short_lab, desc, show_value, always_show_sort FROM stat WHERE player_stat = 0 ORDER BY desc ASC", null);
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
    		
    		// check if preferences say that this stat should be displayed
    		// PREF CHANGE LISTENER NEEDS TO SET playerstats TO NULL WHEN PREFS CHANGE
    		if (s.show_value) {
    			if (!prefs.getBoolean(Settings.PREF_PREFIX_TEAM_STAT + s.db_field, true)) {
    				s.show_value = false;
    			}
    		}
    		
    		teamstats.add(s);
    		//AppClass.log("Add " + s.db_field + " -> " + s.desc);
    		curStat.moveToNext();
    	}
    	curStat.close();
    }
    
    private static final int TEAM_TICKER_WEEKS = 14;
    
    // bind cursor to main list row item
    public class MyViewBinder implements SimpleCursorAdapter.ViewBinder {
    	Teams parent_players;
    	
        public MyViewBinder(Teams players) {
        	parent_players = players;
		}
        
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            int viewId = view.getId();
            if (viewId == R.id.shirtPic) {
            	ImageView i = (ImageView) view;
            	i.setImageResource(SquadUtil.getShirtResource(cursor.getInt(columnIndex)));
            } else if (viewId == R.id.worm) {
            	Worm worm = (Worm) view;
            	worm.max_pts = maxPoints;
            	int home_pts = cursor.getInt(columnIndex);
            	int away_pts = cursor.getInt(columnIndex+1);
            	worm.total_pts = home_pts + away_pts;
            	// home/away
            	worm.points[0] = home_pts;
            	worm.points[1] = away_pts;
            	worm.points[2] = 0;
            	worm.points[3] = 0;
            	worm.team = true;
            	worm.invalidate();
            } else if (viewId == R.id.ticker) {
            	Ticker t = (Ticker) view;
            	String input = cursor.getString(columnIndex);
            	// home/away merge
            	String aw = cursor.getString(columnIndex+1);
            	boolean home = (input!=null && !input.equals(""));
            	boolean away = (aw!=null && !aw.equals(""));
            	if (!home && away) {
            		input = aw;
            	} else if (home && away) {
            		input = input + "," + aw;
            	}
            	TickerData data = Ticker.parseTickerData(input, ratingHigh[traffic], ratingLow[traffic], (traffic==TRAF_DEF), 1, false, true);
            	t.setData(data, TEAM_TICKER_WEEKS, next_gw);
            } else {
            	TextView t = (TextView) view;
            	boolean setText = true;
            	
            	if (viewId == R.id.teamname) {
            		t.setText(App.id2team.get(cursor.getInt(columnIndex)));
            		setText = false;
            	} else if (viewId == R.id.statvalue) {
            		t.setText(sortNames[stat] + " " + cursor.getString(columnIndex));
            		setText = false;
            	}
            	
            	// if nothing special done above for this entry..
            	if (setText) t.setText(cursor.getString(columnIndex));
            }
            
            return true;
        }
    }
    
    private static final int TEAM_ROTATE_TICKER_WEEKS = 16;
    
    private class RotationCursorAdapter extends CursorAdapter {
    	private int i_team_a, i_team_b, i_cs_perc, i_gameweeks_home, i_ticker_string, i_cost, i_cs;
    	private int i_name_a, i_name_b;
    	private boolean loaded_indexes;
    	
    	public RotationCursorAdapter(Context context, Cursor c) {
			super(context, c);
		}
    	
		private void loadIndexes(Cursor cursor) {
        	loaded_indexes = true;
        	i_team_a = cursor.getColumnIndex("team_a");
        	i_team_b = cursor.getColumnIndex("team_b");
        	i_cs_perc = cursor.getColumnIndex("cs_perc");
        	i_ticker_string = cursor.getColumnIndex("ticker_string");
        	i_gameweeks_home = cursor.getColumnIndex("gameweeks_home");
        	i_cost = cursor.getColumnIndex("cost");
        	i_cs = cursor.getColumnIndex("clean_sheets");
        	i_name_a = cursor.getColumnIndex("name_a");
        	i_name_b = cursor.getColumnIndex("name_b");
        }
    	
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// load column indexes if required
			if (!loaded_indexes) loadIndexes(cursor);
			
			int team_a = cursor.getInt(i_team_a);
			int team_b = cursor.getInt(i_team_b);
			
			TextView t;
			ImageView i;
			// shirt pic a
			i = (ImageView) view.findViewById(R.id.shirtPicA);
			i.setImageResource(SquadUtil.getShirtResource(team_a));
			// shirt pic b
			i = (ImageView) view.findViewById(R.id.shirtPicB);
			i.setImageResource(SquadUtil.getShirtResource(team_b));
			// team name a
    		t = (TextView) view.findViewById(R.id.teamNameA);
    		t.setText(App.id2team.get(team_a));
    		// player name a
    		t = (TextView) view.findViewById(R.id.playerNameA);
    		t.setText(cursor.getString(i_name_a));
    		// team name b
    		t = (TextView) view.findViewById(R.id.teamNameB);
    		t.setText(App.id2team.get(team_b));
    		// player name b
    		t = (TextView) view.findViewById(R.id.playerNameB);
    		t.setText(cursor.getString(i_name_b));
    		// cs perc
    		t = (TextView) view.findViewById(R.id.csPerc);
    		t.setText(cursor.getString(i_cs_perc));
    		// gws home
    		t = (TextView) view.findViewById(R.id.gwHome);
    		t.setText(cursor.getString(i_gameweeks_home));
    		// cost
    		t = (TextView) view.findViewById(R.id.cost);
    		t.setText("£" + cursor.getString(i_cost));
    		// cs GUESS
    		t = (TextView) view.findViewById(R.id.cs);
    		t.setText(cursor.getString(i_cs));
    		// ticker
        	Ticker tick = (Ticker) view.findViewById(R.id.ticker);
        	byte[] ticker_compressed = cursor.getBlob(i_ticker_string);
        	//String input = cursor.getString(i_ticker_string);
        	String input = DbGen.decompress(ticker_compressed);
        	if (input != null) {
        		if (input.length() > 0) {
        			TickerData data = Ticker.parseTickerData(input, CS_HIGH, CS_LOW, false, 1, true);
        			tick.setData(data, TEAM_ROTATE_TICKER_WEEKS, next_gw);
        		}
        	}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final View view = LayoutInflater.from(context).inflate(R.layout.teams_screen_rotation_item, parent, false);
		    return view;
		}
    	
    }
    
    // callback
    public void dataChanged() {
    	if (loader != null) {
			loader.cancel(true);
    	}
    	if (progress != null) {
    		progress.dismiss();
    	}
    	App.log("dc");
    	popList();
    }
    
    public ProgressDialog progress;
    
    protected void onDestroy() {
    	if (loader != null) {
			loader.cancel(true);
    	}
    	if (progress != null) {
    		progress.dismiss();
    	}
    	closeCursors();
    	super.onDestroy();
    }
    
}