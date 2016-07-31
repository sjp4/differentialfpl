package com.pennas.fpl;

import com.pennas.fpl.process.FPLService;
import com.pennas.fpl.process.FPLService.ProcRes;
import com.pennas.fpl.util.GraphicUtil;
import com.pennas.fpl.util.Refreshable;
import com.pennas.fpl.util.DbGen;
import com.pennas.fpl.util.FPLActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

public class Home extends FPLActivity implements Refreshable {
	
	private GraphicUtil gUtil;
	
	private ImageView b_scores;
	private ImageView b_selection;
	private ImageView b_leagues;
	private ImageView b_vidiprinter;
	private ImageView b_stats;
	private ImageView b_players;
	private ImageView b_teams;
	private ImageView b_links;
	private ImageView b_live;
	private ImageView b_matches;
	private TextView info_squad;
	private TextView info_next;
	
	private TextView log_in_out;
	public static final String P_SHOW_SYNC = "com.pennas.fpl.show_sync";
	private boolean show_sync = false;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_screen);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
			App.log("got extras:");
			show_sync = extras.getBoolean(P_SHOW_SYNC);
		}
		
		hasTips = true;
        tipsPref = Settings.PREF_TIPS_LIVE;
		if (!App.logged_in) tips.add("Log in to FPL below to see your team, leagues, and much more");
		tips.add("Download the latest data using the sync panel");
		tips.add("Sync icon in top right shows data transfer/processing in background");
		tips.add("Sync and Notification settings can be found from the menu");
		tips.add("Tips like these appears on most screens; you can hide them, and recall from the menu");
		
		b_scores = (ImageView) findViewById(R.id.b_scores);
        b_selection = (ImageView) findViewById(R.id.b_selection);
        b_leagues = (ImageView) findViewById(R.id.b_leagues);
        b_vidiprinter = (ImageView) findViewById(R.id.b_vidiprinter);
        b_stats = (ImageView) findViewById(R.id.b_stats);
        b_players = (ImageView) findViewById(R.id.b_players);
        b_teams = (ImageView) findViewById(R.id.b_teams);
        b_links = (ImageView) findViewById(R.id.b_links);
        b_live = (ImageView) findViewById(R.id.b_live);
        b_matches = (ImageView) findViewById(R.id.b_matches);
        
        info_squad = (TextView) findViewById(R.id.info_squad);
        info_next = (TextView) findViewById(R.id.info_next);
        
        log_in_out = (TextView) findViewById(R.id.log_in_out);
        
        gUtil = new GraphicUtil(this);
        
        App.login_task_ref = this;
        
        pop();
        
        //STOP SHIP
        //DbGen.copy_to_sd_card(this);
    }
    
    private void pop() {
    	popImg(b_selection, R.drawable.icon_selection);
    	popImg(b_leagues, R.drawable.icon_leagues);
    	popImg(b_vidiprinter, R.drawable.icon_vidiprinter);
    	popImg(b_stats, R.drawable.icon_stats);
    	popImg(b_players, R.drawable.icon_players);
    	popImg(b_teams, R.drawable.icon_teams);
    	popImg(b_links, R.drawable.icon_links);
    	popImg(b_live, R.drawable.icon_hot);
    	
    	popDynamic();
    }
    
    private void popDynamic() {
    	info_squad.setText(App.my_squad_name);
    	long diff = App.next_deadline - App.currentUkTime();
    	boolean soon = (diff < FPLService.COUNTDOWN_THRESH);
    	
    	if (App.cur_gameweek < App.num_gameweeks) {
    		info_next.setText(App.printDate(App.next_deadline) + " (" + App.printTimeDiff(diff, soon) + ")");
    	} else {
    		info_next.setText("");
    	}
    	
    	actionBar.setTitle("Differential FPL");
    	if (App.cur_gameweek > 0) {
    		actionBar.setSubtitle("Gameweek " + App.cur_gameweek);
    	} else {
    		actionBar.setSubtitle("Pre-season");
    	}
    	
    	popImg(b_matches, R.drawable.icon_matches, null, App.live);
    	popImg(b_scores, R.drawable.icon_points, App.gw_score.points_string, App.live);
    	
    	if (App.logged_in) {
    		log_in_out.setText("Log Out");
    	} else {
    		log_in_out.setText("Log In");
    	}
    }
    
    private void popImg(ImageView i, int img) { popImg(i, img, null, false); }
    private void popImg(ImageView i, int img, String score, boolean live) {
    	Bitmap b = gUtil.getReflection(img, null, false, score, false, live, false);
		//Drawable d = new BitmapDrawable(getResources(), b);
		//t.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
    	i.setImageBitmap(b);
    }
    
    private static final int DIALOG_LOGIN = 1;
    private static final int DIALOG_AUTO_UPDATE = 2;
    
    // log out
    private void logOut(boolean pop) {
    	App.logOut(this);
    	if (pop) {
			popDynamic();
		}
    }
    
    public void clickLogInOut(View v) {
    	if (App.logged_in) {
    		// log out
    		logOut(true);
    	} else {
    		// log in
    		showDialog(DIALOG_LOGIN);
    	}
    }
    
    // init login procs
    private void logIn() {
    	Settings.setBoolPref(Settings.PREF_UPDATE_DATA, false, this);
    	FPLActivity.progress_text = LOGIN_LOGGING_IN;
	    showDialog(FPLActivity.DIALOG_PROGRESS_NEW);
	    App.login_task = true;
	    login_login = true;
		App.initProc(true, FPLService.PROC_SCRAPE_PLAYERS);
    }
    
    public static final String LOGIN_LOGGING_IN = "Logging in (1/2)..";
    public static final String LOGIN_LEAGUES_HISTORY_TEXT = "Getting leagues/history (2/2)..";
    private boolean login_login;
    
    // callback from login procs when they complete
    public void callBack(ProcRes result) {
    	if (result.failed) {
    		App.log("callback failed");
    		dismissDialog(FPLActivity.DIALOG_PROGRESS_NEW);
    		logOut(true);
    		App.login_task = false;
    		return;
    	}
    	
    	if (result.procType == FPLService.PROC_SCRAPE_PLAYERS) {
    		App.log("callback ok 1");
    		login_login = false;
    		App.initProc(true, FPLService.PROC_SCRAPE_SQUADS);
    		FPLActivity.progress_text = LOGIN_LEAGUES_HISTORY_TEXT;
    		dismissDialog(FPLActivity.DIALOG_PROGRESS_NEW);
    		showFPLDialog(FPLActivity.DIALOG_PROGRESS_NEW);
    	} else if (result.procType == FPLService.PROC_SCRAPE_SQUADS) {
    		App.log("callback ok 2");
    		App.logged_in = true;
    		App.login_task = false;
    		dismissDialog(FPLActivity.DIALOG_PROGRESS_NEW);
        	popDynamic();
        	showFPLDialog(DIALOG_AUTO_UPDATE);
    	}
    }
    
    public void updateLoginText(int progress) {
    	if (login_login) {
    		FPLActivity.progress_text = LOGIN_LOGGING_IN;
    	} else {
    		FPLActivity.progress_text = LOGIN_LEAGUES_HISTORY_TEXT;	
    	}
    	FPLActivity.progress_text += " " + progress + "%";
    	showFPLDialog(FPLActivity.DIALOG_PROGRESS_NEW);
    }
    
    // dialog shown when long-click on stat: choose stat for GUI
    protected Dialog onCreateDialog(int id) {
        Dialog new_dialog = null;
        AlertDialog.Builder builder;
        switch(id) {
	        case DIALOG_LOGIN:
	        	// Preparing views
	            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
	            View layout = inflater.inflate(R.layout.login_dialog, (ViewGroup) findViewById(R.id.layout_root));
	            // layout_root should be the name of the "top-level" layout node in the dialog_layout.xml file.
	            final EditText email = (EditText) layout.findViewById(R.id.email);
	            final EditText password = (EditText) layout.findViewById(R.id.password);
	            // Building dialog
	            builder = new AlertDialog.Builder(this);
	            builder.setView(layout);
	            builder.setPositiveButton("Log In", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialoglog, int which) {
	                    dialoglog.dismiss();
	                    String emailset = email.getText().toString();
	                    String passwordset = password.getText().toString();
	                    if ( (emailset != null) && (emailset.length() > 0) ) {
	                    	if ( (passwordset != null) && (passwordset.length() > 0) ) {
	                    		Settings.setStringPref(Settings.PREF_FPL_EMAIL, emailset, Home.this);
	    	            		Settings.setStringPref(Settings.PREF_FPL_PASSWORD, passwordset, Home.this);
	    	            		logIn();
	                    	}
	                    }
	                }
	            });
	            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialoglog, int which) {
	                    dialoglog.dismiss();
	                }
	            });
	            new_dialog = builder.create();
	            break;
	        case DIALOG_AUTO_UPDATE:
	        	// build dialogue from array
	        	builder = new AlertDialog.Builder(this);
	        	builder.setTitle("Update Automatically?");
	        	builder.setCancelable(false);
	        	builder.setMessage("Would you like Differential to automatically update players, scores, fixtures and leagues?"
	        			+ "\nYou can choose in more detail on the Settings page.");
	            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialogp, int idp) {
	                	Settings.setBoolPref(Settings.PREF_UPDATE_DATA, true, Home.this);
	                }
	            });
	            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialogp, int idp) {
	                	Settings.setBoolPref(Settings.PREF_UPDATE_DATA, false, Home.this);
	                }
	            });
	        	new_dialog = builder.create();
	            break;
        }
        if (new_dialog == null) {
        	return super.onCreateDialog(id);
        }
        return new_dialog;
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	switch(id){
    		case(DIALOG_LOGIN):
    			EditText email = (EditText) dialog.findViewById(R.id.email);
            	EditText password = (EditText) dialog.findViewById(R.id.password);
            	email.setText(null);
            	password.setText(null);
    			break;
    		default:
    			super.onPrepareDialog(id, dialog);
    	}
    }
    
    // home screen click handlers
    public void clickSelection(View v) {
    	if (App.my_squad_id == 0) {
    		Toast.makeText(this, "Squad not set - please login", Toast.LENGTH_LONG).show();
    	} else {
	    	if (App.next_gameweek > 0) {
	    		Intent intent = new Intent(this, Selection.class);
		    	intent.putExtra(Selection.P_SELECTION, true);
		    	intent.putExtra(Selection.P_SQUADID, App.my_squad_id);
		    	intent.putExtra(Selection.P_GAMEWEEK, App.next_gameweek);
		    	startActivity(intent);
	    	} else {
	    		Toast.makeText(this, "The season has ended", Toast.LENGTH_LONG).show();
	    	}
    	}
    }
    public void clickScores(View v) {
    	if (App.my_squad_id == 0) {
    		Toast.makeText(this, "Squad not set - please login", Toast.LENGTH_LONG).show();
    	} else {
    		if (App.cur_gameweek > 0) {
	    		Intent intent = new Intent(this, Selection.class);
		    	intent.putExtra(Selection.P_SELECTION, false);
		    	intent.putExtra(Selection.P_SQUADID, App.my_squad_id);
		    	intent.putExtra(Selection.P_GAMEWEEK, App.cur_gameweek);
		    	startActivity(intent);
    		} else {
	    		Toast.makeText(this, "The season has not started", Toast.LENGTH_LONG).show();
	    	}
    	}
    }
    public void clickPlayers(View v) {
    	Intent intent = new Intent(this, Players.class);
    	startActivity(intent);
    }
    public void clickVidiprinter(View v) {
    	Intent intent = new Intent(this, Vidiprinter.class);
    	startActivity(intent);
    }
    public void clickStats(View v) {
    	Intent intent = new Intent(this, Stats.class);
    	startActivity(intent);
    }
    public void clickTeams(View v) {
    	Intent intent = new Intent(this, Teams.class);
    	startActivity(intent);
    }
    public void clickLeagues(View v) {
    	Intent intent = new Intent(this, Leagues.class);
    	startActivity(intent);
    }
    public void clickLinks(View v) {
    	Intent intent = new Intent(this, Links.class);
    	startActivity(intent);
    }
    public void clickLive(View v) {
    	if (App.cur_gameweek > 0) {
    		Intent intent = new Intent(this, Hot.class);
    		startActivity(intent);
    	} else {
    		Toast.makeText(this, "The season has not started", Toast.LENGTH_LONG).show();
    	}
    }
    public void clickMatches(View v) {
    	Intent intent = new Intent(this, Matches.class);
    	startActivity(intent);
    }
    
    protected void onResume() {
    	super.onResume();
    	popDynamic();
    	
    	if (show_sync) {
        	App.log("opening drawer");
        	SlidingDrawer slide = App.slide;
	    	if (slide != null) {
	    		slide.open();
	    	}
        }
    }
    
    // callback
    public void dataChanged() {
    	popDynamic();
    }
    
    protected void onDestroy() {
    	App.login_task_ref = null;
    	super.onDestroy();
    }

}