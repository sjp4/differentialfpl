package com.pennas.fpl;

import com.pennas.fpl.Selection.ViewPagerAdapter;
import com.pennas.fpl.util.FPLActivity;
import com.viewpagerindicator.TitlePageIndicator;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

public class About extends FPLActivity {
	
	private static final String DIFF_URL = "http://DifferentialFPL.blogspot.com/";
	private static final String TEMPLAY_ICON_URL = "http://www.templay.de";
	private static final String EMAIL_ADDRESS = "differentialfplandroid@gmail.com";
	
	private ViewPager viewPager;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    actionBar.setTitle("About Differential FPL");
		
	    setContentView(R.layout.about_screen);
		
		LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ScrollView versionHist = (ScrollView) vi.inflate(R.layout.about_screen_version_hist, null);
        ScrollView help = (ScrollView) vi.inflate(R.layout.about_screen_help, null);
        ScrollView smallPrint = (ScrollView) vi.inflate(R.layout.about_screen_small_print, null);
		
		viewPager = (ViewPager) findViewById(R.id.viewpager);
		final String[] titles = new String[] { "Version Changes", "Help", "Small Print" };
		final View[] views = new View[] { versionHist, help, smallPrint };
		ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(titles, views);
	    TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.titles);
	    viewPager.setAdapter(pagerAdapter);
	    indicator.setViewPager(viewPager);
	    indicator.setCurrentItem(1);
	    
	    // load about data
	    PackageManager manager = getPackageManager();
		PackageInfo info;
		String version = null;
		try {
			info = manager.getPackageInfo(getPackageName(), 0);
			version = "v" + info.versionName;
		} catch (NameNotFoundException e) {
			App.exception(e);
		}
		
		String androidplot_licence = getString(R.string.androidplot_licence);
		String version_history = getString(R.string.version_history);
		String small_print_string = getString(R.string.small_print);
		String help_string = getString(R.string.help_text);
		
		final TextView history = (TextView) versionHist.findViewById(R.id.history);
        final TextView androidplot = (TextView) smallPrint.findViewById(R.id.androidplot);
        final TextView small_print_text = (TextView) smallPrint.findViewById(R.id.small_print_text);
        final TextView help_text = (TextView) help.findViewById(R.id.help_text);
        final TextView email = (TextView) help.findViewById(R.id.email);
        final TextView diffBlog = (TextView) help.findViewById(R.id.diffBlog);
        
        history.setText(version_history);
        androidplot.setText(androidplot_licence);
        actionBar.setSubtitle(version);
        small_print_text.setText(small_print_string);
        help_text.setText(help_string);
        email.setText(EMAIL_ADDRESS);
        diffBlog.setText(DIFF_URL);
	}
    
	public void clickFPLBlog(View v) {
		Uri uri = Uri.parse(DIFF_URL);
		if (App.active != null) {
			App.active.startActivity(new Intent(Intent.ACTION_VIEW, uri));
		}
    }
	
	public void clickEmail(View v) {
		Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
		String emailList[] = { EMAIL_ADDRESS };
		emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, emailList);
		emailIntent.setType("plain/text");
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Differential FPL Support");  
		startActivity(Intent.createChooser(emailIntent, "Send mail..."));
	}
	
	public void clickTemplay(View v) {
		Uri uri = Uri.parse(TEMPLAY_ICON_URL);
		if (App.active != null) {
			App.active.startActivity(new Intent(Intent.ACTION_VIEW, uri));
		}
    }
	
}
