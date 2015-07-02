package com.avtar.truckkeeper;

import android.app.Service;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

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
import java.util.List;
import java.util.Locale;

/**
 * Created by avtar on 6/26/15.
 */
public class LocationService extends Service implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener, GlobalConstants
{
    private static final String TAG = "GPS SERVICE";
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LocationDataSource mLocationDataSource;


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
        mLocationRequest.setInterval(20000);
        mLocationRequest.setFastestInterval(5000);
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
                LocationPOJO prev_loc_pojo = mLocationDataSource.getMostRecentLocation();
                Location prev_loc = null;
                if(prev_loc_pojo != null){
                    Log.d("GPS", "prev_loc_pojo is not null");
                    prev_loc = new Location("");
                    prev_loc.setLongitude(prev_loc_pojo.getLongitude());
                    prev_loc.setLatitude(prev_loc_pojo.getLatitude());
                }
                if(prev_loc == null) {

                }


                if((prev_loc == null) || (location.distanceTo(prev_loc) > 250)){

                    Log.d("GPS", "displacement sufficient to record point");
                    Thread t = new Thread(new Runnable(){
                        public void run(){

                            reverseGeoCode(location);
                        }
                    });
                    t.start();
                }else{
                    Log.d("GPS", "displacement was NOT sufficient to record point");
                }


            }else{
                Log.d(TAG, "GPS location thrown out because its accuracy was: " + location.getAccuracy());
            }

        }
    }

    private void reverseGeoCode(Location l){
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        Log.d(TAG, "handling intent");
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
            Log.e(TAG, "ioexception: "+ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            Log.e(TAG, "Latitude = " + l.getLatitude() +
                    ", Longitude = " +
                    l.getLongitude(), illegalArgumentException);
        }
        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            Log.e(TAG, "no address found for this GPS pin");
            return;
        }
        Log.d(TAG, "address found to be: "+addresses.get(0));
        Address address = addresses.get(0);
        String state = address.getAdminArea();
        LocationPOJO prev_loc = mLocationDataSource.getMostRecentLocation();
        if(prev_loc == null){
            mLocationDataSource.createLocation(l.getLatitude(), l.getLongitude(),
                    state, System.currentTimeMillis(), 0);
        }else{
            mLocationDataSource.createLocation(l.getLatitude(), l.getLongitude(), state, System.currentTimeMillis(),
                    GetDistance(prev_loc.getLatitude(), prev_loc.getLongitude(), l.getLatitude(), l.getLongitude()));
        }
        sendBroadcast(new Intent(LOCATION_UPDATE));

    }

    private int GetDistance(double start_lat, double start_long, double end_lat, double end_long) {
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
            e.printStackTrace();
        } catch(JSONException e){
            e.printStackTrace();
        }

        return 0;
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
