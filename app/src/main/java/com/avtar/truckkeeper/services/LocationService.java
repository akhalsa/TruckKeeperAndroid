package com.avtar.truckkeeper.services;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.avtar.truckkeeper.GlobalConstants;
import com.avtar.truckkeeper.dao.DrivePOJO;
import com.avtar.truckkeeper.dao.LocationPOJO;
import com.avtar.truckkeeper.db.LocationDataSource;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.sql.SQLException;

/**
 * Created by avtar on 6/26/15.
 */
public class LocationService extends Service implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener, GlobalConstants
{
    private static final String TAG = "GPS SERVICE";
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LocationDataSource mLocationDataSource;
    private DrivePOJO mDrive;
    private int most_recent_id;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.e(TAG, "onCreate");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mLocationDataSource = new LocationDataSource(LocationService.this);
        try{
            mLocationDataSource.open();

        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.e(TAG, "onDestory");
        //disable location stuff
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        Log.i(TAG, "Service onStartCommand");
            //start by obtaining last known location
        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if( ConnectionResult.SUCCESS == result ){
            mGoogleApiClient.connect();

        }else{
            Log.d(TAG, "play service availability problem: "+result);
        }

        return Service.START_STICKY;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "connected to service");
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            Log.d(TAG, "last location lat is: " + String.valueOf(mLastLocation.getLatitude()));
            Log.d(TAG, "last location long is: " + String.valueOf(mLastLocation.getLongitude()));

        }
        mLocationRequest = new LocationRequest();
        mDrive = mLocationDataSource.getNewDrive();
        most_recent_id = 0;
        mLocationRequest.setInterval(20000);
        mLocationRequest.setFastestInterval(3000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }
    @Override
    public void onLocationChanged(final Location location) {
        if (location != null) {
            Log.d(TAG, "updated location lat: " + String.valueOf(location.getLatitude()));
            Log.d(TAG, "updated location long: " + String.valueOf(location.getLongitude()));


            if(location.getAccuracy() <= 30){
                boolean sufficient_displacement = false;
                if(most_recent_id == 0){
                    sufficient_displacement = true;
                } else{
                    LocationPOJO prev_loc_pojo = mLocationDataSource.getLocationWithId(most_recent_id);
                    //if (location.distanceTo(prev_loc_pojo.convertToLocation()) > 150){
                        sufficient_displacement = true;
                    //}
                }
                if(sufficient_displacement){
                    Log.d("GPS", "displacement sufficient to record point");
                    most_recent_id = mLocationDataSource.createNewLocationForDrive(mDrive, location.getLatitude(), location.getLongitude());
                }else{
                    Log.d("GPS", "displacement was NOT sufficient to record point");
                }

            }else{
                Log.d(TAG, "GPS location thrown out because its accuracy was: " + location.getAccuracy());
            }

        }
    }




    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection suspended with code: " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection failed with result: " + connectionResult);
    }


}
