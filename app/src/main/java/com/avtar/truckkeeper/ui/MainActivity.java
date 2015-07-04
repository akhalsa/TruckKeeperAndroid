package com.avtar.truckkeeper.ui;

import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.avtar.truckkeeper.GlobalConstants;
import com.avtar.truckkeeper.R;
import com.avtar.truckkeeper.TruckKeeperApplication;
import com.avtar.truckkeeper.dao.LocationPOJO;
import com.avtar.truckkeeper.db.LocationDataSource;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements GlobalConstants {
    private LocationDataSource mLocationDataSource;

    private EditTextListener start_text_listen;
    private EditTextListener end_text_listen;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            reloadData();
        }
    };
    private BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateConnectionStatus();
        }
    };
    private ArrayList<StatePair> pairs;
    public static class StatePair{
        public String state;
        public String distance;
    }

    private class EditTextListener implements DatePickerDialog.OnDateSetListener{
        private EditText edit_text;
        private Calendar myCalendar;

        private void truncateCalenderToDays(){
            myCalendar.set(Calendar.HOUR_OF_DAY, 0);
            myCalendar.set(Calendar.MINUTE, 0);
            myCalendar.set(Calendar.SECOND, 0);
            myCalendar.set(Calendar.MILLISECOND, 0);
        }
        public EditTextListener(EditText et){
            edit_text = et;
            myCalendar = Calendar.getInstance();
            myCalendar.setTimeInMillis(System.currentTimeMillis());
            truncateCalenderToDays();

            edit_text.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction() == MotionEvent.ACTION_UP){
                        new DatePickerDialog(MainActivity.this, EditTextListener.this, myCalendar
                                .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                                myCalendar.get(Calendar.DAY_OF_MONTH)).show();
                    }

                    return true;
                }
            });

            String myFormat = "MM/dd/yy"; //In which you need put here
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
            edit_text.setText(sdf.format(myCalendar.getTime()));

        }
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            // TODO Auto-generated method stub
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            truncateCalenderToDays();
            String myFormat = "MM/dd/yy"; //In which you need put here
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

            edit_text.setText(sdf.format(myCalendar.getTime()));
            reloadData();
        }
        public long getTimeStamp(){
            return myCalendar.getTimeInMillis();
        }

    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLocationDataSource = new LocationDataSource(MainActivity.this);
        try{
            mLocationDataSource.open();
        }catch (SQLException e){
            e.printStackTrace();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d("SQL", "registering from pid: " + android.os.Process.myPid());
        registerReceiver(mReceiver,
                new IntentFilter(LOCATION_UPDATE));
        registerReceiver(mStatusReceiver, new IntentFilter(BEACON_CONNECTION_UPDATE));

        EditText start_text = (EditText) findViewById(R.id.start_text);
        start_text_listen = new EditTextListener(start_text);
        EditText end_text = (EditText) findViewById(R.id.end_text);
        end_text_listen = new EditTextListener(end_text);
        updateConnectionStatus();
        mLocationDataSource.computeUnknownDistances();

        reloadData();
    }

    private void updateConnectionStatus(){
        RelativeLayout bar = (RelativeLayout) findViewById(R.id.connection_status);
        TextView status_text = (TextView) findViewById(R.id.connection_status_text);
        TruckKeeperApplication ta_app = (TruckKeeperApplication)getApplication();
        if(ta_app.getServiceStatus()){
            bar.setBackgroundColor(getResources().getColor(R.color.material_deep_teal_500));
            status_text.setText("Connected");
            status_text.setTextColor(getResources().getColor(R.color.material_blue_grey_900));
        }else{
            bar.setBackgroundColor(getResources().getColor(R.color.material_blue_grey_900));
            status_text.setText("Disconnected");
            status_text.setTextColor(getResources().getColor(R.color.white));

        }

    }
    @Override
    public void onPause(){
        super.onPause();
        unregisterReceiver(mReceiver);
        unregisterReceiver(mStatusReceiver);
    }

    private void reloadData(){
        //first lets get the appropriate time stamps
        if(start_text_listen == null){
            return;
        }else if(end_text_listen == null){
            return;
        }
        long start = start_text_listen.getTimeStamp();
        long end = end_text_listen.getTimeStamp();
        end += 86399999;
        pairs = new ArrayList<StatePair>();
        HashMap<String, Double> mapping = new HashMap<String, Double>();

        List<LocationPOJO> values = mLocationDataSource.getLocationsBetweenTimes(start, end);
        for(LocationPOJO l : values){
            double dist = l.getDist_to_prev();
            Log.d("GPS", "distance between points was: "+dist);
            double total = dist * 0.000621371192f;
            Log.d("GPS", "adding distance: "+total);
            if (!mapping.containsKey(l.getState())){
                mapping.put(l.getState(), 0.0);
            }
            mapping.put(l.getState(), total+mapping.get(l.getState()));
        }

        for(String key : mapping.keySet()){
            StatePair pair = new StatePair();
            pair.distance = Double.valueOf(mapping.get(key)).toString();
            pair.state = key;
            pairs.add(pair);
        }

        LocationAdapter adapter = new LocationAdapter(this,pairs);
        ListView lv = (ListView)findViewById(R.id.list);
        lv.setAdapter(adapter);
        adapter.notifyDataSetChanged();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if(id == R.id.gas_log){
            Intent intent = new Intent(this, GasLogActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }


}
