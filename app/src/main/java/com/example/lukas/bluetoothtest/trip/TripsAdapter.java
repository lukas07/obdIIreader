package com.example.lukas.bluetoothtest.trip;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.lukas.bluetoothtest.R;


/**
 * Created by Lukas on 03.12.2017.
 */

public class TripsAdapter extends CursorAdapter {
    private static final int col_rowid = 0, col_driver = 1, col_startTimestamp = 5, col_endTimestamp = 6;


    private LayoutInflater inflater;


    public TripsAdapter(Context context, Cursor c) {
        super(context, c, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(R.layout.activity_trip_entry, null);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView tv_startTimestamp = (TextView) view.findViewById(R.id.tv_startTimestamp);
        TextView tv_endTimestamp = (TextView) view.findViewById(R.id.tv_endTimestamp);
        TextView tv_driver = (TextView) view.findViewById(R.id.tv_driver);
        // Inhalt der Textviews füllen
        tv_driver.setText(cursor.getString(col_driver));
        tv_startTimestamp.setText(convertDate(cursor.getLong(col_startTimestamp)));
        String test = tv_startTimestamp.getText().toString();
        tv_endTimestamp.setText(convertDate(cursor.getLong(col_endTimestamp)));
    }

    // Kovertiert einen Timestamp in das gewünschte Format, da das Datum in diesem Format im Triprecord steht
    // Beispiel: 20171202151123 --> 02.12.2017 15:11
    public static String convertDate(long timestamp) {
        String day, month, year, hours, minutes;
        year = String.valueOf(timestamp).substring(0, 4);
        month = String.valueOf(timestamp).substring(4, 6);
        day = String.valueOf(timestamp).substring(6, 8);
        hours = String.valueOf(timestamp).substring(8, 10);
        minutes = String.valueOf(timestamp).substring(10, 12);

        String test =  day + "." + month + "." + year + ' ' + hours + ":" + minutes;
        return test;
    }

}
