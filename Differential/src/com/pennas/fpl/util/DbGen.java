package com.pennas.fpl.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.pennas.fpl.App;
import com.pennas.fpl.Settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Build;

public class DbGen extends SQLiteOpenHelper{
	 
    //The Android's default system path of your application database.
    //private static String DB_PATH = "/data/data/com.pennas.fpl/databases/";
 
    private static String DB_NAME_IN = "DiffGen.mp3";
    private static String DB_NAME = "DiffGen";
    
    public static final String TRANSACTION_IMMEDIATE_BEGIN = "BEGIN IMMEDIATE TRANSACTION";
    public static final String TRANSACTION_END = "END TRANSACTION";
    
    // genenerally, this will not be changed now. use upgrade scripts instead
    // (still package updated db for new users)
    public static final int DB_VERSION = 46;
    // v46 2.2.2 10/08/2015
    //  - Add chip to SGW (force wipe..)
    // v45 2.2
    //  - 2015/16
    // v41 2.1
    //  - 2014/15
    // v25->40 2.0 16/07/2013
    //  - correct team short names
    // xx (not issued as new version so dbgen not replaced) 1.8.4 17/10/2012
    //  - new rival stats + nti stats + nti columns
    // v24 1.8.0 09/09/2012
    //  - correct team short names
    // v23 1.7.3 24/08/2012
    //  - fpl match IDs for 2012/13
    // v22 1.7.2 18/08/2012
    //  - update prem team names
    // v20/21 1.7.0 30/06/2012
    //  - extend gw38 end to 2050
    //  - all updates for 2012/13
    // v18/19 1.6.0
    //  - with got_bonus marked for 10/11 
    //  - correct team_goals_on_pitch hist
    // v16/17 14/03/2012
    //  - generic stats bonus readability + owned perc
    // v15 11/03/2012
    //  - games over y/z text
    // v14 28/01/2012
    //  - first choice keepers updated
    // v13 07/01/2012
    //  - more squad stat defs
    // v12 02/01/2012
    //  - add pm.price
    // v11 02/01/2012
    //  - squad stat defs
    // v10 29/12/2011
    //  - updated deadlines
    // v8/9 03/12/2011
    //  - updated team prem names
    // v7 1/10/2011
    //  - force same db in, as may have been corrupted by prev version
    // v5/6 26/09/2011
    //  - add generic bonus stats
    // v4 24/09/2011
    //  - player remove first name, compress news
    // v3 23/09/2011
    //  - fixture/player ticker int/compression
    // v2 23/09/2011
    //  - player_season to int for 1.2.0
    
    private Context myContext;
 
    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     * @param context
     */
    public DbGen(Context context) {
    	super(context, DB_NAME, null, DB_VERSION);
        this.myContext = context;
    }
    
    @SuppressLint("NewApi")
	private static File get_db_sd_card_file() {
    	return new File(App.appContext.getExternalFilesDir(null), "diffgen15.db3");
    }
    
    public static void copy_to_sd_card(Activity activity) {
    	InputStream in;
		try {
			in = new FileInputStream(activity.getDatabasePath(DB_NAME));
			
			File outFile = get_db_sd_card_file();
			App.log("db outfile = " + outFile.getAbsolutePath());
	        OutputStream out = new FileOutputStream(outFile);
	
	        // Transfer bytes from in to out
	        byte[] buf = new byte[1024];
	        int len;
	        while ((len = in.read(buf)) > 0) {
	            out.write(buf, 0, len);
	        }
	        in.close();
	        out.close();
	        App.log("db outfile written!");
	        
	        Intent intent=new Intent(android.content.Intent.ACTION_SEND);
	        intent.setType("text/plain");
	        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        intent.putExtra(Intent.EXTRA_SUBJECT, "db");

	        // Add data to the intent, the receiving app will decide what to do with it.
	        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(outFile));
	        
	        activity.startActivity(Intent.createChooser(intent, "save db"));
		} catch (FileNotFoundException e) {
			App.exception(e);
		} catch (IOException e) {
			App.exception(e);
		}
    }
    
    public static void delete_db_from_sd_card(Context context) {
    	try {
	    	File f = get_db_sd_card_file();
			f.delete();
    	} catch (Exception e) {
    		App.exception(e);
    	}
    }
 
  /**
     * Creates a empty database on the system and rewrites it with your own database.
     * */
    public SQLiteDatabase createDataBase(Context context) throws IOException{
    	boolean dbExist = checkDataBase(context);
    	int installed_version = Settings.getIntPref(Settings.PREF_DB_GEN_VERSION);
    	App.log("dbGen dbExist = " + dbExist + ",  installed_version = " + installed_version
    			+ " DB_VERSION = " + DB_VERSION);
    	boolean create = false;
    	
    	if(!dbExist || (installed_version < DB_VERSION) ){
    		try {
    			copyDataBase(context);
    			Settings.setIntPref(Settings.PREF_DB_GEN_VERSION, DB_VERSION, App.appContext);
    			create = true;
    		} catch (IOException e) {
        		App.exception(e);
    			throw new Error("Error copying database");
        	}
    	}
    	
    	SQLiteDatabase db = getWritableDatabase();
    	
    	if (create) {
    		DbMoi.create_dbMoi_tables(db);
    	}
    	
    	// init database
    	if (!dbExist) {
    		db.execSQL("UPDATE updated SET updated_datetime = -1");
    	}
    	
    	return db;
    }
 
    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     * @return true if it exists, false if it doesn't
     */
    private boolean checkDataBase(Context context){
    	File file = context.getDatabasePath(DB_NAME);
    	return file.exists();
    }
 
    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     * */
    private void copyDataBase(Context context) throws IOException{
    	App.log("Copy new database: " + DB_NAME);
    	
    	//Open your local db as the input stream
    	InputStream myInput = myContext.getAssets().open(DB_NAME_IN);
    	ZipInputStream zin = new ZipInputStream(myInput);
    	ZipEntry ze = zin.getNextEntry();
    	
    	//By calling this method and empty database will be created into the default system path
        //of your application so we are gonna be able to overwrite that database with our database.
		SQLiteDatabase t = this.getReadableDatabase();
		if (t != null) t.close();
 
    	// Path to the just created empty db
    	//String outFileName = DB_PATH + DB_NAME;
		File outFileName = context.getDatabasePath(DB_NAME);
 
    	App.log("Unzipping " + ze.getName() + " to " + outFileName);
    	
    	//Open the empty db as the output stream
    	OutputStream myOutput = new FileOutputStream(outFileName);
    	
    	//transfer bytes from the inputfile to the outputfile
    	byte[] buffer = new byte[2056];
    	int length;
    	while ((length = zin.read(buffer))>0){
    		myOutput.write(buffer, 0, length);
    	}
    	
        zin.closeEntry();
 
    	//Close the streams
    	myOutput.flush();
    	myOutput.close();
    	zin.close();
    	myInput.close();
    	
    	myContext = null;
    }
    
    @SuppressLint("NewApi")
	public static void enable_write_ahead() {
    	if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
    		App.log("enable write-ahead logging..");
        	boolean wal = App.dbGen.enableWriteAheadLogging();
    		App.log("wal = " + wal);
    	}
    }
 
    @Override
	public void onCreate(SQLiteDatabase db) {

	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
 
	}
 
    public static byte[] compress(String string) {
    	if (string == null) {
	    	return null;
	    }
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream(string.length());
		    GZIPOutputStream gos = new GZIPOutputStream(os);
		    gos.write(string.getBytes());
		    gos.close();
		    byte[] compressed = os.toByteArray();
		    os.close();
		    return compressed;
		} catch (IOException e) {
			App.exception(e);
			return null;
		}
	}

	public static String decompress(byte[] compressed) {
	    if (compressed == null) {
	    	return null;
	    }
		try {
			final int BUFFER_SIZE = 32;
		    ByteArrayInputStream is = new ByteArrayInputStream(compressed);
		    GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
		    StringBuilder string = new StringBuilder();
		    byte[] data = new byte[BUFFER_SIZE];
		    int bytesRead;
		    while ((bytesRead = gis.read(data)) != -1) {
		        string.append(new String(data, 0, bytesRead));
		    }
		    gis.close();
		    is.close();
		    return string.toString();
	    } catch (IOException e) {
	    	App.exception(e);
	    	return null;
	    }
	}

}
