package com.pennas.fpl;

import java.util.HashMap;
import java.util.HashSet;

import javax.crypto.BadPaddingException;

import com.pennas.fpl.Players.Stat;
import com.pennas.fpl.process.DiffBroadcastReceiver;
import com.pennas.fpl.process.UpdateManager;
import com.pennas.fpl.util.SimpleCryptoNew;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.EditText;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	public static final int DIALOG_DIAG = 886;
	
	// sharedpreference keys
	public static final String PREF_FPL_EMAIL = "FPLEmail";
	public static final String PREF_FPL_PASSWORD = "FPLPassword";
	public static final String PREF_FPL_SQUADID = "FPLSquadId";
	
	public static final String PREF_NOTIF_GOAL = "notif_goal";
	public static final String PREF_NOTIF_ASSIST = "notif_assist";
	public static final String PREF_NOTIF_RED = "notif_red";
	public static final String PREF_NOTIF_PRICE = "notif_price";
	public static final String PREF_NOTIF_DEADLINE = "notif_deadline";
	public static final String PREF_NOTIF_TRANSFER = "notif_transfer";
	public static final String PREF_NOTIF_KICKOFF = "notif_kickoff";
	public static final String PREF_NOTIF_FINAL_WHISTLE = "notif_finalwhistle";
	public static final String PREF_NOTIF_SOUND_SCORING = "notif_sound_scoring";
	public static final String PREF_NOTIF_SOUND_ALERTS = "notif_sound_alerts";
	
	public static final String PREF_UPDATE_DATA = "update_data";
	public static final String PREF_UPDATE_PLAYERS = "update_players";
	public static final String PREF_UPDATE_SCORES = "update_scores";
	public static final String PREF_UPDATE_SQUADS = "update_squads";
	
	public static final String PREF_START_ON_BOOT = "start_on_boot";
	public static final String PREF_WAKE = "wake";
	
	public static final String PREF_WIDGETS_MODE_MY = "widgets_mode_my";
	
	public static final String PREF_CHECK_FOR_UPDATES = "check_for_updates";
	public static final String PREF_LIVE_BONUS = "live_bonus";
	public static final String PREF_ALTERNATE_TICKER_COLOURS = "alternate_ticker_colours";
	
	public static final String PREF_PREFIX_UPDATE = "update";
	public static final String PREF_PREFIX_PLAYER_STAT = "ps_";
	public static final String PREF_PREFIX_TEAM_STAT = "ts_";
	
	// non-settings-screen prefs
	public static final String PREF_MIN_GAMES_FILTER = "min_games_filter";
	public static final String PREF_PLAYERS_VIEW_TYPE = "players_view_type";
	public static final String PREF_PLAYERS_FIELDS = "players_fields";
	public static final String PREF_PLAYERS_TRAFFIC = "players_traffic";
	public static final String PREF_SELECTION_STAT = "selection_stat";
	public static final String PREF_SELECTION_TRAFFIC = "selection_traffic";
	
	public static final String PREF_WARNED_SD = "warned_sd";
	public static final String PREF_DEADLINE_NOTIF_DONE = "deadline_notif_done";
	
	public static final String PREF_DB_GEN_VERSION = "db_version";
	public static final String PREF_FAILS = "fails";
	
	public static final String PREF_NOTES = "notes";
	
	public static final String INSTALLED_APP_VERSION = "app_version";
	
	public static final String NOTIFIED_NEW_VERSION = "new_version";
	public static final String LAST_CHECKED_NEW_VERSION = "last_checked_new_version";
	public static final String LAST_NOTIFIED_NOTICE = "last_notified_notice";
	
	// tips
	public static final String PREF_TIPS_SELECTION = "pref_tips_selection";
	public static final String PREF_TIPS_SCORES = "pref_tips_scores";
	public static final String PREF_TIPS_PLAYER = "pref_tips_player";
	public static final String PREF_TIPS_PLAYERS = "pref_tips_players";
	public static final String PREF_TIPS_PLAYERS_TRANSFER = "pref_tips_players_transfer";
	public static final String PREF_TIPS_TEAMS = "pref_tips_teams";
	public static final String PREF_TIPS_TEAM = "pref_tips_team";
	public static final String PREF_TIPS_LEAGUES = "pref_tips_leagues";
	public static final String PREF_TIPS_LEAGUE = "pref_tips_league";
	public static final String PREF_TIPS_SQUAD = "pref_tips_squad";
	public static final String PREF_TIPS_STATS = "pref_tips_stats";
	public static final String PREF_TIPS_LIVE = "pref_tips_live";
	public static final String PREF_TIPS_MATCHES = "pref_tips_matches";
	
	// values for update players pref
	public static final int UPDATE_PLAYERS_NEVER = 0;
	public static final int UPDATE_PLAYERS_DAILY = 1;
	public static final int UPDATE_PLAYERS_GW = 2;
	
	// values for update scores pref
	public static final int UPDATE_SCORES_NEVER = 0;
	
	// values for update squads pref
	public static final int UPDATE_SQUADS_NEVER = 0;
	public static final int UPDATE_SQUADS_GW = 1;
	
	// values for notif pref
	public static final int NOTIF_OFF = 0;
	public static final int NOTIF_MY_TEAM = 1;
	public static final int NOTIF_MY_TEAM_RIVALS = 2;
	public static final int NOTIF_ALL_PLAYERS = 3;
	
	// values for notif price pref
	public static final int NOTIF_PRICE_OFF = 0;
	public static final int NOTIF_PRICE_MY_TEAM = 1;
	public static final int NOTIF_PRICE_WATCHLIST = 2;
	public static final int NOTIF_PRICE_MY_TEAM_WATCHLIST = 3;
	
	// current values of each preference, by type
	private static HashMap<String, String> stringPrefs = new HashMap<String, String>();
	private static HashMap<String, HashSet<Integer>> intSetPrefs = new HashMap<String, HashSet<Integer>>();
	private static HashMap<String, Integer> intPrefs = new HashMap<String, Integer>();
	private static HashMap<String, Boolean> boolPrefs = new HashMap<String, Boolean>();
	private static HashMap<String, Long> longPrefs = new HashMap<String, Long>();
	
	// allocation of preferences to type
	private static String[] stringPrefList = { PREF_FPL_EMAIL, PREF_FPL_PASSWORD, PREF_PLAYERS_FIELDS, LAST_NOTIFIED_NOTICE
		, PREF_NOTES };
	
	private static String[] intSetPrefList = { PREF_WIDGETS_MODE_MY };
	
	private static String[] intPrefList = { PREF_NOTIF_GOAL, PREF_NOTIF_ASSIST, PREF_NOTIF_RED, PREF_NOTIF_PRICE
		, PREF_MIN_GAMES_FILTER, PREF_PLAYERS_VIEW_TYPE, PREF_FPL_SQUADID, PREF_SELECTION_STAT, PREF_SELECTION_TRAFFIC
		, PREF_PLAYERS_TRAFFIC, PREF_UPDATE_PLAYERS, PREF_UPDATE_SCORES, PREF_UPDATE_SQUADS, PREF_DB_GEN_VERSION
		, PREF_DEADLINE_NOTIF_DONE, INSTALLED_APP_VERSION, NOTIFIED_NEW_VERSION, PREF_FAILS };
	
	private static String[] longPrefList = { LAST_CHECKED_NEW_VERSION };
	
	private static String[] boolPrefList = { PREF_NOTIF_DEADLINE, PREF_NOTIF_TRANSFER, PREF_TIPS_SELECTION, PREF_TIPS_SCORES
		, PREF_TIPS_PLAYERS, PREF_TIPS_PLAYERS_TRANSFER, PREF_TIPS_TEAMS, PREF_TIPS_LEAGUES, PREF_TIPS_TEAM, PREF_TIPS_SQUAD
		, PREF_TIPS_LEAGUE, PREF_TIPS_LIVE, PREF_UPDATE_DATA, PREF_TIPS_PLAYER, PREF_TIPS_STATS
		, PREF_TIPS_MATCHES, PREF_WARNED_SD, PREF_NOTIF_KICKOFF, PREF_NOTIF_FINAL_WHISTLE, PREF_START_ON_BOOT, PREF_WAKE
		, PREF_NOTIF_SOUND_SCORING, PREF_NOTIF_SOUND_ALERTS, PREF_CHECK_FOR_UPDATES, PREF_LIVE_BONUS, PREF_ALTERNATE_TICKER_COLOURS };
	
	private static String[] boolDefaultTrue = { PREF_TIPS_SELECTION, PREF_TIPS_SCORES, PREF_TIPS_PLAYERS, PREF_TIPS_PLAYERS_TRANSFER
		, PREF_TIPS_TEAMS, PREF_TIPS_LEAGUES, PREF_TIPS_TEAM, PREF_TIPS_SQUAD, PREF_TIPS_LEAGUE, PREF_TIPS_LIVE, PREF_TIPS_PLAYER
		, PREF_TIPS_STATS, PREF_TIPS_MATCHES, PREF_NOTIF_FINAL_WHISTLE, PREF_CHECK_FOR_UPDATES, PREF_LIVE_BONUS };
	
	private static final String SET_DELIMETER = ",";
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (App.style != App.DEFAULT_STYLE) {
			setTheme(App.style);
		}
		
        // load preference def
        addPreferencesFromResource(R.xml.preferences);
        
        // load player stats
        PreferenceScreen playersScreen = (PreferenceScreen) findPreference("FourthPrefScreen");
        Players.loadStats();
	 	for (Stat s : Players.playerstats_array) {
	 		if (s.show_in_prefs) {
		 		//create one check box for each setting you need
		        CheckBoxPreference pref = new CheckBoxPreference(this);
		        //make sure each key is unique  
		        pref.setKey(PREF_PREFIX_PLAYER_STAT + s.db_field);
		        pref.setTitle(s.desc);
		        pref.setDefaultValue(true);
		        
		        playersScreen.addPreference(pref);
	 		}
	 	}
        
	 	// load player stats
        PreferenceScreen teamsScreen = (PreferenceScreen) findPreference("FifthPrefScreen");
        Teams.loadStats();
	 	for (Stat s : Teams.teamstats) {
	 		if (s.show_in_prefs) {
		 		//create one check box for each setting you need
		        CheckBoxPreference pref = new CheckBoxPreference(this);
		        //make sure each key is unique  
		        pref.setKey(PREF_PREFIX_TEAM_STAT + s.db_field);
		        pref.setTitle(s.desc);
		        pref.setDefaultValue(true);
		        
		        teamsScreen.addPreference(pref);
	 		}
	 	}
        
        // set default values
        PreferenceManager.setDefaultValues(Settings.this, R.xml.preferences, false);
        
        // listener for diags button
        Preference diagsPref = (Preference) findPreference("diags");
        if (App.diags) {
        	diagsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                	if (App.logger != null) {
                		App.logger.sendDiagnostics(Settings.this);
                	}
                	return true;
                }
            });
		} else {
			PreferenceCategory category = (PreferenceCategory) findPreference("misc");
			category.removePreference(diagsPref);
		}
        
        // show the current value of each pref in summary
        for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
        	initSummary(getPreferenceScreen().getPreference(i));
        }
    }

    @Override 
    protected void onResume(){
        super.onResume();
        // Set up a listener whenever a key changes             
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override 
    protected void onPause() { 
        super.onPause();
        // Unregister the listener whenever a key changes             
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);     
    }
    
    protected Dialog onCreateDialog(int dialog_id) {
    	if (dialog_id == DIALOG_DIAG) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			// Set an EditText view to get user input 
			final EditText input = new EditText(this);
			input.setHint("Enter details of issue");
			//input.setInputType(EditorInfo.TYPE_TEXT_VARIATION_NORMAL);
			builder.setView(input);
	    	builder.setTitle("Send diagnostics");
	    	builder.setCancelable(true);
	    	builder.setMessage("Send diagnostic/debug information to help the developer solve issues? Please enter details of any problems you are encountering");
	        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	            	App.logger.createDiagnostics(input.getText().toString());
	            	input.setText(null);
	            }
	        });
	        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	            	
	            }
	        });
	        Dialog new_dialog = builder.create();
			return new_dialog;
		} else {
			return null;
		}
    }
    
    public static void setDefaults(Context c) {
    	// init default values from xml first
    	PreferenceManager.setDefaultValues(c, R.xml.preferences, false);
    	
    	// populate static global variables with current preference values
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    	for (int i=0; i<stringPrefList.length; i++) {
    		loadStringPref(stringPrefList[i], prefs);
    	}
    	for (int i=0; i<intSetPrefList.length; i++) {
    		loadIntSetPref(intSetPrefList[i], prefs);
    	}
    	for (int i=0; i<intPrefList.length; i++) {
    		loadIntPref(intPrefList[i], prefs);
    	}
    	for (int i=0; i<boolPrefList.length; i++) {
    		loadBoolPref(boolPrefList[i], prefs);
    	}
    	for (int i=0; i<longPrefList.length; i++) {
    		loadLongPref(longPrefList[i], prefs);
    	}
    }
    
    // load preference values into memory
    //
    // load string pref into memory
    private static void loadStringPref(String key, SharedPreferences prefs) {
    	String val = prefs.getString(key, null);
    	if (key.equals(PREF_FPL_PASSWORD) && (val != null) )
			try {
				//App.log("before decrypt: " + val);
				val = SimpleCryptoNew.decrypt(App.appThing, val);
				//App.log("after decrypt");
			} catch (BadPaddingException e) {
				App.log("badpaddingexception; logging out");
				App.logOut(App.appContext);
				App.exception(e);
	    	} catch (Exception e) {
				App.exception(e);
			}
		stringPrefs.put(key, val);
    }
    // load int set pref into memory
    private static void loadIntSetPref(String key, SharedPreferences prefs) {
    	String rawVal = prefs.getString(key, null);
    	HashSet<Integer> val = new HashSet<Integer>();
    	if (rawVal != null) {
	    	String[] vals = rawVal.split(SET_DELIMETER);
	    	for (String s : vals) {
	    		if (s.length() > 0) {
	    			val.add(Integer.valueOf(s));
	    		}
	    	}
    	}
    	intSetPrefs.put(key, val);
    }
    // load int pref into memory
    private static void loadIntPref(String key, SharedPreferences prefs) {
    	String val = prefs.getString(key, null);
    	int ret;
		if (val == null) {
    		ret = 0;
    	} else {
    		ret = Integer.parseInt(val);
    	}
		intPrefs.put(key, ret);
    }
    // load bool pref into memory
    private static void loadBoolPref(String key, SharedPreferences prefs) {
    	boolean def = false;
    	
    	for (int i=0; i<boolDefaultTrue.length; i++) {
    		if (key.equals(boolDefaultTrue[i])) def = true;
    	}
    	
    	Boolean val = prefs.getBoolean(key, def);
    	boolPrefs.put(key, val);
    }
    // load long pref into memory
    private static void loadLongPref(String key, SharedPreferences prefs) {
    	String val = prefs.getString(key, null);
    	long ret;
		if (val == null) {
    		ret = 0;
    	} else {
    		ret = Long.parseLong(val);
    	}
		longPrefs.put(key, ret);
    }
    
    // set pref value
    //
    // set string pref value
    public static void setStringPref(String key, String value, Context c) {
    	String valUse = value;
    	if (key.equals(PREF_FPL_PASSWORD) && (value != null) )
			try {
				//App.log("before encrypt: " + valUse);
				valUse = SimpleCryptoNew.encrypt(App.appThing, value);
				//App.log("after encrypt");
			} catch (Exception e) {
				App.exception(e);
			}
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    	prefs.edit().putString(key, valUse).commit();
    	stringPrefs.put(key, value);
    }
    // set int set pref value
    public static void setIntSetPref(String key, HashSet<Integer> value, Context c) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    	StringBuilder rawVal = new StringBuilder();
    	boolean comma = false;
    	for (Integer i : value) {
    		if (comma) {
    			rawVal.append(SET_DELIMETER);
    		}
    		comma = true;
    		rawVal.append(i);
    	}
    	prefs.edit().putString(key, rawVal.toString()).commit();
    	intSetPrefs.put(key, value);
    }
    // set int pref value
    public static void setIntPref(String key, int value, Context c) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    	prefs.edit().putString(key, String.valueOf(value)).commit();
    	intPrefs.put(key, value);
    }
    // set bool pref value
    public static void setBoolPref(String key, boolean value, Context c) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    	prefs.edit().putBoolean(key, value).commit();
    	boolPrefs.put(key, value);
    }
    // set long pref value
    public static void setLongPref(String key, long value, Context c) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    	prefs.edit().putString(key, String.valueOf(value)).commit();
    	longPrefs.put(key, value);
    }
    
    // get pref values from memory
    //
    // get string pref (from memory)
    public static String getStringPref(String key) {
    	if (!isStringPref(key)) {
    		App.log("ERROR: tried to access String pref but " + key + " is not a String pref");
    		return null;
    	}
    	return stringPrefs.get(key);
    }
    // get int set pref (from memory)
    public static HashSet<Integer> getIntSetPref(String key) {
    	if (!isStringSetPref(key)) {
    		App.log("ERROR: tried to access String pref but " + key + " is not an int set Set pref");
    		return null;
    	}
    	return intSetPrefs.get(key);
    }
    // get int pref (from memory)
    public static int getIntPref(String key) {
    	if (!isIntPref(key)) {
    		App.log("ERROR: tried to access int pref but " + key + " is not an int pref");
    		return 0;
    	}
    	return intPrefs.get(key);
    }
    // get bool pref (from memory)
    public static boolean getBoolPref(String key) {
    	if (!isBoolPref(key)) {
    		App.log("ERROR: tried to access bool pref but " + key + " is not a bool pref");
    		return false;
    	}
    	Boolean val = boolPrefs.get(key);
    	if (val == null) {
    		return false;
    	} else {
    		return val;
    	}
    }
    // get int pref (from memory)
    public static long getLongPref(String key) {
    	if (!isLongPref(key)) {
    		App.log("ERROR: tried to access int pref but " + key + " is not a long pref");
    		return 0;
    	}
    	return longPrefs.get(key);
    }
    
    // preference type checks
    //
    // does this key represent a string pref?
    private static boolean isStringPref(String key) {
    	for (int i=0; i<stringPrefList.length; i++) {
    		if (key.equals(stringPrefList[i])) return true;
    	}
    	return false;
    }
    // does this key represent an int set pref?
    private static boolean isStringSetPref(String key) {
    	for (int i=0; i<intSetPrefList.length; i++) {
    		if (key.equals(intSetPrefList[i])) return true;
    	}
    	return false;
    }
    // does this key represent an int pref?
    private static boolean isIntPref(String key) {
    	for (int i=0; i<intPrefList.length; i++) {
    		if (key.equals(intPrefList[i])) return true;
    	}
    	return false;
    }
    // does this key represent a bool pref?
    private static boolean isBoolPref(String key) {
    	for (int i=0; i<boolPrefList.length; i++) {
    		if (key.equals(boolPrefList[i])) return true;
    	}
    	return false;
    }
    // does this key represent an long pref?
    private static boolean isLongPref(String key) {
    	for (int i=0; i<longPrefList.length; i++) {
    		if (key.equals(longPrefList[i])) return true;
    	}
    	return false;
    }
    
    // load preference value into memory
    private void loadPref(SharedPreferences prefs, String key) {
    	if (isStringPref(key)) {
    		loadStringPref(key, prefs);
    	} else if (isIntPref(key)) {
    		loadIntPref(key, prefs);
    	} else if (isBoolPref(key)) {
    		loadBoolPref(key, prefs);
    	} else if (isLongPref(key)) {
    		loadLongPref(key, prefs);
    	}
    }
    
    // pref value change listener
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) { 
        App.log("sharedprefchangedlistener");
    	updatePrefSummary(findPreference(key));
        loadPref(sharedPreferences, key);
        
        // if key is one of the update prefs, recalc UpdateManager stale times
        if (key.startsWith(PREF_PREFIX_UPDATE)) {
        	App.log("pref changed: updating stake times");
        	UpdateManager.setStaleTimes();
        }
        
        // broadcast receiver change required
        if (key.equals(PREF_START_ON_BOOT) || key.equals(PREF_UPDATE_DATA)) {
        	boolean start_on_boot = getBoolPref(PREF_START_ON_BOOT);
        	App.log("setting start_on_boot = " + start_on_boot);
        	boolean update_data = getBoolPref(PREF_UPDATE_DATA);
        	App.log("setting update_data = " + update_data);
        	
        	int flag;
        	if (start_on_boot && update_data) {
        		App.log("Enabling boot receiver");
        		flag = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        	} else {
        		App.log("Disabling boot receiver");
        		flag = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        	}
        	
        	ComponentName component = new ComponentName(Settings.this, DiffBroadcastReceiver.class);
        	getPackageManager().setComponentEnabledSetting(component, flag, PackageManager.DONT_KILL_APP);
        }
        
        // player stats
        if (key.startsWith(PREF_PREFIX_PLAYER_STAT)) {
        	App.log("pref changed: nulling player stats list");
        	Players.playerstats = null;
        	Players.playerstats_array = null;
        	Players.loadStats();
        	// set all activities to refresh when they resume
			App.dataUpdatedTime = App.currentUTCTime();
        }
        
        // team stats
        if (key.startsWith(PREF_PREFIX_TEAM_STAT)) {
        	App.log("pref changed: nulling team stats list");
        	Teams.teamstats = null;
        	Teams.loadStats();
        	// set all activities to refresh when they resume
			App.dataUpdatedTime = App.currentUTCTime();
        }
    }
    
    // set summary to current value - helper
    private void initSummary(Preference p){
        if (p instanceof PreferenceCategory){
            PreferenceCategory pCat = (PreferenceCategory) p;
            for(int i=0;i<pCat.getPreferenceCount();i++){
                initSummary(pCat.getPreference(i));
            }
        } else if (p instanceof PreferenceScreen) {
        	PreferenceScreen pScreen = (PreferenceScreen) p;
        	for(int i=0;i<pScreen.getPreferenceCount();i++){
                initSummary(pScreen.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    // set summary to current value
    private void updatePrefSummary(Preference p){
        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p; 
            p.setSummary(listPref.getEntry()); 
        // PasswordPreference has to go before EditTextPreference because it is a subclass of it
    	} else if (p instanceof EditTextPreference) {
        	EditTextPreference editTextPref = (EditTextPreference) p; 
            p.setSummary(editTextPref.getText()); 
        }
    }  
    
} 