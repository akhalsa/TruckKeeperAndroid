package com.avtar.truckkeeper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by avtar on 6/29/15.
 */
public class GasDataSource {
    private SQLiteDatabase database;
    private TruckKeeperSQLiteHelper dbHelper;
    private String[] allColumns = {TruckKeeperSQLiteHelper.COLUMN_ID,
            TruckKeeperSQLiteHelper.COLUMN_GALLONS, TruckKeeperSQLiteHelper.COLUMN_STATE,
            TruckKeeperSQLiteHelper.COLUMN_TIME_STAMP};

    public GasDataSource(Context context) {
        dbHelper = new TruckKeeperSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public GasEventPOJO createGasEvent(double gallons, String state, long timestamp) {
        ContentValues values = new ContentValues();
        values.put(TruckKeeperSQLiteHelper.COLUMN_GALLONS, gallons);
        values.put(TruckKeeperSQLiteHelper.COLUMN_STATE, state);
        values.put(TruckKeeperSQLiteHelper.COLUMN_TIME_STAMP, timestamp);
        long insertId = database.insert(TruckKeeperSQLiteHelper.TABLE_GAS_EVENT, null,
                values);
        Cursor cursor = database.query(TruckKeeperSQLiteHelper.TABLE_GAS_EVENT,
                allColumns, TruckKeeperSQLiteHelper.COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        GasEventPOJO event = cursorToGasEvent(cursor);
        cursor.close();

        return event;
    }

    private GasEventPOJO cursorToGasEvent(Cursor cursor) {
        GasEventPOJO event = new GasEventPOJO();

        event.setId(cursor.getLong(0));
        event.setGallons(cursor.getDouble(1));
        event.setState(cursor.getString(2));
        event.setTime_stamp(cursor.getLong(3));


        return event;
    }

    public List<GasEventPOJO> getAllEvents() {
        List<GasEventPOJO> events = new ArrayList<GasEventPOJO>();

        Cursor cursor = database.query(TruckKeeperSQLiteHelper.TABLE_GAS_EVENT,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            GasEventPOJO event = cursorToGasEvent(cursor);
            events.add(event);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return events;
    }

}