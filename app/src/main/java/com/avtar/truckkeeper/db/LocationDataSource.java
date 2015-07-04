package com.avtar.truckkeeper.db;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import com.avtar.truckkeeper.GlobalConstants;
import com.avtar.truckkeeper.dao.DrivePOJO;
import com.avtar.truckkeeper.dao.LocationPOJO;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by avtar on 6/26/15.
 */
public class LocationDataSource implements GlobalConstants{
    private Context ctx;
    private SQLiteDatabase database;
    private TruckKeeperSQLiteHelper dbHelper;
    private String[] allLocationColumns = {
            TruckKeeperSQLiteHelper.COLUMN_ID, TruckKeeperSQLiteHelper.COLUMN_DRIVE_ID,
            TruckKeeperSQLiteHelper.COLUMN_LATITUDE, TruckKeeperSQLiteHelper.COLUMN_LONGITUDE,
            TruckKeeperSQLiteHelper.COLUMN_STATE,TruckKeeperSQLiteHelper.COLUMN_DISTANCE_TO_PREV,
            TruckKeeperSQLiteHelper.COLUMN_PREV_LOC_ID, TruckKeeperSQLiteHelper.COLUMN_TIME_STAMP};
    List<String> allLocationColumnsList = Arrays.asList(allLocationColumns);

    private String[] allDriveColumns = {
            TruckKeeperSQLiteHelper.COLUMN_ID
    };


    public LocationDataSource(Context context) {
        dbHelper = new TruckKeeperSQLiteHelper(context);
        ctx = context;
    }


    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }



    private LocationPOJO cursorToLocation(Cursor cursor) {
        LocationPOJO location = new LocationPOJO();


        location.setId(cursor.getInt(allLocationColumnsList.indexOf(TruckKeeperSQLiteHelper.COLUMN_ID)));
        location.setDrive_id(cursor.getLong(allLocationColumnsList.indexOf(TruckKeeperSQLiteHelper.COLUMN_DRIVE_ID)));
        location.setLatitude(cursor.getDouble(allLocationColumnsList.indexOf(TruckKeeperSQLiteHelper.COLUMN_LATITUDE)));
        location.setLongitude(cursor.getDouble(allLocationColumnsList.indexOf(TruckKeeperSQLiteHelper.COLUMN_LONGITUDE)));
        location.setState(cursor.getString(allLocationColumnsList.indexOf(TruckKeeperSQLiteHelper.COLUMN_STATE)));
        location.setDist_to_prev(cursor.getDouble(allLocationColumnsList.indexOf(TruckKeeperSQLiteHelper.COLUMN_DISTANCE_TO_PREV)));
        location.setPrev_id(cursor.getInt(allLocationColumnsList.indexOf(TruckKeeperSQLiteHelper.COLUMN_PREV_LOC_ID)));
        location.setTime_stamp(cursor.getLong(allLocationColumnsList.indexOf(TruckKeeperSQLiteHelper.COLUMN_TIME_STAMP)));

        return location;
    }


    public List<LocationPOJO> getLocationsBetweenTimes(long start, long end){
        List<LocationPOJO> locations = new ArrayList<LocationPOJO>();
        String date_constraints = TruckKeeperSQLiteHelper.COLUMN_TIME_STAMP+" < "+end+" AND "
                + TruckKeeperSQLiteHelper.COLUMN_TIME_STAMP+" > "+start
                + " AND "+TruckKeeperSQLiteHelper.COLUMN_STATE+" IS NOT NULL AND "
                + TruckKeeperSQLiteHelper.COLUMN_STATE+" != \"\"";

        Cursor cursor = database.query(TruckKeeperSQLiteHelper.TABLE_LOCATIONS,
                allLocationColumns, date_constraints, null, null, null, null);

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

    public DrivePOJO getNewDrive(){
        long insertId = database.insert(TruckKeeperSQLiteHelper.TABLE_DRIVES, null,
                null);
        DrivePOJO drive = new DrivePOJO();
        drive.setId(insertId);
        return drive;

    }

    public int createNewLocationForDrive(DrivePOJO drive, double latitude, double longitude){
        //first we need to find out if there are any other locations for this drive
        String drive_constraint = TruckKeeperSQLiteHelper.COLUMN_DRIVE_ID +" = "+drive.getId();
        Cursor cursor = database.query(TruckKeeperSQLiteHelper.TABLE_LOCATIONS,
                allLocationColumns, drive_constraint, null, null, null, null);
        int prev_id = 0;
        if(cursor.getCount() != 0){
            cursor.moveToLast();
            prev_id = cursor.getInt(allLocationColumnsList.indexOf(TruckKeeperSQLiteHelper.COLUMN_ID));
            Log.d("DATABASE", "found other locations for this drive, so prev_id is: "+prev_id);
        }
        ContentValues values = new ContentValues();
        values.put(TruckKeeperSQLiteHelper.COLUMN_LATITUDE, latitude);
        values.put(TruckKeeperSQLiteHelper.COLUMN_LONGITUDE, longitude);
        values.put(TruckKeeperSQLiteHelper.COLUMN_TIME_STAMP, System.currentTimeMillis());
        values.put(TruckKeeperSQLiteHelper.COLUMN_PREV_LOC_ID, prev_id);
        values.put(TruckKeeperSQLiteHelper.COLUMN_DRIVE_ID, drive.getId());
        int insertId = (int)database.insert(TruckKeeperSQLiteHelper.TABLE_LOCATIONS, null,
                values);
        if(insertId == -1){
            Log.d("DATABASE", "location insertion failure");
        }else{
            Log.d("DATABASE", "successful location insertion: " + insertId);
        }
        return insertId;
    }
    public LocationPOJO getLocationWithId(int id){
        String id_constraint = TruckKeeperSQLiteHelper.COLUMN_ID +" = "+id;
        Cursor cursor = database.query(TruckKeeperSQLiteHelper.TABLE_LOCATIONS,
                allLocationColumns, id_constraint, null, null, null, null);
        if(cursor.getCount() != 1){
            Log.e(DATABASE, "INVALID COUNT SEARCHING FOR LOCATION");
            System.exit(0);
        }
        cursor.moveToFirst();
        return cursorToLocation(cursor);
    }

    public void updateLocationPOJO(LocationPOJO loc, String state, double dist_to_prev){
        String strFilter = TruckKeeperSQLiteHelper.COLUMN_ID +" = "+loc.getId();

        ContentValues args = new ContentValues();
        args.put(TruckKeeperSQLiteHelper.COLUMN_STATE, state);
        args.put(TruckKeeperSQLiteHelper.COLUMN_DISTANCE_TO_PREV, dist_to_prev);
        int i = database.update(TruckKeeperSQLiteHelper.TABLE_LOCATIONS, args, strFilter, null);
        if(i != 1){
            Log.e(DATABASE, "UPDATED THE WRONG NUMBER OF LOCATIONS");
            System.exit(0);
        }

    }
    public void computeUnknownDistances(){
        //this is the workhorse function
        //lets first get a list of all the locations with no states and populate state positions


        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (LocationDataSource.this){
                    String missing_state_constraint = TruckKeeperSQLiteHelper.COLUMN_STATE +" IS NULL";
                    Cursor cursor = database.query(TruckKeeperSQLiteHelper.TABLE_LOCATIONS,
                            allLocationColumns, missing_state_constraint, null, null, null, null);
                    while (cursor.moveToNext()) {
                        LocationPOJO loc = cursorToLocation(cursor);
                        try{
                            String state = computeState(loc.convertToLocation());
                            int distance_meters = 0;
                            if(loc.getPrev_id() != 0){
                                //how far is it to the prev location
                                LocationPOJO prev_loc = getLocationWithId(loc.getPrev_id());
                                Log.d("UPDATE", "running for start coordinates: "+prev_loc.getLatitude()+","+prev_loc.getLongitude());
                                Log.d("UPDATE", "running for end coordinates: "+loc.getLatitude()+","+loc.getLongitude());
                                Log.d("UPDATE", "checking between id: "+loc.getId()+" and id: "+prev_loc.getId());
                                distance_meters = GetDistance(prev_loc.getLatitude(), prev_loc.getLongitude(),
                                        loc.getLatitude(), loc.getLongitude());
                            }
                            Log.d(DATABASE, "computed distance of: "+distance_meters+" between id: "+loc.getId()+ " and "+loc.getPrev_id());
                            updateLocationPOJO(loc, state, distance_meters);
                        }catch(IOException e){
                            e.printStackTrace();
                            Log.e(DATABASE, "IO EXCEPTION... BAILING UNTIL NEXT UPDATE COMMAND");
                            break;
                        }
                    }
                    cursor.close();
                    cursor = database.query(TruckKeeperSQLiteHelper.TABLE_LOCATIONS,
                            allLocationColumns, missing_state_constraint, null, null, null, null);
                    if(cursor.getCount() != 0){
                        Log.e(DATABASE, "WTF, we still have missing states?!?!");
                    }else{
                        ctx.sendBroadcast(new Intent(LOCATION_UPDATE));
                    }

                }
            }
        });
        t.start();
    }
    private int GetDistance(final double start_lat, final double start_long, final double end_lat, final double end_long) throws IOException {
        Log.d("GPS", "starting get distance routine");
        StringBuilder urlString = new StringBuilder();
        urlString.append("http://maps.googleapis.com/maps/api/directions/json?");
        urlString.append("origin=");//from
        urlString.append( Double.toString(start_lat));
        urlString.append(",");
        urlString.append( Double.toString(start_long));
        urlString.append("&destination=");//to
        urlString.append(Double.toString(end_lat));
        urlString.append(",");
        urlString.append( Double.toString(end_long));
        urlString.append("&mode=driving&sensor=true");
        Log.d("xxx","URL="+urlString.toString());

        // get the JSON And parse it to get the directions data.
        HttpURLConnection urlConnection= null;
        URL url = null;

        try{
            url = new URL(urlString.toString());
            urlConnection=(HttpURLConnection)url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.connect();
            InputStream inStream = urlConnection.getInputStream();
            BufferedReader bReader = new BufferedReader(new InputStreamReader(inStream));

            String temp, response = "";
            while((temp = bReader.readLine()) != null){
                //Parse data
                response += temp;
            }
            //Close the reader, stream & connection
            bReader.close();
            inStream.close();
            urlConnection.disconnect();

            Log.d(GMAPS_RESPONSE, response);
            //Sortout JSONresponse
            JSONObject object = (JSONObject) new JSONTokener(response).nextValue();
            JSONArray array = object.getJSONArray("routes");
            //Log.d("JSON","array: "+array.toString());

            //Routes is a combination of objects and arrays
            JSONObject routes = array.getJSONObject(0);
            //Log.d("JSON","routes: "+routes.toString());

            String summary = routes.getString("summary");
            //Log.d("JSON","summary: "+summary);

            JSONArray legs = routes.getJSONArray("legs");
            Log.d("GPS","legs: "+legs.toString());

            JSONObject steps = legs.getJSONObject(0);
            //Log.d("JSON","steps: "+steps.toString());

            JSONObject distance = steps.getJSONObject("distance");
            //Log.d("JSON","distance: "+distance.toString());

            String sDistance = distance.getString("text");
            int iDistance = distance.getInt("value");

            Log.d("GPS", "found distance: "+iDistance);
            return iDistance;
        }catch (MalformedURLException e){
            e.printStackTrace();
        } catch(IOException e){
            throw e;
        } catch(JSONException e){
            e.printStackTrace();
        }

        return 0;
    }

    private String computeState(Location l) throws IOException{
        Geocoder geocoder = new Geocoder(ctx, Locale.getDefault());

        Log.d(STATE_COMPUTE, "handling intent");
        // Check if receiver was properly registered.

        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(
                    l.getLatitude(),
                    l.getLongitude(),
                    // In this sample, get just a single address.
                    1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            //Log.e(STATE_COMPUTE, "ioexception: "+ioException);
            throw ioException;
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            Log.e(STATE_COMPUTE, "Latitude = " + l.getLatitude() +
                    ", Longitude = " +
                    l.getLongitude(), illegalArgumentException);
        }
        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            Log.e(STATE_COMPUTE, "no address found for this GPS pin");
            return "";
        }
        Log.d(STATE_COMPUTE, "address found to be: " + addresses.get(0));
        return addresses.get(0).getAdminArea();
    }


}
