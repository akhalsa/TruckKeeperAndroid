package com.avtar.truckkeeper.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.avtar.truckkeeper.R;
import com.avtar.truckkeeper.dao.GasEventPOJO;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by avtar on 7/2/15.
 */
public class GasLogAdapter extends ArrayAdapter<GasEventPOJO> {
    public GasLogAdapter(Context context, ArrayList<GasEventPOJO> events) {
        super(context, 0, events);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        GasEventPOJO event = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.gas_event_view, parent, false);
        }
        // Lookup view for data population
        TextView state_name = (TextView) convertView.findViewById(R.id.gas_event_state);
        state_name.setText(event.getState());

        TextView quantity = (TextView) convertView.findViewById(R.id.gas_event_quantity);
        quantity.setText(Double.valueOf(event.getGallons()).toString());

        TextView time_stamp = (TextView) convertView.findViewById(R.id.gas_event_date);
        Calendar myCalendar = Calendar.getInstance();
        myCalendar.setTimeInMillis(event.getTime_stamp());
        myCalendar.set(Calendar.HOUR_OF_DAY, 0);
        myCalendar.set(Calendar.MINUTE, 0);
        myCalendar.set(Calendar.SECOND, 0);
        myCalendar.set(Calendar.MILLISECOND, 0);
        String myFormat = "MM/dd/yy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        time_stamp.setText(sdf.format(myCalendar.getTime()));
        return convertView;
    }

}
