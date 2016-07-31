package com.pennas.fpl.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import android.os.Build;

import com.pennas.fpl.App;
import com.pennas.fpl.Settings;

public class URLUtil {
	private static final String GZIP = "gzip";
	private static final String DEFLATE = "deflate";
	private static final String GZIP_DEFLATE = "gzip,deflate";
	//private static final String REFERER = "Referer";
	private static final String ACCEPT_ENCODING = "Accept-Encoding";
	private static final String X_REQUESTED_WITH = "X-Requested-With";
	private static final String X_CSRF_TOKEN = "X-CSRFToken";
	private static final String XML_HTTP_REQUEST = "XMLHttpRequest";
	//private static final String USER_AGENT = "User-Agent";
	//private static final String CONNECTION = "connection";
	//private static final String CLOSE = "close";
	private static final String SET_COOKIE = "Set-Cookie";
	private static final int HTTP_OK = 200;
	public static final int HTTP_REDIR = 302;
	public static final int HTTP_MOVED_PERM= 301;
	
	//private static final String POST_USER = "user_name";
	//private static final String POST_PASSWORD = "password";
	//private static final String HIDDEN_MISC = "FPL|login_cb";
	//private static final String HIDDEN_NEXTURL = "NextPage";
	//private static final String NEXTURL_VAL = "/M/myteam.mc";
	
	private static final String COOKIE_PLUSER = "pluser";
	//private static final String COOKIE_ISMFAPL = "ismfapl";
	private static final String COOKIE_ISMFAPL_MOBILE = "ismfapl_mobile";
	private static final String COOKIE_CSRFTOKEN = "csrftoken";
	
	// 5 seconds timeout
	private static final int URL_TIMEOUT = 10000;
	private static final int DEFAULT_ATTEMPTS = 2;
	private static final int TOTAL_FAIL = -1;
	
	public static final int NO_AUTH = -1;
	public static final int AUTH_FULL = 0;
	public static final int AUTH_MOB = 1;
	
	private static String mob_cookie;
	private static String pluser_cookie;
	private static ArrayList<String> full_cookies = new ArrayList<String>();
	
	private static final String LOGIN_URL_FULL = "http://fantasy.premierleague.com/accounts/login/";
	private static final String LOGIN_URL_MOB = "http://m.fantasy.premierleague.com/M/login.mc";
	
	private static final String UPDATING = "/updating";
	
	public static void logOut() {
		mob_cookie = null;
		pluser_cookie = null;
		full_cookies.clear();
	}
	
	private static boolean doneDisableReuse = false;
	private static void disableConnectionReuseIfNecessary() {
	    if (doneDisableReuse) {
	    	return;
	    }
		doneDisableReuse = true;
	    
		// HTTP connection reuse which was buggy pre-froyo
	    if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
	        App.log("setting http.keepAlive = false (pre-froyo)");
	    	System.setProperty("http.keepAlive", "false");
	    }
	}
	
	// wrapped but available to clients who want to post
	public static UrlObj getUrlStream(String urlString, boolean xmlRequest, int use_auth
			, String post, boolean allow302, boolean allowRedir) throws IOException {
		return getUrlStream(urlString, xmlRequest, use_auth, post, null, allow302, allowRedir, false, false);
	}
	public static UrlObj getUrlStream(String urlString, boolean xmlRequest, int use_auth, String post
			, String token, boolean allow302, boolean followRedir, boolean getToken, boolean put) throws IOException {
		// work-around android bug on 2.1
		disableConnectionReuseIfNecessary();
		
		// attempt to log in
		if (use_auth != NO_AUTH) {
			// if already got auth for type don't bother
			if ( (use_auth == AUTH_MOB) && (mob_cookie != null) ) {
				// nout
			} else if ( (use_auth == AUTH_FULL) && (full_cookies.size() > 0) ) {
				// nout
			} else {
				String authUrl;
				UrlObj auth = new UrlObj();
				if (use_auth == AUTH_FULL) {
					authUrl = LOGIN_URL_FULL;
					boolean stage1 = getPLUserCookie();
					if (!stage1) {
						App.log("stage 1 auth fail");
						auth.auth_fail = true;
						return auth;
					}
				} else {
					authUrl = LOGIN_URL_MOB;
				}
				auth = getUrlStreamCore(authUrl, false, use_auth, NO_AUTH, DEFAULT_ATTEMPTS, null, null, true, false, false, false);
				if (auth.stream != null) auth.stream.close();
				auth.closeCon();
				// if login failure, return that
				if (!auth.ok) {
					App.log("stage 2 auth fail");
					auth.auth_fail = true;
					return auth;
				}
			}
		}
		// login worked, open requested page
		return getUrlStreamCore(urlString, xmlRequest, NO_AUTH, use_auth, DEFAULT_ATTEMPTS, post, token, allow302, followRedir, getToken, put);
	}
	
	// main function called by clients
	public static UrlObj getUrlStream(String urlString, boolean xmlRequest, int use_auth) throws IOException {
		return getUrlStream(urlString, xmlRequest, use_auth, null, false, false);
	}
	
	public static class UrlObj {
		public String orig_url;
		public boolean ok;
		public boolean auth_fail;
		public InputStream stream;
		public int resp_code;
		public String final_url;
		public boolean updating;
		public String token;
		
		HttpURLConnection conn;
		public void closeCon() {
			/*
			if (conn == null) {
				App.log("NPE closing UrlCon");
			} else {
				//App.log("streamcore closing con: " + orig_url);
				conn.disconnect();
			}
			*/
		}
	}
	
	private static String PLUserUrl = "https://users.premierleague.com/PremierUser/restLogin";
	
	public static boolean getPLUserCookie() {
		String username = Settings.getStringPref(Settings.PREF_FPL_EMAIL);
		String password = Settings.getStringPref(Settings.PREF_FPL_PASSWORD);
		int resp = 0;
		App.log("getPLUserCookie");
		
		if ( (username == null) || (password == null) ) {
			App.log("Username/password not set");
			return false;
		}
		
		try {
			String buildUrl = PLUserUrl + "?email=" + URLEncoder.encode(username, "UTF-8") + "&password=" + URLEncoder.encode(password, "UTF-8");
			//AppClass.log(buildUrl);
			URL url = new URL(buildUrl);
			
			HttpURLConnection urlConnection;
			HttpURLConnection.setFollowRedirects(false);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setUseCaches(false);
			urlConnection.setConnectTimeout(URL_TIMEOUT);
			urlConnection.setReadTimeout(URL_TIMEOUT);
			
			String headerName = null;
			
			resp = urlConnection.getResponseCode();
			
			for (int i=1; (headerName = urlConnection.getHeaderFieldKey(i))!=null; i++) {
			 	if (headerName.equalsIgnoreCase(SET_COOKIE)) {   
			 		String cookie = urlConnection.getHeaderField(i);
			 		// only store required cookie
			 		if (cookie.startsWith(COOKIE_PLUSER)) {
				 		cookie = cookie.substring(0, cookie.indexOf(";"));
				 		pluser_cookie = cookie;
				 		//AppClass.log("add cookie: " + cookie);
				 		App.log("got plUserCookie");
				 		return true;
			 		}
			 		
			 	}
			}
		} catch (MalformedURLException e) {
			App.exception(e);
		} catch (UnsupportedEncodingException e) {
			App.exception(e);
		} catch (IOException e) {
			App.exception(e);
		}
		
		App.log("resp: " + resp);
		return false;
	}
	
	private static UrlObj getUrlStreamCore(String urlString, boolean xmlRequest, int get_auth, int use_auth
			, int attempts, String post, String token, boolean allow302, boolean followRedir
			, boolean getToken, boolean put) throws IOException {
		App.log("getUrlStreamCore " + urlString + " get_auth: " + get_auth + " use_auth: " + use_auth);
		URL url = new URL(urlString);
		HttpURLConnection.setFollowRedirects(followRedir);
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		
		UrlObj ret = new UrlObj();
		ret.orig_url = urlString;
		ret.auth_fail = false;
		ret.ok = true;
		
		urlConnection.setRequestProperty(ACCEPT_ENCODING, GZIP_DEFLATE);
		//urlConnection.setIfModifiedSince(newValue);
		urlConnection.setUseCaches(false);
		urlConnection.setConnectTimeout(URL_TIMEOUT);
		urlConnection.setReadTimeout(URL_TIMEOUT);
		
		if (put) {
			App.log("PUT");
			urlConnection.setRequestMethod("PUT");
		}
		
		//urlConnection.setRequestProperty(CONNECTION, CLOSE);
		
		// tesing for fix new
		if (xmlRequest) {
			urlConnection.setRequestProperty(X_REQUESTED_WITH, XML_HTTP_REQUEST);
		}
		//urlConnection.setRequestProperty(USER_AGENT, "User-Agent=Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.18) Gecko/20110614 Firefox/3.6.18");
		
		if (use_auth == AUTH_MOB) {
			if (mob_cookie != null) {
				//AppClass.log("setting cookie: " + mob_cookie);
			    urlConnection.setRequestProperty("Cookie", mob_cookie);
		    } 
		} else if (use_auth == AUTH_FULL) {
			if ( (pluser_cookie != null) && (full_cookies.size() > 0) ) {
				StringBuffer cookies = new StringBuffer(pluser_cookie);
		    	//App.log("setting cookie: " + pluser_cookie);
		    	for (String s : full_cookies) {
		    		//App.log("setting cookie: " + s);
			    	cookies.append("; " + s);
		    	}
		    	if (token != null) {
		    		cookies.append("; csrftoken=" + token);
		    		if (xmlRequest) {
		    			urlConnection.setRequestProperty(X_CSRF_TOKEN, token);
		    		}
		    		//App.log("setting cookie: csrftoken=" + token);
		    	}
		    	//App.log("cookies: " + cookies);
		    	urlConnection.setRequestProperty("Cookie", cookies.toString());
	    	}
	    }
		
		if (get_auth == AUTH_FULL) {
			if (pluser_cookie != null) {
				//AppClass.log("setting cookie: " + pluser_cookie);
			    urlConnection.setRequestProperty("Cookie", pluser_cookie);
			}
		}
		
		/*
		for (String key : urlConnection.getRequestProperties().keySet()) {
			App.log("key: " + key);
			for (String value : urlConnection.getRequestProperties().get(key)) {
				App.log("     value: " + value);
			}
		}
		*/
		
		if (get_auth == AUTH_MOB) {
			String username = Settings.getStringPref(Settings.PREF_FPL_EMAIL);
			String password = Settings.getStringPref(Settings.PREF_FPL_PASSWORD);
			
			if ( (username == null) || (password == null) ) {
				ret.ok = false;
				ret.auth_fail = true;
				App.log("Username/password not set");
				return ret;
			}
			
			// post login details
			String data = "user_name=" + username + "&password=" + password + "&submit=Go&FPL%7Clogin_cb=1";
			
			App.log(data);
			
			urlConnection.setDoOutput(true);
			OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
		    wr.write(data);
		    wr.flush();
		    wr.close();
		} else {
			if (post != null) {
				urlConnection.setDoOutput(true);
				OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
			    App.log("posting: " + post);
			    //for (String p : post.split("&")) AppClass.log("   post item: " + p);
				wr.write(post);
			    wr.flush();
			    wr.close();
			}
		}
		
		try {
			String encoding = urlConnection.getContentEncoding();
			int resp = urlConnection.getResponseCode();
			
			// encoding == null here causing problems (extra retries which actually work once the retry
			// count runs out
			//if ( (resp == TOTAL_FAIL) || ((encoding == null) && !allow302) ) {
			if (resp == TOTAL_FAIL) {
				// retry if failed in a certain (bad) way.
				// this is a work-around for shitty android bug
				// http://code.google.com/p/android/issues/detail?id=7786
				if (attempts > 1) {
					App.log("RETRY URL.." + urlString + " (resp = " + resp + " encoding = " + encoding
							+ " message = " + urlConnection.getResponseMessage() + ")");
					return getUrlStreamCore(urlString, xmlRequest, get_auth, use_auth, attempts-1, post, token, allow302
							, followRedir, getToken, put);
				} else {
					// (else... just return error below)
					App.log("Would have retried if any were left");
				}
			}
			
			App.log("url size: " + urlConnection.getContentLength() + " / encoding: " + encoding + " / resp code: " + resp);
			
			if ((encoding != null) && (encoding.equals(GZIP))) {
				ret.stream = new GZIPInputStream(urlConnection.getInputStream());
			}
			else if ((encoding != null) && (encoding.equals(DEFLATE))) {
				ret.stream = new InflaterInputStream(urlConnection.getInputStream(), new Inflater(true));
			}
			else {
				ret.stream = urlConnection.getInputStream();
			}
			
			ret.conn = urlConnection;
			ret.final_url = urlConnection.getURL().toString();
			App.log("final URL: " + ret.final_url);
			ret.resp_code = resp;
			
			// get csrf token if required
			if (getToken) {
				String headerName = null;
				for (int i=1; (headerName = urlConnection.getHeaderFieldKey(i))!=null; i++) {
				 	if (headerName.equalsIgnoreCase(SET_COOKIE)) {   
				 		String cookie = urlConnection.getHeaderField(i);
				 		// only store required cookie
				 		if (cookie.startsWith(COOKIE_CSRFTOKEN)) {
					 		cookie = cookie.substring(0, cookie.indexOf(";"));
					 		App.log("cookie: " + cookie);
					 		cookie = cookie.substring(cookie.indexOf("=") + 1);
					 		App.log("got csrf: " + cookie);
					 		ret.token = cookie;
				 		}
				 	}
				}
			}
			
			if (get_auth != NO_AUTH) {
				ret.ok = false;
				
				String headerName = null;
				for (int i=1; (headerName = urlConnection.getHeaderFieldKey(i))!=null; i++) {
				 	if (headerName.equalsIgnoreCase(SET_COOKIE)) {   
				 		String cookie = urlConnection.getHeaderField(i);
				 		cookie = cookie.substring(0, cookie.indexOf(";"));
				 		//AppClass.log("found cookie: " + cookie);
				 		
				 		if (get_auth == AUTH_FULL) {
				 			//AppClass.log("add cookie: " + cookie);
				 			full_cookies.add(cookie);
				 			ret.ok = true;
				 		} else {
				 			// only store required cookie
					 		if (cookie.startsWith(COOKIE_ISMFAPL_MOBILE)) {
						 		//AppClass.log("add cookie: " + cookie);
						 		mob_cookie = cookie;
						 		ret.ok = true;
					 		}
				 		}
				 		
				 	}
				}
			// not getting auth
			} else {
				// either updating or auth has expired
				if (ret.final_url.contains(UPDATING) || ( (resp == HTTP_REDIR) && (!allow302) ) ) {
					ret.ok = false;
					ret.updating = true;
					logOut();
					App.log("Game is updating");
				} else if ( (use_auth == AUTH_FULL) && (get_auth == NO_AUTH) && (resp == HTTP_REDIR) ) {
					ret.auth_fail = true;
					full_cookies.clear();
					pluser_cookie = null;
				} else if ( (resp == HTTP_OK) 
						|| ( (resp == HTTP_REDIR) && (allow302) ) 
						|| (resp == HTTP_MOVED_PERM) ) {
					ret.ok = true;
				} else {
					ret.ok = false;	
				}
			}
			
			if (ret.ok) {
				App.log("URL success: " + resp + ": " + urlConnection.getResponseMessage());
			} else {
				App.log("URL fail: " + resp + ": " + urlConnection.getResponseMessage());
			}
		// catch dodgy exception (should be IOException:
		// http://gitorious.org/ginger/libcore/commit/c6dae581716b9362a5c7f166c80a7f2b46ed1124/diffs?diffmode=sidebyside&fragment=1 )
		} catch (IllegalStateException e) {
			throw new IOException(e.toString());
		}
		
		return ret;
	}
	
	private static final String URL_TOKEN = "http://m.fantasy.premierleague.com/my-team/";
	
	// get CSRF token. or use on things other than transfers (when it is recieved anyway)
	public static String getToken() {
		try {
			UrlObj token = getUrlStream(URL_TOKEN, false, AUTH_FULL, null, null, false, false, true, false);
			return token.token;
		} catch (IOException e) {
			return null;
		}
	}
	
	public static String dumpStream(InputStream stream) {
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"), 1024);
			String line = reader.readLine();
			while (line != null) {
				sb.append(line + "\n");
				line = reader.readLine();
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return sb.toString();
	}
}
