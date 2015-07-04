package com.avtar.truckkeeper.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.avtar.truckkeeper.R;

import java.util.ArrayList;

/**
 * Created by avtar on 6/27/15.
 */

public class LocationAdapter extends  ArrayAdapter<MainActivity.StatePair> {


    public LocationAdapter(Context context, ArrayList<MainActivity.StatePair> state_pairs) {
        super(context, 0, state_pairs);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        MainActivity.StatePair pair = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.state_distance, parent, false);
        }
        // Lookup view for data population
        TextView state_name = (TextView) convertView.findViewById(R.id.state_name);
        TextView state_distance = (TextView) convertView.findViewById(R.id.state_distance);
        // Populate the data into the template view using the data object
        state_name.setText(pair.state);
        state_distance.setText(pair.distance);
        // Return the completed view to render on screen
        return convertView;
    }

}
