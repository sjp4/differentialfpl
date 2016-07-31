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
import com.pennas.fpl.Selection.TransferHistItem;
import com.pennas.fpl.Selection.ViewPagerAdapter;
import com.pennas.fpl.process.FPLService;
import com.pennas.fpl.ui.Worm;
import com.pennas.fpl.util.ClickFPL;
import com.pennas.fpl.util.FPLActivity;
import com.pennas.fpl.util.BitmapCache;
import com.pennas.fpl.util.DbGen;
import com.pennas.fpl.util.FPLAsyncTask;
import com.pennas.fpl.util.Refreshable;
import com.pennas.fpl.util.SquadUtil;
import com.viewpagerindicator.TitlePageIndicator;

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
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class FPLSquad extends FPLActivity implements Refreshable, ClickFPL {
	SimpleCursorAdapter adapter;
	private int squadId;
	private String squadName, managerName;
	
	private ListView statsList, gameweeksList, playersList, transfersList, vidiprinter;
	private XYPlot seasongraph;
	private TextView meVsThem, rivalText;
	private float max_score_gw, max_score_season;
	private LinearLayout tab_graph;
	
	Cursor curHist, curPlayers, curVidi;
	
	public static final String P_SQUADID = "com.pennas.fpl.squadid";
	private ArrayList<SquadStatEntry> statValues;
	
	private Worm seasonWorm;
	private int rival;
	private BitmapCache bmp;
	
	private boolean players_tab_selected = false;
	private boolean player_tab_requires_refresh = true;
	
	private static final int TAB_PLAYERS = 0;
	private static final int TAB_STATS = 1;
	private static final int TAB_HISTORY = 2;
	private static final int TAB_TRANSFERS = 3;
	private static final int TAB_VIDIPRINTER = 4;
	private static final int TAB_GRAPH = 5;
	
	private ViewPager viewPager;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    setContentView(R.layout.squad_screen);
	    
	    hasTips = true;
        tipsPref = Settings.PREF_TIPS_SQUAD;
		tips.add("Mark as a rival (press the x) to automatically live-update selection and points");
		tips.add("Click on a gameweek item to see selection/points for that week");
		tips.add("Blue section on the gameweek score worm indicates a points hit");
		
	    // get ID of player
	    Bundle extras = getIntent().getExtras();
	    if(extras ==null ) return;
	    squadId = extras.getInt(P_SQUADID);
	    
	    // get details from db
        Cursor cur = App.dbGen.rawQuery("SELECT name, player_name FROM squad WHERE _id = " + squadId, null);
	 	cur.moveToFirst();
	    
	    rivalText 				= (TextView) findViewById(R.id.rivalText);
	 	seasonWorm    	    	= (Worm)  findViewById(R.id.seasonworm);
	 	
	 	squadName = DbGen.decompress(cur.getBlob(0));
	 	managerName = DbGen.decompress(cur.getBlob(1));
	 	
	 	// get/resolve team
	 	actionBar.setTitle(squadName);
	 	actionBar.setSubtitle(managerName);
	 	
	 	cur.close();
	 	
	 	LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	 	tab_graph = (LinearLayout) inflater.inflate(R.layout.squad_screen_graph_tab, null);
	 	seasongraph 			= (XYPlot) tab_graph.findViewById(R.id.seasongraph);
	 	meVsThem				= (TextView) tab_graph.findViewById(R.id.mevsthem);
	 	
		statsList = new ListView(this);
	 	gameweeksList = new ListView(this);
	 	playersList = new ListView(this);
	 	transfersList = new ListView(this);
	 	vidiprinter = new ListView(this);
	 	
	 	viewPager = (ViewPager) findViewById(R.id.viewpager);
		final String[] titles = new String[] { "Players", "Stats", "History", "Transfers", "Vidiprinter", "Graph" };
		final View[] views = new View[] { playersList, statsList, gameweeksList, transfersList, vidiprinter, tab_graph };
		ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(titles, views);
	    TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.titles);
	    viewPager.setAdapter(pagerAdapter);
	    indicator.setViewPager(viewPager);
	    indicator.setCurrentItem(2);
	    
	 	indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				if(position == TAB_PLAYERS) {
        	    	players_tab_selected = true;
        	    	loadPlayers();
        	    } else {
        	    	players_tab_selected = false;
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
	 	
        if (squadId == App.my_squad_id) {
	 		rivalText.setVisibility(View.GONE);
	 		rivalText.setEnabled(false);
	 		rivalText.setClickable(false);
	 	}
	 	
        // load player data into matches list, top section, etc
        popData();
	}
	
	private void loadPlayers() {
		if (player_tab_requires_refresh) {
			playerLoader = new LoadPlayersTask();
			playerLoader.fplExecute();
			
			player_tab_requires_refresh = false;
		}
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
	
	private XYSeries seriesGraphSquad;
	private XYSeries seriesGraphMe;
	
	private void drawGraph() {
		int max_week = 1;
		
		Cursor curSquad = App.dbGen.rawQuery("SELECT gameweek, CASE WHEN c_points ISNULL THEN points ELSE c_points END pts"
				+ " , points_hit FROM squad_gameweek WHERE squad_id = " + squadId + " and gameweek > 0"
				+ " ORDER BY gameweek ASC", null);
		Cursor curMe = App.dbGen.rawQuery("SELECT gameweek, CASE WHEN c_points ISNULL THEN points ELSE c_points END pts"
				+ " , points_hit FROM squad_gameweek WHERE squad_id = " + App.my_squad_id + " and gameweek > 0"
				+ " ORDER BY gameweek ASC", null);
		
		// find gameweek range (of this squad)
		curSquad.moveToFirst();
		while(!curSquad.isAfterLast()) {
			int gameweek = curSquad.getInt(0);
			if (gameweek > max_week) {
				max_week = gameweek;
			}
			curSquad.moveToNext();
		}
		// and my squad
		curMe.moveToFirst();
		while(!curMe.isAfterLast()) {
			int gameweek = curMe.getInt(0);
			if (gameweek > max_week) {
				max_week = gameweek;
			}
			curMe.moveToNext();
		}
		
		if (max_week > App.cur_gameweek) {
			max_week = App.cur_gameweek;
		}
		
		Number[] seriesSquad = new Number[max_week + 1];
		Number[] seriesMe = new Number[max_week + 1];
		
		// now fill in data for this squad
		curSquad.moveToFirst();
		while(!curSquad.isAfterLast()) {
			int gameweek = curSquad.getInt(0);
			int points = curSquad.getInt(1);
			int points_hit = curSquad.getInt(2);
			if (gameweek <= App.cur_gameweek) {
				seriesSquad[gameweek] = points - points_hit;
			}
			curSquad.moveToNext();
		}
		// and for my squad
		curMe.moveToFirst();
		while(!curMe.isAfterLast()) {
			int gameweek = curMe.getInt(0);
			int points = curMe.getInt(1);
			int points_hit = curMe.getInt(2);
			if (gameweek <= App.cur_gameweek) {
				seriesMe[gameweek] = points - points_hit;
			}
			curMe.moveToNext();
		}
		
		curSquad.close();
		curMe.close();
		
		if (squadId != App.my_squad_id) {
			// do week-by-week comparison
			int wins_me = 0;
			int wins_them = 0;
			for (int i=1; i<=max_week; i++) {
				if ( (seriesMe[i] != null) && (seriesSquad[i] != null) ) {
					if (seriesMe[i].intValue() > seriesSquad[i].intValue()) {
						wins_me++;
					} else if (seriesSquad[i].intValue() > seriesMe[i].intValue()) {
						wins_them++;
					}
				}
			}
			meVsThem.setText(App.my_squad_name + " " + wins_me + " " + squadName + " " + wins_them);
		}
		
		if (seriesGraphSquad != null) {
			seasongraph.removeSeries(seriesGraphSquad);
		}
		if (seriesGraphMe != null) {
			if (squadId != App.my_squad_id) {
				seasongraph.removeSeries(seriesGraphMe);
			}
		}
		
		seriesGraphSquad = new SimpleXYSeries(Arrays.asList(seriesSquad), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, squadName);
		seriesGraphMe = new SimpleXYSeries(Arrays.asList(seriesMe), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, App.my_squad_name);  
		
		seasongraph.addSeries(seriesGraphSquad, new LineAndPointFormatter(0xff0000ff, 0xff0000ff, 0xff0000ff));
		if (squadId != App.my_squad_id) {
			seasongraph.addSeries(seriesGraphMe, new LineAndPointFormatter(0xffffff00, 0xffffff00, 0x55ffff00));
		}
		
		seasongraph.setRangeLowerBoundary(0, BoundaryMode.AUTO);
		
		seasongraph.disableAllMarkup();
		seasongraph.setRangeLabel("Points");
		seasongraph.setDomainLabel("Gameweek");
		seasongraph.getGraphWidget().setDomainValueFormat(new DecimalFormat("0"));
		seasongraph.getGraphWidget().setRangeValueFormat(new DecimalFormat("0"));
		
		seasongraph.redraw();
	}
	
	// class describing a stat entry
	private static class SquadStatEntry {
		public String desc;
		public String value;
		public String statId;
	}
	
	// adapter for stats listview
	private class SquadStatEntryAdapter extends ArrayAdapter<SquadStatEntry> {
		private ArrayList<SquadStatEntry> items;

		public SquadStatEntryAdapter(Context context, int textViewResourceId, ArrayList<SquadStatEntry> items) {
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
			
			SquadStatEntry it = items.get(position);
			if (it != null) {
				TextView t = (TextView) v.findViewById(R.id.statdesc);
				t.setText(it.desc);
				t = (TextView) v.findViewById(R.id.statvalue);
				t.setText(it.value);
			}
			
			return v;
		}
	}
	
	// adapter for transfers listview
	private class TransferEntryAdapter extends ArrayAdapter<TransferHistItem> {
		
		public TransferEntryAdapter(Context context, int textViewResourceId, ArrayList<TransferHistItem> items) {
			super(context, textViewResourceId, items);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		        v = vi.inflate(R.layout.squad_screen_transfer_item, null);
			}
			
			try {
				TransferHistItem in  = transferHistIn.get(position);
				TransferHistItem out = transferHistOut.get(position);
				
				if ( (in != null) && (out != null) ) {
					RelativeLayout 	rl = (RelativeLayout)    v;
	    			TextView 		name_out   = (TextView)  rl.findViewById(R.id.playernameout);
	    			TextView 		name_in    = (TextView)  rl.findViewById(R.id.playernamein);
	    			ImageView 		pic_out    = (ImageView) rl.findViewById(R.id.playerpicout);
	    			ImageView 		pic_in     = (ImageView) rl.findViewById(R.id.playerpicin);
	    			TextView 		points_out = (TextView)  rl.findViewById(R.id.pointsout);
	    			TextView 		points_in  = (TextView)  rl.findViewById(R.id.pointsin);
	    			TextView 		gameweek   = (TextView)  rl.findViewById(R.id.gameweek);
	    			
	    			name_out.setText(out.name);
	    			name_in.setText(in.name);
	    			pic_out.setImageResource(Players.getPlayerPic(FPLSquad.this, out.pic_code));
	    			pic_in.setImageResource(Players.getPlayerPic(FPLSquad.this, in.pic_code));
	    			points_out.setText(String.valueOf(out.gw_points));
	    			points_in.setText(String.valueOf(in.gw_points));
	    			gameweek.setText("GW " + out.gameweek);
	    			
	    			if ((position % 2) == 0) {
	    				rl.setBackgroundColor(Selection.COLOUR1);
	    			} else {
	    				rl.setBackgroundColor(Selection.COLOUR2);
	    			}
				}
			} catch (IndexOutOfBoundsException e) {
				App.exception(e);
			}
			
			return v;
		}
	}
	
	// populate data
	private void popData() {
		// get details from db
        Cursor cur = App.dbGen.rawQuery("SELECT * FROM squad WHERE _id = " + squadId, null);
	 	cur.moveToFirst();
	 	
	 	// get/resolve team
	 	int points = cur.getInt(cur.getColumnIndex("points"));
	 	int c_points = cur.getInt(cur.getColumnIndex("c_points"));
	 	int use_points;
	 	if (c_points > 0) {
	 		use_points = c_points;
	 	} else {
	 		use_points = points;
	 	}
	 	
	 	rival = cur.getInt(cur.getColumnIndex("rival"));
	 	if (rival == 1) {
	 		rivalText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_tick, 0);
	 	} else {
	 		rivalText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_x, 0);
	 	}
	 	
	 	// get player stats from db
	 	loadStats();
	 	statValues = new ArrayList<SquadStatEntry>();
	 	for (Stat s : squadstats) {
	 		String val;
	 		if (s.divide > 0) {
	 			float num = cur.getFloat(cur.getColumnIndex(s.db_field)) / s.divide;
	 			val = String.valueOf(num);
	 		} else {
	 			val = cur.getString(cur.getColumnIndex(s.db_field));
	 		}
	 		if (s.show_value && (val != null) ) {
	 			SquadStatEntry stat = new SquadStatEntry();
	 			stat.desc = s.desc;
	 			stat.value = val;
	 			stat.statId = s.db_field;
	 			statValues.add(stat);
	 		}
	 	}
	 	
	 	cur.close();
	 	
	 	// adapter for stats array->list. sort by stat description
	 	SquadStatEntryAdapter statAdapter = new SquadStatEntryAdapter(this, R.layout.player_screen_stat_item, statValues);
	 	statAdapter.sort(new Comparator<SquadStatEntry>() {
    		public int compare(SquadStatEntry object1, SquadStatEntry object2) {
    			return object1.desc.compareTo(object2.desc);
    		}
    	});
	 	statsList.setAdapter(statAdapter);
	 	// click listener for stats list, goes to players screen, sorted by that stat
		OnItemClickListener listListener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
	            // load right stats screen
				Intent intent = new Intent(view.getContext(), Leagues.class);
				SquadStatEntry it = statValues.get(position);
				intent.putExtra(Leagues.P_STATID, it.statId);
				startActivity(intent);
	        }
	    };
		statsList.setOnItemClickListener(listListener);
	 	
		// get all players max gw score from db for worm
	 	Cursor curMax = App.dbGen.rawQuery("SELECT MAX(points) max_score, MAX(c_points) max_c_score FROM squad_gameweek", null);
	 	curMax.moveToFirst();
	 	max_score_gw = curMax.getFloat(1);
	 	float temp =  curMax.getFloat(0);
	 	if (temp > max_score_gw) {
	 		max_score_gw = temp;
	 	}
	 	curMax.close();
	 	
	 	Cursor minmax = App.dbGen.rawQuery("SELECT MAX(points) max_score, MAX(c_points) max_c_score FROM squad", null);
	 	minmax.moveToFirst();
	 	max_score_season = minmax.getFloat(1);
	 	temp = minmax.getFloat(0);
	 	if (temp > max_score_season) {
	 		max_score_season = temp;
	 	}
	 	minmax.close();
	 	
	 	int cur_gw_score = 0;
	 	Cursor curCur = App.dbGen.rawQuery("SELECT points, c_points FROM squad_gameweek WHERE squad_id = " + squadId
	 			+ " AND gameweek = " + App.cur_gameweek, null);
	 	curCur.moveToFirst();
	 	if (!curCur.isAfterLast()) {
	 		int gwp = curCur.getInt(0);
	 		int gwc_p = curCur.getInt(1);
	 		if (gwc_p > 0) {
	 			cur_gw_score = gwc_p;
	 		} else {
	 			cur_gw_score = gwp;
	 		}
	 	}
	 	curCur.close();
	 	
	 	// initialise the big season worm at the top of the screen
	 	seasonWorm.max_pts = max_score_season;
	 	seasonWorm.squad = true;
    	seasonWorm.total_pts = use_points;
    	seasonWorm.points[0] = use_points - cur_gw_score;
    	seasonWorm.points[1] = cur_gw_score;
    	seasonWorm.points[2] = 0;
    	seasonWorm.points[3] = 0;
    	seasonWorm.invalidate();
    	
    	player_tab_requires_refresh = true;
		
    	loader = new LoadTask();
		loader.fplExecute();
	}
	
	ArrayList<TransferHistItem> transferHistIn;
	ArrayList<TransferHistItem> transferHistOut;
	
	private void popDataMain() {
		curHist = App.dbGen.rawQuery("SELECT gameweek _id, c_points, points, c_complete, points_hit FROM squad_gameweek WHERE squad_id = " + squadId
				+ " AND gameweek <= " + App.cur_gameweek + " and gameweek > 0 ORDER BY gameweek ASC", null);
	 	// get current/next gw list position if possible
	 	curHist.moveToFirst();
	 	
	 	curVidi = App.dbGen.rawQuery("SELECT v.player_fpl_id _id, v.gameweek, v.datetime, v.message, v.category"
				+ " FROM vidiprinter v, squad_gameweek_player sgp WHERE sgp.squad_id = " + squadId
				+ " AND v.gameweek = sgp.gameweek AND v.player_fpl_id = sgp.fpl_player_id"
				+ " ORDER BY v.gameweek DESC, v.datetime DESC LIMIT " + Vidiprinter.limit, null);
		curVidi.moveToFirst();
		
		// load transfers
		Cursor curTran = App.dbGen.rawQuery("SELECT DISTINCT p._id, t.t_in, p.name, p.team_id"
				+ " , (SELECT SUM(pm.total) points FROM player_match pm WHERE pm.season = " + App.season
				+ "    AND pm.gameweek = t.gameweek AND pm.player_player_id = p._id) gw_points"
				+ " , p.code_14, t.gameweek"
				+ " FROM transfer t, player_season ps, player p"
				+ " WHERE t.squad_id = " + squadId
				+ " AND ps.season = " + App.season + " AND ps.fpl_id = t.player_fpl_id"
				+ " AND p._id = ps.player_id"
				+ " ORDER BY t.gameweek ASC, ps.position ASC", null);
		curTran.moveToFirst();
		transferHistIn = new ArrayList<TransferHistItem>();
		transferHistOut = new ArrayList<TransferHistItem>();
		while(!curTran.isAfterLast()) {
			TransferHistItem tran = new TransferHistItem();
			tran.player_id = curTran.getInt(0);
			tran.name = curTran.getString(2);
			tran.team_id = curTran.getInt(3);
			tran.gw_points = curTran.getInt(4);
			tran.pic_code = curTran.getInt(5);
			tran.gameweek = curTran.getInt(6);
			
			int t_in = curTran.getInt(1);
			if (t_in == 1) {
				transferHistIn.add(tran);
			} else {
				transferHistOut.add(tran);
			}
			curTran.moveToNext();
		}
		curTran.close();
	}
	
	private void closeCursorsMain() {
		if (curHist != null) {
			curHist.close();
		}
		if (curVidi != null) {
			curVidi.close();
		}
	}
	
	private void closeCursorsPlayers() {
		if (curPlayers != null) {
			curPlayers.close();
		}
	}
	
	// async task to load data
    private class LoadTask extends FPLAsyncTask<Void, Void, Void> {
    	ProgressDialog dialog;
    	
    	protected Void doInBackground(Void... params) {
    		App.log("FPLSquad LoadTask start");
    		popDataMain();
    		App.log("FPLSquad LoadTask complete");
    		return null;
        }
		protected void onPreExecute() {
			popBmpCache();
			
			dialog = new ProgressDialog(FPLSquad.this);
			dialog.setMessage("Loading..");
			dialog.setCancelable(false);
			dialog.show();
			FPLSquad.this.progress = dialog;
			
			closeCursorsMain();
		}
        protected void onPostExecute(Void res) {
        	String[] adapter_from = new String[] { "_id", "c_points", "c_complete" };
    		int[] adapter_to = new int[] { R.id.gameweek, R.id.worm, R.id.complete };
    		SimpleCursorAdapter adapterM = new SimpleCursorAdapter(FPLSquad.this, R.layout.squad_screen_gameweek_item, curHist, adapter_from, adapter_to);
    		adapterM.setViewBinder(new MatchesViewBinder());
    		gameweeksList.setAdapter(adapterM);
    		OnItemClickListener listListener = new OnItemClickListener() {
    			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
    				// load scores screen
    				Intent intent = new Intent(view.getContext(), Selection.class);
    		    	intent.putExtra(Selection.P_SELECTION, false);
    		    	intent.putExtra(Selection.P_SQUADID, squadId);
    		    	intent.putExtra(Selection.P_GAMEWEEK, (int) id);
    		    	startActivity(intent);
    			}
    	    };
    	    gameweeksList.setOnItemClickListener(listListener);
    	    
    	    int defPos = -1;
    	 	while (!curHist.isAfterLast()) {
    			int gw = curHist.getInt(0);
    			if ( (gw == App.cur_gameweek) || (gw == App.next_gameweek) ) {
    				// -3 to try and position current gw in middle of screen
    				defPos = curHist.getPosition() - 3;
    				break;
    			} // if
    			curHist.moveToNext();
    		} // while
    		
    		// set position in list to current/next gw if possible
    		if (defPos > -1) {
    			gameweeksList.setSelection(defPos);
    		}
    		
    		String[] columns = new String[] { "datetime", "message", "gameweek", "category" };
    		int[] to = new int[] { R.id.datetime, R.id.message, R.id.gameweek, R.id.cat };
    		SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(FPLSquad.this, R.layout.vidiprinter_screen_item, curVidi, columns, to);
    		mAdapter.setViewBinder(new Vidiprinter.MyViewBinder(false));
    		vidiprinter.setAdapter(mAdapter);
    		
    		OnItemClickListener vidiListener = new OnItemClickListener() {
    			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
    				Vidiprinter.vidiClickAction(id, view.getContext());
    			}
    	    };
    	    vidiprinter.setOnItemClickListener(vidiListener);
    		
    		drawGraph();
    		
    		// adapter for transfers array->list.
    	 	TransferEntryAdapter transferAdapter = new TransferEntryAdapter(FPLSquad.this, R.layout.squad_screen_transfer_item, transferHistIn);
    	 	transfersList.setAdapter(transferAdapter);
    	 	
    		dialog.dismiss();
    		loader = null;
    		
    		if (players_tab_selected) {
    			loadPlayers();
    		} else {
    			set_screenshot_view();
    		}
        }
    }
    //final View[] views = new View[] { playersList, statsList, gameweeksList, transfersList, vidiprinter, tab_graph };
    private void set_screenshot_view() {
		String squad_string = squadName + " (" + managerName + ")";
    	if (viewPager.getCurrentItem() == TAB_PLAYERS) {
			setScreenshotView(playersList, "Top contributing players for " + squad_string + " over the season");
		} else if (viewPager.getCurrentItem() == TAB_STATS) {
			setScreenshotView(statsList, "Squad stats for " + squad_string, squadstats.size());
		} else if (viewPager.getCurrentItem() == TAB_HISTORY) {
			setScreenshotView(gameweeksList, "Gameweek history for " + squad_string);
		} else if (viewPager.getCurrentItem() == TAB_TRANSFERS) {
			setScreenshotView(transfersList, "Transfer history for " + squad_string);
		} else if (viewPager.getCurrentItem() == TAB_VIDIPRINTER) {
			setScreenshotView(vidiprinter, "Vidipinter for " + squad_string);
		} else if (viewPager.getCurrentItem() == TAB_GRAPH) {
			String graph_string;
			if (squadId != App.my_squad_id) {
				graph_string = "Comparison graph showing " + squad_string + " vs " + App.my_squad_name;
			} else {
				graph_string = "Graph showing " + squad_string;
			}
			setScreenshotView(tab_graph, graph_string);
		}
	}
    
	// async task to load players
    private class LoadPlayersTask extends FPLAsyncTask<Void, Void, Void> {
    	ProgressDialog dialog;
    	
    	protected Void doInBackground(Void... params) {
    		App.log("FPLSquad LoadPlayersTask start");
    		
    		curPlayers = App.dbGen.rawQuery("SELECT p._id _id, p.team_id team_id, ps.fpl_id fpl_id, p.name name"
    	 			+ ", (ps.price / 10.0) price, ps.position position"
    	 			+ ", SUM(CASE WHEN squad.selected = 1 AND squad.captain = 1 THEN pm.total * 2"
    	 			+ "           WHEN squad.selected = 1 AND squad.captain = 0 THEN pm.total"
    	 			+ "           ELSE 0 END) points"
    	 			+ " FROM player p, player_season ps, player_match pm, "
    	 			+ " (SELECT sgp.gameweek gameweek, sgp.fpl_player_id fpl_player_id, "
    	 			+ " CASE WHEN sgp.selected = " + SquadUtil.SEL_SELECTED + " AND sgp.autosub IS NULL THEN 1"
    	 			+ "      WHEN sgp.selected > " + SquadUtil.SEL_SELECTED + " AND sgp.autosub = 1 THEN 1"
    	 			+ " ELSE 0 END selected,"
    	 			+ " CASE WHEN sgp.captain = " + SquadUtil.CAPTAIN + " THEN 1"
    	 			+ "      WHEN sgp.captain = " + SquadUtil.VICE_CAPTAIN + " AND sgp.vc = 1 THEN 1"
    	 			+ " ELSE 0 END captain"
    	 			+ " FROM squad_gameweek_player sgp"
    	 			+ " WHERE sgp.squad_id = " + squadId + ") squad"
    	 			+ " WHERE pm.season = " + App.season
    	 			+ " AND ps.season = pm.season"
    	 			+ " AND ps.fpl_id = squad.fpl_player_id"
    	 			+ " AND p._id = ps.player_id"
    	 			+ " AND pm.gameweek = squad.gameweek"
    	 			+ " AND pm.player_player_id = p._id"
    	 			+ " GROUP BY p._id"
    	 			+ " ORDER BY 7 DESC", null);
    	 	curPlayers.moveToFirst();
    	 	
    		App.log("FPLSquad LoadPlayersTask complete");
    		return null;
        }
		protected void onPreExecute() {
			popBmpCache();
			
			dialog = new ProgressDialog(FPLSquad.this);
			dialog.setMessage("Loading..");
			dialog.setCancelable(false);
			dialog.show();
			FPLSquad.this.progress = dialog;
			
			closeCursorsPlayers();
		}
        protected void onPostExecute(Void res) {
        	if (curPlayers != null) {
    			int max_player_points = 0;
        		
    			while (!curPlayers.isAfterLast()) {
    				int pp = curPlayers.getInt(curPlayers.getColumnIndex("points"));
    				if (pp > max_player_points) {
    					max_player_points = pp;
    				}
    				curPlayers.moveToNext();
    			}
    			
    			PlayersCursorAdapter playersAdapter = new PlayersCursorAdapter(FPLSquad.this, curPlayers, max_player_points);
	    		playersList.setAdapter(playersAdapter);
	    		OnItemClickListener playersListener = new OnItemClickListener() {
	    			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
	    				// load player screen for player "id"
	    				Intent intent = new Intent(view.getContext(), Player.class);
	    				intent.putExtra(Player.P_PLAYERID, (int) id);
	    				intent.putExtra(Player.P_SEASON, App.season);
	    		    	startActivity(intent);
	    			}
	    	    };
	    	    playersList.setOnItemClickListener(playersListener);
        	}
    		
    		dialog.dismiss();
    		playerLoader = null;
    		set_screenshot_view();
        }
    }
    
    private LoadTask loader;
    private LoadPlayersTask playerLoader;
    
    private class PlayersCursorAdapter extends CursorAdapter {
    	private int i_team_id, i_points, i_fpl_id, i_name, i_price, i_position;
    	private boolean loaded_indexes;
    	private int max_points;
    	
    	public PlayersCursorAdapter(Context context, Cursor c, int p_max_points) {
			super(context, c);
			max_points = p_max_points;
		}
    	
		private void loadIndexes(Cursor cursor) {
        	loaded_indexes = true;
        	i_points = cursor.getColumnIndex("points");
        	i_fpl_id = cursor.getColumnIndex("fpl_id");
        	i_team_id = cursor.getColumnIndex("team_id");
        	i_name = cursor.getColumnIndex("name");
        	i_position = cursor.getColumnIndex("position");
        	i_price = cursor.getColumnIndex("price");
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
			worm.squad = true;
			worm.max_pts = max_points;
        	worm.total_pts = cursor.getInt(i_points);
        	worm.points[0] = worm.total_pts;
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
        	if (App.mySquadNext.contains(fpl_id)) {
        		rl.setBackgroundResource(R.drawable.list_item_gradient_green);
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
	
	// viewbinder for matches listview
    private class MatchesViewBinder implements SimpleCursorAdapter.ViewBinder {
    	
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			int viewId = view.getId();
			if (viewId == R.id.worm) {
				int use_pts;
				int c_score = cursor.getInt(columnIndex);
				if (c_score > 0) {
					use_pts = c_score;
				} else {
					use_pts = cursor.getInt(columnIndex + 1);
				}
				int points_hit = cursor.getInt(columnIndex + 3);
				Worm worm = (Worm) view;
            	worm.max_pts = max_score_gw;
            	worm.squad_gw = true;
            	worm.total_pts = use_pts;
            	// 8/9/10 assists/bonus/goals
            	worm.points[0] = use_pts - points_hit;
            	worm.points[1] = points_hit;
            	worm.points[2] = 0;
            	worm.points[3] = 0;
            	worm.invalidate();
            } else if (viewId == R.id.complete) {
            	
            } else {
            	TextView t = (TextView) view;
            	t.setText(cursor.getString(columnIndex));
            }
			
			return true;
        }
    }
    
    public void clickRival(View v) {
    	// do before db update for quick gui update..
    	if (rival == 0) {
	 		rival = 1;
	 		rivalText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_tick, 0);
	 	} else {
	 		rival = 0;
	 		rivalText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_x, 0);
	 	}
    	App.dbGen.execSQL("UPDATE squad SET rival = " + rival + " WHERE _id = " + squadId);
    	// do after db update.. just added as rival
    	if (rival == 1) {
    		// squad scrape but only for rival lineups (ie not leagues)
    		if (Settings.getBoolPref(Settings.PREF_UPDATE_DATA)) {
    			App.initProc(true, FPLService.PROC_SCRAPE_SQUADS, 1);
    		} else  {
    			Toast.makeText(this, "Automatic sync disabled; please run Leagues sync to finish adding this rival", Toast.LENGTH_LONG).show();
    		}
    	} else {
    		// remove SGW and transfers
    		App.dbGen.execSQL("DELETE FROM squad_gameweek_player WHERE squad_id = " + squadId);
    		App.dbGen.execSQL("DELETE FROM transfer WHERE squad_id = " + squadId);
    	}
    }
    
    public void clickFPL() {
    	Uri uri = Uri.parse( "http://fantasy.premierleague.com/entry/" + squadId + "/history/");
		startActivity( new Intent( Intent.ACTION_VIEW, uri ) );
    }
    
    public static ArrayList<Stat> squadstats;
    
    // load stat defs into memory
    public static void loadStats() {
    	if (squadstats != null) {
    		return;
    	}
    	
    	squadstats = new ArrayList<Stat>();
    	
    	Cursor curStat = App.dbGen.rawQuery("SELECT db_field, type, short_lab, desc, show_value, always_show_sort, divide FROM stat WHERE player_stat = 4 ORDER BY desc ASC", null);
    	curStat.moveToFirst();
    	while (!curStat.isAfterLast()) {
    		Stat s = new Stat();
    		s.db_field = curStat.getString(0);
    		s.type = curStat.getInt(1);
    		s.short_lab = curStat.getString(2);
    		s.desc = curStat.getString(3);
    		s.show_value = (curStat.getInt(4) == 1);
    		s.always_show_sort = curStat.getInt(5);
    		s.divide = curStat.getFloat(6);
    		squadstats.add(s);
    		//AppClass.log("Add " + s.db_field + " -> " + s.desc);
    		curStat.moveToNext();
    	}
    	curStat.close();
    }
    
    // callback
    public void dataChanged() {
    	if (loader != null) {
			loader.cancel(true);
    	}
    	if (playerLoader != null) {
    		playerLoader.cancel(true);
    	}
    	if (progress != null) {
    		progress.dismiss();
    	}
    	popData();
    }
    
    // not used here, but needed to avoid crash when wrong part of player item is clicked
    public void clickStat(View v) {
    	
    }
    
    public ProgressDialog progress;
    
    protected void onDestroy() {
    	if (loader != null) {
			loader.cancel(true);
    	}
    	if (playerLoader != null) {
    		playerLoader.cancel(true);
    	}
    	if (progress != null) {
    		progress.dismiss();
    	}
    	closeCursorsMain();
    	closeCursorsPlayers();
    	super.onDestroy();
    }

}
