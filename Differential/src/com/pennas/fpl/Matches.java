package com.pennas.fpl;

import com.pennas.fpl.util.FPLActivity;
import com.pennas.fpl.util.BitmapCache;
import com.pennas.fpl.util.FPLAsyncTask;
import com.pennas.fpl.util.Refreshable;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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

public class Matches extends FPLActivity implements Refreshable {
	
	private ListView matchesList;
	Cursor curMatches;
	
	private int season = App.season;
	private int gameweek = App.cur_gameweek;
	
	private BitmapCache bmp;
	
	private static final int FILTER_GAMEWEEK = 1;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    actionBar.setTitle("Matches");
	    
	    hasTips = true;
        tipsPref = Settings.PREF_TIPS_MATCHES;
		tips.add("Live scores for the current gameweek are displayed here");
		tips.add("Click a match for more complete player scores");
		tips.add("'Bonus Pts' means that bonus points have been allocated for this match");
		tips.add("Use sync panel to update scores if auto-sync is disabled");
		
		setContentView(R.layout.matches_screen);
		
		if (gameweek == 0) {
			gameweek = 1;
		}
		
		LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View matchesHeader = vi.inflate(R.layout.matches_screen_header, null);
        
		matchesList = (ListView) findViewById(R.id.matches);
		matchesList.addHeaderView(matchesHeader, null, false);
		
		String[] gameweeks = new String[App.num_gameweeks];
		for (int i=0; i<App.num_gameweeks; i++) {
			gameweeks[i] = String.valueOf(i + 1);
		}
		
		addFilter(FILTER_GAMEWEEK, "Gameweek", gameweeks, gameweek - 1, true, true);
		
	    popList();
	}
	
	private void popBmpCache() {
		if (bmp == null) {
			bmp = new BitmapCache(this);
			
			bmp.getBitmap(R.drawable.icon_bonus);
			bmp.getBitmap(R.drawable.icon_tick);
			bmp.getBitmap(R.drawable.icon_playing);
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
    
    // async task to load stats
	private class LoadTask extends FPLAsyncTask<Void, Void, Void> {
		ProgressDialog dialog;
		
		protected Void doInBackground(Void... params) {
			// MATCHES
		 	curMatches = App.dbGen.rawQuery("SELECT _id, team_home_id, team_away_id, res_goals_home, res_goals_away, datetime, got_bonus FROM fixture"
		 			+ " WHERE season = " + season + " AND gameweek = " + gameweek + " ORDER BY datetime ASC", null);
			
			return null;
        }
		protected void onPreExecute() {
			App.log("onPreExecute()");
	    	popBmpCache();
			
			dialog = new ProgressDialog(Matches.this);
			dialog.setMessage("Loading..");
			dialog.setCancelable(false);
			dialog.show();
			Matches.this.progress = dialog;
			
		    if (curMatches != null) {
		    	App.log("Matches close/null cursor");
		    	curMatches.close();
		 		curMatches = null;
		 	}
		}
        protected void onPostExecute(Void result) {
        	if (curMatches != null) {
	        	MatchesCursorAdapter matchesAdapter = new MatchesCursorAdapter(Matches.this, curMatches);
	        	matchesList.setAdapter(matchesAdapter);
	        	OnItemClickListener matchesListener = new OnItemClickListener() {
	    			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
	    				// load fixture screen for fixture "id"
	    				Intent intent = new Intent(view.getContext(), Fixture.class);
	    				App.log("id: " + id);
	    				intent.putExtra(Fixture.P_FIXTURE_ID, (int) id);
	    		    	startActivity(intent);
	    			}
	    	    };
	    	    matchesList.setOnItemClickListener(matchesListener);
	    	    matchesList.invalidate();
        	}
        	setScreenshotView(matchesList, "Matches for gameweek " + gameweek);
        	
        	dialog.dismiss();
        	loader = null;
        }
    }
	
	private LoadTask loader;
	
	// load data and populate listview
	private void popList() {
		loader = new LoadTask();
		loader.fplExecute();
	}
	
	public class MatchesCursorAdapter extends CursorAdapter {
    	private int i_team_home_id, i_team_away_id, i_res_goals_home, i_res_goals_away;
    	private int i_got_bonus, i_datetime;
    	private boolean loaded_indexes;
    	
    	public MatchesCursorAdapter(Context context, Cursor c) {
			super(context, c);
		}
    	//_id, name, player_name, points, c_points
		private void loadIndexes(Cursor cursor) {
        	loaded_indexes = true;
        	i_team_home_id = cursor.getColumnIndex("team_home_id");
        	i_team_away_id = cursor.getColumnIndex("team_away_id");
        	i_res_goals_home = cursor.getColumnIndex("res_goals_home");
        	i_res_goals_away = cursor.getColumnIndex("res_goals_away");
        	i_got_bonus = cursor.getColumnIndex("got_bonus");
        	i_datetime = cursor.getColumnIndex("datetime");
        }
    	
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// load column indexes if required
			if (!loaded_indexes) loadIndexes(cursor);
			TextView t;
			
			t = (TextView) view.findViewById(R.id.homename);
			t.setText(App.id2team.get(cursor.getInt(i_team_home_id)));
			t = (TextView) view.findViewById(R.id.awayname);
			t.setText(App.id2team.get(cursor.getInt(i_team_away_id)));
			
			t = (TextView) view.findViewById(R.id.homescore);
			t.setText(cursor.getString(i_res_goals_home));
			t = (TextView) view.findViewById(R.id.awayscore);
			t.setText(cursor.getString(i_res_goals_away));
			
			ImageView i = (ImageView) view.findViewById(R.id.state);
			long datetime = cursor.getLong(i_datetime);
			int got_bonus = cursor.getInt(i_got_bonus);
			long now = App.currentUkTime();
			if (datetime <= now) {
				if (got_bonus == 2) {
					i.setImageBitmap(bmp.getBitmap(R.drawable.icon_bonus));
				} else if(got_bonus == 1) {
					i.setImageBitmap(bmp.getBitmap(R.drawable.icon_tick));
				} else {
					i.setImageBitmap(bmp.getBitmap(R.drawable.icon_playing));
				}
			} else {
				i.setImageBitmap(null);
			}
			
			t = (TextView) view.findViewById(R.id.date);
			t.setText(App.printDate(App.ukToLocal(datetime)));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final View view = LayoutInflater.from(context).inflate(R.layout.matches_screen_item, parent, false);
		    return view;
		}
    }
    
    // callback
    public void dataChanged() {
    	App.log("dataChanged()");
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
    	if (curMatches != null) {
	 		curMatches.close();
	 	}
    	super.onDestroy();
    }
    
}
