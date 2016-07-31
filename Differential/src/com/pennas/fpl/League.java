package com.pennas.fpl;

import com.pennas.fpl.Selection.ViewPagerAdapter;
import com.pennas.fpl.process.FPLService;
import com.pennas.fpl.util.ClickFPL;
import com.pennas.fpl.util.FPLActivity;
import com.pennas.fpl.util.DbGen;
import com.pennas.fpl.util.Refreshable;
import com.viewpagerindicator.TitlePageIndicator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

public class League extends FPLActivity implements Refreshable, ClickFPL {
	
	private ListView teamsList;
	private Cursor curTeams;
	
	private float max_score_season;
	private int league_id;
	public static final String P_LEAGUE_ID = "com.pennas.fpl.league_id";
	
	private int leagueCount;
	
	private ViewPager viewPager;
	private LinearLayout tab_details;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    // get ID of league
	    Bundle extras = getIntent().getExtras();
	    if (extras == null) return;
	    league_id = extras.getInt(P_LEAGUE_ID);
	    
	    hasTips = true;
        tipsPref = Settings.PREF_TIPS_LEAGUE;
		tips.add("Click a team to add them as a Rival and see gameweek scores");
		tips.add("Not all leagues started scoring from week 1: see Info tab");
		tips.add("At the moment, only the first few teams in each league are displayed");
		tips.add("Brighter coloured scores are live-updated (Rivals)");
		
		setContentView(R.layout.league_screen);
		
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	 	tab_details = (LinearLayout) inflater.inflate(R.layout.league_screen_details_tab, null);
	 	
		teamsList = new ListView(this);
	    
		viewPager = (ViewPager) findViewById(R.id.viewpager);
		final String[] titles = new String[] { "Teams", "Details" };
		final View[] views = new View[] { teamsList, tab_details };
		ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(titles, views);
	    TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.titles);
	    viewPager.setAdapter(pagerAdapter);
	    indicator.setViewPager(viewPager);
	    
		popList();
	}
	
	// load data and populate listview
	private void popList() {
		Cursor curLeague = App.dbGen.rawQuery("SELECT name, start_week FROM minileague"
				+ " WHERE _id = " + league_id, null);
		curLeague.moveToFirst();
		if (!curLeague.isAfterLast()) {
			TextView leagueInfo = (TextView) tab_details.findViewById(R.id.leagueinfo);
			actionBar.setTitle(DbGen.decompress(curLeague.getBlob(0)));
			String gwString = curLeague.getString(1);
			if (gwString == null) {
				gwString = "1";
			}
			leagueInfo.setText("Scoring starting from gameweek " + gwString);
			curLeague.moveToNext();
		}
		curLeague.close();
		
		Cursor minmax = App.dbGen.rawQuery("SELECT MAX(points) max_score, MAX(c_points) max_c_score FROM minileague_team"
				+ " WHERE minileague_id = " + league_id, null);
	 	minmax.moveToFirst();
	 	if (!minmax.isAfterLast()) {
		 	max_score_season = minmax.getFloat(1);
		 	float temp = minmax.getFloat(0);
		 	if (temp > max_score_season) {
		 		max_score_season = temp;
		 	}
	 	} else {
	 		max_score_season = 0;
	 		max_score_season = 0;
	 	}
	 	minmax.close();
	 	
	 	Cursor myscore = App.dbGen.rawQuery("SELECT points, c_points FROM minileague_team"
	 			+ " WHERE minileague_id = " + league_id + " AND squad_id = " + App.my_squad_id, null);
	    myscore.moveToFirst();
	    int my_score = 0;
	    if (!myscore.isAfterLast()) {
		 	my_score = myscore.getInt(1);
		 	if (my_score < 1) {
		 		my_score = myscore.getInt(0);
		 	}
	    }
	 	myscore.close();
	    
	 	if (curTeams != null) {
	 		curTeams.close();
	 	}
	 	
	 	// Teams
	 	curTeams = App.dbGen.rawQuery("SELECT s._id, s.name, s.player_name, mt.points, mt.c_points, s.rival"
	 			+ ", (SELECT CASE WHEN sg.c_points IS NULL THEN sg.points ELSE sg.c_points END pts"
				+ " FROM squad_gameweek sg WHERE sg.squad_id = s._id AND gameweek = " + App.cur_gameweek + ") gw_pts"
				+ " FROM minileague_team mt, squad s"
	 			+ " WHERE mt.squad_id = s._id AND mt.minileague_id = " + league_id
	 			+ " ORDER BY CASE WHEN mt.c_points ISNULL THEN mt.points ELSE mt.c_points END DESC", null);
		Leagues.SquadListCursorAdapter pAdapter = new Leagues.SquadListCursorAdapter(this, curTeams, max_score_season, my_score, false, true, null, false, 0);
		teamsList.setAdapter(pAdapter);
		OnItemClickListener listListener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
				// load squad screen
				Intent intent = new Intent(view.getContext(), FPLSquad.class);
				intent.putExtra(FPLSquad.P_SQUADID, (int) id);
		    	startActivity(intent);
			}
	    };
	    teamsList.setOnItemClickListener(listListener);
	    leagueCount = curTeams.getCount();
	}
	
	private static final int WARNING_NUM_SQUADS = 15;
	
	public void clickRivals(View v) {
		if (leagueCount > WARNING_NUM_SQUADS) {
			showDialog(DIALOG_CONFIRM);
		} else {
			doAddRivals();
		}	
	}
	
	private static final int DIALOG_CONFIRM = 33;
	
	protected Dialog onCreateDialog(int dialog_id) {
        Dialog new_dialog;
        switch(dialog_id) {
	        case DIALOG_CONFIRM:
	        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    	builder.setTitle("Add Rivals?");
		    	builder.setCancelable(true);
		    	builder.setMessage("Add all teams in this league as rivals?"
		    			+ " The more rivals you add, the more squad lineups must be downloaded every week"
		    			+ " (Individual teams can be added as rivals by clicking on them)");
		        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int id) {
		            	doAddRivals();
		            }
		        });
		        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int id) {
		            	
		            }
		        });
		        new_dialog = builder.create();
				break;
	        default:
	            new_dialog = null;
        }
        if (new_dialog == null) {
        	return super.onCreateDialog(dialog_id);
        }
        return new_dialog;
    }
    
    private void doAddRivals() {
		App.dbGen.execSQL("UPDATE squad SET rival = 1 WHERE _id IN"
				+ " (SELECT squad_id FROM minileague_team WHERE minileague_id = " + league_id + ")");
    	// squad scrape but only for rival lineups (ie not leagues)
    	if (Settings.getBoolPref(Settings.PREF_UPDATE_DATA)) App.initProc(true, FPLService.PROC_SCRAPE_SQUADS, 1);
	}
    
    public void clickFPL() {
    	Uri uri = Uri.parse( "http://fantasy.premierleague.com/my-leagues/" + league_id + "/standings/");
		startActivity( new Intent( Intent.ACTION_VIEW, uri ) );
    }
	
	// callback
    public void dataChanged() {
    	popList();
    }
    
    protected void onDestroy() {
	    if (curTeams != null) {
	 		curTeams.close();
	 	}
	    super.onDestroy();
    }
    
}
