CREATE TABLE "android_metadata" ("locale" TEXT DEFAULT 'en_US');
INSERT INTO "android_metadata" VALUES ('en_US');

CREATE TABLE team (
	_id 			INTEGER PRIMARY KEY AUTOINCREMENT,
	name	 		TEXT NOT NULL,
	short_name 		TEXT NOT NULL,
	bbc_name		TEXT NOT NULL,
	prem_name		TEXT NOT NULL,
	active	  		INTEGER NOT NULL
	);
	
CREATE TABLE gameweek (
	season 			INTEGER NOT NULL,
	num				INTEGER NOT NULL,
	start_datetime  INTEGER NOT NULL,
	end_datetime  	INTEGER NOT NULL,
	PRIMARY KEY (season, num)
	);

CREATE TABLE fixture (
	_id 			INTEGER PRIMARY KEY AUTOINCREMENT,
	season 			INTEGER NOT NULL,
	gameweek		INTEGER,
	datetime		INTEGER,
	fpl_id			INTEGER,
	team_home_id    INTEGER NOT NULL,
	team_away_id    INTEGER NOT NULL,
	res_goals_home	INTEGER,
	res_goals_away	INTEGER,
	res_points_home	INTEGER,
	res_points_away	INTEGER,
	pred_goals_home DECIMAL(5,2),
	pred_goals_away DECIMAL(5,2),
	pred_points_home INTEGER,
	pred_points_away INTEGER,
	pred_ratio_home  INTEGER,
	pred_ratio_away  INTEGER,
	stats_datetime	 INTEGER NOT NULL,
	got_bonus        INTEGER NOT NULL,
	UNIQUE (season, team_home_id, team_away_id)
	);

CREATE TABLE player (
	_id 			INTEGER PRIMARY KEY AUTOINCREMENT,
	name 			TEXT NOT NULL,
	first_name		TEXT,
	team_id			INTEGER,
	fpl_yellow_flag INTEGER,
	fpl_red_flag  	INTEGER,
	fpl_news		TEXT,
	diff_flag		INTEGER, -- my "unavailable" flag
	picture_code	INTEGER NOT NULL,
	twitter_username TEXT,
	ticker_string   TEXT,
	got_history		INTEGER
	);

CREATE TABLE player_season (
	fpl_id 			INTEGER NOT NULL,
	player_id		INTEGER NOT NULL,
	season			INTEGER NOT NULL,
	position	  	INTEGER NOT NULL,
	price			DECIMAL(5,1),
	price_start		DECIMAL(5,1),
	points			INTEGER NOT NULL,
	minutes			INTEGER NOT NULL,
	minutes_qual	INTEGER,
	goals			INTEGER NOT NULL,
	assists			INTEGER NOT NULL,
	bonus			INTEGER NOT NULL,
	yellow			INTEGER,
	red				INTEGER,
	clean_sheets	INTEGER,
	ppg				DECIMAL(5,2),
	form			DECIMAL(5,2),
	ea_ppi			INTEGER,
	ti_gw			INTEGER,
	to_gw			INTEGER,
	nti_gw			INTEGER,
	c_ea_g_x		DECIMAL(5,2),
	c_ppg_home		DECIMAL(5,2),
	c_ppg_away		DECIMAL(5,2),
	c_ppg_x_mins_home DECIMAL(5,2),
	c_ppg_x_mins_away DECIMAL(5,2),
	c_ppg_x_mins	DECIMAL(5,2),
	c_games_x_mins	INTEGER,
	c_games_left	INTEGER,
	c_avg_mins_recent INTEGER,
	c_games_recent	INTEGER,
	c_games_avl_recent INTEGER,
	c_ppg_x_mins_rec DECIMAL(5,2),
	c_sum_error		INTEGER,
	c_team_goals	INTEGER,
	c_assists_perc_team DECIMAL(5,2),
	c_goals_perc_team   DECIMAL(5,2),
	c_cs_perc_games		DECIMAL(5,2),
	c_bonus_per_game    DECIMAL(5,2),
	c_bonus_per_win     DECIMAL(5,2),
	c_bonus_per_draw    DECIMAL(5,2),
	c_bonus_per_loss    DECIMAL(5,2),
	c_points_per_win    DECIMAL(5,2),
	c_points_per_draw   DECIMAL(5,2),
	c_points_per_loss   DECIMAL(5,2),
	c_perc_games_won    DECIMAL(5,2),
	c_pp90			DECIMAL(5,2),
	c_pp90_home		DECIMAL(5,2),
	c_pp90_away		DECIMAL(5,2),
	c_g_over_y		INTEGER,
	c_g_over_z		INTEGER,
	c_ppg_easy		DECIMAL(5,2),
	c_ppg_med		DECIMAL(5,2),
	c_ppg_hard		DECIMAL(5,2),
	c_flat_track_bully DECIMAL(5,2),
	c_fixture_proof    DECIMAL(5,2),
	c_value_ppg_x	DECIMAL(5,2),
	c_value_pp90	DECIMAL(5,2),
	c_gp90			DECIMAL(5,2),
	c_ap90			DECIMAL(5,2),
	c_emerge		DECIMAL(5,2),
	c_diff			DECIMAL(5,2),
	c_value_diff	DECIMAL(5,2),
	c_highest_score	INTEGER,
	pred_left_goals		DECIMAL(5,2),
	pred_left_assists 	DECIMAL(5,2),
	pred_left_bonus_pts	DECIMAL(5,2),
	pred_left_cs_pts	DECIMAL(5,2),
	pred_left_pts		DECIMAL(5,2),
	pred_left_value		DECIMAL(5,2),
	pred_season_pts		DECIMAL(5,2),
	diff_upcom_fix      DECIMAL(5,2),
	diff_upcom_pred	    DECIMAL(5,2),
	diff_value_fix      DECIMAL(5,2),
	diff_value_pred     DECIMAL(5,2),
	PRIMARY KEY (fpl_id, season),
	UNIQUE (player_id, season)
	);
CREATE INDEX player_season_points_ind ON player_season(points, season, price, position);
CREATE INDEX player_season_minutes_ind ON player_season(season, minutes);

CREATE TABLE player_match (
	season	 		INTEGER NOT NULL,
	player_player_id INTEGER,-- NOT NULL
	fixture_id		INTEGER NOT NULL,
	pl_team_id		INTEGER,-- NOT NULL,
	opp_team_id		INTEGER,-- NOT NULL,
	gameweek		INTEGER,
	goals	  		INTEGER,
	assists			INTEGER,
	bonus			INTEGER,
	minutes			INTEGER,
	total			INTEGER,
	team_goals_on_pitch	INTEGER,
	conceded		INTEGER,
	pen_sav			INTEGER,
	pen_miss		INTEGER,
	yellow			INTEGER,
	red				INTEGER,
	saves			INTEGER,
	own_goals		INTEGER,
	is_home			INTEGER,
	pred_total_pts	INTEGER,
	result_points   INTEGER,
	PRIMARY KEY (season, player_player_id, fixture_id)
	);
----CREATE INDEX player_match_selection_ind ON player_match(season, gameweek, player_fpl_id);
----CREATE INDEX player_match_wdl_ind ON player_match(player_player_id, result_points);
----CREATE INDEX player_match_pl_team_ind ON player_match(season, pl_team_id);
----CREATE INDEX player_match_player_ind ON player_match(season, player_player_id);

CREATE TABLE team_season (
	team_id	 		INTEGER NOT NULL,
	fpl_id			INTEGER NOT NULL,
	season	 		INTEGER NOT NULL,
	games_played	INTEGER,
	goal_diff		INTEGER,
	points			INTEGER,
	c_sum_h_gs		INTEGER,
	c_sum_h_gc		INTEGER,
	c_sum_h_w		INTEGER,
	c_sum_h_d		INTEGER,
	c_sum_h_l		INTEGER,
	c_sum_a_gs		INTEGER,
	c_sum_a_gc		INTEGER,
	c_sum_a_w		INTEGER,
	c_sum_a_d		INTEGER,
	c_sum_a_l		INTEGER,
	c_sum_gs		INTEGER,
	c_sum_gc		INTEGER,
	c_gpg			DECIMAL(5,2),
	c_gcpg			DECIMAL(5,2),
	c_h_wd_perc		DECIMAL(5,2),
	c_h_w_perc		DECIMAL(5,2),
	c_h_rating		INTEGER,
	c_h_form_rating	INTEGER,
	c_h_gpg			DECIMAL(5,2),
	c_h_gcpg		DECIMAL(5,2),
	c_a_wd_perc		DECIMAL(5,2),
	c_a_w_perc		DECIMAL(5,2),
	c_a_rating		INTEGER,
	c_a_form_rating	INTEGER,
	c_a_gpg			DECIMAL(5,2),
	c_a_gcpg		DECIMAL(5,2),
	c_sum_games_played INTEGER,
	c_sum_goal_diff	INTEGER,
	c_sum_points	INTEGER,
	c_sum_error		INTEGER,
	c_bonus_per_game DECIMAL(5,2),
	c_cs_perc 		DECIMAL(5,2),
	c_cs_home		INTEGER,
	c_cs_away		INTEGER,
	c_cs			INTEGER,
	pred_games		INTEGER,
	pred_points 	INTEGER,
	pred_goal_diff  INTEGER,
	gk_player_id	INTEGER,
	PRIMARY KEY (team_id, season)
	);
	
CREATE TABLE updated (
	data				INTEGER NOT NULL,
	updated_datetime 	INTEGER,
	PRIMARY KEY (data)
	);
	
CREATE TABLE stat (
	db_field  TEXT NOT NULL,
	type	  INTEGER NOT NULL,
	short_lab TEXT NOT NULL,
	desc	  TEXT NOT NULL,
	show_value INTEGER NOT NULL,
	always_show_sort  INTEGER NOT NULL,
	player_stat INTEGER NOT NULL,
	divide INTEGER
	);
CREATE INDEX stat_player_ind ON stat(player_stat);

CREATE TABLE stat_season (
	season		INTEGER NOT NULL,
	stat_id		TEXT NOT NULL,
	min_games	INTEGER NOT NULL,
	max_value	DECIMAL(5,2),
	player_id	INTEGER NOT NULL,
	plus		INTEGER NOT NULL,
	PRIMARY KEY (season, stat_id, min_games)
	);

CREATE TABLE generic_stat (
	season	INTEGER NOT NULL,
	id		TEXT NOT NULL,
	value	DECIMAL(5,2),
	PRIMARY KEY (season, id)
	);
	
CREATE TABLE team_rotation (
	team_a	INTEGER NOT NULL,
	team_b 	INTEGER NOT NULL,
	cs_perc DECIMAL(5,2) NOT NULL,
	gameweeks_home INTEGER NOT NULL,
	clean_sheets	INTEGER NOT NULL,
	ticker_string  TEXT NOT NULL,
	cost	DECIMAL(5,1) NOT NULL,
	PRIMARY KEY (team_a, team_b)
	);