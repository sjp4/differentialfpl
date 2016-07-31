package com.pennas.fpl;

import com.pennas.fpl.util.FPLActivity;
import com.pennas.fpl.util.Refreshable;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class Fixture extends FPLActivity implements Refreshable {
	
	private int fixture_id;
	private Cursor curPl;
	private int home_team_id;
	private ListView list;
	private int fix_season = 0;
	
	public static final String P_FIXTURE_ID = "com.pennas.fpl.fixture_id";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    setContentView(R.layout.fixture_screen);
	    
	    actionBar.setTitle("Fixture");
	    
	    // get ID of player
	    Bundle extras = getIntent().getExtras();
	    if(extras ==null ) return;
	    fixture_id = extras.getInt(P_FIXTURE_ID);
	    
	    popData();
	}
	
	// load data and populate listview
	private void popData() {
		App.log("popData()  (Fixture)");
		
		Cursor cur = App.dbGen.rawQuery("SELECT _id, gameweek, team_home_id, team_away_id, res_goals_home, res_goals_away, stats_datetime, got_bonus, season, datetime FROM fixture WHERE _id = " + fixture_id, null);
		cur.moveToFirst();
		TextView score = (TextView) findViewById(R.id.score);
		int team_id_a = cur.getInt(2);
		int team_id_b = cur.getInt(3);
		home_team_id = team_id_a;
		String team_a = App.id2team.get(team_id_a);
		String team_b = App.id2team.get(team_id_b);
		actionBar.setTitle(team_a + " v " + team_b);
		
		String res_goals_home_str = cur.getString(4);
		
		list = (ListView) findViewById(R.id.list);
		int got_bonus = cur.getInt(7);
		boolean played = false;
		long datetime = cur.getLong(9);
		actionBar.setSubtitle(App.printDate(App.ukToLocal(datetime)));
		long now = App.currentUkTime();
		if (datetime <= now) {
			if (got_bonus >= 1) {
				score.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_tick, 0);
				played = true;
			} else if (res_goals_home_str != null) {
				score.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_playing, 0);
				played = true;
			}
		} else {
			score.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		}
		
		if (played) score.setText(cur.getInt(4) + " - " + cur.getInt(5));
		fix_season = cur.getInt(8);
		
		cur.close();
		
		if (curPl != null) {
			curPl.close();
		}
		String sort_dir;
		if (team_id_a < team_id_b) sort_dir = "ASC";
		else sort_dir = "DESC";
		// player match details
		curPl = App.dbGen.rawQuery("SELECT p._id _id, pm.minutes, pm.total, ps.position, p.name, p.code_14, pm.pl_team_id"
				+ ", pm.goals, pm.assists, pm.bonus, pm.saves, pm.conceded, pm.pen_sav, pm.pen_miss"
				+ ", pm.own_goals, pm.yellow, pm.red, ps.fpl_id, pm.bps"
				+ " FROM player_match pm, player p, player_season ps"
				+ " WHERE pm.fixture_id = " + fixture_id + " AND pm.minutes > 0 AND p._id = pm.player_player_id AND ps.player_id = pm.player_player_id AND ps.season = pm.season"
				+ " ORDER BY pm.pl_team_id " + sort_dir + ", ps.position ASC", null);
		PlayersCursorAdapter rAdapter = new PlayersCursorAdapter(this, curPl);
		list.setAdapter(rAdapter);
		OnItemClickListener listListener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
	            // load player screen for player "id"
				Intent intent = new Intent(view.getContext(), Player.class);
				intent.putExtra(Player.P_PLAYERID, (int) id);
				intent.putExtra(Player.P_SEASON, App.season);
		    	startActivity(intent);
			}
	    };
	    list.setOnItemClickListener(listListener);
	    
	    setScreenshotView(this.getWindow().getDecorView(), team_a + " v " + team_b + " (" + App.printDate(App.ukToLocal(datetime)) + ")");
	}
	
	// callback
    public void dataChanged() {
    	popData();
    }
    
    private static int getBG(boolean home, int row) {
		if ((row % 2) == 0) {
			if (home) {
				return 0xe2005533;
			} else {
				return 0xe2220055;
			}
		} else {
			if (home) {
				return 0x99005522;
			} else {
				return 0x99220044;
			}
		}
	}
    
    private class PlayersCursorAdapter extends CursorAdapter {
    	private int i_name, i_picture_code, i_minutes, i_total, i_pl_team_id, i_goals, i_assists, i_bonus;
    	private int i_saves, i_conceded, i_pen_sav, i_pen_miss, i_own_goals, i_yellow, i_red, i_fpl_id, i_bps;
    	private boolean loaded_indexes;
    	
    	public PlayersCursorAdapter(Context context, Cursor c) {
			super(context, c);
		}
    	
		private void loadIndexes(Cursor cursor) {
        	loaded_indexes = true;
        	i_name = cursor.getColumnIndex("name");
        	i_picture_code = cursor.getColumnIndex("code_14");
        	i_minutes = cursor.getColumnIndex("minutes");
        	i_total = cursor.getColumnIndex("total");
        	i_goals = cursor.getColumnIndex("goals");
        	i_assists = cursor.getColumnIndex("assists");
        	i_bonus = cursor.getColumnIndex("bonus");
        	i_pl_team_id = cursor.getColumnIndex("pl_team_id");
        	i_saves = cursor.getColumnIndex("saves");
        	i_conceded = cursor.getColumnIndex("conceded");
        	i_pen_sav = cursor.getColumnIndex("pen_sav");
        	i_pen_miss = cursor.getColumnIndex("pen_miss");
        	i_own_goals = cursor.getColumnIndex("own_goals");
        	i_yellow = cursor.getColumnIndex("yellow");
        	i_red = cursor.getColumnIndex("red");
        	i_fpl_id = cursor.getColumnIndex("fpl_id");
        	i_bps = cursor.getColumnIndex("bps");
        }
		
		private String val_string(Cursor cur, int index) {
			int val = cur.getInt(index);
			if (val != 0) {
				return String.valueOf(val);
			} else {
				return "";
			}
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// load column indexes if required
			if (!loaded_indexes) loadIndexes(cursor);
			int team_id = cursor.getInt(i_pl_team_id);
			boolean home_team = (home_team_id == team_id);
			TextView t;
			ImageView i;
			
			int fpl_id = cursor.getInt(i_fpl_id);
			boolean my_squad = false;
			if (fix_season == App.season) {
				if (App.mySquadCur.contains(fpl_id)) {
					my_squad = true;
				}
			}
			
			view.setBackgroundColor(getBG(home_team, cursor.getPosition()));
			
			int yellow = cursor.getInt(i_yellow);
			int red = cursor.getInt(i_red);
			
			t = (TextView) view.findViewById(R.id.name);
			t.setText(cursor.getString(i_name));
			if (red == 1) {
				t.setTextColor(0xffff0000);
			} else if (yellow == 1) {
				t.setTextColor(0xffffff00);
			} else {
				t.setTextColor(0xffffffff);
			}
			if (my_squad) {
				t.setTypeface(null, Typeface.BOLD);
			} else {
				t.setTypeface(null, Typeface.NORMAL);
			}
			
			t = (TextView) view.findViewById(R.id.minutes);
			t.setText(val_string(cursor, i_minutes));
			
			t = (TextView) view.findViewById(R.id.points);
			t.setText(String.valueOf(cursor.getInt(i_total)));
			
			t = (TextView) view.findViewById(R.id.goals);
			t.setText(val_string(cursor, i_goals));
			
			t = (TextView) view.findViewById(R.id.assists);
			t.setText(val_string(cursor, i_assists));
			
			t = (TextView) view.findViewById(R.id.bonus);
			t.setText(val_string(cursor, i_bonus));
			
			t = (TextView) view.findViewById(R.id.saves);
			t.setText(val_string(cursor, i_saves));
			
			t = (TextView) view.findViewById(R.id.conc);
			t.setText(val_string(cursor, i_conceded));
			
			t = (TextView) view.findViewById(R.id.psav);
			t.setText(val_string(cursor, i_pen_sav));
			
			t = (TextView) view.findViewById(R.id.pmiss);
			t.setText(val_string(cursor, i_pen_miss));
			
			t = (TextView) view.findViewById(R.id.og);
			t.setText(val_string(cursor, i_own_goals));
			
			t = (TextView) view.findViewById(R.id.bps);
			t.setText(val_string(cursor, i_bps));
			
			i = (ImageView) view.findViewById(R.id.playerpic);
			i.setImageResource(Players.getPlayerPic(context, cursor.getInt(i_picture_code)));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final View view = LayoutInflater.from(context).inflate(R.layout.fixture_screen_player_item, parent, false);
		    return view;
		}
    	
    }
    
    protected void onDestroy() {
	    if (curPl != null) {
			curPl.close();
		}
	    super.onDestroy();
    }
    
}
