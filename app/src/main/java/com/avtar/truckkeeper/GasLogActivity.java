package com.avtar.truckkeeper;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.sql.SQLException;

/**
 * Created by avtar on 6/29/15.
 */
public class GasLogActivity extends AppCompatActivity implements GlobalConstants {

    private Button add_event;
    private GasDataSource mGasDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gas_log);
        mGasDataSource = new GasDataSource(GasLogActivity.this);
        try{
            mGasDataSource.open();
        }catch (SQLException e){
            e.printStackTrace();
        }

        add_event = (Button) findViewById(R.id.add_entry);
        add_event.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater factory = LayoutInflater.from(GasLogActivity.this);
                final View gasEntryView = factory.inflate(R.layout.gas_dialog, null);
                AlertDialog.Builder alert = new AlertDialog.Builder(GasLogActivity.this);
                alert.setTitle("Add Gas Entry");
                alert.setView(gasEntryView);
                alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final EditText gallons = (EditText) gasEntryView.findViewById(R.id.editTextGallons);

                        final EditText state = (EditText) gasEntryView.findViewById(R.id.editTextState);

                        Log.d("GAS", "will add: " + gallons.getText() + " in state: " + state.getText());
                        mGasDataSource.createGasEvent(Double.parseDouble(gallons.getText().toString()),
                                state.getText().toString(), System.currentTimeMillis() );
                    }
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });
                alert.show();
            }
        });


    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);

    }


}
