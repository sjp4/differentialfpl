package com.pennas.fpl.util;

import java.util.ArrayList;
import java.util.HashMap;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.pennas.fpl.About;
import com.pennas.fpl.App;
import com.pennas.fpl.Home;
import com.pennas.fpl.Leagues;
import com.pennas.fpl.Players;
import com.pennas.fpl.R;
import com.pennas.fpl.Selection;
import com.pennas.fpl.Settings;
import com.pennas.fpl.Vidiprinter;
import com.pennas.fpl.ui.Worm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.actionbarsherlock.view.Window;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

public abstract class FPLActivity extends SherlockActivity implements ActionBar.OnNavigationListener
																		, OnClickListener {
	
	public static final int DIALOG_PROGRESS_NEW = 889;
	public static final int DIALOG_SD_WARN = 887;
	public static final int DIALOG_ADDRIVAL = 885;
	public static final int DIALOG_NEW_VERSION = 884;
	public static final int DIALOG_NOTICE = 883;
	public static final int DIALOG_GENERIC = 882;
	public static final int DIALOG_JOINLEAGUE = 881;
	
	private static final int MAX_ITEMS_IN_LIST = 1000;
	private static final int FIDDLE = 1000;
	
	public static String progress_text;
	
	protected boolean hasTips = false;
	protected boolean tipsActive = false;
	protected ArrayList<String> tips = new ArrayList<String>();
	protected String tipsPref;
	protected int curTip = 0;
	protected long wasPaused = NEVER;
	protected boolean paused = false;
	private int showDialogOnResume = 0;
	public ActionBar actionBar;
	
	private ImageView sync_img;
	private RotateAnimation sync_anim;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (App.style != App.DEFAULT_STYLE) {
			setTheme(App.style);
		}
		
		requestWindowFeature(Window.FEATURE_PROGRESS);
		
		actionBar = getSupportActionBar();
		// enable home icon click for api 14+
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);
		
		sync_img = new ImageView(this);
		sync_img.setOnClickListener(this);
		sync_img.setImageResource(R.drawable.ic_popup_sync_1);
		sync_img.setBackgroundResource(0);
		
		super.onCreate(savedInstanceState);
	}

	private static class FilterEntry {
		String name;
		SubMenu menu;
		String[] items;
		int cur_selection;
		boolean visible;
		boolean dropdown;
		int id;
	}
	
	private class ListEntry {
		String label;
		int pos;
		
		public String toString() {
			if (actionBar.getSelectedNavigationIndex() == pos) {
				return dropdownEntry.name + ": " + label;
			} else {
				return label;
			}
		}
	}
	
	private HashMap<Integer, FilterEntry> filters = new HashMap<Integer, FilterEntry>();
	private FilterEntry dropdownEntry = null;
	
	protected void addFilter(int pId, String pName, int resource, int selection
			, boolean pVis, boolean pDropdown) {
		addFilter(pId, pName, getResources().getStringArray(resource), selection, pVis, pDropdown);
	}
	protected void addFilter(int pId, String pName, String[] entries, int selection
			, boolean pVis, boolean pDropdown) {
		FilterEntry entry = new FilterEntry();
		entry.dropdown = pDropdown;
		entry.name = pName;
		entry.items = entries;
		entry.cur_selection = selection;
		entry.id = pId;
		entry.visible = pVis;
		
		// check for existing dropdown entry
		if (entry.dropdown) {
			if (dropdownEntry != null) {
				entry.dropdown = false;
			}
		}
		
		filters.put(pId, entry);
		
		setItems(entry);
	}
	protected void updateFilterList(int pId, String[] entries) {
		FilterEntry fil = filters.get(pId);
		fil.items = entries;
		
		setItems(fil);
	}
	private void setItems(FilterEntry entry) {
		// check for existing dropdown entry
		if (entry.dropdown) {
			dropdownEntry = entry;
			
			ArrayList<ListEntry> converted = new ArrayList<ListEntry>();
			for (int i=0; i<entry.items.length; i++) {
				ListEntry e = new ListEntry();
				e.label = entry.items[i];
				e.pos = i;
				converted.add(e);
			}
			
			ArrayAdapter<ListEntry> adapt = new ArrayAdapter<ListEntry>(
    				this, R.layout.sherlock_spinner_item, converted);
    		adapt.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
    		
    		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
    		actionBar.setListNavigationCallbacks(adapt, this);
		}
		refreshActionMenu();
	}
	protected void setFilterVisible(int pId, boolean pVis) {
		FilterEntry fil = filters.get(pId);
		fil.visible = pVis;
		refreshActionMenu();
	}
	
	private void refreshActionMenu() {
		if (dropdownEntry != null) {
			if (dropdownEntry.visible) {
				actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
				actionBar.setSelectedNavigationItem(dropdownEntry.cur_selection);
	        } else {
	        	actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	        }
		}
		invalidateOptionsMenu();
	}
	
	private boolean watchlist_on;
	private boolean mysquad_on;
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem m;
		
		m = menu.findItem(R.id.tips);
		if (!tipsActive && hasTips) {
			m.setEnabled(true);
			m.setVisible(true);
		} else {
			m.setEnabled(false);
			m.setVisible(false);
		}
		
		if (this instanceof Players) {
			m = menu.findItem(R.id.changeview);
			m.setEnabled(true);
			m.setVisible(true);
			
			m = menu.findItem(R.id.watchlist);
			m.setEnabled(true);
			m.setVisible(true);
			if (watchlist_on) {
				m.setIcon(R.drawable.btn_star_big_on);
			} else {
				m.setIcon(R.drawable.btn_star_big_off);
			}
		}
		
		if (this instanceof Vidiprinter) {
			m = menu.findItem(R.id.mysquad);
			m.setEnabled(true);
			m.setVisible(true);
			if (mysquad_on) {
				m.setIcon(R.drawable.btn_star_big_on);
			} else {
				m.setIcon(R.drawable.btn_star_big_off);
			}
		}
		
		if (this instanceof Leagues) {
			m = menu.findItem(R.id.addrival);
			m.setEnabled(true);
			m.setVisible(true);
			
			m = menu.findItem(R.id.joinleague);
			m.setEnabled(true);
			m.setVisible(true);
		}
		
		if (this instanceof About) {
			m = menu.findItem(R.id.about);
			m.setEnabled(false);
			m.setVisible(false);
		}
		
		m = menu.findItem(R.id.sync);
		m.setActionView(sync_img);
		
		boolean fpl = false;
		if (this instanceof Selection) {
			Selection sel = (Selection) this;
			fpl = !sel.c_selection;
		} else if (this instanceof ClickFPL) {
			fpl = true;
		}
		if (fpl) {
			m = menu.findItem(R.id.fpl);
			m.setEnabled(true);
			m.setVisible(true);
		}
		
		boolean showFilters = false;
		for (FilterEntry entry : filters.values()) {
			if (entry.visible && !entry.dropdown) {
				showFilters = true;
			}
		}
		if (showFilters) {
			m = menu.findItem(R.id.filter);
			m.setEnabled(true);
			m.setVisible(true);
			
			SubMenu sub = m.getSubMenu();
			if (sub == null) {
				App.log("null sub menu");
			} else {
				sub.clear();
				for (FilterEntry entry : filters.values()) {
					if (entry.visible && !entry.dropdown) {
						// html is crashing ICS at the moment
						//entry.menu = sub.addSubMenu(Html.fromHtml(
						//		"<b>" + entry.name + "</b>: " + entry.items[entry.cur_selection]));
						if ( (entry.items.length > 0) && (entry.cur_selection < entry.items.length) ) {
							entry.menu = sub.addSubMenu(entry.name + ": " + entry.items[entry.cur_selection]);
							
						    for (int i=0; i<entry.items.length; i++) {
						    	int id = (MAX_ITEMS_IN_LIST * entry.id) + i + FIDDLE;
						    	MenuItem add = entry.menu.add(Menu.NONE, id, Menu.NONE, entry.items[i]);
						    	//App.log("id: " + id + " entry: " + entry.items[i]);
						    	if (i == entry.cur_selection) {
						    		add.setIcon(R.drawable.ic_menu_mark);
						    	}
						    } // entries
						} // has enough entries
					} // visible
				} // for entries
			} // if sub null/else
		} // has filters
		
		m = menu.findItem(R.id.screenshot);
		if (screenshot_view != null) {
			m.setEnabled(true);
			m.setVisible(true);
		} else {
			m.setEnabled(false);
			m.setVisible(false);
		}
		
		return super.onPrepareOptionsMenu(menu);
	}
	
	protected void setWatchlistIcon(boolean on) {
		watchlist_on = on;
		invalidateOptionsMenu();
	}

	protected void setMySquadIcon(boolean on) {
		mysquad_on = on;
		invalidateOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	private boolean menuAction(int id) {
		switch (id) {
	    case android.R.id.home:
	    	if (!(this instanceof Home)) {
		    	Intent intent = new Intent(this, Home.class);
		    	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    	startActivity(intent);
	    	}
	        return true;
	    case R.id.sync:
	    	SlidingDrawer slide = App.slide;
	    	if (slide != null) {
	    		if (slide.isOpened()) {
	    			slide.close();
	    		} else {
	    			slide.open();
	    		}
	    	}
	        return true;
	    case R.id.settings:
	    	Intent intent_s = new Intent(this, Settings.class);
	    	startActivity(intent_s);
	        return true;
	    case R.id.changeview:
	    	if (this instanceof Players) {
	    		Players pl = (Players) this;
	    		pl.changeView();
	    	}
	        return true;
	    case R.id.tips:
	    	showTips();
	    	tipsActive = true;
	        return true;
	    case R.id.about:
	    	Intent intent_a = new Intent(this, About.class);
	    	startActivity(intent_a);
	        return true;
	    case R.id.addrival:
	    	showDialog(DIALOG_ADDRIVAL);
	    	return true;
	    case R.id.joinleague:
	    	showDialog(DIALOG_JOINLEAGUE);
	    	return true;
	    case R.id.watchlist:
	    	if (this instanceof Players) {
	    		Players pl = (Players) this;
	    		pl.toggleWatchlist();
	    	}
	    	return true;
	    case R.id.mysquad:
	    	if (this instanceof Vidiprinter) {
	    		Vidiprinter v = (Vidiprinter) this;
	    		v.toggleMySquad();
	    	}
	    	return true;
	    case R.id.fpl:
	    	if (this instanceof ClickFPL) {
	    		ClickFPL click = (ClickFPL) this;
	    		click.clickFPL();
	    	}
	    	return true;
	    case R.id.screenshot:
	    	if (screenshot_view != null) {
	    		Screenshot.screenshot(this, screenshot_view, screenshot_string, screenshot_max);
	    	}
	    	return true;
	    default:
	    	//App.log("menu onclick: " + id);
	        if (id >= FIDDLE) {
		        int id_fid = id - FIDDLE;
		        //App.log("- fiddle: " + id);
		        int filter_id = id_fid / MAX_ITEMS_IN_LIST;
		        //App.log("filter_id: " + filter_id);
		        FilterEntry fil = filters.get(filter_id);
		        if (fil != null) {
		        	id_fid = id_fid - (MAX_ITEMS_IN_LIST * filter_id);
			        fil.cur_selection = id_fid;
			        filterSelection(filter_id, id_fid);
			        refreshActionMenu();
		        }
	        }
	    	return false;
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    int id = item.getItemId();
		App.log("menu: " + id);
		return menuAction(id);
	}
	
	public void startSyncAnimate() {
		if (sync_anim == null) {
			sync_anim = (RotateAnimation) AnimationUtils.loadAnimation(this, R.anim.rotation);
		}
	    sync_img.startAnimation(sync_anim);
	}
	
	public void stopSyncAnimate() {
		sync_img.clearAnimation();
	}
	
	public void onClick(View v) {
		int id = v.getId();
		menuAction(id);
	}
		
	// can be overridden, but doesn't have to be if not using filters, so not abstract
	protected void filterSelection(int id, int pos) {
		App.log("superclass");
	}
	
	// listener for actionbar dropdown
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		dropdownEntry.cur_selection = itemPosition;
		filterSelection(dropdownEntry.id, itemPosition);
        return true;
    }
	
	protected void onResume() {
    	App.log(this.getLocalClassName() + " onResume()");
		super.onResume();
    	App.initBottom(this);
    	
    	if (hasTips) tipsActive = setupTips();
    	
    	if (wasPaused != NEVER) {
    		if (App.dataUpdatedTime > wasPaused) {
    			if (this instanceof Refreshable) {
    				Refreshable r = (Refreshable) this;
    				r.dataChanged();
    			}
    		}
    	}
    	wasPaused = NEVER;
    	paused = false;
    	
    	if (showDialogOnResume > 0) {
    		showDialog(showDialogOnResume);
    		showDialogOnResume = 0;
    	}
    	
    	checkSdCardWarning();
    	checkVersionWarning();
    }
    
    protected void onPause() {
    	App.log(this.getLocalClassName() + " onPause()");
		App.destroyActivity();
		Worm.clean();
		wasPaused = App.currentUTCTime();
		paused = true;
		super.onPause();
    }
    
    public void clickHide(View v) {
		hideTips();
		tipsActive = false;
    }
    public void clickLeft(View v) {
    	curTip--;
    	if (curTip < 0) curTip = tips.size() - 1;
    	setTip(curTip);
    }
    public void clickRight(View v) {
    	curTip++;
    	if (curTip >= tips.size()) curTip = 0;
    	setTip(curTip);
    }
    
    protected Dialog onCreateDialog(int dialog_id) {
    	if (dialog_id == DIALOG_PROGRESS_NEW) {
			Dialog dialog = new ProgressDialog(this);
			dialog.setCancelable(false);
			return dialog;
		} else if (dialog_id == DIALOG_SD_WARN) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("SD Card Installation");
			builder.setMessage("You have installed Differential to the SD Card. Android places some limitations on apps"
					+ " when you do this, and I do not recommend doing so:-"
					+ " \n\nIf you are using the Differential Widget, then this"
					+ " will not work after removing/replacing the SD Card (or when you mount the SD Card as a mass"
					+ " storage device on your computer - when you plug in using a USB cable).\n\n"
					+ " After doing this, the homescreen must be reset (a phone restart or launcher restart will do this)"
					+ " or the widget must be deleted and re-created on your homescreen.\n\n"
					+ " Any background data/score/player updates will not happen until you manually start Differential"
					+ " after the SD Card has been removed or mounted on your computer as a mass storage device, "
					+ " and the 'Start on boot' option will not work.");
			builder.setCancelable(true);
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                
			           }
			       });
			AlertDialog alert = builder.create();
			return alert;
		} else if (dialog_id == DIALOG_NEW_VERSION) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("New version available");
			builder.setCancelable(true);
			builder.setMessage("");
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                
			           }
			       });
			AlertDialog alert = builder.create();
			return alert;
		} else if (dialog_id == DIALOG_NOTICE) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Notice");
			builder.setCancelable(true);
			builder.setMessage("");
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                
			           }
			       });
			AlertDialog alert = builder.create();
			return alert;
		} else if (dialog_id == DIALOG_GENERIC) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Message");
			builder.setCancelable(true);
			builder.setMessage("");
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                
			           }
			       });
			AlertDialog alert = builder.create();
			return alert;
		} else if (dialog_id == DIALOG_ADDRIVAL) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				// Set an EditText view to get user input 
				final EditText input = new EditText(this);
				input.setHint("Squad ID");
				input.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
				builder.setView(input);
		    	builder.setTitle("Add Rival Manually");
		    	builder.setCancelable(true);
		    	builder.setMessage("Enter squad ID to add a rival manually");
		        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int id) {
		            	try {
		            		Leagues.addRival(Integer.parseInt(input.getText().toString()));
		            	} catch (NumberFormatException e) {
		            		App.log("Invalid squad id for manual rival add: " + input.getText().toString());
		            		if (App.active != null) {
		            			Toast.makeText(App.active, "Invalid squad ID entered", Toast.LENGTH_LONG).show();
		            		}
		            	}
		            	input.setText(null);
		            }
		        });
		        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int id) {
		            	
		            }
		        });
		        Dialog new_dialog = builder.create();
				return new_dialog;
		} else if (dialog_id == DIALOG_JOINLEAGUE) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Join Differential FPL Minileague?");
	    	builder.setCancelable(true);
	    	builder.setMessage("Join Differential FPL Minileague?");
	        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	            	Leagues.joinLeague();
	            }
	        });
	        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	            	
	            }
	        });
	        Dialog new_dialog = builder.create();
			return new_dialog;
	} else {
			return null;
		}
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	if (id == DIALOG_PROGRESS_NEW) {
    		ProgressDialog pd = (ProgressDialog) dialog;
    		pd.setMessage(progress_text);
    	} else if (id == DIALOG_NEW_VERSION) {
    		AlertDialog pd = (AlertDialog) dialog;
    		pd.setMessage(App.notify_new_version);
    		App.notify_new_version = null;
    	} else if (id == DIALOG_NOTICE) {
    		AlertDialog pd = (AlertDialog) dialog;
    		App.log("prepareFPLDialog sertting message for notice: " + App.notice);
    		pd.setMessage(App.notice);
    	} else if (id == DIALOG_GENERIC) {
    		AlertDialog pd = (AlertDialog) dialog;
    		pd.setMessage(generic_dialog_text);
    		pd.setTitle(generic_dialog_title);
    	}
    }
    
    public void showFPLDialog(int id) {
       	if (!paused) {
       		showDialog(id);
       	} else {
       		showDialogOnResume = id;
    	}
    }
	
	public static String generic_dialog_text;
	public static String generic_dialog_title;
	
	private boolean setupTips() {
		boolean showTipsNow = Settings.getBoolPref(tipsPref);
		
		setTip(0);
		if (showTipsNow) {
			return true;
		} else {
			LinearLayout ll = (LinearLayout) findViewById(R.id.tipsview);
			ll.setVisibility(View.GONE);
			return false;
		}
	}
	
	private void setTip(int ind) {
		TextView tip = (TextView) findViewById(R.id.tipstext);
		TextView tipsOf = (TextView) findViewById(R.id.tipsof);
		
		int page = ind + 1;
		int of = tips.size();
		
		tip.setText(tips.get(ind));
		tipsOf.setText(page + " of " + of);
	}
	
	private void hideTips() {
		LinearLayout ll = (LinearLayout) findViewById(R.id.tipsview);
		ll.setVisibility(View.GONE);
		Settings.setBoolPref(tipsPref, false, this);
	}
	
	private void showTips() {
		LinearLayout ll = (LinearLayout) findViewById(R.id.tipsview);
		ll.setVisibility(View.VISIBLE);
		Settings.setBoolPref(tipsPref, true, this);
	}
	
	private void checkSdCardWarning() {
		if (App.need_warn_sd_card) {
			showDialog(DIALOG_SD_WARN);
			
			App.need_warn_sd_card = false;
			Settings.setBoolPref(Settings.PREF_WARNED_SD, true, this);
		}
	}
	
	private void checkVersionWarning() {
		if (App.notify_new_version != null) {
			showDialog(DIALOG_NEW_VERSION);
			Settings.setIntPref(Settings.NOTIFIED_NEW_VERSION, App.notify_new_version_version, this);
		}
		if (App.notice != null) {
			String last_notified = Settings.getStringPref(Settings.LAST_NOTIFIED_NOTICE);
			boolean show = true;
			if (last_notified != null) {
				if (last_notified.equals(App.notice)) {
					show = false;
				}
			}
			if (show) {
				App.log("show notice dialog for: " + App.notice);
				showDialog(DIALOG_NOTICE);
				Settings.setStringPref(Settings.LAST_NOTIFIED_NOTICE, App.notice, this);
			}
		}
	}
	
	protected static final int NEVER = 0;
	
	private View   screenshot_view;
	private String screenshot_string;
	private int    screenshot_max;
	
	protected void setScreenshotView(View v, String s) {
		screenshot_view = v;
		screenshot_string = s;
		screenshot_max = Screenshot.NOT_SET;
		invalidateOptionsMenu();
	}
	
	protected void setScreenshotView(View v, String s, int max) {
		screenshot_view = v;
		screenshot_string = s;
		screenshot_max = max;
		invalidateOptionsMenu();
	}

}
