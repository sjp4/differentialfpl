package com.pennas.fpl;

import java.util.ArrayList;
import java.util.Comparator;

import com.pennas.fpl.Players.Stat;
import com.pennas.fpl.Selection.ViewPagerAdapter;
import com.pennas.fpl.util.FPLActivity;
import com.pennas.fpl.util.DbGen;
import com.pennas.fpl.util.FPLAsyncTask;
import com.pennas.fpl.util.Refreshable;
import com.pennas.fpl.util.SquadUtil;
import com.viewpagerindicator.TitlePageIndicator;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class Stats extends FPLActivity implements Refreshable {
	private ListView playerStatsList, teamStatsList, rivalStatsList, genericStatsList;
	
	// static content for min games filter
	public static final String[] dropdown = { "None", "5", "10" };
	public static final int[] game_nums = { 0, 450, 900 };
	private int min_games;
	private int season = App.season;
	
	private ArrayList<PlayerStatEntry> listOfPlayerStats;
	private ArrayList<TeamStatEntry> listOfTeamStats;
	private ArrayList<GenericStatEntry> listOfGenericStats;
	private ArrayList<RivalStatEntry> listOfRivalStats;
	
	private boolean doPopData;
	
	// data for one stat list entry
	private static class PlayerStatEntry {
		public String statDesc;
		public String statValue;
		public String playerName;
		public int pictureCode;
		public String statId;
	}
	private static class TeamStatEntry {
		public String statDesc;
		public String statValue;
		public String teamName;
		public int teamId;
		public String statId;
	}
	private static class RivalStatEntry {
		public String statDesc;
		public String statValue;
		public String squadName;
		public String playerName;
		public int squadId;
		public String statId;
	}
	private static class GenericStatEntry {
		public String statDesc;
		public String statValue;
		//public String statId;
	}
	
	private static final int FILTER_MIN = 1;
	private static final int FILTER_SEASON = 2;
	
	private ViewPager viewPager;
	
	private static final int TAB_TEAM = 0;
	private static final int TAB_PLAYER = 1;
	private static final int TAB_RIVAL = 2;
	private static final int TAB_GENERIC = 3;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    setContentView(R.layout.stats_screen);
	    
	    actionBar.setTitle("Stats");
	    
	    hasTips = true;
        tipsPref = Settings.PREF_TIPS_STATS;
		tips.add("The leading performer for each statistic is shown");
		tips.add("Click to see a list of leading entries for statistic");
		tips.add("View previous seasons, and overall performance using filter dropdown/icon");
		tips.add("After a few weeks of the season, filter by minimum games played for better results");
		
	    doPopData = false;
	    
	    min_games = Settings.getIntPref(Settings.PREF_MIN_GAMES_FILTER);
	 	
	    playerStatsList = new ListView(this);
	 	teamStatsList = new ListView(this);
	 	rivalStatsList = new ListView(this);
	 	genericStatsList = new ListView(this);
	 	
	 	// minFilter: populate (static)
		int sel = 0;
	    if (min_games > 0) {
	    	for (int i=0; i<game_nums.length; i++) {
	    		if (game_nums[i] == min_games) {
	    			sel = i;
	    		}
	    	}
	    }
	    
	    addFilter(FILTER_MIN, "Minimum Games", dropdown, sel, true, false);
	    
	    // season filter: populate (static)
		if (App.cur_gameweek < 1) {
			season = App.season - 1;
			sel = 1;
		} else {
			sel = 0;
		}
		
		addFilter(FILTER_SEASON, "Season", Player.season_desc, sel, true, true);
	    
		viewPager = (ViewPager) findViewById(R.id.viewpager);
		final String[] titles = new String[] { "Teams", "Players", "Rivals", "General" };
		final View[] views = new View[] { teamStatsList, playerStatsList, rivalStatsList, genericStatsList };
		ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(titles, views);
	    TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.titles);
	    viewPager.setAdapter(pagerAdapter);
	    indicator.setViewPager(viewPager);
	    indicator.setCurrentItem(1);
	    
		indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				if(position == TAB_PLAYER) {
        	    	setFilterVisible(FILTER_MIN, true);
        	    	setFilterVisible(FILTER_SEASON, true);
        	    } else {
        	    	setFilterVisible(FILTER_MIN, false);
        	    	setFilterVisible(FILTER_SEASON, false);
        	    }
				
				set_screenshot_view();
			}

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

			@Override
			public void onPageScrollStateChanged(int state) { }
		});
	 	
	    // load stat values for listview in async task - takes a while
	    doPopData = true;
	    popData();
	}
	
	private LoadTask loader;
	
	// start async task to load stats
	private void popData() {
		if (doPopData) {
			//AppClass.log("popData() stats");
			loader = new LoadTask();
			loader.fplExecute(season);
		}
	}
	
	private static class StatReturn {
		ArrayList<PlayerStatEntry> players;
		ArrayList<TeamStatEntry> teams;
		ArrayList<GenericStatEntry> generic;
		ArrayList<RivalStatEntry> rivals;
	}

	// async task to load stats
	private class LoadTask extends FPLAsyncTask<Integer, Void, StatReturn> {
		ProgressDialog dialog;
		
		protected StatReturn doInBackground(Integer... params) {
			ArrayList<PlayerStatEntry> tempPlayers = new ArrayList<PlayerStatEntry>();
        	Cursor cur;
    		int c_season = params[0];
    		
    		// PLAYERS
    		cur = App.dbGen.rawQuery("SELECT p.name, p.code_14, ss.max_value, ss.stat_id, ss.plus"
    				+ " FROM stat_season ss, player p"
    				+ " WHERE ss.season = " + c_season + " AND p._id = ss.player_id AND ss.min_games = " + min_games, null);
    		cur.moveToFirst();
    		while (!cur.isAfterLast()) {
    			PlayerStatEntry se = new PlayerStatEntry();
		 		se.playerName = cur.getString(0);
		 		se.statId = cur.getString(3);
		 		
		 		Stat s = Players.playerstats.get(se.statId);
		 		if (s.show_value) {
		 			String val;
			 		if (s.divide > 0) {
			 			float num = cur.getFloat(2) / s.divide;
			 			val = String.valueOf(num);
			 		} else {
			 			val = cur.getString(2);
			 		}
			 		se.statValue = val;
			 		
			 		se.statDesc = s.desc;
			 		se.pictureCode = cur.getInt(1);
			 		// more with same value
			 		if (cur.getInt(4) > 0) {
			 			se.playerName += " +";
			 		}
		 		
		 			tempPlayers.add(se);
		 		}
    			cur.moveToNext();
    		}
    		cur.close();
    		
    		// TEAMS
    		ArrayList<TeamStatEntry> tempTeams = new ArrayList<TeamStatEntry>();
    		for (Stat s : Teams.teamstats) {
    	 		if (s.show_value) {
    	 			cur = App.dbGen.rawQuery("SELECT ts.team_id, ts." + s.db_field + ", t.name FROM team_season ts, team t WHERE ts." + s.db_field + " = (SELECT MAX(" + s.db_field + ") FROM team_season WHERE season = " + App.season + ") AND ts.season = " + App.season + " AND ts.team_id = t._id", null);
	    		 	cur.moveToFirst();
	    		 	// store top entry, and mark if there are more with same value
	    		 	if (!cur.isAfterLast()) {
	    		 		TeamStatEntry se = new TeamStatEntry();
	    		 		se.teamName = cur.getString(2);
	    		 		se.statValue = cur.getString(1);
	    		 		se.statDesc = s.desc;
	    		 		se.teamId = cur.getInt(0);
	    		 		se.statId = s.db_field;
	    		 		// more with same value
	    		 		if (!cur.isLast()) {
	    		 			se.teamName += " +";
	    		 		}
	    		 		
	    		 		tempTeams.add(se);
	    		 	} 
	    		 	cur.close();
    	 		}
    	    }
    		
    		// RIVALS
    		ArrayList<RivalStatEntry> tempRivals = new ArrayList<RivalStatEntry>();
    		for (Stat s : FPLSquad.squadstats) {
    	 		if (s.show_value) {
    	 			cur = App.dbGen.rawQuery("SELECT _id, name, player_name, " + s.db_field
    	 					+ " FROM squad"
    	 					+ " WHERE (rival = 1 OR _id = " + App.my_squad_id + ")"
    	 					+ "   AND " + s.db_field + " = (SELECT MAX(" + s.db_field + ") FROM squad WHERE rival = 1 OR _id = " + App.my_squad_id + ")", null);
	    		 	cur.moveToFirst();
	    		 	// store top entry, and mark if there are more with same value
	    		 	if (!cur.isAfterLast()) {
	    		 		RivalStatEntry re = new RivalStatEntry();
	    		 		re.playerName = DbGen.decompress(cur.getBlob(2));
	    		 		re.squadName = DbGen.decompress(cur.getBlob(1));
	    		 		re.statDesc = s.desc;
	    		 		re.squadId = cur.getInt(0);
	    		 		re.statId = s.db_field;
	    		 		
	    		 		String val;
	    		 		if (s.divide > 1) {
	    		 			float num = cur.getFloat(3) / s.divide;
	    		 			val = String.valueOf(num);
	    		 		} else {
	    		 			val = cur.getString(3);
	    		 		}
	    		 		re.statValue = val;
	    		 		
	    		 		// more with same value
	    		 		if (!cur.isLast()) {
	    		 			re.squadName += " +";
	    		 		}
	    		 		tempRivals.add(re);
	    		 	} 
	    		 	cur.close();
    	 		}
    	    }
    		
    		// GENERIC
    		ArrayList<GenericStatEntry> tempGeneric = new ArrayList<GenericStatEntry>();
    		Cursor curStat = App.dbGen.rawQuery("SELECT s.desc, gs.value FROM stat s, generic_stat gs WHERE gs.id = s.db_field AND gs.season = " + App.season + " AND s.player_stat = 2 ORDER BY s.desc ASC", null);
        	curStat.moveToFirst();
        	while (!curStat.isAfterLast()) {
        		GenericStatEntry s = new GenericStatEntry();
        		s.statDesc = curStat.getString(0);
        		s.statValue = curStat.getString(1);
        		tempGeneric.add(s);
        		curStat.moveToNext();
        	}
        	curStat.close();
    		
        	// return
    		StatReturn ret = new StatReturn();
    		ret.players = tempPlayers;
    		ret.teams = tempTeams;
    		ret.rivals = tempRivals;
    		ret.generic = tempGeneric;
    	    return ret;
        }
		protected void onPreExecute() {
			dialog = new ProgressDialog(Stats.this);
			dialog.setMessage("Loading..");
			dialog.setCancelable(false);
			dialog.show();
			// load stat defs into memory
		    FPLSquad.loadStats();
		    Players.loadStats();
		    Teams.loadStats();
		    Stats.this.progress = dialog;
		}
		protected void onPostExecute(StatReturn result) {
        	// PLAYERS
        	// populate GUI with results
        	listOfPlayerStats = result.players;
        	PlayerStatEntryAdapter plAdapter = new PlayerStatEntryAdapter(Stats.this, R.layout.stats_screen_item, listOfPlayerStats);
        	plAdapter.sort(new Comparator<PlayerStatEntry>() {
        		public int compare(PlayerStatEntry object1, PlayerStatEntry object2) {
        			return object1.statDesc.compareTo(object2.statDesc);
        		}
        	});
    	 	playerStatsList.setAdapter(plAdapter);
    	 	// listener for stat click: display players screen sorted by that stat
    		OnItemClickListener plListListener = new OnItemClickListener() {
    			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
    	            // load right stats screen
    				Intent intent = new Intent(view.getContext(), Players.class);
    				PlayerStatEntry it = listOfPlayerStats.get(position);
    				intent.putExtra(Players.P_STATID, it.statId);
    				intent.putExtra(Players.P_SEASON, season);
    				startActivity(intent);
    	        }
    	    };
    		playerStatsList.setOnItemClickListener(plListListener);
    		
    		// TEAMS
    		listOfTeamStats = result.teams;
        	TeamStatEntryAdapter tAdapter = new TeamStatEntryAdapter(Stats.this, R.layout.stats_screen_item, listOfTeamStats);
        	tAdapter.sort(new Comparator<TeamStatEntry>() {
        		public int compare(TeamStatEntry object1, TeamStatEntry object2) {
        			return object1.statDesc.compareTo(object2.statDesc);
        		}
        	});
    	 	teamStatsList.setAdapter(tAdapter);
    	 	// listener for stat click: display players screen sorted by that stat
    		OnItemClickListener tListListener = new OnItemClickListener() {
    			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
    	            // load right stats screen
    				Intent intent = new Intent(view.getContext(), Teams.class);
    				TeamStatEntry it = listOfTeamStats.get(position);
    				intent.putExtra(Teams.P_SORT, it.statId);
    				startActivity(intent);
    	        }
    	    };
    	    teamStatsList.setOnItemClickListener(tListListener);
    	    
    	    // RIVALS
    		listOfRivalStats = result.rivals;
        	RivalStatEntryAdapter rAdapter = new RivalStatEntryAdapter(Stats.this, R.layout.stats_screen_rival_item, listOfRivalStats);
        	rAdapter.sort(new Comparator<RivalStatEntry>() {
        		public int compare(RivalStatEntry object1, RivalStatEntry object2) {
        			return object1.statDesc.compareTo(object2.statDesc);
        		}
        	});
    	 	rivalStatsList.setAdapter(rAdapter);
    	 	// listener for stat click: display players screen sorted by that stat
    		OnItemClickListener rListListener = new OnItemClickListener() {
    			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
    	            // load right stats screen
    				Intent intent = new Intent(view.getContext(), Leagues.class);
    				RivalStatEntry it = listOfRivalStats.get(position);
    				intent.putExtra(Leagues.P_STATID, it.statId);
    		    	startActivity(intent);
    	        }
    	    };
    	    rivalStatsList.setOnItemClickListener(rListListener);
    	    
    	    // GENERIC
    		listOfGenericStats = result.generic;
        	GenericStatEntryAdapter gAdapter = new GenericStatEntryAdapter(Stats.this, R.layout.player_screen_stat_item, listOfGenericStats);
        	gAdapter.sort(new Comparator<GenericStatEntry>() {
        		public int compare(GenericStatEntry object1, GenericStatEntry object2) {
        			return object1.statDesc.compareTo(object2.statDesc);
        		}
        	});
    	 	genericStatsList.setAdapter(gAdapter);
    	 	// (no on-click for generic stats)
    	 	
    	 	set_screenshot_view();
    	 	
    	 	dialog.dismiss();
    	 	loader = null;
        }
    }
	
	//{ teamStatsList, playerStatsList, rivalStatsList, genericStatsList };
	private void set_screenshot_view() {
		if (viewPager.getCurrentItem() == TAB_TEAM) {
			setScreenshotView(teamStatsList, "Leading teams", Players.SCREENSHOT_LISTVIEW_COUNT_LARGE_ITEMS);
		} else if (viewPager.getCurrentItem() == TAB_PLAYER) {
			setScreenshotView(playerStatsList, "Leading players for " + Player.season_desc[Player.getSeasonInd(season)], Players.SCREENSHOT_LISTVIEW_COUNT_LARGE_ITEMS);
		} else if (viewPager.getCurrentItem() == TAB_RIVAL) {
			setScreenshotView(rivalStatsList, "Leading rivals", Players.SCREENSHOT_LISTVIEW_COUNT_LARGE_ITEMS);
		} else if (viewPager.getCurrentItem() == TAB_GENERIC) {
			setScreenshotView(genericStatsList, "General stats");
		}
	}
	
	// adapater mapping player stats array to listview item
	private class PlayerStatEntryAdapter extends ArrayAdapter<PlayerStatEntry> {
		private ArrayList<PlayerStatEntry> items;
		
		public PlayerStatEntryAdapter(Context context, int textViewResourceId, ArrayList<PlayerStatEntry> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		        v = vi.inflate(R.layout.stats_screen_item, null);
			}
			PlayerStatEntry it = items.get(position);
			if (it != null) {
				TextView t = (TextView) v.findViewById(R.id.statdesc);
				t.setText(it.statDesc);
				t = (TextView) v.findViewById(R.id.playername);
				t.setText(it.playerName);
				t = (TextView) v.findViewById(R.id.statvalue);
				t.setText(it.statValue);
				ImageView i = (ImageView) v.findViewById(R.id.shirtPic);
				i.setImageResource(Players.getPlayerPic(Stats.this, it.pictureCode));
			}
			return v;
		}
	}
	
	// adapater mapping player stats array to listview item
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
		        v = vi.inflate(R.layout.stats_screen_item, null);
			}
			TeamStatEntry it = items.get(position);
			if (it != null) {
				TextView t = (TextView) v.findViewById(R.id.statdesc);
				t.setText(it.statDesc);
				t = (TextView) v.findViewById(R.id.playername);
				t.setText(it.teamName);
				t = (TextView) v.findViewById(R.id.statvalue);
				t.setText(it.statValue);
				ImageView i = (ImageView) v.findViewById(R.id.shirtPic);
				i.setImageResource(SquadUtil.getShirtResource(it.teamId));
			}
			return v;
		}
	}
	
	// adapater mapping player stats array to listview item
	private class RivalStatEntryAdapter extends ArrayAdapter<RivalStatEntry> {
		private ArrayList<RivalStatEntry> items;
		
		public RivalStatEntryAdapter(Context context, int textViewResourceId, ArrayList<RivalStatEntry> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		        v = vi.inflate(R.layout.stats_screen_rival_item, null);
			}
			RivalStatEntry it = items.get(position);
			if (it != null) {
				TextView t = (TextView) v.findViewById(R.id.statdesc);
				t.setText(it.statDesc);
				t = (TextView) v.findViewById(R.id.playername);
				t.setText(it.playerName);
				t = (TextView) v.findViewById(R.id.squadname);
				t.setText(it.squadName);
				t = (TextView) v.findViewById(R.id.statvalue);
				t.setText(it.statValue);
			}
			
			RelativeLayout rl = (RelativeLayout) v;
			if (it.squadId == App.my_squad_id) {
				rl.setBackgroundResource(R.drawable.list_item_gradient_green);
    	 	} else {
    	 		rl.setBackgroundResource(R.drawable.list_item_gradient);
    	 	}
			return v;
		}
	}
	
	// adapater mapping player stats array to listview item
	private class GenericStatEntryAdapter extends ArrayAdapter<GenericStatEntry> {
		private ArrayList<GenericStatEntry> items;
		
		public GenericStatEntryAdapter(Context context, int textViewResourceId, ArrayList<GenericStatEntry> items) {
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
			GenericStatEntry it = items.get(position);
			if (it != null) {
				TextView t = (TextView) v.findViewById(R.id.statdesc);
				t.setText(it.statDesc);
				t = (TextView) v.findViewById(R.id.statvalue);
				t.setText(it.statValue);
			}
			return v;
		}
	}

	@Override
	protected void filterSelection(int id, int pos) {
		App.log("subclass");
    	
    	switch(id) {
	    	case FILTER_MIN:
				if (min_games != game_nums[pos]) {
		        	min_games = game_nums[pos];
		        	Settings.setIntPref(Settings.PREF_MIN_GAMES_FILTER, min_games, Stats.this);
		        	popData();
	        	}
				break;
			case FILTER_SEASON:
	    		if (season != Player.seasons[pos]) {
	    			season = Player.seasons[pos];
	    			popData();
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
    	popData();
    }
    
    public ProgressDialog progress;
    
    protected void onDestroy() {
    	if (loader != null) {
			loader.cancel(true);
    	}
    	if (progress != null) {
    		progress.dismiss();
    	}
    	super.onDestroy();
    }

}
