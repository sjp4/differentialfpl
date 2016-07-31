-- arsenal: Szczesny
UPDATE team_season SET gk_player_id = 582 WHERE team_id = 1;
-- villa: Given
UPDATE team_season SET gk_player_id = 234 WHERE team_id = 2;
-- blackburn: Robinson
UPDATE team_season SET gk_player_id = 76 WHERE team_id = 4;
-- bolton: Jaaskelainen
UPDATE team_season SET gk_player_id = 114 WHERE team_id = 6;
-- chelsea: Cech
UPDATE team_season SET gk_player_id = 137 WHERE team_id = 7;
-- everton: Howard
UPDATE team_season SET gk_player_id = 164 WHERE team_id = 8;
-- fulham: Schwarzer
UPDATE team_season SET gk_player_id = 187 WHERE team_id = 9;
-- liverpool: Reina
UPDATE team_season SET gk_player_id = 210 WHERE team_id = 10;
-- man city: Hart
UPDATE team_season SET gk_player_id = 235 WHERE team_id = 11;
-- man utd: de Gea
UPDATE team_season SET gk_player_id = 680 WHERE team_id = 12;
-- newcastle: Harper
UPDATE team_season SET gk_player_id = 290 WHERE team_id = 13;
-- norwich: Ruddy
UPDATE team_season SET gk_player_id = 683 WHERE team_id = 21;
-- qpr: Kenny
UPDATE team_season SET gk_player_id = 707 WHERE team_id = 22;
-- stoke: Begovic
UPDATE team_season SET gk_player_id = 313 WHERE team_id = 14;
-- sunderland: Mignolet
UPDATE team_season SET gk_player_id = 336 WHERE team_id = 15;
-- swansea: Moreira
UPDATE team_season SET gk_player_id = 761 WHERE team_id = 23;
-- tottenham: Friedel
UPDATE team_season SET gk_player_id = 31 WHERE team_id = 16;
-- west brom: Myhill (until Foster added)
UPDATE team_season SET gk_player_id = 501 WHERE team_id = 17;
-- wigan: Al-Habsi
UPDATE team_season SET gk_player_id = 113 WHERE team_id = 19;
-- wolves: Hennessey
UPDATE team_season SET gk_player_id = 457 WHERE team_id = 20;
 
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("minutes", 1, "mins", "Minutes", 1, 1, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("goals", 1, "goal", "Goals", 1, 0, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("assists", 1, "ass", "Assists", 1, 0, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("bonus", 1, "bon", "Bonus Pts", 1, 0, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("yellow", 1, "yel", "Yellow Cards", 1, 0, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("ppg", 1, "ppg", "PPG", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("form", 1, "form", "Form", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("owned_perc", 1, "own", "Ownership %", 1, 0, 1, 10);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_ppg_home", 2, "ppgh", "PPG Home", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_ppg_away", 2, "ppga", "PPG Away", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_ppg_x_mins_home", 2, "ppxh", "PPG Played 60 Mins Home", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_ppg_x_mins_away", 2, "ppxa", "PPG Played 60 Mins Away", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_ppg_x_mins", 2, "ppgx", "PPG Played 60 Mins", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_games_x_mins", 2, "gamx", "Games Played 60 Mins", 1, 0, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_games_left", 2, "gaml", "Games Left", 1, 0, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_avg_mins_recent", 2, "avgmi", "Average Minutes Recent", 1, 0, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_games_recent", 2, "gamr", "Games Recent", 1, 0, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_ppg_x_mins_rec", 2, "ppxr", "PPG Played 60 Mins Recent", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_team_goals", 2, "teamg", "Team Goals While on Pitch", 1, 0, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_goals_perc_team", 2, "goal%", "% of Team Goals While on Pitch", 1, 0, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_assists_perc_team", 2, "ass%", "% of Team Assists While on Pitch", 1, 0, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_cs_perc_games", 2, "cs%", "Clean Sheet Games %", 1, 0, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_bonus_per_game", 2, "bon", "Bonus Per Game", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_bonus_per_win", 2, "bonw", "Bonus Per Win", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_bonus_per_draw", 2, "bond", "Bonus Per Draw", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_bonus_per_loss", 2, "bonl", "Bonus Per Loss", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_pp90", 2, "pp90", "PP90 Points Per 90 Minutes", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_pp90_home", 2, "p90h", "PP90 Home", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_pp90_away", 2, "p90a", "PP90 Away", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_g_over_y", 2, "go", "Games Scored Over ", 1, 0, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_g_over_z", 2, "go", "Games Scored Over ", 1, 0, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("pred_left_goals", 3, "goaL", "Predicted Goals Left", 0, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("pred_left_assists", 3, "assL", "Predicted Assists Left", 0, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("pred_left_bonus_pts", 3, "bonL", "Predicted Bonus Left", 0, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("pred_left_cs_pts", 3, "cspL", "Predicted CS Pts Left", 0, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("pred_left_pts", 3, "poiL", "Predicted Points Left", 0, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("pred_left_value", 3, "valL", "Predicted Value Left", 3, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("pred_season_pts", 3, "poiP", "Predicted Season Points", 0, 1, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("price_start", 1, "prSt", "Season Starting Price", 1, 0, 1, 10);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("diff_upcom_fix", 3, "diff", "Upcoming Fixtures Rating", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("diff_upcom_pred", 3, "pred", "Upcoming Score Rating", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("diff_value_fix", 3, "fval", "Upcoming Fixture Value", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("diff_value_pred", 3, "pval", "Upcoming Score Value", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_ppg_easy", 3, "ppge", "PPG in Easy Games", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_ppg_med", 3, "ppgm", "PPG in Medium Games", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_ppg_hard", 3, "ppgh", "PPG in Hard Games", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_flat_track_bully", 3, "ftp", "Flat Track Bully", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_fixture_proof", 3, "fp", "Fixture-Proof", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_value_ppg_x", 3, "vpx", "Value (PPG Played 60 Minutes)", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_value_pp90", 3, "vp90", "Value (PP90 Points Per 90 Minutes)", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_highest_score", 3, "hs", "Highest Gameweek Score", 1, 0, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_gp90", 2, "gp90", "Goals Per 90 Minutes", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_ap90", 2, "ap90", "Assists Per 90 Minutes", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_emerge", 2, "emg", "Emerging", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_diff", 2, "diff", "Differential Rating", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_value_diff", 2, "diff", "Differential Value", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("ea_ppi", 2, "eai", "EA PPI", 1, 0, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_ea_g_x", 2, "eax", "EA PPI played 60 Minutes", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_points_per_win", 2, "ppw", "Points Per Win", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_points_per_draw", 2, "ppd", "Points Per Draw", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_points_per_loss", 2, "ppl", "Points Per Loss", 1, 0, 1, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("ti_gw", 1, "tigw", "Transfers In Gameweek", 1, 0, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("to_gw", 1, "togw", "Transfers Out Gameweek", 1, 0, 1);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("nti_gw", 1, "nti", "Net Transfers In Gameweek", 1, 0, 1);

INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_h_w_perc", 2, "hw", "Home Games Won %", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_h_wd_perc", 2, "hwd", "Home Games Won/Drawn %", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_h_rating", 2, "hr", "Home Rating", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_h_form_rating", 2, "hfr", "Home Form Rating", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_h_gpg", 2, "hgp", "Home Goals Per Game", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_h_gcpg", 2, "hgc", "Home Goals Conceded Per Game", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_a_w_perc", 2, "aw", "Away Games Won %", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_a_wd_perc", 2, "awd", "Away Games Won/Drawn %", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_a_rating", 2, "ar", "Away Rating", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_a_form_rating", 2, "afr", "Away Form Rating", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_a_gpg", 2, "agp", "Away Goals Per Game", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_a_gcpg", 2, "agc", "Away Goals Conceded Per Game", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("games_played", 2, "gp", "Games Played", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("goal_diff", 2, "gd", "Goal Difference", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("points", 2, "pts", "Points", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_bonus_per_game", 2, "bpg", "Bonus Points Per Game", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("pred_points", 3, "psp", "Predicted Season Points", 0, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_gpg", 2, "gpg", "Goals Per Game", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_gcpg", 2, "gcp", "Goals Conceded Per Game", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_sum_gs", 2, "gs", "Goals Scored", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_sum_gc", 2, "gc", "Goals Conceded", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_cs_perc", 2, "cs%", "Clean Sheet %", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_cs_home", 2, "csh", "Clean Sheets Home", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_cs_away", 2, "csa", "Clean Sheets Away", 1, 0, 0);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_cs", 2, "cs", "Clean Sheets", 1, 0, 0);

INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("bonus_w", 2, "bw", "Bonus Points When Winning", 1, 0, 2);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("bonus_d", 2, "bd", "Bonus Points When Drawing", 1, 0, 2);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("bonus_l", 2, "bl", "Bonus Points When Losing", 1, 0, 2);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("cs_perc_home", 2, "csh%", "Clean Sheet % Home", 1, 0, 2);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("cs_perc_away", 2, "csa%", "Clean Sheet % Away", 1, 0, 2);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("cs_perc", 2, "cs%", "Clean Sheet %", 1, 0, 2);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("bonus_gk", 2, "bgk", "Bonus Points For Goalkeepers", 1, 0, 2);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("bonus_df", 2, "bdf", "Bonus Points For Defenders", 1, 0, 2);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("bonus_mf", 2, "bmf", "Bonus Points For Midfielders", 1, 0, 2);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("bonus_st", 2, "bst", "Bonus Points For Strikers", 1, 0, 2);

INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_lowest_week", 2, "lw", "Lowest GW Score", 1, 0, 3);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_highest_week", 2, "hw", "Highest GW Score", 1, 0, 3);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_avg_week", 2, "aw", "Average GW Score", 1, 0, 3);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("bank", 2, "ban", "Bank", 1, 0, 3);
 
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_points", 2, "", "Points", 1, 0, 4);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_form", 2, "", "Form", 1, 0, 4, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_hits", 2, "", "Points Hits", 1, 0, 4);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_transfers", 2, "", "Transfers", 1, 0, 4);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_bench", 2, "", "Bench Points", 1, 0, 4);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_goals", 2, "n", "Goals", 1, 0, 4);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_assists", 2, "", "Assists", 1, 0, 4);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_cs", 2, "", "Clean Sheets", 1, 0, 4);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_lowest_week", 2, "", "Lowest Gameweek Score", 1, 0, 4);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_highest_week", 2, "", "Highest Gameweek Score", 1, 0, 4);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_avg_week", 2, "", "Average Score", 1, 0, 4, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_captain_points", 2, "", "Captain Points", 1, 0, 4);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat, divide)
 VALUES ("c_captain_percent", 2, "", "Captain Selection Rating %", 1, 0, 4, 100);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("bank", 2, "", "Bank", 1, 0, 4);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_autosubs", 2, "", "Autosubs", 1, 0, 4);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_autosub_points", 2, "", "Autosub Points", 1, 0, 4);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("c_transfer_gains", 2, "", "Transfer GW Gains", 1, 0, 4);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("value", 2, "", "Value", 1, 0, 4);
INSERT INTO stat (db_field, type, short_lab, desc, show_value, always_show_sort, player_stat)
 VALUES ("bankvalue", 2, "", "Total Value", 1, 0, 4);

UPDATE player_match
SET opp_team_id = (SELECT fixture.team_away_id FROM fixture WHERE fixture._id = player_match.fixture_id)
WHERE is_home = 1;
UPDATE player_match
SET opp_team_id = (SELECT fixture.team_home_id FROM fixture WHERE fixture._id = player_match.fixture_id)
WHERE is_home = 0;

UPDATE player_match
SET pl_team_id = (SELECT fixture.team_home_id FROM fixture WHERE fixture._id = player_match.fixture_id)
WHERE is_home = 1;
UPDATE player_match
SET pl_team_id = (SELECT fixture.team_away_id FROM fixture WHERE fixture._id = player_match.fixture_id)
WHERE is_home = 0;

UPDATE player_match
SET result_points = (SELECT fixture.res_points_home FROM fixture WHERE fixture._id = player_match.fixture_id)
WHERE is_home = 1;
UPDATE player_match
SET result_points = (SELECT fixture.res_points_away FROM fixture WHERE fixture._id = player_match.fixture_id)
WHERE is_home = 0;

UPDATE player SET team_id = NULL;

UPDATE player_match SET player_player_id = 
(SELECT player_id FROM player_season WHERE season = player_match.season AND fpl_id = player_match.player_fpl_id);

-- opt player_match
update player_match set goals = null where goals = 0;
update player_match set assists = null where assists = 0;
update player_match set bonus = null where bonus = 0;
update player_match set minutes = null where minutes = 0;
update player_match set total = null where total = 0;
update player_match set team_goals_on_pitch = null where team_goals_on_pitch = 0;
update player_match set conceded = null where conceded = 0;
update player_match set pen_sav = null where pen_sav = 0;
update player_match set pen_miss = null where pen_miss = 0;
update player_match set yellow = null where yellow = 0;
update player_match set red = null where red = 0;
update player_match set saves = null where saves = 0;
update player_match set own_goals = null where own_goals = 0;
update player_match set pred_total_pts = null where pred_total_pts = 0;

delete from player_match where (season = 12 and gameweek <= 5) and (minutes is null or minutes < 1);
delete from player_match where season < 12 and (minutes is null or minutes < 1);
delete from player_match where player_player_id not in (select player_id from player_season where season = 12);

update player_match set team_goals_on_pitch = round(team_goals_on_pitch) where team_goals_on_pitch is not null;
update player_match set pred_total_pts = null where season = 11;
update player_match set pred_total_pts = round(pred_total_pts * 100) where pred_total_pts is not null;

update player_match set is_home = null where is_home = 0;

update player_match set pred_total_pts = null where season < 12;

update player set ticker_string = null where not exists (select 1 from player_season where season = 12 and player_id = _id);
