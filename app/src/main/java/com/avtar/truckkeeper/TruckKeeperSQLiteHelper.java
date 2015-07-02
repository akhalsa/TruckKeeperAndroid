package com.avtar.truckkeeper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by avtar on 6/26/15.
 */
public class TruckKeeperSQLiteHelper extends SQLiteOpenHelper {


    private static final String DATABASE_NAME = "truck_keeper.db";
    private static final int DATABASE_VERSION = 2;


    public static final String TABLE_LOCATIONS = "locations";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_STATE = "state";
    public static final String COLUMN_DISTANCE_TO_PREV = "dist";
    public static final String COLUMN_TIME_STAMP = "time";

    public static final String TABLE_GAS_EVENT = "gas_events";
    public static final String COLUMN_GALLONS = "gallons";




    // Database creation sql statement
    private static final String LOCATIONS_TABLE_CREATE = "create table "
            + TABLE_LOCATIONS + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_LATITUDE + " FLOAT not null, "
            + COLUMN_LONGITUDE + " FLOAT not null, "
            + COLUMN_STATE + " text, "
            + COLUMN_DISTANCE_TO_PREV + " FLOAT not null,"
            + COLUMN_TIME_STAMP + " DATETIME not null);";


    private static final String GAS_TABLE_CREATE = "create table "
            + TABLE_GAS_EVENT + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_GALLONS +" integer not null, "
            + COLUMN_STATE + "text, "
            + COLUMN_TIME_STAMP + " DATETIME not null);";


    public TruckKeeperSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d("SQL", "CONSTRUCTOR FOR SQLite Helper finished");
    }





    @Override
    public void onCreate(SQLiteDatabase database) {

        database.execSQL(LOCATIONS_TABLE_CREATE);
        database.execSQL(GAS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TruckKeeperSQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        onCreate(db);
    }
}
