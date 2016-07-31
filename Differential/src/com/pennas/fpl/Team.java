package com.pennas.fpl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.androidplot.series.XYSeries;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;
import com.pennas.fpl.Players.Stat;
import com.pennas.fpl.Selection.ViewPagerAdapter;
import com.pennas.fpl.process.ProcessTeam;
import com.pennas.fpl.ui.Ticker;
import com.pennas.fpl.ui.Worm;
import com.pennas.fpl.ui.Ticker.TickerData;
import com.pennas.fpl.util.Refreshable;
import com.pennas.fpl.util.SquadUtil;
import com.pennas.fpl.util.FPLActivity;
import com.viewpagerindicator.TitlePageIndicator;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class Team extends FPLActivity implements Refreshable {

	SimpleCursorAdapter adapter;
	
	private int teamId;
	private String teamName;

	private ListView statsList, matchesList;
	private XYPlot seasongraph;
	
	private int season = App.season;
	Cursor curM;
	
	public static final int[] seasons = { 13, 12, 11 };
	public static final String[] season_desc = { "2012/13", "2011/12", "2010/11" };
	
	public static final String P_TEAM_ID = "com.pennas.fpl.team_id";
	private ArrayList<TeamStatEntry> statValues;
	
	private Worm seasonWorm;
	private static final int FILTER_SEASON = 1;
	
	private ViewPager viewPager;
	private LinearLayout tab_information;
	
	private static final int TAB_STATS = 0;
	private static final int TAB_MATCHES = 1;
	private static final int TAB_GRAPH = 2;
	private static final int TAB_INFORMATION = 3;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    setContentView(R.layout.team_screen);
	    
	    if (App.cur_gameweek < 1) {
	    	season = App.season - 1;
	    }
	    
	    // get ID of player
	    Bundle extras = getIntent().getExtras();
	    if (extras == null) return;
	    teamId = extras.getInt(P_TEAM_ID);
	    
	    hasTips = true;
        tipsPref = Settings.PREF_TIPS_TEAM;
		tips.add("Click on a stat to show leading teams in that statistic");
		tips.add("Stats are updated by Processing in the sync panel");
		tips.add("Click a match to see details");
		
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	 	seasongraph = (XYPlot) inflater.inflate(R.layout.graph, null);
	 	tab_information = (LinearLayout) inflater.inflate(R.layout.team_screen_information_tab, null);
		
	    ImageView shirtPic = (ImageView) findViewById(R.id.shirtPic);
	 	seasonWorm = (Worm)  findViewById(R.id.seasonworm);
	 	
	 	// get/resolve team
	 	teamName = App.id2team.get(teamId);
	 	
	 	// init GUI from data
	 	actionBar.setTitle(teamName);
	 	shirtPic.setImageResource(SquadUtil.getShirtResource(teamId));
	 	
	 	// season filter: populate (static)
	    addFilter(FILTER_SEASON, "Season", season_desc, getSeasonInd(season), true, true);
		
	 	statsList = new ListView(this);
	 	matchesList = new ListView(this);
	 	
	 	viewPager = (ViewPager) findViewById(R.id.viewpager);
		final String[] titles = new String[] { "Stats", "History", "Graph", "Information" };
		final View[] views = new View[] { statsList, matchesList, seasongraph, tab_information };
		ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(titles, views);
	    TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.titles);
	    viewPager.setAdapter(pagerAdapter);
	    indicator.setViewPager(viewPager);
	    viewPager.setCurrentItem(1);
	    
	    indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				set_screenshot_view();
			}

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

			@Override
			public void onPageScrollStateChanged(int state) { }
		});
	    
		popData();
	}
	
	private void popData() {
		App.log("Team::popData()");
		// get team details from db
        Cursor cur = App.dbGen.rawQuery("SELECT ts.*, ((ts.c_sum_h_w*3)+ts.c_sum_h_d) points_home"
        	+ ", ((ts.c_sum_a_w*3)+ts.c_sum_a_d) points_away"
        	+ " FROM team_season ts"
        	+ " WHERE season = " + season + " AND team_id = " + teamId, null);
	 	cur.moveToFirst();
	 	
	 	if (!cur.isAfterLast()) {
		 	// get player stats from db
		 	Teams.loadStats();
		 	statValues = new ArrayList<TeamStatEntry>();
		 	for (Stat s : Teams.teamstats) {
		 		String val = cur.getString(cur.getColumnIndex(s.db_field));
		 		if (s.show_value && (val != null) ) {
		 			TeamStatEntry stat = new TeamStatEntry();
		 			stat.desc = s.desc;
		 			stat.value = val;
		 			stat.statId = s.db_field;
		 			statValues.add(stat);
		 		}
		 	}
		 	
		 	// initialise the big season worm at the top of the screen
		 	seasonWorm.max_pts = Teams.getMaxScore(App.dbGen, season);
			seasonWorm.total_pts = cur.getInt(cur.getColumnIndex("points"));
			int home_pts = cur.getInt(cur.getColumnIndex("points_home"));
	    	int away_pts = cur.getInt(cur.getColumnIndex("points_away"));
	    	seasonWorm.total_pts = home_pts + away_pts;
	    	// home/away
	    	seasonWorm.points[0] = home_pts;
	    	seasonWorm.points[1] = away_pts;
	    	seasonWorm.points[2] = 0;
	    	seasonWorm.points[3] = 0;
	    	seasonWorm.team = true;
	    	seasonWorm.invalidate();
	 	}
	 	
	 	cur.close();
	 	
	 	// adapter for stats array->list. sort by stat description
	 	TeamStatEntryAdapter statAdapter = new TeamStatEntryAdapter(this, R.layout.player_screen_stat_item, statValues);
	 	statAdapter.sort(new Comparator<TeamStatEntry>() {
    		public int compare(TeamStatEntry object1, TeamStatEntry object2) {
    			return object1.desc.compareTo(object2.desc);
    		}
    	});
	 	statsList.setAdapter(statAdapter);
	 	// click listener for stats list, goes to players screen, sorted by that stat
		OnItemClickListener listListener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
	            // load right stats screen
				Intent intent = new Intent(view.getContext(), Teams.class);
				TeamStatEntry it = statValues.get(position);
				intent.putExtra(Teams.P_SORT, it.statId);
				startActivity(intent);
	        }
	    };
		statsList.setOnItemClickListener(listListener);
		
		drawGraph();
		
		if (curM != null) {
			curM.close();
		}
		
		// matches list
		curM = App.dbGen.rawQuery("SELECT _id, gameweek, team_home_id, team_away_id, res_goals_home, res_goals_away"
			+ ", res_points_home, res_points_away, got_bonus, pred_ratio_home, pred_ratio_away, pred_goals_home, pred_goals_away"
			+ " FROM fixture"
			+ " WHERE season = " + season
			+ " AND (team_home_id = " + teamId + " OR team_away_id = " + teamId + ")"
			+ " ORDER BY datetime ASC", null);
	 	// get current/next gw list position if possible
	 	curM.moveToFirst();
	 	int defPos = -1;
	 	while (!curM.isAfterLast()) {
			int gw = curM.getInt(1);
			if ( (gw == App.cur_gameweek) || (gw == App.next_gameweek) ) {
				// -3 to try and position current gw in middle of screen
				defPos = curM.getPosition() - 3;
				break;
			} // if
			curM.moveToNext();
		} // while
	 	
	 	MatchesCursorAdapter mAdapter = new MatchesCursorAdapter(this, curM);
	 	matchesList.setAdapter(mAdapter);
	 	OnItemClickListener listListenerMatch = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
				// load fixture screen for fixture "id"
				Intent intent = new Intent(view.getContext(), Fixture.class);
				intent.putExtra(Fixture.P_FIXTURE_ID, (int) id);
		    	startActivity(intent);
			}
	    };
	    matchesList.setOnItemClickListener(listListenerMatch);
		
		// set position in list to current/next gw if possible
		if (defPos > -1) {
			matchesList.setSelection(defPos);
		}
		
		set_screenshot_view();
	}
	
	private void set_screenshot_view() {
		if (viewPager.getCurrentItem() == TAB_STATS) {
			setScreenshotView(statsList, teamName + " stats for " + season_desc[getSeasonInd(season)]);
		} else if (viewPager.getCurrentItem() == TAB_MATCHES) {
			setScreenshotView(matchesList, teamName + " fixtures for " + season_desc[getSeasonInd(season)]);
		} else if (viewPager.getCurrentItem() == TAB_GRAPH) {
			setScreenshotView(seasongraph, teamName + " graph for " + season_desc[getSeasonInd(season)]);
		} else if (viewPager.getCurrentItem() == TAB_INFORMATION) {
			setScreenshotView(null, null);
		}
	}
	
	public static int getSeasonInd(int season) {
		for (int i=0; i<seasons.length; i++) {
			if (seasons[i] == season) return i;
		}
		return 0;
	}
	
	@Override
	protected void filterSelection(int id, int pos) {
    	App.log("subclass");
    	
    	switch(id) {
    		case FILTER_SEASON:
    			if (season != seasons[pos]) {
    	        	season = seasons[pos];
    	        	popData();
            	}
    			break;
    	}
    }
	
	private void drawGraph() {
		Cursor curGraph = App.dbGen.rawQuery("SELECT datetime, res_goals_away scored, res_goals_home conceded FROM fixture"
				+ " WHERE season = " + season + " AND team_away_id = " + teamId
				+ " AND got_bonus >= 1"
				+ " UNION ALL"
				+ " SELECT datetime, res_goals_home scored, res_goals_away conceded FROM fixture"
				+ " WHERE season = " + season + " AND team_home_id = " + teamId
				+ " AND got_bonus >= 1"
				+ " ORDER BY datetime ASC", null);
		curGraph.moveToFirst();
		int numGames = curGraph.getCount();
		
		Number[] seriesScored = new Number[numGames + 1];
		Number[] seriesConceded = new Number[numGames + 1];
		Number[] seriesPointsCumul = new Number[numGames + 1];
		
		float max = 0;
		
		int i = 1;
		float pointsRunning = 0;
		while(!curGraph.isAfterLast()) {
			int scored = curGraph.getInt(1);
			int conceded = curGraph.getInt(2);
			seriesScored[i] = scored;
			seriesConceded[i] = conceded;
			if (scored > conceded) {
				pointsRunning += 3;
			} else if (scored == conceded) {
				pointsRunning += 1;
			}
			if (scored > max) max = scored;
			if (conceded > max) max = conceded;
			seriesPointsCumul[i] = pointsRunning;
			i++;
			curGraph.moveToNext();
		}
		curGraph.close();
		
		float pointsFactor = max / pointsRunning;
		for (int k=1; k<=numGames; k++) {
			seriesPointsCumul[k] = seriesPointsCumul[k].floatValue() * pointsFactor;
		}
		
		if (seriesGraphScored != null) {
			seasongraph.removeSeries(seriesGraphScored);
		}
		if (seriesGraphConceded != null) {
			seasongraph.removeSeries(seriesGraphConceded);
		}
		if (seriesGraphPointsCumul != null) {
			seasongraph.removeSeries(seriesGraphPointsCumul);
		}
		
		seriesGraphScored = new SimpleXYSeries(Arrays.asList(seriesScored), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Scored");  
		seriesGraphConceded = new SimpleXYSeries(Arrays.asList(seriesConceded), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Conceded");  
		seriesGraphPointsCumul = new SimpleXYSeries(Arrays.asList(seriesPointsCumul), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Points");  
		
		seasongraph.addSeries(seriesGraphScored, new LineAndPointFormatter(0xff0000ff, 0xff0000ff, 0xff0000ff));
		seasongraph.addSeries(seriesGraphConceded, new LineAndPointFormatter(0xffff0000, 0xffff0000, 0x44ff0000));
		seasongraph.addSeries(seriesGraphPointsCumul, new LineAndPointFormatter(0xff000000, 0xff000000, null));
		
		seasongraph.disableAllMarkup();
		seasongraph.setRangeLabel("Goals/Points");
		seasongraph.setDomainLabel("Games");
		seasongraph.getGraphWidget().setDomainValueFormat(new DecimalFormat("0"));
		seasongraph.getGraphWidget().setRangeValueFormat(new DecimalFormat("0"));
		seasongraph.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 1);
		
		seasongraph.redraw();
	}
	
	private XYSeries seriesGraphScored;
	private XYSeries seriesGraphConceded;
	private XYSeries seriesGraphPointsCumul;
	
	// class describing a stat entry
	private static class TeamStatEntry {
		public String desc;
		public String value;
		public String statId;
	}
	
	// click handler - load web browser for google news search on player
	public void googleNews(View v) {
		//Uri uri = Uri.parse( "http://news.google.co.uk/news/search?q=" + Uri.encode(playerName + " " + teamName));
		Uri uri = Uri.parse( "http://www.google.com/m/search?tbs=nws:1&ned=uk&q=" + Uri.encode(teamName));
		startActivity( new Intent( Intent.ACTION_VIEW, uri ) );
    }
	// click handler - load web browser for twitter search on player
    public void twitterSearch(View v) {
		Uri uri = Uri.parse( "http://search.twitter.com/search?q=" + Uri.encode(teamName));
		startActivity( new Intent( Intent.ACTION_VIEW, uri ) );
    }
    
    public void clickPlayers(View v) {
    	// load right stats screen
		Intent intent = new Intent(v.getContext(), Players.class);
		intent.putExtra(Players.P_TEAM, teamId);
		startActivity(intent);
    }
    
    // adapter for stats listview
	private class TeamStatEntryAdapter extends ArrayAdapter<TeamStatEntry> {

		private ArrayList<TeamStatEntry> items;

		public TeamStatEntryAdapter(Context context, int textViewResourceId, ArrayList<TeamStatEntry> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		        v = vi.inflate(R.layout.player_screen_stat_item, null);
			}
			
			TeamStatEntry it = items.get(position);
			if (it != null) {
				TextView t = (TextView) v.findViewById(R.id.statdesc);
				t.setText(it.desc);
				t = (TextView) v.findViewById(R.id.statvalue);
				t.setText(it.value);
			}
			
			return v;
		}
	}
	
	// gameweek, team_home_id, team_away_id, res_goals_home, res_goals_away, res_points_home, res_points_away
	private class MatchesCursorAdapter extends CursorAdapter {
    	private int i_gameweek, i_team_home_id, i_team_away_id, i_res_goals_home, i_res_goals_away
    	, i_res_points_home, i_res_points_away, i_got_bonus, i_pred_goals_home, i_pred_goals_away
    	, i_pred_ratio_home, i_pred_ratio_away;
    	private boolean loaded_indexes;
    	
    	public MatchesCursorAdapter(Context context, Cursor c) {
			super(context, c);
		}
    	
		private void loadIndexes(Cursor cursor) {
        	loaded_indexes = true;
        	i_gameweek = cursor.getColumnIndex("gameweek");
        	i_got_bonus = cursor.getColumnIndex("got_bonus");
        	i_team_home_id = cursor.getColumnIndex("team_home_id");
        	i_team_away_id = cursor.getColumnIndex("team_away_id");
        	i_res_goals_home = cursor.getColumnIndex("res_goals_home");
        	i_res_goals_away = cursor.getColumnIndex("res_goals_away");
        	i_pred_goals_home = cursor.getColumnIndex("pred_goals_home");
        	i_pred_goals_away = cursor.getColumnIndex("pred_goals_away");
        	i_pred_ratio_home = cursor.getColumnIndex("pred_ratio_home");
        	i_pred_ratio_away = cursor.getColumnIndex("pred_ratio_away");
        	i_res_points_home = cursor.getColumnIndex("res_points_home");
        	i_res_points_away = cursor.getColumnIndex("res_points_away");
        }
    	
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// load column indexes if required
			if (!loaded_indexes) loadIndexes(cursor);
			
			int team_opp;
			String gameweek = cursor.getString(i_gameweek);
			String goals_for;
			String goals_against;
			int points;
			boolean home;
			
			int team_home = cursor.getInt(i_team_home_id);
			int team_away = cursor.getInt(i_team_away_id);
			int goals_home = cursor.getInt(i_res_goals_home);
			int goals_away = cursor.getInt(i_res_goals_away);
			
			if (team_home == teamId) {
				team_opp = team_away;
				goals_for = String.valueOf(goals_home);
				goals_against = String.valueOf(goals_away);
				points = cursor.getInt(i_res_points_home);
				home = true;
			} else {
				team_opp = team_home;
				goals_for = String.valueOf(goals_away);
				goals_against = String.valueOf(goals_home);
				points = cursor.getInt(i_res_points_away);
				home = false;
			}
			
			int got_bonus = cursor.getInt(i_got_bonus);
			
			TextView t;
			ImageView i;
			// shirt pic
			i = (ImageView) view.findViewById(R.id.shirtPic);
			i.setImageResource(SquadUtil.getShirtResource(team_opp));
			// gameweek
    		t = (TextView) view.findViewById(R.id.gameweek);
    		t.setText(gameweek);
    		RelativeLayout highlight = (RelativeLayout) view.findViewById(R.id.highlight);
    		if (gameweek != null) {
	    		int gw = Integer.valueOf(gameweek);
	    		if (gw == App.cur_gameweek) {
	    			highlight.setBackgroundResource(R.drawable.this_gw_gradient);
	    			t.setTextColor(Player.TEXT_BLACK);
	    		} else {
	    			highlight.setBackgroundColor(0);
	    			t.setTextColor(Player.TEXT_HALF_WHITE);
	    		}
    		}
    		// team name
    		t = (TextView) view.findViewById(R.id.teamName);
    		String ha;
    		String team = "";
			team = App.id2team.get(team_opp);
    		if (home) {
    			ha = " (h)";
    		} else {
    			ha = " (a)";
    		}
    		t.setText(team + ha);
    		
    		if ( (goals_for != null) && (goals_against != null) ) {
    			t = (TextView) view.findViewById(R.id.result);
    			Worm worm = (Worm) view.findViewById(R.id.worm);
            	
    			if (got_bonus >= 1) {
    				t.setText(" " + goals_for + "-" + goals_against);
    				worm.showtotal = true;
    			} else {
    				t.setText("");
    				worm.showtotal = false;
    			}
    			
    			worm.max_pts = 3;
            	worm.total_pts = points;
            	// home/away
            	if (home) {
            		worm.points[0] = points;
            		worm.points[1] = 0;
            	} else {
            		worm.points[0] = 0;
            		worm.points[1] = points;
            	}
            	worm.points[2] = 0;
            	worm.points[3] = 0;
            	worm.team = true;
            	worm.invalidate();
    		}
    		
    		// fix diff ticker
    		Ticker tick = (Ticker) view.findViewById(R.id.tickerDiff);
    		tick.setData(getTickerData(home, Teams.TRAF_OVERALL, i_pred_ratio_home, i_pred_ratio_away, cursor), 1, 1);
    		
    		tick = (Ticker) view.findViewById(R.id.tickerAtt);
    		tick.setData(getTickerData(home, Teams.TRAF_ATTACK, i_pred_goals_home, i_pred_goals_away, cursor), 1, 1);
    		
    		tick = (Ticker) view.findViewById(R.id.tickerCS);
    		tick.setData(getTickerData(home, Teams.TRAF_DEF, i_pred_goals_away, i_pred_goals_home, cursor), 1, 1);
		}
		
		// populate Ticker from input (don't use standard Ticker fuction... nothing to parse)
		private TickerData getTickerData(boolean home, int dataType, int i_home, int i_away, Cursor cur) {
			TickerData data = new TickerData();
			data.rating_low = Teams.ratingLow[dataType];
		    data.rating_high = Teams.ratingHigh[dataType];
			data.gameweek = new int[1];
	    	data.gameweek[0] = 1;
	    	data.home = new boolean[1];
	    	data.home[0] = home;
	    	data.rating = new float[1];
	    	float value;
	    	if (home) {
    			value = cur.getFloat(i_home);
    		} else {
    			value = cur.getFloat(i_away);
    		}
	    	data.rating[0] = value / 100f;
	    	if (dataType == Teams.TRAF_DEF) {
	    		data.rating[0] = ProcessTeam.getCSPerc(data.rating[0]);
	    	}
	    	return data;
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final View view = LayoutInflater.from(context).inflate(R.layout.team_screen_gameweek_item, parent, false);
		    return view;
		}
    	
    }
    
    // callback
    public void dataChanged() {
    	popData();
    }
    
    protected void onDestroy() {
	    if (curM != null) {
			curM.close();
		}
	    super.onDestroy();
    }

}
