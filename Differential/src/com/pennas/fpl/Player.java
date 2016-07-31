package com.pennas.fpl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.androidplot.series.XYSeries;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.pennas.fpl.Players.Stat;
import com.pennas.fpl.Selection.ViewPagerAdapter;
import com.pennas.fpl.process.FPLService;
import com.pennas.fpl.process.ProcessData;
import com.pennas.fpl.process.ProcessPlayer;
import com.pennas.fpl.process.ProcessTeam;
import com.pennas.fpl.ui.Pie;
import com.pennas.fpl.ui.Ticker;
import com.pennas.fpl.ui.Worm;
import com.pennas.fpl.ui.Ticker.TickerData;
import com.pennas.fpl.util.DbGen;
import com.pennas.fpl.util.FPLAsyncTask;
import com.pennas.fpl.util.GraphicUtil;
import com.pennas.fpl.util.Refreshable;
import com.pennas.fpl.util.SquadUtil;
import com.pennas.fpl.util.FPLActivity;
import com.viewpagerindicator.TitlePageIndicator;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

@SuppressLint("DefaultLocale")
public class Player extends FPLActivity implements Refreshable {

	SimpleCursorAdapter adapter;
	
	private int playerId, playerFplId, playerPaId, player_position;
	private boolean in_watchlist;
	
	private ListView statsList, matchesList, rivalsList, vidiprinter;
	private RelativeLayout information, upcoming;
	private XYPlot seasongraph;
	private float max_score_gw, max_score_season;
	private Cursor curHist, curVidi, curOppHist;
	
	public static final String P_PLAYERID = "com.pennas.fpl.playerid";
	public static final String P_SEASON = "com.pennas.fpl.season";
	private ArrayList<PStatEntry> statValues;
	private ArrayList<Rival> rivals;
	private boolean includeMeInRivals;
	
	private String playerName, teamName, twitterUsername;
	private float price;
	private ImageView watchlistIcon;
	private Worm seasonWorm;
	private Pie seasonPie;
	private ImageView playerPic, shirtPic, flag;
	private TextView  news, pointsforme, twitterOfficialButton;
	
	private static final int TRAFFIC_FIXDIFF = 0;
	private static final int TRAFFIC_PREDSCORE = 1;
	private static final int TRAFFIC_PREDCS = 2;
	
	private static final int SEASON_ALL = 0;
	public static final int[] seasons = { 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 0 };
	public static final String[] season_desc = { "2015/16", "2014/15", "2013/14", "2012/13", "2011/12", "2010/11", "2009/10", "2008/09", "2007/08", "2006/07", "All Seasons" };
	
	private int season = App.season;
	private int upcoming_game = 0;
	
	private static final int ASSIST_PTS = 3;
	
	private static final int FILTER_SEASON = 1;
	private static final int FILTER_UPCOMING_GAME = 2;
	
	private ViewPager viewPager;
	
	private String[] upcoming_games_text = new String[Players.NUM_TEAMS];
	private int[] upcoming_games_fix_id = new int[Players.NUM_TEAMS];
	private int[] upcoming_games_team_id = new int[Players.NUM_TEAMS];
	private UpcomingPackage upcoming_stats;
	
	private static class UpcomingPackage {
		public int opp_team_id;
		public String opp_name;
		public long datetime;
		public float pred_total_pts;
		public float ppg_vs;
	}
	
	private static final int TAB_STATS = 0;
	private static final int TAB_UPCOMING = 1;
	private static final int TAB_GAMES = 2;
	private static final int TAB_INFO_RIVALS = 3;
	private static final int TAB_VIDIPRINTER = 4;
	private static final int TAB_GRAPH = 5;
	
	private int games_in_squad, games_out_of_squad, points_in_squad, points_out_of_squad;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    setContentView(R.layout.player_screen);
	    
	    hasTips = true;
        tipsPref = Settings.PREF_TIPS_LIVE;
		tips.add("View stats from previous seasons (or overall totals) from filter");
		tips.add("Click the star to add/remove from watchlist. This updates your FPL watchlist");
		tips.add("Stats and predictions are updated by Processing from sync panel");
		tips.add("Green/red backgrounds show if player was selected/benched by you (or rival) that week");
		tips.add("'Pie' shows breakdown of bad/good/excellent games");
		tips.add("Official player twitter links are manually added, so may be missing");
		tips.add("Click on a stat to see leading players in that statistic");
		tips.add("See which rivals have this player on the Info tab");
		
	    //if (App.cur_gameweek < 1) season = App.season - 1;
	    
	    // get ID of player
	    Bundle extras = getIntent().getExtras();
	    if(extras ==null ) return;
	    playerId = extras.getInt(P_PLAYERID);
	    if (extras.containsKey(P_SEASON)) {
	    	season = extras.getInt(P_SEASON);
	    }
	    
	    // season filter: populate (static)
	    addFilter(FILTER_SEASON, "Season", season_desc, getSeasonInd(season), true, false);
		
	    playerPic = (ImageView) findViewById(R.id.playerPic);
	 	shirtPic  = (ImageView) findViewById(R.id.shirtPic);
	 	flag      = (ImageView) findViewById(R.id.flag);
	 	news      = (TextView)  findViewById(R.id.news);
	 	pointsforme = (TextView) findViewById(R.id.pointsforme);
	 	seasonWorm    	    = (Worm)  findViewById(R.id.seasonworm);
	 	seasonPie    	    = (Pie)  findViewById(R.id.seasonpie);
	 	
	 	LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	 	information = (RelativeLayout) inflater.inflate(R.layout.player_screen_information_tab, null);
	 	upcoming = (RelativeLayout) inflater.inflate(R.layout.player_screen_upcoming_tab, null);
	 	seasongraph = (XYPlot) inflater.inflate(R.layout.graph, null);
	 	
	 	rivalsList = (ListView) information.findViewById(R.id.rivalslist);
	 	twitterOfficialButton = (TextView) information.findViewById(R.id.twitterOfficialButton);
	 	
	 	matchesList = new ListView(this);
	 	statsList = new ListView(this);
	 	vidiprinter = new ListView(this);
	 	
	 	watchlistIcon = (ImageView) findViewById(R.id.watchlisticon);
	 	
	 	// click listener for stats list, goes to players screen, sorted by that stat
		OnItemClickListener listListener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
	            // load right stats screen
				Intent intent = new Intent(view.getContext(), Players.class);
				PStatEntry it = statValues.get(position);
				intent.putExtra(Players.P_STATID, it.statId);
				intent.putExtra(Players.P_SEASON, season);
				startActivity(intent);
	        }
	    };
		statsList.setOnItemClickListener(listListener);
		
		viewPager = (ViewPager) findViewById(R.id.viewpager);
		final String[] titles = new String[] { "Stats", "Upcoming", "Games", "Info/Rivals", "Vidiprinter", "Graph"};
		final View[] views = new View[] { statsList, upcoming, matchesList, information, vidiprinter, seasongraph };
		ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(titles, views);
	    TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.titles);
	    viewPager.setAdapter(pagerAdapter);
	    indicator.setViewPager(viewPager);
	    viewPager.setCurrentItem(2);
	    
	    String[] tmp = new String[1];
	    addFilter(FILTER_UPCOMING_GAME, "Vs", tmp, 0, false, true);
	    
	    indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				if(position == 1) {
        	    	// upcoming
					setFilterVisible(FILTER_UPCOMING_GAME, true);
        	    } else {
        	    	setFilterVisible(FILTER_UPCOMING_GAME, false);
        	    }
				
				set_screenshot_view();
			}

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				
			}

			@Override
			public void onPageScrollStateChanged(int state) {
				
			}
		});
	    
		// load player data into matches list, top section, etc
        popData(true);
	}
	
	private void popTop() {
    	// get player details from db
        Cursor cur = App.dbGen.rawQuery("SELECT p._id, p.name, p.team_id, p.fpl_yellow_flag, p.fpl_red_flag, p.diff_flag"
        		+ ", ps.price price, p.code_14, ps.fpl_id, p.twitter_username, ps.position, p.fpl_news"
        		+ " FROM player p, player_season ps"
        		+ " WHERE ps.player_id = p._id AND ps.season = " + App.season + " AND p._id = " + playerId, null);
	 	cur.moveToFirst();
	 	if (!cur.isAfterLast()) {
		 	playerName = cur.getString(cur.getColumnIndex("name"));
		 	int teamId = cur.getInt(cur.getColumnIndex("team_id"));
		 	teamName = App.id2team.get(teamId);
		 	
		 	twitterUsername = cur.getString(cur.getColumnIndex("twitter_username"));
		 	if (twitterUsername == null) {
		 		//twitterOfficialButton.setTextColor(0xcc000000);
		 		twitterOfficialButton.setVisibility(View.GONE);
		 	}
		 	
		 	player_position = cur.getInt(cur.getColumnIndex("position"));
		 	
		 	playerPaId = cur.getInt(cur.getColumnIndex("code_14"));
		 	
		 	// init GUI from data
		 	int imgResource = Players.getPlayerPic(this, playerPaId);
		 	GraphicUtil g = new GraphicUtil(this);
		 	playerPic.setImageBitmap(g.getReflection(imgResource, null, false, null, true, false, false));
		 	shirtPic.setImageResource(SquadUtil.getShirtResource(teamId));
		 	price = cur.getFloat(cur.getColumnIndex("price")) / 10f;
		 	actionBar.setTitle(playerName);
		 	actionBar.setSubtitle("£" + price + ": " + Players.position_text_exp[player_position - 1] + " " + season_desc[getSeasonInd(season)]);
		 	playerFplId = cur.getInt(cur.getColumnIndex("fpl_id"));
		 	
		 	byte[] news_compressed = cur.getBlob(cur.getColumnIndex("fpl_news"));
			String newsString = DbGen.decompress(news_compressed);
		 	news.setText(newsString);
			
			int fpl_yellow_flag = cur.getInt(cur.getColumnIndex("fpl_yellow_flag"));
		 	int fpl_red_flag = cur.getInt(cur.getColumnIndex("fpl_red_flag"));
		 	int diff_flag = cur.getInt(cur.getColumnIndex("diff_flag"));
		 	
		 	if (fpl_yellow_flag == 1) {
		 		flag.setImageResource(R.drawable.yellow_flag);
		 	} else if (fpl_red_flag == 1) {
		 		flag.setImageResource(R.drawable.red_flag);
		 	} else if (diff_flag == 1) {
		 		flag.setImageResource(R.drawable.diff_flag);
		 	} else {
		 		flag.setImageResource(0);
		 	}
	 	}
	 	
	 	cur.close();
	 	
	 	popTopWatchlist();
    }
    
    public static int getSeasonInd(int season) {
		for (int i=0; i<seasons.length; i++) {
			if (seasons[i] == season) return i;
		}
		return 0;
	}
	
	private void drawGraph() {
		int pos_goal_pts;
		if (player_position <= 2) {
			pos_goal_pts = 6;
		} else if (player_position == 3) {
			pos_goal_pts = 5;
		} else {
			pos_goal_pts = 4;
		}
		String [] stat_field = { "(goals * " + pos_goal_pts + ") goal_pts",
				 "bonus", "(assists * 3) assists", "total", "yellow", "red" };
		String [] stat_desc = { "Goal Pts",
				 "Bon", "Ass", "Pts", "Yel", "Red" };
		int [] colour = { 0xff00ffff,
				 0xff0000ff, 0xff00ff00, 0xffff7e00, 0xffffff00, 0xffff0000 };
		
		int num_series = stat_field.length;
		
		StringBuffer query = new StringBuffer();
		query.append("SELECT ");
		for (int p=0; p<num_series; p++) {
			if (p>0) query.append(", ");
			query.append(stat_field[p]);
		}
		query.append(", gameweek FROM player_match WHERE season = " + season
				+ " AND player_player_id = " + playerId + " AND result_points IS NOT NULL ORDER BY gameweek, fixture_id ASC");
		Cursor curGraph = App.dbGen.rawQuery(query.toString(), null);
		curGraph.moveToFirst();
		//int num_games = curGraph.getCount();
		
		Number[][] series = new Number[num_series][App.num_gameweeks + 1];
		
		while(!curGraph.isAfterLast()) {
			for (int p=0; p<num_series; p++) {
				int gw = curGraph.getInt(num_series);
				int newVal;
				newVal = curGraph.getInt(p);
				if (series[p][gw] != null) {
					newVal += series[p][gw].intValue();
				}
				series[p][gw] = newVal;
			}
			
			curGraph.moveToNext();
		}
		curGraph.close();
		
		if (seriesGraph != null) {
			for (int p=0; p<num_series; p++) {
				seasongraph.removeSeries(seriesGraph[p]);
			}
		}
		
		seriesGraph = new XYSeries[num_series];
		for (int p=0; p<num_series; p++) {
			seriesGraph[p] = new SimpleXYSeries(Arrays.asList(series[p]), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, stat_desc[p]);
			seasongraph.addSeries(seriesGraph[p], new LineAndPointFormatter(colour[p], colour[p], null));
		}
		
		seasongraph.disableAllMarkup();
		seasongraph.setRangeLabel("");
		seasongraph.setDomainLabel("Games");
		seasongraph.setDomainLowerBoundary(1, BoundaryMode.FIXED);
		seasongraph.getGraphWidget().setDomainValueFormat(new DecimalFormat("0"));
		seasongraph.getGraphWidget().setRangeValueFormat(new DecimalFormat("0"));
		
		seasongraph.redraw();
	}
	
	private XYSeries[] seriesGraph;
	
	// class describing a stat entry
	private static class PStatEntry {
		public String desc;
		public String value;
		public String statId;
	}
	
	// class describing a stat entry
	private static class Rival {
		public String team_name;
		public String player_name;
		public int team_id;
		public boolean selected;
		public boolean captain;
		public boolean vice_captain;
		public boolean autosub;
		public boolean vc_auto;
		public float bought_price;
		public float sell_price;
	}
	
	// click handler - load web browser for google news search on player
	public void googleNews(View v) {
		//Uri uri = Uri.parse( "http://news.google.co.uk/news/search?q=" + Uri.encode(playerName + " " + teamName));
		Uri uri = Uri.parse( "http://www.google.com/m/search?tbs=nws:1&ned=uk&q=" + Uri.encode(playerName + " " + teamName));
		startActivity( new Intent( Intent.ACTION_VIEW, uri ) );
    }
	// click handler - load web browser for twitter search on player
    public void twitterSearch(View v) {
		Uri uri = Uri.parse( "http://search.twitter.com/search?q=" + Uri.encode(playerName + " " + teamName));
		startActivity( new Intent( Intent.ACTION_VIEW, uri ) );
    }
    // click handler - load web browser for twitter search on player
    // http://www.footballersontwitter.com/premier-league-players-on-twitter.html
    public void twitterOfficial(View v) {
		if (twitterUsername != null) {
	    	Uri uri = Uri.parse( "http://twitter.com/" + twitterUsername);
			startActivity( new Intent( Intent.ACTION_VIEW, uri ) );
		}
    }
	
    // adapter for stats listview
	private class PStatEntryAdapter extends ArrayAdapter<PStatEntry> {

		private ArrayList<PStatEntry> items;

		public PStatEntryAdapter(Context context, int textViewResourceId, ArrayList<PStatEntry> items) {
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
			
			PStatEntry it = items.get(position);
			if (it != null) {
				TextView t = (TextView) v.findViewById(R.id.statdesc);
				t.setText(it.desc);
				t = (TextView) v.findViewById(R.id.statvalue);
				t.setText(it.value);
			}
			
			return v;
		}
	}
	
	// adapter for rivals
	private class RivalAdapter extends ArrayAdapter<Rival> {

		private ArrayList<Rival> items;
		
		public RivalAdapter(Context context, int textViewResourceId, ArrayList<Rival> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		        v = vi.inflate(R.layout.player_screen_rival_item, null);
			}
			
			Rival r = items.get(position);
			if (r != null) {
				if (r.selected) {
					v.setBackgroundResource(R.drawable.list_item_gradient_green);
	            } else {
	            	v.setBackgroundResource(R.drawable.list_item_gradient_red);
	            }
				
				TextView t = (TextView) v.findViewById(R.id.teamname);
				t.setText(r.team_name);
				t = (TextView) v.findViewById(R.id.playername);
				t.setText(r.player_name);
				
				TextView c = (TextView) v.findViewById(R.id.captain);
            	if (r.captain) {
            		if (r.vc_auto) {
	            		c.setText(T_CAPTAIN_AUTO);
	            	} else {
	            		c.setText(T_CAPTAIN);
	            	}
	            	c.setVisibility(View.VISIBLE);
	            } else if (r.vice_captain) {
	            	c.setText(T_VICE);
	            	c.setVisibility(View.VISIBLE);
	            } else {
	            	c.setVisibility(View.INVISIBLE);
	            }
            	
            	ImageView a = (ImageView) v.findViewById(R.id.autosubicon);
	            if (r.autosub) {
	            	a.setVisibility(View.VISIBLE);
	            	if (r.selected) {
	            		a.setImageResource(R.drawable.icon_up_green);
	            		a.setAlpha(ALPHA);
	            	} else {
	            		a.setImageResource(R.drawable.icon_down_orange);
	            		a.setAlpha(ALPHA);
	            	}
	            } else {
	            	a.setVisibility(View.GONE);
	            }
	            
	            t = (TextView) v.findViewById(R.id.boughtsold);
            	if (r.sell_price == NOT_MY_PLAYER) {
	            	t.setVisibility(View.GONE);
	            } else {
	            	t.setVisibility(View.VISIBLE);
	            	t.setText("Bought: £" + r.bought_price + "M  Sell: £" + r.sell_price + "M");
	            }
			}
			
			return v;
		}
	}
	
	private void closeCursors() {
		if (curVidi != null) {
			curVidi.close();
		}
		if (curHist != null) {
			curHist.close();
		}
		if (curOppHist != null) {
			curOppHist.close();
		}
	}
	
	// async task to load data
    private class LoadTask extends FPLAsyncTask<Void, Void, Void> {
    	private boolean main;
    	public LoadTask(boolean p_main) {
    		main = p_main;
    	}
    	protected Void doInBackground(Void... params) {
    		if (main) {
    			popDataMain();
    		}
    		popDataUpcoming();
    		return null;
        }
		protected void onPreExecute() {
			progress = new ProgressDialog(Player.this);
			progress.setMessage("Loading..");
			progress.setCancelable(false);
			progress.show();
			closeCursors();
		}
        protected void onPostExecute(Void res) {
        	if (main) {
	        	seasonWorm.invalidate();
	        	
	        	updateFilterList(FILTER_UPCOMING_GAME, upcoming_games_text);
	    		
	    		// pie
		    	if ( (season >= ProcessPlayer.SEASON_FULL_START) || (season == ProcessPlayer.ALL_SEASONS) ) {
		    		seasonPie.setVisibility(View.VISIBLE);
		    		seasonPie.invalidate();
		    	} else {
		    		seasonPie.setVisibility(View.INVISIBLE);
		    	}
	        	
	        	if (season == SEASON_ALL) {
	        		String[] adapter_from = new String[] { "_id", "points" };
	    			int[] adapter_to = new int[] { R.id.season, R.id.worm };
	    			SimpleCursorAdapter adapterM = new SimpleCursorAdapter(Player.this, R.layout.player_screen_season_item, curHist, adapter_from, adapter_to);
	    			adapterM.setViewBinder(new SeasonViewBinder());
	    			matchesList.setAdapter(adapterM);
	    			
	    			OnItemClickListener listListener = new OnItemClickListener() {
	    				public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
	    					if (season == SEASON_ALL) {
	    						season = (int) id;
	    						popData(true);
	    					}
	    				}
	    		    };
	    		    matchesList.setOnItemClickListener(listListener);
	        	} else {
	        		String[] adapter_from = new String[] { "gameweek", "total", "opp_team_id", "team_name", "res_goals_home", "pred_total_pts", "fix_rating", "fix_cs", "minutes", "captain", "price" };
	    			int[] adapter_to = new int[] { R.id.gameweek, R.id.worm, R.id.shirtPic, R.id.teamName, R.id.result, R.id.tickerPred, R.id.tickerFix, R.id.tickerCS, R.id.minutes, R.id.captain, R.id.value };
	    			SimpleCursorAdapter adapterM = new SimpleCursorAdapter(Player.this, R.layout.player_screen_gameweek_item, curHist, adapter_from, adapter_to);
	    			adapterM.setViewBinder(matchesBinder);
	    			matchesList.setAdapter(adapterM);
	        		
	        		OnItemClickListener listListener = new OnItemClickListener() {
	    				public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
	    					// load fixture screen for fixture "id"
	    					if (season != SEASON_ALL) {
		    					Intent intent = new Intent(view.getContext(), Fixture.class);
		    					intent.putExtra(Fixture.P_FIXTURE_ID, (int) id);
		    			    	startActivity(intent);
	    					}
	    				}
	    		    };
	    		    matchesList.setOnItemClickListener(listListener);
	    		    // set position in list to current/next gw if possible
	    			if (defPos > -1) matchesList.setSelection(defPos);
	        	}
	        	
	        	// adapter for stats array->list. sort by stat description
	        	PStatEntryAdapter statAdapter = new PStatEntryAdapter(Player.this, R.layout.player_screen_stat_item, statValues);
	    	 	statAdapter.sort(new Comparator<PStatEntry>() {
	        		public int compare(PStatEntry object1, PStatEntry object2) {
	        			return object1.desc.compareTo(object2.desc);
	        		}
	        	});
	    	 	statsList.setAdapter(statAdapter);
	        	
	    	 	String[] columns = new String[] { "datetime", "message", "category", "gameweek" };
	    		int[] to = new int[] { R.id.datetime, R.id.message, R.id.cat, R.id.gameweek };
	    		SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(Player.this, R.layout.vidiprinter_screen_item, curVidi, columns, to);
	    		mAdapter.setViewBinder(new Vidiprinter.MyViewBinder(false));
	    		vidiprinter.setAdapter(mAdapter);
	    		
	    		float ppg_in_squad = points_in_squad;
	    		if (games_in_squad > 0) {
	    			ppg_in_squad /= games_in_squad;
	    		} else {
	    			ppg_in_squad = 0;
	    		}
	    		float ppg_out_of_squad = points_out_of_squad;
	    		if (games_out_of_squad > 0) {
	    			ppg_out_of_squad /= games_out_of_squad;
	    		} else {
	    			ppg_out_of_squad = 0;
	    		}
	    		String for_me_summary = games_in_squad + " games in my squad, scoring " + points_in_squad
	    				+ " at " + ProcessData.trunc(ppg_in_squad, 1)
	    				+ ". " + games_out_of_squad + " games not in my squad, scoring " + points_out_of_squad
	    				+ " at " + ProcessData.trunc(ppg_out_of_squad, 1);
	    	 	
	        	TextView rivalstext = (TextView) information.findViewById(R.id.rivalstext);
	        	int rivalsCount = rivals.size();
	        	String andMe = "";
	        	if (includeMeInRivals) {
	        		rivalsCount--;
	        		andMe = " (plus me)";
	        	}
	    		rivalstext.setText(for_me_summary + "\n" + rivalsCount + " rivals" + andMe + " have " + playerName + ":");
	    		// adapter for rivals array->list
	    	 	RivalAdapter rivalAdapter = new RivalAdapter(Player.this, R.layout.player_screen_rival_item, rivals);
	    	 	rivalAdapter.sort(new Comparator<Rival>() {
	        		@SuppressLint("DefaultLocale")
					public int compare(Rival object1, Rival object2) {
	        			// my team always goes at top
	        			if (object1.team_id == App.my_squad_id) {
	        				return -1;
	        			}
	        			if (object2.team_id == App.my_squad_id) {
	        				return 1;
	        			}
	        			return object1.player_name.toUpperCase().compareTo(object2.player_name.toUpperCase());
	        		}
	        	});
	    	 	rivalsList.setAdapter(rivalAdapter);
	    	 	OnItemClickListener listListener = new OnItemClickListener() {
	    			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
	    				// load squad screen
	    				Intent intent = new Intent(view.getContext(), FPLSquad.class);
	    				intent.putExtra(FPLSquad.P_SQUADID, rivals.get(position).team_id);
	    		    	startActivity(intent);
	    			}
	    	    };
	    	    rivalsList.setOnItemClickListener(listListener);
	    		
	    		drawGraph();
	    		
	    		pointsforme.setText("  " + points_for_me + " points for me");
        	}
    		
    		ImageView opp = (ImageView) upcoming.findViewById(R.id.opponentShirt);
    		opp.setImageResource(SquadUtil.getShirtResource(upcoming_stats.opp_team_id));
    		
    		TextView header = (TextView) upcoming.findViewById(R.id.header);
    		if (upcoming_stats.datetime > 0) {
    			header.setText(App.printDate(upcoming_stats.datetime));
    		} else {
    			header.setText("No scheduled fixture");
    		}
    		
    		TextView blurb = (TextView) upcoming.findViewById(R.id.blurb);
    		String blurbExtra = "";
    		if (upcoming_stats.datetime > 0) {
    			blurbExtra = ", and is predicted to score " + generalise_prediction(upcoming_stats.pred_total_pts) + " in this match";
    		}
    		blurb.setText(playerName + " has averaged " + upcoming_stats.ppg_vs + " points in " + curOppHist.getCount()
    				+ " games against " + upcoming_stats.opp_name + blurbExtra + ". Previous matches against "
    				+ upcoming_stats.opp_name + " are listed below");
    		
    		ListView oppHist = (ListView) upcoming.findViewById(R.id.pastList);
    		String[] adapter_from = new String[] { "gameweek", "total", "opp_team_id", "team_name", "res_goals_home", "pred_total_pts", "fix_rating", "fix_cs", "minutes", "captain", "price" };
			int[] adapter_to = new int[] { R.id.gameweek, R.id.worm, R.id.shirtPic, R.id.teamName, R.id.result, R.id.tickerPred, R.id.tickerFix, R.id.tickerCS, R.id.minutes, R.id.captain, R.id.value };
			SimpleCursorAdapter adapterM = new SimpleCursorAdapter(Player.this, R.layout.player_screen_gameweek_item, curOppHist, adapter_from, adapter_to);
			adapterM.setViewBinder(matchesBinder);
			oppHist.setAdapter(adapterM);
			
			OnItemClickListener listListener = new OnItemClickListener() {
				public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
					// load fixture screen for fixture "id"
					Intent intent = new Intent(view.getContext(), Fixture.class);
    				intent.putExtra(Fixture.P_FIXTURE_ID, (int) id);
    			    startActivity(intent);
				}
		    };
		    oppHist.setOnItemClickListener(listListener);
		    
		    set_screenshot_view();
    		
    		progress.dismiss();
    		loader = null;
        }
    }
    
    //final View[] views = new View[] { statsList, upcoming, matchesList, information, vidiprinter, seasongraph };
    private void set_screenshot_view() {
    	String player = playerName + ", for " + season_desc[getSeasonInd(season)];
    	if (viewPager.getCurrentItem() == TAB_STATS) {
			setScreenshotView(statsList, "Stats for " + player);
		} else if (viewPager.getCurrentItem() == TAB_UPCOMING) {
			if (upcoming_stats != null) {
				setScreenshotView(upcoming, "History/predictions for " + playerName + " vs " + upcoming_stats.opp_name);
			} else {
				setScreenshotView(null, null);
			}
		} else if (viewPager.getCurrentItem() == TAB_GAMES) {
			setScreenshotView(matchesList, "Season fixtures for " + player);
		} else if (viewPager.getCurrentItem() == TAB_INFO_RIVALS) {
			setScreenshotView(information, "Rivals who have " + playerName);
		} else if (viewPager.getCurrentItem() == TAB_VIDIPRINTER) {
			setScreenshotView(vidiprinter, "Vidiprinter for " + playerName);
		} else if (viewPager.getCurrentItem() == TAB_GRAPH) {
			setScreenshotView(seasongraph, "Graph for " + player);
		}
	}
    
    // 6.5 -> 6-7
    // 6.2 -> 5.5-6.5
    // 6.0 -> ..
    public static final String generalise_prediction(float pred) {
    	float doub = Math.round(pred * 2);
    	float doub_min = doub - 1;
    	float doub_plus = doub + 1;
    	float from = doub_min / 2;
    	float to = doub_plus / 2;
    	String ret = from + "-" + to;
    	//App.log("pred: " + pred + " -> " + ret);
    	return ret;
    }
    
    private MyViewBinder matchesBinder = new MyViewBinder();
    
    private void popTopWatchlist() {
    	Integer in = playerFplId;
		if (App.watchlist.contains(in)) {
	 		in_watchlist = true;
	 		watchlistIcon.setImageResource(R.drawable.btn_star_big_on);
	 	} else {
	 		in_watchlist = false;
	 		watchlistIcon.setImageResource(R.drawable.btn_star_big_off);
		}
    }
    
    private LoadTask loader;
    
    private void popData(boolean main) {
    	popTop();
    	
    	loader = new LoadTask(main);
		loader.fplExecute();
    }
    
    //private PStatEntryAdapter statAdapter;
    //private SimpleCursorAdapter adapterM;
    private int defPos;
    private int points_for_me;
    
    private static final int NOT_MY_PLAYER = -1;
	
	// populate data
	private void popDataMain() {
		App.log("Player::popData() season=" + season);
		
		// get all players max gw score from db for worm
		if (season == SEASON_ALL) {
			Cursor curMax = App.dbGen.rawQuery("SELECT MAX(points) score FROM player_season WHERE season != " + SEASON_ALL, null);
		 	curMax.moveToFirst();
		 	max_score_gw = curMax.getFloat(0);
		 	curMax.close();
		} else if ( (season == App.season) && (App.cur_gameweek == 0) ) {
			Cursor curMax = App.dbGen.rawQuery("SELECT MAX(total) max_score FROM player_match", null);
		 	curMax.moveToFirst();
		 	max_score_gw = curMax.getFloat(0);
		 	curMax.close();
		} else {
		 	Cursor curMax = App.dbGen.rawQuery("SELECT MAX(total) max_score FROM player_match WHERE season = " + season, null);
		 	curMax.moveToFirst();
		 	max_score_gw = curMax.getFloat(0);
		 	curMax.close();
		}
		
		// get player stats from db
    	// use prev season for this if gw0
    	int stat_season = season;
    	if ( (App.cur_gameweek == 0) && (stat_season == App.season) ) {
    		stat_season --;
    	}
	 	
	 	Cursor minmax = App.dbGen.rawQuery("SELECT MAX(points) score FROM player_season WHERE season = " + stat_season, null);
	 	minmax.moveToFirst();
	 	max_score_season = minmax.getFloat(0);
	 	minmax.close();
	 	
	 	statValues = new ArrayList<PStatEntry>();
	 	seasonWorm.max_pts = max_score_season;
		seasonWorm.total_pts = 0;
		seasonWorm.points[0] = 0;
    	seasonWorm.points[1] = 0;
    	seasonWorm.points[2] = 0;
    	seasonWorm.points[3] = 0;
	 	
	 	Cursor curStat = App.dbGen.rawQuery("SELECT *,"
	 + " (CASE WHEN position <= 2 THEN goals * 6 WHEN position = 3 THEN goals * 5 WHEN position = 4 THEN goals * 4 END) goal_pts"
	 + " FROM player_season WHERE season = " + stat_season + " AND player_id = " + playerId, null);
	 	curStat.moveToFirst();
	 	if (!curStat.isAfterLast()) {
		 	Players.loadStats();
		 	for (Stat s : Players.playerstats_array) {
		 		String val;
		 		if (s.divide > 0) {
		 			float num = curStat.getFloat(curStat.getColumnIndex(s.db_field)) / s.divide;
		 			val = String.valueOf(num);
		 		} else {
		 			val = curStat.getString(curStat.getColumnIndex(s.db_field));
		 		}
		 		if (s.show_value && (val != null) ) {
		 			PStatEntry stat = new PStatEntry();
		 			stat.desc = s.desc;
		 			stat.value = val;
		 			stat.statId = s.db_field;
		 			statValues.add(stat);
		 		}
		 	}
		 	
		 	//playerSeasonFplId = curStat.getInt(curStat.getColumnIndex("fpl_id"));
		 	
		 	// initialise the big season worm at the top of the screen
		 	seasonWorm.total_pts = curStat.getInt(curStat.getColumnIndex("points"));
	    	// 8/9/10 assists/bonus/goals
	    	int ass_pts = curStat.getInt(curStat.getColumnIndex("assists")) * ASSIST_PTS;
	    	int bon_pts = curStat.getInt(curStat.getColumnIndex("bonus"));
	    	int goal_pts = curStat.getInt(curStat.getColumnIndex("goal_pts"));
	    	int app_pts = seasonWorm.total_pts - ass_pts - bon_pts - goal_pts;
	    	seasonWorm.points[0] = app_pts;
	    	seasonWorm.points[1] = goal_pts;
	    	seasonWorm.points[2] = ass_pts;
	    	seasonWorm.points[3] = bon_pts;
	    	
	    	// pie
	    	if ( (stat_season >= ProcessPlayer.SEASON_FULL_START) || (stat_season == ProcessPlayer.ALL_SEASONS) ) {
	    		int g_x = curStat.getInt(curStat.getColumnIndex("c_games_x_mins"));
	    		int g_o_y = curStat.getInt(curStat.getColumnIndex("c_g_over_y"));
	    		int g_o_z = curStat.getInt(curStat.getColumnIndex("c_g_over_z"));
	    		seasonPie.games[0] = Math.max(g_x - g_o_y, 0);
	    		seasonPie.games[1] = Math.max(g_o_y - g_o_z, 0);
	    		seasonPie.games[2] = Math.max(g_o_z, 0);
	    		//App.log(seasonPie.games[0] + ", " + seasonPie.games[1] + ", " + seasonPie.games[2]);
	    	}
	 	} else {
	 		App.log("season stats record not found: " + stat_season);
	 	}
	 	curStat.close();
	 	
	 	if (season == SEASON_ALL) {
			App.log("all seasons history");
			curHist = App.dbGen.rawQuery("SELECT season _id, points, (assists * 3) assist_pts, bonus, (CASE WHEN position <= 2 THEN goals * 6 WHEN position = 3 THEN goals * 5 WHEN position = 4 THEN goals * 4 END) goal_pts FROM player_season WHERE player_id = " + playerId + " AND season != " + SEASON_ALL, null);
			curHist.moveToFirst();
		} else {
			App.log("season " + season + " history");
			curHist = App.dbGen.rawQuery("SELECT pm.fixture_id _id, f.gameweek, pm.total, (pm.assists * 3) assist_pts"
					+ ", pm.bonus bonus_pts, (CASE WHEN ps.position <= 2 THEN pm.goals * 6 WHEN ps.position = 3 THEN pm.goals * 5 WHEN ps.position = 4 THEN pm.goals * 4 END) goal_pts"
					+ ", pm.opp_team_id, t.name team_name, pm.is_home, f.res_goals_home, f.res_goals_away, (pm.pred_total_pts/100) pred_total_pts"
					+ ", (CASE WHEN pm.is_home = 1 THEN f.pred_ratio_home ELSE f.pred_ratio_away END) fix_rating"
					+ ", (CASE WHEN pm.is_home = 1 THEN f.pred_goals_away ELSE f.pred_goals_home END) fix_cs, pm.minutes"
					+ ", (sgp.selected + 1) selected, sgp.captain, sgp.autosub, sgp.vc, pm.price, f.datetime"
					+ " FROM player_match pm, player_season ps, team t, fixture f"
					+ " LEFT OUTER JOIN squad_gameweek_player sgp"
					+ " ON sgp.squad_id = " + App.my_squad_id + " AND sgp.gameweek = pm.gameweek AND sgp.fpl_player_id = ps.fpl_id"
					+ " WHERE pm.player_player_id = ps.player_id AND pm.season =  " + season + " AND ps.season = " + season
					+ " AND ps.player_id = " + playerId + " AND t._id = pm.opp_team_id AND f._id = pm.fixture_id"
					+ " ORDER BY f.datetime ASC", null);
			// get current/next gw list position if possible
		 	curHist.moveToFirst();
		 	defPos = -1;
		 	while (!curHist.isAfterLast()) {
				int gw = curHist.getInt(1);
				if ( (gw == App.cur_gameweek) || (gw == App.next_gameweek) ) {
					// -3 to try and position current gw in middle of screen
					defPos = curHist.getPosition() - 3;
					break;
				} // if
				curHist.moveToNext();
			} // while
		 	
		 	// only do 'upcoming' populate if current season
		 	if (season == App.season) {
			 	int upcoming_i = 0;
			 	long cur_time = App.currentUkTime();
			 	curHist.moveToFirst();
			 	while (!curHist.isAfterLast()) {
			 		long datetime = curHist.getLong(curHist.getColumnIndex("datetime"));
			 		if (datetime >= cur_time) {
			 			int fix_id = curHist.getInt(0);
			 			int opp_id = curHist.getInt(curHist.getColumnIndex("opp_team_id"));
			 			if (!teamInList(opp_id)) {
			 				int gw = curHist.getInt(1);
			 				upcoming_games_text[upcoming_i] = gw + " - " + curHist.getString(curHist.getColumnIndex("team_name"));
			 				upcoming_games_fix_id[upcoming_i] = fix_id;
			 				upcoming_games_team_id[upcoming_i] = opp_id;
			 				
			 				upcoming_i++;
			 			}
			 		}
			 		curHist.moveToNext();
			 	}
			 	// add any teams that don't have any scheuled fix left
			 	Cursor clubs = App.dbGen.rawQuery("SELECT _id, name FROM team WHERE active = 1 ORDER BY NAME ASC", null);
			    clubs.moveToFirst();
			    while (clubs.isAfterLast() == false) {
		            int team_id = clubs.getInt(0);
		            if (!teamInList(team_id)) {
		            	upcoming_games_text[upcoming_i] = clubs.getString(1);
		 				upcoming_games_fix_id[upcoming_i] = 0;
		 				upcoming_games_team_id[upcoming_i] = team_id;
		 				
		 				upcoming_i++;
		            }
			    	clubs.moveToNext();
		        }
			 	clubs.close();
		 	} // do upcoming
		} // season/else
		
	 	curVidi = App.dbGen.rawQuery("SELECT _id, gameweek, datetime, message, category"
	 			+ " FROM vidiprinter WHERE player_fpl_id = " + playerFplId
	 			+ " ORDER BY gameweek DESC, datetime DESC LIMIT " + Vidiprinter.limit, null);
		curVidi.moveToFirst();
	 	
		includeMeInRivals = false;
		rivals = new ArrayList<Rival>();
		Cursor curRiv = App.dbGen.rawQuery("SELECT s._id, s.name, s.player_name, sgp.selected, sgp.captain, sgp.autosub, sgp.vc"
				+ " FROM squad s, squad_gameweek_player sgp"
				+ " WHERE sgp.fpl_player_id = " + playerFplId + " AND s._id = sgp.squad_id AND sgp.gameweek = " + App.cur_gameweek
				+ " AND (s.rival = 1 OR s._id = " + App.my_squad_id + ")", null);
		curRiv.moveToFirst();
		while(!curRiv.isAfterLast()) {
			Rival r = new Rival();
			r.team_id = curRiv.getInt(0);
			r.team_name = DbGen.decompress(curRiv.getBlob(1));
			r.player_name = DbGen.decompress(curRiv.getBlob(2));
			r.selected = (curRiv.getInt(3) == SquadUtil.SEL_SELECTED);
			r.captain = (curRiv.getInt(4) == SquadUtil.CAPTAIN);
			r.vice_captain = (curRiv.getInt(4) == SquadUtil.VICE_CAPTAIN);
			r.autosub = (curRiv.getInt(5) == 1);
			r.vc_auto = (curRiv.getInt(6) == 1);
			
			if (r.autosub) {
            	r.selected = !r.selected;
            }
			if (r.vc_auto && r.vice_captain) {
            	r.captain = true;
            }
			
			// find out bought_price from NEXT GW selection (if there is one)
			r.sell_price = NOT_MY_PLAYER;
			if (r.team_id == App.my_squad_id) {
				includeMeInRivals = true;
				Cursor curBought = App.dbGen.rawQuery("SELECT bought_price FROM squad_gameweek_player"
						+ " WHERE squad_id = " + App.my_squad_id + " AND fpl_player_id = " + playerFplId
						+ " AND gameweek = " + App.next_gameweek + " AND bought_price IS NOT NULL", null);
				curBought.moveToFirst();
				if (!curBought.isAfterLast()) {
					r.bought_price = curBought.getFloat(0);
					r.sell_price = SquadUtil.calc_sell_price(r.bought_price, price);
				}
				curBought.close();
			}
			
			rivals.add(r);
			curRiv.moveToNext();
		}
		curRiv.close();
		
		points_for_me = 0;
		games_in_squad = 0;
		games_out_of_squad = 0;
		points_in_squad = 0;
		points_out_of_squad = 0;
		curHist.moveToFirst();
		final int i_total = curHist.getColumnIndex("total");
		final int i_selected = curHist.getColumnIndex("selected");
		final int i_captain = curHist.getColumnIndex("captain");
		final int i_autosub = curHist.getColumnIndex("autosub");
		final int i_vc = curHist.getColumnIndex("vc");
		final int i_minutes = curHist.getColumnIndex("minutes");
		if (season == App.season) {
			while(!curHist.isAfterLast()) {
				int minutes = curHist.getInt(i_minutes);
				if (minutes > 0) {
					int points = curHist.getInt(i_total);
					int sel = curHist.getInt(i_selected) - 1;
					boolean selected = (sel == SquadUtil.SEL_SELECTED);
					boolean captain = (curHist.getInt(i_captain) == SquadUtil.CAPTAIN);
					boolean vice_captain = (curHist.getInt(i_captain) == SquadUtil.VICE_CAPTAIN);
					boolean autosub = (curHist.getInt(i_autosub) == 1);
					boolean vc_auto = (curHist.getInt(i_vc) == 1);
					
					// -1 for skewed index in query. means is in squad
					if (sel > -1) {
						if (autosub) {
							selected = !selected;
						}
						if (vc_auto && vice_captain) {
							captain = true;
						}
						
						if (selected) {
							points_for_me += points;
							if (captain) {
								points_for_me += points;
							}
						}
						
						games_in_squad++;
						points_in_squad += points;
					} else {
						games_out_of_squad++;
						points_out_of_squad += points;
					}
				} // minutes > 0
				
				curHist.moveToNext();
			} // loop
		} // if current season
	}
	
	private boolean teamInList(int team_id) {
		for (int t : upcoming_games_team_id) {
			if (t == team_id) {
				return true;
			}
		}
		return false;
	}
	
	// load data for selected upcoming game
	private void popDataUpcoming() {
		upcoming_stats = new UpcomingPackage();
		int fix = upcoming_games_fix_id[upcoming_game];
		App.log("popDataUpcoming() [" + upcoming_game + "] for fixture " + fix);
		
		if (fix > 0) {
			Cursor curFix = App.dbGen.rawQuery("SELECT f.datetime, pm.opp_team_id, f.gameweek, pm.pred_total_pts"
										    + " FROM fixture f, player_match pm"
										    + " WHERE f._id = " + fix
										    + " AND pm.fixture_id = f._id AND pm.player_playeR_id = " + playerId, null);
			curFix.moveToFirst();
			if (!curFix.isAfterLast()) {
				upcoming_stats.datetime = curFix.getInt(0);
				upcoming_stats.pred_total_pts = curFix.getFloat(3) / 100;;
			}
			curFix.close();
		}
		
		upcoming_stats.opp_team_id = upcoming_games_team_id[upcoming_game];
		upcoming_stats.opp_name = App.id2team.get(upcoming_stats.opp_team_id);
		
		curOppHist = App.dbGen.rawQuery("SELECT pm.fixture_id _id, ((f.season-1) || '/' || f.season) gameweek, pm.total, (pm.assists * 3) assist_pts"
				+ ", pm.bonus bonus_pts, (CASE WHEN ps.position <= 2 THEN pm.goals * 6 WHEN ps.position = 3 THEN pm.goals * 5 WHEN ps.position = 4 THEN pm.goals * 4 END) goal_pts"
				+ ", 0 opp_team_id, t.name team_name, pm.is_home, f.res_goals_home, f.res_goals_away, 0 pred_total_pts"
				+ ", 0 fix_rating"
				+ ", 0 fix_cs, pm.minutes"
				+ ", (sgp.selected + 1) selected, sgp.captain, sgp.autosub, sgp.vc, pm.price"
				+ " FROM player_match pm, player_season ps, team t, fixture f"
				+ " LEFT OUTER JOIN squad_gameweek_player sgp"
				+ " ON sgp.squad_id = " + App.my_squad_id + " AND sgp.gameweek = pm.gameweek AND sgp.fpl_player_id = ps.fpl_id"
				+ " WHERE pm.player_player_id = ps.player_id AND pm.season = ps.season"
				+ " AND ps.player_id = " + playerId + " AND t._id = pm.opp_team_id AND f._id = pm.fixture_id"
				+ " AND pm.opp_team_id = " + upcoming_stats.opp_team_id
				+ " AND f.got_bonus > 0"
				+ " ORDER BY f.datetime ASC", null);
		curOppHist.moveToFirst();
		float points = 0;
		int games = 0;
		while(!curOppHist.isAfterLast()) {
			points += curOppHist.getInt(2);
			games++;
			curOppHist.moveToNext();
		}
		if (games > 0) {
			upcoming_stats.ppg_vs = ProcessData.trunc(points / games, 2);
		}
	}
	
	// populate Ticker from input (don't use standard Ticker fuction... nothing to parse)
	private static TickerData getTickerData(float value, boolean home, int dataType) {
		TickerData data = new TickerData();
		if (dataType == TRAFFIC_PREDCS) {
			data.rating_low = Teams.CS_LOW;
	    	data.rating_high = Teams.CS_HIGH;
		} else if (dataType == TRAFFIC_FIXDIFF) {
			data.rating_low = Players.RATING_LOW_FIX;
	    	data.rating_high = Players.RATING_HIGH_FIX;
		} else {
	    	data.rating_low = Players.RATING_LOW_PRED;
	    	data.rating_high = Players.RATING_HIGH_PRED;
		}
    	data.gameweek = new int[1];
    	data.gameweek[0] = 1;
    	data.home = new boolean[1];
    	data.home[0] = home;
    	data.rating = new float[1];
    	if (dataType == TRAFFIC_PREDCS) {
    		data.rating[0] = ProcessTeam.getCSPerc(value);
    	} else {
    		data.rating[0] = value;
    	}
    	return data;
	}
	
	private static final int ALPHA = 190;
	private static final String T_CAPTAIN = "C";
	private static final String T_CAPTAIN_AUTO = "C+";
	private static final String T_VICE = "V";
	
	// viewbinder for matches listview
    private class MyViewBinder implements SimpleCursorAdapter.ViewBinder {
    	private int i_minutes = -1;
    	private int i_selected = -1;
    	private int i_captain = -1;
    	private int i_autosub = -1;
    	private int i_vc = -1;
    	
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            int viewId = view.getId();
            if (i_minutes == -1) {
            	i_minutes = cursor.getColumnIndex("minutes");
            	i_selected = cursor.getColumnIndex("selected");
            	i_captain = cursor.getColumnIndex("captain");
            	i_autosub = cursor.getColumnIndex("autosub");
            	i_vc = cursor.getColumnIndex("vc");
            }
            
            if (viewId == R.id.shirtPic) {
            	ImageView i = (ImageView) view;
	            i.setImageResource(SquadUtil.getShirtResource(cursor.getInt(columnIndex)));
	            
	            RelativeLayout rl = (RelativeLayout) view.getParent().getParent();
	            int selected = cursor.getInt(i_selected) - 1;
	            boolean autosub = (cursor.getInt(i_autosub) == 1);
	            
	            if (autosub) {
	            	if (selected == SquadUtil.SEL_SELECTED) {
	            		selected = SquadUtil.SEL_SELECTED + 1;
	            	} else {
	            		selected = SquadUtil.SEL_SELECTED;
	            	}
	            }
	            
	            if (selected == SquadUtil.SEL_SELECTED) {
	            	rl.setBackgroundResource(R.drawable.list_item_gradient_green);
	            } else if (selected > SquadUtil.SEL_SELECTED) {
	            	rl.setBackgroundResource(R.drawable.list_item_gradient_red);
	            } else {
	            	rl.setBackgroundResource(R.drawable.list_item_gradient);
	            }
	            
	            ImageView a = (ImageView) rl.findViewById(R.id.autosubicon);
	            if (autosub) {
	            	a.setVisibility(View.VISIBLE);
	            	if (selected == SquadUtil.SEL_SELECTED) {
	            		a.setImageResource(R.drawable.icon_up_green);
	            		a.setAlpha(ALPHA);
	            	} else {
	            		a.setImageResource(R.drawable.icon_down_orange);
	            		a.setAlpha(ALPHA);
	            	}
	            } else {
	            	a.setVisibility(View.GONE);
	            }
            } else if (viewId == R.id.captain) {
            	TextView t = (TextView) view;
            	int captain = cursor.getInt(i_captain);
            	boolean vc = (cursor.getInt(i_vc) == 1);
	            
            	if (vc) {
            		if (captain == SquadUtil.VICE_CAPTAIN) {
            			captain = SquadUtil.CAPTAIN;
            		}
            	}
            	
	            if (captain == SquadUtil.CAPTAIN) {
	            	if (vc) {
	            		t.setText(T_CAPTAIN_AUTO);
	            	} else {
	            		t.setText(T_CAPTAIN);
	            	}
	            	t.setVisibility(View.VISIBLE);
	            } else if (captain == SquadUtil.VICE_CAPTAIN) {
	            	t.setText(T_VICE);
	            	t.setVisibility(View.VISIBLE);
	            } else {
	            	t.setVisibility(View.INVISIBLE);
	            }
            } else if (viewId == R.id.worm) {
            	int minutes = cursor.getInt(i_minutes);
            	Worm worm = (Worm) view;
            	if (minutes > 0) {
            		worm.showtotal = true;
            	} else {
            		worm.showtotal = false;
            	}
            	worm.showsubscore = false;
            	worm.max_pts = max_score_gw;
            	worm.total_pts = cursor.getInt(columnIndex);
            	// 8/9/10 assists/bonus/goals
            	int ass_pts = cursor.getInt(3);
            	int bon_pts = cursor.getInt(4);
            	int goal_pts = cursor.getInt(5);
            	int app_pts = worm.total_pts - ass_pts - bon_pts - goal_pts;
            	worm.points[0] = app_pts;
            	worm.points[1] = goal_pts;
            	worm.points[2] = ass_pts;
            	worm.points[3] = bon_pts;
            	worm.invalidate();
            } else if ( (viewId == R.id.tickerPred) || (viewId == R.id.tickerFix) ) {
            	if (season == App.season) {
            		float val = cursor.getFloat(columnIndex);
            		if (val != 0) {
	            		Ticker t = (Ticker) view;
		            	boolean home = (cursor.getInt(8) == 1);
		            	int type = TRAFFIC_FIXDIFF;
		            	if (viewId == R.id.tickerPred) type = TRAFFIC_PREDSCORE;
		            	
		            	
		            	if (viewId == R.id.tickerFix) {
		            		val /= 100f;
		            	}
		            	t.setData(getTickerData(val, home, type), 1, 1);
            		}
            	}
            } else if (viewId == R.id.tickerCS) {
            	if (season == App.season) {
	            	float val = cursor.getFloat(columnIndex);
	            	if (val != 0) {
	            		Ticker t = (Ticker) view;
		            	boolean home = (cursor.getInt(8) == 1);
		            	t.setData(getTickerData(val / 100f, home, TRAFFIC_PREDCS), 1, 1);
	            	}
            	}
            } else {
            	TextView t = (TextView) view;
	        	boolean setText = true;
	        	
	        	if (viewId == R.id.teamName) {
	        		String team = "";
        			team = cursor.getString(columnIndex);
        			if (cursor.getInt(8) == 1) {
	        			t.setText(team + " (h)");
	        		} else {
	        			t.setText(team + " (a)");
	        		}
	        		setText = false;
	        	}
	        	
	        	if (viewId == R.id.result) {
	        		boolean home = (cursor.getInt(8) == 1);
	        		int h,a;
	        		// reverse score for away
	        		if (home) {
	        			h = 0;
	        			a = 1;
	        		} else {
	        			h = 1;
	        			a = 0;
	        		}
	        		String homescore = cursor.getString(columnIndex + h);
	        		String awayscore = cursor.getString(columnIndex + a);
	        		if ( (homescore != null) && (awayscore != null) ) {
	        			t.setText(" " + homescore + "-" + awayscore);
	        		} else {
	        			t.setText("");
	        		}
	        		setText = false;
	        	}
	        	
	        	if (viewId == R.id.gameweek) {
	        		RelativeLayout rl = (RelativeLayout) view.getParent();
	        		TextView capt = (TextView) rl.findViewById(R.id.captain);
	        		if (cursor.getInt(columnIndex) == App.cur_gameweek) {
	        			rl.setBackgroundResource(R.drawable.this_gw_gradient);
	        			t.setTextColor(TEXT_BLACK);
	        			capt.setTextColor(TEXT_BLACK);
	        		} else {
	        			rl.setBackgroundColor(0);
	        			t.setTextColor(TEXT_HALF_WHITE);
	        			capt.setTextColor(TEXT_HALF_WHITE);
	        		}
	        	}
	        	
	        	if (viewId == R.id.value) {
	        		float gw_price = cursor.getFloat(columnIndex) / 10f;
	        		if (gw_price > 0) {
	        			t.setText("£" + gw_price);
	        		} else {
	        			t.setText("");
	        		}
	        		setText = false;
	        	}
	        	
	        	if (setText) t.setText(cursor.getString(columnIndex));
            }
	            
	        return true;
        }
    }
    
    public static final int TEXT_HALF_WHITE = 0xffbbbbbb;
    public static final int TEXT_BLACK = 0xff000000;
    
    // viewbinder for matches listview
    private class SeasonViewBinder implements SimpleCursorAdapter.ViewBinder {

		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            int viewId = view.getId();
            
            if (viewId == R.id.worm) {
            	Worm worm = (Worm) view;
            	worm.max_pts = max_score_gw;
            	worm.total_pts = cursor.getInt(columnIndex);
            	// 8/9/10 assists/bonus/goals
            	int ass_pts = cursor.getInt(columnIndex + 1);
            	int bon_pts = cursor.getInt(columnIndex + 2);
            	int goal_pts = cursor.getInt(columnIndex + 3);
            	int app_pts = worm.total_pts - ass_pts - bon_pts - goal_pts;
            	worm.points[0] = app_pts;
            	worm.points[1] = goal_pts;
            	worm.points[2] = ass_pts;
            	worm.points[3] = bon_pts;
            	worm.invalidate();
            } else {
            	TextView t = (TextView) view;
            	if (viewId == R.id.season) {
            		int year_2 = cursor.getInt(columnIndex);
            		int year_1 = year_2 - 1;
            		String year_1_str;
            		if (year_1 >= 10) {
            			year_1_str = String.valueOf(year_1);
            		} else {
            			year_1_str = "0" + year_1;
            		}
            		String year_2_str;
            		if (year_2 >= 10) {
            			year_2_str = String.valueOf(year_2);
            		} else {
            			year_2_str = "0" + year_2;
            		}
            		t.setText("20" + year_1_str + "/" + year_2_str);
            	}
            }
	            
	        return true;
        }
    }
    
    @Override
	protected void filterSelection(int id, int pos) {
    	App.log("subclass");
    	
    	switch(id) {
    		case FILTER_SEASON:
    			if (season != seasons[pos]) {
    	        	season = seasons[pos];
    	        	popData(true);
            	}
    			break;
    		case FILTER_UPCOMING_GAME:
    			if (upcoming_game != pos) {
    				upcoming_game = pos;
    	        	popData(false);
            	}
    			break;
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
    	popData(true);
    }
    
    // listener
    public void clickWatchlist(View v) {
    	int newWL;
    	Integer i = playerFplId;
    	if (in_watchlist) {
    		newWL = 0;
    		App.watchlist.remove(i);
    		watchlistIcon.setImageResource(R.drawable.btn_star_big_off);
    	} else {
    		newWL = 1;
    		App.watchlist.add(i);
    		watchlistIcon.setImageResource(R.drawable.btn_star_big_on);
    	}
    	App.initProc(true, FPLService.UPDATE_WATCHLIST, playerFplId, playerPaId, newWL);
    	popTopWatchlist();
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
