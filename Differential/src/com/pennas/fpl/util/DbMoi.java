package com.pennas.fpl.util;

import com.pennas.fpl.App;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbMoi extends SQLiteOpenHelper{
	public static final String DATABASE_NAME = "DiffMoi";
	private static final int DATABASE_VERSION = 13;
	// v13 add cup opponent to sgw (1.9.0)
	// v12 add value, bankvalue to squad (1.8.4)
	// v11 add current_gw_pts to minileague_team (1.8.4)
	// v10 clean slate for 2012/13
	
	public DbMoi(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		App.log("creating DiffMoi");
		
		create_dbMoi_tables(db);
	}
	
	public static void create_dbMoi_tables(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE squad ("
				 + "_id 			INTEGER PRIMARY KEY,"
				 + "name 			TEXT NOT NULL,"
				 + "player_name		TEXT NOT NULL,"
				 + "points			INTEGER,"
				 + "rival			INTEGER,"
				 + "c_points	    INTEGER,"
				 + "c_pred_left_pts INTEGER,"
				 + "c_pred_total_pts INTEGER,"
				 + "c_lowest_week	INTEGER,"
				 + "c_highest_week  INTEGER,"
				 + "c_avg_week		INTEGER,"
				 + "bank			DECIMAL(5,1),"
				 + "value			DECIMAL(5,1),"
				 + "bankvalue		DECIMAL(5,1),"
				 + "c_form			NUMBER,"
				 + "c_hits			INTEGER,"
				 + "c_transfers		INTEGER,"
				 + "c_bench			INTEGER,"
				 + "c_goals			INTEGER,"
				 + "c_assists			INTEGER,"
				 + "c_cs				INTEGER,"
				 + "c_captain_points	INTEGER,"
				 + "c_captain_percent	INTEGER,"
				 + "c_autosubs			INTEGER,"
				 + "c_autosub_points	INTEGER,"
				 + "c_transfer_gains	INTEGER"
				 + ");");
		
		db.execSQL("CREATE TABLE squad_gameweek ("
				 + "squad_id		INTEGER NOT NULL,"
				 + "gameweek		INTEGER NOT NULL,"
				 + "points			INTEGER,"
				 + "points_hit		INTEGER,"
				 + "transfers_next	INTEGER,"
				 + "c_bench			INTEGER,"
				 + "c_points	    INTEGER,"
				 + "c_complete		INTEGER,"
				 + "c_p_playing		INTEGER,"
				 + "c_p_to_play		INTEGER,"
				 + "c_goals			INTEGER,"
				 + "c_assists		INTEGER,"
				 + "c_cs			INTEGER,"
				 + "c_bonus			INTEGER,"
				 + "not_started		INTEGER,"
				 + "cup_opp			INTEGER,"
				 + "chip			INTEGER,"
				 + "PRIMARY KEY (squad_id, gameweek)"
				 + ");");
		
		db.execSQL("CREATE TABLE squad_gameweek_player ("
				 + "squad_id		INTEGER NOT NULL,"
				 + "gameweek		INTEGER NOT NULL,"
				 + "fpl_player_id	INTEGER NOT NULL,"
				 + "selected		INTEGER NOT NULL,"
				 + "captain			INTEGER NOT NULL,"
				 + "autosub			INTEGER,"
				 + "vc   			INTEGER,"
				 + "bought_price	DECIMAL(5,1),"
				 + "PRIMARY KEY (squad_id, gameweek, fpl_player_id)"
				 + ");");
		
		db.execSQL("CREATE TABLE watchlist ("
				 + "player_fpl_id INTEGER PRIMARY KEY"
				 + ");");
		
		db.execSQL("CREATE TABLE minileague ("
				 + "_id 			INTEGER PRIMARY KEY,"
				 + "name			TEXT NOT NULL,"
				 + "num_teams		INTEGER,"
				 + "position		INTEGER,"
				 + "start_week		INTEGER"
				 + ");");
		
		db.execSQL("CREATE TABLE minileague_team ("
				 + "squad_id 		INTEGER NOT NULL,"
				 + "minileague_id	INTEGER NOT NULL,"
				 + "points			INTEGER,"
				 + "c_points		INTEGER,"
				 + "current_gw_pts	INTEGER,"
				 + "PRIMARY KEY (squad_id, minileague_id)"
				 + ");");
		
		db.execSQL("CREATE INDEX minileague_team_squad_ind ON minileague_team (squad_id);");
		
		db.execSQL("CREATE INDEX minileague_team_minileague_ind ON minileague_team (minileague_id);");
		
		db.execSQL("CREATE TABLE vidiprinter ("
				 + "_id				INTEGER PRIMARY KEY AUTOINCREMENT,"
				 + "gameweek		INTEGER NOT NULL,"
				 + "datetime		INTEGER NOT NULL,"
				 + "message			TEXT NOT NULL,"
				 + "player_fpl_id	INTEGER,"
				 + "category		INTEGER"
				 + ");");
		
		db.execSQL("CREATE INDEX vidiprinter_player_ind ON vidiprinter (player_fpl_id, gameweek);");
		
		db.execSQL("CREATE TABLE transfer ("
				 + "player_fpl_id INTEGER NOT NULL,"
				 + "squad_id      INTEGER NOT NULL,"
				 + "gameweek      INTEGER NOT NULL,"
				 + "t_in          INTEGER,"
				 + "PRIMARY KEY (player_fpl_id, squad_id, gameweek, t_in)"
				 + ");");
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		App.log("onUpgrade dbMoi " + oldVersion + " -> " + newVersion);
		
		if (oldVersion < 11) {
			v11_add_current_gw_pts(db);
		}
		
		if (oldVersion < 12) {
			v12_add_value(db);
		}
		
		if (oldVersion < 13) {
			v13_add_cup_opp(db);
		}
		
	}
	
	private void v13_add_cup_opp(SQLiteDatabase db) {
		App.log("v13_add_cup_opp");
		
		db.execSQL("ALTER TABLE squad_gameweek ADD cup_opp INTEGER;");
		
		App.log("done v13");
	}

	private void v12_add_value(SQLiteDatabase db) {
		App.log("v12_add_value");
		
		db.execSQL("ALTER TABLE squad ADD value	DECIMAL(5,1);");
		db.execSQL("ALTER TABLE squad ADD bankvalue	DECIMAL(5,1);");
		
		App.log("done v12");
	}

	private void v11_add_current_gw_pts(SQLiteDatabase db) {
		App.log("v11_add_current_gw_pts");
		
		db.execSQL("ALTER TABLE minileague_team ADD current_gw_pts INTEGER;");
		
		App.log("done v11");
	}

}

