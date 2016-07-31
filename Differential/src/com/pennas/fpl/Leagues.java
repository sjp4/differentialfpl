package com.pennas.fpl;

import com.pennas.fpl.Players.Stat;
import com.pennas.fpl.Selection.ViewPagerAdapter;
import com.pennas.fpl.process.FPLService;
import com.pennas.fpl.scrape.ScrapeSquadGameweek;
import com.pennas.fpl.ui.Worm;
import com.pennas.fpl.util.FPLActivity;
import com.pennas.fpl.util.DbGen;
import com.pennas.fpl.util.Refreshable;
import com.viewpagerindicator.TitlePageIndicator;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class Leagues extends FPLActivity implements Refreshable {
	
	private LeaguesViewBinder leaguesBinder = new LeaguesViewBinder();
	private ListView leaguesList;
	private ListView rivalsList;
	TextView rivalsHeader;
	Cursor curLeagues;
	Cursor curRivals;
	private RelativeLayout tab_rivals;
	
	private static final String POINTS = "c_points";
	private String statId = POINTS;
	
	private String[] sortNames;
    private Stat[] sortStats;
	private int stat = 0;
	
	public static final String P_STATID = "com.pennas.fpl.statid";
	
	public static final String TAB_LEAGUES = "tab_leagues";
	public static final String TAB_RIVALS = "tab_rivals";
	
	private static final int TAB_I_RIVALS = 0;
	private static final int TAB_I_LEAGUES = 1;
	
	public static final int FILTER_STATSORT = 1;
	
	private float max_score_season;
	
	private ViewPager viewPager;
	
	public static final String CAPT_STRING_BLANK = "";
	public static final String CAPT_STRING_C = "c";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    hasTips = true;
        tipsPref = Settings.PREF_TIPS_LEAGUES;
		tips.add("Selected rivals shown on 'rivals' tab");
		tips.add("Only Rivals' scores are Live Updated on matchdays");
		tips.add("Select a rival or league for more details");
		tips.add("Only classic leagues are displayed");
		tips.add("Manually add a rival who is not in your minileagues from the menu");
		
		actionBar.setTitle("");
		
		setContentView(R.layout.leagues_screen);
		
		// get ID of player
	    Bundle extras = getIntent().getExtras();
	    if(extras != null ) {
		    if (extras.containsKey(P_STATID)) {
		    	statId = extras.getString(P_STATID);
		    }
	    }
	    
	    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	 	tab_rivals = (RelativeLayout) inflater.inflate(R.layout.leagues_screen_rivals_tab, null);
	 	
		leaguesList = new ListView(this);
	    rivalsList = (ListView) tab_rivals.findViewById(R.id.rivals);
	    rivalsHeader = (TextView) tab_rivals.findViewById(R.id.rivalsheader);
	    
	    viewPager = (ViewPager) findViewById(R.id.viewpager);
		final String[] titles = new String[] { "Rivals", "Leagues" };
		final View[] views = new View[] { tab_rivals, leaguesList };
		ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(titles, views);
	    TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.titles);
	    viewPager.setAdapter(pagerAdapter);
	    indicator.setViewPager(viewPager);
	    
		indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				if(position == 0) {
        	    	setFilterVisible(FILTER_STATSORT, true);
        	    } else {
        	    	setFilterVisible(FILTER_STATSORT, false);
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
	 	
        FPLSquad.loadStats();
        // sort dropdown
	 	sortNames = new String[FPLSquad.squadstats.size()];
    	sortStats = new Stat[FPLSquad.squadstats.size()];
    	int i = 0;
    	for (Stat s : FPLSquad.squadstats) {
    		sortNames[i] = s.desc;
    		sortStats[i] = s;
    		if (s.db_field.equals(statId)) {
    			stat = i;
    		}
    		i++;
    	}
    	
    	addFilter(FILTER_STATSORT, "Sort", sortNames, stat, true, true);
        
	    popList();
	}
	
	@Override
	protected void filterSelection(int id, int pos) {
		App.log("subclass");
    	
    	switch(id) {
	    	case (FILTER_STATSORT):
	    		if (pos != stat) {
  	        	  	stat = pos;
  	        	  	statId = sortStats[stat].db_field;
  	        	  	popList();
        		}
				break;
    	}
	}
	
	private void closeCursors() {
		if (curLeagues != null) {
	 		curLeagues.close();
	 	}
	 	if (curRivals != null) {
	 		curRivals.close();
	 	}
	}
	
	// load data and populate listview
	private void popList() {
		//App.log("poplist");
		Cursor minmax = App.dbGen.rawQuery("SELECT MAX(points) max_score, MAX(c_points) max_c_score FROM squad"
				+ " WHERE rival = 1 OR _id = " + App.my_squad_id, null);
	 	minmax.moveToFirst();
	 	max_score_season = minmax.getFloat(1);
	 	float temp = minmax.getFloat(0);
	 	if (temp > max_score_season) {
	 		max_score_season = temp;
	 	}
	 	minmax.close();
	 	
	 	closeCursors();
	    
	    // LEAGUES
		curLeagues = App.dbGen.rawQuery("SELECT _id, name, num_teams, position FROM minileague", null);
		String[] colLeagues = new String[] { "name", "num_teams", "position" };
		int[] toLeagues = new int[] { R.id.leaguename, R.id.numteams, R.id.position };
		SimpleCursorAdapter leaguesAdapter = new SimpleCursorAdapter(this, R.layout.leagues_screen_league_item, curLeagues, colLeagues, toLeagues);
		leaguesAdapter.setViewBinder(leaguesBinder);
		leaguesList.setAdapter(leaguesAdapter);
		OnItemClickListener leagueListener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
				// load squad screen
				Intent intent = new Intent(view.getContext(), League.class);
				intent.putExtra(League.P_LEAGUE_ID, (int) id);
		    	startActivity(intent);
			}
	    };
	    leaguesList.setOnItemClickListener(leagueListener);
	    leaguesList.refreshDrawableState();
	    
	    Cursor myscore = App.dbGen.rawQuery("SELECT points, c_points FROM squad WHERE _id = " + App.my_squad_id, null);
	    myscore.moveToFirst();
	    if (!myscore.isAfterLast()) {
		 	int my_score = myscore.getInt(1);
		 	if (my_score < 1) {
		 		my_score = myscore.getInt(0);
		 	}
		 	
		 	String order_by;
		 	if (sortStats[stat].db_field.equals(POINTS)) {
		 		order_by = "CASE WHEN c_points ISNULL THEN points ELSE c_points END";
		 	} else {
		 		order_by = sortStats[stat].db_field;
		 	}
		 	
		 	// RIVALS
			curRivals = App.dbGen.rawQuery("SELECT _id, name, player_name, points, c_points"
					+ ", " + sortStats[stat].db_field
					+ ", (SELECT CASE WHEN sg.c_points IS NULL THEN sg.points ELSE sg.c_points END pts"
					+ " FROM squad_gameweek sg WHERE sg.squad_id = _id AND gameweek = " + App.cur_gameweek + ") gw_pts"
					+ " FROM squad WHERE rival = 1 OR _id = " + App.my_squad_id
					+ " ORDER BY " + order_by + " DESC", null);
			SquadListCursorAdapter pAdapter = new SquadListCursorAdapter(this, curRivals, max_score_season, my_score, false, false, sortStats[stat], false, 0);
			rivalsList.setAdapter(pAdapter);
			OnItemClickListener listListener = new OnItemClickListener() {
				public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
					// load squad screen
					Intent intent = new Intent(view.getContext(), FPLSquad.class);
					intent.putExtra(FPLSquad.P_SQUADID, (int) id);
			    	startActivity(intent);
				}
		    };
		    rivalsList.setOnItemClickListener(listListener);
			// rivals header
			rivalsHeader.setText(curRivals.getCount() + " Rivals");
			rivalsList.refreshDrawableState();
	    } else {
	    	App.log("leagues assert thing 1");
	    }
	    myscore.close();
	    
	    set_screenshot_view();
	}
	
	// bind cursor to main list row item
    private static class LeaguesViewBinder implements SimpleCursorAdapter.ViewBinder {
    	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            int viewId = view.getId();
            TextView t = (TextView) view;
            boolean setText = true;
            
            if (viewId == R.id.numteams) {
            	String s = cursor.getString(columnIndex);
            	if (s != null) t.setText("/" + s);
            	setText = false;
            } else if (viewId == R.id.leaguename) {
            	t.setText(DbGen.decompress(cursor.getBlob(columnIndex)));
            	setText = false;
            }
            
            if (setText) t.setText(cursor.getString(columnIndex));
            return true;
        }
    }
    
    //public static final int DONT_SHOW = -1;
    
    public static class SquadListCursorAdapter extends CursorAdapter {
    	private int i_id, i_name, i_player_name, i_points, i_c_points, i_c_p_playing, i_stat, i_chip;
    	private int i_c_p_to_play, i_c_complete, i_gw_pts, i_c_goals, i_c_assists, i_c_cs, i_rival, i_c_bonus;
    	private boolean loaded_indexes;
    	private float max_season;
    	private int my_score;
    	private boolean gameweek;
    	private boolean mark_rivals;
    	private boolean showgac;
    	private Stat use_stat = null;
    	private Bitmap tick;
    	private Context c;
    	private int cup_opp;
    	
    	public SquadListCursorAdapter(Context context, Cursor cur, float max_score_season, int p_my_score
    			, boolean gw, boolean markRivals, Stat stat, boolean show_g_a_c, int cupopp) {
			super(context, cur);
			max_season = max_score_season;
			my_score = p_my_score;
			gameweek = gw;
			mark_rivals = markRivals;
			use_stat = stat;
			showgac = show_g_a_c;
			c = context;
			cup_opp = cupopp;
		}
    	//_id, name, player_name, points, c_points
		private void loadIndexes(Cursor cursor) {
        	loaded_indexes = true;
        	i_id = cursor.getColumnIndex("_id");
        	i_name = cursor.getColumnIndex("name");
        	i_player_name = cursor.getColumnIndex("player_name");
        	i_points = cursor.getColumnIndex("points");
        	i_c_points = cursor.getColumnIndex("c_points");
        	if (showgac) {
    	        i_c_p_playing = cursor.getColumnIndex("c_p_playing");
    	        i_c_p_to_play = cursor.getColumnIndex("c_p_to_play");
    	        i_c_complete = cursor.getColumnIndex("c_complete");
        		i_c_goals = cursor.getColumnIndex("c_goals");
	        	i_c_assists = cursor.getColumnIndex("c_assists");
	        	i_c_cs = cursor.getColumnIndex("c_cs");
	        	i_c_bonus = cursor.getColumnIndex("c_bonus");
	        	i_chip = cursor.getColumnIndex("chip");
        	}
        	i_gw_pts = cursor.getColumnIndex("gw_pts");
        	if (mark_rivals) {
        		i_rival = cursor.getColumnIndex("rival");
        		tick = BitmapFactory.decodeResource(c.getResources(), R.drawable.icon_tick);
        	}
        	if (use_stat != null) {
        		i_stat = cursor.getColumnIndex(use_stat.db_field);
        	}
        }
    	
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// load column indexes if required
			if (!loaded_indexes) loadIndexes(cursor);
			TextView t;
			ImageView i;
			
			int squad_id = cursor.getInt(i_id);
        	
			t = (TextView) view.findViewById(R.id.teamname);
			t.setText(DbGen.decompress(cursor.getBlob(i_name)));
			
			t = (TextView) view.findViewById(R.id.chip);
			String chip_suffix = "";
			if (showgac) {
				int chip = cursor.getInt(i_chip);
				if (chip != 0) {
					chip_suffix = ScrapeSquadGameweek.chip_strings[chip];
				}
			}
			t.setText(chip_suffix);
			
			t = (TextView) view.findViewById(R.id.playername);
			t.setText(DbGen.decompress(cursor.getBlob(i_player_name)));
			
			int complete = cursor.getInt(i_c_complete);
			i = (ImageView) view.findViewById(R.id.complete_img);
			if (showgac && (complete == 1) ) {
				i.setVisibility(View.VISIBLE);
			} else {
				i.setVisibility(View.GONE);
			}
			
			i = (ImageView) view.findViewById(R.id.cup);
			if ( (cup_opp > 0) && (cup_opp == squad_id) ) {
				i.setVisibility(View.VISIBLE);
			} else {
				i.setVisibility(View.GONE);
			}
			
			if (mark_rivals) {
				int rival = cursor.getInt(i_rival);
				i = (ImageView) view.findViewById(R.id.rivalTick);
				if (rival > 0) {
					i.setImageBitmap(tick);
				} else {
					i.setImageBitmap(null);
				}
			}
			
			int playing = cursor.getInt(i_c_p_playing);
			boolean capt_playing = false;
			if (playing < 0) {
				capt_playing = true;
				playing = -playing;
			}
			i = (ImageView) view.findViewById(R.id.playing_img);
			t = (TextView) view.findViewById(R.id.playing);
			if (showgac && (playing > 0) ) {
				i.setVisibility(View.VISIBLE);
				t.setVisibility(View.VISIBLE);
				String capt_string;
				if (capt_playing) {
					capt_string = CAPT_STRING_C;
				} else {
					capt_string = CAPT_STRING_BLANK;
				}
				t.setText(playing + capt_string);
			} else {
				i.setVisibility(View.GONE);
				t.setVisibility(View.GONE);
			}
			
			int to_play = cursor.getInt(i_c_p_to_play);
			boolean capt_to_play = false;
			if (to_play < 0) {
				capt_to_play = true;
				to_play = -to_play;
			}
			i = (ImageView) view.findViewById(R.id.to_play_img);
			t = (TextView) view.findViewById(R.id.to_play);
			if (showgac && (to_play > 0) ) {
				i.setVisibility(View.VISIBLE);
				t.setVisibility(View.VISIBLE);
				String capt_string;
				if (capt_to_play) {
					capt_string = CAPT_STRING_C;
				} else {
					capt_string = CAPT_STRING_BLANK;
				}
				t.setText(to_play + capt_string);
			} else {
				i.setVisibility(View.GONE);
				t.setVisibility(View.GONE);
			}
			
			int goals = cursor.getInt(i_c_goals);
			i = (ImageView) view.findViewById(R.id.goals_img);
			t = (TextView) view.findViewById(R.id.goals);
			if (!showgac) {
				i.setVisibility(View.GONE);
				t.setVisibility(View.GONE);
			} else {
				i.setVisibility(View.VISIBLE);
				t.setVisibility(View.VISIBLE);
				t.setText(String.valueOf(goals));
			}
			
			int assists = cursor.getInt(i_c_assists);
			i = (ImageView) view.findViewById(R.id.assists_img);
			t = (TextView) view.findViewById(R.id.assists);
			if (!showgac) {
				i.setVisibility(View.GONE);
				t.setVisibility(View.GONE);
			} else {
				i.setVisibility(View.VISIBLE);
				t.setVisibility(View.VISIBLE);
				t.setText(String.valueOf(assists));
			}
			
			int cs = cursor.getInt(i_c_cs);
			i = (ImageView) view.findViewById(R.id.cs_img);
			t = (TextView) view.findViewById(R.id.cs);
			if (!showgac) {
				i.setVisibility(View.GONE);
				t.setVisibility(View.GONE);
			} else {
				i.setVisibility(View.VISIBLE);
				t.setVisibility(View.VISIBLE);
				t.setText(String.valueOf(cs));
			}
			
			int bonus = cursor.getInt(i_c_bonus);
			i = (ImageView) view.findViewById(R.id.bonus_img);
			t = (TextView) view.findViewById(R.id.bonus);
			if (!showgac) {
				i.setVisibility(View.GONE);
				t.setVisibility(View.GONE);
			} else {
				i.setVisibility(View.VISIBLE);
				t.setVisibility(View.VISIBLE);
				t.setText(String.valueOf(bonus));
			}
			
			Worm worm = (Worm) view.findViewById(R.id.seasonworm);
			t = (TextView) view.findViewById(R.id.diff);
			
			int points = cursor.getInt(i_points);
			int c_points = cursor.getInt(i_c_points);
			int use_points;
			if (c_points > 0) {
				use_points = c_points;
				t.setTextColor(0xffffffff);
				worm.fadescore = false;
			} else {
				use_points = points;
				t.setTextColor(0xffaaaaaa);
				worm.fadescore = true;
			}
			
			// either points diff or stat
			boolean show_stat = false;
			if (use_stat != null) {
				if (!use_stat.db_field.equals(POINTS)) {
					show_stat = true;
				}
			}
			if (show_stat) {
				if (use_stat.divide > 0) {
		 			float num = cursor.getFloat(i_stat) / use_stat.divide;
		 			t.setText(String.valueOf(num));
		 		} else {
		 			t.setText(cursor.getString(i_stat));
		 		}
			} else {
				if (squad_id != App.my_squad_id) {
					int diff = use_points - my_score;
					String mod = "";
					if (diff > 0) {
						mod = "+";
					}
					t.setText(mod + diff);
				} else {
					t.setText("");
				}
			}
			
			int gw_pts = cursor.getInt(i_gw_pts);
			
			worm.max_pts = max_season;
			if (gameweek) {
				worm.squad = false;
				worm.squad_gw = true;
			} else {
				worm.squad = true;
				worm.squad_gw = false;
			}
			worm.total_pts = use_points;
	    	worm.points[0] = use_points - gw_pts;
			worm.points[1] = gw_pts;
			worm.points[2] = 0;
			worm.points[3] = 0;
			worm.invalidate();
			
			RelativeLayout rl = (RelativeLayout) view.findViewById(R.id.backing);
			if (squad_id == App.my_squad_id) {
				rl.setBackgroundResource(R.drawable.list_item_gradient_green);
    	 	} else {
    	 		rl.setBackgroundResource(R.drawable.list_item_gradient);
    	 	}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final View view = LayoutInflater.from(context).inflate(R.layout.leagues_screen_rival_item, parent, false);
		    return view;
		}
    	
    }
    
    public static void addRival(int id) {
    	Cursor exists = App.dbGen.rawQuery("SELECT name FROM squad WHERE _id = " + id, null);
    	exists.moveToFirst();
    	if (exists.isAfterLast()) {
    		// create
    		ContentValues ins = new ContentValues();
    		ins.put("_id", id);
    		ins.put("name", DbGen.compress(" "));
    		ins.put("player_name", DbGen.compress(" "));
    		ins.put("rival", 1);
    		App.dbGen.insert("squad", null, ins);
    	} else {
    		// update
    		App.dbGen.execSQL("UPDATE squad SET rival = 1 WHERE _id = " + id);
    	}
    	exists.close();
    	
    	// squad scrape but only for rival lineups (ie not leagues)
		if (Settings.getBoolPref(Settings.PREF_UPDATE_DATA)) {
			App.initProc(true, FPLService.PROC_SCRAPE_SQUADS, 1);
		} else {
			if (App.active != null) {
				Toast.makeText(App.active, "Automatic sync disabled; please run Leagues sync to finish adding this rival", Toast.LENGTH_LONG).show();
			}
		}
    }
    
    public static void joinLeague() {
    	App.initProc(false, FPLService.JOIN_LEAGUE);
    }
    
    private void set_screenshot_view() {
		if (viewPager.getCurrentItem() == TAB_I_RIVALS) {
			setScreenshotView(rivalsList, "All of my FPL rivals");
		} else if (viewPager.getCurrentItem() == TAB_I_LEAGUES) {
			setScreenshotView(null, null);
		}
	}
    
    // callback
    public void dataChanged() {
    	popList();
    }
    
    protected void onDestroy() {
    	closeCursors();
    	super.onDestroy();
    }
    
}
