package com.pennas.fpl;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

import com.pennas.fpl.scrape.ScrapeMySquad;
import com.pennas.fpl.scrape.ScrapeSquadGameweek;
import com.pennas.fpl.ui.Ticker;
import com.pennas.fpl.ui.Ticker.TickerData;
import com.pennas.fpl.util.ClickFPL;
import com.pennas.fpl.util.FPLActivity;
import com.pennas.fpl.util.FPLAsyncTask;
import com.pennas.fpl.util.GraphicUtil;
import com.pennas.fpl.util.Refreshable;
import com.pennas.fpl.util.SquadUtil;
import com.pennas.fpl.util.SquadUtil.*;
import com.pennas.fpl.Players.Stat;
import com.pennas.fpl.process.FPLService;
import com.pennas.fpl.process.ProcessData;
import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

// this activity has two distinct modes: selection/scores
public class Selection extends FPLActivity implements Refreshable, ClickFPL {
	// main grid
	public SquadPlayer[][] playerGrid = new SquadPlayer[5][5];
	private boolean allow_load = true;
	
	// activity mode
	public boolean c_selection;
	
	// activity params
	private int c_squadid;
	private int c_gameweek;
	
	// intent params
	public static final String P_SELECTION = "com.pennas.fpl.selection.selection";
	public static final String P_SQUADID = "com.pennas.fpl.selection.squadid";
	public static final String P_GAMEWEEK = "com.pennas.fpl.selection.gameweek";
	
	public static final int CURRENT_GW = -1;
	
	public static final int NONE = -1;

	public TextView[][] pCell = new TextView[5][5];
	public Ticker[][] pCellTicker = new Ticker[5][5];
	
	public Squad squad;
	
	private static final int NUM_TEAMS = 33;
	private static final int MAX_PLAYERS_FROM_TEAM = 3;
	
	private static final int TRAFFIC_NONE = 0;
	private static final int TRAFFIC_FIXDIFF = 1;
	private static final int TRAFFIC_PREDSCORE = 2;
	private static final int TRAFFIC_PREDCS = 3;
	
	private TextView gwScore, statTotals, totalScore, squadSellValueLab, squadSellValue, squadValue, squadBankLab, squadBank, transfersField;
	
	private TextView transferButton, nullMsg, confirmButton, vcButton, captButton, subButton, chip_header;
	
	private EditText notes;
	private TextView notes_left;
	
	private ScrollView tab_transfers_scroll;
	private LinearLayout transfersList, tab_grid, tab_stats, playersList, grid_scrollview_content;
	private RelativeLayout tab_notes;
	
	private int selected_player;
	
	private boolean subMode = false;
	private int subId;
	
	private GraphicUtil gUtil;
	public static final int RESPONSE_DIALOG = 1;
	
	private int season = App.season;
	private int stat = 0;
	private int traffic = 0;
	
	private static final int FILTER_GAMEWEEK = 1;
    private static final int FILTER_STAT = 2;
    private static final int FILTER_TRAF = 3;
    
    private static final int TAB_NOTES_STATS = 0;
	private static final int TAB_PLAYERS = 1;
	private static final int TAB_TRANSFERS = 2;
    
    public static Selection currentSelection;
	
	private ViewPager viewPager;
	
	private SquadUtil squadUtil = new SquadUtil();
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Debug.startMethodTracing("selection2");
		setContentView(R.layout.selection_screen);
		
		// process intent params
		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			allow_load = false;
			return;
		}
		c_selection = extras.getBoolean(P_SELECTION);
		c_squadid = extras.getInt(P_SQUADID);
		c_gameweek = extras.getInt(P_GAMEWEEK);
		App.log("c_selection param: " + c_selection + " c_squadid = " + c_squadid);
		App.log("c_gameweek param: " + c_gameweek + " (App.cur_gameweek = " + App.cur_gameweek + ")");
		if (c_gameweek == CURRENT_GW) {
			c_gameweek = App.cur_gameweek;
			if ( (c_gameweek == 0) && !c_selection) {
				App.log("not loading points for gw0");
				Toast.makeText(this, "Points not available during gameweek 0", Toast.LENGTH_LONG).show();
				allow_load = false;
				return;
			}
			App.log("changed c_gameweek from CURRENT_GW to: " + c_gameweek);
		}
		
		currentSelection = this;
		
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		tab_transfers_scroll = (ScrollView) inflater.inflate(R.layout.selection_screen_transfers_tab, null);
		transfersList = (LinearLayout) tab_transfers_scroll.findViewById(R.id.tab_transfers);
		if (c_selection) {
			tab_notes = (RelativeLayout) inflater.inflate(R.layout.selection_screen_notes_tab, null);
		} else {
			tab_stats = (LinearLayout) inflater.inflate(R.layout.selection_screen_stats_tab, null);
		}
		tab_grid = (LinearLayout) inflater.inflate(R.layout.selection_screen_grid_tab, null);
		grid_scrollview_content = (LinearLayout) tab_grid.findViewById(R.id.grid_scrollview_content);
		
		transfersField = (TextView) tab_grid.findViewById(R.id.transfers);
		squadBankLab = (TextView) tab_grid.findViewById(R.id.squadBankLab);
		squadBank = (TextView) tab_grid.findViewById(R.id.squadBank);
		squadValue = (TextView) tab_grid.findViewById(R.id.squadValue);
		squadSellValue = (TextView) tab_grid.findViewById(R.id.squadSellValue);
		squadSellValueLab = (TextView) tab_grid.findViewById(R.id.squadSellValueLab);
		subButton = (TextView) tab_grid.findViewById(R.id.subButton);
		captButton = (TextView) tab_grid.findViewById(R.id.captButton);
		vcButton = (TextView) tab_grid.findViewById(R.id.vcButton);
		vcButton.setTextColor(DISABLED_COLOUR);
		gwScore = (TextView) tab_grid.findViewById(R.id.gwScore);
		totalScore = (TextView) tab_grid.findViewById(R.id.totalScore);
		statTotals = (TextView) tab_grid.findViewById(R.id.statTotals);
		confirmButton = (TextView) tab_grid.findViewById(R.id.confirmButton);
		transferButton = (TextView) tab_grid.findViewById(R.id.transferButton);
		nullMsg = (TextView) tab_grid.findViewById(R.id.nullMsg);
		chip_header = (TextView) tab_grid.findViewById(R.id.chip_header);
		
		if (!c_selection) {
			playersList = (LinearLayout) tab_stats.findViewById(R.id.playersList);
		}
		
		if (c_selection) {
			gwScore.setVisibility(View.GONE);
			LinearLayout key = (LinearLayout) tab_grid.findViewById(R.id.key);
			key.setVisibility(View.GONE);
			LinearLayout tri = (LinearLayout) tab_grid.findViewById(R.id.transferInfo);
			tri.setVisibility(View.GONE);
		} else {
			LinearLayout tb = (LinearLayout) tab_grid.findViewById(R.id.topbuttons);
			tb.setVisibility(View.GONE);
			statTotals.setVisibility(View.GONE);
			squadSellValueLab.setVisibility(View.GONE);
			squadSellValue.setVisibility(View.GONE);
			squadBank.setVisibility(View.GONE);
			squadBankLab.setVisibility(View.GONE);
		}
		
		hasTips = true;
		if (c_selection) {
			tipsPref = Settings.PREF_TIPS_SELECTION;
			tips.add("Touch a player to select them");
			tips.add("Click sub/transfer to swap highlighted player");
			tips.add("If subbing, eligable players to swap will be highlighted in green - touch to swap");
			tips.add("Once transfers/subs have been chosen, click 'Confirm' to send to FPL");
			tips.add("Any required tranfers will be alerted to you before confirming with FPL");
			tips.add("Once selected, touch player again for more details");
			tips.add("Use the filter icon to display stats and/or traffic lights for each player");
		} else {
			tipsPref = Settings.PREF_TIPS_SCORES;
			tips.add("Points are updated live as they are downloaded");
			tips.add("Icons show current status of each player in the gameweek - see key below");
			tips.add("'Finished + Bonus' means that bonus points have been allocated for match");
			tips.add("Auto-subs are processed as soon as outgoing player's match has finished");
		}
		
		// set xml resource ids for player grid
		setCell(0, 2, R.id.p0_2);
		
		setCell(1, 0, R.id.p1_0);
		setCell(1, 1, R.id.p1_1);
		setCell(1, 2, R.id.p1_2);
		setCell(1, 3, R.id.p1_3);
		setCell(1, 4, R.id.p1_4);
		
		setCell(2, 0, R.id.p2_0);
		setCell(2, 1, R.id.p2_1);
		setCell(2, 2, R.id.p2_2);
		setCell(2, 3, R.id.p2_3);
		setCell(2, 4, R.id.p2_4);
		
		setCell(3, 1, R.id.p3_1);
		setCell(3, 2, R.id.p3_2);
		setCell(3, 3, R.id.p3_3);
		
		setCell(4, 0, R.id.p4_0);
		setCell(4, 2, R.id.p4_2);
		setCell(4, 3, R.id.p4_3);
		setCell(4, 4, R.id.p4_4);
		
		traffic = Settings.getIntPref(Settings.PREF_SELECTION_TRAFFIC);
		stat = Settings.getIntPref(Settings.PREF_SELECTION_STAT);
		
		// populate gameweek dropdown
		if (!c_selection) {
			String[] gameweeks = new String[App.cur_gameweek];
			for (int i=0; i<App.cur_gameweek; i++) {
				gameweeks[i] = String.valueOf(i + 1);
			}
			addFilter(FILTER_GAMEWEEK, "Gameweek", gameweeks, c_gameweek - 1, true, false);
		}
		
		subMode = false;
		
		if (c_selection) {
			initStatDropdown();
			
		    addFilter(FILTER_TRAF, "Traffic Lights", R.array.traffic_array_with_none, traffic, true, false);
		}
		
		if (c_selection) {
			notes = (EditText) tab_notes.findViewById(R.id.notes);
			InputFilter[] FilterArray = new InputFilter[1];
			FilterArray[0] = new InputFilter.LengthFilter(ScrapeMySquad.NOTES_MAX_SIZE);
			notes.setFilters(FilterArray);
			notes.addTextChangedListener(new TextWatcher(){
		        public void afterTextChanged(Editable s) {
		            notes_left.setText(notes.length() + " / " + ScrapeMySquad.NOTES_MAX_SIZE);
		        }
		        public void beforeTextChanged(CharSequence s, int start, int count, int after){}
		        public void onTextChanged(CharSequence s, int start, int before, int count){}
		    }); 
			
			notes_left = (TextView) tab_notes.findViewById(R.id.notes_left);
		}
		
		String[] titles;
		View[] views;
		if (c_selection) {
			titles = new String[] { "Notes", "Players", "Transfers" };
			views = new View[] { tab_notes, tab_grid, tab_transfers_scroll };
		} else {
			titles = new String[] { "Stats", "Players", "Transfers" };
			views = new View[] { tab_stats, tab_grid, tab_transfers_scroll };
		}
		ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(titles, views);
		viewPager = (ViewPager) findViewById(R.id.viewpager);
	    TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.titles);
	    viewPager.setAdapter(pagerAdapter);
	    indicator.setViewPager(viewPager);
	    viewPager.setCurrentItem(1);
	    
	    indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				if (c_selection) {
					if(position == 1) {
	        	    	setFilterVisible(FILTER_TRAF, true);
	        	    	setFilterVisible(FILTER_STAT, true);
	        	    } else {
	        	    	setFilterVisible(FILTER_TRAF, false);
	        	    	setFilterVisible(FILTER_STAT, false);
	        	    }
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
		
		Selection old = (Selection) getLastNonConfigurationInstance();
        if (old != null) {
        	App.log("Selection: restoring pre-rotation squad");
        	squad = old.squad;
        	playerGrid = old.playerGrid;
        	transfers = old.transfers;
        	selected_player = old.selected_player;
        	populateGrid(true);
        } else {
        	// load data/populate GUI
        	load();
        }
	}
	
	private ArrayList<String> stat_names;
	private ArrayList<String> stat_fields;
	private static final String STAT_NONE = "none";
	private static final String STAT_SELL_PRICE = "sell_price";
	private static final String STAT_PRICE = "price";
	private static final String STAT_POINTS = "points";
	private static final String STAT_PROFIT = "profit";
	private static final String STAT_BOUGHT_PRICE = "bought_price";
	
	public static class ViewPagerAdapter extends PagerAdapter implements TitleProvider {
		private String[] titles;
		private View[] views;
		
		public ViewPagerAdapter(String[] pTitles, View[] pViews) {
			titles = pTitles;
			views = pViews;
		}
	    
	    @Override
	    public String getTitle(int position) {
	        return titles[position];
	    }
	 
	    @Override
	    public int getCount() {
	        return titles.length;
	    }
	 
	    @Override
	    public Object instantiateItem(View pager, int position) {
	        View v = views[position];
	        if (v == null) {
	        	App.log(position + " is null");
	        }
	        ((ViewPager)pager).addView(v, 0);
	        return v;
	    }
	 
	    @Override
	    public void destroyItem(View pager, int position, Object view) {
	        ((ViewPager)pager).removeView((View) view);
	    }
	 
	    @Override
	    public boolean isViewFromObject(View view, Object object) {
	        return view.equals(object);
	    }
	}
	
	private void initStatDropdown() {
		stat_names = new ArrayList<String>();
		stat_fields = new ArrayList<String>();
		
		stat_fields.add(STAT_NONE);
		stat_names.add("None");
		
		stat_fields.add(STAT_SELL_PRICE);
		stat_names.add("Selling Price");
		
		stat_fields.add(STAT_PROFIT);
		stat_names.add("Profit");
		
		stat_fields.add(STAT_PRICE);
		stat_names.add("Price");
		
		stat_fields.add(STAT_BOUGHT_PRICE);
		stat_names.add("Bought Price");
		
		stat_fields.add(STAT_POINTS);
		stat_names.add("Points");
		
		Players.loadStats();
		for (Stat s : Players.playerstats_array) {
	 		if (s.show_value) {
	 			stat_fields.add(s.db_field);
	 			stat_names.add(s.desc);
	 		}
	 	}
		
		if (stat >= stat_names.size()) {
	    	stat = 0;
	    }
	    addFilter(FILTER_STAT, "Stat", stat_names.toArray(new String[stat_names.size()]), stat, true, false);
	}
	
	@Override
	protected void filterSelection(int id, int pos) {
		App.log("subclass");
    	
    	switch(id) {
	    	case (FILTER_GAMEWEEK):
				int newGw = pos + 1;
				if (newGw != c_gameweek) {
					c_gameweek = newGw;
					load();
				}
				break;
			case (FILTER_STAT):
				if (pos != stat) {
					stat = pos;
					populateGrid(true);
					Settings.setIntPref(Settings.PREF_SELECTION_STAT, stat, Selection.this);
				}
				break;
			case (FILTER_TRAF) :
				if (pos != traffic) {
					traffic = pos;
					populateGrid(false);
					Settings.setIntPref(Settings.PREF_SELECTION_TRAFFIC, traffic, Selection.this);
				}
				break;
    	}
	}
	
	private SelectionLoadTask loader;
	
	private void load() {
		App.log("Selection::load()");
		if (!allow_load) {
			App.log("not allowed load...");
			return;
		}
		loader = new SelectionLoadTask();
		loader.fplExecute(c_squadid, c_gameweek);
	}
	
	public static class TransferHistItem {
		public int player_id;
		public String name;
		public int team_id;
		public int gw_points;
		public int pic_code;
		public int gameweek;
	}
	
	ArrayList<TransferHistItem> transferHistIn;
	ArrayList<TransferHistItem> transferHistOut;
	
	// async load data
	public class SelectionLoadTask extends FPLAsyncTask<Integer, Void, Squad> {
		ProgressDialog dialog;
		
		protected Squad doInBackground(Integer... params) {
			// load squad from db
			Squad newSquad = squadUtil.loadSquad(params[0], params[1], true, !c_selection);
			if (newSquad == null) {
				return null;
			}
			// arrange squad into grid
			SquadUtil.arrangeSquad(newSquad.players, playerGrid);
			// load transfers
			Cursor curTran = App.dbGen.rawQuery("SELECT DISTINCT p._id, t.t_in, p.name, p.team_id"
					+ " , (SELECT SUM(pm.total) points FROM player_match pm WHERE pm.season = " + App.season
					+ "    AND pm.gameweek = " + params[1] + " AND pm.player_player_id = p._id) gw_points"
					+ " , p.code_14"
					+ " FROM transfer t, player_season ps, player p"
					+ " WHERE t.squad_id = " + params[0] + " AND t.gameweek = " + params[1]
					+ " AND ps.season = " + App.season + " AND ps.fpl_id = t.player_fpl_id"
					+ " AND p._id = ps.player_id"
					+ " ORDER BY ps.position ASC", null);
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
				
				int t_in = curTran.getInt(1);
				if (t_in == 1) {
					transferHistIn.add(tran);
				} else {
					transferHistOut.add(tran);
				}
				curTran.moveToNext();
			}
			curTran.close();
			
			return newSquad;
        }
		protected void onPreExecute() {
			dialog = new ProgressDialog(Selection.this);
			dialog.setMessage("Loading..");
			dialog.setCancelable(false);
			dialog.show();
			Selection.this.progress = dialog;
		}
        protected void onPostExecute(Squad result) {
        	// mark captain as initially selected
        	squad = result;
        	if (squad != null) {
	        	for (int i = 0; i < squad.players.length; i++) {
					if (squad.players[i].captain) {
						selected_player = i;
					}
				}
        	}
        	populateGrid(true);
        	
        	LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        
        	transfersList.removeAllViews();
        	if ( (transferHistIn != null) && (transferHistOut != null) ) {
	        	if ( (transferHistIn.size() == transferHistOut.size()) && (transferHistIn.size() > 0) ) {
	        		for (int i = 0; i < transferHistIn.size(); i++) {
	        			TransferHistItem in  = transferHistIn.get(i);
	        			TransferHistItem out = transferHistOut.get(i);
	        			
	        			RelativeLayout rl = (RelativeLayout) inflater.inflate(R.layout.squad_screen_transfer_item, null);
	        			TextView name_out = (TextView)  rl.findViewById(R.id.playernameout);
	        			TextView name_in  = (TextView)  rl.findViewById(R.id.playernamein);
	        			ImageView pic_out = (ImageView) rl.findViewById(R.id.playerpicout);
	        			ImageView pic_in  = (ImageView) rl.findViewById(R.id.playerpicin);
	        			TextView points_out = (TextView)  rl.findViewById(R.id.pointsout);
	        			TextView points_in  = (TextView)  rl.findViewById(R.id.pointsin);
	        			
	        			name_out.setText(out.name);
	        			name_in.setText(in.name);
	        			pic_out.setImageResource(Players.getPlayerPic(Selection.this, out.pic_code));
	        			pic_in.setImageResource(Players.getPlayerPic(Selection.this, in.pic_code));
	        			
	        			if (!c_selection) {
	        				points_out.setText(String.valueOf(out.gw_points));
	        				points_in.setText(String.valueOf(in.gw_points));
	        			}
	        			
	        			if ((i % 2) == 0) {
	        				rl.setBackgroundColor(COLOUR1);
	        			} else {
	        				rl.setBackgroundColor(COLOUR2);
	        			}
	        			
	        			transfersList.addView(rl);
	        		}
	        	}
        	}
        	
        	if (!c_selection) {
	        	playersList.removeAllViews();
	        	q = 0;
	        	SquadPlayer totals = new SquadPlayer();
	        	int last_sel = SquadUtil.SEL_SELECTED;
	        	// iterate through players in selection/position order using the grid
	        	for (SquadPlayer[] row : playerGrid) {
	        		for (SquadPlayer p : row) {
	        			if (p != null) {
		        			if ( (last_sel == SquadUtil.SEL_SELECTED) && (p.selected != SquadUtil.SEL_SELECTED) ) {
		        				popStatRecord(inflater, totals, null, false);
		        				totals = new SquadPlayer();
		        			}
	        				popStatRecord(inflater, p, totals, false);
	        				last_sel = p.selected;
	        			}
	        		}
		        }
	        	popStatRecord(inflater, totals, null, false);
	        	
	        	if (squad != null && squad.gameweek_chip != 0) {
	        		chip_header.setVisibility(View.VISIBLE);
	        		chip_header.setText(ScrapeSquadGameweek.chip_strings[squad.gameweek_chip]);
	        	}
        	}
        	
        	if (c_selection) {
	        	String notes_raw = Settings.getStringPref(Settings.PREF_NOTES);
				if (notes_raw != null) {
					try {
						String notes_decoded =  URLDecoder.decode(notes_raw, "utf-8");
						notes.setText(notes_decoded);
					} catch (UnsupportedEncodingException e) {
						App.exception(e);
						App.log("failed decoding notes");
					}
				}
        	}
        	
        	dialog.dismiss();
        	loader = null;
        }
    }
	
	private int q;
	
	OnClickListener playerStatsListener = new View.OnClickListener() {
		public void onClick(View v) {
			SquadPlayer p = (SquadPlayer) v.getTag();
			Intent intent = new Intent(Selection.this, Player.class);
			intent.putExtra(Player.P_PLAYERID, (int) p.player_id);
	    	startActivity(intent);
		}
	};
	
	private static int getBG(boolean home, boolean dgw, int row) {
		if ((row % 2) == 0) {
			if (home) {
				return 0xdd004422;
			} else if (dgw) {
				return 0xddccccaa;
			} else {
				return 0xdd220044;
			}
		} else {
			if (home) {
				return 0x99004422;
			} else if (dgw) {
				return 0x99ccccaa;
			} else {
				return 0x99220044;
			}
		}
	}
	
	private static String val_string(int val) {
		if (val != 0) {
			return String.valueOf(val);
		} else {
			return "";
		}
	}
	
	private void popStatRecord(LayoutInflater inflater, SquadPlayer p, SquadPlayer totals, boolean match) {
		LinearLayout ll = (LinearLayout) inflater.inflate(R.layout.fixture_screen_player_item, null);
		ImageView playerpic = (ImageView) ll.findViewById(R.id.playerpic);
		TextView name = (TextView)  ll.findViewById(R.id.name);
		TextView points = (TextView)  ll.findViewById(R.id.points);
		TextView minutes  = (TextView)  ll.findViewById(R.id.minutes);
		TextView goals  = (TextView)  ll.findViewById(R.id.goals);
		TextView assists  = (TextView)  ll.findViewById(R.id.assists);
		TextView bonus  = (TextView)  ll.findViewById(R.id.bonus);
		TextView saves  = (TextView)  ll.findViewById(R.id.saves);
		TextView conc  = (TextView)  ll.findViewById(R.id.conc);
		TextView psav  = (TextView)  ll.findViewById(R.id.psav);
		TextView pmiss  = (TextView)  ll.findViewById(R.id.pmiss);
		TextView og  = (TextView)  ll.findViewById(R.id.og);
		TextView bps  = (TextView)  ll.findViewById(R.id.bps);
		
		boolean totalrow = (totals != null);
		ll.setBackgroundColor(getBG(totalrow, match, q++));
		
		name.setText(p.name);
		if (p.gw_red == 1) {
			name.setTextColor(0xffff0000);
		} else if (p.gw_yellow == 1) {
			name.setTextColor(0xffffff00);
		} else {
			name.setTextColor(0xffffffff);
		}
		
		points.setText(String.valueOf(p.gw_score));
		minutes.setText(val_string(p.gw_minutes));
		goals.setText(val_string(p.gw_goals));
		assists.setText(val_string(p.gw_assists));
		bonus.setText(val_string(p.gw_bonus));
		saves.setText(val_string(p.gw_saves));
		conc.setText(val_string(p.gw_conceded));
		psav.setText(val_string(p.gw_pen_save));
		pmiss.setText(val_string(p.gw_pen_miss));
		og.setText(val_string(p.gw_own_goals));
		bps.setText(val_string(p.gw_bps));
		
		if (totals != null) {
			totals.gw_score += p.gw_score;
			totals.gw_minutes +=p.gw_minutes;
			totals.gw_goals += p.gw_goals;
			totals.gw_assists += p.gw_assists;
			totals.gw_bonus += p.gw_bonus;
			totals.gw_saves += p.gw_saves;
			totals.gw_conceded += p.gw_conceded;
			totals.gw_pen_save += p.gw_pen_save;
			totals.gw_pen_miss += p.gw_pen_miss;
			totals.gw_own_goals += p.gw_own_goals;
			totals.gw_bps += p.gw_bps;
		}
		
		if ( (totals != null) || match ) {
			int status = 0;
			if (p.status_gw_complete && !p.status_played) {
				status = R.drawable.icon_x;
			} else if (p.status_gw_got_all_bonus) {
				status = R.drawable.icon_bonus;
			} else if (p.status_gw_complete) {
				status = R.drawable.icon_tick;
			} else if (p.status_playing_now) {
				status = R.drawable.icon_playing;
			}
			if (status > 0) {
				playerpic.setImageResource(status);
			}
			
			if (p.captain) {
				name.setTypeface(null, Typeface.BOLD); 
			}
		}
		
		if (totals != null) {
			ll.setTag(p);
			ll.setOnClickListener(playerStatsListener);
			ll.setClickable(true);
		}
		playersList.addView(ll);
		
		if (p.dgw_matches != null) {
			for (SquadPlayer dgw_match : p.dgw_matches) {
				popStatRecord(inflater, dgw_match, null, true);
			}
		}
	}
	
	public static final int COLOUR1 = 0xff222222;
	public static final int COLOUR2 = 0xff111111;
	
	// init link to grid cell
	private void setCell(int x, int y, int id) {
		RelativeLayout rl = (RelativeLayout) tab_grid.findViewById(id);
		pCell[x][y] = (TextView) rl.findViewById(R.id.playertextview);
		pCellTicker[x][y] = (Ticker) rl.findViewById(R.id.ticker);
		pCell[x][y].setTag(new MyTag(x, y));
	}

	// tag lets cell know where it is
	private class MyTag {
		public MyTag(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public int x;
		public int y;
	}

	// listener for sub button
	public void clickSub(View v) {
		if (subMode) {
			subMode = false;
			populateGrid(false);
			return;
		}
		
		SquadPlayer p = squad.players[selected_player];
		
		// special case for keeper - just do the sub
		if (p.position == SquadUtil.POS_KEEP) {
			for (int i = 0; i < SquadUtil.SQUAD_SIZE; i++) {
				if (squad.players[i].position == SquadUtil.POS_KEEP) {
					if (squad.players[i].selected == SquadUtil.SEL_SELECTED) {
						squad.players[i].selected = SquadUtil.SEL_GK_NOT_SELECTED;
					} else {
						squad.players[i].selected = SquadUtil.SEL_SELECTED;
						selected_player = i;
					}
				}
			}
			SquadUtil.arrangeSquad(squad.players, playerGrid);
			populateGrid(true);
			return;
		}

		// players in each position; used to determine which subs are permissable
		int[] sel = { 0, 0, 0, 0, 0 };
		boolean[] sub = { false, false, false, false, false };

		// init all players to not selectable for change
		for (int i = 0; i < SquadUtil.SQUAD_SIZE; i++) {
			if (squad.players[i].selected == SquadUtil.SEL_SELECTED) {
				sel[squad.players[i].position]++;
			}
		}
		
		// the only sub allowed
		boolean only = false;
		
		// if clicked player is in team already, which subs can come in?
		if (p.selected == SquadUtil.SEL_SELECTED) {
			sel[p.position]--;
			for (int i = SquadUtil.POS_DEF; i <= SquadUtil.POS_FWD; i++) {
				if (sel[i] < SquadUtil.MIN_SEL_DEFAULT[i]) {
					sub[i] = true;
					only = true;
				}
			}
			if (!only) {
				for (int i = SquadUtil.POS_DEF; i <= SquadUtil.POS_FWD; i++) {
					if (sel[i] < SquadUtil.MAX_SEL[i]) {
						sub[i] = true;
					}
				}
			}
		// clicked player is sub, who can he come in for?
		} else {
			sel[p.position]++;
			for (int i = SquadUtil.POS_DEF; i <= SquadUtil.POS_FWD; i++) {
				if (sel[i] > SquadUtil.MAX_SEL[i]) {
					sub[i] = true;
					only = true;
				}
			}
			if (!only) {
				for (int i = SquadUtil.POS_DEF; i <= SquadUtil.POS_FWD; i++) {
					if (sel[i] > SquadUtil.MIN_SEL_DEFAULT[i]) {
						sub[i] = true;
					}
				}
			}
		}
		
		// set selection flags for players
		for (int i = 0; i < SquadUtil.SQUAD_SIZE; i++) {
			SquadPlayer otherP = squad.players[i];
			if ((p.selected != otherP.selected) && sub[otherP.position]) {
				otherP.canSelect = true;
			} else {
				otherP.canSelect = false;
			}
			// special case - outfield sub can be swapped for any other outfield sub
			// (ie sub order change)
			if (p.selected > SquadUtil.SEL_SELECTED) {
				// player is not this player and player is an outfield sub
				if ( (otherP.fpl_id != p.fpl_id) && (otherP.selected > (SquadUtil.SEL_SELECTED + 1)) ) {
					otherP.canSelect = true;
				}
			}
		}
		
		if (tipsActive) Toast.makeText(this, "Tip: click green player to sub, or 'Sub' again to cancel", Toast.LENGTH_LONG).show();
		
		// refresh gui with green/red markings
		subMode = true;
		subId = selected_player;
		populateGrid(false);
	}
	
	public void clickTotalScore(View v) {
		Intent intent = new Intent(v.getContext(), FPLSquad.class);
		intent.putExtra(FPLSquad.P_SQUADID, (int) c_squadid);
    	startActivity(intent);
	}
	
	// listener: set captain
	public void clickCapt(View v) {
		if (squad.players[selected_player].selected != SquadUtil.SEL_SELECTED){
			return;
		}
		if (squad.players[selected_player].vice_captain) {
			return;
		}
		
		for (int i = 0; i < SquadUtil.SQUAD_SIZE; i++) {
			squad.players[i].captain = false;
		}
		squad.players[selected_player].captain = true;
		
		vcButton.setTextColor(DISABLED_COLOUR);
		vcButton.setEnabled(false);

		populateGrid(true);
	}
	// listener: set vice captain
	public void clickVC(View v) {
		if (squad.players[selected_player].selected != SquadUtil.SEL_SELECTED){
			return;
		}
		if (squad.players[selected_player].captain) {
			return;
		}
		
		for (int i = 0; i < SquadUtil.SQUAD_SIZE; i++) {
			squad.players[i].vice_captain = false;
		}
		squad.players[selected_player].vice_captain = true;
		
		captButton.setTextColor(DISABLED_COLOUR);
		captButton.setEnabled(false);
		
		populateGrid(true);
	}
	
	public boolean confirming = false;
	
	// listener: confirm changes
	public void clickConfirm(View v) {
		if (squad.bank < 0) {
			FPLActivity.generic_dialog_title = "Over Budget";
			FPLActivity.generic_dialog_text  = "You are currently over budget, so this selection cannot be confirmed";
			showDialog(FPLActivity.DIALOG_GENERIC);
		} else {
			confirming = true;
			new TransferCheckSquad(false).fplExecute(squad);
		}
	}
	
	public com.pennas.fpl.scrape.DoTransfersNew.TransferConfirm transfers;
	
	// dialog shown when long-click on stat: choose stat for GUI
    protected Dialog onCreateDialog(int dialog_id) {
        Dialog new_dialog;
        switch(dialog_id) {
	        case RESPONSE_DIALOG:
	        	if (transfers != null) {
		        	// build dialogue from array
		        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
		        	builder.setTitle("Confirm Transfers");
		        	builder.setCancelable(false);
		        	builder.setMessage(transfers.fplMessage);
		            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		                public void onClick(DialogInterface dialog, int id) {
		                	new TransferConfirmTask().fplExecute(transfers);
		                }
		            });
		            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
		                public void onClick(DialogInterface dialog, int id) {
		                	Toast.makeText(Selection.this, "Transfer(s) abandoned", Toast.LENGTH_LONG).show();
		                }
		            });
		        	new_dialog = builder.create();
		        	return new_dialog;
	        	} else {
	        		App.log("Selection RESPONSE_DIALOG: transfers null");
	        		Toast.makeText(Selection.this, "Error displaying potential transfers", Toast.LENGTH_LONG).show();
	        		return null;
	        	}
	        default:
	        	return super.onCreateDialog(dialog_id);
        }
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	switch(id){
    		case(RESPONSE_DIALOG):
    			//App.log(transfers.fplMessage);
    			AlertDialog a = (AlertDialog) dialog;
    			a.setMessage(transfers.fplMessage);
    			break;
    		default:
    			super.onPrepareDialog(id, dialog);
    	}
    }
	
	public static final int TRANSFER = 1;
	public static int transferPlayerID;
	
	// listener: transfer
	public void clickTransfer(View v) {
		Intent intent = new Intent(this, Players.class);
    	intent.putExtra(Players.P_TRANSFER, true);
    	int position = squad.players[selected_player].position;
    	intent.putExtra(Players.P_POSITION, position);
    	float money = squad.bank + squad.players[selected_player].sell_price;
    	intent.putExtra(Players.P_PRICE, money);
    	// build "where in" string for current squad
	    StringBuffer squad_string = new StringBuffer();
	    boolean first = true;
 		for (int i=0; i<squad.players.length; i++) {
 			if (first) {
 				squad_string.append(squad.players[i].fpl_id);
 				first = false;
 			} else {
 				squad_string.append(", " + squad.players[i].fpl_id);
 			}
 		}
 		intent.putExtra(Players.P_SQUAD_STRING, squad_string.toString());
 		intent.putExtra(Players.P_PLAYER_OUT, squad.players[selected_player].name);
 		// check if any teams cannot have any more players in this squad
 		int[] teamcount = new int[NUM_TEAMS+1];
 		for (int i=1; i<=NUM_TEAMS; i++) {
 			teamcount[i] = 0;
 		}
 		StringBuffer teams_string = new StringBuffer();
 		first = true;
 		for (int i=0; i<squad.players.length; i++) {
 			// ignore outgoing player - can be replaced by someone in same team
 			if (i != selected_player) {
 				// increment team count
	 			SquadPlayer p = squad.players[i];
	 			
 				teamcount[p.team_id]++;
 				if (teamcount[p.team_id] >= MAX_PLAYERS_FROM_TEAM) {
		 			// build string if over limit
	 				if (first) {
		 				teams_string.append(p.team_id);
		 				first = false;
		 			} else {
		 				teams_string.append(", " + p.team_id);
		 			}
	 			}
 			}
 		}
 		if (!first) intent.putExtra(Players.P_TEAMS_STRING, teams_string.toString());
 		startActivityForResult(intent, TRANSFER);
	}
	
	@Override 
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {     
		super.onActivityResult(requestCode, resultCode, data); 
		switch(requestCode) { 
	    	case (TRANSFER) : { 
	    		if (resultCode == Activity.RESULT_OK) { 
	    			if (squad != null) {
	    				if (squad.players != null) {
			    			SquadPlayer oldp = squad.players[selected_player];
			    			
			    			// get info on new player
			    			Cursor curPrice = App.dbGen.rawQuery("SELECT price, fpl_id FROM player_season WHERE season = " + season + " AND player_id = " + transferPlayerID, null);
			    			curPrice.moveToFirst();
			    			float price = curPrice.getFloat(0) / 10f;
			    			int fpl_id = curPrice.getInt(1);
			    			curPrice.close();
			    			
			    			// create new player entry
			    			int oldCapt = 0;
			    			if (oldp.captain) {
			    				oldCapt = SquadUtil.CAPTAIN;
			    			} else if (oldp.vice_captain) {
			    				oldCapt = SquadUtil.VICE_CAPTAIN;
			    			}
			    			SquadPlayer newp = squadUtil.loadPlayer(c_gameweek, oldp.selected, oldCapt, fpl_id, price, oldp.i, true);
			    			squad.players[selected_player] = newp;
			    			
			    			// cascade initial selling price if doing multiple replacements in same position before transfer confirmation
			    			newp.outgoing_player_sell_price = oldp.outgoing_player_sell_price;
			    			newp.transferred_in = true;
			    			
			    			// adjust bank
			    			squad.bank += oldp.sell_price;
			    			squad.bank -= newp.price;
			    			
			    			// adjust values
			    			squad.value -= oldp.price;
			    			squad.value += newp.price;
			    			squad.sell_value -= oldp.sell_price;
			    			squad.sell_value += newp.price;
			    			
			    			// redraw
			    			SquadUtil.arrangeSquad(squad.players, playerGrid);
			    			populateGrid(true);
	    				}
	    			}
	    		} 
	    		break; 
	    	} 
		} 
	}
	
	// listener for player click - load details of player
	public void clickPlayer(View v) {
		MyTag tag = (MyTag) v.getTag();
		if (tag != null) {
			// clicked player
			SquadPlayer p = playerGrid[tag.x][tag.y];
			
			if (p != null) {
				// process substitution if active
				if (subMode && p.canSelect) {
					SquadPlayer in;
					SquadPlayer out;
					
					if (p.selected == SquadUtil.SEL_SELECTED) {
						in = squad.players[subId];
						out = p;
					} else {
						out = squad.players[subId];
						in = p;
					}
					
					int oldout = out.selected;
					out.selected = in.selected;
					in.selected = oldout;
	
					selected_player = in.i;
					
					if (out.captain) {
						out.captain = false;
						in.captain = true;
					} else if (out.vice_captain) {
						out.vice_captain = false;
						in.vice_captain = true;
					}
	
					SquadUtil.arrangeSquad(squad.players, playerGrid);
					subMode = false;
					populateGrid(true);
				// not sub mode
				} else {
					int old = selected_player;
					selected_player = p.i;
					if (old == selected_player) {
						// second click on player
						// load player screen for player "id"
						Intent intent = new Intent(this, Player.class);
						intent.putExtra(Player.P_PLAYERID, (int) squad.players[selected_player].player_id);
				    	startActivity(intent);
					}
					subMode = false;
					populateGrid(false);
				} // if submode / else
			} // p != null

		} // if player in cell
		
		if (squad != null) {
			SquadPlayer p = squad.players[selected_player];
			if (p != null) {
				if (p.selected == SquadUtil.SEL_SELECTED) {
					if (p.vice_captain) {
						captButton.setTextColor(DISABLED_COLOUR);
						captButton.setEnabled(false);
					} else {
						captButton.setTextColor(ENABLED_COLOUR);
						captButton.setEnabled(true);
					}
					if (p.captain) {
						vcButton.setTextColor(DISABLED_COLOUR);
						vcButton.setEnabled(false);
					} else {
						vcButton.setTextColor(ENABLED_COLOUR);
						vcButton.setEnabled(true);
					}
				} else {
					captButton.setTextColor(DISABLED_COLOUR);
					vcButton.setTextColor(DISABLED_COLOUR);
					captButton.setEnabled(false);
					vcButton.setEnabled(false);
				}
			} else {
				captButton.setTextColor(DISABLED_COLOUR);
				vcButton.setTextColor(DISABLED_COLOUR);
				captButton.setEnabled(false);
				vcButton.setEnabled(false);
			}
		}
		
		subMode = false;
	}
	
	private static final int DISABLED_COLOUR = 0xcc000000;
	private static final int ENABLED_COLOUR = 0xffffffff;
	
	private Drawable selection_gradient;
	private Drawable selection_gradient_sub;
	private Drawable selection_gradient_red;
	
	private void set_screenshot_view() {
		if (squad == null) {
			setScreenshotView(null, null);
		} else {
			if (viewPager.getCurrentItem() == TAB_NOTES_STATS) {
				if (c_selection) {
					setScreenshotView(null, null);
				} else {
					setScreenshotView(playersList, "Gameweek " + c_gameweek + " stats for " + squad.team_name + ": " + squad.c_gameweek_score + " Points");
				}
			} else if (viewPager.getCurrentItem() == TAB_PLAYERS) {
				if (c_selection) {
					setScreenshotView(grid_scrollview_content, "Gameweek " + c_gameweek + " selection for " + squad.team_name);
				} else {
					setScreenshotView(grid_scrollview_content, "Gameweek " + c_gameweek + " points for " + squad.team_name + ": " + squad.c_gameweek_score + " Points");
				}
			} else if (viewPager.getCurrentItem() == TAB_TRANSFERS) {
				setScreenshotView(transfersList, "Gameweek " + c_gameweek + " transfers for " + squad.team_name);
			}
		}
	}
	
	// draw data onto grid
	public void populateGrid(boolean players_changed) {
		if (squad == null) {
			// wipe all fields
			squadBank.setText("");
			squadSellValue.setText("");
			squadValue.setText("");
			totalScore.setText("");
			set_screenshot_view();
			
			for (int i = 0; i < 5; i++) {
				for (int j = 0; j < 5; j++) {
					if (pCell[i][j] != null) {
						pCell[i][j].setBackgroundDrawable(null);
						pCell[i][j].setText("");
						pCell[i][j].setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
					}
					if (pCellTicker[i][j] != null) {
						pCellTicker[i][j].setData(null, 0, 0);
					}
				}
			}
			
			confirmButton.setEnabled(false);
			transferButton.setEnabled(false);
			vcButton.setEnabled(false);
			subButton.setEnabled(false);
			captButton.setEnabled(false);
			
			App.log("populateGrid(): squad null");
			if (c_selection) {
				nullMsg.setText("Please run 'Players' sync to retrieve selection for next gameweek");
			} else {
				Cursor c = App.dbGen.rawQuery("SELECT rival FROM squad WHERE _id = " + c_squadid, null);
				c.moveToFirst();
				boolean rival = false;
				if (!c.isAfterLast()) {
					if (c.getInt(0) == 1) {
						rival = true;
					}
				}
				c.close();
				if (rival) {
					nullMsg.setText("Please run 'Leagues' sync to retrieve selection/points for this gameweek");
				} else {
					nullMsg.setText("Please mark as a rival to retrieve selection/points for this gameweek");
				}
			}
			
			return;
		}
		nullMsg.setText("");
		
		Resources res = getResources();
		if (selection_gradient == null) {
			selection_gradient = res.getDrawable(R.drawable.selection_gradient);
		}
		if (selection_gradient_sub == null) {
			selection_gradient_sub = res.getDrawable(R.drawable.selection_gradient_sub);
		}
		if (selection_gradient_red == null) {
			selection_gradient_red = res.getDrawable(R.drawable.selection_gradient_red);
		}
		//Debug.startMethodTracing("populategrid");
		long startTime = System.currentTimeMillis();
		
		App.log("populateGrid()");
		
		String gwScoreString = String.valueOf(squad.c_gameweek_score);
		if (squad.gw_hits > 0) {
			gwScoreString += " (-" + squad.gw_hits + ")";
		}
		if (c_selection) {
			actionBar.setTitle(squad.team_name);
			actionBar.setSubtitle("GW " + c_gameweek + " Selection");
		} else {
			actionBar.setTitle(squad.team_name);
			actionBar.setSubtitle("GW " + c_gameweek + ": " + gwScoreString + " Points");
			gwScore.setText("GW Pts: " + gwScoreString);
		}
		
		set_screenshot_view();
		
		if (players_changed) {
			squadBank.setText("£" + ProcessData.trunc(squad.bank, 1) + "M");
			if (c_selection) {
				squadSellValue.setText("£" + ProcessData.trunc(squad.sell_value, 1) + "M");
			}
			squadValue.setText("£" + ProcessData.trunc(squad.value, 1) + "M");
			if (c_selection) {
				totalScore.setText("Total Pts " + squad.c_total_points);
			} else {
				totalScore.setText("Total Pts " + squad.total_points);
			}
			transfersField.setText(String.valueOf(squad.gw_transfers));
		}
		
		if (subMode) {
			subButton.setTextColor(0xff7cfc00);
		} else {
			subButton.setTextColor(0xffffffff);
		}
		
		if (gUtil == null) gUtil = new GraphicUtil(this);
		
		SquadPlayer p;
		float sel_stat = 0;
		float sub_stat = 0;
		String stat_field = null;
		if (c_selection) stat_field = stat_fields.get(stat);
		
		int count_players_in_squad = 0;
		
		// iterate 5x5 grid
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < 5; j++) {
				// is there a player in this cell?
				p = playerGrid[i][j];
				if (p != null) {
					count_players_in_squad++;
					
					// set background for selected/sub indicator
					if (p.i == selected_player) {
						pCell[i][j].setBackgroundDrawable(selection_gradient);
					} else {
						if (subMode) {
							if (p.canSelect) {
								pCell[i][j].setBackgroundDrawable(selection_gradient_sub);
							} else {
								pCell[i][j].setBackgroundDrawable(selection_gradient_red);
							}
						} else {
							pCell[i][j].setBackgroundDrawable(null);
						}
					}
					
					if (c_selection) {
						// set ticker
						float rating_high = 0;
						float rating_low = 0;
						switch (traffic) {
							case TRAFFIC_PREDSCORE:
								rating_low = Players.RATING_LOW_PRED;
								rating_high = Players.RATING_HIGH_PRED;
								break;
							case TRAFFIC_FIXDIFF:
								rating_low = Players.RATING_LOW_FIX;
								rating_high = Players.RATING_HIGH_FIX;
								break;
							case TRAFFIC_PREDCS:
								rating_low = Teams.CS_LOW;
								rating_high = Teams.CS_HIGH;
								break;
				    	}
						boolean set_ticker = false;
						if ( (traffic != TRAFFIC_NONE) && (p.ticker_string != null) ) {
			        		if (p.ticker_string.length() > 0) {
			        			TickerData data = Ticker.parseTickerData(p.ticker_string, rating_high, rating_low, (traffic==TRAFFIC_PREDCS), traffic, false);
			        			pCellTicker[i][j].setData(data, 4, App.next_gameweek);
			        			set_ticker = true;
			        		}
			        	}
						if (!set_ticker) pCellTicker[i][j].setData(null, 0, 0);
					}
					
					if (players_changed) {
						// opposition label
						if (p.opp != null) {
							pCell[i][j].setText(p.opp);
						} else {
							pCell[i][j].setText("");
						}
						
						float stat_val = 0;
						boolean dis_stat = false;
						// no stat display
						if (c_selection) {
							if (stat_field == STAT_NONE) {
							// known values
							} else if (stat_field == STAT_SELL_PRICE) {
								p.display_stat = "£" + p.sell_price + "M";
								stat_val = p.sell_price;
								dis_stat = true;
							} else if (stat_field == STAT_PRICE) {
								p.display_stat = "£" + p.price + "M";
								stat_val = p.price;
								dis_stat = true;
							} else if (stat_field == STAT_PROFIT) {
								p.display_stat = "£" + ProcessData.trunc(p.sell_price - p.bought_price, 1) + "M";
								stat_val = ProcessData.trunc(p.sell_price - p.bought_price, 1);
								dis_stat = true;
							} else if (stat_field == STAT_BOUGHT_PRICE) {
								p.display_stat = "£" + p.bought_price + "M";
								stat_val = p.bought_price;
								dis_stat = true;
							// unknown values... load
							} else {
								if (p.stat2value != null) {
									p.display_stat = p.stat2value.get(stat_field);
									if (p.display_stat != null) {
										stat_val = Float.parseFloat(p.display_stat);
										dis_stat = true;
									}
								}
							}
							if (!dis_stat) p.display_stat = "";
						}
						
						if (p.selected == SquadUtil.SEL_SELECTED) {
							sel_stat += stat_val;
						} else {
							sub_stat += stat_val;
						}
	
						// draw shirt, and add indicators + reflection
						int resource = SquadUtil.getShirtResource(p.team_id);
						if (resource > 0) {
							Bitmap b = gUtil.getReflection(resource, p, c_selection, squad.gameweek_chip == ScrapeSquadGameweek.c_chip_triple_captain);
							Drawable d = new BitmapDrawable(getResources(), b);
							pCell[i][j].setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
						} else {
							App.log("0 drawable resource for shirt in populateGrid()");
							pCell[i][j].setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
						}
						
						pCell[i][j].setVisibility(View.VISIBLE);
					}
				// no player - set visibility of cell to false
				} else {
					if (players_changed) {
						if (pCell[i][j] != null) {
							pCell[i][j].setVisibility(View.INVISIBLE);
						}
						if (pCellTicker[i][j] != null) {
							pCellTicker[i][j].setData(null, 0, 0);
						}
					}
				}
			}
		}
		
		if (count_players_in_squad != SquadUtil.SQUAD_SIZE) {
			confirmButton.setEnabled(false);
			transferButton.setEnabled(false);
			vcButton.setEnabled(false);
			subButton.setEnabled(false);
			captButton.setEnabled(false);
		} else {
			confirmButton.setEnabled(true);
			transferButton.setEnabled(true);
			vcButton.setEnabled(true);
			subButton.setEnabled(true);
			captButton.setEnabled(true);
		}
		
		if (players_changed) {
			sel_stat = ProcessData.trunc(sel_stat, 1);
			sub_stat = ProcessData.trunc(sub_stat, 1);
			float tot_stat = ProcessData.trunc(sel_stat + sub_stat, 1);
			if (stat_field != STAT_NONE) {
				statTotals.setText(sel_stat + " / " + sub_stat + " / " + tot_stat);
			} else {
				statTotals.setText("");
			}
		}
		
		//Debug.stopMethodTracing();
		Long timeDiff = System.currentTimeMillis() - startTime;
		float timeSecs = (float)timeDiff / 1000;
		App.log("done populateGrid() in " + timeSecs + " seconds");
	}
	
	// sub mode disable on resume
	protected void onResume() {
    	super.onResume();
    	subMode = false;
	}

	// callback
	public void dataChanged() {
		// if any transfers are in progress, then don't refresh
		if (confirming) {
			return;
		}
		if (loader != null) {
			loader.cancel(true);
    	}
    	if (progress != null) {
    		progress.dismiss();
    	}
    	load();
	}
	
	public ProgressDialog progress;
	
	protected void onDestroy() {
		currentSelection = null;
		if (loader != null) {
			loader.cancel(true);
    	}
    	if (progress != null) {
    		progress.dismiss();
    	}
		super.onDestroy();
	}
	
	public void clickFPL() {
    	Uri uri = Uri.parse( "http://fantasy.premierleague.com/entry/" + c_squadid + "/event-history/" + c_gameweek + "/");
		startActivity( new Intent( Intent.ACTION_VIEW, uri ) );
    }
    
	public void clickGWPoints(View v) {
		Intent intent = new Intent(v.getContext(), Hot.class);
		intent.putExtra(Hot.P_GAMEWEEK, (int) c_gameweek);
		intent.putExtra(Hot.P_SHOWRIVALS, true);
    	startActivity(intent);
    }
    
	public static String notes_for_upload;
	
    public void clickNotes(View v) {
		notes_for_upload = notes.getText().toString();
		App.initProc(true, FPLService.UPDATE_NOTES);
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        return this;
    }

}