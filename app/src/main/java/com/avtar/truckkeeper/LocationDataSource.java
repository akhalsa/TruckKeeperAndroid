package com.avtar.truckkeeper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by avtar on 6/26/15.
 */
public class LocationDataSource {
    private SQLiteDatabase database;
    private TruckKeeperSQLiteHelper dbHelper;
    private String[] allColumns = { TruckKeeperSQLiteHelper.COLUMN_ID,
            TruckKeeperSQLiteHelper.COLUMN_LATITUDE, TruckKeeperSQLiteHelper.COLUMN_LONGITUDE,
            TruckKeeperSQLiteHelper.COLUMN_STATE,TruckKeeperSQLiteHelper.COLUMN_DISTANCE_TO_PREV,
            TruckKeeperSQLiteHelper.COLUMN_TIME_STAMP};


    public LocationDataSource(Context context) {
        dbHelper = new TruckKeeperSQLiteHelper(context);
    }


    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public LocationPOJO createLocation(double latitude, double longitude, String state, long timestamp, double prev_dist) {


        ContentValues values = new ContentValues();
        values.put(TruckKeeperSQLiteHelper.COLUMN_LATITUDE, latitude);
        values.put(TruckKeeperSQLiteHelper.COLUMN_LONGITUDE, longitude);
        values.put(TruckKeeperSQLiteHelper.COLUMN_STATE, state);
        values.put(TruckKeeperSQLiteHelper.COLUMN_DISTANCE_TO_PREV, prev_dist);
        values.put(TruckKeeperSQLiteHelper.COLUMN_TIME_STAMP, timestamp);
        long insertId = database.insert(TruckKeeperSQLiteHelper.TABLE_LOCATIONS, null,
                values);
        Cursor cursor = database.query(TruckKeeperSQLiteHelper.TABLE_LOCATIONS,
                allColumns, TruckKeeperSQLiteHelper.COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        LocationPOJO location = cursorToLocation(cursor);
        cursor.close();

        return location;
    }

    private LocationPOJO cursorToLocation(Cursor cursor) {
        LocationPOJO location = new LocationPOJO();

        location.setId(cursor.getLong(0));
        location.setLatitude(cursor.getDouble(1));
        location.setLongitude(cursor.getDouble(2));
        location.setState(cursor.getString(3));
        location.setDist_to_prev((cursor.getDouble(4)));
        location.setTime_stamp(cursor.getLong(5));


        return location;
    }

    public void deleteLocation(LocationPOJO loc) {
        long id = loc.getId();
        database.delete(TruckKeeperSQLiteHelper.TABLE_LOCATIONS, TruckKeeperSQLiteHelper.COLUMN_ID
                + " = " + id, null);

    }

    public List<LocationPOJO> getAllLocations() {
        List<LocationPOJO> locations = new ArrayList<LocationPOJO>();

        Cursor cursor = database.query(TruckKeeperSQLiteHelper.TABLE_LOCATIONS,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            LocationPOJO location = cursorToLocation(cursor);
            locations.add(location);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return locations;
    }
    public List<LocationPOJO> getLocationsBetweenTimes(long start, long end){
        List<LocationPOJO> locations = new ArrayList<LocationPOJO>();
        String date_constraints = TruckKeeperSQLiteHelper.COLUMN_TIME_STAMP+" < "+end+" AND "
                + TruckKeeperSQLiteHelper.COLUMN_TIME_STAMP+" > "+start;

        Cursor cursor = database.query(TruckKeeperSQLiteHelper.TABLE_LOCATIONS,
                allColumns, date_constraints, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            LocationPOJO location = cursorToLocation(cursor);
            locations.add(location);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return locations;
    }

    public LocationPOJO getMostRecentLocation(){
        Cursor cursor = database.query(TruckKeeperSQLiteHelper.TABLE_LOCATIONS,
                allColumns, null, null, null, null, null);

        if(cursor.getCount() == 0){
            return null;
        }
        cursor.moveToLast();
        LocationPOJO location = cursorToLocation(cursor);
        Log.d("DATA SOURCE", "most recent location id: "+location.getId());
        cursor.close();
        return location;
    }
}
