package com.avtar.truckkeeper;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Created by avtar on 6/26/15.
 */
public class TruckKeeperApplication  extends Application implements BootstrapNotifier {
    private static final String TAG = "AndroidProximity";
    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;

    private BeaconManager beaconManager;
    private Handler mHandler;
    private AtomicBoolean serviceActive;
    private Beacon targetBeacon;

    private Runnable stopLocationService = new Runnable(){
        public void run(){
            Log.d(TAG, "filtered did exit region.");
            Intent intent = new Intent(TruckKeeperApplication.this, LocationService.class);
            getApplicationContext().stopService(intent);
            serviceActive.set(false);
        }
    };

    public void onCreate() {
        super.onCreate();
        beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        serviceActive = new AtomicBoolean(false);
        // By default the AndroidBeaconLibrary will only find AltBeacons.  If you wish to make it
        // find a different type of beacon, you must specify the byte layout for that beacon's
        // advertisement with a line like below.  The example shows how to find a beacon with the
        // same byte layout as AltBeacon but with a beaconTypeCode of 0xaabb.  To find the proper
        // layout expression for other beacon types, do a web search for "setBeaconLayout"
        // including the quotes.
        //
        // beaconManager.getBeaconParsers().add(new BeaconParser().
        //        setBeaconLayout("m:2-3=aabb,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        //

        Log.d(TAG, "setting up background monitoring for beacons and power saving");
        // wake up the app when a beacon is seen
        Region region = new Region("backgroundRegion",
                Identifier.parse("2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6"), null, null);
        //Region region = new Region("backgroundRegion",null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);


        // simply constructing this class and holding a reference to it in your custom Application
        // class will automatically cause the BeaconLibrary to save battery whenever the application
        // is not visible.  This reduces bluetooth power usage by about 60%
        //backgroundPowerSaver = new BackgroundPowerSaver(this);
        beaconManager.setDebug(true);
        beaconManager.setBackgroundBetweenScanPeriod(7000l);
        beaconManager.setBackgroundScanPeriod(3000l);
        beaconManager.setForegroundBetweenScanPeriod(7000l);
        beaconManager.setForegroundScanPeriod(3000l);
        try{
            beaconManager.updateScanPeriods();
        }catch (RemoteException e){
            e.printStackTrace();
        }

        mHandler = new Handler();

        // If you wish to test beacon detection in the Android Emulator, you can use code like this:
        // BeaconManager.setBeaconSimulator(new TimedBeaconSimulator() );
        // ((TimedBeaconSimulator) BeaconManager.getBeaconSimulator()).createTimedSimulatedBeacons();
    }

    @Override
    public void didEnterRegion(Region arg0) {
        // In this example, this class sends a notification to the user whenever a Beacon
        // matching a Region (defined above) are first seen.
        Log.d(TAG, "did enter region");
        Log.d(TAG, "region is: "+arg0.toString());
        Log.d(TAG, "region unique: "+arg0.getUniqueId());

        if(!serviceActive.get()){
            Intent intent = new Intent(this, LocationService.class);
            getApplicationContext().startService(intent);
            serviceActive.set(true);
        }
        //no matter what we dont want the handler to be posting messages to the callback
        //mHandler.removeCallbacks(stopLocationService);

    }

    @Override
    public void didExitRegion(Region region) {
        //Log.d(TAG, "did exit region.");
        //apply first order filter ... cancal if
        Log.v(TAG, "noisy exit");
        //mHandler.postDelayed(stopLocationService, 120000l);
        if(serviceActive.get() == true){
            Intent intent = new Intent(TruckKeeperApplication.this, LocationService.class);
            getApplicationContext().stopService(intent);
            serviceActive.set(false);
        }



    }
    @Override
    public void didDetermineStateForRegion(int i, Region region) {
        Log.d(TAG, "did determine region state");
    }



}
