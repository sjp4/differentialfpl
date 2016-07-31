package com.pennas.fpl.scrape;

import java.io.IOException;
import java.util.Scanner;
import java.util.regex.MatchResult;
import com.pennas.fpl.App;
import com.pennas.fpl.process.FPLService.ProcRes;
import com.pennas.fpl.util.URLUtil;
import com.pennas.fpl.util.URLUtil.UrlObj;

// just grabs points total for current match from player api hist - used for
// distinguishing between players with the same name
public class ScrapePlayerLiveStats {
	
	public static class LiveStatsRet {
		boolean found;
		int ptot;
	}
	
	public static LiveStatsRet getPlayerLiveStats(int player_fpl_id, ProcRes result, int gameweek, int opp_id) {
		Scanner scan = null;
		UrlObj con = null;
		LiveStatsRet ret = new LiveStatsRet();
		
		try {
			con = URLUtil.getUrlStream(ScrapePlayers.URL_PLAYER_HISTORY + player_fpl_id + "/", false, URLUtil.NO_AUTH);
		
			if (con.ok) {
				scan = new Scanner(con.stream);
			
				// team (player scores inside match)
				while (!ret.found && scan.findWithinHorizon(ScrapeMatchScoresCatchup.p_match_hist, 0) != null) {
					MatchResult m = scan.match();
					String team_short_name = ScrapeMatchScoresCatchup.getString(m, ScrapeMatchScoresCatchup.H_OPP);
					
					// get data from scrape into structure
					int gw = ScrapeMatchScoresCatchup.getInt(m, ScrapeMatchScoresCatchup.H_GW);
					
					if (gw == gameweek) {
						int opp_team_id = App.team_short_2id.get(team_short_name);
						//String oppname = App.id2team.get(opp_team_id);
						ret.ptot = ScrapeMatchScoresCatchup.getInt(m, ScrapeMatchScoresCatchup.H_POINTS);
						
						if (opp_team_id == opp_id) {
							ret.found = true;
							break;
						}
					}
				}
			} else {
				App.log("player hist: url failed");
				if (con.updating) {
					result.updating = true;
				}
			}
		} catch (IOException e) {
			App.exception(e);
		} finally {
			if (scan != null) scan.close();
			try {
				if (con!=null && con.stream != null) con.stream.close();
			} catch (Exception e) {
				App.exception(e);
			}
			if (con != null) con.closeCon();
		}
		
		return ret;
	}
	
}
