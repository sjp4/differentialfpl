package com.pennas.fpl;

import com.pennas.fpl.Leagues.SquadListCursorAdapter;
import com.pennas.fpl.Selection.ViewPagerAdapter;
import com.pennas.fpl.ui.Worm;
import com.pennas.fpl.util.FPLActivity;
import com.pennas.fpl.util.BitmapCache;
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
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class Hot extends FPLActivity implements Refreshable {
	
	private ListView playersList, rivalsList;
	Cursor curPlayers, curRivals;
	
	private float max_score, max_squad_score;
	private int my_gw_score, cup_opp_id;
	private int season = App.season;
	private int gameweek = App.cur_gameweek;
	
	public static final String P_GAMEWEEK = "com.pennas.fpl.gameweek";
	public static final String P_SHOWRIVALS = "com.pennas.fpl.showrivals";
	
	private static final int LIMIT = 50;
	private BitmapCache bmp;
	
	private static final int FILTER_GAMEWEEK =1;
	private ViewPager viewPager;
	
	private static final int TAB_PLAYERS = 0;
	private static final int TAB_RIVALS = 1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    boolean showRivals = false;
	    
	    // process intent params
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			if (extras.containsKey(P_GAMEWEEK)) {
				gameweek = extras.getInt(P_GAMEWEEK);
			}
			if (extras.containsKey(P_SHOWRIVALS)) {
				showRivals = extras.getBoolean(P_SHOWRIVALS);
			}
		}
		
		actionBar.setTitle("Hot");
		
	    hasTips = true;
        tipsPref = Settings.PREF_TIPS_LIVE;
		tips.add("Live scores for the current gameweek are displayed here");
		tips.add("Click a player or rival for more details");
		tips.add("Current goals/assists totals are shown on Rivals tab, along with number of players playing/to-play");
		tips.add("Use sync panel (swipe from bottom or from menu) to update scores if auto-sync is disabled");
		tips.add("Blue section on the squad gameweek score worm indicates a points hit");
		
		setContentView(R.layout.hot_screen);
		
		LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rivalsHeader = vi.inflate(R.layout.live_rivals_key, null);
		
		playersList = new ListView(this);
		rivalsList = new ListView(this);
		rivalsList.addHeaderView(rivalsHeader);
		
		String[] gameweeks = new String[App.cur_gameweek];
		for (int i=0; i<App.cur_gameweek; i++) {
			gameweeks[i] = String.valueOf(i + 1);
		}
		
	    addFilter(FILTER_GAMEWEEK, "Gameweek", gameweeks, gameweek - 1, true, true);
		
	    viewPager = (ViewPager) findViewById(R.id.viewpager);
		final String[] titles = new String[] { "Players", "Rivals" };
		final View[] views = new View[] { playersList, rivalsList };
		ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(titles, views);
	    TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.titles);
	    viewPager.setAdapter(pagerAdapter);
	    indicator.setViewPager(viewPager);
	    
	    if (showRivals) {
	    	indicator.setCurrentItem(1);
	    }
	    
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
        
	    popList();
	}
	
	private void popBmpCache() {
		if (bmp == null) {
			bmp = new BitmapCache(this);
			
			for (int i : SquadUtil.SHIRT_IMAGES) {
				if (i != 0) {
					bmp.getBitmap(i);
				}
			}
			bmp.getBitmap(R.drawable.btn_star_big_on);
		}
	}
	
	@Override
	protected void filterSelection(int id, int pos) {
		App.log("subclass");
    	
    	switch(id) {
	    	case (FILTER_GAMEWEEK):
	    		int newGw = pos + 1;
				if (newGw != gameweek) {
					gameweek = newGw;
					popList();
				}
				break;
    	}
	}
    
    public void closeCursors() {
    	if (curPlayers != null) {
	 		curPlayers.close();
	 		curPlayers = null;
	 	}
	 	if (curRivals != null) {
	 		curRivals.close();
	 		curRivals = null;
	 	}
    }
    
    // async task to load stats
	private class LoadTask extends FPLAsyncTask<Void, Void, Void> {
		ProgressDialog dialog;
		
		protected Void doInBackground(Void... params) {
			Cursor minmax = App.dbGen.rawQuery("SELECT MAX(total) max_score FROM"
					+ " (SELECT player_player_id, SUM(total) total"
					+ "  FROM player_match WHERE season = " + season
					+ "  AND gameweek =  " + gameweek + " GROUP BY player_player_id)", null);
		 	minmax.moveToFirst();
		 	max_score = minmax.getFloat(0);
		 	minmax.close();
		 	
		 	minmax = App.dbGen.rawQuery("SELECT MAX(sg.points) max_score, MAX(sg.c_points) max_c_score"
		 			+ " FROM squad s, squad_gameweek sg"
		 			+ " WHERE sg.squad_id = s._id AND (s.rival = 1 OR s._id = " + App.my_squad_id + ")"
		 			+ " AND sg.gameweek = " + gameweek, null);
		 	minmax.moveToFirst();
		 	max_squad_score = minmax.getFloat(1);
		 	float temp = minmax.getFloat(0);
		 	if (temp > max_squad_score) {
		 		max_squad_score = temp;
		 	}
		 	minmax.close();
		 	
		 	// PLAYERS
		 	curPlayers = App.dbGen.rawQuery("SELECT p._id _id, (CASE WHEN ps.position <= 2 THEN SUM(pm.goals) * 6 WHEN ps.position = 3 THEN SUM(pm.goals) * 5 WHEN ps.position = 4 THEN SUM(pm.goals) * 4 END) goal_pts"
		 			+ ", (SUM(pm.assists) * 3) assist_pts, SUM(pm.bonus) bonus_pts, p.team_id team_id, SUM(pm.total) points, ps.fpl_id fpl_id"
		 			+ ", p.name name, (ps.price / 10.0) price, ps.position position"
		 			+ ", (sgp.selected + 1) selected, sgp.autosub autosub"
					+ " FROM player p, player_season ps, player_match pm"
		 			+ " LEFT OUTER JOIN squad_gameweek_player sgp"
					+ " ON sgp.squad_id = " + App.my_squad_id + " AND sgp.gameweek = pm.gameweek AND sgp.fpl_player_id = ps.fpl_id"
					+ " WHERE ps.player_id = p._id AND ps.season = " + season + " AND pm.player_player_id = p._id AND pm.season = " + season
		 			+ " AND pm.gameweek = " + gameweek
		 			+ " GROUP BY p._id"
		 			+ " ORDER BY points DESC LIMIT " + LIMIT, null);
		 	
		 	String cup_opp = "";
		 	Cursor curCup = App.dbGen.rawQuery("SELECT cup_opp FROM squad_gameweek"
					+ " WHERE squad_id = " + App.my_squad_id + " AND gameweek = " + gameweek, null);
		 	curCup.moveToFirst();
			if (!curCup.isAfterLast()) {
				cup_opp_id = curCup.getInt(0);
				if (cup_opp_id > 0) {
					cup_opp = " OR s._id = " + cup_opp_id;
				}
			}
			curCup.close();
			
			// RIVALS
			curRivals = App.dbGen.rawQuery("SELECT s._id, s.name, s.player_name, sg.points, sg.c_points, sg.c_p_playing"
					+ ", sg.c_p_to_play, sg.c_complete, sg.c_goals, sg.c_assists, sg.c_cs, sg.points_hit gw_pts, sg.c_bonus, sg.chip"
					+ " FROM squad s, squad_gameweek sg WHERE"
					+ " (s.rival = 1 OR s._id = " + App.my_squad_id + cup_opp + ")"
					+ " AND sg.squad_id = s._id AND sg.gameweek = " + gameweek
					+ " ORDER BY CASE WHEN sg.c_points ISNULL THEN sg.points ELSE sg.c_points END DESC", null);
			
			my_gw_score = 0;
			Cursor curMy = App.dbGen.rawQuery("SELECT sg.points, sg.c_points"
		 			+ " FROM squad_gameweek sg"
		 			+ " WHERE sg.squad_id = " + App.my_squad_id
		 			+ " AND sg.gameweek = " + gameweek, null);
			curMy.moveToFirst();
			if (!curMy.isAfterLast()) {
				my_gw_score = curMy.getInt(1);
			 	int tempb = curMy.getInt(0);
			 	if (tempb > my_gw_score) {
			 		my_gw_score = tempb;
			 	}
			}
		 	curMy.close();
			
			return null;
        }
		protected void onPreExecute() {
			popBmpCache();
			
			dialog = new ProgressDialog(Hot.this);
			dialog.setMessage("Loading..");
			dialog.setCancelable(false);
			dialog.show();
			Hot.this.progress = dialog;
			
		    closeCursors();
		}
        protected void onPostExecute(Void result) {
        	if (curPlayers != null) {
	    	    PlayersCursorAdapter playersAdapter = new PlayersCursorAdapter(Hot.this, curPlayers);
	    		playersList.setAdapter(playersAdapter);
	    		OnItemClickListener playersListener = new OnItemClickListener() {
	    			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
	    				// load player screen for player "id"
	    				Intent intent = new Intent(view.getContext(), Player.class);
	    				intent.putExtra(Player.P_PLAYERID, (int) id);
	    				intent.putExtra(Player.P_SEASON, season);
	    		    	startActivity(intent);
	    			}
	    	    };
	    	    playersList.setOnItemClickListener(playersListener);
        	}
    		
        	if (curRivals != null) {
	    	    SquadListCursorAdapter pAdapter = new SquadListCursorAdapter(Hot.this, curRivals, max_squad_score, my_gw_score, true, false, null, true, cup_opp_id);
	    		rivalsList.setAdapter(pAdapter);
	    		OnItemClickListener listListener = new OnItemClickListener() {
	    			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
	    				if (id > 0) {
		    				// load squad screen
		    				Intent intent = new Intent(view.getContext(), Selection.class);
		    		    	intent.putExtra(Selection.P_SELECTION, false);
		    		    	intent.putExtra(Selection.P_SQUADID, (int) id);
		    		    	intent.putExtra(Selection.P_GAMEWEEK, gameweek);
		    		    	startActivity(intent);
	    				}
	    			}
	    	    };
	    	    rivalsList.setOnItemClickListener(listListener);
        	}
        	
        	set_screenshot_view();
    	    
        	dialog.dismiss();
        	loader = null;
        }
    }
	
	private void set_screenshot_view() {
		if (viewPager.getCurrentItem() == TAB_PLAYERS) {
			setScreenshotView(playersList, "Hot players for gameweek " + gameweek);
		} else if (viewPager.getCurrentItem() == TAB_RIVALS) {
			setScreenshotView(rivalsList, "Hot rivals for gameweek " + gameweek);
		}
	}
	
	private LoadTask loader;
	
	// load data and populate listview
	private void popList() {
		loader = new LoadTask();
		loader.fplExecute();
	}
	
	private class PlayersCursorAdapter extends CursorAdapter {
    	private int i_goal_pts, i_bonus_pts, i_assist_pts;
    	private int i_team_id, i_points, i_fpl_id, i_name, i_price, i_position;
    	private int i_selected, i_autosub;
    	private boolean loaded_indexes;
    	
    	public PlayersCursorAdapter(Context context, Cursor c) {
			super(context, c);
		}
    	
		private void loadIndexes(Cursor cursor) {
        	loaded_indexes = true;
        	i_goal_pts = cursor.getColumnIndex("goal_pts");
        	i_bonus_pts = cursor.getColumnIndex("bonus_pts");
        	i_assist_pts = cursor.getColumnIndex("assist_pts");
        	i_team_id = cursor.getColumnIndex("team_id");
        	i_points = cursor.getColumnIndex("points");
        	i_fpl_id = cursor.getColumnIndex("fpl_id");
        	i_name = cursor.getColumnIndex("name");
        	i_position = cursor.getColumnIndex("position");
        	i_price = cursor.getColumnIndex("price");
        	i_selected = cursor.getColumnIndex("selected");
        	i_autosub = cursor.getColumnIndex("autosub");
        }
    	
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// load column indexes if required
			if (!loaded_indexes) loadIndexes(cursor);
			
			TextView t;
			ImageView i;
			// shirt pic
			i = (ImageView) view.findViewById(R.id.shirtPic);
			int shirt = SquadUtil.getShirtResource(cursor.getInt(i_team_id));
			i.setImageBitmap(bmp.getBitmap(shirt));
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
        	// watchlist
        	i = (ImageView) view.findViewById(R.id.watchlisticon);
        	int fpl_id = cursor.getInt(i_fpl_id);
        	if (App.watchlist.contains(fpl_id)) {
        		i.setImageBitmap(bmp.getBitmap(R.drawable.btn_star_big_on));
    	 	} else {
    	 		i.setImageBitmap(null);
    	 	}
        	// my squad
        	RelativeLayout rl = (RelativeLayout) view.findViewById(R.id.backing);
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
        	// flags + name
        	t = (TextView) view.findViewById(R.id.pName);
        	t.setText(cursor.getString(i_name));
        	// stats
    		
    		// price
    		t = (TextView) view.findViewById(R.id.value);
    		t.setText("£" + cursor.getString(i_price) + "M");
    		// position
    		t = (TextView) view.findViewById(R.id.position);
    		t.setText(Players.position_text[cursor.getInt(i_position) - 1]);
		}
		
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final View view = LayoutInflater.from(context).inflate(R.layout.players_screen_item_smaller, parent, false);
		    return view;
		}
    	
    }
    
    // listener for stat desc toasts
    // not actually used in this class but here to avoid crash when clicked
    public void clickStat(View v) {
    	
    }
    
    // callback
    public void dataChanged() {
    	if (loader != null) {
			loader.cancel(true);
    	}
    	if (progress != null) {
    		progress.dismiss();
    	}
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
