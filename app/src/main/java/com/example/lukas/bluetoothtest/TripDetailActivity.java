package com.example.lukas.bluetoothtest;

import android.database.Cursor;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class TripDetailActivity extends AppCompatActivity {
    private TextView tv_detail_driver, tv_detail_mode, tv_detail_startTs,
            tv_detail_endTs, tv_detail_startMil, tv_detail_endMil,
            tv_detail_startAdd, tv_detail_endAdd;

    private int col_driver = 1, col_mode = 2, col_startMil = 3,
            col_endMil = 4, col_startTs = 5, col_endTs = 6,
            col_startAdd = 7, col_endAdd = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        // Back-Button hinzufügen
        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);

        // Referenzvariablen zu den Feldern deklarieren
        tv_detail_driver = (TextView) findViewById(R.id.tv_detail_driver);
        tv_detail_mode = (TextView) findViewById(R.id.tv_detail_mode);
        tv_detail_startTs = (TextView) findViewById(R.id.tv_detail_startTs);
        tv_detail_endTs = (TextView) findViewById(R.id.tv_detail_endTs);
        tv_detail_startMil = (TextView) findViewById(R.id.tv_detail_startMil);
        tv_detail_endMil = (TextView) findViewById(R.id.tv_detail_endMil);
        tv_detail_startAdd = (TextView) findViewById(R.id.tv_detail_startAdd);
        tv_detail_endAdd = (TextView) findViewById(R.id.tv_detail_endAdd);


        // Daten des Trips aus DB lesen
        final Cursor cursor;
        Bundle extra = getIntent().getExtras();
        long rowid = extra.getLong("rowid");
        cursor = getContentResolver().query(TripProvider.CONTENT_URI, null, "_id=?", new String[]{Long.toString(rowid)}, null);
        if(!cursor.moveToFirst()) {
            Log.e(TripDetailActivity.class.getName(), "Result set is empty");
            this.finish();
        }
        // TextViews mit den Werten füllen
        tv_detail_driver.setText(cursor.getString(col_driver));
        tv_detail_mode.setText(cursor.getString(col_mode));
        tv_detail_startTs.setText(TripsAdapter.convertDate(cursor.getLong(col_startTs)));
        tv_detail_endTs.setText(TripsAdapter.convertDate(cursor.getLong(col_endTs)));
        tv_detail_startMil.setText(cursor.getString(col_startMil));
        tv_detail_endMil.setText(cursor.getString(col_endMil));
        tv_detail_startAdd.setText(cursor.getString(col_startAdd));
        tv_detail_endAdd.setText(cursor.getString(col_endAdd));
    }
}
