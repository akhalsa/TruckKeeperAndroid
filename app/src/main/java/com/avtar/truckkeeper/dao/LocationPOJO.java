package com.avtar.truckkeeper.dao;

import android.location.Location;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by avtar on 6/26/15.
 */
public class LocationPOJO {
    private int id;
    private int prev_id;
    private long drive_id;
    private double latitude;
    private double longitude;
    private String state;
    private double dist_to_prev;
    private long time_stamp;


    public int getPrev_id() {
        return prev_id;
    }

    public void setPrev_id(int prev_id) {
        this.prev_id = prev_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getTime_stamp() {
        return time_stamp;
    }

    public void setTime_stamp(long time_stamp) {
        this.time_stamp = time_stamp;
    }
    public double getDist_to_prev() {
        return dist_to_prev;
    }

    public void setDist_to_prev(double dist_to_prev) {
        this.dist_to_prev = dist_to_prev;
    }

    public long getDrive_id() {
        return drive_id;
    }

    public void setDrive_id(long drive_id) {
        this.drive_id = drive_id;
    }




    // Will be used by the ArrayAdapter in the ListView
    @Override
    public String toString() {
        String time_format_string =  "MM/dd/yyyy";


        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(time_format_string);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time_stamp);
        String timestamp =  formatter.format(calendar.getTime());
        return state+": "+Double.valueOf(latitude)+", "+Double.valueOf(longitude)+" at: "+timestamp;
    }

    public Location convertToLocation(){
        Location l = new Location("");
        l.setLatitude(latitude);
        l.setLongitude(longitude);
        return l;

    }
}
