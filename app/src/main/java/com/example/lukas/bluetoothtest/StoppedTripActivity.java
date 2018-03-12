package com.example.lukas.bluetoothtest;

import android.content.ContentValues;
import android.icu.text.DateFormat;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class StoppedTripActivity extends AppCompatActivity {
    private static final String CLASS = StoppedTripActivity.class.getName();

    private EditText et_mileageEnd;
    private TextView tv_tsStart;
    private TextView tv_tsEnd;
    private Button bt_save;

    private TripRecord record;
    private TripOpenHelper helper;

    private boolean bt_change_menu = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stopped_trip);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        // Back-Button hinzufügen
        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);

        // Referenzvariablen zu den Feldern deklarieren
        et_mileageEnd = (EditText) findViewById(R.id.et_mileageEnd);
        tv_tsStart = (TextView) findViewById(R.id.tv_tsStart);
        tv_tsEnd = (TextView) findViewById(R.id.tv_tsEnd);
        bt_save = (Button) findViewById(R.id.bt_save);

        // Listener für den Save-Button
        bt_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bt_change_menu) {
                    // Zurück zum Hauptmenü
                    finish();
                    bt_change_menu = false;
                } else {
                    // Trip speichern und auf der Oberfläche bleiben
                    saveRecord();
                }
            }
        });

        // Felder mit Inhalten füllen
        record = record.getTripRecord();

        String date = TripsAdapter.convertDate(record.getStartTimestamp());
        tv_tsStart.setText(date);
        date = TripsAdapter.convertDate(record.getEndTimestamp());
        tv_tsEnd.setText(date);

        // Google Map hinzufügen
        GoogleMapFragment mapFragment = GoogleMapFragment.newInstance(getApplicationContext(), -1, GoogleMapFragment.MAP_MODE_DISPLAY);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.stopped_container, mapFragment);
        fragmentTransaction.commit();

    }

    // Überprüfen der Eingaben und ggf. abspeichern des Trips in die DB
    private void saveRecord() {
        String mileageEnd = et_mileageEnd.getText().toString();
        // Der Kilometerstand muss eingegeben werden
        if(mileageEnd.length() == 0) {
            showToast(R.string.stop_odometer_missing, Toast.LENGTH_SHORT);
        // Der Kilometerstand nach der Fahrt muss höher als der Startkilometerstand sein
        } else if(Integer.parseInt(mileageEnd) <= record.getStartMileage()) {
            showToast(R.string.stop_odometer_low, Toast.LENGTH_SHORT);
        }

        else {
            // Kilometerstand nach dem Trip noch in den Record schreiben
            record.setEndMileage(Integer.parseInt(mileageEnd));
            // ansonsten Trip in DB schreiben
            ContentValues values = new ContentValues();
            values.put(TripOpenHelper.COL_DRIVER_NAME, record.getDriver());
            values.put(TripOpenHelper.COL_MILEAGE_START, record.getStartMileage());
            values.put(TripOpenHelper.COL_MILEAGE_END, record.getEndMileage());
            values.put(TripOpenHelper.COL_DRIVE_MODE, record.getDriveMode());
            values.put(TripOpenHelper.COL_TS_START, record.getStartTimestamp());
            values.put(TripOpenHelper.COL_TS_END, record.getEndTimestamp());
            values.put(TripOpenHelper.COL_ADDRESS_START, record.getStartAddress());
            values.put(TripOpenHelper.COL_ADDRESS_END, record.getEndAddress());
            values.put(TripOpenHelper.COL_ROUTE_POINTS, record.getRoutePoints());
            getContentResolver().insert(TripProvider.CONTENT_URI, values);
            /*helper = helper.getHelper(this);
            helper.insert(record.getDriver(),
                    record.getStartMileage(),
                    record.getEndMileage(),
                    record.getDriveMode(),
                    record.getStartTimestamp(),
                    record.getEndTimestamp(),
                    record.getStartAddress(),
                    record.getEndAddress());*/
            bt_save.setText(R.string.stop_button_menu);
            // Flag, um den Button anzupassen (--> man gelangt anschließend ins Hauptmenü zurück)
            bt_change_menu = true;
        }
    }

    private void showToast (int message, int duration) {
        Toast.makeText(StoppedTripActivity.this, message, duration).show();
    }
}
