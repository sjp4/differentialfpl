package com.pennas.fpl.scrape;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import com.pennas.fpl.App;
import com.pennas.fpl.Settings;
import com.pennas.fpl.process.FPLService.ProcRes;
import com.pennas.fpl.util.URLUtil;
import com.pennas.fpl.util.URLUtil.UrlObj;

public class ScrapeVersion {
	private static final String URL_VERSION = "https://sites.google.com/site/differentialfpl/version.txt?attredirects=0&d=1";
	
	public void checkVersion(ProcRes result) {
		UrlObj con = null;
		BufferedReader reader = null;
		InputStreamReader iread = null;
		result.setMaxName(1, "version check");
		
		try {
			// see if we already got the squad for that week (it can change)
			App.log("starting get latest version..");
			long startTime = System.currentTimeMillis();
			
			con = URLUtil.getUrlStream(URL_VERSION, false, URLUtil.NO_AUTH, null, true, true);
			if (con.ok) {
				iread = new InputStreamReader(con.stream, "UTF-8");
			    reader = new BufferedReader(iread, 1024);
			    
			    // read version line
			    String line = reader.readLine();
			    if (line != null) {
			    	//App.log("line:" + line);
			    	
			    	try {
				    	int comma_pos = line.indexOf(',');
				    	if (comma_pos > 0) {
					    	int new_version = Integer.parseInt(line.substring(0, comma_pos));
					    	App.log("New version (int only): " + new_version);
					    	
					    	// mark as checked
						    Settings.setLongPref(Settings.LAST_CHECKED_NEW_VERSION, App.currentUkTime(), result.context);
					    	
					    	if (new_version > Settings.getIntPref(Settings.NOTIFIED_NEW_VERSION)) {
								if (new_version > Settings.getIntPref(Settings.INSTALLED_APP_VERSION)) {
									App.notify_new_version = line.substring(comma_pos + 1);
									App.notify_new_version_version = new_version;
									App.log("New version found: " + new_version + " / " + App.notify_new_version);
								}
							}
				    	}
			    	} catch (NumberFormatException e) {
			    		App.log("Couldn't parse version string");
			    		App.exception(e);
			    	}
			    }
			    
			    // read notice line (if there is one)
			    String notice = reader.readLine();
			    if (notice != null) {
			    	App.notice = notice;
			    	App.log("Notice: " + notice);
			    }
				
				Long timeDiff = System.currentTimeMillis() - startTime;
				float timeSecs = (float)timeDiff / 1000;
				App.log("done get latest version in " + timeSecs + " seconds");
			} else {
				App.log("version: url failed");
				result.setError("url failed");
			}
		} catch (IOException e) {
			App.exception(e);
			result.setError("1 " + e);
		} finally {
			if (reader != null) {
		    	try { 
		    		reader.close();
		    		iread.close();
		    	} catch (IOException e) {
		    		App.exception(e);
		    	}
		    }
			if (con != null) con.closeCon();
		}
		
		//Debug.stopMethodTracing();
		result.setComplete();
	}
}
